package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.notify.NotifyCategory
import net.sentinelsafeguard.app.notify.QuietHours
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class QuietHoursTest {

    // Default window from the spec: 11:00 → 18:00 (the owner's sleep).
    private val start = 11 * 60
    private val end = 18 * 60

    @Test
    fun ticketsAndMailAreSilentDuringQuietHours() {
        val afternoon = LocalTime.of(14, 30)
        assertFalse(
            QuietHours.shouldNotify(NotifyCategory.TICKETS, afternoon, start, end, true)
        )
        assertFalse(
            QuietHours.shouldNotify(NotifyCategory.MAIL, afternoon, start, end, true)
        )
    }

    @Test
    fun signInAlertsAreExemptFromQuietHours() {
        val afternoon = LocalTime.of(14, 30)
        assertTrue(
            QuietHours.shouldNotify(NotifyCategory.SIGN_IN_ALERT, afternoon, start, end, true)
        )
    }

    @Test
    fun outsideQuietHoursEverythingEnabledNotifies() {
        val evening = LocalTime.of(21, 0)
        assertTrue(QuietHours.shouldNotify(NotifyCategory.TICKETS, evening, start, end, true))
        assertTrue(QuietHours.shouldNotify(NotifyCategory.MAIL, evening, start, end, true))
    }

    @Test
    fun disabledCategoryNeverNotifies() {
        val evening = LocalTime.of(21, 0)
        assertFalse(QuietHours.shouldNotify(NotifyCategory.TICKETS, evening, start, end, false))
        assertFalse(QuietHours.shouldNotify(NotifyCategory.SIGN_IN_ALERT, evening, start, end, false))
    }

    @Test
    fun windowBoundaries() {
        assertTrue(QuietHours.isQuiet(11 * 60, start, end))
        assertFalse(QuietHours.isQuiet(18 * 60, start, end))
        assertFalse(QuietHours.isQuiet(10 * 60 + 59, start, end))
    }

    @Test
    fun wrappingWindowWorks() {
        val wrapStart = 23 * 60
        val wrapEnd = 7 * 60
        assertTrue(QuietHours.isQuiet(23 * 60 + 30, wrapStart, wrapEnd))
        assertTrue(QuietHours.isQuiet(2 * 60, wrapStart, wrapEnd))
        assertFalse(QuietHours.isQuiet(12 * 60, wrapStart, wrapEnd))
    }

    @Test
    fun timeParsingAndFormatting() {
        assertEquals(11 * 60, QuietHours.parseTime("11:00"))
        assertEquals(18 * 60 + 30, QuietHours.parseTime("18:30"))
        assertNull(QuietHours.parseTime("25:00"))
        assertNull(QuietHours.parseTime("noon"))
        assertEquals("11:00", QuietHours.toHhMm(11 * 60))
        assertEquals("11:00 AM", QuietHours.formatMinutes(11 * 60))
        assertEquals("6:00 PM", QuietHours.formatMinutes(18 * 60))
    }
}
