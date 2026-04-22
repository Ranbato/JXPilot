package org.lambertland.kxpilot.client

import org.lambertland.kxpilot.common.IPos
import org.lambertland.kxpilot.common.IRect
import org.lambertland.kxpilot.common.ShipShape

// ---------------------------------------------------------------------------
// Ported from: client/client.h
// ---------------------------------------------------------------------------

// ------------------------------------------------------------------
// Client-side constants
// ------------------------------------------------------------------

/** Packet measurement status codes. */
object PacketStatus {
    const val LOSS: Int = 0
    const val DROP: Int = 1
    const val DRAW: Int = 2
}

/**
 * Client-side display / UI limit constants.
 * Maps to the `MAX_*` / `MIN_*` `#define` values in client/client.h.
 */
object ClientConst {
    const val MAX_SCORE_OBJECTS: Int = 10
    const val MAX_SPARK_SIZE: Int = 8
    const val MIN_SPARK_SIZE: Int = 1
    const val MAX_MAP_POINT_SIZE: Int = 8
    const val MIN_MAP_POINT_SIZE: Int = 0
    const val MAX_SHOT_SIZE: Int = 20
    const val MIN_SHOT_SIZE: Int = 1
    const val MAX_TEAMSHOT_SIZE: Int = 20
    const val MIN_TEAMSHOT_SIZE: Int = 1
    const val MIN_SHOW_ITEMS_TIME: Double = 0.0
    const val MAX_SHOW_ITEMS_TIME: Double = 300.0
    const val MIN_SCALEFACTOR: Double = 0.1
    const val MAX_SCALEFACTOR: Double = 20.0
    const val FUEL_NOTIFY_TIME: Double = 3.0
    const val CONTROL_TIME: Double = 8.0
    const val MAX_MSGS: Int = 15
    const val MAX_HIST_MSGS: Int = 128
    const val MSG_LIFE_TIME: Double = 120.0
    const val MSG_FLASH_TIME: Double = 105.0
    const val MAX_POINTER_BUTTONS: Int = 5
}

// ------------------------------------------------------------------
// Client-side aggregate data
// ------------------------------------------------------------------

/**
 * Top-level client runtime state.
 * Maps to C `client_data_t` in client/client.h.
 */
data class ClientData(
    val talking: Boolean = false,
    val pointerControl: Boolean = false,
    val restorePointerControl: Boolean = false,
    val quitMode: Boolean = false,
    val clientLag: Double = 0.0,
    val scaleFactor: Double = 1.0,
    val scale: Double = 1.0,
    val fscale: Float = 1f,
    val altScaleFactor: Double = 1.0,
)

/**
 * HUD instrument display toggles.
 * Maps to C `instruments_t` in client/client.h.
 */
data class Instruments(
    val clientRanker: Boolean = false,
    val clockAMPM: Boolean = false,
    val filledDecor: Boolean = false,
    val filledWorld: Boolean = false,
    val outlineDecor: Boolean = false,
    val outlineWorld: Boolean = false,
    val showDecor: Boolean = false,
    val showItems: Boolean = false,
    val showLivesByShip: Boolean = false,
    val showMessages: Boolean = true,
    val showMyShipShape: Boolean = true,
    val showNastyShots: Boolean = false,
    val showShipShapes: Boolean = true,
    val showShipShapesHack: Boolean = false,
    val slidingRadar: Boolean = false,
    val texturedDecor: Boolean = false,
    val texturedWalls: Boolean = false,
)

/**
 * Command-line arguments parsed at startup.
 * Maps to C `xp_args_t` in client/client.h.
 */
data class XpArgs(
    val help: Boolean = false,
    val version: Boolean = false,
    val text: Boolean = false,
    val listServers: Boolean = false,
    val autoConnect: Boolean = false,
    val shutdownReason: String = "",
)

// ------------------------------------------------------------------
// Remote player view
// ------------------------------------------------------------------

/**
 * Client-side view of a remote player (score table entry).
 * Maps to C `other_t` in client/client.h.
 */
data class OtherPlayer(
    val score: Double = 0.0,
    val id: Short = 0,
    val team: UShort = 0u,
    val check: Short = 0,
    val round: Short = 0,
    val timingLoops: Long = 0L,
    val timing: Short = 0,
    val life: Short = 0,
    val myChar: Short = 0,
    val alliance: Short = 0,
    val nameWidth: Short = 0,
    val nameLen: Short = 0,
    val maxCharsInNames: Short = 0,
    val ignoreLevel: Short = 0,
    val ship: ShipShape? = null,
    val nickName: String = "",
    val userName: String = "",
    val hostName: String = "",
    val idString: String = "",
)

// ------------------------------------------------------------------
// Map static entities (client-side view)
// ------------------------------------------------------------------

/** A fuel station as seen by the client.  Maps to C `fuelstation_t`. */
data class FuelStation(
    val pos: Int,
    val fuel: Double,
    val bounds: IRect,
)

/** A spawn base as seen by the client.  Maps to C `homebase_t`. */
data class HomeBase(
    val pos: Int,
    val id: Short,
    val team: UShort,
    val bounds: IRect,
    val type: Int,
    val appearTime: Long,
)

/** Cannon activity timer.  Maps to C `cannontime_t`. */
data class CannonTime(
    val pos: Int,
    val deadTime: Short,
    val dot: Short,
)

/** A destructible target as seen by the client.  Maps to C `target_t` (client version). */
data class ClientTarget(
    val pos: Int,
    val deadTime: Short,
    val damage: Double,
)

