# XPilot-NG → KXPilot Port Inventory

This is the master iterable checklist of every C type (struct, enum, typedef, union, constant group) that must be ported to Kotlin.

**Columns:**
- **C Name** — original C identifier
- **Source File** — relative to `src/`
- **Category** — `enum`, `struct`, `typedef`, `union`, `constants`
- **Kotlin Name** — target Kotlin identifier
- **Kotlin Kind** — `data class`, `enum class`, `sealed class`, `value class`, `object`, `typealias`
- **Java Equiv** — closest class in `JXPilot/legacy/jxpilot` (if any)
- **Status** — `TODO` / `IN PROGRESS` / `DONE`
- **Notes**

---

## Module: common/

### common/types.h

| C Name | Source File | Category | Kotlin Name | Kotlin Kind | Java Equiv | Status | Notes |
|---|---|---|---|---|---|---|---|
| `byte` (typedef signed char) | common/types.h | typedef | — | `Byte` (built-in) | — | DONE | No Kotlin type needed; use `Byte` directly |
| `u_byte` (typedef unsigned char) | common/types.h | typedef | — | `UByte` (built-in) | — | DONE | No Kotlin type needed; use `UByte` directly |
| `bool` (macro over char) | common/types.h | typedef | — | `Boolean` (built-in) | — | DONE | No Kotlin type needed |
| `vector_t` (float x, y) | common/types.h | struct | `Vector` | `data class` | — | DONE | `data class Vector(val x: Float, val y: Float)` |
| `position_t` (alias of vector_t) | common/types.h | typedef | `Position` | `typealias` | — | DONE | `typealias Position = Vector` |
| `ivec_t` (int x, y) | common/types.h | struct | `IVec` | `data class` | — | DONE | `data class IVec(val x: Int, val y: Int)` |
| `ipos_t` (alias of ivec_t) | common/types.h | typedef | `IPos` | `typealias` | — | DONE | `typealias IPos = IVec` |
| `irec_t` (int x, y, w, h) | common/types.h | struct | `IRect` | `data class` | — | DONE | `data class IRect(val x: Int, val y: Int, val w: Int, val h: Int)` |
| `blkvec_t` (int bx, by) | common/types.h | struct | `BlkVec` | `data class` | — | DONE | `data class BlkVec(val bx: Int, val by: Int)` |
| `blkpos_t` (alias of blkvec_t) | common/types.h | typedef | `BlkPos` | `typealias` | — | DONE | `typealias BlkPos = BlkVec` |

### common/click.h

| C Name | Source File | Category | Kotlin Name | Kotlin Kind | Java Equiv | Status | Notes |
|---|---|---|---|---|---|---|---|
| `click_t` (typedef int) | common/click.h | typedef | `Click` | `typealias` | — | DONE | `typealias Click = Int` |
| `clpos_t` (click_t cx, cy) | common/click.h | struct | `ClPos` | `data class` | — | DONE | `data class ClPos(val cx: Int, val cy: Int)` |
| `clvec_t` (click_t cx, cy) | common/click.h | struct | `ClVec` | `data class` | — | DONE | `data class ClVec(val cx: Int, val cy: Int)` |
| Click conversion constants | common/click.h | constants | `ClickConst` | `object` | — | DONE | `CLICK_SHIFT=6`, `CLICK=64`, `PIXEL_CLICKS`, `BLOCK_CLICKS`; conversion funs as extensions |

### common/const.h

| C Name | Source File | Category | Kotlin Name | Kotlin Kind | Java Equiv | Status | Notes |
|---|---|---|---|---|---|---|---|
| Game constants (`RES`, `BLOCK_SZ`, `MAX_CHARS`, `MSG_LEN`, etc.) | common/const.h | constants | `GameConst` | `object` | — | DONE | All `#define` numeric constants become `const val` in an `object GameConst` |
| Colour constants (`BLACK`, `WHITE`, `BLUE`, `RED`, `NUM_COLORS`) | common/const.h | constants | `GameColor` | `enum class` | — | DONE | `enum class GameColor { BLACK, WHITE, BLUE, RED }` with `val index: Int` |
| View size constants (`MIN_VIEW_SIZE`, `MAX_VIEW_SIZE`, `DEF_VIEW_SIZE`) | common/const.h | constants | `GameConst` | `object` | — | DONE | Added to `GameConst` object |
| Polygon style flags (`STYLE_FILLED`, etc.) | common/const.h | constants | `PolygonStyleFlags` | `object` | — | DONE | Bit-flag constants in `PolygonStyleFlags` object |
| Physics limits (`SPEED_LIMIT`, `MAX_PLAYER_POWER`, etc.) | common/const.h | constants | `GameConst` | `object` | — | DONE | Grouped with other constants |

