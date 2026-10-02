package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlin.math.max

class CricketRepository(private val dao: CricketDao) {

    val allPlayersFlow: Flow<List<PlayerEntity>> = dao.getAllPlayersFlow()
    val allTournamentsFlow: Flow<List<TournamentEntity>> = dao.getAllTournamentsFlow()
    val allTournamentTeamsFlow: Flow<List<TournamentTeamEntity>> = dao.getAllTournamentTeamsFlow()
    val allTournamentFixturesFlow: Flow<List<TournamentFixtureEntity>> = dao.getAllTournamentFixturesFlow()
    val allMatchesFlow: Flow<List<MatchEntity>> = dao.getAllMatchesFlow()
    val allInningsFlow: Flow<List<InningsEntity>> = dao.getAllInningsFlow()
    val allBallEventsFlow: Flow<List<BallEventEntity>> = dao.getAllBallEventsFlow()
    val latestLiveMatchFlow: Flow<MatchEntity?> = dao.getLatestLiveMatchFlow()

    fun getMatchByIdFlow(matchId: Long): Flow<MatchEntity?> = dao.getMatchByIdFlow(matchId)
    fun getInningsForMatchFlow(matchId: Long): Flow<List<InningsEntity>> = dao.getInningsForMatchFlow(matchId)
    fun getBallEventsForMatchFlow(matchId: Long): Flow<List<BallEventEntity>> = dao.getBallEventsForMatchFlow(matchId)

    suspend fun resetAllData() {
        dao.deleteAllBallEvents()
        dao.deleteAllInnings()
        dao.deleteAllMatches()
        dao.deleteAllTournamentFixtures()
        dao.deleteAllTournamentTeams()
        dao.deleteAllTournaments()
        dao.deleteAllPlayers()
    }

    suspend fun clearMatchHistoryOnly() {
        dao.deleteAllBallEvents()
        dao.deleteAllInnings()
        dao.deleteAllMatches()
    }

    suspend fun deleteTeamAndPlayers(teamName: String) {
        dao.deletePlayersByTeam(teamName)
    }

    // =========================================================================
    // TOURNAMENT MANAGEMENT
    // =========================================================================

    suspend fun createTournament(
        name: String,
        organizer: String,
        venue: String,
        format: String,
        oversLimit: Int,
        maxOversPerBowler: Int,
        playersPerTeam: Int,
        pointsForWin: Int = 2,
        pointsForTie: Int = 1,
        teamNames: List<String>,
        autoGenerateLeagueFixtures: Boolean
    ): Long {
        val cleanName = name.trim().ifBlank { "Championship Cup" }
        val tournamentId = dao.insertTournament(
            TournamentEntity(
                name = cleanName,
                organizer = organizer.trim(),
                venue = venue.trim().ifBlank { "Main Cricket Ground" },
                format = format,
                oversLimit = oversLimit,
                maxOversPerBowler = maxOversPerBowler,
                playersPerTeam = playersPerTeam,
                pointsForWin = pointsForWin,
                pointsForTie = pointsForTie,
                status = "ONGOING"
            )
        )

        val distinctTeams = teamNames.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (distinctTeams.isNotEmpty()) {
            dao.insertTournamentTeams(
                distinctTeams.map { tName ->
                    TournamentTeamEntity(
                        tournamentId = tournamentId,
                        teamName = tName,
                        groupName = "Group A"
                    )
                }
            )
        }

        if (autoGenerateLeagueFixtures && distinctTeams.size >= 2) {
            generateRoundRobinFixtures(tournamentId)
        }

        return tournamentId
    }

    suspend fun registerTeamInTournament(
        tournamentId: Long,
        teamName: String,
        groupName: String = "Group A"
    ) {
        val clean = teamName.trim()
        if (clean.isBlank()) return
        val existing = dao.getTournamentTeamsOnce(tournamentId)
        if (existing.none { it.teamName.equals(clean, ignoreCase = true) }) {
            dao.insertTournamentTeam(
                TournamentTeamEntity(
                    tournamentId = tournamentId,
                    teamName = clean,
                    groupName = groupName.trim().ifBlank { "Group A" }
                )
            )
        }
    }

    suspend fun removeTeamFromTournament(tournamentTeamId: Long) {
        dao.deleteTournamentTeam(tournamentTeamId)
    }

    suspend fun generateRoundRobinFixtures(tournamentId: Long) {
        val tourney = dao.getTournamentByIdOnce(tournamentId) ?: return
        val teams = dao.getTournamentTeamsOnce(tournamentId).map { it.teamName }
        if (teams.size < 2) return

        val existingFixtures = dao.getFixturesForTournamentOnce(tournamentId)
        var nextMatchNum = (existingFixtures.maxOfOrNull { it.matchNumber } ?: 0) + 1
        val newFixtures = mutableListOf<TournamentFixtureEntity>()

        for (i in 0 until teams.size) {
            for (j in (i + 1) until teams.size) {
                val tA = teams[i]
                val tB = teams[j]
                val alreadyScheduled = existingFixtures.any {
                    it.stage == "LEAGUE" &&
                        ((it.teamA.equals(tA, true) && it.teamB.equals(tB, true)) ||
                            (it.teamA.equals(tB, true) && it.teamB.equals(tA, true)))
                }
                if (!alreadyScheduled) {
                    newFixtures.add(
                        TournamentFixtureEntity(
                            tournamentId = tournamentId,
                            matchNumber = nextMatchNum++,
                            stage = "LEAGUE",
                            teamA = tA,
                            teamB = tB,
                            scheduledDate = "Match #${nextMatchNum - 1}",
                            venue = tourney.venue,
                            status = "SCHEDULED"
                        )
                    )
                }
            }
        }

        if (newFixtures.isNotEmpty()) {
            dao.insertFixtures(newFixtures)
        }
    }

