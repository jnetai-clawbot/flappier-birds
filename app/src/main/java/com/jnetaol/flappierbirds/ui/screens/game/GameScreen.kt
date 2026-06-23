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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jnetaol.flappierbirds.data.repository.GameRepository
import com.jnetaol.flappierbirds.engine.GameEngine
import com.jnetaol.flappierbirds.engine.GameViewport
import com.jnetaol.flappierbirds.engine.SoundManager
import com.jnetaol.flappierbirds.logger.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

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
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var showPauseMenu by remember { mutableStateOf(false) }
    var gameEndRecorded by remember { mutableStateOf(false) }

    val viewport = remember(canvasSize) {
        GameViewport.from(
            canvasSize.width.toFloat(),
            canvasSize.height.toFloat()
        )
    }

    val vibrator = remember {
        try {
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
        } catch (_: Exception) {
            null
        }
    }

    LaunchedEffect(canvasSize, gameMode) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return@LaunchedEffect

        engine.gameMode = gameMode
        engine.init()
        DebugLogger.i(
            "FB-200",
            "Game started: mode=$gameMode viewport=${canvasSize.width}x${canvasSize.height} scale=${viewport.scale}",
            null
        )

        gameEndRecorded = false
        while (isActive) {
            withFrameNanos {
                engine.update()
                frameTick++
            }

            if (engine.isGameOver && !gameEndRecorded) {
                gameEndRecorded = true
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
                    } catch (_: Exception) {
                    }
                }
                onGameEnd(engine.score, finalCoins, finalFlaps, finalObstacles, finalDurationMs)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF3A9FB0))
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
                            try {
                                vibrator?.vibrate(
                                    VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE)
                                )
                            } catch (_: Exception) {
                            }
                        }
                    }
                })
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
        ) {
            val tick = frameTick
            if (tick < 0 || size.width <= 0f || size.height <= 0f) return@Canvas

            try {
                drawRect(Color(0xFF3A9FB0), topLeft = Offset.Zero, size = size)

                withTransform({
                    translate(viewport.offsetX, viewport.offsetY)
                    scale(viewport.scale, viewport.scale, pivot = Offset.Zero)
                }) {
                    val virtual = viewport.virtualSize
                    drawClassicBackground(virtual, engine.getGroundScroll())
                    drawPipes(engine, virtual)
                    drawGround(engine, virtual)
                    drawBird(engine)
                    drawParticles(engine, graphicsQuality)
                }

                drawScore(engine, viewport)
                if (showFps) drawFps(tick, viewport)
                if (!engine.gameStarted && !engine.isGameOver) {
                    drawStartPrompt(engine, viewport)
                }
            } catch (e: Exception) {
                DebugLogger.logException("FB-220", "Canvas draw error", e)
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
                onRestart = { engine.resetGame(); gameEndRecorded = false },
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
                .padding(12.dp)
                .statusBarsPadding()
        ) {
            Icon(
                Icons.Default.Pause,
                contentDescription = "Pause",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

private fun DrawScope.drawClassicBackground(size: Size, scroll: Float) {
    drawRect(Color(0xFF4EC0CA), topLeft = Offset.Zero, size = size)

    val skylineY = size.height * 0.62f
    val buildingWidth = 42f
    val gap = 18f
    var x = -(scroll * 0.25f % (buildingWidth + gap))

    while (x < size.width + buildingWidth) {
        val height = 70f + ((x / buildingWidth).toInt() % 4) * 28f
        drawRect(
            color = Color(0xFF73BF2E),
            topLeft = Offset(x, skylineY - height),
            size = Size(buildingWidth, height)
        )
        x += buildingWidth + gap
    }

    val cloudColor = Color.White.copy(alpha = 0.85f)
    val cloudY1 = size.height * 0.18f
    val cloudY2 = size.height * 0.30f
    val cloudSpacing = 180f
    var cloudX = -(scroll * 0.12f % cloudSpacing)

    while (cloudX < size.width + cloudSpacing) {
        drawCircle(cloudColor, 18f, Offset(cloudX, cloudY1))
        drawCircle(cloudColor, 24f, Offset(cloudX + 20f, cloudY1))
        drawCircle(cloudColor, 16f, Offset(cloudX + 40f, cloudY1))

        drawCircle(cloudColor, 14f, Offset(cloudX + 60f, cloudY2))
        drawCircle(cloudColor, 20f, Offset(cloudX + 82f, cloudY2))
        drawCircle(cloudColor, 12f, Offset(cloudX + 100f, cloudY2))

        cloudX += cloudSpacing
    }
}

private fun DrawScope.drawPipes(engine: GameEngine, size: Size) {
    val body = Color(0xFF73BF2E)
    val cap = Color(0xFF558B2F)
    val lip = Color(0xFF6DAF3A)
    val pipeWidth = engine.getPipeWidth()
    val capHeight = 26f
    val capOverhang = 6f
    val lipHeight = 4f

    for (pipe in engine.getPipes()) {
        val topBodyHeight = (pipe.topHeight - capHeight - lipHeight).coerceAtLeast(0f)
        drawRect(color = body, topLeft = Offset(pipe.x, 0f), size = Size(pipeWidth, topBodyHeight))
        drawRect(
            color = cap,
            topLeft = Offset(pipe.x - capOverhang, pipe.topHeight - capHeight - lipHeight),
            size = Size(pipeWidth + capOverhang * 2f, capHeight)
        )
        drawRect(
            color = lip,
            topLeft = Offset(pipe.x - capOverhang - 2f, pipe.topHeight - lipHeight),
            size = Size(pipeWidth + capOverhang * 2f + 4f, lipHeight)
        )

        val bottomTop = pipe.bottomY + lipHeight
        val bottomBodyHeight = (size.height - engine.getGroundHeight() - bottomTop).coerceAtLeast(0f)
        drawRect(color = body, topLeft = Offset(pipe.x, bottomTop), size = Size(pipeWidth, bottomBodyHeight))
        drawRect(
            color = cap,
            topLeft = Offset(pipe.x - capOverhang, pipe.bottomY),
            size = Size(pipeWidth + capOverhang * 2f, capHeight)
        )
        drawRect(
            color = lip,
            topLeft = Offset(pipe.x - capOverhang - 2f, pipe.bottomY + capHeight),
            size = Size(pipeWidth + capOverhang * 2f + 4f, lipHeight)
        )
    }
}

private fun DrawScope.drawGround(engine: GameEngine, size: Size) {
    val groundHeight = engine.getGroundHeight()
    val groundTop = size.height - groundHeight
    val scroll = engine.getGroundScroll()

    drawRect(Color(0xFFDED895), topLeft = Offset(0f, groundTop), size = Size(size.width, groundHeight))
    drawRect(Color(0xFF73BF2E), topLeft = Offset(0f, groundTop), size = Size(size.width, 14f))
    drawRect(
        Color(0xFF5CBF2E).copy(alpha = 0.5f),
        topLeft = Offset(0f, groundTop),
        size = Size(size.width, 3f)
    )

    val stripeWidth = 24f
    val stripeSpacing = 48f
    var stripeX = -(scroll % stripeSpacing)
    while (stripeX < size.width + stripeSpacing) {
        drawRect(
            color = Color(0xFFCBB968),
            topLeft = Offset(stripeX, groundTop + 18f),
            size = Size(stripeWidth, 5f)
        )
        stripeX += stripeSpacing
    }
}

private fun DrawScope.drawBird(engine: GameEngine) {
    val birdX = engine.birdX
    val birdY = engine.birdY
    val radius = engine.birdRadius
    val wingOffset = if (engine.gameStarted && !engine.isGameOver) {
        sin(engine.getWingPhase()).toFloat() * 6f
    } else {
        0f
    }

    rotate(engine.birdRotation, pivot = Offset(birdX, birdY)) {
        drawOval(
            color = Color(0xFFF7DC6F),
            topLeft = Offset(birdX - radius, birdY - radius * 0.85f),
            size = Size(radius * 2f, radius * 1.7f)
        )

        drawOval(
            color = Color(0xFFE8C547),
            topLeft = Offset(birdX - radius * 0.55f, birdY - radius * 0.2f + wingOffset),
            size = Size(radius * 1.1f, radius * 0.55f)
        )

        drawCircle(
            color = Color.White,
            radius = radius * 0.34f,
            center = Offset(birdX + radius * 0.25f, birdY - radius * 0.2f)
        )
        drawCircle(
            color = Color.Black,
            radius = radius * 0.16f,
            center = Offset(birdX + radius * 0.32f, birdY - radius * 0.2f)
        )

        val beak = Path().apply {
            moveTo(birdX + radius * 0.55f, birdY - radius * 0.05f)
            lineTo(birdX + radius * 1.15f, birdY + radius * 0.05f)
            lineTo(birdX + radius * 0.55f, birdY + radius * 0.18f)
            close()
        }
        drawPath(beak, Color(0xFFFF8C00))

        drawOval(
            color = Color(0xFFE8C547),
            topLeft = Offset(birdX - radius * 0.95f, birdY + radius * 0.05f),
            size = Size(radius * 0.45f, radius * 0.25f)
        )
    }
}

private fun DrawScope.drawParticles(engine: GameEngine, graphicsQuality: String) {
    val maxParticles = when (graphicsQuality) {
        "low" -> 8
        "medium" -> 16
        else -> 32
    }

    for (particle in engine.getParticles().take(maxParticles)) {
        val alpha = (particle.life / particle.maxLife).coerceIn(0f, 1f)
        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.8f),
            radius = particle.size * alpha,
            center = Offset(particle.x, particle.y)
        )
    }
}

private fun DrawScope.drawScore(engine: GameEngine, viewport: GameViewport) {
    if (!engine.gameStarted && !engine.isGameOver) return

    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = viewport.textSize(52f)
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
        setShadowLayer(4f, 2f, 2f, android.graphics.Color.argb(120, 0, 0, 0))
    }
    drawContext.canvas.nativeCanvas.drawText(
        engine.score.toString(),
        viewport.toScreenX(viewport.virtualWidth / 2f),
        viewport.toScreenY(72f),
        paint
    )
}

