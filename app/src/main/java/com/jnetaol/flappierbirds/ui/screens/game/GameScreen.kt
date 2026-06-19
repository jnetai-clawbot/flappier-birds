@file:OptIn(ExperimentalMaterial3Api::class)

package com.jnetaol.flappierbirds.ui.screens.game

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
    var screenWidth by remember { mutableFloatStateOf(1080f) }
    var screenHeight by remember { mutableFloatStateOf(1920f) }
    var frameCount by remember { mutableIntStateOf(0) }
    var fps by remember { mutableIntStateOf(0) }
    var fpsTimer by remember { mutableLongStateOf(0L) }
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

            while (isActive) {
                try {
                    if (!engine.isPaused && !engine.isGameOver && engine.gameStarted) {
                        engine.update()
                        engine.increaseDifficulty()
                    }

                    frameCount++
                    val now = System.currentTimeMillis()
                    if (now - fpsTimer >= 1000) {
                        fps = frameCount; frameCount = 0; fpsTimer = now
                    }

                    if (engine.isGameOver && !gameEndRecorded) {
                        gameEndRecorded = true
                        val duration = engine.getSessionDurationMs()
                        DebugLogger.i("FB-201", "Game over: score=${engine.score} coins=${engine.coins}", null)
                        launch(Dispatchers.IO) {
                            try {
                                repository.recordGameEnd(
                                    mode = gameMode, score = engine.score, coins = engine.coins,
                                    flaps = engine.flaps, obstaclesPassed = engine.obstaclesPassed,
                                    sessionDurationMs = duration
                                )
                            } catch (e: Exception) {
                                DebugLogger.logException("FB-203", "Failed to record game end", e)
                            }
                        }
                        onGameEnd(engine.score, engine.coins, engine.flaps, engine.obstaclesPassed, duration)
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
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    if (engine.isGameOver) {
                        engine.resetGame()
                        gameEndRecorded = false
                    } else if (showPauseMenu) {
                        showPauseMenu = false
                        engine.isPaused = false
                    } else {
                        engine.flap()
                        soundManager.playFlap()
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
                screenWidth = size.width
                screenHeight = size.height
                if (!initDone) {
                    engine.init(screenWidth, screenHeight)
                    initDone = true
                }

                drawGameBackground(engine, size)
                drawClouds(engine, size)
                drawPipes(engine, size)
                drawCoins(engine, size)
                drawProjectiles(engine, size)
                drawGround(engine, size)
                drawBird(engine, size)
                drawParticles(engine, size)
                drawWeatherEffects(engine, size)
                drawHUD(engine, size)

                if (!engine.gameStarted && !engine.isGameOver) {
                    drawStartPrompt(engine, size)
                }
            } catch (e: Exception) {
                DebugLogger.logException("FB-220", "Canvas draw error", e)
            }
        }

        if (engine.weaponAmmo > 0 && engine.gameStarted && !engine.isGameOver) {
            FloatingActionButton(
                onClick = { engine.fireWeapon() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .navigationBarsPadding()
                    .size(56.dp),
                containerColor = Color(0xFFFF5722),
                contentColor = Color.White
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FlashOn, contentDescription = "Fire", modifier = Modifier.size(22.dp))
                    Text("${engine.weaponAmmo}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showPauseMenu) {
            PauseOverlay(
                onResume = { showPauseMenu = false; engine.isPaused = false },
                onRestart = { showPauseMenu = false; engine.resetGame(); gameEndRecorded = false },
                onMainMenu = onNavigateBack
            )
        }

        if (engine.isGameOver) {
            GameOverOverlay(
                score = engine.score,
                bestScore = engine.bestScore,
                coins = engine.coins,
                level = engine.getLevel(),
                onRestart = { engine.resetGame(); gameEndRecorded = false },
                onMainMenu = onNavigateBack
            )
        }

        IconButton(
            onClick = {
                if (!engine.isGameOver && engine.gameStarted) {
                    showPauseMenu = true; engine.isPaused = true
                }
            },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).statusBarsPadding()
        ) {
            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(32.dp))
        }

        if (showFps) {
            Text("FPS: $fps", modifier = Modifier.align(Alignment.TopStart).padding(16.dp).statusBarsPadding(),
                color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

private fun DrawScope.drawGameBackground(engine: GameEngine, size: Size) {
    drawRect(color = Color(engine.getSkyColor()), size = size)

    val starCount = if (engine.isNight) 60 else 0
    if (starCount > 0) {
        val starAlpha = (sin(engine.getScrollOffset() * 0.008f) * 0.3f + 0.7f).toFloat()
        for (i in 0 until starCount) {
            val sx = (i * 137.5f + engine.getScrollOffset() * 0.015f) % size.width
            val sy = (i * 73.1f) % (size.height * 0.45f)
            drawCircle(Color.White.copy(alpha = starAlpha * (0.4f + (i % 5) * 0.12f)), 1f + (i % 3).toFloat(), Offset(sx, sy))
        }
    }

    val fogAlpha = engine.getFogAlpha()
    if (fogAlpha > 0f) drawRect(Color(0xFF576574).copy(alpha = fogAlpha), size = size)
}

private fun DrawScope.drawClouds(engine: GameEngine, size: Size) {
    val cloudAlpha = engine.getCloudAlpha()
    for (cloud in engine.getClouds()) {
        val cx = cloud.x; val cy = cloud.y; val cw = cloud.width; val ch = cloud.height
        drawOval(Color(0xFFC8D6E5).copy(alpha = cloudAlpha * cloud.alpha), Offset(cx - cw / 2, cy), Size(cw, ch))
        drawOval(Color(0xFFC8D6E5).copy(alpha = cloudAlpha * cloud.alpha * 0.7f), Offset(cx - cw / 3, cy - ch * 0.3f), Size(cw * 0.7f, ch * 0.8f))
        drawOval(Color(0xFFC8D6E5).copy(alpha = cloudAlpha * cloud.alpha * 0.5f), Offset(cx + cw * 0.1f, cy - ch * 0.1f), Size(cw * 0.5f, ch * 0.6f))
    }
}

private fun DrawScope.drawPipes(engine: GameEngine, size: Size) {
    val pipeColor = Color(engine.getPipeColor())
    val pipeHighlight = Color(engine.getPipeHighlightColor())
    val pw = engine.getPipeWidth()
    val capH = 28f; val capW = pw + 12f

    for (pipe in engine.getPipes()) {
        if (pipe.destroyed) {
            val alpha = (pipe.destroyTimer / 20f).coerceIn(0f, 1f)
            val debrisColor = Color(0xFFFF8C00).copy(alpha = alpha)
            for (j in 0 until 6) {
                val dx = (j * 17f - 40f) * alpha
                val dy = (j * 13f - 30f) * alpha
                drawCircle(debrisColor, 5f * alpha, Offset(pipe.x + pw / 2 + dx, pipe.topHeight + dy))
                drawCircle(debrisColor, 4f * alpha, Offset(pipe.x + pw / 2 + dx + 10f, pipe.bottomY + dy))
            }
            continue
        }

        drawRect(pipeColor, Offset(pipe.x, 0f), Size(pw, pipe.topHeight - capH))
        drawRect(pipeHighlight, Offset(pipe.x - 6f, pipe.topHeight - capH), Size(capW, capH))
        drawRect(pipeColor, Offset(pipe.x, pipe.bottomY + capH), Size(pw, size.height - pipe.bottomY - capH))
        drawRect(pipeHighlight, Offset(pipe.x - 6f, pipe.bottomY), Size(capW, capH))

        drawRect(pipeHighlight.copy(alpha = 0.25f), Offset(pipe.x + 4f, 0f), Size(6f, pipe.topHeight - capH))
        drawRect(pipeHighlight.copy(alpha = 0.25f), Offset(pipe.x + 4f, pipe.bottomY + capH), Size(6f, size.height - pipe.bottomY - capH))
    }
}

private fun DrawScope.drawCoins(engine: GameEngine, size: Size) {
    for (coin in engine.getCoinItems()) {
        if (coin.collected) continue
        val rotation = (engine.getScrollOffset() * 0.06f) % 360f
        val scaleX = abs(cos(rotation * PI / 180f)).toFloat().coerceAtLeast(0.15f)
        val r = coin.scale * 16f
        drawOval(Color(0xFFFFD700).copy(alpha = coin.alpha), Offset(coin.x - r * scaleX, coin.y - r), Size(r * 2 * scaleX, r * 2))
        drawOval(Color(0xFFFFA500).copy(alpha = coin.alpha * 0.4f), Offset(coin.x - r * 0.6f * scaleX, coin.y - r * 0.6f), Size(r * 1.2f * scaleX, r * 1.2f))
    }
}

private fun DrawScope.drawProjectiles(engine: GameEngine, size: Size) {
    for (proj in engine.getProjectiles()) {
        val alpha = (proj.life / 60f).coerceIn(0f, 1f)
        val color = Color(proj.color).copy(alpha = alpha)
        drawCircle(color, proj.radius, Offset(proj.x, proj.y))
        drawCircle(Color.White.copy(alpha = alpha * 0.6f), proj.radius * 0.4f, Offset(proj.x, proj.y))
        drawLine(color.copy(alpha = alpha * 0.3f), Offset(proj.x - proj.radius * 3, proj.y), Offset(proj.x, proj.y), strokeWidth = proj.radius * 0.8f)
    }
}

private fun DrawScope.drawGround(engine: GameEngine, size: Size) {
    val groundColor = Color(engine.getGroundColor())
    val groundY = size.height - engine.getGroundHeight()
    val scrollOffset = engine.getScrollOffset()

    drawRect(groundColor, Offset(0f, groundY), Size(size.width, engine.getGroundHeight()))

    val stripeColor = groundColor.copy(alpha = 0.4f)
    val stripeW = 35f; val stripeSpacing = 70f
    var sx = -(scrollOffset % stripeSpacing)
    while (sx < size.width) {
        drawRect(stripeColor, Offset(sx, groundY + 8f), Size(stripeW, 6f))
        sx += stripeSpacing
    }

    drawRect(Color(0xFF1B3D1E).copy(alpha = 0.7f), Offset(0f, groundY), Size(size.width, 3f))
}

private fun DrawScope.drawBird(engine: GameEngine, size: Size) {
    val birdColor = Color(android.graphics.Color.parseColor(engine.birdColor))
    val bx = engine.birdX; val by = engine.birdY; val r = engine.birdRadius
    val rotation = engine.birdRotation

    rotate(rotation, pivot = Offset(bx, by)) {
        drawCircle(birdColor, r, Offset(bx, by))

        drawCircle(Color.White.copy(alpha = 0.9f), r * 0.22f, Offset(bx + r * 0.35f, by - r * 0.25f))
        drawCircle(Color.Black, r * 0.1f, Offset(bx + r * 0.4f, by - r * 0.25f))

        val wingFlap = if (engine.gameStarted && !engine.isGameOver) sin(engine.getScrollOffset() * 0.35f).toFloat() * 7f else 0f
        drawOval(birdColor.copy(alpha = 0.85f), Offset(bx - r * 0.15f, by - r * 0.55f + wingFlap), Size(r * 1.3f, r * 0.55f))

        val beakColor = Color(0xFFFFA500)
        val path = Path().apply {
            moveTo(bx + r * 0.75f, by - r * 0.12f)
            lineTo(bx + r * 1.25f, by)
            lineTo(bx + r * 0.75f, by + r * 0.12f)
            close()
        }
        drawPath(path, beakColor)

        val tailColor = birdColor.copy(alpha = 0.7f)
        val tailPath = Path().apply {
            moveTo(bx - r * 0.8f, by - r * 0.3f)
            lineTo(bx - r * 1.3f, by - r * 0.6f)
            lineTo(bx - r * 1.1f, by)
            lineTo(bx - r * 1.3f, by + r * 0.4f)
            lineTo(bx - r * 0.8f, by + r * 0.2f)
            close()
        }
        drawPath(tailPath, tailColor)
    }
}

private fun DrawScope.drawParticles(engine: GameEngine, size: Size) {
    for (p in engine.getParticles()) {
        val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
        drawCircle(Color(p.color).copy(alpha = alpha), p.size * alpha, Offset(p.x, p.y))
    }
}

private fun DrawScope.drawWeatherEffects(engine: GameEngine, size: Size) {
    val rainAlpha = engine.getRainAlpha()
    if (rainAlpha > 0f) {
        val rainColor = Color(0xFF8395A7).copy(alpha = rainAlpha)
        val so = engine.getScrollOffset()
        for (i in 0 until 35) {
            val rx = (i * 97.3f + so * 0.4f) % size.width
            val ry = (i * 53.7f + so * 1.1f) % size.height
            drawLine(rainColor, Offset(rx, ry), Offset(rx - 2f, ry + 12f), 1.2f)
        }
    }
}

private fun DrawScope.drawHUD(engine: GameEngine, size: Size) {
    if (!engine.gameStarted && !engine.isGameOver) return

    val scale = size.width / 1080f

    val scorePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE; textSize = 56f * scale
        textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
        setShadowLayer(3f, 0f, 2f, android.graphics.Color.BLACK)
    }
    drawContext.canvas.nativeCanvas.drawText("${engine.score}", size.width / 2, 100f * scale, scorePaint)

    val levelPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(255, 215, 0); textSize = 22f * scale
        textAlign = android.graphics.Paint.Align.LEFT; isAntiAlias = true
        setShadowLayer(2f, 0f, 1f, android.graphics.Color.BLACK)
    }
    drawContext.canvas.nativeCanvas.drawText("Lv.${engine.getLevel()}", 16f * scale, 60f * scale, levelPaint)

    if (engine.coins > 0) {
        val coinPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(255, 215, 0); textSize = 24f * scale
            textAlign = android.graphics.Paint.Align.LEFT; isAntiAlias = true
            setShadowLayer(2f, 0f, 1f, android.graphics.Color.BLACK)
        }
        drawContext.canvas.nativeCanvas.drawText("${engine.coins}", 16f * scale, 90f * scale, coinPaint)
    }
}

private fun DrawScope.drawStartPrompt(engine: GameEngine, size: Size) {
    val scale = size.width / 1080f
    val alpha = (sin(engine.getScrollOffset() * 0.015f) * 0.3f + 0.7f).toFloat()

    val promptPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE; textSize = 28f * scale
        textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
        setShadowLayer(2f, 0f, 1f, android.graphics.Color.BLACK)
        this.alpha = (alpha * 255).toInt()
    }
    drawContext.canvas.nativeCanvas.drawText("Tap to Flap", size.width / 2, size.height * 0.5f, promptPaint)

    val modePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(88, 166, 255); textSize = 18f * scale
        textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
        this.alpha = (alpha * 200).toInt()
    }
    val modeName = when (engine.gameMode) { "endless" -> "Classic"; "challenge" -> "Challenge"; "practice" -> "Practice"; else -> engine.gameMode }
    drawContext.canvas.nativeCanvas.drawText("$modeName Mode", size.width / 2, size.height * 0.55f, modePaint)

    if (engine.weaponAmmo > 0) {
        val weaponPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(255, 80, 50); textSize = 14f * scale
            textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
            this.alpha = (alpha * 180).toInt()
        }
        drawContext.canvas.nativeCanvas.drawText("Weapon: ${engine.weaponType.uppercase()} (${engine.weaponAmmo} shots)", size.width / 2, size.height * 0.6f, weaponPaint)
    }
}

