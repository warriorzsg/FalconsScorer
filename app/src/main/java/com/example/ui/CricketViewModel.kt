package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CricketAnalytics
import com.example.data.CricketDatabase
import com.example.data.CricketRepository
import com.example.data.InningsEntity
import com.example.data.InningsScorecardAnalysis
import com.example.data.MatchEntity
import com.example.data.MvpCandidate
import com.example.data.PlayerCareerStat
import com.example.data.PlayerEntity
import com.example.data.TournamentEntity
import com.example.data.TournamentFixtureEntity
import com.example.data.TournamentOverviewStats
import com.example.data.TournamentTeamEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    HOME,
    TOURNAMENTS,
    NEW_MATCH,
    LIVE_SCORER,
    SCORECARD,
    PLAYERS,
    SETTINGS,
    ABOUT
}

data class AppSettingsState(
    val themeMode: String = "SYSTEM", // SYSTEM, DARK, LIGHT
    val defaultFormat: String = "T20",
    val defaultOvers: Int = 20,
    val defaultMaxBowlerOvers: Int = 4,
    val defaultVenue: String = "Cricket Ground",
    val confirmBeforeUndo: Boolean = false
)

data class PendingFixtureSetup(
    val tournament: TournamentEntity,
    val fixture: TournamentFixtureEntity
)

data class ActiveTournamentDetailState(
    val tournament: TournamentEntity? = null,
    val registeredTeams: List<TournamentTeamEntity> = emptyList(),
    val fixtures: List<TournamentFixtureEntity> = emptyList(),
    val tournamentMatches: List<MatchEntity> = emptyList(),
    val overviewStats: TournamentOverviewStats? = null
)

