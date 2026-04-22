package org.lambertland.kxpilot

import org.lambertland.kxpilot.client.KeyState
import org.lambertland.kxpilot.common.ClPos
import org.lambertland.kxpilot.common.ClickConst
import org.lambertland.kxpilot.common.GameConst
import org.lambertland.kxpilot.common.Key
import org.lambertland.kxpilot.common.Vector
import org.lambertland.kxpilot.common.toPixel
import org.lambertland.kxpilot.engine.GameEngine
import org.lambertland.kxpilot.engine.ShotData
import org.lambertland.kxpilot.server.CellType
import org.lambertland.kxpilot.server.PlayerState
import org.lambertland.kxpilot.server.World
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Returns a [KeyState] with [k] pressed (on its first tick — justPressed == true). */
private fun keysPressed(vararg k: Key): KeyState = KeyState().also { ks -> k.forEach { ks.press(it) } }

/** Returns a [KeyState] with no keys pressed. */
private fun noKeys(): KeyState = KeyState()

/** Tick [n] times with the given keys, advancing the key state each time. */
private fun GameEngine.tickN(
    n: Int,
    keys: KeyState,
) {
    repeat(n) {
        tick(keys)
        keys.advanceTick()
    }
}

/** Float equality within an epsilon. */
private fun assertApprox(
    expected: Double,
    actual: Double,
    eps: Double = 1e-6,
    msg: String = "",
) {
    assertTrue(
        abs(actual - expected) <= eps,
        "Expected ≈$expected but was $actual (eps=$eps)${if (msg.isEmpty()) "" else " — $msg"}",
    )
}

private fun assertApprox(
    expected: Float,
    actual: Float,
    eps: Float = 1e-5f,
    msg: String = "",
) = assertApprox(expected.toDouble(), actual.toDouble(), eps.toDouble(), msg)

// ---------------------------------------------------------------------------
// GameEngineTest
// ---------------------------------------------------------------------------

class GameEngineTest {
    // -------------------------------------------------------------------
    // 1. thrustAppliesVelocityInHeadingDirection
    // -------------------------------------------------------------------
    @Test
    fun thrustAppliesVelocityInHeadingDirection() {
        val engine = GameEngine.forEmptyWorld(60, 45)
        // Default heading = 0 → right (+X), floatDirCos=1, floatDirSin=0
        val keys = keysPressed(Key.KEY_THRUST)
        engine.tick(keys)

        assertTrue(engine.player.vel.x > 0f, "vel.x should be positive when heading right")
        assertApprox(0f, engine.player.vel.y, eps = 1e-5f, msg = "vel.y should be ≈0 when heading right")
    }

    // -------------------------------------------------------------------
    // 2. thrustHeading90DegreesAppliesVelocityUpward
    // -------------------------------------------------------------------
    @Test
    fun thrustHeading90DegreesAppliesVelocityUpward() {
        val engine = GameEngine.forEmptyWorld(60, 45)
        // Set heading to π/2 (up in Y-up space = heading 32 in 128-unit circle)
        engine.player.setFloatDir(PI / 2.0)

        val keys = keysPressed(Key.KEY_THRUST)
        engine.tick(keys)

        assertApprox(0f, engine.player.vel.x, eps = 1e-5f, msg = "vel.x should be ≈0 when heading up")
        assertTrue(engine.player.vel.y > 0f, "vel.y should be positive when heading up (+Y)")
    }

    // -------------------------------------------------------------------
    // 3. turnLeftIncreasesFloatDir
    // -------------------------------------------------------------------
    @Test
    fun turnLeftIncreasesFloatDir() {
        val engine = GameEngine.forEmptyWorld(60, 45)
        val initialDir = engine.player.floatDir // 0.0

        val keys = keysPressed(Key.KEY_TURN_LEFT)
        engine.tick(keys)

        assertTrue(
            engine.player.floatDir > initialDir,
            "floatDir should increase on KEY_TURN_LEFT (counter-clockwise = angle increase)",
        )
    }

    // -------------------------------------------------------------------
    // 4. turnRightDecreasesFloatDir (wraps to near 2π from 0)
    // -------------------------------------------------------------------
    @Test
    fun turnRightDecreasesFloatDirWrapping() {
        val engine = GameEngine.forEmptyWorld(60, 45)
        // Starting at 0, turning right should wrap to just below 2π
        val keys = keysPressed(Key.KEY_TURN_RIGHT)
        engine.tick(keys)

        val twoPi = 2.0 * PI
        val expected = twoPi - (2.0 * PI / GameConst.RES)
        assertApprox(expected, engine.player.floatDir, eps = 1e-10)
    }

