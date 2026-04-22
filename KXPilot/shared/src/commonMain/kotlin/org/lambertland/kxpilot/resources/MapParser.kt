package org.lambertland.kxpilot.resources

// ---------------------------------------------------------------------------
// XPilot map data model
// ---------------------------------------------------------------------------

/** A single 2D integer point in world or click-space coordinates. */
data class MapPoint(
    val x: Int,
    val y: Int,
)

/** Block types for the legacy tile-based (.xp) map format. */
enum class BlockType {
    SPACE,
    FILLED,
    REC_LU,
    REC_LD,
    REC_RU,
    REC_RD,
    FUEL,
    CANNON,
    CHECK,
    POS_GRAV,
    NEG_GRAV,
    CWISE_GRAV,
    ACWISE_GRAV,
    UP_GRAV,
    DOWN_GRAV,
    RIGHT_GRAV,
    LEFT_GRAV,
    WORMHOLE,
    WORM_IN,
    WORM_OUT,
    TREASURE,
    EMPTY_TREASURE,
    TARGET,
    ITEM_CONCENTRATOR,
    ASTEROID_CONCENTRATOR,
    DECOR_FILLED,
    DECOR_LU,
    DECOR_LD,
    DECOR_RU,
    DECOR_RD,
    FRICTION,
    BASE,
    BASE_ATTRACTOR,
    UNKNOWN,
}

/** One row * column tile grid entry for a legacy (.xp) map. */
data class MapTile(
    val col: Int,
    val row: Int,
    val type: BlockType,
    val team: Int = 0,
)

/** A player start base. */
data class MapBase(
    val x: Int,
    val y: Int,
    val team: Int,
    val dir: Int,
    val order: Int = 0,
)

/** A cannon placement. */
data class MapCannon(
    val x: Int,
    val y: Int,
    val dir: Int,
    val team: Int = 0,
)

/** A treasure / ball target. */
data class MapTreasure(
    val x: Int,
    val y: Int,
    val team: Int,
)

/** A wormhole. */
data class MapWormhole(
    val x: Int,
    val y: Int,
    /** "normal", "in", or "out" */
    val type: String = "normal",
)

/** A checkpoint. */
data class MapCheckpoint(
    val x: Int,
    val y: Int,
    /** Sequential index (0-based) from the tile character or order in file. */
    val index: Int = 0,
)

/** A fuel station. */
data class MapFuel(
    val x: Int,
    val y: Int,
)

/** A target tile. */
data class MapTarget(
    val x: Int,
    val y: Int,
    val team: Int = 0,
)

/** A gravity source. */
data class MapGrav(
    val x: Int,
    val y: Int,
    /** "pos", "neg", "cwise", "acwise", "up", "down", "right", "left" */
    val type: String,
)

/** An item-concentrator tile. */
data class MapItemConcentrator(
    val x: Int,
    val y: Int,
)

/** An asteroid-concentrator tile. */
data class MapAsteroidConcentrator(
    val x: Int,
    val y: Int,
)

/** A friction-area tile. */
data class MapFrictionArea(
    val x: Int,
    val y: Int,
)

/**
 * A polygon obstacle / decoration used in `.xp2` maps.
 *
 * [origin] is the absolute anchor point in click-space.
 * [offsets] are cumulative offsets that form the polygon vertices.
 * [style] is the style id string from the map file (e.g. "emptywhite").
 */
data class MapPolygon(
    val origin: MapPoint,
    val offsets: List<MapPoint>,
    val style: String = "",
)

/** Edge style definition (xp2). */
data class EdgeStyle(
    val id: String,
    val width: Int,
    val color: String,
    val style: Int,
)

/** Polygon style definition (xp2). */
data class PolyStyle(
    val id: String,
    val color: String,
    val defEdge: String,
    val flags: Int,
    val texture: String = "",
)

/**
 * Parsed representation of an XPilot map file (either `.xp` legacy tile format
 * or `.xp2` XML polygon format).
 *
 * For `.xp` maps the tile grid is populated; polygons will be empty.
 * For `.xp2` maps polygons are populated; the tile grid is typically empty
 * (the server synthesises block data from the polygon geometry).
 *
 * Coordinate spaces:
 *  - `.xp`  : [width]/[height] are in **blocks**.  Entity x/y are in **blocks**.
 *  - `.xp2` : [width]/[height] are in **pixels** (mapwidth/mapheight options).
 *             Entity x/y are in **clicks** (1 pixel = 64 clicks).
 */
