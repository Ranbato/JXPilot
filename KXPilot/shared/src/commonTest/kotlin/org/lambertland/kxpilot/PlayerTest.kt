package org.lambertland.kxpilot

import org.lambertland.kxpilot.server.ObjStatus
import org.lambertland.kxpilot.server.Player
import org.lambertland.kxpilot.server.PlayerState
import org.lambertland.kxpilot.server.PlayerType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerTest {
    // -----------------------------------------------------------------------
    // State predicates
    // -----------------------------------------------------------------------

    @Test fun isAliveWhenStateAlive() {
        val p = Player().also { it.plState = PlayerState.ALIVE }
        assertTrue(p.isAlive())
        assertFalse(p.isDead())
    }

    @Test fun isDeadWhenStateDead() {
        val p = Player().also { it.plState = PlayerState.DEAD }
        assertTrue(p.isDead())
        assertFalse(p.isAlive())
    }

    @Test fun isActiveWhenAlive() {
        val p = Player().also { it.plState = PlayerState.ALIVE }
        assertTrue(p.isActive())
    }

    @Test fun isActiveWhenKilled() {
        val p = Player().also { it.plState = PlayerState.KILLED }
        assertTrue(p.isActive())
    }

    @Test fun isNotActiveWhenDead() {
        val p = Player().also { it.plState = PlayerState.DEAD }
        assertFalse(p.isActive())
    }

    @Test fun isHumanByDefault() {
        assertTrue(Player().isHuman())
    }

    @Test fun isRobotWhenTypeRobot() {
        val p = Player().also { it.plType = PlayerType.ROBOT }
        assertTrue(p.isRobot())
        assertFalse(p.isHuman())
    }

    @Test fun isTankWhenTypeTank() {
        val p = Player().also { it.plType = PlayerType.TANK }
        assertTrue(p.isTank())
    }

    // -----------------------------------------------------------------------
    // isThrusting — bit-flag check
    // -----------------------------------------------------------------------

    @Test fun isNotThrustingByDefault() {
        assertFalse(Player().isThrusting())
    }

    @Test fun isThrustingWhenFlagSet() {
        val p = Player()
        p.objStatus = ObjStatus.THRUSTING.toUShort()
        assertTrue(p.isThrusting())
    }

    @Test fun isNotThrustingWhenOtherFlagSet() {
        val p = Player()
        p.objStatus = ObjStatus.GRAVITY.toUShort() // different flag
        assertFalse(p.isThrusting())
    }

    @Test fun isThrustingToleratesOtherFlagsSet() {
        val p = Player()
        p.objStatus = (ObjStatus.THRUSTING.toInt() or ObjStatus.GRAVITY.toInt()).toUShort()
        assertTrue(p.isThrusting())
    }

    // -----------------------------------------------------------------------
    // floatDir invariant — setFloatDir atomically updates cos/sin
    // -----------------------------------------------------------------------

    @Test fun floatDirInitialisedToZero() {
        val p = Player()
        assertEquals(0.0, p.floatDir)
        assertEquals(1.0, p.floatDirCos, 1e-15)
        assertEquals(0.0, p.floatDirSin, 1e-15)
    }

    @Test fun setFloatDirUpdatesCosAndSin() {
        val p = Player()
        val angle = Math.PI / 4.0 // 45 degrees
        p.setFloatDir(angle)
        assertEquals(angle, p.floatDir)
        assertTrue(
            abs(p.floatDirCos - cos(angle)) < 1e-15,
            "Expected floatDirCos ≈ ${cos(angle)} but was ${p.floatDirCos}",
        )
        assertTrue(
            abs(p.floatDirSin - sin(angle)) < 1e-15,
            "Expected floatDirSin ≈ ${sin(angle)} but was ${p.floatDirSin}",
        )
    }

    @Test fun setFloatDirPiUpdatesCorrectly() {
        val p = Player()
        p.setFloatDir(Math.PI)
        assertTrue(abs(p.floatDirCos - (-1.0)) < 1e-15)
        assertTrue(abs(p.floatDirSin) < 1e-15)
    }

    @Test fun setFloatDirTwiceReflectsLatestValue() {
        val p = Player()
        p.setFloatDir(1.0)
        p.setFloatDir(2.5)
        assertEquals(2.5, p.floatDir)
        assertTrue(abs(p.floatDirCos - cos(2.5)) < 1e-15)
        assertTrue(abs(p.floatDirSin - sin(2.5)) < 1e-15)
    }

    @Test fun setFloatDirNegativeAngle() {
        val p = Player()
        p.setFloatDir(-Math.PI / 2.0)
        assertTrue(abs(p.floatDirCos - cos(-Math.PI / 2.0)) < 1e-14)
        assertTrue(abs(p.floatDirSin - sin(-Math.PI / 2.0)) < 1e-14)
    }
}
