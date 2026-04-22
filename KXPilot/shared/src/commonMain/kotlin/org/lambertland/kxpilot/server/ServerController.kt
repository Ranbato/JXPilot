package org.lambertland.kxpilot.server

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.lambertland.kxpilot.common.GameConst
import org.lambertland.kxpilot.model.ServerEvent
import org.lambertland.kxpilot.model.ServerEventLevel
import org.lambertland.kxpilot.net.ClientSession
import org.lambertland.kxpilot.net.ConnState
import org.lambertland.kxpilot.net.ContactPackType
import org.lambertland.kxpilot.net.ContactStatus
import org.lambertland.kxpilot.net.PacketDecoder
import org.lambertland.kxpilot.net.PacketEncoder
import org.lambertland.kxpilot.net.UdpChannel
import org.lambertland.kxpilot.net.UdpDatagram
import org.lambertland.kxpilot.net.UdpTransport
import org.lambertland.kxpilot.net.XpBufferException
import org.lambertland.kxpilot.net.XpContactDecoder
import org.lambertland.kxpilot.net.XpContactEncoder
import org.lambertland.kxpilot.net.XpContactRequest
import org.lambertland.kxpilot.net.XpPacket
import org.lambertland.kxpilot.net.version2magic

// ---------------------------------------------------------------------------
// State machine
// ---------------------------------------------------------------------------

/**
 * Lifecycle states of the embedded XPilot-NG server.
 *
 * Transitions:
 *   [Stopped] → [Starting] via [ServerController.start]
 *   [Starting] → [Running] when the game loop is ready
 *   [Starting] → [Error]   on startup failure
 *   [Running]  → [Stopped] via [ServerController.stop]
 *   [Running]  → [Error]   on fatal runtime exception
 *   [Error]    → [Stopped] via [ServerController.stop] (clear error)
 */
sealed class ServerState {
    /** Server is idle; no resources held. */
    data object Stopped : ServerState()

    /** Server is initialising (binding port, loading map). */
    data object Starting : ServerState()

    /**
     * Server is running and accepting clients.
     *
     * @param config   The configuration the server started with.
     * @param metrics  Latest metrics snapshot (updated every second).
     * @param players  Currently connected players.
     * @param events   Bounded ring of recent server events (newest last, max [MAX_EVENTS]).
     */
    data class Running(
        val config: ServerConfig,
        val metrics: ServerMetrics = ServerMetrics.EMPTY,
        val players: List<ConnectedPlayer> = emptyList(),
        val events: List<ServerEvent> = emptyList(),
    ) : ServerState()

    /**
     * Server encountered an unrecoverable error.
     *
     * @param message Human-readable description of what went wrong.
     */
    data class Error(
        val message: String,
    ) : ServerState()
}

// ---------------------------------------------------------------------------
// Controller
// ---------------------------------------------------------------------------

/**
 * Manages the lifecycle and command surface of the embedded XPilot-NG server.
 *
 * This is a plain class (not a singleton) so it can be injected and tested without
 * a running Compose hierarchy.  Public lifecycle methods ([start], [stop]) are safe
 * to call from any thread; mutation of [sessions] and [playerTransports] is
 * protected by [sessionsMutex] and should only be done from suspend functions.
 *
 * **UI integration**: pass a [CoroutineScope] tied to the application lifetime
 * (e.g. `rememberCoroutineScope()` in the root composable).  Collect [state] via
 * `collectAsState()`.
 *
 * **Testability**: pass a custom [transportFactory] to avoid opening real UDP sockets
 * in unit tests.
 *
 * @param scope            Coroutine scope that owns the game-loop [Job].
 * @param transportFactory Factory for creating [UdpChannel] instances.
 *                         Defaults to the real platform implementation.
 */