### common/item.h

| C Name | Source File | Category | Kotlin Name | Kotlin Kind | Java Equiv | Status | Notes |
|---|---|---|---|---|---|---|---|
| `Item_t` enum (NO_ITEM=-1 … NUM_ITEMS=21) | common/item.h | enum | `Item` | `enum class` | — | DONE | `NO_ITEM` excluded; use `Item?` nullable; `val id` and `val bit` properties on each entry |
| `ITEM_BIT_*` flags | common/item.h | constants | `Item` companion | `companion object` | — | DONE | `val bit: Int` property on each `Item` enum entry |

### common/keys.h

| C Name | Source File | Category | Kotlin Name | Kotlin Kind | Java Equiv | Status | Notes |
|---|---|---|---|---|---|---|---|
| `keys_t` enum (KEY_DUMMY … NUM_KEYS, plus client-only keys) | common/keys.h | enum | `Key` | `enum class` | — | DONE | Split at `NUM_KEYS`; client-only keys annotated `// client only` |

### common/rules.h

| C Name | Source File | Category | Kotlin Name | Kotlin Kind | Java Equiv | Status | Notes |
|---|---|---|---|---|---|---|---|
| Game mode bit constants (`CRASH_WITH_PLAYER`, `TEAM_PLAY`, etc.) | common/rules.h | constants | `GameMode` | `object` | — | DONE | Bit flags for `rules_t.mode` |
| Old player status bits (`OLD_PLAYING`, `OLD_PAUSE`, `OLD_GAME_OVER`) | common/rules.h | constants | `OldPlayerStatus` | `object` | — | DONE | Network-protocol-only bits |
| `rules_t` (int lives, long mode) | common/rules.h | struct | `Rules` | `data class` | — | DONE | `data class Rules(val lives: Int, val mode: Long)` |

### common/setup.h

| C Name | Source File | Category | Kotlin Name | Kotlin Kind | Java Equiv | Status | Notes |
|---|---|---|---|---|---|---|---|
| Map block type constants (`SETUP_SPACE` … `SETUP_ASTEROID_CONCENTRATOR`) | common/setup.h | constants | `MapBlock` | `object` | — | DONE | ~40 integer constants |
| Map compression/order constants (`SETUP_COMPRESSED`, `SETUP_MAP_ORDER_*`) | common/setup.h | constants | `MapBlock` | `object` | — | DONE | Added to same `MapBlock` object |
| Tile render flags (`BLUE_UP`, `BLUE_BIT`, `DECOR_*`) | common/setup.h | constants | `TileFlags` | `object` | — | DONE | Bit-flag rendering hints |
| `setup_t` struct | common/setup.h | struct | `Setup` | `data class` | — | DONE | `mapData` field is `ByteArray`; `data_url` as `String` |

### common/shipshape.h

| C Name | Source File | Category | Kotlin Name | Kotlin Kind | Java Equiv | Status | Notes |
|---|---|---|---|---|---|---|---|
| `shape_t` struct | common/shipshape.h | struct | `Shape` | `class` | — | DONE | Contains pre-rotated point arrays; mutable `class` with `Array<Array<ClPos>>` |
| `shipshape_t` struct | common/shipshape.h | struct | `ShipShape` | `class` | — | DONE | Extends shape; engine/gun/light/rack positions indexed by direction; mutable class |

### common/packet.h

| C Name | Source File | Category | Kotlin Name | Kotlin Kind | Java Equiv | Status | Notes |
|---|---|---|---|---|---|---|---|
| `PKT_*` constants (0–102, 128) | common/packet.h | constants | `PacketType` | `object` | `net/packet/` classes | DONE | `object PacketType { const val UNDEFINED = 0; … }` |
| `KEYBOARD_SIZE` constant | common/packet.h | constants | `PacketType` | `object` | — | DONE | Added as `const val KEYBOARD_SIZE = 9` |

---

## Module: server/

### server/modifiers.h

