package com.jnetaol.flappierbirds.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.min

/**
 * Maps the fixed 288x512 Flappy Bird playfield onto any screen size.
 * Uses uniform scaling with letterboxing so gameplay proportions stay consistent.
 */
data class GameViewport(
    val screenWidth: Float,
    val screenHeight: Float,
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val virtualWidth: Float = VIRTUAL_WIDTH,
    val virtualHeight: Float = VIRTUAL_HEIGHT
) {
    val virtualSize: Size get() = Size(virtualWidth, virtualHeight)

    fun toScreenX(x: Float): Float = offsetX + x * scale
    fun toScreenY(y: Float): Float = offsetY + y * scale
    fun toScreen(offset: Offset): Offset = Offset(toScreenX(offset.x), toScreenY(offset.y))
    fun toScreen(size: Float): Float = size * scale

    fun textSize(baseVirtual: Float): Float = (baseVirtual * scale).coerceAtLeast(18f)

    companion object {
        const val VIRTUAL_WIDTH = 288f
        const val VIRTUAL_HEIGHT = 512f

        fun from(screenWidth: Float, screenHeight: Float): GameViewport {
            if (screenWidth <= 0f || screenHeight <= 0f) {
                return GameViewport(screenWidth, screenHeight, 1f, 0f, 0f)
            }

            val scale = min(
                screenWidth / VIRTUAL_WIDTH,
                screenHeight / VIRTUAL_HEIGHT
            )
            val contentWidth = VIRTUAL_WIDTH * scale
            val contentHeight = VIRTUAL_HEIGHT * scale

            return GameViewport(
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                scale = scale,
                offsetX = (screenWidth - contentWidth) / 2f,
                offsetY = (screenHeight - contentHeight) / 2f
            )
        }
    }
}
