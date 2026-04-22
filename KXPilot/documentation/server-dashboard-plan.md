# Server Dashboard Plan

## Milestones

| Milestone | Description | Status |
|-----------|-------------|--------|
| M0 | UI plumbing (Screen + button) | ✅ DONE |
| M1 | Server domain layer | ✅ DONE |
| M2 | UDP transport + XPilot wire protocol | ✅ DONE |
| M3 | ServerGameLoop skeleton | ✅ DONE |
| M4 | Dashboard wired to real server data | ✅ DONE |
| M5 | Physics port | ✅ DONE (all 50 expert review findings fixed; 307 tests pass) |
| M6 | Full playability (C client connects) | ⬜ TODO |

---

## M0 — UI Plumbing ✅

All steps complete:

- [x] `Screen.ServerDashboard` added to `Screen.kt`
- [x] `Screen.ServerDashboard -> ServerDashboardScreen()` branch added to `App.kt`
- [x] "SERVER" `GameButton` added to `MainMenuScreen.kt` left sidebar (between CONFIG and QUIT)
- [x] Full `ServerDashboardScreen.kt` created (composable with metrics, player table, config form, event log, dialogs)

---

## M1 — Server Domain Layer ✅

All steps complete:

- [x] `server/ServerConfig.kt` — `data class ServerConfig(port=15345, mapPath, maxPlayers, targetFps, welcomeMessage, serverName, teamPlay, allowRobots)` with `init` validation
- [x] `server/ConnectedPlayer.kt` — `data class ConnectedPlayer(id, name, team, score, pingMs, isMuted, address)`
- [x] `server/ServerMetrics.kt` — `data class ServerMetrics(uptimeMs, tickRateActual, tickRateTarget, playerCount, bandwidthInBps, bandwidthOutBps, cpuPercent, heapUsedMb)`
- [x] `server/ServerController.kt` — `sealed class ServerState { Stopped, Starting, Running, Error }`; `class ServerController` with `start()`, `stop()`, `changeMap()`, `kickPlayer()`, `mutePlayer()`, `sendMessageAll()`, `sendMessageOne()`; exposes `StateFlow<ServerState>`; placeholder game loop emitting metrics every second
- [x] `internal expect fun currentTimeMs(): Long` + `actual` in desktopMain / androidMain / jsMain
- [x] `model/ServerDashboardModels.kt` — `DashboardPlayerRow`, `ServerEvent`, `ServerEventLevel`
- [x] `ui/stateholder/ServerDashboardStateHolder.kt` — observes `ServerController.state`, exposes UI-facing state, delegates commands to controller, manages dialog state
- [x] `ui/screens/ServerDashboardScreen.kt` — full dashboard composable (status bar, config form, metrics tiles, player table, event log, modal dialogs)
- [x] Server config options added to `XpOptionRegistry.kt` (serverPort, serverMaxPlayers, serverMapPath, serverName, serverWelcomeMessage)
- [x] `commonTest/ServerControllerTest.kt` — 15 unit tests covering state transitions, player commands, config validation, event ring buffer

---

## M2 — UDP Transport + Wire Protocol ✅

All steps complete:

- [x] `net/XpBuf.kt` — `XpReader` / `XpWriter` big-endian helpers mirroring `Packet_printf/scanf`
- [x] `net/XpPacket.kt` — sealed hierarchy + `PktType` constants for all client→server packets
- [x] `net/PacketDecoder.kt` — `ByteArray → XpPacket` (game-layer packets)
- [x] `net/PacketEncoder.kt` — `XpPacket → ByteArray` + server→client factory methods
- [x] `net/XpContactPacket.kt` — contact-layer codec: `XpContactDecoder`, `XpContactEncoder`, magic helpers, `ContactPackType` / `ContactStatus` constants
- [x] `net/ClientSession.kt` — per-client FSM (`ConnState` enum, `handleVerify/Ack/Play/Display`, reliable-data queue)
- [x] `net/UdpTransport.kt` — `expect class`; `actual` on desktop = `DatagramSocket` + `Dispatchers.IO`; stubs on Android/wasm
- [x] `ServerController` updated — binds `UdpTransport` on `start()`, launches contact-packet dispatch coroutine, closes socket on `stop()`
- [x] `commonTest/net/PacketCodecTest.kt` — round-trip tests for all `XpPacket` variants + server factory byte-layout checks
- [x] `commonTest/net/XpBufTest.kt` — boundary tests for `XpReader`/`XpWriter` (underflow, NUL strings, big-endian order, magic helpers)
- All tests pass: `./gradlew :shared:desktopTest`

---

## M3 — Game Loop Skeleton ✅

All steps complete:

