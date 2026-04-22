# KXPilot — Screen Migration Proposal

**Status:** Draft v2 — revised after architectural review  
**Scope:** 8 screens ported from xpilot-ng-4.7.3 (C/X11) and JXPilot (Java/Swing)  
**Platform priority:** Desktop (JVM) first, then Android and wasmJs  
**Network:** Stubbed/hardcoded until a real network layer is added  
**Config persistence:** Port the xpilotrc `xpilot.<key>: <value>` format  

---

## Changelog

| Version | Changes |
|---|---|
| v1 | Initial draft |
| v2 | Architectural review fixes: layered config model; nav back stack + SharedFlow; timestamp-based message aging; fixed-slot message log; Colors tab consolidation; AppConfig as injectable class + CompositionLocal; MotdStateHolder buffer complete; KeyBinding multi-key rebind spec; TalkStateHolder sealed send result; scoreboard Column not LazyColumn; model file consolidation; @Preview scope clarification; OptionRow visitor pattern; implementation order corrected |

---

## 0. Guiding Principles

### Layer boundaries

```
┌────────────────────────────────────────┐
│  UI layer  (Compose, commonMain)       │  ← composables, state holders, CompositionLocals
├────────────────────────────────────────┤
│  Domain layer  (pure Kotlin)           │  ← models, config defs, game actions — NO Compose imports
├────────────────────────────────────────┤
│  Platform I/O  (expect/actual)         │  ← file read/write, network, clock
└────────────────────────────────────────┘
```

**Rule:** Nothing in the domain layer may import `androidx.compose.*`. Domain models are plain Kotlin data classes. Compose state (`mutableStateOf`, `StateFlow`) lives exclusively in the UI layer.

### Navigation architecture

The sealed `Screen` hierarchy is the route vocabulary. `Colors` is a tab inside `Config`, not a separate screen — it is not listed here.

```kotlin
// shared/commonMain — domain layer
sealed class Screen {
    object MainMenu    : Screen()
    object Config      : Screen()   // contains DEFAULT + COLORS tabs internally
    object KeyBindings : Screen()
    object About       : Screen()
    object Motd        : Screen()
    data class InGame(val serverId: String) : Screen()
}
```

Navigation is handled by a `Navigator` held in the root composable and distributed via `CompositionLocal`. It owns an explicit back stack and exposes a `SharedFlow<Screen>` so state holders can emit navigation events without holding a reference to the UI tree.

```kotlin
// shared/commonMain — UI layer
class Navigator(initial: Screen = Screen.MainMenu) {
    private val stack = ArrayDeque<Screen>().also { it.addLast(initial) }

    // Observed by the root App composable via collectAsState()
    private val _current = MutableStateFlow(initial)
    val current: StateFlow<Screen> = _current.asStateFlow()

    fun push(screen: Screen) {
        stack.addLast(screen)
        _current.value = screen
    }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeLast()
        _current.value = stack.last()
        return true
    }

    fun replace(screen: Screen) {
        stack[stack.size - 1] = screen
        _current.value = screen
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("No Navigator provided")
}
```

The `App` root composable provides `Navigator` and renders the current screen:

```kotlin
@Composable
fun App(navigator: Navigator = remember { Navigator() }) {
    val appConfig = remember { AppConfig.load() }
    val currentScreen by navigator.current.collectAsState()

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalAppConfig provides appConfig,
    ) {
        when (val screen = currentScreen) {
            Screen.MainMenu    -> MainMenuScreen()
            Screen.Config      -> ConfigScreen()
            Screen.KeyBindings -> KeyBindingsScreen()
            Screen.About       -> AboutScreen()
            Screen.Motd        -> MotdScreen()
            is Screen.InGame   -> InGameScreen(screen.serverId)
        }
    }
}
```

State holders navigate by calling `navigator.push(Screen.X)` — they receive `Navigator` as a constructor parameter, not a Compose reference.

```kotlin
class MainMenuStateHolder(private val navigator: Navigator) {
    fun join(s: ServerInfo) { navigator.push(Screen.InGame(s.host)) }
}
```

### State management

- Domain models: immutable `data class`, no Compose imports
- State holders: plain Kotlin classes, `@MainThread` enforced by `Dispatchers.Main` in coroutines
- Observable state: `StateFlow` for values shared across coroutine boundaries; `mutableStateOf` for UI-local state that never leaves the composition
- Coroutines that touch UI state must be launched in a `CoroutineScope` tied to the screen lifecycle, on `Dispatchers.Main`
- I/O operations (file read/write, network) always dispatched to `Dispatchers.IO` via `withContext`

### Compose best practices

- Hoist state to the lowest common ancestor
- Prefer plain `Column`/`Row` for bounded, small lists; `LazyColumn`/`LazyRow` only when item count is large or unbounded
- `remember { }` only for expensive objects (path caches, text measurers); derive cheap values inline
- `derivedStateOf` when a computed value depends on multiple state objects and should only recompose on change
- Colors and dimensions defined as `val`s in `KXPilotColors` / `KXPilotDimens` objects (no inline literals)
- Every interactive element has a `contentDescription` for accessibility
- Previews: screens live in `shared/commonMain` — use a dedicated `:preview` Android module for `@Preview` composables, or run desktop previews via a `main()` function that opens a window with the target composable

### File layout

