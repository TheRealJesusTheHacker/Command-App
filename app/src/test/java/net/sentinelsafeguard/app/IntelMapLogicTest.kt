package net.sentinelsafeguard.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import net.sentinelsafeguard.app.ui.IntelMapLogic
import net.sentinelsafeguard.app.ui.IntelMapPulse
import net.sentinelsafeguard.app.ui.IntelMapTiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lane C (1.4.0 Expansion B) pins for the Live Intel map logic.
 * Fixtures are the REAL payload shapes: the threat/geo bodies
 * are sentinel-live feeds.py's own output shapes as proxied by
 * Command inside {connected, stale, data}; the flights body is
 * the LIVE_FRESH fixture from Command's tests/test_item8.py
 * (verified against production /api/live-flights), and the
 * lookup result is that suite's SEARCH_FLIGHT.
 */
class IntelMapLogicTest {

    private fun envelope(json: String) = Json.parseToJsonElement(json).jsonObject

    // ------------------------------------------------------------ threat

    private val threatEnvelope = """
        {"authorized": true, "connected": true, "stale": false,
         "data": {
           "updated": "2026-11-07T17:00:00+00:00", "status": "ok",
           "attacks_today": 123456, "sources_today": 789,
           "families": [],
           "countries": [
             {"code": "RU", "count": 400},
             {"code": "SC", "count": 100},
             {"code": "?", "count": 50},
             {"code": "XX", "count": 25}
           ],
           "attack_types": [{"name": "SSH", "count": 900}],
           "markers": [
             {"lat": 55.7, "lon": 37.6, "country": "Russia",
              "ip": "203.0.113.7", "attacks": 100,
              "asname": "EXAMPLE-NET", "types": ["SSH", "Telnet"]},
             {"lat": 55.7, "country": "Nowhere",
              "ip": "203.0.113.8", "attacks": 5,
              "asname": "", "types": []}
           ],
           "arcs": [
             {"from": [61.5, 90.0], "to": [39.8, -98.5],
              "from_cc": "RU", "to_cc": "US", "label": "400 attacks"}
           ],
           "feed": [
             {"ip": "203.0.113.7", "attacks": 100, "country": "RU",
              "asname": "EXAMPLE-NET", "types": "SSH, Telnet"}
           ],
           "error": null
         }}
    """.trimIndent()

    @Test
    fun threatParseKeepsOnlyPlottableMarkers() {
        val model = IntelMapLogic.parseThreat(envelope(threatEnvelope))
        assertTrue(model.connected)
        assertFalse(model.stale)
        assertEquals(123456, model.attacksToday)
        assertEquals(789, model.sourcesToday)
        // The marker without a longitude is dropped, never plotted at 0,0.
        assertEquals(1, model.markers.size)
        val marker = model.markers.single()
        assertEquals("203.0.113.7", marker.ip)
        assertEquals(100, marker.attacks)
        assertEquals(listOf("SSH", "Telnet"), marker.types)
        assertTrue(marker.pulse)
        assertEquals("threat:203.0.113.7", marker.markerId)
        // The detail sheet carries the payload's own fields.
        val lines = marker.detailLines.toMap()
        assertEquals("203.0.113.7", lines["IP"])
        assertEquals("Russia", lines["Country"])
        assertEquals("100", lines["Attacks"])
        assertEquals("EXAMPLE-NET", lines["Network"])
        assertEquals("SSH, Telnet", lines["Attack types"])
    }

    @Test
    fun threatRadiusScalingIsPinnedSqrt() {
        assertEquals(30_000.0, IntelMapLogic.markerRadiusMeters(0), 0.001)
        assertEquals(30_000.0, IntelMapLogic.markerRadiusMeters(-4), 0.001)
        // sqrt(100) = 10 → 30k + 10 × 12k.
        assertEquals(150_000.0, IntelMapLogic.markerRadiusMeters(100), 0.001)
        // Huge counts cap at the pinned maximum.
        assertEquals(450_000.0, IntelMapLogic.markerRadiusMeters(10_000), 0.001)
        // Country volume: the top country fills the max radius…
        assertEquals(900_000.0, IntelMapLogic.countryRadiusMeters(400, 400), 0.001)
        // …a quarter of the volume gets sqrt(1/4) of the span.
        assertEquals(510_000.0, IntelMapLogic.countryRadiusMeters(100, 400), 0.001)
        assertEquals(120_000.0, IntelMapLogic.countryRadiusMeters(0, 400), 0.001)
        assertEquals(120_000.0, IntelMapLogic.countryRadiusMeters(10, 0), 0.001)
    }

