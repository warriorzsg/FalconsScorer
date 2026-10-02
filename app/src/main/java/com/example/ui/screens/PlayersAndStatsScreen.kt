package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.PlayerCareerStat
import com.example.data.PlayerEntity
import com.example.ui.theme.FalconGold
import com.example.ui.theme.StadiumGreenDark
import com.example.ui.theme.StadiumGreenPrimary
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Lightweight, low-memory photo saver that downscales a picked image into app-private storage
 * so player profile photos work 100% offline and smoothly on low-end Android devices.
 */
fun savePlayerPhotoLocally(context: Context, sourceUri: Uri): String? {
    return runCatching {
        val dir = File(context.filesDir, "player_photos").apply { mkdirs() }
        val targetFile = File(dir, "player_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            val original = BitmapFactory.decodeStream(input) ?: return null
            val maxDim = 256
            val ratio = minOf(
                maxDim.toFloat() / original.width.coerceAtLeast(1),
                maxDim.toFloat() / original.height.coerceAtLeast(1),
                1.0f
            )
            val w = (original.width * ratio).toInt().coerceAtLeast(1)
            val h = (original.height * ratio).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(original, w, h, true)
            FileOutputStream(targetFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
            }
        }
        targetFile.absolutePath
    }.getOrNull()
}