```
shared/commonMain/kotlin/org/lambertland/kxpilot/
  ui/
    App.kt                  ← root composable, Navigator, CompositionLocals
    Navigator.kt            ← Navigator class, LocalNavigator
    screens/
      MainMenuScreen.kt
      ConfigScreen.kt       ← DEFAULT + COLORS tabs combined
      KeyBindingsScreen.kt
      AboutScreen.kt
      MotdScreen.kt
      InGameScreen.kt       ← HUD layout, ScoreOverlay, TalkOverlay
    components/
      MeterBar.kt           ← reusable fuel/power/speed meter
      ServerListItem.kt     ← reusable server row
      OptionRow.kt          ← OptionRowRenderer visitor + per-type composables
    theme/
      KXPilotColors.kt      ← 16-slot palette + named semantic colors
      KXPilotTheme.kt       ← optional MaterialTheme wrapper
  config/
    XpOptionDef.kt          ← sealed domain class hierarchy (NO Compose)
    XpOptionRegistry.kt     ← canonical list of all options
    XpilotrcParser.kt       ← parse/write ~/.kxpilotrc
    AppConfig.kt            ← Compose-aware state adapter over XpOptionRegistry
  model/
    ServerModels.kt         ← ServerInfo, ServerBrowserState
    InGameModels.kt         ← PlayerInfo, HudState, MessageEntry, MessageColor
    KeyBindingModels.kt     ← GameAction, KeyBinding, BindingMode
```

---

## 1. Main Menu / Server Browser

### Source reference
- `x11/welcome.c` — `Welcome_screen()`, modes: Waiting, LocalNet, Internet, Status, Quit
- `sdl/sdlmeta.c` — SDL/OpenGL table variant

### Purpose
Pre-game launcher. Player enters their name, picks a server (LAN or metaserver list), and joins.

### Layout (desktop)
```
┌─────────────────────────────────────────────────────┐
│  KXPilot                          [version string]  │
│  ─────────────────────────────────────────────────  │
│  Player name: [___________________]                  │
│                                                      │
│  ┌──────────┐  ┌─────────────────────────────────┐  │
│  │ LOCAL    │  │  Server list / status panel      │  │
│  │ INTERNET │  │  (swapped by tab selection)      │  │
│  │ ──────── │  │                                  │  │
│  │ QUIT     │  │                                  │  │
│  └──────────┘  └─────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

### Domain models (`model/ServerModels.kt`)

```kotlin
// Domain layer — no Compose imports

data class ServerInfo(
    val host: String,
    val port: Int,
    val mapName: String,
    val playerCount: Int,
    val queueCount: Int,
    val maxPlayers: Int,
    val fps: Int,
    val version: String,
    val pingMs: Int?,      // null = not yet measured
    val status: String,
)

sealed class ServerBrowserState {
    object Idle                                                   : ServerBrowserState()
    object Scanning                                               : ServerBrowserState()
    data class Loaded(val servers: List<ServerInfo>)              : ServerBrowserState()
    data class Detail(val server: ServerInfo, val players: List<String>) : ServerBrowserState()
    data class Error(val message: String)                         : ServerBrowserState()
}
```

### State holder (UI layer)

`Navigator` is injected so `join()` navigates without reaching into the UI tree.

```kotlin
class MainMenuStateHolder(
    private val navigator: Navigator,
    private val scope: CoroutineScope,
) {
    var playerName by mutableStateOf("")
    var tab        by mutableStateOf(Tab.LOCAL)
    var browser    by mutableStateOf<ServerBrowserState>(ServerBrowserState.Idle)

    fun scanLocal() {
        browser = ServerBrowserState.Scanning
        scope.launch(Dispatchers.Main) {
            // Stub: real UDP broadcast goes here
            browser = ServerBrowserState.Loaded(STUB_LOCAL_SERVERS)
        }
    }

    fun fetchInternet() {
        browser = ServerBrowserState.Scanning
        scope.launch(Dispatchers.Main) {
            // Stub: real HTTP metaserver fetch goes here
            browser = ServerBrowserState.Loaded(STUB_INTERNET_SERVERS)
        }
    }

    fun selectServer(s: ServerInfo) {
        browser = ServerBrowserState.Detail(s, emptyList()) // detail fetch goes here
    }

    fun join(s: ServerInfo) {
        // Persist player name before navigating
        navigator.push(Screen.InGame(s.host))
    }
}
```

### Kotlin / Compose notes
- `LazyColumn` for the server list — item count is unbounded (metaserver can return hundreds of entries)
- Column headers in a sticky `stickyHeader {}` — Pl, Q, Ba, FPS, Map, Server, Ping, Status
- `Tab.LOCAL` / `Tab.INTERNET` uses a custom `Row` of `Button`s; avoid `TabRow` from Material3 to keep the dark game aesthetic
- Player name input: `BasicTextField` styled manually (to avoid Material text field chrome in a game UI)
- `STUB_LOCALHOST = ServerInfo("localhost", 15345, "teamcup", 0, 0, 10, 30, "4.7.3", null, "running")`

### Migration steps
- [ ] Define `ServerInfo`, `ServerBrowserState` in `model/ServerModels.kt`
- [ ] Implement `MainMenuStateHolder`
- [ ] Implement `MainMenuScreen` composable
- [ ] Implement `ServerListItem` composable (reusable row)
- [ ] Wire stub scan/fetch functions
- [ ] Write desktop preview `main()` showing Idle, Loaded, and Detail states

---

## 2. Config / Settings Screen

### Source reference
- `x11/configure.c` — `Config()`, filtered by `XP_OPTFLAG_CONFIG_DEFAULT` or `XP_OPTFLAG_CONFIG_COLORS`
- Option types: bool (Yes/No), int (◀ N ▶), double (◀ N.NNN ▶), color index (0–15)

### Purpose
Pre-game and in-game option editing. Options are live-updated in memory. "Save" writes `~/.kxpilotrc`.

`Config` and `Colors` are **tabs within one screen**, not separate navigation destinations.

### Domain model — option definitions (`config/XpOptionDef.kt`)

The domain layer defines what options exist and their constraints. It has **no Compose imports**.

```kotlin
// Domain layer — pure Kotlin, no androidx.compose imports

