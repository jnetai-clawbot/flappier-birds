@file:OptIn(ExperimentalMaterial3Api::class)

package com.jnetaol.flappierbirds.ui.screens.game

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jnetaol.flappierbirds.engine.GameEngine
import com.jnetaol.flappierbirds.engine.SoundManager
import com.jnetaol.flappierbirds.data.repository.GameRepository
import com.jnetaol.flappierbirds.logger.DebugLogger
import kotlinx.coroutines.*
import kotlin.math.*

@Composable
fun GameScreen(
    gameMode: String,
    repository: GameRepository,
    soundManager: SoundManager,
    hapticEnabled: Boolean,
    showFps: Boolean,
    graphicsQuality: String,
    onGameEnd: (score: Int, coins: Int, flaps: Int, obstaclesPassed: Int, durationMs: Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val engine = remember { GameEngine() }
    var frameTick by remember { mutableIntStateOf(0) }
    var screenWidth by remember { mutableFloatStateOf(1080f) }
    var screenHeight by remember { mutableFloatStateOf(1920f) }
    var showPauseMenu by remember { mutableStateOf(false) }
    var gameEndRecorded by remember { mutableStateOf(false) }
    var initDone by remember { mutableStateOf(false) }

    val vibrator = remember {
        try { context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator }
        catch (_: Exception) { null }
    }

    LaunchedEffect(Unit) {
        try {
            engine.gameMode = gameMode
            DebugLogger.i("FB-200", "Game started: mode=$gameMode", null)
            var fc = 0
            while (isActive) {
                try {
                    engine.update()
                    frameTick = fc; fc++
                    if (engine.isGameOver && !gameEndRecorded) {
                        gameEndRecorded = true
                        DebugLogger.i("FB-201", "Game over: score=${engine.score}", null)
                        val finalDurationMs = engine.getSessionDurationMs()
                        val finalFlaps = engine.flaps
                        val finalObstacles = engine.obstaclesPassed
                        val finalCoins = engine.score
                        launch(Dispatchers.IO) {
                            try {
                                repository.recordGameEnd(
                                    mode = gameMode,
                                    score = engine.score,
                                    coins = finalCoins,
                                    flaps = finalFlaps,
                                    obstaclesPassed = finalObstacles,
                                    sessionDurationMs = finalDurationMs
                                )
                            } catch (_: Exception) {}
                        }
                        onGameEnd(engine.score, finalCoins, finalFlaps, finalObstacles, finalDurationMs)
                    }
                } catch (e: Exception) {
                    DebugLogger.logException("FB-210", "Game loop error", e)
                }
                delay(16L)
            }
        } catch (e: Exception) {
            DebugLogger.logException("FB-211", "Game loop fatal", e)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF70C5DE))
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    if (engine.isGameOver) { engine.resetGame(); gameEndRecorded = false }
                    else if (showPauseMenu) { showPauseMenu = false; engine.isPaused = false }
                    else {
                        engine.flap(); soundManager.playFlap()
                        if (hapticEnabled) {
                            try { vibrator?.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE)) }
                            catch (_: Exception) {}
                        }
                    }
                })
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            try {
                screenWidth = size.width; screenHeight = size.height
                if (!initDone) { engine.init(screenWidth, screenHeight); initDone = true }

                drawSky(engine, size)
                drawPipes(engine, size)
                drawGround(engine, size)
                drawBird(engine, size)
                drawParticles(engine, size, graphicsQuality)
                drawScore(engine, size)
                if (showFps) drawFps(frameTick, size)
                if (!engine.gameStarted && !engine.isGameOver) drawStartPrompt(engine, size)
            } catch (e: Exception) {
                DebugLogger.logException("FB-220", "Canvas draw error", e)
            }
        }

        if (showPauseMenu) PauseOverlay(
            onResume = { showPauseMenu = false; engine.isPaused = false },
            onRestart = { showPauseMenu = false; engine.resetGame(); gameEndRecorded = false },
            onMainMenu = onNavigateBack
        )

        if (engine.isGameOver) GameOverOverlay(
            score = engine.score, bestScore = engine.bestScore,
            onRestart = { engine.resetGame(); gameEndRecorded = false },
            onMainMenu = onNavigateBack
        )

        IconButton(
            onClick = { if (!engine.isGameOver && engine.gameStarted) { showPauseMenu = true; engine.isPaused = true } },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).statusBarsPadding()
        ) { Icon(Icons.Default.Pause, "Pause", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(32.dp)) }
    }
}

private fun DrawScope.drawSky(engine: GameEngine, size: Size) {
    drawRect(Color(engine.getSkyColor()), Offset.Zero, size)
}

private fun DrawScope.drawPipes(engine: GameEngine, size: Size) {
    val body = Color(engine.getPipeBodyColor())
    val cap = Color(engine.getPipeCapColor())
    val lip = Color(engine.getPipeLipColor())
    val pw = engine.getPipeWidth()
    val capH = 24f; val capW = pw + 16f; val lipH = 6f; val lipW = pw + 24f

    for (pipe in engine.getPipes()) {
        drawRect(body, Offset(pipe.x, 0f), Size(pw, pipe.topHeight - capH - lipH))
        drawRect(cap, Offset(pipe.x - 8f, pipe.topHeight - capH - lipH), Size(capW, capH))
        drawRect(lip, Offset(pipe.x - 12f, pipe.topHeight - lipH), Size(lipW, lipH))

        drawRect(body, Offset(pipe.x, pipe.bottomY + lipH), Size(pw, size.height - pipe.bottomY - lipH))
        drawRect(cap, Offset(pipe.x - 8f, pipe.bottomY), Size(capW, capH))
        drawRect(lip, Offset(pipe.x - 12f, pipe.bottomY + capH), Size(lipW, lipH))
    }
}

