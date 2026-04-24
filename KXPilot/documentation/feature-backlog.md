# KXPilot Feature Backlog

Items discovered during the Phase 8 engine audit. No code changes are required before these are picked up; each entry is a self-contained scope of work. Priorities are relative to gameplay completeness.

---

## Legend

| Tag | Meaning |
|-----|---------|
| **(d)** | Completely absent — not implemented at all |
| **(c)** | Constants/keys wired, no engine behaviour |
| **(b)** | Partial — core loop exists but notable gaps remain |
| **(arch)** | Architecture debt — no feature impact today |

---

## Partial Implementations (b)

> **All BL-17 through BL-21 implemented — see Resolved section below.**

---

## Architecture Debt (arch)

> **BL-21 implemented — see Resolved section below.**

---

## Resolved / Removed from Backlog

| Item | Resolution |
|------|-----------|
| `MINE_ARM_TICKS` unused | Implemented in Phase 8 follow-up — `MineData.armTicksRemaining` + tick-loop guard. |
| BL-01 Laser weapon | Implemented — `fireLaserPulse()`, `tickLaserPulses()`, `KEY_FIRE_LASER` (held, gated by `laserFireTimer`), requires `playerItems.laser > 0`. Tests in `Bl01To11Test`. |
| BL-02 Wormholes | Implemented — `tickWormholes()`, respects `WormType.OUT` (exit-only), stable countdown, `FIXED` type. Tests in `Bl01To11Test`. |
| BL-03 Bouncing shots | Implemented — `sweepMoveShotBounce()` replaces removal; all shots and laser pulses bounce with `life × 0.9` factor. Tests in `Bl01To11Test`. |
| BL-04 Shot spread toggle | Implemented — `spreadLevel` cycles 0–3 on `KEY_TOGGLE_SPREAD`; `spawnSpreadShots()` fires 3-fan when `playerItems.wideangle > 0`. Tests in `Bl01To11Test`. |
| BL-05 Checkpoints | Implemented — `tickCheckpoints()`, `checkIndex`/`laps` state, advances on proximity. Tests in `Bl01To11Test`. |
| BL-06 Friction areas | Implemented — `tickFrictionAreas()`, block-coord match, velocity decay. Tests in `Bl01To11Test`. |
| BL-07 Item spawning system | Implemented — `WorldItem` class, `worldItems` list, `tickWorldItems()`, spawn from `world.items` config, `applyItemPickup()`, player overlap pickup. Bug fixed: block-coord division error. Tests in `Bl01To11Test`. |
| BL-08 Self-destruct | Implemented — `selfDestructTicks`, `KEY_SELF_DESTRUCT` toggles start/cancel, countdown in tick step 27, `killPlayer()` at zero. Tests in `Bl01To11Test`. |
| BL-09 Hyperjump | Implemented — `doHyperjump()`, `KEY_HYPERJUMP`, requires `playerItems.hyperjump > 0`, consumes charge, 8-attempt safe-position search. Tests in `Bl01To11Test`. |
| BL-10 Detonate all mines | Implemented — `detonateAllMines()`, `KEY_DETONATE_MINES`, iterates owned mines, spawns debris. Tests in `Bl01To11Test`. |
| BL-11 Cannon weapon variety | Implemented — `tickCannons()` dispatches on `cannon.weapon`: SHOT→shot, LASER→laserPulse, MISSILE→missile, others→plain shot. Tests in `Bl01To11Test`. |
| BL-17 Afterburner level scaling | Implemented — `AFTER_BURN_POWER_FACTOR(n)` and `AFTER_BURN_FUEL(f,n)` C formulas; `MAX_AFTERBURNER = 15`. Tests in `Bl01To11Test`. |
| BL-18 Tractor beam counter-force + fuel scaling | Implemented — Newton's-3rd counter-force on beam operator; fuel cost = `1.5 * percent * HZ_RATIO` dynamic formula. Tests in `Bl01To11Test`. |
| BL-19a NPC ball-carry + CTF scoring | Implemented — `DemoShip.carryingBallId/score`; `updateBalls` detects NPC–ball proximity; `checkBallGoal` awards NPC score. Tests in `Bl01To11Test`. |
| BL-20 NPC weapon variety | Implemented — `NpcWeaponEvent` sealed class (Shot/Missile/Mine/ShieldChange); `dispatchNpcWeaponEvents`. Tests in `Bl01To11Test`. |
| BL-12 Deflector item | Implemented — `tickDeflector()` radial repulsion field; `deflectorActive` toggle via `KEY_DEFLECTOR`; fuel drain `ED_DEFLECTOR × HZ_RATIO`; bypasses receding objects via dot-product check. Tests in `Bl12To16Test`. |
| BL-13 Cloaking device | Implemented — `cloakActive` toggle via `KEY_CLOAK`; fuel drain `ED_CLOAKING_DEVICE × HZ_RATIO`; probabilistic `canSeePlayerFromNpc()` roll (sensor×cloak); missiles targeting cloaked player receive `confusedTicks`. Tests in `Bl12To16Test`. |
| BL-14 Phasing device | Implemented — `phasingActive`, `phasingTicksLeft`; `PHASING_TIME_TICKS = 48 × HZ_RATIO`; auto-consumes next charge; wall/shot/missile/mine collision bypassed; `isInsideWall()` check kills player on de-phase inside wall. Tests in `Bl12To16Test`. |
| BL-15 Emergency shield | Implemented — `emergencyShieldActive`, `emergencyShieldTicksLeft`; auto-activates on first pickup; timer advances only on actual collision block; full shot/missile/torpedo/heat/nuke shield parity. Tests in `Bl12To16Test`. |
| BL-19b Treasure/ball sync to clients | Implemented — `PktType.BALL = 17`; `PacketEncoder.ball(posX, posY, id, style, hasBallStyle)` sends 8-byte long form (clientVersion ≥ 0x4F14) or 7-byte short form; `ViewportCuller` mirrors C `clpos_inview()` with wrap adjustment and exclusive bounds; `FrameBroadcast.sendFrame()` snapshots live balls once then sends per-client only balls in the client's viewport. Tests: wire layout, short form, viewport culling (out-of-view, multi-ball), zero-ball edge case, `ViewportCullerTest` (centre, boundary, wrap, fullWorld). |
| BL-21 `Player` god-object refactor | Implemented — `PlayerStats` class extracts score/race/bookkeeping fields (score, kills, deaths, plLife, plDeathsSinceJoin, plPrevTeam, survivalTime, check/lap/round, ecmCount, snafuCount, fs); `Player` exposes all as delegating properties including `ecmCount` and `snafuCount` (previously silent split fixed). `PhysicsState` and `PlayerStats` are independently testable. Tests: `PlayerPhysicsIsolationTest` including `physicsStateResetDirectlyClearsAllFields` calling `PhysicsState.reset()` without a `Player`. |
