package net.sentinelsafeguard.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import net.sentinelsafeguard.app.ui.OsintLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 1.5.0 pins for the nine new Command OSINT legs wired into the
 * deck. Fixtures are the contracts' own examples
 * (sentinel-live-deploy/COMMAND-TOOLS-PROGRAM-2026-11-07/
 * contracts/) in the standard _osint_src shape — the parsing and
 * grading the deck renders are exercised against them verbatim.
 *
 * Honesty rails pinned here:
 *  - the IP-intelligence payload carries NO coordinates (text
 *    location only) → no pin is produced, ever;
 *  - a payload that DOES carry coordinates plots them verbatim;
 *  - wallet copy carries the server's identity note and no fiat;
 *  - the breach coverage line states HIBP is NOT covered;
 *  - the two reconciliations: no standalone on-device variation
 *    batch tool remains, and the dork generator is labeled an
 *    on-device preview.
 */
class OsintLegs150Test {

    private fun src(json: String) = Json.parseToJsonElement(json).jsonObject

    // ---- deck shape ---------------------------------------------------

    @Test
    fun deckCarriesTheNewLegsAsDistinctTools() {
        assertEquals(23, OsintLogic.TOOLS.size)
        val want: Map<String, Pair<String, OsintLogic.Group>> = mapOf(
            "subdomain" to ("subdomains" to OsintLogic.Group.NETWORK),
            "dns" to ("dns" to OsintLogic.Group.NETWORK),
            "site" to ("siteInspector" to OsintLogic.Group.NETWORK),
            "archive" to ("archive" to OsintLogic.Group.BACKGROUND),
            "btc" to ("btc" to OsintLogic.Group.NETWORK),
            "eth" to ("eth" to OsintLogic.Group.NETWORK),
            "breach" to ("emailBreach" to OsintLogic.Group.BREACH),
        )
        for ((type, idGroup) in want) {
            val (id, group) = idGroup
            val tool = OsintLogic.tool(id)!!
            assertEquals("tool $id type", type, tool.typeKey)
            assertEquals("tool $id group", group, tool.group)
        }
        // Every one of the 18 server types has a dedicated tool.
        for (type in OsintLogic.SERVER_TYPES) {
            assertTrue("no dedicated tool for $type", OsintLogic.typeHasDedicatedTool(type))
        }
    }

    @Test
    fun reconciliationsHoldInTheDeckModel() {
        // Username variations: the sweep is server-side now; the
        // standalone on-device batch tool is gone, not duplicated.
        assertNull(OsintLogic.tool("usernameVariations"))
        // Dork generator: kept, but labeled exactly what it is.
        val dork = OsintLogic.tool("dork")!!
        assertEquals("ON-DEVICE PREVIEW", dork.chip)
        assertNull(dork.typeKey)
        // EXIF keeps its on-device chip.
        assertEquals("ON-DEVICE", OsintLogic.tool("exif")!!.chip)
        // Email breach runs the dedicated server type, not "email".
        assertEquals("breach", OsintLogic.tool("emailBreach")!!.typeKey)
        assertEquals("email", OsintLogic.tool("email")!!.typeKey)
    }

    @Test
    fun lawCopyIsVerbatim() {
        assertEquals(
            "An address is not an identity. Only public chain facts are " +
                "shown; no dollar value is stated anywhere in this result " +
                "(no price source is consulted).",
            OsintLogic.WALLET_NOTE,
        )
        assertFalse(OsintLogic.WALLET_NOTE.contains("$"))
        assertEquals(
            "Corpora searched: XposedOrNot + LeakCheck public index. " +
                "Have I Been Pwned is NOT covered — its account lookup " +
                "requires a paid key.",
            OsintLogic.BREACH_COVERAGE_LINE,
        )
        assertEquals(
            "IP intelligence (allocation & geo)",
            OsintLogic.IP_INTEL_SOURCE,
        )
    }

    // ---- subdomain / dns fixtures (contracts' examples) ---------------

