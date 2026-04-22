package org.lambertland.kxpilot

import org.lambertland.kxpilot.common.EnergyDrain
import org.lambertland.kxpilot.common.GameConst
import org.lambertland.kxpilot.server.Player
import org.lambertland.kxpilot.server.PlayerAbility
import org.lambertland.kxpilot.server.ServerConfig
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// ConstantsTest
// ---------------------------------------------------------------------------
// Verifies that every constant added / changed in this audit pass matches
// the corresponding value in the C source (common/const.h, server/serverconst.h).
// ---------------------------------------------------------------------------

class ConstantsTest {
    // -----------------------------------------------------------------------
    // C-2: MAX_PLAYER_FUEL
    // -----------------------------------------------------------------------

    @Test
    fun maxPlayerFuel_matches_C_value() {
        // serverconst.h: #define MAX_PLAYER_FUEL 2600.0
        assertEquals(2600.0, GameConst.MAX_PLAYER_FUEL)
    }

    // -----------------------------------------------------------------------
    // Fuel helpers
    // -----------------------------------------------------------------------

    @Test
    fun refuelRate_matches_C_value() {
        // serverconst.h: #define REFUEL_RATE 5.0
        assertEquals(5.0, GameConst.REFUEL_RATE)
    }

    @Test
    fun fuelMassCoeff_matches_C_value() {
        // serverconst.h: #define FUEL_MASS(f) ((f) * 0.005)
        assertEquals(0.005, GameConst.FUEL_MASS_COEFF)
    }

    @Test
    fun fuelMass_helper_multiplesByCoeff() {
        assertEquals(0.0, GameConst.fuelMass(0.0))
        assertEquals(1.0, GameConst.fuelMass(200.0)) // 200 * 0.005 = 1.0
        assertEquals(13.0, GameConst.fuelMass(2600.0)) // MAX_PLAYER_FUEL * 0.005 = 13.0
    }

    @Test
    fun thrustMass_matches_C_value() {
        // serverconst.h: #define THRUST_MASS 0.7
        assertEquals(0.7, GameConst.THRUST_MASS)
    }

    // -----------------------------------------------------------------------
    // M-4: NO_ID
    // -----------------------------------------------------------------------

    @Test
    fun noId_matches_C_value() {
        // serverconst.h: #define NO_ID (-1)
        assertEquals(-1, GameConst.NO_ID)
    }

    // -----------------------------------------------------------------------
    // M-5: NUM_IDS
    // -----------------------------------------------------------------------

    @Test
    fun numIds_matches_C_value() {
        // serverconst.h: #define NUM_IDS 256
        assertEquals(256, GameConst.NUM_IDS)
    }

    // -----------------------------------------------------------------------
    // M-6: Cannon ID range
    // -----------------------------------------------------------------------

    @Test
    fun minCannonId_isOneAboveExpiredMineId() {
        // serverconst.h: #define MIN_CANNON_ID (EXPIRED_MINE_ID + 1)
        assertEquals(GameConst.EXPIRED_MINE_ID + 1, GameConst.MIN_CANNON_ID)
        assertEquals(4097, GameConst.MIN_CANNON_ID)
    }

    @Test
    fun maxCannonId_isExpiredMineIdPlusNumCannonIds() {
        // serverconst.h: #define MAX_CANNON_ID (EXPIRED_MINE_ID + NUM_CANNON_IDS)
        assertEquals(GameConst.EXPIRED_MINE_ID + GameConst.NUM_CANNON_IDS, GameConst.MAX_CANNON_ID)
        assertEquals(14096, GameConst.MAX_CANNON_ID)
    }

    @Test
    fun numCannonIds_matchesRange() {
        // The range [MIN_CANNON_ID, MAX_CANNON_ID] contains exactly NUM_CANNON_IDS values
        assertEquals(
            GameConst.NUM_CANNON_IDS,
            GameConst.MAX_CANNON_ID - GameConst.MIN_CANNON_ID + 1,
        )
    }

    // -----------------------------------------------------------------------
    // M-7: ALLIANCE_NOT_SET
    // -----------------------------------------------------------------------

