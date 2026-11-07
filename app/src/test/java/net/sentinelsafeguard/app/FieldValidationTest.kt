package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.ui.FieldValidators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Lane B (1.4.0) pins for the inline field validators,
 * including the forms' exact existing required messages.
 */
class FieldValidationTest {

    @Test
    fun requiredKeepsTheFormsExactWords() {
        assertEquals("Legal name is required.", FieldValidators.required("", "Legal name"))
        assertEquals("Title is required.", FieldValidators.required("  ", "Title"))
        assertEquals("A case title is required.", FieldValidators.required("", "A case title"))
        assertEquals("A job title is required.", FieldValidators.required("", "A job title"))
        assertEquals("Name is required.", FieldValidators.required("", "Name"))
        assertNull(FieldValidators.required("Acme", "Legal name"))
    }

    @Test
    fun emailShape() {
        assertNull(FieldValidators.emailIfPresent(""))
        assertNull(FieldValidators.emailIfPresent("owner@example.com"))
        assertNull(FieldValidators.emailIfPresent("a.b@sentinelsafeguard.net"))
        assertEquals(FieldValidators.EMAIL_MESSAGE, FieldValidators.emailIfPresent("not-an-email"))
        assertEquals(FieldValidators.EMAIL_MESSAGE, FieldValidators.emailIfPresent("a@b"))
        assertEquals(FieldValidators.EMAIL_MESSAGE, FieldValidators.emailIfPresent("a@@b.com"))
        assertEquals(FieldValidators.EMAIL_MESSAGE, FieldValidators.emailIfPresent("a b@c.com"))
        assertEquals(FieldValidators.EMAIL_MESSAGE, FieldValidators.emailIfPresent("@b.com"))
        assertEquals(FieldValidators.EMAIL_MESSAGE, FieldValidators.email(""))
        assertNull(FieldValidators.email("owner@example.com"))
    }

    @Test
    fun phoneShape() {
        assertNull(FieldValidators.phoneIfPresent(""))
        assertNull(FieldValidators.phoneIfPresent("(816) 555-1234"))
        assertNull(FieldValidators.phoneIfPresent("+1 816 555 1234"))
        assertEquals(FieldValidators.PHONE_MESSAGE, FieldValidators.phoneIfPresent("123"))
        assertEquals(FieldValidators.PHONE_MESSAGE, FieldValidators.phoneIfPresent("call me"))
        assertEquals(
            FieldValidators.PHONE_MESSAGE,
            FieldValidators.phoneIfPresent("1234567890123456"),
        )
    }

    @Test
    fun positiveMoney() {
        assertNull(FieldValidators.positiveMoneyIfPresent(""))
        assertNull(FieldValidators.positiveMoneyIfPresent("50"))
        assertNull(FieldValidators.positiveMoneyIfPresent("50.00"))
        assertNull(FieldValidators.positiveMoneyIfPresent("$1,250.00"))
        assertEquals(
            FieldValidators.POSITIVE_MONEY_MESSAGE,
            FieldValidators.positiveMoneyIfPresent("0"),
        )
        assertEquals(
            FieldValidators.POSITIVE_MONEY_MESSAGE,
            FieldValidators.positiveMoneyIfPresent("-5"),
        )
        assertEquals(
            FieldValidators.MONEY_MESSAGE,
            FieldValidators.positiveMoneyIfPresent("fifty"),
        )
        assertEquals(FieldValidators.POSITIVE_MONEY_MESSAGE, FieldValidators.positiveMoney(""))
        assertNull(FieldValidators.positiveMoney("75.00"))
    }

    @Test
    fun datesMustBeRealCalendarDates() {
        assertNull(FieldValidators.dateIfPresent(""))
        assertNull(FieldValidators.dateIfPresent("2026-11-07"))
        assertNull(FieldValidators.dateIfPresent("2028-02-29")) // leap year
        assertEquals(FieldValidators.DATE_MESSAGE, FieldValidators.dateIfPresent("2026-02-29"))
        assertEquals(FieldValidators.DATE_MESSAGE, FieldValidators.dateIfPresent("2026-13-01"))
        assertEquals(FieldValidators.DATE_MESSAGE, FieldValidators.dateIfPresent("2026-11-31"))
        assertEquals(FieldValidators.DATE_MESSAGE, FieldValidators.dateIfPresent("11/07/2026"))
        assertEquals(FieldValidators.DATE_MESSAGE, FieldValidators.dateIfPresent("2026-1-7"))
        assertEquals(FieldValidators.DATE_MESSAGE, FieldValidators.date(""))
        assertNull(FieldValidators.date("2026-12-12"))
    }

    @Test
    fun wholeNumberAndFirstError() {
        assertNull(FieldValidators.wholeNumberIfPresent(""))
        assertNull(FieldValidators.wholeNumberIfPresent("42"))
        assertEquals(
            FieldValidators.NUMBER_MESSAGE,
            FieldValidators.wholeNumberIfPresent("4.2"),
        )
        assertNull(FieldValidators.firstError(null, null))
        assertEquals("first", FieldValidators.firstError(null, "first", "second"))
    }
}
