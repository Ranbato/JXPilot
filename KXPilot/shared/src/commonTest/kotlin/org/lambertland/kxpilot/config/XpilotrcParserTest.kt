package org.lambertland.kxpilot.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class XpilotrcParserTest {
    private val defs = XpOptionRegistry.all

    // -------------------------------------------------------------------------
    // Read tests
    // -------------------------------------------------------------------------

    @Test
    fun `read parses double option`() {
        val result = XpilotrcParser.read("xpilot.power : 42.5", defs)
        assertEquals(42.5, result["power"])
    }

    @Test
    fun `read parses bool yes`() {
        val result = XpilotrcParser.read("xpilot.autoShield : yes", defs)
        assertEquals(true, result["autoShield"])
    }

    @Test
    fun `read parses bool no`() {
        val result = XpilotrcParser.read("xpilot.autoShield : no", defs)
        assertEquals(false, result["autoShield"])
    }

    @Test
    fun `read parses bool true and false`() {
        val t = XpilotrcParser.read("xpilot.autoShield : true", defs)
        val f = XpilotrcParser.read("xpilot.toggleShield : false", defs)
        assertEquals(true, t["autoShield"])
        assertEquals(false, f["toggleShield"])
    }

    @Test
    fun `read parses int option`() {
        val result = XpilotrcParser.read("xpilot.maxFPS : 30", defs)
        assertEquals(30, result["maxFPS"])
    }

    @Test
    fun `read parses color index`() {
        val result = XpilotrcParser.read("xpilot.hudColor : 7", defs)
        assertEquals(7, result["hudColor"])
    }

    @Test
    fun `read ignores comment lines`() {
        val text =
            """
            ; xpilot.power : 99.0
            # xpilot.maxFPS : 10
            """.trimIndent()
        val result = XpilotrcParser.read(text, defs)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `read ignores blank lines`() {
        val result = XpilotrcParser.read("  \n\n  ", defs)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `read ignores unknown options`() {
        val result = XpilotrcParser.read("xpilot.nonExistentOption : 123", defs)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `read clamps int to bounds`() {
        val result = XpilotrcParser.read("xpilot.maxFPS : 9999", defs)
        assertEquals(200, result["maxFPS"]) // max is 200
    }

    @Test
    fun `read clamps double to bounds`() {
        val result = XpilotrcParser.read("xpilot.power : 9999.0", defs)
        assertEquals(200.0, result["power"]) // max is 200.0
    }

    @Test
    fun `read clamps color index to 15`() {
        val result = XpilotrcParser.read("xpilot.hudColor : 99", defs)
        assertEquals(15, result["hudColor"])
    }

    @Test
    fun `read is case-insensitive for key`() {
        val result = XpilotrcParser.read("XPILOT.POWER : 10.0", defs)
        assertEquals(10.0, result["power"])
    }

    @Test
    fun `read handles multiple options`() {
        val text =
            """
            xpilot.power : 70.0
            xpilot.turnSpeed : 20.0
            xpilot.showShipShapes : yes
            """.trimIndent()
        val result = XpilotrcParser.read(text, defs)
        assertEquals(70.0, result["power"])
        assertEquals(20.0, result["turnSpeed"])
        assertEquals(true, result["showShipShapes"])
    }

    @Test
    fun `read parses nickName when present`() {
        val result = XpilotrcParser.read("xpilot.nickName : Tester", defs)
        assertEquals("Tester", result["nickName"])
    }

    // -------------------------------------------------------------------------
    // Write tests
    // -------------------------------------------------------------------------

    @Test
    fun `write includes non-default value without comment`() {
        val values = mapOf<XpOptionDef<*>, Any>(XpOptionRegistry.power to 80.0)
        val output = XpilotrcParser.write(defs) { def -> values[def] ?: def.defaultValue!! }
        val line = output.lines().first { it.contains("power") && !it.startsWith(";") }
        assertTrue(line.contains("80.0"), "Expected 80.0 in: $line")
    }

    @Test
    fun `write comments out default value`() {
        val output = XpilotrcParser.write(defs) { def -> def.defaultValue!! }
        val line = output.lines().first { it.contains("power") }
        assertTrue(line.startsWith(";"), "Expected commented line but got: $line")
    }

    @Test
    fun `write includes nickName`() {
        val output = XpilotrcParser.write(defs) { def -> def.defaultValue!! }
        assertTrue(output.contains("nickName"), "Expected nickName in output")
    }

    @Test
    fun `round-trip preserves values`() {
        // Build a set of non-default values
        val overrides: Map<String, Any> =
            mapOf(
                "power" to 90.0,
                "turnSpeed" to 12.0,
                "autoShield" to true,
                "maxFPS" to 45,
                "hudColor" to 5,
            )

        val text =
            XpilotrcParser.write(defs) { def ->
                overrides[def.name] ?: def.defaultValue!!
            }

        val readBack = XpilotrcParser.read(text, defs)
        assertEquals(90.0, readBack["power"])
        assertEquals(12.0, readBack["turnSpeed"])
        assertEquals(true, readBack["autoShield"])
        assertEquals(45, readBack["maxFPS"])
        assertEquals(5, readBack["hudColor"])
    }
}
