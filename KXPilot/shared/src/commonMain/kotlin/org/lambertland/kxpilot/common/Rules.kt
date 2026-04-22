package org.lambertland.kxpilot.common

// ---------------------------------------------------------------------------
// Ported from: common/rules.h
// ---------------------------------------------------------------------------

/**
 * Game-mode bit flags.  The [Rules.mode] field is a bitfield of these values.
 * Maps to the `#define` constants in common/rules.h.
 */
object GameMode {
    val CRASH_WITH_PLAYER: Long = 1L shl 0
    val BOUNCE_WITH_PLAYER: Long = 1L shl 1
    val PLAYER_KILLINGS: Long = 1L shl 2
    val LIMITED_LIVES: Long = 1L shl 3
    val TIMING: Long = 1L shl 4
    val PLAYER_SHIELDING: Long = 1L shl 6
    val LIMITED_VISIBILITY: Long = 1L shl 7
    val TEAM_PLAY: Long = 1L shl 8
    val WRAP_PLAY: Long = 1L shl 9
    val ALLOW_NUKES: Long = 1L shl 10
    val ALLOW_CLUSTERS: Long = 1L shl 11
    val ALLOW_MODIFIERS: Long = 1L shl 12
    val ALLOW_LASER_MODIFIERS: Long = 1L shl 13
    val ALLIANCES: Long = 1L shl 14

    /** Subset of mode flags the client cares about. */
    val CLIENT_RULES_MASK: Long =
        WRAP_PLAY or TEAM_PLAY or TIMING or LIMITED_LIVES or ALLIANCES
}

/**
 * Legacy player-status bits used only in the network protocol.
 * Maps to `OLD_PLAYING`, `OLD_PAUSE`, `OLD_GAME_OVER` in common/rules.h.
 */
object OldPlayerStatus {
    val OLD_PLAYING: UInt = 1u shl 0
    val OLD_PAUSE: UInt = 1u shl 1
    val OLD_GAME_OVER: UInt = 1u shl 2
}

/**
 * Server game configuration (lives + mode).
 * Maps to C `rules_t { int lives; long mode; }` in common/rules.h.
 */
data class Rules(
    val lives: Int,
    val mode: Long,
)
