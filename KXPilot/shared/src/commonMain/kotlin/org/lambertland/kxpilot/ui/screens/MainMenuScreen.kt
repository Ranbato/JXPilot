package org.lambertland.kxpilot.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.lambertland.kxpilot.AppInfo
import org.lambertland.kxpilot.config.AppConfig
import org.lambertland.kxpilot.config.LocalAppConfig
import org.lambertland.kxpilot.config.XpOptionRegistry
import org.lambertland.kxpilot.model.ServerBrowserState
import org.lambertland.kxpilot.model.ServerInfo
import org.lambertland.kxpilot.model.ServerSource
import org.lambertland.kxpilot.model.ServerTab
import org.lambertland.kxpilot.net.fetchMetaserverList
import org.lambertland.kxpilot.resources.ShipShapeDef
import org.lambertland.kxpilot.server.ServerConfig
import org.lambertland.kxpilot.server.ServerController
import org.lambertland.kxpilot.server.ServerMetrics
import org.lambertland.kxpilot.server.ServerState
import org.lambertland.kxpilot.ui.LocalNavigator
import org.lambertland.kxpilot.ui.LocalServerController
import org.lambertland.kxpilot.ui.Screen
import org.lambertland.kxpilot.ui.components.GameButton
import org.lambertland.kxpilot.ui.components.GameButtonDanger
import org.lambertland.kxpilot.ui.theme.KXPilotColors
import org.lambertland.kxpilot.ui.util.formatUptime
import org.lambertland.kxpilot.ui.util.roundOne

// ---------------------------------------------------------------------------
// Navigation events
// ---------------------------------------------------------------------------

/**
 * Navigation intents emitted by [MainMenuStateHolder].
 *
 * The composable observes [MainMenuStateHolder.navigationEvent] and calls
 * the appropriate [Navigator] method.  The state holder itself never holds
 * a [Navigator] reference, which avoids the stale-capture problem that arises
 * from injecting a composition-local value into a `remember {}`-d object.
 */
sealed class MainMenuNavEvent {
    data class Push(
        val screen: Screen,
    ) : MainMenuNavEvent()

    data object Pop : MainMenuNavEvent()
}

// ---------------------------------------------------------------------------
// State holder
// ---------------------------------------------------------------------------

/**
 * UI-facing state holder for [MainMenuScreen].
 *
 * **No Navigator injection** — navigation intents are emitted via [navigationEvent]
 * and handled by the composable.  This avoids the stale-capture bug that occurs
 * when a composition-local is injected at construction time via `remember {}`.
 *
 * **No writes during composition** — config sync is performed via [syncConfig],
 * which must be called from a [LaunchedEffect], not directly in the composable body.
 *
 * **Async fetchInternet** — call [fetchInternet] from a coroutine scope so the
 * [ServerBrowserState.Scanning] state is actually observable before results arrive.
 *
 * @param scope  Coroutine scope for async operations (fetchInternet, scanLocal).
 *               Supply [rememberCoroutineScope] in the composable.
 */
