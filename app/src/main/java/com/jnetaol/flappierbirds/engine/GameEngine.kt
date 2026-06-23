package com.jnetaol.flappierbirds.engine

import kotlin.math.sin
import kotlin.random.Random

/**
 * Classic Flappy Bird engine operating in a fixed 288x512 virtual coordinate space.
 * Screen scaling is handled separately by [GameViewport].
 */
class GameEngine {
    var screenWidth = GameViewport.VIRTUAL_WIDTH
    var screenHeight = GameViewport.VIRTUAL_HEIGHT

    var birdX = 0f
    var birdY = 0f
    var birdVelocity = 0f
    var birdRadius = 17f
    var birdRotation = 0f

    var score = 0
    var bestScore = 0

    var isGameOver = false
    var isPaused = false
    var gameStarted = false
    var gameMode = "endless"
    var difficulty = Difficulty.EXTREME
    var initialized = false
    var flaps = 0
    var sessionFrames = 0L
    var obstaclesPassed = 0

    private var gravity = 0.45f
    private var flapStrength = -7.8f
    private var maxVelocity = 10f
    private var minVelocity = -8.5f
    private var pipeWidth = 52f
    private var pipeGap = 100f
    private var pipeSpeed = 2.4f
    private var groundHeight = 112f
    private var pipeSpawnDistance = 170f

    private val pipes = mutableListOf<Pipe>()
    private val particles = mutableListOf<Particle>()

    private var distanceSinceLastPipe = 0f
    private var groundScroll = 0f
    private var bobTimer = 0f
    private var wingPhase = 0f

    private val random = Random(System.currentTimeMillis())

    data class Pipe(
        var x: Float,
        var topHeight: Float,
        var bottomY: Float,
        var passed: Boolean = false
    )

