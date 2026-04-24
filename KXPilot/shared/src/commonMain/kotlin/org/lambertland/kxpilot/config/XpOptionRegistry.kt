package org.lambertland.kxpilot.config

import org.lambertland.kxpilot.config.OptionFlag.CONFIG_DEFAULT

/**
 * Canonical list of all client options, mirroring xpilot-ng default_options[].
 * The ordering determines display order in the Config screen.
 */
object XpOptionRegistry {
    // ---- Ship / flight ----
    val power =
        XpOptionDef.Double(
            "power",
            "Engine power (thrust).",
            setOf(CONFIG_DEFAULT),
            55.0,
            1.0,
            200.0,
        )
    val turnSpeed =
        XpOptionDef.Double(
            "turnSpeed",
            "Turn rate in degrees per frame.",
            setOf(CONFIG_DEFAULT),
            16.0,
            1.0,
            90.0,
        )
    val turnResistance =
        XpOptionDef.Double(
            "turnResistance",
            "Resistance to rotation.",
            setOf(CONFIG_DEFAULT),
            0.12,
            0.0,
            1.0,
        )
    val altPower =
        XpOptionDef.Double(
            "altPower",
            "Alternate engine power.",
            setOf(CONFIG_DEFAULT),
            35.0,
            1.0,
            200.0,
        )
    val altTurnSpeed =
        XpOptionDef.Double(
            "altTurnSpeed",
            "Alternate turn speed.",
            setOf(CONFIG_DEFAULT),
            8.0,
            1.0,
            90.0,
        )

    // ---- Gameplay ----
    val autoShield =
        XpOptionDef.Bool(
            "autoShield",
            "Automatically raise shields when threatened.",
            setOf(CONFIG_DEFAULT),
            false,
        )
    val toggleShield =
        XpOptionDef.Bool(
            "toggleShield",
            "Shield key toggles instead of requiring held key.",
            setOf(CONFIG_DEFAULT),
            false,
        )

    // ---- Display / HUD ----
    val maxFPS =
        XpOptionDef.Int(
            "maxFPS",
            "Maximum frames per second to render.",
            setOf(CONFIG_DEFAULT),
            60,
            5,
            200,
        )
    val hudScale =
        XpOptionDef.Double(
            "hudScale",
            "Scale factor for HUD elements.",
            setOf(CONFIG_DEFAULT),
            1.0,
            0.5,
            3.0,
        )
    val showShipShapes =
        XpOptionDef.Bool(
            "showShipShapes",
            "Render ships using player ship shapes.",
            setOf(CONFIG_DEFAULT),
            true,
        )
    val slidingRadar =
        XpOptionDef.Bool(
            "slidingRadar",
            "Radar scrolls to keep own ship centered.",
            setOf(CONFIG_DEFAULT),
            false,
        )
    val outlineWorld =
        XpOptionDef.Bool(
            "outlineWorld",
            "Draw world map in outline style.",
            setOf(CONFIG_DEFAULT),
            false,
        )
    val filledWorld =
        XpOptionDef.Bool(
            "filledWorld",
            "Draw world map filled.",
            setOf(CONFIG_DEFAULT),
            true,
        )
    val showDecor =
        XpOptionDef.Bool(
            "showDecor",
            "Show map decoration objects.",
            setOf(CONFIG_DEFAULT),
            true,
        )
    val backgroundPointDist =
        XpOptionDef.Int(
            "backgroundPointDist",
            "Distance between background stars.",
            setOf(CONFIG_DEFAULT),
            32,
            8,
            128,
        )
    val sparkProb =
        XpOptionDef.Double(
            "sparkProb",
            "Probability of spark particle emission.",
            setOf(CONFIG_DEFAULT),
            0.3,
            0.0,
            1.0,
        )
    val sparkSize =
        XpOptionDef.Int(
            "sparkSize",
            "Radius of spark particles in pixels.",
            setOf(CONFIG_DEFAULT),
            2,
            1,
            8,
        )
    val fuelNotify =
        XpOptionDef.Int(
            "fuelNotify",
            "Fuel level below which to show warning.",
            setOf(CONFIG_DEFAULT),
            500,
            0,
            10000,
        )
    val dirPrediction =
        XpOptionDef.Bool(
            "dirPrediction",
            "Use direction prediction for smoother rendering.",
            setOf(CONFIG_DEFAULT),
            true,
        )