| C Name | Source File | Category | Kotlin Name | Kotlin Kind | Java Equiv | Status | Notes |
|---|---|---|---|---|---|---|---|
| `modifiers_t` (typedef uint16_t) | server/modifiers.h | typedef | `Modifiers` | `@JvmInline value class` | — | DONE | `@JvmInline value class Modifiers(val raw: UShort)`; get/set extension funs |
| `modifier_t` enum (ModsNuclear … ModsLaser) | server/modifiers.h | enum | `Modifier` | `enum class` | — | DONE | 8 entries; bit position/shift encoded into enum |
| `MODS_*` bit constants | server/modifiers.h | constants | `Modifier` companion | `companion object` | — | DONE | `MODS_NUCLEAR`, `MODS_FULLNUCLEAR`, `MODS_LASER_STUN`, `MODS_LASER_BLIND` as `val` |
| `MODS_*_MAX` limit constants | server/modifiers.h | constants | `Modifier` companion | `companion object` | — | DONE | Per-modifier max values |

### server/map.h

| C Name | Source File | Category | Kotlin Name | Kotlin Kind | Java Equiv | Status | Notes |
|---|---|---|---|---|---|---|---|
| Map cell type constants (`SPACE`, `BASE`, `FILLED`, … `ASTEROID_CONCENTRATOR`) | server/map.h | constants | `CellType` | `object` | — | DONE | ~28 integer constants |
| Direction constants (`DIR_RIGHT`, `DIR_UP`, `DIR_LEFT`, `DIR_DOWN`) | server/map.h | constants | `Direction` | `enum class` | — | DONE | Values in RES units |
| `fuel_t` struct | server/map.h | struct | `Fuel` | `data class` | — | DONE | pos: ClPos, fuel: Double, connMask: UInt, lastChange: Long, team: Int |
| `grav_t` struct | server/map.h | struct | `Grav` | `data class` | — | DONE | pos: ClPos, force: Double, type: Int |
| `base_t` struct | server/map.h | struct | `Base` | `data class` | — | DONE | pos, dir, ind, team, order, initialItems: IntArray(NUM_ITEMS) |
| `cannon_t` struct | server/map.h | struct | `Cannon` | `data class` | — | DONE | Large struct; ~20 fields; item IntArray included |
| `check_t` struct | server/map.h | struct | `Check` | `data class` | — | DONE | Single field: pos: ClPos |
| `item_t` struct (server item config) | server/map.h | struct | `ItemConfig` | `data class` | — | DONE | Renamed to avoid clash with `Item` enum |
| `asteroid_t` struct (world asteroid config) | server/map.h | struct | `AsteroidConfig` | `data class` | — | DONE | prob, max, num, chance fields |
| `wormtype_t` enum | server/map.h | enum | `WormType` | `enum class` | — | DONE | NORMAL, IN, OUT, FIXED |
| `wormhole_t` struct | server/map.h | struct | `Wormhole` | `data class` | — | DONE | |
| `treasure_t` struct | server/map.h | struct | `Treasure` | `data class` | — | DONE | |
| `target_t` struct | server/map.h | struct | `Target` | `data class` | — | DONE | |
| `team_t` struct | server/map.h | struct | `Team` | `data class` | — | DONE | |
| `item_concentrator_t` struct | server/map.h | struct | `ItemConcentrator` | `data class` | — | DONE | Single field: pos: ClPos |
| `asteroid_concentrator_t` struct | server/map.h | struct | `AsteroidConcentrator` | `data class` | — | DONE | Single field: pos: ClPos |
| `friction_area_t` struct | server/map.h | struct | `FrictionArea` | `data class` | — | DONE | pos, frictionSetting, friction, group |
| `ecm_t` struct (server version) | server/map.h | struct | `Ecm` | `data class` | — | DONE | size: Double, pos: ClPos, id: Int |
| `transporter_t` struct | server/map.h | struct | `Transporter` | `data class` | — | DONE | pos, victimId, id, count |
| `world` struct | server/map.h | struct | `World` | `class` (singleton-ish) | — | DONE | Large mutable state container; MutableLists of map objects |

### server/object.h

