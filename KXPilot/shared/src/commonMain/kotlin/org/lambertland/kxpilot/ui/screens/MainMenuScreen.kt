package org.lambertland.kxpilot.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lambertland.kxpilot.config.LocalAppConfig
import org.lambertland.kxpilot.config.XpOptionRegistry
import org.lambertland.kxpilot.model.ServerBrowserState
import org.lambertland.kxpilot.model.ServerInfo
import org.lambertland.kxpilot.model.ServerTab
import org.lambertland.kxpilot.net.fetchMetaserverList
import org.lambertland.kxpilot.resources.ShipShapeDef
import org.lambertland.kxpilot.server.ServerConfig
import org.lambertland.kxpilot.server.ServerController
import org.lambertland.kxpilot.server.ServerMetrics
import org.lambertland.kxpilot.server.ServerState
import org.lambertland.kxpilot.ui.LocalNavigator
import org.lambertland.kxpilot.ui.Navigator
import org.lambertland.kxpilot.ui.Screen
import org.lambertland.kxpilot.ui.components.GameButton
import org.lambertland.kxpilot.ui.components.GameButtonDanger
import org.lambertland.kxpilot.ui.theme.KXPilotColors

// ---------------------------------------------------------------------------
// State holder
// ---------------------------------------------------------------------------

class MainMenuStateHolder(
    private val scope: CoroutineScope,
    private val serverController: ServerController,
) {
    var playerName by mutableStateOf("Player")
    var shipName by mutableStateOf("")
    var tab by mutableStateOf(ServerTab.LOCAL)
    var browserState by mutableStateOf<ServerBrowserState>(ServerBrowserState.Idle)

    // Direct-connect input state — lives here, not in ConnectLocal, because these
    // are text-field UI state not domain state.  They survive tab switches.
    var directHost by mutableStateOf("")
    var directPort by mutableStateOf(ServerConfig.DEFAULT_PORT.toString())

    /** Parsed port value; null when the current [directPort] string is invalid or out of range. */
    val directPortInt: Int?
        get() = directPort.toIntOrNull()?.takeIf { it in 1..65535 }

    /** True when the direct-connect form has a valid host and a valid port. */
    val canConnectDirect: Boolean
        get() = directHost.isNotBlank() && directPortInt != null

    fun scanLocal() {
        val running = serverController.state.value as? ServerState.Running
        val localServer =
            if (running != null) {
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
                )
            } else {
                null
            }
        browserState = ServerBrowserState.ConnectLocal(localServer = localServer, scanning = false)
    }

    fun fetchInternet() {
        scope.launch {
            browserState = ServerBrowserState.Scanning
            // N1: delegates to fetchMetaserverList() expect/actual.
            // On desktop this returns STUB_INTERNET_SERVERS until ktor-client is wired in.
            // Replace the desktop actual with a real HTTP GET to AppInfo.METASERVER_URL.
            val servers = fetchMetaserverList()
            lastLoadedServers = servers
            browserState = ServerBrowserState.Loaded(servers)
        }
    }

    fun selectServer(s: ServerInfo) {
        browserState = ServerBrowserState.Detail(s, s.players)
    }

    fun join(
        host: String,
        port: Int = ServerConfig.DEFAULT_PORT,
    ) {
        navigator.push(Screen.InGame(serverHost = host, serverPort = port))
    }

    fun quit() {
        navigator.pop()
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

    // Sync state holder ↔ AppConfig on first composition
    state.playerName = config.get(XpOptionRegistry.nickName)
    state.shipName = config.get(XpOptionRegistry.shipName)

    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .background(KXPilotColors.Background),
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
                "v0.1-alpha",
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

            // Tab selection — LOCAL / INTERNET
            TabButton("LOCAL", state.tab == ServerTab.LOCAL) {
                state.tab = ServerTab.LOCAL
                state.scanLocal()
            }
            TabButton("INTERNET", state.tab == ServerTab.INTERNET) {
                state.tab = ServerTab.INTERNET
                state.fetchInternet()
            }

            Spacer(Modifier.weight(1f))

            // Navigation buttons
            GameButton("ABOUT", onClick = { navigator.push(Screen.About) }, modifier = Modifier.fillMaxWidth())
            GameButton("KEYS", onClick = { navigator.push(Screen.KeyBindings) }, modifier = Modifier.fillMaxWidth())
            GameButton("CONFIG", onClick = { navigator.push(Screen.Config) }, modifier = Modifier.fillMaxWidth())
            GameButton("SERVER", onClick = { navigator.push(Screen.ServerDashboard) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            GameButtonDanger("QUIT", onClick = { state.quit() }, modifier = Modifier.fillMaxWidth())
        }

        // ---- Right panel: server list or detail ----
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(KXPilotColors.Background),
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
                    ServerListPanel(bs.servers, state)
                }

                is ServerBrowserState.Detail -> {
                    ServerDetailPanel(bs, state)
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
}

// ---------------------------------------------------------------------------
// Sub-panels
// ---------------------------------------------------------------------------

// Note: IdlePanel and ScanningPanel are only reachable from the INTERNET tab.
// The LOCAL tab transitions directly to ConnectLocal via scanLocal(), so it
// never passes through Idle or Scanning states.

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
 * Provides two ways to connect:
 *  1. Local server — shows a detected local instance or a "None found" message with a re-scan button.
 *  2. Direct connect — free-form host/port entry.
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
        // ---- Section: Local server ----
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "No local server found.",
                    style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                )
                GameButton("SCAN AGAIN", onClick = onScanAgain)
            }
        }

        Spacer(Modifier.height(8.dp))

        // ---- Section: Direct connect ----
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

        Text(
            "Port:",
            style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
        )
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