- [x] `server/SystemMetrics.kt` — `expect fun sampleSystemMetrics(): SystemSnapshot`; `actual` on desktop reads JVM `OperatingSystemMXBean` (CPU %) and `Runtime` (heap); stubs on Android/JS return `UNAVAILABLE`
- [x] `server/ServerGameLoop.kt` — `internal suspend fun runGameLoop(config, scope, onMetrics, onTimeoutScan)`: tick-based fixed-rate loop (targetFps Hz), emits `ServerMetrics` once per second, calls `onTimeoutScan` every tick
- [x] `net/UdpChannel.kt` — `interface UdpChannel : AutoCloseable` extracted so tests can inject fakes without subclassing `expect class UdpTransport`
- [x] `UdpTransport` updated to implement `UdpChannel` on all platforms
- [x] `ServerController` refactored:
  - Constructor now accepts `transportFactory: (port: Int) -> UdpChannel` for dependency injection
  - `start()` launches contact loop + game loop as child coroutines
  - Contact loop: `ENTER_GAME` → allocates per-player `UdpChannel` + `ClientSession`, sends `SUCCESS` reply with login port
  - Per-player loop: handles `PKT_VERIFY` → `SETUP`, `PKT_PLAY` → `PLAYING` (`ConnectedPlayer` added to state), `PKT_DISPLAY`, `PKT_KEYBOARD`, `PKT_TALK` (broadcast + `TALK_ACK`), `PKT_QUIT` (remove player)
  - `kickPlayer` sends `PKT_LEAVE` to all and closes session
  - `sendMessageAll` / `sendMessageOne` encode and deliver `PKT_MESSAGE`
  - Game loop `onMetrics` updates `ServerMetrics` incl. `playerCount`, CPU, heap
  - `onTimeoutScan` evicts stale sessions (LISTENING/SETUP/LOGIN timeouts)
- [x] `commonTest/ServerGameLoopTest.kt` — 11 tests: lifecycle with fake transport, metrics after virtual-time advance, player management, event log cap, changeMap
- All tests pass: `./gradlew :shared:desktopTest`

---

## M4 — Dashboard Wired to Real Data ✅

All steps complete:

- [x] `expect/actual showFilePicker(): String?` — `JFileChooser` on desktop, null on Android/wasm
- [x] Wire "CHANGE MAP" button to file picker via `ServerDashboardStateHolder.pickAndChangeMap()`
- [x] Real uptime, tick rate, player count flowing through `ServerMetrics` (done in M3 game loop)

---

## M5 — Physics Port ✅ DONE

### Completed

- [x] M5.1 `PacketEncoder` game-frame methods: `start(frameLoop, lastKeyChange)`, `end(frameLoop)`, `self(...)`, `score(...)` — correct wire format from C `netserver.c`
- [x] M5.2 `ServerGameWorld` — holds `World` + `MutableMap<Int, Player>`; `spawnPlayer`, `despawnPlayer`, `playerForSession`, `advanceFrame`, `applyKeyBitmap`; default 60×60 open-field world
- [x] M5.3+M5.4 `ServerPhysics.tickPlayer` — thrust, turning (turnacc/turnvel damping), gravity integration, speed limit clamp, position integration, world wrap, AABB wall collision with bounce/kill
- [x] M5.5 `FrameBroadcast.sendFrame` — PKT_START → PKT_SELF → PKT_SCORE (all players) → PKT_END per PLAYING session every tick
- [x] M5.6 `ScoreSystem` — `wallDeath`, `playerKill`, `environmentKill`, `respawn`; +1 kill / +1 death / ±score
- [x] M5.10 Wired into `ServerController` — `gameWorld` created in `start()`; `spawnPlayer` in `promoteToPlaying`; `despawnPlayer` in `removePlayerLocked/Sync`; `applyKeyBitmap` in `Keyboard` handler; `tickWorld` (physics + broadcast) called every game-loop tick via `onTick`
- [x] M5.11 Unit tests — `ServerPhysicsTest` (thrust, THRUSTING bit, speed limit, wrap, wall kill, wall bounce, turning, NONE free movement); `PacketEncoderFrameTest`; `ScoreSystemTest`; `FrameBroadcastTest`; `ServerGameWorldTest`; `ApplyKeyBitmapTest`; all 307 tests pass
- [x] M5.12 Expert review: all 50 findings fixed (6 critical, 10 significant, 12 code quality, 8 test gaps); 307 tests all green

### Still TODO

- [ ] M5.7 `CannonAI` — scan players, fire simplified cannon shot when player in range
- [ ] M5.8 `AsteroidSystem` — spawn at `AsteroidConcentrator` tiles, split on hit
- [ ] M5.9 `RobotAI` — wander + target acquisition using key bitmaps on robot players

---

## M6 — Full Playability ⬜

- [ ] C xpilot-ng client connects via UDP and can play a full game
- [ ] End-to-end integration test (JVM subprocess connecting to embedded server)
