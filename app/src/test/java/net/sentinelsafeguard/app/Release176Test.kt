package net.sentinelsafeguard.app

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import net.sentinelsafeguard.app.config.ApiConfig
import net.sentinelsafeguard.app.data.api.HttpResponse
import net.sentinelsafeguard.app.data.api.SentinelApi
import net.sentinelsafeguard.app.data.api.ctosRunTool
import net.sentinelsafeguard.app.data.api.ctosRunToolAsync
import net.sentinelsafeguard.app.ui.CtosLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RELEASE 1.7.6 pins — CToS trio parity (Composite Profiler +
 * AI Analyst), against Main's CTOS-TRIO-DEPLOY-2026-11-14
 * contracts (production server.py eab33b57…, registry 37).
 *
 * The deck is driven by the server's registry metadata — both
 * tools arrive through the EXISTING twins with NO new
 * endpoints and NO contract changes, so the pins hold: the
 * registry entries parse verbatim and group under Profiler;
 * the deck lists exactly what the server sends (there are no
 * local tool definitions — which is also why YouTube DL,
 * blocked server-side on containment, has no surface here and
 * can never be listed); the run body is one construction whose
 * top-level caseId both dispatch paths resolve through the
 * identical F-C1 mutate scoping check (the Analyst REQUIRES
 * it); the composite's Identity Graph parses faithfully
 * (verbatim provenance, the server's categorical corroboration
 * confidence, legs, edges, the truncation flag); the analyst's
 * `configured` state is read with the key-status
 * strict-literal discipline, and its no-key not_configured
 * result is a designed state, never an error; and the
 * key-status surface carries anthropic / openai in the same
 * strict style as the existing six.
 */
class Release176Test {

    private fun obj(json: String): JsonObject =
        Json.parseToJsonElement(json) as JsonObject

    private val config = ApiConfig(
        baseUrl = "https://command.example.test",
        cfAccessClientId = "test-client-id",
        cfAccessClientSecret = "test-client-secret",
    )

    private fun api(transport: FakeTransport): SentinelApi =
        SentinelApi(config, transport).also { it.deviceToken = "tok-test" }

    // ---- Registry parity -------------------------------------------

    /** The workspace tools array as _ctos_tools_meta sends it —
     *  the trio-era entries verbatim beside two standing ones. */
    private val workspace = obj(
        """{"tools": [
          {"name": "username", "label": "Username Profiler",
           "desc": "Profile presence across the 400+ platform Sentinel catalog.",
           "input": "username", "network": true, "kind": "tool", "gated": false},
          {"name": "dns", "label": "DNS Intelligence",
           "desc": "Full record set plus email-auth posture for a domain.",
           "input": "domain", "network": true, "kind": "tool", "gated": false},
          {"name": "composite", "label": "Composite Profiler",
           "desc": "Capstone: one target -> merged people, infra and breach profile across the passive tools that fit its kind, with a confidence-scored Identity Graph. Passive tools only -- never the gated active tools.",
           "input": "username / email / domain / IP / name",
           "network": true, "kind": "tool", "gated": false},
          {"name": "analyst", "label": "AI Analyst",
           "desc": "AI-generated analysis of ONE case's findings, answering a case-bound question. Needs an Anthropic or OpenAI key in CToS source keys (Anthropic wins when both are set); with no key it reports not_configured and generates nothing.",
           "input": "question about this case",
           "network": true, "kind": "tool", "gated": false}
        ]}""",
    )

    @Test
    fun trioToolsParseVerbatimFromTheRegistry() {
        val tools = CtosLogic.parseTools(workspace)
        // The deck lists exactly what the server sent — nothing
        // invented, nothing dropped.
        assertEquals(4, tools.size)
        val composite = tools.first { it.name == "composite" }
        assertEquals("Composite Profiler", composite.label)
        assertEquals(
            "Capstone: one target -> merged people, infra and breach " +
                "profile across the passive tools that fit its kind, " +
                "with a confidence-scored Identity Graph. Passive tools " +
                "only -- never the gated active tools.",
            composite.desc,
        )
        assertEquals("username / email / domain / IP / name", composite.inputHint)
        assertTrue(composite.network)
        assertEquals("tool", composite.kind)
        assertFalse(composite.gated)
        val analyst = tools.first { it.name == "analyst" }
        assertEquals("AI Analyst", analyst.label)
        assertEquals("question about this case", analyst.inputHint)
        assertTrue(analyst.network)
        assertFalse(analyst.gated)
    }

