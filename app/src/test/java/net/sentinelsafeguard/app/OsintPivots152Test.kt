package net.sentinelsafeguard.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.sentinelsafeguard.app.ui.OsintLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 1.5.2 (Elevation F3) pins for OSINT pivot actions. Fixtures
 * are the contract's own shapes (LANE-FEAT-F3F4.md relay
 * block + test_elev_f3_pivots.py): search objects and full
 * reports gain `pivots` ([{fromType, value, type, label}])
 * and `parentSearchId`; running a pivot POSTs
 * {parentSearchId, identifiers: [{type, value}]} with NO
 * purpose — the server carries the parent run's verbatim.
 *
 * Honesty rails pinned here:
 *  - offers are the server's: nothing parses into an offer
 *    the payload did not carry, malformed entries drop;
 *  - the run body can never grow a purpose key.
 */
class OsintPivots152Test {

    private fun obj(json: String) = Json.parseToJsonElement(json).jsonObject

    // A completed ip search, as GET /api/app/osint returns it.
    private val ipSearch = """
        {"id": 41, "kind": "identifier", "status": "done",
         "subject": "IP 203.0.113.7", "purpose": "Case #7 - pivot proof",
         "parentSearchId": null, "reportId": 9,
         "pivots": [
           {"fromType": "ip", "value": "203.0.113.7",
            "type": "site", "label": "Site inspector"},
           {"fromType": "ip", "value": "203.0.113.7",
            "type": "subdomain", "label": "Subdomains"}
         ]}
    """.trimIndent()

    @Test
    fun parsePivotsReadsTheServerOffersVerbatim() {
        val pivots = OsintLogic.parsePivots(obj(ipSearch))
        assertEquals(2, pivots.size)
        assertEquals("ip", pivots[0].fromType)
        assertEquals("site", pivots[0].type)
        assertEquals("203.0.113.7", pivots[0].value)
        assertEquals("Site inspector", pivots[0].label)
        assertEquals("subdomain", pivots[1].type)
        assertEquals(
            "Pivot: run Site inspector on 203.0.113.7",
            OsintLogic.pivotActionLabel(pivots[0]),
        )
    }

    @Test
    fun emailPivotsCarryVerbatimAndLocalPartTargets() {
        // The server pre-fills both targets: breach carries the
        // address verbatim; username carries its local part.
        val search = obj(
            """
            {"id": 42, "kind": "identifier", "status": "done",
             "pivots": [
               {"fromType": "email", "value": "pat@example.com",
                "type": "breach", "label": "Email breach check"},
               {"fromType": "email", "value": "pat",
                "type": "username", "label": "Username"}
             ]}
            """.trimIndent(),
        )
        val pivots = OsintLogic.parsePivots(search)
        assertEquals(
            listOf("breach" to "pat@example.com", "username" to "pat"),
            pivots.map { it.type to it.value },
        )
    }

    @Test
    fun noOffersParseToNoActions() {
        // Unmapped types and criminal searches carry pivots: []
        // server-side; older payloads may omit the field. Both
        // must yield zero buttons, never a derived offer.
        assertTrue(OsintLogic.parsePivots(obj("""{"id": 1, "pivots": []}""")).isEmpty())
        assertTrue(OsintLogic.parsePivots(obj("""{"id": 2}""")).isEmpty())
        assertTrue(OsintLogic.parsePivots(null).isEmpty())
    }

    @Test
    fun malformedOffersDropAndValidOnesSurvive() {
        val search = obj(
            """
            {"pivots": [
               {"fromType": "domain", "type": "dns", "label": "DNS explorer"},
               {"fromType": "domain", "value": "example.com",
                "type": "archive", "label": "Wayback Machine"}
             ]}
            """.trimIndent(),
        )
        val pivots = OsintLogic.parsePivots(search)
        assertEquals(1, pivots.size)
        assertEquals("archive", pivots[0].type)
    }

    @Test
    fun pivotRunBodyHasLineageAndNoPurpose() {
        val pivot = OsintLogic.parsePivots(obj(ipSearch))[0]
        val body = OsintLogic.pivotRunBody(41, pivot)
        assertEquals(setOf("parentSearchId", "identifiers"), body.keys)
        assertFalse("a pivot body must never carry a purpose",
            body.containsKey("purpose"))
        assertEquals(41, body["parentSearchId"]!!.jsonPrimitive.int)
        val identifiers = body["identifiers"]!!.jsonArray
        assertEquals(1, identifiers.size)
        val only = identifiers[0].jsonObject
        assertEquals(setOf("type", "value"), only.keys)
        assertEquals("site", only["type"]!!.jsonPrimitive.content)
        assertEquals("203.0.113.7", only["value"]!!.jsonPrimitive.content)
    }

    @Test
    fun reportLevelPivotsParseFromTheReportObject() {
        // GET /api/app/osint/reports/{id}: the full report
        // itself gains purpose + parentSearchId + pivots.
        val report = obj(
            """
            {"id": 9, "searchId": 41, "purpose": "Case #7 - pivot proof",
             "parentSearchId": null,
             "pivots": [
               {"fromType": "domain", "value": "example.com",
                "type": "dns", "label": "DNS explorer"}
             ]}
            """.trimIndent(),
        )
        val pivots = OsintLogic.parsePivots(report)
        assertEquals(1, pivots.size)
        assertEquals("dns", pivots[0].type)
        assertEquals("example.com", pivots[0].value)
    }

    @Test
    fun childReportLineageReadsAsParentSearchId() {
        // A pivot child report carries parentSearchId = the
        // parent search's id (lineage), which the view renders.
        val child = obj(
            """
            {"id": 12, "searchId": 43, "parentSearchId": 41,
             "purpose": "Case #7 - pivot proof", "pivots": []}
            """.trimIndent(),
        )
        assertEquals(
            41,
            net.sentinelsafeguard.app.ui.Phase3Logic.int(child, "parentSearchId"),
        )
    }
}