data class XPilotMap(
    // ----- common metadata -----
    val name: String = "",
    val author: String = "",
    val width: Int = 0,  // .xp → blocks;  .xp2 → pixels
    val height: Int = 0, // .xp → blocks;  .xp2 → pixels
    val isXp2: Boolean = false,
    val options: Map<String, String> = emptyMap(),
    // ----- legacy tile data (.xp) -----
    val tiles: List<MapTile> = emptyList(),
    // ----- polygon data (.xp2) -----
    val polygons: List<MapPolygon> = emptyList(),
    val edgeStyles: List<EdgeStyle> = emptyList(),
    val polyStyles: List<PolyStyle> = emptyList(),
    // ----- shared placements -----
    val bases: List<MapBase> = emptyList(),
    val cannons: List<MapCannon> = emptyList(),
    val treasures: List<MapTreasure> = emptyList(),
    val wormholes: List<MapWormhole> = emptyList(),
    val checkpoints: List<MapCheckpoint> = emptyList(),
    val fuels: List<MapFuel> = emptyList(),
    val targets: List<MapTarget> = emptyList(),
    val gravs: List<MapGrav> = emptyList(),
    val itemConcentrators: List<MapItemConcentrator> = emptyList(),
    val asteroidConcentrators: List<MapAsteroidConcentrator> = emptyList(),
    val frictionAreas: List<MapFrictionArea> = emptyList(),
)

// ---------------------------------------------------------------------------
// Parser entry point
// ---------------------------------------------------------------------------

/**
 * Parse an XPilot map file.
 *
 * @param text   Raw file contents as a string.
 * @param isXp2  Pass `true` for `.xp2` XML format, `false` for `.xp` legacy
 *               key:value format.  If omitted the parser auto-detects by
 *               checking whether the first non-blank line starts with `<`.
 */
fun parseXPilotMap(
    text: String,
    isXp2: Boolean? = null,
): XPilotMap {
    val firstLine = text.trimStart().take(1)
    val xml = isXp2 ?: (firstLine == "<")
    return if (xml) parseXp2Map(text) else parseXpMap(text)
}

// ---------------------------------------------------------------------------
// .xp legacy parser
// ---------------------------------------------------------------------------

/**
 * Parse the classic `.xp` key:value + ASCII grid format.
 *
 * Format:
 *   key : value
 *   ...
 *   mapData: \multiline: EndOfMapdata
 *   <grid rows, bottom row first>
 *   EndOfMapdata
 *   (optional further key:value lines)
 */
