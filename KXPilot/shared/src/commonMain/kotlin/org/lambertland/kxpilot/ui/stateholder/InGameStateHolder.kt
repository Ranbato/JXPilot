package org.lambertland.kxpilot.ui.stateholder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.lambertland.kxpilot.model.MessageColor
import org.lambertland.kxpilot.model.MessageEntry
import org.lambertland.kxpilot.model.PlayerInfo
import org.lambertland.kxpilot.model.TalkResult

/**
 * UI-layer state for the in-game screen.
 *
 * Owns:
 * - [showScoreboard] — Tab toggle
 * - [talkState] — chat input state holder
 * - [hudMessages] — scrolling message log
 * - [players] — current scoreboard entries
 *
 * Created via `remember { InGameStateHolder() }` in InGameScreen.
 * Callers should seed [hudMessages] and [players] after construction.
 */
class InGameStateHolder {
    var showScoreboard by mutableStateOf(false)
    val talkState = TalkStateHolder()

    var hudMessages by mutableStateOf<List<MessageEntry>>(emptyList())

    var players by mutableStateOf<List<PlayerInfo>>(emptyList())

    fun toggleScoreboard() {
        showScoreboard = !showScoreboard
    }

    fun openTalk() {
        talkState.open()
    }

    /**
     * Handle a submitted talk message.  Adds a "[You]" entry to [hudMessages]
     * if the result is [TalkResult.Send], using [nowMs] as the arrival time.
     */
    fun submitTalk(nowMs: Long): TalkResult {
        val result = talkState.submit()
        if (result is TalkResult.Send) {
            appendMessage("[You] ${result.message}", MessageColor.NORMAL, nowMs)
        }
        return result
    }

    fun appendMessage(
        text: String,
        color: MessageColor = MessageColor.NORMAL,
        nowMs: Long,
    ) {
        hudMessages = (hudMessages + MessageEntry(text, color, nowMs)).takeLast(32)
    }
}
