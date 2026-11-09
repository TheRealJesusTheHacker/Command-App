package net.sentinelsafeguard.app

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import net.sentinelsafeguard.app.config.ApiConfig
import net.sentinelsafeguard.app.data.api.ApiException
import net.sentinelsafeguard.app.data.api.HttpResponse
import net.sentinelsafeguard.app.data.api.SentinelApi
import net.sentinelsafeguard.app.data.api.SignedOutException
import net.sentinelsafeguard.app.data.api.analytics
import net.sentinelsafeguard.app.ui.AnalyticsLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * 1.5.4 pins for the native website-analytics mirror
 * (GET /api/app/analytics).
 *
 * The AVAILABLE / UNAVAILABLE / NOT_CONFIGURED / FORBIDDEN /
 * UNAUTHORIZED fixtures below are BYTE-VERBATIM captures from
 * the 2026-11-09 rehearsal against the staging Command server
 * (analytics-rehearsal/goldens/ — a throwaway DB, a stub Umami
 * upstream with known numbers, the app's own endpoint called
 * exactly as the client calls it; production never contacted).
 * The demo + fetch-in-progress bodies are the server builder's
 * own shapes ({authorized:true, available:false, reason}) as
 * pinned by the server's test_app_analytics.py pin 4.
 *
 * Honesty rails pinned here:
 *  - numbers render ONLY from the payload — a headline metric
 *    missing or non-numeric is Malformed, never zero-filled;
 *  - the gate's 403 classifies to Unauthorized (the dashboard
 *    card's render-nothing state); 401 stays the standard
 *    SignedOutException path;
 *  - a genuinely empty period is detected as the empty
 *    state, distinct from a dashboard of zeros.
 */
class Analytics154Test {

    private fun body(text: String) = Json.parseToJsonElement(text).jsonObject

    // ---- the rehearsal-captured shapes ----

    @Test
    fun availableGoldenParsesEveryServerValue() {
        val state = AnalyticsLogic.parse(body(AVAILABLE))
        val stats = (state as? AnalyticsLogic.State.Available)?.stats
        assertTrue("expected Available, was $state", stats != null)
        stats!!
        assertEquals(1876L, stats.visitors)
        assertEquals(4321L, stats.pageviews)
        assertEquals(2102L, stats.visits)
        assertEquals(38.2, stats.bounceRate, 0.0)
        assertEquals(120L, stats.avgVisitSecs)
        assertEquals(7L, stats.activeVisitors)
        assertEquals(30, stats.periodDays)
        assertEquals(1794208110L, stats.asOfEpochSec)
        assertFalse(stats.stale)
        assertEquals(
            listOf("/", "/services", "/academy", "/contact", "/trust"),
            stats.topPages.map { it.label },
        )
        assertEquals(listOf(1500L, 900L, 640L, 310L, 205L), stats.topPages.map { it.count })
        assertEquals(3, stats.topReferrers.size)
        assertEquals("", stats.topReferrers[0].label) // Umami's direct-traffic row, as served
        assertEquals(1042L, stats.topReferrers[0].count)
        assertFalse(AnalyticsLogic.isEmptyPeriod(stats))
    }

    @Test
    fun staleCachedCopyIsFlaggedNotHidden() {
        val staleBody = AVAILABLE.replace(
            "\"available\": true,",
            "\"available\": true, \"cached\": true, \"stale\": true,",
        )
        val stats = (AnalyticsLogic.parse(body(staleBody)) as AnalyticsLogic.State.Available).stats
        assertTrue(stats.stale)
        assertEquals(1794208110L, stats.asOfEpochSec)
    }

    @Test
    fun unavailableGoldenCarriesTheServerReason() {
        val state = AnalyticsLogic.parse(body(UNAVAILABLE))
        assertEquals(AnalyticsLogic.State.Unavailable("unavailable"), state)
        assertEquals(
            "Analytics is unreachable right now.",
            AnalyticsLogic.reasonCopy("unavailable"),
        )
    }

    @Test
    fun notConfiguredGoldenCarriesTheServerReason() {
        val state = AnalyticsLogic.parse(body(NOT_CONFIGURED))
        assertEquals(AnalyticsLogic.State.Unavailable("not-configured"), state)
        assertEquals(
            "Analytics isn't configured on the server yet.",
            AnalyticsLogic.reasonCopy("not-configured"),
        )
    }

