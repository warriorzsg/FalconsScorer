package com.example.data

import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

data class BatterInningsStat(
    val playerId: Long,
    val playerName: String,
    val runs: Int,
    val balls: Int,
    val fours: Int,
    val sixes: Int,
    val dots: Int,
    val isOut: Boolean,
    val isRetiredHurt: Boolean,
    val dismissalText: String,
    val isStriker: Boolean,
    val isNonStriker: Boolean
) {
    val strikeRate: Double
        get() = if (balls > 0) (runs.toDouble() * 100.0) / balls.toDouble() else 0.0

    val strikeRateFormatted: String
        get() = String.format(Locale.US, "%.1f", strikeRate)
}

data class BowlerInningsStat(
    val playerId: Long,
    val playerName: String,
    val legalBalls: Int,
    val maidens: Int,
    val runsConceded: Int,
    val wickets: Int,
    val dots: Int,
    val wides: Int,
    val noBalls: Int,
    val isCurrentBowler: Boolean
) {
    val oversFormatted: String
        get() = "${legalBalls / 6}.${legalBalls % 6}"

    val economy: Double
        get() = if (legalBalls > 0) (runsConceded.toDouble() * 6.0) / legalBalls.toDouble() else 0.0

    val economyFormatted: String
        get() = String.format(Locale.US, "%.2f", economy)
}

data class PartnershipStat(
    val wicketNumber: Int,
    val batter1Id: Long,
    val batter1Name: String,
    val batter1Runs: Int,
    val batter1Balls: Int,
    val batter2Id: Long,
    val batter2Name: String,
    val batter2Runs: Int,
    val batter2Balls: Int,
    val extras: Int,
    val totalRuns: Int,
    val totalBalls: Int,
    val isUnbroken: Boolean
)

data class FallOfWicketStat(
    val wicketNumber: Int,
    val scoreAtFall: Int,
    val oversAtFall: String,
    val playerName: String,
    val bowlerName: String
)

data class OverSummary(
    val overNumber: Int, // 1-indexed for UI display
    val bowlerName: String,
    val balls: List<BallEventEntity>,
    val runsInOver: Int,
    val wicketsInOver: Int,
    val cumulativeRuns: Int,
    val cumulativeWickets: Int,
    val isMaiden: Boolean
)

data class InningsScorecardAnalysis(
    val innings: InningsEntity,
    val batters: List<BatterInningsStat>,
    val didNotBat: List<PlayerEntity>,
    val bowlers: List<BowlerInningsStat>,
    val partnerships: List<PartnershipStat>,
    val fallOfWickets: List<FallOfWicketStat>,
    val overSummaries: List<OverSummary>,
    val currentOverBalls: List<BallEventEntity>
)

data class PlayerCareerStat(
    val player: PlayerEntity,
    val matchesPlayed: Int,
    val inningsBatted: Int,
    val notOuts: Int,
    val totalRuns: Int,
    val ballsFaced: Int,
    val highestScore: Int,
    val fours: Int,
    val sixes: Int,
    val fifties: Int,
    val hundreds: Int,
    val ballsBowled: Int,
    val runsConceded: Int,
    val wicketsTaken: Int,
    val maidens: Int,
    val bestBowlingWickets: Int,
    val bestBowlingRuns: Int,
    val catchesAndRunOuts: Int
) {
    val battingAverage: Double
        get() {
            val dismissals = inningsBatted - notOuts
            return if (dismissals > 0) totalRuns.toDouble() / dismissals else totalRuns.toDouble()
        }

    val battingStrikeRate: Double
        get() = if (ballsFaced > 0) (totalRuns.toDouble() * 100.0) / ballsFaced else 0.0

    val bowlingEconomy: Double
        get() = if (ballsBowled > 0) (runsConceded.toDouble() * 6.0) / ballsBowled else 0.0

    val bowlingAverage: Double
        get() = if (wicketsTaken > 0) runsConceded.toDouble() / wicketsTaken else 0.0

    val oversBowledFormatted: String
        get() = "${ballsBowled / 6}.${ballsBowled % 6}"

    val bestBowlingFormatted: String
        get() = if (wicketsTaken > 0 || ballsBowled > 0) "$bestBowlingWickets/$bestBowlingRuns" else "-"
}

data class MvpCandidate(
    val playerName: String,
    val teamName: String,
    val impactPoints: Double,
    val breakdown: String
)

data class DlsCalculationResult(
    val team1Score: Int,
    val maxOvers: Int,
    val revisedOvers: Double,
    val currentOversBowled: Double,
    val wicketsLost: Int,
    val resourceRemainingPct: Double,
    val parScoreNow: Int,
    val revisedFinalTarget: Int
)

data class TournamentStandingRow(
    val teamName: String,
    val groupName: String,
    val played: Int,
    val won: Int,
    val lost: Int,
    val tied: Int,
    val points: Int,
    val runsFor: Int,
    val ballsFor: Int,
    val runsAgainst: Int,
    val ballsAgainst: Int,
    val netRunRate: Double
) {
    val nrrFormatted: String
        get() {
            val sign = if (netRunRate > 0.0005) "+" else ""
            return "$sign${String.format(Locale.US, "%.3f", netRunRate)}"
        }
}