/** A timing checkpoint as seen by the client.  Maps to C `checkpoint_t`. */
data class Checkpoint(
    val pos: Int,
    val bounds: IRect,
)

// ------------------------------------------------------------------
// Polygon / style types
// ------------------------------------------------------------------

/** Edge rendering style.  Maps to C `edge_style_t`. */
data class EdgeStyle(
    val width: Int,
    val color: Long,
    val rgb: Int,
    val style: Int,
)

/** Polygon fill/texture style.  Maps to C `polygon_style_t`. */
data class PolygonStyleDef(
    val color: Long,
    val rgb: Int,
    val texture: Int,
    val flags: Int,
    val defEdgeStyle: Int,
)

/** A client-side polygon (map decoration or wall).  Maps to C `xp_polygon_t`. */
data class XpPolygon(
    val points: Array<IPos>,
    val bounds: IRect,
    val edgeStyles: IntArray?,
    val style: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is XpPolygon) return false
        return bounds == other.bounds && style == other.style &&
            points.contentEquals(other.points) &&
            (edgeStyles?.contentEquals(other.edgeStyles ?: intArrayOf()) ?: (other.edgeStyles == null))
    }

    override fun hashCode(): Int = 31 * bounds.hashCode() + points.contentHashCode()
}

// ------------------------------------------------------------------
// Dynamic per-frame game render data
// ------------------------------------------------------------------

/** A refueling beam line.  Maps to C `refuel_t`. */
data class Refuel(
    val x0: Short,
    val y0: Short,
    val x1: Short,
    val y1: Short,
)

/** A connector / tractor beam line.  Maps to C `connector_t`. */
data class Connector(
    val x0: Short,
    val y0: Short,
    val x1: Short,
    val y1: Short,
    val tractor: UByte,
)

/** A laser beam segment.  Maps to C `laser_t`. */
data class Laser(
    val color: UByte,
    val dir: UByte,
    val x: Short,
    val y: Short,
    val len: Short,
)

/** A missile render packet.  Maps to C `missile_t`. */
data class Missile(
    val x: Short,
    val y: Short,
    val dir: Short,
    val len: UByte,
)

/** A ball render packet.  Maps to C `ball_t`. */
data class Ball(
    val x: Short,
    val y: Short,
    val id: Short,
    val style: UByte,
)

/** A ship render packet.  Maps to C `ship_t`. */
data class Ship(
    val x: Short,
    val y: Short,
    val id: Short,
    val dir: Short,
    val shield: UByte,
    val cloak: UByte,
    val eshield: UByte,
    val phased: UByte,
    val deflector: UByte,
)

/** A mine render packet.  Maps to C `mine_t`. */
data class Mine(
    val x: Short,
    val y: Short,
    val teammine: Short,
    val id: Short,
)

/** An item floating in space.  Maps to C `itemtype_t`. */
data class ItemAppearance(
    val x: Short,
    val y: Short,
    val type: Short,
)

/** A client-side ECM pulse.  Maps to C `ecm_t` (client version). */
data class ClientEcm(
    val x: Short,
    val y: Short,
    val size: Short,
)

/** A transporter beam.  Maps to C `trans_t`. */
data class Trans(
    val x1: Short,
    val y1: Short,
    val x2: Short,
    val y2: Short,
)

/** A paused player indicator.  Maps to C `paused_t`. */
data class Paused(
    val x: Short,
    val y: Short,
    val count: Short,
)

/** An appearing player indicator.  Maps to C `appearing_t`. */
data class Appearing(
    val x: Short,
    val y: Short,
    val id: Short,
    val count: Short,
)

/** Radar type (friend or foe).  Maps to C `radar_type_t`. */
enum class RadarType { ENEMY, FRIEND }

/** A radar blip.  Maps to C `radar_t`. */
data class Radar(
    val x: Short,
    val y: Short,
    val size: Short,
    val type: RadarType,
)

/** A visible cannon.  Maps to C `vcannon_t`. */
data class VCannon(
    val x: Short,
    val y: Short,
    val type: Short,
)

/** A visible fuel station.  Maps to C `vfuel_t`. */
data class VFuel(
    val x: Short,
    val y: Short,
    val fuel: Double,
)

/** A visible base tile.  Maps to C `vbase_t`. */
data class VBase(
    val x: Short,
    val y: Short,
    val xi: Short,
    val yi: Short,
    val type: Short,
)

/** A debris/spark pixel.  Maps to C `debris_t`. */
data class Debris(
    val x: UByte,
    val y: UByte,
)

/** A visible decoration tile.  Maps to C `vdecor_t`. */
data class VDecor(
    val x: Short,
    val y: Short,
    val xi: Short,
    val yi: Short,
    val type: Short,
)

/** A ship wreckage object.  Maps to C `wreckage_t`. */
data class Wreckage(
    val x: Short,
    val y: Short,
    val wreckType: UByte,
    val size: UByte,
    val rotation: UByte,
)

/** A client-side asteroid.  Maps to C `asteroid_t` (client version). */
data class ClientAsteroid(
    val x: Short,
    val y: Short,
    val type: UByte,
    val size: UByte,
    val rotation: UByte,
)

/** A client-side wormhole marker.  Maps to C `wormhole_t` (client version). */
data class ClientWormhole(
    val x: Short,
    val y: Short,
)

/** A floating score annotation.  Maps to C `score_object_t`. */
data class ScoreObject(
    val score: Double,
    val lifeTime: Double,
    val x: Int,
    val y: Int,
    val msg: String,
    val hudMsg: String,
)