    suspend fun addCustomFixture(
        tournamentId: Long,
        stage: String,
        teamA: String,
        teamB: String,
        scheduledDate: String,
        venue: String
    ) {
        val tourney = dao.getTournamentByIdOnce(tournamentId) ?: return
        val existing = dao.getFixturesForTournamentOnce(tournamentId)
        val nextNum = (existing.maxOfOrNull { it.matchNumber } ?: 0) + 1
        dao.insertFixture(
            TournamentFixtureEntity(
                tournamentId = tournamentId,
                matchNumber = nextNum,
                stage = stage,
                teamA = teamA.trim(),
                teamB = teamB.trim(),
                scheduledDate = scheduledDate.trim().ifBlank { "Match #$nextNum" },
                venue = venue.trim().ifBlank { tourney.venue },
                status = "SCHEDULED"
            )
        )
    }

    suspend fun generateKnockoutStageFromStandings(
        tournamentId: Long,
        rankedTeams: List<String>,
        knockoutType: String // SEMI_FINALS, FINAL
    ) {
        val tourney = dao.getTournamentByIdOnce(tournamentId) ?: return
        val existing = dao.getFixturesForTournamentOnce(tournamentId)
        var nextNum = (existing.maxOfOrNull { it.matchNumber } ?: 0) + 1

        if (knockoutType == "SEMI_FINALS" && rankedTeams.size >= 4) {
            dao.insertFixtures(
                listOf(
                    TournamentFixtureEntity(
                        tournamentId = tournamentId,
                        matchNumber = nextNum++,
                        stage = "SEMI_FINAL",
                        teamA = rankedTeams[0],
                        teamB = rankedTeams[3],
                        scheduledDate = "Semi-Final 1 (1st vs 4th)",
                        venue = tourney.venue
                    ),
                    TournamentFixtureEntity(
                        tournamentId = tournamentId,
                        matchNumber = nextNum++,
                        stage = "SEMI_FINAL",
                        teamA = rankedTeams[1],
                        teamB = rankedTeams[2],
                        scheduledDate = "Semi-Final 2 (2nd vs 3rd)",
                        venue = tourney.venue
                    )
                )
            )
        } else if (knockoutType == "FINAL" && rankedTeams.size >= 2) {
            // If semi-finals were completed, pick semi-final winners; otherwise top 2 from standings
            val completedSemis = existing.filter { it.stage == "SEMI_FINAL" && it.winnerTeam.isNotBlank() }
            val finalistA = completedSemis.getOrNull(0)?.winnerTeam ?: rankedTeams[0]
            val finalistB = completedSemis.getOrNull(1)?.winnerTeam ?: rankedTeams[1]

            dao.insertFixture(
                TournamentFixtureEntity(
                    tournamentId = tournamentId,
                    matchNumber = nextNum,
                    stage = "FINAL",
                    teamA = finalistA,
                    teamB = finalistB,
                    scheduledDate = "Grand Final",
                    venue = tourney.venue
                )
            )
        }
    }

    suspend fun deleteFixture(fixtureId: Long) {
        dao.deleteFixture(fixtureId)
    }

    suspend fun deleteTournament(tournamentId: Long) {
        dao.deleteFixturesForTournament(tournamentId)
        dao.deleteTournamentTeamsForTournament(tournamentId)
        dao.deleteTournament(tournamentId)
    }

    // =========================================================================
    // PLAYER & SQUAD MANAGEMENT
    // =========================================================================