data class TournamentOverviewStats(
    val standings: List<TournamentStandingRow>,
    val topRunScorers: List<PlayerCareerStat>,
    val topWicketTakers: List<PlayerCareerStat>,
    val mvpLeaderboard: List<MvpCandidate>,
    val highestTeamScore: String,
    val totalRuns: Int,
    val totalWickets: Int,
    val totalSixes: Int,
    val totalFours: Int
)

object CricketAnalytics {

    private val bowlerCreditedWickets = setOf(
        "BOWLED", "CAUGHT", "LBW", "STUMPED", "HIT_WICKET", "CAUGHT_AND_BOWLED"
    )

    fun analyzeInnings(
        innings: InningsEntity,
        events: List<BallEventEntity>,
        battingSquad: List<PlayerEntity>,
        bowlingSquad: List<PlayerEntity>
    ): InningsScorecardAnalysis {
        val playerMap = (battingSquad + bowlingSquad).associateBy { it.id }

        // Track Batting Order
        val battingOrderIds = mutableListOf<Long>()
        val batterNames = mutableMapOf<Long, String>()

        fun registerBatter(id: Long?, fallbackName: String = "") {
            if (id != null && id > 0 && !battingOrderIds.contains(id)) {
                battingOrderIds.add(id)
                val name = playerMap[id]?.formattedNameWithRoles ?: fallbackName.ifBlank { "Player #$id" }
                batterNames[id] = name
            }
        }

        registerBatter(innings.openingStrikerId)
        registerBatter(innings.openingNonStrikerId)
        for (ev in events) {
            registerBatter(ev.strikerId, ev.strikerName)
            registerBatter(ev.nonStrikerId, ev.nonStrikerName)
            if (ev.nextBatterId != null && ev.nextBatterId > 0) {
                registerBatter(ev.nextBatterId, ev.nextBatterName)
            }
        }
        registerBatter(innings.strikerId)
        registerBatter(innings.nonStrikerId)

        // Accumulate Batter Stats
        class MutableBatter(val id: Long, val name: String) {
            var runs = 0
            var balls = 0
            var fours = 0
            var sixes = 0
            var dots = 0
            var isOut = false
            var isRetiredHurt = false
            var dismissalText = "not out"
        }

        val batterMap = linkedMapOf<Long, MutableBatter>()
        for (id in battingOrderIds) {
            batterMap[id] = MutableBatter(id, batterNames[id] ?: "Player")
        }

        // Accumulate Bowler Stats
        class MutableBowler(val id: Long, val name: String) {
            var legalBalls = 0
            var maidens = 0
            var runsConceded = 0
            var wickets = 0
            var dots = 0
            var wides = 0
            var noBalls = 0
        }

        val bowlerMap = linkedMapOf<Long, MutableBowler>()
        if (innings.openingBowlerId != null && innings.openingBowlerId > 0) {
            val bName = playerMap[innings.openingBowlerId]?.formattedNameWithRoles ?: "Bowler"
            bowlerMap[innings.openingBowlerId] = MutableBowler(innings.openingBowlerId, bName)
        }
        if (innings.currentBowlerId != null && innings.currentBowlerId > 0 && !bowlerMap.containsKey(innings.currentBowlerId)) {
            val bName = playerMap[innings.currentBowlerId]?.formattedNameWithRoles ?: "Bowler"
            bowlerMap[innings.currentBowlerId] = MutableBowler(innings.currentBowlerId, bName)
        }

        // Partnerships & Fall of Wickets
        val partnerships = mutableListOf<PartnershipStat>()
        val fallOfWickets = mutableListOf<FallOfWicketStat>()

        var currentP1Id = innings.openingStrikerId ?: events.firstOrNull()?.strikerId ?: 0L
        var currentP1Name = playerMap[currentP1Id]?.name ?: events.firstOrNull()?.strikerName ?: "Striker"
        var currentP2Id = innings.openingNonStrikerId ?: events.firstOrNull()?.nonStrikerId ?: 0L
        var currentP2Name = playerMap[currentP2Id]?.name ?: events.firstOrNull()?.nonStrikerName ?: "Non-Striker"
        var p1Runs = 0
        var p1Balls = 0
        var p2Runs = 0
        var p2Balls = 0
        var pExtras = 0
        var pTotalRuns = 0
        var pTotalBalls = 0
        var wicketCounter = 0

        var runningTotalRuns = 0
        var runningLegalBalls = 0

        for (ev in events) {
            if (ev.extraType == "PENALTY") {
                runningTotalRuns += ev.extraRuns
                pExtras += ev.extraRuns
                pTotalRuns += ev.extraRuns
                continue
            }

            val strikerStat = batterMap.getOrPut(ev.strikerId) {
                MutableBatter(ev.strikerId, ev.strikerName.ifBlank { "Batter" })
            }
            if (ev.nonStrikerId > 0) {
                batterMap.getOrPut(ev.nonStrikerId) {
                    MutableBatter(ev.nonStrikerId, ev.nonStrikerName.ifBlank { "Batter" })
                }
            }

            val bowlerStat = bowlerMap.getOrPut(ev.bowlerId) {
                MutableBowler(ev.bowlerId, ev.bowlerName.ifBlank { "Bowler" })
            }

            // Batter balls faced: counts on legal deliveries + No Balls (NOT Wides)
            val countsAsBallFaced = ev.extraType != "WIDE"
            if (countsAsBallFaced) {
                strikerStat.balls += 1
                if (ev.runsOffBat == 0) strikerStat.dots += 1
            }
            strikerStat.runs += ev.runsOffBat
            if (ev.isBoundaryFour || ev.runsOffBat == 4) strikerStat.fours += 1
            if (ev.isBoundarySix || ev.runsOffBat == 6) strikerStat.sixes += 1

            // If batter returns after Retired Hurt, reset retired flag
            if (strikerStat.isRetiredHurt) {
                strikerStat.isRetiredHurt = false
                strikerStat.dismissalText = "not out"
            }

            // Bowler stats
            if (ev.isLegalDelivery) {
                bowlerStat.legalBalls += 1
                runningLegalBalls += 1
            }
            when (ev.extraType) {
                "WIDE" -> {
                    bowlerStat.wides += ev.extraRuns
                    bowlerStat.runsConceded += ev.totalBallRuns
                }
                "NO_BALL" -> {
                    bowlerStat.noBalls += 1
                    bowlerStat.runsConceded += ev.runsOffBat + ev.extraRuns
                }
                "BYE", "LEG_BYE" -> {
                    // Byes and Leg Byes are not charged against the bowler
                    if (ev.runsOffBat == 0) bowlerStat.dots += 1
                }
                else -> {
                    bowlerStat.runsConceded += ev.runsOffBat
                    if (ev.runsOffBat == 0 && ev.isLegalDelivery) {
                        bowlerStat.dots += 1
                    }
                }
            }

            runningTotalRuns += ev.totalBallRuns

            // Partnership contribution
            if (currentP1Id == 0L) {
                currentP1Id = ev.strikerId
                currentP1Name = ev.strikerName
            }
            if (currentP2Id == 0L) {
                currentP2Id = ev.nonStrikerId
                currentP2Name = ev.nonStrikerName
            }

            if (ev.strikerId == currentP1Id) {
                p1Runs += ev.runsOffBat
                if (countsAsBallFaced) p1Balls += 1
            } else if (ev.strikerId == currentP2Id) {
                p2Runs += ev.runsOffBat
                if (countsAsBallFaced) p2Balls += 1
            } else {
                // In case a retired hurt batter was swapped
                currentP1Id = ev.strikerId
                currentP1Name = ev.strikerName
                p1Runs += ev.runsOffBat
                if (countsAsBallFaced) p1Balls += 1
            }
            pExtras += ev.extraRuns
            pTotalRuns += ev.totalBallRuns
            if (countsAsBallFaced) pTotalBalls += 1

            // Wicket processing
            if (ev.isWicket) {
                val outId = ev.dismissedPlayerId ?: ev.strikerId
                val outBatter = batterMap.getOrPut(outId) {
                    MutableBatter(outId, ev.dismissedPlayerName.ifBlank { ev.strikerName })
                }

                if (ev.wicketType == "RETIRED_HURT") {
                    outBatter.isRetiredHurt = true
                    outBatter.isOut = false
                    outBatter.dismissalText = "retired hurt"
                } else {
                    outBatter.isOut = true
                    outBatter.isRetiredHurt = false
                    wicketCounter += 1
                    if (ev.wicketType in bowlerCreditedWickets) {
                        bowlerStat.wickets += 1
                    }
                    outBatter.dismissalText = formatDismissalText(
                        wicketType = ev.wicketType,
                        bowlerName = ev.bowlerName,
                        fielderName = ev.fielderName
                    )
                    fallOfWickets.add(
                        FallOfWicketStat(
                            wicketNumber = wicketCounter,
                            scoreAtFall = runningTotalRuns,
                            oversAtFall = "${runningLegalBalls / 6}.${runningLegalBalls % 6}",
                            playerName = outBatter.name,
                            bowlerName = ev.bowlerName
                        )
                    )
                }

                // Close partnership
                if (currentP1Id > 0 || currentP2Id > 0) {
                    partnerships.add(
                        PartnershipStat(
                            wicketNumber = if (ev.wicketType == "RETIRED_HURT") wicketCounter else wicketCounter,
                            batter1Id = currentP1Id,
                            batter1Name = currentP1Name,
                            batter1Runs = p1Runs,
                            batter1Balls = p1Balls,
                            batter2Id = currentP2Id,
                            batter2Name = currentP2Name,
                            batter2Runs = p2Runs,
                            batter2Balls = p2Balls,
                            extras = pExtras,
                            totalRuns = pTotalRuns,
                            totalBalls = pTotalBalls,
                            isUnbroken = false
                        )
                    )
                }

                // Start new partnership with remaining batter + next batter
                val survivorId = if (outId == currentP1Id) currentP2Id else currentP1Id
                val survivorName = if (outId == currentP1Id) currentP2Name else currentP1Name
                currentP1Id = survivorId
                currentP1Name = survivorName
                currentP2Id = ev.nextBatterId ?: 0L
                currentP2Name = ev.nextBatterName.ifBlank {
                    playerMap[currentP2Id]?.name ?: "Next Batter"
                }
                p1Runs = 0
                p1Balls = 0
                p2Runs = 0
                p2Balls = 0
                pExtras = 0
                pTotalRuns = 0
                pTotalBalls = 0
            }
        }

        // Add current unbroken partnership if any balls bowled or innings active
        if ((pTotalBalls > 0 || pTotalRuns > 0 || events.isEmpty()) && (currentP1Id > 0 || currentP2Id > 0)) {
            if (innings.strikerId != null && innings.nonStrikerId != null) {
                if (currentP2Id == 0L) {
                    val otherId = if (innings.strikerId == currentP1Id) innings.nonStrikerId else innings.strikerId
                    currentP2Id = otherId
                    currentP2Name = playerMap[otherId]?.name ?: "Non-Striker"
                }
            }
            partnerships.add(
                PartnershipStat(
                    wicketNumber = wicketCounter + 1,
                    batter1Id = currentP1Id,
                    batter1Name = currentP1Name,
                    batter1Runs = p1Runs,
                    batter1Balls = p1Balls,
                    batter2Id = currentP2Id,
                    batter2Name = currentP2Name,
                    batter2Runs = p2Runs,
                    batter2Balls = p2Balls,
                    extras = pExtras,
                    totalRuns = pTotalRuns,
                    totalBalls = pTotalBalls,
                    isUnbroken = !innings.isCompleted
                )
            )
        }

        // Over-by-over summaries & Maiden calculation
        val eventsByOver = events.filter { it.extraType != "PENALTY" }.groupBy { it.overNumber }
        val overSummaries = mutableListOf<OverSummary>()
        var cumRuns = 0
        var cumWickets = 0

        for ((overIdx, overBalls) in eventsByOver.toSortedMap()) {
            val legalCount = overBalls.count { it.isLegalDelivery }
            val overRuns = overBalls.sumOf { it.totalBallRuns }
            val overWickets = overBalls.count { it.isWicket && it.wicketType != "RETIRED_HURT" }
            cumRuns += overRuns
            cumWickets += overWickets

            val bowlerRunsInOver = overBalls.sumOf { b ->
                when (b.extraType) {
                    "BYE", "LEG_BYE", "PENALTY" -> 0
                    else -> b.totalBallRuns
                }
            }
            val isMaiden = legalCount == 6 && bowlerRunsInOver == 0
            if (isMaiden) {
                val bId = overBalls.last().bowlerId
                bowlerMap[bId]?.let { it.maidens += 1 }
            }

            overSummaries.add(
                OverSummary(
                    overNumber = overIdx + 1,
                    bowlerName = overBalls.lastOrNull()?.bowlerName ?: "Bowler",
                    balls = overBalls,
                    runsInOver = overRuns,
                    wicketsInOver = overWickets,
                    cumulativeRuns = cumRuns,
                    cumulativeWickets = cumWickets,
                    isMaiden = isMaiden
                )
            )
        }

        val currentOverIndex = innings.legalBalls / 6
        val currentOverBalls = events.filter { it.overNumber == currentOverIndex && it.extraType != "PENALTY" }

        val batterStats = batterMap.values.map { b ->
            BatterInningsStat(
                playerId = b.id,
                playerName = b.name,
                runs = b.runs,
                balls = b.balls,
                fours = b.fours,
                sixes = b.sixes,
                dots = b.dots,
                isOut = b.isOut,
                isRetiredHurt = b.isRetiredHurt,
                dismissalText = b.dismissalText,
                isStriker = !innings.isCompleted && innings.strikerId == b.id,
                isNonStriker = !innings.isCompleted && innings.nonStrikerId == b.id
            )
        }

        val battedSet = batterMap.keys
        val didNotBat = battingSquad.filter { it.id !in battedSet }

        val bowlerStats = bowlerMap.values.map { bw ->
            BowlerInningsStat(
                playerId = bw.id,
                playerName = bw.name,
                legalBalls = bw.legalBalls,
                maidens = bw.maidens,
                runsConceded = bw.runsConceded,
                wickets = bw.wickets,
                dots = bw.dots,
                wides = bw.wides,
                noBalls = bw.noBalls,
                isCurrentBowler = !innings.isCompleted && innings.currentBowlerId == bw.id
            )
        }

        return InningsScorecardAnalysis(
            innings = innings,
            batters = batterStats,
            didNotBat = didNotBat,
            bowlers = bowlerStats,
            partnerships = partnerships,
            fallOfWickets = fallOfWickets,
            overSummaries = overSummaries,
            currentOverBalls = currentOverBalls
        )
    }

