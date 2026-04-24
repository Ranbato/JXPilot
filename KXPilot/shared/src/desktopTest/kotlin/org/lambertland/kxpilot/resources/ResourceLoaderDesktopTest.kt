package org.lambertland.kxpilot.resources

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [readResourceText] using the jvmMain actual (classpath-based).
 * Runs in desktopTest where classpath resources are available.
 */
class ResourceLoaderDesktopTest {
    /**
     * R42: assert that a known bundled resource is actually readable and has
     * non-blank content.  "/data/shipshapes.json" is in commonMain/resources and
     * must be on the desktop test classpath.
     */
    @Test
    fun readResourceText_existingResource_returnsNonBlankContent() {
        val text = readResourceText("/data/shipshapes.json")
        assertNotNull(text, "/data/shipshapes.json must be on the test classpath")
        assertTrue(text.isNotBlank(), "/data/shipshapes.json must have non-blank content")
    }

    @Test
    fun readResourceText_knownMapResource_returnsNonBlankContent() {
        val text = readResourceText("/maps/teamcup.xp")
        assertNotNull(text, "/maps/teamcup.xp must be on the test classpath")
        assertTrue(text.isNotBlank(), "/maps/teamcup.xp must have non-blank content")
    }

    @Test
    fun readResourceText_nonExistentPath_returnsNull() {
        val text = readResourceText("/nonexistent/file.txt")
        assertNull(text)
    }
}