| C Name | Source File | Category | Kotlin Name | Kotlin Kind | Java Equiv | Status | Notes |
|---|---|---|---|---|---|---|---|
| `OBJ_*` type constants | server/object.h | constants | `ObjType` | `object` | — | DONE | 14 constants (PLAYER … CANNON_SHOT) |
| `OBJ_*_BIT` bit constants | server/object.h | constants | `ObjType` | `object` | — | DONE | Bit values derived from type constants |
| Object status bit constants (`GRAVITY`, `WARPING`, etc.) | server/object.h | constants | `ObjStatus` | `object` | — | DONE | 11 bit flags |
| Lock constants (`LOCK_NONE`, `LOCK_PLAYER`, `LOCK_VISIBLE`) | server/object.h | constants | `LockMode` | `object` | — | DONE | |
| `OBJECT_BASE` macro fields | server/object.h | struct (base) | `GameObjectBase` | `abstract class` | — | DONE | id, team, pos, prevPos, extMove, wallTime, vel, acc, mass, life, mods, type, color, collMode, missileDir, wormHoleHit, wormHoleDest, objStatus |
| `OBJECT_EXTEND` macro fields | server/object.h | struct (mixin) | `GameObjectBase` | `abstract class` | — | DONE | Merged into same abstract base: cell, plRange, plRadius, fuse |
| `cell_node_t` struct | server/object.h | struct | `CellNode` | `class` | — | DONE | Replaced with `MutableList` position tracking in `GameObjectBase` |
| `object_t` (generic) | server/object.h | struct | `GameObject` | `open class` | — | DONE | Extends `GameObjectBase` with no extra fields |
| `mineobject_t` | server/object.h | struct | `MineObject` | `data class` | — | DONE | Extends `GameObjectBase`; extra: mineCount, mineEcmRange, mineSpreadLeft, mineOwner |
| `missileobject_t` | server/object.h | struct | `MissileObject` | `open class` | — | DONE | Extends `GameObjectBase`; adds maxSpeed, turnspeed |
| `smartobject_t` | server/object.h | struct | `SmartObject` | `data class` | — | DONE | Extends `MissileObject`; adds ecmRange, count, lockId, relockId |
| `torpobject_t` | server/object.h | struct | `TorpObject` | `data class` | — | DONE | Extends `MissileObject`; adds spreadLeft, count |
| `heatobject_t` | server/object.h | struct | `HeatObject` | `data class` | — | DONE | Extends `MissileObject`; adds count, lockId |
| `ballobject_t` | server/object.h | struct | `BallObject` | `data class` | — | DONE | Extends `GameObjectBase`; adds looseTicks, treasure ref, owner, style |
| `wireobject_t` | server/object.h | struct | `WireObject` | `data class` | — | DONE | Extends `GameObjectBase`; adds turnSpeed, wireType, wireSize, wireRotation |
| `pulseobject_t` | server/object.h | struct | `PulseObject` | `data class` | — | DONE | Extends `GameObjectBase`; adds pulseLen, pulseDir, pulseRefl |
| `itemobject_t` | server/object.h | struct | `ItemObject` | `data class` | — | DONE | Extends `GameObjectBase`; adds itemType, itemCount |
| `anyobject_t` union | server/object.h | union | `AnyObject` | `sealed class` | — | DONE | `sealed class AnyObject` with subclasses for each object type |

### server/player.h

| C Name | Source File | Category | Kotlin Name | Kotlin Kind | Java Equiv | Status | Notes |
|---|---|---|---|---|---|---|---|
| `PL_TYPE_*` constants | server/player.h | constants | `PlayerType` | `enum class` | — | DONE | HUMAN=0, ROBOT=1, TANK=2 |
| `PL_STATE_*` constants | server/player.h | constants | `PlayerState` | `enum class` | — | DONE | UNDEFINED…PAUSED (7 states) |
| `HAS_*` / `USES_*` bit constants | server/player.h | constants | `PlayerAbility` | `object` | — | DONE | ~18 bit flags for `have` and `used` fields |
| Player status bits (`HOVERPAUSE`, `REPROGRAM`, `FINISH`, `RACE_OVER`) | server/player.h | constants | `PlayerStatus` | `object` | — | DONE | Additional bits for `pl_status` field |
| `pl_fuel_t` struct | server/player.h | struct | `PlayerFuel` | `data class` | — | DONE | sum, max: Double, current, numTanks: Int, tank: DoubleArray |
| `visibility_t` struct | server/player.h | struct | `Visibility` | `data class` | — | DONE | canSee: Boolean, lastChange: Long |
| `shove_t` struct | server/player.h | struct | `Shove` | `data class` | — | DONE | pusherId: Int, time: Int |
| `player_t` struct | server/player.h | struct | `Player` | `class` | `Player.java`, `SelfHolder.java` | DONE | Large mutable state class (~60 fields + OBJECT_BASE); extends `GameObjectBase` |

