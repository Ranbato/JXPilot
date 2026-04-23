package org.lambertland.kxpilot

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.lambertland.kxpilot.config.AppConfig
import org.lambertland.kxpilot.model.deserializeBindings
import org.lambertland.kxpilot.resources.ShipShapeDef
import org.lambertland.kxpilot.resources.parseShipShapes
import org.lambertland.kxpilot.server.ServerController
import org.lambertland.kxpilot.ui.App
import org.lambertland.kxpilot.ui.Navigator
import java.io.File

private val rcFile = File(System.getProperty("user.home"), ".kxpilotrc")
private val keysFile = File(System.getProperty("user.home"), ".kxpilotrc.keys")

private fun loadShipShapes(): List<ShipShapeDef> =
    try {
        val stream = object {}.javaClass.getResourceAsStream("/data/shipshapes.json")
        if (stream != null) {
            parseShipShapes(stream.bufferedReader().readText())
        } else {
            emptyList()
        }
    } catch (_: Exception) {
        emptyList()
    }

fun main() =
    application {
        val navigator = Navigator(onExitRequested = ::exitApplication)
        val windowState = rememberWindowState(size = DpSize(1024.dp, 768.dp))

        val config = AppConfig.load(rcFile.takeIf { it.exists() }?.readText())
        val allShapes = loadShipShapes()
        val initialBindings =
            deserializeBindings(
                keysFile.takeIf { it.exists() }?.readText() ?: "",
            )

        // Application-lifetime scope — survives navigation and composition changes.
        // SupervisorJob ensures a child coroutine failure (e.g. a bad packet) does
        // not tear down the whole server.
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val serverController = ServerController(appScope)

        Window(
            onCloseRequest = ::exitApplication,
            title = "KXPilot",
            state = windowState,
        ) {
            App(
                navigator = navigator,
                config = config,
                serverController = serverController,
                onSaveConfig = { text -> rcFile.writeText(text) },
                onSaveBindings = { text -> keysFile.writeText(text) },
                availableShips = allShapes,
                initialKeyBindings = initialBindings,
            )
        }
    }