data class ActiveMatchDetailState(
    val match: MatchEntity? = null,
    val allInnings: List<InningsEntity> = emptyList(),
    val activeInnings: InningsEntity? = null,
    val analyses: List<InningsScorecardAnalysis> = emptyList(),
    val activeAnalysis: InningsScorecardAnalysis? = null,
    val battingSquad: List<PlayerEntity> = emptyList(),
    val bowlingSquad: List<PlayerEntity> = emptyList(),
    val mvpCandidates: List<MvpCandidate> = emptyList(),
    val needsNextBatter: Boolean = false,
    val needsNextBowler: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class CricketViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CricketRepository
    private val prefs = application.getSharedPreferences("falcons_scorer_prefs", Context.MODE_PRIVATE)

    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _selectedMatchId = MutableStateFlow<Long?>(null)
    val selectedMatchId: StateFlow<Long?> = _selectedMatchId.asStateFlow()

    private val _selectedTournamentId = MutableStateFlow<Long?>(null)
    val selectedTournamentId: StateFlow<Long?> = _selectedTournamentId.asStateFlow()

    private val _pendingFixtureSetup = MutableStateFlow<PendingFixtureSetup?>(null)
    val pendingFixtureSetup: StateFlow<PendingFixtureSetup?> = _pendingFixtureSetup.asStateFlow()

    private val _settingsState = MutableStateFlow(loadSettingsFromPrefs())
    val settingsState: StateFlow<AppSettingsState> = _settingsState.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    init {
        val db = CricketDatabase.getDatabase(application)
        repository = CricketRepository(db.cricketDao())
        // Zero demo data is ever inserted into the database.
    }

    private fun loadSettingsFromPrefs(): AppSettingsState {
        return AppSettingsState(
            themeMode = prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM",
            defaultFormat = prefs.getString("default_format", "T20") ?: "T20",
            defaultOvers = prefs.getInt("default_overs", 20),
            defaultMaxBowlerOvers = prefs.getInt("default_max_bowler_overs", 4),
            defaultVenue = prefs.getString("default_venue", "Cricket Ground") ?: "Cricket Ground",
            confirmBeforeUndo = prefs.getBoolean("confirm_before_undo", false)
        )
    }

    fun updateSettings(newSettings: AppSettingsState) {
        prefs.edit()
            .putString("theme_mode", newSettings.themeMode)
            .putString("default_format", newSettings.defaultFormat)
            .putInt("default_overs", newSettings.defaultOvers)
            .putInt("default_max_bowler_overs", newSettings.defaultMaxBowlerOvers)
            .putString("default_venue", newSettings.defaultVenue)
            .putBoolean("confirm_before_undo", newSettings.confirmBeforeUndo)
            .apply()
        _settingsState.value = newSettings
        _statusBannerMessage.value = "Settings saved."
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    val allMatches: StateFlow<List<MatchEntity>> = repository.allMatchesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlayers: StateFlow<List<PlayerEntity>> = repository.allPlayersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInnings: StateFlow<List<InningsEntity>> = repository.allInningsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTournaments: StateFlow<List<TournamentEntity>> = repository.allTournamentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val careerStats: StateFlow<List<PlayerCareerStat>> = combine(
        repository.allPlayersFlow,
        repository.allInningsFlow,
        repository.allBallEventsFlow
    ) { players, innings, events ->
        CricketAnalytics.calculateCareerStats(players, innings, events)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Tournament State (Points Table, NRR, Fixtures, Knockout Stages, Stats)
    val activeTournamentState: StateFlow<ActiveTournamentDetailState> = combine(
        _selectedTournamentId,
        repository.allTournamentsFlow,
        repository.allTournamentTeamsFlow,
        repository.allTournamentFixturesFlow,
        combine(
            repository.allMatchesFlow,
            repository.allInningsFlow,
            repository.allBallEventsFlow,
            repository.allPlayersFlow
        ) { m, i, e, p -> Tuple4(m, i, e, p) }
    ) { selectedId, tournaments, allTTeams, allFixtures, matchData ->
        val activeTourney = tournaments.find { it.id == selectedId } ?: tournaments.firstOrNull()
        if (activeTourney == null) {
            ActiveTournamentDetailState()
        } else {
            val regTeams = allTTeams.filter { it.tournamentId == activeTourney.id }
            val fixtures = allFixtures.filter { it.tournamentId == activeTourney.id }
            val tourneyMatches = matchData.a.filter { it.tournamentId == activeTourney.id }
            val overview = CricketAnalytics.calculateTournamentOverview(
                tournament = activeTourney,
                registeredTeams = regTeams,
                tournamentMatches = tourneyMatches,
                allInnings = matchData.b,
                allEvents = matchData.c,
                allPlayers = matchData.d
            )
            ActiveTournamentDetailState(
                tournament = activeTourney,
                registeredTeams = regTeams,
                fixtures = fixtures,
                tournamentMatches = tourneyMatches,
                overviewStats = overview
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActiveTournamentDetailState())

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    private val effectiveMatchIdFlow = combine(
        _selectedMatchId,
        repository.latestLiveMatchFlow,
        repository.allMatchesFlow
    ) { selectedId, liveMatch, matches ->
        selectedId ?: liveMatch?.id ?: matches.firstOrNull()?.id
    }

    val activeMatchState: StateFlow<ActiveMatchDetailState> = effectiveMatchIdFlow
        .flatMapLatest { matchId ->
            if (matchId == null) {
                flowOf(ActiveMatchDetailState())
            } else {
                combine(
                    repository.getMatchByIdFlow(matchId),
                    repository.getInningsForMatchFlow(matchId),
                    repository.getBallEventsForMatchFlow(matchId),
                    repository.allPlayersFlow
                ) { match, inningsList, events, players ->
                    if (match == null) {
                        ActiveMatchDetailState()
                    } else {
                        val eventsByInnings = events.groupBy { it.inningsId }
                        val analyses = inningsList.map { inn ->
                            val batSquad = players.filter { it.teamName.equals(inn.battingTeam, ignoreCase = true) }
                            val bowlSquad = players.filter { it.teamName.equals(inn.bowlingTeam, ignoreCase = true) }
                            CricketAnalytics.analyzeInnings(
                                innings = inn,
                                events = eventsByInnings[inn.id] ?: emptyList(),
                                battingSquad = batSquad,
                                bowlingSquad = bowlSquad
                            )
                        }
                        val activeInn = inningsList.find { it.inningsNumber == match.currentInningsNumber }
                            ?: inningsList.lastOrNull()
                        val activeAnalysis = analyses.find { it.innings.id == activeInn?.id }
                        val batSquad = players.filter { it.teamName.equals(activeInn?.battingTeam ?: "", ignoreCase = true) }
                        val bowlSquad = players.filter { it.teamName.equals(activeInn?.bowlingTeam ?: "", ignoreCase = true) }
                        val mvps = CricketAnalytics.calculateMatchMvp(players, events)

                        val needsNextBatter = activeInn != null &&
                            !activeInn.isCompleted &&
                            (activeInn.strikerId == null || activeInn.nonStrikerId == null)

                        val needsNextBowler = activeInn != null &&
                            !activeInn.isCompleted &&
                            (activeInn.currentBowlerId == null ||
                                (!match.allowConsecutiveOvers &&
                                    activeInn.legalBalls > 0 &&
                                    activeInn.legalBalls % 6 == 0 &&
                                    activeInn.lastOverBowlerId != null &&
                                    activeInn.currentBowlerId == activeInn.lastOverBowlerId))

                        ActiveMatchDetailState(
                            match = match,
                            allInnings = inningsList,
                            activeInnings = activeInn,
                            analyses = analyses,
                            activeAnalysis = activeAnalysis,
                            battingSquad = batSquad,
                            bowlingSquad = bowlSquad,
                            mvpCandidates = mvps,
                            needsNextBatter = needsNextBatter,
                            needsNextBowler = needsNextBowler
                        )
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActiveMatchDetailState())

    fun navigateToTab(tab: AppTab) {
        if (tab != AppTab.NEW_MATCH) {
            _pendingFixtureSetup.value = null
        }
        _currentTab.value = tab
    }

    fun selectMatchAndOpen(matchId: Long, targetTab: AppTab = AppTab.LIVE_SCORER) {
        _selectedMatchId.value = matchId
        _currentTab.value = targetTab
    }

    fun selectTournament(tournamentId: Long) {
        _selectedTournamentId.value = tournamentId
    }

    fun startFixtureMatchSetup(tournament: TournamentEntity, fixture: TournamentFixtureEntity) {
        if (fixture.linkedMatchId != null) {
            _selectedMatchId.value = fixture.linkedMatchId
            _currentTab.value = if (fixture.status == "COMPLETED") AppTab.SCORECARD else AppTab.LIVE_SCORER
        } else {
            _pendingFixtureSetup.value = PendingFixtureSetup(tournament, fixture)
            _currentTab.value = AppTab.NEW_MATCH
        }
    }

    // Tournament Actions
    fun createTournament(
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
    ) {
        viewModelScope.launch {
            val id = repository.createTournament(
                name = name,
                organizer = organizer,
                venue = venue,
                format = format,
                oversLimit = oversLimit,
                maxOversPerBowler = maxOversPerBowler,
                playersPerTeam = playersPerTeam,
                pointsForWin = pointsForWin,
                pointsForTie = pointsForTie,
                teamNames = teamNames,
                autoGenerateLeagueFixtures = autoGenerateFixtures
            )
            _selectedTournamentId.value = id
            _statusBannerMessage.value = "Tournament '$name' created!"
        }
    }

    fun registerTeamInTournament(tournamentId: Long, teamName: String, groupName: String = "Group A") {
        viewModelScope.launch {
            repository.registerTeamInTournament(tournamentId, teamName, groupName)
        }
    }

    fun removeTeamFromTournament(tournamentTeamId: Long) {
        viewModelScope.launch {
            repository.removeTeamFromTournament(tournamentTeamId)
        }
    }

    fun generateRoundRobinFixtures(tournamentId: Long) {
        viewModelScope.launch {
            repository.generateRoundRobinFixtures(tournamentId)
            _statusBannerMessage.value = "Round-robin league fixtures generated!"
        }
    }

    fun addCustomFixture(
        tournamentId: Long,
        stage: String,
        teamA: String,
        teamB: String,
        scheduledDate: String,
        venue: String
    ) {
        viewModelScope.launch {
            repository.addCustomFixture(tournamentId, stage, teamA, teamB, scheduledDate, venue)
        }
    }

    fun generateKnockoutStage(tournamentId: Long, rankedTeams: List<String>, knockoutType: String) {
        viewModelScope.launch {
            repository.generateKnockoutStageFromStandings(tournamentId, rankedTeams, knockoutType)
            _statusBannerMessage.value = "Knockout fixtures ($knockoutType) generated from standings!"
        }
    }

    fun deleteFixture(fixtureId: Long) {
        viewModelScope.launch {
            repository.deleteFixture(fixtureId)
        }
    }

    fun deleteTournament(tournamentId: Long) {
        viewModelScope.launch {
            repository.deleteTournament(tournamentId)
            if (_selectedTournamentId.value == tournamentId) {
                _selectedTournamentId.value = null
            }
            _statusBannerMessage.value = "Tournament deleted."
        }
    }

    // Data Reset & Player Upload Actions
    fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllData()
            _selectedMatchId.value = null
            _selectedTournamentId.value = null
            _pendingFixtureSetup.value = null
            _statusBannerMessage.value = "All players, teams, matches, and tournaments have been reset."
        }
    }

    fun clearMatchHistoryOnly() {
        viewModelScope.launch {
            repository.clearMatchHistoryOnly()
            _selectedMatchId.value = null
            _statusBannerMessage.value = "Match history cleared. Your teams & players were kept."
        }
    }

    fun deleteTeamAndPlayers(teamName: String) {
        viewModelScope.launch {
            repository.deleteTeamAndPlayers(teamName)
            _statusBannerMessage.value = "Deleted team '$teamName'."
        }
    }

    fun bulkUploadPlayers(teamName: String, rawText: String, onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val count = repository.bulkImportPlayers(teamName, rawText)
            _statusBannerMessage.value = if (count > 0) {
                "Added $count players to '$teamName'!"
            } else {
                "No valid player lines found."
            }
            onComplete?.invoke(count)
        }
    }

    fun createCustomMatch(
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
        allowConsecutiveOvers: Boolean = false,
        tournamentId: Long? = null,
        fixtureId: Long? = null,
        stage: String = "FRIENDLY",
        customStrikerName: String = "",
        customNonStrikerName: String = "",
        customBowlerName: String = ""
    ) {
        viewModelScope.launch {
            val matchId = repository.startNewMatch(
                teamA = teamA,
                teamB = teamB,
                format = format,
                oversLimit = oversLimit,
                maxOversPerBowler = maxOversPerBowler,
                playersPerTeam = playersPerTeam,
                tossWinner = tossWinner,
                tossDecision = tossDecision,
                venue = venue,
                openingStrikerId = openingStrikerId,
                openingNonStrikerId = openingNonStrikerId,
                openingBowlerId = openingBowlerId,
                allowConsecutiveOvers = allowConsecutiveOvers,
                tournamentId = tournamentId,
                fixtureId = fixtureId,
                stage = stage,
                customStrikerName = customStrikerName,
                customNonStrikerName = customNonStrikerName,
                customBowlerName = customBowlerName
            )
            _pendingFixtureSetup.value = null
            _selectedMatchId.value = matchId
            _currentTab.value = AppTab.LIVE_SCORER
        }
    }

    fun recordDelivery(
        runsOffBat: Int,
        extraType: String = "NONE",
        extraRuns: Int = 0,
        isWicket: Boolean = false,
        wicketType: String = "NONE",
        dismissedPlayerId: Long? = null,
        fielderName: String = "",
        nextBatterId: Long? = null
    ) {
        val state = activeMatchState.value
        val matchId = state.match?.id ?: return
        val inningsId = state.activeInnings?.id ?: return
        viewModelScope.launch {
            repository.recordBall(
                matchId = matchId,
                inningsId = inningsId,
                runsOffBat = runsOffBat,
                extraType = extraType,
                extraRuns = extraRuns,
                isWicket = isWicket,
                wicketType = wicketType,
                dismissedPlayerId = dismissedPlayerId,
                fielderName = fielderName,
                nextBatterId = nextBatterId
            )
        }
    }

    fun undoLastBall() {
        val state = activeMatchState.value
        val matchId = state.match?.id ?: return
        val inningsId = state.activeInnings?.id ?: return
        viewModelScope.launch {
            repository.undoLastBall(matchId, inningsId)
        }
    }

    fun swapStrike() {
        val inningsId = activeMatchState.value.activeInnings?.id ?: return
        viewModelScope.launch {
            repository.swapStrike(inningsId)
        }
    }

    fun changeCurrentBowler(bowlerId: Long) {
        val inningsId = activeMatchState.value.activeInnings?.id ?: return
        viewModelScope.launch {
            repository.setCurrentBowler(inningsId, bowlerId)
        }
    }

    fun setActivePlayers(strikerId: Long?, nonStrikerId: Long?, bowlerId: Long?) {
        val inningsId = activeMatchState.value.activeInnings?.id ?: return
        viewModelScope.launch {
            repository.setOpeningOrActivePlayers(inningsId, strikerId, nonStrikerId, bowlerId)
        }
    }

    fun awardPenaltyRuns(runs: Int = 5) {
        val state = activeMatchState.value
        val matchId = state.match?.id ?: return
        val inningsId = state.activeInnings?.id ?: return
        viewModelScope.launch {
            repository.addPenaltyRuns(matchId, inningsId, runs)
        }
    }

    fun declareOrCompleteCurrentInnings(isDeclared: Boolean = false) {
        val state = activeMatchState.value
        val matchId = state.match?.id ?: return
        val inningsId = state.activeInnings?.id ?: return
        viewModelScope.launch {
            repository.declareOrEndInnings(matchId, inningsId, isDeclared)
        }
    }

    fun startNextInnings(
        enforceFollowOn: Boolean = false,
        openingStrikerId: Long? = null,
        openingNonStrikerId: Long? = null,
        openingBowlerId: Long? = null
    ) {
        val matchId = activeMatchState.value.match?.id ?: return
        viewModelScope.launch {
            repository.startNextInnings(
                matchId = matchId,
                enforceFollowOn = enforceFollowOn,
                openingStrikerId = openingStrikerId,
                openingNonStrikerId = openingNonStrikerId,
                openingBowlerId = openingBowlerId
            )
        }
    }

    fun concludeMatchAsDraw() {
        val matchId = activeMatchState.value.match?.id ?: return
        viewModelScope.launch {
            repository.concludeTestMatchAsDraw(matchId)
        }
    }

    fun applyDlsRevisedTarget(revisedOvers: Int, revisedTarget: Int) {
        val matchId = activeMatchState.value.match?.id ?: return
        viewModelScope.launch {
            repository.applyDlsRevisedTarget(matchId, revisedOvers, revisedTarget)
        }
    }

    fun savePlayer(player: PlayerEntity, onSaved: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.addOrUpdatePlayer(player)
            onSaved?.invoke(id)
        }
    }

    fun deletePlayer(playerId: Long) {
        viewModelScope.launch {
            repository.deletePlayer(playerId)
        }
    }

    fun deleteMatch(matchId: Long) {
        viewModelScope.launch {
            repository.deleteMatch(matchId)
            if (_selectedMatchId.value == matchId) {
                _selectedMatchId.value = null
            }
        }
    }
}
