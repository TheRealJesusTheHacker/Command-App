package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.ui.DampingLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lane B (1.4.0) M-02 pins: the pull-damping rubber band.
 * The slope at the origin is the 35% target resistance and
 * the offset asymptotes to the cap instead of running away.
 */
class DampingLogicTest {

    @Test
    fun resistanceIsThirtyFivePercent() {
        assertEquals(0.35f, DampingLogic.RESISTANCE, 0.0001f)
    }

    @Test
    fun dampedOffsetMatchesTheCurve() {
        assertEquals(0f, DampingLogic.dampedOffsetPx(0f, 200f), 0.0001f)
        // 200 * (1 - exp(-0.35 * 50 / 200)) ≈ 16.7562.
        assertEquals(16.7562f, DampingLogic.dampedOffsetPx(50f, 200f), 0.001f)
        // 200 * (1 - exp(-0.35 * 100 / 200)) ≈ 32.1086.
        assertEquals(32.1086f, DampingLogic.dampedOffsetPx(100f, 200f), 0.001f)
    }

    @Test
    fun dampedOffsetNeverExceedsTheCap() {
        var raw = 0f
        while (raw <= 5000f) {
            val damped = DampingLogic.dampedOffsetPx(raw, 200f)
            assertTrue("damped $damped exceeds cap at raw $raw", damped in 0f..200f)
            raw += 25f
        }
        // Far pulls asymptote to the cap, never past it.
        assertEquals(200f, DampingLogic.dampedOffsetPx(100000f, 200f), 0.01f)
    }

    @Test
    fun dampedOffsetRestsOnNonPositiveInput() {
        assertEquals(0f, DampingLogic.dampedOffsetPx(-10f, 200f), 0.0001f)
        assertEquals(0f, DampingLogic.dampedOffsetPx(10f, 0f), 0.0001f)
        assertEquals(0f, DampingLogic.dampedOffsetPx(10f, -5f), 0.0001f)
    }

    @Test
    fun dampedFractionIsNormalizedToTheTarget() {
        assertEquals(0f, DampingLogic.dampedFraction(0f), 0.0001f)
        assertEquals(0f, DampingLogic.dampedFraction(-1f), 0.0001f)
        // (1 - exp(-0.175)) / (1 - exp(-0.35)) ≈ 0.5436.
        assertEquals(0.5436f, DampingLogic.dampedFraction(0.5f), 0.001f)
        assertEquals(1f, DampingLogic.dampedFraction(1f), 0.0001f)
        // Beyond the target the indicator clamps at full.
        assertEquals(1f, DampingLogic.dampedFraction(2f), 0.0001f)
    }
}
