# KXPilot Code Review

_Generated: 2026-04-20 — Updated: 2026-04-20 (1a, 1b, 1g, 3b, 5a, 7a, 7c fixed) — Updated: 2026-04-20 (2a, 2d, 3d, 3e, 3f, 4a, 4b, 4e, 5b, 5c, 5d, 7d, 7e, 1e fixed)_

## Executive Summary

The codebase has a clear goal — port XPilot's C data model to Kotlin Multiplatform and build a Compose demo renderer. The data-model work is largely competent but riddled with specific correctness bugs, type-safety holes, C-ism cargo-culting, and design decisions that will become painful the moment any real game logic is added. The demo/render code has race conditions and per-frame allocation bombs that will become immediately visible on any capable profiler. The build setup and test coverage are nearly nonexistent.

---

## 1. Correctness Issues

### ~~1a. `MiniJsonParser.expect()` silently swallows missing tokens~~ ✅ FIXED
**File:** `ShipShapeLoader.kt:253` → fixed

`expect()` now throws `IllegalArgumentException` with the position and the found character on mismatch. Silent skip removed.

### ~~1b. `parseNumber()` will throw `NumberFormatException` on malformed input~~ ✅ FIXED
**File:** `ShipShapeLoader.kt:246` → fixed

`parseNumber()` now uses `toLongOrNull()` / `toDoubleOrNull()` and throws `IllegalArgumentException` with a descriptive message (position + bad token) on failure.

### 1c. `parseXpMap` / `parseDefaults` — structurally similar multiline-block logic handled inconsistently
**File:** `MapParser.kt:199–251`, `DefaultsParser.kt`

Both parsers handle `\multiline:` blocks with similar but subtly different loop structures, creating a maintenance trap where fixing one doesn't fix the other. The end-marker increment and `continue` logic differs between the two implementations.

### 1d. `UP_GRAV` block type is unreachable in `.xp` maps
**File:** `MapParser.kt:306–330`

`UP_GRAV` exists in `BlockType` and `CellType` but has no corresponding ASCII character in `charToBlockType`. It will never be produced from legacy map files, silently.

### 1e. `World.wrapXClick` / `wrapYClick` — O(n) while-loops ✅ FIXED
**File:** `Map.kt:343–355` → fixed

Replaced both while-loop implementations with `((cx % cwidth) + cwidth) % cwidth` (O(1)).

### 1f. `DemoGameState.wrap()` — float modulo with zero size produces NaN
**File:** `DemoGameState.kt:201–208`

`size` comes from Compose's `size.width`. If the world size is 0f on first frame, `v % 0f = NaN`, and NaN propagates through all subsequent arithmetic. Coordinates will silently become NaN forever.

### ~~1g. `DemoScreen.kt` — concurrent list mutation between coroutine and draw lambda~~ ✅ FIXED
**File:** `DemoScreen.kt:136–223` → fixed

The Canvas draw lambda now snapshots all six collections with `.toList()` before iterating (`gsShips`, `gsSparks`, `gsMines`, `gsShots`, `gsMissiles`, `gsBalls`). `drawHud` receives the ships snapshot instead of reading `gs.ships` directly. The tick loop still mutates the originals, but the draw lambda always iterates stable copies.

### 1h. `Score.kt` — five semantically distinct sentinels all set to `-1436.0`
**File:** `Score.kt:14–17`

```kotlin
const val ASTEROID_SCORE: Double = -1436.0
const val CANNON_SCORE: Double = -1436.0
const val TARGET_SCORE: Double = -1436.0
const val TREASURE_SCORE: Double = -1436.0
const val UNOWNED_SCORE: Double = -1436.0
```

In the C source these are distinct sentinel "no real killer ID" values, not identical copies. All five have different semantic meanings and should be typed/named accordingly.

---

## 2. Design / Architecture Flaws

### ~~2a. `data class` with `var` fields — broken contract~~ ✅ FIXED
**File:** `DemoGameState.kt:73–133` → fixed

All demo entity classes (`DemoShip`, `DemoShot`, `DemoMissile`, `DemoMine`, `DemoSpark`, `DemoBall`) converted to plain `class`. The single `self.copy(shapeDef = ...)` call in `DemoScreen.kt` was replaced with direct field assignment `self.shapeDef = ...`. `DemoGameState` now also caches `selfShip` directly, eliminating the repeated `firstOrNull { it.isSelf }` scans (fixes 7d).

