package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.PlayerEntity
import com.example.data.TournamentEntity
import com.example.ui.AppSettingsState
import com.example.ui.theme.CricketCrimson
import com.example.ui.theme.FalconGold
import com.example.ui.theme.StadiumGreenDark
import com.example.ui.theme.StadiumGreenPrimary
import java.util.Locale

/**
 * Easy-to-update registry for Upcoming Features displayed inside Settings.
 * Add or modify entries here in future releases.
 */
data class UpcomingFeatureItem(
    val id: String,
    val title: String,
    val description: String,
    val badgeLabel: String = "COMING SOON"
)

val UPCOMING_UPDATES_LIST: List<UpcomingFeatureItem> = listOf(
    UpcomingFeatureItem(
        id = "wagon_wheel_pitch_map",
        title = "360° Wagon Wheel & Pitch Map Tracker",
        description = "Record shot directions and delivery pitch lengths on an interactive turf radar for every boundary and wicket.",
        badgeLabel = "COMING SOON"
    ),
    UpcomingFeatureItem(
        id = "live_stream_ticker_overlay",
        title = "Live Broadcast Overlay & PDF Match Reports",
        description = "Export broadcast-grade PDF scorebooks and generate a live scoreboard ticker overlay for local tournament streams.",
        badgeLabel = "COMING SOON"
    )
)

private val BATTING_STYLE_OPTIONS = listOf(
    "Right-hand bat",
    "Left-hand bat"
)

private val BOWLING_STYLE_OPTIONS = listOf(
    "Right-arm fast",
    "Right-arm medium",
    "Right-arm off-spin",
    "Right-arm leg-spin",
    "Left-arm fast",
    "Left-arm medium",
    "Left-arm orthodox",
    "Left-arm wrist-spin",
    "None"
)

