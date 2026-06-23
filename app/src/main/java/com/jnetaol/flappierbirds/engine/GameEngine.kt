package com.jnetaol.flappierbirds.engine

import kotlin.math.sin
import kotlin.random.Random

/**
 * Classic Flappy Bird engine in a fixed 288x512 virtual coordinate space.
 * Endless/classic mode uses progressive levels that get harder over time.
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

    var currentLevel = 1
    var pipesThisLevel = 0
    var levelUpFlashFrames = 0

    var isGameOver = false
    var isPaused = false
    var isLevelTransition = false
    var gameStarted = false
    var gameMode = "endless"
    var difficulty = Difficulty.EASY
    var initialized = false
    var flaps = 0
    var sessionFrames = 0L
    var obstaclesPassed = 0

    private var gravity = 0.18f
    private var flapStrength = -5.8f
    private var maxVelocity = 7.5f
    private var minVelocity = -6.8f
    private var pipeWidth = 50f
    private var pipeGap = 178f
    private var pipeSpeed = 1.2f
    private var groundHeight = 112f
    private var pipeSpawnDistance = 210f
    private var collisionInset = 0.22f

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
        resetGame()
        initialized = true
    }

    fun resetGame() {
        currentLevel = 1
        pipesThisLevel = 0
        levelUpFlashFrames = 0
        isLevelTransition = false

        birdX = screenWidth * 0.25f
        birdY = screenHeight * 0.42f
        birdVelocity = 0f
        birdRotation = 0f
        score = 0
        isGameOver = false
        isPaused = false
        gameStarted = false
        groundScroll = 0f
        bobTimer = 0f
        wingPhase = 0f
        pipes.clear()
        particles.clear()
        flaps = 0
        sessionFrames = 0
        obstaclesPassed = 0

        applyPhysics()
        distanceSinceLastPipe = pipeSpawnDistance
        spawnPipe(screenWidth * 0.65f)
        spawnPipe(screenWidth * 1.15f)
    }

    fun flap() {
        if (isGameOver || isPaused || isLevelTransition) return
        if (!gameStarted) gameStarted = true
        birdVelocity = flapStrength
        flaps += 1
    }

    fun update() {
        if (!initialized) return

        bobTimer += 1f
        wingPhase += 0.35f
        groundScroll += if (gameStarted && !isGameOver && !isPaused) pipeSpeed else pipeSpeed * 0.35f

        if (isLevelTransition) {
            levelUpFlashFrames--
            if (levelUpFlashFrames <= 0) {
                isLevelTransition = false
            }
            return
        }

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
                onPipeCleared()
            }

            if (pipe.x + pipeWidth < -pipeWidth) {
                iterator.remove()
            }
        }
    }

    private fun onPipeCleared() {
        if (!usesLevelProgression()) {
            applyPhysics()
            return
        }

        pipesThisLevel += 1
        applyPhysics()

        val required = LevelProgression.pipesRequiredForLevel(currentLevel)
        if (pipesThisLevel >= required) {
            advanceLevel()
        }
    }

    private fun advanceLevel() {
        currentLevel += 1
        pipesThisLevel = 0
        isLevelTransition = true
        levelUpFlashFrames = 90
        applyPhysics()
        distanceSinceLastPipe = 0f
    }

    private fun spawnPipe(startX: Float) {
        val margin = 40f
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
        val inset = birdRadius * collisionInset
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

    private fun usesLevelProgression(): Boolean = gameMode == "endless"

    private fun applyPhysics() {
        val physics = if (usesLevelProgression()) {
            LevelProgression.resolve(currentLevel, pipesThisLevel, difficulty)
        } else {
            fixedModePhysics()
        }

        gravity = physics.gravity
        flapStrength = physics.flapStrength
        maxVelocity = physics.maxVelocity
        minVelocity = physics.minVelocity
        pipeWidth = physics.pipeWidth
        pipeGap = physics.pipeGap
        pipeSpeed = physics.pipeSpeed
        groundHeight = physics.groundHeight
        pipeSpawnDistance = physics.pipeSpawnDistance
        birdRadius = physics.birdRadius
        collisionInset = physics.collisionInset
    }

    private fun fixedModePhysics(): LevelProgression.Physics {
        val base = when (gameMode) {
            "practice" -> LevelProgression.Physics(
                gravity = 0.24f,
                flapStrength = -6.2f,
                maxVelocity = 8.5f,
                minVelocity = -7.2f,
                pipeGap = 165f,
                pipeSpeed = 1.5f,
                pipeSpawnDistance = 200f,
                collisionInset = 0.2f
            )

            "challenge" -> LevelProgression.Physics(
                gravity = 0.34f,
                flapStrength = -7f,
                maxVelocity = 9.5f,
                minVelocity = -8f,
                pipeGap = 120f,
                pipeSpeed = 2.2f,
                pipeSpawnDistance = 165f,
                collisionInset = 0.12f
            )

            else -> LevelProgression.resolve(3, 0, difficulty)
        }

        return LevelProgression.applyWithinLevelRamp(
            base = base,
            pipesCleared = pipesThisLevel,
            pipesRequired = LevelProgression.pipesRequiredForLevel(currentLevel),
            rampMult = LevelProgression.rampMultiplier(difficulty) * 0.8f
        )
    }

    fun pipesRequiredThisLevel(): Int = LevelProgression.pipesRequiredForLevel(currentLevel)

    fun getWingPhase(): Float = wingPhase
    fun getPipes(): List<Pipe> = pipes
    fun getParticles(): List<Particle> = particles
    fun getGroundScroll(): Float = groundScroll
    fun getGroundHeight(): Float = groundHeight
    fun getPipeGap(): Float = pipeGap
    fun getPipeWidth(): Float = pipeWidth
    fun getSessionDurationMs(): Long = sessionFrames * 16L
}