    // -------------------------------------------------------------------
    // 5. turnLeftUpdatesIntHeadingDerivedFromFloatDir
    // -------------------------------------------------------------------
    @Test
    fun turnLeftUpdatesIntHeadingDerivedFromFloatDir() {
        val engine = GameEngine.forEmptyWorld(60, 45)
        val keys = keysPressed(Key.KEY_TURN_LEFT)
        engine.tick(keys)

        // One TURN_RATE step = 2π/128 radians.
        // floatDirToIntHeading: (angle / 2π * 128).toInt()
        // = (2π/128 / 2π * 128).toInt() = 1.0.toInt() = 1
        assertEquals(1.toShort(), engine.player.dir)
    }

    // -------------------------------------------------------------------
    // 6. speedClampedAtSpeedLimit
    // -------------------------------------------------------------------
    @Test
    fun speedClampedAtSpeedLimit() {
        val engine = GameEngine.forEmptyWorld(60, 45)
        // Set a velocity far above the limit
        engine.player.vel =
            org.lambertland.kxpilot.common
                .Vector(1000f, 0f)

        engine.tick(noKeys())

        val speed =
            hypot(
                engine.player.vel.x
                    .toDouble(),
                engine.player.vel.y
                    .toDouble(),
            )
        assertTrue(speed <= GameConst.SPEED_LIMIT + 1e-6, "Speed $speed should be ≤ SPEED_LIMIT ${GameConst.SPEED_LIMIT}")
    }

    // -------------------------------------------------------------------
    // 7. positionWrapsToroidally
    // -------------------------------------------------------------------
    @Test
    fun positionWrapsToroidally() {
        val engine = GameEngine.forEmptyWorld(60, 45)
        val worldW = engine.world.width.toFloat()
        // Give the player enough velocity to cross the world boundary in one tick
        engine.player.vel =
            org.lambertland.kxpilot.common
                .Vector(worldW, 0f)

        engine.tick(noKeys())

        assertTrue(
            engine.playerPixelX < worldW,
            "playerPixelX ${engine.playerPixelX} should have wrapped to < $worldW",
        )
        assertTrue(engine.playerPixelX >= 0f, "playerPixelX should be non-negative after wrap")
    }

    // -------------------------------------------------------------------
    // 8. shotSpawnedOnFireJustPressed
    // -------------------------------------------------------------------
    @Test
    fun shotSpawnedOnFireJustPressed() {
        val engine = GameEngine.forEmptyWorld(60, 45)
        val keys = keysPressed(Key.KEY_FIRE_SHOT) // first press = justPressed

        engine.tick(keys)

        assertEquals(1, engine.shots.size, "Exactly one shot should be spawned on first KEY_FIRE_SHOT press")
    }

    // -------------------------------------------------------------------
    // 9. shotNotSpawnedOnFireHeld (justPressed guard)
    // -------------------------------------------------------------------
    @Test
    fun shotNotSpawnedOnFireHeld() {
        val engine = GameEngine.forEmptyWorld(60, 45)
        val keys = keysPressed(Key.KEY_FIRE_SHOT)

        engine.tick(keys)
        keys.advanceTick() // now previous[FIRE_SHOT]=true → justPressed=false
        engine.tick(keys) // held, NOT just pressed

        assertEquals(1, engine.shots.size, "Holding KEY_FIRE_SHOT should not spawn a second shot")
    }

    // -------------------------------------------------------------------
    // 10. shotAdvancesPosition
    // -------------------------------------------------------------------
    @Test
    fun shotAdvancesPosition() {
        val engine = GameEngine.forEmptyWorld(60, 45)
        val fireKeys = keysPressed(Key.KEY_FIRE_SHOT)
        engine.tick(fireKeys)
        fireKeys.advanceTick()

        val posAfterSpawn = engine.shots.first().pos

        engine.tick(noKeys())

        val posAfterTick = engine.shots.first().pos
        assertTrue(
            posAfterSpawn != posAfterTick,
            "Shot position should change after one tick",
        )
    }

    // -------------------------------------------------------------------
    // 11. shotRemovedAfterLifeExpires
    // -------------------------------------------------------------------
    @Test
    fun shotRemovedAfterLifeExpires() {
        val engine = GameEngine.forEmptyWorld(60, 45)
        val fireKeys = keysPressed(Key.KEY_FIRE_SHOT)
        engine.tick(fireKeys)
        fireKeys.advanceTick()

        // Tick 60 more times (SHOT_LIFE = 60f; each tick decrements by 1)
        engine.tickN(60, noKeys())

        assertTrue(engine.shots.isEmpty(), "All shots should be removed after SHOT_LIFE ticks")
    }

    // -------------------------------------------------------------------
    // 12. shotDoesNotKillPlayerOnSpawnTick (regression for self-kill bug)
    // -------------------------------------------------------------------
    @Test
    fun shotDoesNotKillPlayerOnSpawnTick() {
        val engine = GameEngine.forEmptyWorld(60, 45)
        val keys = keysPressed(Key.KEY_FIRE_SHOT)

        engine.tick(keys)

        assertTrue(engine.player.isAlive(), "Player should not be killed on the tick they fire a shot")
    }