    fun formatDismissalText(
        wicketType: String,
        bowlerName: String,
        fielderName: String
    ): String {
        val f = fielderName.trim().ifBlank { "sub" }
        return when (wicketType) {
            "BOWLED" -> "b $bowlerName"
            "CAUGHT" -> "c $f b $bowlerName"
            "CAUGHT_AND_BOWLED" -> "c & b $bowlerName"
            "LBW" -> "lbw b $bowlerName"
            "STUMPED" -> "st †$f b $bowlerName"
            "RUN_OUT" -> "run out ($f)"
            "HIT_WICKET" -> "hit wicket b $bowlerName"
            "OBSTRUCTING_FIELD" -> "obstructing the field"
            "TIMED_OUT" -> "timed out"
            "RETIRED_HURT" -> "retired hurt"
            else -> "out"
        }
    }

    fun calculateCareerStats(
        players: List<PlayerEntity>,
        allInnings: List<InningsEntity>,
        allEvents: List<BallEventEntity>
    ): List<PlayerCareerStat> {
        val eventsByInnings = allEvents.groupBy { it.inningsId }
        val inningsById = allInnings.associateBy { it.id }

        return players.map { player ->
            var matchesSet = mutableSetOf<Long>()
            var inningsBatted = 0
            var notOuts = 0
            var totalRuns = 0
            var ballsFaced = 0
            var highestScore = 0
            var fours = 0
            var sixes = 0
            var fifties = 0
            var hundreds = 0

            var ballsBowled = 0
            var runsConceded = 0
            var wicketsTaken = 0
            var maidens = 0
            var bestWickets = 0
            var bestRuns = 999
            var catchesAndRunOuts = 0

            for ((inningsId, evList) in eventsByInnings) {
                val inn = inningsById[inningsId]
                val matchId = inn?.matchId ?: evList.firstOrNull()?.matchId ?: 0L

                val battedHere = evList.any { it.strikerId == player.id || it.nonStrikerId == player.id } ||
                    inn?.openingStrikerId == player.id || inn?.openingNonStrikerId == player.id
                if (battedHere) {
                    matchesSet.add(matchId)
                    inningsBatted += 1
                    var innRuns = 0
                    var innOut = false
                    for (ev in evList) {
                        if (ev.strikerId == player.id && ev.extraType != "PENALTY") {
                            innRuns += ev.runsOffBat
                            if (ev.extraType != "WIDE") ballsFaced += 1
                            if (ev.isBoundaryFour || ev.runsOffBat == 4) fours += 1
                            if (ev.isBoundarySix || ev.runsOffBat == 6) sixes += 1
                        }
                        if (ev.isWicket && ev.dismissedPlayerId == player.id && ev.wicketType != "RETIRED_HURT") {
                            innOut = true
                        }
                    }
                    totalRuns += innRuns
                    if (!innOut) notOuts += 1
                    if (innRuns > highestScore) highestScore = innRuns
                    if (innRuns >= 100) hundreds += 1
                    else if (innRuns >= 50) fifties += 1
                }

                val bowledEvents = evList.filter { it.bowlerId == player.id && it.extraType != "PENALTY" }
                if (bowledEvents.isNotEmpty()) {
                    matchesSet.add(matchId)
                    var innBalls = 0
                    var innConceded = 0
                    var innWickets = 0
                    for (ev in bowledEvents) {
                        if (ev.isLegalDelivery) innBalls += 1
                        when (ev.extraType) {
                            "WIDE" -> innConceded += ev.totalBallRuns
                            "NO_BALL" -> innConceded += ev.runsOffBat + ev.extraRuns
                            "BYE", "LEG_BYE" -> Unit
                            else -> innConceded += ev.runsOffBat
                        }
                        if (ev.isWicket && ev.wicketType in bowlerCreditedWickets) {
                            innWickets += 1
                        }
                    }
                    // Maidens per over
                    val byOver = bowledEvents.groupBy { it.overNumber }
                    for ((_, ob) in byOver) {
                        val legals = ob.count { it.isLegalDelivery }
                        val bRuns = ob.sumOf {
                            if (it.extraType in setOf("BYE", "LEG_BYE", "PENALTY")) 0 else it.totalBallRuns
                        }
                        if (legals == 6 && bRuns == 0) maidens += 1
                    }

                    ballsBowled += innBalls
                    runsConceded += innConceded
                    wicketsTaken += innWickets

                    if (innWickets > bestWickets || (innWickets == bestWickets && innWickets > 0 && innConceded < bestRuns)) {
                        bestWickets = innWickets
                        bestRuns = innConceded
                    } else if (bestWickets == 0 && bestRuns == 999 && innBalls > 0) {
                        bestRuns = innConceded
                    }
                }

                val fieldingCount = evList.count {
                    it.isWicket && it.fielderName.equals(player.name, ignoreCase = true)
                }
                if (fieldingCount > 0) {
                    matchesSet.add(matchId)
                    catchesAndRunOuts += fieldingCount
                }
            }

            PlayerCareerStat(
                player = player,
                matchesPlayed = matchesSet.size,
                inningsBatted = inningsBatted,
                notOuts = notOuts,
                totalRuns = totalRuns,
                ballsFaced = ballsFaced,
                highestScore = highestScore,
                fours = fours,
                sixes = sixes,
                fifties = fifties,
                hundreds = hundreds,
                ballsBowled = ballsBowled,
                runsConceded = runsConceded,
                wicketsTaken = wicketsTaken,
                maidens = maidens,
                bestBowlingWickets = bestWickets,
                bestBowlingRuns = if (bestRuns == 999) 0 else bestRuns,
                catchesAndRunOuts = catchesAndRunOuts
            )
        }
    }

