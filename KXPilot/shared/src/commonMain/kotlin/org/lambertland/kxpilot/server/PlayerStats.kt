package org.lambertland.kxpilot.server

// ---------------------------------------------------------------------------
// PlayerStats
//
// Score / bookkeeping fields extracted from the Player god-object (BL-21).
//
// Player holds a `val stats: PlayerStats` and exposes delegating properties
// for every field here so all existing call sites compile unchanged.
//
// Fields kept here (score/race/bookkeeping):
//   score, updateScore, kills, deaths,
//   plLife, plDeathsSinceJoin, plPrevTeam,
//   survivalTime,
//   check, prevCheck, time, round, prevRound,
//   bestLap, lastLap, lastLapTime, lastCheckDir,
//   ecmCount, snafuCount, fs
// ---------------------------------------------------------------------------

/**
 * Score and race-bookkeeping state for a player.
 *
 * Extracted from [Player] so that score logic can be tested without
 * constructing a full [Player] with physics and identity scaffolding.
 *
 * Maps to the corresponding fields in C `player_t` (server/player.h).
 */
class PlayerStats {
    // Score / kills
    var score: Double = 0.0
    var updateScore: Boolean = false
    var kills: Int = 0
    var deaths: Int = 0

    // Lifecycle counters
    var plLife: Int = 0
    var plDeathsSinceJoin: Int = 0
    var plPrevTeam: UShort = 0u

    // Survival / race
    var survivalTime: Double = 0.0

    // Checkpoints / laps (race mode)
    var check: Int = 0
    var prevCheck: Int = 0
    var time: Int = 0
    var round: Int = 0
    var prevRound: Int = 0
    var bestLap: Int = 0
    var lastLap: Int = 0
    var lastLapTime: Int = 0
    var lastCheckDir: Int = 0

    // Misc counters
    var ecmCount: Int = 0
    var snafuCount: Double = 0.0
    var fs: Int = 0

    // -----------------------------------------------------------------------
    // Reset
    // -----------------------------------------------------------------------

    /** Zero all fields back to defaults. */
    internal fun reset() {
        score = 0.0
        updateScore = false
        kills = 0
        deaths = 0
        plLife = 0
        plDeathsSinceJoin = 0
        plPrevTeam = 0u
        survivalTime = 0.0
        check = 0
        prevCheck = 0
        time = 0
        round = 0
        prevRound = 0
        bestLap = 0
        lastLap = 0
        lastLapTime = 0
        lastCheckDir = 0
        ecmCount = 0
        snafuCount = 0.0
        fs = 0
    }
}