class MainMenuStateHolder(
    private val scope: CoroutineScope,
    private val serverController: ServerController,
) {
    // -----------------------------------------------------------------------
    // Navigation
    // -----------------------------------------------------------------------

    var navigationEvent by mutableStateOf<MainMenuNavEvent?>(null)
        private set

    /** Called by the composable after it has handled and acted on a navigation event. */
    fun consumeNavigationEvent() {
        navigationEvent = null
    }

    fun navigateTo(screen: Screen) {
        navigationEvent = MainMenuNavEvent.Push(screen)
    }

    fun quit() {
        navigationEvent = MainMenuNavEvent.Pop
    }

    fun join(
        host: String,
        port: Int = ServerConfig.DEFAULT_PORT,
    ) {
        navigateTo(Screen.InGame(serverHost = host, serverPort = port))
    }

    // -----------------------------------------------------------------------
    // Config fields (synced from AppConfig via syncConfig, not during composition)
    // -----------------------------------------------------------------------

    var playerName by mutableStateOf("Player")
    var shipName by mutableStateOf("")

    /**
     * Sync config values into this holder.  Must be called from a [LaunchedEffect],
     * never directly in the composable body, to avoid writing during composition.
     */
    fun syncConfig(config: AppConfig) {
        playerName = config.get(XpOptionRegistry.nickName)
        shipName = config.get(XpOptionRegistry.shipName)
    }

    // -----------------------------------------------------------------------
    // Server browser state
    // -----------------------------------------------------------------------

    var tab by mutableStateOf(ServerTab.LOCAL)
    var browserState by mutableStateOf<ServerBrowserState>(ServerBrowserState.Idle)

    /**
     * The last successfully loaded internet server list.  Retained so BACK from
     * the detail panel restores the real list instead of a hardcoded stub.
     */
    private var lastLoadedServers: List<ServerInfo> = emptyList()

    // -----------------------------------------------------------------------
    // Direct-connect input state (UI state — not domain state)
    // -----------------------------------------------------------------------

    var directHost by mutableStateOf("")
    var directPort by mutableStateOf(ServerConfig.DEFAULT_PORT.toString())

    /** Parsed port value; null when the current [directPort] string is invalid or out of range. */
    val directPortInt: Int?
        get() = directPort.toIntOrNull()?.takeIf { it in 1..65535 }

    /** True when the direct-connect form has a valid host and a valid port. */
    val canConnectDirect: Boolean
        get() = directHost.isNotBlank() && directPortInt != null

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------

    fun scanLocal() {
        fetchAll()
    }

    /**
     * Fetch the internet server list.  Delegates to [fetchAll] so that both local
     * and internet servers are returned in a single combined list.
     */
    fun fetchInternet() {
        fetchAll()
    }

    /**
     * Fetch both local and internet servers simultaneously and merge them into a
     * single combined list.  Transitions through [ServerBrowserState.Scanning] so
     * the UI can show a progress indicator during the async fetch.
     */
    fun fetchAll() {
        scope.launch {
            browserState = ServerBrowserState.Scanning
            // Local: check embedded server
            val localList: List<ServerInfo> =
                buildList {
                    val running = serverController.state.value as? ServerState.Running
                    if (running != null) {
                        add(
                            ServerInfo(
                                host = "127.0.0.1",
                                port = running.config.port,
                                mapName =
                                    running.config.mapPath
                                        ?.substringAfterLast('/')
                                        ?.removeSuffix(".xp") ?: "default",
                                playerCount = running.players.size,
                                queueCount = 0,
                                maxPlayers = running.config.maxPlayers,
                                fps = running.config.targetFps,
                                version = AppInfo.VERSION_STRING,
                                pingMs = 0,
                                status = "running",
                                players = running.players.map { it.name },
                                source = ServerSource.LOCAL,
                            ),
                        )
                    }
                }
            // Internet: fetch metaserver
            val internetList: List<ServerInfo> =
                try {
                    fetchMetaserverList().map { it.copy(source = ServerSource.INTERNET) }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    emptyList()
                }
            val combined = localList + internetList
            lastLoadedServers = combined
            browserState =
                if (combined.isEmpty()) {
                    ServerBrowserState.Error("No servers found. Check your network connection.")
                } else {
                    ServerBrowserState.Loaded(combined)
                }
        }
    }

    fun selectServer(s: ServerInfo) {
        browserState = ServerBrowserState.Detail(s)
    }

    /** Navigate back from a [ServerBrowserState.Detail] to the last loaded list. */
    fun backFromDetail() {
        browserState =
            if (lastLoadedServers.isNotEmpty()) {
                ServerBrowserState.Loaded(lastLoadedServers)
            } else {
                ServerBrowserState.Idle
            }
    }
}

// ---------------------------------------------------------------------------
// Screen composable
// ---------------------------------------------------------------------------

