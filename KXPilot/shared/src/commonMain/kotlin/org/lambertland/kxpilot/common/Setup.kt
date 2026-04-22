package org.lambertland.kxpilot.common

// ---------------------------------------------------------------------------
// Ported from: common/setup.h
// ---------------------------------------------------------------------------

/**
 * Map block-type integer codes used in the compressed map stream.
 * Maps to the `SETUP_*` `#define` constants in common/setup.h.
 */
object MapBlock {
    // Compression marker
    const val COMPRESSED: Int = 0x80

    // Map transfer order (legacy)
    const val MAP_ORDER_XY: Int = 1
    const val MAP_ORDER_YX: Int = 2
    const val MAP_UNCOMPRESSED: Int = 3

    // Block types
    const val SPACE: Int = 0
    const val FILLED: Int = 1
    const val FILLED_NO_DRAW: Int = 2
    const val FUEL: Int = 3
    const val REC_RU: Int = 4
    const val REC_RD: Int = 5
    const val REC_LU: Int = 6
    const val REC_LD: Int = 7
    const val ACWISE_GRAV: Int = 8
    const val CWISE_GRAV: Int = 9
    const val POS_GRAV: Int = 10
    const val NEG_GRAV: Int = 11
    const val WORM_NORMAL: Int = 12
    const val WORM_IN: Int = 13
    const val WORM_OUT: Int = 14
    const val CANNON_UP: Int = 15
    const val CANNON_RIGHT: Int = 16
    const val CANNON_DOWN: Int = 17
    const val CANNON_LEFT: Int = 18
    const val SPACE_DOT: Int = 19
    const val TREASURE: Int = 20 // + team (0-9)
    const val BASE_LOWEST: Int = 30
    const val BASE_UP: Int = 30 // + team (0-9)
    const val BASE_RIGHT: Int = 40 // + team (0-9)
    const val BASE_DOWN: Int = 50 // + team (0-9)
    const val BASE_LEFT: Int = 60 // + team (0-9)
    const val BASE_HIGHEST: Int = 69
    const val TARGET: Int = 70 // + team (0-9)
    const val CHECK: Int = 80 // + checkpoint number (0-25)
    const val ITEM_CONCENTRATOR: Int = 110
    const val DECOR_FILLED: Int = 111
    const val DECOR_RU: Int = 112
    const val DECOR_RD: Int = 113
    const val DECOR_LU: Int = 114
    const val DECOR_LD: Int = 115
    const val DECOR_DOT_FILLED: Int = 116
    const val DECOR_DOT_RU: Int = 117
    const val DECOR_DOT_RD: Int = 118
    const val DECOR_DOT_LU: Int = 119
    const val DECOR_DOT_LD: Int = 120
    const val UP_GRAV: Int = 121
    const val DOWN_GRAV: Int = 122
    const val RIGHT_GRAV: Int = 123
    const val LEFT_GRAV: Int = 124
    const val ASTEROID_CONCENTRATOR: Int = 125
}

/**
 * Per-tile render hint bit-flags for the "blue" (client-side wall) drawing pass.
 * Maps to `BLUE_UP`, `BLUE_RIGHT`, … `BLUE_BIT` in common/setup.h.
 */
object TileFlags {
    const val BLUE_UP: Int = 0x01
    const val BLUE_RIGHT: Int = 0x02
    const val BLUE_DOWN: Int = 0x04
    const val BLUE_LEFT: Int = 0x08
    const val BLUE_OPEN: Int = 0x10
    const val BLUE_CLOSED: Int = 0x20
    const val BLUE_FUEL: Int = 0x30
    const val BLUE_BELOW: Int = 0x40
    const val BLUE_BIT: Int = 0x80

    const val DECOR_LEFT: Int = 0x01
    const val DECOR_RIGHT: Int = 0x02
    const val DECOR_DOWN: Int = 0x04
    const val DECOR_UP: Int = 0x08
    const val DECOR_OPEN: Int = 0x10
    const val DECOR_CLOSED: Int = 0x20
    const val DECOR_BELOW: Int = 0x40
}

/**
 * Server configuration packet sent to clients at connect time.
 * Maps to C `setup_t` in common/setup.h.
 *
 * [mapData] is the raw compressed map byte stream (variable length).
 */
data class Setup(
    val setupSize: Long,
    val mapDataLen: Long,
    val mode: Long,
    val lives: Short,
    /** Legacy width in blocks (old protocol). */
    val xBlocks: Short,
    /** Legacy height in blocks (old protocol). */
    val yBlocks: Short,
    val width: Short,
    val height: Short,
    val framesPerSecond: Short,
    val mapOrder: Short,
    val name: String,
    val author: String,
    val dataUrl: String,
    val mapData: ByteArray,
) {
    // ByteArray requires manual equals/hashCode
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Setup) return false
        return setupSize == other.setupSize &&
            mapDataLen == other.mapDataLen &&
            mode == other.mode &&
            lives == other.lives &&
            xBlocks == other.xBlocks &&
            yBlocks == other.yBlocks &&
            width == other.width &&
            height == other.height &&
            framesPerSecond == other.framesPerSecond &&
            mapOrder == other.mapOrder &&
            name == other.name &&
            author == other.author &&
            dataUrl == other.dataUrl &&
            mapData.contentEquals(other.mapData)
    }

    override fun hashCode(): Int {
        var result = setupSize.hashCode()
        result = 31 * result + mapDataLen.hashCode()
        result = 31 * result + mode.hashCode()
        result = 31 * result + lives.hashCode()
        result = 31 * result + width.hashCode()
        result = 31 * result + height.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + mapData.contentHashCode()
        return result
    }
}