private val PLAYER_ROLE_OPTIONS = listOf(
    "ALL_ROUNDER" to "All-Rounder",
    "BATSMAN" to "Batsman",
    "BOWLER" to "Bowler",
    "WICKET_KEEPER" to "Wicketkeeper"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettingsState,
    allPlayers: List<PlayerEntity>,
    tournaments: List<TournamentEntity>,
    totalPlayersCount: Int,
    totalTeamsCount: Int,
    totalMatchesCount: Int,
    totalTournamentsCount: Int,
    statusMessage: String?,
    onSaveSettings: (AppSettingsState) -> Unit,
    onSavePlayer: (PlayerEntity) -> Unit,
    onDeletePlayer: (Long) -> Unit,
    onClearMatchHistoryOnly: () -> Unit,
    onResetAllData: () -> Unit,
    onOpenUploadPlayers: () -> Unit,
    onOpenTournamentsHub: () -> Unit,
    onSelectTournament: (Long) -> Unit,
    onDeleteTournament: (Long) -> Unit,
    onOpenAboutCredits: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var selectedSectionFilter by remember { mutableStateOf("ALL") } // ALL, PLAYERS, DEMO, CUP_TROPHY, ABOUT, UPDATES, APP_SETTINGS

    // Settings Player Management State (Save & Edit reusable players directly in Settings)
    var editingPlayer by remember { mutableStateOf<PlayerEntity?>(null) }
    var playerNameInput by remember { mutableStateOf("") }
    var selectedBattingStyle by remember { mutableStateOf(BATTING_STYLE_OPTIONS.first()) }
    var selectedBowlingStyle by remember { mutableStateOf(BOWLING_STYLE_OPTIONS.first()) }
    var selectedPlayerRole by remember { mutableStateOf("ALL_ROUNDER") }
    var playerTeamInput by remember { mutableStateOf("") }
    var playerSearchQuery by remember { mutableStateOf("") }

    val filteredPlayers = remember(allPlayers, playerSearchQuery) {
        if (playerSearchQuery.isBlank()) {
            allPlayers
        } else {
            val q = playerSearchQuery.trim().lowercase()
            allPlayers.filter {
                it.name.lowercase().contains(q) ||
                    it.battingStyle.lowercase().contains(q) ||
                    it.bowlingStyle.lowercase().contains(q) ||
                    it.teamName.lowercase().contains(q)
            }
        }
    }

    var themeMode by remember(settings) { mutableStateOf(settings.themeMode) }
    var defaultFormat by remember(settings) { mutableStateOf(settings.defaultFormat) }
    var defaultOversText by remember(settings) { mutableStateOf(settings.defaultOvers.toString()) }
    var defaultMaxBowlerOversText by remember(settings) { mutableStateOf(settings.defaultMaxBowlerOvers.toString()) }
    var defaultVenue by remember(settings) { mutableStateOf(settings.defaultVenue) }
    var confirmBeforeUndo by remember(settings) { mutableStateOf(settings.confirmBeforeUndo) }

    var showResetAllConfirmDialog by remember { mutableStateOf(false) }
    var showClearMatchesConfirmDialog by remember { mutableStateOf(false) }
    var showDemoSandboxDialog by remember { mutableStateOf(false) }

    fun loadPlayerForEdit(player: PlayerEntity) {
        editingPlayer = player
        playerNameInput = player.name
        selectedBattingStyle = player.battingStyle.ifBlank { BATTING_STYLE_OPTIONS.first() }
        selectedBowlingStyle = player.bowlingStyle.ifBlank { BOWLING_STYLE_OPTIONS.first() }
        selectedPlayerRole = player.role.ifBlank { "ALL_ROUNDER" }
        playerTeamInput = if (player.teamName == "Saved Pool") "" else player.teamName
    }

    fun clearPlayerForm() {
        editingPlayer = null
        playerNameInput = ""
        selectedBattingStyle = BATTING_STYLE_OPTIONS.first()
        selectedBowlingStyle = BOWLING_STYLE_OPTIONS.first()
        selectedPlayerRole = "ALL_ROUNDER"
        playerTeamInput = ""
    }

    fun savePlayerFromSettings() {
        val cleanName = playerNameInput.trim()
        if (cleanName.isBlank()) return
        val existing = editingPlayer
        val targetTeam = playerTeamInput.trim().ifBlank { existing?.teamName ?: "Saved Pool" }
        val entity = if (existing != null) {
            existing.copy(
                name = cleanName,
                teamName = targetTeam,
                role = selectedPlayerRole,
                battingStyle = selectedBattingStyle,
                bowlingStyle = selectedBowlingStyle,
                isWicketKeeper = selectedPlayerRole == "WICKET_KEEPER" || existing.isWicketKeeper
            )
        } else {
            PlayerEntity(
                name = cleanName,
                teamName = targetTeam,
                role = selectedPlayerRole,
                battingStyle = selectedBattingStyle,
                bowlingStyle = selectedBowlingStyle,
                isWicketKeeper = selectedPlayerRole == "WICKET_KEEPER"
            )
        }
        onSavePlayer(entity)
        clearPlayerForm()
    }

    fun openEmailClient() {
        runCatching {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:warriorzorg@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "Falcons Scorer App Feedback / Inquiry")
            }
            context.startActivity(Intent.createChooser(intent, "Contact Developer via Email"))
        }
    }

    fun openInstagramProfile() {
        runCatching {
            val uri = Uri.parse("https://www.instagram.com/ig.samir22/")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen")
    ) {
        // Header
        Surface(
            color = StadiumGreenDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = FalconGold,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Settings",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Player Management • Demo Data • Cup/Trophy • About & Credits • App Settings",
                            color = FalconGold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Section Filter Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL" to "All Sections",
                        "PLAYERS" to "Player Management",
                        "DEMO" to "Demo Data",
                        "CUP_TROPHY" to "Cup/Trophy",
                        "ABOUT" to "About & Credits",
                        "UPDATES" to "Upcoming Updates",
                        "APP_SETTINGS" to "App Settings"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedSectionFilter == key,
                            onClick = { selectedSectionFilter = key },
                            label = {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedSectionFilter == key) Color(0xFF1E1300) else Color.White
                                )
                            },
                            modifier = Modifier.testTag("settings_filter_$key")
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!statusMessage.isNullOrBlank()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "✓ $statusMessage",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // =========================================================================
            // SECTION 1: PLAYER MANAGEMENT SYSTEM (Save & Reuse Players Across Matches)
            // =========================================================================
            if (selectedSectionFilter in setOf("ALL", "PLAYERS")) {
                item {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_section_player_management"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Player Management (Reusable Profiles)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Save player names, batting styles & bowling styles to reuse in any match",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Surface(
                                    color = FalconGold,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "${allPlayers.size} Saved",
                                        color = Color(0xFF1E1300),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Add / Edit Player Form inside Settings
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = if (editingPlayer != null) {
                                            "Editing Saved Player: ${editingPlayer?.name}"
                                        } else {
                                            "Add New Reusable Player Profile"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = playerNameInput,
                                            onValueChange = { playerNameInput = it },
                                            label = { Text("Player Name *") },
                                            placeholder = { Text("e.g., Samir Parajuli") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1.2f)
                                                .testTag("settings_player_name_input")
                                        )
                                        OutlinedTextField(
                                            value = playerTeamInput,
                                            onValueChange = { playerTeamInput = it },
                                            label = { Text("Default Team (Optional)") },
                                            placeholder = { Text("Any / Pool") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("settings_player_team_input")
                                        )
                                    }

                                    // Batting Style Selection
                                    Text(
                                        text = "Batting Style:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        BATTING_STYLE_OPTIONS.forEach { style ->
                                            FilterChip(
                                                selected = selectedBattingStyle == style,
                                                onClick = { selectedBattingStyle = style },
                                                label = {
                                                    Text(
                                                        text = "🏏 $style",
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("settings_batting_style_${style.replace(" ", "_").lowercase()}")
                                            )
                                        }
                                    }

                                    // Bowling Style Selection
                                    Text(
                                        text = "Bowling Style:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        BOWLING_STYLE_OPTIONS.forEach { style ->
                                            FilterChip(
                                                selected = selectedBowlingStyle == style,
                                                onClick = { selectedBowlingStyle = style },
                                                label = {
                                                    Text(
                                                        text = if (style == "None") "None" else "⚾ $style",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = if (selectedBowlingStyle == style) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                modifier = Modifier.testTag(
                                                    "settings_bowling_style_${style.replace(" ", "_").lowercase()}"
                                                )
                                            )
                                        }
                                    }

                                    // Playing Role Selection
                                    Text(
                                        text = "Playing Role:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        PLAYER_ROLE_OPTIONS.forEach { (roleKey, roleLabel) ->
                                            FilterChip(
                                                selected = selectedPlayerRole == roleKey,
                                                onClick = { selectedPlayerRole = roleKey },
                                                label = { Text(roleLabel, style = MaterialTheme.typography.labelMedium) },
                                                modifier = Modifier.testTag("settings_player_role_$roleKey")
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (editingPlayer != null) {
                                            OutlinedButton(
                                                onClick = { clearPlayerForm() },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Cancel Edit", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Button(
                                            onClick = { savePlayerFromSettings() },
                                            enabled = playerNameInput.isNotBlank(),
                                            modifier = Modifier
                                                .weight(1.5f)
                                                .testTag("settings_save_player_btn"),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (editingPlayer != null) Icons.Default.Save else Icons.Default.PersonAdd,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (editingPlayer != null) "Update Player" else "Save Player for Reuse",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Saved Players List (Search, Edit, Delete)
                            if (allPlayers.isEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No players saved yet",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Save player names, batting styles, and bowling styles above so you can pick them with one tap in any future match.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = playerSearchQuery,
                                    onValueChange = { playerSearchQuery = it },
                                    label = { Text("Search Saved Players (${allPlayers.size})") },
                                    placeholder = { Text("Filter by name, batting/bowling style, or team") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settings_player_search_input")
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    filteredPlayers.take(25).forEach { player ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("settings_saved_player_${player.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = player.formattedNameWithRoles,
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            fontWeight = FontWeight.ExtraBold
                                                        )
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.primaryContainer,
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                text = player.teamName,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "🏏 ${player.battingStyle}  •  ⚾ ${player.bowlingStyle}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(
                                                        onClick = { loadPlayerForEdit(player) },
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .testTag("settings_edit_player_${player.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Edit Player",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { onDeletePlayer(player.id) },
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .testTag("settings_delete_player_${player.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.DeleteOutline,
                                                            contentDescription = "Delete Player",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            FilledTonalButton(
                                onClick = onOpenUploadPlayers,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_upload_players_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Full Player Directory (Photos, Bulk Import & Career Stats)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // SECTION 2: DEMO DATA (Strictly inside Settings only, isolated from user DB)
            // =========================================================================
            if (selectedSectionFilter in setOf("ALL", "DEMO")) {
                item {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_section_demo_data"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Demo Data (Sandbox Only)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "Strictly isolated from your real matches, players & tournaments",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Test ball-by-ball scoring, strike rotation, extras, and wickets in an isolated in-memory sandbox without adding any fake matches or players to your real database.",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Button(
                                onClick = { showDemoSandboxDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_load_demo_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Launch Interactive Demo Sandbox", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // SECTION 3: CUP / TROPHY SECTION (Champions Cabinet & Cup Manager)
            // =========================================================================
            if (selectedSectionFilter in setOf("ALL", "CUP_TROPHY")) {
                item {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_section_cup_trophy"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = FalconGold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Cup / Trophy Cabinet",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Championship Trophies, Cup Winners & Tournament Records",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Surface(
                                    color = FalconGold,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "${tournaments.size} Cups",
                                        color = Color(0xFF1E1300),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            val completedCups = tournaments.filter { it.championTeam.isNotBlank() }
                            if (completedCups.isNotEmpty()) {
                                Text(
                                    text = "🏆 Hall of Champions:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                completedCups.forEach { cup ->
                                    Surface(
                                        color = StadiumGreenPrimary,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSelectTournament(cup.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "🏆 ${cup.name}",
                                                    color = FalconGold,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Text(
                                                    text = "Champion: ${cup.championTeam}" +
                                                        if (cup.runnerUpTeam.isNotBlank()) " • Runner-Up: ${cup.runnerUpTeam}" else "",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.EmojiEvents,
                                                contentDescription = null,
                                                tint = FalconGold
                                            )
                                        }
                                    }
                                }
                            }

                            if (tournaments.isEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No Cups or Trophies recorded yet",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Create a custom Cup or Tournament to track fixtures, standings, NRR, knockouts, and finals trophies.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Registered Cups & Tournaments:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                tournaments.forEach { tourney ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSelectTournament(tourney.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = tourney.name,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Text(
                                                    text = "${tourney.format} (${tourney.oversLimit} Ov) • ${tourney.status}" +
                                                        if (tourney.championTeam.isNotBlank()) " • 🏆 ${tourney.championTeam}" else "",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteTournament(tourney.id) },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete Cup",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = onOpenTournamentsHub,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_open_cup_trophy_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Cup / Trophy & Tournament Manager", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // SECTION 4: ABOUT & CREDITS (Strictly inside Settings only)
            // =========================================================================
            if (selectedSectionFilter in setOf("ALL", "ABOUT")) {
                item {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_section_about_credits"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "About & Credits",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Official App Branding, Developer & Contact Information",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Official Credit Banner
                            Surface(
                                color = StadiumGreenDark,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("official_branding_credit_text")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "WzStudio Present Falcons Scorer Developed by Samir Parajuli ",
                                        color = FalconGold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Version 1.2.0 • © 2026 WzStudio & Samir Parajuli",
                                        color = Color.White.copy(alpha = 0.85f),
                                        style = MaterialTheme.typography.labelMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Company & Developer Details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Business,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Company", style = MaterialTheme.typography.labelSmall)
                                            Text("WzStudio", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Code,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Developer", style = MaterialTheme.typography.labelSmall)
                                            Text("Samir Parajuli", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }

                            // Clickable Email Link
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { openEmailClient() }
                                    .testTag("settings_email_link")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = "Email",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Contact Email", style = MaterialTheme.typography.labelSmall)
                                            Text(
                                                text = "warriorzorg@gmail.com",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "Send Email",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Clickable Instagram Link
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { openInstagramProfile() }
                                    .testTag("settings_instagram_link")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Public,
                                            contentDescription = "Instagram",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Instagram Profile", style = MaterialTheme.typography.labelSmall)
                                            Text(
                                                text = "@ig.samir22",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "Open Instagram",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = onOpenAboutCredits,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_open_about_credits_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View Full About & Credits Page", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // SECTION 5: UPCOMING UPDATES (Easy to update in future versions)
            // =========================================================================
            if (selectedSectionFilter in setOf("ALL", "UPDATES")) {
                item {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upcoming_updates_card"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NewReleases,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Upcoming Updates",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Roadmap features arriving in future versions",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            UPCOMING_UPDATES_LIST.forEachIndexed { index, feature ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("upcoming_feature_${feature.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${index + 1}. ${feature.title}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = FalconGold,
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Text(
                                                    text = feature.badgeLabel,
                                                    color = Color(0xFF1E1300),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = feature.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // SECTION 6: APP SETTINGS (Theme, Scorer Defaults & Data Reset)
            // =========================================================================
            if (selectedSectionFilter in setOf("ALL", "APP_SETTINGS")) {
                item {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_section_app_settings"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "App Settings & Data Reset",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Saved User Data: $totalTeamsCount Teams • $totalPlayersCount Profiles • $totalMatchesCount Matches • $totalTournamentsCount Cups",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text("App Appearance Theme:", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("SYSTEM" to "System", "DARK" to "Dark Stadium", "LIGHT" to "Daylight").forEach { (key, label) ->
                                    FilterChip(
                                        selected = themeMode == key,
                                        onClick = { themeMode = key },
                                        label = { Text(label, fontWeight = FontWeight.Bold) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("theme_chip_$key")
                                    )
                                }
                            }

                            HorizontalDivider()

                            Text("Default Match Format:", style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("T20" to 20, "ODI" to 50, "TEST" to 90, "CUSTOM" to 10).forEach { (fmt, ov) ->
                                    FilterChip(
                                        selected = defaultFormat == fmt,
                                        onClick = {
                                            defaultFormat = fmt
                                            defaultOversText = ov.toString()
                                            defaultMaxBowlerOversText = when (fmt) {
                                                "T20" -> "4"
                                                "ODI" -> "10"
                                                "TEST" -> "90"
                                                else -> "2"
                                            }
                                        },
                                        label = { Text(fmt, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = defaultOversText,
                                    onValueChange = { defaultOversText = it.filter { c -> c.isDigit() } },
                                    label = { Text("Default Overs") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = defaultMaxBowlerOversText,
                                    onValueChange = { defaultMaxBowlerOversText = it.filter { c -> c.isDigit() } },
                                    label = { Text("Max Ov / Bowler") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = defaultVenue,
                                onValueChange = { defaultVenue = it },
                                label = { Text("Default Ground / Venue") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Confirm Before Undo Ball",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Ask for confirmation before undoing the last delivery",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = confirmBeforeUndo,
                                    onCheckedChange = { confirmBeforeUndo = it }
                                )
                            }

                            Button(
                                onClick = {
                                    onSaveSettings(
                                        AppSettingsState(
                                            themeMode = themeMode,
                                            defaultFormat = defaultFormat,
                                            defaultOvers = (defaultOversText.toIntOrNull() ?: 20).coerceIn(1, 200),
                                            defaultMaxBowlerOvers = (defaultMaxBowlerOversText.toIntOrNull() ?: 4).coerceAtLeast(1),
                                            defaultVenue = defaultVenue.trim().ifBlank { "Cricket Ground" },
                                            confirmBeforeUndo = confirmBeforeUndo
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("save_settings_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save App Preferences", fontWeight = FontWeight.Black)
                            }

                            HorizontalDivider()

                            Text(
                                text = "Reset Real User Data:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.error
                            )

                            OutlinedButton(
                                onClick = { showClearMatchesConfirmDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("clear_matches_only_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Clear Match History Only (Keep Profiles)", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showResetAllConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CricketCrimson,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reset_all_data_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reset All Data (Wipe Profiles, Matches & Cups)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDemoSandboxDialog) {
        DemoPreviewSandboxDialog(onDismiss = { showDemoSandboxDialog = false })
    }

    if (showResetAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetAllConfirmDialog = false },
            title = {
                Text("Reset All App Data?", fontWeight = FontWeight.Black)
            },
            text = {
                Text("This will permanently delete all custom teams, player profiles, tournaments, matches, innings, and ball-by-ball records.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAllData()
                        showResetAllConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CricketCrimson),
                    modifier = Modifier.testTag("confirm_reset_all_btn")
                ) {
                    Text("Yes, Reset Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAllConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearMatchesConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearMatchesConfirmDialog = false },
            title = {
                Text("Clear Match History Only?", fontWeight = FontWeight.Black)
            },
            text = {
                Text("This will delete all matches and scorecards while keeping all your custom teams, player profiles, and tournaments intact.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearMatchHistoryOnly()
                        showClearMatchesConfirmDialog = false
                    },
                    modifier = Modifier.testTag("confirm_clear_matches_btn")
                ) {
                    Text("Clear Matches", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearMatchesConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Self-contained, in-memory Demo Preview Sandbox strictly limited to Settings ONLY.
 * Never writes or leaks demo matches, players, scores, or tournaments into actual scoring,
 * match history, player leaderboards, or tournament records.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DemoPreviewSandboxDialog(onDismiss: () -> Unit) {
    var runs by remember { mutableIntStateOf(42) }
    var wickets by remember { mutableIntStateOf(1) }
    var legalBalls by remember { mutableIntStateOf(26) } // 4.2 overs
    var strikerRuns by remember { mutableIntStateOf(24) }
    var strikerBalls by remember { mutableIntStateOf(15) }
    var nonStrikerRuns by remember { mutableIntStateOf(14) }
    var nonStrikerBalls by remember { mutableIntStateOf(9) }
    var strikerOnStrike by remember { mutableStateOf(true) }
    val recentBalls = remember { mutableStateListOf("1", "4", "0", "2", "6", "1") }

    val oversFormatted = "${legalBalls / 6}.${legalBalls % 6}"
    val crr = if (legalBalls > 0) (runs * 6.0) / legalBalls else 0.0

    fun simulateBall(ballRuns: Int, isWicket: Boolean = false, isWide: Boolean = false) {
        if (isWide) {
            runs += 1
            recentBalls.add("wd")
            return
        }
        legalBalls += 1
        runs += ballRuns
        if (isWicket) {
            wickets = (wickets + 1).coerceAtMost(10)
            recentBalls.add("W")
            if (strikerOnStrike) {
                strikerRuns = 0
                strikerBalls = 0
            } else {
                nonStrikerRuns = 0
                nonStrikerBalls = 0
            }
        } else {
            recentBalls.add(ballRuns.toString())
            if (strikerOnStrike) {
                strikerRuns += ballRuns
                strikerBalls += 1
            } else {
                nonStrikerRuns += ballRuns
                nonStrikerBalls += 1
            }
            if (ballRuns % 2 == 1) {
                strikerOnStrike = !strikerOnStrike
            }
        }
        if (legalBalls % 6 == 0) {
            strikerOnStrike = !strikerOnStrike
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Interactive Demo Sandbox (Settings Only)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Isolated preview — never saved to real Match History, Scorer, or Tournaments",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = StadiumGreenDark,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "DEMO T20 • SAMPLE TEAM A vs SAMPLE TEAM B",
                            color = FalconGold,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "$runs/$wickets",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "($oversFormatted / 20 Ov) • CRR ${String.format(Locale.US, "%.2f", crr)}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${if (strikerOnStrike) "🏏 " else ""}Demo Batter 1: $strikerRuns ($strikerBalls)   |   ${if (!strikerOnStrike) "🏏 " else ""}Demo Batter 2: $nonStrikerRuns ($nonStrikerBalls)",
                            color = FalconGold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Text(
                    text = "Recent Balls: ${recentBalls.takeLast(8).joinToString(" • ")}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider()

                Text(
                    text = "Tap to test ball-by-ball simulation:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0, 1, 2, 3, 4, 6).forEach { r ->
                        FilterChip(
                            selected = false,
                            onClick = { simulateBall(r) },
                            label = { Text(if (r == 0) "0 (Dot)" else "+$r", fontWeight = FontWeight.Bold) }
                        )
                    }
                    FilterChip(
                        selected = false,
                        onClick = { simulateBall(0, isWide = true) },
                        label = { Text("Wide (+1)", fontWeight = FontWeight.Bold) }
                    )
                    FilterChip(
                        selected = false,
                        onClick = { simulateBall(0, isWicket = true) },
                        label = { Text("WICKET", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_demo_sandbox_btn")
            ) {
                Text("Close Demo", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    runs = 0
                    wickets = 0
                    legalBalls = 0
                    strikerRuns = 0
                    strikerBalls = 0
                    nonStrikerRuns = 0
                    nonStrikerBalls = 0
                    recentBalls.clear()
                }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset Sandbox")
            }
        }
    )
}
