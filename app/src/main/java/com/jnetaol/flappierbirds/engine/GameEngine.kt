package com.jnetaol.flappierbirds.engine

import android.graphics.*
import kotlin.math.*
import kotlin.random.Random

class GameEngine {
    var screenWidth = 1080f
    var screenHeight = 1920f

    var birdX = 0f
    var birdY = 0f
    var birdVelocity = 0f
    var birdRadius = 30f
    var birdColor = "#FFFFD700"
    var birdRotation = 0f

    var score = 0
    var coins = 0
    var flaps = 0
    var obstaclesPassed = 0
    var sessionStartMs = 0L

    var isGameOver = false
    var isPaused = false
    var gameStarted = false
    var gameMode = "endless"

    private val gravity = 0.45f
    private val flapStrength = -8.5f
    private val maxVelocity = 12f
    private val minVelocity = -10f
    private val pipeWidth = 80f
    private val pipeGap = 220f
    private val pipeSpeed = 3.5f
    private val coinRadius = 18f
    private val groundHeight = 120f

    private val pipes = mutableListOf<Pipe>()
    private val coinItems = mutableListOf<CoinItem>()
    private val particles = mutableListOf<Particle>()
    private val clouds = mutableListOf<Cloud>()

    private var pipeSpawnTimer = 0f
    private val pipeSpawnInterval = 90f
    private var difficultyLevel = 1f
    private var scrollOffset = 0f

    var isNight = false
    var isRaining = false
    var isFoggy = false
    private var dayNightTimer = 0f
    private val dayNightCycleDuration = 600f
    private var weatherTimer = 0f
    private var weatherChangeInterval = 300f
    private var currentWeather = "clear"

    private val random = Random(System.currentTimeMillis())

    data class Pipe(
        var x: Float,
        var topHeight: Float,
        var bottomY: Float,
        var passed: Boolean = false
    )

    data class CoinItem(
        var x: Float,
        var y: Float,
        var collected: Boolean = false,
        var alpha: Float = 1f,
        var scale: Float = 1f
    )

