package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CricketDao {

    // Players
    @Query("SELECT * FROM players ORDER BY teamName ASC, id ASC")
    fun getAllPlayersFlow(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players ORDER BY teamName ASC, id ASC")
    suspend fun getAllPlayersOnce(): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE teamName = :teamName ORDER BY id ASC")
    suspend fun getPlayersByTeamOnce(teamName: String): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE id = :playerId LIMIT 1")
    suspend fun getPlayerById(playerId: Long): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<PlayerEntity>): List<Long>

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Query("DELETE FROM players WHERE id = :playerId")
    suspend fun deletePlayer(playerId: Long)

    @Query("DELETE FROM players WHERE teamName = :teamName")
    suspend fun deletePlayersByTeam(teamName: String)

    @Query("DELETE FROM players")
    suspend fun deleteAllPlayers()

    // Tournaments
    @Query("SELECT * FROM tournaments ORDER BY createdAt DESC")
    fun getAllTournamentsFlow(): Flow<List<TournamentEntity>>

    @Query("SELECT * FROM tournaments WHERE id = :tournamentId LIMIT 1")
    suspend fun getTournamentByIdOnce(tournamentId: Long): TournamentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: TournamentEntity): Long

    @Update
    suspend fun updateTournament(tournament: TournamentEntity)

    @Query("DELETE FROM tournaments WHERE id = :tournamentId")
    suspend fun deleteTournament(tournamentId: Long)

    @Query("DELETE FROM tournaments")
    suspend fun deleteAllTournaments()

    // Tournament Teams
    @Query("SELECT * FROM tournament_teams ORDER BY tournamentId ASC, id ASC")
    fun getAllTournamentTeamsFlow(): Flow<List<TournamentTeamEntity>>

    @Query("SELECT * FROM tournament_teams WHERE tournamentId = :tournamentId ORDER BY id ASC")
    suspend fun getTournamentTeamsOnce(tournamentId: Long): List<TournamentTeamEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournamentTeam(team: TournamentTeamEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournamentTeams(teams: List<TournamentTeamEntity>)

    @Query("DELETE FROM tournament_teams WHERE id = :id")
    suspend fun deleteTournamentTeam(id: Long)

    @Query("DELETE FROM tournament_teams WHERE tournamentId = :tournamentId")
    suspend fun deleteTournamentTeamsForTournament(tournamentId: Long)

    @Query("DELETE FROM tournament_teams")
    suspend fun deleteAllTournamentTeams()

    // Tournament Fixtures
    @Query("SELECT * FROM tournament_fixtures ORDER BY tournamentId DESC, matchNumber ASC, id ASC")
    fun getAllTournamentFixturesFlow(): Flow<List<TournamentFixtureEntity>>

    @Query("SELECT * FROM tournament_fixtures WHERE tournamentId = :tournamentId ORDER BY matchNumber ASC, id ASC")
    suspend fun getFixturesForTournamentOnce(tournamentId: Long): List<TournamentFixtureEntity>

    @Query("SELECT * FROM tournament_fixtures WHERE id = :fixtureId LIMIT 1")
    suspend fun getFixtureByIdOnce(fixtureId: Long): TournamentFixtureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixture(fixture: TournamentFixtureEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixtures(fixtures: List<TournamentFixtureEntity>)

    @Update
    suspend fun updateFixture(fixture: TournamentFixtureEntity)

    @Query("DELETE FROM tournament_fixtures WHERE id = :fixtureId")
    suspend fun deleteFixture(fixtureId: Long)

    @Query("DELETE FROM tournament_fixtures WHERE tournamentId = :tournamentId")
    suspend fun deleteFixturesForTournament(tournamentId: Long)

    @Query("DELETE FROM tournament_fixtures")
    suspend fun deleteAllTournamentFixtures()

    // Matches
    @Query("SELECT * FROM matches ORDER BY createdAt DESC")
    fun getAllMatchesFlow(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :matchId LIMIT 1")
    fun getMatchByIdFlow(matchId: Long): Flow<MatchEntity?>

    @Query("SELECT * FROM matches WHERE id = :matchId LIMIT 1")
    suspend fun getMatchByIdOnce(matchId: Long): MatchEntity?

    @Query("SELECT * FROM matches WHERE status = 'LIVE' ORDER BY createdAt DESC LIMIT 1")
    fun getLatestLiveMatchFlow(): Flow<MatchEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity): Long

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Query("DELETE FROM matches WHERE id = :matchId")
    suspend fun deleteMatch(matchId: Long)

    @Query("DELETE FROM matches")
    suspend fun deleteAllMatches()

    // Innings
    @Query("SELECT * FROM innings WHERE matchId = :matchId ORDER BY inningsNumber ASC")
    fun getInningsForMatchFlow(matchId: Long): Flow<List<InningsEntity>>

    @Query("SELECT * FROM innings WHERE matchId = :matchId ORDER BY inningsNumber ASC")
    suspend fun getInningsForMatchOnce(matchId: Long): List<InningsEntity>

    @Query("SELECT * FROM innings ORDER BY matchId DESC, inningsNumber ASC")
    fun getAllInningsFlow(): Flow<List<InningsEntity>>

    @Query("SELECT * FROM innings WHERE id = :inningsId LIMIT 1")
    suspend fun getInningsByIdOnce(inningsId: Long): InningsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInnings(innings: InningsEntity): Long

    @Update
    suspend fun updateInnings(innings: InningsEntity)

    @Query("DELETE FROM innings WHERE matchId = :matchId")
    suspend fun deleteInningsForMatch(matchId: Long)

    @Query("DELETE FROM innings")
    suspend fun deleteAllInnings()

    // Ball Events
    @Query("SELECT * FROM ball_events WHERE matchId = :matchId ORDER BY inningsNumber ASC, sequenceNumber ASC")
    fun getBallEventsForMatchFlow(matchId: Long): Flow<List<BallEventEntity>>

    @Query("SELECT * FROM ball_events WHERE inningsId = :inningsId ORDER BY sequenceNumber ASC")
    suspend fun getBallEventsForInningsOnce(inningsId: Long): List<BallEventEntity>

    @Query("SELECT * FROM ball_events ORDER BY matchId ASC, inningsNumber ASC, sequenceNumber ASC")
    fun getAllBallEventsFlow(): Flow<List<BallEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBallEvent(event: BallEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBallEvents(events: List<BallEventEntity>)

    @Query("DELETE FROM ball_events WHERE id = :ballId")
    suspend fun deleteBallEventById(ballId: Long)

    @Query("DELETE FROM ball_events WHERE matchId = :matchId")
    suspend fun deleteBallEventsForMatch(matchId: Long)

    @Query("DELETE FROM ball_events")
    suspend fun deleteAllBallEvents()
}