    fun calculateMatchMvp(
        allPlayers: List<PlayerEntity>,
        events: List<BallEventEntity>
    ): List<MvpCandidate> {
        if (events.isEmpty()) return emptyList()
        val playerTeams = allPlayers.associate { it.name to it.teamName }

        class ImpactAcc(val name: String) {
            var runs = 0
            var balls = 0
            var fours = 0
            var sixes = 0
            var wickets = 0
            var ballsBowled = 0
            var runsConceded = 0
            var fieldingDismissals = 0
        }

        val accMap = mutableMapOf<String, ImpactAcc>()
        for (ev in events) {
            if (ev.extraType == "PENALTY") continue
            val sAcc = accMap.getOrPut(ev.strikerName) { ImpactAcc(ev.strikerName) }
            sAcc.runs += ev.runsOffBat
            if (ev.extraType != "WIDE") sAcc.balls += 1
            if (ev.isBoundaryFour || ev.runsOffBat == 4) sAcc.fours += 1
            if (ev.isBoundarySix || ev.runsOffBat == 6) sAcc.sixes += 1

            val bAcc = accMap.getOrPut(ev.bowlerName) { ImpactAcc(ev.bowlerName) }
            if (ev.isLegalDelivery) bAcc.ballsBowled += 1
            if (ev.extraType !in setOf("BYE", "LEG_BYE")) {
                bAcc.runsConceded += ev.totalBallRuns
            }
            if (ev.isWicket && ev.wicketType in bowlerCreditedWickets) {
                bAcc.wickets += 1
            }
            if (ev.isWicket && ev.fielderName.isNotBlank()) {
                val fAcc = accMap.getOrPut(ev.fielderName) { ImpactAcc(ev.fielderName) }
                fAcc.fieldingDismissals += 1
            }
        }

        return accMap.values.map { p ->
            var pts = p.runs * 1.0 + p.fours * 1.0 + p.sixes * 2.0
            if (p.runs >= 50) pts += 12.0
            if (p.runs >= 100) pts += 20.0
            if (p.balls >= 10) {
                val sr = (p.runs * 100.0) / p.balls
                if (sr >= 150.0) pts += 8.0
                else if (sr >= 130.0) pts += 4.0
            }
            pts += p.wickets * 22.0
            if (p.wickets >= 3) pts += 12.0
            if (p.wickets >= 5) pts += 20.0
            if (p.ballsBowled >= 12) {
                val econ = (p.runsConceded * 6.0) / p.ballsBowled
                if (econ <= 6.0) pts += 10.0
                else if (econ <= 7.5) pts += 5.0
            }
            pts += p.fieldingDismissals * 8.0

            val parts = mutableListOf<String>()
            if (p.runs > 0 || p.balls > 0) parts.add("${p.runs} (${p.balls})")
            if (p.wickets > 0 || p.ballsBowled > 0) parts.add("${p.wickets}/${p.runsConceded}")
            if (p.fieldingDismissals > 0) parts.add("${p.fieldingDismissals} Fld")

            MvpCandidate(
                playerName = p.name,
                teamName = playerTeams[p.name] ?: "",
                impactPoints = (pts * 10.0).roundToInt() / 10.0,
                breakdown = parts.joinToString(" • ")
            )
        }.sortedByDescending { it.impactPoints }
    }

