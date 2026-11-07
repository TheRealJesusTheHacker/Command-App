package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.ui.ElevationLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lane B (1.4.0) pins for the shared elevation kit's pure
 * numbers: shimmer range, top glow, deck stagger, and the
 * glow saturation curve. Pure arithmetic — a retune changes
 * these pins deliberately, in the open.
 */
class ElevationLogicTest {

    @Test
    fun shimmerTravelsTheSharedPulseRange() {
        assertEquals(0.35f, ElevationLogic.SHIMMER_MIN_ALPHA, 0.0001f)
        assertEquals(0.80f, ElevationLogic.SHIMMER_MAX_ALPHA, 0.0001f)
        assertEquals(0.35f, ElevationLogic.shimmerAlpha(0f), 0.0001f)
        assertEquals(0.575f, ElevationLogic.shimmerAlpha(0.5f), 0.0001f)
        assertEquals(0.80f, ElevationLogic.shimmerAlpha(1f), 0.0001f)
        // Out-of-range phases clamp to the range ends.
        assertEquals(0.35f, ElevationLogic.shimmerAlpha(-2f), 0.0001f)
        assertEquals(0.80f, ElevationLogic.shimmerAlpha(3f), 0.0001f)
    }

    @Test
    fun topGlowIsFourPercent() {
        assertEquals(0.04f, ElevationLogic.TOP_GLOW_ALPHA, 0.0001f)
    }

    @Test
    fun deckStaggerStepsSixtyMsAndCapsAtEight() {
        assertEquals(0L, ElevationLogic.deckStaggerDelayMs(0))
        assertEquals(60L, ElevationLogic.deckStaggerDelayMs(1))
        assertEquals(420L, ElevationLogic.deckStaggerDelayMs(7))
        assertEquals(-1L, ElevationLogic.deckStaggerDelayMs(8))
        assertEquals(-1L, ElevationLogic.deckStaggerDelayMs(100))
        assertEquals(0L, ElevationLogic.deckStaggerDelayMs(-1))
    }

    @Test
    fun glowSaturatesWithElevation() {
        assertEquals(0f, ElevationLogic.glowAlpha(0f), 0.0001f)
        assertEquals(0f, ElevationLogic.glowAlpha(-3f), 0.0001f)
        // 1 - exp(-0.25 * 4) = 1 - exp(-1) ≈ 0.6321.
        assertEquals(0.6321f, ElevationLogic.glowAlpha(4f), 0.001f)
        var e = 0f
        var previous = -1f
        while (e <= 40f) {
            val alpha = ElevationLogic.glowAlpha(e)
            assertTrue("glow $alpha out of range at $e", alpha in 0f..1f)
            assertTrue("glow not monotonic at $e", alpha >= previous)
            previous = alpha
            e += 2f
        }
    }
}