private fun parseXpMap(text: String): XPilotMap {
    val lines = text.lines()
    val options = mutableMapOf<String, String>()
    val tiles = mutableListOf<MapTile>()
    val bases = mutableListOf<MapBase>()
    val cannons = mutableListOf<MapCannon>()
    val treasures = mutableListOf<MapTreasure>()
    val wormholes = mutableListOf<MapWormhole>()
    val checkpoints = mutableListOf<MapCheckpoint>()
    val fuels = mutableListOf<MapFuel>()
    val targets = mutableListOf<MapTarget>()
    val gravs = mutableListOf<MapGrav>()
    val itemConcs = mutableListOf<MapItemConcentrator>()
    val asteroidConcs = mutableListOf<MapAsteroidConcentrator>()
    val frictionAreas = mutableListOf<MapFrictionArea>()

    // Track ordered checkpoint indices across the grid (A=0, B=1, … Z=25).
    // Checkpoints are stored in alphabetical order as encountered.
    val checkByIndex = mutableMapOf<Int, MapCheckpoint>()

    var i = 0
    while (i < lines.size) {
        val line = lines[i]

        // Detect multiline mapData block
        val mdMatch = Regex("""(?i)mapData\s*:\s*\\multiline\s*:\s*(\S+)""").find(line)
        if (mdMatch != null) {
            val endMarker = mdMatch.groupValues[1]
            i++
            val gridLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trimEnd() != endMarker) {
                gridLines.add(lines[i])
                i++
            }
            // gridLines[0] = bottom row (y = height-1 in Cartesian coords)
            val mapHeight = gridLines.size
            val mapWidth =
                options["mapwidth"]?.trim()?.toIntOrNull()
                    ?: gridLines.maxOfOrNull { it.length } ?: 0

            for ((rowIdx, rowStr) in gridLines.withIndex()) {
                // row 0 in gridLines = row mapHeight-1 in Cartesian (y-up)
                val cartY = mapHeight - 1 - rowIdx
                for (col in 0 until mapWidth) {
                    val ch = if (col < rowStr.length) rowStr[col] else ' '
                    val (type, meta) = charToBlockType(ch)
                    when (type) {
                        BlockType.BASE -> {
                            bases.add(MapBase(col, cartY, team = meta, dir = 0))
                        }

                        BlockType.BASE_ATTRACTOR -> {
                            tiles.add(MapTile(col, cartY, BlockType.BASE_ATTRACTOR))
                        }

                        BlockType.CANNON -> {
                            // meta is already the RES heading: 'r'→32(UP), 'd'→64(LEFT), 'f'→0(RIGHT), 'c'→96(DOWN)
                            cannons.add(MapCannon(col, cartY, dir = meta))
                        }

                        BlockType.TREASURE, BlockType.EMPTY_TREASURE -> {
                            treasures.add(MapTreasure(col, cartY, team = 0))
                        }

                        BlockType.WORMHOLE -> {
                            wormholes.add(MapWormhole(col, cartY, type = "normal"))
                        }

                        BlockType.WORM_IN -> {
                            wormholes.add(MapWormhole(col, cartY, type = "in"))
                        }

                        BlockType.WORM_OUT -> {
                            wormholes.add(MapWormhole(col, cartY, type = "out"))
                        }

                        BlockType.CHECK -> {
                            // meta = checkpoint index 0–25
                            checkByIndex[meta] = MapCheckpoint(col, cartY, index = meta)
                        }

                        BlockType.FUEL -> {
                            fuels.add(MapFuel(col, cartY))
                            tiles.add(MapTile(col, cartY, BlockType.FUEL))
                        }

                        BlockType.TARGET -> {
                            targets.add(MapTarget(col, cartY))
                            tiles.add(MapTile(col, cartY, BlockType.TARGET))
                        }

                        BlockType.ITEM_CONCENTRATOR -> {
                            itemConcs.add(MapItemConcentrator(col, cartY))
                            tiles.add(MapTile(col, cartY, BlockType.ITEM_CONCENTRATOR))
                        }

                        BlockType.ASTEROID_CONCENTRATOR -> {
                            asteroidConcs.add(MapAsteroidConcentrator(col, cartY))
                            tiles.add(MapTile(col, cartY, BlockType.ASTEROID_CONCENTRATOR))
                        }

                        BlockType.FRICTION -> {
                            frictionAreas.add(MapFrictionArea(col, cartY))
                            tiles.add(MapTile(col, cartY, BlockType.FRICTION))
                        }

                        BlockType.POS_GRAV -> {
                            gravs.add(MapGrav(col, cartY, "pos"))
                            tiles.add(MapTile(col, cartY, BlockType.POS_GRAV))
                        }

                        BlockType.NEG_GRAV -> {
                            gravs.add(MapGrav(col, cartY, "neg"))
                            tiles.add(MapTile(col, cartY, BlockType.NEG_GRAV))
                        }

                        BlockType.CWISE_GRAV -> {
                            gravs.add(MapGrav(col, cartY, "cwise"))
                            tiles.add(MapTile(col, cartY, BlockType.CWISE_GRAV))
                        }

                        BlockType.ACWISE_GRAV -> {
                            gravs.add(MapGrav(col, cartY, "acwise"))
                            tiles.add(MapTile(col, cartY, BlockType.ACWISE_GRAV))
                        }

                        BlockType.UP_GRAV -> {
                            gravs.add(MapGrav(col, cartY, "up"))
                            tiles.add(MapTile(col, cartY, BlockType.UP_GRAV))
                        }

                        BlockType.DOWN_GRAV -> {
                            gravs.add(MapGrav(col, cartY, "down"))
                            tiles.add(MapTile(col, cartY, BlockType.DOWN_GRAV))
                        }

                        BlockType.RIGHT_GRAV -> {
                            gravs.add(MapGrav(col, cartY, "right"))
                            tiles.add(MapTile(col, cartY, BlockType.RIGHT_GRAV))
                        }

                        BlockType.LEFT_GRAV -> {
                            gravs.add(MapGrav(col, cartY, "left"))
                            tiles.add(MapTile(col, cartY, BlockType.LEFT_GRAV))
                        }

                        BlockType.SPACE -> { /* skip air */ }

                        else -> {
                            tiles.add(MapTile(col, cartY, type, meta))
                        }
                    }
                }
            }
            // Emit checkpoints in sorted order
            checkByIndex.entries.sortedBy { it.key }.forEach { (_, cp) -> checkpoints.add(cp) }
            checkByIndex.clear()
            i++ // skip end marker line
            continue
        }

        // Key:value options (colon-separated, ignore blank / comment lines)
        if (line.isNotBlank() && !line.startsWith("//") && !line.startsWith(";")) {
            val colon = line.indexOf(':')
            if (colon > 0) {
                val key = line.substring(0, colon).trim().lowercase()
                val value = line.substring(colon + 1).trim()
                options[key] = value
            }
        }
        i++
    }

    return XPilotMap(
        name = options["mapname"] ?: "",
        author = options["mapauthor"]?.trim('"', '\'') ?: "",
        width = options["mapwidth"]?.trim()?.toIntOrNull() ?: 0,
        height = options["mapheight"]?.trim()?.toIntOrNull() ?: 0,
        isXp2 = false,
        options = options,
        tiles = tiles,
        bases = bases,
        cannons = cannons,
        treasures = treasures,
        wormholes = wormholes,
        checkpoints = checkpoints,
        fuels = fuels,
        targets = targets,
        gravs = gravs,
        itemConcentrators = itemConcs,
        asteroidConcentrators = asteroidConcs,
        frictionAreas = frictionAreas,
    )
}

