package org.lambertland.kxpilot.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

// ---------------------------------------------------------------------------
// Tests for PlayerIdentity extraction (D1)
// ---------------------------------------------------------------------------

class PlayerIdentityTest {
    @Test
    fun `PlayerIdentity holds default values`() {
        val id = PlayerIdentity()
        assertEquals(' ', id.myChar)
        assertEquals("", id.name)
        assertEquals("", id.userName)
        assertEquals("", id.hostname)
        assertEquals(0u, id.pseudoTeam)
        assertEquals(0, id.alliance)
        assertEquals(0, id.invite)
        assertEquals(PlayerType.HUMAN, id.plType)
        assertEquals(' ', id.plTypeMyChar)
        assertEquals(0u, id.version)
    }

    @Test
    fun `Player exposes identity field`() {
        val player = Player()
        assertNotNull(player.identity)
    }

    @Test
    fun `Player delegating properties read and write through identity`() {
        val player = Player()
        player.name = "Alice"
        player.userName = "alice"
        player.hostname = "localhost"
        player.myChar = 'A'
        player.plType = PlayerType.ROBOT
        player.version = 42u
        player.muted = true
        player.privs = 3

        // Verify via identity object
        assertEquals("Alice", player.identity.name)
        assertEquals("alice", player.identity.userName)
        assertEquals("localhost", player.identity.hostname)
        assertEquals('A', player.identity.myChar)
        assertEquals(PlayerType.ROBOT, player.identity.plType)
        assertEquals(42u, player.identity.version)
        assertEquals(true, player.identity.muted)
        assertEquals(3, player.identity.privs)

        // Verify via delegating properties
        assertEquals("Alice", player.name)
        assertEquals(PlayerType.ROBOT, player.plType)
        assertEquals(42u, player.version)
    }

    @Test
    fun `Player reset clears identity fields`() {
        val player = Player()
        player.name = "Bob"
        player.userName = "bob"
        player.hostname = "example.com"
        player.plType = PlayerType.TANK
        player.version = 99u
        player.muted = true
        player.isOperator = true

        player.reset()

        assertEquals("", player.name)
        assertEquals("", player.userName)
        assertEquals("", player.hostname)
        assertEquals(PlayerType.HUMAN, player.plType)
        assertEquals(0u, player.version)
        assertEquals(false, player.muted)
        assertEquals(false, player.isOperator)
    }

    @Test
    fun `identity mutation is reflected in Player delegating properties`() {
        val player = Player()
        player.identity.name = "Charlie"
        player.identity.alliance = 7
        player.identity.pseudoTeam = 3u

        assertEquals("Charlie", player.name)
        assertEquals(7, player.alliance)
        assertEquals(3u, player.pseudoTeam)
    }
}
