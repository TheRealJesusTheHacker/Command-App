package net.sentinelsafeguard.app

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import net.sentinelsafeguard.app.config.ApiConfig
import net.sentinelsafeguard.app.data.api.HttpResponse
import net.sentinelsafeguard.app.data.api.SentinelApi
import net.sentinelsafeguard.app.data.api.casePhotos
import net.sentinelsafeguard.app.data.api.clientFiles
import net.sentinelsafeguard.app.data.api.completeChecklistRun
import net.sentinelsafeguard.app.data.api.runPlans
import net.sentinelsafeguard.app.data.api.uploadPhoto
import net.sentinelsafeguard.app.ui.InteractionLogic
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * hunt17 (Polish Pass 3) pins:
 *  - the keyboard heuristic's word-based audit: every hunt16
 *    substring false positive found in the real label census is
 *    plain text now, the true positives still raise their pads,
 *    and search boxes never raise a pad they cannot type with;
 *  - GET coalescing: concurrent identical GETs share one
 *    transport call, sequential ones always fetch fresh, and
 *    mutations never coalesce;
 *  - the three contract-census additions (case photos, checklist
 *    complete-run, plans run) hit their production paths.
 */
class Polish3Test {

    private val config = ApiConfig(
        baseUrl = "https://command.example.test",
        cfAccessClientId = "",
        cfAccessClientSecret = "",
    )

    private fun kind(label: String) = InteractionLogic.fieldKindForLabel(label)

    @Test
    fun keyboardFalsePositivesAreTextNow() {
        // "count" inside Account/Countersign, "port" inside
        // reporting/Support, "age" inside stage/engagement —
        // all plain-text fields that raised the number pad.
        assertEquals(InteractionLogic.FieldKind.TEXT, kind("Account type"))
        assertEquals(InteractionLogic.FieldKind.TEXT, kind("Countersign — signer name"))
        assertEquals(InteractionLogic.FieldKind.TEXT, kind("Intel source URL (the reporting)"))
        assertEquals(InteractionLogic.FieldKind.TEXT, kind("Support expires (renewal watch)"))
        assertEquals(InteractionLogic.FieldKind.TEXT, kind("Name for the current stage filter"))
        assertEquals(InteractionLogic.FieldKind.TEXT, kind("Rules of engagement"))
        assertEquals(InteractionLogic.FieldKind.TEXT, kind("Cost note"))
    }

    @Test
    fun keyboardSearchBoxesAreAlwaysText() {
        assertEquals(InteractionLogic.FieldKind.TEXT, kind("Search name, title, email or phone"))
        assertEquals(InteractionLogic.FieldKind.TEXT, kind("Search the phone book"))
        assertEquals(InteractionLogic.FieldKind.TEXT, kind("Search tickets"))
    }

    @Test
    fun keyboardTruePositivesKept() {
        assertEquals(InteractionLogic.FieldKind.EMAIL, kind("Contact email"))
        assertEquals(InteractionLogic.FieldKind.EMAIL, kind("Recipient email"))
        assertEquals(InteractionLogic.FieldKind.PHONE, kind("Contact phone"))
        assertEquals(InteractionLogic.FieldKind.DECIMAL, kind("Amount ($)"))
        assertEquals(InteractionLogic.FieldKind.DECIMAL, kind("Rate ($/hour, optional)"))
        assertEquals(InteractionLogic.FieldKind.DECIMAL, kind("Tax rate (%)"))
        assertEquals(InteractionLogic.FieldKind.DECIMAL, kind("Unit cost ($)"))
        assertEquals(InteractionLogic.FieldKind.NUMBER, kind("Quantity"))
        assertEquals(InteractionLogic.FieldKind.NUMBER, kind("Qty on hand"))
        assertEquals(InteractionLogic.FieldKind.NUMBER, kind("Client ID"))
        assertEquals(InteractionLogic.FieldKind.NUMBER, kind("Assign to (user ID)"))
        assertEquals(InteractionLogic.FieldKind.NUMBER, kind("Duration (minutes)"))
        assertEquals(InteractionLogic.FieldKind.NUMBER, kind("Retention days"))
        assertEquals(InteractionLogic.FieldKind.NUMBER, kind("ZIP"))
    }

    @Test
    fun keyboardFlightNumberStaysText() {
        // The app's one "… number" field is alphanumeric
        // (airline code + digits) — no bare-"number" rule.
        assertEquals(InteractionLogic.FieldKind.TEXT, kind("Flight number"))
    }