    /**
     * Standard Duckworth-Lewis-Stern (DLS) Exponential Resource Curve Calculator
     * Computes remaining resource % based on overs left and wickets lost,
     * providing both the Revised Final Target and current Par Score for rain interruptions.
     */
    fun calculateDlsTarget(
        team1Score: Int,
        maxOvers: Int,
        revisedTotalOvers: Double,
        currentOversBowled: Double,
        wicketsLost: Int
    ): DlsCalculationResult {
        val safeMaxOvers = max(1, maxOvers).toDouble()
        val safeRevisedOvers = revisedTotalOvers.coerceIn(1.0, safeMaxOvers)
        val safeCurrentOvers = currentOversBowled.coerceIn(0.0, safeRevisedOvers)
        val safeWickets = wicketsLost.coerceIn(0, 9)

        fun resourceRemaining(oversLeft: Double, wickets: Int): Double {
            if (oversLeft <= 0.0) return 0.0
            // Exponential decay model calibrated to standard limited-overs resource curves
            val z0 = 100.0 * (1.0 - 0.082 * wickets).coerceAtLeast(0.12)
            val b = 0.038 + (0.014 * wickets)
            val raw = z0 * (1.0 - kotlin.math.exp(-b * oversLeft * (50.0 / safeMaxOvers)))
            val fullResource = 100.0 * (1.0 - kotlin.math.exp(-0.038 * 50.0))
            return ((raw / fullResource) * 100.0).coerceIn(0.0, 100.0)
        }

        val team2StartResource = resourceRemaining(safeRevisedOvers, 0)
        val revisedFinalTarget = ((team1Score * (team2StartResource / 100.0)).roundToInt() + 1).coerceAtLeast(1)

        val oversLeftNow = (safeRevisedOvers - safeCurrentOvers).coerceAtLeast(0.0)
        val resourceLeftNow = resourceRemaining(oversLeftNow, safeWickets)
        val resourceUsedNow = (team2StartResource - resourceLeftNow).coerceAtLeast(0.0)
        val parScoreNow = ((team1Score * (resourceUsedNow / 100.0)).roundToInt()).coerceAtLeast(0)

        return DlsCalculationResult(
            team1Score = team1Score,
            maxOvers = maxOvers,
            revisedOvers = safeRevisedOvers,
            currentOversBowled = safeCurrentOvers,
            wicketsLost = safeWickets,
            resourceRemainingPct = (resourceLeftNow * 10.0).roundToInt() / 10.0,
            parScoreNow = parScoreNow,
            revisedFinalTarget = revisedFinalTarget
        )
    }