@Composable
fun MainMenuScreen(
    onSaveConfig: (String) -> Unit = {},
    availableShips: List<ShipShapeDef> = emptyList(),
) {
    val navigator = LocalNavigator.current
    val config = LocalAppConfig.current
    val serverController = LocalServerController.current
    val scope = rememberCoroutineScope()
    val state = remember(scope) { MainMenuStateHolder(scope, serverController) }

    // Sync config → state exactly once per config change, not on every recomposition.
    LaunchedEffect(config) { state.syncConfig(config) }

    // Handle navigation events emitted by the state holder.
    val navEvent = state.navigationEvent
    LaunchedEffect(navEvent) {
        when (navEvent) {
            is MainMenuNavEvent.Push -> {
                navigator.push(navEvent.screen)
                state.consumeNavigationEvent()
            }

            is MainMenuNavEvent.Pop -> {
                navigator.pop()
                state.consumeNavigationEvent()
            }

            null -> {
                Unit
            }
        }
    }

    // Observe server state for button colour and metrics overlay.
    val serverState by serverController.state.collectAsState()
    val isServerRunning = serverState is ServerState.Running
    val serverMetrics: ServerMetrics? = (serverState as? ServerState.Running)?.metrics
    var showQuitConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxSize().background(KXPilotColors.Background),
        ) {
            // ---- Left sidebar: title + player name + ship picker + nav buttons ----
            Column(
                modifier =
                    Modifier
                        .width(180.dp)
                        .fillMaxHeight()
                        .background(KXPilotColors.SurfaceVariant)
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "KXPilot",
                    style =
                        TextStyle(
                            color = KXPilotColors.AccentBright,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        ),
                )
                Text(
                    AppInfo.VERSION_LABEL,
                    style =
                        TextStyle(
                            color = KXPilotColors.OnSurfaceDim,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Player name:",
                    style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                )
                BasicTextField(
                    value = state.playerName,
                    onValueChange = { v ->
                        state.playerName = v
                        config.set(XpOptionRegistry.nickName, v)
                        onSaveConfig(config.toRcText())
                    },
                    singleLine = true,
                    textStyle =
                        TextStyle(
                            color = KXPilotColors.OnSurface,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                    cursorBrush = SolidColor(KXPilotColors.Accent),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(KXPilotColors.Surface)
                            .border(1.dp, KXPilotColors.Accent)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                )

                if (availableShips.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Ship:",
                        style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    )
                    ShipPicker(
                        ships = availableShips,
                        selected = state.shipName,
                        onSelect = { name ->
                            state.shipName = name
                            config.set(XpOptionRegistry.shipName, name)
                            onSaveConfig(config.toRcText())
                        },
                    )
                }

                Spacer(Modifier.height(12.dp))

                TabButton("LOCAL", state.tab == ServerTab.LOCAL) {
                    state.tab = ServerTab.LOCAL
                    state.fetchAll()
                }
                TabButton("INTERNET", state.tab == ServerTab.INTERNET) {
                    state.tab = ServerTab.INTERNET
                    state.fetchAll()
                }

                Spacer(Modifier.weight(1f))

                GameButton("ABOUT", onClick = { state.navigateTo(Screen.About) }, modifier = Modifier.fillMaxWidth())
                GameButton("KEYS", onClick = { state.navigateTo(Screen.KeyBindings) }, modifier = Modifier.fillMaxWidth())
                GameButton("CONFIG", onClick = { state.navigateTo(Screen.Config) }, modifier = Modifier.fillMaxWidth())
                // SERVER button: green when a server is running.
                GameButton(
                    "SERVER",
                    onClick = { state.navigateTo(Screen.ServerDashboard) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isServerRunning) KXPilotColors.Success else KXPilotColors.Accent,
                )
                Spacer(Modifier.height(4.dp))
                GameButtonDanger(
                    "QUIT",
                    onClick = {
                        if (isServerRunning) {
                            showQuitConfirm = true
                        } else {
                            state.quit()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ---- Right panel: server browser ----
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().background(KXPilotColors.Background),
            ) {
                when (val bs = state.browserState) {
                    ServerBrowserState.Idle -> {
                        IdlePanel(state.tab) {
                            if (state.tab == ServerTab.LOCAL) state.scanLocal() else state.fetchInternet()
                        }
                    }

                    ServerBrowserState.Scanning -> {
                        ScanningPanel()
                    }

                    is ServerBrowserState.Loaded -> {
                        ServerListPanel(
                            servers = bs.servers,
                            onSelectServer = { state.selectServer(it) },
                        )
                    }

                    is ServerBrowserState.Detail -> {
                        ServerDetailPanel(
                            detail = bs,
                            onJoin = { s -> state.join(s.host, s.port) },
                            onBack = { state.backFromDetail() },
                        )
                    }

                    is ServerBrowserState.Error -> {
                        ErrorPanel(bs.message)
                    }

                    is ServerBrowserState.ConnectLocal -> {
                        ConnectLocalPanel(
                            connectState = bs,
                            directHost = state.directHost,
                            directPort = state.directPort,
                            canConnect = state.canConnectDirect,
                            onHostChange = { state.directHost = it },
                            onPortChange = { state.directPort = it },
                            onConnect = {
                                state.join(state.directHost.trim(), state.directPortInt ?: ServerConfig.DEFAULT_PORT)
                            },
                            onConnectLocal = { s -> state.join(s.host, s.port) },
                            onScanAgain = { state.scanLocal() },
                        )
                    }
                }
            }
        }

        // ---- Metrics overlay: bottom-right corner, only when server is running ----
        if (serverMetrics != null) {
            ServerMetricsOverlay(
                metrics = serverMetrics,
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
            )
        }

        // Quit confirmation overlay — shown when QUIT is pressed and server is running
        if (showQuitConfirm) {
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
                            .border(1.dp, KXPilotColors.Danger)
                            .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        "A server is running.",
                        style =
                            TextStyle(
                                color = KXPilotColors.Danger,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                            ),
                    )
                    Text(
                        "Quit anyway?",
                        style =
                            TextStyle(
                                color = KXPilotColors.OnSurface,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                            ),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GameButtonDanger("QUIT", onClick = { state.quit() })
                        GameButton("CANCEL", onClick = { showQuitConfirm = false })
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Server metrics overlay (bottom-right, visible when server is running)
// ---------------------------------------------------------------------------

/**
 * Compact read-only metrics panel shown in the bottom-right corner of the main
 * menu whenever an embedded server is active.
 */
@Composable
private fun ServerMetricsOverlay(
    metrics: ServerMetrics,
    modifier: Modifier = Modifier,
) {
    // Memoize string formatting — metrics update at most once per second so this
    // avoids needless String allocations on unrelated recompositions.
    val uptimeStr = remember(metrics.uptimeMs) { formatUptime(metrics.uptimeMs) }
    val tickStr =
        remember(metrics.tickRateActual, metrics.tickRateTarget) {
            "${metrics.tickRateActual.roundOne()} / ${metrics.tickRateTarget} Hz"
        }
    val cpuStr =
        remember(metrics.cpuPercent) {
            if (metrics.cpuPercent < 0) "—" else "${metrics.cpuPercent.roundOne()}%"
        }

    Column(
        modifier =
            modifier
                .background(KXPilotColors.Background.copy(alpha = 0.85f))
                .border(1.dp, KXPilotColors.Success)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            "● SERVER RUNNING",
            style =
                TextStyle(
                    color = KXPilotColors.Success,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                ),
        )
        OverlayRow("Uptime", uptimeStr)
        OverlayRow("Tick", tickStr)
        OverlayRow("Players", metrics.playerCount.toString())
        OverlayRow("CPU", cpuStr)
    }
}

@Composable
private fun OverlayRow(
    label: String,
    value: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // Fixed-width label column; avoids String.padEnd() allocations on every recomposition.
        Text(
            label,
            style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 9.sp, fontFamily = FontFamily.Monospace),
            modifier = Modifier.width(48.dp),
        )
        Text(
            value,
            style = TextStyle(color = KXPilotColors.OnSurface, fontSize = 9.sp, fontFamily = FontFamily.Monospace),
        )
    }
}

// ---------------------------------------------------------------------------
// Sub-panels
// ---------------------------------------------------------------------------

// Note: IdlePanel and ScanningPanel are only reachable from the INTERNET tab.
// The LOCAL tab transitions directly to ConnectLocal via scanLocal().

@Composable
private fun IdlePanel(
    tab: ServerTab,
    onScan: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (tab == ServerTab.LOCAL) "Scan for local servers" else "Fetch internet server list",
                style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
            )
            GameButton("SCAN", onClick = onScan)
        }
    }
}

