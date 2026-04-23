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
import org.lambertland.kxpilot.AppInfo
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

        /** Polygon-format server protocol version — see [AppInfo.PROTOCOL_VERSION]. */
        private val SERVER_VERSION: Int = AppInfo.PROTOCOL_VERSION
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

    // Secondary index: sessionId → ClientSession.  Maintained in parallel with
    // [sessions] (keyed by loginPort) to allow O(1) lookups by id (#15).
    // All mutations guarded by [sessionsMutex].
    private val sessionById: MutableMap<Int, ClientSession> = mutableMapOf()

    // nextSessionId is only mutated inside sessionsMutex.withLock, so a plain Int is safe.
    private var nextSessionId: Int = 0

    // Per-player transport sockets keyed by loginPort (created when a contact
    // ENTER_GAME is accepted; closed on disconnect).
    private val playerTransports: MutableMap<Int, UdpChannel> = mutableMapOf()

    // The contact-port transport; held so stop() can close it cleanly.
    private var contactTransport: UdpChannel? = null

    // Active game world.  Created when the server starts; null when stopped.
    private var gameWorld: ServerGameWorld? = null

    // Pre-allocated map reused each tick for shot-cooldown counting (avoids per-tick allocation).
    // Only ever accessed on the game-loop coroutine — no lock needed.
    private val liveShotCounts: HashMap<Int, Int> = HashMap(64)

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

        // Guard against platforms where UDP server is unsupported (I22).
        if (!isServerSupported()) {
            _state.value = ServerState.Error("Embedded UDP server is not supported on this platform.")
            return
        }

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
                    closeAllPlayerTransports() // clears sessions, sessionById, playerTransports
                    transport.close()
                    contactTransport = null
                    gameWorld = null
                    // Transition to Stopped only if we're still Running (not Error).
                    // Doing this here (not in stop()) guarantees all teardown is
                    // complete before the state is observable as Stopped.
                    _state.update { if (it is ServerState.Running) ServerState.Stopped else it }
                }
            }
    }

    /**
     * Stop the server gracefully.
     *
     * Cancels the game-loop coroutine; all resource cleanup and state transition
     * to [ServerState.Stopped] happens in the coroutine's `finally` block, so
     * resources are guaranteed torn down before the state is observable as Stopped.
     * Safe to call from any state.
     */
    fun stop() {
        loopJob?.cancel()
        loopJob = null
    }

    // -----------------------------------------------------------------------
    // Player management
    // -----------------------------------------------------------------------

    /**
     * Kick [playerId] from the server with an optional [reason] message.
     * No-op if the server is not [ServerState.Running] or the player is not found.
     *
     * Closes the player's transport socket, which interrupts their [runPlayerLoop]
     * receive; the loop's `finally` block calls [removePlayerLocked] for proper
     * cleanup.  The [ServerState.Running.players] list and event log are updated
     * immediately for UI responsiveness.
     */
    suspend fun kickPlayer(
        playerId: Int,
        reason: String = "",
    ) {
        val running = _state.value as? ServerState.Running ?: return
        val player = running.players.firstOrNull { it.id == playerId } ?: return
        val msg = if (reason.isBlank()) "Player '${player.name}' kicked." else "Player '${player.name}' kicked: $reason"
        sendLeaveToAll(playerId)
        // Snapshot the session and transport under the mutex, then close outside the lock.
        val transportToClose =
            sessionsMutex.withLock {
                val session = sessionById[playerId]
                if (session != null) {
                    session.state = ConnState.DRAIN
                    playerTransports[session.loginPort]
                } else {
                    null
                }
            }
        try {
            transportToClose?.close()
        } catch (_: Exception) {
        }
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
     * No-op if the server is not [ServerState.Running].
     */
    suspend fun sendMessageAll(message: String) {
        if (_state.value !is ServerState.Running) return
        broadcastPacket(PacketEncoder.message(message))
        updateRunning { running ->
            running.copy(events = appendEvent(running.events, ServerEventLevel.INFO, "[broadcast] $message", running.metrics.uptimeMs))
        }
    }

    /**
     * Send [message] to the player with [playerId] only.
     * No-op if the server is not [ServerState.Running] or the player is not connected.
     */
    suspend fun sendMessageOne(
        playerId: Int,
        message: String,
    ) {
        val running = _state.value as? ServerState.Running ?: return
        val name = running.players.firstOrNull { it.id == playerId }?.name ?: return
        sendToPlayer(playerId, PacketEncoder.message(message))
        updateRunning { r ->
            r.copy(events = appendEvent(r.events, ServerEventLevel.INFO, "[msg → $name] $message", r.metrics.uptimeMs))
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
        // Do NOT set _state.value = Stopped here: the coroutine's finally block
        // already transitions Running → Stopped once all teardown is complete.
        // Setting it here would race with the finally block and could discard an
        // Error state, or briefly expose Stopped before resources are released.
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
                        sessionById[s.id] = s
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
                // N2/S2: reply includes server name and current player list.
                val running = _state.value as? ServerState.Running
                val playerNames = running?.players?.map { it.name } ?: emptyList()
                val serverName = running?.config?.serverName ?: config.serverName
                transport.send(
                    XpContactEncoder.replyStatus(
                        serverVersion = SERVER_VERSION,
                        serverName = serverName,
                        playerCount = playerNames.size,
                        playerNames = playerNames,
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

    private suspend fun handlePlayerDatagram(
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
                session.handleAck(packet, currentTimeMs())
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
                removePlayerLocked(session)
            }

            is XpPacket.MotdRequest -> {
                // N3: send the server's welcome message as a single PKT_MOTD chunk.
                val running = _state.value as? ServerState.Running
                val motdText = running?.config?.welcomeMessage ?: ""
                transport.send(PacketEncoder.motd(packet.offset, motdText), datagram.srcAddr, datagram.srcPort)
            }

            else -> { /* unknown / server-only — ignore */ }
        }
    }

    private suspend fun promoteToPlaying(
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
        // Announce the new player to all existing clients (I11: PKT_PLAYER broadcast).
        broadcastPacket(
            PacketEncoder.player(
                id = session.id,
                team = session.team,
                myChar = ' ',
                nick = session.nick,
            ),
        )
        // Also send each already-connected player to the new joiner so they know
        // who else is in the game.  Re-read the player list under the current state
        // so we include the newly added player in the announcements if needed.
        val alreadyConnected = (_state.value as? ServerState.Running)?.players ?: emptyList()
        for (existing in alreadyConnected) {
            if (existing.id == session.id) continue
            sendToPlayer(
                session.id,
                PacketEncoder.player(
                    id = existing.id,
                    team = existing.team,
                    myChar = ' ',
                    nick = existing.name,
                ),
            )
        }
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
        val removedTransport: UdpChannel? =
            sessionsMutex.withLock {
                sessions.remove(loginPort)
                sessionById.remove(session.id)
                playerTransports.remove(loginPort)
            }
        removedTransport?.close()

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
     * 2. Fires shots for players holding KEY_FIRE_SHOT.
     * 3. Runs [ServerPhysics.tickPlayer] for every ALIVE player.
     * 4. Handles wall-kill results via [ScoreSystem].
     * 4b. Detects player-player ship collisions and awards kill credit.
     * 4c. Ticks all live shots: movement, wall collision, ship collision.
     * 4d. Awards kill credit for shot kills via [ScoreSystem].
     * 4e. Advances the KILLED→APPEARING→ALIVE recovery state machine.
     *     (Runs AFTER all kill-detection steps so KILLED is visible for one tick
     *      regardless of kill source — wall, ship collision, or shot.)
     * 5. Syncs per-player shot cooldowns.
     * 6. Broadcasts the frame to all PLAYING sessions via [FrameBroadcast].
     */
    private suspend fun tickWorld(world: ServerGameWorld) {
        world.advanceFrame()

        // 2. Shot firing — must happen before player physics so shotTime starts
        //    decrementing from this tick rather than the next.
        for ((sessionId, pl) in world.players) {
            ServerPhysics.tryFireShot(pl, world.pools, sessionId)
        }

        // 2b. Depot proximity — sets REFUEL bit before tickPlayer calls useItems
        ServerPhysics.tickDepotProximity(world.players, world.world.fuels)

        // 3 & 4. Player physics + wall kills
        for ((_, pl) in world.players) {
            val result = ServerPhysics.tickPlayer(pl, world.world, world.frameLoop)
            if (result == WallHitResult.KILLED) {
                pl.plState = PlayerState.KILLED
                pl.recoveryCount = GameConst.RECOVERY_DELAY.toDouble()
                // Use wallDeath to give credit to the last player who shoved
                // the victim (I5).  lastWallAttacker is set by tickPlayerCollisions
                // in the same tick or a prior tick.
                val attackerSession = pl.physics.lastWallAttacker
                val attacker = if (attackerSession != null) world.players[attackerSession] else null
                ScoreSystem.wallDeath(pl, attacker)
                pl.physics.lastWallAttacker = null
            }
        }

        // 4b. Player-player ship collisions
        val shipCollisions = ServerPhysics.tickPlayerCollisions(world.players)
        for (event in shipCollisions) {
            val victim = world.players[event.victimSessionId]
            val killer = world.players[event.killerSessionId]
            if (victim != null && killer != null) {
                ScoreSystem.playerKill(victim, killer)
            } else if (victim != null) {
                ScoreSystem.environmentKill(victim)
            }
        }

        // 4c. Item pickup
        ServerPhysics.tickItemPickup(world.players, world.pools, world.world)

        // 4d. Cannon AI — dead-tick countdown + fire at players in range
        ServerPhysics.tickCannons(world.world.cannons, world.players, world.pools)

        // 4e. Shot movement + ship collision + kill credit
        val shotKills = ServerPhysics.tickShots(world.pools, world.world, world.players)
        for (kill in shotKills) {
            val victim = world.players[kill.victimSessionId]
            val killer = world.players[kill.killerSessionId]
            if (victim != null && killer != null) {
                ScoreSystem.playerKill(victim, killer)
            } else if (victim != null) {
                // killerSessionId < 0 (e.g. CANNON_SHOT_ID) → environment kill
                ScoreSystem.environmentKill(victim)
            }
        }

        // 4f. Recovery/respawn state machine (KILLED→APPEARING→ALIVE).
        //     Runs LAST so every kill source (wall, ship, shot) has symmetric
        //     one-tick visibility of KILLED before transitioning to APPEARING.
        for ((_, pl) in world.players) {
            ServerPhysics.tickRecovery(pl, world)
        }

        // 5. Shot cooldown — single O(S) pass to build id→count, then O(P) lookup
        liveShotCounts.clear()
        world.pools.shots.forEach { shot ->
            val id = shot.id.toInt()
            liveShotCounts[id] = (liveShotCounts[id] ?: 0) + 1
        }
        for ((sessionId, pl) in world.players) {
            ServerPhysics.tickShotCooldown(pl, liveShotCounts[sessionId] ?: 0)
        }

        // 5b. Sync live Player.score and RTT back into ServerState.Running.players so
        //     the UI score column and ping column reflect current values (I6, N4).
        if (world.players.isNotEmpty()) {
            updateRunning { r ->
                r.copy(
                    players =
                        r.players.map { cp ->
                            val pl = world.players[cp.id]
                            val session = sessionById[cp.id]
                            val newScore = if (pl != null && pl.score.toInt() != cp.score) pl.score.toInt() else cp.score
                            val newPing = session?.rttMs() ?: cp.pingMs
                            if (newScore != cp.score || newPing != cp.pingMs) {
                                cp.copy(score = newScore, pingMs = newPing)
                            } else {
                                cp
                            }
                        },
                )
            }
        }

        // 6. Frame broadcast — take immutable snapshots under mutex to avoid
        //    races with contact-loop or stop() mutating the live maps.
        val (sessionSnapshot, transportSnapshot) =
            sessionsMutex.withLock {
                sessions.toMap() to playerTransports.toMap()
            }

        val running = _state.value as? ServerState.Running ?: return
        FrameBroadcast.sendFrame(
            gameWorld = world,
            sessions = sessionSnapshot,
            playerTransports = transportSnapshot,
            connectedPlayers = running.players,
        )

        // S1. Reliable retransmit — resend any un-acked reliable data that has timed out.
        checkRetransmit(currentTimeMs(), sessionSnapshot, transportSnapshot, running)
    }

    /**
     * Iterate all sessions and retransmit any un-acked reliable data whose
     * retransmit timeout has elapsed.
     */
    private suspend fun checkRetransmit(
        nowMs: Long,
        sessionSnapshot: Map<Int, ClientSession>,
        transportSnapshot: Map<Int, UdpChannel>,
        running: ServerState.Running,
    ) {
        for ((loginPort, session) in sessionSnapshot) {
            if (!session.shouldRetransmit(nowMs)) continue
            val packet = session.buildReliablePacket() ?: continue
            val transport = transportSnapshot[loginPort] ?: continue
            val player = running.players.firstOrNull { it.id == session.id } ?: continue
            val (addr, port) = player.parseHostPort() ?: continue
            transport.send(packet, addr, port)
            session.recordReliableSent(nowMs)
        }
    }

    // -----------------------------------------------------------------------
    // Packet broadcasting helpers
    // -----------------------------------------------------------------------

    /** Send [packet] to all PLAYING players. */
    private suspend fun broadcastPacket(packet: ByteArray) {
        val running = _state.value as? ServerState.Running ?: return
        for (player in running.players) {
            sendToPlayer(player.id, packet)
        }
    }

    /** Send [packet] to the player identified by [playerId]. */
    private suspend fun sendToPlayer(
        playerId: Int,
        packet: ByteArray,
    ) {
        // Snapshot session + transport under mutex to avoid races with the
        // contact-loop coroutine that modifies these maps.
        val (session, transport) =
            sessionsMutex.withLock {
                val s = sessionById[playerId] ?: return
                s to (playerTransports[s.loginPort] ?: return)
            }
        val running = _state.value as? ServerState.Running ?: return
        val player = running.players.firstOrNull { it.id == playerId } ?: return
        val (addr, port) = player.parseHostPort() ?: return
        transport.send(packet, addr, port)
    }

    /** Send PKT_LEAVE to all connected players for [playerId]. */
    private suspend fun sendLeaveToAll(playerId: Int) {
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
        // Keep sessionById consistent with sessions (both cleared in finally, but
        // clearing here ensures the index is never stale if closeAllPlayerTransports
        // is called independently of the finally block in future).
        sessions.clear()
        sessionById.clear()
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
        // Trim to MAX_EVENTS-1 before appending so total never exceeds MAX_EVENTS.
        // takeLast + plus is still O(N) on List, but avoids the double-allocation
        // of `events + element` followed by `drop(k)`.
        val trimmed = if (events.size >= MAX_EVENTS) events.takeLast(MAX_EVENTS - 1) else events
        return trimmed + ServerEvent(uptimeMs, level, message)
    }
}

/** Platform-agnostic current-time helper (milliseconds since epoch). */
internal expect fun currentTimeMs(): Long
