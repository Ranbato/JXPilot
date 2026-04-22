package org.lambertland.kxpilot.client

// ---------------------------------------------------------------------------
// Ported from: client/client.h  (message / selection types)
// ---------------------------------------------------------------------------

/**
 * Ball-mode status flags used in messages.
 * Maps to C `msg_bms_t` enum in client/client.h.
 */
enum class MsgBms {
    NONE,
    BALL,
    SAFE,
    COVER,
    POP,
}

/**
 * A single display message (chat or game event).
 * Maps to C `message_t` in client/client.h.
 */
data class Message(
    val txt: String,
    val lifeTime: Double,
    val bmsInfo: MsgBms = MsgBms.NONE,
)

// ------------------------------------------------------------------
// Selection state (talk window / draw window text selection)
// ------------------------------------------------------------------

/** State of a text selection in the talk window. */
data class TalkSelection(
    val state: Boolean = false,
    val x1: Int = 0,
    val x2: Int = 0,
    val inclNl: Boolean = false,
)

/** State of a text selection in the draw (game) window. */
data class DrawSelection(
    val state: Boolean = false,
    val x1: Int = 0,
    val x2: Int = 0,
    val y1: Int = 0,
    val y2: Int = 0,
)

/**
 * Combined selection state for both talk and draw windows.
 * Maps to C `selection_t` in client/client.h.
 *
 * Mutable because it is updated interactively during UI events.
 */
class Selection {
    var talk: TalkSelection = TalkSelection()
    var draw: DrawSelection = DrawSelection()
    var txt: String = ""
    var keepEmphasizing: Boolean = false
}