/**
 * Map a single ASCII character from an `.xp` mapData grid to a (BlockType, meta) pair.
 *
 * The `meta` field meaning depends on BlockType:
 *  - BASE        → team index (0–9)
 *  - CHECK       → checkpoint index (0–25, A=0 … Z=25)
 *  - CANNON      → raw direction heading in XPilot RES units:
 *                    'r' (UP)=RES/4=32, 'd' (LEFT)=RES/2=64,
 *                    'f' (RIGHT)=0,     'c' (DOWN)=3*RES/4=96
 *  - all others  → 0
 *
 * Reference: common/xpmap.h
 */
private fun charToBlockType(ch: Char): Pair<BlockType, Int> =
    when (ch) {
        // Walls
        'x'                     -> BlockType.FILLED to 0
        's'                     -> BlockType.REC_LU to 0
        'w'                     -> BlockType.REC_LD to 0
        'a'                     -> BlockType.REC_RU to 0
        'q'                     -> BlockType.REC_RD to 0

        // Fuel
        '#'                     -> BlockType.FUEL to 0

        // Cannons — direction encoded in meta (RES heading units: 0=right, 32=up, 64=left, 96=down)
        'r'                     -> BlockType.CANNON to 32   // CANNON_UP    → faces up
        'd'                     -> BlockType.CANNON to 64   // CANNON_LEFT  → faces left
        'f'                     -> BlockType.CANNON to 0    // CANNON_RIGHT → faces right
        'c'                     -> BlockType.CANNON to 96   // CANNON_DOWN  → faces down

        // Gravity
        '+'                     -> BlockType.POS_GRAV to 0
        '-'                     -> BlockType.NEG_GRAV to 0
        '>'                     -> BlockType.CWISE_GRAV to 0
        '<'                     -> BlockType.ACWISE_GRAV to 0
        'i'                     -> BlockType.UP_GRAV to 0
        'm'                     -> BlockType.DOWN_GRAV to 0
        'k'                     -> BlockType.RIGHT_GRAV to 0
        'j'                     -> BlockType.LEFT_GRAV to 0

        // Wormholes
        '@'                     -> BlockType.WORMHOLE to 0
        '('                     -> BlockType.WORM_IN to 0
        ')'                     -> BlockType.WORM_OUT to 0

        // Treasures
        '*'                     -> BlockType.TREASURE to 0
        '^'                     -> BlockType.EMPTY_TREASURE to 0

        // Target
        '!'                     -> BlockType.TARGET to 0

        // Concentrators
        '%'                     -> BlockType.ITEM_CONCENTRATOR to 0
        '&'                     -> BlockType.ASTEROID_CONCENTRATOR to 0

        // Decoration (solid for collision; rendered differently)
        'b'                     -> BlockType.DECOR_FILLED to 0
        'h'                     -> BlockType.DECOR_LU to 0
        'y'                     -> BlockType.DECOR_LD to 0
        'g'                     -> BlockType.DECOR_RU to 0
        't'                     -> BlockType.DECOR_RD to 0

        // Friction area
        'z'                     -> BlockType.FRICTION to 0

        // Bases — teamless ('_') or team 0–9
        '_'                     -> BlockType.BASE to 0
        '$'                     -> BlockType.BASE_ATTRACTOR to 0
        in '0'..'9'             -> BlockType.BASE to (ch - '0')

        // Checkpoints A–Z → indices 0–25
        in 'A'..'Z'             -> BlockType.CHECK to (ch - 'A')

        // Space (explicit dots are also valid space in some maps)
        ' ', '\t', '.'          -> BlockType.SPACE to 0

        else                    -> BlockType.SPACE to 0
    }

