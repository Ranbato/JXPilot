package org.lambertland.kxpilot.net

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/** N1 — verify fetchMetaserverList() does not throw and returns a non-null list. */
class MetaserverClientTest {
    @Test
    fun `fetchMetaserverList returns a non-null list without throwing`() =
        runTest {
            val result = fetchMetaserverList()
            assertNotNull(result)
            // Result may be empty (android/wasm) or non-empty (desktop stub) — both are valid.
        }
}