@Composable
private fun ScanningPanel() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = KXPilotColors.Accent)
            Text("Scanning…", style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace))
        }
    }
}

@Composable
private fun ErrorPanel(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, style = TextStyle(color = KXPilotColors.Danger, fontSize = 12.sp, fontFamily = FontFamily.Monospace))
    }
}

/**
 * Panel shown when the LOCAL tab is active.
 *
 * All mutable state is owned by the caller; this composable only reads and
 * fires callbacks, making it independently testable and previewable.
 */
@Composable
private fun ConnectLocalPanel(
    connectState: ServerBrowserState.ConnectLocal,
    directHost: String,
    directPort: String,
    canConnect: Boolean,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnect: () -> Unit,
    onConnectLocal: (ServerInfo) -> Unit,
    onScanAgain: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "LOCAL SERVER",
            style =
                TextStyle(
                    color = KXPilotColors.AccentBright,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                ),
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(KXPilotColors.Accent))

        if (connectState.scanning) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(
                    color = KXPilotColors.Accent,
                    modifier = Modifier.width(18.dp).height(18.dp),
                    strokeWidth = 2.dp,
                )
                Text(
                    "Scanning…",
                    style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                )
            }
        } else if (connectState.localServer != null) {
            val s = connectState.localServer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${s.host}:${s.port}",
                        style = TextStyle(color = KXPilotColors.OnSurface, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                    )
                    Text(
                        "Map: ${s.mapName}  Players: ${s.playerCount}/${s.maxPlayers}  FPS: ${s.fps}",
                        style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    )
                }
                GameButton("CONNECT", onClick = { onConnectLocal(s) })
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "No local server found.",
                    style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                )
                GameButton("SCAN AGAIN", onClick = onScanAgain)
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "CONNECT DIRECTLY",
            style =
                TextStyle(
                    color = KXPilotColors.AccentBright,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                ),
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(KXPilotColors.Accent))

        Text(
            "Host / IP address:",
            style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
        )
        BasicTextField(
            value = directHost,
            onValueChange = onHostChange,
            singleLine = true,
            textStyle = TextStyle(color = KXPilotColors.OnSurface, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(KXPilotColors.Accent),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(KXPilotColors.Surface)
                    .border(1.dp, KXPilotColors.Accent)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
        )

        Text("Port:", style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace))
        BasicTextField(
            value = directPort,
            onValueChange = onPortChange,
            singleLine = true,
            textStyle = TextStyle(color = KXPilotColors.OnSurface, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(KXPilotColors.Accent),
            modifier =
                Modifier
                    .width(100.dp)
                    .background(KXPilotColors.Surface)
                    .border(1.dp, KXPilotColors.Accent)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
        )

        GameButton("CONNECT", onClick = onConnect, enabled = canConnect)
    }
}