    @Test
    fun trioToolsGroupUnderProfiler() {
        assertEquals(CtosLogic.Group.PROFILER, CtosLogic.groupFor("composite"))
        assertEquals(CtosLogic.Group.PROFILER, CtosLogic.groupFor("analyst"))
        val tools = CtosLogic.parseTools(workspace)
        val profiler = CtosLogic.toolsInGroup(tools, CtosLogic.Group.PROFILER)
        // Registry order preserved within the group.
        assertEquals(
            listOf("username", "composite", "analyst"),
            profiler.map { it.name },
        )
        assertTrue(CtosLogic.groupsPresent(tools).contains(CtosLogic.Group.PROFILER))
    }

    @Test
    fun noYouTubeDlSurfaceExists() {
        // YouTube DL did NOT ship server-side (containment), and
        // the deck has no local tool definitions to add one:
        // nothing in the parsed deck can be a YouTube tool.
        val tools = CtosLogic.parseTools(workspace)
        assertTrue(
            tools.none {
                it.name.contains("youtube") || it.name.contains("ytdlp") ||
                    it.name.contains("yt-dlp") || it.label.contains("YouTube")
            },
        )
    }

    // ---- Run body: caseId binding on the shared construction --------

    @Test
    fun analystRunBodyCarriesTheCaseBindingAndNoOptions() {
        val body = CtosLogic.toolRunBody(
            toolName = "analyst",
            input = "  What stands out? ",
            caseId = 7,
            depth = "quick",
            mode = "",
            fileB64 = null,
            fileName = null,
        )
        // Input trimmed; the binding rides the top level, where
        // BOTH dispatch paths resolve it through the F-C1 mutate
        // check; no form options exist for this tool, so no
        // options object is sent at all.
        assertEquals("""{"input":"What stands out?","caseId":7}""", body.toString())
    }

    @Test
    fun compositeRunBodyBindsLikeEveryOtherTool() {
        val bound = CtosLogic.toolRunBody(
            "composite", "person@example.com", 12, "quick", "", null, null,
        )
        assertEquals(
            """{"input":"person@example.com","caseId":12}""",
            bound.toString(),
        )
        val unbound = CtosLogic.toolRunBody(
            "composite", "person@example.com", null, "quick", "", null, null,
        )
        assertEquals("""{"input":"person@example.com"}""", unbound.toString())
    }

    @Test
    fun standingToolsRunBodiesAreUnchanged() {
        // Depth tools keep their depth option…
        assertEquals(
            """{"input":"bob","caseId":7,"options":{"depth":"full"}}""",
            CtosLogic.toolRunBody(
                "username", "bob", 7, "full", "", null, null,
            ).toString(),
        )
        // …mode tools keep their mode (and a blank mode sends none)…
        assertEquals(
            """{"input":"aGk=","options":{"mode":"decode"}}""",
            CtosLogic.toolRunBody(
                "base64", "aGk=", null, "quick", "decode", null, null,
            ).toString(),
        )
        assertEquals(
            """{"input":"aGk="}""",
            CtosLogic.toolRunBody(
                "base64", "aGk=", null, "quick", "", null, null,
            ).toString(),
        )
        // …and the metadata file still rides options verbatim.
        assertEquals(
            """{"input":"","options":{"fileB64":"QUJD","filename":"img.png"}}""",
            CtosLogic.toolRunBody(
                "metadata", "", null, "quick", "", "QUJD", "img.png",
            ).toString(),
        )
    }

    @Test
    fun caseBindingRidesBothDispatchPathsVerbatim() = runTest {
        val body = CtosLogic.toolRunBody(
            "analyst", "What stands out?", 7, "quick", "", null, null,
        )
        // Sync twin.
        val syncTransport = FakeTransport {
            HttpResponse(200, """{"ok": true, "tool": "analyst", "recorded": 0}""")
        }
        api(syncTransport).ctosRunTool("analyst", body)
        val syncSent = syncTransport.requests.single()
        assertEquals("POST", syncSent.method)
        assertEquals("/api/app/osint/ctos/tools/analyst", syncSent.path)
        assertEquals(body.toString(), syncSent.body)
        // Async twin — the same body, the same binding.
        val asyncTransport = FakeTransport {
            HttpResponse(
                202,
                """{"ok": true, "runId": "run-trio", "status": "running",
                   "tool": "composite"}""",
            )
        }
        val compositeBody = CtosLogic.toolRunBody(
            "composite", "person@example.com", 7, "quick", "", null, null,
        )
        val out = api(asyncTransport).ctosRunToolAsync("composite", compositeBody)
        val asyncSent = asyncTransport.requests.single()
        assertEquals("POST", asyncSent.method)
        assertEquals("/api/app/osint/ctos/tools/composite/async", asyncSent.path)
        assertEquals(compositeBody.toString(), asyncSent.body)
        assertEquals("run-trio", CtosLogic.parseRunStart(out)?.runId)
    }