### server/score.h

| C Name | Source File | Category | Kotlin Name | Kotlin Kind | Java Equiv | Status | Notes |
|---|---|---|---|---|---|---|---|
| Score constants (`ASTEROID_SCORE`, `WALL_SCORE`, etc.) | server/score.h | constants | `ScoreConst` | `object` | — | DONE | |
| `scoretype_t` enum | server/score.h | enum | `ScoreType` | `enum class` | — | DONE | 16 entries |

### server/rank.h

| C Name | Source File | Category | Kotlin Name | Kotlin Kind | Java Equiv | Status | Notes |
|---|---|---|---|---|---|---|---|
| `ranknode_t` struct | server/rank.h | struct | `RankNode` | `data class` | — | DONE | name, user, host: String; timestamp: Long; kills, deaths, rounds, shots, deadliest, ballsCashed, ballsSaved, ballsWon, ballsLost: Int; bestball, score, maxSurvivalTime: Double; pl: Player? |

### server/cannon.h

| C Name | Source File | Category | Kotlin Name | Kotlin Kind | Java Equiv | Status | Notes |
|---|---|---|---|---|---|---|---|
| `CW_*` weapon type constants | server/cannon.h | constants | `CannonWeapon` | `object` | — | DONE | SHOT=0, MINE=1, MISSILE=2, LASER=3, ECM=4, TRACTORBEAM=5, TRANSPORTER=6, GASJET=7 |
| `CD_*` defense type constants | server/cannon.h | constants | `CannonDefense` | `object` | — | DONE | EM_SHIELD=0, PHASING=1 |
| `CANNON_DISTANCE` etc. physics constants | server/cannon.h | constants | `CannonConst` | `object` | — | DONE | DISTANCE, DROP_ITEM_PROB, MINE_MASS_FACTOR, SHOT_MASS, PULSES, SPREAD, SMARTNESS_MAX |

### server/asteroid.h

| C Name | Source File | Category | Kotlin Name | Kotlin Kind | Java Equiv | Status | Notes |
|---|---|---|---|---|---|---|---|
| `ASTEROID_MAX_SIZE` and related constants | server/asteroid.h | constants | `AsteroidConst` | `object` | — | DONE | MAX_SIZE=4, LIFE=1000, DELTA_DIR_DIVISOR=8, DUST_MASS=0.25, DUST_FACT≈0.111, hitsRequired(size) |

---

## Module: client/

### client/netclient.h

| C Name | Source File | Category | Kotlin Name | Kotlin Kind | Java Equiv | Status | Notes |
|---|---|---|---|---|---|---|---|
| `display_t` struct | client/netclient.h | struct | `DisplayConfig` | `data class` | — | DONE | viewWidth, viewHeight, sparkRand, numSparkColors: Int |
| `pointer_move_t` struct | client/netclient.h | struct | `PointerMove` | `data class` | — | DONE | movement: Int, turnspeed: Double, id: Int |
| Net constants (`MIN/MAX_RECEIVE_WINDOW_SIZE`, `MAX_SUPPORTED_FPS`) | client/netclient.h | constants | `NetConst` | `object` | — | DONE | |

### client/client.h