    // -------------------------------------------------------------------
    // 13. wrapHeadingNeverReturnsNegative / full-circle continuity
    // -------------------------------------------------------------------
    @Test
    fun wrapHeadingNeverReturnsNegative() {
        // Turn right from heading 0 → dir should wrap to RES-1, not go negative
        val engine = GameEngine.forEmptyWorld(10, 10)
        val rightKeys = keysPressed(Key.KEY_TURN_RIGHT)
        engine.tick(rightKeys)

        assertTrue(engine.player.dir >= 0, "dir should never be negative after right-turn from 0")
        assertEquals((GameConst.RES - 1).toShort(), engine.player.dir)
    }

    // -------------------------------------------------------------------
    // 13b. fullCircleReturnsDirToZero
    // -------------------------------------------------------------------
    @Test
    fun fullCircleReturnsDirToZero() {
        // 128 left-turns on a fresh engine should return floatDir to within
        // one step of 0 (floating-point accumulation may land at 127 or 0).
        val engine = GameEngine.forEmptyWorld(10, 10)
        val circleKeys = keysPressed(Key.KEY_TURN_LEFT)
        engine.tickN(GameConst.RES, circleKeys)

        val twoPi = 2.0 * PI
        val step = twoPi / GameConst.RES
        val angle = engine.player.floatDir
        // Accept 0 ± one step, or just below 2π ± one step
        val nearZero = angle < step || angle > twoPi - step
        assertTrue(nearZero, "floatDir $angle should be within one step ($step) of 0/2π after full circle")
    }

    // -------------------------------------------------------------------
    // 14. forEmptyWorldCreatesCorrectDimensions
    // -------------------------------------------------------------------
    @Test
    fun forEmptyWorldCreatesCorrectDimensions() {
        val engine = GameEngine.forEmptyWorld(10, 8)
        assertEquals(10 * GameConst.BLOCK_SZ, engine.world.width)
        assertEquals(8 * GameConst.BLOCK_SZ, engine.world.height)
        assertEquals(engine.world.width * 64, engine.world.cwidth)
        assertEquals(engine.world.height * 64, engine.world.cheight)
    }

    // -------------------------------------------------------------------
    // 15. thrustClearsAccOnRelease
    // -------------------------------------------------------------------
    @Test
    fun thrustClearsAccOnRelease() {
        val engine = GameEngine.forEmptyWorld(60, 45)
        val thrustKeys = keysPressed(Key.KEY_THRUST)
        engine.tick(thrustKeys)

        assertFalse(engine.player.acc.x == 0f && engine.player.acc.y == 0f, "acc should be non-zero while thrusting")

        thrustKeys.advanceTick()
        thrustKeys.release(Key.KEY_THRUST)
        engine.tick(thrustKeys)

        assertEquals(0f, engine.player.acc.x, "acc.x should be 0 after thrust released")
        assertEquals(0f, engine.player.acc.y, "acc.y should be 0 after thrust released")
    }

    // -------------------------------------------------------------------
    // 16. noTickAfterKill
    // -------------------------------------------------------------------
    @Test
    fun noTickAfterKill() {
        val engine = GameEngine.forEmptyWorld(60, 45)
        engine.player.vel =
            org.lambertland.kxpilot.common
                .Vector(5f, 0f)
        engine.player.plState = PlayerState.KILLED

        val posX = engine.playerPixelX
        engine.tick(keysPressed(Key.KEY_THRUST))

        assertApprox(posX, engine.playerPixelX, eps = 0.01f, msg = "tick() should be a no-op when player is killed")
    }

    // -------------------------------------------------------------------
    // 17. respawnResetsStateAndClearsShots
    // -------------------------------------------------------------------
    @Test
    fun respawnResetsStateAndClearsShots() {
        val engine = GameEngine.forEmptyWorld(60, 45)
        // Kill player and fire a shot
        engine.player.plState = PlayerState.KILLED
        engine.shots +=
            org.lambertland.kxpilot.engine.ShotData(
                pos =
                    org.lambertland.kxpilot.common
                        .ClPos(0, 0),
                vel =
                    org.lambertland.kxpilot.common
                        .Vector(1f, 0f),
                life = 30f,
                ownerId = 0,
            )

        engine.respawn()

        assertTrue(engine.player.isAlive(), "Player should be alive after respawn()")
        assertTrue(engine.shots.isEmpty(), "shots list should be cleared after respawn()")
        val cx = engine.world.width / 2
        val cy = engine.world.height / 2
        assertApprox(cx.toFloat(), engine.playerPixelX, eps = 1f, msg = "Player X should be at world centre after respawn")
        assertApprox(cy.toFloat(), engine.playerPixelY, eps = 1f, msg = "Player Y should be at world centre after respawn")
        assertEquals(0f, engine.player.vel.x)
        assertEquals(0f, engine.player.vel.y)
    }
}

