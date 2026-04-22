package org.lambertland.kxpilot.ui.stateholder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.lambertland.kxpilot.model.TalkResult

/**
 * UI-layer state for the chat/talk overlay.
 *
 * Open with [open], cancel with [close], submit with [submit].
 * History navigation: [browseHistory] with delta +1 (older) / -1 (newer).
 */
class TalkStateHolder {
    var isVisible by mutableStateOf(false)
    var text by mutableStateOf("")
    private val history = ArrayDeque<String>(32)
    private var historyPos = -1

    fun open() {
        text = ""
        historyPos = -1
        isVisible = true
    }

    fun close() {
        isVisible = false
    }

    /**
     * Submit the current message.
     * Returns [TalkResult.Send] with the text, or [TalkResult.Cancel] if blank.
     * In both cases the overlay is hidden.
     */
    fun submit(): TalkResult {
        val msg = text.trim()
        isVisible = false
        return if (msg.isEmpty()) {
            TalkResult.Cancel
        } else {
            if (history.isEmpty() || history.first() != msg) {
                history.addFirst(msg)
                if (history.size > 32) history.removeLast()
            }
            text = ""
            TalkResult.Send(msg)
        }
    }

    /** Browse history; [delta] = +1 older, -1 newer. */
    fun browseHistory(delta: Int) {
        if (history.isEmpty()) return
        historyPos = (historyPos + delta).coerceIn(-1, history.size - 1)
        text = if (historyPos < 0) "" else history[historyPos]
    }
}