    suspend fun bulkImportPlayers(defaultTeamName: String, rawText: String): Int {
        val cleanDefaultTeam = defaultTeamName.trim().ifBlank { "Custom Team" }
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") && !it.lowercase().startsWith("name,") }

        if (lines.isEmpty()) return 0

        val playersToInsert = mutableListOf<PlayerEntity>()
        lines.forEachIndexed { idx, line ->
            val parts = line.split(",", ";", "\t").map { it.trim() }
            val name = parts.getOrNull(0).orEmpty()
            if (name.isNotBlank()) {
                val rawRole = parts.getOrNull(1)?.uppercase()?.replace(" ", "_")?.replace("-", "_") ?: ""
                val role = when {
                    rawRole in setOf("BATSMAN", "BOWLER", "ALL_ROUNDER", "WICKET_KEEPER") -> rawRole
                    rawRole == "WK" -> "WICKET_KEEPER"
                    rawRole == "AR" -> "ALL_ROUNDER"
                    rawRole == "BAT" -> "BATSMAN"
                    rawRole == "BOWL" -> "BOWLER"
                    else -> "ALL_ROUNDER"
                }
                val batStyle = parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "Right-Hand Bat"
                val bowlStyle = parts.getOrNull(3)?.takeIf { it.isNotBlank() } ?: "Right-Arm Medium"
                val jersey = parts.getOrNull(4)?.toIntOrNull() ?: ((idx + 1) * 7 % 99).coerceAtLeast(1)

                playersToInsert.add(
                    PlayerEntity(
                        name = name,
                        teamName = cleanDefaultTeam,
                        role = role,
                        battingStyle = batStyle,
                        bowlingStyle = bowlStyle,
                        jerseyNumber = jersey,
                        isWicketKeeper = (role == "WICKET_KEEPER"),
                        squadStatus = "PLAYING_XI"
                    )
                )
            }
        }

        if (playersToInsert.isNotEmpty()) {
            dao.insertPlayers(playersToInsert)
        }
        return playersToInsert.size
    }

    suspend fun addOrUpdatePlayer(player: PlayerEntity): Long {
        return if (player.id == 0L) {
            dao.insertPlayer(player)
        } else {
            dao.updatePlayer(player)
            player.id
        }
    }

    suspend fun deletePlayer(playerId: Long) {
        dao.deletePlayer(playerId)
    }

    // =========================================================================
    // MATCH & INNINGS SCORING ENGINE
    // =========================================================================

    suspend fun startNewMatch(
        teamA: String,
        teamB: String,
        format: String,
        oversLimit: Int,
        maxOversPerBowler: Int,
        playersPerTeam: Int,
        tossWinner: String,
        tossDecision: String,
        venue: String,
        openingStrikerId: Long?,
        openingNonStrikerId: Long?,
        openingBowlerId: Long?,
        allowConsecutiveOvers: Boolean = false,
        tournamentId: Long? = null,
        fixtureId: Long? = null,
        stage: String = "FRIENDLY",
        customStrikerName: String = "",
        customNonStrikerName: String = "",
        customBowlerName: String = ""
    ): Long {
        val cleanTeamA = teamA.trim().ifBlank { "Team A" }
        val cleanTeamB = teamB.trim().ifBlank { "Team B" }

        val battingFirst = if (tossDecision == "BAT") {
            tossWinner.trim().ifBlank { cleanTeamA }
        } else {
            if (tossWinner.trim().equals(cleanTeamA, ignoreCase = true)) cleanTeamB else cleanTeamA
        }
        val bowlingFirst = if (battingFirst.equals(cleanTeamA, ignoreCase = true)) cleanTeamB else cleanTeamA

        var striker = openingStrikerId
        var nonStriker = openingNonStrikerId
        var bowler = openingBowlerId

        // Support instant on-the-fly player creation for random local match players
        if (striker == null && customStrikerName.isNotBlank()) {
            striker = dao.insertPlayer(
                PlayerEntity(
                    name = customStrikerName.trim(),
                    teamName = battingFirst,
                    role = "BATSMAN",
                    squadStatus = "PLAYING_XI"
                )
            )
        }
        if (nonStriker == null && customNonStrikerName.isNotBlank()) {
            nonStriker = dao.insertPlayer(
                PlayerEntity(
                    name = customNonStrikerName.trim(),
                    teamName = battingFirst,
                    role = "BATSMAN",
                    squadStatus = "PLAYING_XI"
                )
            )
        }
        if (bowler == null && customBowlerName.isNotBlank()) {
            bowler = dao.insertPlayer(
                PlayerEntity(
                    name = customBowlerName.trim(),
                    teamName = bowlingFirst,
                    role = "BOWLER",
                    squadStatus = "PLAYING_XI"
                )
            )
        }

        val batSquad = dao.getPlayersByTeamOnce(battingFirst)
            .filter { it.squadStatus != "SUBSTITUTE" }
        val bowlSquad = dao.getPlayersByTeamOnce(bowlingFirst)
            .filter { it.squadStatus != "SUBSTITUTE" }

        if (striker == null) striker = batSquad.getOrNull(0)?.id
        if (nonStriker == null) nonStriker = batSquad.firstOrNull { it.id != striker }?.id
        if (bowler == null) bowler = bowlSquad.lastOrNull()?.id

        val maxInnings = if (format == "TEST") 4 else 2

        val matchId = dao.insertMatch(
            MatchEntity(
                teamA = cleanTeamA,
                teamB = cleanTeamB,
                format = format,
                oversLimit = oversLimit,
                maxOversPerBowler = maxOversPerBowler,
                playersPerTeam = playersPerTeam.coerceIn(2, 15),
                maxInnings = maxInnings,
                tossWinner = tossWinner.trim().ifBlank { cleanTeamA },
                tossDecision = tossDecision,
                venue = venue.trim().ifBlank { "Cricket Ground" },
                status = "LIVE",
                currentInningsNumber = 1,
                allowConsecutiveOvers = allowConsecutiveOvers,
                tournamentId = tournamentId,
                fixtureId = fixtureId,
                stage = stage
            )
        )

        dao.insertInnings(
            InningsEntity(
                matchId = matchId,
                inningsNumber = 1,
                battingTeam = battingFirst,
                bowlingTeam = bowlingFirst,
                strikerId = striker,
                nonStrikerId = nonStriker,
                currentBowlerId = bowler,
                openingStrikerId = striker,
                openingNonStrikerId = nonStriker,
                openingBowlerId = bowler
            )
        )

        // Link Tournament Fixture if started from a Tournament
        if (fixtureId != null) {
            dao.getFixtureByIdOnce(fixtureId)?.let { fixture ->
                dao.updateFixture(
                    fixture.copy(
                        linkedMatchId = matchId,
                        status = "LIVE"
                    )
                )
            }
        }

        return matchId
    }