    @Test
    fun threatCountryCirclesUseCentroidFallbackAndSkipUnknowns() {
        val model = IntelMapLogic.parseThreat(envelope(threatEnvelope))
        // RU (has a marker) and SC (NO marker — centroid fallback)
        // are plotted; "?" and the unknown code "XX" never are.
        assertEquals(listOf("country:RU", "country:SC"), model.countryCircles.map { it.id })
        val sc = model.countryCircles.first { it.id == "country:SC" }
        assertEquals(-4.6, sc.center.lat, 0.0001)
        assertEquals(55.4, sc.center.lon, 0.0001)
        assertEquals(510_000.0, sc.radiusMeters, 0.001)
        val ru = model.countryCircles.first { it.id == "country:RU" }
        assertEquals(900_000.0, ru.radiusMeters, 0.001)
        assertTrue(model.hasContent)
    }

    @Test
    fun threatArcsAndFeedRowsParse() {
        val model = IntelMapLogic.parseThreat(envelope(threatEnvelope))
        assertEquals(1, model.arcs.size)
        val arc = model.arcs.single()
        assertEquals("arc:RU->US", arc.id)
        assertEquals(61.5, arc.points[0].lat, 0.0001)
        assertEquals(-98.5, arc.points[1].lon, 0.0001)
        assertEquals("400 attacks", arc.label)
        val row = model.feed.single()
        assertEquals("203.0.113.7", row.ip)
        assertEquals("RU", row.countryCode)
        assertEquals("SSH, Telnet", row.typesLabel)
    }

    @Test
    fun threatDisconnectedEnvelopeIsEmptyNotInvented() {
        val model = IntelMapLogic.parseThreat(
            envelope("""{"authorized": true, "connected": false, "stale": false, "data": null}"""),
        )
        assertFalse(model.connected)
        assertFalse(model.hasContent)
        assertTrue(model.markers.isEmpty())
        assertEquals(0, model.attacksToday)
    }

    // ------------------------------------------------------- geopolitics

    private val geoServerEnvelope = """
        {"authorized": true, "connected": true, "stale": true,
         "data": {
           "updated": "2026-11-07T17:05:00+00:00", "status": "ok",
           "items": [
             {"title": "Ceasefire talks resume in Gaza", "link": "https://example.invalid/1",
              "published": "Sat, 07 Nov 2026", "source": "Wire", "impact": "Monitoring."}
           ],
           "hotspots": [
             {"lat": 31.5, "lon": 34.4, "label": "Gaza",
              "title": "Ceasefire talks resume in Gaza"}
           ],
           "error": null
         }}
    """.trimIndent()

    @Test
    fun geoServerHotspotsWinAndUnplacedIsUnknown() {
        val model = IntelMapLogic.parseGeo(envelope(geoServerEnvelope))
        assertTrue(model.stale)
        assertTrue(model.usedServerHotspots)
        assertNull(model.unplacedCount)
        assertEquals(1, model.spots.size)
        assertEquals("Gaza", model.spots.single().label)
        assertEquals("geo:Gaza", model.spots.single().markerId)
        assertEquals(1, model.items.size)
        assertTrue(model.hasContent)
    }

