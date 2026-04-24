package org.lambertland.kxpilot.server

import org.lambertland.kxpilot.common.GameConst
import org.lambertland.kxpilot.common.Key
import org.lambertland.kxpilot.resources.parseXPilotMap
import org.lambertland.kxpilot.resources.toWorld
import org.lambertland.kxpilot.server.ObjectPools

// ---------------------------------------------------------------------------
// ServerGameWorld
// ---------------------------------------------------------------------------
//
// Holds the live game-world state (World geometry + all active Player objects)
// for the duration of a running server.  One instance per server run; discarded
// and re-created on restart or map change.
//
// The Map is either built from a .xp file (via parseXPilotMap / toWorld)
// or from a built-in default "open field" world used when no map is configured.
//
// Responsibilities:
//   - Provide a default empty world when no map path is configured.
//   - Spawn / despawn Player objects as clients join / leave.
//   - Expose the player map for physics and net-frame broadcasting.

/**
 * Default world size (blocks) used when no .xp map file is configured.
 * Chosen to give a spacious open-field arena.
 */
private const val DEFAULT_WORLD_COLS = 60
private const val DEFAULT_WORLD_ROWS = 60

/**
 * Default player physics parameters.
 * Mirrors the C server defaults in server/option.c.
 */
internal object PlayerDefaults {
    const val POWER: Double = 35.0
    const val TURNSPEED: Double = 30.0

    // C server sets turnresistance = 0.2 for human players (server/update.c:261).
    // The previous value 0.12 is the robot default (server/robot.c:743) — wrong for humans.
    const val TURNRESISTANCE: Double = 0.2

    /** Starting fuel (same as C `MAX_PLAYER_FUEL * 0.5`). */
    const val START_FUEL: Double = GameConst.MAX_PLAYER_FUEL * 0.5

    /** Maximum fuel. */
    const val MAX_FUEL: Double = GameConst.MAX_PLAYER_FUEL

    /** Empty ship mass (pixels²/tick²). */
    const val EMPTY_MASS: Double = 20.0
}

/**
 * Live server world.  Constructed on [ServerController.start] and passed into
 * [ServerGameLoop] for physics and broadcasting.
 *
 * @param config The current server configuration.
 */
