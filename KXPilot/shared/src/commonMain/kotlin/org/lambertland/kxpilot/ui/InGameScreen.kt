package org.lambertland.kxpilot.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.lambertland.kxpilot.AppLogger
import org.lambertland.kxpilot.client.KeyState
import org.lambertland.kxpilot.common.GameConst
import org.lambertland.kxpilot.common.Key
import org.lambertland.kxpilot.common.toPixel
import org.lambertland.kxpilot.config.LocalAppConfig
import org.lambertland.kxpilot.config.XpOptionRegistry
import org.lambertland.kxpilot.engine.DemoGameState
import org.lambertland.kxpilot.engine.DemoShip
import org.lambertland.kxpilot.engine.EngineTarget
import org.lambertland.kxpilot.engine.GameEngine
import org.lambertland.kxpilot.engine.GameEngineFactory
import org.lambertland.kxpilot.engine.NpcAiManager
import org.lambertland.kxpilot.engine.RenderConst
import org.lambertland.kxpilot.engine.buildNpcShipsFromBases
import org.lambertland.kxpilot.model.GameAction
import org.lambertland.kxpilot.model.MessageColor
import org.lambertland.kxpilot.model.MessageEntry
import org.lambertland.kxpilot.model.PlayerInfo
import org.lambertland.kxpilot.model.TalkResult
import org.lambertland.kxpilot.platform.saveTextFile
import org.lambertland.kxpilot.resources.BlockType
import org.lambertland.kxpilot.resources.ShipShapeDef
import org.lambertland.kxpilot.resources.XPilotMap
import org.lambertland.kxpilot.resources.parseShipShapes
import org.lambertland.kxpilot.resources.parseXPilotMap
import org.lambertland.kxpilot.resources.readResourceText
import org.lambertland.kxpilot.server.currentTimeMs
import org.lambertland.kxpilot.ui.LocalNavigator
import org.lambertland.kxpilot.ui.components.GameButton
import org.lambertland.kxpilot.ui.components.MessageLog
import org.lambertland.kxpilot.ui.components.RadarMinimap
import org.lambertland.kxpilot.ui.components.ScoreOverlay
import org.lambertland.kxpilot.ui.components.TalkOverlay
import org.lambertland.kxpilot.ui.screens.KeyBindingsStateHolder
import org.lambertland.kxpilot.ui.stateholder.InGameStateHolder
import org.lambertland.kxpilot.ui.theme.KXPilotColors
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import androidx.compose.ui.input.key.Key as ComposeKey

// ---------------------------------------------------------------------------
// HUD snapshot — updated once per tick, read by HUD composables
// ---------------------------------------------------------------------------

/**
 * Immutable snapshot of all values the HUD composables need.
 * Written once per simulation tick; Compose only recomposes HUD children
 * when the reference changes (i.e. every tick), but each composable is
 * cheap to recompose since it reads only primitive fields.
 */
private data class PlayerStats(
    val fuel: Float,
    val fuelMax: Float,
    val power: Float,
    val speed: Float,
    val headingRad: Float,
    val shieldActive: Boolean,
    val score: Int,
    val kills: Int,
    val deaths: Int,
    /** Velocity components (pixels/tick) for the speed-vector pointer. */
    val velX: Float,
    val velY: Float,
    /** Current turn speed (heading-units/tick) for the turnspeed meter. */
    val turnSpeed: Float,
    /**
     * Active weapon modifier string shown at HUD bottom-left (e.g. "N-", "C-").
     * Empty string when no modifiers are active.
     */
    val modifiers: String,
)

private data class ViewState(
    /** World-space pixel position of the player (used by minimap and tractor beam). */
    val playerX: Float,
    val playerY: Float,
    val originX: Float,
    val originY: Float,
    val viewW: Float,
    val viewH: Float,
    /** Minimap blip positions (copy avoids holding mutable DemoShip refs). */
    val npcPositions: List<Pair<Float, Float>>,
    /**
     * Remaining game time in whole seconds, or -1 if the server has no time limit
     * (demo mode always sends -1).
     */
    val timeLeftSec: Int,
)

private data class LockState(
    /** Direction to locked target in radians (Y-up), or NaN if no lock. */
    val dirRad: Float,
    /** Distance to locked target in pixels, or 0 if no lock. */
    val distPx: Float,
    /** True when locked target is an ally (hollow blue dot on HUD). */
    val isAlly: Boolean,
    /** Display name of the locked target, or empty when no lock. */
    val targetName: String,
    /** Distance to target in blocks (for HUD text), or -1 when no lock. */
    val targetDistBlocks: Int,
) {
    companion object {
        val NONE = LockState(Float.NaN, 0f, false, "", -1)
    }
}

private data class HudSnapshot(
    val stats: PlayerStats,
    val view: ViewState,
    val lock: LockState,
    /** Slow loop counter (increments each tick) used for blink timing. */
    val loopCount: Int,
)

// ---------------------------------------------------------------------------
// Colours (game-domain only)
// ---------------------------------------------------------------------------
private val COL_BACKGROUND = Color(0xFF000000)
private val COL_ENEMY_SHIP = Color(0xFFFFFFFF)
private val COL_ALLY_SHIP = Color(0xFF4488FF)
private val COL_SHIELD = Color(0xFF4488FF)
private val COL_SHOT = Color(0xFFFFFFFF)
private val COL_LABEL = Color(0xFFCCCCCC)
private val COL_HUD = Color(0xFF88FF88)
private val COL_MAP_WALL = Color(0xFF334466)
private val COL_MAP_DIAG = Color(0xFF445577)
private val COL_MAP_FUEL = Color(0xFF226622)
private val COL_MAP_BASE = Color(0xFF664422)
private val COL_MAP_CANNON = Color(0xFF662222)

/** Debris color. C: color = RED for OBJ_DEBRIS (shot.c:1214). */
private val COL_DEBRIS = Color(0xFFFF3333)

/** Team index → ball colour.  Team 0 = neutral white. */
private val TEAM_COLORS =
    arrayOf(
        Color(0xFFFFFFFF), // 0 neutral
        Color(0xFFFF4444), // 1 red
        Color(0xFF4488FF), // 2 blue
        Color(0xFF44FF44), // 3 green
        Color(0xFFFFFF44), // 4 yellow
    )

// ---------------------------------------------------------------------------
// Resource loaders
// ---------------------------------------------------------------------------