    suspend fun setOpeningOrActivePlayers(
        inningsId: Long,
        strikerId: Long?,
        nonStrikerId: Long?,
        bowlerId: Long?
    ) {
        val inn = dao.getInningsByIdOnce(inningsId) ?: return
        dao.updateInnings(
            inn.copy(
                strikerId = strikerId ?: inn.strikerId,
                nonStrikerId = nonStrikerId ?: inn.nonStrikerId,
                currentBowlerId = bowlerId ?: inn.currentBowlerId,
                openingStrikerId = inn.openingStrikerId ?: strikerId,
                openingNonStrikerId = inn.openingNonStrikerId ?: nonStrikerId,
                openingBowlerId = inn.openingBowlerId ?: bowlerId
            )
        )
    }

    suspend fun swapStrike(inningsId: Long) {
        val inn = dao.getInningsByIdOnce(inningsId) ?: return
        dao.updateInnings(
            inn.copy(
                strikerId = inn.nonStrikerId,
                nonStrikerId = inn.strikerId
            )
        )
    }

    suspend fun setCurrentBowler(inningsId: Long, newBowlerId: Long): Boolean {
        val inn = dao.getInningsByIdOnce(inningsId) ?: return false
        val match = dao.getMatchByIdOnce(inn.matchId)
        val allowConsecutive = match?.allowConsecutiveOvers == true

        if (!allowConsecutive &&
            inn.legalBalls > 0 &&
            inn.legalBalls % 6 == 0 &&
            inn.lastOverBowlerId != null &&
            newBowlerId == inn.lastOverBowlerId
        ) {
            return false
        }

        dao.updateInnings(inn.copy(currentBowlerId = newBowlerId))
        return true
    }

    suspend fun retireOrReplaceBatter(
        matchId: Long,
        inningsId: Long,
        retiringPlayerId: Long,
        newBatterId: Long
    ) {
        recordBall(
            matchId = matchId,
            inningsId = inningsId,
            runsOffBat = 0,
            extraType = "NONE",
            extraRuns = 0,
            isWicket = true,
            wicketType = "RETIRED_HURT",
            dismissedPlayerId = retiringPlayerId,
            fielderName = "",
            nextBatterId = newBatterId,
            countsAsLegalBallOverride = false
        )
    }

    suspend fun addPenaltyRuns(matchId: Long, inningsId: Long, penaltyRuns: Int = 5) {
        val inn = dao.getInningsByIdOnce(inningsId) ?: return
        val events = dao.getBallEventsForInningsOnce(inningsId)
        val striker = inn.strikerId?.let { dao.getPlayerById(it) }
        val nonStriker = inn.nonStrikerId?.let { dao.getPlayerById(it) }
        val bowler = inn.currentBowlerId?.let { dao.getPlayerById(it) }

        val event = BallEventEntity(
            matchId = matchId,
            inningsId = inningsId,
            inningsNumber = inn.inningsNumber,
            sequenceNumber = (events.lastOrNull()?.sequenceNumber ?: 0) + 1,
            overNumber = inn.legalBalls / 6,
            ballInOver = (inn.legalBalls % 6) + 1,
            strikerId = inn.strikerId ?: 0L,
            strikerName = striker?.name ?: "Striker",
            nonStrikerId = inn.nonStrikerId ?: 0L,
            nonStrikerName = nonStriker?.name ?: "Non-Striker",
            bowlerId = inn.currentBowlerId ?: 0L,
            bowlerName = bowler?.name ?: "Bowler",
            runsOffBat = 0,
            extraType = "PENALTY",
            extraRuns = penaltyRuns,
            totalBallRuns = penaltyRuns,
            isLegalDelivery = false,
            isWicket = false,
            wicketType = "NONE",
            commentary = "Penalty +$penaltyRuns runs awarded to ${inn.battingTeam}."
        )
        dao.insertBallEvent(event)
        recalculateInningsAndMatchState(matchId, inningsId)
    }

