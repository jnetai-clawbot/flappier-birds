package com.jnetaol.flappierbirds.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jnetaol.flappierbirds.data.model.*

@Database(
    entities = [
        GameStats::class,
        Achievement::class,
        UnlockableItem::class,
        HighScore::class,
        DailyReward::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameStatsDao(): GameStatsDao
    abstract fun achievementDao(): AchievementDao
    abstract fun unlockableItemDao(): UnlockableItemDao
    abstract fun highScoreDao(): HighScoreDao
    abstract fun dailyRewardDao(): DailyRewardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return try {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flappier_birds.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
            } catch (e: Exception) {
                context.deleteDatabase("flappier_birds.db")
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flappier_birds.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
            }
        }
    }
}