enum class OptionFlag { CONFIG_DEFAULT, CONFIG_COLORS, KEEP, NEVER_SAVE }
enum class OptionOrigin { DEFAULT, CMDLINE, ENV, XPILOTRC, CONFIG }

sealed class XpOptionDef<T>(
    val name: String,
    val help: String,
    val flags: Set<OptionFlag>,
    val defaultValue: T,
) {
    class Bool(name: String, help: String, flags: Set<OptionFlag>, defaultValue: Boolean)
        : XpOptionDef<Boolean>(name, help, flags, defaultValue)

    class Int(name: String, help: String, flags: Set<OptionFlag>, defaultValue: kotlin.Int,
              val min: kotlin.Int, val max: kotlin.Int)
        : XpOptionDef<kotlin.Int>(name, help, flags, defaultValue)

    class Double(name: String, help: String, flags: Set<OptionFlag>, defaultValue: kotlin.Double,
                 val min: kotlin.Double, val max: kotlin.Double)
        : XpOptionDef<kotlin.Double>(name, help, flags, defaultValue)

    class Str(name: String, help: String, flags: Set<OptionFlag>, defaultValue: String)
        : XpOptionDef<String>(name, help, flags, defaultValue)

    // Color-index option: Int in [0, 15], always CONFIG_COLORS
    class ColorIndex(name: String, help: String, defaultValue: kotlin.Int)
        : Int(name, help, setOf(OptionFlag.CONFIG_COLORS), defaultValue, 0, 15)
}
```

### Live config state — UI layer (`config/AppConfig.kt`)

`AppConfig` holds mutable values as a `Map<String, MutableState<Any>>` keyed by option name. It is a plain class (not a singleton object) created once at app startup and distributed via `CompositionLocal`. This makes it injectable in tests and previews.

```kotlin
// UI layer — Compose imports allowed

class AppConfig private constructor(
    private val defs: List<XpOptionDef<*>>,
    private val values: Map<String, MutableState<Any>>,
) {
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> stateOf(def: XpOptionDef<T>): MutableState<T> =
        values[def.name] as MutableState<T>

    fun <T : Any> get(def: XpOptionDef<T>): T = stateOf(def).value
    fun <T : Any> set(def: XpOptionDef<T>, v: T) { stateOf(def).value = v }

    fun allDefs(): List<XpOptionDef<*>> = defs

    // Run on Dispatchers.IO
    suspend fun saveTo(path: String) = withContext(Dispatchers.IO) {
        XpilotrcParser.write(path, defs, ::get)
    }

    companion object {
        fun load(path: String? = null): AppConfig {
            val defs = XpOptionRegistry.all
            // Read values from disk on IO thread; called before first composition
            val fileValues: Map<String, Any> = if (path != null) {
                XpilotrcParser.read(path, defs)  // pure, no Compose
            } else emptyMap()
            val states = defs.associate { def ->
                @Suppress("UNCHECKED_CAST")
                def.name to mutableStateOf(fileValues[def.name] ?: def.defaultValue as Any)
            }
            return AppConfig(defs, states)
        }

        // For tests and previews: create an instance with all defaults, no file I/O
        fun defaults(): AppConfig = load(path = null)
    }
}

val LocalAppConfig = staticCompositionLocalOf<AppConfig> {
    error("No AppConfig provided — wrap with CompositionLocalProvider(LocalAppConfig provides ...)")
}
```

### xpilotrc file format

```
xpilot.power            : 55.0
xpilot.turnSpeed        : 16.0
xpilot.showShipShapes   : yes
; xpilot.sparkSize       : 2      ← commented-out default
```

`XpilotrcParser` is pure Kotlin (no Compose), safe to call from `Dispatchers.IO`:
- **Read:** split on first `:`, strip `xpilot.` prefix (case-insensitive), look up `XpOptionDef` by name, parse value by def type. Returns `Map<String, Any>`.
- **Write:** for each def, if `value == def.defaultValue` emit a commented-out line; otherwise emit the live value. `NEVER_SAVE` defs skipped; `KEEP` defs emitted only if present in the original file. Non-matching lines (comments, blanks) are preserved verbatim.

### OptionRow — visitor pattern (`components/OptionRow.kt`)

Rather than a `when (option is ...)` dispatch in a single function (which requires touching `OptionRow` every time a new option type is added), use an `OptionRowRenderer` interface:

```kotlin
interface OptionRowRenderer {
    @Composable fun Bool(def: XpOptionDef.Bool, value: Boolean, onSet: (Boolean) -> Unit)
    @Composable fun Int(def: XpOptionDef.Int, value: Int, onSet: (Int) -> Unit)
    @Composable fun Double(def: XpOptionDef.Double, value: Double, onSet: (Double) -> Unit)
    @Composable fun ColorIndex(def: XpOptionDef.ColorIndex, value: Int, onSet: (Int) -> Unit)
}

