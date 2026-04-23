package org.lambertland.kxpilot.server

import org.lambertland.kxpilot.common.toPixel
import org.lambertland.kxpilot.net.ClientSession
import org.lambertland.kxpilot.net.ConnState
import org.lambertland.kxpilot.net.PacketEncoder
import org.lambertland.kxpilot.net.UdpChannel
import org.lambertland.kxpilot.server.Modifier

// ---------------------------------------------------------------------------
// FrameBroadcast
// ---------------------------------------------------------------------------
//
// Sends one complete game-frame update to every PLAYING session.
//
// Per-client frame sequence (mirrors C netserver.c Send_self + Score_update):
//   PKT_START  — frame begin, echoes lastKeyChange so client knows input lag
//   PKT_SELF   — ship state for the *owning* client only
//   PKT_SCORE  — one packet per player (all players, every frame)
//   PKT_END    — frame complete
//
// Called once per game tick from [ServerGameLoop] after physics has run.
// All access is single-threaded with respect to [ServerGameWorld] (no mutex
// needed here — the game loop is the sole writer during a tick).

/**
 * Sends per-frame game packets to all PLAYING sessions.
 *
 * @param gameWorld       The live game world (frame counter + player objects).
 * @param sessions        All active [org.lambertland.kxpilot.net.ClientSession]s,
 *                        keyed by loginPort.
 * @param playerTransports Per-player [UdpChannel]s, keyed by loginPort.
 * @param connectedPlayers The UI-facing list of [ConnectedPlayer]s (for address lookup).
 */
object FrameBroadcast {
    /**
     * Send one game frame to every PLAYING client.
     *
     * @param gameWorld        Live world state.
     * @param sessions         Active sessions (keyed by loginPort).
     * @param playerTransports Per-player channels (keyed by loginPort).
     * @param connectedPlayers List of [ConnectedPlayer]s for address resolution.
     */
    fun sendFrame(
        gameWorld: ServerGameWorld,
        sessions: Map<Int, ClientSession>,
        playerTransports: Map<Int, UdpChannel>,
        connectedPlayers: List<ConnectedPlayer>,
    ) {
        val frameLoop = gameWorld.frameLoop.toInt()
        val allPlayers = gameWorld.players // Map<sessionId, Player>

        // Pre-build PKT_SCORE for every live player (same for all recipients).
        // Use the map key directly instead of O(n) search per player (#7a).
        val scorePackets: List<ByteArray> =
            allPlayers.entries.map { (sessionId, pl) ->
                PacketEncoder.score(
                    id = sessionId,
                    score = pl.score,
                    lives = pl.deaths,
                    myChar = pl.myChar,
                )
            }

        // Pre-build a Map<playerId, ConnectedPlayer> to avoid O(n) lookup per session (#7b).
        val cpById: Map<Int, ConnectedPlayer> = connectedPlayers.associateBy { it.id }

        for ((loginPort, session) in sessions) {
            if (session.state != ConnState.PLAYING) continue

            val transport: UdpChannel = playerTransports[loginPort] ?: continue
            val cp = cpById[session.id] ?: continue
            val (addr, port) = cp.parseHostPort() ?: continue

            // PKT_START
            transport.send(
                PacketEncoder.start(frameLoop, session.lastKeyChangeId),
                addr,
                port,
            )

            // PKT_SELF — for ALIVE, KILLED, and APPEARING players.
            // KILLED/APPEARING players need PKT_SELF so the client can display
            // the respawn countdown; the status byte encodes the dead/respawn state.
            val pl = gameWorld.playerForSession(session.id)
            if (pl != null && (pl.isAlive() || pl.isKilled() || pl.isAppearing())) {
                val posX = pl.pos.cx.toPixel()
                val posY = pl.pos.cy.toPixel()
                val velX = pl.vel.x.toInt()
                val velY = pl.vel.y.toInt()
                transport.send(
                    PacketEncoder.self(
                        posX = posX,
                        posY = posY,
                        velX = velX,
                        velY = velY,
                        dir = pl.dir.toInt() and 0xFF,
                        power = pl.power.toInt(),
                        turnspeed = pl.turnspeed.toInt(),
                        turnresistance = (pl.turnresistance * 255).toInt().coerceIn(0, 255),
                        fuelSum = pl.fuel.sum.toInt(),
                        fuelMax = pl.fuel.max.toInt(),
                        status = pl.objStatus.toInt() and 0xFF,
                    ),
                    addr,
                    port,
                )
                // PKT_SELF_ITEMS — item counts (I21)
                transport.send(PacketEncoder.selfItems(pl.item), addr, port)
                // PKT_MODIFIERS — active modifier bank (I21)
                val mods = pl.modbank[0]
                transport.send(
                    PacketEncoder.modifiers(
                        mini = mods.get(Modifier.Mini),
                        nuclear = mods.get(Modifier.Nuclear),
                        cluster = mods.get(Modifier.Cluster),
                        implosion = mods.get(Modifier.Implosion),
                        velocity = mods.get(Modifier.Velocity),
                        spread = mods.get(Modifier.Spread),
                        front = 0, // no Front modifier in current Modifier enum
                        laser = mods.get(Modifier.Laser),
                        target = 0, // no Target modifier in current Modifier enum
                        itempf = 0,
                    ),
                    addr,
                    port,
                )
            }

            // PKT_SCORE — all players
            for (pkt in scorePackets) {
                transport.send(pkt, addr, port)
            }

            // PKT_END
            transport.send(PacketEncoder.end(frameLoop), addr, port)
        }
    }
}
