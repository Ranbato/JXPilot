package org.lambertland.kxpilot

import org.lambertland.kxpilot.client.KeyState
import org.lambertland.kxpilot.common.ClPos
import org.lambertland.kxpilot.common.ClickConst
import org.lambertland.kxpilot.common.Key
import org.lambertland.kxpilot.common.Vector
import org.lambertland.kxpilot.engine.GameEngine
import org.lambertland.kxpilot.engine.MissileData
import org.lambertland.kxpilot.engine.PlayerItems
import org.lambertland.kxpilot.engine.ShotData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun keysDown(vararg k: Key): KeyState = KeyState().also { ks -> k.forEach { ks.press(it) } }

private fun noKeys() = KeyState()

class Bl12DeflectorTest {
    @Test
    fun deflectorToggleOnWithItem() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(deflector = 1)
        assertFalse(engine.deflectorActive)
        val keys = keysDown(Key.KEY_DEFLECTOR)
        engine.tick(keys)
        keys.advanceTick()
        assertTrue(engine.deflectorActive, "KEY_DEFLECTOR should activate deflector")
    }

    @Test
    fun deflectorRequiresItem() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(deflector = 0)
        val keys = keysDown(Key.KEY_DEFLECTOR)
        engine.tick(keys)
        keys.advanceTick()
        assertFalse(engine.deflectorActive, "Deflector should not activate without item")
    }

    @Test
    fun deflectorDrainsFuel() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(deflector = 1)
        val keys = keysDown(Key.KEY_DEFLECTOR)
        engine.tick(keys)
        keys.advanceTick()
        assertTrue(engine.deflectorActive)
        val fuelBefore = engine.fuel
        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertTrue(engine.fuel < fuelBefore, "Deflector should drain fuel each tick")
    }

    @Test
    fun deflectorDeactivatesOnFuelEmpty() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(deflector = 1)
        engine.deflectorActive = true
        engine.fuel = 0.001 // nearly empty
        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertFalse(engine.deflectorActive, "Deflector should deactivate when fuel runs out")
    }

    @Test
    fun deflectorPushesApproachingShot() {
        val engine = GameEngine.forEmptyWorld(30, 30)
        engine.playerItems = PlayerItems(deflector = 2)
        engine.deflectorActive = true
        // Place a shot 50px to the right moving left toward the player
        val shotCx = engine.world.wrapXClick(engine.player.pos.cx + 50 * ClickConst.CLICK)
        val shotCy = engine.player.pos.cy
        val shotVelBefore = Vector(-3f, 0f) // moving left toward player
        val shot =
            ShotData(
                pos = ClPos(shotCx, shotCy),
                vel = shotVelBefore,
                life = 100f,
                ownerId = 99,
            )
        engine.shots.add(shot)
        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        // Shot x-velocity should have been pushed rightward (away from player)
        assertTrue(
            shot.vel.x > shotVelBefore.x,
            "Deflector should push approaching shot away (vx went from ${shotVelBefore.x} to ${shot.vel.x})",
        )
    }
}

// ---------------------------------------------------------------------------
// BL-13 — Cloaking device
// ---------------------------------------------------------------------------

class Bl13CloakTest {
    @Test
    fun cloakToggleOnWithItem() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(cloak = 1)
        assertFalse(engine.cloakActive)
        val keys = keysDown(Key.KEY_CLOAK)
        engine.tick(keys)
        keys.advanceTick()
        assertTrue(engine.cloakActive, "KEY_CLOAK should activate cloak")
    }

    @Test
    fun cloakRequiresItem() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(cloak = 0)
        val keys = keysDown(Key.KEY_CLOAK)
        engine.tick(keys)
        keys.advanceTick()
        assertFalse(engine.cloakActive, "Cloak should not activate without item")
    }

    @Test
    fun cloakDrainsFuel() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(cloak = 1)
        engine.cloakActive = true
        val fuelBefore = engine.fuel
        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertTrue(engine.fuel < fuelBefore, "Cloak should drain fuel per tick")
    }

    @Test
    fun cloakDeactivatesOnFuelEmpty() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(cloak = 1)
        engine.cloakActive = true
        engine.fuel = 0.001
        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertFalse(engine.cloakActive, "Cloak should deactivate when fuel runs out")
    }

    @Test
    fun cloakCausesMissileToLoseLock() {
        // With cloak active and NO sensor NPCs, the random roll should almost always
        // resolve to confused (cloak=1 vs sensor=0 → P(visible) = rfrac()*1 > rfrac()*2 ≈ 25%).
        // Run enough ticks that at least one confusion event occurs.
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(cloak = 3) // 3 cloak items for reliable suppression
        engine.cloakActive = true
        // Spawn a missile targeting the player
        val missileCx = engine.player.pos.cx + 200 * ClickConst.CLICK
        val missileCy = engine.player.pos.cy
        engine.missiles.add(
            MissileData(
                pos = ClPos(missileCx, missileCy),
                headingRad = Math.PI, // pointing left toward player
                life = 11520f,
                targetNpcId = engine.player.id.toInt(),
                ownerId = 99,
            ),
        )
        val noKeys = noKeys()
        // Tick several times — missile should become confused at least once
        var everConfused = false
        repeat(20) {
            engine.tick(noKeys)
            noKeys.advanceTick()
            if (engine.missiles.any { it.confusedTicks > 0f }) everConfused = true
        }
        assertTrue(everConfused, "Cloaked player should cause missile to enter confused state")
    }
}

