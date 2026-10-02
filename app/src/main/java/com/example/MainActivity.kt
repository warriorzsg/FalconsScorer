package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppTab
import com.example.ui.CricketViewModel
import com.example.ui.screens.AboutCreditsScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.LiveScoringScreen
import com.example.ui.screens.NewMatchSetupScreen
import com.example.ui.screens.PlayersAndStatsScreen
import com.example.ui.screens.ScorecardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TournamentScreen
import com.example.ui.theme.FalconGold
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StadiumGreenDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val cricketViewModel: CricketViewModel = viewModel()
            val settings by cricketViewModel.settingsState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (settings.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemDark
            }

            MyApplicationTheme(darkTheme = useDarkTheme) {
                FalconsScorerApp(viewModel = cricketViewModel)
            }
        }
    }
}

private data class NavDestination(
    val tab: AppTab,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

// Clean, minimal main navigation with only the 4 essential sections
private val mainNavDestinations = listOf(
    NavDestination(AppTab.HOME, "Home", Icons.Default.Home, "nav_tab_home"),
    NavDestination(AppTab.LIVE_SCORER, "Match/Score", Icons.Default.SportsCricket, "nav_tab_scorer"),
    NavDestination(AppTab.TOURNAMENTS, "Tournaments", Icons.Default.EmojiEvents, "nav_tab_tournaments"),
    NavDestination(AppTab.SETTINGS, "Settings", Icons.Default.Settings, "nav_tab_settings")
)

private val matchSubTabs = listOf(
    NavDestination(AppTab.NEW_MATCH, "New Match", Icons.Default.AddCircle, "nav_tab_new_match"),
    NavDestination(AppTab.LIVE_SCORER, "Live Score", Icons.Default.SportsCricket, "sub_tab_scorer"),
    NavDestination(AppTab.SCORECARD, "Scorecard", Icons.Default.Assessment, "nav_tab_scorecard"),
    NavDestination(AppTab.PLAYERS, "Players", Icons.Default.Groups, "nav_tab_players")
)

private fun isMainDestinationSelected(destTab: AppTab, currentTab: AppTab): Boolean {
    return when (destTab) {
        AppTab.HOME -> currentTab == AppTab.HOME
        AppTab.LIVE_SCORER -> currentTab in setOf(
            AppTab.NEW_MATCH,
            AppTab.LIVE_SCORER,
            AppTab.SCORECARD,
            AppTab.PLAYERS
        )
        AppTab.TOURNAMENTS -> currentTab == AppTab.TOURNAMENTS
        AppTab.SETTINGS -> currentTab in setOf(AppTab.SETTINGS, AppTab.ABOUT)
        else -> currentTab == destTab
    }
}

@Composable
fun FalconsScorerApp(viewModel: CricketViewModel = viewModel()) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val allMatches by viewModel.allMatches.collectAsStateWithLifecycle()
    val allInnings by viewModel.allInnings.collectAsStateWithLifecycle()
    val allPlayers by viewModel.allPlayers.collectAsStateWithLifecycle()
    val allTournaments by viewModel.allTournaments.collectAsStateWithLifecycle()
    val careerStats by viewModel.careerStats.collectAsStateWithLifecycle()
    val activeMatchState by viewModel.activeMatchState.collectAsStateWithLifecycle()
    val activeTournamentState by viewModel.activeTournamentState.collectAsStateWithLifecycle()
    val pendingFixture by viewModel.pendingFixtureSetup.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    val distinctTeamsCount = remember(allPlayers) {
        allPlayers.map { it.teamName }.distinct().size
    }

    val isMatchWorkspace = currentTab in setOf(
        AppTab.NEW_MATCH,
        AppTab.LIVE_SCORER,
        AppTab.SCORECARD,
        AppTab.PLAYERS
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar {
                        mainNavDestinations.forEach { dest ->
                            val selected = isMainDestinationSelected(dest.tab, currentTab)
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (dest.tab == AppTab.LIVE_SCORER && activeMatchState.match == null && allMatches.isEmpty()) {
                                        viewModel.navigateToTab(AppTab.NEW_MATCH)
                                    } else {
                                        viewModel.navigateToTab(dest.tab)
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail {
                        mainNavDestinations.forEach { dest ->
                            val selected = isMainDestinationSelected(dest.tab, currentTab)
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (dest.tab == AppTab.LIVE_SCORER && activeMatchState.match == null && allMatches.isEmpty()) {
                                        viewModel.navigateToTab(AppTab.NEW_MATCH)
                                    } else {
                                        viewModel.navigateToTab(dest.tab)
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Clean Match/Score workspace switcher when inside Match/Score section
                    if (isMatchWorkspace) {
                        Surface(
                            color = StadiumGreenDark,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                matchSubTabs.forEach { sub ->
                                    val subSelected = currentTab == sub.tab
                                    FilterChip(
                                        selected = subSelected,
                                        onClick = { viewModel.navigateToTab(sub.tab) },
                                        label = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = sub.icon,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(15.dp),
                                                    tint = if (subSelected) Color(0xFF1E1300) else Color.White
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = sub.label,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (subSelected) Color(0xFF1E1300) else Color.White
                                                )
                                            }
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = FalconGold,
                                            containerColor = Color.White.copy(alpha = 0.12f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag(sub.testTag)
                                    )
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentTab) {
                            AppTab.HOME -> HomeDashboardScreen(
                                matches = allMatches,
                                allInnings = allInnings,
                                totalPlayersCount = allPlayers.size,
                                totalTeamsCount = distinctTeamsCount,
                                statusMessage = statusBanner,
                                onClearStatusMessage = { viewModel.clearStatusBanner() },
                                onNavigateToTab = { viewModel.navigateToTab(it) },
                                onOpenMatchScorer = { matchId ->
                                    viewModel.selectMatchAndOpen(matchId, AppTab.LIVE_SCORER)
                                },
                                onOpenMatchScorecard = { matchId ->
                                    viewModel.selectMatchAndOpen(matchId, AppTab.SCORECARD)
                                },
                                onDeleteMatch = { matchId ->
                                    viewModel.deleteMatch(matchId)
                                }
                            )

                            AppTab.TOURNAMENTS -> TournamentScreen(
                                allTournaments = allTournaments,
                                activeState = activeTournamentState,
                                allPlayers = allPlayers,
                                allInnings = allInnings,
                                onSelectTournament = { viewModel.selectTournament(it) },
                                onCreateTournament = { name, org, venue, fmt, overs, maxBowler, playersPerTeam, ptsWin, ptsTie, teams, autoGen ->
                                    viewModel.createTournament(
                                        name = name,
                                        organizer = org,
                                        venue = venue,
                                        format = fmt,
                                        oversLimit = overs,
                                        maxOversPerBowler = maxBowler,
                                        playersPerTeam = playersPerTeam,
                                        pointsForWin = ptsWin,
                                        pointsForTie = ptsTie,
                                        teamNames = teams,
                                        autoGenerateFixtures = autoGen
                                    )
                                },
                                onRegisterTeam = { tourneyId, teamName, groupName ->
                                    viewModel.registerTeamInTournament(tourneyId, teamName, groupName)
                                },
                                onRemoveTeam = { viewModel.removeTeamFromTournament(it) },
                                onGenerateRoundRobin = { viewModel.generateRoundRobinFixtures(it) },
                                onAddCustomFixture = { tourneyId, stage, teamA, teamB, dateStr, venueStr ->
                                    viewModel.addCustomFixture(tourneyId, stage, teamA, teamB, dateStr, venueStr)
                                },
                                onGenerateKnockouts = { tourneyId, rankedTeams, knockoutType ->
                                    viewModel.generateKnockoutStage(tourneyId, rankedTeams, knockoutType)
                                },
                                onDeleteFixture = { viewModel.deleteFixture(it) },
                                onDeleteTournament = { viewModel.deleteTournament(it) },
                                onStartOrOpenFixtureMatch = { tourney, fixture ->
                                    viewModel.startFixtureMatchSetup(tourney, fixture)
                                },
                                onSavePlayer = { viewModel.savePlayer(it) },
                                onBack = { viewModel.navigateToTab(AppTab.HOME) }
                            )

                            AppTab.NEW_MATCH -> NewMatchSetupScreen(
                                allPlayers = allPlayers,
                                allTournaments = allTournaments,
                                pendingFixture = pendingFixture,
                                settings = settings,
                                onBack = { viewModel.navigateToTab(AppTab.HOME) },
                                onSavePlayer = { viewModel.savePlayer(it) },
                                onDeletePlayer = { viewModel.deletePlayer(it) },
                                onCreateMatch = { teamA, teamB, format, overs, maxBowler, playersPerTeam, tossWinner, tossDecision, venue, strikerId, nonStrikerId, bowlerId, allowConsecutive, tournamentId, fixtureId, stage, customStriker, customNonStriker, customBowler ->
                                    viewModel.createCustomMatch(
                                        teamA = teamA,
                                        teamB = teamB,
                                        format = format,
                                        oversLimit = overs,
                                        maxOversPerBowler = maxBowler,
                                        playersPerTeam = playersPerTeam,
                                        tossWinner = tossWinner,
                                        tossDecision = tossDecision,
                                        venue = venue,
                                        openingStrikerId = strikerId,
                                        openingNonStrikerId = nonStrikerId,
                                        openingBowlerId = bowlerId,
                                        allowConsecutiveOvers = allowConsecutive,
                                        tournamentId = tournamentId,
                                        fixtureId = fixtureId,
                                        stage = stage,
                                        customStrikerName = customStriker,
                                        customNonStrikerName = customNonStriker,
                                        customBowlerName = customBowler
                                    )
                                }
                            )

                            AppTab.LIVE_SCORER -> LiveScoringScreen(
                                state = activeMatchState,
                                onBack = { viewModel.navigateToTab(AppTab.HOME) },
                                onOpenScorecard = { viewModel.navigateToTab(AppTab.SCORECARD) },
                                onStartNewMatch = { viewModel.navigateToTab(AppTab.NEW_MATCH) },
                                onRecordDelivery = { runsOffBat, extraType, extraRuns, isWicket, wicketType, dismissedId, fielder, nextBatterId ->
                                    viewModel.recordDelivery(
                                        runsOffBat = runsOffBat,
                                        extraType = extraType,
                                        extraRuns = extraRuns,
                                        isWicket = isWicket,
                                        wicketType = wicketType,
                                        dismissedPlayerId = dismissedId,
                                        fielderName = fielder,
                                        nextBatterId = nextBatterId
                                    )
                                },
                                onUndoLastBall = { viewModel.undoLastBall() },
                                onSwapStrike = { viewModel.swapStrike() },
                                onChangeBowler = { viewModel.changeCurrentBowler(it) },
                                onSetActivePlayers = { s, ns, b -> viewModel.setActivePlayers(s, ns, b) },
                                onAwardPenaltyRuns = { viewModel.awardPenaltyRuns(it) },
                                onDeclareOrEndInnings = { viewModel.declareOrCompleteCurrentInnings(it) },
                                onStartNextInnings = { enforceFollowOn ->
                                    viewModel.startNextInnings(enforceFollowOn = enforceFollowOn)
                                },
                                onConcludeMatchAsDraw = { viewModel.concludeMatchAsDraw() },
                                onApplyDlsTarget = { revOvers, revTarget ->
                                    viewModel.applyDlsRevisedTarget(revOvers, revTarget)
                                },
                                onQuickSavePlayer = { player, callback ->
                                    viewModel.savePlayer(player, callback)
                                }
                            )

                            AppTab.SCORECARD -> ScorecardScreen(
                                state = activeMatchState,
                                allMatches = allMatches,
                                onSelectMatch = { matchId ->
                                    viewModel.selectMatchAndOpen(matchId, AppTab.SCORECARD)
                                },
                                onBack = { viewModel.navigateToTab(AppTab.HOME) }
                            )

                            AppTab.PLAYERS -> PlayersAndStatsScreen(
                                allPlayers = allPlayers,
                                careerStats = careerStats,
                                onSavePlayer = { viewModel.savePlayer(it) },
                                onDeletePlayer = { viewModel.deletePlayer(it) },
                                onDeleteTeam = { viewModel.deleteTeamAndPlayers(it) },
                                onBulkUploadPlayers = { teamName, rawText ->
                                    viewModel.bulkUploadPlayers(teamName, rawText)
                                },
                                onBack = { viewModel.navigateToTab(AppTab.HOME) }
                            )

                            AppTab.SETTINGS -> SettingsScreen(
                                settings = settings,
                                allPlayers = allPlayers,
                                tournaments = allTournaments,
                                totalPlayersCount = allPlayers.size,
                                totalTeamsCount = distinctTeamsCount,
                                totalMatchesCount = allMatches.size,
                                totalTournamentsCount = allTournaments.size,
                                statusMessage = statusBanner,
                                onSaveSettings = { viewModel.updateSettings(it) },
                                onSavePlayer = { viewModel.savePlayer(it) },
                                onDeletePlayer = { viewModel.deletePlayer(it) },
                                onClearMatchHistoryOnly = { viewModel.clearMatchHistoryOnly() },
                                onResetAllData = { viewModel.resetAllData() },
                                onOpenUploadPlayers = { viewModel.navigateToTab(AppTab.PLAYERS) },
                                onOpenTournamentsHub = { viewModel.navigateToTab(AppTab.TOURNAMENTS) },
                                onSelectTournament = { tourneyId ->
                                    viewModel.selectTournament(tourneyId)
                                    viewModel.navigateToTab(AppTab.TOURNAMENTS)
                                },
                                onDeleteTournament = { viewModel.deleteTournament(it) },
                                onOpenAboutCredits = { viewModel.navigateToTab(AppTab.ABOUT) },
                                onBack = { viewModel.navigateToTab(AppTab.HOME) }
                            )

                            AppTab.ABOUT -> AboutCreditsScreen(
                                onBack = { viewModel.navigateToTab(AppTab.SETTINGS) }
                            )
                        }
                    }
                }
            }
        }
    }
}
