package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.ui.OsintLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lane E pins for the OSINT deck logic: variant generation,
 * deck grouping, grade mapping, dork substitution, coordinate
 * readouts, severity ranking, and EXIF shaping. The honesty
 * rails are pinned too: the possible-match and no-records
 * labels are the server's verbatim strings.
 */
class OsintLogicTest {

    // ---- labels -------------------------------------------------------

    @Test
    fun possibleMatchLabelIsTheServersVerbatim() {
        assertEquals(
            "POSSIBLE MATCH — identity not verified",
            OsintLogic.POSSIBLE_MATCH_LABEL,
        )
        assertEquals(
            "No public records located in the sources searched",
            OsintLogic.NO_RECORDS_LABEL,
        )
        assertTrue(OsintLogic.REPORT_FOOTER.contains("Not a consumer report under the FCRA"))
        assertTrue(OsintLogic.CRIMINAL_FOOTER.contains("not an FCRA consumer report"))
    }

    // ---- deck grouping --------------------------------------------------

    @Test
    fun everyServerTypeHasADedicatedTool() {
        assertEquals(
            listOf(
                "name", "username", "email", "phone", "address",
                "domain", "ip", "plate", "vin", "tail", "callsign",
                "subdomain", "dns", "site", "archive", "btc", "eth", "breach",
            ),
            OsintLogic.SERVER_TYPES,
        )
        for (type in OsintLogic.SERVER_TYPES) {
            assertTrue("no dedicated tool for $type", OsintLogic.typeHasDedicatedTool(type))
        }
    }

    @Test
    fun deckGroupsCoverEveryToolExactlyOnce() {
        val grouped = OsintLogic.GROUPS.flatMap { OsintLogic.toolsInGroup(it) }
        assertEquals(OsintLogic.TOOLS.size, grouped.size)
        assertEquals(OsintLogic.TOOLS.map { it.id }.toSet(), grouped.map { it.id }.toSet())
        // The brief's five groups all exist; on-device is additive.
        val titles = OsintLogic.GROUPS.map { it.title }
        for (want in listOf("Identity", "Network", "Vehicles & Air", "Background", "Breach")) {
            assertTrue(want in titles)
        }
        // Criminal is the Background group's server-kind tool.
        val criminal = OsintLogic.tool("criminal")!!
        assertEquals("criminal", criminal.kind)
        assertEquals(OsintLogic.Group.BACKGROUND, criminal.group)
        // The people-search pack is labeled manual in the model.
        assertTrue(OsintLogic.tool("peopleLinks")!!.manual)
        // EXIF is on-device and carries no server type.
        val exif = OsintLogic.tool("exif")!!
        assertNull(exif.typeKey)
        assertEquals(OsintLogic.Group.ON_DEVICE, exif.group)
    }

    // ---- grades -----------------------------------------------------------

    @Test
    fun gradeMappingMatchesServerStatuses() {
        assertEquals(OsintLogic.Grade.HIT, OsintLogic.gradeForStatus("found"))
        assertEquals(OsintLogic.Grade.NO_HIT, OsintLogic.gradeForStatus("not_found"))
        assertEquals(OsintLogic.Grade.UNAVAILABLE, OsintLogic.gradeForStatus("unavailable"))
        assertEquals(OsintLogic.Grade.MANUAL, OsintLogic.gradeForStatus("manual"))
    }

    @Test
    fun unknownStatusIsNeverANegative() {
        assertEquals(OsintLogic.Grade.UNAVAILABLE, OsintLogic.gradeForStatus("error"))
        assertEquals(OsintLogic.Grade.UNAVAILABLE, OsintLogic.gradeForStatus(""))
        assertEquals(OsintLogic.Grade.UNAVAILABLE, OsintLogic.gradeForStatus(null))
        assertEquals(OsintLogic.Grade.UNAVAILABLE, OsintLogic.gradeForStatus("weird"))
        // Case/space tolerance, like the server's own normalization.
        assertEquals(OsintLogic.Grade.HIT, OsintLogic.gradeForStatus(" Found "))
    }

    @Test
    fun confidenceLabelsMirrorTheServerVocabulary() {
        assertEquals("Confirmed public record", OsintLogic.confidenceLabel("record"))
        assertEquals("Unverified mention", OsintLogic.confidenceLabel("mention"))
        assertEquals(OsintLogic.POSSIBLE_MATCH_LABEL, OsintLogic.confidenceLabel("possible"))
        assertEquals("Reference", OsintLogic.confidenceLabel("info"))
        assertTrue(OsintLogic.carriesPossibleMatch("possible", emptyList()))
        assertTrue(
            OsintLogic.carriesPossibleMatch(
                "record",
                listOf("POSSIBLE MATCH — identity not verified. Matched on: name"),
            ),
        )
        assertFalse(OsintLogic.carriesPossibleMatch("record", listOf("ordinary finding")))
    }

