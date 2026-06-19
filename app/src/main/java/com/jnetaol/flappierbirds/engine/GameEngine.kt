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
    var birdRadius = 24f
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

    private var gravity = 0.55f
    private var flapStrength = -8.5f
    private var maxVelocity = 13f
    private var minVelocity = -11f
    private var pipeWidth = 72f
    private var pipeGap = 180f
    private var pipeSpeed = 3.0f
    private var groundHeight = 112f
    private val coinRadius = 14f

    private val pipes = mutableListOf<Pipe>()
    private val coinItems = mutableListOf<CoinItem>()
    private val particles = mutableListOf<Particle>()
    private val clouds = mutableListOf<Cloud>()
    private val projectiles = mutableListOf<Projectile>()

    private var pipeSpawnTimer = 0f
    private var pipeSpawnInterval = 110f
    private var difficultyLevel = 1f
    private var scrollOffset = 0f
    private var level = 1

    var isNight = false
    var isRaining = false
    var isFoggy = false
    private var dayNightTimer = 0f
    private val dayNightCycleDuration = 900f
    private var weatherTimer = 0f
    private var weatherChangeInterval = 500f
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
                gravity = 0.55f; flapStrength = -8.5f; pipeGap = 180f; pipeSpeed = 3.0f
                pipeSpawnInterval = 110f; weaponType = "none"; weaponAmmo = 0
            }
            "challenge" -> {
                gravity = 0.6f; flapStrength = -9f; pipeGap = 160f; pipeSpeed = 3.8f
                pipeSpawnInterval = 90f; weaponType = "laser"; weaponAmmo = 5
            }
            "practice" -> {
                gravity = 0.45f; flapStrength = -7.5f; pipeGap = 230f; pipeSpeed = 2.4f
                pipeSpawnInterval = 130f; weaponType = "none"; weaponAmmo = 0
            }
        }
    }

    fun resetGame() {
        birdX = screenWidth * 0.2f
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
        for (i in 0 until 5) {
            clouds.add(Cloud(
                x = random.nextFloat() * screenWidth,
                y = 30f + random.nextFloat() * (screenHeight * 0.3f),
                width = 60f + random.nextFloat() * 100f,
                height = 20f + random.nextFloat() * 40f,
                speed = 0.15f + random.nextFloat() * 0.4f,
                alpha = 0.2f + random.nextFloat() * 0.35f
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
            x = birdX + birdRadius + 8f,
            y = birdY,
            vx = 12f,
            vy = 0f,
            life = 60f,
            radius = 7f,
            color = projColor
        ))
    }

    fun update() {
        if (isPaused || isGameOver || !gameStarted) return

        birdVelocity += gravity
        birdVelocity = birdVelocity.coerceIn(minVelocity, maxVelocity)
        birdY += birdVelocity
        birdRotation = (birdVelocity / maxVelocity * 35f).coerceIn(-20f, 50f)

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
            endGame()
        }
        if (birdY < birdRadius) {
            birdY = birdRadius
            birdVelocity = 0f
        }

        if (gameMode != "endless") {
            checkLevelUp()
        }
    }

    private fun checkLevelUp() {
        val newLevel = (score / 10) + 1
        if (newLevel > level) {
            level = newLevel
            difficultyLevel = min(1f + (level - 1) * 0.12f, 3.0f)
            pipeGap = max(pipeGap - 6f, 110f)
            pipeSpawnInterval = max(pipeSpawnInterval - 2f, 60f)
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
                in 0..6 -> "clear"
                in 7..8 -> "rain"
                in 9..9 -> "fog"
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
        val minTop = 60f
        val maxTop = screenHeight - groundHeight - pipeGap - 60f
        val topHeight = minTop + random.nextFloat() * (maxTop - minTop)
        val bottomY = topHeight + pipeGap

        pipes.add(Pipe(
            x = screenWidth + 50f,
            topHeight = topHeight,
            bottomY = bottomY
        ))

        if (random.nextFloat() < 0.4f) {
            val coinY = topHeight + pipeGap / 2 + random.nextFloat() * 40f - 20f
            coinItems.add(CoinItem(
                x = screenWidth + 50f + pipeWidth / 2,
                y = coinY.coerceIn(topHeight + 20f, bottomY - 20f)
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
            p.vy += 0.06f
            p.life -= 1f
            if (p.life <= 0f) iterator.remove()
        }
    }

    private fun updateClouds() {
        for (cloud in clouds) {
            cloud.x -= cloud.speed
            if (cloud.x + cloud.width < -30f) {
                cloud.x = screenWidth + 30f
                cloud.y = 30f + random.nextFloat() * (screenHeight * 0.3f)
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
        val birdLeft = birdX - birdRadius * 0.85f
        val birdRight = birdX + birdRadius * 0.85f
        val birdTop = birdY - birdRadius * 0.85f
        val birdBottom = birdY + birdRadius * 0.85f

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
        if (gameMode != "endless") {
            difficultyLevel = min(difficultyLevel + 0.0003f, 3.0f)
        }
    }

    private fun spawnFlapParticles() {
        for (i in 0 until 5) {
            particles.add(Particle(
                x = birdX - 3f, y = birdY + birdRadius,
                vx = -1f + random.nextFloat() * 2f,
                vy = 0.8f + random.nextFloat() * 2f,
                life = 10f + random.nextFloat() * 6f,
                maxLife = 16f, size = 2f + random.nextFloat() * 3f,
                color = Color.argb(160, 255, 255, 255), type = "flap"
            ))
        }
    }

    private fun spawnScoreParticles(x: Float, y: Float) {
        for (i in 0 until 8) {
            particles.add(Particle(
                x = x, y = y,
                vx = -1.5f + random.nextFloat() * 3f,
                vy = -2f + random.nextFloat() * 4f,
                life = 14f + random.nextFloat() * 10f,
                maxLife = 24f, size = 2f + random.nextFloat() * 3f,
                color = Color.argb(180, 63, 185, 80), type = "score"
            ))
        }
    }

    private fun spawnCoinParticles(x: Float, y: Float) {
        for (i in 0 until 6) {
            particles.add(Particle(
                x = x, y = y,
                vx = -2f + random.nextFloat() * 4f,
                vy = -3f + random.nextFloat() * 6f,
                life = 10f + random.nextFloat() * 6f,
                maxLife = 16f, size = 1.5f + random.nextFloat() * 2.5f,
                color = Color.argb(180, 255, 215, 0), type = "coin"
            ))
        }
    }

    private fun spawnHitParticles() {
        for (i in 0 until 20) {
            particles.add(Particle(
                x = birdX, y = birdY,
                vx = -5f + random.nextFloat() * 10f,
                vy = -6f + random.nextFloat() * 12f,
                life = 18f + random.nextFloat() * 12f,
                maxLife = 30f, size = 2.5f + random.nextFloat() * 4f,
                color = Color.argb(200, 248, 81, 73), type = "hit"
            ))
        }
    }

    private fun spawnDestroyParticles(x: Float, y: Float) {
        for (i in 0 until 12) {
            particles.add(Particle(
                x = x, y = y,
                vx = -3f + random.nextFloat() * 6f,
                vy = -4f + random.nextFloat() * 8f,
                life = 12f + random.nextFloat() * 8f,
                maxLife = 20f, size = 2.5f + random.nextFloat() * 4f,
                color = Color.argb(200, 255, 140, 30), type = "destroy"
            ))
        }
    }

    fun getSkyColor(): Int = when (gameMode) {
        "challenge" -> if (isNight) Color.parseColor("#FF2D0A0A") else Color.parseColor("#FF5C1A1A")
        "practice" -> if (isNight) Color.parseColor("#FF0A1A2D") else Color.parseColor("#FF1A4A6C")
        else -> if (isNight) Color.parseColor("#FF0D1B2A") else Color.parseColor("#FF70C5DE")
    }

    fun getGroundColor(): Int = when (gameMode) {
        "challenge" -> if (isNight) Color.parseColor("#FF3D1A0A") else Color.parseColor("#FF6B2A10")
        "practice" -> if (isNight) Color.parseColor("#FF0A2D1A") else Color.parseColor("#FF1A5C30")
        else -> if (isNight) Color.parseColor("#FF1A3316") else Color.parseColor("#FFDED895")
    }

    fun getPipeColor(): Int = when (gameMode) {
        "challenge" -> if (isNight) Color.parseColor("#FF6B1A0A") else Color.parseColor("#FF8B3A20")
        "practice" -> if (isNight) Color.parseColor("#FF1A3D6B") else Color.parseColor("#FF2A5D8B")
        else -> if (isNight) Color.parseColor("#FF1B5E20") else Color.parseColor("#FF74BF2E")
    }

    fun getPipeHighlightColor(): Int = when (gameMode) {
        "challenge" -> if (isNight) Color.parseColor("#FF8B2A10") else Color.parseColor("#FFAB5A30")
        "practice" -> if (isNight) Color.parseColor("#FF2A5D8B") else Color.parseColor("#FF4A7DAB")
        else -> if (isNight) Color.parseColor("#FF2D8A30") else Color.parseColor("#FF8FD94E")
    }

    fun getPipeCapColor(): Int = when (gameMode) {
        "challenge" -> if (isNight) Color.parseColor("#FF9B3A20") else Color.parseColor("#FFBB6A40")
        "practice" -> if (isNight) Color.parseColor("#FF3A5D9B") else Color.parseColor("#FF5A7DBB")
        else -> if (isNight) Color.parseColor("#FF3D8A30") else Color.parseColor("#FFA0E060")
    }

    fun getCloudAlpha(): Float = if (isNight) 0.1f else 0.3f
    fun getFogAlpha(): Float = if (isFoggy) 0.2f else 0f
    fun getRainAlpha(): Float = if (isRaining) 0.35f else 0f

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