// ---------------------------------------------------------------------------
// .xp2 XML parser (hand-rolled; no DOM/SAX dependency)
// ---------------------------------------------------------------------------

/**
 * Parse the `.xp2` XML polygon map format.
 *
 * Coordinate space note: In `.xp2` files:
 *  - The `<Option name="mapwidth" .../>` value is in **pixels** (not blocks).
 *    Convert to blocks by dividing by BLOCK_SZ (35).
 *  - Entity x/y attributes are in **clicks** (64 clicks = 1 pixel = 1/35 block).
 *
 * Recognises:
 *  - `<GeneralOptions>` / `<Option name="..." value="..."/>`
 *  - `<Edgestyle .../>` and `<Polystyle .../>`
 *  - `<Polygon x="..." y="..." style="..."><Offset x="..." y="..."/>...</Polygon>`
 *  - `<Cannon x="..." y="..." dir="..." team="..."><Polygon ...>...</Polygon></Cannon>`
 *  - `<Base team="..." x="..." y="..." dir="..." order="..."/>`
 *  - `<Treasure team="..." x="..." y="..."/>`
 *  - `<Wormhole x="..." y="..." type="..."/>`
 *  - `<Check x="..." y="..."/>` (note: tag is "check", not "checkpoint")
 *  - `<Fuel x="..." y="..."/>`
 *  - `<Target x="..." y="..." team="..."/>`
 *  - `<Grav x="..." y="..." type="..."/>`
 *  - `<ItemConcentrator x="..." y="..."/>`
 *  - `<AsteroidConcentrator x="..." y="..."/>`
 *  - `<FrictionArea x="..." y="..."/>`
 */