| C Name | Source File | Category | Kotlin Name | Kotlin Kind | Java Equiv | Status | Notes |
|---|---|---|---|---|---|---|---|
| `client_data_t` struct | client/client.h | struct | `ClientData` | `data class` | — | DONE | talking, pointerControl, restorePointerControl, quitMode: Boolean; clientLag, scaleFactor, scale, fscale, altScaleFactor |
| `instruments_t` struct | client/client.h | struct | `Instruments` | `data class` | — | DONE | 17 boolean display toggles |
| `xp_args_t` struct | client/client.h | struct | `XpArgs` | `data class` | — | DONE | Command-line flags |
| `PACKET_LOSS/DROP/DRAW` constants | client/client.h | constants | `PacketStatus` | `object` | — | DONE | |
| UI size constants (`MAX_SPARK_SIZE`, `MAX_SHOT_SIZE`, etc.) | client/client.h | constants | `ClientConst` | `object` | — | DONE | Group all client-only limits |
| `other_t` struct (remote player view) | client/client.h | struct | `OtherPlayer` | `data class` | — | DONE | Score, id, team, check, round, timing, life, mychar, alliance, ship, names |
| `fuelstation_t` struct | client/client.h | struct | `FuelStation` | `data class` | — | DONE | pos: Int (block index), fuel: Double, bounds: IRect |
| `homebase_t` struct | client/client.h | struct | `HomeBase` | `data class` | — | DONE | pos, id, team, bounds, type, appearTime |
| `cannontime_t` struct | client/client.h | struct | `CannonTime` | `data class` | — | DONE | pos, deadTime, dot |
| `target_t` struct (client version) | client/client.h | struct | `ClientTarget` | `data class` | — | DONE | Renamed to avoid clash with server `target_t` → `Target` |
| `checkpoint_t` struct | client/client.h | struct | `Checkpoint` | `data class` | — | DONE | pos, bounds |
| `edge_style_t` struct | client/client.h | struct | `EdgeStyle` | `data class` | — | DONE | width, color, rgb, style |
| `polygon_style_t` struct | client/client.h | struct | `PolygonStyleDef` | `data class` | — | DONE | Renamed to avoid clash with style flag constants |
| `xp_polygon_t` struct | client/client.h | struct | `XpPolygon` | `data class` | — | DONE | points: Array<IPos>, numPoints, bounds: IRect, edgeStyles: IntArray?, style: Int |
| `refuel_t` struct | client/client.h | struct | `Refuel` | `data class` | — | DONE | x0, y0, x1, y1: Short |
| `connector_t` struct | client/client.h | struct | `Connector` | `data class` | — | DONE | x0, y0, x1, y1: Short, tractor: UByte |
| `laser_t` struct | client/client.h | struct | `Laser` | `data class` | — | DONE | color, dir: UByte, x, y, len: Short |
| `missile_t` struct | client/client.h | struct | `Missile` | `data class` | — | DONE | x, y, dir: Short, len: UByte |
| `ball_t` struct | client/client.h | struct | `Ball` | `data class` | — | DONE | x, y, id: Short, style: UByte |
| `ship_t` struct | client/client.h | struct | `Ship` | `data class` | — | DONE | x, y, id, dir: Short, shield, cloak, eshield, phased, deflector: UByte |
| `mine_t` struct | client/client.h | struct | `Mine` | `data class` | — | DONE | x, y, teammine, id: Short |
| `itemtype_t` struct | client/client.h | struct | `ItemAppearance` | `data class` | — | DONE | Renamed; x, y, type: Short |
| `ecm_t` struct (client version) | client/client.h | struct | `ClientEcm` | `data class` | — | DONE | x, y, size: Short; differs from server ecm_t |
| `trans_t` struct | client/client.h | struct | `Trans` | `data class` | — | DONE | x1, y1, x2, y2: Short |
| `paused_t` struct | client/client.h | struct | `Paused` | `data class` | — | DONE | x, y, count: Short |
| `appearing_t` struct | client/client.h | struct | `Appearing` | `data class` | — | DONE | x, y, id, count: Short |
| `radar_type_t` enum | client/client.h | enum | `RadarType` | `enum class` | — | DONE | ENEMY, FRIEND |
| `radar_t` struct | client/client.h | struct | `Radar` | `data class` | — | DONE | x, y, size: Short, type: RadarType |
| `vcannon_t` struct | client/client.h | struct | `VCannon` | `data class` | — | DONE | x, y, type: Short |
| `vfuel_t` struct | client/client.h | struct | `VFuel` | `data class` | — | DONE | x, y: Short, fuel: Double |
| `vbase_t` struct | client/client.h | struct | `VBase` | `data class` | — | DONE | x, y, xi, yi, type: Short |
| `debris_t` struct | client/client.h | struct | `Debris` | `data class` | — | DONE | x, y: UByte |
| `vdecor_t` struct | client/client.h | struct | `VDecor` | `data class` | — | DONE | x, y, xi, yi, type: Short |
| `wreckage_t` struct | client/client.h | struct | `Wreckage` | `data class` | — | DONE | x, y: Short, wrecktype, size, rotation: UByte |
| `asteroid_t` struct (client version) | client/client.h | struct | `ClientAsteroid` | `data class` | — | DONE | x, y: Short, type, size, rotation: UByte |
| `wormhole_t` struct (client version) | client/client.h | struct | `ClientWormhole` | `data class` | — | DONE | x, y: Short |
| `score_object_t` struct | client/client.h | struct | `ScoreObject` | `data class` | — | DONE | score, lifeTime: Double, x, y, hudMsgLen, hudMsgWidth, msgWidth, msgLen: Int, msg: String, hudMsg: String |
| `selection_t` struct | client/client.h | struct | `Selection` | `class` | — | DONE | Complex mutable struct with nested talk/draw selection state |
| `msg_bms_t` enum | client/client.h | enum | `MsgBms` | `enum class` | — | DONE | NONE, BALL, SAFE, COVER, POP |
| `message_t` struct | client/client.h | struct | `Message` | `data class` | — | DONE | txt: String, len: Int (or derived), lifeTime: Double, bmsInfo: MsgBms |

