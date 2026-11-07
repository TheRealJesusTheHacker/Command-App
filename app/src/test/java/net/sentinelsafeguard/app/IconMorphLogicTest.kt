package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.ui.IconMorphLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Icon Morph Program (2026-11-07) pins: the M-18 numbers the
 * app ships — Search↔X 400 ms with the circle undrawing and
 * the cross completing in the second half; Send arc 500 ms
 * with the fresh plane arriving in the last 30%; Download
 * story 1.2 s phased arrow → ring → tick; Unlock spring with
 * the 34% overshoot peak. Pure arithmetic, same contract as
 * MotionLogicTest: retuning a pattern changes these pins
 * deliberately, in the open.
 */
class IconMorphLogicTest {

    @Test
    fun searchMorphIs400msCircleUndrawsCrossCompletesLate() {
        assertEquals(400, IconMorphLogic.SEARCH_MORPH_MS)
        assertEquals(1f, IconMorphLogic.searchCircleFraction(0f), 0.0001f)
        assertEquals(0.5f, IconMorphLogic.searchCircleFraction(0.5f), 0.0001f)
        assertEquals(0f, IconMorphLogic.searchCircleFraction(1f), 0.0001f)
        // The cross stroke waits for the handle's swing (first half).
        assertEquals(0f, IconMorphLogic.searchCrossFraction(0.25f), 0.0001f)
        assertEquals(0f, IconMorphLogic.searchCrossFraction(0.5f), 0.0001f)
        assertEquals(0.5f, IconMorphLogic.searchCrossFraction(0.75f), 0.0001f)
        assertEquals(1f, IconMorphLogic.searchCrossFraction(1f), 0.0001f)
        // Out-of-range clamps.
        assertEquals(1f, IconMorphLogic.searchCircleFraction(-1f), 0.0001f)
        assertEquals(1f, IconMorphLogic.searchCrossFraction(2f), 0.0001f)
    }

    @Test
    fun sendArcIs500msAndTheFreshPlaneArrivesInTheLastStretch() {
        assertEquals(500, IconMorphLogic.SEND_ARC_MS)
        assertEquals(0f, IconMorphLogic.sendArcXDp(0f), 0.0001f)
        assertEquals(26f, IconMorphLogic.sendArcXDp(1f), 0.0001f)
        // The arc rises (negative y) and never dips below rest.
        var i = 0
        while (i <= 100) {
            assertTrue(IconMorphLogic.sendArcYDp(i / 100f) <= 0.001f)
            i++
        }
        assertEquals(0f, IconMorphLogic.sendArcYDp(0f), 0.0001f)
        // Departing plane is fully visible until its last 30%…
        assertEquals(1f, IconMorphLogic.sendDepartAlpha(0.5f), 0.0001f)
        assertEquals(0f, IconMorphLogic.sendDepartAlpha(1f), 0.0001f)
        // …and the fresh plane only springs in over that stretch.
        assertEquals(0f, IconMorphLogic.sendArriveScale(0.5f), 0.0001f)
        assertEquals(1f, IconMorphLogic.sendArriveScale(1f), 0.0001f)
    }

    @Test
    fun downloadStoryIs1200msPhasedArrowRingTick() {
        assertEquals(1200, IconMorphLogic.DOWNLOAD_STORY_MS)
        // Arrow owns the first quarter.
        assertEquals(0f, IconMorphLogic.downloadArrowFraction(0f), 0.0001f)
        assertEquals(0.5f, IconMorphLogic.downloadArrowFraction(0.125f), 0.0001f)
        assertEquals(1f, IconMorphLogic.downloadArrowFraction(0.25f), 0.0001f)
        // Ring owns the middle; it has not started during the arrow.
        assertEquals(0f, IconMorphLogic.downloadRingFraction(0.25f), 0.0001f)
        assertEquals(0.5f, IconMorphLogic.downloadRingFraction(0.525f), 0.0001f)
        assertEquals(1f, IconMorphLogic.downloadRingFraction(0.8f), 0.0001f)
        assertEquals(1f, IconMorphLogic.downloadRingFraction(1f), 0.0001f)
    }

    @Test
    fun unlockSpringOvershootsByThirtyFourPercentAndSettles() {
        assertEquals(560, IconMorphLogic.UNLOCK_SPRING_MS)
        assertEquals(1.34f, IconMorphLogic.UNLOCK_OVERSHOOT, 0.0001f)
        assertEquals(0f, IconMorphLogic.unlockLift(0f), 0.0001f)
        assertEquals(1f, IconMorphLogic.unlockLift(1f), 0.0001f)
        // Scan the curve: the peak is the spec'd 34% overshoot.
        var peak = 0f
        var i = 0
        while (i <= 1000) {
            peak = maxOf(peak, IconMorphLogic.unlockLift(i / 1000f))
            i++
        }
        assertEquals(1.34f, peak, 0.005f)
        // The swing is the lift scaled to its degrees.
        assertEquals(0f, IconMorphLogic.unlockSwingDegrees(0f), 0.0001f)
        assertEquals(
            IconMorphLogic.UNLOCK_SWING_DEGREES,
            IconMorphLogic.unlockSwingDegrees(1f),
            0.0001f,
        )
    }
}
