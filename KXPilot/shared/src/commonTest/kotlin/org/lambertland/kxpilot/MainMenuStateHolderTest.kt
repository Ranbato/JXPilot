package org.lambertland.kxpilot

import org.lambertland.kxpilot.model.ServerBrowserState
import org.lambertland.kxpilot.server.ServerConfig
import org.lambertland.kxpilot.ui.Navigator
import org.lambertland.kxpilot.ui.Screen
import org.lambertland.kxpilot.ui.screens.MainMenuStateHolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// MainMenuStateHolderTest
// ---------------------------------------------------------------------------
// Covers state transitions, validation logic, and navigation calls for
// MainMenuStateHolder.  No Compose runtime required — all observable state
// is plain Kotlin (mutableStateOf delegates are read directly).
// ---------------------------------------------------------------------------

class MainMenuStateHolderTest {
    private fun makeHolder(): Pair<MainMenuStateHolder, Navigator> {
        val nav = Navigator()
        return MainMenuStateHolder(nav) to nav
    }

    // -----------------------------------------------------------------------
    // scanLocal
    // -----------------------------------------------------------------------

    @Test
    fun scanLocal_transitionsToConnectLocalState() {
        val (holder, _) = makeHolder()
        holder.scanLocal()
        assertIs<ServerBrowserState.ConnectLocal>(holder.browserState)
    }

    @Test
    fun scanLocal_stubLeavesScanning_false() {
        val (holder, _) = makeHolder()
        holder.scanLocal()
        val bs = holder.browserState as ServerBrowserState.ConnectLocal
        assertFalse(bs.scanning, "Stub scanLocal() should settle with scanning=false")
    }

    @Test
    fun scanLocal_stubLeavesNoLocalServer() {
        val (holder, _) = makeHolder()
        holder.scanLocal()
        val bs = holder.browserState as ServerBrowserState.ConnectLocal
        assertNull(bs.localServer, "Stub scanLocal() should produce no localServer")
    }

    @Test
    fun scanLocal_doesNotPassThroughScanningState() {
        // Regression: previous implementation set scanning=true then immediately
        // scanning=false in the same call.  The Scanning frame was never rendered.
        // Verify the state is ConnectLocal, never Scanning, at the end of the call.
        val (holder, _) = makeHolder()
        holder.scanLocal()
        assertIs<ServerBrowserState.ConnectLocal>(holder.browserState)
    }

    // -----------------------------------------------------------------------
    // directHost / directPort state
    // -----------------------------------------------------------------------

    @Test
    fun directHost_initiallyEmpty() {
        val (holder, _) = makeHolder()
        assertEquals("", holder.directHost)
    }

    @Test
    fun directPort_initiallyDefaultPort() {
        val (holder, _) = makeHolder()
        assertEquals(ServerConfig.DEFAULT_PORT.toString(), holder.directPort)
    }

    @Test
    fun directHost_canBeUpdatedDirectly() {
        val (holder, _) = makeHolder()
        holder.directHost = "192.168.1.5"
        assertEquals("192.168.1.5", holder.directHost)
    }

    @Test
    fun directPort_canBeUpdatedDirectly() {
        val (holder, _) = makeHolder()
        holder.directPort = "9999"
        assertEquals("9999", holder.directPort)
    }

    @Test
    fun directHost_updateDoesNotDependOnBrowserState() {
        // directHost lives in the state holder — updates work in any browser state.
        val (holder, _) = makeHolder()
        // browserState starts as Idle, not ConnectLocal
        holder.directHost = "myserver.local"
        assertEquals("myserver.local", holder.directHost)
    }

    // -----------------------------------------------------------------------
    // directPortInt validation
    // -----------------------------------------------------------------------

    @Test
    fun directPortInt_parsesValidPort() {
        val (holder, _) = makeHolder()
        holder.directPort = "15345"
        assertEquals(15345, holder.directPortInt)
    }

    @Test
    fun directPortInt_nullForNonNumeric() {
        val (holder, _) = makeHolder()
        holder.directPort = "abc"
        assertNull(holder.directPortInt)
    }

    @Test
    fun directPortInt_nullForPortZero() {
        val (holder, _) = makeHolder()
        holder.directPort = "0"
        assertNull(holder.directPortInt, "Port 0 is out of valid range 1..65535")
    }

    @Test
    fun directPortInt_nullForPortAbove65535() {
        val (holder, _) = makeHolder()
        holder.directPort = "65536"
        assertNull(holder.directPortInt, "Port 65536 is out of valid range 1..65535")
    }

    @Test
    fun directPortInt_validForPort1() {
        val (holder, _) = makeHolder()
        holder.directPort = "1"
        assertEquals(1, holder.directPortInt)
    }

    @Test
    fun directPortInt_validForPort65535() {
        val (holder, _) = makeHolder()
        holder.directPort = "65535"
        assertEquals(65535, holder.directPortInt)
    }

