package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.BowlerInningsStat
import com.example.data.CricketAnalytics
import com.example.data.InningsEntity
import com.example.data.MatchEntity
import com.example.data.PlayerEntity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WicketDismissalDialog(
    striker: PlayerEntity?,
    nonStriker: PlayerEntity?,
    availableNextBatters: List<PlayerEntity>,
    bowlingSquad: List<PlayerEntity>,
    isFreeHit: Boolean,
    onDismiss: () -> Unit,
    onQuickAddBatter: (String) -> Unit,
    onConfirmWicket: (
        wicketType: String,
        dismissedPlayerId: Long?,
        fielderName: String,
        runsCompleted: Int,
        extraType: String,
        nextBatterId: Long?
    ) -> Unit
) {
    val wicketTypes = remember(isFreeHit) {
        if (isFreeHit) {
            // On a Free Hit, only Run Out, Hit Ball Twice, Obstructing Field, or Retired Hurt are valid
            listOf("RUN_OUT", "OBSTRUCTING_FIELD", "RETIRED_HURT")
        } else {
            listOf(
                "CAUGHT", "BOWLED", "LBW", "RUN_OUT",
                "STUMPED", "CAUGHT_AND_BOWLED", "HIT_WICKET", "RETIRED_HURT"
            )
        }
    }

    var selectedType by remember { mutableStateOf(wicketTypes.first()) }
    var dismissedIsStriker by remember { mutableStateOf(true) }
    var fielderName by remember { mutableStateOf(bowlingSquad.firstOrNull()?.name ?: "") }
    var runsCompleted by remember { mutableIntStateOf(0) }
    var extraOnWicket by remember { mutableStateOf("NONE") } // NONE, WIDE, NO_BALL
    var selectedNextBatterId by remember { mutableStateOf(availableNextBatters.firstOrNull()?.id) }
    var customNewBatterName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SportsCricket,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isFreeHit) "Wicket on Free Hit" else "Record Wicket / Dismissal",
                    fontWeight = FontWeight.Black
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Dismissal Type:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    wicketTypes.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.replace("_", " ")) },
                            modifier = Modifier.testTag("wicket_type_$type")
                        )
                    }
                }

                if (selectedType == "RUN_OUT" || selectedType == "RETIRED_HURT") {
                    Text("Batter Dismissed / Retiring:", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = dismissedIsStriker,
                            onClick = { dismissedIsStriker = true },
                            label = { Text(striker?.name ?: "Striker") }
                        )
                        FilterChip(
                            selected = !dismissedIsStriker,
                            onClick = { dismissedIsStriker = false },
                            label = { Text(nonStriker?.name ?: "Non-Striker") }
                        )
                    }
                }

                if (selectedType in setOf("CAUGHT", "RUN_OUT", "STUMPED")) {
                    OutlinedTextField(
                        value = fielderName,
                        onValueChange = { fielderName = it },
                        label = { Text("Fielder / Wicket-Keeper Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (bowlingSquad.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            bowlingSquad.take(6).forEach { f ->
                                FilterChip(
                                    selected = fielderName == f.name,
                                    onClick = { fielderName = f.name },
                                    label = { Text(f.name) }
                                )
                            }
                        }
                    }
                }

                if (selectedType == "RUN_OUT" || selectedType == "STUMPED") {
                    Text("Completed Runs / Extra on Ball:", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0, 1, 2, 3).forEach { r ->
                            FilterChip(
                                selected = runsCompleted == r,
                                onClick = { runsCompleted = r },
                                label = { Text("+$r R") }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("NONE", "WIDE", "NO_BALL").forEach { ex ->
                            FilterChip(
                                selected = extraOnWicket == ex,
                                onClick = { extraOnWicket = ex },
                                label = { Text(if (ex == "NONE") "Legal Ball" else ex) }
                            )
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Select Incoming Next Batter:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                if (availableNextBatters.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        availableNextBatters.forEach { candidate ->
                            FilterChip(
                                selected = selectedNextBatterId == candidate.id,
                                onClick = { selectedNextBatterId = candidate.id },
                                label = { Text(candidate.name) }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "All squad batters have batted (or Last Wicket).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = customNewBatterName,
                        onValueChange = { customNewBatterName = it },
                        label = { Text("Or quick-add new batter") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (customNewBatterName.isNotBlank()) {
                        Button(
                            onClick = {
                                onQuickAddBatter(customNewBatterName.trim())
                                customNewBatterName = ""
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val outId = if (dismissedIsStriker) striker?.id else nonStriker?.id
                    onConfirmWicket(
                        selectedType,
                        outId,
                        fielderName,
                        runsCompleted,
                        extraOnWicket,
                        selectedNextBatterId
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.testTag("confirm_wicket_button")
            ) {
                Text("Confirm Wicket", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExtrasSelectionDialog(
    initialExtraType: String, // WIDE, NO_BALL, BYE, LEG_BYE
    onDismiss: () -> Unit,
    onConfirmExtra: (runsOffBat: Int, extraType: String, extraRuns: Int) -> Unit
) {
    var extraType by remember { mutableStateOf(initialExtraType) }
    var additionalRuns by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Record Extra Delivery ($extraType)", fontWeight = FontWeight.Black)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Extra Type:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("WIDE", "NO_BALL", "BYE", "LEG_BYE").forEach { t ->
                        FilterChip(
                            selected = extraType == t,
                            onClick = { extraType = t },
                            label = { Text(t.replace("_", " ")) }
                        )
                    }
                }

                val labelText = when (extraType) {
                    "WIDE" -> "Additional runs ran on Wide (on top of +1 Wide):"
                    "NO_BALL" -> "Runs scored off the Bat on No-Ball (on top of +1 NB):"
                    "BYE" -> "Total Bye Runs (1 to 4):"
                    else -> "Total Leg-Bye Runs (1 to 4):"
                }
                Text(labelText, style = MaterialTheme.typography.labelLarge)

                val options = if (extraType in setOf("BYE", "LEG_BYE")) {
                    listOf(1, 2, 3, 4)
                } else {
                    listOf(0, 1, 2, 3, 4, 6)
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { r ->
                        FilterChip(
                            selected = additionalRuns == r,
                            onClick = { additionalRuns = r },
                            label = {
                                Text(
                                    text = when (extraType) {
                                        "WIDE" -> if (r == 0) "Wide (1)" else "WD + $r (${r + 1})"
                                        "NO_BALL" -> if (r == 0) "NB Only (1)" else "NB + $r Bat (${r + 1})"
                                        else -> "$r ${if (extraType == "BYE") "B" else "LB"}"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (extraType) {
                        "WIDE" -> onConfirmExtra(0, "WIDE", 1 + additionalRuns)
                        "NO_BALL" -> onConfirmExtra(additionalRuns, "NO_BALL", 1)
                        "BYE" -> onConfirmExtra(0, "BYE", additionalRuns.coerceAtLeast(1))
                        "LEG_BYE" -> onConfirmExtra(0, "LEG_BYE", additionalRuns.coerceAtLeast(1))
                    }
                },
                modifier = Modifier.testTag("confirm_extra_button")
            ) {
                Text("Record Ball", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectBowlerDialog(
    bowlingTeam: String,
    bowlingSquad: List<PlayerEntity>,
    bowlerStats: List<BowlerInningsStat>,
    lastOverBowlerId: Long?,
    maxOversPerBowler: Int,
    allowConsecutiveOvers: Boolean = false,
    completedOverNumber: Int? = null,
    isTestFormat: Boolean = false,
    onDismiss: () -> Unit,
    onSelectBowler: (Long) -> Unit,
    onQuickAddBowler: (String) -> Unit
) {
    val statsById = remember(bowlerStats) { bowlerStats.associateBy { it.playerId } }
    val eligibleBowlers = remember(bowlingSquad, lastOverBowlerId, statsById, allowConsecutiveOvers, maxOversPerBowler, isTestFormat) {
        bowlingSquad.filter { p ->
            val isConsecutiveBlocked = !allowConsecutiveOvers && p.id == lastOverBowlerId
            val oversDone = (statsById[p.id]?.legalBalls ?: 0) / 6
            val isQuotaFull = !isTestFormat && oversDone >= maxOversPerBowler
            !isConsecutiveBlocked && !isQuotaFull
        }
    }
    var selectedId by remember(eligibleBowlers, bowlingSquad) {
        mutableStateOf(eligibleBowlers.firstOrNull()?.id)
    }
    var newBowlerName by remember { mutableStateOf("") }
    val lastBowlerStat = remember(statsById, lastOverBowlerId) {
        lastOverBowlerId?.let { statsById[it] }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (completedOverNumber != null && completedOverNumber > 0) {
                        "Over $completedOverNumber Completed • Select Next Bowler"
                    } else {
                        "Select Bowler ($bowlingTeam)"
                    },
                    fontWeight = FontWeight.Black
                )
                if (completedOverNumber != null && completedOverNumber > 0) {
                    Text(
                        text = "Prepare for Over ${completedOverNumber + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (lastBowlerStat != null && completedOverNumber != null && completedOverNumber > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Last Over Bowled By: ${lastBowlerStat.playerName}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Updated Figures: ${lastBowlerStat.oversFormatted}-${lastBowlerStat.maidens}-${lastBowlerStat.runsConceded}-${lastBowlerStat.wickets} (Econ ${lastBowlerStat.economyFormatted})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Text(
                    text = if (allowConsecutiveOvers) {
                        "Custom Format: Consecutive overs by the same bowler are allowed. Max $maxOversPerBowler overs/bowler."
                    } else {
                        "Standard Rule: The same bowler cannot bowl consecutive overs. Max $maxOversPerBowler overs/bowler."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    bowlingSquad.forEach { p ->
                        val st = statsById[p.id]
                        val isLastBowler = p.id == lastOverBowlerId
                        val isConsecutiveBlocked = !allowConsecutiveOvers && isLastBowler
                        val oversDone = (st?.legalBalls ?: 0) / 6
                        val isQuotaFull = !isTestFormat && oversDone >= maxOversPerBowler
                        val isEnabled = !isConsecutiveBlocked && !isQuotaFull

                        val figureText = if (st != null) {
                            "${st.oversFormatted}-${st.maidens}-${st.runsConceded}-${st.wickets} (Econ ${st.economyFormatted})"
                        } else {
                            "0.0-0-0-0 (Yet to bowl)"
                        }

                        val statusBadge = when {
                            isConsecutiveBlocked -> " [Bowled Last Over]"
                            isQuotaFull -> " [Quota Full: $oversDone/$maxOversPerBowler Ov]"
                            isLastBowler && allowConsecutiveOvers -> " [Bowled Last • Allowed]"
                            else -> ""
                        }

                        FilterChip(
                            selected = selectedId == p.id,
                            enabled = isEnabled,
                            onClick = { selectedId = p.id },
                            label = {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(
                                        text = p.name + statusBadge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Figures: $figureText",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_bowler_chip_${p.id}")
                        )
                    }
                }

                HorizontalDivider()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = newBowlerName,
                        onValueChange = { newBowlerName = it },
                        label = { Text("Quick-add new bowler to $bowlingTeam") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (newBowlerName.isNotBlank()) {
                        Button(
                            onClick = {
                                onQuickAddBowler(newBowlerName.trim())
                                newBowlerName = ""
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedId?.let { onSelectBowler(it) }
                },
                enabled = selectedId != null,
                modifier = Modifier.testTag("confirm_bowler_button")
            ) {
                Text("Confirm Next Bowler", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DlsCalculatorDialog(
    match: MatchEntity,
    firstInningsRuns: Int,
    currentInnings: InningsEntity?,
    onDismiss: () -> Unit,
    onApplyTarget: (revisedOvers: Int, revisedTarget: Int) -> Unit
) {
    var team1ScoreText by remember { mutableStateOf(firstInningsRuns.coerceAtLeast(150).toString()) }
    var revisedOversText by remember {
        mutableStateOf((match.revisedOvers ?: match.oversLimit).toString())
    }
    var currentOversText by remember {
        mutableStateOf(currentInnings?.oversFormatted ?: "0.0")
    }
    var currentWicketsText by remember {
        mutableStateOf((currentInnings?.wickets ?: 0).toString())
    }

    val calcResult = remember(team1ScoreText, revisedOversText, currentOversText, currentWicketsText) {
        val t1 = team1ScoreText.toIntOrNull() ?: firstInningsRuns
        val revOv = revisedOversText.toDoubleOrNull() ?: match.oversLimit.toDouble()
        val curOv = currentOversText.toDoubleOrNull() ?: 0.0
        val wkts = currentWicketsText.toIntOrNull() ?: 0
        CricketAnalytics.calculateDlsTarget(
            team1Score = t1,
            maxOvers = match.oversLimit,
            revisedTotalOvers = revOv,
            currentOversBowled = curOv,
            wicketsLost = wkts
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("DLS / Rain Target Calculator", fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Calculates Duckworth-Lewis-Stern (DLS) revised target & par score for rain-shortened matches.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = team1ScoreText,
                        onValueChange = { team1ScoreText = it.filter { c -> c.isDigit() } },
                        label = { Text("1st Inn Score") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = revisedOversText,
                        onValueChange = { revisedOversText = it.filter { c -> c.isDigit() } },
                        label = { Text("Revised Overs") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentOversText,
                        onValueChange = { currentOversText = it },
                        label = { Text("2nd Inn Overs Now") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = currentWicketsText,
                        onValueChange = { currentWicketsText = it.filter { c -> c.isDigit() } },
                        label = { Text("Wickets Lost") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Revised Final Target: ${calcResult.revisedFinalTarget} Runs (in ${calcResult.revisedOvers.toInt()} Overs)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "DLS Par Score right now: ${calcResult.parScoreNow} Runs",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Resources Remaining: ${calcResult.resourceRemainingPct}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val revOv = (revisedOversText.toIntOrNull() ?: match.oversLimit).coerceAtLeast(1)
                    onApplyTarget(revOv, calcResult.revisedFinalTarget)
                    onDismiss()
                }
            ) {
                Text("Apply Revised Target", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun AboutCreditsDialog(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Falcons Scorer • Credits", fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "WzStudio Present Falcons Scorer Developed by Samir Parajuli ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Text(
                    text = "Company: WzStudio\nDeveloper: Samir Parajuli\nEmail: warriorzorg@gmail.com\nInstagram: @ig.samir22",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_SENDTO,
                                    android.net.Uri.parse("mailto:warriorzorg@gmail.com")
                                )
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Email", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://www.instagram.com/ig.samir22/")
                                )
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("@ig.samir22", fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = "Version 1.2.0 • © 2026 WzStudio. All Rights Reserved.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}
