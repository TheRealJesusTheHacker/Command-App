package net.sentinelsafeguard.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import net.sentinelsafeguard.app.ui.CtosLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CTOS-APP-2026-11-11 pins for the native "OSINT tools"
 * section (Command App 1.7.0). The fixtures mirror the twin
 * payloads the staging battery (tests/test_ctos_app.py) proved
 * against the real handlers — the shapes below are the
 * handlers' own, not inventions. The honesty rails are pinned
 * hardest: an unrecognized status grades UNAVAILABLE, never a
 * negative; key state is a boolean and nothing more.
 */
class CtosLogicTest {

    private fun obj(json: String): JsonObject =
        Json.parseToJsonElement(json) as JsonObject

    // ---- deck grouping -------------------------------------------------

    @Test
    fun wave1ToolsGroupAsPlanned() {
        val profiler = listOf("quickrecon", "username", "diligence")
        val infra = listOf("dashboard", "dns", "ip", "whois", "fingerprint")
        val files = listOf("metadata", "filehash", "phone")
        val utils = listOf(
            "base64", "hex", "jsonformat", "jwt", "password", "timestamp",
            "urlanalyze", "email", "cidr", "defang", "extractor",
            "hashanalyze", "uaparse", "ports",
        )
        profiler.forEach {
            assertEquals("$it", CtosLogic.Group.PROFILER, CtosLogic.groupFor(it))
        }
        infra.forEach {
            assertEquals("$it", CtosLogic.Group.INFRASTRUCTURE, CtosLogic.groupFor(it))
        }
        files.forEach {
            assertEquals("$it", CtosLogic.Group.FILES, CtosLogic.groupFor(it))
        }
        utils.forEach {
            assertEquals("$it", CtosLogic.Group.UTILITIES, CtosLogic.groupFor(it))
        }
        // A tool from a later wave is visible by default, never hidden.
        assertEquals(CtosLogic.Group.INFRASTRUCTURE, CtosLogic.groupFor("wayback"))
    }

    @Test
    fun runFormOptionsMirrorTheServer() {
        assertEquals(setOf("username", "quickrecon"), CtosLogic.DEPTH_TOOLS)
        assertEquals(
            listOf("encode", "decode"),
            CtosLogic.MODE_TOOLS.getValue("base64").second.map { it.first },
        )
        assertEquals(
            listOf("defang", "refang"),
            CtosLogic.MODE_TOOLS.getValue("defang").second.map { it.first },
        )
        assertEquals(
            listOf("pretty", "minify"),
            CtosLogic.MODE_TOOLS.getValue("jsonformat").second.map { it.first },
        )
        assertFalse(CtosLogic.MODE_TOOLS.containsKey("dns"))
    }

    // ---- status vocabulary ----------------------------------------------

    @Test
    fun statusesKeepTheServersHonestySemantics() {
        assertEquals(CtosLogic.Status.FOUND, CtosLogic.statusFor("found"))
        assertEquals(CtosLogic.Status.NOT_FOUND, CtosLogic.statusFor("not_found"))
        assertEquals(CtosLogic.Status.UNKNOWN, CtosLogic.statusFor("unknown"))
        assertEquals(CtosLogic.Status.MANUAL, CtosLogic.statusFor("manual"))
        assertEquals(
            CtosLogic.Status.NOT_CONFIGURED,
            CtosLogic.statusFor("not_configured"),
        )
        // Anything else — including the engine's "unavailable" and
        // outright garbage — is UNAVAILABLE, never a negative.
        assertEquals(CtosLogic.Status.UNAVAILABLE, CtosLogic.statusFor("unavailable"))
        assertEquals(CtosLogic.Status.UNAVAILABLE, CtosLogic.statusFor("blocked"))
        assertEquals(CtosLogic.Status.UNAVAILABLE, CtosLogic.statusFor(""))
        assertEquals(CtosLogic.Status.UNAVAILABLE, CtosLogic.statusFor(null))
        assertFalse(
            CtosLogic.statusFor("unavailable") == CtosLogic.Status.NOT_FOUND,
        )
    }

    // ---- workspace parsing ------------------------------------------------

