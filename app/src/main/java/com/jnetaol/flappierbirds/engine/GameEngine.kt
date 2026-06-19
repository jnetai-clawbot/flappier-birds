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
    var birdRadius = 28f
    var birdColor = "#FFFFD700"
    var birdRotation = 0f

    var score = 0
    var coins = 0
    var flaps = 0
    var obstaclesPassed = 0
    var sessionStartMs = 0L
    var bestScore = 0

    var isGameOver = false
    var isPaused = false
    var gameStarted = false
    var gameMode = "endless"
    var initialized = false

    private var gravity = 0.5f
    private var flapStrength = -9f
    private var maxVelocity = 14f
    private var minVelocity = -12f
    private var pipeWidth = 78f
    private var pipeGap = 200f
    private var pipeSpeed = 3.2f
    private var groundHeight = 100f
    private val coinRadius = 16f

    private val pipes = mutableListOf<Pipe>()
    private val coinItems = mutableListOf<CoinItem>()
    private val particles = mutableListOf<Particle>()
    private val clouds = mutableListOf<Cloud>()
    private val projectiles = mutableListOf<Projectile>()

    private var pipeSpawnTimer = 0f
    private var pipeSpawnInterval = 100f
    private var difficultyLevel = 1f
    private var scrollOffset = 0f
    private var level = 1

    var isNight = false
    var isRaining = false
    var isFoggy = false
    private var dayNightTimer = 0f
    private val dayNightCycleDuration = 800f
    private var weatherTimer = 0f
    private var weatherChangeInterval = 400f
    private var currentWeather = "clear"

    var weaponAmmo = 0
    var weaponType = "none"
    private var weaponCooldown = 0f
    private val weaponCooldownMax = 30f

    private val random = Random(System.currentTimeMillis())

    data class Pipe(
        var x: Float,
        var topHeight: Float,
        var bottomY: Float,
        var passed: Boolean = false,
        var destroyed: Boolean = false,
        var destroyTimer: Float = 0f
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

    data class Projectile(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var life: Float,
        var radius: Float,
        var color: Int
    )

    fun init(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
        applyModeConfig()
        resetGame()
        initClouds()
        initialized = true
    }

    private fun applyModeConfig() {
        when (gameMode) {
            "endless" -> {
                gravity = 0.5f; flapStrength = -9f; pipeGap = 200f; pipeSpeed = 3.2f
                pipeSpawnInterval = 100f; weaponType = "none"; weaponAmmo = 0
            }
            "challenge" -> {
                gravity = 0.55f; flapStrength = -9.5f; pipeGap = 170f; pipeSpeed = 4.0f
                pipeSpawnInterval = 85f; weaponType = "laser"; weaponAmmo = 5
            }
            "practice" -> {
                gravity = 0.4f; flapStrength = -8f; pipeGap = 250f; pipeSpeed = 2.5f
                pipeSpawnInterval = 120f; weaponType = "none"; weaponAmmo = 0
            }
        }
    }

    fun resetGame() {
        birdX = screenWidth * 0.22f
        birdY = screenHeight * 0.45f
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
        level = 1
        scrollOffset = 0f
        pipeSpawnTimer = 0f
        sessionStartMs = System.currentTimeMillis()
        weaponCooldown = 0f
        pipes.clear()
        coinItems.clear()
        particles.clear()
        projectiles.clear()
        applyModeConfig()
    }

    private fun initClouds() {
        clouds.clear()
        for (i in 0 until 7) {
            clouds.add(Cloud(
                x = random.nextFloat() * screenWidth,
                y = 40f + random.nextFloat() * (screenHeight * 0.35f),
                width = 70f + random.nextFloat() * 140f,
                height = 25f + random.nextFloat() * 55f,
                speed = 0.2f + random.nextFloat() * 0.6f,
                alpha = 0.25f + random.nextFloat() * 0.45f
            ))
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

    fun fireWeapon() {
        if (isGameOver || !gameStarted || weaponAmmo <= 0 || weaponCooldown > 0f) return
        weaponAmmo--
        weaponCooldown = weaponCooldownMax
        val projColor = when (weaponType) {
            "laser" -> Color.argb(255, 255, 80, 50)
            "rocket" -> Color.argb(255, 255, 180, 40)
            else -> Color.argb(255, 255, 255, 100)
        }
        projectiles.add(Projectile(
            x = birdX + birdRadius + 10f,
            y = birdY,
            vx = 12f,
            vy = 0f,
            life = 60f,
            radius = 8f,
            color = projColor
        ))
    }

    fun update() {
        if (isPaused || isGameOver || !gameStarted) return

        birdVelocity += gravity
        birdVelocity = birdVelocity.coerceIn(minVelocity, maxVelocity)
        birdY += birdVelocity
        birdRotation = (birdVelocity / maxVelocity * 40f).coerceIn(-25f, 55f)

        scrollOffset += pipeSpeed * difficultyLevel

        if (weaponCooldown > 0f) weaponCooldown -= 1f

        updateDayNightCycle()
        updateWeather()
        updatePipes()
        updateCoins()
        updateParticles()
        updateClouds()
        updateProjectiles()
        checkProjectileCollisions()
        checkCollisions()

        if (birdY > screenHeight - groundHeight - birdRadius) {
            birdY = screenHeight - groundHeight - birdRadius
            if (gameStarted) endGame()
        }
        if (birdY < birdRadius) {
            birdY = birdRadius
            birdVelocity = 0f
        }

        checkLevelUp()
    }

    private fun checkLevelUp() {
        val newLevel = (score / 10) + 1
        if (newLevel > level) {
            level = newLevel
            difficultyLevel = min(1f + (level - 1) * 0.15f, 3.5f)
            pipeGap = max(pipeGap - 8f, 100f)
            pipeSpawnInterval = max(pipeSpawnInterval - 3f, 55f)
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
            if (!pipe.destroyed) {
                pipe.x -= pipeSpeed * difficultyLevel
            } else {
                pipe.destroyTimer -= 1f
                if (pipe.destroyTimer <= 0f) {
                    iterator.remove()
                    continue
                }
            }

            if (!pipe.passed && !pipe.destroyed && pipe.x + pipeWidth < birdX) {
                pipe.passed = true
                score++
                obstaclesPassed++
                spawnScoreParticles(pipe.x + pipeWidth / 2, pipe.topHeight + pipeGap / 2)
            }

            if (pipe.x + pipeWidth < -60f) {
                iterator.remove()
            }
        }
    }

    private fun spawnPipe() {
        val minTop = 70f
        val maxTop = screenHeight - groundHeight - pipeGap - 70f
        val topHeight = minTop + random.nextFloat() * (maxTop - minTop)
        val bottomY = topHeight + pipeGap

        pipes.add(Pipe(
            x = screenWidth + 60f,
            topHeight = topHeight,
            bottomY = bottomY
        ))

        if (random.nextFloat() < 0.45f) {
            val coinY = topHeight + pipeGap / 2 + random.nextFloat() * 50f - 25f
            coinItems.add(CoinItem(
                x = screenWidth + 60f + pipeWidth / 2,
                y = coinY.coerceIn(topHeight + 25f, bottomY - 25f)
            ))
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
                coin.alpha -= 0.06f
                coin.scale += 0.06f
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
            p.vy += 0.08f
            p.life -= 1f
            if (p.life <= 0f) iterator.remove()
        }
    }

    private fun updateClouds() {
        for (cloud in clouds) {
            cloud.x -= cloud.speed
            if (cloud.x + cloud.width < -30f) {
                cloud.x = screenWidth + 30f
                cloud.y = 40f + random.nextFloat() * (screenHeight * 0.35f)
            }
        }
    }

    private fun updateProjectiles() {
        val iterator = projectiles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx
            p.y += p.vy
            p.life -= 1f
            if (p.life <= 0f || p.x > screenWidth + 100f) {
                iterator.remove()
            }
        }
    }

    private fun checkProjectileCollisions() {
        for (proj in projectiles) {
            for (pipe in pipes) {
                if (pipe.destroyed) continue
                if (proj.x + proj.radius > pipe.x && proj.x - proj.radius < pipe.x + pipeWidth) {
                    if (proj.y < pipe.topHeight || proj.y > pipe.bottomY) {
                        pipe.destroyed = true
                        pipe.destroyTimer = 20f
                        spawnDestroyParticles(pipe.x + pipeWidth / 2, if (proj.y < pipe.topHeight) pipe.topHeight else pipe.bottomY)
                        proj.life = 0f
                        break
                    }
                }
            }
        }
    }

    private fun checkCollisions() {
        val birdLeft = birdX - birdRadius * 0.7f
        val birdRight = birdX + birdRadius * 0.7f
        val birdTop = birdY - birdRadius * 0.7f
        val birdBottom = birdY + birdRadius * 0.7f

        for (pipe in pipes) {
            if (pipe.destroyed) continue
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
        if (score > bestScore) bestScore = score
    }

    fun getSessionDurationMs(): Long = System.currentTimeMillis() - sessionStartMs

    fun increaseDifficulty() {
        difficultyLevel = min(difficultyLevel + 0.0003f, 3.5f)
    }

    private fun spawnFlapParticles() {
        for (i in 0 until 6) {
            particles.add(Particle(
                x = birdX - 5f, y = birdY + birdRadius,
                vx = -1.5f + random.nextFloat() * 3f,
                vy = 1f + random.nextFloat() * 2.5f,
                life = 12f + random.nextFloat() * 8f,
                maxLife = 20f, size = 2f + random.nextFloat() * 4f,
                color = Color.argb(180, 255, 255, 255), type = "flap"
            ))
        }
    }

    private fun spawnScoreParticles(x: Float, y: Float) {
        for (i in 0 until 10) {
            particles.add(Particle(
                x = x, y = y,
                vx = -2f + random.nextFloat() * 4f,
                vy = -3f + random.nextFloat() * 6f,
                life = 18f + random.nextFloat() * 12f,
                maxLife = 30f, size = 2f + random.nextFloat() * 4f,
                color = Color.argb(200, 63, 185, 80), type = "score"
            ))
        }
    }

    private fun spawnCoinParticles(x: Float, y: Float) {
        for (i in 0 until 8) {
            particles.add(Particle(
                x = x, y = y,
                vx = -3f + random.nextFloat() * 6f,
                vy = -4f + random.nextFloat() * 8f,
                life = 12f + random.nextFloat() * 8f,
                maxLife = 20f, size = 2f + random.nextFloat() * 3f,
                color = Color.argb(200, 255, 215, 0), type = "coin"
            ))
        }
    }

    private fun spawnHitParticles() {
        for (i in 0 until 25) {
            particles.add(Particle(
                x = birdX, y = birdY,
                vx = -6f + random.nextFloat() * 12f,
                vy = -7f + random.nextFloat() * 14f,
                life = 20f + random.nextFloat() * 15f,
                maxLife = 35f, size = 3f + random.nextFloat() * 5f,
                color = Color.argb(200, 248, 81, 73), type = "hit"
            ))
        }
    }

    private fun spawnDestroyParticles(x: Float, y: Float) {
        for (i in 0 until 15) {
            particles.add(Particle(
                x = x, y = y,
                vx = -4f + random.nextFloat() * 8f,
                vy = -5f + random.nextFloat() * 10f,
                life = 15f + random.nextFloat() * 10f,
                maxLife = 25f, size = 3f + random.nextFloat() * 5f,
                color = Color.argb(200, 255, 140, 30), type = "destroy"
            ))
        }
    }

    fun getSkyColor(): Int = when (gameMode) {
        "challenge" -> if (isNight) Color.parseColor("#FF2D0A0A") else Color.parseColor("#FF5C1A1A")
        "practice" -> if (isNight) Color.parseColor("#FF0A1A2D") else Color.parseColor("#FF1A4A6C")
        else -> if (isNight) Color.parseColor("#FF0D1B2A") else Color.parseColor("#FF4A90D9")
    }

    fun getGroundColor(): Int = when (gameMode) {
        "challenge" -> if (isNight) Color.parseColor("#FF3D1A0A") else Color.parseColor("#FF6B2A10")
        "practice" -> if (isNight) Color.parseColor("#FF0A2D1A") else Color.parseColor("#FF1A5C30")
        else -> if (isNight) Color.parseColor("#FF1A3316") else Color.parseColor("#FF2D5A27")
    }

    fun getPipeColor(): Int = when (gameMode) {
        "challenge" -> if (isNight) Color.parseColor("#FF6B1A0A") else Color.parseColor("#FF8B3A20")
        "practice" -> if (isNight) Color.parseColor("#FF1A3D6B") else Color.parseColor("#FF2A5D8B")
        else -> if (isNight) Color.parseColor("#FF1B5E20") else Color.parseColor("#FF3FB950")
    }

    fun getPipeHighlightColor(): Int = when (gameMode) {
        "challenge" -> if (isNight) Color.parseColor("#FF8B2A10") else Color.parseColor("#FFAB5A30")
        "practice" -> if (isNight) Color.parseColor("#FF2A5D8B") else Color.parseColor("#FF4A7DAB")
        else -> if (isNight) Color.parseColor("#FF2D8A30") else Color.parseColor("#FF5FD968")
    }

    fun getCloudAlpha(): Float = if (isNight) 0.12f else 0.35f
    fun getFogAlpha(): Float = if (isFoggy) 0.25f else 0f
    fun getRainAlpha(): Float = if (isRaining) 0.4f else 0f

    fun getPipes(): List<Pipe> = pipes
    fun getCoinItems(): List<CoinItem> = coinItems
    fun getParticles(): List<Particle> = particles
    fun getClouds(): List<Cloud> = clouds
    fun getProjectiles(): List<Projectile> = projectiles
    fun getScrollOffset(): Float = scrollOffset
    fun getGroundHeight(): Float = groundHeight
    fun getDifficultyLevel(): Float = difficultyLevel
    fun getLevel(): Int = level
    fun getPipeGap(): Float = pipeGap
    fun getPipeWidth(): Float = pipeWidth
}