private fun loadShipShapes(): List<ShipShapeDef> =
    try {
        val text = readResourceText("/data/shipshapes.json")
        if (text != null) {
            parseShipShapes(text)
        } else {
            AppLogger.log("KXPilot: resource /data/shipshapes.json not found")
            emptyList()
        }
    } catch (e: Exception) {
        AppLogger.log("KXPilot: failed to load ship shapes: ${e::class.simpleName}: ${e.message}")
        emptyList()
    }

private fun loadMap(resourcePath: String): XPilotMap? =
    try {
        val text = readResourceText(resourcePath)
        if (text != null) {
            parseXPilotMap(text)
        } else {
            AppLogger.log("KXPilot: map resource $resourcePath not found")
            null
        }
    } catch (e: Exception) {
        AppLogger.log("KXPilot: failed to load map $resourcePath: ${e::class.simpleName}: ${e.message}")
        null
    }

// ---------------------------------------------------------------------------
// InGameScreen
// ---------------------------------------------------------------------------

/**
 * Full-window in-game screen.  Lean composable — all UI state is owned by
 * [InGameStateHolder]; Canvas rendering is delegated to private functions.
 *
 * NOTE: This screen currently runs **demo mode only** — a local offline simulation
 * driven by [GameEngine] and NPC AI.  The [serverHost]/[serverPort] passed via
 * [Screen.InGame] are intentionally unused (see [Screen.InGame] @Suppress).
 *
 * TODO (network client): To support real multiplayer, the following would be needed:
 *  1. A UDP/TCP client that connects to serverHost:serverPort and sends input packets.
 *  2. A server-frame decoder that maps XPilot-NG protocol frames to [InGameStateHolder]
 *     state (player positions, shots, items, scores, chat messages).
 *  3. A render path that draws from server-supplied state rather than local [GameEngine].
 *  4. The local [GameEngine] + [DemoGameState] loop below would be replaced or
 *     conditionally disabled when a real server connection is active.
 */