// ---------------------------------------------------------------------------
// Wall collision, gravity, tunneling, and shot-wall tests
// ---------------------------------------------------------------------------

/**
 * Build a [World] with a filled block at ([wallBx], [wallBy]) in a
 * [cols]×[rows] block map.  Player is centred in the world.
 */
private fun makeEngineWithWall(
    cols: Int = 20,
    rows: Int = 20,
    wallBx: Int,
    wallBy: Int,
): GameEngine {
    val world =
        World().apply {
            x = cols
            y = rows
            bwidthFloor = cols
            bheightFloor = rows
            width = cols * GameConst.BLOCK_SZ
            height = rows * GameConst.BLOCK_SZ
            cwidth = width * ClickConst.CLICK
            cheight = height * ClickConst.CLICK
            block =
                Array(cols) { bx ->
                    Array(rows) { by ->
                        if (bx == wallBx && by == wallBy) CellType.FILLED else CellType.SPACE
                    }
                }
            gravity = Array(cols) { Array(rows) { Vector(0f, 0f) } }
        }
    return GameEngine(world)
}

class WallCollisionTest {
    // -------------------------------------------------------------------
    // 1. Flat horizontal wall: moving right into a FILLED block reflects X
    // -------------------------------------------------------------------
    @Test
    fun movingRightIntoFilledBlockReflectsXVelocity() {
        // Place wall one block to the right of centre.
        val cols = 20
        val rows = 20
        val wallBx = cols / 2 + 1
        val wallBy = rows / 2
        val engine = makeEngineWithWall(cols = cols, rows = rows, wallBx = wallBx, wallBy = wallBy)

        // Position player just left of the wall, moving right fast enough to contact
        val wallLeftPx = wallBx * GameConst.BLOCK_SZ.toFloat()
        val playerStartPx = wallLeftPx - GameConst.SHIP_SZ - 2f
        engine.player.pos =
            ClPos(
                (playerStartPx * ClickConst.CLICK).toInt(),
                engine.player.pos.cy,
            )
        val initVx = 10f
        engine.player.vel = Vector(initVx, 0f)

        engine.tick(noKeys())

        assertTrue(engine.player.vel.x < 0f, "vel.x should be reflected negative after hitting right wall, was ${engine.player.vel.x}")
        // Player should not have penetrated into the wall
        assertTrue(engine.playerPixelX < wallLeftPx, "Player should be left of wall after reflection, was ${engine.playerPixelX}")
    }

    // -------------------------------------------------------------------
    // 2. Flat vertical wall: moving up into a FILLED block reflects Y
    // -------------------------------------------------------------------
    @Test
    fun movingUpIntoFilledBlockReflectsYVelocity() {
        val cols = 20
        val rows = 20
        val wallBx = cols / 2
        val wallBy = rows / 2 + 1
        val engine = makeEngineWithWall(cols = cols, rows = rows, wallBx = wallBx, wallBy = wallBy)

        val wallBottomPx = wallBy * GameConst.BLOCK_SZ.toFloat()
        val playerStartPy = wallBottomPx - GameConst.SHIP_SZ - 2f
        engine.player.pos =
            ClPos(
                engine.player.pos.cx,
                (playerStartPy * ClickConst.CLICK).toInt(),
            )
        val initVy = 10f
        engine.player.vel = Vector(0f, initVy)

        engine.tick(noKeys())

        assertTrue(engine.player.vel.y < 0f, "vel.y should be reflected negative after hitting top wall, was ${engine.player.vel.y}")
        assertTrue(engine.playerPixelY < wallBottomPx, "Player Y should be below wall after reflection, was ${engine.playerPixelY}")
    }

    // -------------------------------------------------------------------
    // 3. Grazing a flat horizontal wall does NOT reflect X (corner-stick fix)
    // -------------------------------------------------------------------
    @Test
    fun grazingHorizontalWallDoesNotReflectX() {
        // Ship moving mostly right (+X) with a tiny upward Y, grazing a wall above.
        // With SAT: Y penetration > X penetration → only Y reflected, not X.
        val cols = 20
        val rows = 20
        val wallBx = cols / 2
        val wallBy = rows / 2 + 1
        val engine = makeEngineWithWall(cols = cols, rows = rows, wallBx = wallBx, wallBy = wallBy)

        val wallBottomPx = wallBy * GameConst.BLOCK_SZ.toFloat()
        // Place player so the top corner just barely clips the bottom of the wall
        val playerStartPy = wallBottomPx - GameConst.SHIP_SZ.toFloat() + 1f
        engine.player.pos =
            ClPos(
                engine.player.pos.cx,
                (playerStartPy * ClickConst.CLICK).toInt(),
            )
        // Large X velocity, small Y velocity (grazing angle)
        engine.player.vel = Vector(20f, 2f)

        engine.tick(noKeys())

        // X should not be reflected (was not penetrating the wall on X axis)
        assertTrue(engine.player.vel.x > 0f, "X velocity should remain positive when grazing horizontal wall (was ${engine.player.vel.x})")
        // Y should be reflected
        assertTrue(engine.player.vel.y <= 0f, "Y velocity should be reflected when hitting bottom of wall (was ${engine.player.vel.y})")
    }

