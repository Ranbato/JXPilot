package org.lambertland.kxpilot.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lambertland.kxpilot.model.DashboardPlayerRow
import org.lambertland.kxpilot.model.ServerEvent
import org.lambertland.kxpilot.model.ServerEventLevel
import org.lambertland.kxpilot.server.ServerController
import org.lambertland.kxpilot.server.ServerState
import org.lambertland.kxpilot.ui.LocalNavigator
import org.lambertland.kxpilot.ui.components.GameButton
import org.lambertland.kxpilot.ui.components.GameButtonDanger
import org.lambertland.kxpilot.ui.stateholder.DashboardDialog
import org.lambertland.kxpilot.ui.stateholder.ServerDashboardStateHolder
import org.lambertland.kxpilot.ui.theme.KXPilotColors
import org.lambertland.kxpilot.ui.util.formatUptime
import org.lambertland.kxpilot.ui.util.roundOne

// ---------------------------------------------------------------------------
// Screen entry point
// ---------------------------------------------------------------------------

/**
 * Server dashboard screen.
 *
 * Wires a [ServerDashboardStateHolder] to a shared [ServerController] and
 * renders:
 *   - Status bar (state, uptime, tick rate, map)
 *   - Six metrics tiles
 *   - Player table with kick / mute / message controls
 *   - Server config form (port, max players, map, etc.)
 *   - Event log
 *
 * Pass a [controller] created at the application root when you need the server
 * to outlive this screen.  When [controller] is null a new [ServerController]
 * is created and scoped to this composition.
 */
