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
import net.sentinelsafeguard.app.data.api.ctosRunToolAsync
import net.sentinelsafeguard.app.data.api.ctosToolRun
import net.sentinelsafeguard.app.ui.CtosLogic
import net.sentinelsafeguard.app.ui.PulseLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.math.abs

/**
 * RELEASE 1.7.5 pins — Command Pulse + R-6 async CToS runs.
 *
 * PULSE (Live's COMMAND-PULSE-2026-11-13 relay, binding): the
 * panels payload's top-level `pulse` is server-computed by the
 * single helper _queue_pulse; the app renders the bpm VERBATIM
 * and builds its caption from the same counts. The pins hold:
 * the relay sample and the empty-queue shape parse exactly;
 * an absent or partial pulse is NO pulse (never filled in);
 * the mode decision (live / flat-under-reduced-motion /
 * unavailable) is total; the waveform is steady, bounded, and
 * periodic — tempo is the only signal it carries.
 *
 * R-6 (Perfection relay): the async twins' paths and shapes —
 * POST /api/app/osint/ctos/tools/<tool>/async (202 start),
 * GET /api/app/osint/ctos/runs/<runId> (poll) — the start/poll
 * payload parses, the done poll's shape feeding parseToolResult
 * unchanged, the failed run's error string verbatim, and the
 * run-gone refusal recognized exactly (403 + the server's
 * sentence), never confused with the case-gone refusal or a
 * dead token.
 */
class Release175Test {

    private fun obj(json: String): JsonObject =
        Json.parseToJsonElement(json) as JsonObject

    private val config = ApiConfig(
        baseUrl = "https://command.example.test",
        cfAccessClientId = "test-client-id",
        cfAccessClientSecret = "test-client-secret",
    )

    private fun api(transport: FakeTransport): SentinelApi =
        SentinelApi(config, transport).also { it.deviceToken = "tok-test" }

    // ---- Pulse: the relay shapes ------------------------------------

    @Test
    fun relaySampleParsesVerbatimWithCaptionAndReadout() {
        // The Live relay sample (battery seed S2: 2 Normal +
        // 1 Low + 2 High + 1 Critical, all open), riding the
        // panels response's top level beside `panels`.
        val response = obj(
            """{"ok": true, "panels": {},
               "pulse": {"open": 6, "normal": 3, "high": 2,
                          "urgent": 1, "pressure": 10, "bpm": 68}}""",
        )
        val pulse = PulseLogic.parse(response)
        assertEquals(6, pulse?.open)
        assertEquals(3, pulse?.normal)
        assertEquals(2, pulse?.high)
        assertEquals(1, pulse?.urgent)
        assertEquals(10, pulse?.pressure)
        // The bpm is the server's number, carried — not derived.
        assertEquals(68, pulse?.bpm)
        assertEquals("Queue pulse · 6 open · 1 urgent", PulseLogic.caption(pulse!!))
        assertEquals("68 bpm · pressure 10", PulseLogic.readout(pulse))
    }

    @Test
    fun emptyQueueShapeParsesAtTheRestingBpm() {
        val response = obj(
            """{"ok": true, "panels": {},
               "pulse": {"open": 0, "normal": 0, "high": 0,
                          "urgent": 0, "pressure": 0, "bpm": 48}}""",
        )
        val pulse = PulseLogic.parse(response)
        assertEquals(48, pulse?.bpm)
        assertEquals(0, pulse?.pressure)
        assertEquals("Queue pulse · 0 open · 0 urgent", PulseLogic.caption(pulse!!))
        assertEquals("48 bpm · pressure 0", PulseLogic.readout(pulse))
    }

    @Test
    fun absentOrPartialPulseIsNoPulse() {
        // An older server's panels response carries no pulse.
        assertNull(PulseLogic.parse(obj("""{"ok": true, "panels": {}}""")))
        assertNull(PulseLogic.parse(null))
        // A partial object is not a pulse — the app never fills
        // a missing count in.
        assertNull(
            PulseLogic.parse(
                obj(
                    """{"pulse": {"open": 6, "normal": 3, "high": 2,
                                  "urgent": 1, "pressure": 10}}""",
                ),
            ),
        )
        // A non-integer field is not a count.
        assertNull(
            PulseLogic.parse(
                obj(
                    """{"pulse": {"open": "6", "normal": 3, "high": 2,
                                  "urgent": 1, "pressure": 10, "bpm": 68}}""",
                ),
            ),
        )
    }