// ---------------------------------------------------------------------------
// BL-14 — Phasing device
// ---------------------------------------------------------------------------

class Bl14PhasingTest {
    @Test
    fun phasingActivatesAndConsumesCharge() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(phasing = 1)
        assertFalse(engine.phasingActive)
        val keys = keysDown(Key.KEY_PHASING)
        engine.tick(keys)
        keys.advanceTick()
        assertTrue(engine.phasingActive, "KEY_PHASING should activate phasing")
        assertEquals(0, engine.playerItems.phasing, "Phasing should consume one charge on activate")
    }

    @Test
    fun phasingRequiresCharge() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(phasing = 0)
        val keys = keysDown(Key.KEY_PHASING)
        engine.tick(keys)
        keys.advanceTick()
        assertFalse(engine.phasingActive, "Phasing should not activate without charge")
    }

    @Test
    fun phasingDrainsFuel() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(phasing = 1)
        engine.phasingActive = true
        engine.phasingTicksLeft = 1000.0
        val fuelBefore = engine.fuel
        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertTrue(engine.fuel < fuelBefore, "Phasing should drain fuel per tick")
    }

    @Test
    fun phasingShotPassesThrough() {
        // Place a wall next to the player; when phasing, player should survive a shot
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(phasing = 1)
        engine.phasingActive = true
        engine.phasingTicksLeft = 1000.0
        // Spawn a shot directly on the player
        val shot =
            ShotData(
                pos = engine.player.pos,
                vel =
                    org.lambertland.kxpilot.common
                        .Vector(0f, 0f),
                life = 100f,
                ownerId = 99,
            )
        engine.shots.add(shot)
        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertTrue(engine.player.isAlive(), "Phased player should not be killed by shots")
    }

    @Test
    fun phasingTimerAutoConsumesNextCharge() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(phasing = 1)
        engine.phasingActive = true
        engine.phasingTicksLeft = 1.0 // about to expire
        // 1 charge remaining; when timer expires it should be consumed and timer reset
        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        // Timer expired; should have consumed the next charge and reset timer
        assertTrue(engine.phasingActive, "Phasing should stay active when next charge is consumed")
        assertEquals(0, engine.playerItems.phasing, "Second charge should be consumed")
        assertTrue(engine.phasingTicksLeft > 0.0, "Timer should be reset for next charge")
    }

    @Test
    fun phasingDeactivatesWhenChargesExhausted() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(phasing = 0)
        engine.phasingActive = true
        engine.phasingTicksLeft = 1.0
        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertFalse(engine.phasingActive, "Phasing should deactivate when no charges remain")
    }
}

// ---------------------------------------------------------------------------
// BL-15 — Emergency shield
// ---------------------------------------------------------------------------