@Composable
fun InGameScreen() {
    val config = LocalAppConfig.current
    val playerName = config.get(XpOptionRegistry.nickName).ifBlank { "Player" }
    val chosenShipName = config.get(XpOptionRegistry.shipName)

    val allShapes = remember { loadShipShapes() }
    val demoMap = remember { loadMap("/maps/teamcup.xp") }

    // Resolve the player's chosen shape by name; null = default triangle.
    val chosenShape: ShipShapeDef? =
        remember(chosenShipName, allShapes) {
            allShapes.firstOrNull { it.name == chosenShipName }
                ?: allShapes.firstOrNull()
        }

    val inGameState =
        remember(playerName) {
            InGameStateHolder().also { s ->
                val now = currentTimeMs()
                s.appendMessage("[Server] Game starts in 10 seconds", MessageColor.NORMAL, now)
                s.appendMessage("[Server] Welcome to KXPilot Demo!", MessageColor.SAFE, now - 2_000L)
                s.players =
                    listOf(
                        PlayerInfo(1, playerName, 3, 512.0, 0, isSelf = true),
                        PlayerInfo(2, "Alice", 2, 1024.5, 0, isSelf = false),
                        PlayerInfo(3, "Bob", 0, 0.0, 1, isSelf = false),
                        PlayerInfo(4, "Charlie", 5, 3200.0, -1, isSelf = false),
                    )
            }
        }

    var gameState by remember { mutableStateOf(DemoGameState(1f, 1f)) }
    var frame by remember { mutableIntStateOf(0) }
    var hud by remember {
        mutableStateOf(
            HudSnapshot(
                stats = PlayerStats(0f, 1000f, 0f, 0f, 0f, false, 0, 0, 0, 0f, 0f, 0f, ""),
                view = ViewState(0f, 0f, 0f, 0f, 0f, 0f, emptyList(), -1),
                lock = LockState.NONE,
                loopCount = 0,
            ),
        )
    }
    val shipPathCache = remember { HashMap<ShipShapeDef?, Path>() }
    val textMeasurer = rememberTextMeasurer()
    var showLog by remember { mutableStateOf(false) }
    val navigator = LocalNavigator.current
    var showExitConfirm by remember { mutableStateOf(false) }

    // nowMs updated every 100ms (not every frame) to throttle MessageLog recomposition
    var nowMs by remember { mutableLongStateOf(currentTimeMs()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(100L)
            nowMs = currentTimeMs()
        }
    }

    val engine =
        remember {
            if (demoMap != null) {
                GameEngineFactory.fromMap(demoMap)
            } else {
                GameEngine.forEmptyWorld(60, 45)
            }
        }
    val keys = remember { KeyState() }
    val keyBindings = remember { KeyBindingsStateHolder() }
    val npcAiManager = remember { NpcAiManager(engine.world.width.toFloat(), engine.world.height.toFloat()) }
    val camera =
        remember {
            Camera(worldW = engine.world.width.toFloat(), worldH = engine.world.height.toFloat())
        }

    // R9: side-effects must not live inside `remember` — they run on every
    // recomposition that has a new `engine` key, causing a recomposition loop.
    // LaunchedEffect(engine) runs exactly once per unique engine instance on the
    // composition thread, which is the correct place for one-shot initialisation.
    LaunchedEffect(engine) {
        engine.spawnAtBase(0)
        gameState = buildNpcShipsFromBases(engine, allShapes)
        // Register all NPC ships with the AI manager
        npcAiManager.clear()
        gameState.ships.forEach { npcAiManager.register(it) }
        shipPathCache.clear()
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Animation loop
    LaunchedEffect(Unit) {
        var lastMs = 0L
        var tickAccum = 0L
        val tickMs = 16L
        while (isActive) {
            val frameMs = withFrameMillis { it }
            val delta = if (lastMs == 0L) tickMs else (frameMs - lastMs).coerceAtMost(100L)
            lastMs = frameMs
            tickAccum += delta
            while (tickAccum >= tickMs) {
                val gs = gameState
                // Remove NPCs that were killed in the previous tick BEFORE engine.tick()
                // so the engine never sees dead NPCs during collision / mine-trigger passes.
                val removed = gs.ships.filter { it.hp <= 0f }
                if (removed.isNotEmpty()) {
                    gs.ships.removeAll { it.hp <= 0f }
                    removed.forEach { npcAiManager.remove(it.id) }
                }
                // #B Clear stale lock if locked NPC was removed
                if (engine.lockedNpcId >= 0 && gs.ships.none { it.id == engine.lockedNpcId }) {
                    engine.clearLock()
                }
                engine.tick(keys, @Suppress("UNCHECKED_CAST") (gs.ships as MutableList<EngineTarget>))
                keys.advanceTick()
                // NPC AI tick: update NPC headings/velocities and collect weapon events
                val npcEvents =
                    npcAiManager.tickAll(
                        npcs = gs.ships,
                        playerX = engine.playerPixelX,
                        playerY = engine.playerPixelY,
                        playerVx = engine.player.vel.x,
                        playerVy = engine.player.vel.y,
                        playerAlive = engine.player.isAlive(),
                        treasureGoals = engine.treasureGoals,
                    )
                engine.dispatchNpcWeaponEvents(npcEvents, gs.ships)
                camera.follow(engine.playerPixelX, engine.playerPixelY)
                gameState.tick()
                tickAccum -= tickMs
                frame++
                // Snapshot HUD values once per tick so composable HUD subtree
                // only recomposes when these values actually change.
                val lockedNpc = gs.ships.firstOrNull { it.id == engine.lockedNpcId }
                hud =
                    HudSnapshot(
                        stats =
                            PlayerStats(
                                fuel = engine.fuel.toFloat(),
                                fuelMax = engine.fuelMax.toFloat(),
                                power = engine.player.power.toFloat(),
                                speed =
                                    hypot(
                                        engine.player.vel.x
                                            .toDouble(),
                                        engine.player.vel.y
                                            .toDouble(),
                                    ).toFloat(),
                                headingRad = engine.player.floatDir.toFloat(),
                                shieldActive = engine.shieldActive,
                                score = engine.player.score.toInt(),
                                kills = engine.player.kills,
                                deaths = engine.player.deaths,
                                velX = engine.player.vel.x,
                                velY = engine.player.vel.y,
                                turnSpeed = engine.player.turnspeed.toFloat(),
                                modifiers = "", // no modifier system yet
                            ),
                        view =
                            ViewState(
                                playerX = engine.playerPixelX,
                                playerY = engine.playerPixelY,
                                originX = camera.worldOriginX,
                                originY = camera.worldOriginY,
                                viewW = camera.viewW,
                                viewH = camera.viewH,
                                npcPositions = gs.ships.map { Pair(it.x, it.y) },
                                timeLeftSec = -1, // no server time limit in demo mode
                            ),
                        lock =
                            if (engine.lockedNpcId >= 0) {
                                LockState(
                                    dirRad = engine.lockDirRad.toFloat(),
                                    distPx = engine.lockDistPx.toFloat(),
                                    isAlly = lockedNpc?.id?.rem(2) == 0, // even id = ally (demo heuristic)
                                    targetName = lockedNpc?.label ?: "",
                                    targetDistBlocks = (engine.lockDistPx / GameConst.BLOCK_SZ).toInt(),
                                )
                            } else {
                                LockState.NONE
                            },
                        loopCount = frame,
                    )
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(COL_BACKGROUND)
                .onSizeChanged { size ->
                    camera.resize(size.width.toFloat(), size.height.toFloat())
                }.focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    // Build the key map once per event; reused for both UI and engine dispatch.
                    val keyMap = keyBindings.buildKeyMap()

                    // --- Talk overlay intercepts all keys while visible ---
                    if (event.type == KeyEventType.KeyDown && inGameState.talkState.isVisible) {
                        when (event.key) {
                            ComposeKey.Enter -> {
                                inGameState.submitTalk(currentTimeMs())
                                focusRequester.requestFocus()
                                return@onKeyEvent true
                            }

                            ComposeKey.Escape -> {
                                inGameState.talkState.close()
                                focusRequester.requestFocus()
                                return@onKeyEvent true
                            }

                            ComposeKey.DirectionUp -> {
                                inGameState.talkState.browseHistory(1)
                                return@onKeyEvent true
                            }

                            ComposeKey.DirectionDown -> {
                                inGameState.talkState.browseHistory(-1)
                                return@onKeyEvent true
                            }

                            else -> {
                                return@onKeyEvent false
                            }
                        }
                    }

                    // --- Single-pass dispatch: UI actions (KeyDown only) + engine key state ---
                    // Each action in the list is handled exactly once regardless of whether
                    // it maps to a UI action, an engine key, or both.
                    val actions = keyMap[event.key] ?: emptyList()
                    var consumed = false
                    for (action in actions) {
                        // UI actions — only on key-down
                        if (event.type == KeyEventType.KeyDown) {
                            when (action) {
                                GameAction.SCOREBOARD -> {
                                    inGameState.toggleScoreboard()
                                    consumed = true
                                    continue
                                }

                                GameAction.TALK -> {
                                    inGameState.openTalk()
                                    consumed = true
                                    continue
                                }

                                GameAction.RESPAWN -> {
                                    engine.spawnAtBase()
                                    return@onKeyEvent true
                                }

                                GameAction.EXIT_TO_MENU -> {
                                    showExitConfirm = true
                                    return@onKeyEvent true
                                }

                                else -> {} // fall through to engine key handling
                            }
                        }

                        // Engine key state (press on KeyDown, release on KeyUp)
                        val kxpKey = gameActionToKey(action) ?: continue
                        when (event.type) {
                            KeyEventType.KeyDown -> {
                                keys.press(kxpKey)
                            }

                            KeyEventType.KeyUp -> {
                                keys.release(kxpKey)
                            }

                            else -> {}
                        }
                        consumed = true
                    }
                    consumed
                },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gs = gameState
            // R10: take an immutable snapshot of the NPC list so the Canvas draw
            // phase reads a stable collection even if the game loop modifies ships
            // concurrently on the next frame.
            val gsShips = gs.ships.toList()

            if (demoMap != null) drawMapTiles(demoMap, camera)

            // World items: bonus items spawned on the map.
            // Drawn before ships so ships render on top.
            for (item in engine.worldItems.toList()) {
                val px =
                    item.pos.cx
                        .toPixel()
                        .toFloat()
                val py =
                    item.pos.cy
                        .toPixel()
                        .toFloat()
                val iconSize = 16f
                if (camera.isVisible(px, py, margin = iconSize)) {
                    val sc = camera.worldToScreen(px, py)
                    drawItemIcon(
                        itemType = item.itemType,
                        iconSize = iconSize,
                        cx = sc.x,
                        cy = sc.y,
                    )
                }
            }

            for (s in gsShips) {
                drawShip(s, camera, textMeasurer, allShapes.size, shipPathCache)
            }

            drawEnginePlayer(engine, camera, textMeasurer, playerName, chosenShape)

            for (shot in engine.shots.toList()) {
                val px =
                    shot.pos.cx
                        .toPixel()
                        .toFloat()
                val py =
                    shot.pos.cy
                        .toPixel()
                        .toFloat()
                if (camera.isVisible(px, py)) {
                    drawCircle(color = COL_SHOT, radius = RenderConst.SHOT_RADIUS, center = camera.worldToScreen(px, py))
                }
            }

            // Missiles: small triangle pointing in heading direction
            for (m in engine.missiles.toList()) {
                val px =
                    m.pos.cx
                        .toPixel()
                        .toFloat()
                val py =
                    m.pos.cy
                        .toPixel()
                        .toFloat()
                if (camera.isVisible(px, py)) {
                    val sc = camera.worldToScreen(px, py)
                    val angleDeg = (-m.headingRad * (180.0 / kotlin.math.PI)).toFloat()
                    translate(sc.x, sc.y) {
                        rotate(degrees = angleDeg, pivot = Offset.Zero) {
                            val mPath =
                                Path().apply {
                                    moveTo(8f, 0f)
                                    lineTo(-4f, 3f)
                                    lineTo(-4f, -3f)
                                    close()
                                }
                            drawPath(mPath, color = Color(0xFFFFAA00), style = Stroke(width = 1.5f))
                        }
                    }
                }
            }

            // Mines: circle with four spikes
            for (mine in engine.mines.toList()) {
                val px =
                    mine.pos.cx
                        .toPixel()
                        .toFloat()
                val py =
                    mine.pos.cy
                        .toPixel()
                        .toFloat()
                if (camera.isVisible(px, py)) {
                    val sc = camera.worldToScreen(px, py)
                    val mineColor = Color(0xFFFF4444) // mines are always armed (no arming delay, C default)
                    drawCircle(color = mineColor, radius = 5f, center = sc, style = Stroke(width = 1.5f))
                    // 4 spikes at NSEW
                    for (ang in MINE_SPIKE_ANGLES) {
                        val sx = cos(ang).toFloat()
                        val sy = sin(ang).toFloat()
                        drawLine(
                            mineColor,
                            Offset(sc.x + sx * 5f, sc.y + sy * 5f),
                            Offset(sc.x + sx * 9f, sc.y + sy * 9f),
                            strokeWidth = 1.5f,
                        )
                    }
                }
            }

            // Debris: small red dots (C color = RED for OBJ_DEBRIS, shot.c:1214)
            for (d in engine.debris.toList()) {
                val px =
                    d.pos.cx
                        .toPixel()
                        .toFloat()
                val py =
                    d.pos.cy
                        .toPixel()
                        .toFloat()
                if (camera.isVisible(px, py)) {
                    drawCircle(
                        color = COL_DEBRIS,
                        radius = 2f,
                        center = camera.worldToScreen(px, py),
                    )
                }
            }

            // Balls: filled circle in team colour + connector line when attached.
            for (ball in engine.balls) {
                val bpx =
                    ball.pos.cx
                        .toPixel()
                        .toFloat()
                val bpy =
                    ball.pos.cy
                        .toPixel()
                        .toFloat()
                val ballColor = TEAM_COLORS.getOrElse(ball.touchTeam) { TEAM_COLORS[0] }
                if (camera.isVisible(bpx, bpy, margin = 12f)) {
                    val sc = camera.worldToScreen(bpx, bpy)
                    drawCircle(color = ballColor, radius = 10f, center = sc)
                    drawCircle(color = Color(0xFF000000), radius = 10f, center = sc, style = Stroke(width = 1.5f))
                }
                // Connector line to player when attached
                if (ball.connectedPlayerId == engine.player.id.toInt()) {
                    val playerSc = camera.worldToScreen(engine.playerPixelX, engine.playerPixelY)
                    val ballSc = camera.worldToScreen(bpx, bpy)
                    drawLine(
                        color = ballColor,
                        start = playerSc,
                        end = ballSc,
                        strokeWidth = 2f,
                        alpha = 0.8f,
                    )
                }
            }

            // Tractor beam: dashed line from player to locked NPC when beam is active
            if (engine.lockedNpcId >= 0 && keys.isDown(Key.KEY_TRACTOR_BEAM)) {
                val target = gs.ships.firstOrNull { it.id == engine.lockedNpcId }
                if (target != null && camera.isVisible(target.x, target.y)) {
                    val playerSc = camera.worldToScreen(hud.view.playerX, hud.view.playerY)
                    val targetSc = camera.worldToScreen(target.x, target.y)
                    val dashLen = 6f
                    val gapLen = 4f
                    val period = dashLen + gapLen
                    val dx = targetSc.x - playerSc.x
                    val dy = targetSc.y - playerSc.y
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (dist > 1f) {
                        val ux = dx / dist
                        val uy = dy / dist
                        var t = 0f
                        while (t < dist) {
                            val tEnd = minOf(t + dashLen, dist)
                            drawLine(
                                color = COL_HUD,
                                start = Offset(playerSc.x + ux * t, playerSc.y + uy * t),
                                end = Offset(playerSc.x + ux * tEnd, playerSc.y + uy * tEnd),
                                strokeWidth = 1.5f,
                                alpha = 0.7f,
                            )
                            t += period
                        }
                    }
                }
            }

            drawHud(hud, textMeasurer)
        }

        // Bottom-right: message log
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
            MessageLog(messages = inGameState.hudMessages, maxMessages = 8, nowMs = nowMs)
        }

        // Top-right: radar minimap
        if (demoMap != null) {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                RadarMinimap(
                    tiles = demoMap.tiles,
                    mapWidthBlocks = demoMap.width,
                    mapHeightBlocks = demoMap.height,
                    worldW = camera.worldW,
                    worldH = camera.worldH,
                    playerX = hud.view.playerX,
                    playerY = hud.view.playerY,
                    npcPositions = hud.view.npcPositions,
                    viewportOriginX = hud.view.originX,
                    viewportOriginY = hud.view.originY,
                    viewportW = hud.view.viewW,
                    viewportH = hud.view.viewH,
                    size = 180.dp,
                )
            }
        }

        // Top-left: scoreboard overlay (shown on TAB).
        // R17: was incorrectly placed at TopEnd, overlapping the minimap.
        Box(modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) {
            ScoreOverlay(players = inGameState.players, visible = inGameState.showScoreboard)
        }

        // Bottom-center: talk overlay
        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)) {
            TalkOverlay(state = inGameState.talkState)
        }

        // Bottom-left: LOG toggle button + MENU button
        Row(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GameButton(label = if (showLog) "LOG ×" else "LOG", onClick = { showLog = !showLog })
            GameButton(label = "MENU", onClick = { showExitConfirm = true })
        }

        // Log overlay (shown when showLog is true)
        if (showLog) {
            LogOverlay(onClose = {
                showLog = false
                focusRequester.requestFocus()
            })
        }

        // Exit-to-menu confirmation dialog
        if (showExitConfirm) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xAA000000)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier =
                        Modifier
                            .background(KXPilotColors.SurfaceVariant)
                            .border(1.dp, KXPilotColors.Accent)
                            .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        "Return to main menu?",
                        style =
                            TextStyle(
                                color = KXPilotColors.OnSurface,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GameButton("YES", onClick = { navigator.pop() })
                        GameButton("NO", onClick = {
                            showExitConfirm = false
                            focusRequester.requestFocus()
                        })
                    }
                }
            }
        }

        // Platform-specific input overlay (empty on desktop/web, touch controls on Android)
        PlatformControls(keys = keys)
    }
}

