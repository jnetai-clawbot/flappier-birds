package com.jnetaol.flappierbirds.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.jnetaol.flappierbirds.data.db.AppDatabase
import com.jnetaol.flappierbirds.data.model.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "FlappierBirdsPrefs")

class GameRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)

    val stats: Flow<GameStats?> = db.gameStatsDao().getStats()
    val achievements: Flow<List<Achievement>> = db.achievementDao().getAll()
    val birdSkins: Flow<List<UnlockableItem>> = db.unlockableItemDao().getByType("bird_skin")
    val backgrounds: Flow<List<UnlockableItem>> = db.unlockableItemDao().getByType("background")
    val highScores: Flow<List<HighScore>> = db.highScoreDao().getAll()
    val topScores: Flow<List<HighScore>> = db.highScoreDao().getTopScores()

    suspend fun initDefaults() {
        val existingStats = db.gameStatsDao().getStatsOnce()
        if (existingStats == null) {
            db.gameStatsDao().insertOrUpdate(GameStats())
        }

        val existingAchievements = db.achievementDao().getAll().first()
        if (existingAchievements.isEmpty()) {
            db.achievementDao().insertAll(defaultAchievements)
        }

        val existingItems = db.unlockableItemDao().getAll().first()
        if (existingItems.isEmpty()) {
            db.unlockableItemDao().insertAll(defaultBirdSkins + defaultBackgrounds)
        }
    }

    suspend fun recordGameEnd(
        mode: String,
        score: Int,
        coins: Int,
        flaps: Int,
        obstaclesPassed: Int,
        sessionDurationMs: Long
    ) {
        db.gameStatsDao().incrementGamesPlayed()
        db.gameStatsDao().addTotalScore(score.toLong())
        db.gameStatsDao().updateHighestScore(score)
        db.gameStatsDao().addCoins(coins)
        db.gameStatsDao().addFlaps(flaps)
        db.gameStatsDao().updateLongestSession(sessionDurationMs)
        db.gameStatsDao().addObstaclesPassed(obstaclesPassed)

        when (mode) {
            "endless" -> db.gameStatsDao().incrementEndlessSessions()
            "challenge" -> db.gameStatsDao().incrementChallengeSessions()
            "practice" -> db.gameStatsDao().incrementPracticeSessions()
        }

        val existing = db.highScoreDao().getByMode(mode)
        if (existing == null || score > existing.score) {
            db.highScoreDao().insertOrUpdate(
                HighScore(mode = mode, score = score, coins = coins, timestamp = System.currentTimeMillis())
            )
        }

        checkAchievements(score, coins, mode)
    }

    private suspend fun checkAchievements(score: Int, coins: Int, mode: String) {
        val stats = db.gameStatsDao().getStatsOnce() ?: return
        val now = System.currentTimeMillis()

        if (stats.gamesPlayed >= 1) unlockAchievement("first_flap", now)
        if (score >= 10) unlockAchievement("score_10", now)
        if (score >= 50) unlockAchievement("score_50", now)
        if (score >= 100) unlockAchievement("score_100", now)
        if (score >= 500) unlockAchievement("score_500", now)
        if (stats.totalCoins >= 100) unlockAchievement("coin_collector", now)
        if (stats.totalCoins >= 500) unlockAchievement("coin_hoarder", now)
        if (stats.gamesPlayed >= 50) unlockAchievement("marathon", now)
        if (stats.gamesPlayed >= 100) unlockAchievement("centurion", now)
        if (stats.practiceSessions >= 10) unlockAchievement("practice_makes_perfect", now)

        val unlockedSkins = db.unlockableItemDao().getByType("bird_skin").first().count { it.unlocked }
        if (unlockedSkins >= 3) unlockAchievement("skin_collector", now)
        if (unlockedSkins >= 8) unlockAchievement("full_wardrobe", now)
    }

    suspend fun unlockAchievement(id: String, timestamp: Long) {
        val achievement = db.achievementDao().getById(id)
        if (achievement != null && !achievement.unlocked) {
            db.achievementDao().unlock(id, timestamp)
        }
    }

    suspend fun unlockItem(id: String): Boolean {
        val item = db.unlockableItemDao().getById(id) ?: return false
        val stats = db.gameStatsDao().getStatsOnce() ?: return false
        if (item.unlocked) return true
        if (stats.totalCoins < item.cost) return false
        db.gameStatsDao().addCoins(-item.cost)
        db.unlockableItemDao().unlock(id)
        return true
    }

    suspend fun equipItem(id: String) {
        val item = db.unlockableItemDao().getById(id) ?: return
        if (!item.unlocked) return
        db.unlockableItemDao().unequipAllOfType(item.type)
        db.unlockableItemDao().equip(id)
    }

    suspend fun getEquippedBirdSkin(): UnlockableItem? {
        return db.unlockableItemDao().getEquipped("bird_skin")
    }

    suspend fun getEquippedBackground(): UnlockableItem? {
        return db.unlockableItemDao().getEquipped("background")
    }

    suspend fun claimDailyReward(): Int {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val existing = db.dailyRewardDao().getByDate(today)
        if (existing?.claimed == true) return 0

        val latest = db.dailyRewardDao().getLatest()
        val streak = if (latest != null) {
            val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
            if (latest.date == yesterday) latest.dayStreak + 1 else 1
        } else {
            1
        }

        val amount = when {
            streak >= 7 -> 50
            streak >= 5 -> 30
            streak >= 3 -> 20
            else -> 10
        }

        db.dailyRewardDao().insertOrUpdate(
            DailyReward(date = today, claimed = true, amount = amount, dayStreak = streak)
        )
        db.gameStatsDao().addCoins(amount)
        return amount
    }

    suspend fun canClaimDailyReward(): Boolean {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val existing = db.dailyRewardDao().getByDate(today)
        return existing?.claimed != true
    }

    suspend fun getDailyStreak(): Int {
        val latest = db.dailyRewardDao().getLatest()
        return latest?.dayStreak ?: 0
    }

    companion object {
        val defaultAchievements = listOf(
            Achievement("first_flap", "First Flap", "Complete your first game", target = 1),
            Achievement("score_10", "Score 10", "Reach a score of 10", target = 10),
            Achievement("score_50", "Score 50", "Reach a score of 50", target = 50),
            Achievement("score_100", "Score 100", "Reach a score of 100", target = 100),
            Achievement("score_500", "Score 500", "Reach a score of 500", target = 500),
            Achievement("coin_collector", "Coin Collector", "Collect 100 coins", target = 100),
            Achievement("coin_hoarder", "Coin Hoarder", "Collect 500 coins", target = 500),
            Achievement("marathon", "Marathon", "Play 50 games", target = 50),
            Achievement("centurion", "Centurion", "Play 100 games", target = 100),
            Achievement("night_owl", "Night Owl", "Play during night cycle", target = 1),
            Achievement("storm_rider", "Storm Rider", "Play during rain", target = 1),
            Achievement("skin_collector", "Skin Collector", "Unlock 3 bird skins", target = 3),
            Achievement("full_wardrobe", "Full Wardrobe", "Unlock all bird skins", target = 8),
            Achievement("practice_makes_perfect", "Practice Makes Perfect", "Complete 10 practice sessions", target = 10)
        )

        val defaultBirdSkins = listOf(
            UnlockableItem("bird_default", "bird_skin", "Classic Yellow", 0, unlocked = true, equipped = true, colorHex = "#FFFFD700"),
            UnlockableItem("bird_red", "bird_skin", "Crimson", 50, colorHex = "#FFF85149"),
            UnlockableItem("bird_blue", "bird_skin", "Sky Blue", 50, colorHex = "#FF58A6FF"),
            UnlockableItem("bird_green", "bird_skin", "Forest Green", 50, colorHex = "#FF3FB950"),
            UnlockableItem("bird_purple", "bird_skin", "Royal Purple", 100, colorHex = "#FFA371F7"),
            UnlockableItem("bird_orange", "bird_skin", "Sunset Orange", 100, colorHex = "#FFDB6D28"),
            UnlockableItem("bird_pink", "bird_skin", "Bubblegum Pink", 150, colorHex = "#FFFF77B5"),
            UnlockableItem("bird_cyan", "bird_skin", "Aqua Cyan", 150, colorHex = "#FF39D2C0"),
            UnlockableItem("bird_white", "bird_skin", "Ghost White", 200, colorHex = "#FFFFFFFF")
        )

        val defaultBackgrounds = listOf(
            UnlockableItem("bg_default", "background", "Classic Sky", 0, unlocked = true, equipped = true, colorHex = "#FF4A90D9"),
            UnlockableItem("bg_sunset", "background", "Sunset Glow", 100, colorHex = "#FFFF6B35"),
            UnlockableItem("bg_night", "background", "Starry Night", 100, colorHex = "#FF0D1B2A"),
            UnlockableItem("bg_forest", "background", "Deep Forest", 150, colorHex = "#FF1B5E20"),
            UnlockableItem("bg_ocean", "background", "Ocean Depths", 150, colorHex = "#FF1B3A5C"),
            UnlockableItem("bg_desert", "background", "Desert Dunes", 200, colorHex = "#FFEDC9AF")
        )
    }
}