// Default implementation — can be replaced in tests/previews without touching core logic
object DefaultOptionRowRenderer : OptionRowRenderer {
    @Composable override fun Bool(def, value, onSet) {
        Row { Text(def.name); Spacer(Modifier.weight(1f)); Switch(checked = value, onCheckedChange = onSet) }
    }
    @Composable override fun Int(def, value, onSet) {
        Row {
            Text(def.name); Spacer(Modifier.weight(1f))
            IconButton(onClick = { if (value > def.min) onSet(value - 1) }) { Text("◀") }
            Text("$value")
            IconButton(onClick = { if (value < def.max) onSet(value + 1) }) { Text("▶") }
        }
    }
    // Double and ColorIndex similarly...
}

@Composable
fun OptionRow(
    def: XpOptionDef<*>,
    config: AppConfig,
    renderer: OptionRowRenderer = DefaultOptionRowRenderer,
) {
    when (def) {
        is XpOptionDef.Bool       -> renderer.Bool(def, config.get(def)) { config.set(def, it) }
        is XpOptionDef.ColorIndex -> renderer.ColorIndex(def, config.get(def)) { config.set(def, it) }
        is XpOptionDef.Int        -> renderer.Int(def, config.get(def)) { config.set(def, it) }
        is XpOptionDef.Double     -> renderer.Double(def, config.get(def)) { config.set(def, it) }
        is XpOptionDef.Str        -> { /* string options not shown in config UI */ }
    }
}
```

Adding a new option type requires adding a branch to `XpOptionDef` and a method to `OptionRowRenderer`. The `OptionRow` `when` expression remains the single dispatch point and Kotlin's exhaustive `when` enforces completeness at compile time.

### Config screen layout

```
┌────────────────────────────────┐
│  Settings       [SAVE] [CLOSE] │
│  ┌──────────┬───────────────┐  │
│  │ DEFAULT  │    COLORS     │  │
│  ├──────────┴───────────────┤  │
│  │ power          ◀ 55.0 ▶  │  │
│  │ turnSpeed      ◀ 16.0 ▶  │  │
│  │ showShipShapes  [YES]     │  │
│  │  ...                     │  │
│  └───────────────────────────┘  │
└────────────────────────────────┘
```

`LazyColumn` is appropriate here — the full option list is ~55 entries across both tabs.

### Kotlin / Compose notes
- `TabRow` with two tabs: DEFAULT and COLORS — replaces the C client's separate `Config(what)` entry points
- `Int`/`Double` stepper: long-press on ▶ / ◀ uses `InteractionSource` + `pointerInput` to repeat while held
- Save triggers `scope.launch { config.saveTo(path) }` and shows a `Snackbar` on completion
- `AppConfig.defaults()` is used for previews and tests — no file I/O, no `@Preview` limitations in commonMain. Desktop previews use a `main()` function.

### Migration steps
- [ ] Define `XpOptionDef` sealed hierarchy in `config/XpOptionDef.kt`
- [ ] Define `XpOptionRegistry.all` list with all options from the C `default_options[]` and color options
- [ ] Implement `XpilotrcParser.read()` and `XpilotrcParser.write()` — pure Kotlin, unit-testable
- [ ] Write round-trip unit tests: parse a known xpilotrc string, verify values; mutate values, write, re-parse, verify
- [ ] Implement `AppConfig` class
- [ ] Implement `OptionRowRenderer` interface and `DefaultOptionRowRenderer`
- [ ] Implement `OptionRow` composable
- [ ] Implement `ConfigScreen` with DEFAULT and COLORS tabs
- [ ] Wire Save button

---

## 3. Score / Scoreboard Overlay

### Source reference
- `x11/xinit.c` — `playersWindow` (always-visible sidebar in C client)
- `JXPilot/game/PlayerTable.java` — `PlayerTable implements Drawable`
- Columns: Name, Lives, Score; grouped by team

### Purpose
In-game semi-transparent overlay listing all players with name, lives, and score grouped by team. Toggled by a key binding (not always-visible as in the C client — the game canvas takes the full window).

### Domain model (`model/InGameModels.kt`)

```kotlin
data class PlayerInfo(
    val id: Int,
    val name: String,
    val lives: Int,
    val score: Double,
    val team: Int,        // -1 = no team
    val isSelf: Boolean,
)
```

### Layout

```
┌─────────────────────────┐
│ SCOREBOARD              │
│ ─────────────────────── │
│ Team 0                  │
│   Alice   3  1024.5     │
│   ● Bob   1   512.0     │  ← ● = self
│ Team 1                  │
│   Charlie 0    —        │  ← zero lives greyed
│ ─────────────────────── │
│ No team                 │
│   Dave    5  3200.0     │
└─────────────────────────┘
```

### Kotlin / Compose notes
- A plain `Column` — typical XPilot server has ≤ 32 players. `LazyColumn` is not warranted; the overhead of a `LazyListState` and item recycling is unneeded for a list this small.
- `AnimatedVisibility(visible = showScoreboard)` with `fadeIn() + slideInHorizontally()` from the right edge
- Semi-transparent dark background (`Color.Black.copy(alpha = 0.75f)`) rendered as a `Box` overlay inside `InGameScreen`
- Team header rows use a distinct background tint
- Zero-lives row uses `KXPilotColors.zeroLivesColor` (maps to `guiobject_options.zeroLivesColor` config index)
- Score formatted using `showScoreDecimals` config option: `"%.${decimals}f".format(score)`
- Toggle via `InGameStateHolder.toggleScoreboard()` called from the key event handler

### Migration steps
- [ ] Define `PlayerInfo` in `model/InGameModels.kt`
- [ ] Implement `ScoreOverlay` composable
- [ ] Stub with a hardcoded player list
- [ ] Add toggle key binding in `InGameScreen` key handler

---

## 4. Key Bindings Editor

### Source reference
- `x11/about.c` — `Keys_callback()`, read-only scrollable key→action table
- C key actions: `KEY_TURN_LEFT`, `KEY_TURN_RIGHT`, `KEY_THRUST`, `KEY_FIRE_SHOT`, `KEY_SHIELD`, etc.

### Purpose
View and remap keyboard→action bindings. KXPilot makes it editable (the C client is read-only).

### Domain model (`model/KeyBindingModels.kt`)

```kotlin
enum class GameAction(val description: String) {
    TURN_LEFT("Turn left"),
    TURN_RIGHT("Turn right"),
    THRUST("Thrust"),
    FIRE_SHOT("Fire shot"),
    SHIELD("Shield"),
    RESPAWN("Respawn at base"),
    TALK("Open chat"),
    SCOREBOARD("Toggle scoreboard"),
}