/**
 * Server list panel.  Takes a targeted [onSelectServer] callback instead of the
 * full state holder to limit coupling.
 */
@Composable
private fun ServerListPanel(
    servers: List<ServerInfo>,
    onSelectServer: (ServerInfo) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(KXPilotColors.SurfaceVariant).padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            ColHeader("Src", Modifier.width(50.dp))
            ColHeader("Pl", Modifier.width(40.dp))
            ColHeader("FPS", Modifier.width(40.dp))
            ColHeader("Map", Modifier.weight(1f))
            ColHeader("Server", Modifier.weight(1f))
            ColHeader("Ping", Modifier.width(50.dp))
            ColHeader("Status", Modifier.width(70.dp))
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Use a stable typed key: host + port uniquely identify a server entry.
            items(servers, key = { it.host to it.port }) { server ->
                ServerRow(server, onClick = { onSelectServer(server) })
            }
        }
    }
}

/**
 * Server detail panel.  Takes [onJoin] and [onBack] callbacks; restoring the
 * previous list is the caller's responsibility via [onBack].
 */
@Composable
private fun ServerDetailPanel(
    detail: ServerBrowserState.Detail,
    onJoin: (ServerInfo) -> Unit,
    onBack: () -> Unit,
) {
    val s = detail.server
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            s.host,
            style =
                TextStyle(
                    color = KXPilotColors.AccentBright,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                ),
        )
        DetailRow("Port", s.port.toString())
        DetailRow("Map", s.mapName)
        DetailRow("Players", "${s.playerCount} / ${s.maxPlayers}  (${s.queueCount} queued)")
        DetailRow("FPS", s.fps.toString())
        DetailRow("Version", s.version)
        DetailRow("Ping", s.pingMs?.let { "${it}ms" } ?: "—")
        DetailRow("Status", s.status)
        if (detail.server.players.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Players:", style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace))
            for (p in detail.server.players) {
                Text("  $p", style = TextStyle(color = KXPilotColors.OnSurface, fontSize = 12.sp, fontFamily = FontFamily.Monospace))
            }
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GameButton("JOIN THIS SERVER", onClick = { onJoin(s) })
            GameButton("BACK", onClick = onBack)
        }
    }
}