    // ---- Composite: the Identity Graph -------------------------------

    /** A composite result envelope in the server's real shape
     *  (an email target: email + breach + dns + emailaudit legs). */
    private val compositePayload = obj(
        """{"ok": true, "tool": "composite", "recorded": 2,
           "result": {
             "sources": [
               {"source": "Composite leg: DNS Intelligence",
                "status": "found",
                "summary": "DNS Intelligence on example.com: 3 source(s) answered (found: 3). Full per-source detail is in data.modules.",
                "note": "", "url": "", "confidence": "info",
                "findings": [], "tool": "dns"}
             ],
             "data": {
               "target": "person@example.com",
               "detectedType": "email",
               "detectedCandidates": ["email"],
               "plan": ["email", "breach", "dns", "emailaudit"],
               "modules": [],
               "identityGraph": {
                 "nodes": [
                   {"id": "email:person@example.com",
                    "label": "person@example.com", "kind": "email",
                    "sources": [], "tools": [], "seed": true,
                    "confidence": "low", "corroborations": 0},
                   {"id": "domain:example.com",
                    "label": "example.com", "kind": "domain",
                    "sources": [
                      {"tool": "dns", "source": "DNS records", "status": "found"},
                      {"tool": "emailaudit", "source": "Email Security Auditor", "status": "found"}],
                    "tools": ["dns", "emailaudit"],
                    "confidence": "high", "corroborations": 2},
                   {"id": "breach:person@example.com",
                    "label": "person@example.com", "kind": "breach",
                    "sources": [
                      {"tool": "breach", "source": "Breach Exposure", "status": "not_configured"}],
                    "tools": ["breach"],
                    "confidence": "low", "corroborations": 0}
                 ],
                 "edges": [
                   {"a": "email:person@example.com", "b": "domain:example.com",
                    "rel": "dns", "tool": "dns", "status": "found"},
                   {"a": "email:person@example.com", "b": "domain:example.com",
                    "rel": "emailaudit", "tool": "emailaudit", "status": "found"}
                 ],
                 "legs": [
                   {"tool": "email", "input": "person@example.com", "status": "found"},
                   {"tool": "breach", "input": "person@example.com", "status": "not_configured"},
                   {"tool": "dns", "input": "example.com", "status": "found"},
                   {"tool": "emailaudit", "input": "example.com", "status": "found"}
                 ],
                 "truncated": false
               },
               "summary": "Composite profile of person@example.com (detected as email): 4/4 passive leg(s) ran, 1 merged finding(s), 3 identifier(s) in the Identity Graph. Legs marked not_configured or unavailable said nothing either way -- they are shown, not hidden.",
               "counts": {"legs": 4, "legsOk": 4, "nodes": 3, "edges": 2, "findings": 1}
             },
             "findings": [
               {"type": "domain", "value": "example.com",
                "detail": "MX handled by example.com.", "url": ""}
             ]
           }}""",
    )

    @Test
    fun identityGraphParsesFaithfully() {
        val parsed = CtosLogic.parseToolResult(compositePayload)
        assertEquals(2, parsed.recorded)
        assertEquals("Composite leg: DNS Intelligence", parsed.sources[0].source)
        // Merged findings carry no tool of their own — the run's
        // tool (composite) is the honest attribution.
        assertEquals(1, parsed.findings.size)
        assertEquals("composite", parsed.findings[0].tool)
        assertEquals("domain", parsed.findings[0].type)

        val graph = CtosLogic.parseIdentityGraph(parsed.data)
        assertEquals(3, graph?.nodes?.size)
        assertEquals(2, graph?.edges?.size)
        assertEquals(4, graph?.legs?.size)
        assertEquals(false, graph?.truncated)

        val seed = graph!!.nodes.first { it.seed }
        assertEquals("person@example.com", seed.label)
        assertEquals("email", seed.kind)

        val domain = graph.nodes.first { it.id == "domain:example.com" }
        assertEquals("high", domain.confidence)
        assertEquals(2, domain.corroborations)
        assertEquals(listOf("dns", "emailaudit"), domain.tools)
        // Provenance verbatim: tool, source, and the server's
        // status through the shared vocabulary.
        assertEquals("dns", domain.provenance[0].tool)
        assertEquals("DNS records", domain.provenance[0].source)
        assertEquals(CtosLogic.Status.FOUND, domain.provenance[0].status)

        // A not_configured leg's node reads exactly that — its
        // silence is never a negative.
        val breach = graph.nodes.first { it.id == "breach:person@example.com" }
        assertEquals(
            CtosLogic.Status.NOT_CONFIGURED,
            breach.provenance[0].status,
        )
        assertEquals(
            CtosLogic.Status.NOT_CONFIGURED,
            graph.legs[1].status,
        )
        assertEquals("dns", graph.edges[0].tool)
        assertEquals(CtosLogic.Status.FOUND, graph.edges[0].status)
    }

