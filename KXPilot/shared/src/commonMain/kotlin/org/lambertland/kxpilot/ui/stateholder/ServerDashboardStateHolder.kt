package org.lambertland.kxpilot.ui.stateholder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.lambertland.kxpilot.model.DashboardPlayerRow
import org.lambertland.kxpilot.model.ServerEvent
import org.lambertland.kxpilot.platform.showFilePicker
import org.lambertland.kxpilot.server.ServerConfig
import org.lambertland.kxpilot.server.ServerController
import org.lambertland.kxpilot.server.ServerMetrics
import org.lambertland.kxpilot.server.ServerState

// ---------------------------------------------------------------------------
// Dialog state
// ---------------------------------------------------------------------------

/** Which (if any) modal dialog is open on the dashboard. */
sealed class DashboardDialog {
    object None : DashboardDialog()

    object MessageAll : DashboardDialog()

    data class MessageOne(
        val playerId: Int,
        val playerName: String,
    ) : DashboardDialog()

    data class ConfirmKick(
        val playerId: Int,
        val playerName: String,
    ) : DashboardDialog()

    object ChangeMap : DashboardDialog()
}

// ---------------------------------------------------------------------------
// State holder
// ---------------------------------------------------------------------------

/**
 * UI-facing state holder for [ServerDashboardScreen].
 *
 * Observes [ServerController.state] and projects it into Compose-observable
 * properties.  Never references any Compose APIs beyond [mutableStateOf] so
 * it can be unit-tested without a running composition.
 *
 * @param controller  The shared [ServerController] instance.
 * @param scope       Coroutine scope for collecting the state flow.
 */
class ServerDashboardStateHolder(
    private val controller: ServerController,
    private val scope: CoroutineScope,
) {
    // -----------------------------------------------------------------------
    // Observed server state (projected for UI consumption)
    // -----------------------------------------------------------------------

    var serverState: ServerState by mutableStateOf(ServerState.Stopped)
        private set

    val isRunning: Boolean get() = serverState is ServerState.Running
    val isStopped: Boolean get() = serverState is ServerState.Stopped
    val isStarting: Boolean get() = serverState is ServerState.Starting
    val errorMessage: String? get() = (serverState as? ServerState.Error)?.message

    val metrics: ServerMetrics get() = (serverState as? ServerState.Running)?.metrics ?: ServerMetrics.EMPTY

    val players: List<DashboardPlayerRow>
        get() =
            (serverState as? ServerState.Running)
                ?.players
                ?.map { DashboardPlayerRow.from(it) }
                ?: emptyList()

    val events: List<ServerEvent>
        get() = (serverState as? ServerState.Running)?.events ?: emptyList()

    // -----------------------------------------------------------------------
    // Editable config fields (bound to form inputs on the dashboard)
    // -----------------------------------------------------------------------

    var editPort: String by mutableStateOf(ServerConfig.DEFAULT_PORT.toString())
    var editMaxPlayers: String by mutableStateOf("16")
    var editMapPath: String by mutableStateOf("")
    var editServerName: String by mutableStateOf("KXPilot Server")
    var editWelcomeMessage: String by mutableStateOf("Welcome to KXPilot!")
    var editTeamPlay: Boolean by mutableStateOf(false)
    var editAllowRobots: Boolean by mutableStateOf(true)

    // -----------------------------------------------------------------------
    // Dialog state
    // -----------------------------------------------------------------------

    var dialog: DashboardDialog by mutableStateOf(DashboardDialog.None)
        private set

    var dialogInput: String by mutableStateOf("")

    // -----------------------------------------------------------------------
    // Selected player (for per-player action buttons)
    // -----------------------------------------------------------------------

    var selectedPlayerId: Int? by mutableStateOf(null)

    // -----------------------------------------------------------------------
    // Init — collect server state flow
    // -----------------------------------------------------------------------

    init {
        controller.state
            .onEach { serverState = it }
            .launchIn(scope)
    }

    // -----------------------------------------------------------------------
    // Commands delegated to ServerController
    // -----------------------------------------------------------------------

    /** Start the server using the current edit-field values.  */
    fun startServer() {
        val config =
            ServerConfig(
                port = editPort.toIntOrNull()?.coerceIn(1, 65535) ?: ServerConfig.DEFAULT_PORT,
                maxPlayers = editMaxPlayers.toIntOrNull()?.coerceIn(1, 256) ?: 16,
                mapPath = editMapPath.ifBlank { null },
                serverName = editServerName,
                welcomeMessage = editWelcomeMessage,
                teamPlay = editTeamPlay,
                allowRobots = editAllowRobots,
            )
        controller.start(config)
    }

    fun stopServer() = controller.stop()

    fun kickPlayer(id: Int) = controller.kickPlayer(id)

    fun mutePlayer(id: Int) = controller.mutePlayer(id)

    fun sendMessageAll(message: String) = controller.sendMessageAll(message)

    fun sendMessageOne(
        id: Int,
        message: String,
    ) = controller.sendMessageOne(id, message)

    fun changeMap(path: String) {
        scope.launch { controller.changeMap(path) }
    }

    /**
     * Show a native file picker and, if the user selects a file, change the map.
     * No-op if the server is not running.
     */
    fun pickAndChangeMap() {
        scope.launch {
            val path = showFilePicker(title = "Select Map File", extension = "")
            if (path != null) controller.changeMap(path)
        }
    }

    /**
     * Show a native file picker and populate [editMapPath] with the chosen file.
     * No-op if the server is running (map path is locked while running).
     */
    fun pickMapPath() {
        if (isRunning) return
        scope.launch {
            val path = showFilePicker(title = "Select Map File", extension = "")
            if (path != null) editMapPath = path
        }
    }

    // -----------------------------------------------------------------------
    // Dialog helpers
    // -----------------------------------------------------------------------

    fun openMessageAll() {
        dialogInput = ""
        dialog = DashboardDialog.MessageAll
    }

    fun openMessageOne(
        id: Int,
        name: String,
    ) {
        dialogInput = ""
        dialog = DashboardDialog.MessageOne(id, name)
    }

    fun openConfirmKick(
        id: Int,
        name: String,
    ) {
        dialog = DashboardDialog.ConfirmKick(id, name)
    }

    fun openChangeMap() {
        dialogInput = (serverState as? ServerState.Running)?.config?.mapPath ?: ""
        dialog = DashboardDialog.ChangeMap
    }

    fun dismissDialog() {
        dialog = DashboardDialog.None
        dialogInput = ""
    }

    fun confirmDialog() {
        when (val d = dialog) {
            is DashboardDialog.MessageAll -> sendMessageAll(dialogInput)
            is DashboardDialog.MessageOne -> sendMessageOne(d.playerId, dialogInput)
            is DashboardDialog.ConfirmKick -> kickPlayer(d.playerId)
            is DashboardDialog.ChangeMap -> changeMap(dialogInput)
            DashboardDialog.None -> Unit
        }
        dismissDialog()
    }
}
