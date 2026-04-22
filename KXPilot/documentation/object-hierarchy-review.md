# Object Hierarchy Review — Action Plan

Generated: 2026-04-21  
Source: Architect review of server object hierarchy and best practices.

---

## Status Legend
- `[ ]` pending
- `[x]` done
- `[-]` skipped / deferred

---

## HIGH Priority

### H1 — `Player` has no `reset()` — pool reuse is broken
**File:** `server/Player.kt`  
**Decision:** Add both `reset()` (full wipe, pool-safe) AND `resetForRespawn()` (combat state only, identity preserved).

- [x] Add `override fun reset()` — zeros all ~60 player fields + calls `super.reset()`
- [x] Add `fun resetForRespawn()` — resets score, fuel, shots, mods, state; leaves name/rank/host intact
- [x] Compile-check

### H2 — `ObjType.bit` overflows for code ≥ 32
**File:** `server/Object.kt`  
**Decision:** Add `init { require(code < 32) }` guard to `ObjType`.

- [x] Add `init` block to `ObjType`
- [x] Compile-check

### H3 — `RankNode.pl` in `data class` constructor breaks equals/hashCode/toString
**File:** `server/Rank.kt`  
**Decision:** Move `pl` out of constructor params into class body so `data class` excludes it. Remove `@Transient`.

- [x] Move `pl: Player?` to class body as `var pl: Player? = null`
- [x] Remove `@Transient`
- [x] Compile-check

---

## MEDIUM Priority

### M4 — `Base` and `PlayerFuel` are `data class` with `IntArray`/`DoubleArray` — shallow copy is wrong
**Files:** `server/Map.kt` (`Base`), `server/Player.kt` (`PlayerFuel`)  
**Decision:** Drop `data class`; use plain `class` with explicit `equals`/`hashCode` where needed.

- [x] Convert `Base` to plain `class` (keep explicit `equals`/`hashCode`)
- [x] Convert `PlayerFuel` to mutable `class` (drop `data class`; mutable fields)
- [x] Compile-check

### M5 — `CellType` is not type-safe
**File:** `server/Map.kt`  
**Decision:** Full enum `CellType` with `fromRaw(Int)`. `World.block` becomes `Array<Array<CellType>>`.

- [x] Convert `object CellType` to `enum class CellType(val code: Int)` with `fromRaw(Int)` companion
- [x] Update `World.block` to `Array<Array<CellType>>`
- [x] Update `getBlock` → returns `CellType`; `getBlockRaw` returns `CellType`
- [x] Update `setBlock` to accept `CellType`
- [x] Update all call sites in `Map.kt`, `GameEngine.kt`, resource layer
- [x] Compile-check

### M6 — `ObjStatus` constants are `val UShort` — unnecessary box allocation on hot path
**File:** `server/Object.kt`  
**Decision:** Store as `const val Int`; cast to `UShort` at use site.

- [x] Convert `ObjStatus` vals to `const val Int`
- [x] Update all bit-check call sites (GameEngine.kt, Player.kt) to use `.toInt()` or cast
- [x] Compile-check

### M7 — `PlayerFuel` is `data class` with hot-path allocation on every fuel change
*(Covered by M4)*

### M8 — `Wormhole` and `Treasure` are `data class` with mutable-intent fields
**File:** `server/Map.kt`  
**Decision:** Convert to mutable `class`.

- [x] Convert `Wormhole` to mutable `class`
- [x] Convert `Treasure` to mutable `class`
- [x] Compile-check

### M9 — `MissileObject` is `open` with no fields — pool-eligible types should be sealed/final
**File:** `server/Object.kt`  
**Decision:** Seal `MissileObject`; add `@Pooled` doc comment to pool-eligible leaf types.

- [x] Make `MissileObject` `final` (remove `open`)
- [x] Add pool eligibility comments to `GameObject`, `MissileObject`, `SmartObject`, `TorpObject`, `HeatObject`, `BallObject`, `WireObject`, `PulseObject`, `ItemObject`
- [x] Compile-check

### M10 — `ObjectPool.allocate()` does not call `reset()` — silent corruption risk
**File:** `server/ObjectPool.kt`  
**Decision:** `allocate()` calls `obj.reset()` automatically before returning.

- [x] Add `obj.reset()` call inside `allocate()`
- [x] Remove warning KDoc comment (invariant is now enforced)
- [x] Compile-check

---

## LOW Priority

### L11 — `LockInfo` is `data class` but `distance` mutates every tick
**File:** `server/Player.kt`  
**Decision:** Make `LockInfo` a mutable class or inline its fields into `Player`.

- [x] Convert `LockInfo` to mutable `class` with `var` fields; add `reset()`; update call sites

### L12 — Duplicate `-1436.0` sentinel in `ScoreConst`
**File:** `server/Score.kt`  
**Decision:** Add `UNOWNED_SENTINEL` constant and document meaning; reference from the five entries.

- [x] Add `UNOWNED_SENTINEL` constant and comment
- [x] Update duplicate constants to reference it

### L13 — `Modifier` companion mixes `MAX_*` and per-modifier bit values
**File:** `server/Modifiers.kt`  
**Decision:** Group per-modifier bit values closer to their enum entry via nested companion or comment blocks.

- [x] Reorganize companion constants into logical groups with section comments

### L14 — `PlayerAbility` gaps between bits 4 and 15 are undocumented
**File:** `server/Player.kt`  
**Decision:** Add comment noting intentional gaps matching C source.

- [x] Add comment block documenting sparse bit allocation

### L15 — `AsteroidConst.DELTA_DIR_DIVISOR` is awkward — store the value, not the divisor
**File:** `server/Asteroid.kt`  
**Decision:** Replace with `const val DELTA_DIR: Int = GameConst.RES / 8`.

- [x] Replace `DELTA_DIR_DIVISOR` with `DELTA_DIR`

---

## Commit Plan

| Commit | Content |
|--------|---------|
| ~~`arch-h`~~ `c4abe8b` | H1 Player reset, H2 ObjType guard, H3 RankNode pl, M10 auto-reset in allocate ✅ |
| `arch-m-types` | M4 Base/PlayerFuel, M5 CellType enum, M6 ObjStatus, M8 Wormhole/Treasure, M9 MissileObject |
| `arch-l` | L11 LockInfo, L12 Score sentinel, L13 Modifiers, L14 PlayerAbility, L15 AsteroidConst |