    fun generateShareableScorecard(
        match: MatchEntity,
        analyses: List<InningsScorecardAnalysis>,
        mvp: MvpCandidate?
    ): String {
        val sb = StringBuilder()
        sb.appendLine("==========================================")
        sb.appendLine("🏏 ${match.teamA} vs ${match.teamB} (${match.format})")
        sb.appendLine("📍 Venue: ${match.venue}")
        if (match.resultText.isNotBlank()) {
            sb.appendLine("🏆 Result: ${match.resultText}")
        }
        if (mvp != null) {
            sb.appendLine("⭐ Player of the Match: ${mvp.playerName} (${mvp.breakdown})")
        }
        sb.appendLine("==========================================")

        for (analysis in analyses) {
            val inn = analysis.innings
            sb.appendLine()
            sb.appendLine("--- Innings ${inn.inningsNumber}: ${inn.battingTeam} ---")
            sb.appendLine("Score: ${inn.totalRuns}/${inn.wickets} (${inn.oversFormatted} Ov) | CRR: ${String.format(Locale.US, "%.2f", inn.currentRunRate)}")
            sb.appendLine("Batting:")
            for (b in analysis.batters) {
                val star = if (b.isStriker) "*" else ""
                sb.appendLine(" • ${b.playerName}$star (${b.dismissalText}): ${b.runs}(${b.balls}) [4s:${b.fours}, 6s:${b.sixes}, SR:${b.strikeRateFormatted}]")
            }
            sb.appendLine("Extras: ${inn.totalExtras} (wd ${inn.extrasWide}, nb ${inn.extrasNoBall}, b ${inn.extrasBye}, lb ${inn.extrasLegBye}, p ${inn.extrasPenalty})")
            sb.appendLine("Bowling:")
            for (bw in analysis.bowlers) {
                sb.appendLine(" • ${bw.playerName}: ${bw.oversFormatted}-${bw.maidens}-${bw.runsConceded}-${bw.wickets} (Econ ${bw.economyFormatted})")
            }
        }

        sb.appendLine()
        sb.appendLine("==========================================")
        return sb.toString()
    }