    private val workspace = """
        {"ok": true,
         "cases": [{"id": 7, "title": "Seed case", "target": "seed.example.invalid",
                    "targetType": "domain", "note": "n", "status": "open",
                    "crmCaseId": null, "clientId": null, "createdBy": 1,
                    "findings": 3, "artifacts": 2,
                    "createdAt": 1762700000000, "updatedAt": 1762700001000}],
         "findingTypes": ["domain", "subdomain", "ip", "email", "username",
                          "url", "hash", "port", "technology", "phone", "other"],
         "tools": [
           {"name": "username", "label": "Username Profiler", "desc": "d",
            "input": "username", "network": true, "kind": "tool"},
           {"name": "filehash", "label": "File Hash Generator", "desc": "d",
            "input": "file", "network": false, "kind": "browser"},
           {"name": "dns", "label": "DNS Intelligence", "desc": "d",
            "input": "domain", "network": true, "kind": "tool"}],
         "catalog": {"platforms": 436, "quick": 65},
         "keySources": {"hibp": false, "shodan": true}}
    """.trimIndent()

    @Test
    fun workspaceParsesCasesToolsAndCatalog() {
        val ws = obj(workspace)
        val cases = CtosLogic.parseCases(ws)
        assertEquals(1, cases.size)
        assertEquals(7, cases[0].id)
        assertEquals("seed.example.invalid", cases[0].target)
        assertEquals(3, cases[0].findings)
        assertEquals(2, cases[0].artifacts)
        val tools = CtosLogic.parseTools(ws)
        assertEquals(listOf("username", "filehash", "dns"), tools.map { it.name })
        assertEquals("browser", tools[1].kind)
        assertTrue(tools[0].network)
        assertEquals(
            "Username catalog: 436 platforms (65 at quick depth)",
            CtosLogic.catalogLine(ws),
        )
        assertEquals(11, CtosLogic.findingTypes(ws).size)
        assertEquals(
            listOf(CtosLogic.Group.PROFILER, CtosLogic.Group.INFRASTRUCTURE,
                CtosLogic.Group.FILES),
            CtosLogic.groupsPresent(tools),
        )
    }

    @Test
    fun keySourcesAreBooleansOnly() {
        val keys = CtosLogic.parseKeySources(obj(workspace))
        assertEquals(2, keys.size)
        val hibp = keys.first { it.source == "hibp" }
        val shodan = keys.first { it.source == "shodan" }
        assertEquals("Have I Been Pwned", hibp.label)
        assertFalse(hibp.configured)
        assertTrue(shodan.configured)
        // A source the app has no friendly name for still renders.
        val extra = CtosLogic.parseKeySources(
            obj("""{"keySources": {"newsource": true}}"""),
        )
        assertEquals("newsource", extra[0].label)
        assertTrue(extra[0].configured)
    }

    // ---- case detail parsing ----------------------------------------------

    @Test
    fun caseDetailParsesFindingsAndArtifacts() {
        val payload = obj(
            """
            {"ok": true, "cases": [],
             "findings": [{"id": 5, "tool": "dns", "type": "ip",
                           "value": "192.0.2.1", "detail": "A record",
                           "url": "", "createdAt": 1762700002000}],
             "artifacts": [{"id": 9, "tool": "dns", "kind": "result",
                            "label": "dns: seed", "createdAt": 1762700002000}]}
            """.trimIndent(),
        )
        val findings = CtosLogic.parseFindings(payload)
        assertEquals(1, findings.size)
        assertEquals("192.0.2.1", findings[0].value)
        assertEquals("ip", findings[0].type)
        val artifacts = CtosLogic.parseArtifacts(payload)
        assertEquals(1, artifacts.size)
        assertEquals("result", artifacts[0].kind)
    }

    // ---- tool results -------------------------------------------------------

    @Test
    fun toolResultParsesSourcesAndRecorded() {
        val payload = obj(
            """
            {"ok": true, "tool": "base64", "recorded": 0,
             "result": {"sources": [{"source": "Base64", "status": "found",
                                     "summary": "Mode: encode.", "note": "",
                                     "url": "", "confidence": "record",
                                     "findings": []}],
                        "data": {"output": "aGVsbG8="}, "findings": []}}
            """.trimIndent(),
        )
        val parsed = CtosLogic.parseToolResult(payload)
        assertEquals(1, parsed.sources.size)
        assertEquals("Base64", parsed.sources[0].source)
        assertEquals("found", parsed.sources[0].status)
        assertEquals(0, parsed.recorded)
        assertEquals("aGVsbG8=", (parsed.data?.get("output") as?
            kotlinx.serialization.json.JsonPrimitive)?.content)
    }