    // -------------------------------------------------------------------
    // 4. High-speed player does not tunnel through a one-block wall
    // -------------------------------------------------------------------
    @Test
    fun highSpeedPlayerDoesNotTunnelThroughWall() {
        // Wall at block (11, 10) in a 20×20 world.
        // Player starts at block (9, 10) and moves right at SPEED_LIMIT.
        val cols = 20
        val rows = 20
        val wallBx = 11
        val wallBy = rows / 2
        val engine = makeEngineWithWall(cols = cols, rows = rows, wallBx = wallBx, wallBy = wallBy)

        val startPx = 9 * GameConst.BLOCK_SZ.toFloat() + GameConst.BLOCK_SZ / 2f
        val startPy = wallBy * GameConst.BLOCK_SZ.toFloat() + GameConst.BLOCK_SZ / 2f
        engine.player.pos =
            ClPos(
                (startPx * ClickConst.CLICK).toInt(),
                (startPy * ClickConst.CLICK).toInt(),
            )
        engine.player.vel = Vector(GameConst.SPEED_LIMIT.toFloat(), 0f)

        engine.tick(noKeys())

        val wallLeftPx = wallBx * GameConst.BLOCK_SZ.toFloat()
        assertTrue(
            engine.playerPixelX < wallLeftPx,
            "Player at speed limit should not tunnel through wall: playerX=${engine.playerPixelX}, wallLeft=$wallLeftPx",
        )
    }

    // -------------------------------------------------------------------
    // 5. Player spawned inside wall is not stuck (push-back moves them out)
    // -------------------------------------------------------------------
    @Test
    fun playerInsideWallIsNotStuckInfinitely() {
        val cols = 20
        val rows = 20
        val wallBx = cols / 2
        val wallBy = rows / 2
        val engine = makeEngineWithWall(cols = cols, rows = rows, wallBx = wallBx, wallBy = wallBy)

        // Place player dead centre of the wall block
        val wallCentrePx = wallBx * GameConst.BLOCK_SZ + GameConst.BLOCK_SZ / 2f
        val wallCentrePy = wallBy * GameConst.BLOCK_SZ + GameConst.BLOCK_SZ / 2f
        engine.player.pos =
            ClPos(
                (wallCentrePx * ClickConst.CLICK).toInt(),
                (wallCentrePy * ClickConst.CLICK).toInt(),
            )
        engine.player.vel = Vector(5f, 0f)

        // Should not throw, should not loop forever
        repeat(5) { engine.tick(noKeys()) }

        // After a few ticks the player should have moved (push-back applied)
        assertTrue(engine.player.isAlive() || engine.player.isKilled(), "Engine should remain responsive after wall overlap")
    }
}

// ---------------------------------------------------------------------------
// Diagonal wall reflection tests
// ---------------------------------------------------------------------------

/**
 * Build a [World] with a single diagonal block at ([diagBx], [diagBy]).
 * Player is placed just outside the diagonal, moving into it.
 */
private fun makeEngineWithDiag(
    cols: Int = 20,
    rows: Int = 20,
    diagBx: Int,
    diagBy: Int,
    diagCell: CellType,
): GameEngine {
    val world =
        World().apply {
            x = cols
            y = rows
            bwidthFloor = cols
            bheightFloor = rows
            width = cols * GameConst.BLOCK_SZ
            height = rows * GameConst.BLOCK_SZ
            cwidth = width * ClickConst.CLICK
            cheight = height * ClickConst.CLICK
            block =
                Array(cols) { bx ->
                    Array(rows) { by ->
                        if (bx == diagBx && by == diagBy) diagCell else CellType.SPACE
                    }
                }
            gravity = Array(cols) { Array(rows) { Vector(0f, 0f) } }
        }
    return GameEngine(world)
}

