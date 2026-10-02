package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "players",
    indices = [Index(value = ["teamName"]), Index(value = ["name"])]
)
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val teamName: String,
    val role: String = "ALL_ROUNDER", // BATSMAN, BOWLER, ALL_ROUNDER, WICKET_KEEPER
    val battingStyle: String = "Right-Hand Bat", // Right-Hand Bat, Left-Hand Bat
    val bowlingStyle: String = "Right-Arm Medium",
    val jerseyNumber: Int = 7,
    val isCaptain: Boolean = false,
    val isViceCaptain: Boolean = false,
    val isWicketKeeper: Boolean = false,
    val squadStatus: String = "PLAYING_XI", // PLAYING_XI, SUBSTITUTE, IMPACT_PLAYER
    val photoUri: String = "",
    val age: Int? = null,
    val nickname: String = "",
    val hometownOrClub: String = "",
    val bioNotes: String = ""
) {
    val formattedNameWithRoles: String
        get() {
            val tags = mutableListOf<String>()
            if (isCaptain) tags.add("c")
            if (isViceCaptain) tags.add("vc")
            if (isWicketKeeper || role == "WICKET_KEEPER") tags.add("wk")
            val roleSuffix = if (tags.isNotEmpty()) " (${tags.joinToString("/")})" else ""
            val statusSuffix = when (squadStatus) {
                "IMPACT_PLAYER" -> " [IMP]"
                "SUBSTITUTE" -> " [SUB]"
                else -> ""
            }
            return "$name$roleSuffix$statusSuffix"
        }
}

@Entity(
    tableName = "tournaments",
    indices = [Index(value = ["status"]), Index(value = ["createdAt"])]
)
data class TournamentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val organizer: String = "",
    val venue: String = "",
    val format: String = "T20", // T20, ODI, TEST, CUSTOM
    val oversLimit: Int = 20,
    val maxOversPerBowler: Int = 4,
    val playersPerTeam: Int = 11,
    val pointsForWin: Int = 2,
    val pointsForTie: Int = 1,
    val status: String = "ONGOING", // UPCOMING, ONGOING, COMPLETED
    val championTeam: String = "",
    val runnerUpTeam: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tournament_teams",
    indices = [Index(value = ["tournamentId"]), Index(value = ["teamName"])]
)
data class TournamentTeamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tournamentId: Long,
    val teamName: String,
    val groupName: String = "Group A"
)

@Entity(
    tableName = "tournament_fixtures",
    indices = [Index(value = ["tournamentId"]), Index(value = ["linkedMatchId"])]
)
data class TournamentFixtureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tournamentId: Long,
    val matchNumber: Int,
    val stage: String = "LEAGUE", // LEAGUE, QUARTER_FINAL, SEMI_FINAL, ELIMINATOR, QUALIFIER_1, QUALIFIER_2, FINAL
    val teamA: String,
    val teamB: String,
    val scheduledDate: String = "Today",
    val venue: String = "",
    val linkedMatchId: Long? = null,
    val status: String = "SCHEDULED", // SCHEDULED, LIVE, COMPLETED
    val winnerTeam: String = "",
    val resultSummary: String = ""
)

@Entity(
    tableName = "matches",
    indices = [
        Index(value = ["status"]),
        Index(value = ["createdAt"]),
        Index(value = ["tournamentId"]),
        Index(value = ["fixtureId"])
    ]
)
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val teamA: String,
    val teamB: String,
    val format: String, // T20, ODI, TEST, CUSTOM
    val oversLimit: Int,
    val maxOversPerBowler: Int,
    val playersPerTeam: Int = 11,
    val maxInnings: Int = 2,
    val tossWinner: String,
    val tossDecision: String, // BAT, BOWL
    val venue: String = "Cricket Ground",
    val status: String = "LIVE", // LIVE, COMPLETED
    val currentInningsNumber: Int = 1,
    val resultText: String = "",
    val manOfTheMatch: String = "",
    val revisedTarget: Int? = null,
    val revisedOvers: Int? = null,
    val allowConsecutiveOvers: Boolean = false,
    val tournamentId: Long? = null,
    val fixtureId: Long? = null,
    val stage: String = "FRIENDLY", // FRIENDLY, LEAGUE, QUARTER_FINAL, SEMI_FINAL, FINAL
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "innings",
    indices = [Index(value = ["matchId", "inningsNumber"])]
)
data class InningsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchId: Long,
    val inningsNumber: Int, // 1, 2, 3, 4
    val battingTeam: String,
    val bowlingTeam: String,
    val totalRuns: Int = 0,
    val wickets: Int = 0,
    val legalBalls: Int = 0,
    val extrasWide: Int = 0,
    val extrasNoBall: Int = 0,
    val extrasBye: Int = 0,
    val extrasLegBye: Int = 0,
    val extrasPenalty: Int = 0,
    val targetRuns: Int? = null,
    val isCompleted: Boolean = false,
    val isDeclared: Boolean = false,
    val isFollowOn: Boolean = false,
    val strikerId: Long? = null,
    val nonStrikerId: Long? = null,
    val currentBowlerId: Long? = null,
    val lastOverBowlerId: Long? = null,
    val isFreeHit: Boolean = false,
    val openingStrikerId: Long? = null,
    val openingNonStrikerId: Long? = null,
    val openingBowlerId: Long? = null
) {
    val totalExtras: Int
        get() = extrasWide + extrasNoBall + extrasBye + extrasLegBye + extrasPenalty

    val oversFormatted: String
        get() = "${legalBalls / 6}.${legalBalls % 6}"

    val currentRunRate: Double
        get() = if (legalBalls > 0) (totalRuns.toDouble() * 6.0) / legalBalls.toDouble() else 0.0
}

@Entity(
    tableName = "ball_events",
    indices = [Index(value = ["matchId", "inningsId", "sequenceNumber"])]
)
data class BallEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchId: Long,
    val inningsId: Long,
    val inningsNumber: Int,
    val sequenceNumber: Int,
    val overNumber: Int,
    val ballInOver: Int,
    val strikerId: Long,
    val strikerName: String,
    val nonStrikerId: Long,
    val nonStrikerName: String,
    val bowlerId: Long,
    val bowlerName: String,
    val runsOffBat: Int = 0,
    val extraType: String = "NONE", // NONE, WIDE, NO_BALL, BYE, LEG_BYE, PENALTY
    val extraRuns: Int = 0,
    val totalBallRuns: Int = 0,
    val isLegalDelivery: Boolean = true,
    val isWicket: Boolean = false,
    val wicketType: String = "NONE",
    val dismissedPlayerId: Long? = null,
    val dismissedPlayerName: String = "",
    val fielderName: String = "",
    val nextBatterId: Long? = null,
    val nextBatterName: String = "",
    val nextOverBowlerId: Long? = null,
    val isBoundaryFour: Boolean = false,
    val isBoundarySix: Boolean = false,
    val commentary: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val chipLabel: String
        get() = when {
            wicketType == "RETIRED_HURT" -> "RH"
            isWicket && totalBallRuns > 0 -> "W+$totalBallRuns"
            isWicket -> "W"
            extraType == "WIDE" -> if (extraRuns > 1) "${extraRuns}wd" else "wd"
            extraType == "NO_BALL" -> if (runsOffBat > 0) "${runsOffBat}+nb" else "nb"
            extraType == "BYE" -> "${extraRuns}b"
            extraType == "LEG_BYE" -> "${extraRuns}lb"
            extraType == "PENALTY" -> "+${extraRuns}P"
            else -> runsOffBat.toString()
        }
}
