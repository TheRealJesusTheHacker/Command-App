package net.sentinelsafeguard.app

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.config.ApiConfig
import net.sentinelsafeguard.app.data.api.ApiException
import net.sentinelsafeguard.app.data.api.HttpResponse
import net.sentinelsafeguard.app.data.api.SentinelApi
import net.sentinelsafeguard.app.data.api.SignedOutException
import net.sentinelsafeguard.app.data.api.analytics
import net.sentinelsafeguard.app.data.api.ctosCase
import net.sentinelsafeguard.app.data.api.ctosCaseReport
import net.sentinelsafeguard.app.data.api.ctosClearCase
import net.sentinelsafeguard.app.data.api.ctosDeleteCase
import net.sentinelsafeguard.app.data.api.ctosExportCase
import net.sentinelsafeguard.app.data.api.ctosRecord
import net.sentinelsafeguard.app.data.api.ctosRunTool
import net.sentinelsafeguard.app.data.api.wysAudio
import net.sentinelsafeguard.app.ui.CtosLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * PERFECTION MIRROR pins (Command App 1.7.4) — the app side of
 * Live's Perfection Program relays R-1 / R-3 / R-5.
 *
 * R-1 (F-C1): CToS cases are creator-scoped server-side. The
 * scoped refusal is byte-identical to the unknown-id refusal —
 * 403 {"ok": false, "authorized": false, "message": "That CToS
 * case does not exist."} — and every case-scoped surface maps
 * exactly that (status + verbatim message) to the deleted-case
 * end state. The pins below hold the recognition exact: any
 * other 403 keeps its own handling, and a dead token (401 →
 * SignedOutException) can never be mistaken for it.
 *
 * R-3 (C-2): the app calls only the app twins for analytics
 * and briefing audio — the paths are pinned; the tree carries
 * no bare /actions request at all (grep evidence in the
 * PROGRAM-RECORD's PERFECTION MIRROR section).
 *
 * R-5 (W-2): the server's unpaged totals parse beside the
 * payloads, and the case-count line can never dress a partial
 * page as the whole set. The case list renders the payload
 * verbatim — scoping is the server's, never the client's.
 */
class PerfectionMirrorTest {

    private fun obj(json: String): JsonObject =
        Json.parseToJsonElement(json) as JsonObject

    private val config = ApiConfig(
        baseUrl = "https://command.example.test",
        cfAccessClientId = "test-client-id",
        cfAccessClientSecret = "test-client-secret",
    )

    private fun api(transport: FakeTransport): SentinelApi =
        SentinelApi(config, transport).also { it.deviceToken = "tok-test" }

    private val goneBody =
        """{"ok": false, "authorized": false,
           "message": "That CToS case does not exist."}"""

    // ---- R-1: the recognition predicate -----------------------------

    @Test
    fun goneRefusalIsExactly403AndTheServersSentence() {
        assertTrue(
            CtosLogic.isCaseGoneRefusal(403, "That CToS case does not exist."),
        )
        // Any other 403 is a different refusal with its own handling.
        assertFalse(CtosLogic.isCaseGoneRefusal(403, "Not authorized."))
        assertFalse(
            CtosLogic.isCaseGoneRefusal(
                403,
                "Only the owner can change a client's tier.",
            ),
        )
        // The unknown-id shape on another status is not this refusal.
        assertFalse(
            CtosLogic.isCaseGoneRefusal(404, "That CToS case does not exist."),
        )
        // Near-misses never match: the comparison is verbatim.
        assertFalse(
            CtosLogic.isCaseGoneRefusal(403, "That CToS case does not exist. "),
        )
        assertFalse(
            CtosLogic.isCaseGoneRefusal(403, "That CToS case does not exist"),
        )
        assertFalse(CtosLogic.isCaseGoneRefusal(403, null))
        assertFalse(CtosLogic.isCaseGoneRefusal(403, ""))
    }

    @Test
    fun goneRecognitionReadsOnlyTheApiException() {
        assertTrue(
            CtosLogic.isCaseGone(
                ApiException(403, "That CToS case does not exist."),
            ),
        )
        assertFalse(CtosLogic.isCaseGone(ApiException(403, "Not authorized.")))
        assertFalse(
            CtosLogic.isCaseGone(
                ApiException(404, "That CToS case does not exist."),
            ),
        )
        // A dead token is a sign-in event, never a gone case.
        assertFalse(CtosLogic.isCaseGone(SignedOutException()))
        assertFalse(CtosLogic.isCaseGone(IllegalStateException("boom")))
        assertFalse(CtosLogic.isCaseGone(null))
    }

    // ---- R-1: the mapping at the API layer --------------------------

    private suspend fun expectGone(block: suspend () -> Unit) {
        try {
            block()
            fail("expected the gone refusal to throw")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertEquals("That CToS case does not exist.", e.message)
            assertTrue(CtosLogic.isCaseGone(e))
        }
    }

    @Test
    fun everyCaseScopedTwinSurfacesTheGoneRefusalIntact() = runTest {
        val recordBody = buildJsonObject {
            put("kind", "finding")
            put("type", "other")
            put("value", "x")
        }
        val runBody = buildJsonObject {
            put("input", "example.com")
            put("caseId", 7)
        }
        val calls: List<Pair<String, suspend (SentinelApi) -> Unit>> = listOf(
            "GET /api/app/osint/ctos/cases/7" to { api -> api.ctosCase(7) },
            "GET /api/app/osint/ctos/cases/7/export" to { api ->
                api.ctosExportCase(7)
            },
            "GET /api/app/osint/ctos/cases/7/report" to { api ->
                api.ctosCaseReport(7)
            },
            "DELETE /api/app/osint/ctos/cases/7" to { api ->
                api.ctosDeleteCase(7)
            },
            "POST /api/app/osint/ctos/cases/7/clear" to { api ->
                api.ctosClearCase(7)
            },
            "POST /api/app/osint/ctos/cases/7/record" to { api ->
                api.ctosRecord(7, recordBody)
            },
            "POST /api/app/osint/ctos/tools/base64" to { api ->
                api.ctosRunTool("base64", runBody)
            },
        )
        for ((label, call) in calls) {
            val transport = FakeTransport { HttpResponse(403, goneBody) }
            expectGone { call(api(transport)) }
            val sent = transport.requests.single()
            assertEquals(label, "${sent.method} ${sent.path}")
            assertEquals("Bearer tok-test", sent.headers["Authorization"])
        }
    }

    @Test
    fun aDifferent403IsNotMistakenForGone() = runTest {
        val transport = FakeTransport {
            HttpResponse(
                403,
                """{"ok": false, "authorized": false,
                   "message": "Not authorized."}""",
            )
        }
        try {
            api(transport).ctosCase(7)
            fail("expected the refusal to throw")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertEquals("Not authorized.", e.message)
            assertFalse(CtosLogic.isCaseGone(e))
        }
    }

    @Test
    fun aDeadTokenIsASignInEventNeverAGoneCase() = runTest {
        val transport = FakeTransport {
            HttpResponse(401, """{"ok": false, "message": "expired"}""")
        }
        try {
            api(transport).ctosCase(7)
            fail("expected the sign-out to throw")
        } catch (e: SignedOutException) {
            assertFalse(CtosLogic.isCaseGone(e))
        }
    }

    // ---- R-1(a): the list renders the payload verbatim ---------------

    @Test
    fun caseListRendersThePayloadVerbatimInServerOrder() {
        val ws = obj(
            """{"ok": true, "casesTotal": 3, "cases": [
                 {"id": 9, "title": "b.example", "target": "b.example",
                  "findings": 1, "artifacts": 0, "updatedAt": 30},
                 {"id": 4, "title": "a.example", "target": "a.example",
                  "findings": 0, "artifacts": 2, "updatedAt": 20},
                 {"id": 12, "title": "c.example", "target": "c.example",
                  "findings": 5, "artifacts": 1, "updatedAt": 10}
               ]}""",
        )
        val rows = CtosLogic.parseCases(ws)
        // No client-side scoping, filtering, or re-sorting: the
        // server scoped the list; the app renders what arrived.
        assertEquals(listOf(9, 4, 12), rows.map { it.id })
        assertEquals(3, CtosLogic.casesTotal(ws))
    }

    // ---- R-5: totals --------------------------------------------------

    @Test
    fun serverTotalsParseAndAbsentReadsNull() {
        val detail = obj(
            """{"ok": true, "findingsTotal": 41, "artifactsTotal": 7,
               "findings": [], "artifacts": []}""",
        )
        assertEquals(41, CtosLogic.findingsTotal(detail))
        assertEquals(7, CtosLogic.artifactsTotal(detail))
        // Payloads predating W-2 carry no totals — null, never 0.
        val legacy = obj("""{"ok": true, "findings": [], "artifacts": []}""")
        assertNull(CtosLogic.findingsTotal(legacy))
        assertNull(CtosLogic.artifactsTotal(legacy))
        assertNull(CtosLogic.casesTotal(legacy))
        assertNull(CtosLogic.casesTotal(null))
        // A non-numeric total is not a total.
        assertNull(CtosLogic.casesTotal(obj("""{"casesTotal": "many"}""")))
    }

    @Test
    fun caseCountLineNeverDressesAPageAsTheWholeSet() {
        assertEquals("5 cases", CtosLogic.caseCountLine(5, 5))
        assertEquals("3 cases", CtosLogic.caseCountLine(null, 3))
        assertEquals("0 cases", CtosLogic.caseCountLine(0, 0))
        assertEquals("4 of 9 cases", CtosLogic.caseCountLine(9, 4))
    }

    // ---- R-3: the twins are the only analytics / audio paths ---------

    @Test
    fun analyticsRidesOnlyTheAppTwin() = runTest {
        val transport = FakeTransport { HttpResponse(200, """{"ok": true}""") }
        api(transport).analytics()
        val sent = transport.requests.single()
        assertEquals("GET", sent.method)
        assertEquals("/api/app/analytics", sent.path)
    }

    @Test
    fun wysAudioRidesOnlyTheAppTwin() = runTest {
        val transport = FakeTransport { HttpResponse(200, """{"ok": true}""") }
        api(transport).wysAudio()
        val sent = transport.requests.single()
        assertEquals("GET", sent.method)
        assertEquals("/api/app/wysaudio", sent.path)
    }
}
