package org.lambertland.kxpilot.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.lambertland.kxpilot.client.KeyState
import org.lambertland.kxpilot.common.GameConst
import org.lambertland.kxpilot.common.Key
import org.lambertland.kxpilot.common.toPixel
import org.lambertland.kxpilot.config.LocalAppConfig
import org.lambertland.kxpilot.config.XpOptionRegistry
import org.lambertland.kxpilot.engine.DemoGameState
import org.lambertland.kxpilot.engine.DemoShip
import org.lambertland.kxpilot.engine.GameEngine
import org.lambertland.kxpilot.engine.GameEngineFactory
import org.lambertland.kxpilot.engine.RenderConst
import org.lambertland.kxpilot.engine.buildNpcShipsFromBases
import org.lambertland.kxpilot.model.GameAction
import org.lambertland.kxpilot.model.MessageColor
import org.lambertland.kxpilot.model.MessageEntry
import org.lambertland.kxpilot.model.PlayerInfo
import org.lambertland.kxpilot.model.TalkResult
import org.lambertland.kxpilot.resources.BlockType
import org.lambertland.kxpilot.resources.ShipShapeDef
import org.lambertland.kxpilot.resources.XPilotMap
import org.lambertland.kxpilot.resources.parseShipShapes
import org.lambertland.kxpilot.resources.parseXPilotMap
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
// Colours (game-domain only — lives in desktopMain, not commonMain)
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
        val stream = object {}.javaClass.getResourceAsStream("/data/shipshapes.json")
        if (stream != null) {
            parseShipShapes(stream.bufferedReader().readText())
        } else {
            System.err.println("KXPilot: resource /data/shipshapes.json not found on classpath")
            emptyList()
        }
    } catch (e: Exception) {
        System.err.println("KXPilot: failed to load ship shapes: ${e::class.simpleName}: ${e.message}")
        emptyList()
    }

private fun loadMap(resourcePath: String): XPilotMap? =
    try {
        val stream = object {}.javaClass.getResourceAsStream(resourcePath)
        if (stream != null) {
            parseXPilotMap(stream.bufferedReader().readText())
        } else {
            System.err.println("KXPilot: map resource $resourcePath not found")
            null
        }
    } catch (e: Exception) {
        System.err.println("KXPilot: failed to load map $resourcePath: ${e::class.simpleName}: ${e.message}")
        null
    }

// ---------------------------------------------------------------------------
// InGameScreen
// ---------------------------------------------------------------------------