    data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var life: Float,
        var maxLife: Float,
        var size: Float,
        var color: Int,
        var type: String = "generic"
    )

    data class Cloud(
        var x: Float,
        var y: Float,
        var width: Float,
        var height: Float,
        var speed: Float,
        var alpha: Float
    )

    fun init(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
        resetGame()
        initClouds()
    }

    fun resetGame() {
        birdX = screenWidth * 0.25f
        birdY = screenHeight * 0.4f
        birdVelocity = 0f
        birdRotation = 0f
        score = 0
        coins = 0
        flaps = 0
        obstaclesPassed = 0
        isGameOver = false
        isPaused = false
        gameStarted = false
        difficultyLevel = 1f
        scrollOffset = 0f
        pipeSpawnTimer = 0f
        sessionStartMs = System.currentTimeMillis()
        pipes.clear()
        coinItems.clear()
        particles.clear()
    }

    private fun initClouds() {
        clouds.clear()
        for (i in 0 until 6) {
            clouds.add(
                Cloud(
                    x = random.nextFloat() * screenWidth,
                    y = 50f + random.nextFloat() * (screenHeight * 0.3f),
                    width = 80f + random.nextFloat() * 120f,
                    height = 30f + random.nextFloat() * 50f,
                    speed = 0.3f + random.nextFloat() * 0.5f,
                    alpha = 0.3f + random.nextFloat() * 0.4f
                )
            )
        }
    }

    fun flap() {
        if (isGameOver) return
        if (!gameStarted) {
            gameStarted = true
            sessionStartMs = System.currentTimeMillis()
        }
        birdVelocity = flapStrength
        flaps++
        spawnFlapParticles()
    }

    fun update() {
        if (isPaused || isGameOver || !gameStarted) return

        birdVelocity += gravity
        birdVelocity = birdVelocity.coerceIn(minVelocity, maxVelocity)
        birdY += birdVelocity

        birdRotation = (birdVelocity / maxVelocity * 45f).coerceIn(-30f, 60f)

        scrollOffset += pipeSpeed * difficultyLevel

        updateDayNightCycle()
        updateWeather()
        updatePipes()
        updateCoins()
        updateParticles()
        updateClouds()

        checkCollisions()

        if (birdY > screenHeight - groundHeight - birdRadius) {
            birdY = screenHeight - groundHeight - birdRadius
            if (gameStarted) endGame()
        }
        if (birdY < birdRadius) {
            birdY = birdRadius
            birdVelocity = 0f
        }
    }

    private fun updateDayNightCycle() {
        dayNightTimer += 1f
        if (dayNightTimer >= dayNightCycleDuration) {
            dayNightTimer = 0f
            isNight = !isNight
        }
    }

    private fun updateWeather() {
        weatherTimer += 1f
        if (weatherTimer >= weatherChangeInterval) {
            weatherTimer = 0f
            currentWeather = when (random.nextInt(10)) {
                in 0..5 -> "clear"
                in 6..7 -> "rain"
                in 8..8 -> "fog"
                else -> "clear"
            }
            isRaining = currentWeather == "rain"
            isFoggy = currentWeather == "fog"
        }
    }

    private fun updatePipes() {
        pipeSpawnTimer += pipeSpeed * difficultyLevel
        if (pipeSpawnTimer >= pipeSpawnInterval) {
            pipeSpawnTimer = 0f
            spawnPipe()
        }

        val iterator = pipes.iterator()
        while (iterator.hasNext()) {
            val pipe = iterator.next()
            pipe.x -= pipeSpeed * difficultyLevel

            if (!pipe.passed && pipe.x + pipeWidth < birdX) {
                pipe.passed = true
                score++
                obstaclesPassed++
                spawnScoreParticles(pipe.x + pipeWidth / 2, pipe.topHeight + pipeGap / 2)
            }

            if (pipe.x + pipeWidth < -50f) {
                iterator.remove()
            }
        }
    }

    private fun spawnPipe() {
        val minTop = 80f
        val maxTop = screenHeight - groundHeight - pipeGap - 80f
        val topHeight = minTop + random.nextFloat() * (maxTop - minTop)
        val bottomY = topHeight + pipeGap

        pipes.add(
            Pipe(
                x = screenWidth + 50f,
                topHeight = topHeight,
                bottomY = bottomY
            )
        )

        if (random.nextFloat() < 0.4f) {
            val coinY = topHeight + pipeGap / 2 + random.nextFloat() * 40f - 20f
            coinItems.add(
                CoinItem(
                    x = screenWidth + 50f + pipeWidth / 2,
                    y = coinY.coerceIn(topHeight + 30f, bottomY - 30f)
                )
            )
        }
    }

    private fun updateCoins() {
        val iterator = coinItems.iterator()
        while (iterator.hasNext()) {
            val coin = iterator.next()
            coin.x -= pipeSpeed * difficultyLevel

            if (!coin.collected) {
                val dx = birdX - coin.x
                val dy = birdY - coin.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < birdRadius + coinRadius) {
                    coin.collected = true
                    coins++
                    spawnCoinParticles(coin.x, coin.y)
                }
            }

            if (coin.collected) {
                coin.alpha -= 0.05f
                coin.scale += 0.05f
                if (coin.alpha <= 0f) iterator.remove()
            } else if (coin.x < -50f) {
                iterator.remove()
            }
        }
    }

    private fun updateParticles() {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.1f
            p.life -= 1f
            if (p.life <= 0f) iterator.remove()
        }
    }

    private fun updateClouds() {
        for (cloud in clouds) {
            cloud.x -= cloud.speed
            if (cloud.x + cloud.width < -20f) {
                cloud.x = screenWidth + 20f
                cloud.y = 50f + random.nextFloat() * (screenHeight * 0.3f)
            }
        }
    }

    private fun checkCollisions() {
        val birdLeft = birdX - birdRadius
        val birdRight = birdX + birdRadius
        val birdTop = birdY - birdRadius
        val birdBottom = birdY + birdRadius

        for (pipe in pipes) {
            val pipeLeft = pipe.x
            val pipeRight = pipe.x + pipeWidth

            if (birdRight > pipeLeft && birdLeft < pipeRight) {
                if (birdTop < pipe.topHeight || birdBottom > pipe.bottomY) {
                    endGame()
                    spawnHitParticles()
                    return
                }
            }
        }
    }

    private fun endGame() {
        isGameOver = true
        gameStarted = false
    }

    fun getSessionDurationMs(): Long {
        return System.currentTimeMillis() - sessionStartMs
    }

    fun getCurrentSpeed(): Float = pipeSpeed * difficultyLevel

    fun increaseDifficulty() {
        difficultyLevel = min(difficultyLevel + 0.0005f, 2.5f)
    }

    private fun spawnFlapParticles() {
        for (i in 0 until 8) {
            particles.add(
                Particle(
                    x = birdX - 10f,
                    y = birdY + birdRadius,
                    vx = -1f + random.nextFloat() * 2f - 1f,
                    vy = 1f + random.nextFloat() * 2f,
                    life = 15f + random.nextFloat() * 10f,
                    maxLife = 25f,
                    size = 3f + random.nextFloat() * 4f,
                    color = Color.argb(200, 255, 255, 255),
                    type = "flap"
                )
            )
        }
    }

    private fun spawnScoreParticles(x: Float, y: Float) {
        for (i in 0 until 12) {
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = -2f + random.nextFloat() * 4f,
                    vy = -3f + random.nextFloat() * 6f,
                    life = 20f + random.nextFloat() * 15f,
                    maxLife = 35f,
                    size = 2f + random.nextFloat() * 5f,
                    color = Color.argb(200, 63, 185, 80),
                    type = "score"
                )
            )
        }
    }

    private fun spawnCoinParticles(x: Float, y: Float) {
        for (i in 0 until 10) {
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = -3f + random.nextFloat() * 6f,
                    vy = -4f + random.nextFloat() * 8f,
                    life = 15f + random.nextFloat() * 10f,
                    maxLife = 25f,
                    size = 2f + random.nextFloat() * 4f,
                    color = Color.argb(200, 255, 215, 0),
                    type = "coin"
                )
            )
        }
    }

    private fun spawnHitParticles() {
        for (i in 0 until 20) {
            particles.add(
                Particle(
                    x = birdX,
                    y = birdY,
                    vx = -5f + random.nextFloat() * 10f,
                    vy = -6f + random.nextFloat() * 12f,
                    life = 25f + random.nextFloat() * 20f,
                    maxLife = 45f,
                    size = 3f + random.nextFloat() * 6f,
                    color = Color.argb(200, 248, 81, 73),
                    type = "hit"
                )
            )
        }
    }

    fun getSkyColor(): Int {
        return if (isNight) Color.parseColor("#FF0D1B2A") else Color.parseColor("#FF4A90D9")
    }

    fun getGroundColor(): Int {
        return if (isNight) Color.parseColor("#FF1A3316") else Color.parseColor("#FF2D5A27")
    }

    fun getPipeColor(): Int {
        return if (isNight) Color.parseColor("#FF1B5E20") else Color.parseColor("#FF3FB950")
    }

    fun getPipeHighlightColor(): Int {
        return if (isNight) Color.parseColor("#FF2D8A30") else Color.parseColor("#FF5FD968")
    }

    fun getCloudAlpha(): Float {
        return if (isNight) 0.15f else 0.4f
    }

    fun getFogAlpha(): Float {
        return if (isFoggy) 0.3f else 0f
    }

    fun getRainAlpha(): Float {
        return if (isRaining) 0.5f else 0f
    }

    fun getPipes(): List<Pipe> = pipes
    fun getCoinItems(): List<CoinItem> = coinItems
    fun getParticles(): List<Particle> = particles
    fun getClouds(): List<Cloud> = clouds
    fun getScrollOffset(): Float = scrollOffset
    fun getGroundHeight(): Float = groundHeight
    fun getDifficultyLevel(): Float = difficultyLevel
}