    @Test
    fun allianceNotSet_matches_C_value() {
        // serverconst.h: #define ALLIANCE_NOT_SET (-1)
        assertEquals(-1, GameConst.ALLIANCE_NOT_SET)
    }

    // -----------------------------------------------------------------------
    // M-8: RECOVERY_DELAY
    // -----------------------------------------------------------------------

    @Test
    fun recoveryDelay_matches_C_value() {
        // serverconst.h: #define RECOVERY_DELAY (12 * 3)
        assertEquals(36, GameConst.RECOVERY_DELAY)
    }

    // -----------------------------------------------------------------------
    // M-9: EnergyDrain constants
    // -----------------------------------------------------------------------

    @Test
    fun energyDrain_allNegative() {
        // All ED_* values are negative (they drain fuel)
        assertTrue(EnergyDrain.SHOT < 0.0)
        assertTrue(EnergyDrain.SMART_SHOT < 0.0)
        assertTrue(EnergyDrain.MINE < 0.0)
        assertTrue(EnergyDrain.ECM < 0.0)
        assertTrue(EnergyDrain.TRANSPORTER < 0.0)
        assertTrue(EnergyDrain.HYPERJUMP < 0.0)
        assertTrue(EnergyDrain.SHIELD < 0.0)
        assertTrue(EnergyDrain.PHASING_DEVICE < 0.0)
        assertTrue(EnergyDrain.CLOAKING_DEVICE < 0.0)
        assertTrue(EnergyDrain.DEFLECTOR < 0.0)
        assertTrue(EnergyDrain.SHOT_HIT < 0.0)
        assertTrue(EnergyDrain.SMART_SHOT_HIT < 0.0)
        assertTrue(EnergyDrain.LASER < 0.0)
        assertTrue(EnergyDrain.LASER_HIT < 0.0)
    }

    @Test
    fun energyDrain_spot_check_C_values() {
        // serverconst.h spot-checks
        assertEquals(-0.2, EnergyDrain.SHOT)
        assertEquals(-30.0, EnergyDrain.SMART_SHOT)
        assertEquals(-60.0, EnergyDrain.MINE)
        assertEquals(-60.0, EnergyDrain.ECM)
        assertEquals(-0.20, EnergyDrain.SHIELD)
        assertEquals(-0.40, EnergyDrain.PHASING_DEVICE)
        assertEquals(-0.07, EnergyDrain.CLOAKING_DEVICE)
        assertEquals(-0.15, EnergyDrain.DEFLECTOR)
        assertEquals(-25.0, EnergyDrain.SHOT_HIT)
        assertEquals(-120.0, EnergyDrain.SMART_SHOT_HIT)
        assertEquals(-10.0, EnergyDrain.LASER)
        assertEquals(-100.0, EnergyDrain.LASER_HIT)
    }

    // -----------------------------------------------------------------------
    // M-10: Timer constants
    // -----------------------------------------------------------------------

    @Test
    fun shieldTime_matches_C_value() {
        // serverconst.h: #define SHIELD_TIME (2 * 12)
        assertEquals(24, GameConst.SHIELD_TIME)
    }

    @Test
    fun emergencyShieldTime_matches_C_value() {
        // serverconst.h: #define EMERGENCY_SHIELD_TIME (4 * 12)
        assertEquals(48, GameConst.EMERGENCY_SHIELD_TIME)
    }

    @Test
    fun phasingTime_matches_C_value() {
        // serverconst.h: #define PHASING_TIME (4 * 12)
        assertEquals(48, GameConst.PHASING_TIME)
    }

    @Test
    fun emergencyThrustTime_matches_C_value() {
        // serverconst.h: #define EMERGENCY_THRUST_TIME (4 * 12)
        assertEquals(48, GameConst.EMERGENCY_THRUST_TIME)
    }

    // -----------------------------------------------------------------------
    // N-4: MAX_SERVER_FPS / ServerConfig validation
    // -----------------------------------------------------------------------

    @Test
    fun maxServerFps_matches_C_value() {
        // serverconst.h: #define MAX_SERVER_FPS 255
        assertEquals(255, GameConst.MAX_SERVER_FPS)
    }

