package org.lambertland.kxpilot.server

import org.lambertland.kxpilot.common.GameMode

// ---------------------------------------------------------------------------
// Ported from: server/modifiers.c  (Mods_set, Mods_filter, Mods_to_string,
//                                   Mods_get, Player_set_modbank)
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// Rules-gated modifier operations
// ---------------------------------------------------------------------------

/**
 * Return true if setting [modifier] to [value] is permitted by [rulesMode].
 *
 * Value 0 (clear) is always allowed.
 * All other values obey the corresponding `ALLOW_*` flag in [rulesMode].
 *
 * Maps to the `allow` logic inside C `Mods_set`.
 */
fun modifierAllowed(
    modifier: Modifier,
    value: Int,
    rulesMode: Long,
): Boolean {
    if (value == 0) return true
    return when (modifier) {
        Modifier.Nuclear -> (rulesMode and GameMode.ALLOW_NUKES) != 0L

        Modifier.Cluster -> (rulesMode and GameMode.ALLOW_CLUSTERS) != 0L

        Modifier.Laser -> (rulesMode and GameMode.ALLOW_LASER_MODIFIERS) != 0L

        // Implosion, Velocity, Mini, Spread, Power
        else -> (rulesMode and GameMode.ALLOW_MODIFIERS) != 0L
    }
}

/**
 * Return a new [Modifiers] with [modifier] set to [value] if allowed by
 * [rulesMode], or `null` if the rules disallow it.
 *
 * Maps to C `Mods_set` return value (-1 on disallowed).
 */
fun Modifiers.trySet(
    modifier: Modifier,
    value: Int,
    rulesMode: Long,
): Modifiers? = if (modifierAllowed(modifier, value, rulesMode)) set(modifier, value) else null

/**
 * Return a copy of [this] with all modifiers stripped that are not permitted
 * by [rulesMode].
 *
 * Maps to C `Mods_filter`.
 */
fun Modifiers.filter(rulesMode: Long): Modifiers {
    var m = this
    if ((rulesMode and GameMode.ALLOW_NUKES) == 0L) {
        m = m.set(Modifier.Nuclear, 0)
    }
    if ((rulesMode and GameMode.ALLOW_CLUSTERS) == 0L) {
        m = m.set(Modifier.Cluster, 0)
    }
    if ((rulesMode and GameMode.ALLOW_MODIFIERS) == 0L) {
        m = m.set(Modifier.Implosion, 0)
        m = m.set(Modifier.Velocity, 0)
        m = m.set(Modifier.Mini, 0)
        m = m.set(Modifier.Spread, 0)
        m = m.set(Modifier.Power, 0)
    }
    if ((rulesMode and GameMode.ALLOW_LASER_MODIFIERS) == 0L) {
        m = m.set(Modifier.Laser, 0)
    }
    return m
}

// ---------------------------------------------------------------------------
// String serialisation / deserialisation
// ---------------------------------------------------------------------------

/**
 * Serialize [this] modifiers to the compact display string used by the HUD
 * and saved modifier banks, e.g. `"FN C V2 X3 Z1 B2 LS"`.
 *
 * Format mirrors C `Mods_to_string`.  KMP-safe: no `String.format`.
 */
fun Modifiers.toModString(): String {
    val sb = StringBuilder()

    fun sep() {
        if (sb.isNotEmpty()) sb.append(' ')
    }

    // Nuclear: F (fullnuclear bit), N (nuclear bit)
    val nuc = get(Modifier.Nuclear)
    if (nuc and Modifier.FULLNUCLEAR != 0) sb.append('F')
    if (nuc and Modifier.NUCLEAR != 0) sb.append('N')

    if (get(Modifier.Cluster) != 0) sb.append('C')
    if (get(Modifier.Implosion) != 0) sb.append('I')

    val vel = get(Modifier.Velocity)
    if (vel != 0) {
        sep()
        sb.append('V')
        sb.append(vel)
    }

    // Mini display is 1-indexed: X2 means mini=1, X3 means mini=2, X4 means mini=3
    val mini = get(Modifier.Mini)
    if (mini != 0) {
        sep()
        sb.append('X')
        sb.append(mini + 1)
    }

    val spread = get(Modifier.Spread)
    if (spread != 0) {
        sep()
        sb.append('Z')
        sb.append(spread)
    }

    val power = get(Modifier.Power)
    if (power != 0) {
        sep()
        sb.append('B')
        sb.append(power)
    }

    val laser = get(Modifier.Laser)
    if (laser != 0) {
        sep()
        sb.append('L')
        if (laser and Modifier.LASER_STUN != 0) sb.append('S')
        if (laser and Modifier.LASER_BLIND != 0) sb.append('B')
    }

    return sb.toString()
}

