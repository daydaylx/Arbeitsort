package de.montagezeit.app.logging

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lokaler Ringbuffer Logger für Debug-Zwecke
 * 
 * Speichert Logs rotierend in einer Datei (1-2MB).
 * Keine Cloud/Netzwerk-Kommunikation.
 */
@Singleton
class RingBufferLogger @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val logFile: File
    private val maxFileSize = 2 * 1024 * 1024L // 2MB
    private val timestampFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.GERMAN)
    }
    
    init {
        val logDir = File(context.filesDir, "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        logFile = File(logDir, "debug.log")
    }
    
    /**
     * Loggt eine Nachricht
     * 
     * @param level Log-Level (DEBUG, INFO, WARN, ERROR)
     * @param tag Tag für Kategorisierung
     * @param message Die Nachricht
     * @param throwable Optionales Exception-Objekt
     */
    suspend fun log(level: Level, tag: String, message: String, throwable: Throwable? = null) {
        withContext(Dispatchers.IO) {
            try {
                val timestamp = timestampFormat.get()!!.format(Date())
                val stackTrace = throwable?.let { "\n${it.stackTraceToString()}" } ?: ""
                
                val logLine = "$timestamp [${level.name}] [$tag] $message$stackTrace\n"
                
                // Rotieren wenn Datei zu groß
                if (logFile.exists() && logFile.length() > maxFileSize) {
                    rotateLog()
                }
                
                // Anhängen
                logFile.appendText(logLine)
            } catch (e: Exception) {
                // Silent fail - Logger sollte App nicht abstürzen lassen
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Liest alle Logs aus der Datei
     * 
     * @return Die Log-Nachrichten als String
     */
    suspend fun readLogs(): String = withContext(Dispatchers.IO) {
        return@withContext if (logFile.exists()) {
            logFile.readText()
        } else {
            "Keine Logs vorhanden"
        }
    }
    
    /**
     * Löscht alle Logs
     */
    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        if (logFile.exists()) {
            logFile.delete()
        }
    }
    
    /**
     * Rotiert die Log-Datei (löscht älteste Einträge)
     */
    private fun rotateLog() {
        try {
            val lines = logFile.readLines()
            
            // Behalte nur die ersten 50% der Zeilen
            val keepLines = (lines.size * 0.5).toInt()
            val newContent = lines.take(keepLines).joinToString("\n") + "\n"
            
            logFile.writeText(newContent)
        } catch (e: Exception) {
            // Bei Fehler einfach komplett löschen
            logFile.delete()
        }
    }
    
    /**
     * Log-Level Enum
     */
    enum class Level {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
}

/**
 * Convenience-Funktionen für den Logger
 */
suspend fun RingBufferLogger.d(tag: String, message: String, throwable: Throwable? = null) {
    log(RingBufferLogger.Level.DEBUG, tag, message, throwable)
}

suspend fun RingBufferLogger.i(tag: String, message: String, throwable: Throwable? = null) {
    log(RingBufferLogger.Level.INFO, tag, message, throwable)
}

suspend fun RingBufferLogger.w(tag: String, message: String, throwable: Throwable? = null) {
    log(RingBufferLogger.Level.WARN, tag, message, throwable)
}

suspend fun RingBufferLogger.e(tag: String, message: String, throwable: Throwable? = null) {
    log(RingBufferLogger.Level.ERROR, tag, message, throwable)
}