// ---------------------------------------------------------------------------
// Log overlay
// ---------------------------------------------------------------------------

/**
 * Semi-transparent overlay showing the last [AppLogger.MAX_ENTRIES] log lines.
 * Live-updates as new entries arrive via [AppLogger.entries] StateFlow.
 * A "SAVE" button triggers a platform save-file dialog.
 */
@Composable
private fun LogOverlay(onClose: () -> Unit) {
    val entries by AppLogger.entries.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom whenever entries change
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.size - 1)
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xCC000000)),
    ) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(fraction = 0.85f)
                    .heightIn(max = 500.dp)
                    .background(Color(0xEE0A0A0A))
                    .border(width = 1.dp, color = Color(0xFF334466))
                    .padding(12.dp),
        ) {
            // Header row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "LOG  (${entries.size}/${AppLogger.MAX_ENTRIES})",
                    color = Color(0xFF88AAFF),
                    style =
                        TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                        ),
                    modifier = Modifier.weight(1f),
                )
                GameButton(label = "SAVE", onClick = {
                    val snap = AppLogger.dump()
                    coroutineScope.launch {
                        saveTextFile(
                            title = "Save KXPilot Log",
                            defaultName = "kxpilot.log",
                            content = snap,
                        )
                    }
                })
                Spacer(modifier = Modifier.width(8.dp))
                GameButton(label = "CLEAR", onClick = { AppLogger.clear() })
                Spacer(modifier = Modifier.width(8.dp))
                GameButton(label = "CLOSE", onClick = onClose)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable log entries
            LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                items(entries) { line ->
                    Text(
                        text = line,
                        color = Color(0xFFCCCCCC),
                        style =
                            TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                            ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Key mapping
// ---------------------------------------------------------------------------

/**
 * Maps a [GameAction] to the engine [Key] it drives.
 * Actions that are purely UI (SCOREBOARD, TALK) return null — they are handled
 * in the key event handler directly rather than forwarded to the engine.
 */
private fun gameActionToKey(action: GameAction): Key? =
    when (action) {
        GameAction.TURN_LEFT -> Key.KEY_TURN_LEFT
        GameAction.TURN_RIGHT -> Key.KEY_TURN_RIGHT
        GameAction.THRUST -> Key.KEY_THRUST
        GameAction.FIRE_SHOT -> Key.KEY_FIRE_SHOT
        GameAction.SHIELD -> Key.KEY_SHIELD
        GameAction.FIRE_MISSILE -> Key.KEY_FIRE_MISSILE
        GameAction.DROP_MINE -> Key.KEY_DROP_MINE
        GameAction.CLOAK -> Key.KEY_CLOAK
        GameAction.SWAP_SETTINGS -> Key.KEY_SWAP_SETTINGS
        GameAction.LOCK_NEXT -> Key.KEY_LOCK_NEXT
        GameAction.LOCK_PREV -> Key.KEY_LOCK_PREV
        GameAction.TRACTOR_BEAM -> Key.KEY_TRACTOR_BEAM
        GameAction.GRAB_BALL -> Key.KEY_CONNECTOR
        GameAction.RESPAWN, GameAction.TALK, GameAction.SCOREBOARD, GameAction.EXIT_TO_MENU -> null
    }

// ---------------------------------------------------------------------------
// HUD rendering  (mirrors painthud.c gunsight-frame style)
// ---------------------------------------------------------------------------

private const val MIN_HUD_SIZE = 90f
private const val HUD_OFFSET = 20f
private const val FUEL_GAUGE_OFFSET = 6f
private const val HUD_SCALE = 2f

// Derived geometry — values are statically known, declared as const val to
// avoid runtime-computed field access on every drawHud call.
private const val HUD_SIZE = MIN_HUD_SIZE * HUD_SCALE // 180f
private const val HUD_FUEL_GAUGE_SIZE =
    2f * (MIN_HUD_SIZE - HUD_OFFSET - FUEL_GAUGE_OFFSET) // 128f
private const val METER_WIDTH = 60f
private const val METER_HEIGHT = 10f

// R16: use GameConst canonical values instead of local copies that could drift.
private val MAX_PLAYER_POWER = GameConst.MAX_PLAYER_POWER.toFloat()
private val MAX_PLAYER_TURNSPEED = GameConst.MAX_PLAYER_TURNSPEED.toFloat()
private const val MAX_SPEED_PX_TICK = 30f // practical cap for the speed meter bar

/** Pre-allocated spike angles for mine rendering — avoids per-frame `listOf` allocation. */
private val MINE_SPIKE_ANGLES = doubleArrayOf(0.0, kotlin.math.PI / 2.0, kotlin.math.PI, 3.0 * kotlin.math.PI / 2.0)

private fun DrawScope.drawHud(
    hud: HudSnapshot,
    textMeasurer: TextMeasurer,
) {
    val cx = size.width / 2f
    val cy = size.height / 2f

    val hudHalf = HUD_SIZE / 2f // 90px
    val off = HUD_OFFSET // 20px
    val BORDER = 3f

    // -------------------------------------------------------------------
    // Speed vector pointer — line from center toward velocity direction.
    // -------------------------------------------------------------------
    val velX = hud.stats.velX
    val velY = hud.stats.velY
    if (velX != 0f || velY != 0f) {
        val ptrFact = 5f
        drawLine(
            color = COL_HUD,
            start = Offset(cx, cy),
            end = Offset(cx - velX * ptrFact, cy + velY * ptrFact),
            strokeWidth = 1f,
            alpha = 0.6f,
        )
    }

    // -------------------------------------------------------------------
    // HUD frame — open-corner cross (4 dashed lines, painthud.c style).
    // -------------------------------------------------------------------
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
    val inset = hudHalf - off // 70px — where the lines terminate at corners

    // Top horizontal: full width, y = cy - inset
    drawLine(
        color = COL_HUD,
        start = Offset(cx - hudHalf, cy - inset),
        end = Offset(cx + hudHalf, cy - inset),
        strokeWidth = 1f,
        pathEffect = dashEffect,
    )
    // Bottom horizontal: full width, y = cy + inset
    drawLine(
        color = COL_HUD,
        start = Offset(cx - hudHalf, cy + inset),
        end = Offset(cx + hudHalf, cy + inset),
        strokeWidth = 1f,
        pathEffect = dashEffect,
    )
    // Left vertical: x = cx - inset, full height
    drawLine(
        color = COL_HUD,
        start = Offset(cx - inset, cy - hudHalf),
        end = Offset(cx - inset, cy + hudHalf),
        strokeWidth = 1f,
        pathEffect = dashEffect,
    )
    // Right vertical: x = cx + inset, full height
    drawLine(
        color = COL_HUD,
        start = Offset(cx + inset, cy - hudHalf),
        end = Offset(cx + inset, cy + hudHalf),
        strokeWidth = 1f,
        pathEffect = dashEffect,
    )

    // -------------------------------------------------------------------
    // Fuel gauge — vertical bar just inside the right vertical line.
    // -------------------------------------------------------------------
    val gaugeX = cx + hudHalf - off + FUEL_GAUGE_OFFSET
    val gaugeY = cy - hudHalf + off - FUEL_GAUGE_OFFSET
    val gaugeW = off - 2f * FUEL_GAUGE_OFFSET // 8px
    val gaugeH = HUD_FUEL_GAUGE_SIZE // 128px

    drawRect(
        color = COL_HUD,
        topLeft = Offset(gaugeX, gaugeY),
        size = Size(gaugeW, gaugeH),
        style = Stroke(width = 1f),
    )
    val fuelFrac = (hud.stats.fuel / hud.stats.fuelMax).coerceIn(0f, 1f)
    val fuelWarning = fuelFrac < 0.25f
    val fuelCritical = fuelFrac < 0.10f
    val showFuel =
        when {
            fuelCritical -> hud.loopCount % 8 < 4
            fuelWarning -> hud.loopCount % 4 < 2
            else -> true
        }
    if (showFuel && fuelFrac > 0f) {
        val fillH = gaugeH * fuelFrac
        drawRect(
            color =
                when {
                    fuelCritical -> Color(0xFFFF4444)
                    fuelWarning -> Color(0xFFFFAA00)
                    else -> COL_HUD
                },
            topLeft = Offset(gaugeX, gaugeY + gaugeH - fillH),
            size = Size(gaugeW, fillH),
        )
    }

    // -------------------------------------------------------------------
    // Fuel number — bottom-right of HUD frame
    // -------------------------------------------------------------------
    val fuelStr =
        hud.stats.fuel
            .toInt()
            .toString()
            .padStart(4, '0')
    val fuelMeasured = textMeasurer.measure(fuelStr, style = TextStyle(color = COL_HUD, fontSize = 9.sp))
    drawText(fuelMeasured, topLeft = Offset(cx + inset + BORDER, cy + inset + BORDER))

    // -------------------------------------------------------------------
    // Direction pointer — 15px segment r=85→100 in heading direction
    // -------------------------------------------------------------------
    val headingRad = hud.stats.headingRad.toDouble()
    val hdx = cos(headingRad).toFloat()
    val hdy = -sin(headingRad).toFloat() // screen Y is down
    drawLine(
        color = COL_HUD,
        start = Offset(cx + hdx * 85f, cy + hdy * 85f),
        end = Offset(cx + hdx * 100f, cy + hdy * 100f),
        strokeWidth = 2f,
    )

    // -------------------------------------------------------------------
    // Right-side meters: Power, Turnspeed, Speed
    // -------------------------------------------------------------------
    val meterX = size.width - METER_WIDTH - 10f
    drawHudMeter(
        textMeasurer,
        meterX,
        40f,
        "Power",
        (hud.stats.power / MAX_PLAYER_POWER).coerceIn(0f, 1f),
    )
    drawHudMeter(
        textMeasurer,
        meterX,
        60f,
        "Turnspeed",
        (hud.stats.turnSpeed / MAX_PLAYER_TURNSPEED).coerceIn(0f, 1f),
    )
    drawHudMeter(
        textMeasurer,
        meterX,
        80f,
        "Speed",
        (hud.stats.speed / MAX_SPEED_PX_TICK).coerceIn(0f, 1f),
    )

    // -------------------------------------------------------------------
    // Score line — centered below HUD frame
    // -------------------------------------------------------------------
    val scoreStr = "Score: ${hud.stats.score}  K: ${hud.stats.kills}  D: ${hud.stats.deaths}"
    val scoreMeasured = textMeasurer.measure(scoreStr, style = TextStyle(color = COL_HUD, fontSize = 9.sp))
    drawText(scoreMeasured, topLeft = Offset(cx - scoreMeasured.size.width / 2f, cy + hudHalf + 18f))

    // -------------------------------------------------------------------
    // Modifier string — bottom-left corner of HUD frame
    // -------------------------------------------------------------------
    if (hud.stats.modifiers.isNotEmpty()) {
        val modMeasured =
            textMeasurer.measure(
                hud.stats.modifiers,
                style = TextStyle(color = COL_HUD, fontSize = 9.sp),
            )
        drawText(
            modMeasured,
            topLeft =
                Offset(
                    cx - inset - BORDER - modMeasured.size.width,
                    cy + inset + BORDER,
                ),
        )
    }

    // -------------------------------------------------------------------
    // Time-left countdown — top-left corner of HUD frame
    // -------------------------------------------------------------------
    if (hud.view.timeLeftSec >= 0) {
        val mins = hud.view.timeLeftSec / 60
        val secs = hud.view.timeLeftSec % 60
        val timeStr = "${mins.toString().padStart(3, ' ')}:${secs.toString().padStart(2, '0')}"
        val timeMeasured = textMeasurer.measure(timeStr, style = TextStyle(color = COL_HUD, fontSize = 9.sp))
        drawText(
            timeMeasured,
            topLeft =
                Offset(
                    cx - inset - BORDER - timeMeasured.size.width,
                    cy - inset - BORDER - timeMeasured.size.height,
                ),
        )
    }

    // -------------------------------------------------------------------
    // Lock indicator
    // -------------------------------------------------------------------
    if (!hud.lock.dirRad.isNaN()) {
        val lockOrbitR = MIN_HUD_SIZE * 0.6f // 54px
        val WARNING_DIST = 150f
        val showLock = hud.lock.distPx < WARNING_DIST || hud.loopCount % 2 == 0
        if (showLock) {
            val dotSize = (10f * (1f - (hud.lock.distPx / 1200f).coerceIn(0f, 0.9f))).coerceAtLeast(2f)
            val dotCx = cx + cos(hud.lock.dirRad.toDouble()).toFloat() * lockOrbitR
            val dotCy = cy - sin(hud.lock.dirRad.toDouble()).toFloat() * lockOrbitR
            if (hud.lock.isAlly) {
                drawCircle(
                    color = Color(0xFF4488FF),
                    radius = dotSize / 2f,
                    center = Offset(dotCx, dotCy),
                    style = Stroke(width = 1.5f),
                )
            } else {
                drawCircle(
                    color = Color(0xFFFF6622),
                    radius = dotSize / 2f,
                    center = Offset(dotCx, dotCy),
                )
            }
        }

        // Target name — centered above top of HUD frame
        if (hud.lock.targetName.isNotEmpty()) {
            val nameMeasured =
                textMeasurer.measure(
                    hud.lock.targetName,
                    style =
                        TextStyle(
                            color = if (hud.lock.isAlly) Color(0xFF4488FF) else COL_HUD,
                            fontSize = 9.sp,
                        ),
                )
            drawText(
                nameMeasured,
                topLeft =
                    Offset(
                        cx - nameMeasured.size.width / 2f,
                        cy - inset - BORDER - nameMeasured.size.height,
                    ),
            )
        }

        // Distance in blocks — top-right corner of HUD frame
        if (hud.lock.targetDistBlocks >= 0) {
            val distStr =
                hud.lock.targetDistBlocks
                    .toString()
                    .padStart(3, '0')
            val distMeasured =
                textMeasurer.measure(
                    distStr,
                    style = TextStyle(color = COL_HUD, fontSize = 9.sp),
                )
            drawText(
                distMeasured,
                topLeft =
                    Offset(
                        cx + inset + BORDER,
                        cy - inset - BORDER - distMeasured.size.height,
                    ),
            )
        }
    }
}

/** Draws a single labeled horizontal meter bar at screen position (x, y). */
private fun DrawScope.drawHudMeter(
    textMeasurer: TextMeasurer,
    x: Float,
    y: Float,
    label: String,
    fraction: Float,
) {
    // Label
    val labelMeasured = textMeasurer.measure(label, style = TextStyle(color = COL_HUD, fontSize = 8.sp))
    drawText(labelMeasured, topLeft = Offset(x - labelMeasured.size.width - 4f, y))

    // Outline
    drawRect(
        color = COL_HUD,
        topLeft = Offset(x, y),
        size = Size(METER_WIDTH, METER_HEIGHT),
        style = Stroke(width = 1f),
    )
    // Fill
    if (fraction > 0f) {
        drawRect(
            color = COL_HUD,
            topLeft = Offset(x, y),
            size = Size(METER_WIDTH * fraction, METER_HEIGHT),
        )
    }

    // Tick marks at 0%, 25%, 50%, 75%, 100%
    val tickDefs = floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1.0f)
    val tickExts = floatArrayOf(4f, 1f, 3f, 1f, 4f)
    for (i in tickDefs.indices) {
        val tx = x + METER_WIDTH * tickDefs[i]
        val ext = tickExts[i]
        drawLine(
            color = COL_HUD,
            start = Offset(tx, y - ext),
            end = Offset(tx, y + METER_HEIGHT + ext),
            strokeWidth = 1f,
        )
    }
}

// ---------------------------------------------------------------------------
// Engine player rendering
// ---------------------------------------------------------------------------

private fun DrawScope.drawEnginePlayer(
    engine: GameEngine,
    camera: Camera,
    textMeasurer: TextMeasurer,
    playerName: String,
    shipShape: ShipShapeDef?,
) {
    val px = engine.playerPixelX
    val py = engine.playerPixelY
    if (!camera.isVisible(px, py, margin = RenderConst.SHIP_RADIUS + 4f)) return
    val screenPos = camera.worldToScreen(px, py)
    val angleDeg = (-engine.player.floatDir * (180.0 / kotlin.math.PI)).toFloat()
    val alive = engine.player.isAlive()
    val shipColor = if (alive) Color(0xFF00FF88) else Color(0xFFFF4444)
    val label = if (alive) "$playerName (${px.toInt()},${py.toInt()})" else "KILLED — press R"

    translate(left = screenPos.x, top = screenPos.y) {
        if (engine.shieldActive) {
            drawCircle(color = COL_SHIELD, radius = RenderConst.SHIP_RADIUS, center = Offset.Zero, style = Stroke(width = 2f))
        }
        if (alive) {
            rotate(degrees = angleDeg, pivot = Offset.Zero) {
                if (engine.player.isThrusting()) {
                    val flamePath =
                        Path().apply {
                            moveTo(-8f, 0f)
                            lineTo(-8f - 14f, 4f)
                            lineTo(-8f - 10f, 0f)
                            lineTo(-8f - 14f, -4f)
                            close()
                        }
                    drawPath(flamePath, color = Color(0xFFFF8800), style = Stroke(width = 2f))
                }
                val hull = shipShape?.hull
                val path =
                    if (hull != null && hull.size >= 2) {
                        Path().apply {
                            hull.forEachIndexed { i, (lx, ly) ->
                                if (i == 0) {
                                    moveTo(lx.toFloat(), -ly.toFloat())
                                } else {
                                    lineTo(lx.toFloat(), -ly.toFloat())
                                }
                            }
                            close()
                        }
                    } else {
                        Path().apply {
                            moveTo(RenderConst.SHIP_LOCAL_X[0], -RenderConst.SHIP_LOCAL_Y[0])
                            lineTo(RenderConst.SHIP_LOCAL_X[1], -RenderConst.SHIP_LOCAL_Y[1])
                            lineTo(RenderConst.SHIP_LOCAL_X[2], -RenderConst.SHIP_LOCAL_Y[2])
                            close()
                        }
                    }
                drawPath(path, color = shipColor, style = Stroke(width = 2f))
            }
        } else {
            drawLine(shipColor, Offset(-8f, -8f), Offset(8f, 8f), strokeWidth = 2f)
            drawLine(shipColor, Offset(8f, -8f), Offset(-8f, 8f), strokeWidth = 2f)
        }
    }

    val measured = textMeasurer.measure(label, style = TextStyle(color = shipColor, fontSize = 9.sp))
    drawText(
        measured,
        topLeft =
            Offset(
                screenPos.x - measured.size.width / 2f,
                screenPos.y - RenderConst.SHIP_RADIUS - measured.size.height - 2f,
            ),
    )
}

// ---------------------------------------------------------------------------
// Ship drawing
// ---------------------------------------------------------------------------

private fun DrawScope.drawShip(
    ship: DemoShip,
    camera: Camera,
    textMeasurer: TextMeasurer,
    totalShapes: Int,
    pathCache: HashMap<ShipShapeDef?, Path>,
) {
    if (!camera.isVisible(ship.x, ship.y, margin = RenderConst.SHIP_RADIUS + 4f)) return
    val screenPos = camera.worldToScreen(ship.x, ship.y)
    val color =
        when {
            ship.id % 2 == 0 -> COL_ALLY_SHIP
            else -> COL_ENEMY_SHIP
        }
    val angleDeg = -ship.heading * (360f / RenderConst.HEADING_MAX)
    translate(left = screenPos.x, top = screenPos.y) {
        if (ship.shield) {
            drawCircle(color = COL_SHIELD, radius = RenderConst.SHIP_RADIUS, center = Offset.Zero, style = Stroke(width = 1.5f))
        }
        rotate(degrees = angleDeg, pivot = Offset.Zero) {
            val path = pathCache.getOrPut(ship.shapeDef) { buildShipPath(ship.shapeDef) }
            drawPath(path, color = color, style = Stroke(width = 1.5f))
            val eng = ship.shapeDef?.engine
            if (eng != null) drawCircle(Color(0xFFFF8800), radius = 2.5f, center = Offset(eng.first.toFloat(), -eng.second.toFloat()))
            val gun = ship.shapeDef?.mainGun
            if (gun != null) drawCircle(Color(0xFFFFFF44), radius = 1.5f, center = Offset(gun.first.toFloat(), -gun.second.toFloat()))
        }
    }
    val shapeName = ship.shapeDef?.name?.let { " [$it]" } ?: ""
    val label = textMeasurer.measure("${ship.label}$shapeName", style = TextStyle(color = COL_LABEL, fontSize = 9.sp))
    drawText(label, topLeft = Offset(screenPos.x - label.size.width / 2f, screenPos.y - RenderConst.SHIP_RADIUS - label.size.height - 2f))
}

private fun buildShipPath(shapeDef: ShipShapeDef?): Path {
    if (shapeDef == null || shapeDef.hull.isEmpty()) {
        return Path().apply {
            moveTo(RenderConst.SHIP_LOCAL_X[0], -RenderConst.SHIP_LOCAL_Y[0])
            lineTo(RenderConst.SHIP_LOCAL_X[1], -RenderConst.SHIP_LOCAL_Y[1])
            lineTo(RenderConst.SHIP_LOCAL_X[2], -RenderConst.SHIP_LOCAL_Y[2])
            close()
        }
    }
    return Path().apply {
        val hull = shapeDef.hull
        val first = hull[0]
        moveTo(first.first.toFloat(), -first.second.toFloat())
        for (i in 1 until hull.size) lineTo(hull[i].first.toFloat(), -hull[i].second.toFloat())
        close()
    }
}

// ---------------------------------------------------------------------------
// Map tile rendering
// ---------------------------------------------------------------------------

private fun DrawScope.drawMapTiles(
    map: XPilotMap,
    camera: Camera,
) {
    if (map.width <= 0 || map.height <= 0) return
    val bs = GameConst.BLOCK_SZ.toFloat()

    fun tileScreenTopLeft(
        col: Int,
        row: Int,
    ): Offset {
        val worldCx = col * bs + bs / 2f
        val worldCy = row * bs + bs / 2f
        val centre = camera.worldToScreen(worldCx, worldCy)
        return Offset(centre.x - bs / 2f, centre.y - bs / 2f)
    }

    for (tile in map.tiles) {
        val topLeft = tileScreenTopLeft(tile.col, tile.row)
        if (topLeft.x > camera.viewW + bs || topLeft.x < -bs) continue
        if (topLeft.y > camera.viewH + bs || topLeft.y < -bs) continue

        when (tile.type) {
            BlockType.FILLED -> {
                drawRect(color = COL_MAP_WALL, topLeft = topLeft, size = Size(bs, bs))
            }

            BlockType.FUEL -> {
                drawRect(color = COL_MAP_FUEL, topLeft = topLeft, size = Size(bs, bs))
            }

            BlockType.REC_LU, BlockType.REC_LD, BlockType.REC_RU, BlockType.REC_RD -> {
                val left = topLeft.x
                val top = topLeft.y
                val path =
                    Path().apply {
                        when (tile.type) {
                            BlockType.REC_LU -> {
                                moveTo(left, top + bs)
                                lineTo(left, top)
                                lineTo(left + bs, top)
                            }

                            BlockType.REC_LD -> {
                                moveTo(left, top)
                                lineTo(left, top + bs)
                                lineTo(left + bs, top + bs)
                            }

                            BlockType.REC_RU -> {
                                moveTo(left, top)
                                lineTo(left + bs, top)
                                lineTo(left + bs, top + bs)
                            }

                            BlockType.REC_RD -> {
                                moveTo(left, top + bs)
                                lineTo(left + bs, top)
                                lineTo(left + bs, top + bs)
                            }

                            else -> {}
                        }
                        close()
                    }
                drawPath(path, color = COL_MAP_DIAG)
            }

            else -> {}
        }
    }

    for (base in map.bases) {
        val topLeft = tileScreenTopLeft(base.x, base.y)
        if (topLeft.x > camera.viewW + bs || topLeft.x < -bs) continue
        if (topLeft.y > camera.viewH + bs || topLeft.y < -bs) continue
        drawRect(color = COL_MAP_BASE, topLeft = topLeft, size = Size(bs, bs))
    }

    for (cannon in map.cannons) {
        val topLeft = tileScreenTopLeft(cannon.x, cannon.y)
        if (topLeft.x > camera.viewW + bs || topLeft.x < -bs) continue
        if (topLeft.y > camera.viewH + bs || topLeft.y < -bs) continue
        drawRect(color = COL_MAP_CANNON, topLeft = topLeft, size = Size(bs, bs))
    }
}