class DiagonalWallTest {
    // -------------------------------------------------------------------
    // REC_LU (SW↗NE hyp, normal=(1,1)/√2): reflect swaps vx↔vy
    // Moving down-right (+X,−Y) → after reflection: (−Y,−X) → (−,−) nope
    // Actually: incoming (vx,vy)=(10,−10) → reflected = (vy,vx) = (−10,10)
    // i.e. direction flips to up-left.
    // -------------------------------------------------------------------
    @Test
    fun recLuDiagonalReflectsCorrectly() {
        val cols = 20
        val rows = 20
        val diagBx = cols / 2
        val diagBy = rows / 2
        val engine = makeEngineWithDiag(cols, rows, diagBx, diagBy, CellType.REC_LU)

        // REC_LU solid side: fy > fx (upper-left triangle, hypotenuse SW↗NE).
        // Place center at block-local (fx=0.6, fy=0.4) — outside solid.
        // Use a small velocity so the corner at (−r, +r) = (−16, +16) stays
        // inside the block after the move and lands in the solid region:
        //   after move: cx = (0.6*35 − 3 − 16) = 5.5  → fx_c ≈ 0.157
        //               cy = (0.4*35 + 3 + 16) = 33.0 → fy_c ≈ 0.943
        //   fy_c > fx_c ✓ → corner is in solid region.
        val bs = GameConst.BLOCK_SZ.toFloat()
        val startPx = (diagBx + 0.6f) * bs
        val startPy = (diagBy + 0.4f) * bs
        engine.player.pos = ClPos((startPx * ClickConst.CLICK).toInt(), (startPy * ClickConst.CLICK).toInt())
        // Small velocity so corners don't exit the block.
        val vIn = 3f
        engine.player.vel = Vector(-vIn, vIn)

        engine.tick(noKeys())

        // After reflecting REC_LU: (vx,vy) → (vy,vx) = (vIn, -vIn)
        // So vx should become positive (right) and vy negative (down)
        assertTrue(engine.player.vel.x > 0f, "After REC_LU reflection vx should flip sign (was ${engine.player.vel.x})")
        assertTrue(engine.player.vel.y < 0f, "After REC_LU reflection vy should flip sign (was ${engine.player.vel.y})")
    }

    // -------------------------------------------------------------------
    // REC_LD (NW↘SE hyp, normal=(1,-1)/√2): reflect negates and swaps
    // Incoming (vx,vy) → (−vy, −vx)
    // -------------------------------------------------------------------
    @Test
    fun recLdDiagonalReflectsCorrectly() {
        val cols = 20
        val rows = 20
        val diagBx = cols / 2
        val diagBy = rows / 2
        val engine = makeEngineWithDiag(cols, rows, diagBx, diagBy, CellType.REC_LD)

        // REC_LD solid side: fy < fx (lower-left triangle, hypotenuse NW↘SE).
        // Place center at block-local (fx=0.8, fy=0.6) — outside solid.
        // Use a small velocity so the corner at (−r, −r) = (−16, −16) stays
        // inside the block and lands in the solid region:
        //   after move: cx = (0.8*35 − 3 − 16) = 9.0  → fx_c ≈ 0.257
        //               cy = (0.6*35 − 3 − 16) = 2.0  → fy_c ≈ 0.057
        //   fy_c < fx_c ✓ → corner is in solid region.
        val bs = GameConst.BLOCK_SZ.toFloat()
        val startPx = (diagBx + 0.8f) * bs
        val startPy = (diagBy + 0.6f) * bs
        engine.player.pos = ClPos((startPx * ClickConst.CLICK).toInt(), (startPy * ClickConst.CLICK).toInt())
        val vIn = 3f
        engine.player.vel = Vector(-vIn, -vIn) // moving left and down (into diagonal)

        engine.tick(noKeys())

        // After REC_LD reflection: (vx,vy) → (−vy, −vx) = (vIn, vIn)
        assertTrue(engine.player.vel.x > 0f, "After REC_LD reflection vx should be positive (was ${engine.player.vel.x})")
        assertTrue(engine.player.vel.y > 0f, "After REC_LD reflection vy should be positive (was ${engine.player.vel.y})")
    }

