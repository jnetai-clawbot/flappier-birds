package com.jnetaol.flappierbirds.data.db

import androidx.room.*
import com.jnetaol.flappierbirds.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameStatsDao {
    @Query("SELECT * FROM game_stats WHERE id = 1")
    fun getStats(): Flow<GameStats?>

    @Query("SELECT * FROM game_stats WHERE id = 1")
    suspend fun getStatsOnce(): GameStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: GameStats)

    @Query("UPDATE game_stats SET gamesPlayed = gamesPlayed + 1 WHERE id = 1")
    suspend fun incrementGamesPlayed()

    @Query("UPDATE game_stats SET totalScore = totalScore + :score WHERE id = 1")
    suspend fun addTotalScore(score: Long)

    @Query("UPDATE game_stats SET highestScore = MAX(highestScore, :score) WHERE id = 1")
    suspend fun updateHighestScore(score: Int)

    @Query("UPDATE game_stats SET totalCoins = totalCoins + :coins WHERE id = 1")
    suspend fun addCoins(coins: Int)

    @Query("UPDATE game_stats SET totalFlaps = totalFlaps + :flaps WHERE id = 1")
    suspend fun addFlaps(flaps: Int)

    @Query("UPDATE game_stats SET longestSessionMs = MAX(longestSessionMs, :duration) WHERE id = 1")
    suspend fun updateLongestSession(duration: Long)

    @Query("UPDATE game_stats SET obstaclesPassed = obstaclesPassed + :count WHERE id = 1")
    suspend fun addObstaclesPassed(count: Int)

    @Query("UPDATE game_stats SET practiceSessions = practiceSessions + 1 WHERE id = 1")
    suspend fun incrementPracticeSessions()

    @Query("UPDATE game_stats SET challengeSessions = challengeSessions + 1 WHERE id = 1")
    suspend fun incrementChallengeSessions()

    @Query("UPDATE game_stats SET endlessSessions = endlessSessions + 1 WHERE id = 1")
    suspend fun incrementEndlessSessions()
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY unlocked DESC, id ASC")
    fun getAll(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getById(id: String): Achievement?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(achievement: Achievement)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(achievements: List<Achievement>)

    @Query("UPDATE achievements SET progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Int)

    @Query("UPDATE achievements SET unlocked = 1, unlockedTimestamp = :timestamp WHERE id = :id")
    suspend fun unlock(id: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM achievements WHERE unlocked = 1")
    suspend fun getUnlockedCount(): Int
}

@Dao
interface UnlockableItemDao {
    @Query("SELECT * FROM unlockable_items WHERE type = :type ORDER BY id ASC")
    fun getByType(type: String): Flow<List<UnlockableItem>>

    @Query("SELECT * FROM unlockable_items ORDER BY type, id ASC")
    fun getAll(): Flow<List<UnlockableItem>>

    @Query("SELECT * FROM unlockable_items WHERE id = :id")
    suspend fun getById(id: String): UnlockableItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: UnlockableItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<UnlockableItem>)

    @Query("UPDATE unlockable_items SET unlocked = 1 WHERE id = :id")
    suspend fun unlock(id: String)

    @Query("UPDATE unlockable_items SET equipped = 0 WHERE type = :type")
    suspend fun unequipAllOfType(type: String)

    @Query("UPDATE unlockable_items SET equipped = 1 WHERE id = :id")
    suspend fun equip(id: String)

    @Query("SELECT * FROM unlockable_items WHERE type = :type AND equipped = 1")
    suspend fun getEquipped(type: String): UnlockableItem?
}

@Dao
interface HighScoreDao {
    @Query("SELECT * FROM high_scores ORDER BY score DESC")
    fun getAll(): Flow<List<HighScore>>

    @Query("SELECT * FROM high_scores WHERE mode = :mode")
    suspend fun getByMode(mode: String): HighScore?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(score: HighScore)

    @Query("SELECT * FROM high_scores ORDER BY score DESC LIMIT 10")
    fun getTopScores(): Flow<List<HighScore>>
}

@Dao
interface DailyRewardDao {
    @Query("SELECT * FROM daily_rewards WHERE date = :date")
    suspend fun getByDate(date: String): DailyReward?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(reward: DailyReward)

    @Query("SELECT * FROM daily_rewards ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): DailyReward?

    @Query("SELECT COUNT(*) FROM daily_rewards WHERE claimed = 1")
    suspend fun getClaimedCount(): Int
}
