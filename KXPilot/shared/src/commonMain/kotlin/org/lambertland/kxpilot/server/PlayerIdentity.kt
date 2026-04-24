package org.lambertland.kxpilot.server

// ---------------------------------------------------------------------------
// Ported from: server/player.h  (identity / network fields)
// ---------------------------------------------------------------------------

/**
 * Mutable snapshot of a player's identity and session metadata.
 *
 * Extracted from [Player] as part of the god-object decomposition (D1).
 * All fields here are set at connection time and do not change during normal
 * gameplay (unlike physics state which changes every tick).
 *
 * [Player] holds a mutable [PlayerIdentity] reference and exposes delegating
 * properties for every field so all existing call sites compile unchanged.
 *
 * R24/R25: plain `class` not `data class` — all fields are `var`, so the
 * generated `equals`/`hashCode` from `data class` would produce incorrect
 * results after any mutation, and `copy()` would silently snapshot stale state.
 * Identity equality (reference equality) is the correct semantic here.
 */
class PlayerIdentity(
    /** Single-character display token (maps to C `pl_mychar`). */
    var myChar: Char = ' ',
    /** Player's display name (maps to C `pl_name`). */
    var name: String = "",
    /** OS username of the player (maps to C `pl_username`). */
    var userName: String = "",
    /** Hostname of the player's machine (maps to C `pl_hostname`). */
    var hostname: String = "",
    /** Pseudo-team assignment (maps to C `pl_pseudo_team`). */
    var pseudoTeam: UShort = 0u,
    /** Alliance bitmask (maps to C `pl_alliance`). */
    var alliance: Int = 0,
    /** Pending alliance invite (maps to C `pl_invite`). */
    var invite: Int = 0,
    /** Player type: human, robot, or tank (maps to C `pl_type`). */
    var plType: PlayerType = PlayerType.HUMAN,
    /** Single-char representation of [plType] (maps to C `pl_type_mychar`). */
    var plTypeMyChar: Char = ' ',
    /** XPilot client version reported at login (maps to C `pl_version`). */
    var version: UInt = 0u,
    /** Whether this player is muted (server-side flag). */
    var muted: Boolean = false,
    /** Whether this player has operator privileges. */
    var isOperator: Boolean = false,
    /** Whether this player wants audio (maps to C `pl_want_audio`). */
    var wantAudio: Boolean = false,
    /** Privilege bitmask (maps to C `pl_privs`). */
    var privs: Int = 0,
    /** Rank node for leaderboard tracking (maps to C `pl_rank`). */
    var rank: RankNode? = null,
)