    suspend fun recordBall(
        matchId: Long,
        inningsId: Long,
        runsOffBat: Int,
        extraType: String = "NONE",
        extraRuns: Int = 0,
        isWicket: Boolean = false,
        wicketType: String = "NONE",
        dismissedPlayerId: Long? = null,
        fielderName: String = "",
        nextBatterId: Long? = null,
        countsAsLegalBallOverride: Boolean? = null
    ) {
        val inn = dao.getInningsByIdOnce(inningsId) ?: return
        if (inn.isCompleted) return

        val match = dao.getMatchByIdOnce(matchId)
        val allowConsecutive = match?.allowConsecutiveOvers == true
        if (wicketType != "RETIRED_HURT" && extraType != "PENALTY") {
            if (inn.currentBowlerId == null) return
            if (!allowConsecutive &&
                inn.legalBalls > 0 &&
                inn.legalBalls % 6 == 0 &&
                inn.lastOverBowlerId != null &&
                inn.currentBowlerId == inn.lastOverBowlerId
            ) {
                return
            }
        }

        val events = dao.getBallEventsForInningsOnce(inningsId)
        val strikerId = inn.strikerId ?: 0L
        val nonStrikerId = inn.nonStrikerId ?: 0L
        val bowlerId = inn.currentBowlerId ?: 0L

        val striker = dao.getPlayerById(strikerId)
        val nonStriker = dao.getPlayerById(nonStrikerId)
        val bowler = dao.getPlayerById(bowlerId)
        val dismissedPlayer = dismissedPlayerId?.let { dao.getPlayerById(it) } ?: striker
        val nextBatter = nextBatterId?.let { dao.getPlayerById(it) }

        val isLegal = countsAsLegalBallOverride ?: (extraType !in setOf("WIDE", "NO_BALL", "PENALTY") && wicketType != "RETIRED_HURT")
        val overNum = inn.legalBalls / 6
        val ballInOver = if (isLegal) (inn.legalBalls % 6) + 1 else (inn.legalBalls % 6)
        val totalRuns = runsOffBat + extraRuns

        val comm = buildCommentary(
            bowlerName = bowler?.name ?: "Bowler",
            strikerName = striker?.name ?: "Striker",
            runsOffBat = runsOffBat,
            extraType = extraType,
            extraRuns = extraRuns,
            isWicket = isWicket,
            wicketType = wicketType,
            dismissedName = dismissedPlayer?.name ?: "Batter",
            fielderName = fielderName
        )

        val event = BallEventEntity(
            matchId = matchId,
            inningsId = inningsId,
            inningsNumber = inn.inningsNumber,
            sequenceNumber = (events.lastOrNull()?.sequenceNumber ?: 0) + 1,
            overNumber = overNum,
            ballInOver = ballInOver,
            strikerId = strikerId,
            strikerName = striker?.name ?: "Striker",
            nonStrikerId = nonStrikerId,
            nonStrikerName = nonStriker?.name ?: "Non-Striker",
            bowlerId = bowlerId,
            bowlerName = bowler?.name ?: "Bowler",
            runsOffBat = runsOffBat,
            extraType = extraType,
            extraRuns = extraRuns,
            totalBallRuns = totalRuns,
            isLegalDelivery = isLegal,
            isWicket = isWicket,
            wicketType = wicketType,
            dismissedPlayerId = if (isWicket) (dismissedPlayerId ?: strikerId) else null,
            dismissedPlayerName = if (isWicket) (dismissedPlayer?.name ?: "") else "",
            fielderName = fielderName,
            nextBatterId = nextBatterId,
            nextBatterName = nextBatter?.name ?: "",
            isBoundaryFour = (runsOffBat == 4),
            isBoundarySix = (runsOffBat == 6),
            commentary = comm
        )

        dao.insertBallEvent(event)
        recalculateInningsAndMatchState(matchId, inningsId)
    }

    suspend fun undoLastBall(matchId: Long, inningsId: Long) {
        val events = dao.getBallEventsForInningsOnce(inningsId)
        val lastEvent = events.lastOrNull() ?: return
        dao.deleteBallEventById(lastEvent.id)
        recalculateInningsAndMatchState(matchId, inningsId, forceReopenIfUndone = true)
    }