// ---------------------------------------------------------------------------
// Small reusable row/cell helpers
// ---------------------------------------------------------------------------

@Composable
private fun TabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) KXPilotColors.Accent else KXPilotColors.SurfaceVariant
    val textColor = if (selected) KXPilotColors.Accent else KXPilotColors.OnSurfaceDim
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(KXPilotColors.Surface)
                .border(1.dp, borderColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Text(label, style = TextStyle(color = textColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace))
    }
}

@Composable
private fun ServerRow(
    server: ServerInfo,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(KXPilotColors.Surface)
                .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Source badge
        Text(
            if (server.source == ServerSource.LOCAL) "LOCAL" else "NET",
            style =
                TextStyle(
                    color = if (server.source == ServerSource.LOCAL) KXPilotColors.Success else KXPilotColors.Accent,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                ),
            modifier = Modifier.width(50.dp),
        )
        CellText("${server.playerCount}/${server.maxPlayers}", Modifier.width(40.dp))
        CellText(server.fps.toString(), Modifier.width(40.dp))
        CellText(server.mapName, Modifier.weight(1f))
        CellText(server.host, Modifier.weight(1f))
        CellText(server.pingMs?.let { "${it}ms" } ?: "—", Modifier.width(50.dp))
        CellText(server.status, Modifier.width(70.dp))
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Row {
        Text(
            "$label:".padEnd(12),
            style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
        )
        Text(value, style = TextStyle(color = KXPilotColors.OnSurface, fontSize = 12.sp, fontFamily = FontFamily.Monospace))
    }
}

@Composable
private fun RowScope.ColHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        style =
            TextStyle(
                color = KXPilotColors.OnSurfaceDim,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            ),
        modifier = modifier,
    )
}

@Composable
private fun RowScope.CellText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        style = TextStyle(color = KXPilotColors.OnSurface, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
        modifier = modifier,
        maxLines = 1,
    )
}

// ---------------------------------------------------------------------------
// Ship picker (autocomplete dropdown with hull preview)
// ---------------------------------------------------------------------------

private const val PREVIEW_SIZE_DP = 28
private const val HULL_EXTENT = 15f
private const val ROTATION_PERIOD_MS = 15_000f

@Composable
private fun ShipHullPreview(
    shape: ShipShapeDef?,
    color: Color,
    angleDeg: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = (size.width / 2f) / (HULL_EXTENT + 2f)

        val hull = shape?.hull
        val path = Path()
        if (hull != null && hull.size >= 2) {
            hull.forEachIndexed { i, (lx, ly) ->
                if (i == 0) {
                    path.moveTo(lx * scale, -ly * scale)
                } else {
                    path.lineTo(lx * scale, -ly * scale)
                }
            }
            path.close()
        } else {
            path.moveTo(14f * scale, 0f)
            path.lineTo(-8f * scale, -8f * scale)
            path.lineTo(-8f * scale, 8f * scale)
            path.close()
        }

        rotate(
            degrees = angleDeg,
            pivot =
                androidx.compose.ui.geometry
                    .Offset(cx, cy),
        ) {
            translate(left = cx, top = cy) {
                drawPath(path, color = color, style = Stroke(width = 1.5f))
            }
        }
    }
}