    @Test
    fun gazetteerFirstMatchWinsDedupesAndCountsUnplaced() {
        val items = listOf(
            // "ukraine" precedes "kyiv" in the table → Ukraine wins.
            IntelMapLogic.GeoItem("Ukraine strikes rock Kyiv region", "Wire", "", ""),
            IntelMapLogic.GeoItem("Ceasefire talks in Gaza City", "Wire", "", ""),
            // Same label again: neither a second spot nor unplaced.
            IntelMapLogic.GeoItem("GAZA: second story of the day", "Wire", "", ""),
            // Keyword arrives via the SOURCE, not the headline.
            IntelMapLogic.GeoItem("Aid convoy delayed at border", "Taiwan Bureau", "", ""),
            // Nothing matches: counted, never plotted.
            IntelMapLogic.GeoItem("Markets rally on tech earnings", "Wire", "", ""),
        )
        val (spots, unplaced) = IntelMapLogic.gazetteerSpots(items)
        assertEquals(listOf("Ukraine", "Gaza", "Taiwan"), spots.map { it.label })
        assertEquals(1, unplaced)
        val gaza = spots.first { it.label == "Gaza" }
        assertEquals(31.5, gaza.lat, 0.0001)
        assertEquals(34.4, gaza.lon, 0.0001)
        assertEquals("Ceasefire talks in Gaza City", gaza.headline)
    }

    @Test
    fun geoWithoutServerHotspotsFallsBackToGazetteer() {
        val json = """
            {"authorized": true, "connected": true, "stale": false,
             "data": {"updated": "t", "status": "ok",
               "items": [
                 {"title": "Tensions rise near Jerusalem", "link": "",
                  "published": "", "source": "Wire", "impact": ""},
                 {"title": "Local sports roundup", "link": "",
                  "published": "", "source": "Wire", "impact": ""}
               ],
               "hotspots": [], "error": null}}
        """.trimIndent()
        val model = IntelMapLogic.parseGeo(envelope(json))
        assertFalse(model.usedServerHotspots)
        assertEquals(listOf("Jerusalem"), model.spots.map { it.label })
        assertEquals(1, model.unplacedCount)
    }

    // ------------------------------------------------------------ flights

    /** Verbatim from Command tests/test_item8.py LIVE_FRESH. */
    private val flightsEnvelope = """
        {"authorized": true, "connected": true, "stale": false,
         "data": {
           "aircraft": [
             {"hex": "a0c7b3", "callsign": "TST100", "registration": "N100TA",
              "type": "B763", "lat": 39.05, "lon": -94.61, "altitude_ft": 37025,
              "on_ground": false, "speed_kt": 485.4, "heading": 92.36,
              "vertical_rate_fpm": -64, "squawk": "2473", "emergency": false,
              "category": "A5", "seen_s": 0.3},
             {"hex": "ae0042", "callsign": "TST200", "registration": "N200TA",
              "type": "T38", "lat": 39.20, "lon": -94.50, "altitude_ft": null,
              "on_ground": true, "speed_kt": 0.0, "heading": 180.0,
              "vertical_rate_fpm": 0, "squawk": "7700", "emergency": true,
              "category": "A6", "seen_s": 4.0}
           ],
           "total": 2, "center": {"lat": 39.1, "lon": -94.6}, "dist": 100,
           "fetched_at": "2026-10-25T18:00:00+00:00", "source": "adsb.lol",
           "note": "Live positions from community ADS-B receivers via adsb.lol (ODbL).",
           "age_s": 1.5, "stale": false
         }}
    """.trimIndent()

    @Test
    fun flightsParseMatchesTheRealPayload() {
        val model = IntelMapLogic.parseFlights(envelope(flightsEnvelope))
        assertTrue(model.connected)
        assertFalse(model.stale)
        assertEquals(2, model.total)
        assertEquals(2, model.aircraft.size)
        assertNotNull(model.center)
        assertEquals(39.1, model.center!!.lat, 0.0001)
        assertEquals(-94.6, model.center!!.lon, 0.0001)
        assertEquals(100, model.distNm)
        assertEquals("adsb.lol", model.source)
        assertEquals(1.5, model.ageS!!, 0.0001)

        val cruise = model.aircraft[0]
        assertEquals("TST100", cruise.title)
        assertEquals(37025, cruise.altitudeFt)
        assertFalse(cruise.onGround)
        assertFalse(cruise.emergency)
        assertFalse(cruise.pulse)
        assertEquals("37,025 ft", cruise.detailLines.toMap()["Altitude"])
        assertEquals("485 kt", cruise.detailLines.toMap()["Ground speed"])
        assertEquals("-64 fpm", cruise.detailLines.toMap()["Vertical rate"])

        val ground = model.aircraft[1]
        assertNull(ground.altitudeFt)
        assertTrue(ground.onGround)
        assertTrue(ground.emergency)
        assertTrue(ground.pulse)
        assertEquals("On ground", ground.detailLines.toMap()["Altitude"])
        assertEquals("7700 — EMERGENCY", ground.detailLines.toMap()["Squawk"])
        assertEquals("Level", ground.detailLines.toMap()["Vertical rate"])
        assertTrue(model.hasContent)
    }