    private suspend fun recalculateInningsAndMatchState(
        matchId: Long,
        inningsId: Long,
        forceReopenIfUndone: Boolean = false
    ) {
        val match = dao.getMatchByIdOnce(matchId) ?: return
        val inn = dao.getInningsByIdOnce(inningsId) ?: return
        val events = dao.getBallEventsForInningsOnce(inningsId)

        var totalRuns = 0
        var wickets = 0
        var legalBalls = 0
        var wides = 0
        var noBalls = 0
        var byes = 0
        var legByes = 0
        var penalties = 0

        var strikerId = inn.openingStrikerId ?: events.firstOrNull()?.strikerId ?: inn.strikerId
        var nonStrikerId = inn.openingNonStrikerId ?: events.firstOrNull()?.nonStrikerId ?: inn.nonStrikerId
        var currentBowlerId = inn.openingBowlerId ?: events.firstOrNull()?.bowlerId ?: inn.currentBowlerId
        var lastOverBowlerId: Long? = null
        var isFreeHit = false

        for (ev in events) {
            totalRuns += ev.totalBallRuns
            when (ev.extraType) {
                "WIDE" -> wides += ev.extraRuns
                "NO_BALL" -> noBalls += ev.extraRuns
                "BYE" -> byes += ev.extraRuns
                "LEG_BYE" -> legByes += ev.extraRuns
                "PENALTY" -> {
                    penalties += ev.extraRuns
                    continue
                }
            }

            strikerId = ev.strikerId
            nonStrikerId = ev.nonStrikerId
            currentBowlerId = ev.bowlerId

            if (ev.isWicket) {
                if (ev.wicketType != "RETIRED_HURT") {
                    wickets += 1
                }
                val outId = ev.dismissedPlayerId ?: strikerId
                if (outId == strikerId) {
                    strikerId = ev.nextBatterId
                } else if (outId == nonStrikerId) {
                    nonStrikerId = ev.nextBatterId
                }
            }

            val physicalRuns = when (ev.extraType) {
                "WIDE" -> max(0, ev.extraRuns - 1)
                "NO_BALL" -> if (ev.runsOffBat > 0) ev.runsOffBat else max(0, ev.extraRuns - 1)
                "BYE", "LEG_BYE" -> ev.extraRuns
                else -> ev.runsOffBat
            }

            if (physicalRuns % 2 == 1) {
                val temp = strikerId
                strikerId = nonStrikerId
                nonStrikerId = temp
            }

            if (ev.isLegalDelivery) {
                legalBalls += 1
                isFreeHit = false
                if (legalBalls % 6 == 0) {
                    val temp = strikerId
                    strikerId = nonStrikerId
                    nonStrikerId = temp
                    lastOverBowlerId = ev.bowlerId
                    currentBowlerId = if (match.allowConsecutiveOvers) {
                        ev.bowlerId
                    } else {
                        null
                    }
                }
            } else if (ev.extraType == "NO_BALL") {
                isFreeHit = (match.format != "TEST")
            }
        }

        val currentOverIdx = legalBalls / 6
        val hasBallsInCurrentOver = events.any { it.overNumber == currentOverIdx && it.extraType != "PENALTY" }
        if (!hasBallsInCurrentOver && legalBalls > 0 && legalBalls % 6 == 0 && currentBowlerId == null) {
            if (inn.currentBowlerId != null && (match.allowConsecutiveOvers || inn.currentBowlerId != lastOverBowlerId)) {
                currentBowlerId = inn.currentBowlerId
            }
        }

        val effectiveOversLimit = if (inn.inningsNumber == 2 && match.revisedOvers != null) {
            match.revisedOvers
        } else {
            match.oversLimit
        }
        val maxLegalBalls = effectiveOversLimit * 6
        val maxWickets = (match.playersPerTeam - 1).coerceAtLeast(1)
        val effectiveTarget = if (inn.inningsNumber == 2 && match.revisedTarget != null) {
            match.revisedTarget
        } else {
            inn.targetRuns
        }

        val targetReached = effectiveTarget != null && totalRuns >= effectiveTarget
        val allOut = wickets >= maxWickets
        val oversCompleted = match.format != "TEST" && legalBalls >= maxLegalBalls

        val isInningsComplete = if (forceReopenIfUndone) {
            targetReached || allOut || oversCompleted
        } else {
            inn.isDeclared || targetReached || allOut || oversCompleted
        }

        val updatedInnings = inn.copy(
            totalRuns = totalRuns,
            wickets = wickets,
            legalBalls = legalBalls,
            extrasWide = wides,
            extrasNoBall = noBalls,
            extrasBye = byes,
            extrasLegBye = legByes,
            extrasPenalty = penalties,
            targetRuns = effectiveTarget,
            isCompleted = isInningsComplete,
            strikerId = strikerId,
            nonStrikerId = nonStrikerId,
            currentBowlerId = currentBowlerId,
            lastOverBowlerId = lastOverBowlerId,
            isFreeHit = isFreeHit
        )
        dao.updateInnings(updatedInnings)

        evaluateMatchStatusAndResult(matchId)
    }

    suspend fun declareOrEndInnings(matchId: Long, inningsId: Long, isDeclared: Boolean = false) {
        val inn = dao.getInningsByIdOnce(inningsId) ?: return
        dao.updateInnings(
            inn.copy(
                isCompleted = true,
                isDeclared = isDeclared
            )
        )
        evaluateMatchStatusAndResult(matchId)
    }