    data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var life: Float,
        var maxLife: Float,
        var size: Float
    )

    fun init() {
        screenWidth = GameViewport.VIRTUAL_WIDTH
        screenHeight = GameViewport.VIRTUAL_HEIGHT
        applyModeConfig()
        resetGame()
        initialized = true
    }

    fun resetGame() {
        applyModeConfig()

        birdX = screenWidth * 0.25f
        birdY = screenHeight * 0.42f
        birdVelocity = 0f
        birdRotation = 0f
        score = 0
        isGameOver = false
        isPaused = false
        gameStarted = false
        groundScroll = 0f
        distanceSinceLastPipe = pipeSpawnDistance
        bobTimer = 0f
        wingPhase = 0f
        pipes.clear()
        particles.clear()
        flaps = 0
        sessionFrames = 0
        obstaclesPassed = 0

        spawnPipe(screenWidth * 0.65f)
        spawnPipe(screenWidth * 1.15f)
    }

    fun flap() {
        if (isGameOver || isPaused) return
        if (!gameStarted) gameStarted = true
        birdVelocity = flapStrength
        flaps += 1
    }

    fun update() {
        if (!initialized) return

        bobTimer += 1f
        wingPhase += 0.35f
        groundScroll += if (gameStarted && !isGameOver && !isPaused) pipeSpeed else pipeSpeed * 0.35f

        if (!gameStarted && !isGameOver) {
            birdY = screenHeight * 0.42f + sin(bobTimer * 0.08f) * 8f
            return
        }

        if (isPaused || isGameOver) return
        sessionFrames += 1

        birdVelocity = (birdVelocity + gravity).coerceIn(minVelocity, maxVelocity)
        birdY += birdVelocity
        birdRotation = ((birdVelocity / maxVelocity) * 70f).coerceIn(-25f, 90f)

        updatePipes()
        updateParticles()
        checkCollisions()

        val floorY = playableBottom()
        if (birdY + birdRadius >= floorY) {
            birdY = floorY - birdRadius
            endGame()
        }
        if (birdY - birdRadius <= 0f) {
            birdY = birdRadius
            birdVelocity = 0f
        }
    }

    private fun updatePipes() {
        distanceSinceLastPipe += pipeSpeed
        if (distanceSinceLastPipe >= pipeSpawnDistance) {
            distanceSinceLastPipe = 0f
            spawnPipe(screenWidth + pipeWidth)
        }

        val iterator = pipes.iterator()
        while (iterator.hasNext()) {
            val pipe = iterator.next()
            pipe.x -= pipeSpeed

            if (!pipe.passed && pipe.x + pipeWidth < birdX) {
                pipe.passed = true
                score += 1
                obstaclesPassed += 1
            }

            if (pipe.x + pipeWidth < -pipeWidth) {
                iterator.remove()
            }
        }
    }

    private fun spawnPipe(startX: Float) {
        val margin = 48f
        val minTop = margin
        val maxTop = playableBottom() - pipeGap - margin
        if (maxTop <= minTop) return

        val top = minTop + random.nextFloat() * (maxTop - minTop)
        pipes.add(
            Pipe(
                x = startX,
                topHeight = top,
                bottomY = top + pipeGap
            )
        )
    }

    private fun updateParticles() {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val particle = iterator.next()
            particle.x += particle.vx
            particle.y += particle.vy
            particle.vy += 0.08f
            particle.life -= 1f
            if (particle.life <= 0f) iterator.remove()
        }
    }

    private fun checkCollisions() {
        val inset = birdRadius * 0.15f
        val left = birdX - birdRadius + inset
        val right = birdX + birdRadius - inset
        val top = birdY - birdRadius + inset
        val bottom = birdY + birdRadius - inset

        for (pipe in pipes) {
            val pipeLeft = pipe.x
            val pipeRight = pipe.x + pipeWidth
            if (right <= pipeLeft || left >= pipeRight) continue

            if (top < pipe.topHeight || bottom > pipe.bottomY) {
                endGame()
                return
            }
        }
    }

    private fun endGame() {
        isGameOver = true
        gameStarted = false
        if (score > bestScore) bestScore = score
    }

    private fun playableBottom(): Float = screenHeight - groundHeight

    private fun applyModeConfig() {
        val baseGravity: Float
        val baseFlap: Float
        val baseMaxVelocity: Float
        val baseMinVelocity: Float
        val basePipeWidth: Float
        val basePipeGap: Float
        val basePipeSpeed: Float
        val baseGroundHeight: Float
        val basePipeSpawnDistance: Float
        val baseBirdRadius: Float

        when (gameMode) {
            "practice" -> {
                baseGravity = 0.38f
                baseFlap = -7.2f
                baseMaxVelocity = 9.5f
                baseMinVelocity = -8f
                basePipeWidth = 50f
                basePipeGap = 128f
                basePipeSpeed = 2f
                baseGroundHeight = 112f
                basePipeSpawnDistance = 185f
                baseBirdRadius = 17f
            }

            "challenge" -> {
                baseGravity = 0.5f
                baseFlap = -8.2f
                baseMaxVelocity = 11f
                baseMinVelocity = -9f
                basePipeWidth = 54f
                basePipeGap = 92f
                basePipeSpeed = 2.9f
                baseGroundHeight = 112f
                basePipeSpawnDistance = 155f
                baseBirdRadius = 17f
            }

            else -> {
                baseGravity = 0.45f
                baseFlap = -7.8f
                baseMaxVelocity = 10f
                baseMinVelocity = -8.5f
                basePipeWidth = 52f
                basePipeGap = 100f
                basePipeSpeed = 2.4f
                baseGroundHeight = 112f
                basePipeSpawnDistance = 170f
                baseBirdRadius = 17f
            }
        }

        val tuned = difficulty.applyTo(
            gravity = baseGravity,
            flapStrength = baseFlap,
            pipeGap = basePipeGap,
            pipeSpeed = basePipeSpeed,
            pipeSpawnDistance = basePipeSpawnDistance,
            maxVelocity = baseMaxVelocity,
            minVelocity = baseMinVelocity
        )

        gravity = tuned.gravity
        flapStrength = tuned.flapStrength
        maxVelocity = tuned.maxVelocity
        minVelocity = tuned.minVelocity
        pipeWidth = basePipeWidth
        pipeGap = tuned.pipeGap.coerceAtLeast(56f)
        pipeSpeed = tuned.pipeSpeed
        groundHeight = baseGroundHeight
        pipeSpawnDistance = tuned.pipeSpawnDistance.coerceAtLeast(120f)
        birdRadius = baseBirdRadius
    }

    fun getWingPhase(): Float = wingPhase
    fun getPipes(): List<Pipe> = pipes
    fun getParticles(): List<Particle> = particles
    fun getGroundScroll(): Float = groundScroll
    fun getGroundHeight(): Float = groundHeight
    fun getPipeGap(): Float = pipeGap
    fun getPipeWidth(): Float = pipeWidth
    fun getSessionDurationMs(): Long = sessionFrames * 16L
}
