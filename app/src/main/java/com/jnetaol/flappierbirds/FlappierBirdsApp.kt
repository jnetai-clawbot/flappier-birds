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
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        DebugLogger.init(this, enableExtraLogging = false)
        DebugLogger.i("FB-000", "FlappierBirdsApp starting", null)

        applicationScope.launch {
            try {
                repository.initDefaults()
                DebugLogger.i("FB-004", "Database defaults initialized", null)
            } catch (e: Exception) {
                DebugLogger.logException("FB-005", "Failed to initialize defaults", e)
            }
        }

        val diagnostics = DebugLogger.runDiagnostics(this)
        DebugLogger.i("FB-010", "Startup diagnostics: ${diagnostics.summary()}", null)
    }
}
