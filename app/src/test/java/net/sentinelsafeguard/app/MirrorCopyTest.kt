package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.ui.MirrorCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Polish Pass 1 (hunt15) pins: the exact view titles and header
 * subtitles production Command renders (re-read from the staging
 * static tree for this pass). If web copy changes, these fail
 * loudly instead of the app silently drifting again.
 */
class MirrorCopyTest {

    @Test
    fun viewTitlesWhereWebH1DiffersFromNavLabel() {
        assertEquals("Operations Views", MirrorCopy.title("b3cops", "Ops Views"))
        assertEquals("Pipeline & Field Jobs", MirrorCopy.title("b3dfield", "Pipeline & Field"))
        assertEquals("Client CSV Import", MirrorCopy.title("b4dimport", "Client Import"))
        assertEquals("Firm Operations", MirrorCopy.title("cf2firmops", "Firm Ops"))
        assertEquals("OSINT Toolkit", MirrorCopy.title("osint", "OSINT"))
        // Keys without an override keep their nav label.
        assertEquals("Reports & Feedback", MirrorCopy.title("cf2reports", "Reports & Feedback"))
        assertEquals("Packets & Routes", MirrorCopy.title("b4jops", "Packets & Routes"))
    }

    @Test
    fun coreViewSubtitlesAreWebsExactStrings() {
        assertEquals(
            "Every engagement as a working case — board, directory, and full case files.",
            MirrorCopy.subtitle("cases"),
        )
        assertEquals(
            "Two registers, kept separate: Sentinel's own equipment first, then the gear tracked for clients.",
            MirrorCopy.subtitle("assets"),
        )
        assertEquals(
            "Real billing documents — line items, tax, a true lifecycle, and a printable invoice for every client.",
            MirrorCopy.subtitle("invoices"),
        )
        assertEquals(
            "The team directory — who is here, what each person is carrying, and the work still waiting for an owner.",
            MirrorCopy.subtitle("employees"),
        )
        assertEquals(
            "Every job and every person on one calendar — pick a person to see only their jobs.",
            MirrorCopy.subtitle("schedule"),
        )
        assertEquals("Only the jobs assigned to you appear here.", MirrorCopy.subtitle("myschedule"))
        assertEquals(
            "Grouped by module. A module your role cannot open never appears — not even as a count.",
            MirrorCopy.subtitle("search"),
        )
    }

    @Test
    fun moneyDeskSubtitleKeepsTheDraftsOnlySentence() {
        assertTrue(MirrorCopy.subtitle("b3money")!!.contains("Automations create DRAFTS only — nothing is sent or ordered."))
        assertTrue(MirrorCopy.subtitle("b4etax")!!.endsWith("America/Chicago."))
    }

    @Test
    fun portalSubtitlesAreWebsExactStrings() {
        assertEquals(
            "Your Sentinel Safeguard client portal — your profile, and the work we hold for you.",
            MirrorCopy.subtitle("portal_profile"),
        )
        assertEquals(
            "For an emergency happening right now — a Critical Alert files a ticket at Critical priority, the highest in our system.",
            MirrorCopy.subtitle("portal_alert"),
        )
        assertEquals(
            "Contracts sent to you for signature, and your published engagement reports.",
            MirrorCopy.subtitle("portal_contracts"),
        )
        // Views web renders with no subtitle stay subtitle-free.
        assertNull(MirrorCopy.subtitle("portal_signoffs"))
        assertNull(MirrorCopy.subtitle("portal_status"))
        assertNull(MirrorCopy.subtitle("dashboard"))
        assertNull(MirrorCopy.subtitle("admin"))
        assertNull(MirrorCopy.subtitle("archive"))
    }

    @Test
    fun everySubtitleKeyResolvesForItsScreen() {
        // The injected screens look subtitles up by their nav key;
        // every full view among them carries web's subtitle.
        for (key in listOf("b3cops", "b3dfield", "cf2reports", "b4cadmin", "b4dimport", "b4jops")) {
            assertTrue("missing subtitle for $key", MirrorCopy.subtitle(key) != null)
        }
        // Overlay sheets mirrored as screens have no web page subtitle.
        for (key in listOf("parts", "collections", "staffgear", "automation", "clientops", "pipelinepackets")) {
            assertNull("overlay $key should have no subtitle", MirrorCopy.subtitle(key))
        }
    }

    @Test
    fun todaySubtitleMatchesWebsDateLine() {
        val tuesday = ZonedDateTime.of(2026, 11, 3, 12, 0, 0, 0, ZoneId.of("America/Chicago"))
        assertEquals("Tuesday, November 3, 2026 — what needs you first.", MirrorCopy.todaySubtitle(tuesday))
    }
}