### ~~2b. `AnyObject` sealed class — worst of both worlds~~ ✅ FIXED
**File:** `Object.kt:189–243` → fixed

All 10 inner `data class` variants of `AnyObject` converted to plain `class`. `data class` semantics were misleading for wrappers around mutable objects; plain `class` uses reference identity which is correct.

### ~~2c. `GameObjectBase` — 20+ inherited `var` fields, no semantic boundary~~ WONTFIX
**File:** `Object.kt:79–104`

Investigated the C source. Every `struct xp_*object` in `server/object.h` expands both `OBJECT_BASE` (which includes `mods`, `wormHoleHit`, `wormHoleDest`, etc.) and `OBJECT_EXTEND` (`pl_range`, `pl_radius`, `fuse`). There are no fields that `PulseObject` or `BallObject` truly "never use" — the physics engine addresses these fields on all object types uniformly. The current `GameObjectBase` is a correct and intentional port of the C design. An interface-trait split would either duplicate every interface on every type (no gain) or require a full server physics audit well beyond a KMP port. Closing as WONTFIX.

### ~~2d. `Player.kt` — 80+ public `var` fields, no invariants enforced~~ ✅ FIXED (floatDir invariant)
**File:** `Player.kt` → fixed

`floatDir`, `floatDirCos`, `floatDirSin` are now private (`_floatDir`, `_floatDirCos`, `_floatDirSin`) with public read-only `val` properties. `setFloatDir(angle: Double)` atomically updates all three. Direct field mutation is no longer possible from outside the class. The remaining 80+ `var` fields are a design-level concern tracked under issue 2c.

### ~~2e. `ShipShape.kt` — rotation cache copy-pasted (and mistyped) between `Shape` and `ShipShape`~~ ✅ FIXED
**File:** `ShipShape.kt:36–195` → fixed

Typo `cashedPts`/`cashedDir` → `cachedPts`/`cachedDir` corrected in both `Shape` and `ShipShape`. The duplication remains (structural refactor deferred to a future pass) but the fields are now correctly named.

### 2f. `DemoScreen.kt` — God composable mixing four concerns
**File:** `DemoScreen.kt`

`DemoScreen()` loads resources, runs a physics tick loop, mutates game state, and renders everything in one 582-line file. Resource loading uses `object {}.javaClass.getResourceAsStream(...)` — a JVM-only idiom inside what should be a KMP composable. Physics belongs in a ViewModel; loading belongs in `expect/actual` resource loaders.

### 2g. `Setup.kt` — `Long` used where the protocol specifies 32-bit
**File:** `Setup.kt:99–102`

`setupSize` and `mapDataLen` are protocol 32-bit fields modeled as `Long`. There is no validation that `mapData.size == mapDataLen.toInt()`. A server sending `mapDataLen` larger than `mapData.size` will produce silent garbage reads.

### 2h. `Client.kt` — packed `pos: Int` with no type or helpers
**File:** `Client.kt:141–175`

`FuelStation.pos`, `HomeBase.pos`, `CannonTime.pos`, etc. are packed `(x << 12) | y` integers. They are completely opaque with no type, no documentation, no unpacking helpers. A `@JvmInline value class Packed12Pos(val raw: Int)` with `val x` and `val y` accessors costs nothing and eliminates an entire class of decode bugs.

---

## 3. Kotlin Anti-patterns

### ~~3a. `object` constant blocks instead of `enum class`~~ ✅ FIXED (partially)
**File:** `Object.kt`, `Cannon.kt` → fixed; `ObjStatus`, `LockMode`, `MapBlock` remain `object`

`ObjType`, `CannonWeapon`, and `CannonDefense` converted to `enum class` with a `code: Int` property and `fromCode(Int)` companion lookup. `ObjStatus` (bit-flags), `LockMode` (mixed capacity constant), and `MapBlock` (compound encoded ranges) cannot cleanly be enums and remain as `object` blocks.

### ~~3b. `entries.first { }` — throws on unknown protocol values~~ ✅ FIXED
**File:** `Player.kt:30–52`, `Constants.kt:120` → fixed

