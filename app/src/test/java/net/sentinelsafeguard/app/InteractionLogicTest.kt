package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.ui.InteractionLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * hunt16 (Polish Pass 2) interaction pins: date round-trips
 * between the wire's yyyy-MM-dd and the Material date picker's
 * UTC millis, label-driven keyboard kinds, exact money parsing,
 * and the plain-language session/offline wording.
 */
class InteractionLogicTest {

    // ---- Dates ----

    @Test
    fun ymdRoundTripsThroughPickerMillis() {
        listOf("2026-11-04", "2026-01-01", "2024-02-29", "2030-12-31").forEach { ymd ->
            val millis = InteractionLogic.ymdToPickerMillis(ymd)!!
            assertEquals(ymd, InteractionLogic.pickerMillisToYmd(millis))
        }
    }

    @Test
    fun pickerMillisAreUtcMidnight() {
        // 2026-11-04T00:00:00Z
        assertEquals(1793750400000L, InteractionLogic.ymdToPickerMillis("2026-11-04"))
    }

    @Test
    fun invalidDatesAreRejected() {
        assertNull(InteractionLogic.parseYmd("2026-02-30"))
        assertNull(InteractionLogic.parseYmd("2026-13-01"))
        assertNull(InteractionLogic.parseYmd("2026-1-4"))
        assertNull(InteractionLogic.parseYmd("11/04/2026"))
        assertNull(InteractionLogic.parseYmd(""))
        assertNull(InteractionLogic.parseYmd(null))
        assertFalse(InteractionLogic.isValidYmd("not a date"))
        assertTrue(InteractionLogic.isValidYmd("2026-11-04"))
    }

    @Test
    fun prettyDatesRenderForTheOwner() {
        assertEquals("Wed, Nov 4, 2026", InteractionLogic.prettyYmd("2026-11-04"))
        assertEquals("", InteractionLogic.prettyYmd(""))
        assertEquals("", InteractionLogic.prettyYmd(null))
        // An unparseable stored value passes through visibly.
        assertEquals("legacy", InteractionLogic.prettyYmd("legacy"))
    }

    @Test
    fun formatYmdIsTheWireFormat() {
        assertEquals("2026-11-04", InteractionLogic.formatYmd(LocalDate.of(2026, 11, 4)))
        assertEquals("2026-01-05", InteractionLogic.formatYmd(LocalDate.of(2026, 1, 5)))
    }

    @Test
    fun pickerFallsBackToTodayForBlankOrBadValues() {
        val today = LocalDate.of(2026, 11, 4)
        assertEquals(
            InteractionLogic.ymdToPickerMillis("2026-11-04"),
            InteractionLogic.pickerMillisFor("", today),
        )
        assertEquals(
            InteractionLogic.ymdToPickerMillis("2026-11-04"),
            InteractionLogic.pickerMillisFor("garbage", today),
        )
    }

    // ---- Keyboard kinds ----

    @Test
    fun keyboardKindsFollowTheLabel() {
        assertEquals(
            InteractionLogic.FieldKind.EMAIL,
            InteractionLogic.fieldKindForLabel("Email"),
        )
        assertEquals(
            InteractionLogic.FieldKind.PHONE,
            InteractionLogic.fieldKindForLabel("Phone"),
        )
        assertEquals(
            InteractionLogic.FieldKind.DECIMAL,
            InteractionLogic.fieldKindForLabel("Amount ($)"),
        )
        assertEquals(
            InteractionLogic.FieldKind.DECIMAL,
            InteractionLogic.fieldKindForLabel("Rate ($/hour, optional)"),
        )
        assertEquals(
            InteractionLogic.FieldKind.DECIMAL,
            InteractionLogic.fieldKindForLabel("Unit cost ($)"),
        )
        assertEquals(
            InteractionLogic.FieldKind.NUMBER,
            InteractionLogic.fieldKindForLabel("Qty on hand"),
        )
        assertEquals(
            InteractionLogic.FieldKind.NUMBER,
            InteractionLogic.fieldKindForLabel("Expires in days"),
        )
        assertEquals(
            InteractionLogic.FieldKind.TEXT,
            InteractionLogic.fieldKindForLabel("Title"),
        )
        assertEquals(
            InteractionLogic.FieldKind.TEXT,
            InteractionLogic.fieldKindForLabel("Notes"),
        )
    }

    // ---- Money: exact cents ----

    @Test
    fun dollarsParseToExactCents() {
        assertEquals(1999L, InteractionLogic.dollarsToCentsExact("19.99"))
        assertEquals(7L, InteractionLogic.dollarsToCentsExact("0.07"))
        assertEquals(50000L, InteractionLogic.dollarsToCentsExact("500"))
        assertEquals(50000L, InteractionLogic.dollarsToCentsExact("500.00"))
        assertEquals(123456L, InteractionLogic.dollarsToCentsExact("$1,234.56"))
        assertEquals(100L, InteractionLogic.dollarsToCentsExact("1"))
        // Half-up to the cent, exactly — never a truncation.
        assertEquals(101L, InteractionLogic.dollarsToCentsExact("1.005"))
        assertEquals(268L, InteractionLogic.dollarsToCentsExact("2.675"))
    }

    @Test
    fun dollarsRejectNonNumbersAndNegatives() {
        assertNull(InteractionLogic.dollarsToCentsExact(""))
        assertNull(InteractionLogic.dollarsToCentsExact("abc"))
        assertNull(InteractionLogic.dollarsToCentsExact("-5"))
        assertNull(InteractionLogic.dollarsToCentsExact("$"))
    }

    @Test
    fun phase4DollarsToCentsUsesTheExactParser() {
        // The form path (Phase4Logic) must agree cent-for-cent.
        assertEquals(1999L, net.sentinelsafeguard.app.ui.Phase4Logic.dollarsToCents("19.99"))
        assertEquals(123456L, net.sentinelsafeguard.app.ui.Phase4Logic.dollarsToCents("$1,234.56"))
    }

    // ---- Plain-language wording ----

    @Test
    fun sessionAndOfflineWordingIsPlain() {
        assertEquals(
            "Your sign-in has ended. Please sign in again.",
            InteractionLogic.SESSION_ENDED_NOTICE,
        )
        assertTrue(InteractionLogic.NO_CONNECTION_MESSAGE.startsWith("No connection."))
        assertTrue(InteractionLogic.TOTP_RETRY_MESSAGE.contains("start over"))
        // No technical vocabulary in any owner-facing constant.
        listOf(
            InteractionLogic.SESSION_ENDED_NOTICE,
            InteractionLogic.NO_CONNECTION_MESSAGE,
            InteractionLogic.TOTP_RETRY_MESSAGE,
        ).forEach { text ->
            assertFalse(text.contains("401"))
            assertFalse(text.contains("token", ignoreCase = true))
            assertFalse(text.contains("HTTP"))
        }
    }
}
