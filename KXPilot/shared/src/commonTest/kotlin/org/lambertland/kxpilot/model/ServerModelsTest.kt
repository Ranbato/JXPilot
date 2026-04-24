package org.lambertland.kxpilot.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ServerModelsTest {
    @Test
    fun serverInfoDefaultSourceIsInternet() {
        val s = ServerInfo("host", 15345, "map", 0, 0, 8, 30, "4.7.3", null, "running")
        assertEquals(ServerSource.INTERNET, s.source)
    }

    @Test
    fun serverInfoLocalSource() {
        val s = ServerInfo("127.0.0.1", 15345, "map", 0, 0, 8, 30, "4.7.3", 0, "running", source = ServerSource.LOCAL)
        assertEquals(ServerSource.LOCAL, s.source)
    }
}