    @Test
    fun headingProjectionIsPinnedDeadReckoning() {
        // 60 nm due north is exactly one degree of latitude.
        val north = IntelMapLogic.projectPoint(
            IntelMapLogic.MapPoint(39.0, -94.6), 0.0, 60.0,
        )
        assertEquals(40.0, north.lat, 0.0001)
        assertEquals(-94.6, north.lon, 0.0001)
        // 60 nm due east on the equator is one degree of longitude.
        val east = IntelMapLogic.projectPoint(
            IntelMapLogic.MapPoint(0.0, 0.0), 90.0, 60.0,
        )
        assertEquals(0.0, east.lat, 0.0001)
        assertEquals(1.0, east.lon, 0.0001)
        // West decreases longitude; south decreases latitude.
        val west = IntelMapLogic.projectPoint(
            IntelMapLogic.MapPoint(0.0, 0.0), 270.0, 60.0,
        )
        assertEquals(-1.0, west.lon, 0.0001)
        val south = IntelMapLogic.projectPoint(
            IntelMapLogic.MapPoint(39.0, -94.6), 180.0, 60.0,
        )
        assertEquals(38.0, south.lat, 0.0001)
    }

    @Test
    fun headingVectorsComeFromTheFeedsOwnHeadingAndSpeed() {
        val model = IntelMapLogic.parseFlights(envelope(flightsEnvelope))
        // The ambient feed has NO origin/destination: exactly one
        // vector (the cruising TST100); the on-ground emergency
        // TST200 gets none.
        assertEquals(1, model.headingVectors.size)
        val vector = model.headingVectors.single()
        assertEquals("vector:a0c7b3", vector.id)
        assertEquals(2f, vector.strokeWidth)
        assertEquals(110, vector.alpha)
        // 485.4 kt for 120 s = 16.18 nm on heading 092.36°:
        // east and very slightly south of the reported position.
        val end = vector.points[1]
        assertEquals(39.0389, end.lat, 0.001)
        assertEquals(-94.2633, end.lon, 0.001)
        // A route polyline (from a lookup) keeps the strong style.
        val route = IntelMapLogic.routeForLookup(envelope(lookupResult))!!
        assertEquals(3f, route.polyline.strokeWidth)
        assertEquals(170, route.polyline.alpha)
    }

    @Test
    fun headingVectorIsNullForSlowOrDirectionlessAircraft() {
        val slow = IntelMapLogic.FlightMarker(
            hex = "slow1", callsign = "SLOW1", registration = "", type = "C172",
            altitudeFt = 3000, onGround = false, speedKt = 10.0,
            headingDeg = 90.0, verticalRateFpm = 0, squawk = "1200",
            emergency = false, category = "A1", seenS = 1.0,
            lat = 39.0, lon = -94.6,
        )
        assertNull(IntelMapLogic.headingVectorFor(slow))
        assertNull(IntelMapLogic.headingVectorFor(slow.copy(speedKt = null)))
        assertNull(IntelMapLogic.headingVectorFor(slow.copy(speedKt = 100.0, headingDeg = null)))
        assertNull(IntelMapLogic.headingVectorFor(slow.copy(speedKt = 100.0, onGround = true)))
        assertNotNull(IntelMapLogic.headingVectorFor(slow.copy(speedKt = 100.0)))
    }

    @Test
    fun flightsPayloadStaleFlagFoldsIntoTheModel() {
        val staleJson = flightsEnvelope.replace(
            "\"age_s\": 1.5, \"stale\": false",
            "\"age_s\": 222.0, \"stale\": true",
        )
        val model = IntelMapLogic.parseFlights(envelope(staleJson))
        assertTrue(model.stale)
        assertEquals(222.0, model.ageS!!, 0.0001)
        assertEquals("TST100", model.aircraft[0].callsign)
    }