class ServerGameWorld(
    config: ServerConfig,
) {
    /** The map geometry and entity lists. */
    val world: World = World()

    /** Active physics players, keyed by their session [org.lambertland.kxpilot.net.ClientSession.id]. */
    private val _players: MutableMap<Int, Player> = mutableMapOf()
    val players: Map<Int, Player> get() = _players

    /** Object pools for shots and other game objects. */
    val pools: ObjectPools = ObjectPools()

    /** Monotonically increasing frame counter. */
    var frameLoop: Long = 0L
        private set

    init {
        // Load the map from config.mapPath if provided and readable;
        // fall back to a default open-field world otherwise.
        val loaded: Boolean =
            if (config.mapPath != null) {
                val text = readFileTextOrNull(config.mapPath)
                if (text != null) {
                    try {
                        val xpMap = parseXPilotMap(text)
                        val loadedWorld = xpMap.toWorld()
                        // Copy all geometry from the parsed world into `world`
                        world.copyFrom(loadedWorld)
                        true
                    } catch (_: Exception) {
                        false
                    }
                } else {
                    false
                }
            } else {
                false
            }
        if (!loaded) {
            world.initGrid(DEFAULT_WORLD_COLS, DEFAULT_WORLD_ROWS)
        }
        world.name = config.serverName
    }

    // -----------------------------------------------------------------------
    // Player lifecycle
    // -----------------------------------------------------------------------

    /**
     * Create and spawn a [Player] for the given [sessionId].
     *
     * The player is placed at the first available base, or at the world
     * centre if no bases are defined.
     *
     * @return The newly spawned [Player].
     */
    fun spawnPlayer(
        sessionId: Int,
        nick: String,
        userName: String,
        team: Int,
    ): Player {
        val pl = Player()
        pl.name = nick
        pl.userName = userName
        pl.team = team.toUShort()
        pl.plState = PlayerState.ALIVE
        pl.plType = PlayerType.HUMAN

        initPlayerPhysics(pl)

        // Spawn position: first base for this team, otherwise world centre
        val base = world.findSpawnBase(team)
        if (base != null) {
            pl.pos = base.pos
            pl.setFloatDir(dirToRadians(base.dir))
        } else {
            val cx = world.cwidth / 2
            val cy = world.cheight / 2
            pl.pos =
                org.lambertland.kxpilot.common
                    .ClPos(cx, cy)
            pl.setFloatDir(0.0)
        }
        pl.dir = radToDir(pl.floatDir)
        pl.lastSafePos = pl.pos // initialise to spawn position (never ClPos(0,0) landmine)

        _players[sessionId] = pl
        return pl
    }

    /**
     * Respawn a dead player at [spawnPos] facing [spawnDir].
     *
     * Resets combat state (fuel, position, velocity) but preserves identity and
     * accumulated score/kills/deaths across lives.
     */
    fun respawn(
        pl: Player,
        spawnPos: org.lambertland.kxpilot.common.ClPos,
        spawnDir: Double,
    ) {
        val savedScore = pl.score
        val savedKills = pl.kills
        val savedDeaths = pl.deaths
        val savedMyChar = pl.myChar
        val savedName = pl.name
        val savedUserName = pl.userName
        val savedTeam: UShort = pl.team

        pl.resetForRespawn()

        pl.score = savedScore
        pl.kills = savedKills
        pl.deaths = savedDeaths
        pl.myChar = savedMyChar
        pl.name = savedName
        pl.userName = savedUserName
        pl.team = savedTeam

        initPlayerPhysics(pl)

        pl.pos = spawnPos
        pl.setFloatDir(spawnDir)
        pl.dir = radToDir(spawnDir)
        pl.lastSafePos = spawnPos // initialise to spawn position (never ClPos(0,0) landmine)
        pl.plState = PlayerState.ALIVE
    }

    /** Set default physics parameters shared by spawnPlayer and respawn. */
    private fun initPlayerPhysics(pl: Player) {
        pl.power = PlayerDefaults.POWER
        pl.turnspeed = PlayerDefaults.TURNSPEED
        pl.turnresistance = PlayerDefaults.TURNRESISTANCE
        pl.fuel.sum = PlayerDefaults.START_FUEL
        pl.fuel.max = PlayerDefaults.MAX_FUEL
        pl.fuel.tank[0] = PlayerDefaults.START_FUEL
        pl.emptyMass = PlayerDefaults.EMPTY_MASS
        pl.mass = PlayerDefaults.EMPTY_MASS.toFloat()
        // Clear all objStatus flags from a previous life (phasing, cloaking, etc.)
        // then re-apply gravity.  Stale flags surviving a respawn can cause the
        // player to start cloaked or phased without any item grant.
        pl.objStatus = ObjStatus.GRAVITY.toUShort()
        // Mirror C DEF_HAVE / DEF_USED (rules.c:52-54):
        //   DEF_HAVE = HAS_SHIELD|HAS_COMPASS|HAS_REFUEL|HAS_REPAIR|HAS_CONNECTOR|HAS_SHOT|HAS_LASER
        //   DEF_USED = HAS_SHIELD|HAS_COMPASS
        // Full assignment is intentional — matches C Kill_player() which resets `have`
        // to DEF_HAVE on each spawn.  Extra items picked up before death are not
        // carried over (consistent with the C server's item-on-spawn semantics).
        pl.have = (
            PlayerAbility.SHIELD
                or PlayerAbility.COMPASS
                or PlayerAbility.REFUEL
                or PlayerAbility.REPAIR
                or PlayerAbility.CONNECTOR
                or PlayerAbility.SHOT
                or PlayerAbility.LASER
        )
        pl.used = (PlayerAbility.SHIELD or PlayerAbility.COMPASS)
    }

    /**
     * Remove the player associated with [sessionId] from the world.
     */
    fun despawnPlayer(sessionId: Int) {
        _players.remove(sessionId)
    }

    /**
     * Retrieve the [Player] for [sessionId], or null if not present.
     */
    fun playerForSession(sessionId: Int): Player? = _players[sessionId]

    // -----------------------------------------------------------------------
    // Frame tick
    // -----------------------------------------------------------------------

    /** Advance the frame counter (called once per game tick). */
    fun advanceFrame() {
        frameLoop++
    }

    // -----------------------------------------------------------------------
    // Keyboard input → player state
    // -----------------------------------------------------------------------

    /**
     * Decode a 9-byte keyboard bitmap and update [pl]'s key vectors.
     *
     * The bitmap encodes all [Key.NUM_KEYS] (72) key states as bits, with
     * key `k` at bit `k % 8` of byte `k / 8`.  Matches the C client's
     * `Client_encode_keys()` in client/keys.c.
     */
    fun applyKeyBitmap(
        pl: Player,
        keyBitmap: ByteArray,
    ) {
        pl.prevKeyv.copyFrom(pl.lastKeyv)
        for (k in 0 until Key.NUM_KEYS) {
            val byteIdx = k ushr 3
            val bitIdx = k and 7
            pl.lastKeyv[k] = byteIdx < keyBitmap.size &&
                ((keyBitmap[byteIdx].toInt() ushr bitIdx) and 1) != 0
        }
    }

    // -----------------------------------------------------------------------
    // Coordinate helpers
    // -----------------------------------------------------------------------

    companion object {
        /** Convert a direction integer (0..RES-1) to radians. */
        fun dirToRadians(dir: Int): Double = dir.toDouble() * 2.0 * GameConst.PI_VALUE / GameConst.RES

        /** Convert radians to a direction integer (0..RES-1). */
        fun radToDir(rad: Double): Short {
            val normalised = ((rad % (2.0 * GameConst.PI_VALUE)) + 2.0 * GameConst.PI_VALUE) % (2.0 * GameConst.PI_VALUE)
            return (normalised * GameConst.RES / (2.0 * GameConst.PI_VALUE))
                .toInt()
                .coerceIn(0, GameConst.RES - 1)
                .toShort()
        }
    }
}

/** Copy [src] BooleanArray into [this] (no allocation). */
private fun BooleanArray.copyFrom(src: BooleanArray) {
    val n = minOf(size, src.size)
    for (i in 0 until n) this[i] = src[i]
}