private fun parseXp2Map(text: String): XPilotMap {
    val options = mutableMapOf<String, String>()
    val polygons = mutableListOf<MapPolygon>()
    val edgeStyles = mutableListOf<EdgeStyle>()
    val polyStyles = mutableListOf<PolyStyle>()
    val bases = mutableListOf<MapBase>()
    val cannons = mutableListOf<MapCannon>()
    val treasures = mutableListOf<MapTreasure>()
    val wormholes = mutableListOf<MapWormhole>()
    val checkpoints = mutableListOf<MapCheckpoint>()
    val fuels = mutableListOf<MapFuel>()
    val targets = mutableListOf<MapTarget>()
    val gravs = mutableListOf<MapGrav>()
    val itemConcs = mutableListOf<MapItemConcentrator>()
    val asteroidConcs = mutableListOf<MapAsteroidConcentrator>()
    val frictionAreas = mutableListOf<MapFrictionArea>()

    // Tokenise the XML into a flat list of tags and text runs.
    val tagRegex = Regex("""<(/?\w[\w\-]*)((?:\s+[\w\-:]+\s*=\s*(?:"[^"]*"|'[^']*'|[^\s/>]*))*)\s*(/?)>""")

    // Stack to track nesting for Cannon > Polygon association
    var inCannon = false
    var currentCannonX = 0
    var currentCannonY = 0
    var currentCannonDir = 0
    var currentCannonTeam = 0

    // Current polygon being accumulated
    var currentPolyOrigin: MapPoint? = null
    var currentPolyStyle = ""
    var currentPolyOffsets = mutableListOf<MapPoint>()
    var inPolygon = false
    var inCannonPolygon = false

    var checkIndex = 0

    for (match in tagRegex.findAll(text)) {
        val tagName = match.groupValues[1].lowercase()
        val attrsStr = match.groupValues[2]
        val selfClose = match.groupValues[3] == "/"
        val isClose = tagName.startsWith("/")

        if (isClose) {
            when (tagName) {
                "/polygon" -> {
                    val origin = currentPolyOrigin
                    if (origin != null) {
                        if (!inCannonPolygon) {
                            polygons.add(
                                MapPolygon(
                                    origin = origin,
                                    offsets = currentPolyOffsets.toList(),
                                    style = currentPolyStyle,
                                ),
                            )
                        }
                    }
                    currentPolyOrigin = null
                    currentPolyOffsets = mutableListOf()
                    currentPolyStyle = ""
                    inPolygon = false
                    inCannonPolygon = false
                }

                "/cannon" -> {
                    inCannon = false
                }
            }
            continue
        }

        val attrs = parseAttrs(attrsStr)

        when (tagName) {
            "option" -> {
                val key = attrs["name"]?.lowercase() ?: continue
                val value = attrs["value"] ?: ""
                options[key] = value
            }

            "edgestyle" -> {
                edgeStyles.add(
                    EdgeStyle(
                        id = attrs["id"] ?: "",
                        width = attrs["width"]?.toIntOrNull() ?: 1,
                        color = attrs["color"] ?: "FFFFFF",
                        style = attrs["style"]?.toIntOrNull() ?: 0,
                    ),
                )
            }

            "polystyle" -> {
                polyStyles.add(
                    PolyStyle(
                        id = attrs["id"] ?: "",
                        color = attrs["color"] ?: "FF",
                        defEdge = attrs["defedge"] ?: "",
                        flags = attrs["flags"]?.toIntOrNull() ?: 0,
                        texture = attrs["texture"] ?: "",
                    ),
                )
            }

            "cannon" -> {
                inCannon = true
                currentCannonX = attrs["x"]?.toIntOrNull() ?: 0
                currentCannonY = attrs["y"]?.toIntOrNull() ?: 0
                currentCannonDir = attrs["dir"]?.toIntOrNull() ?: 0
                currentCannonTeam = attrs["team"]?.toIntOrNull() ?: 0
                cannons.add(MapCannon(currentCannonX, currentCannonY, currentCannonDir, currentCannonTeam))
                if (selfClose) inCannon = false
            }

            "polygon" -> {
                currentPolyOrigin =
                    MapPoint(
                        attrs["x"]?.toIntOrNull() ?: 0,
                        attrs["y"]?.toIntOrNull() ?: 0,
                    )
                currentPolyStyle = attrs["style"] ?: ""
                currentPolyOffsets = mutableListOf()
                inPolygon = true
                inCannonPolygon = inCannon
                if (selfClose) {
                    if (!inCannonPolygon) {
                        polygons.add(
                            MapPolygon(
                                origin = currentPolyOrigin!!,
                                offsets = emptyList(),
                                style = currentPolyStyle,
                            ),
                        )
                    }
                    currentPolyOrigin = null
                    inPolygon = false
                    inCannonPolygon = false
                }
            }

            "offset" -> {
                if (inPolygon) {
                    currentPolyOffsets.add(
                        MapPoint(
                            attrs["x"]?.toIntOrNull() ?: 0,
                            attrs["y"]?.toIntOrNull() ?: 0,
                        ),
                    )
                }
            }

            "base" -> {
                bases.add(
                    MapBase(
                        x = attrs["x"]?.toIntOrNull() ?: 0,
                        y = attrs["y"]?.toIntOrNull() ?: 0,
                        team = attrs["team"]?.toIntOrNull() ?: 0,
                        dir = attrs["dir"]?.toIntOrNull() ?: 0,
                        order = attrs["order"]?.toIntOrNull() ?: bases.size,
                    ),
                )
            }

            "treasure" -> {
                treasures.add(
                    MapTreasure(
                        x = attrs["x"]?.toIntOrNull() ?: 0,
                        y = attrs["y"]?.toIntOrNull() ?: 0,
                        team = attrs["team"]?.toIntOrNull() ?: 0,
                    ),
                )
            }

            "wormhole" -> {
                wormholes.add(
                    MapWormhole(
                        x = attrs["x"]?.toIntOrNull() ?: 0,
                        y = attrs["y"]?.toIntOrNull() ?: 0,
                        type = attrs["type"]?.lowercase() ?: "normal",
                    ),
                )
            }

            // NOTE: The correct tag in xp2map.c is <Check ...>, NOT <Checkpoint ...>.
            "check" -> {
                checkpoints.add(
                    MapCheckpoint(
                        x = attrs["x"]?.toIntOrNull() ?: 0,
                        y = attrs["y"]?.toIntOrNull() ?: 0,
                        index = checkIndex++,
                    ),
                )
            }

            "fuel" -> {
                fuels.add(
                    MapFuel(
                        x = attrs["x"]?.toIntOrNull() ?: 0,
                        y = attrs["y"]?.toIntOrNull() ?: 0,
                    ),
                )
            }

            "target" -> {
                targets.add(
                    MapTarget(
                        x = attrs["x"]?.toIntOrNull() ?: 0,
                        y = attrs["y"]?.toIntOrNull() ?: 0,
                        team = attrs["team"]?.toIntOrNull() ?: 0,
                    ),
                )
            }

            "grav" -> {
                gravs.add(
                    MapGrav(
                        x = attrs["x"]?.toIntOrNull() ?: 0,
                        y = attrs["y"]?.toIntOrNull() ?: 0,
                        type = attrs["type"]?.lowercase() ?: "pos",
                    ),
                )
            }

            "itemconcentrator" -> {
                itemConcs.add(
                    MapItemConcentrator(
                        x = attrs["x"]?.toIntOrNull() ?: 0,
                        y = attrs["y"]?.toIntOrNull() ?: 0,
                    ),
                )
            }

            "asteroidconcentrator" -> {
                asteroidConcs.add(
                    MapAsteroidConcentrator(
                        x = attrs["x"]?.toIntOrNull() ?: 0,
                        y = attrs["y"]?.toIntOrNull() ?: 0,
                    ),
                )
            }

            "frictionarea" -> {
                frictionAreas.add(
                    MapFrictionArea(
                        x = attrs["x"]?.toIntOrNull() ?: 0,
                        y = attrs["y"]?.toIntOrNull() ?: 0,
                    ),
                )
            }
        }
    }

    return XPilotMap(
        name = options["mapname"]?.trim('\'', '"') ?: "",
        author = options["mapauthor"]?.trim('\'', '"') ?: "",
        // .xp2 mapwidth/mapheight options are in PIXELS; store as-is.
        // toWorld() converts pixels → blocks via BLOCK_SZ.
        width = options["mapwidth"]?.toIntOrNull() ?: 0,
        height = options["mapheight"]?.toIntOrNull() ?: 0,
        isXp2 = true,
        options = options,
        polygons = polygons,
        edgeStyles = edgeStyles,
        polyStyles = polyStyles,
        bases = bases,
        cannons = cannons,
        treasures = treasures,
        wormholes = wormholes,
        checkpoints = checkpoints,
        fuels = fuels,
        targets = targets,
        gravs = gravs,
        itemConcentrators = itemConcs,
        asteroidConcentrators = asteroidConcs,
        frictionAreas = frictionAreas,
    )
}

/**
 * Parse an XML attribute string like:
 *   ` name="foo" value="bar" dir = "64"`
 * Returns a lowercase-keyed map of attribute values (without surrounding quotes).
 */
private fun parseAttrs(attrsStr: String): Map<String, String> {
    val result = mutableMapOf<String, String>()
    val attrRegex = Regex("""([\w\-:]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|(\S+))""")
    for (m in attrRegex.findAll(attrsStr)) {
        val key = m.groupValues[1].lowercase()
        val value = m.groupValues[2].ifEmpty { m.groupValues[3].ifEmpty { m.groupValues[4] } }
        result[key] = value
    }
    return result
}