data class KeyBinding(
    val action: GameAction,
    val primary: Key?,       // first / main binding; null = unbound
    val secondary: Key?,     // optional second binding (e.g. ← and A both for TURN_LEFT)
)

// Describes what the rebind UI is currently doing for one action
sealed class BindingMode {
    object Idle                                    : BindingMode()
    data class AwaitingPrimary(val action: GameAction)   : BindingMode()
    data class AwaitingSecondary(val action: GameAction) : BindingMode()
}
```

`KeyBinding` models exactly two slots (primary + secondary) matching the C client's multi-keysym convention. This is intentionally not `List<Key>` — an unbounded list introduces UX complexity for no practical gain; two bindings per action is the XPilot norm.

### State holder (UI layer)

```kotlin
class KeyBindingsStateHolder {
    var bindings by mutableStateOf(defaultBindings())
    var mode: BindingMode by mutableStateOf(BindingMode.Idle)

    /** Enter "press a key" capture for the primary slot of [action]. */
    fun startRebindPrimary(action: GameAction) {
        mode = BindingMode.AwaitingPrimary(action)
    }

    /** Enter "press a key" capture for the secondary slot of [action]. */
    fun startRebindSecondary(action: GameAction) {
        mode = BindingMode.AwaitingSecondary(action)
    }

    /**
     * Called by the window's onKeyEvent when mode != Idle.
     * Applies [key] to whichever slot is being awaited, then returns to Idle.
     * If [key] is already used by another action, the conflicting slot is cleared first.
     */
    fun applyKey(key: Key) {
        val current = mode
        if (current is BindingMode.Idle) return
        // Clear conflicts
        bindings = bindings.map { b ->
            when {
                b.primary == key   -> b.copy(primary = null)
                b.secondary == key -> b.copy(secondary = null)
                else               -> b
            }
        }
        // Apply to correct slot
        bindings = bindings.map { b ->
            if (b.action != (current as? BindingMode.AwaitingPrimary)?.action
                    ?: (current as? BindingMode.AwaitingSecondary)?.action) return@map b
            when (current) {
                is BindingMode.AwaitingPrimary   -> b.copy(primary = key)
                is BindingMode.AwaitingSecondary -> b.copy(secondary = key)
                else -> b
            }
        }
        mode = BindingMode.Idle
    }

    /** Cancel an in-progress rebind without changing anything. */
    fun cancelRebind() { mode = BindingMode.Idle }