    /**
     * Computes full Tournament Points Table, Official ICC Net Run Rate (NRR),
     * Top Run Scorers, Top Wicket Takers, and Tournament MVP Leaderboard directly from
     * real matches linked to this tournament.
     */
    fun calculateTournamentOverview(
        tournament: TournamentEntity,
        registeredTeams: List<TournamentTeamEntity>,
        tournamentMatches: List<MatchEntity>,
        allInnings: List<InningsEntity>,
        allEvents: List<BallEventEntity>,
        allPlayers: List<PlayerEntity>
    ): TournamentOverviewStats {
        val matchIds = tournamentMatches.map { it.id }.toSet()
        val tourneyInnings = allInnings.filter { it.matchId in matchIds }
        val tourneyEvents = allEvents.filter { it.matchId in matchIds }
        val inningsByMatch = tourneyInnings.groupBy { it.matchId }

        class MutableStanding(val teamName: String, val groupName: String) {
            var played = 0
            var won = 0
            var lost = 0
            var tied = 0
            var points = 0
            var runsFor = 0
            var ballsFor = 0
            var runsAgainst = 0
            var ballsAgainst = 0
        }

        val standingMap = linkedMapOf<String, MutableStanding>()
        for (rt in registeredTeams) {
            standingMap[rt.teamName.lowercase()] = MutableStanding(rt.teamName, rt.groupName)
        }
        // Also ensure any team in a tournament match is included
        for (m in tournamentMatches) {
            standingMap.getOrPut(m.teamA.lowercase()) { MutableStanding(m.teamA, "Group A") }
            standingMap.getOrPut(m.teamB.lowercase()) { MutableStanding(m.teamB, "Group A") }
        }

        var highestScoreRuns = -1
        var highestScoreText = "-"

        // Only completed matches count toward W/L/Pts; League (or all completed) count toward standings
        for (m in tournamentMatches) {
            val mInnings = inningsByMatch[m.id].orEmpty()
            for (inn in mInnings) {
                if (inn.totalRuns > highestScoreRuns && (inn.legalBalls > 0 || inn.totalRuns > 0)) {
                    highestScoreRuns = inn.totalRuns
                    highestScoreText = "${inn.battingTeam} ${inn.totalRuns}/${inn.wickets} (${inn.oversFormatted} Ov)"
                }
            }

            if (m.status != "COMPLETED") continue
            // Standings apply primarily to League stage (or all matches if no stage distinction)
            val isKnockout = m.stage in setOf("QUARTER_FINAL", "SEMI_FINAL", "ELIMINATOR", "QUALIFIER_1", "QUALIFIER_2", "FINAL")
            if (isKnockout && tournamentMatches.any { it.stage == "LEAGUE" }) continue

            val stA = standingMap[m.teamA.lowercase()] ?: continue
            val stB = standingMap[m.teamB.lowercase()] ?: continue

            stA.played += 1
            stB.played += 1

            val resLower = m.resultText.lowercase()
            when {
                resLower.startsWith(m.teamA.lowercase() + " won") -> {
                    stA.won += 1
                    stA.points += tournament.pointsForWin
                    stB.lost += 1
                }
                resLower.startsWith(m.teamB.lowercase() + " won") -> {
                    stB.won += 1
                    stB.points += tournament.pointsForWin
                    stA.lost += 1
                }
                else -> {
                    stA.tied += 1
                    stB.tied += 1
                    stA.points += tournament.pointsForTie
                    stB.points += tournament.pointsForTie
                }
            }

            val maxWickets = (m.playersPerTeam - 1).coerceAtLeast(1)
            val maxQuotaBalls = (m.revisedOvers ?: m.oversLimit) * 6

            for (inn in mInnings) {
                val batSt = standingMap[inn.battingTeam.lowercase()]
                val bowlSt = standingMap[inn.bowlingTeam.lowercase()]
                // Per ICC NRR rule: if a team is bowled out (all out), they are charged the full quota of overs
                val effectiveBalls = if (inn.wickets >= maxWickets && m.format != "TEST") {
                    maxQuotaBalls
                } else {
                    inn.legalBalls
                }
                if (batSt != null) {
                    batSt.runsFor += inn.totalRuns
                    batSt.ballsFor += effectiveBalls
                }
                if (bowlSt != null) {
                    bowlSt.runsAgainst += inn.totalRuns
                    bowlSt.ballsAgainst += effectiveBalls
                }
            }
        }

        val standings = standingMap.values.map { s ->
            val rateFor = if (s.ballsFor > 0) (s.runsFor.toDouble() * 6.0) / s.ballsFor.toDouble() else 0.0
            val rateAgainst = if (s.ballsAgainst > 0) (s.runsAgainst.toDouble() * 6.0) / s.ballsAgainst.toDouble() else 0.0
            val nrr = rateFor - rateAgainst
            TournamentStandingRow(
                teamName = s.teamName,
                groupName = s.groupName,
                played = s.played,
                won = s.won,
                lost = s.lost,
                tied = s.tied,
                points = s.points,
                runsFor = s.runsFor,
                ballsFor = s.ballsFor,
                runsAgainst = s.runsAgainst,
                ballsAgainst = s.ballsAgainst,
                netRunRate = nrr
            )
        }.sortedWith(
            compareByDescending<TournamentStandingRow> { it.points }
                .thenByDescending { it.netRunRate }
                .thenByDescending { it.won }
                .thenBy { it.teamName }
        )

        val tourneyCareerStats = calculateCareerStats(
            players = allPlayers,
            allInnings = tourneyInnings,
            allEvents = tourneyEvents
        )

        val topRuns = tourneyCareerStats
            .filter { it.totalRuns > 0 || it.ballsFaced > 0 }
            .sortedWith(
                compareByDescending<PlayerCareerStat> { it.totalRuns }
                    .thenByDescending { it.battingStrikeRate }
            )

        val topWickets = tourneyCareerStats
            .filter { it.wicketsTaken > 0 || it.ballsBowled > 0 }
            .sortedWith(
                compareByDescending<PlayerCareerStat> { it.wicketsTaken }
                    .thenBy { it.runsConceded }
            )

        val mvpList = calculateMatchMvp(allPlayers, tourneyEvents)

        val totalRuns = tourneyInnings.sumOf { it.totalRuns }
        val totalWickets = tourneyInnings.sumOf { it.wickets }
        val totalSixes = tourneyEvents.count { it.isBoundarySix || it.runsOffBat == 6 }
        val totalFours = tourneyEvents.count { it.isBoundaryFour || it.runsOffBat == 4 }

        return TournamentOverviewStats(
            standings = standings,
            topRunScorers = topRuns,
            topWicketTakers = topWickets,
            mvpLeaderboard = mvpList,
            highestTeamScore = highestScoreText,
            totalRuns = totalRuns,
            totalWickets = totalWickets,
            totalSixes = totalSixes,
            totalFours = totalFours
        )
    }
}
