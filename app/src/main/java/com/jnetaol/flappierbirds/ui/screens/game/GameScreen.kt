package com.jnetaol.flappierbirds.ui.screens.game

import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import androidx.compose.ui.platform.LocalDensity
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
    val density = LocalDensity.current
    val engine = remember { GameEngine() }
    var screenWidth by remember { mutableFloatStateOf(1080f) }
    var screenHeight by remember { mutableFloatStateOf(1920f) }
    var frameCount by remember { mutableIntStateOf(0) }
    var fps by remember { mutableIntStateOf(0) }
    var fpsTimer by remember { mutableLongStateOf(0L) }
    var showPauseMenu by remember { mutableStateOf(false) }
    var gameEndRecorded by remember { mutableStateOf(false) }

    val vibrator = remember {
        if (hapticEnabled) {
            try {
                val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } catch (_: Exception) {
                context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } else null
    }

    LaunchedEffect(Unit) {
        engine.gameMode = gameMode
        engine.init(screenWidth, screenHeight)
        DebugLogger.i("FB-200", "Game started: mode=$gameMode", null)

        while (isActive) {
            if (!engine.isPaused && !engine.isGameOver && engine.gameStarted) {
                engine.update()
                engine.increaseDifficulty()
            }

            frameCount++
            val now = System.currentTimeMillis()
            if (now - fpsTimer >= 1000) {
                fps = frameCount
                frameCount = 0
                fpsTimer = now
            }

            if (engine.isGameOver && !gameEndRecorded) {
                gameEndRecorded = true
                val duration = engine.getSessionDurationMs()
                DebugLogger.i("FB-201", "Game over: score=${engine.score} coins=${engine.coins} duration=${duration}ms", null)
                launch(Dispatchers.IO) {
                    repository.recordGameEnd(
                        mode = gameMode,
                        score = engine.score,
                        coins = engine.coins,
                        flaps = engine.flaps,
                        obstaclesPassed = engine.obstaclesPassed,
                        sessionDurationMs = duration
                    )
                }
                onGameEnd(engine.score, engine.coins, engine.flaps, engine.obstaclesPassed, duration)
            }

            delay(16L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
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
                                try {
                                    vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                                } catch (_: Exception) {}
                            }
                        }
                    }
                )
            }
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            screenWidth = size.width
            screenHeight = size.height
            if (!engine.gameStarted && !engine.isGameOver) {
                engine.init(screenWidth, screenHeight)
            }

            drawGameBackground(engine, size)
            drawClouds(engine, size)
            drawPipes(engine, size)
            drawCoins(engine, size)
            drawGround(engine, size)
            drawBird(engine, size)
            drawParticles(engine, size)
            drawWeatherEffects(engine, size)
            drawHUD(engine, size)

            if (!engine.gameStarted && !engine.isGameOver) {
                drawStartPrompt(engine, size)
            }
        }

        if (showPauseMenu) {
            PauseOverlay(
                onResume = {
                    showPauseMenu = false
                    engine.isPaused = false
                },
                onRestart = {
                    showPauseMenu = false
                    engine.resetGame()
                    gameEndRecorded = false
                },
                onMainMenu = onNavigateBack
            )
        }

        if (engine.isGameOver) {
            GameOverOverlay(
                score = engine.score,
                bestScore = 0,
                coins = engine.coins,
                onRestart = {
                    engine.resetGame()
                    gameEndRecorded = false
                },
                onMainMenu = onNavigateBack
            )
        }

        IconButton(
            onClick = {
                if (!engine.isGameOver && engine.gameStarted) {
                    showPauseMenu = true
                    engine.isPaused = true
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .statusBarsPadding()
        ) {
            Icon(
                Icons.Default.Pause,
                contentDescription = "Pause",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(32.dp)
            )
        }

        if (showFps) {
            Text(
                "FPS: $fps",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .statusBarsPadding(),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun DrawScope.drawGameBackground(engine: GameEngine, size: Size) {
    val skyColor = engine.getSkyColor()
    drawRect(color = Color(skyColor), size = size)

    val starCount = if (engine.isNight) 80 else 0
    if (starCount > 0) {
        val starAlpha = (sin(engine.getScrollOffset() * 0.01f) * 0.3f + 0.7f).toFloat()
        for (i in 0 until starCount) {
            val sx = (i * 137.5f + engine.getScrollOffset() * 0.02f) % size.width
            val sy = (i * 73.1f) % (size.height * 0.5f)
            val starSize = 1f + (i % 3).toFloat()
            drawCircle(
                color = Color.White.copy(alpha = starAlpha * (0.5f + (i % 5) * 0.1f)),
                radius = starSize,
                center = Offset(sx, sy)
            )
        }
    }

    val fogAlpha = engine.getFogAlpha()
    if (fogAlpha > 0f) {
        drawRect(
            color = Color(0xFF576574).copy(alpha = fogAlpha),
            size = size
        )
    }
}

private fun DrawScope.drawClouds(engine: GameEngine, size: Size) {
    val cloudAlpha = engine.getCloudAlpha()
    for (cloud in engine.getClouds()) {
        val cx = cloud.x
        val cy = cloud.y
        val cw = cloud.width
        val ch = cloud.height

        drawOval(
            color = Color(0xFFC8D6E5).copy(alpha = cloudAlpha * cloud.alpha),
            topLeft = Offset(cx - cw / 2, cy),
            size = Size(cw, ch)
        )
        drawOval(
            color = Color(0xFFC8D6E5).copy(alpha = cloudAlpha * cloud.alpha * 0.8f),
            topLeft = Offset(cx - cw / 3, cy - ch * 0.3f),
            size = Size(cw * 0.7f, ch * 0.8f)
        )
        drawOval(
            color = Color(0xFFC8D6E5).copy(alpha = cloudAlpha * cloud.alpha * 0.6f),
            topLeft = Offset(cx + cw * 0.1f, cy - ch * 0.1f),
            size = Size(cw * 0.5f, ch * 0.6f)
        )
    }
}

private fun DrawScope.drawPipes(engine: GameEngine, size: Size) {
    val pipeColor = Color(engine.getPipeColor())
    val pipeHighlight = Color(engine.getPipeHighlightColor())
    val pipeWidth = 80f
    val capHeight = 30f
    val capWidth = 90f

    for (pipe in engine.getPipes()) {
        drawRect(
            color = pipeColor,
            topLeft = Offset(pipe.x, 0f),
            size = Size(pipeWidth, pipe.topHeight - capHeight)
        )
        drawRect(
            color = pipeHighlight,
            topLeft = Offset(pipe.x - 5f, pipe.topHeight - capHeight),
            size = Size(capWidth, capHeight)
        )
        drawRect(
            color = pipeColor,
            topLeft = Offset(pipe.x, pipe.bottomY + capHeight),
            size = Size(pipeWidth, size.height - pipe.bottomY - capHeight)
        )
        drawRect(
            color = pipeHighlight,
            topLeft = Offset(pipe.x - 5f, pipe.bottomY),
            size = Size(capWidth, capHeight)
        )

        drawRect(
            color = pipeHighlight.copy(alpha = 0.3f),
            topLeft = Offset(pipe.x + 5f, 0f),
            size = Size(8f, pipe.topHeight - capHeight)
        )
        drawRect(
            color = pipeHighlight.copy(alpha = 0.3f),
            topLeft = Offset(pipe.x + 5f, pipe.bottomY + capHeight),
            size = Size(8f, size.height - pipe.bottomY - capHeight)
        )
    }
}

private fun DrawScope.drawCoins(engine: GameEngine, size: Size) {
    for (coin in engine.getCoinItems()) {
        if (coin.collected) continue
        val rotation = (engine.getScrollOffset() * 0.05f) % 360f
        val scaleX = abs(cos(rotation * PI / 180f)).toFloat().coerceAtLeast(0.2f)

        drawOval(
            color = Color(0xFFFFD700).copy(alpha = coin.alpha),
            topLeft = Offset(coin.x - coin.scale * 18f * scaleX, coin.y - coin.scale * 18f),
            size = Size(coin.scale * 36f * scaleX, coin.scale * 36f)
        )
        drawOval(
            color = Color(0xFFFFA500).copy(alpha = coin.alpha * 0.5f),
            topLeft = Offset(coin.x - coin.scale * 12f * scaleX, coin.y - coin.scale * 12f),
            size = Size(coin.scale * 24f * scaleX, coin.scale * 24f)
        )
    }
}

private fun DrawScope.drawGround(engine: GameEngine, size: Size) {
    val groundColor = Color(engine.getGroundColor())
    val groundY = size.height - engine.getGroundHeight()
    val scrollOffset = engine.getScrollOffset()

    drawRect(
        color = groundColor,
        topLeft = Offset(0f, groundY),
        size = Size(size.width, engine.getGroundHeight())
    )

    val stripeColor = groundColor.copy(alpha = 0.5f)
    val stripeWidth = 40f
    val stripeSpacing = 80f
    var sx = -(scrollOffset % stripeSpacing)
    while (sx < size.width) {
        drawRect(
            color = stripeColor,
            topLeft = Offset(sx, groundY + 10f),
            size = Size(stripeWidth, 8f)
        )
        sx += stripeSpacing
    }

    drawRect(
        color = Color(0xFF1B3D1E).copy(alpha = 0.8f),
        topLeft = Offset(0f, groundY),
        size = Size(size.width, 4f)
    )
}

private fun DrawScope.drawBird(engine: GameEngine, size: Size) {
    val birdColor = Color(android.graphics.Color.parseColor(engine.birdColor))
    val bx = engine.birdX
    val by = engine.birdY
    val radius = engine.birdRadius
    val rotation = engine.birdRotation

    rotate(rotation, pivot = Offset(bx, by)) {
        drawCircle(color = birdColor, radius = radius, center = Offset(bx, by))

        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = radius * 0.25f,
            center = Offset(bx + radius * 0.3f, by - radius * 0.2f)
        )
        drawCircle(
            color = Color.Black,
            radius = radius * 0.12f,
            center = Offset(bx + radius * 0.35f, by - radius * 0.2f)
        )

        val wingFlap = if (engine.gameStarted && !engine.isGameOver) {
            sin(engine.getScrollOffset() * 0.3f).toFloat() * 8f
        } else 0f

        drawOval(
            color = birdColor.copy(alpha = 0.8f),
            topLeft = Offset(bx - radius * 0.1f, by - radius * 0.5f + wingFlap),
            size = Size(radius * 1.2f, radius * 0.6f)
        )

        val beakColor = Color(0xFFFFA500)
        val path = Path().apply {
            moveTo(bx + radius * 0.8f, by - radius * 0.1f)
            lineTo(bx + radius * 1.3f, by)
            lineTo(bx + radius * 0.8f, by + radius * 0.1f)
            close()
        }
        drawPath(path, beakColor)
    }
}

private fun DrawScope.drawParticles(engine: GameEngine, size: Size) {
    for (p in engine.getParticles()) {
        val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
        val particleColor = Color(p.color).copy(alpha = alpha)
        drawCircle(
            color = particleColor,
            radius = p.size * alpha,
            center = Offset(p.x, p.y)
        )
    }
}

private fun DrawScope.drawWeatherEffects(engine: GameEngine, size: Size) {
    val rainAlpha = engine.getRainAlpha()
    if (rainAlpha > 0f) {
        val rainColor = Color(0xFF8395A7).copy(alpha = rainAlpha)
        val scrollOffset = engine.getScrollOffset()
        for (i in 0 until 40) {
            val rx = (i * 97.3f + scrollOffset * 0.5f) % size.width
            val ry = (i * 53.7f + scrollOffset * 1.2f) % size.height
            drawLine(
                color = rainColor,
                start = Offset(rx, ry),
                end = Offset(rx - 3f, ry + 15f),
                strokeWidth = 1.5f
            )
        }
    }
}

private fun DrawScope.drawHUD(engine: GameEngine, size: Size) {
    if (!engine.gameStarted && !engine.isGameOver) return

    val scoreText = "${engine.score}"
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 64f * size.width / 1080f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
    }

    drawContext.canvas.nativeCanvas.drawText(
        scoreText,
        size.width / 2,
        120f * size.width / 1080f,
        textPaint
    )

    if (engine.coins > 0) {
        val coinPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(255, 215, 0)
            textSize = 28f * size.width / 1080f
            textAlign = android.graphics.Paint.Align.LEFT
            isAntiAlias = true
            setShadowLayer(2f, 0f, 1f, android.graphics.Color.BLACK)
        }
        drawContext.canvas.nativeCanvas.drawText(
            "Coins: ${engine.coins}",
            20f * size.width / 1080f,
            80f * size.width / 1080f,
            coinPaint
        )
    }
}

private fun DrawScope.drawStartPrompt(engine: GameEngine, size: Size) {
    val promptPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 32f * size.width / 1080f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
    }

    val alpha = (sin(engine.getScrollOffset() * 0.02f) * 0.3f + 0.7f).toFloat()
    promptPaint.alpha = (alpha * 255).toInt()

    drawContext.canvas.nativeCanvas.drawText(
        "Tap to Start",
        size.width / 2,
        size.height * 0.55f,
        promptPaint
    )

    val modePaintAlpha = (alpha * 255).toInt()
    val modePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(88, 166, 255)
        textSize = 20f * size.width / 1080f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        this.alpha = modePaintAlpha
    }
    drawContext.canvas.nativeCanvas.drawText(
        "Mode: ${engine.gameMode.replaceFirstChar { it.uppercase() }}",
        size.width / 2,
        size.height * 0.6f,
        modePaint
    )
}

@Composable
private fun PauseOverlay(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onMainMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Paused", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3FB950))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Resume", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF58A6FF))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Restart", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onMainMenu,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Main Menu", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GameOverOverlay(
    score: Int,
    bestScore: Int,
    coins: Int,
    onRestart: () -> Unit,
    onMainMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Game Over", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF85149))
                Spacer(Modifier.height(20.dp))
                Text("Score", fontSize = 14.sp, color = Color(0xFF8B949E))
                Text("$score", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text("Best: $bestScore", fontSize = 16.sp, color = Color(0xFFD29922))
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+$coins", fontSize = 16.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3FB950))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Play Again", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onMainMenu,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Main Menu", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
