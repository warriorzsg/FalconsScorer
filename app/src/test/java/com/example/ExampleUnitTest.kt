package com.example

import com.example.data.BallEventEntity
import com.example.data.CricketAnalytics
import com.example.data.InningsEntity
import com.example.data.MatchEntity
import com.example.data.PlayerEntity
import com.example.data.TournamentEntity
import com.example.data.TournamentTeamEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `analyzeInnings calculates batting bowling partnerships and extras accurately`() {
        val p1 = PlayerEntity(
            id = 1,
            name = "Samir Parajuli",
            teamName = "Falcons XI",
            isCaptain = true,
            isWicketKeeper = true,
            squadStatus = "PLAYING_XI"
        )
        val p2 = PlayerEntity(
            id = 2,
            name = "Aarav Sharma",
            teamName = "Falcons XI",
            isViceCaptain = true,
            squadStatus = "IMPACT_PLAYER"
        )
        val b1 = PlayerEntity(id = 3, name = "Haris Rauf", teamName = "Royal Strikers")

        assertEquals("Samir Parajuli (c/wk)", p1.formattedNameWithRoles)
        assertEquals("Aarav Sharma (vc) [IMP]", p2.formattedNameWithRoles)

        val innings = InningsEntity(
            id = 10,
            matchId = 1,
            inningsNumber = 1,
            battingTeam = "Falcons XI",
            bowlingTeam = "Royal Strikers",
            totalRuns = 11,
            wickets = 0,
            legalBalls = 2,
            extrasWide = 1,
            strikerId = 1,
            nonStrikerId = 2,
            currentBowlerId = 3,
            openingStrikerId = 1,
            openingNonStrikerId = 2,
            openingBowlerId = 3
        )

        val events = listOf(
            BallEventEntity(
                id = 100,
                matchId = 1,
                inningsId = 10,
                inningsNumber = 1,
                sequenceNumber = 1,
                overNumber = 0,
                ballInOver = 1,
                strikerId = 1,
                strikerName = "Samir Parajuli",
                nonStrikerId = 2,
                nonStrikerName = "Aarav Sharma",
                bowlerId = 3,
                bowlerName = "Haris Rauf",
                runsOffBat = 4,
                extraType = "NONE",
                extraRuns = 0,
                totalBallRuns = 4,
                isLegalDelivery = true,
                isBoundaryFour = true
            ),
            BallEventEntity(
                id = 101,
                matchId = 1,
                inningsId = 10,
                inningsNumber = 1,
                sequenceNumber = 2,
                overNumber = 0,
                ballInOver = 1,
                strikerId = 1,
                strikerName = "Samir Parajuli",
                nonStrikerId = 2,
                nonStrikerName = "Aarav Sharma",
                bowlerId = 3,
                bowlerName = "Haris Rauf",
                runsOffBat = 0,
                extraType = "WIDE",
                extraRuns = 1,
                totalBallRuns = 1,
                isLegalDelivery = false
            ),
            BallEventEntity(
                id = 102,
                matchId = 1,
                inningsId = 10,
                inningsNumber = 1,
                sequenceNumber = 3,
                overNumber = 0,
                ballInOver = 2,
                strikerId = 1,
                strikerName = "Samir Parajuli",
                nonStrikerId = 2,
                nonStrikerName = "Aarav Sharma",
                bowlerId = 3,
                bowlerName = "Haris Rauf",
                runsOffBat = 6,
                extraType = "NONE",
                extraRuns = 0,
                totalBallRuns = 6,
                isLegalDelivery = true,
                isBoundarySix = true
            )
        )

        val analysis = CricketAnalytics.analyzeInnings(
            innings = innings,
            events = events,
            battingSquad = listOf(p1, p2),
            bowlingSquad = listOf(b1)
        )

        val samirStat = analysis.batters.first { it.playerId == 1L }
        assertEquals(10, samirStat.runs)
        assertEquals(2, samirStat.balls)
        assertEquals(1, samirStat.fours)
        assertEquals(1, samirStat.sixes)

        val bowlerStat = analysis.bowlers.first { it.playerId == 3L }
        assertEquals(2, bowlerStat.legalBalls)
        assertEquals(11, bowlerStat.runsConceded)
        assertEquals(1, bowlerStat.wides)

        val dls = CricketAnalytics.calculateDlsTarget(
            team1Score = 180,
            maxOvers = 20,
            revisedTotalOvers = 15.0,
            currentOversBowled = 5.0,
            wicketsLost = 1
        )
        assertTrue(dls.revisedFinalTarget in 120..180)
    }

    @Test
    fun `calculateTournamentOverview computes points table and NRR accurately`() {
        val tourney = TournamentEntity(
            id = 5,
            name = "Champions Cup",
            format = "T20",
            oversLimit = 20,
            pointsForWin = 2,
            pointsForTie = 1
        )
        val regTeams = listOf(
            TournamentTeamEntity(id = 1, tournamentId = 5, teamName = "Team Alpha"),
            TournamentTeamEntity(id = 2, tournamentId = 5, teamName = "Team Bravo")
        )
        val match = MatchEntity(
            id = 50,
            teamA = "Team Alpha",
            teamB = "Team Bravo",
            format = "T20",
            oversLimit = 20,
            maxOversPerBowler = 4,
            tossWinner = "Team Alpha",
            tossDecision = "BAT",
            status = "COMPLETED",
            resultText = "Team Alpha won by 20 runs",
            tournamentId = 5
        )
        val inn1 = InningsEntity(
            id = 501,
            matchId = 50,
            inningsNumber = 1,
            battingTeam = "Team Alpha",
            bowlingTeam = "Team Bravo",
            totalRuns = 160,
            wickets = 5,
            legalBalls = 120,
            isCompleted = true
        )
        val inn2 = InningsEntity(
            id = 502,
            matchId = 50,
            inningsNumber = 2,
            battingTeam = "Team Bravo",
            bowlingTeam = "Team Alpha",
            totalRuns = 140,
            wickets = 8,
            legalBalls = 120,
            isCompleted = true
        )

        val overview = CricketAnalytics.calculateTournamentOverview(
            tournament = tourney,
            registeredTeams = regTeams,
            tournamentMatches = listOf(match),
            allInnings = listOf(inn1, inn2),
            allEvents = emptyList(),
            allPlayers = emptyList()
        )

        assertEquals(2, overview.standings.size)
        val leader = overview.standings.first()
        assertEquals("Team Alpha", leader.teamName)
        assertEquals(2, leader.points)
        assertEquals(1, leader.won)
        assertTrue(leader.netRunRate > 0.99)
    }
}
