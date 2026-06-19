package com.jnetaol.flappierbirds.engine

import android.graphics.Color
import kotlin.math.*
import kotlin.random.Random

class GameEngine {
    var screenWidth = 1080f
    var screenHeight = 1920f

    var birdX = 0f
    var birdY = 0f
    var birdVelocity = 0f
    var birdRadius = 42f
    var birdRotation = 0f

    var score = 0
    var bestScore = 0

    var isGameOver = false
    var isPaused = false
    var gameStarted = false
    var gameMode = "endless"
    var initialized = false

    private val gravity = 0.55f
    private val flapStrength = -9f
    private val maxVelocity = 13f
    private val minVelocity = -11f
    private val pipeWidth = 80f
    private val pipeGap = 200f
    private val pipeSpeed = 2.8f
    private val groundHeight = 140f

    private val pipes = mutableListOf<Pipe>()
    private val particles = mutableListOf<Particle>()

    private var pipeSpawnTimer = 0f
    private val pipeSpawnInterval = 130f
    private var scrollOffset = 0f

    private var bobTimer = 0f
    private var wingPhase = 0f

    private val random = Random(System.currentTimeMillis())

    data class Pipe(var x: Float, var topHeight: Float, var bottomY: Float, var passed: Boolean = false)
    data class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, var maxLife: Float, var size: Float, var color: Int)

    fun init(width: Float, height: Float) {
        screenWidth = width; screenHeight = height
        resetGame(); initialized = true
    }

    fun resetGame() {
        birdX = screenWidth * 0.22f
        birdY = screenHeight * 0.45f
        birdVelocity = 0f
        birdRotation = 0f
        score = 0
        isGameOver = false
        isPaused = false
        gameStarted = false
        scrollOffset = 0f
        pipeSpawnTimer = 0f
        bobTimer = 0f
        wingPhase = 0f
        pipes.clear()
        particles.clear()
    }

    fun flap() {
        if (isGameOver) return
        if (!gameStarted) gameStarted = true
        birdVelocity = flapStrength
    }

    fun update() {
        bobTimer += 1f
        wingPhase += 0.4f

        if (!gameStarted && !isGameOver) {
            birdY = screenHeight * 0.45f + sin(bobTimer * 0.05f) * 8f
            scrollOffset += 1f
            return
        }

        if (isPaused || isGameOver) return

        birdVelocity += gravity
        birdVelocity = birdVelocity.coerceIn(minVelocity, maxVelocity)
        birdY += birdVelocity
        birdRotation = (birdVelocity / maxVelocity * 40f).coerceIn(-20f, 50f)

        scrollOffset += pipeSpeed

        updatePipes()
        updateParticles()
        checkCollisions()

        if (birdY > screenHeight - groundHeight - birdRadius) {
            birdY = screenHeight - groundHeight - birdRadius
            endGame()
        }
        if (birdY < birdRadius) {
            birdY = birdRadius; birdVelocity = 0f
        }
    }

    private fun updatePipes() {
        pipeSpawnTimer += pipeSpeed
        if (pipeSpawnTimer >= pipeSpawnInterval) {
            pipeSpawnTimer = 0f
            spawnPipe()
        }
        val it = pipes.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.x -= pipeSpeed
            if (!p.passed && p.x + pipeWidth < birdX) { p.passed = true; score++; spawnScoreParticles(p.x + pipeWidth / 2, p.topHeight + pipeGap / 2) }
            if (p.x + pipeWidth < -60f) it.remove()
        }
    }

    private fun spawnPipe() {
        val minTop = 50f
        val maxTop = screenHeight - groundHeight - pipeGap - 50f
        val top = minTop + random.nextFloat() * (maxTop - minTop)
        pipes.add(Pipe(x = screenWidth + 50f, topHeight = top, bottomY = top + pipeGap))
    }

    private fun updateParticles() {
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.x += p.vx; p.y += p.vy; p.vy += 0.04f; p.life -= 1f
            if (p.life <= 0f) it.remove()
        }
    }

    private fun checkCollisions() {
        val m = birdRadius * 0.2f
        val bl = birdX - birdRadius + m; val br = birdX + birdRadius - m
        val bt = birdY - birdRadius + m; val bb = birdY + birdRadius - m
        for (p in pipes) {
            if (br > p.x && bl < p.x + pipeWidth) {
                if (bt < p.topHeight || bb > p.bottomY) { endGame(); spawnHitParticles(); return }
            }
        }
    }

    private fun endGame() {
        isGameOver = true; gameStarted = false
        if (score > bestScore) bestScore = score
    }

    fun getWingPhase(): Float = wingPhase

    private fun spawnScoreParticles(x: Float, y: Float) {
        for (i in 0 until 4) {
            particles.add(Particle(x, y, -0.5f + random.nextFloat(), -1f + random.nextFloat() * 2f, 8f + random.nextFloat() * 5f, 13f, 1.5f + random.nextFloat() * 2f, Color.argb(140, 255, 255, 255)))
        }
    }

    private fun spawnHitParticles() {
        for (i in 0 until 10) {
            particles.add(Particle(birdX, birdY, -3f + random.nextFloat() * 6f, -4f + random.nextFloat() * 8f, 10f + random.nextFloat() * 6f, 16f, 2f + random.nextFloat() * 3f, Color.argb(160, 248, 81, 73)))
        }
    }

    fun getSkyColor(): Int = Color.parseColor("#FF70C5DE")
    fun getGroundColor(): Int = Color.parseColor("#FFDED895")
    fun getPipeBodyColor(): Int = Color.parseColor("#FF74BF2E")
    fun getPipeCapColor(): Int = Color.parseColor("#FF558B2F")
    fun getPipeLipColor(): Int = Color.parseColor("#FF6DAF3A")
    fun getGrassColor(): Int = Color.parseColor("#FF5CBF2E")

    fun getPipes(): List<Pipe> = pipes
    fun getParticles(): List<Particle> = particles
    fun getScrollOffset(): Float = scrollOffset
    fun getGroundHeight(): Float = groundHeight
    fun getPipeGap(): Float = pipeGap
    fun getPipeWidth(): Float = pipeWidth
}