    @Test
    fun identityGraphDisplayOrderIsSeedThenConfidence() {
        val graph = CtosLogic.parseIdentityGraph(
            CtosLogic.parseToolResult(compositePayload).data,
        )!!
        val display = CtosLogic.graphDisplayNodes(graph)
        assertEquals(
            listOf(
                "email:person@example.com",
                "domain:example.com",
                "breach:person@example.com",
            ),
            display.map { it.id },
        )
        assertEquals("High", CtosLogic.confidenceLabel("high"))
        assertEquals("Medium", CtosLogic.confidenceLabel("medium"))
        assertEquals("Low", CtosLogic.confidenceLabel("low"))
        // An unrecognized bucket word is the server's — shown raw.
        assertEquals("odd", CtosLogic.confidenceLabel("odd"))
        assertTrue(
            CtosLogic.confidenceRank("odd") > CtosLogic.confidenceRank("low"),
        )
    }

    @Test
    fun absentGraphIsNoGraph() {
        assertNull(CtosLogic.parseIdentityGraph(null))
        assertNull(
            CtosLogic.parseIdentityGraph(obj("""{"target": "x", "modules": []}""")),
        )
        // And a truncated graph says so, verbatim from the flag.
        val truncated = CtosLogic.parseIdentityGraph(
            obj(
                """{"identityGraph": {"nodes": [], "edges": [],
                   "legs": [], "truncated": true}}""",
            ),
        )
        assertEquals(true, truncated?.truncated)
        assertEquals(0, truncated?.nodes?.size)
    }

    // ---- AI Analyst ---------------------------------------------------

    @Test
    fun analystNotConfiguredIsADesignedState() {
        val payload = obj(
            """{"ok": true, "tool": "analyst", "recorded": 0,
               "result": {
                 "sources": [
                   {"source": "AI Analyst", "status": "not_configured",
                    "summary": "No LLM provider key is configured, so no analysis was generated. Add an Anthropic or OpenAI key in Command Settings (CToS source keys) and run again. Nothing was inferred or fabricated in place of an answer.",
                    "note": "", "url": "", "confidence": "info",
                    "findings": []}
                 ],
                 "data": {"configured": false, "provider": null,
                          "question": "What stands out?",
                          "analysis": null},
                 "findings": []
               }}""",
        )
        val parsed = CtosLogic.parseToolResult(payload)
        // The source carries the server's status through the
        // shared vocabulary — NOT_CONFIGURED, its own pill,
        // never an error and never a negative.
        assertEquals(
            CtosLogic.Status.NOT_CONFIGURED,
            CtosLogic.statusFor(parsed.sources[0].status),
        )
        val analyst = CtosLogic.parseAnalystData(parsed.data)
        assertEquals(CtosLogic.AnalystState.NOT_CONFIGURED, analyst?.state)
        assertNull(analyst?.analysis)
        assertEquals("", analyst?.provider)
        assertEquals("What stands out?", analyst?.question)
        assertTrue(parsed.findings.isEmpty())
    }