---

## Summary Statistics

| Module | Total C Types | Enums | Structs | Typedefs | Constant Groups | Unions | Done | TODO |
|---|---|---|---|---|---|---|---|---|
| common/ | 28 | 3 | 8 | 8 | 8 | 0 | 28 | 0 |
| server/ | 42 | 5 | 22 | 2 | 10 | 1 | 42 | 0 |
| client/ | 38 | 3 | 28 | 0 | 5 | 0 | 38 | 0 |
| **Total** | **108** | **11** | **58** | **10** | **23** | **1** | **108** | **0** |

---

## Recommended Porting Order

1. **common/** primitives and coordinates: `Vector`, `Position`, `IVec`, `IPos`, `IRect`, `BlkVec`, `BlkPos`, `ClPos`, `ClVec`
2. **common/** constants: `GameConst`, `ClickConst`, `MapBlock`, `TileFlags`, `PolygonStyleFlags`, `PacketType`
3. **common/** enums: `Item`, `Key`, `GameColor`
4. **common/** simple structs: `Rules`, `Setup`
5. **common/** complex types: `Shape`, `ShipShape`
6. **server/** modifiers: `Modifiers`, `Modifier`
7. **server/** map types: `Fuel`, `Grav`, `Base`, `Cannon`, `Check`, `WormType`, `Wormhole`, `Treasure`, `Target`, `Team`, `ItemConcentrator`, `AsteroidConcentrator`, `FrictionArea`, `Ecm`, `Transporter`, `ItemConfig`, `AsteroidConfig`, `World`
8. **server/** object hierarchy: `GameObjectBase`, `GameObject`, `MineObject`, `MissileObject`, `SmartObject`, `TorpObject`, `HeatObject`, `BallObject`, `WireObject`, `PulseObject`, `ItemObject`, `AnyObject`
9. **server/** player: `PlayerFuel`, `Visibility`, `Shove`, `PlayerType`, `PlayerState`, `PlayerAbility`, `PlayerStatus`, `Player`
10. **server/** scoring: `ScoreType`, `ScoreConst`, `RankNode`
11. **server/** cannon/asteroid constants: `CannonWeapon`, `CannonDefense`, `AsteroidConst`
12. **client/** all types (after server foundation is done)

---

## Package Layout

```
shared/src/commonMain/kotlin/org/lambertland/kxpilot/
  common/
    Types.kt          -- Vector, IVec, IRect, BlkVec, etc.
    Click.kt          -- ClPos, ClVec, ClickConst
    Constants.kt      -- GameConst, GameColor, PolygonStyleFlags
    Item.kt           -- Item enum class
    Key.kt            -- Key enum class
    Rules.kt          -- Rules data class, GameMode, OldPlayerStatus
    Setup.kt          -- Setup data class, MapBlock, TileFlags
    ShipShape.kt      -- Shape, ShipShape, ShipShapeConst
    Packet.kt         -- PacketType
  server/
    Modifiers.kt      -- Modifiers value class, Modifier enum
    Map.kt            -- Fuel, Grav, Base, Cannon, ... World
    Object.kt         -- GameObjectBase, object hierarchy, AnyObject
    Player.kt         -- Player, PlayerFuel, PlayerType, PlayerState, etc.
    Score.kt          -- ScoreType, ScoreConst
    Rank.kt           -- RankNode
    Cannon.kt         -- CannonWeapon, CannonDefense, CannonConst
    Asteroid.kt       -- AsteroidConst (incl. DUST_MASS, DUST_FACT, hitsRequired)
  client/
    NetClient.kt      -- DisplayConfig, PointerMove, NetConst
    Client.kt         -- ClientData, Instruments, OtherPlayer, game render types
    Messages.kt       -- Message, MsgBms, Selection
```
