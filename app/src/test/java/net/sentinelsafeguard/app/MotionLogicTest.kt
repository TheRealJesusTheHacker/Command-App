package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.ui.MotionLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Motion pass (2026-11-04) pins: the estate motion numbers the
 * app ships — M-01 sink depth, M-09 stagger cap, M-13 shake
 * shape, M-06 tick beats, and the reduced-motion fade token.
 * Pure arithmetic: if a future edit retunes a pattern, it
 * changes these pins deliberately, in the open.
 */
class MotionLogicTest {

    @Test
    fun pressSinkIsNinetyFivePercent() {
        assertEquals(0.95f, MotionLogic.PRESS_SINK_SCALE, 0.0001f)
    }

    @Test
    fun staggerStepsSeventyMsAndCapsAtTenRows() {
        assertEquals(0L, MotionLogic.staggerDelayMs(0))
        assertEquals(70L, MotionLogic.staggerDelayMs(1))
        assertEquals(350L, MotionLogic.staggerDelayMs(5))
        assertEquals(630L, MotionLogic.staggerDelayMs(9))
        // At and past the cap: the -1 "render instantly" sentinel.
        assertEquals(-1L, MotionLogic.staggerDelayMs(10))
        assertEquals(-1L, MotionLogic.staggerDelayMs(250))
        // A bogus negative index never delays.
        assertEquals(0L, MotionLogic.staggerDelayMs(-3))
    }

    @Test
    fun shakeStartsAndEndsAtRest() {
        assertEquals(0f, MotionLogic.shakeOffsetDp(0f), 0.0001f)
        assertEquals(0f, MotionLogic.shakeOffsetDp(1f), 0.0001f)
        // Half-way: sin(3π/2) = -1, decayed by (1 - 0.5) → -4 dp.
        assertEquals(-4f, MotionLogic.shakeOffsetDp(0.5f), 0.001f)
    }

    @Test
    fun shakeNeverExceedsItsPeak() {
        var i = 0
        while (i <= 100) {
            val offset = MotionLogic.shakeOffsetDp(i / 100f)
            assertTrue(
                "offset $offset at ${i / 100f} exceeds the peak",
                kotlin.math.abs(offset) <= MotionLogic.SHAKE_PEAK_DP + 0.001f,
            )
            i++
        }
        assertEquals(360, MotionLogic.SHAKE_DURATION_MS)
        assertEquals(8f, MotionLogic.SHAKE_PEAK_DP, 0.0001f)
    }

    @Test
    fun tickDrawsCircleThenMark() {
        assertEquals(470, MotionLogic.TICK_DURATION_MS)
        // Nothing drawn at 0; circle complete at the 60% beat
        // with the mark not yet started; both complete at 1.
        assertEquals(0f, MotionLogic.tickCircleFraction(0f), 0.0001f)
        assertEquals(0f, MotionLogic.tickMarkFraction(0f), 0.0001f)
        assertEquals(1f, MotionLogic.tickCircleFraction(0.6f), 0.0001f)
        assertEquals(0f, MotionLogic.tickMarkFraction(0.6f), 0.0001f)
        assertEquals(0.5f, MotionLogic.tickMarkFraction(0.8f), 0.0001f)
        assertEquals(1f, MotionLogic.tickCircleFraction(1f), 0.0001f)
        assertEquals(1f, MotionLogic.tickMarkFraction(1f), 0.0001f)
        // Out-of-range progress clamps instead of overdrawing.
        assertEquals(1f, MotionLogic.tickCircleFraction(2f), 0.0001f)
        assertEquals(0f, MotionLogic.tickMarkFraction(-1f), 0.0001f)
    }

    @Test
    fun reducedMotionFadeIsTheToken() {
        assertEquals(120, MotionLogic.REDUCED_FADE_MS)
    }
}
