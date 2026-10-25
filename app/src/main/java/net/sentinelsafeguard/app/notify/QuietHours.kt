package net.sentinelsafeguard.app.notify

import java.time.LocalTime

enum class NotifyCategory { TICKETS, MAIL, SIGN_IN_ALERT }

/**
 * Quiet-hours rule (spec §6): during the sleep window, ticket and mail
 * notifications stay silent. Sign-in alerts are exempt by default —
 * a sign-in alert during sleep is exactly the one worth waking for.
 * Pure logic, unit-tested.
 */
object QuietHours {

    /** Minutes since midnight → true when [minuteOfDay] is inside the window. */
    fun isQuiet(minuteOfDay: Int, startMinutes: Int, endMinutes: Int): Boolean {
        if (startMinutes == endMinutes) return false
        return if (startMinutes < endMinutes) {
            minuteOfDay in startMinutes until endMinutes
        } else {
            // Window wraps past midnight (e.g. 23:00 → 07:00).
            minuteOfDay >= startMinutes || minuteOfDay < endMinutes
        }
    }

    fun shouldNotify(
        category: NotifyCategory,
        now: LocalTime,
        startMinutes: Int,
        endMinutes: Int,
        categoryEnabled: Boolean,
    ): Boolean {
        if (!categoryEnabled) return false
        if (category == NotifyCategory.SIGN_IN_ALERT) return true
        val minute = now.hour * 60 + now.minute
        return !isQuiet(minute, startMinutes, endMinutes)
    }

    fun formatMinutes(minutes: Int): String {
        val h = (minutes / 60) % 24
        val m = minutes % 60
        val suffix = if (h < 12) "AM" else "PM"
        val hour12 = when (val r = h % 12) { 0 -> 12; else -> r }
        return "%d:%02d %s".format(hour12, m, suffix)
    }

    /** Parses "11:00" / "18:30" into minutes since midnight, or null. */
    fun parseTime(text: String): Int? {
        val parts = text.trim().split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    fun toHhMm(minutes: Int): String = "%02d:%02d".format((minutes / 60) % 24, minutes % 60)
}