@Composable
private fun ShipPicker(
    ships: List<ShipShapeDef>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    // query is NOT keyed on `selected` — keying on `selected` would reset in-progress
    // user input whenever an external state change triggers a recomposition with a new
    // `selected` value.  The field is uncontrolled: it initialises to `selected` once
    // and the user owns it thereafter.
    var query by remember { mutableStateOf(selected) }
    var expanded by remember { mutableStateOf(false) }
    var animAngle by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastMs = 0L
        while (true) {
            val frameMs = withFrameMillis { it }
            val delta = if (lastMs == 0L) 0L else frameMs - lastMs
            lastMs = frameMs
            animAngle = (animAngle + delta / ROTATION_PERIOD_MS * 360f) % 360f
        }
    }

    val selectedShip = remember(selected, ships) { ships.firstOrNull { it.name == selected } }
    val filtered =
        remember(query, ships) {
            if (query.isBlank()) ships else ships.filter { it.name.contains(query, ignoreCase = true) }
        }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(KXPilotColors.Surface)
                    .border(1.dp, if (expanded) KXPilotColors.AccentBright else KXPilotColors.Accent)
                    .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShipHullPreview(
                shape = selectedShip,
                color = KXPilotColors.AccentBright,
                angleDeg = animAngle,
                modifier = Modifier.width(PREVIEW_SIZE_DP.dp).height(PREVIEW_SIZE_DP.dp),
            )
            BasicTextField(
                value = query,
                onValueChange = { v ->
                    query = v
                    expanded = true
                },
                singleLine = true,
                textStyle = TextStyle(color = KXPilotColors.OnSurface, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(KXPilotColors.Accent),
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 6.dp),
            )
        }

        if (expanded && filtered.isNotEmpty()) {
            val scrollState = rememberScrollState()
            val thumbFraction =
                if (scrollState.maxValue == 0) {
                    1f
                } else {
                    (scrollState.viewportSize.toFloat() / (scrollState.maxValue + scrollState.viewportSize).toFloat())
                        .coerceIn(0.1f, 1f)
                }
            val thumbOffset =
                if (scrollState.maxValue == 0) {
                    0f
                } else {
                    (scrollState.value.toFloat() / scrollState.maxValue.toFloat()) * (1f - thumbFraction)
                }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .background(KXPilotColors.Surface)
                        .border(1.dp, KXPilotColors.Accent),
            ) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(scrollState),
                ) {
                    filtered.forEach { ship ->
                        val isSelected = ship.name == selected
                        val bg = if (isSelected) KXPilotColors.Accent else KXPilotColors.Surface
                        val textColor = if (isSelected) KXPilotColors.Background else KXPilotColors.OnSurface
                        val hullColor = if (isSelected) KXPilotColors.Background else KXPilotColors.AccentBright
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(bg)
                                    .clickable {
                                        onSelect(ship.name)
                                        query = ship.name
                                        expanded = false
                                    }.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            ShipHullPreview(
                                shape = ship,
                                color = hullColor,
                                angleDeg = animAngle,
                                modifier = Modifier.width(PREVIEW_SIZE_DP.dp).height(PREVIEW_SIZE_DP.dp),
                            )
                            Text(
                                ship.name,
                                style = TextStyle(color = textColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                if (thumbFraction < 1f) {
                    BoxWithConstraints(
                        modifier = Modifier.width(6.dp).fillMaxHeight().background(KXPilotColors.SurfaceVariant),
                    ) {
                        val trackPx = constraints.maxHeight.toFloat()
                        val thumbHeightPx = trackPx * thumbFraction
                        val thumbTopPx = (trackPx - thumbHeightPx) * thumbOffset
                        val density = LocalDensity.current
                        Box(
                            Modifier
                                .width(6.dp)
                                .height(with(density) { thumbHeightPx.toDp() })
                                .offset(y = with(density) { thumbTopPx.toDp() })
                                .background(KXPilotColors.Accent),
                        )
                    }
                }
            }
        }
    }
}