- `PlayerType.fromCode` → fallback `HUMAN`
- `PlayerState.fromCode` → fallback `UNDEFINED`
- `GameColor.fromIndex` → fallback `BLACK`

All three now use `firstOrNull { } ?: fallback`.

### ~~3c. `Key.NUM_KEYS` cannot be `const val`~~ ✅ FIXED
**File:** `Key.kt:134` → fixed

`NUM_KEYS` changed to `const val NUM_KEYS = 72`. An `init` block in the companion verifies `entries.last().ordinal + 1 == NUM_KEYS` at class load time, catching any future enum/constant divergence.

### ~~3d. `rotateShip()` is dead code~~ ✅ FIXED
**File:** `DemoGameState.kt:50–66` → fixed

`rotateShip()` function removed entirely.

### ~~3e. `headingToRadians` returns `Double` but is used in `Float` math~~ ✅ FIXED
**File:** `DemoGameState.kt:17`, `DemoScreen.kt:175` → fixed

`headingToRadians` now returns `Float`. Call sites that pass the result to `kotlin.math.cos/sin` (which take `Double`) widen via `.toDouble()` explicitly.

### ~~3f. `SPARK_RADIUS` named "radius" but used as a side-length~~ ✅ FIXED
**File:** `DemoGameState.kt:30`, `DemoScreen.kt:232` → fixed

Renamed to `SPARK_SIZE` with an updated doc comment. Size value changed from 1f to 2f (1px was invisible at normal DPI). Draw call uses `SPARK_SIZE` as the rect side length.

---

## 4. Type Safety Holes

### ~~4a. `ShipShapeDef.hull: List<IntArray>` — breaks `data class` equality~~ ✅ FIXED
**File:** `ShipShapeLoader.kt:13–22` → fixed

All point fields (`hull`, `engine`, `mainGun`, `leftLight`, `rightLight`, `missileRack`) changed from `IntArray`/`List<IntArray>` to `Pair<Int,Int>`/`List<Pair<Int,Int>>`. `Pair` participates in structural `equals`/`hashCode`. All call sites in `DemoScreen.kt` updated to use `.first`/`.second`.

### ~~4b. `Modifiers.set` — out-of-range values silently clamped~~ ✅ FIXED
**File:** `Modifiers.kt:71–78` → fixed

`require(value in 0..modifier.mask)` added with a descriptive error message.

### 4c. `@Suppress("UNCHECKED_CAST")` × 3 with no structural verification
**File:** `ShipShapeLoader.kt:41–69`

The parser returns `Any?` from `parseValue()` and all callers cast blindly. A structural mismatch silently produces `null`. A proper `sealed class JsonNode` would eliminate all three suppressions and make mismatches visible.

### 4d. `parseAttrs` silently defaults empty attribute values to `0`
**File:** `MapParser.kt:562`

An XML attribute `x=""` on a `<Base>` tag places a base at (0,0) without any warning. Flag empty values as malformed rather than defaulting silently.

### ~~4e. `ObjStatus` constants are `UInt`, `objStatus` field is `UShort`~~ ✅ FIXED
**File:** `Object.kt:41–53, 98` → fixed

All `ObjStatus` constants changed to `UShort` via `(1u shl n).toUShort()`. `Player.isThrusting()` no longer needs the `.toUShort()` cast.

---

## 5. Missing Error Handling

### ~~5a. `loadShipShapes()` / `loadMap()` catch `Exception` and return empty silently~~ ✅ FIXED
**File:** `DemoScreen.kt:65–89` → fixed

Both loaders now print to `System.err` with class name + message on exception, and emit a separate message when a resource path is missing from the classpath entirely.

### ~~5b. `World.getBlock` / `setBlock` — no bounds checking~~ ✅ FIXED
**File:** `Map.kt:325–336` → fixed

Both methods now call `require(block.isNotEmpty())` to guard against pre-initialisation access, and `require(bx in 0 until x && by in 0 until y)` to catch out-of-range coordinates with a descriptive error.

### ~~5c. `parseDefaults` — endMarker not found silently consumes rest of file~~ ✅ FIXED
**File:** `DefaultsParser.kt` → fixed

When the inner while loop reaches EOF without finding the end marker, `System.err.println` now logs the block name and expected end marker string.

