package org.lambertland.kxpilot.server

// ---------------------------------------------------------------------------
// Ported from: server/modifiers.h
// ---------------------------------------------------------------------------

/**
 * Weapon-modifier field (packed 16-bit bitfield).
 * Maps to C `modifiers_t` (typedef uint16_t) in server/modifiers.h.
 *
 * Each [Modifier] occupies a field of 2–3 bits within the UShort.
 * Use [Modifiers.get] / [Modifiers.set] to read/write individual modifier values.
 */
@JvmInline
value class Modifiers(
    val raw: UShort,
) {
    companion object {
        // Note: `const val` is not allowed on @JvmInline value classes —
        // the compiler requires a backing field, which inline classes lack.
        val ZERO: Modifiers = Modifiers(0u)
    }
}

/**
 * Identifies a single weapon modifier field within [Modifiers].
 * Maps to C `modifier_t` enum in server/modifiers.h.
 *
 * [shift] is the bit offset within the 16-bit modifier word.
 * [mask] is the unshifted bitmask of the field (max value = mask).
 */
enum class Modifier(
    val shift: Int,
    val mask: Int,
) {
    Nuclear(0, 0x3), // 0, NUCLEAR, NUCLEAR|FULLNUCLEAR  (2 bits: N_BIT0 + N_BIT1)
    Cluster(2, 0x1), // 0 or 1  (1 bit: C_BIT)
    Implosion(4, 0x1), // 0 or 1  (1 bit: I_BIT)
    Velocity(6, 0x3), // 0 – MODS_VELOCITY_MAX
    Mini(8, 0x3), // 0 – MODS_MINI_MAX
    Spread(10, 0x3), // 0 – MODS_SPREAD_MAX
    Power(12, 0x3), // 0 – MODS_POWER_MAX
    Laser(14, 0x3), // 0, STUN, BLIND
    ;

    companion object {
        // --- Per-modifier max values (for range-checking) ---
        const val NUCLEAR_MAX: Int = 3
        const val CLUSTER_MAX: Int = 1 // boolean flag
        const val IMPLOSION_MAX: Int = 1 // boolean flag
        const val VELOCITY_MAX: Int = 3
        const val MINI_MAX: Int = 3
        const val SPREAD_MAX: Int = 3
        const val POWER_MAX: Int = 3
        const val LASER_MAX: Int = 2

        // --- Nuclear bit values (combined into the Nuclear field) ---

        /** Nuclear modifier: single-nuclear bit. */
        const val NUCLEAR: Int = 1 shl 0

        /** Nuclear modifier: full-nuclear bit (combined with NUCLEAR = 3 = both bits). */
        const val FULLNUCLEAR: Int = 1 shl 1

        // --- Laser bit values (combined into the Laser field) ---

        /** Laser modifier: stun effect. */
        const val LASER_STUN: Int = 1 shl 0

        /** Laser modifier: blind effect. */
        const val LASER_BLIND: Int = 1 shl 1
    }
}

// ---------------------------------------------------------------------------
// Extension functions on Modifiers
// ---------------------------------------------------------------------------

/** Read the value of a [Modifier] field from this [Modifiers] word. */
fun Modifiers.get(modifier: Modifier): Int = (raw.toInt() ushr modifier.shift) and modifier.mask

/** Return a new [Modifiers] with the given [Modifier] field set to [value].
 *
 * [value] must be in `0..modifier.mask`; values outside this range are an
 * error — the caller has constructed an invalid modifier value.
 */
fun Modifiers.set(
    modifier: Modifier,
    value: Int,
): Modifiers {
    require(value in 0..modifier.mask) {
        "Modifiers.set: value $value out of range for $modifier (max ${modifier.mask})"
    }
    val cleared = raw.toInt() and (modifier.mask shl modifier.shift).inv()
    val set = cleared or ((value and modifier.mask) shl modifier.shift)
    return Modifiers(set.toUShort())
}
