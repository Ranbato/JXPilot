package org.lambertland.kxpilot.engine

import org.lambertland.kxpilot.common.toPixel
import org.lambertland.kxpilot.resources.ShipShapeDef
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Render constants (mirror the Java client values)
// ---------------------------------------------------------------------------

object RenderConst {
    /** XPilot heading is 0–127 for a full circle (128 units). */
    const val HEADING_MAX = 128

    /** Convert a 128-unit heading to radians (Float). */
    fun headingToRadians(h: Float): Float = (PI / 64.0 * h).toFloat()

    /** Ship radius used for shield circle and name offset (pixels). */
    const val SHIP_RADIUS = 16f

    // Ship polygon in local coordinates (pointing right = heading 0).
    // Vertices: nose(14,0), left-wing(-8,8), right-wing(-8,-8).
    // Note: Y is Cartesian (up-positive); rendering flips as needed.
    val SHIP_LOCAL_X = floatArrayOf(14f, -8f, -8f)
    val SHIP_LOCAL_Y = floatArrayOf(0f, 8f, -8f)

    // Object sizes (pixels in world/screen space)
    const val SHOT_RADIUS = 2f
}

// ---------------------------------------------------------------------------
// Demo game-state data model
// ---------------------------------------------------------------------------

/**
 * NPC ids are offset by this value so they never equal [GameConst.NO_ID] (0)
 * or the player id (1), and always fit in a Short (≤ 32 767) for
 * [ShotData.ownerId].  With a maximum of ~100 bases per map the upper bound is
 * far from the Short limit.
 */
const val NPC_ID_BASE = 100

/**
 * An NPC ship in the demo/local world.
 *
 * Plain `class` (not `data class`) — fields are mutated every tick, so the
 * generated equals/hashCode of a data class would produce incorrect results
 * after any mutation.
 */
class DemoShip(
    override val id: Int,
    val label: String,
    /** World-space position in XPilot pixels. */
    override var x: Float,
    override var y: Float,
    /** Heading in 128-unit circle (0 = right, 32 = up, 64 = left, 96 = down). */
    var heading: Float,
    /** Velocity in pixels/tick. */
    override var vx: Float,
    override var vy: Float,
    /** Rotation speed in heading-units/tick. */
    var rotSpeed: Float,
    override var shield: Boolean = false,
    /** Fuel used as health (no separate HP concept in C; matches EngineConst.NPC_INITIAL_HP). */
    override var hp: Float = EngineConst.NPC_INITIAL_HP,
    /**
     * Desired velocity set by NPC AI each tick (pixels/tick).
     * [DemoGameState.tick] blends this into [vx]/[vy], allowing external forces
     * (tractor/pressor beams, wall bounces) to add on top before integration.
     * The AI writes [desiredVx]/[desiredVy]; physics reads and integrates [vx]/[vy].
     */
    var desiredVx: Float = 0f,
    var desiredVy: Float = 0f,
    /** Optional ship shape loaded from shipshapes.json; null = default triangle. */
    var shapeDef: ShipShapeDef? = null,
    /**
     * Id of the ball this NPC is currently carrying (connected via its tether).
     * Sentinel value [BallData.NO_PLAYER] (-1) means no ball is attached.
     * Mirrors [BallData.connectedPlayerId] — the engine keeps both in sync.
     */
    var carryingBallId: Int = BallData.NO_PLAYER,
    /**
     * CTF score — incremented each time this NPC delivers a ball to the opposing goal.
     * Matches C's per-robot score field (robotdef.c).
     */
    var score: Double = 0.0,
    // --- BL-20: weapon items ---
    /**
     * Number of missile items held. C: pl->item[ITEM_MISSILE].
     * When > 0, the NPC may fire smart/heat/torpedo missiles.
     */
    var missileCount: Int = 0,
    /**
     * Number of mine items held. C: pl->item[ITEM_MINE].
     * When > 0, the NPC may drop proximity mines.
     */
    var mineCount: Int = 0,
    /**
     * Whether the NPC has a shield item. C: BIT(pl->have, HAS_SHIELD).
     * When true, the NPC activates its shield while low on HP.
     */
    var hasShieldItem: Boolean = false,
) : EngineTarget {
    override fun toString(): String = "DemoShip(id=$id, label=$label, x=$x, y=$y, heading=$heading, hp=$hp)"
}

// ---------------------------------------------------------------------------
// Mutable game state — tick() advances physics for the demo
// ---------------------------------------------------------------------------

