package org.lambertland.kxpilot.server

// ---------------------------------------------------------------------------
// Ported from: server/rank.h
// ---------------------------------------------------------------------------

/**
 * Immutable identity key for a ranking record.
 *
 * These four fields uniquely identify a player across sessions and are used
 * for `equals`/`hashCode` comparisons (e.g. looking up an existing record in
 * a set or map).  They never change after a player connects.
 *
 * Separated from the mutable stats in [RankNode] so that `data class`
 * structural equality only considers the stable identity — not the
 * ever-changing kill/death counters.
 */
data class RankIdentity(
    val name: String = "",
    val user: String = "",
    val host: String = "",
    val timestamp: Long = 0L,
)

/**
 * Per-player persistent ranking / statistics record.
 * Maps to C `ranknode_t` in server/rank.h.
 *
 * [identity] carries the immutable identity key (see [RankIdentity]).
 * The mutable stat fields ([kills], [deaths], …) are updated as the game
 * progresses and are intentionally excluded from `equals`/`hashCode` —
 * hence they live here rather than in [RankIdentity].
 *
 * [pl] is a back-reference to the live [Player]; set to `null` when the
 * player disconnects.  It is a plain `var` (not in the constructor) so that
 * it is excluded from generated structural equality.
 */
class RankNode(
    val identity: RankIdentity = RankIdentity(),
    var kills: Int = 0,
    var deaths: Int = 0,
    var rounds: Int = 0,
    var shots: Int = 0,
    var deadliest: Int = 0,
    var ballsCashed: Int = 0,
    var ballsSaved: Int = 0,
    var ballsWon: Int = 0,
    var ballsLost: Int = 0,
    var bestball: Double = 0.0,
    var score: Double = 0.0,
    var maxSurvivalTime: Double = 0.0,
) {
    // Identity convenience accessors
    val name: String get() = identity.name
    val user: String get() = identity.user
    val host: String get() = identity.host
    val timestamp: Long get() = identity.timestamp

    /** Live player reference — null when disconnected. */
    var pl: Player? = null
}
