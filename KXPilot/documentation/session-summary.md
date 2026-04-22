# KXPilot Session Summary

## Goal

Port XPilot-NG (C/X11) to Kotlin Multiplatform. The project is named KXPilot. Current focus has been:
1. **Object model correctness audit** — ensuring all Kotlin constants, fields, and physics logic correctly match the C original (`xpilot-ng-4.7.3`)
2. **LOCAL button redesign** — replacing the demo map shortcut with a proper local/direct-connect panel
3. **Code quality review and remediation** — addressing architectural issues raised by expert review

---

## Project Configuration

- Package: `org.lambertland.kxpilot`
- Targets: Desktop (JVM) first, then Android and wasmJs
- Standard KMP layout: `:shared`, `:android`, `:desktop`, `:web`
- No network access — all dependencies must be cached locally
- Validate: `./gradlew :shared:compileKotlinDesktop`
- Run tests: `./gradlew :shared:desktopTest`
- Project root: `/Users/Z002M6R/src/XPilot/KXPilot/`
- C reference files (read-only): `XPilot/JXPilot/legacy/xpilot-ng-4.7.3/src/`
- `String.format` and `System.currentTimeMillis()` are JVM-only — do NOT use in `commonMain`
- `const val` cannot be used with `UInt`
- All tests must remain passing
- Build declares `wasmJs { browser() }` target — NOT `jsMain`
- Git repo is at `/Users/Z002M6R/src/XPilot/KXPilot/`

---

## Discoveries

### Current test count
**387 tests, 0 failures** as of last run.

### Git status
All changes are **uncommitted**. The last `git status` showed both modified tracked files and untracked new files. The last commit is `24834e6 arch2-low: ...`. A commit is pending.

---

## Object Model Audit (2 passes, fully applied)

All constants, physics, and timers have been aligned to C source. Key additions:

### `GameConst`
- `MAX_PLAYER_FUEL`, `REFUEL_RATE`, `FUEL_MASS_COEFF`, `fuelMass()`
- `THRUST_MASS`, `MINE_MASS/RADIUS/SPEED_FACT`, `MISSILE_MASS`
- `NO_ID`, `NUM_IDS`, `NUM_CANNON_IDS/MIN/MAX_CANNON_ID`
- `MAX_TOTAL_SHOTS`, `ALLIANCE_NOT_SET`
- `LG2_MAX_AFTERBURNER`, `MAX_AFTERBURNER`
- `MAX_SERVER_FPS`, `RECOVERY_DELAY`
- `SHIELD_TIME`, `EMERGENCY_SHIELD_TIME`, `PHASING_TIME`, `EMERGENCY_THRUST_TIME`
- `MAX_TANKS`

### `EnergyDrain`
Object with all 14 `ED_*` constants. `EnergyDrain.SHIELD` KDoc notes equality with `SHOT` is intentional per C source.

### `ServerPhysics.useItems()`
Per-tick fuel drain (shield, phasing, cloaking, deflector) and timer decrements (`shieldTime`, `phasingLeft`, `emergencyThrustLeft`, `emergencyShieldLeft`) with auto-deactivation.

### Other changes
- `ServerController` kill handler sets `pl.recoveryCount = GameConst.RECOVERY_DELAY.toDouble()`
- `PlayerDefaults.START_FUEL`/`MAX_FUEL` reference `GameConst.MAX_PLAYER_FUEL` (not literals)
- `PlayerFuel.Companion.MAX_TANKS` aliases `GameConst.MAX_TANKS`
- `NetConst.MAX_SUPPORTED_FPS` aliases `GameConst.MAX_SERVER_FPS`
- `TreasureBall.NO_PLAYER` aliases `GameConst.NO_ID`
- `ServerPhysics` constants `BOUNCE_FACTOR`, `WALL_KILL_SPEED`, `FUEL_BURN_COEFF` promoted to `internal`
- `PlayerInfo.team` KDoc documents UI `-1` vs wire `0xffff` two-layer convention

---

## File Picker Fix

`ServerDashboardStateHolder.kt` was passing `extension = "xp"` to `showFilePicker()`, blocking `.xp2` files. Both call sites changed to `extension = ""` to use the full `MAP_EXTENSIONS = setOf("xp", "xp2", "map")` set in `FilePicker.kt`.

---

## LOCAL Button Redesign

The LOCAL tab no longer shows a fake server list. It now shows `ConnectLocalPanel` with:
1. **LOCAL SERVER section** — UDP scan result (stub: "no server found" + SCAN AGAIN), with CONNECT button when a server is detected
2. **CONNECT DIRECTLY section** — host text field, port text field (validated `1..65535`), CONNECT button (disabled until valid)

---

## Architecture Decisions (from expert review)

- `ConnectLocal` data class (domain model) holds only `localServer: ServerInfo?` and `scanning: Boolean` — text field strings are UI state, not domain state
- `directHost: String` and `directPort: String` live in `MainMenuStateHolder` as plain `mutableStateOf` vars
- `directPortInt: Int?` is a computed property on the state holder with `1..65535` range check
- `canConnectDirect: Boolean` is a derived property — composable never re-derives it
- `ConnectLocalPanel` takes explicit callbacks (no state holder reference) — independently testable
- `Screen.InGame.serverPort` default is `ServerConfig.DEFAULT_PORT` (not literal `15345`)
- `join()` default port is `ServerConfig.DEFAULT_PORT`
- `scanLocal()` makes a single assignment (no double-write stub)
- `STUB_LOCAL_SERVERS` deleted
- `IdlePanel`/`ScanningPanel` annotated as INTERNET-tab-only (LOCAL tab never reaches them)
- `updateDirectHost()`/`updateDirectPort()` methods removed — callers write `state.directHost = it` directly