@Composable
fun ServerDashboardScreen(controller: ServerController? = null) {
    val scope = rememberCoroutineScope()
    val effectiveController = remember(controller) { controller ?: ServerController(scope) }
    val state = remember(effectiveController) { ServerDashboardStateHolder(effectiveController, scope) }
    val navigator = LocalNavigator.current

    Box(modifier = Modifier.fillMaxSize().background(KXPilotColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ---- Top bar ----
            DashboardTopBar(state = state, onBack = { navigator.pop() })

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // ---- Left column: config form + metrics ----
                Column(
                    modifier =
                        Modifier
                            .width(260.dp)
                            .fillMaxHeight()
                            .background(KXPilotColors.SurfaceVariant)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SectionHeader("Configuration")
                    ConfigForm(state)

                    Spacer(Modifier.height(8.dp))
                    SectionHeader("Metrics")
                    MetricsTiles(state)
                }

                // ---- Right column: players + event log ----
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                ) {
                    // Player table (top half)
                    Box(modifier = Modifier.weight(1f)) {
                        PlayerTable(state)
                    }
                    // Event log (bottom half)
                    Box(modifier = Modifier.weight(1f)) {
                        EventLog(state.events)
                    }
                }
            }
        }

        // ---- Dialogs (rendered on top of everything) ----
        when (val d = state.dialog) {
            DashboardDialog.None -> {
                Unit
            }

            DashboardDialog.MessageAll -> {
                SimpleInputDialog(
                    title = "Broadcast message",
                    label = "Message",
                    value = state.dialogInput,
                    onValueChange = { state.dialogInput = it },
                    onConfirm = { state.confirmDialog() },
                    onDismiss = { state.dismissDialog() },
                )
            }

            is DashboardDialog.MessageOne -> {
                SimpleInputDialog(
                    title = "Message → ${d.playerName}",
                    label = "Message",
                    value = state.dialogInput,
                    onValueChange = { state.dialogInput = it },
                    onConfirm = { state.confirmDialog() },
                    onDismiss = { state.dismissDialog() },
                )
            }

            is DashboardDialog.ConfirmKick -> {
                ConfirmDialog(
                    title = "Kick ${d.playerName}?",
                    body = "The player will be disconnected immediately.",
                    confirmLabel = "KICK",
                    onConfirm = { state.confirmDialog() },
                    onDismiss = { state.dismissDialog() },
                )
            }

            is DashboardDialog.ChangeMap -> {
                SimpleInputDialog(
                    title = "Change map",
                    label = "Map path",
                    value = state.dialogInput,
                    onValueChange = { state.dialogInput = it },
                    onConfirm = { state.confirmDialog() },
                    onDismiss = { state.dismissDialog() },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Top bar
// ---------------------------------------------------------------------------

@Composable
private fun DashboardTopBar(
    state: ServerDashboardStateHolder,
    onBack: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(KXPilotColors.SurfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GameButton("BACK", onClick = onBack)

        Text(
            "SERVER DASHBOARD",
            style =
                TextStyle(
                    color = KXPilotColors.AccentBright,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                ),
        )

        Spacer(Modifier.weight(1f))

        // Status badge
        val (statusText, statusColor) =
            when (state.serverState) {
                ServerState.Stopped -> "STOPPED" to KXPilotColors.OnSurfaceDim
                ServerState.Starting -> "STARTING…" to KXPilotColors.Accent
                is ServerState.Running -> "RUNNING" to KXPilotColors.Success
                is ServerState.Error -> "ERROR" to KXPilotColors.Danger
            }
        Text(
            statusText,
            style =
                TextStyle(
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                ),
        )

        when {
            state.isStarting -> {
                CircularProgressIndicator(color = KXPilotColors.Accent, modifier = Modifier.width(18.dp).height(18.dp))
            }

            state.isStopped || state.errorMessage != null -> {
                GameButton("START", onClick = { state.startServer() })
            }

            state.isRunning -> {
                GameButtonDanger("STOP", onClick = { state.stopServer() })
                GameButton("MSG ALL", onClick = { state.openMessageAll() })
                GameButton("CHANGE MAP", onClick = { state.pickAndChangeMap() })
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Config form (left panel)
// ---------------------------------------------------------------------------

@Composable
private fun ConfigForm(state: ServerDashboardStateHolder) {
    val running = state.isRunning

    LabeledField("Port", state.editPort, enabled = !running) { state.editPort = it }
    LabeledField("Max players", state.editMaxPlayers, enabled = !running) { state.editMaxPlayers = it }
    Column {
        if (!running) {
            GameButton("BROWSE MAP…", onClick = { state.pickMapPath() })
            Spacer(Modifier.height(4.dp))
        }
        LabeledField("Map path", state.editMapPath, enabled = !running) { state.editMapPath = it }
    }
    LabeledField("Server name", state.editServerName, enabled = !running) { state.editServerName = it }
    LabeledField("Welcome msg", state.editWelcomeMessage, enabled = !running) { state.editWelcomeMessage = it }
    CheckboxRow("Team play", state.editTeamPlay, enabled = !running) { state.editTeamPlay = it }
    CheckboxRow("Allow robots", state.editAllowRobots, enabled = !running) { state.editAllowRobots = it }

    state.errorMessage?.let { err ->
        Spacer(Modifier.height(4.dp))
        Text(
            err,
            style = TextStyle(color = KXPilotColors.Danger, fontSize = 10.sp, fontFamily = FontFamily.Monospace),
        )
    }
}

// ---------------------------------------------------------------------------
// Metrics tiles
// ---------------------------------------------------------------------------

@Composable
private fun MetricsTiles(state: ServerDashboardStateHolder) {
    val m = state.metrics
    val uptimeStr = remember(m.uptimeMs) { formatUptime(m.uptimeMs) }
    val tickStr =
        remember(m.tickRateActual, m.tickRateTarget) {
            "${m.tickRateActual.roundOne()} / ${m.tickRateTarget} Hz"
        }

    MetricTile("Uptime", uptimeStr)
    MetricTile("Tick rate", tickStr)
    MetricTile("Players", m.playerCount.toString())
    MetricTile("BW in", "${m.bandwidthInBps} B/s")
    MetricTile("BW out", "${m.bandwidthOutBps} B/s")
    val cpuStr = remember(m.cpuPercent) { if (m.cpuPercent < 0) "—" else "${m.cpuPercent.roundOne()}%" }
    val heapStr = if (m.heapUsedMb < 0) "—" else "${m.heapUsedMb} MB"
    MetricTile("CPU / Heap", "$cpuStr  $heapStr")
}

// ---------------------------------------------------------------------------
// Player table (right column, top)
// ---------------------------------------------------------------------------

@Composable
private fun PlayerTable(state: ServerDashboardStateHolder) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(KXPilotColors.SurfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            PlayerColHeader("Name", Modifier.weight(2f))
            PlayerColHeader("Team", Modifier.width(40.dp))
            PlayerColHeader("Score", Modifier.width(50.dp))
            PlayerColHeader("Ping", Modifier.width(50.dp))
            PlayerColHeader("Addr", Modifier.weight(1.5f))
            PlayerColHeader("Actions", Modifier.width(180.dp))
        }

        if (!state.isRunning) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Server not running",
                    style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                )
            }
        } else if (state.players.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No players connected",
                    style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.players, key = { it.id }) { row ->
                    PlayerRow(row = row, state = state)
                }
            }
        }
    }
}

@Composable
private fun PlayerRow(
    row: DashboardPlayerRow,
    state: ServerDashboardStateHolder,
) {
    val bg =
        if (state.selectedPlayerId == row.id) KXPilotColors.Surface else KXPilotColors.Background
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(bg)
                .clickable { state.selectedPlayerId = if (state.selectedPlayerId == row.id) null else row.id }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val nameColor = if (row.isMuted) KXPilotColors.OnSurfaceDim else KXPilotColors.OnSurface
        PlayerCell(row.name, Modifier.weight(2f), color = nameColor)
        PlayerCell(row.team, Modifier.width(40.dp))
        PlayerCell(row.score, Modifier.width(50.dp))
        PlayerCell(row.ping, Modifier.width(50.dp))
        PlayerCell(row.address, Modifier.weight(1.5f))

        // Action buttons
        Row(modifier = Modifier.width(180.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SmallButton(if (row.isMuted) "UNMUTE" else "MUTE") { state.mutePlayer(row.id) }
            SmallButton("MSG") { state.openMessageOne(row.id, row.name) }
            SmallDangerButton("KICK") { state.openConfirmKick(row.id, row.name) }
        }
    }
}

