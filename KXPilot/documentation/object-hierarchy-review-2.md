# Object Hierarchy Review — Round 2

Second architect review of the KXPilot server object hierarchy.
Items marked `[x]` are complete; `[ ]` are pending.

---

## Critical

- [x] 1. `PlayerAbility` — all `val Long` → `const val Long` (`1L shl N` is valid `const val`)
- [x] 2. `PlayerStatus` — all `val UInt` → `const val Int` (same pattern as `ObjStatus`); cast to `UInt` at call sites
- [x] 3. `PolygonStyleFlags` — all `val UInt` → `const val Int`
- [x] 4. `CannonConst.DISTANCE` — `val Double = GameConst.VISIBILITY_DISTANCE * 0.5` → `const val Double`

## High

- [x] 5. `GameObject` — `open class` → `class` (final), same reasoning as `MissileObject`
- [x] 6. `Target` — `data class` with mutable-intent fields; add KDoc justifying immutable-update style (`applyHit`/`destroy` extension functions)

## Medium

- [x] 7. `World.initGrid()` — add extension that sets all geometry fields atomically
- [x] 8. `World.initGrid()` — covers `x`, `y`, `bwidthFloor`, `bheightFloor`, `width`, `height`, `cwidth`, `cheight`, `block`, `gravity`
- [x] 9. `RankNode` — split into `RankIdentity` data class + `RankNode` mutable class; fixes `equals`/`hashCode` including mutable stats
- [x] 10. `PlayerFuel.numTanks` — add property setter guard (init guard fires only at construction)

## Low

- [x] 11. `MissileObjectBase` — add "not pool-eligible" KDoc
- [x] 12. `Cannon` entity — move from `Map.kt` to `Cannon.kt` for co-location with `CannonConst`/`CannonWeapon`/`CannonDefense`
- [x] 13. `LockMode.BANK_MAX` — move to `Player` companion object as `LOCK_BANK_MAX`
- [x] 14. `Grav.type: Int` — change to `CellType` now that `CellType` is a typed enum
- [x] 15. `WormType` enum entries — add KDoc explaining semantics
- [x] 16. `ScoreConst.RATE_SIZE` / `RATE_RANGE` — move to `GameConst` (unrelated to kill-attribution)
- [x] 17. `ObjectPools` capacity constants — add derivation comments explaining numeric choices
