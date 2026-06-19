package com.jnetaol.flappierbirds.logger

import android.content.Context
import android.os.Build
import android.os.Environment
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DebugLogger {
    private const val TAG = "FB"
    private const val LOG_DIR = "logs"
    private const val MAX_LOG_FILES = 10
    private const val MAX_LOG_SIZE_BYTES = 5 * 1024 * 1024

    private val _logFlow = MutableSharedFlow<LogEntry>(replay = 50)
    val logFlow: SharedFlow<LogEntry> = _logFlow.asSharedFlow()

    private var logFile: File? = null
    private var writer: FileWriter? = null
    private var initialized = false
    private var extraLogging = false

    data class LogEntry(
        val timestamp: String,
        val level: String,
        val errorCode: String,
        val message: String,
        val stackTrace: String?
    )

    fun init(context: Context, enableExtraLogging: Boolean = false) {
        if (initialized) return
        initialized = true
        extraLogging = enableExtraLogging

        try {
            val logDir = File(context.filesDir, LOG_DIR)
            if (!logDir.exists()) logDir.mkdirs()

            rotateLogs(logDir)

            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            logFile = File(logDir, "flappier_birds_${dateStr}.log")
            writer = FileWriter(logFile, true)

            i("FB-000", "DebugLogger initialized", null)
            i("FB-001", "App version: ${getAppVersion(context)}", null)
            i("FB-002", "Android SDK: ${Build.VERSION.SDK_INT}, Device: ${Build.MODEL}", null)
            i("FB-003", "Extra logging: $enableExtraLogging", null)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to initialize DebugLogger", e)
        }
    }

    private fun rotateLogs(logDir: File) {
        val logFiles = logDir.listFiles { f -> f.name.startsWith("flappier_birds_") && f.name.endsWith(".log") }
            ?.sortedByDescending { it.lastModified() } ?: return

        if (logFiles.size >= MAX_LOG_FILES) {
            logFiles.drop(MAX_LOG_FILES - 1).forEach { it.delete() }
        }
    }

    fun i(errorCode: String, message: String, stackTrace: String?) {
        log("INFO", errorCode, message, stackTrace)
    }

    fun w(errorCode: String, message: String, stackTrace: String?) {
        log("WARN", errorCode, message, stackTrace)
    }

    fun e(errorCode: String, message: String, stackTrace: String?) {
        log("ERROR", errorCode, message, stackTrace)
    }

    fun d(errorCode: String, message: String, stackTrace: String?) {
        if (extraLogging) log("DEBUG", errorCode, message, stackTrace)
    }

    private fun log(level: String, errorCode: String, message: String, stackTrace: String?) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val entry = LogEntry(timestamp, level, errorCode, message, stackTrace)

        try {
            val line = "[$timestamp] [$level] [$errorCode] $message"
            writer?.apply {
                write(line + "\n")
                if (stackTrace != null) {
                    write("  Stack: $stackTrace\n")
                }
                flush()
            }

            if (logFile != null && logFile!!.length() > MAX_LOG_SIZE_BYTES) {
                writer?.close()
                val logDir = logFile!!.parentFile!!
                val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                logFile = File(logDir, "flappier_birds_${dateStr}.log")
                writer = FileWriter(logFile, true)
                rotateLogs(logDir)
            }
        } catch (_: Exception) {}

        _logFlow.tryEmit(entry)

        when (level) {
            "ERROR" -> android.util.Log.e(TAG, "[$errorCode] $message")
            "WARN" -> android.util.Log.w(TAG, "[$errorCode] $message")
            "DEBUG" -> if (extraLogging) android.util.Log.d(TAG, "[$errorCode] $message")
            else -> android.util.Log.i(TAG, "[$errorCode] $message")
        }
    }

    fun logException(errorCode: String, message: String, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        e(errorCode, message, sw.toString())
    }

    fun getLogFiles(context: Context): List<File> {
        val logDir = File(context.filesDir, LOG_DIR)
        return logDir.listFiles { f -> f.name.endsWith(".log") }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun exportLogs(context: Context): File? {
        return try {
            val zipFile = File(context.cacheDir, "flappier_birds_logs.zip")
            ZipOutputStream(zipFile.outputStream()).use { zos ->
                getLogFiles(context).forEach { file ->
                    zos.putNextEntry(ZipEntry(file.name))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
            zipFile
        } catch (e: Exception) {
            logException("FB-900", "Failed to export logs", e)
            null
        }
    }

    fun runDiagnostics(context: Context): DiagnosticsReport {
        val report = DiagnosticsReport()
        report.appVersion = getAppVersion(context)
        report.androidSdk = Build.VERSION.SDK_INT
        report.deviceModel = Build.MODEL
        report.deviceManufacturer = Build.MANUFACTURER
        report.availableMemory = Runtime.getRuntime().maxMemory()
        report.freeMemory = Runtime.getRuntime().freeMemory()
        report.totalMemory = Runtime.getRuntime().totalMemory()

        try {
            val dbFile = context.getDatabasePath("flappier_birds.db")
            report.databaseExists = dbFile.exists()
            report.databaseSize = if (dbFile.exists()) dbFile.length() else 0
        } catch (e: Exception) {
            report.databaseExists = false
            report.databaseSize = 0
        }

        try {
            val prefsFile = File(context.filesDir.parent!!, "shared_prefs/FlappierBirdsPrefs.xml")
            report.prefsExists = prefsFile.exists()
        } catch (e: Exception) {
            report.prefsExists = false
        }

        report.logFileCount = getLogFiles(context).size
        report.totalLogSize = getLogFiles(context).sumOf { it.length() }

        i("FB-010", "Diagnostics complete: ${report.summary()}", null)
        return report
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pkgInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    data class DiagnosticsReport(
        var appVersion: String = "",
        var androidSdk: Int = 0,
        var deviceModel: String = "",
        var deviceManufacturer: String = "",
        var availableMemory: Long = 0,
        var freeMemory: Long = 0,
        var totalMemory: Long = 0,
        var databaseExists: Boolean = false,
        var databaseSize: Long = 0,
        var prefsExists: Boolean = false,
        var logFileCount: Int = 0,
        var totalLogSize: Long = 0
    ) {
        fun summary(): String {
            return "v$appVersion | SDK $androidSdk | ${deviceManufacturer} ${deviceModel} | " +
                    "Mem: ${freeMemory / 1024 / 1024}MB free | DB: $databaseExists (${databaseSize}B) | " +
                    "Logs: $logFileCount files (${totalLogSize}B)"
        }
    }
}