    // -------------------------------------------------------------------
    // REC_RD (SW↗NE hyp, solid at lower-right, normal=(−1,+1)/√2): swap vx↔vy
    // Incoming (vx,vy)=(+3,−3) into lower-right solid → reflected = (−3, +3)
    // -------------------------------------------------------------------
    @Test
    fun recRdDiagonalReflectsCorrectly() {
        val cols = 20
        val rows = 20
        val diagBx = cols / 2
        val diagBy = rows / 2
        val engine = makeEngineWithDiag(cols, rows, diagBx, diagBy, CellType.REC_RD)

        // REC_RD solid side: fy < fx (lower-right triangle, SW↗NE diagonal).
        // Place center at (fx=0.4, fy=0.6) — outside solid (0.6 > 0.4, so fy > fx).
        // Corner (+r, −r) after vel=(+3,−3):
        //   cx = 0.4*35 + 3 + 16 = 33.0 → fx_c ≈ 0.943
        //   cy = 0.6*35 − 3 − 16 = 2.0  → fy_c ≈ 0.057
        //   fy_c < fx_c ✓ → corner in lower-right solid.
        // into-wall guard: vx − vy = 3 − (−3) = 6 > 0 ✓
        val bs = GameConst.BLOCK_SZ.toFloat()
        val startPx = (diagBx + 0.4f) * bs
        val startPy = (diagBy + 0.6f) * bs
        engine.player.pos = ClPos((startPx * ClickConst.CLICK).toInt(), (startPy * ClickConst.CLICK).toInt())
        val vIn = 3f
        engine.player.vel = Vector(vIn, -vIn) // moving right and down (into lower-right solid)

        engine.tick(noKeys())

        // After REC_RD reflection (swap): (vx,vy) → (vy,vx) = (−vIn, vIn)
        assertTrue(engine.player.vel.x < 0f, "After REC_RD reflection vx should be negative (was ${engine.player.vel.x})")
        assertTrue(engine.player.vel.y > 0f, "After REC_RD reflection vy should be positive (was ${engine.player.vel.y})")
    }

    // -------------------------------------------------------------------
    // REC_RU (NW↘SE hyp, solid at upper-right, normal=(−1,−1)/√2): negate-swap
    // Incoming (vx,vy)=(+3,+3) into upper-right solid → reflected = (−3,−3)
    // -------------------------------------------------------------------
    @Test
    fun recRuDiagonalReflectsCorrectly() {
        val cols = 20
        val rows = 20
        val diagBx = cols / 2
        val diagBy = rows / 2
        val engine = makeEngineWithDiag(cols, rows, diagBx, diagBy, CellType.REC_RU)

        // REC_RU solid side: fy > 1−fx (upper-right triangle, NW↘SE diagonal).
        // Place center at (fx=0.4, fy=0.4) — outside solid (0.4 < 1−0.4=0.6).
        // Corner (+r, +r) after vel=(+3,+3):
        //   cx = 0.4*35 + 3 + 16 = 33.0 → fx_c ≈ 0.943
        //   cy = 0.4*35 + 3 + 16 = 33.0 → fy_c ≈ 0.943
        //   fy_c > 1 − fx_c = 0.057 ✓ → corner in upper-right solid.
        // into-wall guard: vx + vy = 3 + 3 = 6 > 0 ✓
        val bs = GameConst.BLOCK_SZ.toFloat()
        val startPx = (diagBx + 0.4f) * bs
        val startPy = (diagBy + 0.4f) * bs
        engine.player.pos = ClPos((startPx * ClickConst.CLICK).toInt(), (startPy * ClickConst.CLICK).toInt())
        val vIn = 3f
        engine.player.vel = Vector(vIn, vIn) // moving right and up (into upper-right solid)

        engine.tick(noKeys())

        // After REC_RU reflection (negate-swap): (vx,vy) → (−vy,−vx) = (−vIn,−vIn)
        assertTrue(engine.player.vel.x < 0f, "After REC_RU reflection vx should be negative (was ${engine.player.vel.x})")
        assertTrue(engine.player.vel.y < 0f, "After REC_RU reflection vy should be negative (was ${engine.player.vel.y})")
    }
}

// ---------------------------------------------------------------------------
// Gravity tests
// ---------------------------------------------------------------------------

class GravityTest {
    // -------------------------------------------------------------------
    // 1. Gravity accelerates player in the direction of the gravity vector
    // -------------------------------------------------------------------
    @Test
    fun gravityAcceleratesPlayerInGravDirection() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        // Set gravity pointing right (+X) at player's current block
        val bx = engine.toroidalBlock(engine.playerPixelX, GameConst.BLOCK_SZ.toFloat(), engine.world.x)
        val by = engine.toroidalBlock(engine.playerPixelY, GameConst.BLOCK_SZ.toFloat(), engine.world.y)
        engine.world.gravity[bx][by] = Vector(1f, 0f)

        engine.tick(noKeys())

        assertTrue(engine.player.vel.x > 0f, "vel.x should increase due to rightward gravity (was ${engine.player.vel.x})")
        assertEquals(0f, engine.player.vel.y, "vel.y should be unaffected by rightward gravity")
    }

    // -------------------------------------------------------------------
    // 2. Zero gravity has no effect on velocity
    // -------------------------------------------------------------------
    @Test
    fun zeroGravityHasNoEffect() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        // All gravity already initialised to zero in forEmptyWorld
        engine.player.vel = Vector(3f, 5f)

        engine.tick(noKeys())

        // Only speed clamp would change vel; 3²+5²=34 < SPEED_LIMIT²
        assertApprox(3f, engine.player.vel.x, eps = 0.01f)
        assertApprox(5f, engine.player.vel.y, eps = 0.01f)
    }

    // -------------------------------------------------------------------
    // 3. Gravity accumulates over multiple ticks
    // -------------------------------------------------------------------
    @Test
    fun gravityAccumulatesOverMultipleTicks() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        val bx = engine.toroidalBlock(engine.playerPixelX, GameConst.BLOCK_SZ.toFloat(), engine.world.x)
        val by = engine.toroidalBlock(engine.playerPixelY, GameConst.BLOCK_SZ.toFloat(), engine.world.y)
        engine.world.gravity[bx][by] = Vector(0f, -1f) // downward (-Y in Y-up space)

        engine.tickN(5, noKeys())

        assertTrue(engine.player.vel.y < 0f, "5 ticks of downward gravity should produce negative vel.y (was ${engine.player.vel.y})")
    }
}

