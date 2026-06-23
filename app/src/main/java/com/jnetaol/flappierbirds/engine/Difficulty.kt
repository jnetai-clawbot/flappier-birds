package com.jnetaol.flappierbirds.engine

enum class Difficulty(
    val id: String,
    val label: String,
    private val gravityMult: Float,
    private val flapMult: Float,
    private val gapMult: Float,
    private val speedMult: Float,
    private val spawnDistMult: Float
) {
    TOO_EASY("too_easy", "Too Easy", 0.72f, 1.18f, 1.55f, 0.68f, 1.25f),
    EASY("easy", "Easy", 0.82f, 1.12f, 1.32f, 0.78f, 1.15f),
    MID("mid", "Mid", 0.9f, 1.06f, 1.15f, 0.9f, 1.08f),
    HARD("hard", "Hard", 1.05f, 0.98f, 0.92f, 1.08f, 0.94f),
    EXTREME("extreme", "Extreme", 1f, 1f, 1f, 1f, 1f),
    IMPOSSIBLE("impossible", "Impossible", 1.18f, 0.9f, 0.72f, 1.28f, 0.82f);

    fun applyTo(
        gravity: Float,
        flapStrength: Float,
        pipeGap: Float,
        pipeSpeed: Float,
        pipeSpawnDistance: Float,
        maxVelocity: Float,
        minVelocity: Float
    ): AppliedDifficulty = AppliedDifficulty(
        gravity = gravity * gravityMult,
        flapStrength = flapStrength * flapMult,
        pipeGap = pipeGap * gapMult,
        pipeSpeed = pipeSpeed * speedMult,
        pipeSpawnDistance = pipeSpawnDistance * spawnDistMult,
        maxVelocity = maxVelocity * (gravityMult * 0.4f + 0.6f),
        minVelocity = minVelocity * (flapMult * 0.4f + 0.6f)
    )

    data class AppliedDifficulty(
        val gravity: Float,
        val flapStrength: Float,
        val pipeGap: Float,
        val pipeSpeed: Float,
        val pipeSpawnDistance: Float,
        val maxVelocity: Float,
        val minVelocity: Float
    )

    companion object {
        val DEFAULT = EXTREME

        fun fromId(id: String): Difficulty =
            entries.find { it.id == id } ?: DEFAULT
    }
}