    fun resetDefaults() { bindings = defaultBindings(); mode = BindingMode.Idle }
}
```

### Layout

```
┌────────────────────────────────────┐
│ Key Bindings        [RESET] [DONE] │
│ ──────────────────────────────────│
│ Action         Primary  Secondary  │
│ Turn left        ←         A       │
│ Turn right       →         D       │
│ Thrust           ↑         W       │
│ Fire shot        Space     —       │
│ Shield           S         —       │
│ Respawn          R         —       │
│                                    │
│ [Click Primary or Secondary to     │
│  rebind — press Esc to cancel]     │
└────────────────────────────────────┘
```

Each row shows two clickable `Button`s for Primary and Secondary slots. Clicking enters the corresponding `BindingMode`. The row highlights and shows "Press a key…" while awaiting. Pressing Escape calls `cancelRebind()`. Pressing any other key calls `applyKey()`.

### Kotlin / Compose notes
- `LazyColumn` of rows — 8+ actions, small but extensible as more actions are added
- `onKeyEvent` at the screen root: if `mode != Idle`, intercept the next `KeyDown` event and call `applyKey()` before passing it to game logic
- `Key.displayName()` extension maps Compose `Key` constants to human-readable strings (e.g. `Key.DirectionLeft → "←"`, `Key.Spacebar → "Space"`)
- Conflict resolution is shown inline: conflicted row briefly flashes before clearing
- Persist via `XpilotrcParser` using `xp_key_option` format (space-separated keysym names for primary then secondary)

### Migration steps
- [ ] Define `GameAction`, `KeyBinding`, `BindingMode` in `model/KeyBindingModels.kt`
- [ ] Implement `KeyBindingsStateHolder`
- [ ] Implement `Key.displayName()` extension
- [ ] Implement `KeyBindingsScreen` composable
- [ ] Refactor `composeKeyToKxpKey()` in `DemoScreen.kt` to use `KeyBinding` from `KeyBindingsStateHolder`
- [ ] Persist to xpilotrc

---

## 5. About Screen

### Source reference
- `x11/about.c` — 5-page popup: "About XPilot", "About XPilot NG", "Game Objective", "Bonus Items" ×2

### Purpose
Multi-page informational popup. KXPilot replaces the C client's fixed-page model with a single scrollable screen — `LazyColumn` makes pagination unnecessary.

### Content sections
1. About KXPilot — origin story, credits, license
2. Game Objective — gameplay description
3. Bonus Items — item list with icons and descriptions (replaces 21 XBM bitmaps with Canvas draws)
4. Credits / Links

### Kotlin / Compose notes
- `LazyColumn` with `item {}` / `items {}` blocks per section; section headers styled separately
- Item icons: small `Canvas`-drawn shapes as placeholders; swap for bundled PNG resources later
- `onDismiss: () -> Unit` lambda → calls `navigator.pop()`
- Content strings defined as Kotlin constants in `AboutContent.kt` or loaded from a string resource file
- Desktop preview via `main()` runner

### Migration steps
- [ ] Write content as Kotlin string constants
- [ ] Implement `AboutScreen` composable
- [ ] Add placeholder item icons
- [ ] Wire navigation (`navigator.pop()` on Close)

---

## 6. MOTD Screen

### Source reference
- `x11/about.c` — `Handle_motd(off, buf, len, filesize)` streaming chunks; `Widget_create_viewer()`

### Purpose
Display the server's message of the day. Auto-shown on join if non-empty; also accessible from the in-game menu.

### Domain model + state holder

```kotlin
// Domain — no Compose
sealed class MotdState {
    object Loading                                      : MotdState()
    data class Receiving(val progress: Float)           : MotdState()  // 0f–1f
    data class Loaded(val text: String, val server: String) : MotdState()
    object Empty                                        : MotdState()
    data class Error(val message: String)               : MotdState()
}

// UI layer
class MotdStateHolder(private val serverName: String) {
    var state: MotdState by mutableStateOf(MotdState.Loading)

    // Internal buffer for streaming assembly — not exposed as state
    private val buffer = StringBuilder()
    private var expectedSize = 0L

    /**
     * Called by the network layer (must be called on Main thread or via
     * withContext(Dispatchers.Main)) with each arriving chunk.
     * [offset] ensures out-of-order chunks are inserted correctly.
     */
    fun appendChunk(offset: Long, chunk: String, totalSize: Long) {
        // Simple append strategy: assume in-order delivery for now.
        // A full implementation would use a sorted TreeMap<Long, String>
        // and flush contiguous ranges.
        expectedSize = totalSize
        buffer.append(chunk)
        state = MotdState.Receiving(
            progress = buffer.length.toFloat() / totalSize.coerceAtLeast(1)
        )
    }

    fun onComplete() {
        state = if (buffer.isBlank()) {
            MotdState.Empty
        } else {
            MotdState.Loaded(buffer.toString(), serverName)
        }
    }

    fun onError(message: String) { state = MotdState.Error(message) }
}
```

### Layout
```
┌─────────────────────────────────────┐
│ MOTD — <servername>         [CLOSE] │
│ ────────────────────────────────── │
│  [████████████░░  72%]             │  ← Receiving state
│                                     │
│  — or —                             │
│                                     │
│  <scrollable monospace text>        │  ← Loaded state
└─────────────────────────────────────┘
```

### Kotlin / Compose notes
- Shown as a `Dialog` composable with `onDismissRequest = { navigator.pop() }`
- `Loading` → `CircularProgressIndicator`
- `Receiving` → `LinearProgressIndicator(progress = state.progress)`
- `Loaded` → `LazyColumn` of text lines (monospace `TextStyle`); one `Text` per line for efficient recomposition on streaming updates — avoids recomposing a single giant `Text` block each chunk
- `Empty` → plain `Text("This server has no MOTD.")`
- `Error` → `Text` with error color

### Migration steps
- [ ] Implement `MotdStateHolder`
- [ ] Implement `MotdScreen` composable as a `Dialog`
- [ ] Stub with a hardcoded multi-line MOTD string
- [ ] Wire to network layer when available

---

## 7. Talk / Chat Input Overlay

### Source reference
- `x11/talk.c` — `Talk_create_window()`, cursor blink, message history, X selection paste

### Purpose
In-game text entry bar overlaid on the game viewport. Activated by a key binding; Enter sends, Escape cancels.

### Domain model + state holder

```kotlin
// Domain
sealed class TalkResult {
    data class Send(val message: String) : TalkResult()
    object Cancel                        : TalkResult()
}
```

```kotlin
// UI layer — @MainThread
class TalkStateHolder {
    var isVisible  by mutableStateOf(false)
    var text       by mutableStateOf("")
    private val history    = ArrayDeque<String>(32)  // not observable — only accessed on Main
    private var historyPos = -1

    fun open() {
        text = ""; historyPos = -1; isVisible = true
    }

    fun close() { isVisible = false }

