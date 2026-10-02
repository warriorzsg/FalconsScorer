package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.InningsEntity
import com.example.data.PlayerEntity
import com.example.data.TournamentEntity
import com.example.data.TournamentFixtureEntity
import com.example.ui.ActiveTournamentDetailState
import com.example.ui.theme.CricketCrimson
import com.example.ui.theme.FalconGold
import com.example.ui.theme.StadiumGreenDark
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TournamentScreen(
    allTournaments: List<TournamentEntity>,
    activeState: ActiveTournamentDetailState,
    allPlayers: List<PlayerEntity>,
    allInnings: List<InningsEntity>,
    onSelectTournament: (Long) -> Unit,
    onCreateTournament: (
        name: String,
        organizer: String,
        venue: String,
        format: String,
        oversLimit: Int,
        maxOversPerBowler: Int,
        playersPerTeam: Int,
        pointsForWin: Int,
        pointsForTie: Int,
        teamNames: List<String>,
        autoGenerateFixtures: Boolean
    ) -> Unit,
    onRegisterTeam: (Long, String, String) -> Unit,
    onRemoveTeam: (Long) -> Unit,
    onGenerateRoundRobin: (Long) -> Unit,
    onAddCustomFixture: (Long, String, String, String, String, String) -> Unit,
    onGenerateKnockouts: (Long, List<String>, String) -> Unit,
    onDeleteFixture: (Long) -> Unit,
    onDeleteTournament: (Long) -> Unit,
    onStartOrOpenFixtureMatch: (TournamentEntity, TournamentFixtureEntity) -> Unit,
    onSavePlayer: (PlayerEntity) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Fixtures & Knockouts, 1 = Points Table, 2 = Teams & Squads, 3 = Stats
    var showCreateTournamentDialog by remember { mutableStateOf(false) }
    var showAddFixtureDialog by remember { mutableStateOf(false) }
    var showRegisterTeamDialog by remember { mutableStateOf(false) }
    var quickAddPlayerForTeam by remember { mutableStateOf<String?>(null) }

    val activeTourney = activeState.tournament
    val inningsByMatch = remember(allInnings) { allInnings.groupBy { it.matchId } }
    val existingTeamNames = remember(allPlayers) { allPlayers.map { it.teamName }.distinct() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tournament_screen")
    ) {
        // Header Bar
        Surface(
            color = StadiumGreenDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = FalconGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = activeTourney?.name ?: "Tournament Manager",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (activeTourney != null) {
                                    "${activeTourney.format} (${activeTourney.oversLimit} Ov) • ${activeState.registeredTeams.size} Teams • ${activeState.fixtures.size} Fixtures"
                                } else {
                                    "Create & Manage Custom Leagues, Knockouts & Stats"
                                },
                                color = FalconGold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Button(
                        onClick = { showCreateTournamentDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("create_tournament_btn")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Cup", fontWeight = FontWeight.Bold)
                    }
                }

                // Switch between tournaments if multiple exist
                if (allTournaments.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allTournaments.forEach { t ->
                            FilterChip(
                                selected = t.id == activeTourney?.id,
                                onClick = { onSelectTournament(t.id) },
                                label = {
                                    Text(
                                        text = "${t.name} (${t.format})",
                                        color = if (t.id == activeTourney?.id) MaterialTheme.colorScheme.onSecondaryContainer else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        if (activeTourney == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "No Tournaments Created Yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Create a custom tournament with any team names, generate round-robin league fixtures & knockout brackets, track points tables & Net Run Rate (NRR), and score matches live.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { showCreateTournamentDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("empty_create_tournament_btn")
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create New Tournament", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Champion Banner if completed
            if (activeTourney.championTeam.isNotBlank()) {
                Surface(
                    color = FalconGold,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏆 CHAMPION: ${activeTourney.championTeam.uppercase()}",
                            color = Color(0xFF1E1300),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        if (activeTourney.runnerUpTeam.isNotBlank()) {
                            Text(
                                text = "Runner-Up: ${activeTourney.runnerUpTeam}",
                                color = Color(0xFF1E1300),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            TabRow(selectedTabIndex = selectedSubTab) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = { Text("Fixtures", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tourney_tab_fixtures")
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = { Text("Standings", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tourney_tab_standings")
                )
                Tab(
                    selected = selectedSubTab == 2,
                    onClick = { selectedSubTab = 2 },
                    text = { Text("Teams", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tourney_tab_teams")
                )
                Tab(
                    selected = selectedSubTab == 3,
                    onClick = { selectedSubTab = 3 },
                    text = { Text("Stats", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tourney_tab_stats")
                )
            }

            when (selectedSubTab) {
                0 -> TournamentFixturesContent(
                    tournament = activeTourney,
                    state = activeState,
                    inningsByMatch = inningsByMatch,
                    onAddCustomFixture = { showAddFixtureDialog = true },
                    onGenerateRoundRobin = { onGenerateRoundRobin(activeTourney.id) },
                    onGenerateKnockouts = { knockoutType ->
                        val ranked = activeState.overviewStats?.standings?.map { it.teamName }
                            ?: activeState.registeredTeams.map { it.teamName }
                        onGenerateKnockouts(activeTourney.id, ranked, knockoutType)
                    },
                    onStartOrOpenFixture = { fixture ->
                        onStartOrOpenFixtureMatch(activeTourney, fixture)
                    },
                    onDeleteFixture = onDeleteFixture,
                    onDeleteTournament = { onDeleteTournament(activeTourney.id) }
                )

                1 -> TournamentStandingsContent(
                    tournament = activeTourney,
                    state = activeState
                )

                2 -> TournamentTeamsAndSquadsContent(
                    tournament = activeTourney,
                    state = activeState,
                    allPlayers = allPlayers,
                    onRegisterTeamClick = { showRegisterTeamDialog = true },
                    onRemoveTeam = onRemoveTeam,
                    onAddPlayerToTeam = { teamName -> quickAddPlayerForTeam = teamName }
                )

                3 -> TournamentStatsContent(
                    tournament = activeTourney,
                    state = activeState
                )
            }
        }
    }

    if (showCreateTournamentDialog) {
        CreateTournamentDialog(
            existingTeams = existingTeamNames,
            onDismiss = { showCreateTournamentDialog = false },
            onConfirm = { name, org, venue, fmt, overs, maxBowler, playersPerTeam, ptsWin, ptsTie, teams, autoGen ->
                onCreateTournament(
                    name, org, venue, fmt, overs, maxBowler, playersPerTeam, ptsWin, ptsTie, teams, autoGen
                )
                showCreateTournamentDialog = false
            }
        )
    }

    if (showAddFixtureDialog && activeTourney != null) {
        val teamNames = activeState.registeredTeams.map { it.teamName }
        AddCustomFixtureDialog(
            registeredTeams = teamNames,
            defaultVenue = activeTourney.venue,
            onDismiss = { showAddFixtureDialog = false },
            onConfirm = { stage, teamA, teamB, dateStr, venueStr ->
                onAddCustomFixture(activeTourney.id, stage, teamA, teamB, dateStr, venueStr)
                showAddFixtureDialog = false
            }
        )
    }

    if (showRegisterTeamDialog && activeTourney != null) {
        RegisterTournamentTeamDialog(
            existingTeams = existingTeamNames,
            onDismiss = { showRegisterTeamDialog = false },
            onConfirm = { teamName, groupName ->
                onRegisterTeam(activeTourney.id, teamName, groupName)
                showRegisterTeamDialog = false
            }
        )
    }

    if (quickAddPlayerForTeam != null) {
        QuickAddSquadPlayerDialog(
            teamName = quickAddPlayerForTeam!!,
            onDismiss = { quickAddPlayerForTeam = null },
            onSave = { player ->
                onSavePlayer(player)
                quickAddPlayerForTeam = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TournamentFixturesContent(
    tournament: TournamentEntity,
    state: ActiveTournamentDetailState,
    inningsByMatch: Map<Long, List<InningsEntity>>,
    onAddCustomFixture: () -> Unit,
    onGenerateRoundRobin: () -> Unit,
    onGenerateKnockouts: (String) -> Unit,
    onStartOrOpenFixture: (TournamentFixtureEntity) -> Unit,
    onDeleteFixture: (Long) -> Unit,
    onDeleteTournament: () -> Unit
) {
    val teamCount = state.registeredTeams.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Scheduling Controls Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Schedule & Knockout Stage Generator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onAddCustomFixture,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_fixture_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Fixture", fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = onGenerateRoundRobin,
                            enabled = teamCount >= 2,
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("auto_round_robin_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Round-Robin", fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onGenerateKnockouts("SEMI_FINALS") },
                            enabled = teamCount >= 4,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Gen Semi-Finals (Top 4)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onGenerateKnockouts("FINAL") },
                            enabled = teamCount >= 2,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Gen Grand Final", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (state.fixtures.isEmpty()) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No Fixtures Scheduled Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (teamCount < 2) {
                                "Register at least 2 teams in the 'Teams' tab, then tap 'Round-Robin' or 'Add Fixture'."
                            } else {
                                "Tap 'Round-Robin' to auto-schedule league matches or 'Add Fixture' for custom matches."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(state.fixtures, key = { it.id }) { fixture ->
                val linkedInnings = fixture.linkedMatchId?.let { inningsByMatch[it] }.orEmpty()
                TournamentFixtureCard(
                    fixture = fixture,
                    linkedInnings = linkedInnings,
                    onActionClick = { onStartOrOpenFixture(fixture) },
                    onDeleteClick = { onDeleteFixture(fixture.id) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDeleteTournament,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Tournament")
                }
            }
        }
    }
}

@Composable
private fun TournamentFixtureCard(
    fixture: TournamentFixtureEntity,
    linkedInnings: List<InningsEntity>,
    onActionClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isLive = fixture.status == "LIVE"
    val isCompleted = fixture.status == "COMPLETED"

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fixture_card_${fixture.id}"),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = when {
                            isLive -> CricketCrimson
                            fixture.stage == "FINAL" -> FalconGold
                            fixture.stage != "LEAGUE" -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primaryContainer
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "M#${fixture.matchNumber} • ${fixture.stage.replace("_", " ")}",
                            color = when {
                                isLive -> Color.White
                                fixture.stage == "FINAL" -> Color(0xFF1E1300)
                                fixture.stage != "LEAGUE" -> Color.White
                                else -> MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = fixture.scheduledDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Fixture",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${fixture.teamA} vs ${fixture.teamB}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )

            if (linkedInnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                linkedInnings.forEach { inn ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = inn.battingTeam,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${inn.totalRuns}/${inn.wickets} (${inn.oversFormatted} Ov)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (fixture.resultSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "🏆 ${fixture.resultSummary}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onActionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fixture_action_btn_${fixture.id}"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isLive -> CricketCrimson
                        isCompleted -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.Leaderboard else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        isLive -> "Resume Live Scoring"
                        isCompleted -> "View Match Scorecard"
                        else -> "Start & Score Match"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TournamentStandingsContent(
    tournament: TournamentEntity,
    state: ActiveTournamentDetailState
) {
    val standings = state.overviewStats?.standings.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "POINTS TABLE & NET RUN RATE (NRR)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Win = ${tournament.pointsForWin} pts • Tie/NR = ${tournament.pointsForTie} pt • Official ICC NRR Tie-Breaker",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, modifier = Modifier.width(22.dp))
                        Text("TEAM", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, modifier = Modifier.weight(1.8f))
                        listOf("P", "W", "L", "T", "PTS").forEach { col ->
                            Text(
                                text = col,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(0.42f)
                            )
                        }
                        Text(
                            text = "NRR",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.85f)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    if (standings.isEmpty()) {
                        Text(
                            text = "Register teams in the 'Teams' tab to view the standings table.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        standings.forEachIndexed { idx, row ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Black,
                                        color = if (idx < 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.width(22.dp)
                                    )
                                    Text(
                                        text = row.teamName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1.8f)
                                    )
                                    Text("${row.played}", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.42f))
                                    Text("${row.won}", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.42f))
                                    Text("${row.lost}", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.42f))
                                    Text("${row.tied}", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.42f))
                                    Text(
                                        text = "${row.points}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(0.42f)
                                    )
                                    Text(
                                        text = row.nrrFormatted,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(0.85f)
                                    )
                                }
                                Text(
                                    text = "For: ${row.runsFor} runs (${row.ballsFor / 6}.${row.ballsFor % 6} ov) • Against: ${row.runsAgainst} runs (${row.ballsAgainst / 6}.${row.ballsAgainst % 6} ov)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 22.dp)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TournamentTeamsAndSquadsContent(
    tournament: TournamentEntity,
    state: ActiveTournamentDetailState,
    allPlayers: List<PlayerEntity>,
    onRegisterTeamClick: () -> Unit,
    onRemoveTeam: (Long) -> Unit,
    onAddPlayerToTeam: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Registered Tournament Teams (${state.registeredTeams.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Add custom teams & manage their Playing XI, Substitutes & Impact Players",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onRegisterTeamClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("register_team_btn")
                ) {
                    Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Team", fontWeight = FontWeight.Bold)
                }
            }
        }

        items(state.registeredTeams, key = { it.id }) { regTeam ->
            val teamPlayers = allPlayers.filter { it.teamName.equals(regTeam.teamName, ignoreCase = true) }
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = regTeam.teamName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${regTeam.groupName} • ${teamPlayers.size} Squad Players",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row {
                            FilledTonalButton(
                                onClick = { onAddPlayerToTeam(regTeam.teamName) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Player", style = MaterialTheme.typography.labelMedium)
                            }
                            IconButton(onClick = { onRemoveTeam(regTeam.id) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Remove Team",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (teamPlayers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            teamPlayers.forEach { p ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = p.formattedNameWithRoles,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (p.isCaptain || p.isWicketKeeper) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No players added yet — tap '+ Player' or add players on the fly during match setup.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentStatsContent(
    tournament: TournamentEntity,
    state: ActiveTournamentDetailState
) {
    val stats = state.overviewStats

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Overview Counters
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "${tournament.name.uppercase()} • TOURNAMENT SUMMARY",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Runs", style = MaterialTheme.typography.labelMedium)
                            Text("${stats?.totalRuns ?: 0}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        }
                        Column {
                            Text("Wickets", style = MaterialTheme.typography.labelMedium)
                            Text("${stats?.totalWickets ?: 0}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        }
                        Column {
                            Text("Sixes (6s)", style = MaterialTheme.typography.labelMedium)
                            Text("${stats?.totalSixes ?: 0}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        }
                        Column {
                            Text("Fours (4s)", style = MaterialTheme.typography.labelMedium)
                            Text("${stats?.totalFours ?: 0}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Highest Team Total: ${stats?.highestTeamScore ?: "-"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Top Run Scorers
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🏏 MOST RUNS IN TOURNAMENT (ORANGE CAP)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val scorers = stats?.topRunScorers.orEmpty()
                    if (scorers.isEmpty()) {
                        Text("Score tournament matches to populate batting statistics.")
                    } else {
                        scorers.take(8).forEachIndexed { idx, s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
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
                                        text = "Inn: ${s.inningsBatted} • HS: ${s.highestScore} • SR: ${String.format(Locale.US, "%.1f", s.battingStrikeRate)} • 4s/6s: ${s.fours}/${s.sixes}",
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
        }

        // Top Wicket Takers
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⚾ MOST WICKETS IN TOURNAMENT (PURPLE CAP)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val bowlers = stats?.topWicketTakers.orEmpty()
                    if (bowlers.isEmpty()) {
                        Text("Score tournament matches to populate bowling statistics.")
                    } else {
                        bowlers.take(8).forEachIndexed { idx, b ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${idx + 1}. ${b.player.formattedNameWithRoles} (${b.player.teamName})",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Ov: ${b.oversBowledFormatted} • Best: ${b.bestBowlingFormatted} • Econ: ${String.format(Locale.US, "%.2f", b.bowlingEconomy)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${b.wicketsTaken} W",
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

        // Tournament MVP Leaderboard
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⭐ PLAYER OF THE TOURNAMENT (MVP RACE)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val mvps = stats?.mvpLeaderboard.orEmpty()
                    if (mvps.isEmpty()) {
                        Text("No MVP impact points recorded yet.")
                    } else {
                        mvps.take(8).forEachIndexed { idx, m ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${idx + 1}. ${m.playerName} (${m.teamName})",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = m.breakdown,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${m.impactPoints} pts",
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateTournamentDialog(
    existingTeams: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        organizer: String,
        venue: String,
        format: String,
        oversLimit: Int,
        maxOversPerBowler: Int,
        playersPerTeam: Int,
        pointsForWin: Int,
        pointsForTie: Int,
        teamNames: List<String>,
        autoGenerateFixtures: Boolean
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var organizer by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("Main Cricket Ground") }
    var format by remember { mutableStateOf("T20") }
    var oversText by remember { mutableStateOf("20") }
    var maxBowlerText by remember { mutableStateOf("4") }
    var customTeamsText by remember { mutableStateOf(existingTeams.joinToString("\n")) }
    var autoGenFixtures by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Tournament", fontWeight = FontWeight.Black) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tournament / Cup Name") },
                    placeholder = { Text("e.g., Falcons Premier League") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tourney_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = organizer,
                        onValueChange = { organizer = it },
                        label = { Text("Organizer") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = venue,
                        onValueChange = { venue = it },
                        label = { Text("Venue") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Match Format:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("T20" to 20, "ODI" to 50, "TEST" to 90, "CUSTOM" to 10).forEach { (fmt, ov) ->
                        FilterChip(
                            selected = format == fmt,
                            onClick = {
                                format = fmt
                                oversText = ov.toString()
                                maxBowlerText = when (fmt) {
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = oversText,
                        onValueChange = { oversText = it.filter { c -> c.isDigit() } },
                        label = { Text("Overs") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxBowlerText,
                        onValueChange = { maxBowlerText = it.filter { c -> c.isDigit() } },
                        label = { Text("Max Ov/Bowler") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = customTeamsText,
                    onValueChange = { customTeamsText = it },
                    label = { Text("Register Custom Teams (1 per line or comma-separated)") },
                    placeholder = { Text("Gorkha XI\nHimalayan Royals\nKathmandu Strikers\nPokhara Falcons") },
                    minLines = 4,
                    maxLines = 7,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tourney_teams_input")
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = autoGenFixtures,
                        onCheckedChange = { autoGenFixtures = it }
                    )
                    Text(
                        text = "Auto-generate Round-Robin League Fixtures",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val teams = customTeamsText.split("\n", ",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    onConfirm(
                        name.ifBlank { "Falcons Cup" },
                        organizer,
                        venue,
                        format,
                        (oversText.toIntOrNull() ?: 20).coerceAtLeast(1),
                        (maxBowlerText.toIntOrNull() ?: 4).coerceAtLeast(1),
                        11,
                        2,
                        1,
                        teams,
                        autoGenFixtures
                    )
                },
                modifier = Modifier.testTag("confirm_create_tourney_btn")
            ) {
                Text("Create Tournament", fontWeight = FontWeight.Bold)
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
private fun AddCustomFixtureDialog(
    registeredTeams: List<String>,
    defaultVenue: String,
    onDismiss: () -> Unit,
    onConfirm: (stage: String, teamA: String, teamB: String, dateStr: String, venueStr: String) -> Unit
) {
    var stage by remember { mutableStateOf("LEAGUE") }
    var teamA by remember { mutableStateOf(registeredTeams.getOrElse(0) { "" }) }
    var teamB by remember { mutableStateOf(registeredTeams.getOrElse(1) { "" }) }
    var dateStr by remember { mutableStateOf("Upcoming") }
    var venueStr by remember { mutableStateOf(defaultVenue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Custom Fixture", fontWeight = FontWeight.Black) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Match Stage:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("LEAGUE", "QUARTER_FINAL", "SEMI_FINAL", "ELIMINATOR", "QUALIFIER_1", "QUALIFIER_2", "FINAL").forEach { st ->
                        FilterChip(
                            selected = stage == st,
                            onClick = { stage = st },
                            label = { Text(st.replace("_", " ")) }
                        )
                    }
                }

                if (registeredTeams.isNotEmpty()) {
                    Text("Pick Team A:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        registeredTeams.forEach { t ->
                            FilterChip(
                                selected = teamA == t,
                                onClick = { teamA = t },
                                label = { Text(t) }
                            )
                        }
                    }
                    Text("Pick Team B:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        registeredTeams.forEach { t ->
                            FilterChip(
                                selected = teamB == t,
                                onClick = { teamB = t },
                                label = { Text(t) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = teamA,
                    onValueChange = { teamA = it },
                    label = { Text("Team A Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = teamB,
                    onValueChange = { teamB = it },
                    label = { Text("Team B Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("Date / Time Slot") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (teamA.isNotBlank() && teamB.isNotBlank()) {
                        onConfirm(stage, teamA, teamB, dateStr, venueStr)
                    }
                },
                enabled = teamA.isNotBlank() && teamB.isNotBlank() && !teamA.trim().equals(teamB.trim(), true)
            ) {
                Text("Schedule Match", fontWeight = FontWeight.Bold)
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
private fun RegisterTournamentTeamDialog(
    existingTeams: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (teamName: String, groupName: String) -> Unit
) {
    var teamName by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("Group A") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Team in Tournament", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (existingTeams.isNotEmpty()) {
                    Text("Or pick an existing team:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        existingTeams.forEach { t ->
                            FilterChip(
                                selected = teamName == t,
                                onClick = { teamName = t },
                                label = { Text(t) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Custom Team Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Group A", "Group B", "Pool 1", "Pool 2").forEach { g ->
                        FilterChip(
                            selected = groupName == g,
                            onClick = { groupName = g },
                            label = { Text(g) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (teamName.isNotBlank()) {
                        onConfirm(teamName.trim(), groupName)
                    }
                },
                enabled = teamName.isNotBlank()
            ) {
                Text("Register Team", fontWeight = FontWeight.Bold)
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
private fun QuickAddSquadPlayerDialog(
    teamName: String,
    onDismiss: () -> Unit,
    onSave: (PlayerEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("ALL_ROUNDER") }
    var squadStatus by remember { mutableStateOf("PLAYING_XI") }
    var isCaptain by remember { mutableStateOf(false) }
    var isViceCaptain by remember { mutableStateOf(false) }
    var isWicketKeeper by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Player to $teamName", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Player Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Squad Status:", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("PLAYING_XI" to "Playing XI", "IMPACT_PLAYER" to "Impact Player", "SUBSTITUTE" to "Substitute").forEach { (k, label) ->
                        FilterChip(
                            selected = squadStatus == k,
                            onClick = { squadStatus = k },
                            label = { Text(label) }
                        )
                    }
                }
                Text("Designations:", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        label = { Text("Vice-Captain (VC)") }
                    )
                    FilterChip(
                        selected = isWicketKeeper,
                        onClick = {
                            isWicketKeeper = !isWicketKeeper
                            if (isWicketKeeper) role = "WICKET_KEEPER"
                        },
                        label = { Text("Wicketkeeper (WK)") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            PlayerEntity(
                                name = name.trim(),
                                teamName = teamName,
                                role = role,
                                isCaptain = isCaptain,
                                isViceCaptain = isViceCaptain,
                                isWicketKeeper = isWicketKeeper,
                                squadStatus = squadStatus
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add Player", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
