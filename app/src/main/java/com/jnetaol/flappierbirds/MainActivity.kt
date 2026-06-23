package com.jnetaol.flappierbirds

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jnetaol.flappierbirds.data.repository.GameRepository
import com.jnetaol.flappierbirds.engine.Difficulty
import com.jnetaol.flappierbirds.engine.SoundManager
import com.jnetaol.flappierbirds.logger.DebugLogger
import com.jnetaol.flappierbirds.ui.screens.about.AboutScreen
import com.jnetaol.flappierbirds.ui.screens.achievements.AchievementsScreen
import com.jnetaol.flappierbirds.ui.screens.game.GameScreen
import com.jnetaol.flappierbirds.ui.screens.menu.MainMenuScreen
import com.jnetaol.flappierbirds.ui.screens.settings.SettingsScreen
import com.jnetaol.flappierbirds.ui.screens.statistics.StatisticsScreen
import com.jnetaol.flappierbirds.BuildConfig
import com.jnetaol.flappierbirds.ui.theme.FlappierBirdsTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {
    private lateinit var repository: GameRepository
    private lateinit var soundManager: SoundManager

    private val _musicVolume = MutableStateFlow(0.7f)
    private val _soundVolume = MutableStateFlow(0.8f)
    private val _hapticEnabled = MutableStateFlow(true)
    private val _graphicsQuality = MutableStateFlow("high")
    private val _difficulty = MutableStateFlow(Difficulty.DEFAULT.id)
    private val _showFps = MutableStateFlow(false)
    private val _debugMode = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as FlappierBirdsApp
        repository = app.repository
        soundManager = SoundManager(this)

        lifecycleScope.launch {
            repository.difficulty.collect { savedDifficulty ->
                _difficulty.value = savedDifficulty
            }
        }

        DebugLogger.i("FB-006", "MainActivity created", null)

        setContent {
            FlappierBirdsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0D1117)
                ) {
                    val musicVolume by _musicVolume.collectAsStateWithLifecycle()
                    val soundVolume by _soundVolume.collectAsStateWithLifecycle()
                    val hapticEnabled by _hapticEnabled.collectAsStateWithLifecycle()
                    val graphicsQuality by _graphicsQuality.collectAsStateWithLifecycle()
                    val difficulty by _difficulty.collectAsStateWithLifecycle()
                    val showFps by _showFps.collectAsStateWithLifecycle()
                    val debugMode by _debugMode.collectAsStateWithLifecycle()

                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "main_menu"
                    ) {
                        composable("main_menu") {
                            MainMenuScreen(
                                repository = repository,
                                onPlay = { mode -> navController.navigate("game/$mode") },
                                onStatistics = { navController.navigate("statistics") },
                                onAchievements = { navController.navigate("achievements") },
                                onSettings = { navController.navigate("settings") },
                                onAbout = { navController.navigate("about") },
                                onExit = { finish() }
                            )
                        }

                        composable("game/{mode}") { backStackEntry ->
                            val mode = backStackEntry.arguments?.getString("mode") ?: "endless"
                            GameScreen(
                                gameMode = mode,
                                repository = repository,
                                soundManager = soundManager,
                                hapticEnabled = hapticEnabled,
                                difficulty = difficulty,
                                showFps = showFps,
                                graphicsQuality = graphicsQuality,
                                onGameEnd = { score, coins, flaps, obstaclesPassed, durationMs ->
                                    DebugLogger.i("FB-202", "Game ended: score=$score coins=$coins", null)
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("statistics") {
                            StatisticsScreen(
                                repository = repository,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("achievements") {
                            AchievementsScreen(
                                repository = repository,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                musicVolume = musicVolume,
                                soundVolume = soundVolume,
                                hapticEnabled = hapticEnabled,
                                graphicsQuality = graphicsQuality,
                                difficulty = difficulty,
                                showFps = showFps,
                                debugMode = debugMode,
                                onMusicVolumeChange = { _musicVolume.value = it },
                                onSoundVolumeChange = { _soundVolume.value = it },
                                onHapticToggle = { _hapticEnabled.value = it },
                                onGraphicsQualityChange = { _graphicsQuality.value = it },
                                onDifficultyChange = { value ->
                                    _difficulty.value = value
                                    lifecycleScope.launch { repository.setDifficulty(value) }
                                },
                                onShowFpsToggle = { _showFps.value = it },
                                onDebugModeToggle = { _debugMode.value = it },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("about") {
                            AboutScreen(
                                versionName = BuildConfig.VERSION_NAME,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
        DebugLogger.i("FB-007", "MainActivity destroyed", null)
    }
}