    @Test
    fun demoAndFetchInProgressReasonsGetTheirOwnCopy() {
        assertEquals(
            AnalyticsLogic.State.Unavailable("demo"),
            AnalyticsLogic.parse(body("""{"authorized": true, "available": false, "reason": "demo"}""")),
        )
        assertEquals(
            "Analytics isn't available in the demo.",
            AnalyticsLogic.reasonCopy("demo"),
        )
        assertEquals(
            AnalyticsLogic.State.Unavailable("fetch-in-progress"),
            AnalyticsLogic.parse(
                body("""{"authorized": true, "available": false, "reason": "fetch-in-progress"}"""),
            ),
        )
        assertEquals(
            "Analytics is being refreshed — try again in a moment.",
            AnalyticsLogic.reasonCopy("fetch-in-progress"),
        )
        // An unknown reason still renders one honest quiet line.
        assertEquals(
            "Analytics isn't available right now.",
            AnalyticsLogic.reasonCopy("something-new"),
        )
    }

    @Test
    fun underscoreReasonSpellingsNormalizeToTheSameCopy() {
        // The brief spelled the reasons with underscores; the
        // server ships hyphens. Both must land on the same copy.
        assertEquals("not-configured", AnalyticsLogic.normalizeReason("not_configured"))
        assertEquals(
            AnalyticsLogic.State.Unavailable("not-configured"),
            AnalyticsLogic.parse(
                body("""{"authorized": true, "available": false, "reason": "not_configured"}"""),
            ),
        )
    }

    @Test
    fun deniedShapeParsesUnauthorizedEvenOn200() {
        // The rehearsal's real 403 body, replayed as a body:
        // authorized:false is the gate's answer in any status.
        assertEquals(
            AnalyticsLogic.State.Unauthorized,
            AnalyticsLogic.parse(body(FORBIDDEN)),
        )
    }

    // ---- honesty rails ----

    @Test
    fun missingHeadlineMetricIsMalformedNeverInvented() {
        val noVisitors = AVAILABLE.replace("\"visitors\": 1876, ", "")
        assertEquals(AnalyticsLogic.State.Malformed, AnalyticsLogic.parse(body(noVisitors)))
        val noAvailable = """{"authorized": true, "visitors": 3}"""
        assertEquals(AnalyticsLogic.State.Malformed, AnalyticsLogic.parse(body(noAvailable)))
    }

    @Test
    fun emptyPeriodIsDetectedAsTheEmptyState() {
        val zeros = """
            {"authorized": true, "available": true, "asOf": 1794208110, "periodDays": 30,
             "visitors": 0, "pageviews": 0, "visits": 0, "bounceRate": 0.0,
             "avgVisitSecs": 0, "activeVisitors": 0, "topPages": [], "topReferrers": []}
        """.trimIndent()
        val stats = (AnalyticsLogic.parse(body(zeros)) as AnalyticsLogic.State.Available).stats
        assertTrue(AnalyticsLogic.isEmptyPeriod(stats))
        assertEquals(
            "No visits recorded in the last 30 days yet.",
            AnalyticsLogic.emptyPeriodCopy(stats),
        )
    }

    @Test
    fun topRowsAreCappedAtFiveAndCountlessRowsDropped() {
        val seven = """
            {"authorized": true, "available": true, "periodDays": 30,
             "visitors": 9, "pageviews": 9, "visits": 9, "bounceRate": 1.0,
             "avgVisitSecs": 3, "activeVisitors": 1,
             "topPages": [
               {"label": "/a", "count": 7}, {"label": "/b", "count": 6},
               {"label": "/c", "count": 5}, {"label": "/d", "count": 4},
               {"label": "/e", "count": 3}, {"label": "/f", "count": 2},
               {"label": "/g", "count": 1}],
             "topReferrers": [{"label": "x"}, {"label": "y", "count": 2}]}
        """.trimIndent()
        val stats = (AnalyticsLogic.parse(body(seven)) as AnalyticsLogic.State.Available).stats
        assertEquals(5, stats.topPages.size)
        assertEquals("/e", stats.topPages.last().label)
        // A row with no numeric count is dropped, never zeroed.
        assertEquals(listOf("y"), stats.topReferrers.map { it.label })
    }

    // ---- formatting mirrors web's widget ----

