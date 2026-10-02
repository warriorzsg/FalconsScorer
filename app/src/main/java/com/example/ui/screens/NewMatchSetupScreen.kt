package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlayerEntity
import com.example.data.TournamentEntity
import com.example.ui.AppSettingsState
import com.example.ui.PendingFixtureSetup
import com.example.ui.theme.FalconGold
import com.example.ui.theme.StadiumGreenDark
import com.example.ui.theme.StadiumGreenPrimary
import kotlin.random.Random

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewMatchSetupScreen(
    allPlayers: List<PlayerEntity>,
    allTournaments: List<TournamentEntity>,
    pendingFixture: PendingFixtureSetup?,
    settings: AppSettingsState,
    onBack: () -> Unit,
    onSavePlayer: (PlayerEntity) -> Unit,
    onDeletePlayer: (Long) -> Unit,
    onCreateMatch: (
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
        allowConsecutiveOvers: Boolean,
        tournamentId: Long?,
        fixtureId: Long?,
        stage: String,
        customStrikerName: String,
        customNonStrikerName: String,
        customBowlerName: String
    ) -> Unit
) {
    BackHandler { onBack() }

    val existingTeams = remember(allPlayers) {
        allPlayers
            .map { it.teamName }
            .filter { !it.equals("Saved Pool", ignoreCase = true) }
            .distinct()
    }

    var selectedTournamentId by remember(pendingFixture) {
        mutableStateOf(pendingFixture?.tournament?.id)
    }
    val linkedFixtureId = pendingFixture?.fixture?.id
    var matchStage by remember(pendingFixture) {
        mutableStateOf(pendingFixture?.fixture?.stage ?: "FRIENDLY")
    }

    var selectedFormat by remember(pendingFixture, settings) {
        mutableStateOf(pendingFixture?.tournament?.format ?: settings.defaultFormat)
    }
    var oversText by remember(pendingFixture, settings) {
        mutableStateOf((pendingFixture?.tournament?.oversLimit ?: settings.defaultOvers).toString())
    }
    var maxBowlerOversText by remember(pendingFixture, settings) {
        mutableStateOf((pendingFixture?.tournament?.maxOversPerBowler ?: settings.defaultMaxBowlerOvers).toString())
    }
    var playersPerTeam by remember(pendingFixture) {
        mutableIntStateOf(pendingFixture?.tournament?.playersPerTeam ?: 11)
    }
    var allowConsecutiveOvers by remember { mutableStateOf(false) }

    var teamA by remember(pendingFixture, existingTeams) {
        mutableStateOf(pendingFixture?.fixture?.teamA ?: existingTeams.getOrElse(0) { "" })
    }
    var teamB by remember(pendingFixture, existingTeams) {
        mutableStateOf(pendingFixture?.fixture?.teamB ?: existingTeams.getOrElse(1) { "" })
    }

    // Digital Coin Toss State & Lightweight Hardware-Accelerated Flip Animation
    var callingTeamIsTeamA by remember { mutableStateOf(true) }
    var selectedCoinCall by remember { mutableStateOf("HEADS") } // HEADS or TAILS
    var coinFlipOutcome by remember { mutableStateOf<String?>(null) } // HEADS or TAILS
    var coinFlipCount by remember { mutableIntStateOf(0) }
    var isCoinSpinning by remember { mutableStateOf(false) }
    val coinSpinProgress = remember { Animatable(0f) }
    var tossWinnerIsTeamA by remember { mutableStateOf(true) }
    var tossDecision by remember { mutableStateOf("BAT") }

    LaunchedEffect(coinFlipCount) {
        if (coinFlipCount > 0) {
            isCoinSpinning = true
            coinSpinProgress.snapTo(0f)
            coinSpinProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
            isCoinSpinning = false
        }
    }

    var venue by remember(pendingFixture, settings) {
        mutableStateOf(
            pendingFixture?.fixture?.venue?.ifBlank { pendingFixture.tournament.venue }
                ?: settings.defaultVenue
        )
    }

    val teamADisplay = teamA.trim().ifBlank { "Team A" }
    val teamBDisplay = teamB.trim().ifBlank { "Team B" }
    val tossWinnerName = if (tossWinnerIsTeamA) teamA.trim() else teamB.trim()
    val tossWinnerDisplay = if (tossWinnerIsTeamA) teamADisplay else teamBDisplay

    val battingFirstTeam = if (tossDecision == "BAT") {
        tossWinnerName
    } else {
        if (tossWinnerIsTeamA) teamB.trim() else teamA.trim()
    }
    val bowlingFirstTeam = if (battingFirstTeam.equals(teamA.trim(), ignoreCase = true)) teamB.trim() else teamA.trim()

    val teamAPlayers = remember(allPlayers, teamA) {
        allPlayers.filter { it.teamName.equals(teamA.trim(), ignoreCase = true) }
    }
    val teamBPlayers = remember(allPlayers, teamB) {
        allPlayers.filter { it.teamName.equals(teamB.trim(), ignoreCase = true) }
    }

    // Saved player profiles from other teams/clubs that can be reused in Team A or Team B
    val savedProfilesPool = remember(allPlayers, teamA, teamB) {
        allPlayers
            .distinctBy { it.name.lowercase() }
            .filter {
                !it.teamName.equals(teamA.trim(), ignoreCase = true) &&
                    !it.teamName.equals(teamB.trim(), ignoreCase = true)
            }
    }

    val batSquad = remember(allPlayers, battingFirstTeam) {
        allPlayers.filter {
            it.teamName.equals(battingFirstTeam, ignoreCase = true) &&
                it.squadStatus != "SUBSTITUTE"
        }
    }
    val bowlSquad = remember(allPlayers, bowlingFirstTeam) {
        allPlayers.filter {
            it.teamName.equals(bowlingFirstTeam, ignoreCase = true) &&
                it.squadStatus != "SUBSTITUTE"
        }
    }

    var selectedStrikerId by remember { mutableStateOf<Long?>(null) }
    var selectedNonStrikerId by remember { mutableStateOf<Long?>(null) }
    var selectedBowlerId by remember { mutableStateOf<Long?>(null) }

    var customStrikerName by remember { mutableStateOf("") }
    var customNonStrikerName by remember { mutableStateOf("") }
    var customBowlerName by remember { mutableStateOf("") }

    var quickAddTeamAPlayerName by remember { mutableStateOf("") }
    var quickAddTeamARole by remember { mutableStateOf("ALL_ROUNDER") }
    var quickAddTeamAStatus by remember { mutableStateOf("PLAYING_XI") }
    var quickAddTeamACaptain by remember { mutableStateOf(false) }
    var quickAddTeamAWk by remember { mutableStateOf(false) }

    var quickAddTeamBPlayerName by remember { mutableStateOf("") }
    var quickAddTeamBRole by remember { mutableStateOf("ALL_ROUNDER") }
    var quickAddTeamBStatus by remember { mutableStateOf("PLAYING_XI") }
    var quickAddTeamBCaptain by remember { mutableStateOf(false) }
    var quickAddTeamBWk by remember { mutableStateOf(false) }

    var profileDialogTargetTeam by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(batSquad) {
        if (selectedStrikerId == null || batSquad.none { it.id == selectedStrikerId }) {
            selectedStrikerId = batSquad.getOrNull(0)?.id
        }
        if (selectedNonStrikerId == null || batSquad.none { it.id == selectedNonStrikerId } || selectedNonStrikerId == selectedStrikerId) {
            selectedNonStrikerId = batSquad.firstOrNull { it.id != selectedStrikerId }?.id
        }
    }
    LaunchedEffect(bowlSquad) {
        if (selectedBowlerId == null || bowlSquad.none { it.id == selectedBowlerId }) {
            selectedBowlerId = bowlSquad.lastOrNull()?.id
        }
    }

    fun selectFormatPreset(fmt: String) {
        selectedFormat = fmt
        when (fmt) {
            "T20" -> {
                oversText = "20"
                maxBowlerOversText = "4"
                playersPerTeam = 11
                allowConsecutiveOvers = false
            }
            "ODI" -> {
                oversText = "50"
                maxBowlerOversText = "10"
                playersPerTeam = 11
                allowConsecutiveOvers = false
            }
            "TEST" -> {
                oversText = "90"
                maxBowlerOversText = "90"
                playersPerTeam = 11
                allowConsecutiveOvers = false
            }
            "CUSTOM" -> {
                oversText = "10"
                maxBowlerOversText = "2"
                playersPerTeam = 11
            }
        }
    }

    fun flipDigitalCoin() {
        val result = if (Random.nextBoolean()) "HEADS" else "TAILS"
        coinFlipOutcome = result
        coinFlipCount += 1
        val callerWon = (result == selectedCoinCall)
        tossWinnerIsTeamA = if (callingTeamIsTeamA) callerWon else !callerWon
    }

    val hasValidTeams = teamA.isNotBlank() &&
        teamB.isNotBlank() &&
        !teamA.trim().equals(teamB.trim(), ignoreCase = true)

    val hasValidOpeners = (selectedStrikerId != null || customStrikerName.isNotBlank()) &&
        (selectedNonStrikerId != null || customNonStrikerName.isNotBlank()) &&
        (selectedBowlerId != null || customBowlerName.isNotBlank())

    val canStartMatch = hasValidTeams && hasValidOpeners

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("new_match_setup_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("new_match_back_button")
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = if (pendingFixture != null) "Start Tournament Match" else "Configure Custom Match",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Teams, Digital Coin Toss, Saved Player Profiles & Openers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Tournament Link Banner
        if (pendingFixture != null) {
            item {
                Surface(
                    color = StadiumGreenDark,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = FalconGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${pendingFixture.tournament.name} • Match #${pendingFixture.fixture.matchNumber} (${pendingFixture.fixture.stage})",
                                color = FalconGold,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "${pendingFixture.fixture.teamA} vs ${pendingFixture.fixture.teamB}",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else if (allTournaments.isNotEmpty()) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Optional: Link Match to Tournament",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedTournamentId == null,
                                onClick = {
                                    selectedTournamentId = null
                                    matchStage = "FRIENDLY"
                                },
                                label = { Text("Friendly / Local Match") }
                            )
                            allTournaments.forEach { t ->
                                FilterChip(
                                    selected = selectedTournamentId == t.id,
                                    onClick = {
                                        selectedTournamentId = t.id
                                        matchStage = "LEAGUE"
                                        selectedFormat = t.format
                                        oversText = t.oversLimit.toString()
                                        maxBowlerOversText = t.maxOversPerBowler.toString()
                                        playersPerTeam = t.playersPerTeam
                                    },
                                    label = { Text(t.name, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. Format & Overs Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Match Format & Rules",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("T20", "ODI", "TEST", "CUSTOM").forEach { fmt ->
                            FilterChip(
                                selected = selectedFormat == fmt,
                                onClick = { selectFormatPreset(fmt) },
                                label = { Text(fmt, fontWeight = FontWeight.Bold) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("format_chip_$fmt")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Quick Overs Presets:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(4 to 1, 5 to 1, 6 to 2, 8 to 2, 10 to 2, 12 to 3, 15 to 3, 20 to 4, 50 to 10).forEach { (ov, maxB) ->
                            FilterChip(
                                selected = oversText == ov.toString() && selectedFormat != "TEST",
                                onClick = {
                                    selectedFormat = when (ov) {
                                        20 -> "T20"
                                        50 -> "ODI"
                                        else -> "CUSTOM"
                                    }
                                    oversText = ov.toString()
                                    maxBowlerOversText = maxB.toString()
                                },
                                label = { Text("$ov Ov") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = oversText,
                            onValueChange = { oversText = it.filter { ch -> ch.isDigit() } },
                            label = { Text(if (selectedFormat == "TEST") "Overs / Day" else "Total Overs") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("overs_limit_input")
                        )

                        OutlinedTextField(
                            value = maxBowlerOversText,
                            onValueChange = { maxBowlerOversText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Max Ov / Bowler") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("max_bowler_overs_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Players Per Team: $playersPerTeam (${playersPerTeam - 1} wickets per innings)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 6, 7, 8, 9, 10, 11, 12).forEach { count ->
                            FilterChip(
                                selected = playersPerTeam == count,
                                onClick = { playersPerTeam = count },
                                label = { Text("$count") }
                            )
                        }
                    }

                    if (selectedFormat == "CUSTOM") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Allow Consecutive Overs by Same Bowler",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Optional for custom local/box rules (disabled in standard T20, ODI & Test)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = allowConsecutiveOvers,
                                onCheckedChange = { allowConsecutiveOvers = it },
                                modifier = Modifier.testTag("allow_consecutive_overs_switch")
                            )
                        }
                    }
                }
            }
        }

        // 2. Custom Team Names & Venue Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. Custom Teams & Venue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Type any custom team names or pick from your saved squads below:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (existingTeams.isNotEmpty()) {
                        Text("Saved Teams (Tap to fill Team A / Team B):", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            existingTeams.forEach { t ->
                                FilterChip(
                                    selected = teamA.equals(t, ignoreCase = true) || teamB.equals(t, ignoreCase = true),
                                    onClick = {
                                        if (teamA.isBlank() || teamA.equals(t, ignoreCase = true)) {
                                            teamA = t
                                        } else {
                                            teamB = t
                                        }
                                    },
                                    label = { Text(t, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = teamA,
                        onValueChange = { teamA = it },
                        label = { Text("Team A Name (Custom)") },
                        placeholder = { Text("Enter any custom Team A name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("team_a_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = teamB,
                        onValueChange = { teamB = it },
                        label = { Text("Team B Name (Custom)") },
                        placeholder = { Text("Enter any custom Team B name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("team_b_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = venue,
                        onValueChange = { venue = it },
                        label = { Text("Match Ground / Venue") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("venue_input")
                    )
                }
            }
        }

        // 3. REALISTIC DIGITAL COIN TOSS FEATURE CARD
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("digital_coin_toss_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = StadiumGreenDark
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Casino,
                                contentDescription = null,
                                tint = FalconGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "3. Official Digital Coin Toss",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Select Heads or Tails, Flip Coin & Choose Bat or Bowl",
                                    color = FalconGold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    // Step A: Calling Captain & Heads/Tails Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Calling Captain:",
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = callingTeamIsTeamA,
                                    onClick = { callingTeamIsTeamA = true },
                                    label = {
                                        Text(
                                            text = teamADisplay,
                                            color = if (callingTeamIsTeamA) MaterialTheme.colorScheme.onSecondaryContainer else Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                )
                                FilterChip(
                                    selected = !callingTeamIsTeamA,
                                    onClick = { callingTeamIsTeamA = false },
                                    label = {
                                        Text(
                                            text = teamBDisplay,
                                            color = if (!callingTeamIsTeamA) MaterialTheme.colorScheme.onSecondaryContainer else Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Coin Call:",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        FilterChip(
                            selected = selectedCoinCall == "HEADS",
                            onClick = { selectedCoinCall = "HEADS" },
                            label = {
                                Text(
                                    text = "🪙 HEADS",
                                    color = if (selectedCoinCall == "HEADS") MaterialTheme.colorScheme.onSecondaryContainer else Color.White,
                                    fontWeight = FontWeight.Black
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("coin_call_heads_chip")
                        )
                        FilterChip(
                            selected = selectedCoinCall == "TAILS",
                            onClick = { selectedCoinCall = "TAILS" },
                            label = {
                                Text(
                                    text = "🪙 TAILS",
                                    color = if (selectedCoinCall == "TAILS") MaterialTheme.colorScheme.onSecondaryContainer else Color.White,
                                    fontWeight = FontWeight.Black
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("coin_call_tails_chip")
                        )
                    }

                    // Realistic Golden Cricket Coin Medallion with Smooth Lightweight Flip Animation
                    val displayedCoinFace = if (isCoinSpinning && coinSpinProgress.value < 0.9f) {
                        if (((coinSpinProgress.value * 8f).toInt() % 2) == 0) "HEADS" else "TAILS"
                    } else {
                        coinFlipOutcome ?: selectedCoinCall
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            color = if (displayedCoinFace == "HEADS") FalconGold else Color(0xFFFFD54F),
                            shape = CircleShape,
                            border = BorderStroke(4.dp, Color(0xFFFFECB3)),
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .size(90.dp)
                                .graphicsLayer {
                                    rotationY = coinSpinProgress.value * 1440f // 4 full 360-degree spins
                                    translationY = -kotlin.math.sin(coinSpinProgress.value * Math.PI).toFloat() * 16f
                                    cameraDistance = 12f * density
                                }
                                .testTag("digital_coin_medallion")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = if (displayedCoinFace == "HEADS") {
                                            Icons.Default.EmojiEvents
                                        } else {
                                            Icons.Default.SportsCricket
                                        },
                                        contentDescription = null,
                                        tint = Color(0xFF1E1300),
                                        modifier = Modifier.size(30.dp)
                                    )
                                    Text(
                                        text = if (isCoinSpinning && coinSpinProgress.value < 0.85f) "SPIN..." else displayedCoinFace,
                                        color = Color(0xFF1E1300),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { flipDigitalCoin() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FalconGold,
                                    contentColor = Color(0xFF1E1300)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("flip_coin_button")
                            ) {
                                Icon(Icons.Default.Casino, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        isCoinSpinning -> "FLIPPING COIN..."
                                        coinFlipOutcome == null -> "FLIP DIGITAL COIN"
                                        else -> "FLIP COIN AGAIN"
                                    },
                                    fontWeight = FontWeight.Black
                                )
                            }

                            if (coinFlipOutcome != null) {
                                val callingName = if (callingTeamIsTeamA) teamADisplay else teamBDisplay
                                Surface(
                                    color = Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = if (isCoinSpinning) {
                                                "🪙 Coin spinning in the air..."
                                            } else {
                                                "🪙 RESULT: $coinFlipOutcome! ($callingName called $selectedCoinCall)"
                                            },
                                            color = FalconGold,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "🏆 $tossWinnerDisplay won the toss!",
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.testTag("coin_toss_result_text")
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Select Heads or Tails and tap 'Flip Digital Coin' to spin the coin and reveal the toss winner.",
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.18f))

                    // Winning Captain Decision (Bat or Bowl First)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Toss Winner:",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = tossWinnerIsTeamA,
                                onClick = { tossWinnerIsTeamA = true },
                                label = {
                                    Text(
                                        text = "$teamADisplay Won Toss",
                                        color = if (tossWinnerIsTeamA) MaterialTheme.colorScheme.onSecondaryContainer else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = !tossWinnerIsTeamA,
                                onClick = { tossWinnerIsTeamA = false },
                                label = {
                                    Text(
                                        text = "$teamBDisplay Won Toss",
                                        color = if (!tossWinnerIsTeamA) MaterialTheme.colorScheme.onSecondaryContainer else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "$tossWinnerDisplay Captain's Choice:",
                            color = FalconGold,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = tossDecision == "BAT",
                                onClick = { tossDecision = "BAT" },
                                label = {
                                    Text(
                                        text = "🏏 BAT FIRST",
                                        color = if (tossDecision == "BAT") MaterialTheme.colorScheme.onSecondaryContainer else Color.White,
                                        fontWeight = FontWeight.Black
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("toss_bat_chip")
                            )
                            FilterChip(
                                selected = tossDecision == "BOWL",
                                onClick = { tossDecision = "BOWL" },
                                label = {
                                    Text(
                                        text = "⚾ BOWL FIRST",
                                        color = if (tossDecision == "BOWL") MaterialTheme.colorScheme.onSecondaryContainer else Color.White,
                                        fontWeight = FontWeight.Black
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("toss_bowl_chip")
                            )
                        }
                    }
                }
            }
        }

        // 4. Match Squads & Saved Player Profile Picker
        if (teamA.isNotBlank() || teamB.isNotBlank()) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "4. Match Squads & Saved Player Profiles",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Reuse saved player profiles, create detailed player profiles (with photo, age & styles), or type random local player names:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Team A Squad Builder
                        if (teamA.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${teamA.trim()} Squad (${teamAPlayers.size} players)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black
                                )
                                OutlinedButton(
                                    onClick = { profileDialogTargetTeam = teamA.trim() },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Full Profile", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = quickAddTeamAPlayerName,
                                    onValueChange = { quickAddTeamAPlayerName = it },
                                    label = { Text("Quick add player to ${teamA.trim()}") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("quick_add_team_a_player_input")
                                )
                                Button(
                                    onClick = {
                                        if (quickAddTeamAPlayerName.isNotBlank()) {
                                            onSavePlayer(
                                                PlayerEntity(
                                                    name = quickAddTeamAPlayerName.trim(),
                                                    teamName = teamA.trim(),
                                                    role = if (quickAddTeamAWk) "WICKET_KEEPER" else quickAddTeamARole,
                                                    jerseyNumber = teamAPlayers.size + 1,
                                                    isCaptain = quickAddTeamACaptain,
                                                    isWicketKeeper = quickAddTeamAWk,
                                                    squadStatus = quickAddTeamAStatus
                                                )
                                            )
                                            quickAddTeamAPlayerName = ""
                                            quickAddTeamACaptain = false
                                            quickAddTeamAWk = false
                                        }
                                    },
                                    enabled = quickAddTeamAPlayerName.isNotBlank(),
                                    modifier = Modifier.testTag("quick_add_team_a_player_btn")
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Player")
                                }
                            }

                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = quickAddTeamACaptain,
                                    onClick = { quickAddTeamACaptain = !quickAddTeamACaptain },
                                    label = { Text("Captain (C)") }
                                )
                                FilterChip(
                                    selected = quickAddTeamAWk,
                                    onClick = { quickAddTeamAWk = !quickAddTeamAWk },
                                    label = { Text("WK") }
                                )
                                listOf("PLAYING_XI" to "XI", "IMPACT_PLAYER" to "Impact", "SUBSTITUTE" to "Sub").forEach { (st, lbl) ->
                                    FilterChip(
                                        selected = quickAddTeamAStatus == st,
                                        onClick = { quickAddTeamAStatus = st },
                                        label = { Text(lbl) }
                                    )
                                }
                            }

                            if (savedProfilesPool.isNotEmpty()) {
                                Text(
                                    text = "Tap a Saved Player (from Settings) to reuse in ${teamA.trim()}:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    savedProfilesPool.take(15).forEach { saved ->
                                        val batShort = if (saved.battingStyle.startsWith("Left", ignoreCase = true)) "LHB" else "RHB"
                                        FilterChip(
                                            selected = false,
                                            onClick = {
                                                onSavePlayer(
                                                    saved.copy(
                                                        id = 0L,
                                                        teamName = teamA.trim()
                                                    )
                                                )
                                            },
                                            label = {
                                                Text("+ ${saved.name} ($batShort • ${saved.bowlingStyle})")
                                            }
                                        )
                                    }
                                }
                            }

                            if (teamAPlayers.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    teamAPlayers.forEach { p ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(start = 8.dp, end = 2.dp, top = 2.dp, bottom = 2.dp)
                                            ) {
                                                Text(
                                                    text = p.formattedNameWithRoles,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                IconButton(
                                                    onClick = { onDeletePlayer(p.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Remove",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        // Team B Squad Builder
                        if (teamB.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${teamB.trim()} Squad (${teamBPlayers.size} players)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black
                                )
                                OutlinedButton(
                                    onClick = { profileDialogTargetTeam = teamB.trim() },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Full Profile", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = quickAddTeamBPlayerName,
                                    onValueChange = { quickAddTeamBPlayerName = it },
                                    label = { Text("Quick add player to ${teamB.trim()}") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("quick_add_team_b_player_input")
                                )
                                Button(
                                    onClick = {
                                        if (quickAddTeamBPlayerName.isNotBlank()) {
                                            onSavePlayer(
                                                PlayerEntity(
                                                    name = quickAddTeamBPlayerName.trim(),
                                                    teamName = teamB.trim(),
                                                    role = if (quickAddTeamBWk) "WICKET_KEEPER" else quickAddTeamBRole,
                                                    jerseyNumber = teamBPlayers.size + 1,
                                                    isCaptain = quickAddTeamBCaptain,
                                                    isWicketKeeper = quickAddTeamBWk,
                                                    squadStatus = quickAddTeamBStatus
                                                )
                                            )
                                            quickAddTeamBPlayerName = ""
                                            quickAddTeamBCaptain = false
                                            quickAddTeamBWk = false
                                        }
                                    },
                                    enabled = quickAddTeamBPlayerName.isNotBlank(),
                                    modifier = Modifier.testTag("quick_add_team_b_player_btn")
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Player")
                                }
                            }

                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = quickAddTeamBCaptain,
                                    onClick = { quickAddTeamBCaptain = !quickAddTeamBCaptain },
                                    label = { Text("Captain (C)") }
                                )
                                FilterChip(
                                    selected = quickAddTeamBWk,
                                    onClick = { quickAddTeamBWk = !quickAddTeamBWk },
                                    label = { Text("WK") }
                                )
                                listOf("PLAYING_XI" to "XI", "IMPACT_PLAYER" to "Impact", "SUBSTITUTE" to "Sub").forEach { (st, lbl) ->
                                    FilterChip(
                                        selected = quickAddTeamBStatus == st,
                                        onClick = { quickAddTeamBStatus = st },
                                        label = { Text(lbl) }
                                    )
                                }
                            }

                            if (savedProfilesPool.isNotEmpty()) {
                                Text(
                                    text = "Tap a Saved Player (from Settings) to reuse in ${teamB.trim()}:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    savedProfilesPool.take(15).forEach { saved ->
                                        val batShort = if (saved.battingStyle.startsWith("Left", ignoreCase = true)) "LHB" else "RHB"
                                        FilterChip(
                                            selected = false,
                                            onClick = {
                                                onSavePlayer(
                                                    saved.copy(
                                                        id = 0L,
                                                        teamName = teamB.trim()
                                                    )
                                                )
                                            },
                                            label = {
                                                Text("+ ${saved.name} ($batShort • ${saved.bowlingStyle})")
                                            }
                                        )
                                    }
                                }
                            }

                            if (teamBPlayers.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    teamBPlayers.forEach { p ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(start = 8.dp, end = 2.dp, top = 2.dp, bottom = 2.dp)
                                            ) {
                                                Text(
                                                    text = p.formattedNameWithRoles,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                IconButton(
                                                    onClick = { onDeletePlayer(p.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Remove",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(14.dp)
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
            }
        }

        // 5. Opening Players Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "5. Select or Type Opening Players",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Opening Striker
                    Text(
                        text = "Opening Striker (${battingFirstTeam.ifBlank { "Batting Team" }}):",
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (batSquad.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            batSquad.forEach { p ->
                                FilterChip(
                                    selected = selectedStrikerId == p.id && customStrikerName.isBlank(),
                                    onClick = {
                                        customStrikerName = ""
                                        selectedStrikerId = p.id
                                        if (selectedNonStrikerId == p.id) {
                                            selectedNonStrikerId = batSquad.firstOrNull { it.id != p.id }?.id
                                        }
                                    },
                                    label = { Text(p.formattedNameWithRoles) }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = customStrikerName,
                        onValueChange = { customStrikerName = it },
                        label = { Text("Or type new Opening Striker name") },
                        placeholder = { Text("e.g., Local Opener 1") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_striker_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Opening Non-Striker
                    Text(
                        text = "Opening Non-Striker (${battingFirstTeam.ifBlank { "Batting Team" }}):",
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (batSquad.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            batSquad.forEach { p ->
                                FilterChip(
                                    selected = selectedNonStrikerId == p.id && customNonStrikerName.isBlank(),
                                    onClick = {
                                        if (p.id != selectedStrikerId) {
                                            customNonStrikerName = ""
                                            selectedNonStrikerId = p.id
                                        }
                                    },
                                    label = { Text(p.formattedNameWithRoles) }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = customNonStrikerName,
                        onValueChange = { customNonStrikerName = it },
                        label = { Text("Or type new Opening Non-Striker name") },
                        placeholder = { Text("e.g., Local Opener 2") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_non_striker_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Opening Bowler
                    Text(
                        text = "Opening Bowler (${bowlingFirstTeam.ifBlank { "Bowling Team" }}):",
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (bowlSquad.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            bowlSquad.forEach { p ->
                                FilterChip(
                                    selected = selectedBowlerId == p.id && customBowlerName.isBlank(),
                                    onClick = {
                                        customBowlerName = ""
                                        selectedBowlerId = p.id
                                    },
                                    label = { Text(p.formattedNameWithRoles) }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = customBowlerName,
                        onValueChange = { customBowlerName = it },
                        label = { Text("Or type new Opening Bowler name") },
                        placeholder = { Text("e.g., Opening Bowler") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_bowler_input")
                    )
                }
            }
        }

        // 6. Start Match Button
        item {
            Button(
                onClick = {
                    val overs = (oversText.toIntOrNull() ?: 20).coerceIn(1, 200)
                    val maxBowler = (maxBowlerOversText.toIntOrNull() ?: 4).coerceIn(1, overs)
                    onCreateMatch(
                        teamA.trim(),
                        teamB.trim(),
                        selectedFormat,
                        overs,
                        maxBowler,
                        playersPerTeam,
                        tossWinnerName,
                        tossDecision,
                        venue.trim().ifBlank { "Cricket Ground" },
                        if (customStrikerName.isNotBlank()) null else selectedStrikerId,
                        if (customNonStrikerName.isNotBlank()) null else selectedNonStrikerId,
                        if (customBowlerName.isNotBlank()) null else selectedBowlerId,
                        if (selectedFormat == "CUSTOM") allowConsecutiveOvers else false,
                        selectedTournamentId,
                        linkedFixtureId,
                        matchStage,
                        customStrikerName.trim(),
                        customNonStrikerName.trim(),
                        customBowlerName.trim()
                    )
                },
                enabled = canStartMatch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("confirm_start_match_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (canStartMatch) "START LIVE SCORING" else "ENTER TEAMS & OPENERS TO START",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }

    if (profileDialogTargetTeam != null) {
        AddOrEditPlayerDialog(
            initialPlayer = null,
            defaultTeam = profileDialogTargetTeam!!,
            onDismiss = { profileDialogTargetTeam = null },
            onConfirm = { newProfile ->
                onSavePlayer(newProfile)
                profileDialogTargetTeam = null
            }
        )
    }
}
