package com.jnetaol.flappierbirds.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_stats")
data class GameStats(
    @PrimaryKey val id: Int = 1,
    val gamesPlayed: Int = 0,
    val totalScore: Long = 0,
    val highestScore: Int = 0,
    val totalCoins: Int = 0,
    val totalFlaps: Int = 0,
    val longestSessionMs: Long = 0,
    val obstaclesPassed: Int = 0,
    val practiceSessions: Int = 0,
    val challengeSessions: Int = 0,
    val endlessSessions: Int = 0
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val unlocked: Boolean = false,
    val progress: Int = 0,
    val target: Int = 1,
    val unlockedTimestamp: Long = 0
)

@Entity(tableName = "unlockable_items")
data class UnlockableItem(
    @PrimaryKey val id: String,
    val type: String,
    val name: String,
    val cost: Int = 0,
    val unlocked: Boolean = false,
    val equipped: Boolean = false,
    val colorHex: String = "#FFFFD700"
)

@Entity(tableName = "high_scores")
data class HighScore(
    @PrimaryKey val mode: String,
    val score: Int = 0,
    val coins: Int = 0,
    val timestamp: Long = 0
)

@Entity(tableName = "daily_rewards")
data class DailyReward(
    @PrimaryKey val date: String,
    val claimed: Boolean = false,
    val amount: Int = 0,
    val dayStreak: Int = 0
)
