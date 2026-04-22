package org.lambertland.kxpilot

import org.lambertland.kxpilot.resources.MiniJsonParser
import org.lambertland.kxpilot.resources.parseShipShapes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MiniJsonParserTest {
    // -----------------------------------------------------------------------
    // Primitives
    // -----------------------------------------------------------------------

    @Test fun parsesTrue() {
        assertEquals(true, MiniJsonParser("true").parseValue())
    }

    @Test fun parsesFalse() {
        assertEquals(false, MiniJsonParser("false").parseValue())
    }

    @Test fun parsesNull() {
        assertNull(MiniJsonParser("null").parseValue())
    }

    @Test fun parsesInteger() {
        assertEquals(42L, MiniJsonParser("42").parseValue())
    }

    @Test fun parsesNegativeInteger() {
        assertEquals(-7L, MiniJsonParser("-7").parseValue())
    }

    @Test fun parsesFloat() {
        val v = MiniJsonParser("3.14").parseValue()
        assertTrue(v is Double)
        assertEquals(3.14, v as Double, 1e-10)
    }

    @Test fun parsesExponentFloat() {
        val v = MiniJsonParser("1e3").parseValue() as Double
        assertEquals(1000.0, v, 1e-10)
    }

    @Test fun parsesString() {
        assertEquals("hello", MiniJsonParser("\"hello\"").parseValue())
    }

    @Test fun parsesEscapedQuoteInString() {
        assertEquals("a\"b", MiniJsonParser("\"a\\\"b\"").parseValue())
    }

    @Test fun parsesEscapedBackslashInString() {
        assertEquals("a\\b", MiniJsonParser("\"a\\\\b\"").parseValue())
    }

    @Test fun parsesEscapedNewlineInString() {
        assertEquals("a\nb", MiniJsonParser("\"a\\nb\"").parseValue())
    }

    // -----------------------------------------------------------------------
    // Arrays
    // -----------------------------------------------------------------------

    @Test fun parsesEmptyArray() {
        val result = MiniJsonParser("[]").parseArray()
        assertTrue(result.isEmpty())
    }

    @Test fun parsesSingleElementArray() {
        val result = MiniJsonParser("[1]").parseArray()
        assertEquals(listOf(1L), result)
    }

    @Test fun parsesIntArray() {
        val result = MiniJsonParser("[1, 2, 3]").parseArray()
        assertEquals(listOf(1L, 2L, 3L), result)
    }

    @Test fun parsesNestedArray() {
        val result = MiniJsonParser("[[1, 2], [3, 4]]").parseArray()
        assertEquals(2, result.size)
        assertEquals(listOf(1L, 2L), result[0])
        assertEquals(listOf(3L, 4L), result[1])
    }

    @Test fun parsesArrayWithWhitespace() {
        val result = MiniJsonParser("[ 1 , 2 , 3 ]").parseArray()
        assertEquals(listOf(1L, 2L, 3L), result)
    }

    // -----------------------------------------------------------------------
    // Objects
    // -----------------------------------------------------------------------

    @Test fun parsesEmptyObject() {
        val result = MiniJsonParser("{}").parseValue()
        assertTrue(result is Map<*, *>)
        assertTrue((result as Map<*, *>).isEmpty())
    }

    @Test fun parsesSimpleObject() {
        @Suppress("UNCHECKED_CAST")
        val result = MiniJsonParser("""{"x": 1, "y": 2}""").parseValue() as Map<String, Any?>
        assertEquals(1L, result["x"])
        assertEquals(2L, result["y"])
    }

    @Test fun parsesObjectWithStringValue() {
        @Suppress("UNCHECKED_CAST")
        val result = MiniJsonParser("""{"name": "Alice"}""").parseValue() as Map<String, Any?>
        assertEquals("Alice", result["name"])
    }

    // -----------------------------------------------------------------------
    // Error cases
    // -----------------------------------------------------------------------

    @Test fun unterminatedArrayReturnsPartialResult() {
        // The parser is lenient — unterminated arrays return whatever was parsed so far
        val result = MiniJsonParser("[1, 2").parseArray()
        assertEquals(listOf(1L, 2L), result)
    }

    @Test fun throwsOnExpectMismatch() {
        assertFailsWith<IllegalArgumentException> {
            MiniJsonParser("1").parseArray() // expects '[' but finds '1'
        }
    }

    @Test fun throwsOnInvalidNumber() {
        assertFailsWith<IllegalArgumentException> {
            MiniJsonParser("1.2.3").parseValue()
        }
    }

    // -----------------------------------------------------------------------
    // parseShipShapes integration
    // -----------------------------------------------------------------------

    @Test fun parseShipShapesReturnsEmptyOnEmptyArray() {
        val result = parseShipShapes("[]")
        assertTrue(result.isEmpty())
    }

    @Test fun parseShipShapesSkipsShapeWithNoHull() {
        val json = """[{"name":"A","author":"x","hull":[]}]"""
        val result = parseShipShapes(json)
        assertTrue(result.isEmpty())
    }

    @Test fun parseShipShapesParsesSingleShape() {
        val json =
            """
            [{
                "name": "Triangle",
                "author": "Bob",
                "hull": [[0,10],[-8,-5],[8,-5]]
            }]
            """.trimIndent()
        val result = parseShipShapes(json)
        assertEquals(1, result.size)
        val shape = result[0]
        assertEquals("Triangle", shape.name)
        assertEquals("Bob", shape.author)
        assertEquals(3, shape.hull.size)
        assertEquals(0 to 10, shape.hull[0])
        assertEquals(-8 to -5, shape.hull[1])
        assertEquals(8 to -5, shape.hull[2])
    }

    @Test fun parseShipShapesHandlesOptionalFields() {
        val json =
            """
            [{
                "name": "S",
                "author": "A",
                "hull": [[0,5],[-3,-3],[3,-3]],
                "engine": [0, -3],
                "mainGun": [0, 5],
                "leftLight": [[-3, 0]],
                "missileRack": [[2, 1], [-2, 1]]
            }]
            """.trimIndent()
        val result = parseShipShapes(json)
        assertEquals(1, result.size)
        val s = result[0]
        assertEquals(0 to -3, s.engine)
        assertEquals(0 to 5, s.mainGun)
        assertEquals(1, s.leftLight.size)
        assertEquals(-3 to 0, s.leftLight[0])
        assertEquals(2, s.missileRack.size)
    }

    @Test fun parseShipShapesStructuralEquality() {
        val json =
            """
            [{
                "name": "X",
                "author": "Y",
                "hull": [[0,1],[1,0]]
            }]
            """.trimIndent()
        val a = parseShipShapes(json)[0]
        val b = parseShipShapes(json)[0]
        assertEquals(a, b) // Pair-based hull: structural equality works
    }
}
