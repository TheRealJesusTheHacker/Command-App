package net.sentinelsafeguard.app.ui

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pure interaction logic for Polish Pass 2 (hunt16) — kept free of
 * Compose so the unit suite can pin every rule:
 *
 *  - Date handling: the app sends the backend's exact yyyy-MM-dd
 *    format, but the owner never types it. [DateFieldRow] shows a
 *    pretty date and opens the Material 3 date picker (already in
 *    the locked material3 1.3.1 artifact — NO new dependency and
 *    NO Gradle relock was needed for this pass). Conversions here
 *    are the single source of truth between the picker's UTC
 *    epoch millis and the wire's yyyy-MM-dd text.
 *  - Keyboard kinds inferred from a field's label, so every form
 *    row estate-wide gets the right keyboard (email / phone /
 *    number / decimal) without 267 call-site edits.
 *  - Exact money parsing: BigDecimal, never binary floating point,
 *    so typing dollars can never lose a cent to truncation.
 */
object InteractionLogic {

    // ---- Dates -------------------------------------------------------

    // STRICT + uuuu: the default SMART resolver silently turns
    // 2026-02-30 into 2026-02-28 — an impossible date must be
    // rejected, never repaired into a different day.
    private val YMD: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.US)
        .withResolverStyle(java.time.format.ResolverStyle.STRICT)
    private val PRETTY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)

    /** Strictly parse a wire date. Null for blank, malformed, or
     *  impossible dates (2026-02-30, 2026-13-01). */
    fun parseYmd(text: String?): LocalDate? {
        val t = text?.trim().orEmpty()
        if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(t)) return null
        return try {
            LocalDate.parse(t, YMD)
        } catch (e: Exception) {
            null
        }
    }

    fun isValidYmd(text: String?): Boolean = parseYmd(text) != null

    /** Wire format, exactly yyyy-MM-dd. */
    fun formatYmd(date: LocalDate): String = date.format(YMD)

    /** What the owner sees in a date field: "Tue, Nov 4, 2026".
     *  Blank stays blank; an unparseable stored value passes
     *  through untouched rather than being hidden. */
    fun prettyYmd(text: String?): String {
        val t = text?.trim().orEmpty()
        if (t.isBlank()) return ""
        return parseYmd(t)?.format(PRETTY) ?: t
    }

    /** Picker millis (UTC midnight) for a wire date, or null so the
     *  picker opens on today. */
    fun ymdToPickerMillis(text: String?): Long? =
        parseYmd(text)?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()

    fun pickerMillisFor(text: String?, today: LocalDate = LocalDate.now()): Long =
        ymdToPickerMillis(text)
            ?: today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    /** The picker's selection back to the wire's yyyy-MM-dd. */
    fun pickerMillisToYmd(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().format(YMD)

    // ---- Keyboard kinds ----------------------------------------------

    enum class FieldKind { TEXT, EMAIL, PHONE, NUMBER, DECIMAL }

    /**
     * The keyboard a labeled field should raise, from web's field
     * meaning as the label carries it. Money is DECIMAL (a user
     * typing dollars gets the decimal pad); counts and ids are
     * NUMBER; contact fields get their own pads. Anything else is
     * plain text.
     */
    fun fieldKindForLabel(label: String): FieldKind {
        val l = label.lowercase(Locale.US)
        return when {
            "email" in l || "e-mail" in l -> FieldKind.EMAIL
            "phone" in l || "mobile" in l -> FieldKind.PHONE
            "($)" in l || "($" in l || "unit $" in l || "amount" in l ||
                "rate" in l || "unit cost" in l || "price" in l ||
                "contract value" in l || "pipeline / contract value" in l ||
                "budget" in l || "salary" in l || "balance" in l ||
                "total" in l && "$" in l -> FieldKind.DECIMAL
            "qty" in l || "quantity" in l || "count" in l ||
                "days" in l || "hours" in l || "minutes" in l ||
                "year" in l || " id" in l || l.endsWith(" id") ||
                "number of" in l || "seats" in l || "age" in l ||
                "port" in l || "zip" in l || "extension" in l -> FieldKind.NUMBER
            else -> FieldKind.TEXT
        }
    }

    // ---- Money ---------------------------------------------------------

    /**
     * Dollars text ("500", "500.00", "$1,234.56", "19.99") into
     * integer cents, EXACTLY: BigDecimal arithmetic with half-up
     * rounding to the cent. (The old `(value * 100).toInt()` sites
     * truncated 19.99 to 1998 cents; the old Double round-trip in
     * Phase4Logic is replaced by this.) Null when the text is not
     * a non-negative number.
     */
    fun dollarsToCentsExact(text: String): Long? {
        val cleaned = text.trim().removePrefix("$").replace(",", "").trim()
        if (cleaned.isBlank()) return null
        val value = try {
            BigDecimal(cleaned)
        } catch (e: Exception) {
            return null
        }
        if (value.signum() < 0) return null
        return value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
    }

    // ---- Session / offline wording (plain language, standing) ---------

    /** Shown on the sign-in screen after a 401 ends the session:
     *  a clean return to sign-in WITH the explanation, never a
     *  silent ejection. */
    const val SESSION_ENDED_NOTICE: String =
        "Your sign-in has ended. Please sign in again."

    /** The offline / unreachable wording for sign-in legs. */
    const val NO_CONNECTION_MESSAGE: String =
        "No connection. Check your internet and try again."

    /** TOTP second-leg recovery wording. */
    const val TOTP_RETRY_MESSAGE: String =
        "That code did not match. Check the code in your authenticator app and try again, or start over."
}