    /**
     * Returns [TalkResult.Send] with the current text, or [TalkResult.Cancel] if blank.
     * In both cases the overlay is closed and history is updated (on Send).
     */
    fun submit(): TalkResult {
        val msg = text.trim()
        isVisible = false
        return if (msg.isEmpty()) {
            TalkResult.Cancel
        } else {
            if (history.isEmpty() || history.first() != msg) {
                history.addFirst(msg)
                if (history.size > 32) history.removeLast()
            }
            TalkResult.Send(msg)
        }
    }

    /** Browse history. [delta] = +1 older, -1 newer. */
    fun browseHistory(delta: kotlin.Int) {
        if (history.isEmpty()) return
        historyPos = (historyPos + delta).coerceIn(-1, history.size - 1)
        text = if (historyPos < 0) "" else history[historyPos]
    }
}
```

`submit()` returns a sealed `TalkResult` — the caller (InGameScreen) handles `Send` by forwarding to the network layer and `Cancel` by doing nothing. The state holder does not know about network; it returns the message and lets the caller decide what to do with it.

### Layout
```
╔══════════════════════════════════════════════╗
║  Talk: [_________________________________|]  ║
╚══════════════════════════════════════════════╝
```
Positioned at 75% screen height, width = 80% of viewport.

### Kotlin / Compose notes
- `AnimatedVisibility(isVisible)` with `slideInVertically { it } + fadeIn()`
- `BasicTextField` with `onValueChange` writing to `talkState.text`
- `onKeyEvent` in the `BasicTextField`: `Enter` → `submit()`, `Escape` → `close()`, `Up`/`Down` → `browseHistory(±1)`
- `LaunchedEffect(isVisible) { if (isVisible) focusRequester.requestFocus() }`
- All calls to `talkState` happen on the composition thread (Main) — no threading concern
- `InGameScreen` handles the returned `TalkResult.Send` in a `LaunchedEffect` that forwards to the network layer

### Migration steps
- [ ] Define `TalkResult` in `model/InGameModels.kt`
- [ ] Implement `TalkStateHolder`
- [ ] Implement `TalkOverlay` composable
- [ ] Wire to `InGameScreen` key handler
- [ ] Stub: log `TalkResult.Send` to the HUD message list

---

## 8. In-Game HUD Layout

### Source reference
- `x11/xinit.c` — window layout: `drawWindow`, `radarWindow`, `playersWindow`, button bar
- `x11/painthud.c` — HUD: fuel gauge, power meter, turn-speed, packet meters, dirPtr, message log

### Purpose
The full game screen: game viewport canvas with HUD overlays, radar minimap, and message log. Refactored from `DemoScreen.kt`.

### Domain models (`model/InGameModels.kt`)

```kotlin
enum class MessageColor { NORMAL, BALL, SAFE, COVER, POP }

data class MessageEntry(
    val text: String,
    val color: MessageColor,
    val arrivedAt: Long,     // System.currentTimeMillis() or equivalent — NOT frame count
)
// Staleness is computed on read: (now - arrivedAt) / displayDurationMs
// No per-frame allocation; the list is only modified when messages arrive or expire.