    // ---- Identity ----
    val nickName =
        XpOptionDef.Str(
            "nickName",
            "Player name shown to other players.",
            setOf(CONFIG_DEFAULT),
            "Player",
        )
    val shipName =
        XpOptionDef.Str(
            "shipName",
            "Name of the ship shape to use.",
            setOf(CONFIG_DEFAULT),
            "",
        )

    /**
     * Metaserver URL for internet server list fetches.
     * KXPilot uses HTTP GET to a CGI endpoint; the default mirrors AppInfo.METASERVER_URL.
     * C XPilot-NG used raw TCP/UDP to meta.xpilot.org:5500 / meta2.xpilot.org:5500
     * (metaserver.h) — architecturally different; not applicable here.
     */
    val metaserverUrl =
        XpOptionDef.Str(
            "metaserverUrl",
            "URL for internet server list (HTTP GET).",
            setOf(CONFIG_DEFAULT),
            "https://xpilot.sourceforge.io/cgi-bin/metaserver.cgi",
        )

    // ---- Color index options (CONFIG_COLORS tab) ----
    val hudColor = XpOptionDef.ColorIndex("hudColor", "HUD text color.", 3)
    val radarColor = XpOptionDef.ColorIndex("radarColor", "Radar overlay color.", 5)
    val selfColor = XpOptionDef.ColorIndex("selfColor", "Color of own ship.", 2)
    val enemyColor = XpOptionDef.ColorIndex("enemyColor", "Color of enemy ships.", 12)
    val allyColor = XpOptionDef.ColorIndex("allyColor", "Color of allied ships.", 6)
    val backgroundColor = XpOptionDef.ColorIndex("backgroundColor", "Space background color.", 0)
    val thrustColor = XpOptionDef.ColorIndex("thrustColor", "Thrust flame color.", 10)
    val sparkColor = XpOptionDef.ColorIndex("sparkColor", "Spark/debris color.", 14)
    val shotColor = XpOptionDef.ColorIndex("shotColor", "Own shots color.", 7)
    val fuelColor = XpOptionDef.ColorIndex("fuelColor", "Fuel pod color.", 11)
    val wallColor = XpOptionDef.ColorIndex("wallColor", "Map wall color.", 4)
    val powerMeterColor = XpOptionDef.ColorIndex("powerMeterColor", "Power meter bar color.", 9)
    val turnSpeedMeterColor = XpOptionDef.ColorIndex("turnSpeedMeterColor", "Turn-speed meter bar color.", 8)

    // ---- Server options ----
    val serverPort =
        XpOptionDef.Int(
            "serverPort",
            "UDP port the embedded server listens on.",
            setOf(CONFIG_DEFAULT),
            15345,
            1,
            65535,
        )
    val serverMaxPlayers =
        XpOptionDef.Int(
            "serverMaxPlayers",
            "Maximum simultaneous connected players.",
            setOf(CONFIG_DEFAULT),
            16,
            1,
            256,
        )
    val serverMapPath =
        XpOptionDef.Str(
            "serverMapPath",
            "Absolute path to the .xp map file; empty = built-in default.",
            setOf(CONFIG_DEFAULT),
            "",
        )
    val serverName =
        XpOptionDef.Str(
            "serverName",
            "Human-readable server name broadcast to the metaserver.",
            setOf(CONFIG_DEFAULT),
            "KXPilot Server",
        )
    val serverWelcomeMessage =
        XpOptionDef.Str(
            "serverWelcomeMessage",
            "Message shown to players on join.",
            setOf(CONFIG_DEFAULT),
            "Welcome to KXPilot!",
        )

    /** All defs in display order (CONFIG_DEFAULT first, then CONFIG_COLORS). */
    val all: List<XpOptionDef<*>> =
        listOf(
            power,
            turnSpeed,
            turnResistance,
            altPower,
            altTurnSpeed,
            autoShield,
            toggleShield,
            maxFPS,
            hudScale,
            showShipShapes,
            slidingRadar,
            outlineWorld,
            filledWorld,
            showDecor,
            backgroundPointDist,
            sparkProb,
            sparkSize,
            fuelNotify,
            dirPrediction,
            nickName,
            shipName,
            metaserverUrl,
            // color options
            hudColor,
            radarColor,
            selfColor,
            enemyColor,
            allyColor,
            backgroundColor,
            thrustColor,
            sparkColor,
            shotColor,
            fuelColor,
            wallColor,
            powerMeterColor,
            turnSpeedMeterColor,
        )
}