    @Test
    fun analystSuccessCarriesTheAnalysisAndItsLabel() {
        val payload = obj(
            """{"ok": true, "tool": "analyst", "recorded": 1,
               "result": {
                 "sources": [],
                 "data": {"configured": true, "provider": "anthropic",
                          "model": "claude-sonnet-4-5",
                          "question": "What stands out?",
                          "analysis": "Two findings corroborate the domain.",
                          "findingsConsidered": 12, "findingsTotal": 30,
                          "contextTruncated": true, "contextCapChars": 24000},
                 "findings": [
                   {"type": "analysis", "value": "AI analysis: What stands out?",
                    "detail": "AI-GENERATED ANALYSIS (Anthropic / claude-sonnet-4-5) -- analysis of this case's findings, not a verified fact.",
                    "url": ""}
                 ]
               }}""",
        )
        val parsed = CtosLogic.parseToolResult(payload)
        val analyst = CtosLogic.parseAnalystData(parsed.data)
        assertEquals(CtosLogic.AnalystState.READY, analyst?.state)
        assertEquals("anthropic", analyst?.provider)
        assertEquals("Anthropic", CtosLogic.analystProviderLabel(analyst!!.provider))
        assertEquals("OpenAI", CtosLogic.analystProviderLabel("openai"))
        assertEquals("claude-sonnet-4-5", analyst.model)
        assertEquals("Two findings corroborate the domain.", analyst.analysis)
        assertEquals(12, analyst.findingsConsidered)
        assertEquals(30, analyst.findingsTotal)
        assertEquals(true, analyst.contextTruncated)
        // The recorded finding keeps the server's AI-generated
        // label and its analysis type.
        assertEquals("analysis", parsed.findings[0].type)
        assertTrue(
            parsed.findings[0].detail.startsWith(
                "AI-GENERATED ANALYSIS (Anthropic / claude-sonnet-4-5)",
            ),
        )
    }

    @Test
    fun analystProviderFailureIsHonestAndVerbatim() {
        val analyst = CtosLogic.parseAnalystData(
            obj(
                """{"configured": true, "provider": "openai",
                   "model": "gpt-4o", "question": "Q?",
                   "analysis": null, "error": "HTTP 500",
                   "httpStatus": 500}""",
            ),
        )
        assertEquals(CtosLogic.AnalystState.READY, analyst?.state)
        assertNull(analyst?.analysis)
        assertEquals("HTTP 500", analyst?.error)
    }

    @Test
    fun analystConfiguredStateIsNeverGuessed() {
        // A string is not a state (the key-status discipline):
        // "false" as a string is UNREPORTED, not NOT_CONFIGURED.
        assertEquals(
            CtosLogic.AnalystState.UNREPORTED,
            CtosLogic.parseAnalystData(obj("""{"configured": "false"}"""))?.state,
        )
        assertEquals(
            CtosLogic.AnalystState.UNREPORTED,
            CtosLogic.parseAnalystData(obj("""{"configured": 1}"""))?.state,
        )
        // No `configured` key at all is not an analyst result.
        assertNull(CtosLogic.parseAnalystData(obj("""{"question": "Q?"}""")))
        assertNull(CtosLogic.parseAnalystData(null))
    }

    // ---- Key status: the two new sources, same strict style ---------

    @Test
    fun keyStatusCarriesAnthropicAndOpenAiStrictly() {
        val workspace = obj(
            """{"keySources": {"hibp": true, "greynoise": false,
               "virustotal": true, "abuseipdb": false, "shodan": false,
               "securitytrails": true, "anthropic": true,
               "openai": "true"}}""",
        )
        val sources = CtosLogic.parseKeySources(workspace)
        // The exact set the server reported — the existing six
        // plus the trio's two, nothing invented.
        assertEquals(8, sources.size)
        val anthropic = sources.first { it.source == "anthropic" }
        assertEquals("Anthropic", anthropic.label)
        assertEquals(CtosLogic.KeyStatus.CONFIGURED, anthropic.status)
        assertNull(CtosLogic.keyGuidance(anthropic))
        // A string "true" is not a reported state — the same
        // strictness the existing six are held to.
        val openai = sources.first { it.source == "openai" }
        assertEquals("OpenAI", openai.label)
        assertEquals(CtosLogic.KeyStatus.UNAVAILABLE, openai.status)
    }

    @Test
    fun keyStatusNotConfiguredGuidanceNamesTheAnalystAndTheWeb() {
        val workspace = obj("""{"keySources": {"anthropic": false, "openai": false}}""")
        val sources = CtosLogic.parseKeySources(workspace)
        val anthropic = sources.first { it.source == "anthropic" }
        assertEquals(CtosLogic.KeyStatus.NOT_CONFIGURED, anthropic.status)
        assertEquals(
            "Unlocks AI Analyst answers over a case's findings. " +
                "Set in web Command settings.",
            CtosLogic.keyGuidance(anthropic),
        )
        val openai = sources.first { it.source == "openai" }
        assertEquals(CtosLogic.KeyStatus.NOT_CONFIGURED, openai.status)
        assertEquals(
            "Unlocks AI Analyst answers over a case's findings. " +
                "Set in web Command settings.",
            CtosLogic.keyGuidance(openai),
        )
    }
}
