package org.lambertland.kxpilot.client

import org.lambertland.kxpilot.common.GameConst

// ---------------------------------------------------------------------------
// Ported from: client/netclient.h
// ---------------------------------------------------------------------------

/**
 * Display parameters sent from the client to the server.
 * Maps to C `display_t` in client/netclient.h.
 */
data class DisplayConfig(
    val viewWidth: Int,
    val viewHeight: Int,
    val sparkRand: Int,
    val numSparkColors: Int,
)

/**
 * A pointer (mouse) movement packet.
 * Maps to C `pointer_move_t` in client/netclient.h.
 */
data class PointerMove(
    val movement: Int,
    val turnspeed: Double,
    val id: Int,
)

/**
 * Network-layer constants.
 * Maps to the `#define` values in client/netclient.h.
 */
object NetConst {
    const val MIN_RECEIVE_WINDOW_SIZE: Int = 1
    const val MAX_RECEIVE_WINDOW_SIZE: Int = 4
    const val MAX_SUPPORTED_FPS: Int = GameConst.MAX_SERVER_FPS
    const val MAX_POINTER_MOVES: Int = 128
}
