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
                    engine.update()
                    engine.increaseDifficulty()
                    frameCount++
                    val now = System.currentTimeMillis()
                    if (now - fpsTimer >= 1000) { fps = frameCount; frameCount = 0; fpsTimer = now }
                    if (engine.isGameOver && !gameEndRecorded) {
                        gameEndRecorded = true
                        val duration = engine.getSessionDurationMs()
                        DebugLogger.i("FB-201", "Game over: score=${engine.score}", null)
                        launch(Dispatchers.IO) {
                            try {
                                repository.recordGameEnd(
                                    mode = gameMode, score = engine.score, coins = engine.coins,
                                    flaps = engine.flaps, obstaclesPassed = engine.obstaclesPassed,
                                    sessionDurationMs = duration
                                )
                            } catch (e: Exception) {
                                DebugLogger.logException("FB-203", "Record game end failed", e)
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
        modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117))
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
                drawClouds(engine, size)
                drawPipes(engine, size)
                drawCoins(engine, size)
                drawProjectiles(engine, size)
                drawGround(engine, size)
                drawBird(engine, size)
                drawParticles(engine, size)
                drawWeather(engine, size)
                drawHUD(engine, size)
                if (!engine.gameStarted && !engine.isGameOver) drawStartPrompt(engine, size)
            } catch (e: Exception) {
                DebugLogger.logException("FB-220", "Canvas draw error", e)
            }
        }

        if (engine.weaponAmmo > 0 && engine.gameStarted && !engine.isGameOver) {
            FloatingActionButton(
                onClick = { engine.fireWeapon() },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp).navigationBarsPadding().size(56.dp),
                containerColor = Color(0xFFFF5722), contentColor = Color.White
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FlashOn, "Fire", Modifier.size(22.dp))
                    Text("${engine.weaponAmmo}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showPauseMenu) PauseOverlay(
            onResume = { showPauseMenu = false; engine.isPaused = false },
            onRestart = { showPauseMenu = false; engine.resetGame(); gameEndRecorded = false },
            onMainMenu = onNavigateBack
        )

        if (engine.isGameOver) GameOverOverlay(
            score = engine.score, bestScore = engine.bestScore, coins = engine.coins,
            level = engine.getLevel(),
            onRestart = { engine.resetGame(); gameEndRecorded = false },
            onMainMenu = onNavigateBack
        )

        IconButton(
            onClick = { if (!engine.isGameOver && engine.gameStarted) { showPauseMenu = true; engine.isPaused = true } },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).statusBarsPadding()
        ) { Icon(Icons.Default.Pause, "Pause", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(32.dp)) }

        if (showFps) Text("FPS: $fps",
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp).statusBarsPadding(),
            color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private fun DrawScope.drawSky(engine: GameEngine, size: Size) {
    drawRect(Color(engine.getSkyColor()), Offset.Zero, size)
    if (!engine.isNight) return
    val alpha = (sin(engine.getScrollOffset() * 0.005f) * 0.3f + 0.7f).toFloat()
    for (i in 0 until 40) {
        val sx = (i * 137.5f + engine.getScrollOffset() * 0.008f) % size.width
        val sy = (i * 73.1f) % (size.height * 0.35f)
        drawCircle(Color.White.copy(alpha = alpha * (0.25f + (i % 5) * 0.08f)), 1f + (i % 3).toFloat(), Offset(sx, sy))
    }
    val fog = engine.getFogAlpha()
    if (fog > 0f) drawRect(Color(0xFF576574).copy(alpha = fog), Offset.Zero, size)
}

private fun DrawScope.drawClouds(engine: GameEngine, size: Size) {
    val ca = engine.getCloudAlpha()
    for (c in engine.getClouds()) {
        val cx = c.x; val cy = c.y; val cw = c.width; val ch = c.height
        drawOval(Color(0xFFF5F8FA).copy(alpha = ca * c.alpha), Offset(cx - cw / 2, cy), Size(cw, ch))
        drawOval(Color(0xFFF5F8FA).copy(alpha = ca * c.alpha * 0.55f), Offset(cx - cw / 3, cy - ch * 0.3f), Size(cw * 0.65f, ch * 0.75f))
        drawOval(Color(0xFFF5F8FA).copy(alpha = ca * c.alpha * 0.35f), Offset(cx + cw * 0.1f, cy - ch * 0.1f), Size(cw * 0.45f, ch * 0.55f))
    }
}

