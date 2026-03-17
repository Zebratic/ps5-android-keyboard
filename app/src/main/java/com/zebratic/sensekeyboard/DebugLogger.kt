package com.zebratic.sensekeyboard

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Debug logger that writes to a file on the device for remote reading via adb.
 * Toggle on/off from the Debug tab in settings.
 */
object DebugLogger {
    private var enabled = false
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val buffer = mutableListOf<String>()
    private const val MAX_BUFFER = 200

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("sensekeyboard_settings", Context.MODE_PRIVATE)
        enabled = prefs.getBoolean("debug_logging", false)
        if (enabled) {
            logFile = File(context.getExternalFilesDir(null), "sensekeyboard_debug.log")
            log("DEBUG", "Logger initialized")
        }
    }

    fun setEnabled(context: Context, on: Boolean) {
        val prefs = context.getSharedPreferences("sensekeyboard_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("debug_logging", on).apply()
        enabled = on
        if (on) {
            logFile = File(context.getExternalFilesDir(null), "sensekeyboard_debug.log")
            log("DEBUG", "Logging enabled")
        }
    }

    fun isEnabled(): Boolean = enabled

    fun log(tag: String, msg: String) {
        val ts = dateFormat.format(Date())
        val line = "[$ts] $tag: $msg"
        Log.d("SenseKeyboard", line)

        synchronized(buffer) {
            buffer.add(line)
            if (buffer.size > MAX_BUFFER) buffer.removeAt(0)
        }

        if (enabled && logFile != null) {
            try {
                logFile!!.appendText("$line\n")
            } catch (_: Exception) {}
        }
    }

    fun getRecentLogs(): List<String> {
        synchronized(buffer) {
            return buffer.toList()
        }
    }

    fun clearLogs() {
        synchronized(buffer) { buffer.clear() }
        try { logFile?.writeText("") } catch (_: Exception) {}
    }

    fun getLogFilePath(): String = logFile?.absolutePath ?: "Not initialized"
}
