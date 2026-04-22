package org.lambertland.kxpilot.resources

/**
 * Represents a parsed ship shape from shipshapes.json.
 *
 * All point lists use `Pair<Int,Int>` (x, y) in local ship coords
 * (x right, y up; range −15..+15) so that structural equality on
 * `ShipShapeDef` works correctly — unlike `IntArray`, `Pair` participates
 * in `equals`/`hashCode`.
 *
 * [hull]        — polygon vertices
 * [engine]      — single point: engine exhaust origin (optional)
 * [mainGun]     — single point: forward gun muzzle (optional)
 * [leftLight]   — one or more points: left navigation light(s) (optional)
 * [rightLight]  — one or more points: right navigation light(s) (optional)
 * [missileRack] — one or more points: missile launch points (optional)
 */
data class ShipShapeDef(
    val name: String,
    val author: String,
    val hull: List<Pair<Int, Int>>,
    val engine: Pair<Int, Int>? = null,
    val mainGun: Pair<Int, Int>? = null,
    val leftLight: List<Pair<Int, Int>> = emptyList(),
    val rightLight: List<Pair<Int, Int>> = emptyList(),
    val missileRack: List<Pair<Int, Int>> = emptyList(),
)

// ---------------------------------------------------------------------------
// Minimal JSON parser — handles only the flat structure produced by
// the shipshapes.json / bots.json converters. No external dependencies.
// ---------------------------------------------------------------------------

/**
 * Parse the shipshapes.json content and return all ship shape definitions.
 */
fun parseShipShapes(json: String): List<ShipShapeDef> {
    val parser = MiniJsonParser(json)
    val rawList = parser.parseArray() as? List<*> ?: return emptyList()
    return rawList.mapNotNull { obj ->
        val m = obj as? Map<*, *> ?: return@mapNotNull null
        val name = m["name"] as? String ?: ""
        val author = m["author"] as? String ?: ""
        val hull = parsePointList(m["hull"])
        if (hull.isEmpty()) return@mapNotNull null
        ShipShapeDef(
            name = name,
            author = author,
            hull = hull,
            engine = parseSinglePoint(m["engine"]),
            mainGun = parseSinglePoint(m["mainGun"]),
            leftLight = parsePointList(m["leftLight"]),
            rightLight = parsePointList(m["rightLight"]),
            missileRack = parsePointList(m["missileRack"]),
        )
    }
}

private fun parseSinglePoint(v: Any?): Pair<Int, Int>? {
    val list = v as? List<*> ?: return null
    if (list.size < 2) return null
    val x = (list[0] as? Number)?.toInt() ?: return null
    val y = (list[1] as? Number)?.toInt() ?: return null
    return x to y
}

private fun parsePointList(v: Any?): List<Pair<Int, Int>> {
    val outerList = v as? List<*> ?: return emptyList()
    if (outerList.isEmpty()) return emptyList()
    // Could be List<List<Number>> or a single List<Number>
    return if (outerList[0] is List<*>) {
        outerList.mapNotNull { parseSinglePoint(it) }
    } else {
        listOfNotNull(parseSinglePoint(outerList))
    }
}

// ---------------------------------------------------------------------------
// MiniJsonParser — tokenizer + recursive descent; handles:
//   null, true, false, numbers (int/double), strings (with \" escapes),
//   arrays, objects.  No whitespace-sensitivity issues.
// ---------------------------------------------------------------------------

internal class MiniJsonParser(
    private val src: String,
) {
    private var pos = 0

    fun parseValue(): Any? {
        skipWhitespace()
        if (pos >= src.length) return null
        return when (src[pos]) {
            '"' -> {
                parseString()
            }

            '[' -> {
                parseArray()
            }

            '{' -> {
                parseObject()
            }

            't' -> {
                pos += 4
                true
            }

            'f' -> {
                pos += 5
                false
            }

            'n' -> {
                pos += 4
                null
            }

            '-', in '0'..'9' -> {
                parseNumber()
            }

            else -> {
                null
            }
        }
    }

    fun parseArray(): List<Any?> {
        expect('[')
        val list = mutableListOf<Any?>()
        skipWhitespace()
        if (pos < src.length && src[pos] == ']') {
            pos++
            return list
        }
        while (pos < src.length) {
            list += parseValue()
            skipWhitespace()
            when {
                pos < src.length && src[pos] == ',' -> {
                    pos++
                }

                pos < src.length && src[pos] == ']' -> {
                    pos++
                    break
                }

                else -> {
                    break
                }
            }
        }
        return list
    }

    private fun parseObject(): Map<String, Any?> {
        expect('{')
        val map = LinkedHashMap<String, Any?>()
        skipWhitespace()
        if (pos < src.length && src[pos] == '}') {
            pos++
            return map
        }
        while (pos < src.length) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            expect(':')
            val value = parseValue()
            map[key] = value
            skipWhitespace()
            when {
                pos < src.length && src[pos] == ',' -> {
                    pos++
                }

                pos < src.length && src[pos] == '}' -> {
                    pos++
                    break
                }

                else -> {
                    break
                }
            }
        }
        return map
    }

    private fun parseString(): String {
        expect('"')
        val sb = StringBuilder()
        while (pos < src.length) {
            val c = src[pos++]
            if (c == '"') break
            if (c == '\\' && pos < src.length) {
                when (src[pos++]) {
                    '"' -> {
                        sb.append('"')
                    }

                    '\\' -> {
                        sb.append('\\')
                    }

                    '/' -> {
                        sb.append('/')
                    }

                    'n' -> {
                        sb.append('\n')
                    }

                    'r' -> {
                        sb.append('\r')
                    }

                    't' -> {
                        sb.append('\t')
                    }

                    else -> {}
                }
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun parseNumber(): Number {
        val start = pos
        if (pos < src.length && src[pos] == '-') pos++
        while (pos < src.length && src[pos].isDigit()) pos++
        val isFloat = pos < src.length && src[pos] in ".eE"
        if (isFloat) {
            while (pos < src.length && src[pos] in "0123456789.eE+-") pos++
        }
        val s = src.substring(start, pos)
        return if (isFloat) {
            s.toDoubleOrNull()
                ?: throw IllegalArgumentException("MiniJsonParser: invalid float '$s' at position $start")
        } else {
            s.toLongOrNull()
                ?: throw IllegalArgumentException("MiniJsonParser: invalid integer '$s' at position $start")
        }
    }

    private fun skipWhitespace() {
        while (pos < src.length && src[pos].isWhitespace()) pos++
    }

    private fun expect(c: Char) {
        skipWhitespace()
        if (pos < src.length && src[pos] == c) {
            pos++
        } else {
            val found = if (pos < src.length) "'${src[pos]}'" else "end of input"
            throw IllegalArgumentException(
                "MiniJsonParser: expected '$c' at position $pos but found $found",
            )
        }
    }
}
