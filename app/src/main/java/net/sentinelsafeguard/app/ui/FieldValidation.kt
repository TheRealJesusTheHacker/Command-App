package net.sentinelsafeguard.app.ui

/**
 * Lane B (1.4.0) inline field validation: pure validators
 * for the create/edit forms, returning plain-language
 * messages or null when the value passes. No Android, no
 * Compose — the whole object is pinned by FieldValidationTest.
 *
 * Message discipline: the required-field message is built
 * from the caller's own field phrase, so the forms keep
 * their exact existing words ("Legal name is required.",
 * "Title is required.", "A case title is required.",
 * "A job title is required.", "Name is required."). The
 * other messages are new only where no message existed
 * before. Server errors are untouched — these validators
 * run client-side at the field, on submit and (after a
 * field's first error) on value change.
 */
object FieldValidators {
    const val EMAIL_MESSAGE: String = "Enter a valid email address."
    const val PHONE_MESSAGE: String = "Enter a valid phone number."
    const val POSITIVE_MONEY_MESSAGE: String = "Enter an amount greater than zero."
    const val MONEY_MESSAGE: String = "Enter the amount as dollars, for example 50.00."
    const val DATE_MESSAGE: String = "Enter a date as YYYY-MM-DD."
    const val NUMBER_MESSAGE: String = "Enter a whole number."

    /** "<phrase> is required." when [value] is blank. */
    fun required(value: String, fieldPhrase: String): String? =
        if (value.isBlank()) "$fieldPhrase is required." else null

    /** Email shape when a value is present; blank passes
     *  (pair with [required] when the field is mandatory). */
    fun emailIfPresent(value: String): String? {
        val v = value.trim()
        if (v.isEmpty()) return null
        return if (isEmailShape(v)) null else EMAIL_MESSAGE
    }

    /** Email shape, mandatory: blank is the email message
     *  too (a form that needs an email says so plainly). */
    fun email(value: String): String? {
        val v = value.trim()
        return if (v.isNotEmpty() && isEmailShape(v)) null else EMAIL_MESSAGE
    }

    private fun isEmailShape(v: String): Boolean {
        val at = v.indexOf('@')
        if (at <= 0 || at != v.lastIndexOf('@')) return false
        val domain = v.substring(at + 1)
        if (domain.length < 3 || domain.startsWith(".") || domain.endsWith(".")) return false
        if (v.any { it.isWhitespace() }) return false
        val dot = domain.lastIndexOf('.')
        return dot > 0 && dot < domain.length - 1
    }

    /** Phone-ish when present: digits (with the usual
     *  separators) numbering 7–15 once stripped. */
    fun phoneIfPresent(value: String): String? {
        val v = value.trim()
        if (v.isEmpty()) return null
        if (v.any { !(it.isDigit() || it in "+()-. ") }) return PHONE_MESSAGE
        val digits = v.count { it.isDigit() }
        return if (digits in 7..15) null else PHONE_MESSAGE
    }

    /** Positive money when present: parses as dollars and
     *  is greater than zero. Accepts "$1,250.00" shapes the
     *  owner actually types. */
    fun positiveMoneyIfPresent(value: String): String? {
        val v = value.trim()
        if (v.isEmpty()) return null
        val parsed = parseDollars(v) ?: return MONEY_MESSAGE
        return if (parsed > 0.0) null else POSITIVE_MONEY_MESSAGE
    }

    /** Positive money, mandatory. */
    fun positiveMoney(value: String): String? {
        val v = value.trim()
        if (v.isEmpty()) return POSITIVE_MONEY_MESSAGE
        val parsed = parseDollars(v) ?: return MONEY_MESSAGE
        return if (parsed > 0.0) null else POSITIVE_MONEY_MESSAGE
    }

    private fun parseDollars(v: String): Double? =
        v.removePrefix("$").replace(",", "").trim().toDoubleOrNull()

    /** A real calendar date in yyyy-MM-dd when present. */
    fun dateIfPresent(value: String): String? {
        val v = value.trim()
        if (v.isEmpty()) return null
        return if (isYmd(v)) null else DATE_MESSAGE
    }

    /** A real calendar date in yyyy-MM-dd, mandatory. */
    fun date(value: String): String? {
        val v = value.trim()
        return if (isYmd(v)) null else DATE_MESSAGE
    }

    private fun isYmd(v: String): Boolean {
        val parts = v.split("-")
        if (parts.size != 3) return false
        if (parts[0].length != 4 || parts[1].length != 2 || parts[2].length != 2) return false
        val year = parts[0].toIntOrNull() ?: return false
        val month = parts[1].toIntOrNull() ?: return false
        val day = parts[2].toIntOrNull() ?: return false
        if (month !in 1..12 || day < 1) return false
        val daysInMonth = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            else -> if (isLeap(year)) 29 else 28
        }
        return day <= daysInMonth
    }

    private fun isLeap(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    /** Whole number when present (optional ID fields). */
    fun wholeNumberIfPresent(value: String): String? {
        val v = value.trim()
        if (v.isEmpty()) return null
        return if (v.toIntOrNull() != null) null else NUMBER_MESSAGE
    }

    /** The first failing message in [errors], or null. */
    fun firstError(vararg errors: String?): String? = errors.firstOrNull { it != null }
}