    suspend fun startNextInnings(
        matchId: Long,
        enforceFollowOn: Boolean = false,
        openingStrikerId: Long? = null,
        openingNonStrikerId: Long? = null,
        openingBowlerId: Long? = null
    ): Long? {
        val match = dao.getMatchByIdOnce(matchId) ?: return null
        val inningsList = dao.getInningsForMatchOnce(matchId)
        val lastInnings = inningsList.lastOrNull() ?: return null

        if (inningsList.size >= match.maxInnings) return null

        val nextNumber = inningsList.size + 1
        val nextBattingTeam: String
        val nextBowlingTeam: String

        if (match.format == "TEST" && nextNumber == 3 && enforceFollowOn) {
            nextBattingTeam = lastInnings.battingTeam
            nextBowlingTeam = lastInnings.bowlingTeam
        } else {
            nextBattingTeam = lastInnings.bowlingTeam
            nextBowlingTeam = lastInnings.battingTeam
        }

        val target: Int? = if (match.maxInnings == 2 && nextNumber == 2) {
            match.revisedTarget ?: (lastInnings.totalRuns + 1)
        } else if (match.maxInnings == 4 && nextNumber == 4) {
            val battingSoFar = inningsList.filter { it.battingTeam == nextBattingTeam }.sumOf { it.totalRuns }
            val bowlingSoFar = inningsList.filter { it.battingTeam == nextBowlingTeam }.sumOf { it.totalRuns }
            val lead = bowlingSoFar - battingSoFar
            if (lead >= 0) lead + 1 else 1
        } else {
            null
        }

        val batSquad = dao.getPlayersByTeamOnce(nextBattingTeam)
            .filter { it.squadStatus != "SUBSTITUTE" }
        val bowlSquad = dao.getPlayersByTeamOnce(nextBowlingTeam)
            .filter { it.squadStatus != "SUBSTITUTE" }

        val sId = openingStrikerId ?: batSquad.getOrNull(0)?.id
        val nsId = openingNonStrikerId ?: batSquad.firstOrNull { it.id != sId }?.id
        val bId = openingBowlerId ?: bowlSquad.lastOrNull()?.id

        val newInningsId = dao.insertInnings(
            InningsEntity(
                matchId = matchId,
                inningsNumber = nextNumber,
                battingTeam = nextBattingTeam,
                bowlingTeam = nextBowlingTeam,
                targetRuns = target,
                isFollowOn = enforceFollowOn,
                strikerId = sId,
                nonStrikerId = nsId,
                currentBowlerId = bId,
                openingStrikerId = sId,
                openingNonStrikerId = nsId,
                openingBowlerId = bId
            )
        )

        dao.updateMatch(
            match.copy(
                currentInningsNumber = nextNumber,
                status = "LIVE",
                resultText = ""
            )
        )
        return newInningsId
    }

    suspend fun applyDlsRevisedTarget(
        matchId: Long,
        revisedOvers: Int,
        revisedTarget: Int
    ) {
        val match = dao.getMatchByIdOnce(matchId) ?: return
        dao.updateMatch(
            match.copy(
                revisedOvers = revisedOvers,
                revisedTarget = revisedTarget
            )
        )
        val inningsList = dao.getInningsForMatchOnce(matchId)
        val secondInnings = inningsList.find { it.inningsNumber == 2 }
        if (secondInnings != null) {
            dao.updateInnings(secondInnings.copy(targetRuns = revisedTarget))
            recalculateInningsAndMatchState(matchId, secondInnings.id)
        }
    }

    private suspend fun evaluateMatchStatusAndResult(matchId: Long) {
        val match = dao.getMatchByIdOnce(matchId) ?: return
        val inningsList = dao.getInningsForMatchOnce(matchId)
        if (inningsList.isEmpty()) return

        val maxWickets = (match.playersPerTeam - 1).coerceAtLeast(1)

        if (match.format != "TEST" || match.maxInnings == 2) {
            if (inningsList.size == 2) {
                val inn1 = inningsList[0]
                val inn2 = inningsList[1]
                val target = match.revisedTarget ?: (inn1.totalRuns + 1)
                val dlsTag = if (match.revisedTarget != null) " (DLS)" else ""

                if (inn2.totalRuns >= target) {
                    val wicketsRemaining = (maxWickets - inn2.wickets).coerceAtLeast(1)
                    val res = "${inn2.battingTeam} won by $wicketsRemaining wicket${if (wicketsRemaining > 1) "s" else ""}$dlsTag"
                    finalizeMatchWithMvp(match, res, winnerTeam = inn2.battingTeam)
                } else if (inn2.isCompleted) {
                    val diff = (target - 1) - inn2.totalRuns
                    val (res, winner) = when {
                        diff > 0 -> "${inn1.battingTeam} won by $diff run${if (diff > 1) "s" else ""}$dlsTag" to inn1.battingTeam
                        diff == 0 -> "Match Tied$dlsTag" to ""
                        else -> "${inn2.battingTeam} won$dlsTag" to inn2.battingTeam
                    }
                    finalizeMatchWithMvp(match, res, winnerTeam = winner)
                } else if (match.status == "COMPLETED") {
                    dao.updateMatch(match.copy(status = "LIVE", resultText = ""))
                }
            }
        } else {
            val teamAScore = inningsList.filter { it.battingTeam == match.teamA }.sumOf { it.totalRuns }
            val teamBScore = inningsList.filter { it.battingTeam == match.teamB }.sumOf { it.totalRuns }
            val teamAInnings = inningsList.filter { it.battingTeam == match.teamA }
            val teamBInnings = inningsList.filter { it.battingTeam == match.teamB }

            if (inningsList.size == 3 && inningsList.all { it.isCompleted }) {
                if (teamAInnings.size == 2 && teamBInnings.size == 1 && teamAScore < teamBScore) {
                    val diff = teamBScore - teamAScore
                    finalizeMatchWithMvp(match, "${match.teamB} won by an innings and $diff runs", winnerTeam = match.teamB)
                    return
                }
                if (teamBInnings.size == 2 && teamAInnings.size == 1 && teamBScore < teamAScore) {
                    val diff = teamAScore - teamBScore
                    finalizeMatchWithMvp(match, "${match.teamA} won by an innings and $diff runs", winnerTeam = match.teamA)
                    return
                }
            }

            if (inningsList.size == 4) {
                val inn4 = inningsList[3]
                val target = inn4.targetRuns ?: 1
                if (inn4.totalRuns >= target) {
                    val wRem = (maxWickets - inn4.wickets).coerceAtLeast(1)
                    finalizeMatchWithMvp(match, "${inn4.battingTeam} won by $wRem wickets", winnerTeam = inn4.battingTeam)
                } else if (inn4.isCompleted) {
                    val diff = (target - 1) - inn4.totalRuns
                    val (res, winner) = when {
                        diff > 0 -> "${inn4.bowlingTeam} won by $diff runs" to inn4.bowlingTeam
                        diff == 0 -> "Test Match Tied" to ""
                        else -> "Match Drawn" to ""
                    }
                    finalizeMatchWithMvp(match, res, winnerTeam = winner)
                }
            }
        }
    }