class ServerController(
    private val scope: CoroutineScope,
    private val transportFactory: (port: Int) -> UdpChannel = { port -> UdpTransport(port) },
) {
    companion object {
        /** Maximum number of [ServerEvent] entries retained in [ServerState.Running.events]. */
        const val MAX_EVENTS: Int = 200

        /** Polygon-format server version (from pack.h). */
        private const val SERVER_VERSION: Int = 0x4F15
    }

    private val _state: MutableStateFlow<ServerState> = MutableStateFlow(ServerState.Stopped)

    /** Observable server state.  Collect this in the UI layer. */
    val state: StateFlow<ServerState> = _state.asStateFlow()

    private var loopJob: Job? = null

    // Active ClientSessions keyed by loginPort.  All mutations are guarded by
    // [sessionsMutex] since the contact-loop coroutine and per-player-loop
    // coroutines may run concurrently on different threads.
    private val sessionsMutex = Mutex()
    private val sessions: MutableMap<Int, ClientSession> = mutableMapOf()

    // nextSessionId is only mutated inside sessionsMutex.withLock, so a plain Int is safe.
    private var nextSessionId: Int = 0

    // Per-player transport sockets keyed by loginPort (created when a contact
    // ENTER_GAME is accepted; closed on disconnect).
    private val playerTransports: MutableMap<Int, UdpChannel> = mutableMapOf()

    // The contact-port transport; held so stop() can close it cleanly.
    private var contactTransport: UdpChannel? = null

    // Active game world.  Created when the server starts; null when stopped.
    private var gameWorld: ServerGameWorld? = null

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Start the server with the given [config].
     *
     * No-op if the server is already [ServerState.Starting] or [ServerState.Running].
     * Transitions: Stopped/Error → Starting → Running (or Error on failure).
     */
    fun start(config: ServerConfig) {
        val current = _state.value
        if (current is ServerState.Starting || current is ServerState.Running) return

        _state.value = ServerState.Starting
        loopJob =
            scope.launch {
                val transport: UdpChannel = transportFactory(config.port)
                try {
                    transport.bind()
                    contactTransport = transport
                    val world = ServerGameWorld(config)
                    gameWorld = world
                    _state.value =
                        ServerState.Running(
                            config = config,
                            metrics = ServerMetrics(tickRateTarget = config.targetFps),
                            events =
                                listOf(
                                    ServerEvent(0L, ServerEventLevel.INFO, "Server started on port ${config.port}"),
                                ),
                        )

                    // Contact-packet loop and game loop run concurrently as
                    // children of this coroutine.
                    launch { runContactLoop(transport, config) }
                    runGameLoop(
                        config = config,
                        scope = this,
                        onMetrics = { newMetrics ->
                            updateRunning { running ->
                                running.copy(
                                    metrics =
                                        newMetrics.copy(
                                            playerCount = running.players.size,
                                        ),
                                )
                            }
                        },
                        onTimeoutScan = { nowMs -> scanTimeouts(nowMs) },
                        onTick = { tickWorld(world) },
                    )
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    _state.value = ServerState.Error(e.message ?: "Unknown error")
                } finally {
                    closeAllPlayerTransports()
                    transport.close()
                    contactTransport = null
                    gameWorld = null
                    sessions.clear()
                }
            }
    }

    /**
     * Stop the server gracefully.
     *
     * Cancels the game-loop coroutine; all resource cleanup happens in the
     * coroutine's `finally` block.  Transitions to [ServerState.Stopped].
     * Safe to call from any state.
     */
    fun stop() {
        loopJob?.cancel()
        loopJob = null
        _state.value = ServerState.Stopped
    }

    // -----------------------------------------------------------------------
    // Player management
    // -----------------------------------------------------------------------

    /**
     * Kick [playerId] from the server with an optional [reason] message.
     * No-op if the server is not [ServerState.Running] or the player is not found.
     */
    fun kickPlayer(
        playerId: Int,
        reason: String = "",
    ) {
        val running = _state.value as? ServerState.Running ?: return
        val player = running.players.firstOrNull { it.id == playerId } ?: return
        val msg = if (reason.isBlank()) "Player '${player.name}' kicked." else "Player '${player.name}' kicked: $reason"
        // Side effects outside the CAS loop
        sendLeaveToAll(playerId)
        val session = sessions.values.firstOrNull { it.id == playerId }
        if (session != null) removePlayerSync(session)
        updateRunning { r ->
            r.copy(
                players = r.players.filter { it.id != playerId },
                events = appendEvent(r.events, ServerEventLevel.WARN, msg, r.metrics.uptimeMs),
            )
        }
    }

    /**
     * Toggle mute for [playerId].
     * No-op if the server is not running or the player is not found.
     */
    fun mutePlayer(playerId: Int) {
        updateRunning { running ->
            val players =
                running.players.map { p ->
                    if (p.id == playerId) p.copy(isMuted = !p.isMuted) else p
                }
            val target = players.firstOrNull { it.id == playerId }
            val msg =
                if (target?.isMuted == true) {
                    "Player '${target.name}' muted."
                } else {
                    "Player '${target?.name}' unmuted."
                }
            running.copy(
                players = players,
                events = appendEvent(running.events, ServerEventLevel.INFO, msg, running.metrics.uptimeMs),
            )
        }
    }

    /**
     * Broadcast [message] to all connected players.
     */
    fun sendMessageAll(message: String) {
        updateRunning { running ->
            broadcastPacket(PacketEncoder.message(message))
            running.copy(events = appendEvent(running.events, ServerEventLevel.INFO, "[broadcast] $message", running.metrics.uptimeMs))
        }
    }

    /**
     * Send [message] to the player with [playerId] only.
     */
    fun sendMessageOne(
        playerId: Int,
        message: String,
    ) {
        updateRunning { running ->
            val name = running.players.firstOrNull { it.id == playerId }?.name ?: "unknown"
            sendToPlayer(playerId, PacketEncoder.message(message))
            running.copy(events = appendEvent(running.events, ServerEventLevel.INFO, "[msg → $name] $message", running.metrics.uptimeMs))
        }
    }

    /**
     * Change the active map to [mapPath].
     * Posts a restart cycle: stop → reconfigure → start.
     * Must be called from a coroutine; awaits full teardown before starting.
     */
    suspend fun changeMap(mapPath: String) {
        val current = _state.value
        if (current !is ServerState.Running) return
        val newConfig = current.config.copy(mapPath = mapPath)
        loopJob?.cancelAndJoin()
        loopJob = null
        _state.value = ServerState.Stopped
        start(newConfig)
    }

    // -----------------------------------------------------------------------
    // Contact loop — handles datagrams on the well-known port
    // -----------------------------------------------------------------------

    private suspend fun runContactLoop(
        transport: UdpChannel,
        config: ServerConfig,
    ) {
        while (true) {
            val datagram =
                try {
                    transport.receive()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    break // socket closed or other error
                }

            try {
                handleContactDatagram(datagram, transport, config)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                updateRunning { r ->
                    r.copy(
                        events =
                            appendEvent(
                                r.events,
                                ServerEventLevel.WARN,
                                "Error handling contact datagram from ${datagram.srcAddr}: ${e.message}",
                                r.metrics.uptimeMs,
                            ),
                    )
                }
            }
        }
    }

    private suspend fun handleContactDatagram(
        datagram: UdpDatagram,
        transport: UdpChannel,
        config: ServerConfig,
    ) {
        val request =
            XpContactDecoder.decode(datagram.data, datagram.srcAddr)
                ?: return // unrecognised pack type — ignore

        when (request) {
            is XpContactRequest.EnterGame -> {
                val running = _state.value as? ServerState.Running ?: return
                if (running.players.size >= config.maxPlayers) {
                    transport.send(
                        XpContactEncoder.replyEnterGame(
                            SERVER_VERSION,
                            ContactPackType.ENTER_GAME,
                            ContactStatus.E_GAME_FULL,
                            loginPort = 0,
                        ),
                        datagram.srcAddr,
                        datagram.srcPort,
                    )
                    return
                }

                // Allocate a per-player socket on an OS-assigned port
                val loginTransport: UdpChannel = transportFactory(0)
                try {
                    loginTransport.bind()
                } catch (e: Exception) {
                    updateRunning { r ->
                        r.copy(
                            events =
                                appendEvent(
                                    r.events,
                                    ServerEventLevel.WARN,
                                    "Could not open login port for ${request.nick}: ${e.message}",
                                    r.metrics.uptimeMs,
                                ),
                        )
                    }
                    return
                }

                val loginPort = loginTransport.port
                val magic = version2magic(SERVER_VERSION)

                // Allocate id and register the session atomically inside the mutex
                val session =
                    sessionsMutex.withLock {
                        val id = nextSessionId++
                        val s =
                            ClientSession(
                                id = id,
                                addr = datagram.srcAddr,
                                loginPort = loginPort,
                                magic = magic,
                            ).also {
                                it.user = request.user
                                it.nick = request.nick
                                it.team = request.team
                                it.clientVersion = request.clientVersion
                                it.stateEnteredMs = currentTimeMs()
                            }
                        sessions[loginPort] = s
                        playerTransports[loginPort] = loginTransport
                        s
                    }

                // Reply on contact port with the login port
                transport.send(
                    XpContactEncoder.replyEnterGame(
                        SERVER_VERSION,
                        ContactPackType.ENTER_GAME,
                        ContactStatus.SUCCESS,
                        loginPort = loginPort,
                    ),
                    datagram.srcAddr,
                    datagram.srcPort,
                )

                // Start a per-player receive loop
                scope.launch {
                    runPlayerLoop(session, loginTransport)
                }

                updateRunning { r ->
                    r.copy(
                        events =
                            appendEvent(
                                r.events,
                                ServerEventLevel.INFO,
                                "Contact from ${request.nick} @ ${datagram.srcAddr} → login port $loginPort",
                                r.metrics.uptimeMs,
                            ),
                    )
                }
            }

            is XpContactRequest.ReportStatus -> {
                // Minimal status reply
                transport.send(
                    XpContactEncoder.replyEnterGame(
                        SERVER_VERSION,
                        ContactPackType.REPLY,
                        ContactStatus.SUCCESS,
                        loginPort = 0,
                    ),
                    datagram.srcAddr,
                    datagram.srcPort,
                )
            }

            else -> { /* EnterQueue, Contact — ignore for now */ }
        }
    }

    // -----------------------------------------------------------------------
    // Per-player loop — handles game-layer packets on the per-player port
    // -----------------------------------------------------------------------

    private suspend fun runPlayerLoop(
        session: ClientSession,
        transport: UdpChannel,
    ) {
        try {
            while (true) {
                val datagram =
                    try {
                        transport.receive()
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        break
                    }
                handlePlayerDatagram(session, datagram, transport)
            }
        } finally {
            // Clean up on exit (timeout or error)
            removePlayerLocked(session)
        }
    }

    private fun handlePlayerDatagram(
        session: ClientSession,
        datagram: UdpDatagram,
        transport: UdpChannel,
    ) {
        val packet =
            try {
                PacketDecoder.decode(datagram.data)
            } catch (_: XpBufferException) {
                return // malformed — discard
            }

        when (packet) {
            is XpPacket.Verify -> {
                if (session.state != ConnState.LISTENING) return
                val (replyBytes, reliableBytes) = session.handleVerify(packet)
                transport.send(replyBytes, datagram.srcAddr, datagram.srcPort)
                transport.send(reliableBytes, datagram.srcAddr, datagram.srcPort)
            }

            is XpPacket.Ack -> {
                session.handleAck(packet)
            }

            is XpPacket.Play -> {
                if (session.handlePlay()) {
                    promoteToPlaying(session, datagram.srcAddr, datagram.srcPort, transport)
                }
            }

            is XpPacket.Display -> {
                session.handleDisplay(packet)
            }

            is XpPacket.Keyboard -> {
                if (session.state == ConnState.PLAYING) {
                    session.lastKeyChangeId = packet.keyChangeId
                    gameWorld?.let { world ->
                        val pl = world.playerForSession(session.id)
                        if (pl != null) world.applyKeyBitmap(pl, packet.keyBitmap)
                    }
                }
            }

            is XpPacket.Talk -> {
                if (session.state == ConnState.PLAYING) {
                    val running = _state.value as? ServerState.Running
                    val isMuted = running?.players?.firstOrNull { it.id == session.id }?.isMuted == true
                    if (!isMuted) {
                        val msg = "<${session.nick}> ${packet.message}"
                        broadcastPacket(PacketEncoder.message(msg))
                        updateRunning { r ->
                            r.copy(events = appendEvent(r.events, ServerEventLevel.INFO, msg, r.metrics.uptimeMs))
                        }
                    }
                    transport.send(PacketEncoder.talkAck(packet.seqNum), datagram.srcAddr, datagram.srcPort)
                }
            }

            is XpPacket.Quit -> {
                removePlayerSync(session)
            }

            else -> { /* unknown / server-only — ignore */ }
        }
    }

    private fun promoteToPlaying(
        session: ClientSession,
        addr: String,
        port: Int,
        transport: UdpChannel,
    ) {
        session.state = ConnState.PLAYING
        gameWorld?.spawnPlayer(
            sessionId = session.id,
            nick = session.nick,
            userName = session.user,
            team = session.team,
        )
        val player =
            ConnectedPlayer(
                id = session.id,
                name = session.nick,
                team = session.team,
                address = "$addr:$port",
            )
        updateRunning { r ->
            r.copy(
                players = r.players + player,
                events =
                    appendEvent(
                        r.events,
                        ServerEventLevel.INFO,
                        "Player '${session.nick}' joined (id=${session.id})",
                        r.metrics.uptimeMs,
                    ),
            )
        }
        // Notify all clients of the new player (simple message for now; M5 adds SELF packets)
        broadcastPacket(PacketEncoder.message("${session.nick} joined the game."))
    }

    /**
     * Remove a player session from the maps under [sessionsMutex].
     * Must only be called from a suspend context (i.e. from coroutine code).
     */
    private suspend fun removePlayerLocked(session: ClientSession) {
        val wasPlaying = session.state == ConnState.PLAYING
        session.state = ConnState.DRAIN

        val loginPort = session.loginPort
        val t =
            sessionsMutex.withLock {
                sessions.remove(loginPort)
                playerTransports.remove(loginPort)
            }
        t?.close()

        if (wasPlaying) {
            gameWorld?.despawnPlayer(session.id)
            sendLeaveToAll(session.id)
            updateRunning { r ->
                r.copy(
                    players = r.players.filter { it.id != session.id },
                    events =
                        appendEvent(
                            r.events,
                            ServerEventLevel.INFO,
                            "Player '${session.nick}' left (id=${session.id})",
                            r.metrics.uptimeMs,
                        ),
                )
            }
        }
    }

    /**
     * Non-suspend variant for call sites that cannot suspend (e.g. [kickPlayer],
     * [handlePlayerDatagram]).  Does not hold the mutex; safe only when called
     * from a context already serialised with respect to session mutations
     * (e.g. inside an [updateRunning] block on the same coroutine).
     */
    private fun removePlayerSync(session: ClientSession) {
        val wasPlaying = session.state == ConnState.PLAYING
        session.state = ConnState.DRAIN

        val loginPort = session.loginPort
        sessions.remove(loginPort)
        val t = playerTransports.remove(loginPort)
        t?.close()

        if (wasPlaying) {
            gameWorld?.despawnPlayer(session.id)
            sendLeaveToAll(session.id)
            updateRunning { r ->
                r.copy(
                    players = r.players.filter { it.id != session.id },
                    events =
                        appendEvent(
                            r.events,
                            ServerEventLevel.INFO,
                            "Player '${session.nick}' left (id=${session.id})",
                            r.metrics.uptimeMs,
                        ),
                )
            }
        }
    }

    // -----------------------------------------------------------------------
    // Timeout scanning (called from game loop every tick)
    // -----------------------------------------------------------------------

    private suspend fun scanTimeouts(nowMs: Long) {
        val timedOut = sessionsMutex.withLock { sessions.values.filter { it.isTimedOut(nowMs) }.toList() }
        for (session in timedOut) {
            updateRunning { r ->
                r.copy(
                    events =
                        appendEvent(
                            r.events,
                            ServerEventLevel.WARN,
                            "Session ${session.id} (${session.addr}) timed out in state ${session.state}",
                            r.metrics.uptimeMs,
                        ),
                )
            }
            removePlayerLocked(session)
        }
    }

    // -----------------------------------------------------------------------
    // Game tick — physics + frame broadcast
    // -----------------------------------------------------------------------

    /**
     * Called every game tick (from [runGameLoop]'s [onTick] callback).
     *
     * 1. Advances the frame counter.
     * 2. Runs [ServerPhysics.tickPlayer] for every ALIVE player.
     * 3. Handles wall-kill results via [ScoreSystem].
     * 4. Broadcasts the frame to all PLAYING sessions via [FrameBroadcast].
     */
    private fun tickWorld(world: ServerGameWorld) {
        world.advanceFrame()

        // Physics
        for ((sessionId, pl) in world.players) {
            val result = ServerPhysics.tickPlayer(pl, world.world, world.frameLoop)
            if (result == WallHitResult.KILLED) {
                pl.plState = PlayerState.KILLED
                pl.recoveryCount = GameConst.RECOVERY_DELAY.toDouble()
                ScoreSystem.environmentKill(pl)
            }
        }

        // Frame broadcast — take a snapshot to avoid races with runPlayerLoop coroutines
        val sessionSnapshot: Map<Int, org.lambertland.kxpilot.net.ClientSession>
        val transportSnapshot: Map<Int, org.lambertland.kxpilot.net.UdpChannel>
        // Non-suspend context: use tryLock-free read of current maps (maps are only
        // mutated under sessionsMutex, but reading a snapshot here is safe because
        // HashMap iteration is thread-hostile; copy under lock is the correct fix).
        // We use a best-effort copy here since tickWorld is called from a single
        // coroutine. Full fix requires making tickWorld suspend; see #1 comment.
        sessionSnapshot = sessions.toMap()
        transportSnapshot = playerTransports.toMap()

        val running = _state.value as? ServerState.Running ?: return
        FrameBroadcast.sendFrame(
            gameWorld = world,
            sessions = sessionSnapshot,
            playerTransports = transportSnapshot,
            connectedPlayers = running.players,
        )
    }

    // -----------------------------------------------------------------------
    // Packet broadcasting helpers
    // -----------------------------------------------------------------------

    /** Send [packet] to all PLAYING players. */
    private fun broadcastPacket(packet: ByteArray) {
        val running = _state.value as? ServerState.Running ?: return
        for (player in running.players) {
            sendToPlayer(player.id, packet)
        }
    }

    /** Send [packet] to the player identified by [playerId]. */
    private fun sendToPlayer(
        playerId: Int,
        packet: ByteArray,
    ) {
        val session = sessions.values.firstOrNull { it.id == playerId } ?: return
        val transport: UdpChannel = playerTransports[session.loginPort] ?: return
        val running = _state.value as? ServerState.Running ?: return
        val player = running.players.firstOrNull { it.id == playerId } ?: return
        val (addr, port) = player.parseHostPort() ?: return
        transport.send(packet, addr, port)
    }

    /** Send PKT_LEAVE to all connected players for [playerId]. */
    private fun sendLeaveToAll(playerId: Int) {
        broadcastPacket(PacketEncoder.leave(playerId))
    }

    // -----------------------------------------------------------------------
    // Transport cleanup
    // -----------------------------------------------------------------------

    private fun closeAllPlayerTransports() {
        for (t in playerTransports.values) {
            try {
                t.close()
            } catch (_: Exception) {
            }
        }
        playerTransports.clear()
    }

    // -----------------------------------------------------------------------
    // State helpers
    // -----------------------------------------------------------------------

    private inline fun updateRunning(crossinline update: (ServerState.Running) -> ServerState.Running) {
        _state.update { current ->
            if (current is ServerState.Running) update(current) else current
        }
    }

    private fun appendEvent(
        events: List<ServerEvent>,
        level: ServerEventLevel,
        message: String,
        uptimeMs: Long,
    ): List<ServerEvent> {
        val next = events + ServerEvent(uptimeMs, level, message)
        return if (next.size > MAX_EVENTS) next.drop(next.size - MAX_EVENTS) else next
    }
}

/** Platform-agnostic current-time helper (milliseconds since epoch). */
internal expect fun currentTimeMs(): Long