private fun DrawScope.drawGround(engine: GameEngine, size: Size) {
    val gy = size.height - engine.getGroundHeight()
    val so = engine.getScrollOffset()

    drawRect(Color(engine.getGroundColor()), Offset(0f, gy), Size(size.width, engine.getGroundHeight()))
    drawRect(Color(engine.getGrassColor()), Offset(0f, gy), Size(size.width, 16f))
    drawRect(Color(engine.getPipeBodyColor()).copy(alpha = 0.4f), Offset(0f, gy), Size(size.width, 3f))

    val sw = 30f; val sp = 60f
    var sx = -(so % sp)
    while (sx < size.width) { drawRect(Color(engine.getGroundColor()).copy(alpha = 0.2f), Offset(sx, gy + 22f), Size(sw, 4f)); sx += sp }
}

private fun DrawScope.drawBird(engine: GameEngine, size: Size) {
    val bc = Color(0xFFE74C3C)
    val bx = engine.birdX; val by = engine.birdY; val r = engine.birdRadius
    val rot = engine.birdRotation

    rotate(rot, pivot = Offset(bx, by)) {
        drawCircle(bc, r, Offset(bx, by))

        drawCircle(Color.White.copy(alpha = 0.95f), r * 0.3f, Offset(bx + r * 0.2f, by - r * 0.15f))
        drawCircle(Color.Black, r * 0.14f, Offset(bx + r * 0.25f, by - r * 0.15f))

        val wing = if (engine.gameStarted && !engine.isGameOver) sin(engine.getWingPhase()).toFloat() * 5f else 0f
        drawOval(bc.copy(alpha = 0.9f), Offset(bx - r * 0.05f, by - r * 0.4f + wing), Size(r * 1.1f, r * 0.4f))

        val beak = Path().apply {
            moveTo(bx + r * 0.6f, by - r * 0.06f)
            lineTo(bx + r * 1.1f, by)
            lineTo(bx + r * 0.6f, by + r * 0.06f)
            close()
        }
        drawPath(beak, Color(0xFFFF6B35))
    }
}

private fun DrawScope.drawParticles(engine: GameEngine, size: Size, graphicsQuality: String) {
    val maxParticles = when (graphicsQuality) {
        "low" -> 6
        "medium" -> 12
        else -> Int.MAX_VALUE
    }
    for (p in engine.getParticles().take(maxParticles)) {
        val a = (p.life / p.maxLife).coerceIn(0f, 1f)
        drawCircle(Color(p.color).copy(alpha = a), p.size * a, Offset(p.x, p.y))
    }
}

private fun DrawScope.drawFps(frameTick: Int, size: Size) {
    val fp = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 28f
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.LEFT
        setShadowLayer(1f, 0f, 1f, android.graphics.Color.BLACK)
    }
    drawContext.canvas.nativeCanvas.drawText("Frame: $frameTick", 16f, size.height - 24f, fp)
}

private fun DrawScope.drawScore(engine: GameEngine, size: Size) {
    if (!engine.gameStarted && !engine.isGameOver) return
    val s = size.width / 1080f
    val sp = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE; textSize = 48f * s
        textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
        setShadowLayer(2f, 0f, 1f, android.graphics.Color.BLACK)
    }
    drawContext.canvas.nativeCanvas.drawText("${engine.score}", size.width / 2, 80f * s, sp)
}

private fun DrawScope.drawStartPrompt(engine: GameEngine, size: Size) {
    val s = size.width / 1080f
    val a = (sin(engine.getScrollOffset() * 0.008f) * 0.3f + 0.7f).toFloat()
    val pp = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE; textSize = 24f * s
        textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
        setShadowLayer(1f, 0f, 1f, android.graphics.Color.BLACK)
        this.alpha = (a * 255).toInt()
    }
    drawContext.canvas.nativeCanvas.drawText("Tap to Flap", size.width / 2, size.height * 0.44f, pp)
}

@Composable
private fun PauseOverlay(onResume: () -> Unit, onRestart: () -> Unit, onMainMenu: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)), modifier = Modifier.padding(32.dp)) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Paused", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(24.dp))
                Button(onResume, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3FB950))) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Resume", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Button(onRestart, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF58A6FF))) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Restart", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onMainMenu, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Home, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Main Menu", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GameOverOverlay(score: Int, bestScore: Int, onRestart: () -> Unit, onMainMenu: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)), modifier = Modifier.padding(32.dp)) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Game Over", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF85149))
                Spacer(Modifier.height(16.dp))
                Text("Score", fontSize = 14.sp, color = Color(0xFF8B949E))
                Text("$score", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text("Best: $bestScore", fontSize = 16.sp, color = Color(0xFFD29922), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(24.dp))
                Button(onRestart, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3FB950))) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Play Again", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onMainMenu, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Home, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Main Menu", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
