package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.ActiveMatchDetailState
import com.example.data.CricketAnalytics
import com.example.data.InningsScorecardAnalysis
import com.example.data.MatchEntity
import com.example.data.OverSummary
import com.example.ui.theme.CricketCrimson
import com.example.ui.theme.FalconGold
import com.example.ui.theme.StadiumGreenDark
import com.example.ui.theme.StadiumGreenPrimary
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScorecardScreen(
    state: ActiveMatchDetailState,
    allMatches: List<MatchEntity>,
    onSelectMatch: (Long) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val match = state.match
    val analyses = state.analyses

    var selectedViewTab by remember { mutableIntStateOf(0) } // 0 = Scorecard, 1 = Overs & Charts, 2 = Match Summary & MVP
    var selectedInningsIndex by remember(analyses.size) {
        mutableIntStateOf((analyses.size - 1).coerceAtLeast(0))
    }

    if (match == null || analyses.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No match scorecard available yet.",
                style = MaterialTheme.typography.titleLarge
            )
        }
        return
    }

    val currentInningsAnalysis = analyses.getOrElse(selectedInningsIndex) { analyses.first() }

    fun shareScorecardText() {
        val shareBody = CricketAnalytics.generateShareableScorecard(
            match = match,
            analyses = analyses,
            mvp = state.mvpCandidates.firstOrNull()
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${match.teamA} vs ${match.teamB} - Falcons Scorer")
            putExtra(Intent.EXTRA_TEXT, shareBody)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Scorecard"))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("scorecard_screen")
    ) {
        // Match Switcher + Header Banner
        Surface(
            color = StadiumGreenDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (allMatches.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allMatches.forEach { m ->
                            FilterChip(
                                selected = m.id == match.id,
                                onClick = {
                                    selectedInningsIndex = 0
                                    onSelectMatch(m.id)
                                },
                                label = {
                                    Text(
                                        text = "${m.teamA} v ${m.teamB} (${m.format})",
                                        color = if (m.id == match.id) MaterialTheme.colorScheme.onSecondaryContainer else Color.White
                                    )
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${match.teamA} vs ${match.teamB}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = if (match.resultText.isNotBlank()) "🏆 ${match.resultText}"
                            else "${match.format} • ${match.venue}",
                            color = FalconGold,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { shareScorecardText() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("share_scorecard_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Scorecard", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Sub-navigation Tabs
        TabRow(selectedTabIndex = selectedViewTab) {
            Tab(
                selected = selectedViewTab == 0,
                onClick = { selectedViewTab = 0 },
                text = { Text("Scorecard", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_full_scorecard")
            )
            Tab(
                selected = selectedViewTab == 1,
                onClick = { selectedViewTab = 1 },
                text = { Text("Overs & Charts", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_overs_charts")
            )
            Tab(
                selected = selectedViewTab == 2,
                onClick = { selectedViewTab = 2 },
                text = { Text("Summary & MVP", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_summary_mvp")
            )
        }

        // Innings Selector Chips (for Scorecard and Overs tabs)
        if (selectedViewTab != 2 && analyses.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                analyses.forEachIndexed { idx, item ->
                    val inn = item.innings
                    FilterChip(
                        selected = selectedInningsIndex == idx,
                        onClick = { selectedInningsIndex = idx },
                        label = {
                            Text(
                                text = "Inn ${inn.inningsNumber}: ${inn.battingTeam} (${inn.totalRuns}/${inn.wickets})",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }
        }

        when (selectedViewTab) {
            0 -> FullInningsScorecardContent(analysis = currentInningsAnalysis)
            1 -> OversAndChartsContent(
                analysis = currentInningsAnalysis,
                allAnalyses = analyses
            )
            2 -> MatchSummaryAndMvpContent(
                match = match,
                analyses = analyses,
                mvpCandidates = state.mvpCandidates,
                onShareScorecard = { shareScorecardText() }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FullInningsScorecardContent(analysis: InningsScorecardAnalysis) {
    val inn = analysis.innings

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Innings Total Banner
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${inn.battingTeam} • Innings ${inn.inningsNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "CRR: ${String.format(Locale.US, "%.2f", inn.currentRunRate)} • Extras: ${inn.totalExtras}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = "${inn.totalRuns}/${inn.wickets} (${inn.oversFormatted})",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Batting Table Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BATTING",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(2.0f)
                        )
                        listOf("R", "B", "4s", "6s", "SR").forEach { h ->
                            Text(
                                text = h,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(if (h == "SR") 0.85f else 0.5f)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    analysis.batters.forEach { b ->
                        Column(modifier = Modifier.padding(vertical = 5.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = b.playerName + if (b.isStriker) " *" else "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(2.0f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = b.runs.toString(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.5f)
                                )
                                Text(
                                    text = b.balls.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.5f)
                                )
                                Text(
                                    text = b.fours.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.5f)
                                )
                                Text(
                                    text = b.sixes.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.5f)
                                )
                                Text(
                                    text = b.strikeRateFormatted,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.85f)
                                )
                            }
                            Text(
                                text = b.dismissalText,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (b.isOut) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Extras", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${inn.totalExtras} (wd ${inn.extrasWide}, nb ${inn.extrasNoBall}, b ${inn.extrasBye}, lb ${inn.extrasLegBye}, p ${inn.extrasPenalty})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (analysis.didNotBat.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Yet to Bat: " + analysis.didNotBat.joinToString(", ") { it.name },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Bowling Table Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BOWLING",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1.9f)
                        )
                        listOf("O", "M", "R", "W", "ECON").forEach { h ->
                            Text(
                                text = h,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(if (h == "ECON") 0.85f else 0.5f)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    analysis.bowlers.forEach { bw ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.9f)) {
                                Text(
                                    text = bw.playerName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Dots: ${bw.dots} • WD: ${bw.wides} • NB: ${bw.noBalls}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = bw.oversFormatted,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(0.5f)
                            )
                            Text(
                                text = bw.maidens.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(0.5f)
                            )
                            Text(
                                text = bw.runsConceded.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(0.5f)
                            )
                            Text(
                                text = bw.wickets.toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(0.5f)
                            )
                            Text(
                                text = bw.economyFormatted,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(0.85f)
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }

        // Partnerships Breakdown Card
        if (analysis.partnerships.isNotEmpty()) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "PARTNERSHIPS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        analysis.partnerships.forEach { p ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${p.batter1Name} ${p.batter1Runs}(${p.batter1Balls})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${p.totalRuns} Runs (${p.totalBalls}b)" + if (p.isUnbroken) "*" else "",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${p.batter2Name} ${p.batter2Runs}(${p.batter2Balls})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val totalBatRuns = (p.batter1Runs + p.batter2Runs).coerceAtLeast(1)
                                val b1Fraction = (p.batter1Runs.toFloat() / totalBatRuns.toFloat()).coerceIn(0.1f, 0.9f)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(b1Fraction)
                                            .fillMaxHeight()
                                            .background(StadiumGreenPrimary)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f - b1Fraction)
                                            .fillMaxHeight()
                                            .background(FalconGold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fall of Wickets Card
        if (analysis.fallOfWickets.isNotEmpty()) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "FALL OF WICKETS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            analysis.fallOfWickets.forEach { fow ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${fow.wicketNumber}-${fow.scoreAtFall} (${fow.playerName}, ${fow.oversAtFall} ov)",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OversAndChartsContent(
    analysis: InningsScorecardAnalysis,
    allAnalyses: List<InningsScorecardAnalysis>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Manhattan Over-by-Over Runs Bar Chart
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "MANHATTAN CHART (RUNS PER OVER)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Red dot above bar indicates wicket(s) in that over",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (analysis.overSummaries.isEmpty()) {
                        Text("No overs bowled yet.")
                    } else {
                        ManhattanBarChart(overSummaries = analysis.overSummaries)
                    }
                }
            }
        }

        // 2. Worm Cumulative Runs Comparison Chart
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "WORM PROGRESSION (CUMULATIVE SCORE)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    WormComparisonChart(analyses = allAnalyses)
                }
            }
        }

        // 3. Over-by-Over Ball Log
        item {
            Text(
                text = "Over-by-Over Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }

        items(analysis.overSummaries.reversed(), key = { it.overNumber }) { over ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Over ${over.overNumber} • ${over.bowlerName}" + if (over.isMaiden) " (MAIDEN)" else "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${over.runsInOver} Runs • Score: ${over.cumulativeRuns}/${over.cumulativeWickets}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        over.balls.forEach { b ->
                            DeliveryBadgeChip(ball = b)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManhattanBarChart(overSummaries: List<OverSummary>) {
    val maxRuns = remember(overSummaries) {
        max(12, overSummaries.maxOfOrNull { it.runsInOver } ?: 12)
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val count = overSummaries.size.coerceAtLeast(1)
        val slotWidth = size.width / count
        val barWidth = (slotWidth * 0.62f).coerceAtMost(28.dp.toPx())
        val chartHeight = size.height - 20.dp.toPx()

        overSummaries.forEachIndexed { idx, ov ->
            val ratio = ov.runsInOver.toFloat() / maxRuns.toFloat()
            val barH = (ratio * chartHeight).coerceAtLeast(4.dp.toPx())
            val left = idx * slotWidth + (slotWidth - barWidth) / 2f
            val top = chartHeight - barH

            drawRoundRect(
                color = if (ov.runsInOver >= 12) FalconGold else StadiumGreenPrimary,
                topLeft = Offset(left, top),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(6f, 6f)
            )

            if (ov.wicketsInOver > 0) {
                drawCircle(
                    color = CricketCrimson,
                    radius = 5.dp.toPx(),
                    center = Offset(left + barWidth / 2f, (top - 8.dp.toPx()).coerceAtLeast(6.dp.toPx()))
                )
            }
        }
    }
}

@Composable
private fun WormComparisonChart(analyses: List<InningsScorecardAnalysis>) {
    val colors = listOf(StadiumGreenPrimary, FalconGold, CricketCrimson, Color(0xFF0284C7))
    val maxOvers = remember(analyses) {
        max(5, analyses.maxOfOrNull { it.overSummaries.size } ?: 5)
    }
    val maxScore = remember(analyses) {
        max(50, analyses.maxOfOrNull { it.innings.totalRuns } ?: 50)
    }

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            analyses.forEachIndexed { idx, a ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors[idx % colors.size], RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Inn ${a.innings.inningsNumber}: ${a.innings.battingTeam}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            analyses.forEachIndexed { idx, a ->
                val path = Path()
                path.moveTo(0f, size.height)
                a.overSummaries.forEach { ov ->
                    val x = (ov.overNumber.toFloat() / maxOvers.toFloat()) * size.width
                    val y = size.height - (ov.cumulativeRuns.toFloat() / maxScore.toFloat()) * (size.height - 12.dp.toPx())
                    path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = colors[idx % colors.size],
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun MatchSummaryAndMvpContent(
    match: MatchEntity,
    analyses: List<InningsScorecardAnalysis>,
    mvpCandidates: List<com.example.data.MvpCandidate>,
    onShareScorecard: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Match Summary & Branding Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SportsCricket,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OFFICIAL MATCH SUMMARY",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${match.teamA} vs ${match.teamB}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Toss: ${match.tossWinner} won the toss and elected to ${match.tossDecision}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    analyses.forEach { a ->
                        val inn = a.innings
                        Text(
                            text = "• Innings ${inn.inningsNumber} (${inn.battingTeam}): ${inn.totalRuns}/${inn.wickets} in ${inn.oversFormatted} Ov",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (match.resultText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🏆 ${match.resultText}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Player of the Match (MVP) Impact Leaderboard
        if (mvpCandidates.isNotEmpty()) {
            val topMvp = mvpCandidates.first()
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = FalconGold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PLAYER OF THE MATCH (MVP LEADER)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = topMvp.playerName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${topMvp.teamName} • ${topMvp.breakdown} • Impact: ${topMvp.impactPoints} pts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "TOP IMPACT PERFORMERS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        mvpCandidates.take(6).forEachIndexed { i, c ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${i + 1}. ${c.playerName} (${c.teamName})",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = c.breakdown,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${c.impactPoints} pts",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }

        // Export Scorecard Button
        item {
            Button(
                onClick = onShareScorecard,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share / Export Full Text Scorecard", fontWeight = FontWeight.Bold)
            }
        }
    }
}