data class HudState(
    val fuel: Float,           // current fuel, 0–1000
    val fuelMax: Float,        // max fuel capacity
    val power: Float,          // normalised 0–1 for power meter
    val turnSpeed: Float,      // normalised 0–1 for turn-speed meter
    val packetLoss: Float,     // normalised 0–1
    val packetLag: Float,      // normalised 0–1
    val directionRad: Float,   // ship heading in radians (Y-up)
    val messages: List<MessageEntry>,  // bounded by maxMessages config option
    val score: Double,
    val lives: Int,
)
```

Using `arrivedAt: Long` (wall-clock millis) means:
- `HudState` is only reallocated when game state actually changes, not every frame
- Alpha fade = `1f - (now - msg.arrivedAt) / displayMs` computed at draw time — pure, no allocation

### Message log — fixed-slot Column

The message log shows at most `maxMessages` entries (default 8, max 32). It is **not** a `LazyColumn`:

```kotlin
@Composable
fun MessageLog(messages: List<MessageEntry>, maxMessages: Int, displayMs: Long) {
    val now = remember { derivedStateOf { System.currentTimeMillis() } }
    // Filter expired messages on read
    val visible = messages
        .filter { (now.value - it.arrivedAt) < displayMs }
        .takeLast(maxMessages)

    Column(verticalArrangement = Arrangement.Bottom) {
        for (msg in visible) {
            val age = (now.value - msg.arrivedAt).toFloat() / displayMs
            Text(
                text = msg.text,
                color = msg.color.toComposeColor().copy(alpha = 1f - age.coerceIn(0f, 1f)),
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
            )
        }
    }
}
```

`derivedStateOf` around `now` prevents recomposition unless time has moved enough to change visible messages. In practice, a `LaunchedEffect` that sets a `currentTimeMs` state variable at a throttled rate (e.g. every 100 ms) is cleaner than reading the clock inline.

### Layout

```
┌────────────────────────────────────────────┐
│                                 ┌────────┐ │
│   Game viewport (Canvas)        │ Radar  │ │ ← top-right, fixed 180×180 dp
│                                 └────────┘ │
│                                            │
│  ┌─────────────────────────────────────┐   │
│  │ Fuel [████████░░░] 450 / 1000       │   │ ← bottom-left stack
│  │ Pwr  [██████░░░░░]                  │   │
│  │ Spd  [████░░░░░░░]                  │   │
│  └─────────────────────────────────────┘   │
│                                            │
│  [Server] Game starts in 10 seconds        │ ← message log, bottom-right
│  [Alice] Killed Bob  +50                   │   plain Column, max 8 rows
│                                            │
│  ╔════════════════════════════════════╗    │
│  ║ Talk: [________________________|] ║    │ ← TalkOverlay, 75% height
│  ╚════════════════════════════════════╝    │
│                                            │
│  ┌─────────────────────────┐              │
│  │ SCOREBOARD (overlay)    │              │ ← ScoreOverlay, top-right or center
│  └─────────────────────────┘              │
└────────────────────────────────────────────┘
```

### Kotlin / Compose notes
- Root: `Box(Modifier.fillMaxSize())` — game `Canvas` fills the box, HUD elements are child `Box`es with `Modifier.align(...)`
- `MeterBar(value, max, color, label)` composable: a `Row` with a `Canvas`-drawn filled rect bar
- Radar: a dedicated `Canvas` composable (fixed `180.dp × 180.dp`), draws wall blips and ship blips using a scaled-down `worldToScreen` equivalent
- `MessageLog` composable: plain `Column`, see above
- `ScoreOverlay` and `TalkOverlay` are layered above the Canvas via `Box` children with `Modifier.align`
- HUD colors resolved via `LocalAppConfig.current.get(XpOptionRegistry.hudColor)` → `KXPilotColors.palette[index]`
- `InGameScreen` refactors `DemoScreen.kt`: the `Canvas` draw logic is preserved; the debug `drawHud()` text overlay is replaced by proper composable HUD elements

### Migration steps
- [ ] Define `HudState`, `MessageEntry`, `MessageColor`, `TalkResult`, `PlayerInfo` in `model/InGameModels.kt`
- [ ] Refactor `DemoScreen.kt` → `InGameScreen.kt`; move to `shared/commonMain`
- [ ] Implement `MeterBar` composable
- [ ] Implement radar minimap composable
- [ ] Implement `MessageLog` composable with timestamp-based fade
- [ ] Implement `ScoreOverlay` (see §3)
- [ ] Implement `TalkOverlay` (see §7)
- [ ] Wire key handlers for scoreboard and chat toggles
- [ ] Replace debug `drawHud()` text with proper HUD composables

---

## Implementation Order

Dependencies between components:

```
Navigator + App scaffold
  └── AboutScreen                    (no deps — validates routing first)
  └── model/KeyBindingModels.kt
        └── KeyBindingsScreen        (validates onKeyEvent capture pattern)
  └── model/ServerModels.kt
        └── MainMenuScreen           (validates navigation to InGame)
  └── config/XpOptionDef.kt
        └── config/XpOptionRegistry.kt
              └── config/XpilotrcParser.kt  (pure Kotlin, unit-tested independently)
                    └── config/AppConfig.kt
                          └── ConfigScreen
  └── MotdScreen                     (Dialog composable, stub content)
  └── model/InGameModels.kt
        └── InGameScreen / HUD       (refactor DemoScreen)
              └── ScoreOverlay
              └── TalkOverlay
```

### Recommended sequence

1. **`Navigator` + `App` root** — routing scaffold with `Screen` sealed class; no screen content yet
2. **`AboutScreen`** — static content, validates `navigator.push()` / `navigator.pop()`, no state holder
3. **`model/KeyBindingModels.kt` + `KeyBindingsScreen`** — validates `onKeyEvent` capture; refactors the existing `composeKeyToKxpKey()` mapping
4. **`MainMenuScreen`** — validates navigation to `InGame`; stub server list
5. **`XpOptionDef` + `XpOptionRegistry` + `XpilotrcParser`** — pure Kotlin; write and pass unit tests before touching UI
6. **`AppConfig` + `ConfigScreen`** — wire live options; Save button; both tabs
7. **`MotdScreen`** — `Dialog` composable, stub content
8. **`model/InGameModels.kt` + `InGameScreen` HUD** — refactor `DemoScreen`; add `MeterBar`, radar, `MessageLog`
9. **`ScoreOverlay`** — layered inside `InGameScreen`
10. **`TalkOverlay`** — layered inside `InGameScreen`

Persistence (`XpilotrcParser`) is implemented at step 5, after the scaffold and two static screens confirm the architecture is sound — not as the very first task.

---

## Progress Tracker

| # | Screen / Component | Status | Notes |
|---|---|---|---|
| — | `Navigator` + `App` scaffold | Not started | |
| — | `KXPilotColors` theme | Not started | |
| 5 | `AboutScreen` | Not started | First routing validation |
| 4 | `KeyBindingsScreen` | Not started | |
| 1 | `MainMenuScreen` | Not started | |
| — | `XpOptionDef` + `XpOptionRegistry` | Not started | Pure domain, no Compose |
| — | `XpilotrcParser` (read + write) | Not started | Pure Kotlin, unit-tested |
| — | `AppConfig` | Not started | Compose adapter over registry |
| 2 | `ConfigScreen` | Not started | |
| 6 | `MotdScreen` | Not started | Dialog + stub MOTD |
| 8 | `InGameScreen` / HUD | Not started | Refactor from DemoScreen |
| 3 | `ScoreOverlay` | Not started | |
| 7 | `TalkOverlay` | Not started | |