    @Test
    fun formattingMirrorsTheWebWidget() {
        assertEquals("1,876", AnalyticsLogic.formatCount(1876))
        assertEquals("4,321", AnalyticsLogic.formatCount(4321))
        assertEquals("0", AnalyticsLogic.formatCount(0))
        assertEquals("38.2%", AnalyticsLogic.formatBounce(38.2))
        assertEquals("40%", AnalyticsLogic.formatBounce(40.0))
        assertEquals("2:00", AnalyticsLogic.formatAvgVisit(120))
        assertEquals("0:45", AnalyticsLogic.formatAvgVisit(45))
        assertEquals("Last 30 days", AnalyticsLogic.periodLabel(30))
        assertEquals("(direct)", AnalyticsLogic.referrerLabel(""))
        assertEquals("(direct)", AnalyticsLogic.referrerLabel("  "))
        assertEquals("https://duckduckgo.com", AnalyticsLogic.referrerLabel("https://duckduckgo.com"))
        assertEquals("Active now (≤5 min old)", AnalyticsLogic.ACTIVE_LABEL)
    }

    // ---- the wire: endpoint, gate, session path ----

    private fun apiWith(status: Int, responseBody: String): SentinelApi {
        val transport = FakeTransport { HttpResponse(status, responseBody) }
        return SentinelApi(ApiConfig("https://command.example.test", "", ""), transport)
            .also { it.deviceToken = "device-token" }
    }

    @Test
    fun analyticsCallRidesTheAppEndpointWithTheDeviceToken() = runTest {
        val transport = FakeTransport { HttpResponse(200, AVAILABLE) }
        val api = SentinelApi(ApiConfig("https://command.example.test", "", ""), transport)
        api.deviceToken = "device-token"
        val parsed = AnalyticsLogic.parse(api.analytics())
        assertTrue(parsed is AnalyticsLogic.State.Available)
        val sent = transport.requests.single()
        assertEquals("GET", sent.method)
        assertEquals("/api/app/analytics", sent.path)
        assertEquals("Bearer device-token", sent.headers["Authorization"])
    }

    @Test
    fun gate403ClassifiesUnauthorized() = runTest {
        val api = apiWith(403, FORBIDDEN)
        var caught: ApiException? = null
        try {
            api.analytics()
        } catch (e: ApiException) {
            caught = e
        }
        assertTrue("expected ApiException", caught != null)
        val error = caught!!
        assertEquals(403, error.status)
        assertEquals(AnalyticsLogic.State.Unauthorized, AnalyticsLogic.stateForError(error))
        // A non-gate failure is NOT a state — the caller shows
        // its honest failure with a retry instead.
        assertEquals(null, AnalyticsLogic.stateForError(ApiException(500, "boom")))
    }

    @Test
    fun deadToken401StaysTheSessionExpiryPath() = runTest {
        val api = apiWith(401, UNAUTHORIZED)
        try {
            api.analytics()
            fail("expected SignedOutException")
        } catch (e: SignedOutException) {
            // The standard path: SentinelApi fired onSignedOut
            // and the shell returns to sign-in.
        }
    }

    private companion object {
        // Byte-verbatim from analytics-rehearsal/goldens/
        // (2026-11-09, staging server + stub Umami).
        const val AVAILABLE =
            """{"authorized": true, "available": true, "asOf": 1794208110, "periodDays": 30, "visitors": 1876, "pageviews": 4321, "visits": 2102, "bounceRate": 38.2, "avgVisitSecs": 120, "activeVisitors": 7, "topPages": [{"label": "/", "count": 1500}, {"label": "/services", "count": 900}, {"label": "/academy", "count": 640}, {"label": "/contact", "count": 310}, {"label": "/trust", "count": 205}], "topReferrers": [{"label": "", "count": 1042}, {"label": "https://www.google.com", "count": 800}, {"label": "https://duckduckgo.com", "count": 260}]}"""
        const val UNAVAILABLE =
            """{"authorized": true, "available": false, "reason": "unavailable"}"""
        const val NOT_CONFIGURED =
            """{"authorized": true, "available": false, "reason": "not-configured"}"""
        const val FORBIDDEN =
            """{"ok": false, "authorized": false, "message": "Not authorized."}"""
        const val UNAUTHORIZED =
            """{"ok": false, "authorized": false, "message": "Sign-in required."}"""
    }
}