    @Test
    fun keyboardNewClassifications() {
        // A threshold percentage is a decimal even with no money
        // word in the label; mileage is fractional; a daily cap
        // is a count.
        assertEquals(InteractionLogic.FieldKind.DECIMAL, kind("Threshold %"))
        assertEquals(InteractionLogic.FieldKind.DECIMAL, kind("Miles (mileage)"))
        assertEquals(InteractionLogic.FieldKind.NUMBER, kind("Daily cap"))
    }

    @Test
    fun concurrentIdenticalGetsShareOneTransportCall() = runBlocking {
        val transport = SlowTransport(60, """{"ok":true,"panels":{}}""")
        val api = SentinelApi(config, transport)
        val results = (1..6).map {
            async(kotlinx.coroutines.Dispatchers.Default) {
                api.getJson("/api/app/today/panels")
            }
        }.awaitAll()
        assertEquals(6, results.size)
        assertEquals(
            "six simultaneous identical GETs must ride one transport call",
            1, transport.count.get(),
        )
    }

    @Test
    fun sequentialIdenticalGetsAlwaysFetchFresh() = runBlocking {
        val transport = FakeTransport {
            HttpResponse(200, """{"ok":true,"panels":{}}""")
        }
        val api = SentinelApi(config, transport)
        api.getJson("/api/app/today/panels")
        api.getJson("/api/app/today/panels")
        assertEquals(
            "no staleness window: a later GET fetches again",
            2, transport.requests.size,
        )
    }

    @Test
    fun concurrentDifferentGetsDoNotCoalesce() = runBlocking {
        val transport = SlowTransport(40)
        val api = SentinelApi(config, transport)
        listOf(
            async(kotlinx.coroutines.Dispatchers.Default) { api.getJson("/api/app/today/panels") },
            async(kotlinx.coroutines.Dispatchers.Default) { api.getJson("/api/app/today/digests") },
        ).awaitAll()
        assertEquals(2, transport.count.get())
    }

    @Test
    fun mutationsNeverCoalesce() = runBlocking {
        val transport = SlowTransport(40)
        val api = SentinelApi(config, transport)
        listOf(
            async(kotlinx.coroutines.Dispatchers.Default) { api.runPlans() },
            async(kotlinx.coroutines.Dispatchers.Default) { api.runPlans() },
        ).awaitAll()
        assertEquals(2, transport.count.get())
    }

    @Test
    fun censusAdditionsHitProductionPaths() = runBlocking {
        val transport = FakeTransport { HttpResponse(200, """{"ok":true,"photos":[]}""") }
        val api = SentinelApi(config, transport)
        api.casePhotos(7)
        api.completeChecklistRun(9)
        api.runPlans()
        api.clientFiles(7)
        assertEquals(
            listOf(
                "GET /api/app/cases/7/photos",
                "POST /api/app/checklists/runs/9/complete",
                "POST /api/app/plans/run",
                "GET /api/app/clients/7/files",
            ),
            transport.requests.map { "${it.method} ${it.path}" },
        )
    }

    @Test
    fun casePhotoUploadCarriesCaseTarget() = runBlocking {
        val transport = FakeTransport { HttpResponse(200, """{"ok":true}""") }
        val api = SentinelApi(config, transport)
        api.uploadPhoto(7, byteArrayOf(1, 2, 3), "site.jpg", "Front", targetType = "case")
        val sent = transport.requests.single()
        assertEquals("/api/app/photos/upload", sent.path)
        assertEquals("case", sent.headers["X-Photo-Target-Type"])
        assertEquals("7", sent.headers["X-Photo-Target-Id"])
    }

}

/** Thread-safe counting transport for the concurrency pins (the
 *  shared FakeTransport's request list is a plain ArrayList —
 *  fine for sequential tests, racy under Dispatchers.Default). */
private class SlowTransport(
    private val sleepMs: Long,
    private val body: String = """{"ok":true}""",
) : net.sentinelsafeguard.app.data.api.HttpTransport {
    val count = java.util.concurrent.atomic.AtomicInteger(0)
    val paths = java.util.Collections.synchronizedList(mutableListOf<String>())

    override suspend fun execute(
        request: net.sentinelsafeguard.app.data.api.HttpRequest,
    ): net.sentinelsafeguard.app.data.api.HttpResponse {
        count.incrementAndGet()
        paths += request.path
        if (sleepMs > 0) Thread.sleep(sleepMs)
        return net.sentinelsafeguard.app.data.api.HttpResponse(200, body)
    }
}
