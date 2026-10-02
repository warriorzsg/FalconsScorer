package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ActiveMatchDetailState
import com.example.data.BallEventEntity
import com.example.data.BatterInningsStat
import com.example.data.PlayerEntity
import com.example.ui.theme.BoundaryFourBlue
import com.example.ui.theme.BoundarySixPurple
import com.example.ui.theme.CricketCrimson
import com.example.ui.theme.DotBallSlate
import com.example.ui.theme.ExtraAmber
import com.example.ui.theme.FalconGold
import com.example.ui.theme.StadiumGreenDark
import com.example.ui.theme.StadiumGreenPrimary
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveScoringScreen(
    state: ActiveMatchDetailState,
    onBack: () -> Unit,
    onOpenScorecard: () -> Unit,
    onStartNewMatch: () -> Unit,
    onRecordDelivery: (
        runsOffBat: Int,
        extraType: String,
        extraRuns: Int,
        isWicket: Boolean,
        wicketType: String,
        dismissedPlayerId: Long?,
        fielderName: String,
        nextBatterId: Long?
    ) -> Unit,
    onUndoLastBall: () -> Unit,
    onSwapStrike: () -> Unit,
    onChangeBowler: (Long) -> Unit,
    onSetActivePlayers: (Long?, Long?, Long?) -> Unit,
    onAwardPenaltyRuns: (Int) -> Unit,
    onDeclareOrEndInnings: (Boolean) -> Unit,
    onStartNextInnings: (Boolean) -> Unit,
    onConcludeMatchAsDraw: () -> Unit,
    onApplyDlsTarget: (Int, Int) -> Unit,
    onQuickSavePlayer: (PlayerEntity, (Long) -> Unit) -> Unit
) {
    BackHandler { onBack() }

    val match = state.match
    val innings = state.activeInnings
    val analysis = state.activeAnalysis

    if (match == null || innings == null || analysis == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.SportsCricket,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Active Match Selected",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onStartNewMatch) {
                    Text("Start New Match", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    var showWicketDialog by remember { mutableStateOf(false) }
    var activeExtraDialogType by remember { mutableStateOf<String?>(null) }
    var showBowlerDialog by remember { mutableStateOf(false) }
    var showSmartToolsSheet by remember { mutableStateOf(false) }
    var showDlsDialog by remember { mutableStateOf(false) }
    var showSelectBatterModal by remember { mutableStateOf(false) }

    // Automatically prompt the scorer to select a new bowler whenever an over is completed
    androidx.compose.runtime.LaunchedEffect(state.needsNextBowler, innings.legalBalls, innings.isCompleted) {
        if (state.needsNextBowler && !innings.isCompleted) {
            showBowlerDialog = true
        }
    }

    fun guardScorerReady(action: () -> Unit) {
        if (state.needsNextBowler && !innings.isCompleted) {
            showBowlerDialog = true
        } else if (state.needsNextBatter && !innings.isCompleted) {
            showSelectBatterModal = true
        } else {
            action()
        }
    }

    val strikerEntity = remember(state.battingSquad, innings.strikerId) {
        state.battingSquad.find { it.id == innings.strikerId }
    }
    val nonStrikerEntity = remember(state.battingSquad, innings.nonStrikerId) {
        state.battingSquad.find { it.id == innings.nonStrikerId }
    }
    val strikerStat = remember(analysis.batters, innings.strikerId) {
        analysis.batters.find { it.playerId == innings.strikerId }
    }
    val nonStrikerStat = remember(analysis.batters, innings.nonStrikerId) {
        analysis.batters.find { it.playerId == innings.nonStrikerId }
    }
    val activeBowlerStat = remember(analysis.bowlers, innings.currentBowlerId) {
        analysis.bowlers.find { it.playerId == innings.currentBowlerId }
    }
    val currentPartnership = remember(analysis.partnerships) {
        analysis.partnerships.lastOrNull()
    }
    val availableNextBatters = remember(state.battingSquad, analysis.batters, innings.strikerId, innings.nonStrikerId) {
        val outIds = analysis.batters.filter { it.isOut }.map { it.playerId }.toSet()
        state.battingSquad.filter { p ->
            p.id !in outIds && p.id != innings.strikerId && p.id != innings.nonStrikerId
        }
    }

    val effectiveOversLimit = if (innings.inningsNumber == 2 && match.revisedOvers != null) {
        match.revisedOvers
    } else {
        match.oversLimit
    }
    val totalMaxBalls = effectiveOversLimit * 6
    val ballsRemaining = max(0, totalMaxBalls - innings.legalBalls)
    val targetRuns = innings.targetRuns
    val runsNeeded = if (targetRuns != null) max(0, targetRuns - innings.totalRuns) else null
    val requiredRunRate = if (runsNeeded != null && ballsRemaining > 0) {
        (runsNeeded.toDouble() * 6.0) / ballsRemaining.toDouble()
    } else null

    val projectedScoreAtCrr = remember(innings.totalRuns, innings.legalBalls, effectiveOversLimit) {
        if (innings.legalBalls > 0) {
            (innings.currentRunRate * effectiveOversLimit).roundToInt()
        } else 0
    }

    // Calculate Test Match Lead / Trail when no target is set
    val testLeadTrailText = remember(state.allInnings, innings) {
        if (match.format == "TEST" && innings.inningsNumber > 1 && targetRuns == null) {
            val battingTeamTotal = state.allInnings.filter { it.battingTeam == innings.battingTeam }.sumOf { it.totalRuns }
            val bowlingTeamTotal = state.allInnings.filter { it.battingTeam == innings.bowlingTeam }.sumOf { it.totalRuns }
            val diff = battingTeamTotal - bowlingTeamTotal
            when {
                diff > 0 -> "${innings.battingTeam} leads by $diff runs"
                diff < 0 -> "${innings.battingTeam} trails by ${-diff} runs"
                else -> "Scores level"
            }
        } else null
    }

    val lastBallCommentary = remember(analysis.overSummaries, analysis.currentOverBalls) {
        analysis.currentOverBalls.lastOrNull()?.commentary
            ?: analysis.overSummaries.lastOrNull()?.balls?.lastOrNull()?.commentary
            ?: "Ready to bowl. Tap a scoring button below to record ball-by-ball action."
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("live_scoring_screen"),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. High-Contrast Broadcast Scoreboard Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_scoreboard_header"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StadiumGreenDark),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Top Row: Match Format, Innings badge, Free Hit Indicator, Scorecard link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = StadiumGreenPrimary,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${match.format} • INN ${innings.inningsNumber}",
                                    color = FalconGold,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            if (innings.isFreeHit && !innings.isCompleted) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = CricketCrimson,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("free_hit_badge")
                                ) {
                                    Text(
                                        text = "⚡ FREE HIT",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        TextButton(
                            onClick = onOpenScorecard,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("header_full_scorecard_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = FalconGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Full Scorecard",
                                color = FalconGold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Main Score & Overs Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = innings.battingTeam.uppercase(),
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${innings.totalRuns}/${innings.wickets}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.displayLarge,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.testTag("main_score_text")
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "(${innings.oversFormatted} / $effectiveOversLimit Ov)",
                                    color = FalconGold,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .padding(bottom = 5.dp)
                                        .testTag("main_overs_text")
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "CRR: ${String.format(Locale.US, "%.2f", innings.currentRunRate)}",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (requiredRunRate != null && !innings.isCompleted) {
                                Text(
                                    text = "RRR: ${String.format(Locale.US, "%.2f", requiredRunRate)}",
                                    color = FalconGold,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            } else if (match.format != "TEST" && projectedScoreAtCrr > 0) {
                                Text(
                                    text = "Proj: $projectedScoreAtCrr",
                                    color = Color.White.copy(alpha = 0.75f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Equation / Target / Result / Extras Strip
                    val statusLine = when {
                        match.resultText.isNotBlank() -> "🏆 ${match.resultText}"
                        runsNeeded != null && !innings.isCompleted ->
                            "Target $targetRuns • Need $runsNeeded runs in $ballsRemaining balls"
                        testLeadTrailText != null -> testLeadTrailText
                        else -> "vs ${innings.bowlingTeam} • Extras: ${innings.totalExtras} (wd ${innings.extrasWide}, nb ${innings.extrasNoBall}, b ${innings.extrasBye}, lb ${innings.extrasLegBye})"
                    }

                    Surface(
                        color = StadiumGreenPrimary.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statusLine,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 2. Prompt Banner if Bowler Change or Next Batter Needed
        if (state.needsNextBowler && !innings.isCompleted) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "End of Over ${innings.legalBalls / 6}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Select the bowler for Over ${(innings.legalBalls / 6) + 1}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Button(
                            onClick = { showBowlerDialog = true },
                            modifier = Modifier.testTag("select_next_over_bowler_btn")
                        ) {
                            Text("Select Bowler", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (state.needsNextBatter && !innings.isCompleted) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Incoming Batter to Continue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { showSelectBatterModal = true }
                        ) {
                            Text("Choose Batter")
                        }
                    }
                }
            }
        }

        // 3. Innings Break / Match Complete Action Card
        if (innings.isCompleted || match.status == "COMPLETED") {
            item {
                InningsCompletedActionCard(
                    match = match,
                    innings = innings,
                    allInnings = state.allInnings,
                    onStartNextInnings = onStartNextInnings,
                    onOpenScorecard = onOpenScorecard,
                    onUndoLastBall = onUndoLastBall,
                    onStartNewMatch = onStartNewMatch
                )
            }
        }

        // 4. Active Batters & Partnership Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BATTER",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.weight(1.8f)
                        )
                        listOf("R", "B", "4s", "6s", "SR").forEach { col ->
                            Text(
                                text = col,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(if (col == "SR") 0.8f else 0.5f)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    ActiveBatterRow(
                        name = strikerEntity?.name ?: strikerStat?.playerName ?: "Select Striker",
                        stat = strikerStat,
                        isOnStrike = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    ActiveBatterRow(
                        name = nonStrikerEntity?.name ?: nonStrikerStat?.playerName ?: "Select Non-Striker",
                        stat = nonStrikerStat,
                        isOnStrike = false
                    )

                    if (currentPartnership != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Partnership: ${currentPartnership.totalRuns} (${currentPartnership.totalBalls})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${currentPartnership.batter1Name.substringBefore(" ")} ${currentPartnership.batter1Runs}(${currentPartnership.batter1Balls}) • ${currentPartnership.batter2Name.substringBefore(" ")} ${currentPartnership.batter2Runs}(${currentPartnership.batter2Balls})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 5. Active Bowler & This Over Timeline Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BOWLER",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "⚾ ${activeBowlerStat?.playerName ?: "Select Bowler"}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "O-M-R-W",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${activeBowlerStat?.oversFormatted ?: "0.0"}-${activeBowlerStat?.maidens ?: 0}-${activeBowlerStat?.runsConceded ?: 0}-${activeBowlerStat?.wickets ?: 0}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "ECON",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = activeBowlerStat?.economyFormatted ?: "0.00",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = { showBowlerDialog = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("change_bowler_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Change Bowler", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Bowler", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // This Over Ball Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "This Over:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (analysis.currentOverBalls.isEmpty()) {
                            Text(
                                text = "New over starting...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            analysis.currentOverBalls.forEach { ball ->
                                DeliveryBadgeChip(ball = ball)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = lastBallCommentary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 6. Fast Tactile Ball-by-Ball Scoring Keypad (Active when innings not completed)
        if (!innings.isCompleted) {
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scoring_keypad_card"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "TAP TO SCORE BALL-BY-BALL",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Row 1: 0, 1, 2, 3 Runs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0, 1, 2, 3).forEach { run ->
                                ScoreRunButton(
                                    label = run.toString(),
                                    subLabel = if (run == 0) "DOT" else if (run == 1) "SINGLE" else "RUNS",
                                    containerColor = if (run == 0) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = if (run == 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f),
                                    testTag = "score_btn_$run",
                                    onClick = {
                                        guardScorerReady {
                                            onRecordDelivery(run, "NONE", 0, false, "NONE", null, "", null)
                                        }
                                    }
                                )
                            }
                        }

                        // Row 2: 4, 6, 5, WICKET
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ScoreRunButton(
                                label = "4",
                                subLabel = "FOUR",
                                containerColor = BoundaryFourBlue,
                                contentColor = Color.White,
                                modifier = Modifier.weight(1f),
                                testTag = "score_btn_4",
                                onClick = {
                                    guardScorerReady {
                                        onRecordDelivery(4, "NONE", 0, false, "NONE", null, "", null)
                                    }
                                }
                            )
                            ScoreRunButton(
                                label = "6",
                                subLabel = "SIX",
                                containerColor = BoundarySixPurple,
                                contentColor = Color.White,
                                modifier = Modifier.weight(1f),
                                testTag = "score_btn_6",
                                onClick = {
                                    guardScorerReady {
                                        onRecordDelivery(6, "NONE", 0, false, "NONE", null, "", null)
                                    }
                                }
                            )
                            ScoreRunButton(
                                label = "5",
                                subLabel = "RUNS",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(0.8f),
                                testTag = "score_btn_5",
                                onClick = {
                                    guardScorerReady {
                                        onRecordDelivery(5, "NONE", 0, false, "NONE", null, "", null)
                                    }
                                }
                            )
                            ScoreRunButton(
                                label = "OUT",
                                subLabel = "WICKET",
                                containerColor = CricketCrimson,
                                contentColor = Color.White,
                                modifier = Modifier.weight(1.2f),
                                testTag = "score_btn_wicket",
                                onClick = {
                                    guardScorerReady {
                                        showWicketDialog = true
                                    }
                                }
                            )
                        }

                        // Row 3: Extras (Wide, No Ball, Bye, Leg Bye)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExtraActionButton(
                                label = "WD",
                                subtitle = "Wide",
                                modifier = Modifier.weight(1f),
                                testTag = "extra_btn_wide",
                                onClick = {
                                    guardScorerReady {
                                        activeExtraDialogType = "WIDE"
                                    }
                                }
                            )
                            ExtraActionButton(
                                label = "NB",
                                subtitle = "No Ball",
                                modifier = Modifier.weight(1f),
                                testTag = "extra_btn_noball",
                                onClick = {
                                    guardScorerReady {
                                        activeExtraDialogType = "NO_BALL"
                                    }
                                }
                            )
                            ExtraActionButton(
                                label = "BYE",
                                subtitle = "Byes",
                                modifier = Modifier.weight(1f),
                                testTag = "extra_btn_bye",
                                onClick = {
                                    guardScorerReady {
                                        activeExtraDialogType = "BYE"
                                    }
                                }
                            )
                            ExtraActionButton(
                                label = "LB",
                                subtitle = "Leg Bye",
                                modifier = Modifier.weight(1f),
                                testTag = "extra_btn_legbye",
                                onClick = {
                                    guardScorerReady {
                                        activeExtraDialogType = "LEG_BYE"
                                    }
                                }
                            )
                        }

                        // Row 4: Controls (Undo, Swap Strike, Smart Tools)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onUndoLastBall,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("undo_ball_button"),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Undo", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onSwapStrike,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("swap_strike_button"),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Swap *", fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { showSmartToolsSheet = true },
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(48.dp)
                                    .testTag("smart_tools_button"),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tools", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs & Modals
    if (showWicketDialog) {
        WicketDismissalDialog(
            striker = strikerEntity,
            nonStriker = nonStrikerEntity,
            availableNextBatters = availableNextBatters,
            bowlingSquad = state.bowlingSquad,
            isFreeHit = innings.isFreeHit,
            onDismiss = { showWicketDialog = false },
            onQuickAddBatter = { newName ->
                onQuickSavePlayer(
                    PlayerEntity(
                        name = newName,
                        teamName = innings.battingTeam,
                        role = "BATSMAN"
                    )
                ) {}
            },
            onConfirmWicket = { wicketType, dismissedId, fielder, runsCompleted, extraType, nextBatterId ->
                val exRuns = if (extraType in setOf("WIDE", "NO_BALL")) 1 else 0
                onRecordDelivery(
                    runsCompleted,
                    extraType,
                    exRuns,
                    true,
                    wicketType,
                    dismissedId,
                    fielder,
                    nextBatterId
                )
                showWicketDialog = false
            }
        )
    }

    if (activeExtraDialogType != null) {
        ExtrasSelectionDialog(
            initialExtraType = activeExtraDialogType!!,
            onDismiss = { activeExtraDialogType = null },
            onConfirmExtra = { runsOffBat, extraType, extraRuns ->
                onRecordDelivery(runsOffBat, extraType, extraRuns, false, "NONE", null, "", null)
                activeExtraDialogType = null
            }
        )
    }

    if (showBowlerDialog) {
        val completedOverNum = if (innings.legalBalls > 0 && innings.legalBalls % 6 == 0 && analysis.currentOverBalls.isEmpty()) {
            innings.legalBalls / 6
        } else null

        SelectBowlerDialog(
            bowlingTeam = innings.bowlingTeam,
            bowlingSquad = state.bowlingSquad,
            bowlerStats = analysis.bowlers,
            lastOverBowlerId = innings.lastOverBowlerId,
            maxOversPerBowler = match.maxOversPerBowler,
            allowConsecutiveOvers = match.allowConsecutiveOvers,
            completedOverNumber = completedOverNum,
            isTestFormat = match.format == "TEST",
            onDismiss = { showBowlerDialog = false },
            onSelectBowler = { newBowlerId ->
                onChangeBowler(newBowlerId)
                showBowlerDialog = false
            },
            onQuickAddBowler = { newName ->
                onQuickSavePlayer(
                    PlayerEntity(
                        name = newName,
                        teamName = innings.bowlingTeam,
                        role = "BOWLER"
                    )
                ) { createdId ->
                    onChangeBowler(createdId)
                    showBowlerDialog = false
                }
            }
        )
    }

    if (showSelectBatterModal) {
        AlertDialog(
            onDismissRequest = { showSelectBatterModal = false },
            title = { Text("Select Active Batters", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tap a player from ${innings.battingTeam} to come to the crease:")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        availableNextBatters.forEach { p ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    if (innings.strikerId == null) {
                                        onSetActivePlayers(p.id, innings.nonStrikerId, innings.currentBowlerId)
                                    } else {
                                        onSetActivePlayers(innings.strikerId, p.id, innings.currentBowlerId)
                                    }
                                    showSelectBatterModal = false
                                },
                                label = { Text(p.name) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSelectBatterModal = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showSmartToolsSheet) {
        AlertDialog(
            onDismissRequest = { showSmartToolsSheet = false },
            title = {
                Text("Scorer Smart Tools & Innings Controls", fontWeight = FontWeight.Black)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = {
                            showSmartToolsSheet = false
                            showDlsDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DLS / Rain Target Par Calculator", fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = {
                            onAwardPenaltyRuns(5)
                            showSmartToolsSheet = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+5 Penalty Runs to ${innings.battingTeam}", fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = {
                            showSmartToolsSheet = false
                            showBowlerDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change Current Bowler", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onDeclareOrEndInnings(true)
                            showSmartToolsSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("declare_end_innings_btn")
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (match.format == "TEST") "Declare / End Current Innings" else "End Current Innings Early",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (match.format == "TEST") {
                        OutlinedButton(
                            onClick = {
                                onConcludeMatchAsDraw()
                                showSmartToolsSheet = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("End Test Match as Draw", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSmartToolsSheet = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showDlsDialog) {
        val firstInnRuns = state.allInnings.firstOrNull()?.totalRuns ?: innings.totalRuns
        DlsCalculatorDialog(
            match = match,
            firstInningsRuns = firstInnRuns,
            currentInnings = innings,
            onDismiss = { showDlsDialog = false },
            onApplyTarget = onApplyDlsTarget
        )
    }
}

@Composable
private fun ActiveBatterRow(
    name: String,
    stat: BatterInningsStat?,
    isOnStrike: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isOnStrike) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1.8f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isOnStrike) "🏏 $name *" else name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isOnStrike) FontWeight.ExtraBold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "${stat?.runs ?: 0}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.5f)
        )
        Text(
            text = "${stat?.balls ?: 0}",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.5f)
        )
        Text(
            text = "${stat?.fours ?: 0}",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.5f)
        )
        Text(
            text = "${stat?.sixes ?: 0}",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.5f)
        )
        Text(
            text = stat?.strikeRateFormatted ?: "0.0",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.8f)
        )
    }
}

@Composable
fun DeliveryBadgeChip(ball: BallEventEntity) {
    val bgColor = when {
        ball.isWicket -> CricketCrimson
        ball.isBoundarySix || ball.runsOffBat == 6 -> BoundarySixPurple
        ball.isBoundaryFour || ball.runsOffBat == 4 -> BoundaryFourBlue
        ball.extraType != "NONE" -> ExtraAmber
        ball.totalBallRuns == 0 -> DotBallSlate
        else -> StadiumGreenPrimary
    }

    Surface(
        color = bgColor,
        shape = CircleShape,
        modifier = Modifier.size(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = ball.chipLabel,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ScoreRunButton(
    label: String,
    subLabel: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ExtraActionButton(
    label: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun InningsCompletedActionCard(
    match: com.example.data.MatchEntity,
    innings: com.example.data.InningsEntity,
    allInnings: List<com.example.data.InningsEntity>,
    onStartNextInnings: (Boolean) -> Unit,
    onOpenScorecard: () -> Unit,
    onUndoLastBall: () -> Unit,
    onStartNewMatch: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("innings_completed_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (match.status == "COMPLETED") "🏆 MATCH COMPLETED" else "INNINGS ${innings.inningsNumber} COMPLETED",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            if (match.resultText.isNotBlank()) {
                Text(
                    text = match.resultText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "${innings.battingTeam} scored ${innings.totalRuns}/${innings.wickets} in ${innings.oversFormatted} overs. Target for ${innings.bowlingTeam}: ${innings.totalRuns + 1} runs.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (match.manOfTheMatch.isNotBlank()) {
                Text(
                    text = "⭐ Player of the Match: ${match.manOfTheMatch}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (match.status != "COMPLETED" && allInnings.size < match.maxInnings) {
                    Button(
                        onClick = { onStartNextInnings(false) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("start_next_innings_btn")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Innings ${allInnings.size + 1}", fontWeight = FontWeight.Bold)
                    }

                    if (match.format == "TEST" && allInnings.size == 2) {
                        OutlinedButton(
                            onClick = { onStartNextInnings(true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Enforce Follow-On", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = onOpenScorecard,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Full Scorecard", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onStartNewMatch,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("New Match", fontWeight = FontWeight.Bold)
                    }
                }
            }

            TextButton(
                onClick = onUndoLastBall,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Undo Last Ball & Reopen Innings")
            }
        }
    }
}