/**
 * Full-window in-game screen.  Lean composable — all UI state is owned by
 * [InGameStateHolder]; Canvas rendering is delegated to private functions.
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
                val now = System.currentTimeMillis()
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

    // nowMs updated every 100ms (not every frame) to throttle MessageLog recomposition
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(100L)
            nowMs = System.currentTimeMillis()
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
    val camera =
        remember {
            Camera(worldW = engine.world.width.toFloat(), worldH = engine.world.height.toFloat())
        }

    remember(engine) {
        engine.spawnAtBase(0)
        gameState = buildNpcShipsFromBases(engine, allShapes)
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
                engine.tick(keys, gs.ships)
                keys.advanceTick()
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
                    if (event.type == KeyEventType.KeyDown) {
                        if (inGameState.talkState.isVisible) {
                            when (event.key) {
                                ComposeKey.Enter -> {
                                    inGameState.submitTalk(System.currentTimeMillis())
                                    return@onKeyEvent true
                                }

                                ComposeKey.Escape -> {
                                    inGameState.talkState.close()
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
                        // Dispatch to all actions bound to this key
                        val actions = keyBindings.buildKeyMap()[event.key] ?: emptyList()
                        for (action in actions) {
                            when (action) {
                                GameAction.SCOREBOARD -> {
                                    inGameState.toggleScoreboard()
                                }

                                GameAction.TALK -> {
                                    inGameState.openTalk()
                                }

                                GameAction.RESPAWN -> {
                                    engine.spawnAtBase()
                                }

                                else -> {}
                            }
                        }
                    }
                    // Engine game keys — press/release for all matching actions
                    val actions = keyBindings.buildKeyMap()[event.key] ?: emptyList()
                    var consumed = false
                    for (action in actions) {
                        val kxpKey = gameActionToKey(action) ?: continue
                        when (event.type) {
                            KeyEventType.KeyDown -> keys.press(kxpKey)
                            KeyEventType.KeyUp -> keys.release(kxpKey)
                        }
                        consumed = true
                    }
                    consumed
                },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gs = gameState
            val gsShips = gs.ships // direct list access — no allocation

            if (demoMap != null) drawMapTiles(demoMap, camera)

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
                    val angleDeg = (-Math.toDegrees(m.headingRad)).toFloat()
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
                    val mineColor = if (mine.armTicks > 0) Color(0xFF888888) else Color(0xFFFF4444)
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

            // Balls: filled circle in team colour + connector line when attached.
            // No toList() copy needed — rendering runs on the same coroutine/thread
            // as the game loop (both inside the same LaunchedEffect/withFrameMillis
            // block), so there is no concurrent modification risk.
            // Ball is rendered in the colour of the team that last touched it
            // (touchTeam), not the home tile's team (homeTeam), so it visually
            // communicates possession.  Neutral balls (touchTeam == -1) fall back
            // to TEAM_COLORS[0] (white).
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

        // Top-right (overlay): scoreboard overlay (shown on TAB)
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
            ScoreOverlay(players = inGameState.players, visible = inGameState.showScoreboard)
        }

        // Bottom-center: talk overlay
        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)) {
            TalkOverlay(state = inGameState.talkState)
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
        GameAction.RESPAWN, GameAction.TALK, GameAction.SCOREBOARD -> null
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
private const val MAX_PLAYER_POWER = 55f // GameConst.MAX_PLAYER_POWER
private const val MAX_PLAYER_TURNSPEED = 64f // GameConst.MAX_PLAYER_TURNSPEED (heading-units/tick)
private const val MAX_SPEED_PX_TICK = 30f // practical cap for the speed meter bar

/** Pre-allocated spike angles for mine rendering — avoids per-frame `listOf` allocation. */
private val MINE_SPIKE_ANGLES = doubleArrayOf(0.0, Math.PI / 2.0, Math.PI, 3.0 * Math.PI / 2.0)

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
    // Factor 5 makes a ~1px/tick speed produce a 5px line; matches
    // painthud.c ptr_move_fact (we use a fixed 5.0).
    // Only drawn when moving.
    // -------------------------------------------------------------------
    val velX = hud.stats.velX
    val velY = hud.stats.velY
    if (velX != 0f || velY != 0f) {
        // C draws: center → (cx - fact*vx, cy + fact*vy).  Y-up ⟹ negate vy on screen.
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
    //
    // painthud.c:
    //   H-lines: y = hud_pos_y ± (hudSize - HUD_OFFSET), x = ±hudSize (full width)
    //   V-lines: x = hud_pos_x ± (hudSize - HUD_OFFSET), y = ±hudSize (full height)
    //
    // This creates four lines that stop short of the corners by HUD_OFFSET,
    // producing an open-corner bracket/crosshair shape.
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
    // gaugeX = cx + hudSize - HUD_OFFSET + FUEL_GAUGE_OFFSET (painthud.c line 753)
    //        = cx + 90 - 20 + 6 = cx + 76
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
    // Fuel number — bottom-right of HUD frame (painthud.c line 643)
    // "hud_pos_x + hudSize - HUD_OFFSET + BORDER" = cx + inset + BORDER
    // "hud_pos_y + hudSize - HUD_OFFSET + BORDER" = cy + inset + BORDER
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
    // (painthud.c lines 562-569, dirPtrColor)
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
    // Right-side meters: Power, Turnspeed, Speed (painthud.c Paint_meters)
    // Drawn right-aligned, stacked at y=40/60/80.
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
    // painthud.c line 717: x = hud_pos_x - hudSize + HUD_OFFSET - BORDER - textWidth
    //                       y = hud_pos_y + hudSize - HUD_OFFSET + BORDER
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
    // painthud.c line 704: "MM:SS" drawn at
    //   x = hud_pos_x - hudSize + HUD_OFFSET - BORDER - textWidth
    //   y = hud_pos_y - hudSize + HUD_OFFSET - BORDER (above top H-line)
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
    // Lock indicator:
    //   - Dot orbiting HUD at r=54 in lock direction
    //   - Target name above top-left corner (painthud.c line 294)
    //   - Distance in blocks at top-right corner (painthud.c line 312)
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
        // painthud.c: x = hud_pos_x - name_width/2,  y = hud_pos_y - hudSize + HUD_OFFSET - BORDER
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
        // painthud.c: x = hud_pos_x + hudSize - HUD_OFFSET + BORDER,  same y as name
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

/** Draws a single labeled horizontal meter bar at screen position (x, y).
 *
 * Mirrors `Paint_meter()` in painthud.c:
 * - Outline rectangle
 * - Filled bar proportional to [fraction]
 * - 5 vertical scale tick marks at 0/25/50/75/100% (±4/1/3/1/4 px tall)
 * - Label text to the left
 */
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

    // Tick marks at 0%, 25%, 50%, 75%, 100% — painthud.c lines 101-105.
    // Extension heights: 0/100% → ±4px, 50% → ±3px, 25/75% → ±1px.
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
    val angleDeg = (-Math.toDegrees(engine.player.floatDir)).toFloat()
    val alive = engine.player.isAlive()
    val shipColor = if (alive) Color(0xFF00FF88) else Color(0xFFFF4444)
    val label = if (alive) "$playerName (${px.toInt()},${py.toInt()})" else "KILLED — press R"

    translate(left = screenPos.x, top = screenPos.y) {
        drawCircle(color = COL_SHIELD, radius = RenderConst.SHIP_RADIUS, center = Offset.Zero, style = Stroke(width = 2f))
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