    @Test
    fun serverConfig_accepts_fps_up_to_255() {
        // Should not throw
        val cfg = ServerConfig(serverName = "t", port = 9999, targetFps = 255)
        assertEquals(255, cfg.targetFps)
    }

    @Test
    fun serverConfig_rejects_fps_above_255() {
        assertFailsWith<IllegalArgumentException> {
            ServerConfig(serverName = "t", port = 9999, targetFps = 256)
        }
    }

    @Test
    fun serverConfig_rejects_fps_of_zero() {
        assertFailsWith<IllegalArgumentException> {
            ServerConfig(serverName = "t", port = 9999, targetFps = 0)
        }
    }

    // -----------------------------------------------------------------------
    // N-5: MAX_TOTAL_SHOTS
    // -----------------------------------------------------------------------

    @Test
    fun maxTotalShots_matches_C_value() {
        // serverconst.h: #define MAX_TOTAL_SHOTS 16384
        assertEquals(16384, GameConst.MAX_TOTAL_SHOTS)
    }

    // -----------------------------------------------------------------------
    // N-6: Missile/mine physics constants
    // -----------------------------------------------------------------------

    @Test
    fun mineMass_matches_C_value() {
        // serverconst.h: #define MINE_MASS 30.0
        assertEquals(30.0, GameConst.MINE_MASS)
    }

    @Test
    fun mineRadius_matches_C_value() {
        // serverconst.h: #define MINE_RADIUS 8
        assertEquals(8, GameConst.MINE_RADIUS)
    }

    @Test
    fun mineSpeedFact_matches_C_value() {
        // serverconst.h: #define MINE_SPEED_FACT 1.3
        assertEquals(1.3, GameConst.MINE_SPEED_FACT)
    }

    @Test
    fun missileMass_matches_C_value() {
        // serverconst.h: #define MISSILE_MASS 5.0
        assertEquals(5.0, GameConst.MISSILE_MASS)
    }

    // -----------------------------------------------------------------------
    // Afterburner level constants
    // -----------------------------------------------------------------------

    @Test
    fun lg2MaxAfterburner_matches_C_value() {
        assertEquals(4, GameConst.LG2_MAX_AFTERBURNER)
    }

    @Test
    fun maxAfterburner_is_15() {
        // serverconst.h: #define MAX_AFTERBURNER ((1<<LG2_MAX_AFTERBURNER)-1) = 15
        assertEquals(15, GameConst.MAX_AFTERBURNER)
        assertEquals((1 shl GameConst.LG2_MAX_AFTERBURNER) - 1, GameConst.MAX_AFTERBURNER)
    }
}

// ---------------------------------------------------------------------------
// FloatDirTest
// ---------------------------------------------------------------------------
// Verifies the C-1 fix: floatDirInRes() correctly converts radians → RES units.
// ---------------------------------------------------------------------------

class FloatDirTest {
    private fun makePlayer(): Player = Player()

    // -----------------------------------------------------------------------
    // floatDirInRes() unit conversion
    // -----------------------------------------------------------------------

    @Test
    fun floatDirInRes_atZero_isZero() {
        val pl = makePlayer()
        pl.setFloatDir(0.0)
        assertEquals(0.0, pl.floatDirInRes(), 1e-10)
    }

    @Test
    fun floatDirInRes_atHalfPi_isQuarterRes() {
        // π/2 radians = 32 RES units  (90° = RES/4 = 32)
        val pl = makePlayer()
        pl.setFloatDir(kotlin.math.PI / 2.0)
        assertEquals(GameConst.RES / 4.0, pl.floatDirInRes(), 1e-10)
    }

    @Test
    fun floatDirInRes_atPi_isHalfRes() {
        // π radians = 64 RES units  (180° = RES/2 = 64)
        val pl = makePlayer()
        pl.setFloatDir(kotlin.math.PI)
        assertEquals(GameConst.RES / 2.0, pl.floatDirInRes(), 1e-10)
    }

