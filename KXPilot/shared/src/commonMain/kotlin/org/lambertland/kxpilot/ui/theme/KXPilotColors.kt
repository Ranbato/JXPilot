package org.lambertland.kxpilot.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// KXPilot colour palette
// ---------------------------------------------------------------------------

/**
 * The 16-slot XPilot palette (indices 0–15).  These match the default values
 * of `color0`–`color15` in xpilot-ng's `colors.c`.
 *
 * Color-index options (hudColor, zeroLivesColor, etc.) store an Int in [0,15]
 * that indexes into this array.
 */
object KXPilotColors {
    // ---- 16-slot palette (XPilot defaults) ----
    val palette =
        arrayOf(
            Color(0xFF000000), // 0  black
            Color(0xFFFFFFFF), // 1  white
            Color(0xFF0000FF), // 2  blue
            Color(0xFF00FF00), // 3  green
            Color(0xFFFF0000), // 4  red
            Color(0xFFFFFF00), // 5  yellow
            Color(0xFF00FFFF), // 6  cyan
            Color(0xFFFF00FF), // 7  magenta
            Color(0xFF888888), // 8  grey
            Color(0xFFFF8800), // 9  orange
            Color(0xFF8888FF), // 10 light blue
            Color(0xFFFF8888), // 11 light red / pink
            Color(0xFF88FF88), // 12 light green
            Color(0xFFBBBBBB), // 13 light grey
            Color(0xFF886600), // 14 brown
            Color(0xFF440044), // 15 dark purple
        )

    // ---- Named semantic colours used in the UI ----
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF0A0A1A)
    val SurfaceVariant = Color(0xFF111133)
    val OnSurface = Color(0xFFCCCCCC)
    val OnSurfaceDim = Color(0xFF666688)
    val Accent = Color(0xFF4488FF)
    val AccentBright = Color(0xFF88AAFF)
    val Danger = Color(0xFFFF4444)
    val Success = Color(0xFF44FF88)
    val Warning = Color(0xFFFFAA00)

    /** Self-ship / player highlight colour. */
    val SelfShip = Color(0xFF00FF88)

    /** Enemy ship colour. */
    val EnemyShip = Color(0xFFFFFFFF)

    /** Allied ship colour. */
    val AllyShip = Color(0xFF4488FF)

    /** HUD element colour (bright green). */
    val Hud = Color(0xFF88FF88)

    /** HUD label / secondary text. */
    val HudDim = Color(0xFF446644)

    // ---- Player-list life colours (guiobject_options defaults) ----
    val ZeroLives = palette[5] // yellow  (zeroLivesColor default = 5)
    val OneLife = palette[11] // pink    (oneLifeColor default = 11)
    val TwoLives = palette[4] // red     (twoLivesColor default = 4)
    val ManyLives =
        palette[0] // black → override to white for visibility
            .let { Color(0xFFFFFFFF) }

    // ---- Map tile colours ----
    val MapWall = Color(0xFF334466)
    val MapDiag = Color(0xFF445577)
    val MapFuel = Color(0xFF226622)
    val MapBase = Color(0xFF664422)
    val MapCannon = Color(0xFF662222)

    // ---- Chat message type colours ----
    val MsgNormal = Color(0xFFCCCCCC)
    val MsgBall = Color(0xFF00CC44) // msgScanBallColor  = green
    val MsgSafe = Color(0xFF4488FF) // msgScanSafeColor  = blue
    val MsgCover = Color(0xFF0000FF) // msgScanCoverColor = blue (index 2)
    val MsgPop = Color(0xFFFF8888) // msgScanPopColor   = light red

    /** Resolve a palette index, clamping to [0,15]. */
    fun fromIndex(index: Int): Color = palette[index.coerceIn(0, 15)]
}