private fun DrawScope.drawPipes(engine: GameEngine, size: Size) {
    val body = Color(engine.getPipeBodyColor())
    val cap = Color(engine.getPipeCapColor())
    val lip = Color(engine.getPipeLipColor())
    val pw = engine.getPipeWidth()
    val capH = 24f; val capW = pw + 16f; val lipH = 6f; val lipW = pw + 22f

    for (pipe in engine.getPipes()) {
        if (pipe.destroyed) {
            val a = (pipe.destroyTimer / 20f).coerceIn(0f, 1f)
            val dc = Color(0xFFFF8C00).copy(alpha = a)
            for (j in 0 until 4) {
                drawCircle(dc, 3.5f * a, Offset(pipe.x + pw / 2 + (j * 12f - 24f) * a, pipe.topHeight + (j * 9f - 18f) * a))
                drawCircle(dc, 2.5f * a, Offset(pipe.x + pw / 2 + (j * 12f - 24f + 6f) * a, pipe.bottomY + (j * 9f - 18f) * a))
            }
            continue
        }

        drawRect(body, Offset(pipe.x, 0f), Size(pw, pipe.topHeight - capH - lipH))
        drawRect(cap, Offset(pipe.x - 8f, pipe.topHeight - capH - lipH), Size(capW, capH))
        drawRect(lip, Offset(pipe.x - 11f, pipe.topHeight - lipH), Size(lipW, lipH))

        drawRect(body, Offset(pipe.x, pipe.bottomY + lipH), Size(pw, size.height - pipe.bottomY - lipH))
        drawRect(cap, Offset(pipe.x - 8f, pipe.bottomY), Size(capW, capH))
        drawRect(lip, Offset(pipe.x - 11f, pipe.bottomY + capH), Size(lipW, lipH))

        drawRect(body.copy(alpha = 0.15f), Offset(pipe.x + 3f, 0f), Size(4f, pipe.topHeight - capH - lipH))
        drawRect(body.copy(alpha = 0.15f), Offset(pipe.x + 3f, pipe.bottomY + lipH), Size(4f, size.height - pipe.bottomY - lipH))
    }
}

private fun DrawScope.drawCoins(engine: GameEngine, size: Size) {
    for (coin in engine.getCoinItems()) {
        if (coin.collected) continue
        val rot = (engine.getScrollOffset() * 0.06f) % 360f
        val sx = abs(cos(rot * PI / 180f)).toFloat().coerceAtLeast(0.12f)
        val r = coin.scale * 14f
        drawOval(Color(0xFFFFD700).copy(alpha = coin.alpha), Offset(coin.x - r * sx, coin.y - r), Size(r * 2 * sx, r * 2))
        drawOval(Color(0xFFFFA500).copy(alpha = coin.alpha * 0.3f), Offset(coin.x - r * 0.45f * sx, coin.y - r * 0.45f), Size(r * 0.9f * sx, r * 0.9f))
    }
}

private fun DrawScope.drawProjectiles(engine: GameEngine, size: Size) {
    for (p in engine.getProjectiles()) {
        val a = (p.life / 60f).coerceIn(0f, 1f)
        val c = Color(p.color).copy(alpha = a)
        drawCircle(c, p.radius, Offset(p.x, p.y))
        drawCircle(Color.White.copy(alpha = a * 0.4f), p.radius * 0.3f, Offset(p.x, p.y))
        drawLine(c.copy(alpha = a * 0.2f), Offset(p.x - p.radius * 3, p.y), Offset(p.x, p.y), p.radius * 0.5f)
    }
}

private fun DrawScope.drawGround(engine: GameEngine, size: Size) {
    val gy = size.height - engine.getGroundHeight()
    val so = engine.getScrollOffset()

    drawRect(Color(engine.getGroundColor()), Offset(0f, gy), Size(size.width, engine.getGroundHeight()))

    val grassH = 16f
    drawRect(Color(engine.getGrassColor()), Offset(0f, gy), Size(size.width, grassH))
    drawRect(Color(engine.getPipeBodyColor()).copy(alpha = 0.5f), Offset(0f, gy), Size(size.width, 3f))

    val stripeW = 28f; val spacing = 56f
    var sx = -(so % spacing)
    while (sx < size.width) {
        drawRect(Color(engine.getGroundColor()).copy(alpha = 0.25f), Offset(sx, gy + 22f), Size(stripeW, 4f))
        sx += spacing
    }
}