---

## Status

### Complete (all in uncommitted working tree)
- Pass 1 audit: constants, `EnergyDrain`, `Player` helpers, `ServerConfig` fps cap (355 tests)
- Pass 2 audit: all 12 findings (C-1 through C-5, M-1 through M-6, N-1, N-2) applied (355 tests)
- File picker `.xp2` fix
- LOCAL button redesign: `ConnectLocalPanel` with local scan + direct connect
- Expert review: all 11 recommendations applied
- `MainMenuStateHolderTest.kt`: 32 new tests covering state transitions, port validation, navigation
- **387 tests, 0 failures**

### Pending
- **Commit the changes** — all work is done and tests pass; needs `git add -A && git commit`

---

## Modified Files (tracked)

| File | Changes |
|------|---------|
| `shared/.../common/Constants.kt` | GameConst expanded, EnergyDrain added, MAX_TANKS added |
| `shared/.../server/Player.kt` | `floatDirInRes()` helper, KDocs, `PlayerFuel.MAX_TANKS` aliases `GameConst.MAX_TANKS` |
| `shared/.../server/ServerPhysics.kt` | `useItems()` added, constants promoted to `internal` |
| `shared/.../server/ServerGameWorld.kt` | `PlayerDefaults.START_FUEL/MAX_FUEL` reference `GameConst.MAX_PLAYER_FUEL` |
| `shared/.../client/NetClient.kt` | `NetConst.MAX_SUPPORTED_FPS` aliases `GameConst.MAX_SERVER_FPS` |
| `shared/.../engine/GameEngine.kt` | `TreasureBall.NO_PLAYER` aliases `GameConst.NO_ID` |
| `shared/.../model/InGameModels.kt` | `PlayerInfo.team` KDoc: UI -1 vs wire 0xffff |
| `shared/.../model/ServerModels.kt` | `ConnectLocal` slimmed, `STUB_LOCAL_SERVERS` deleted |
| `shared/.../ui/Screen.kt` | `InGame.serverPort` default = `ServerConfig.DEFAULT_PORT` |
| `shared/.../ui/App.kt` | passes `serverHost`/`serverPort` to `DemoGameScreen` |
| `shared/.../ui/components/ScoreOverlay.kt` | team sentinel comment added |
| `shared/.../ui/screens/DemoGameScreen.kt` (common) | `expect fun DemoGameScreen(serverHost, serverPort)` |
| `shared/.../ui/screens/MainMenuScreen.kt` | LOCAL tab redesign: `ConnectLocalPanel`, direct connect state |
| `shared/.../ui/screens/DemoGameScreen.kt` (desktop) | `actual fun DemoGameScreen(...)` = `InGameScreen()` |
| `shared/.../platform/FilePicker.kt` | comment corrected (.xp vs .xp2 description) |

## New / Untracked Files

```
shared/src/commonTest/.../ConstantsTest.kt              — 38 tests
shared/src/commonTest/.../MainMenuStateHolderTest.kt    — 32 tests
shared/src/commonTest/.../ServerPhysicsTest.kt
shared/src/commonTest/.../ServerControllerTest.kt
shared/src/commonTest/.../ServerGameLoopTest.kt
shared/src/commonMain/.../server/ServerConfig.kt
shared/src/commonMain/.../server/ServerController.kt
shared/src/commonMain/.../server/ServerGameWorld.kt     (untracked copy)
shared/src/commonMain/.../server/ServerPhysics.kt       (untracked copy)
shared/src/commonMain/.../ui/stateholder/ServerDashboardStateHolder.kt
shared/src/commonMain/.../ui/screens/ServerDashboardScreen.kt
shared/src/commonMain/.../net/                          (entire directory)
shared/src/commonMain/.../platform/                     (entire directory)
shared/src/androidMain/                                 (entire directory)
shared/src/wasmJsMain/                                  (entire directory)
shared/src/desktopMain/.../net/
shared/src/desktopMain/.../platform/
shared/src/desktopMain/.../server/
documentation/server-dashboard-plan.md
```

---

## Suggested Commit Message

```
feat: object model audit pass 1+2, LOCAL connect panel, file picker fix

- Expand GameConst/EnergyDrain with all serverconst.h values
- ServerPhysics: add useItems() per-tick fuel drain + item timers
- ServerController kill handler: set recoveryCount on death
- LOCAL tab: replace demo stub with local-scan + direct-connect panel
  - ConnectLocalPanel uses callbacks (no state holder coupling)
  - directHost/directPort in state holder with 1..65535 port validation
  - Screen.InGame.serverPort default = ServerConfig.DEFAULT_PORT
- FilePicker: accept .xp2 and .map alongside .xp
- Delete STUB_LOCAL_SERVERS
- Add ConstantsTest (38 tests) and MainMenuStateHolderTest (32 tests)
- 387 tests, 0 failures
```

---

## C Reference Files (read-only, never edit)

```
XPilot/JXPilot/legacy/xpilot-ng-4.7.3/src/server/update.c
XPilot/JXPilot/legacy/xpilot-ng-4.7.3/src/server/serverconst.h
XPilot/JXPilot/legacy/xpilot-ng-4.7.3/src/common/const.h
XPilot/JXPilot/legacy/xpilot-ng-4.7.3/src/server/cmdline.c
```