    @Test
    fun usernameDataKeepsCountsAndRowStatuses() {
        val data = obj(
            """
            {"username": "seed", "depth": "quick",
             "counts": {"found": 1, "not_found": 1, "unknown": 1, "total": 3},
             "results": [
               {"site": "GitHub", "cat": "dev", "status": "found",
                "url": "https://github.com/seed"},
               {"site": "Mastodon", "cat": "social", "status": "not_found",
                "url": "https://mastodon.social/@seed"},
               {"site": "Reddit", "cat": "social", "status": "unknown",
                "url": "https://www.reddit.com/user/seed"}]}
            """.trimIndent(),
        )
        val parsed = CtosLogic.parseUsernameData(data)
        assertTrue(parsed != null)
        parsed!!
        assertEquals(1, parsed.found)
        assertEquals(1, parsed.notFound)
        assertEquals(1, parsed.unknown)
        assertEquals(3, parsed.total)
        assertEquals(CtosLogic.Status.FOUND, parsed.rows[0].status)
        assertEquals(CtosLogic.Status.NOT_FOUND, parsed.rows[1].status)
        assertEquals(CtosLogic.Status.UNKNOWN, parsed.rows[2].status)
        // A non-profiler payload does not parse as one.
        assertNull(CtosLogic.parseUsernameData(obj("""{"output": "x"}""")))
    }

    // ---- generic data shaping -----------------------------------------------

    @Test
    fun shapeDataRendersLinesSectionsAndArrays() {
        val data = obj(
            """
            {"domain": "seed.example.invalid",
             "posture": {"spf": true, "dmarcPolicy": "reject"},
             "records": ["192.0.2.1", "192.0.2.2"],
             "ports": [{"port": 443, "service": "https", "risk": "LOW"},
                       {"port": 22, "service": "ssh", "risk": "HIGH"}],
             "blank": "", "nothing": null}
            """.trimIndent(),
        )
        val nodes = CtosLogic.shapeData(data)
        val lines = nodes.filterIsInstance<CtosLogic.DataNode.Line>()
        val sections = nodes.filterIsInstance<CtosLogic.DataNode.Section>()
        assertTrue(lines.any { it.label == "Domain" && it.value == "seed.example.invalid" })
        assertTrue(lines.any { it.label == "Records" && it.value == "192.0.2.1, 192.0.2.2" })
        // Blanks and JSON nulls render nothing (the detail-grid rule).
        assertFalse(lines.any { it.label == "Blank" || it.label == "Nothing" })
        val posture = sections.first { it.title == "Posture" }
        assertTrue(posture.children.any {
            it is CtosLogic.DataNode.Line && it.label == "Spf" && it.value == "Yes"
        })
        val ports = sections.first { it.title == "Ports" }
        assertEquals(2, ports.children.size)
        val first = ports.children[0] as CtosLogic.DataNode.Section
        assertEquals("Port 443", first.title)
    }

    @Test
    fun prettyKeyHumanizesServerKeys() {
        // "spf" is a known acronym: it stays upper-case.
        assertEquals("SPF record", CtosLogic.prettyKey("spfRecord"))
        assertEquals("Detected type", CtosLogic.prettyKey("detected_type"))
        assertEquals("Sha256", CtosLogic.prettyKey("sha256"))
        assertEquals("Size bytes", CtosLogic.prettyKey("sizeBytes"))
    }

    // ---- on-device file hashing ------------------------------------------------

    @Test
    fun fileHashesMatchKnownVectors() {
        val abc = CtosLogic.hashesOf("abc".toByteArray(Charsets.UTF_8))
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            abc.sha256,
        )
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", abc.sha1)
        assertEquals("900150983cd24fb0d6963f7d28e17f72", abc.md5)
        assertEquals(3L, abc.sizeBytes)
        val empty = CtosLogic.hashesOf(ByteArray(0))
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            empty.sha256,
        )
        assertEquals("0 B", CtosLogic.sizeText(0))
        assertEquals("1.5 KB", CtosLogic.sizeText(1536))
    }
}