    @Test
    fun subdomainFixtureParsesAndGrades() {
        val entry = OsintLogic.parseSourceEntry(
            src(
                """
                {"source": "Subdomain enumeration", "status": "found",
                 "summary": "4 unique subdomain name(s) for zone.test (showing 4).",
                 "findings": [
                   {"title": "www.zone.test",
                    "detail": "in certificate-transparency logs; resolves: A 93.184.216.34"},
                   {"title": "app.zone.test",
                    "detail": "resolves: CNAME zone.test."}],
                 "note": "Answered by: crt.sh, Certspotter.",
                 "confidence": "record", "url": "", "retrievedAt": 1790000000000}
                """,
            ),
        )
        assertEquals("Subdomain enumeration", entry.source)
        assertEquals(OsintLogic.Grade.HIT, OsintLogic.gradeForStatus(entry.status))
        assertEquals(2, entry.findings.size)
        assertEquals("www.zone.test", entry.findings[0].title)
        assertTrue(entry.findings[0].detail!!.contains("certificate-transparency"))
        assertEquals("Answered by: crt.sh, Certspotter.", entry.note)
    }

    @Test
    fun dnsFixtureParsesRecordFindings() {
        val entry = OsintLogic.parseSourceEntry(
            src(
                """
                {"source": "DNS explorer", "status": "found",
                 "summary": "Public DNS records for zone.test.",
                 "findings": [
                   {"title": "MX records",
                    "detail": "preference 10: mail.zone.test. (TTL 300)"},
                   {"title": "SOA record",
                    "detail": "primary nameserver (mname): ns1.zone.test.; serial: 2026010101 (TTL 3600)"},
                   {"title": "DNSSEC",
                    "detail": "No DNSKEY or DS records answered — DNSSEC not signed for this name (per the answering resolver)."}],
                 "confidence": "record"}
                """,
            ),
        )
        assertEquals(OsintLogic.Grade.HIT, OsintLogic.gradeForStatus(entry.status))
        assertEquals(
            listOf("MX records", "SOA record", "DNSSEC"),
            entry.findings.map { it.title },
        )
        // An IP input to the DNS tool yields a manual cross-link entry.
        val manual = OsintLogic.parseSourceEntry(
            src("""{"source": "DNS explorer", "status": "manual",
                   "summary": "Input is an IP address — ran PTR + IP intelligence."}"""),
        )
        assertEquals(OsintLogic.Grade.MANUAL, OsintLogic.gradeForStatus(manual.status))
    }

    // ---- IP intelligence: coordinates discipline ----------------------

    private val ipIntelFixture = """
        {"source": "IP intelligence (allocation & geo)", "status": "found",
         "summary": "Allocation and approximate geographic data: ASN: AS64500; ISP: Fallback ISP; Organization: Fallback Org; Location: Kansas City, Missouri, United States; Type: IPv4",
         "findings": [{"title": "203.0.113.8", "url": "",
           "detail": "ASN: AS64500; ISP: Fallback ISP; Organization: Fallback Org; Location: Kansas City, Missouri, United States; Type: IPv4"}],
         "note": "Answered by: ip-api.com. Allocation and geo data describe the network operator, not a person.",
         "confidence": "record"}
    """

    @Test
    fun ipIntelPayloadWithoutCoordinatesYieldsNoPin() {
        // The contract's own example payload: text location only.
        assertNull(OsintLogic.ipIntelCoordinates(src(ipIntelFixture)))
        val entry = OsintLogic.parseSourceEntry(src(ipIntelFixture))
        assertEquals(OsintLogic.Grade.HIT, OsintLogic.gradeForStatus(entry.status))
        assertTrue(entry.note!!.contains("not a person"))
    }