// ---------------------------------------------------------------------------
// Event log (right column, bottom)
// ---------------------------------------------------------------------------

@Composable
private fun EventLog(events: List<ServerEvent>) {
    val listState = rememberLazyListState()

    // Auto-scroll to newest entry
    LaunchedEffect(events.size) {
        if (events.isNotEmpty()) listState.animateScrollToItem(events.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(KXPilotColors.SurfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                "Event log",
                style =
                    TextStyle(
                        color = KXPilotColors.OnSurfaceDim,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    ),
            )
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
            items(events) { evt ->
                val color =
                    when (evt.level) {
                        ServerEventLevel.INFO -> KXPilotColors.OnSurface
                        ServerEventLevel.WARN -> KXPilotColors.Accent
                        ServerEventLevel.ERROR -> KXPilotColors.Danger
                    }
                val ts = formatUptime(evt.uptimeMs)
                Text(
                    "[$ts] ${evt.message}",
                    style = TextStyle(color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Dialog composables
// ---------------------------------------------------------------------------

@Composable
private fun SimpleInputDialog(
    title: String,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    DialogOverlay {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DialogTitle(title)
            Text(label, style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = KXPilotColors.OnSurface, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(KXPilotColors.Accent),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(KXPilotColors.Surface)
                        .border(1.dp, KXPilotColors.Accent)
                        .padding(8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameButton("OK", onClick = onConfirm)
                GameButton("CANCEL", onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    DialogOverlay {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DialogTitle(title)
            Text(body, style = TextStyle(color = KXPilotColors.OnSurface, fontSize = 12.sp, fontFamily = FontFamily.Monospace))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameButtonDanger(confirmLabel, onClick = onConfirm)
                GameButton("CANCEL", onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun DialogOverlay(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(KXPilotColors.Background.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .width(360.dp)
                    .background(KXPilotColors.SurfaceVariant)
                    .border(1.dp, KXPilotColors.Accent)
                    .padding(20.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun DialogTitle(text: String) {
    Text(
        text,
        style =
            TextStyle(
                color = KXPilotColors.AccentBright,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            ),
    )
}

// ---------------------------------------------------------------------------
// Small reusable atoms
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        style =
            TextStyle(
                color = KXPilotColors.Accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            ),
    )
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    Column {
        Text(label, style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace))
        BasicTextField(
            value = value,
            onValueChange = { if (enabled) onValueChange(it) },
            singleLine = true,
            enabled = enabled,
            textStyle =
                TextStyle(
                    color = if (enabled) KXPilotColors.OnSurface else KXPilotColors.OnSurfaceDim,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                ),
            cursorBrush = SolidColor(KXPilotColors.Accent),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(KXPilotColors.Surface)
                    .border(1.dp, if (enabled) KXPilotColors.Accent else KXPilotColors.OnSurfaceDim)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun CheckboxRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
                .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .width(14.dp)
                    .height(14.dp)
                    .border(1.dp, if (enabled) KXPilotColors.Accent else KXPilotColors.OnSurfaceDim)
                    .background(if (checked) KXPilotColors.Accent else KXPilotColors.Surface),
        )
        Text(
            label,
            style =
                TextStyle(
                    color = if (enabled) KXPilotColors.OnSurface else KXPilotColors.OnSurfaceDim,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                ),
        )
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(KXPilotColors.Surface)
                .border(1.dp, KXPilotColors.SurfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace))
        Text(value, style = TextStyle(color = KXPilotColors.OnSurface, fontSize = 11.sp, fontFamily = FontFamily.Monospace))
    }
}

@Composable
private fun RowScope.PlayerColHeader(
    text: String,
    modifier: Modifier,
) {
    Text(
        text,
        style =
            TextStyle(
                color = KXPilotColors.OnSurfaceDim,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            ),
        modifier = modifier,
    )
}

@Composable
private fun RowScope.PlayerCell(
    text: String,
    modifier: Modifier,
    color: androidx.compose.ui.graphics.Color = KXPilotColors.OnSurface,
) {
    Text(
        text,
        style = TextStyle(color = color, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
        modifier = modifier,
        maxLines = 1,
    )
}

@Composable
private fun SmallButton(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .background(KXPilotColors.Surface)
                .border(1.dp, KXPilotColors.Accent)
                .clickable(onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(label, style = TextStyle(color = KXPilotColors.Accent, fontSize = 9.sp, fontFamily = FontFamily.Monospace))
    }
}

@Composable
private fun SmallDangerButton(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .background(KXPilotColors.Surface)
                .border(1.dp, KXPilotColors.Danger)
                .clickable(onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(label, style = TextStyle(color = KXPilotColors.Danger, fontSize = 9.sp, fontFamily = FontFamily.Monospace))
    }
}

// ---------------------------------------------------------------------------
// Private formatting helpers — REMOVED; now in ui.util.UiFormatting
// ---------------------------------------------------------------------------