    @Test
    fun pulseModeDecisionIsTotal() {
        val pulse = PulseLogic.parse(
            obj(
                """{"pulse": {"open": 1, "normal": 1, "high": 0,
                              "urgent": 0, "pressure": 1, "bpm": 50}}""",
            ),
        )
        assertEquals(PulseLogic.Mode.LIVE, PulseLogic.mode(pulse, reducedMotion = false))
        // Reduced motion: flat line + numeric readout.
        assertEquals(PulseLogic.Mode.FLAT, PulseLogic.mode(pulse, reducedMotion = true))
        // No counts: unavailable, whatever the motion setting.
        assertEquals(PulseLogic.Mode.UNAVAILABLE, PulseLogic.mode(null, reducedMotion = false))
        assertEquals(PulseLogic.Mode.UNAVAILABLE, PulseLogic.mode(null, reducedMotion = true))
    }

    @Test
    fun beatPeriodFollowsTheServerBpm() {
        assertEquals(1250.0, PulseLogic.beatPeriodMs(48), 0.001)
        assertEquals(882.353, PulseLogic.beatPeriodMs(68), 0.001)
        assertEquals(461.538, PulseLogic.beatPeriodMs(130), 0.001)
    }

    @Test
    fun waveformIsSteadyBoundedAndPeriodic() {
        // Baseline-anchored: the beat starts and rests at ~0.
        assertTrue(abs(PulseLogic.waveAmplitude(0.0)) < 0.01)
        assertTrue(abs(PulseLogic.waveAmplitude(0.95)) < 0.05)
        // The R spike is the peak of the beat.
        assertTrue(PulseLogic.waveAmplitude(0.320) > 0.9)
        var max = 0.0
        var i = 0
        while (i <= 400) {
            val a = PulseLogic.waveAmplitude(i / 400.0)
            assertTrue("amplitude bounded", abs(a) <= 1.05)
            if (a > max) max = a
            i++
        }
        assertTrue(max <= PulseLogic.waveAmplitude(0.320) + 0.02)
        // The S dip goes below baseline; the T bump rises above it.
        assertTrue(PulseLogic.waveAmplitude(0.355) < 0.0)
        assertTrue(PulseLogic.waveAmplitude(0.560) > 0.15)
        // Exactly periodic: phase p and p+1 are the same point.
        assertEquals(
            PulseLogic.waveAmplitude(0.320),
            PulseLogic.waveAmplitude(1.320),
            1e-9,
        )
        assertEquals(
            PulseLogic.waveAmplitude(0.05),
            PulseLogic.waveAmplitude(3.05),
            1e-9,
        )
    }

    @Test
    fun panelsTwinCarriesThePulseAtTheTopLevel() = runTest {
        val body = """{"ok": true, "panels": {},
           "pulse": {"open": 6, "normal": 3, "high": 2,
                      "urgent": 1, "pressure": 10, "bpm": 68}}"""
        val transport = FakeTransport { HttpResponse(200, body) }
        val response = api(transport).todayPanels()
        val sent = transport.requests.single()
        assertEquals("GET", sent.method)
        assertEquals("/api/app/today/panels", sent.path)
        assertEquals(68, PulseLogic.parse(response)?.bpm)
    }

    // ---- R-6: the async twins ----------------------------------------

    private val runBody = buildJsonObject {
        put("input", "target.example")
        put("caseId", 7)
    }

    @Test
    fun asyncStartRidesTheAsyncTwin() = runTest {
        val transport = FakeTransport {
            HttpResponse(
                202,
                """{"ok": true, "runId": "run-abc", "status": "running",
                   "tool": "username"}""",
            )
        }
        val out = api(transport).ctosRunToolAsync("username", runBody)
        val sent = transport.requests.single()
        assertEquals("POST", sent.method)
        assertEquals("/api/app/osint/ctos/tools/username/async", sent.path)
        assertEquals("Bearer tok-test", sent.headers["Authorization"])
        // The run body rides verbatim — input, case, options are
        // the sync twin's body, unchanged.
        assertEquals(runBody.toString(), sent.body)
        val start = CtosLogic.parseRunStart(out)
        assertEquals("run-abc", start?.runId)
        assertEquals("username", start?.tool)
        assertEquals(CtosLogic.RunPhase.RUNNING, start?.phase)
    }

    @Test
    fun asyncStartParsesDoneImmediatelyAndRejectsRunlessPayloads() {
        // A gate-refused active tool starts already done — its
        // result (the refusal) rides the first poll.
        val done = CtosLogic.parseRunStart(
            obj(
                """{"ok": true, "runId": "run-gate", "status": "done",
                   "tool": "pentest"}""",
            ),
        )
        assertEquals(CtosLogic.RunPhase.DONE, done?.phase)
        assertEquals("run-gate", done?.runId)
        // No run id, nothing honest to poll.
        assertNull(
            CtosLogic.parseRunStart(obj("""{"ok": true, "status": "running"}""")),
        )
        assertNull(CtosLogic.parseRunStart(null))
    }