    @Test
    fun ipIntelCoordinatesArePlottedVerbatimWhenCarried() {
        val withText = ipIntelFixture.replace(
            "Type: IPv4\"}]",
            "Type: IPv4; Coordinates: 39.0997, -94.5786\"}]",
        )
        val pair = OsintLogic.ipIntelCoordinates(src(withText))
        assertEquals(39.0997, pair!!.first, 0.00001)
        assertEquals(-94.5786, pair.second, 0.00001)
        // Structured pair on a finding wins as-is.
        val structured = """
            {"source": "IP intelligence (allocation & geo)", "status": "found",
             "findings": [{"title": "203.0.113.8", "lat": 38.5, "lon": -94.5}]}
        """
        val pair2 = OsintLogic.ipIntelCoordinates(src(structured))
        assertEquals(38.5, pair2!!.first, 0.00001)
        assertEquals(-94.5, pair2.second, 0.00001)
    }

    @Test
    fun coordinatesOnOtherSourcesNeverPlot() {
        // The coordinate gate is the IP-intelligence source only.
        val other = """
            {"source": "RDAP registration record", "status": "found",
             "summary": "Registered near 39.0997, -94.5786 historically."}
        """
        assertNull(OsintLogic.ipIntelCoordinates(src(other)))
        // Out-of-range pairs are rejected, not clamped.
        val bad = ipIntelFixture.replace(
            "Type: IPv4\"}]",
            "Type: IPv4; Coordinates: 391.0997, -940.5786\"}]",
        )
        assertNull(OsintLogic.ipIntelCoordinates(src(bad)))
    }

    // ---- site inspector fixtures --------------------------------------

    @Test
    fun siteInspectorGradesFactsAndIntermediaryHonestly() {
        val tls = OsintLogic.parseSourceEntry(
            src(
                """
                {"source": "TLS certificate (direct handshake)", "status": "found",
                 "summary": "Certificate served by fixture.example.",
                 "findings": [
                   {"title": "Subject", "detail": "CN=fixture.example"},
                   {"title": "Issuer", "detail": "CN=Fixture CA"},
                   {"title": "Validity", "detail": "not before 2026-01-01; not after 2027-01-01"}],
                 "confidence": "record"}
                """,
            ),
        )
        assertEquals(OsintLogic.Grade.HIT, OsintLogic.gradeForStatus(tls.status))
        assertEquals("CN=fixture.example", tls.findings[0].detail)
        // The intermediary path grades unavailable — never dressed as facts.
        val blocked = OsintLogic.parseSourceEntry(
            src(
                """
                {"source": "TLS certificate (direct handshake)", "status": "unavailable",
                 "summary": "",
                 "note": "direct TLS inspection was not possible from this network (an intermediary answered) — certificate facts were taken from the transparency logs instead"}
                """,
            ),
        )
        assertEquals(OsintLogic.Grade.UNAVAILABLE, OsintLogic.gradeForStatus(blocked.status))
        assertTrue(blocked.note!!.contains("intermediary answered"))
    }

    // ---- wayback fixtures ----------------------------------------------

    @Test
    fun waybackGradesFoundEmptyAndDown() {
        val found = OsintLogic.parseSourceEntry(
            src(
                """
                {"source": "Wayback Machine — capture history (CDX)", "status": "found",
                 "summary": "At least 5000 captures.",
                 "findings": [
                   {"title": "2019 — 12 captures", "detail": ""},
                   {"title": "Earliest capture", "detail": "2019-01-01",
                    "url": "https://web.archive.org/web/20190101000000/http://example.com/"}],
                 "confidence": "record"}
                """,
            ),
        )
        assertEquals(OsintLogic.Grade.HIT, OsintLogic.gradeForStatus(found.status))
        assertTrue(found.findings[1].url!!.startsWith("https://web.archive.org/web/"))
        assertEquals(
            OsintLogic.Grade.NO_HIT,
            OsintLogic.gradeForStatus("not_found"),
        )
        assertEquals(
            OsintLogic.Grade.UNAVAILABLE,
            OsintLogic.gradeForStatus("unavailable"),
        )
    }

    // ---- wallet fixtures: precision, no fiat ----------------------------