@Composable
fun PlayerAvatarBadge(
    name: String,
    jerseyNumber: Int,
    photoUri: String,
    size: Dp = 48.dp
) {
    val bitmap = remember(photoUri) {
        if (photoUri.isNotBlank()) {
            runCatching {
                val f = File(photoUri)
                if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
            }.getOrNull()
        } else null
    }

    Surface(
        color = StadiumGreenPrimary,
        shape = CircleShape,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "$name photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "#$jerseyNumber",
                    color = FalconGold,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun PlayersAndStatsScreen(
    allPlayers: List<PlayerEntity>,
    careerStats: List<PlayerCareerStat>,
    onSavePlayer: (PlayerEntity) -> Unit,
    onDeletePlayer: (Long) -> Unit,
    onDeleteTeam: (String) -> Unit,
    onBulkUploadPlayers: (String, String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Player Profiles & Squads, 1 = Career Stats
    var selectedTeamFilter by remember { mutableStateOf("ALL") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var editingPlayer by remember { mutableStateOf<PlayerEntity?>(null) }
    var viewingProfileStat by remember { mutableStateOf<PlayerCareerStat?>(null) }
    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var showBulkUploadDialog by remember { mutableStateOf(false) }

    val distinctTeams = remember(allPlayers) {
        allPlayers.map { it.teamName }.distinct()
    }
    val teamsFilterList = remember(distinctTeams) {
        listOf("ALL") + distinctTeams
    }

    val filteredCareerStats = remember(careerStats, selectedTeamFilter, selectedStatusFilter) {
        careerStats.filter { st ->
            val teamMatch = selectedTeamFilter == "ALL" || st.player.teamName.equals(selectedTeamFilter, ignoreCase = true)
            val statusMatch = selectedStatusFilter == "ALL" || st.player.squadStatus == selectedStatusFilter
            teamMatch && statusMatch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("players_stats_screen")
    ) {
        // Top Header
        Surface(
            color = StadiumGreenDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Player Profiles & Squad Manager",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${allPlayers.size} Saved Profiles • ${distinctTeams.size} Teams • Photos, Styles & Roles",
                            color = FalconGold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            editingPlayer = null
                            showAddPlayerDialog = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_player_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Profile", fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = { showBulkUploadDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bulk_upload_team_btn")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Squad", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        TabRow(selectedTabIndex = selectedSubTab) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Player Profiles", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_squad_roster")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Career Leaderboards", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_career_leaderboards")
            )
        }

        if (allPlayers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("players_empty_state_card"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "No Saved Player Profiles Yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Save individual player profiles with photo, age, batting/bowling style, playing role, and club details to reuse in future matches — or enter random players directly during Match Setup.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                editingPlayer = null
                                showAddPlayerDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Player Profile", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showBulkUploadDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bulk Paste / Upload Team Squad", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Team Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                teamsFilterList.forEach { t ->
                    FilterChip(
                        selected = selectedTeamFilter == t,
                        onClick = { selectedTeamFilter = t },
                        label = { Text(t, fontWeight = FontWeight.Bold) }
                    )
                }
                if (selectedTeamFilter != "ALL") {
                    TextButton(
                        onClick = {
                            val teamToDelete = selectedTeamFilter
                            selectedTeamFilter = "ALL"
                            onDeleteTeam(teamToDelete)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete Team", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Squad Status Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "All Profiles",
                    "PLAYING_XI" to "Playing XI",
                    "IMPACT_PLAYER" to "Impact Players",
                    "SUBSTITUTE" to "Substitutes"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = selectedStatusFilter == key,
                        onClick = { selectedStatusFilter = key },
                        label = { Text(label) }
                    )
                }
            }

            if (selectedSubTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCareerStats, key = { it.player.id }) { st ->
                        PlayerRosterCard(
                            stat = st,
                            onViewProfile = { viewingProfileStat = st },
                            onEdit = {
                                editingPlayer = st.player
                                showAddPlayerDialog = true
                            },
                            onDelete = { onDeletePlayer(st.player.id) }
                        )
                    }
                }
            } else {
                val topScorers = remember(filteredCareerStats) {
                    filteredCareerStats.sortedByDescending { it.totalRuns }
                }
                val topBowlers = remember(filteredCareerStats) {
                    filteredCareerStats.sortedWith(
                        compareByDescending<PlayerCareerStat> { it.wicketsTaken }
                            .thenBy { it.runsConceded }
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val orangeLeader = topScorers.firstOrNull()
                            val purpleLeader = topBowlers.firstOrNull()

                            ElevatedCard(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "🏏 TOP RUN SCORER",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = orangeLeader?.player?.name ?: "-",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "${orangeLeader?.totalRuns ?: 0} Runs (SR ${String.format(Locale.US, "%.1f", orangeLeader?.battingStrikeRate ?: 0.0)})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            ElevatedCard(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "⚾ TOP WICKET TAKER",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = purpleLeader?.player?.name ?: "-",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "${purpleLeader?.wicketsTaken ?: 0} Wkts (Best ${purpleLeader?.bestBowlingFormatted ?: "-"})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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
                                    text = "BATTING LEADERBOARD (ACTUAL MATCHES)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                topScorers.take(10).forEachIndexed { idx, s ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${idx + 1}. ${s.player.formattedNameWithRoles} (${s.player.teamName})",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Inn: ${s.inningsBatted} • HS: ${s.highestScore} • Avg: ${String.format(Locale.US, "%.1f", s.battingAverage)} • SR: ${String.format(Locale.US, "%.1f", s.battingStrikeRate)} • 4s/6s: ${s.fours}/${s.sixes}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "${s.totalRuns} R",
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

                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "BOWLING LEADERBOARD (ACTUAL MATCHES)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                topBowlers.take(10).forEachIndexed { idx, s ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${idx + 1}. ${s.player.formattedNameWithRoles} (${s.player.teamName})",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Ov: ${s.oversBowledFormatted} • Best: ${s.bestBowlingFormatted} • Econ: ${String.format(Locale.US, "%.2f", s.bowlingEconomy)} • Maidens: ${s.maidens}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "${s.wicketsTaken} W",
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
            }
        }
    }

    if (viewingProfileStat != null) {
        val st = viewingProfileStat!!
        PlayerProfileDetailDialog(
            stat = st,
            onDismiss = { viewingProfileStat = null },
            onEdit = {
                viewingProfileStat = null
                editingPlayer = st.player
                showAddPlayerDialog = true
            },
            onDelete = {
                onDeletePlayer(st.player.id)
                viewingProfileStat = null
            }
        )
    }

    if (showAddPlayerDialog) {
        AddOrEditPlayerDialog(
            initialPlayer = editingPlayer,
            defaultTeam = if (selectedTeamFilter != "ALL") selectedTeamFilter else distinctTeams.firstOrNull() ?: "",
            onDismiss = { showAddPlayerDialog = false },
            onConfirm = { player ->
                onSavePlayer(player)
                showAddPlayerDialog = false
            }
        )
    }

    if (showBulkUploadDialog) {
        BulkUploadSquadDialog(
            defaultTeam = if (selectedTeamFilter != "ALL") selectedTeamFilter else "",
            onDismiss = { showBulkUploadDialog = false },
            onUpload = { teamName, rawList ->
                onBulkUploadPlayers(teamName, rawList)
                showBulkUploadDialog = false
            }
        )
    }
}

@Composable
private fun PlayerRosterCard(
    stat: PlayerCareerStat,
    onViewProfile: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val p = stat.player
    val ageTag = if (p.age != null && p.age > 0) " • Age ${p.age}" else ""
    val clubTag = if (p.hometownOrClub.isNotBlank()) " • ${p.hometownOrClub}" else ""

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewProfile() }
            .testTag("player_card_${p.id}"),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerAvatarBadge(
                name = p.name,
                jerseyNumber = p.jerseyNumber,
                photoUri = p.photoUri,
                size = 50.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = p.formattedNameWithRoles + if (p.nickname.isNotBlank()) " \"${p.nickname}\"" else "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${p.teamName} • ${p.role.replace("_", " ")}$ageTag$clubTag",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${p.battingStyle} • ${p.bowlingStyle}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Career: ${stat.totalRuns} Runs (${stat.ballsFaced}b) • ${stat.wicketsTaken} Wkts • Best ${stat.bestBowlingFormatted}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier.testTag("edit_player_${p.id}")
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Player", modifier = Modifier.size(20.dp))
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_player_${p.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Player",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayerProfileDetailDialog(
    stat: PlayerCareerStat,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val p = stat.player
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayerAvatarBadge(
                    name = p.name,
                    jerseyNumber = p.jerseyNumber,
                    photoUri = p.photoUri,
                    size = 56.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = p.formattedNameWithRoles,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${p.teamName} • Jersey #${p.jerseyNumber}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "PERSONAL & CRICKET PROFILE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Playing Role: ${p.role.replace("_", " ")} (${p.squadStatus.replace("_", " ")})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Batting Style: ${p.battingStyle}", style = MaterialTheme.typography.bodyMedium)
                        Text("Bowling Style: ${p.bowlingStyle}", style = MaterialTheme.typography.bodyMedium)
                        if (p.age != null && p.age > 0) {
                            Text("Age: ${p.age} Years", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (p.nickname.isNotBlank()) {
                            Text("Nickname: ${p.nickname}", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (p.hometownOrClub.isNotBlank()) {
                            Text("Club / Hometown: ${p.hometownOrClub}", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (p.bioNotes.isNotBlank()) {
                            Text("Notes: ${p.bioNotes}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CAREER MATCH STATISTICS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Batting: ${stat.totalRuns} Runs (${stat.ballsFaced} balls) • HS: ${stat.highestScore} • Avg: ${String.format(Locale.US, "%.1f", stat.battingAverage)} • SR: ${String.format(Locale.US, "%.1f", stat.battingStrikeRate)} • 4s/6s: ${stat.fours}/${stat.sixes}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Bowling: ${stat.wicketsTaken} Wickets (${stat.oversBowledFormatted} Ov) • Best: ${stat.bestBowlingFormatted} • Econ: ${String.format(Locale.US, "%.2f", stat.bowlingEconomy)} • Maidens: ${stat.maidens}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
                Button(onClick = onEdit) {
                    Text("Edit Profile", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddOrEditPlayerDialog(
    initialPlayer: PlayerEntity?,
    defaultTeam: String,
    onDismiss: () -> Unit,
    onConfirm: (PlayerEntity) -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(initialPlayer?.name ?: "") }
    var teamName by remember { mutableStateOf(initialPlayer?.teamName ?: defaultTeam) }
    var role by remember { mutableStateOf(initialPlayer?.role ?: "ALL_ROUNDER") }
    var battingStyle by remember { mutableStateOf(initialPlayer?.battingStyle ?: "Right-Hand Bat") }
    var bowlingStyle by remember { mutableStateOf(initialPlayer?.bowlingStyle ?: "Right-Arm Medium") }
    var jerseyText by remember { mutableStateOf((initialPlayer?.jerseyNumber ?: 10).toString()) }
    var ageText by remember { mutableStateOf(initialPlayer?.age?.toString() ?: "") }
    var nickname by remember { mutableStateOf(initialPlayer?.nickname ?: "") }
    var hometownOrClub by remember { mutableStateOf(initialPlayer?.hometownOrClub ?: "") }
    var bioNotes by remember { mutableStateOf(initialPlayer?.bioNotes ?: "") }
    var photoUri by remember { mutableStateOf(initialPlayer?.photoUri ?: "") }

    var isCaptain by remember { mutableStateOf(initialPlayer?.isCaptain ?: false) }
    var isViceCaptain by remember { mutableStateOf(initialPlayer?.isViceCaptain ?: false) }
    var isWicketKeeper by remember { mutableStateOf(initialPlayer?.isWicketKeeper ?: false) }
    var squadStatus by remember { mutableStateOf(initialPlayer?.squadStatus ?: "PLAYING_XI") }

    // Zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            savePlayerPhotoLocally(context, uri)?.let { savedPath ->
                photoUri = savedPath
            }
        }
    }

    val bowlingStyles = listOf(
        "Right-Arm Fast",
        "Right-Arm Medium",
        "Right-Arm Off-Spin",
        "Right-Arm Leg-Spin",
        "Left-Arm Fast",
        "Left-Arm Orthodox",
        "Left-Arm Chinaman",
        "None"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialPlayer == null) "Create Player Profile" else "Edit Player Profile",
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Photo Picker Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlayerAvatarBadge(
                        name = name.ifBlank { "P" },
                        jerseyNumber = jerseyText.toIntOrNull() ?: 10,
                        photoUri = photoUri,
                        size = 56.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pick_player_photo_btn")
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (photoUri.isBlank()) "Select Photo (Optional)" else "Change Photo",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (photoUri.isNotBlank()) {
                            TextButton(onClick = { photoUri = "" }) {
                                Text("Remove Photo", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Player Full Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = teamName,
                        onValueChange = { teamName = it },
                        label = { Text("Team Name *") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("player_team_input")
                    )
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { ageText = it.filter { c -> c.isDigit() } },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.7f)
                            .testTag("player_age_input")
                    )
                    OutlinedTextField(
                        value = jerseyText,
                        onValueChange = { jerseyText = it.filter { c -> c.isDigit() } },
                        label = { Text("Jersey #") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("Nickname (Optional)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = hometownOrClub,
                        onValueChange = { hometownOrClub = it },
                        label = { Text("Club / Hometown") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Squad Status:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "PLAYING_XI" to "Playing XI",
                        "IMPACT_PLAYER" to "Impact Player",
                        "SUBSTITUTE" to "Substitute"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = squadStatus == key,
                            onClick = { squadStatus = key },
                            label = { Text(label) }
                        )
                    }
                }

                Text("Leadership & Wicketkeeper Designations:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = isCaptain,
                        onClick = {
                            isCaptain = !isCaptain
                            if (isCaptain) isViceCaptain = false
                        },
                        label = { Text("Captain (C)") }
                    )
                    FilterChip(
                        selected = isViceCaptain,
                        onClick = {
                            isViceCaptain = !isViceCaptain
                            if (isViceCaptain) isCaptain = false
                        },
                        label = { Text("Vice-Capt (VC)") }
                    )
                    FilterChip(
                        selected = isWicketKeeper,
                        onClick = {
                            isWicketKeeper = !isWicketKeeper
                            if (isWicketKeeper) role = "WICKET_KEEPER"
                        },
                        label = { Text("WK") }
                    )
                }

                Text("Playing Role:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("BATSMAN", "BOWLER", "ALL_ROUNDER", "WICKET_KEEPER").forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = {
                                role = r
                                if (r == "WICKET_KEEPER") isWicketKeeper = true
                            },
                            label = { Text(r.replace("_", " ")) }
                        )
                    }
                }

                Text("Batting Style:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Right-Hand Bat", "Left-Hand Bat").forEach { bs ->
                        FilterChip(
                            selected = battingStyle == bs,
                            onClick = { battingStyle = bs },
                            label = { Text(bs) }
                        )
                    }
                }

                Text("Bowling Style:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    bowlingStyles.forEach { bStyle ->
                        FilterChip(
                            selected = bowlingStyle == bStyle,
                            onClick = { bowlingStyle = bStyle },
                            label = { Text(bStyle) }
                        )
                    }
                }

                OutlinedTextField(
                    value = bioNotes,
                    onValueChange = { bioNotes = it },
                    label = { Text("Cricket Notes / Strengths (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && teamName.isNotBlank()) {
                        onConfirm(
                            PlayerEntity(
                                id = initialPlayer?.id ?: 0L,
                                name = name.trim(),
                                teamName = teamName.trim(),
                                role = role,
                                battingStyle = battingStyle,
                                bowlingStyle = bowlingStyle,
                                jerseyNumber = jerseyText.toIntOrNull() ?: 10,
                                isCaptain = isCaptain,
                                isViceCaptain = isViceCaptain,
                                isWicketKeeper = isWicketKeeper,
                                squadStatus = squadStatus,
                                photoUri = photoUri,
                                age = ageText.toIntOrNull(),
                                nickname = nickname.trim(),
                                hometownOrClub = hometownOrClub.trim(),
                                bioNotes = bioNotes.trim()
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && teamName.isNotBlank(),
                modifier = Modifier.testTag("save_player_confirm_btn")
            ) {
                Text("Save Profile", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun BulkUploadSquadDialog(
    defaultTeam: String,
    onDismiss: () -> Unit,
    onUpload: (teamName: String, rawText: String) -> Unit
) {
    val context = LocalContext.current
    var teamName by remember { mutableStateOf(defaultTeam) }
    var rawText by remember { mutableStateOf("") }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()?.let { content ->
                rawText = content
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Import Custom Team Squad", fontWeight = FontWeight.Black)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Custom Team Name (Required)") },
                    placeholder = { Text("Enter any custom team name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bulk_upload_team_name_input")
                )

                OutlinedButton(
                    onClick = { fileLauncher.launch("text/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select .CSV / .TXT Roster File", fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Or paste player names (one per line, or 'Name, Role, BatStyle, BowlStyle, Jersey#'):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("Custom Player List (1 per line)") },
                    placeholder = {
                        Text("Player 1, BATSMAN, Right-Hand Bat, Right-Arm Medium, 7\nPlayer 2, WICKET_KEEPER")
                    },
                    minLines = 5,
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bulk_upload_players_text_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (teamName.isNotBlank() && rawText.isNotBlank()) {
                        onUpload(teamName.trim(), rawText)
                    }
                },
                enabled = teamName.isNotBlank() && rawText.isNotBlank(),
                modifier = Modifier.testTag("confirm_bulk_upload_btn")
            ) {
                Text("Save Squad", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