class DemoGameState(
    val worldW: Float,
    val worldH: Float,
    /** Optional engine used for wall collision.  When null, NPCs wrap freely. */
    private val engine: GameEngine? = null,
) {
    val ships = mutableListOf<DemoShip>()

    /** Advance simulation by one frame. */
    fun tick() {
        for (s in ships) {
            // Blend AI-desired velocity into actual velocity.  Any external forces
            // (tractor/pressor beams, wall bounces) have already been applied to
            // s.vx/vy by engine.tick(); blending at 0.15 lets them persist briefly
            // instead of being instantly wiped by the next AI decision.
            // 0.15 = move 15% toward desired per tick (≈ exponential lag ~6 ticks to
            // converge); NOT 0.85 which would snap 85% immediately — that was inverted.
            s.vx += (s.desiredVx - s.vx) * 0.15f
            s.vy += (s.desiredVy - s.vy) * 0.15f
            if (engine != null) {
                val result = engine.sweepMovePixels(s.x, s.y, s.vx, s.vy)
                s.x = result.first
                s.y = result.second
                s.vx = result.third.first
                s.vy = result.third.second
            } else {
                s.x = wrap(s.x + s.vx, worldW)
                s.y = wrap(s.y + s.vy, worldH)
            }
            s.heading = wrap128(s.heading + s.rotSpeed)
        }
    }

    // ---- helpers ----

    private fun wrap(
        v: Float,
        size: Float,
    ): Float {
        if (size <= 0f) return v
        var r = v % size
        if (r < 0f) r += size
        return r
    }

    private fun wrap128(h: Float): Float {
        var r = h % RenderConst.HEADING_MAX
        if (r < 0f) r += RenderConst.HEADING_MAX
        return r
    }
}

// ---------------------------------------------------------------------------
// Factory: build NPC ships from map bases
// ---------------------------------------------------------------------------

/**
 * Creates one [DemoShip] per map base, starting at base index 1
 * (base 0 is reserved for the engine player).  Each NPC is placed at the
 * base's world-pixel position and given a small initial velocity away from
 * its spawn direction so it immediately starts moving.
 *
 * Returns an empty [DemoGameState] if the engine world has no bases or only
 * one base (the player's).
 */
fun buildNpcShipsFromBases(
    engine: GameEngine,
    availableShapes: List<ShipShapeDef> = emptyList(),
): DemoGameState {
    val worldW = engine.world.width.toFloat()
    val worldH = engine.world.height.toFloat()
    val state = DemoGameState(worldW, worldH, engine)

    val bases = engine.world.bases
    // Base 0 belongs to the player; NPC ships get the remaining bases.
    if (bases.size <= 1) return state

    fun pickShape(index: Int): ShipShapeDef? =
        if (availableShapes.isEmpty()) {
            null
        } else {
            availableShapes[(index * 37 + 7) % availableShapes.size]
        }

    // NPC speed in pixels/tick (slow drift, like XPilot bots idling at spawn)
    val SPAWN_SPEED = 0.6f

    bases.forEachIndexed { idx, base ->
        if (idx == 0) return@forEachIndexed // skip the player's base

        val px =
            base.pos.cx
                .toPixel()
                .toFloat()
        val py =
            base.pos.cy
                .toPixel()
                .toFloat()

        // heading in 0..127; convert to radians for velocity direction
        val headingRad = (base.dir.toFloat() / RenderConst.HEADING_MAX) * (2f * PI.toFloat())
        val vx = cos(headingRad) * SPAWN_SPEED
        val vy = sin(headingRad) * SPAWN_SPEED

        // Deterministic rotation speed: alternates CW / CCW per ship
        val rotSpeed = if (idx % 2 == 0) 0.4f else -0.3f

        val npcId = idx + NPC_ID_BASE
        val teamLabel = if (base.team > 0) "T${base.team}" else "Bot"
        val shape = pickShape(idx)

        // Factory: returns a fresh DemoShip at home position with full HP.
        // Captured variables (px, py, heading, vx, vy, rotSpeed, shape) are
        // immutable for this NPC — safe to close over.
        val factory: () -> EngineTarget = {
            DemoShip(
                id = npcId,
                label = "$teamLabel-$idx",
                x = px,
                y = py,
                heading = base.dir.toFloat(),
                vx = vx,
                vy = vy,
                rotSpeed = rotSpeed,
                shapeDef = shape,
            )
        }
        engine.registerNpcFactory(npcId, factory)

        state.ships +=
            DemoShip(
                // NPC ids are offset by NPC_ID_BASE to avoid colliding with
                // player.id (== 1). Must never equal GameConst.NO_ID (0) or
                // player.id (1), and must fit in a Short (≤ 32767) for ownerId.
                id = npcId,
                label = "$teamLabel-$idx",
                x = px,
                y = py,
                heading = base.dir.toFloat(),
                vx = vx,
                vy = vy,
                rotSpeed = rotSpeed,
                shapeDef = shape,
            )
    }

    return state
}
