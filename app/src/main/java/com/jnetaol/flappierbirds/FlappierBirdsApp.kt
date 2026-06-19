package com.jnetaol.flappierbirds

import android.app.Application
import com.jnetaol.flappierbirds.data.repository.GameRepository
import com.jnetaol.flappierbirds.logger.DebugLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FlappierBirdsApp : Application() {
    val repository by lazy { GameRepository(this) }
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        try {
            DebugLogger.init(this, enableExtraLogging = false)
            DebugLogger.i("FB-000", "FlappierBirdsApp starting", null)
        } catch (e: Exception) {
            android.util.Log.e("FlappierBirds", "Logger init failed", e)
        }

        applicationScope.launch {
            try {
                repository.initDefaults()
                DebugLogger.i("FB-004", "Database defaults initialized", null)
            } catch (e: Exception) {
                DebugLogger.logException("FB-005", "Failed to initialize defaults", e)
            }
        }

        try {
            val diagnostics = DebugLogger.runDiagnostics(this)
            DebugLogger.i("FB-010", "Startup diagnostics: ${diagnostics.summary()}", null)
        } catch (e: Exception) {
            android.util.Log.e("FlappierBirds", "Diagnostics failed", e)
        }
    }
}