    @Test
    fun pollRidesTheRunsTwin() = runTest {
        val transport = FakeTransport {
            HttpResponse(
                200,
                """{"ok": true, "runId": "run-abc", "tool": "username",
                   "status": "running", "recorded": 0}""",
            )
        }
        val out = api(transport).ctosToolRun("run-abc")
        val sent = transport.requests.single()
        assertEquals("GET", sent.method)
        assertEquals("/api/app/osint/ctos/runs/run-abc", sent.path)
        val poll = CtosLogic.parseRunPoll(out)
        assertEquals(CtosLogic.RunPhase.RUNNING, poll.phase)
        assertEquals(0, poll.recorded)
        assertEquals("", poll.error)
    }

    @Test
    fun donePollFeedsTheSyncResultShapeUnchanged() {
        // The done poll payload IS the sync envelope: recorded
        // at the top, the tool result under `result`. The one
        // result parser renders it — no async fork.
        val payload = obj(
            """{"ok": true, "runId": "run-abc", "tool": "username",
               "status": "done", "recorded": 3,
               "result": {"sources": [{"source": "GitHub",
                                        "status": "found",
                                        "summary": "Profile located.",
                                        "note": "", "url": "",
                                        "confidence": "record",
                                        "findings": []}],
                          "data": {}, "findings": []}}""",
        )
        val poll = CtosLogic.parseRunPoll(payload)
        assertEquals(CtosLogic.RunPhase.DONE, poll.phase)
        assertEquals(3, poll.recorded)
        val parsed = CtosLogic.parseToolResult(poll.payload)
        assertEquals(3, parsed.recorded)
        assertEquals(1, parsed.sources.size)
        assertEquals("GitHub", parsed.sources[0].source)
    }

    @Test
    fun failedPollCarriesTheServersErrorVerbatim() {
        val payload = obj(
            """{"ok": true, "runId": "run-abc", "tool": "username",
               "status": "error", "recorded": 0,
               "error": "TimeoutError: the source did not answer"}""",
        )
        val poll = CtosLogic.parseRunPoll(payload)
        assertEquals(CtosLogic.RunPhase.FAILED, poll.phase)
        // The server's own outcome string, unparaphrased.
        assertEquals("TimeoutError: the source did not answer", poll.error)
    }

    @Test
    fun runPhaseVocabularyIsTheServers() {
        assertEquals(CtosLogic.RunPhase.RUNNING, CtosLogic.runPhaseFor("running"))
        assertEquals(CtosLogic.RunPhase.DONE, CtosLogic.runPhaseFor("done"))
        assertEquals(CtosLogic.RunPhase.FAILED, CtosLogic.runPhaseFor("error"))
        assertEquals(CtosLogic.RunPhase.FAILED, CtosLogic.runPhaseFor("failed"))
        assertEquals(CtosLogic.RunPhase.RUNNING, CtosLogic.runPhaseFor("queued"))
        assertEquals(CtosLogic.RunPhase.RUNNING, CtosLogic.runPhaseFor(null))
        assertEquals(CtosLogic.RunPhase.RUNNING, CtosLogic.runPhaseFor(""))
    }

    @Test
    fun runGoneRecognitionIsExact() {
        assertTrue(
            CtosLogic.isRunGoneRefusal(403, "That CToS tool run does not exist."),
        )
        // The case-gone sentence is a DIFFERENT refusal.
        assertFalse(
            CtosLogic.isRunGoneRefusal(403, "That CToS case does not exist."),
        )
        assertFalse(CtosLogic.isRunGoneRefusal(403, "Not authorized."))
        assertFalse(
            CtosLogic.isRunGoneRefusal(404, "That CToS tool run does not exist."),
        )
        assertFalse(
            CtosLogic.isRunGoneRefusal(403, "That CToS tool run does not exist"),
        )
        assertFalse(CtosLogic.isRunGoneRefusal(403, null))
        assertTrue(
            CtosLogic.isRunGone(
                ApiException(403, "That CToS tool run does not exist."),
            ),
        )
        // A dead token is a sign-in event, never a gone run.
        assertFalse(CtosLogic.isRunGone(SignedOutException()))
        assertFalse(CtosLogic.isRunGone(null))
    }

    @Test
    fun pollRefusalSurfacesTheRunGoneBodyIntact() = runTest {
        val transport = FakeTransport {
            HttpResponse(
                403,
                """{"ok": false, "authorized": false,
                   "message": "That CToS tool run does not exist."}""",
            )
        }
        try {
            api(transport).ctosToolRun("run-abc")
            fail("expected the gone refusal to throw")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertEquals("That CToS tool run does not exist.", e.message)
            assertTrue(CtosLogic.isRunGone(e))
        }
    }
}