    // ---- username variants --------------------------------------------------

    @Test
    fun variantsStartWithTheHandleAndStayValid() {
        val variants = OsintLogic.usernameVariants("Skyler.Dog")
        assertEquals("skyler.dog", variants.first())
        assertTrue("skylerdog" in variants)
        assertTrue("skyler_dog" in variants)
        assertTrue("sdog" in variants)
        assertTrue("skyler.dog1" in variants)
        assertEquals(variants.size, variants.toSet().size)
        assertTrue(variants.size <= 12)
        for (v in variants) {
            assertFalse(v.startsWith("."))
            assertFalse(v.endsWith("."))
            assertTrue(v.length in 2..40)
        }
    }

    @Test
    fun variantsStripAtSignAndRejectBlanks() {
        assertEquals("sentinel", OsintLogic.usernameVariants("@Sentinel").first())
        assertTrue(OsintLogic.usernameVariants("").isEmpty())
        assertTrue(OsintLogic.usernameVariants("  ").isEmpty())
        assertTrue(OsintLogic.usernameVariants("a").isEmpty())
    }

    @Test
    fun variantsRespectTheCap() {
        assertEquals(3, OsintLogic.usernameVariants("john.doe.smith", max = 3).size)
        assertEquals(1, OsintLogic.usernameVariants("john.doe.smith", max = 1).size)
    }

    // ---- dorks ---------------------------------------------------------------

    @Test
    fun dorkFillMirrorsTheServerSubstitution() {
        val out = OsintLogic.fillDorks(
            listOf("\"{v}\"", "\"{v}\" {city} {state}", "site:{v} admin"),
            "example.com", city = "Kansas City", state = "MO",
        )
        assertEquals(listOf("\"example.com\"", "\"example.com\" Kansas City MO", "site:example.com admin"), out)
        // Missing context collapses whitespace, like the server.
        assertEquals(
            listOf("\"x\""),
            OsintLogic.fillDorks(listOf("\"{v}\" {city}  {state}"), "x"),
        )
    }

    // ---- coordinates ------------------------------------------------------------

    @Test
    fun coordinatesExtractAndReadOut() {
        val pair = OsintLogic.extractCoordinates("near 38.8977, -77.0365 today")
        assertEquals(38.8977, pair!!.first, 0.0001)
        assertEquals(-77.0365, pair.second, 0.0001)
        assertEquals("LAT 38.897700 · LON -77.036500", OsintLogic.coordinateReadout(38.8977, -77.0365))
        assertNull(OsintLogic.extractCoordinates("no numbers here"))
        assertNull(OsintLogic.extractCoordinates("999.1234, 999.1234"))
        assertNull(OsintLogic.extractCoordinates(null))
    }

    // ---- breach severity -------------------------------------------------------------

    @Test
    fun severityRanksInServerBandOrder() {
        assertTrue(OsintLogic.severityRank("Critical") > OsintLogic.severityRank("High"))
        assertTrue(OsintLogic.severityRank("High") > OsintLogic.severityRank("Medium"))
        assertTrue(OsintLogic.severityRank("Medium") > OsintLogic.severityRank("Low"))
        assertEquals(0, OsintLogic.severityRank(null))
        assertEquals("low", OsintLogic.severityKey("nonsense"))
        assertEquals("critical", OsintLogic.severityKey(" Critical "))
    }

    // ---- EXIF shaping -------------------------------------------------------------------

    @Test
    fun exifRowsOrderKnownTagsAndKeepTheRest() {
        val rows = OsintLogic.exifRows(
            mapOf(
                "Model" to "Pixel 9",
                "Make" to "Google",
                "CustomTag" to "kept",
                "Software" to "  ",
            ),
        )
        assertEquals(listOf("Camera make", "Camera model", "CustomTag"), rows.map { it.label })
        assertEquals("Google", rows[0].value)
        assertTrue(OsintLogic.exifRows(emptyMap()).isEmpty())
    }

    @Test
    fun dmsParsesAndSignsByHemisphere() {
        val north = OsintLogic.dmsToDecimal("38/1,53/1,5160/100", "N")
        assertEquals(38.0 + 53.0 / 60.0 + 51.6 / 3600.0, north!!, 0.000001)
        val west = OsintLogic.dmsToDecimal("77/1,2/1,1140/100", "W")
        assertTrue(west!! < 0)
        assertNull(OsintLogic.dmsToDecimal("garbage", "N"))
        assertNull(OsintLogic.dmsToDecimal("1/0,2/1,3/1", "N"))
        assertNull(OsintLogic.dmsToDecimal(null, "N"))
    }
}