private fun DrawScope.drawStartPrompt(engine: GameEngine, viewport: GameViewport) {
    val pulse = (sin(engine.getGroundScroll() * 0.05f) * 0.25f + 0.75f).coerceIn(0f, 1f)

    val titlePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = viewport.textSize(42f)
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
        setShadowLayer(4f, 2f, 2f, android.graphics.Color.argb(140, 0, 0, 0))
    }

    val tapPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = viewport.textSize(24f)
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        alpha = (pulse * 255).toInt()
        setShadowLayer(3f, 1f, 1f, android.graphics.Color.argb(120, 0, 0, 0))
    }

    val centerX = viewport.toScreenX(viewport.virtualWidth / 2f)
    drawContext.canvas.nativeCanvas.drawText("Get Ready", centerX, viewport.toScreenY(132f), titlePaint)
    drawContext.canvas.nativeCanvas.drawText("Tap to Flap", centerX, viewport.toScreenY(170f), tapPaint)
}

private fun DrawScope.drawFps(frameTick: Int, viewport: GameViewport) {
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = viewport.textSize(14f)
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(
        "frame $frameTick",
        viewport.offsetX + 12f,
        viewport.screenHeight - 16f,
        paint
    )
}

@Composable
private fun PauseOverlay(onResume: () -> Unit, onRestart: () -> Unit, onMainMenu: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 420.dp)
                .padding(horizontal = 16.dp)
        ) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Paused", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(24.dp))
                Button(
                    onResume,
                    Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3FB950))
                ) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Resume", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onRestart,
                    Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF58A6FF))
                ) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Restart", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onMainMenu,
                    Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Home, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Main Menu", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GameOverOverlay(score: Int, bestScore: Int, onRestart: () -> Unit, onMainMenu: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 420.dp)
                .padding(horizontal = 16.dp)
        ) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Game Over", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF85149))
                Spacer(Modifier.height(16.dp))
                Text("Score", fontSize = 14.sp, color = Color(0xFF8B949E))
                Text("$score", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text("Best: $bestScore", fontSize = 16.sp, color = Color(0xFFD29922), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(24.dp))
                Button(
                    onRestart,
                    Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3FB950))
                ) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Play Again", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onMainMenu,
                    Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Home, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Main Menu", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