    suspend fun concludeTestMatchAsDraw(matchId: Long) {
        val match = dao.getMatchByIdOnce(matchId) ?: return
        val inningsList = dao.getInningsForMatchOnce(matchId)
        inningsList.lastOrNull()?.let { last ->
            if (!last.isCompleted) {
                dao.updateInnings(last.copy(isCompleted = true))
            }
        }
        finalizeMatchWithMvp(match, "Match Drawn", winnerTeam = "")
    }

    private suspend fun finalizeMatchWithMvp(
        match: MatchEntity,
        resultText: String,
        winnerTeam: String
    ) {
        val allPlayers = dao.getAllPlayersOnce()
        val inningsList = dao.getInningsForMatchOnce(match.id)
        val events = inningsList.flatMap { dao.getBallEventsForInningsOnce(it.id) }
        val topMvp = CricketAnalytics.calculateMatchMvp(allPlayers, events).firstOrNull()
        dao.updateMatch(
            match.copy(
                status = "COMPLETED",
                resultText = resultText,
                manOfTheMatch = topMvp?.let { "${it.playerName} (${it.breakdown})" } ?: ""
            )
        )

        // Update linked Tournament Fixture & Tournament Champion if applicable
        if (match.fixtureId != null) {
            dao.getFixtureByIdOnce(match.fixtureId)?.let { fixture ->
                dao.updateFixture(
                    fixture.copy(
                        status = "COMPLETED",
                        winnerTeam = winnerTeam,
                        resultSummary = resultText
                    )
                )
                if (fixture.stage == "FINAL" && match.tournamentId != null && winnerTeam.isNotBlank()) {
                    dao.getTournamentByIdOnce(match.tournamentId)?.let { tourney ->
                        val runnerUp = if (winnerTeam.equals(match.teamA, ignoreCase = true)) match.teamB else match.teamA
                        dao.updateTournament(
                            tourney.copy(
                                status = "COMPLETED",
                                championTeam = winnerTeam,
                                runnerUpTeam = runnerUp
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun deleteMatch(matchId: Long) {
        dao.deleteBallEventsForMatch(matchId)
        dao.deleteInningsForMatch(matchId)
        dao.deleteMatch(matchId)
    }

    private fun buildCommentary(
        bowlerName: String,
        strikerName: String,
        runsOffBat: Int,
        extraType: String,
        extraRuns: Int,
        isWicket: Boolean,
        wicketType: String,
        dismissedName: String,
        fielderName: String
    ): String {
        val base = "$bowlerName to $strikerName"
        if (wicketType == "RETIRED_HURT") {
            return "$dismissedName retires hurt."
        }
        if (isWicket) {
            val how = CricketAnalytics.formatDismissalText(wicketType, bowlerName, fielderName)
            return "$base, OUT! $dismissedName ($how)."
        }
        return when (extraType) {
            "WIDE" -> "$base, WIDE ball" + if (extraRuns > 1) " (+${extraRuns} wides)" else ""
            "NO_BALL" -> "$base, NO BALL!" + if (runsOffBat > 0) " $runsOffBat runs off the bat! Free Hit next." else " Free Hit next."
            "BYE" -> "$base, $extraRuns Bye${if (extraRuns > 1) "s" else ""}."
            "LEG_BYE" -> "$base, $extraRuns Leg Bye${if (extraRuns > 1) "s" else ""}."
            else -> when (runsOffBat) {
                0 -> "$base, no run, solid defense."
                4 -> "$base, FOUR! Crisp boundary through the gap."
                6 -> "$base, SIX! Launched high into the stands!"
                else -> "$base, $runsOffBat run${if (runsOffBat > 1) "s" else ""}, quick running between the wickets."
            }
        }
    }
}
