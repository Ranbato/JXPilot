package org.lambertland.kxpilot.net

import kotlin.test.Test
import kotlin.test.assertEquals

/** S4 — verify that SELF_ITEMS and MODIFIERS are public in PktType with correct wire values. */
class PktTypeConstantsTest {
    @Test
    fun `PktType SELF_ITEMS equals 11`() {
        assertEquals(11, PktType.SELF_ITEMS)
    }

    @Test
    fun `PktType MODIFIERS equals 70`() {
        assertEquals(70, PktType.MODIFIERS)
    }
}