private fun DrawScope.drawBird(engine: GameEngine, size: Size) {
    val bc = Color(android.graphics.Color.parseColor(engine.birdColor))
    val bx = engine.birdX; val by = engine.birdY; val r = engine.birdRadius
    val rot = engine.birdRotation

    rotate(rot, pivot = Offset(bx, by)) {
        drawCircle(bc, r, Offset(bx, by))

        drawCircle(Color.White.copy(alpha = 0.95f), r * 0.3f, Offset(bx + r * 0.25f, by - r * 0.18f))
        drawCircle(Color.Black, r * 0.14f, Offset(bx + r * 0.3f, by - r * 0.18f))

        val wing = if (engine.gameStarted && !engine.isGameOver) sin(engine.getScrollOffset() * 0.45f).toFloat() * 5f else 0f
        drawOval(bc.copy(alpha = 0.9f), Offset(bx - r * 0.05f, by - r * 0.45f + wing), Size(r * 1.15f, r * 0.45f))

        val beak = Path().apply {
            moveTo(bx + r * 0.65f, by - r * 0.08f)
            lineTo(bx + r * 1.15f, by)
            lineTo(bx + r * 0.65f, by + r * 0.08f)
            close()
        }
        drawPath(beak, Color(0xFFFF6B35))
    }
}

private fun DrawScope.drawParticles(engine: GameEngine, size: Size) {
    for (p in engine.getParticles()) {
        val a = (p.life / p.maxLife).coerceIn(0f, 1f)
        drawCircle(Color(p.color).copy(alpha = a), p.size * a, Offset(p.x, p.y))
    }
}

private fun DrawScope.drawWeather(engine: GameEngine, size: Size) {
    val ra = engine.getRainAlpha()
    if (ra <= 0f) return
    val rc = Color(0xFF8395A7).copy(alpha = ra)
    val so = engine.getScrollOffset()
    for (i in 0 until 25) {
        val rx = (i * 97.3f + so * 0.25f) % size.width
        val ry = (i * 53.7f + so * 0.8f) % size.height
        drawLine(rc, Offset(rx, ry), Offset(rx - 1.5f, ry + 9f), 0.8f)
    }
}

private fun DrawScope.drawHUD(engine: GameEngine, size: Size) {
    if (!engine.gameStarted && !engine.isGameOver) return
    val s = size.width / 1080f
    val sp = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE; textSize = 48f * s
        textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
        setShadowLayer(3f, 0f, 2f, android.graphics.Color.BLACK)
    }
    drawContext.canvas.nativeCanvas.drawText("${engine.score}", size.width / 2, 80f * s, sp)
    if (engine.coins > 0) {
        val cp = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(255, 215, 0); textSize = 20f * s
            textAlign = android.graphics.Paint.Align.LEFT; isAntiAlias = true
            setShadowLayer(2f, 0f, 1f, android.graphics.Color.BLACK)
        }
        drawContext.canvas.nativeCanvas.drawText("${engine.coins}", 12f * s, 52f * s, cp)
    }
}

private fun DrawScope.drawStartPrompt(engine: GameEngine, size: Size) {
    val s = size.width / 1080f
    val a = (sin(engine.getScrollOffset() * 0.01f) * 0.3f + 0.7f).toFloat()
    val pp = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE; textSize = 24f * s
        textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
        setShadowLayer(2f, 0f, 1f, android.graphics.Color.BLACK)
        this.alpha = (a * 255).toInt()
    }
    drawContext.canvas.nativeCanvas.drawText("Tap to Flap", size.width / 2, size.height * 0.46f, pp)
    val mn = when (engine.gameMode) { "endless" -> "Classic"; "challenge" -> "Challenge"; "practice" -> "Practice"; else -> engine.gameMode }
    val mp = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(88, 166, 255); textSize = 15f * s
        textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
        this.alpha = (a * 160).toInt()
    }
    drawContext.canvas.nativeCanvas.drawText("$mn Mode", size.width / 2, size.height * 0.51f, mp)
    if (engine.weaponAmmo > 0) {
        val wp = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(255, 80, 50); textSize = 12f * s
            textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
            this.alpha = (a * 140).toInt()
        }
        drawContext.canvas.nativeCanvas.drawText("Weapon: ${engine.weaponType.uppercase()} (${engine.weaponAmmo})", size.width / 2, size.height * 0.56f, wp)
    }
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
private fun GameOverOverlay(score: Int, bestScore: Int, coins: Int, level: Int, onRestart: () -> Unit, onMainMenu: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)), modifier = Modifier.padding(32.dp)) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Game Over", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF85149))
                Spacer(Modifier.height(16.dp))
                Text("Score", fontSize = 14.sp, color = Color(0xFF8B949E))
                Text("$score", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text("Best: $bestScore", fontSize = 16.sp, color = Color(0xFFD29922), fontWeight = FontWeight.SemiBold)
                if (level > 1) { Spacer(Modifier.height(2.dp)); Text("Level $level", fontSize = 14.sp, color = Color(0xFF58A6FF)) }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MonetizationOn, null, tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+$coins", fontSize = 16.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                }
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