    @Test
    fun walletFixturesKeepRawUnitPrecision() {
        val btc = OsintLogic.parseSourceEntry(
            src(
                """
                {"source": "Bitcoin address — public chain", "status": "found",
                 "summary": "Public chain data for this address.",
                 "findings": [
                   {"title": "Balance", "detail": "0.00100000 BTC (100000 satoshis)"},
                   {"title": "Transactions", "detail": "3"}],
                 "note": "Answered by: mempool.space. An address is not an identity.",
                 "confidence": "record"}
                """,
            ),
        )
        assertEquals(OsintLogic.Grade.HIT, OsintLogic.gradeForStatus(btc.status))
        assertEquals("0.00100000 BTC (100000 satoshis)", btc.findings[0].detail)
        assertFalse(btc.findings[0].detail!!.contains("$"))
        val eth = OsintLogic.parseSourceEntry(
            src(
                """
                {"source": "Ethereum address — public chain", "status": "found",
                 "findings": [
                   {"title": "Balance",
                    "detail": "1.500000000000000000 ETH (1500000000000000000 wei)"},
                   {"title": "Transactions", "detail": "7"}],
                 "confidence": "record"}
                """,
            ),
        )
        assertEquals(
            "1.500000000000000000 ETH (1500000000000000000 wei)",
            eth.findings[0].detail,
        )
        // A 404-everywhere address is the contract's not_found wording path.
        assertEquals(OsintLogic.Grade.NO_HIT, OsintLogic.gradeForStatus("not_found"))
    }

    // ---- breach battery: two corpora, graded separately ------------------

    @Test
    fun breachCorporaGradeSeparately() {
        val xon = OsintLogic.parseSourceEntry(
            src(
                """
                {"source": "XposedOrNot breach index", "status": "found",
                 "summary": "This email appears in public breach indexes.",
                 "findings": [{"title": "Collection1", "detail": "Breach corpus entry in the public XposedOrNot index."}],
                 "confidence": "possible"}
                """,
            ),
        )
        val leak = OsintLogic.parseSourceEntry(
            src(
                """
                {"source": "LeakCheck public index", "status": "unavailable",
                 "summary": "",
                 "note": "LeakCheck public index: HTTP 429"}
                """,
            ),
        )
        // One corpus answering never upgrades the other: found stays
        // found, a 429 stays unavailable — never a negative.
        assertEquals(OsintLogic.Grade.HIT, OsintLogic.gradeForStatus(xon.status))
        assertEquals(OsintLogic.Grade.UNAVAILABLE, OsintLogic.gradeForStatus(leak.status))
        // A hit in the breach battery is a possible match.
        assertEquals(
            OsintLogic.POSSIBLE_MATCH_LABEL,
            OsintLogic.confidenceLabel(xon.confidence),
        )
        // The clean-address shape (200 {"Error":"Not found"}) is the
        // server's not_found — the deck renders it as a no-hit.
        val clean = OsintLogic.parseSourceEntry(
            src("""{"source": "XposedOrNot breach index", "status": "not_found",
                   "summary": "No breach records in the XposedOrNot breach index."}"""),
        )
        assertEquals(OsintLogic.Grade.NO_HIT, OsintLogic.gradeForStatus(clean.status))
    }

    // ---- username variations: the server entry's shape -------------------

    @Test
    fun usernameVariationsEntryGradesAsMention() {
        val entry = OsintLogic.parseSourceEntry(
            src(
                """
                {"source": "Username variations", "status": "found",
                 "confidence": "mention",
                 "summary": "Profile page(s) located for 1 variation handle(s) of \"john.doe\" on the checked subset sites. These are different handles from the one searched — a matching handle is a lead, not an identification.",
                 "note": "Checked 9 variation/site pairs (3 variation(s) x 3 subset sites)."}
                """,
            ),
        )
        assertEquals(OsintLogic.Grade.HIT, OsintLogic.gradeForStatus(entry.status))
        assertEquals("Unverified mention", OsintLogic.confidenceLabel(entry.confidence))
        assertTrue(entry.summary!!.contains("not an identification"))
    }
}