@Composable
private fun ServerListPanel(
    servers: List<ServerInfo>,
    state: MainMenuStateHolder,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Column headers (sticky via wrapping in a non-lazy Column outside LazyColumn)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(KXPilotColors.SurfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            ColHeader("Pl", Modifier.width(40.dp))
            ColHeader("FPS", Modifier.width(40.dp))
            ColHeader("Map", Modifier.weight(1f))
            ColHeader("Server", Modifier.weight(1f))
            ColHeader("Ping", Modifier.width(50.dp))
            ColHeader("Status", Modifier.width(70.dp))
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(servers, key = { "${it.host}:${it.port}" }) { server ->
                ServerRow(server, onClick = { state.selectServer(server) })
            }
        }
    }
}

@Composable
private fun ServerDetailPanel(
    detail: ServerBrowserState.Detail,
    state: MainMenuStateHolder,
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
        if (detail.players.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Players:", style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace))
            for (p in detail.players) {
                Text("  $p", style = TextStyle(color = KXPilotColors.OnSurface, fontSize = 12.sp, fontFamily = FontFamily.Monospace))
            }
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GameButton("JOIN THIS SERVER", onClick = { state.join(s.host, s.port) })
            GameButton("BACK", onClick = {
                state.browserState = ServerBrowserState.Loaded(STUB_INTERNET_SERVERS)
            })
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

private const val PREVIEW_SIZE_DP = 28 // square canvas size for the hull thumbnail
private const val HULL_EXTENT = 15f // XPilot local coords range ±15
private const val ROTATION_PERIOD_MS = 15_000f // one full clockwise turn every 15 s

/**
 * Draws a ship hull polygon scaled to fit a square canvas, rotated by [angleDeg].
 * Coords are in XPilot local space (x right, y up, ±15); Y is flipped for screen space.
 * Falls back to a simple triangle when [shape] is null or has an empty hull.
 *
 * Rotation is applied around the canvas centre using [rotate] (positive = clockwise in
 * Compose screen space).
 */
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

        // Build path in local coords (origin at centre)
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

        // Rotate around canvas centre, then translate to centre for drawing
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

/**
 * Autocomplete dropdown for ship selection with animated hull previews.
 *
 * - The collapsed field shows the currently selected ship's hull rotating clockwise.
 * - The dropdown rows each show the hull rotating at the same angle.
 * - All previews in this picker share one [animAngle] driven by [withFrameMillis], so
 *   they stay in sync and use only a single animation loop.
 * - One full revolution takes [ROTATION_PERIOD_MS] milliseconds (15 s).
 */
@Composable
private fun ShipPicker(
    ships: List<ShipShapeDef>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    var query by remember(selected) { mutableStateOf(selected) }
    var expanded by remember { mutableStateOf(false) }
    var animAngle by remember { mutableFloatStateOf(0f) }

    // Drive rotation: advance angle proportionally to elapsed wall time.
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
            if (query.isBlank()) {
                ships
            } else {
                ships.filter { it.name.contains(query, ignoreCase = true) }
            }
        }

    Column(modifier = Modifier.fillMaxWidth()) {
        // ---- Collapsed field: preview + text input ----
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
                textStyle =
                    TextStyle(
                        color = KXPilotColors.OnSurface,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                cursorBrush = SolidColor(KXPilotColors.Accent),
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
            )
        }

        // ---- Drop-down list with rotating previews + scrollbar ----
        if (expanded && filtered.isNotEmpty()) {
            val scrollState = rememberScrollState()
            // Thumb fraction: visible window / total content.
            // Clamped so thumb is never smaller than 10% or larger than 100%.
            val thumbFraction =
                if (scrollState.maxValue == 0) {
                    1f
                } else {
                    (1f - scrollState.maxValue.toFloat() / (scrollState.maxValue + scrollState.viewportSize.toFloat()))
                        .coerceIn(0.1f, 1f)
                }
            val thumbOffset =
                if (scrollState.maxValue == 0) {
                    0f
                } else {
                    scrollState.value.toFloat() / scrollState.maxValue.toFloat()
                }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .background(KXPilotColors.Surface)
                        .border(1.dp, KXPilotColors.Accent),
            ) {
                // Scrollable ship list
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(scrollState),
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

                // Scrollbar track + thumb (visible only when content overflows)
                if (thumbFraction < 1f) {
                    Column(
                        modifier = Modifier.width(6.dp).fillMaxHeight().background(KXPilotColors.SurfaceVariant),
                    ) {
                        // Spacer above thumb proportional to scroll position
                        val spaceAbove = (1f - thumbFraction) * thumbOffset
                        Spacer(Modifier.fillMaxWidth().fillMaxHeight(spaceAbove))
                        // Thumb
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(thumbFraction)
                                    .background(KXPilotColors.Accent),
                        )
                        // Spacer below thumb (remaining space)
                        Spacer(Modifier.fillMaxWidth().weight(1f))
                    }
                }
            }
        }
    }
}