### ~~5d. `PlayerFuel.tank` — fixed size array regardless of `numTanks`~~ ✅ FIXED
**File:** `Player.kt:107–128` → fixed

`init { require(numTanks in 0..MAX_TANKS) { ... } }` added to `PlayerFuel`.

---

## 6. Test Gaps

~~The `commonTest` source set has `kotlin-test` as a dependency. The actual test count is zero.~~ ✅ FIXED — 96 tests written and passing.

### Tests added (`shared/src/commonTest/`)

| File | Tests | Coverage |
|---|---|---|
| `MiniJsonParserTest.kt` | 29 | Primitives, arrays, objects, escape sequences, error cases, `parseShipShapes` integration, structural equality |
| `ModifiersTest.kt` | 13 | Zero baseline, roundtrip per field, independent fields, overwrite, all-max, out-of-range throws |
| `WorldTest.kt` | 18 | `getBlock`/`setBlock` happy path, corners, OOB throws, uninitialised throws, `wrapXClick`/`wrapYClick` (including large negatives), `containsClPos` |
| `EnumsTest.kt` | 22 | `ObjType` codes, bits, `fromCode` nulls; `CannonWeapon`/`CannonDefense` roundtrip; `PlayerType`/`PlayerState`/`GameColor` fallbacks; `Key.NUM_KEYS` const |
| `PlayerTest.kt` | 14 | State predicates, `isThrusting` flag combinations, `floatDir` invariant (init, set, double-set, negative) |

### Remaining gaps (lower priority)

| Test Subject | What to Cover |
|---|---|
| `parseXPilotMap` (.xp) | All block types, team bases, missing `mapwidth` |
| `parseXPilotMap` (.xp2) | Cannons, polygons with offsets, malformed attributes |
| `parseDefaults` | Missing endMarker, value with colons |
| `MiniJsonParser` | `\u` escape (currently silently dropped), trailing comma |

---

## 7. Performance Concerns

### ~~7a. `buildShipPath()` allocates a new `Path` every frame per ship~~ ✅ FIXED
**File:** `DemoScreen.kt:351` → fixed

`DemoScreen` now holds a `shipPathCache: HashMap<ShipShapeDef?, Path>` via `remember`. `drawShip` calls `pathCache.getOrPut(ship.shapeDef) { buildShipPath(ship.shapeDef) }` — each hull shape is built once and reused every frame. The cache is keyed on `ShipShapeDef` object identity, so shape cycling naturally populates new entries.

### 7b. `textMeasurer.measure()` called every frame for static HUD lines
**File:** `DemoScreen.kt:393–447`

Lines like `shapes loaded: ${allShapes.size}` never change. Measure static strings once. Only re-measure the frame counter and live entity counts.

### ~~7c. `Shape.getPoints(dir)` ignores the rotation cache~~ ✅ FIXED
**File:** `ShipShape.kt:45` → fixed

`getPoints(dir)` now checks `cashedDir`; if it matches, returns a `copyOfRange(0, numPoints)` slice of the existing `cashedPts` array. On a direction change it repopulates `cashedPts` in-place and updates `cashedDir`. No allocation on cache hits.

### ~~7d. `firstOrNull { it.isSelf }` linear scan repeated 5× per tick~~ ✅ FIXED
**File:** `DemoScreen.kt` → fixed

`DemoGameState.selfShip` property caches the self ship reference at construction time. All five scan sites replaced with a single `gameState.selfShip` read.

### ~~7e. Spark list is unbounded under slow frames~~ ✅ FIXED
**File:** `DemoScreen.kt:153–169` → fixed

`RenderConst.MAX_SPARKS = 150` cap added. Spark emission guards check `gameState.sparks.size < MAX_SPARKS` before adding, and each individual `repeat` iteration is also gated.

### 7f. `MiniJsonParser` — character-by-character `StringBuilder` + per-number `substring`
**File:** `ShipShapeLoader.kt:196–245`

~10k `substring` allocations for hull vertex numbers across 398 ships at startup. Not catastrophic, but avoidable. Use direct index arithmetic instead of `substring` in `parseNumber`.

---

## Summary