    /** Verbatim from Command tests/test_item8.py SEARCH_FLIGHT. */
    private val lookupResult = """
        {"number": "AA100", "airline": "Test Air", "status": "active",
         "date": "2026-10-25",
         "departure": {"airport": "Kansas City Intl", "iata": "MCI",
                       "terminal": "B", "gate": "42",
                       "scheduled": "2026-10-25T08:00:00", "estimated": null,
                       "actual": null, "delay": null},
         "arrival": {"airport": "Dallas Fort Worth", "iata": "DFW",
                     "terminal": "A", "gate": "7",
                     "scheduled": "2026-10-25T09:30:00", "estimated": null,
                     "actual": null, "delay": null},
         "aircraft": "N000TA", "live": null}
    """.trimIndent()

    @Test
    fun lookupRouteResolvesBothEndpointsFromTheHubTable() {
        val route = IntelMapLogic.routeForLookup(envelope(lookupResult))
        assertNotNull(route)
        route!!
        assertEquals("MCI", route.fromIata)
        assertEquals("DFW", route.toIata)
        // feeds.AIRPORT_COORDS values, exactly.
        assertEquals(39.2976, route.from.lat, 0.0001)
        assertEquals(-94.7139, route.from.lon, 0.0001)
        assertEquals(32.8998, route.to.lat, 0.0001)
        assertEquals(-97.0403, route.to.lon, 0.0001)
        assertEquals(listOf(route.from, route.to), route.polyline.points)
        assertEquals(2, route.endpointMarkers().size)
        assertEquals("MCI", route.endpointMarkers()[0].title)
    }

    @Test
    fun lookupRouteIsNullWhenAnEndpointIsUnknown() {
        val unknown = lookupResult.replace("\"iata\": \"DFW\"", "\"iata\": \"XXX\"")
        assertNull(IntelMapLogic.routeForLookup(envelope(unknown)))
        assertNull(IntelMapLogic.routeForLookup(null))
    }

    // ------------------------------------------------------- pulse + tiles

    @Test
    fun pulseNumbersArePinned() {
        assertEquals(1500L, IntelMapPulse.PULSE_PERIOD_MS)
        assertEquals(0f, IntelMapPulse.phaseAt(0L), 0.0001f)
        assertEquals(0.5f, IntelMapPulse.phaseAt(750L), 0.0001f)
        assertEquals(0f, IntelMapPulse.phaseAt(1500L), 0.0001f)
        // Negative elapsed wraps into the period, never negative.
        assertTrue(IntelMapPulse.phaseAt(-1L) > 0.99f)
        assertEquals(0.5f, IntelMapPulse.alpha(0f), 0.0001f)
        assertEquals(0.25f, IntelMapPulse.alpha(0.5f), 0.0001f)
        assertEquals(0f, IntelMapPulse.alpha(1f), 0.0001f)
        assertEquals(1.0, IntelMapPulse.radiusFactor(0f), 0.0001)
        assertEquals(1.9, IntelMapPulse.radiusFactor(0.5f), 0.0001)
        assertEquals(2.8, IntelMapPulse.radiusFactor(1f), 0.0001)
        // Out-of-range phases clamp.
        assertEquals(0f, IntelMapPulse.alpha(2f), 0.0001f)
        assertEquals(1.0, IntelMapPulse.radiusFactor(-1f), 0.0001)
    }

    @Test
    fun tileUrlArgumentOrderIsPinnedPerProvider() {
        // Esri World Imagery takes z/y/x — the swapped order is
        // the classic silent failure, so it is pinned here.
        assertEquals(
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/5/12/10",
            IntelMapTiles.esriUrl(zoom = 5, x = 10, y = 12),
        )
        // CARTO dark_matter takes z/x/y.
        assertEquals(
            "https://basemaps.cartocdn.com/dark_all/5/10/12.png",
            IntelMapTiles.cartoUrl(zoom = 5, x = 10, y = 12),
        )
    }
}
