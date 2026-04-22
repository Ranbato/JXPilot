package org.lambertland.kxpilot.server

import org.lambertland.kxpilot.common.ClPos

// ---------------------------------------------------------------------------
// Ported from: server/score.h
// ---------------------------------------------------------------------------

/**
 * Special constant score values awarded/deducted in specific situations.
 * Maps to the float `#define` constants in server/score.h.
 */
object ScoreConst {
    /**
     * Sentinel score value meaning "not owned by any real player" — assigned
     * to kills attributed to environment hazards (asteroids, cannons, targets,
     * treasure collisions, walls).  Matches the C server literal `-1436.0f`
     * used in `server/score.c` as a magic unowned-player ID.
     */
    const val UNOWNED_SENTINEL: Double = -1436.0

    const val ASTEROID_SCORE: Double = UNOWNED_SENTINEL
    const val CANNON_SCORE: Double = UNOWNED_SENTINEL
    const val TARGET_SCORE: Double = UNOWNED_SENTINEL
    const val TREASURE_SCORE: Double = UNOWNED_SENTINEL
    const val UNOWNED_SCORE: Double = UNOWNED_SENTINEL
    const val WALL_SCORE: Double = 2000.0
}

/**
 * Scoring event type.
 * Maps to C `scoretype_t` enum in server/score.h.
 */
enum class ScoreType {
    CANNON_KILL,
    WALL_DEATH,
    COLLISION,
    ROADKILL,
    BALL_KILL,
    HIT_MINE,
    EXPLOSION,
    ASTEROID_KILL,
    ASTEROID_DEATH,
    SHOT_DEATH,
    LASER,
    TARGET,
    TREASURE,
    SELF_DESTRUCT,
    SHOVE_KILL,
    SHOVE_DEATH,
}