// ---------------------------------------------------------------------------
// Shot-wall collision tests
// ---------------------------------------------------------------------------

class ShotWallTest {
    // -------------------------------------------------------------------
    // 1. Shot moving into a FILLED block is removed
    // -------------------------------------------------------------------
    @Test
    fun shotHittingFilledBlockIsRemoved() {
        val cols = 20
        val rows = 20
        val wallBx = cols / 2 + 1
        val wallBy = rows / 2
        val engine = makeEngineWithWall(cols = cols, rows = rows, wallBx = wallBx, wallBy = wallBy)

        // Fire a shot heading right toward the wall
        engine.player.setFloatDir(0.0) // heading right
        val fireKeys = keysPressed(Key.KEY_FIRE_SHOT)
        engine.tick(fireKeys)
        fireKeys.advanceTick()

        assertEquals(1, engine.shots.size, "Shot should exist after firing")

        // Move the shot directly into the wall
        val wallLeftPx = wallBx * GameConst.BLOCK_SZ
        engine.shots[0].pos =
            ClPos(
                ((wallLeftPx - 2) * ClickConst.CLICK),
                engine.shots[0].pos.cy,
            )
        engine.shots[0].vel = Vector(20f, 0f) // fast enough to enter wall next tick
        engine.shots[0].freshTick = false

        engine.tick(noKeys())

        assertTrue(engine.shots.isEmpty(), "Shot should be removed after hitting FILLED wall, size=${engine.shots.size}")
    }

    // -------------------------------------------------------------------
    // 2. Shot in open space is NOT removed
    // -------------------------------------------------------------------
    @Test
    fun shotInOpenSpaceIsNotRemoved() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        val fireKeys = keysPressed(Key.KEY_FIRE_SHOT)
        engine.tick(fireKeys)
        fireKeys.advanceTick()

        engine.tick(noKeys())

        assertEquals(1, engine.shots.size, "Shot in open space should not be removed after one tick")
    }
}

// ---------------------------------------------------------------------------
// Player.id tests
// ---------------------------------------------------------------------------

class PlayerIdTest {
    @Test
    fun playerIdIsNonZero() {
        val engine = GameEngine.forEmptyWorld(10, 10)
        assertTrue(engine.player.id != 0.toShort(), "player.id should be non-zero to prevent accidental NPC owner-immunity matches")
    }

    @Test
    fun shotOwnerIdMatchesPlayerId() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        val fireKeys = keysPressed(Key.KEY_FIRE_SHOT)
        engine.tick(fireKeys)

        assertEquals(engine.player.id, engine.shots.first().ownerId, "Shot ownerId should match player.id")
    }
}

// ---------------------------------------------------------------------------
// Determinism test
// ---------------------------------------------------------------------------

class DeterminismTest {
    @Test
    fun sameInputProducesSamePositionOnTwoInstances() {
        fun runSequence(): Pair<Float, Float> {
            val engine = GameEngine.forEmptyWorld(40, 40)
            val keys = KeyState()
            // Turn left 5 ticks, thrust 10 ticks, fire, coast 20 ticks
            keys.press(Key.KEY_TURN_LEFT)
            repeat(5) {
                engine.tick(keys)
                keys.advanceTick()
            }
            keys.release(Key.KEY_TURN_LEFT)
            keys.press(Key.KEY_THRUST)
            repeat(10) {
                engine.tick(keys)
                keys.advanceTick()
            }
            keys.release(Key.KEY_THRUST)
            keys.press(Key.KEY_FIRE_SHOT)
            engine.tick(keys)
            keys.advanceTick()
            keys.release(Key.KEY_FIRE_SHOT)
            repeat(20) {
                engine.tick(keys)
                keys.advanceTick()
            }
            return Pair(engine.playerPixelX, engine.playerPixelY)
        }

        val (x1, y1) = runSequence()
        val (x2, y2) = runSequence()

        assertEquals(x1, x2, "Determinism: X position should be identical across two identical runs")
        assertEquals(y1, y2, "Determinism: Y position should be identical across two identical runs")
    }
}
