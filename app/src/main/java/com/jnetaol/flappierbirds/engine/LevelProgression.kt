package com.jnetaol.flappierbirds.engine

import kotlin.math.max

object LevelProgression {
    const val BASE_PIPES_PER_LEVEL = 6

    data class Physics(
        val gravity: Float,
        val flapStrength: Float,
        val maxVelocity: Float,
        val minVelocity: Float,
        val pipeGap: Float,
        val pipeSpeed: Float,
        val pipeSpawnDistance: Float,
        val pipeWidth: Float = 50f,
        val groundHeight: Float = 112f,
        val birdRadius: Float = 17f,
        val collisionInset: Float = 0.22f
    )

    fun pipesRequiredForLevel(level: Int): Int =
        BASE_PIPES_PER_LEVEL + (level - 1).coerceAtMost(4)

    fun levelOffset(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.TOO_EASY -> -2
        Difficulty.EASY -> -1
        Difficulty.MID -> 0
        Difficulty.HARD -> 1
        Difficulty.EXTREME -> 2
        Difficulty.IMPOSSIBLE -> 3
    }

    fun rampMultiplier(difficulty: Difficulty): Float = when (difficulty) {
        Difficulty.TOO_EASY -> 0.55f
        Difficulty.EASY -> 0.72f
        Difficulty.MID -> 0.88f
        Difficulty.HARD -> 1.05f
        Difficulty.EXTREME -> 1.15f
        Difficulty.IMPOSSIBLE -> 1.35f
    }

    fun baseForLevel(level: Int): Physics {
        val index = (level - 1).coerceAtLeast(0)
        return Physics(
            gravity = 0.18f + index * 0.035f,
            flapStrength = -5.8f - index * 0.12f,
            maxVelocity = 7.5f + index * 0.35f,
            minVelocity = -6.8f - index * 0.15f,
            pipeGap = 178f - index * 14f,
            pipeSpeed = 1.2f + index * 0.16f,
            pipeSpawnDistance = 210f - index * 10f,
            pipeWidth = 48f + index.coerceAtMost(3) * 1.5f,
            collisionInset = max(0.08f, 0.28f - index * 0.02f)
        )
    }

    fun applyWithinLevelRamp(base: Physics, pipesCleared: Int, pipesRequired: Int, rampMult: Float): Physics {
        if (pipesRequired <= 0) return base
        val progress = (pipesCleared.toFloat() / pipesRequired).coerceIn(0f, 1f) * rampMult.coerceAtMost(1.2f)

        return base.copy(
            gravity = base.gravity + progress * 0.06f,
            flapStrength = base.flapStrength * (1f - progress * 0.05f),
            maxVelocity = base.maxVelocity + progress * 0.8f,
            pipeGap = (base.pipeGap - progress * 22f).coerceAtLeast(82f),
            pipeSpeed = base.pipeSpeed + progress * 0.28f,
            pipeSpawnDistance = (base.pipeSpawnDistance - progress * 18f).coerceAtLeast(130f),
            collisionInset = (base.collisionInset - progress * 0.04f).coerceAtLeast(0.06f)
        )
    }

    fun resolve(level: Int, pipesCleared: Int, difficulty: Difficulty): Physics {
        val effectiveLevel = (level + levelOffset(difficulty)).coerceAtLeast(1)
        val base = baseForLevel(effectiveLevel)
        val required = pipesRequiredForLevel(level)
        return applyWithinLevelRamp(base, pipesCleared, required, rampMultiplier(difficulty))
    }
}
