package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.ui.IconMorphLogic
import net.sentinelsafeguard.app.ui.VoicePlayerLogic
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Lane C (1.4.0 item 10) pins: the M-18 play/pause morph
 * numbers and the voice player's pure logic (poll cadence,
 * progress fraction, clock text, temp-file extension).
 */
class PlayPauseVoiceLogicTest {

    @Test
    fun playPauseMorphIs300msCollapseAndGrow() {
        assertEquals(300, IconMorphLogic.PLAY_PAUSE_MORPH_MS)
        // At rest (play): the triangle is whole, the bars absent.
        assertEquals(1f, IconMorphLogic.playCollapseFraction(0f), 0.0001f)
        assertEquals(0f, IconMorphLogic.pauseBarFraction(0f), 0.0001f)
        // Mid-morph: the triangle is half-collapsed into the
        // left bar while the bars are half-grown.
        assertEquals(0.5f, IconMorphLogic.playCollapseFraction(0.5f), 0.0001f)
        assertEquals(0.5f, IconMorphLogic.pauseBarFraction(0.5f), 0.0001f)
        // At rest (pause): bars only.
        assertEquals(0f, IconMorphLogic.playCollapseFraction(1f), 0.0001f)
        assertEquals(1f, IconMorphLogic.pauseBarFraction(1f), 0.0001f)
        // Out-of-range clamps.
        assertEquals(1f, IconMorphLogic.playCollapseFraction(-1f), 0.0001f)
        assertEquals(1f, IconMorphLogic.pauseBarFraction(2f), 0.0001f)
    }

    @Test
    fun voicePlayerPollsAt250msAndFractionsClamp() {
        assertEquals(250L, VoicePlayerLogic.POLL_INTERVAL_MS)
        assertEquals(0f, VoicePlayerLogic.progressFraction(0, 1000), 0.0001f)
        assertEquals(0.5f, VoicePlayerLogic.progressFraction(500, 1000), 0.0001f)
        assertEquals(1f, VoicePlayerLogic.progressFraction(1000, 1000), 0.0001f)
        // Past-the-end positions clamp; an unknown duration is 0.
        assertEquals(1f, VoicePlayerLogic.progressFraction(2000, 1000), 0.0001f)
        assertEquals(0f, VoicePlayerLogic.progressFraction(500, 0), 0.0001f)
        assertEquals(0f, VoicePlayerLogic.progressFraction(500, -10), 0.0001f)
    }

    @Test
    fun voicePlayerClockTextIsMinuteSecond() {
        assertEquals("0:00", VoicePlayerLogic.clockText(0))
        assertEquals("0:07", VoicePlayerLogic.clockText(7_400))
        assertEquals("1:05", VoicePlayerLogic.clockText(65_000))
        assertEquals("10:00", VoicePlayerLogic.clockText(600_000))
        assertEquals("0:00", VoicePlayerLogic.clockText(-500))
    }

    @Test
    fun voicePlayerExtensionIsSanitized() {
        assertEquals("mp3", VoicePlayerLogic.extensionFor("note.mp3"))
        assertEquals("wav", VoicePlayerLogic.extensionFor("NOTE.WAV"))
        assertEquals("m4a", VoicePlayerLogic.extensionFor("voice note.m4a"))
        // No extension — or nothing usable after the dot — is wav.
        assertEquals("wav", VoicePlayerLogic.extensionFor("note"))
        assertEquals("wav", VoicePlayerLogic.extensionFor("note."))
        assertEquals("bc", VoicePlayerLogic.extensionFor("a.b@c!"))
    }
}