@Composable
private fun PauseOverlay(onResume: () -> Unit, onRestart: () -> Unit, onMainMenu: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
        Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)), modifier = Modifier.padding(32.dp)) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Paused", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(24.dp))
                Button(onResume, Modifier.fillMaxWidth().height(52.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3FB950))) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Resume", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Button(onRestart, Modifier.fillMaxWidth().height(52.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF58A6FF))) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Restart", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onMainMenu, Modifier.fillMaxWidth().height(52.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Home, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Main Menu", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GameOverOverlay(score: Int, bestScore: Int, coins: Int, level: Int, onRestart: () -> Unit, onMainMenu: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
        Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)), modifier = Modifier.padding(32.dp)) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Game Over", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF85149))
                Spacer(Modifier.height(16.dp))
                Text("Score", fontSize = 14.sp, color = Color(0xFF8B949E))
                Text("$score", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text("Best: $bestScore", fontSize = 16.sp, color = Color(0xFFD29922), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text("Level $level", fontSize = 14.sp, color = Color(0xFF58A6FF))
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MonetizationOn, null, tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+$coins", fontSize = 16.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
                Button(onRestart, Modifier.fillMaxWidth().height(52.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3FB950))) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Play Again", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onMainMenu, Modifier.fillMaxWidth().height(52.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Home, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Main Menu", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