    @Test
    fun directPortInt_nullForEmptyString() {
        val (holder, _) = makeHolder()
        holder.directPort = ""
        assertNull(holder.directPortInt)
    }

    // -----------------------------------------------------------------------
    // canConnectDirect
    // -----------------------------------------------------------------------

    @Test
    fun canConnectDirect_falseWhenHostEmpty() {
        val (holder, _) = makeHolder()
        holder.directHost = ""
        holder.directPort = "15345"
        assertFalse(holder.canConnectDirect)
    }

    @Test
    fun canConnectDirect_falseWhenPortInvalid() {
        val (holder, _) = makeHolder()
        holder.directHost = "myserver"
        holder.directPort = "notaport"
        assertFalse(holder.canConnectDirect)
    }

    @Test
    fun canConnectDirect_falseWhenPortOutOfRange() {
        val (holder, _) = makeHolder()
        holder.directHost = "myserver"
        holder.directPort = "99999"
        assertFalse(holder.canConnectDirect)
    }

    @Test
    fun canConnectDirect_trueWhenHostAndPortValid() {
        val (holder, _) = makeHolder()
        holder.directHost = "myserver.local"
        holder.directPort = "15345"
        assertTrue(holder.canConnectDirect)
    }

    @Test
    fun canConnectDirect_trueForBoundaryPort1() {
        val (holder, _) = makeHolder()
        holder.directHost = "host"
        holder.directPort = "1"
        assertTrue(holder.canConnectDirect)
    }

    @Test
    fun canConnectDirect_trueForBoundaryPort65535() {
        val (holder, _) = makeHolder()
        holder.directHost = "host"
        holder.directPort = "65535"
        assertTrue(holder.canConnectDirect)
    }

    @Test
    fun canConnectDirect_falseForBlankHost() {
        val (holder, _) = makeHolder()
        holder.directHost = "   "
        holder.directPort = "15345"
        assertFalse(holder.canConnectDirect, "Whitespace-only host should be rejected")
    }

    // -----------------------------------------------------------------------
    // join — navigation
    // -----------------------------------------------------------------------

    @Test
    fun join_pushesInGameScreen() {
        val (holder, nav) = makeHolder()
        holder.join("myserver.local", 15345)
        assertIs<Screen.InGame>(nav.current.value)
    }

    @Test
    fun join_pushesCorrctHost() {
        val (holder, nav) = makeHolder()
        holder.join("myserver.local", 15345)
        val screen = nav.current.value as Screen.InGame
        assertEquals("myserver.local", screen.serverHost)
    }

    @Test
    fun join_pushesCorrectPort() {
        val (holder, nav) = makeHolder()
        holder.join("myserver.local", 9999)
        val screen = nav.current.value as Screen.InGame
        assertEquals(9999, screen.serverPort)
    }

    @Test
    fun join_defaultPortMatchesServerConfigDefaultPort() {
        val (holder, nav) = makeHolder()
        holder.join("host")
        val screen = nav.current.value as Screen.InGame
        assertEquals(ServerConfig.DEFAULT_PORT, screen.serverPort)
    }

    // -----------------------------------------------------------------------
    // Screen.InGame default port
    // -----------------------------------------------------------------------

    @Test
    fun screenInGame_defaultPortMatchesServerConfigDefaultPort() {
        val screen = Screen.InGame("host")
        assertEquals(ServerConfig.DEFAULT_PORT, screen.serverPort)
    }

    // -----------------------------------------------------------------------
    // fetchInternet
    // -----------------------------------------------------------------------

    @Test
    fun fetchInternet_producesLoadedState() {
        val (holder, _) = makeHolder()
        holder.fetchInternet()
        assertIs<ServerBrowserState.Loaded>(holder.browserState)
    }

    @Test
    fun fetchInternet_loadedStateContainsServers() {
        val (holder, _) = makeHolder()
        holder.fetchInternet()
        val loaded = holder.browserState as ServerBrowserState.Loaded
        assertTrue(loaded.servers.isNotEmpty(), "Stub internet fetch should return at least one server")
    }

    // -----------------------------------------------------------------------
    // selectServer
    // -----------------------------------------------------------------------

    @Test
    fun selectServer_producesDetailState() {
        val (holder, _) = makeHolder()
        holder.fetchInternet()
        val loaded = holder.browserState as ServerBrowserState.Loaded
        holder.selectServer(loaded.servers.first())
        assertIs<ServerBrowserState.Detail>(holder.browserState)
    }

    @Test
    fun selectServer_detailContainsCorrectServer() {
        val (holder, _) = makeHolder()
        holder.fetchInternet()
        val loaded = holder.browserState as ServerBrowserState.Loaded
        val target = loaded.servers.first()
        holder.selectServer(target)
        val detail = holder.browserState as ServerBrowserState.Detail
        assertEquals(target.host, detail.server.host)
    }
}
