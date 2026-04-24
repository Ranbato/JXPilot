package org.lambertland.kxpilot

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Application-wide in-memory log buffer.
 *
 * Stores up to [MAX_ENTRIES] log lines (oldest entries are dropped when the
 * buffer is full).  A [StateFlow] of the current snapshot is exposed so
 * Compose UI can recompose whenever new lines arrive.
 *
 * Thread-safety: [log] may be called from any thread; internal state is
 * protected with [synchronized].
 */
object AppLogger {
    const val MAX_ENTRIES = 100

    private val _entries = MutableStateFlow<List<String>>(emptyList())

    /** Observable snapshot of the last [MAX_ENTRIES] log lines (newest last). */
    val entries: StateFlow<List<String>> = _entries.asStateFlow()

    private val buffer = ArrayDeque<String>(MAX_ENTRIES + 1)

    /**
     * Append [message] to the log buffer.  If [MAX_ENTRIES] is exceeded the
     * oldest entry is removed.
     */
    fun log(message: String) {
        val snapshot: List<String>
        synchronized(buffer) {
            buffer.addLast(message)
            if (buffer.size > MAX_ENTRIES) buffer.removeFirst()
            snapshot = buffer.toList()
        }
        _entries.value = snapshot
    }

    /**
     * Return all current log entries joined by newlines (suitable for saving
     * to a plain-text file).
     */
    fun dump(): String = synchronized(buffer) { buffer.joinToString("\n") }

    /** Clear the log buffer. */
    fun clear() {
        synchronized(buffer) { buffer.clear() }
        _entries.value = emptyList()
    }
}