class Bl15EmergencyShieldTest {
    @Test
    fun emergencyShieldActivatesOnKeyPress() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(emergencyShield = 1)
        assertFalse(engine.emergencyShieldActive)
        val keys = keysDown(Key.KEY_EMERGENCY_SHIELD)
        engine.tick(keys)
        keys.advanceTick()
        assertTrue(engine.emergencyShieldActive, "KEY_EMERGENCY_SHIELD should activate")
        assertEquals(0, engine.playerItems.emergencyShield, "Charge should be consumed on activate")
    }

    @Test
    fun emergencyShieldRequiresCharge() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(emergencyShield = 0)
        val keys = keysDown(Key.KEY_EMERGENCY_SHIELD)
        engine.tick(keys)
        keys.advanceTick()
        assertFalse(engine.emergencyShieldActive, "Emergency shield needs a charge")
    }

    @Test
    fun emergencyShieldAutoActivatesOnFirstPickup() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(emergencyShield = 0)
        // Manually trigger pickup via internal helper
        engine.applyItemPickup(org.lambertland.kxpilot.common.Item.EMERGENCY_SHIELD)
        assertTrue(engine.emergencyShieldActive, "Emergency shield should auto-activate on first pickup")
    }

    @Test
    fun emergencyShieldBlocksShot() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(emergencyShield = 1)
        engine.emergencyShieldActive = true
        engine.emergencyShieldTicksLeft = 1000.0
        // Place a shot on the player
        val shot =
            ShotData(
                pos = engine.player.pos,
                vel =
                    org.lambertland.kxpilot.common
                        .Vector(0f, 0f),
                life = 100f,
                ownerId = 99,
            )
        engine.shots.add(shot)
        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertTrue(engine.player.isAlive(), "Emergency shield should block shot")
    }

    @Test
    fun emergencyShieldTimerOnlyAdvancesOnCollision() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(emergencyShield = 0)
        engine.emergencyShieldActive = true
        val initialTicks = 100.0
        engine.emergencyShieldTicksLeft = initialTicks
        // No shots — no collision — timer should NOT advance
        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertEquals(initialTicks, engine.emergencyShieldTicksLeft, "Timer should not advance without collision")
    }
}

// ---------------------------------------------------------------------------
// BL-16 — Emergency thrust
// ---------------------------------------------------------------------------

class Bl16EmergencyThrustTest {
    @Test
    fun emergencyThrustActivatesOnKeyPress() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(emergencyThrust = 1)
        assertFalse(engine.emergencyThrustActive)
        val keys = keysDown(Key.KEY_EMERGENCY_THRUST)
        engine.tick(keys)
        keys.advanceTick()
        assertTrue(engine.emergencyThrustActive, "KEY_EMERGENCY_THRUST should activate")
        assertEquals(0, engine.playerItems.emergencyThrust, "Charge should be consumed on activate")
    }

    @Test
    fun emergencyThrustRequiresCharge() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(emergencyThrust = 0)
        val keys = keysDown(Key.KEY_EMERGENCY_THRUST)
        engine.tick(keys)
        keys.advanceTick()
        assertFalse(engine.emergencyThrustActive, "Emergency thrust needs a charge")
    }

    @Test
    fun emergencyThrustGivesMoreThrustThanNone() {
        fun velocityGainAfterThrustTick(emergency: Boolean): Float {
            val engine = GameEngine.forEmptyWorld(20, 20)
            engine.player.setFloatDir(0.0) // face right
            engine.playerItems =
                PlayerItems(
                    emergencyThrust = if (emergency) 0 else 0,
                    afterburner = 0,
                )
            engine.emergencyThrustActive = emergency
            engine.emergencyThrustTicksLeft = if (emergency) 1000.0 else 0.0
            val before = engine.player.vel.x
            val keys = keysDown(Key.KEY_THRUST)
            engine.tick(keys)
            keys.advanceTick()
            return engine.player.vel.x - before
        }
        val normalGain = velocityGainAfterThrustTick(emergency = false)
        val emergencyGain = velocityGainAfterThrustTick(emergency = true)
        assertTrue(
            emergencyGain > normalGain,
            "Emergency thrust should give more velocity than no emergency thrust (normal=$normalGain, emergency=$emergencyGain)",
        )
    }

    @Test
    fun emergencyThrustTimerOnlyDecreasesWhileThrusting() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(emergencyThrust = 0)
        engine.emergencyThrustActive = true
        val initialTicks = 100.0
        engine.emergencyThrustTicksLeft = initialTicks
        // NOT pressing thrust key
        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertEquals(
            initialTicks,
            engine.emergencyThrustTicksLeft,
            "Emergency thrust timer should not decrease when not thrusting",
        )
    }

    @Test
    fun emergencyThrustTimerDecreasesWhileThrusting() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(emergencyThrust = 0)
        engine.emergencyThrustActive = true
        val initialTicks = 100.0
        engine.emergencyThrustTicksLeft = initialTicks
        val keys = keysDown(Key.KEY_THRUST)
        engine.tick(keys)
        keys.advanceTick()
        assertTrue(
            engine.emergencyThrustTicksLeft < initialTicks,
            "Emergency thrust timer should decrease while thrusting",
        )
    }

    @Test
    fun emergencyThrustDeactivatesOnTimerExpiry() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(emergencyThrust = 0) // no charges to consume
        engine.emergencyThrustActive = true
        engine.emergencyThrustTicksLeft = 1.0 // about to expire
        val keys = keysDown(Key.KEY_THRUST)
        engine.tick(keys)
        keys.advanceTick()
        assertFalse(engine.emergencyThrustActive, "Emergency thrust should deactivate on timer expiry with no charges")
    }
}
