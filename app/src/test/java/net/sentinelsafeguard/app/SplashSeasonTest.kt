package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.util.SplashKind
import net.sentinelsafeguard.app.util.SplashSeason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * hunt4 splash seasons: the app mirrors Command's splash windows
 * exactly — Halloween Night Watch Oct 30–Nov 1 (visitor-local,
 * yearly), Thanksgiving Wednesday–Friday of Thanksgiving week
 * (Thanksgiving = fourth Thursday of November, computed yearly).
 * Outside both windows the everyday WELCOME splash plays.
 */
class SplashSeasonTest {

    @Test
    fun halloweenWindowIsOct30ThroughNov1() {
        assertFalse(SplashSeason.isHalloweenWindow(LocalDate.of(2026, 10, 29)))
        assertTrue(SplashSeason.isHalloweenWindow(LocalDate.of(2026, 10, 30)))
        assertTrue(SplashSeason.isHalloweenWindow(LocalDate.of(2026, 10, 31)))
        assertTrue(SplashSeason.isHalloweenWindow(LocalDate.of(2026, 11, 1)))
        assertFalse(SplashSeason.isHalloweenWindow(LocalDate.of(2026, 11, 2)))
        assertEquals(
            SplashKind.HALLOWEEN,
            SplashSeason.kind(LocalDate.of(2025, 10, 31)),
        )
    }

    @Test
    fun thanksgivingThursdayIsTheFourthThursday() {
        assertEquals(
            LocalDate.of(2026, 11, 26),
            SplashSeason.thanksgivingThursday(2026),
        )
        assertEquals(
            LocalDate.of(2025, 11, 27),
            SplashSeason.thanksgivingThursday(2025),
        )
        assertEquals(
            LocalDate.of(2024, 11, 28),
            SplashSeason.thanksgivingThursday(2024),
        )
    }

    @Test
    fun thanksgivingWindowIsWednesdayThroughFriday() {
        // 2026: Thanksgiving Day is Thursday Nov 26.
        assertFalse(SplashSeason.isThanksgivingWindow(LocalDate.of(2026, 11, 24)))
        assertTrue(SplashSeason.isThanksgivingWindow(LocalDate.of(2026, 11, 25)))
        assertTrue(SplashSeason.isThanksgivingWindow(LocalDate.of(2026, 11, 26)))
        assertTrue(SplashSeason.isThanksgivingWindow(LocalDate.of(2026, 11, 27)))
        assertFalse(SplashSeason.isThanksgivingWindow(LocalDate.of(2026, 11, 28)))
        // 2025: Thanksgiving Day is Thursday Nov 27.
        assertTrue(SplashSeason.isThanksgivingWindow(LocalDate.of(2025, 11, 26)))
        assertTrue(SplashSeason.isThanksgivingWindow(LocalDate.of(2025, 11, 28)))
        assertFalse(SplashSeason.isThanksgivingWindow(LocalDate.of(2025, 11, 29)))
        assertEquals(
            SplashKind.THANKSGIVING,
            SplashSeason.kind(LocalDate.of(2026, 11, 26)),
        )
    }

    @Test
    fun ordinaryDaysGetTheWelcomeSplash() {
        assertEquals(
            SplashKind.WELCOME,
            SplashSeason.kind(LocalDate.of(2026, 11, 2)),
        )
        assertEquals(
            SplashKind.WELCOME,
            SplashSeason.kind(LocalDate.of(2026, 3, 15)),
        )
    }
}