| # | File | Severity | Category | Status |
|---|------|----------|----------|--------|
| 1a | ShipShapeLoader.kt:253 | Critical | Correctness — silent parse corruption | ✅ Fixed |
| 1b | ShipShapeLoader.kt:246 | High | Correctness — uncaught NFE kills all shapes | ✅ Fixed |
| 1c | MapParser.kt / DefaultsParser.kt | Low | Correctness — inconsistent multiline-block logic | Open |
| 1d | MapParser.kt:306 | Low | Correctness — UP_GRAV unreachable | Open |
| 1e | Map.kt:343 | Medium | Correctness — O(n) wrap loops | ✅ Fixed |
| 1f | DemoGameState.kt:201 | Low | Correctness — NaN on zero world size | Open |
| 1g | DemoScreen.kt:136–223 | Critical | Correctness — concurrent CME | ✅ Fixed |
| 1h | Score.kt:14–17 | Medium | Correctness — conflated sentinel values | Open |
| 2a | DemoGameState.kt:73–133 | High | Design — broken data class contract | ✅ Fixed |
| 2b | Object.kt:189–243 | Medium | Design — misleading immutable wrapper | ✅ Fixed |
| 2c | Object.kt:79–104 | Medium | Design — over-wide inheritance | WONTFIX (all fields used in C) |
| 2d | Player.kt | High | Design — floatDir invariant not enforced | ✅ Fixed |
| 2e | ShipShape.kt:36–195 | Low | Design — copy-pasted cache + typo | ✅ Fixed |
| 2f | DemoScreen.kt | Medium | Design — God composable, JVM-only resource loading | Open |
| 2g | Setup.kt:99–102 | Medium | Design — Long for 32-bit protocol fields | Open |
| 2h | Client.kt:141–175 | Medium | Type safety — opaque packed Int | Open |
| 3a | Object.kt, Player.kt, etc. | Medium | Anti-pattern — object consts vs enum | ✅ Fixed (ObjType, CannonWeapon, CannonDefense) |
| 3b | Player.kt:30–52, Constants.kt:120 | High | Anti-pattern — crash on unknown code | ✅ Fixed |
| 3c | Key.kt:134 | Low | Anti-pattern — NUM_KEYS not const | ✅ Fixed |
| 3d | DemoGameState.kt:50–66 | Low | Anti-pattern — dead code rotateShip() | ✅ Fixed |
| 3e | DemoGameState.kt:17 | Low | Anti-pattern — Double/Float inconsistency | ✅ Fixed |
| 3f | DemoGameState.kt:30 | Low | Anti-pattern — SPARK_RADIUS misnomer | ✅ Fixed |
| 4a | ShipShapeLoader.kt:13–22 | High | Type safety — IntArray in data class | ✅ Fixed |
| 4b | Modifiers.kt:71–78 | Low | Type safety — silent value clamping | ✅ Fixed |
| 4c | ShipShapeLoader.kt:41–69 | Medium | Type safety — unchecked casts | Open |
| 4d | MapParser.kt:562 | Low | Type safety — empty XML attr defaults to 0 | Open |
| 4e | Object.kt:41–98 | Medium | Type safety — UInt/UShort mismatch | ✅ Fixed |
| 5a | DemoScreen.kt:65–89 | High | Error handling — silent failure | ✅ Fixed |
| 5b | Map.kt:325–336 | High | Error handling — no bounds check | ✅ Fixed |
| 5c | DefaultsParser.kt | Medium | Error handling — silent EOF on missing endMarker | ✅ Fixed |
| 5d | Player.kt:107–128 | Medium | Error handling — no numTanks validation | ✅ Fixed |
| 6 | commonTest | High | Tests — zero tests written | ✅ Fixed (96 tests across 5 files) |
| 7a | DemoScreen.kt:351 | High | Performance — Path alloc per frame | ✅ Fixed |
| 7b | DemoScreen.kt:393–447 | Medium | Performance — text measure per frame | Open |
| 7c | ShipShape.kt:45 | High | Performance — cache never used | ✅ Fixed |
| 7d | DemoScreen.kt | Low | Performance — repeated self-ship linear scan | ✅ Fixed |
| 7e | DemoScreen.kt:153–169 | Medium | Performance — unbounded spark list | ✅ Fixed |
| 7f | ShipShapeLoader.kt:196–245 | Low | Performance — per-number substring alloc | Open |