    @Test
    fun floatDirInRes_atThreeHalvesPi_isThreeQuarterRes() {
        // 3π/2 radians = 96 RES units  (270° = 3*RES/4 = 96)
        val pl = makePlayer()
        pl.setFloatDir(3.0 * kotlin.math.PI / 2.0)
        assertEquals(3.0 * GameConst.RES / 4.0, pl.floatDirInRes(), 1e-10)
    }

    @Test
    fun floatDirInRes_atTwoPi_wrapsToZero() {
        // 2π should wrap to 0 (same as 0 radians = 0 RES units)
        val pl = makePlayer()
        pl.setFloatDir(2.0 * GameConst.PI_VALUE)
        assertEquals(0.0, pl.floatDirInRes(), 1e-10)
    }

    @Test
    fun floatDirInRes_resultAlwaysInRange() {
        val pl = makePlayer()
        val steps = 360
        for (i in 0 until steps) {
            val angle = i * 2.0 * GameConst.PI_VALUE / steps
            pl.setFloatDir(angle)
            val res = pl.floatDirInRes()
            assertTrue(
                res >= 0.0 && res < GameConst.RES,
                "floatDirInRes()=$res not in [0, RES) for angle=$angle",
            )
        }
    }

    @Test
    fun floatDirInRes_trigConsistency() {
        // cos(floatDirInRes * 2π / RES) must equal floatDirCos (and sin likewise)
        val pl = makePlayer()
        val steps = 64
        for (i in 0 until steps) {
            val angle = i * 2.0 * GameConst.PI_VALUE / steps
            pl.setFloatDir(angle)
            val resUnits = pl.floatDirInRes()
            val reconstructedCos = cos(resUnits * 2.0 * GameConst.PI_VALUE / GameConst.RES)
            val reconstructedSin = sin(resUnits * 2.0 * GameConst.PI_VALUE / GameConst.RES)
            assertEquals(
                pl.floatDirCos,
                reconstructedCos,
                1e-10,
                "cos mismatch at angle=$angle resUnits=$resUnits",
            )
            assertEquals(
                pl.floatDirSin,
                reconstructedSin,
                1e-10,
                "sin mismatch at angle=$angle resUnits=$resUnits",
            )
        }
    }

    // -----------------------------------------------------------------------
    // M-3: have/used 32-bit wire truncation round-trip
    // -----------------------------------------------------------------------

    @Test
    fun haveUsed_allDefinedBitsFitIn32Bits() {
        // All PlayerAbility constants must be ≤ bit 30 (within unsigned 32 bits)
        val allBits =
            listOf(
                PlayerAbility.EMERGENCY_THRUST,
                PlayerAbility.AUTOPILOT,
                PlayerAbility.TRACTOR_BEAM,
                PlayerAbility.LASER,
                PlayerAbility.CLOAKING_DEVICE,
                PlayerAbility.SHIELD,
                PlayerAbility.REFUEL,
                PlayerAbility.REPAIR,
                PlayerAbility.COMPASS,
                PlayerAbility.AFTERBURNER,
                PlayerAbility.CONNECTOR,
                PlayerAbility.EMERGENCY_SHIELD,
                PlayerAbility.DEFLECTOR,
                PlayerAbility.PHASING_DEVICE,
                PlayerAbility.MIRROR,
                PlayerAbility.ARMOR,
                PlayerAbility.SHOT,
                PlayerAbility.BALL,
            )
        for (bit in allBits) {
            assertTrue(
                bit <= 0xFFFFFFFFL,
                "PlayerAbility bit $bit exceeds 32-bit unsigned range",
            )
        }
    }

    @Test
    fun haveUsed_wireFormatTruncationLossy() {
        // Truncating a Long `have` to 32 bits must preserve all defined ability bits
        val pl = Player()
        // Set every defined ability
        pl.have = PlayerAbility.EMERGENCY_THRUST or PlayerAbility.SHIELD or
            PlayerAbility.LASER or PlayerAbility.BALL
        val truncated = (pl.have and 0xFFFFFFFFL).toInt().toLong() and 0xFFFFFFFFL
        assertEquals(
            pl.have,
            truncated,
            "Truncating have to 32 bits must not lose any defined ability bits",
        )
    }
}