/**
 * Parse a modifier bank string (e.g. `"FN C V2 X3 Z1 B2 LS"`) into a
 * [Modifiers] word, clamping values to valid ranges.
 *
 * Unknown characters are silently ignored, matching C `Player_set_modbank`.
 * Rules-gating is NOT applied here — call [filter] on the result if needed.
 */
fun modifiersFromString(str: String): Modifiers {
    var mods = Modifiers.ZERO
    var i = 0
    while (i < str.length) {
        when (str[i].uppercaseChar()) {
            'F' -> {
                // FN = full-nuclear; bare F is ignored (must be followed by N)
                if (i + 1 < str.length && str[i + 1].uppercaseChar() == 'N') {
                    mods = mods.set(Modifier.Nuclear, Modifier.NUCLEAR or Modifier.FULLNUCLEAR)
                    i++ // skip the N
                }
            }

            'N' -> {
                // Only set Nuclear if not already set (FN takes priority)
                if (mods.get(Modifier.Nuclear) == 0) {
                    mods = mods.set(Modifier.Nuclear, Modifier.NUCLEAR)
                }
            }

            'C' -> {
                mods = mods.set(Modifier.Cluster, 1)
            }

            'I' -> {
                mods = mods.set(Modifier.Implosion, 1)
            }

            'V' -> {
                val v = parseDigit(str, i + 1, 0, Modifier.VELOCITY_MAX)
                mods = mods.set(Modifier.Velocity, v)
                if (i + 1 < str.length && str[i + 1].isDigit()) i++
            }

            'X' -> {
                // X2 → mini=1, X4 → mini=3; value is 1-indexed in string
                val raw = parseDigit(str, i + 1, 1, Modifier.MINI_MAX + 1)
                mods = mods.set(Modifier.Mini, (raw - 1).coerceIn(0, Modifier.MINI_MAX))
                if (i + 1 < str.length && str[i + 1].isDigit()) i++
            }

            'Z' -> {
                val v = parseDigit(str, i + 1, 0, Modifier.SPREAD_MAX)
                mods = mods.set(Modifier.Spread, v)
                if (i + 1 < str.length && str[i + 1].isDigit()) i++
            }

            'B' -> {
                val v = parseDigit(str, i + 1, 0, Modifier.POWER_MAX)
                mods = mods.set(Modifier.Power, v)
                if (i + 1 < str.length && str[i + 1].isDigit()) i++
            }

            'L' -> {
                // Consume optional 'S' (stun) and/or 'B' (blind) suffix letters.
                var laser = 0
                var j = i + 1
                while (j < str.length) {
                    when (str[j].uppercaseChar()) {
                        'S' -> {
                            laser = laser or Modifier.LASER_STUN
                            j++
                        }

                        'B' -> {
                            laser = laser or Modifier.LASER_BLIND
                            j++
                        }

                        else -> {
                            break
                        }
                    }
                }
                if (laser != 0) mods = mods.set(Modifier.Laser, laser.coerceIn(0, Modifier.LASER_MAX))
                // Advance i to the last character consumed (the outer i++ will move past it).
                i = j - 1
            }
            // spaces and unknowns: ignore
        }
        i++
    }
    return mods
}

/**
 * Parse a single decimal digit at [str][index] in `[min, max]`.
 * Returns [min] if the character is not a digit or index is out of range.
 */
private fun parseDigit(
    str: String,
    index: Int,
    min: Int,
    max: Int,
): Int {
    if (index >= str.length) return min
    val c = str[index]
    if (!c.isDigit()) return min
    return (c - '0').coerceIn(min, max)
}
