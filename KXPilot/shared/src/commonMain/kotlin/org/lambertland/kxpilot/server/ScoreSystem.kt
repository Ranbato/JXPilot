package org.lambertland.kxpilot.server

// ---------------------------------------------------------------------------
// ScoreSystem
// ---------------------------------------------------------------------------
//
// Handles kill credit and scoring adjustments for death events.
//
// Ported from server/score.c (`Score_death`, `Score_kill`, `Score_wall_death`).
//
// Simplified rules:
//   - Wall death: attacker (last player to touch) gets +1 kill; victim gets
//     +1 death and -1 score per WALL_DEATH_SCORE_PENALTY.
//   - Player kill (collision/shot): killer gets +1 kill + KILL_SCORE bonus;
//     victim gets +1 death - KILL_SCORE penalty.
//   - Self-kill: just +1 death, no kill credit.

/**
 * Stateless scoring engine.
 *
 * All methods mutate [Player] objects in-place (matching the mutable C model).
 */
object ScoreSystem {
    /** Score awarded to the killer per kill. */
    private const val KILL_SCORE: Double = 1.0

    /** Score deducted from the victim per death (wall or player kill). */
    private const val DEATH_PENALTY: Double = 1.0

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Apply scoring for a wall-death event.
     *
     * The last player to cause physical contact with the victim (the "attacker")
     * gets kill credit.  If no attacker is known (wall killed spontaneously),
     * only the victim's death counter is incremented.
     *
     * @param victim   The player who died.
     * @param attacker The player credited with the kill, or null if environment kill.
     */
    fun wallDeath(
        victim: Player,
        attacker: Player?,
    ) {
        applyDeathPenalty(victim)

        if (attacker != null && attacker !== victim) {
            attacker.kills++
            attacker.score += KILL_SCORE
            attacker.updateScore = true
        }
    }

    /**
     * Apply scoring for a direct player-kills-player event (collision, shot, laser).
     *
     * Self-kills (victim == killer) only increment the victim's death counter.
     *
     * @param victim The player who died.
     * @param killer The player credited with the kill.
     */
    fun playerKill(
        victim: Player,
        killer: Player,
    ) {
        applyDeathPenalty(victim)

        if (killer !== victim) {
            killer.kills++
            killer.score += KILL_SCORE
            killer.updateScore = true
        }
    }

    /**
     * Apply scoring for a self-destruct or environment kill (cannon, asteroid)
     * where no player gets kill credit.
     *
     * @param victim The player who died.
     */
    fun environmentKill(victim: Player) {
        applyDeathPenalty(victim)
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private fun applyDeathPenalty(victim: Player) {
        victim.deaths++
        victim.score -= DEATH_PENALTY
        victim.updateScore = true
    }
}
