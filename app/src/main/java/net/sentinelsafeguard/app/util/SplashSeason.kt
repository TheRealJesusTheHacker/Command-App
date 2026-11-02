package net.sentinelsafeguard.app.util

import java.time.DayOfWeek
import java.time.LocalDate

/** Which of Command's entry splashes the season calls for. */
enum class SplashKind { WELCOME, HALLOWEEN, THANKSGIVING }

/**
 * The seasonal windows of Sentinel Command's entry splash,
 * mirrored from the web build (static/halloween.js and
 * static/thanksgiving.js in the Command shell) so the app shows
 * what Command shows, on the same dates. Both windows are computed
 * from the device's local date, every year, with no network.
 *
 * - Halloween "Night Watch": October 30 – November 1.
 * - Thanksgiving: Wednesday–Friday of Thanksgiving week, where
 *   Thanksgiving is the fourth Thursday of November (US). The
 *   earliest possible Thanksgiving Wednesday is November 21, so
 *   the two windows can never collide; Thanksgiving is still
 *   checked first, exactly as the web dispatch wraps Halloween's.
 */
object SplashSeason {

    fun kind(date: LocalDate): SplashKind = when {
        isThanksgivingWindow(date) -> SplashKind.THANKSGIVING
        isHalloweenWindow(date) -> SplashKind.HALLOWEEN
        else -> SplashKind.WELCOME
    }

    fun isHalloweenWindow(date: LocalDate): Boolean {
        val month = date.monthValue
        val day = date.dayOfMonth
        return (month == 10 && day >= 30) || (month == 11 && day == 1)
    }

    /** The fourth Thursday of November in [year]. */
    fun thanksgivingThursday(year: Int): LocalDate {
        var day = LocalDate.of(year, 11, 1)
        while (day.dayOfWeek != DayOfWeek.THURSDAY) day = day.plusDays(1)
        return day.plusWeeks(3)
    }

    fun isThanksgivingWindow(date: LocalDate): Boolean {
        val thursday = thanksgivingThursday(date.year)
        return !date.isBefore(thursday.minusDays(1)) && !date.isAfter(thursday.plusDays(1))
    }
}
