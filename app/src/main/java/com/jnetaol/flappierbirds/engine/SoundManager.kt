package com.jnetaol.flappierbirds.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.jnetaol.flappierbirds.logger.DebugLogger

class SoundManager(context: Context) {
    private var soundPool: SoundPool? = null
    private var musicEnabled = true
    private var soundEnabled = true
    private var loaded = false

    private val soundIds = mutableMapOf<String, Int>()

    init {
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(attrs)
                .build()

            soundPool?.setOnLoadCompleteListener { _, _, status ->
                if (status == 0) loaded = true
            }
        } catch (e: Exception) {
            DebugLogger.logException("FB-100", "Failed to initialize SoundPool", e)
        }
    }

    fun loadSounds(context: Context) {
        try {
            val rawDir = context.resources.getIdentifier("raw", null, context.packageName)
        } catch (_: Exception) {}
    }

    fun playFlap() {
        if (!soundEnabled) return
    }

    fun playScore() {
        if (!soundEnabled) return
    }

    fun playCoin() {
        if (!soundEnabled) return
    }

    fun playHit() {
        if (!soundEnabled) return
    }

    fun playAchievement() {
        if (!soundEnabled) return
    }

    fun setMusicEnabled(enabled: Boolean) {
        musicEnabled = enabled
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }

    fun isMusicEnabled(): Boolean = musicEnabled
    fun isSoundEnabled(): Boolean = soundEnabled

    fun release() {
        soundPool?.release()
        soundPool = null
    }
}
