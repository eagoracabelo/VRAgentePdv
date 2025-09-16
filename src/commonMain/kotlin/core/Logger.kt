package core

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object Logger {
    private var logLevel = LogLevel.INFO
    private val logFile = "salt-installer.log"

    enum class LogLevel(val value: Int) {
        DEBUG(0), INFO(1), WARN(2), ERROR(3)
    }

    fun setLevel(level: LogLevel) {
        logLevel = level
    }

    fun debug(message: String) {
        log(LogLevel.DEBUG, message)
    }

    fun info(message: String) {
        log(LogLevel.INFO, message)
    }

    fun warn(message: String) {
        log(LogLevel.WARN, message)
    }

    fun error(message: String) {
        log(LogLevel.ERROR, message)
    }

    private fun log(level: LogLevel, message: String) {
        if (level.value < logLevel.value) return

        val timestamp = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .toString()

        val logEntry = "[$timestamp] [${level.name}] $message"

        // Console output
        when (level) {
            LogLevel.ERROR -> println("❌ $message")
            LogLevel.WARN -> println("⚠️  $message")
            LogLevel.INFO -> println("ℹ️  $message")
            LogLevel.DEBUG -> if (logLevel == LogLevel.DEBUG) println("🔍 $message")
        }

        // File output (platform-specific implementation needed)
        writeToFile(logEntry)
    }

    private fun writeToFile(entry: String) {
        // This will be implemented in platform-specific code
        try {
            appendToLogFile(entry)
        } catch (e: Exception) {
            // Silently fail if we can't write to log file
        }
    }
}

expect fun appendToLogFile(entry: String)