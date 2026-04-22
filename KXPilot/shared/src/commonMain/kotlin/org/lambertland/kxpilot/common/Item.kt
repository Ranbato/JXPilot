package org.lambertland.kxpilot.common

// ---------------------------------------------------------------------------
// Ported from: common/item.h
// ---------------------------------------------------------------------------

/**
 * Playfield item types.  Maps to C `Item_t` enum in common/item.h.
 *
 * [ordinal] matches the C enum integer value (0-based except [NO_ITEM] = -1).
 * [bit] is the bitmask used in item-set fields (1 << ordinal, 0 for NO_ITEM).
 */
enum class Item(
    val id: Int,
) {
    NO_ITEM(-1),
    FUEL(0),
    WIDEANGLE(1),
    REARSHOT(2),
    AFTERBURNER(3),
    CLOAK(4),
    SENSOR(5),
    TRANSPORTER(6),
    TANK(7),
    MINE(8),
    MISSILE(9),
    ECM(10),
    LASER(11),
    EMERGENCY_THRUST(12),
    TRACTOR_BEAM(13),
    AUTOPILOT(14),
    EMERGENCY_SHIELD(15),
    DEFLECTOR(16),
    HYPERJUMP(17),
    PHASING(18),
    MIRROR(19),
    ARMOR(20),
    ;

    /** Bitmask for this item in an item-set Int field (0 for NO_ITEM). */
    val bit: Int get() = if (id >= 0) 1 shl id else 0

    companion object {
        const val NUM_ITEMS: Int = 21

        /** Pixel size of one item icon. */
        const val ITEM_SIZE: Int = 16
        const val ITEM_TRIANGLE_SIZE: Int = (5 * ITEM_SIZE / 7 + 1)

        fun fromId(id: Int): Item =
            entries.firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("Unknown item id: $id")
    }
}
