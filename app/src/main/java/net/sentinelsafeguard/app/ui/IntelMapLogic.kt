package net.sentinelsafeguard.app.ui

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Live Intel map models + parsers (Lane C, Sentinel App 1.4.0 —
 * Expansion B). Pure logic, no Android/osmdroid types, so the
 * unit suite pins every number against the REAL payload shapes:
 *
 *  - Threat: Command `getthreatdata` proxies sentinel-live
 *    `/api/threat-data` verbatim inside `{connected, stale,
 *    data}`. data = {updated, status, attacks_today,
 *    sources_today, families, countries:[{code,count}],
 *    attack_types:[{name,count}], markers:[{lat, lon, country,
 *    ip, attacks, asname, types[]}], arcs:[{from:[lat,lon],
 *    to:[lat,lon], from_cc, to_cc, label}], feed:[{ip, attacks,
 *    country, asname, types}], error} (feeds.fetch_threat).
 *  - Geopolitics: `getgeodata` → `/api/geo-data`: data =
 *    {updated, status, items:[{title, link, published, source,
 *    impact}], hotspots:[{lat, lon, label, title}], error}
 *    (feeds.fetch_geopolitical). The server already computes
 *    hotspots with its GEO_KEYWORDS table; the client gazetteer
 *    below is the FALLBACK for payloads without hotspots,
 *    mirroring the web heuristic (see [GAZETTEER]).
 *  - Flights: `getliveflights` → `/api/live-flights`: data =
 *    {aircraft:[{hex, callsign, registration, type, lat, lon,
 *    altitude_ft (int|null), on_ground, speed_kt, heading,
 *    vertical_rate_fpm, squawk, emergency, category, seen_s}],
 *    total, center:{lat,lon}, dist, fetched_at, source, note,
 *    age_s, stale} (LIVE-FLIGHTS-2026-10-25, adsb.lol via
 *    sentinel-live). The by-number lookup (`getflightlookup`)
 *    result = {number, airline, status, date, departure:
 *    {airport, iata, ...}, arrival:{...}, aircraft} — route
 *    endpoints resolve through [AIRPORT_COORDS], the web
 *    flights page's own hub table (feeds.AIRPORT_COORDS).
 */
object IntelMapLogic {

    // ------------------------------------------------------------ geometry

    data class MapPoint(val lat: Double, val lon: Double)

    /** The never-blank default viewport: whole world. */
    val WORLD_CENTER = MapPoint(20.0, 0.0)
    const val WORLD_ZOOM: Double = 2.0

    /** Kansas City — the flights feed's own default center. */
    val KC_CENTER = MapPoint(39.1, -94.6)
    const val FLIGHTS_ZOOM: Double = 8.0

    /** One plotted point on a Sentinel map. [detailLines] is the
     *  HUD readout the detail sheet shows on tap. */
    interface MapMarkerModel {
        val markerId: String
        val lat: Double
        val lon: Double
        val title: String
        val detailLines: List<Pair<String, String>>
        val pulse: Boolean
        val emergency: Boolean

        /** Base radius (meters) the pulse ring expands from. */
        val pulseBaseRadiusMeters: Double
    }

    data class MapCircleModel(
        val id: String,
        val center: MapPoint,
        val radiusMeters: Double,
        val label: String,
    )

    data class MapPolylineModel(
        val id: String,
        val points: List<MapPoint>,
        val label: String?,
        /** Stroke in px + paint alpha — routes draw strong,
         *  heading vectors draw faint (see [headingVectorFor]). */
        val strokeWidth: Float = 3f,
        val alpha: Int = 170,
    )

    /** A single caller-shaped point — e.g. an OSINT IP-intelligence
     *  result whose payload carries coordinates (1.5.0). The caller
     *  supplies coordinates only when a payload actually carries
     *  them; this model never derives a position. */
    data class SimpleMarker(
        override val markerId: String,
        override val lat: Double,
        override val lon: Double,
        override val title: String,
        override val detailLines: List<Pair<String, String>> = emptyList(),
        override val pulse: Boolean = false,
        override val emergency: Boolean = false,
        override val pulseBaseRadiusMeters: Double = 20_000.0,
    ) : MapMarkerModel

    // ------------------------------------------------------- pinned numbers

    /** Per-attacker marker ground circle: base + sqrt scaling. */
    const val MARKER_BASE_RADIUS_M: Double = 30_000.0
    const val MARKER_RADIUS_PER_SQRT_ATTACK: Double = 12_000.0
    const val MARKER_RADIUS_MAX_M: Double = 450_000.0

    /** Per-country volume circle: sqrt share of the top country. */
    const val COUNTRY_RADIUS_MIN_M: Double = 120_000.0
    const val COUNTRY_RADIUS_MAX_M: Double = 900_000.0

    /** Geopolitics hotspot pulse base (a region, not a point). */
    const val GEO_PULSE_BASE_M: Double = 60_000.0

    /** Emergency-aircraft pulse base. */
    const val EMERGENCY_PULSE_BASE_M: Double = 15_000.0

    fun markerRadiusMeters(attacks: Int): Double {
        if (attacks <= 0) return MARKER_BASE_RADIUS_M
        return (MARKER_BASE_RADIUS_M +
            sqrt(attacks.toDouble()) * MARKER_RADIUS_PER_SQRT_ATTACK)
            .coerceAtMost(MARKER_RADIUS_MAX_M)
    }

    fun countryRadiusMeters(count: Int, maxCount: Int): Double {
        if (count <= 0 || maxCount <= 0) return COUNTRY_RADIUS_MIN_M
        val share = sqrt(count.toDouble() / maxCount.toDouble())
        return COUNTRY_RADIUS_MIN_M +
            (COUNTRY_RADIUS_MAX_M - COUNTRY_RADIUS_MIN_M) * share
    }

    /**
     * Country centroids — the web threat feed's own fallback
     * table (sentinel-live feeds.COUNTRY_COORDS), mirrored so a
     * top attacking country with no geolocated marker still
     * lands on the map at its centroid with a volume circle.
     */
    val COUNTRY_CENTROIDS: Map<String, MapPoint> = mapOf(
        "US" to MapPoint(39.8, -98.5), "CA" to MapPoint(56.1, -106.3),
        "MX" to MapPoint(23.6, -102.5), "BR" to MapPoint(-14.2, -51.9),
        "AR" to MapPoint(-38.4, -63.6), "CL" to MapPoint(-35.6, -71.5),
        "GB" to MapPoint(54.0, -2.0), "IE" to MapPoint(53.1, -8.2),
        "FR" to MapPoint(46.2, 2.2), "DE" to MapPoint(51.1, 10.4),
        "NL" to MapPoint(52.1, 5.3), "BE" to MapPoint(50.6, 4.6),
        "ES" to MapPoint(40.4, -3.7), "PT" to MapPoint(39.5, -8.0),
        "IT" to MapPoint(41.8, 12.5), "CH" to MapPoint(46.8, 8.2),
        "AT" to MapPoint(47.5, 14.5), "SE" to MapPoint(60.1, 18.6),
        "NO" to MapPoint(64.4, 11.5), "FI" to MapPoint(61.9, 25.7),
        "DK" to MapPoint(56.2, 10.0), "PL" to MapPoint(51.9, 19.1),
        "CZ" to MapPoint(49.8, 15.4), "SK" to MapPoint(48.6, 19.7),
        "HU" to MapPoint(47.1, 19.5), "RO" to MapPoint(45.9, 25.0),
        "BG" to MapPoint(42.7, 25.4), "GR" to MapPoint(39.0, 22.0),
        "HR" to MapPoint(45.1, 15.2), "RS" to MapPoint(44.0, 20.8),
        "UA" to MapPoint(48.3, 31.1), "BY" to MapPoint(53.7, 27.9),
        "MD" to MapPoint(47.4, 28.3), "RU" to MapPoint(61.5, 90.0),
        "KZ" to MapPoint(48.0, 66.9), "TR" to MapPoint(38.9, 35.2),
        "IL" to MapPoint(31.0, 34.8), "AE" to MapPoint(24.0, 54.0),
        "SA" to MapPoint(24.6, 46.6), "IR" to MapPoint(32.4, 53.6),
        "IQ" to MapPoint(33.0, 43.6), "EG" to MapPoint(26.8, 30.8),
        "ZA" to MapPoint(-30.5, 22.9), "NG" to MapPoint(9.0, 8.6),
        "KE" to MapPoint(0.0, 37.8), "IN" to MapPoint(21.0, 78.0),
        "PK" to MapPoint(30.3, 69.3), "BD" to MapPoint(23.6, 90.3),
        "CN" to MapPoint(35.8, 104.1), "HK" to MapPoint(22.3, 114.1),
        "TW" to MapPoint(23.6, 121.0), "JP" to MapPoint(36.2, 138.2),
        "KR" to MapPoint(35.9, 127.7), "KP" to MapPoint(40.3, 127.5),
        "MN" to MapPoint(46.8, 103.8), "TH" to MapPoint(15.8, 101.0),
        "VN" to MapPoint(14.0, 108.2), "MY" to MapPoint(4.2, 102.2),
        "SG" to MapPoint(1.3, 103.8), "ID" to MapPoint(-2.5, 118.0),
        "PH" to MapPoint(12.8, 121.7), "AU" to MapPoint(-25.2, 133.7),
        "NZ" to MapPoint(-40.9, 174.8), "PA" to MapPoint(8.5, -80.7),
        "VG" to MapPoint(18.4, -64.6), "SC" to MapPoint(-4.6, 55.4),
        "MU" to MapPoint(-20.3, 57.5), "IS" to MapPoint(64.9, -19.0),
        "EE" to MapPoint(58.5, 25.0), "LV" to MapPoint(56.9, 25.0),
        "LT" to MapPoint(55.1, 23.9), "GE" to MapPoint(42.3, 43.7),
        "AM" to MapPoint(40.0, 45.0), "AZ" to MapPoint(40.1, 47.5),
    )

    // ------------------------------------------------------------- threat

    data class ThreatMarker(
        val ip: String,
        val country: String,
        val attacks: Int,
        val asname: String,
        val types: List<String>,
        override val lat: Double,
        override val lon: Double,
    ) : MapMarkerModel {
        override val markerId: String get() = "threat:$ip"
        override val title: String get() = ip
        override val pulse: Boolean get() = true
        override val emergency: Boolean get() = false
        override val pulseBaseRadiusMeters: Double
            get() = markerRadiusMeters(attacks)
        override val detailLines: List<Pair<String, String>>
            get() = listOfNotNull(
                "IP" to ip,
                "Country" to country.takeIf { it.isNotBlank() },
                "Attacks" to String.format(Locale.US, "%,d", attacks),
                "Network" to asname.takeIf { it.isNotBlank() },
                "Attack types" to types.joinToString(", ").takeIf { it.isNotBlank() },
                "Position" to formatLatLon(lat, lon),
            ).map { (k, v) -> k to (v ?: "") }.filter { it.second.isNotBlank() }
    }

    data class ThreatFeedRow(
        val ip: String,
        val attacks: Int,
        val countryCode: String,
        val asname: String,
        val typesLabel: String,
    )

    data class ThreatMapModel(
        val connected: Boolean,
        val stale: Boolean,
        val updated: String?,
        val status: String?,
        val error: String?,
        val attacksToday: Int,
        val sourcesToday: Int,
        val markers: List<ThreatMarker>,
        val countryCircles: List<MapCircleModel>,
        val arcs: List<MapPolylineModel>,
        val feed: List<ThreatFeedRow>,
    ) {
        val hasContent: Boolean
            get() = markers.isNotEmpty() || countryCircles.isNotEmpty()
    }

    fun parseThreat(envelope: JsonObject): ThreatMapModel {
        val connected = boolOf(envelope, "connected") ?: false
        val stale = boolOf(envelope, "stale") ?: false
        val data = envelope["data"] as? JsonObject
            ?: return ThreatMapModel(
                connected, stale, null, null, null, 0, 0,
                emptyList(), emptyList(), emptyList(), emptyList(),
            )
        val markers = arrOf(data, "markers").mapNotNull { m ->
            val lat = dblOf(m, "lat") ?: return@mapNotNull null
            val lon = dblOf(m, "lon") ?: return@mapNotNull null
            ThreatMarker(
                ip = strOf(m, "ip"),
                country = strOf(m, "country"),
                attacks = intOf(m, "attacks") ?: 0,
                asname = strOf(m, "asname"),
                types = strListOf(m, "types"),
                lat = lat, lon = lon,
            )
        }
        val countries = arrOf(data, "countries").mapNotNull { c ->
            val code = strOf(c, "code").uppercase(Locale.US)
            val count = intOf(c, "count") ?: 0
            if (code.isBlank() || code == "?") null else code to count
        }
        val maxCount = countries.maxOfOrNull { it.second } ?: 0
        // Every top country with a known centroid gets a volume
        // circle — this is also the fallback that keeps a
        // country with NO geolocated markers on the map.
        val circles = countries.mapNotNull { (code, count) ->
            val centroid = COUNTRY_CENTROIDS[code] ?: return@mapNotNull null
            MapCircleModel(
                id = "country:$code",
                center = centroid,
                radiusMeters = countryRadiusMeters(count, maxCount),
                label = "$code · ${String.format(Locale.US, "%,d", count)} attacks",
            )
        }
        val arcs = arrOf(data, "arcs").mapNotNull { a ->
            val from = pointOf(a["from"]) ?: return@mapNotNull null
            val to = pointOf(a["to"]) ?: return@mapNotNull null
            MapPolylineModel(
                id = "arc:${strOf(a, "from_cc")}->${strOf(a, "to_cc")}",
                points = listOf(from, to),
                label = strOf(a, "label").takeIf { it.isNotBlank() },
            )
        }
        val feed = arrOf(data, "feed").map { f ->
            ThreatFeedRow(
                ip = strOf(f, "ip"),
                attacks = intOf(f, "attacks") ?: 0,
                countryCode = strOf(f, "country"),
                asname = strOf(f, "asname"),
                typesLabel = strOf(f, "types"),
            )
        }
        return ThreatMapModel(
            connected = connected,
            stale = stale,
            updated = strOf(data, "updated").takeIf { it.isNotBlank() },
            status = strOf(data, "status").takeIf { it.isNotBlank() },
            error = strOf(data, "error").takeIf { it.isNotBlank() },
            attacksToday = intOf(data, "attacks_today") ?: 0,
            sourcesToday = intOf(data, "sources_today") ?: 0,
            markers = markers,
            countryCircles = circles,
            arcs = arcs,
            feed = feed,
        )
    }

    // -------------------------------------------------------- geopolitics

    data class GeoItem(
        val title: String,
        val source: String,
        val published: String,
        val impact: String,
    )

    data class GeoSpot(
        val label: String,
        val headline: String,
        override val lat: Double,
        override val lon: Double,
    ) : MapMarkerModel {
        override val markerId: String get() = "geo:$label"
        override val title: String get() = label
        override val pulse: Boolean get() = true
        override val emergency: Boolean get() = false
        override val pulseBaseRadiusMeters: Double get() = GEO_PULSE_BASE_M
        override val detailLines: List<Pair<String, String>>
            get() = listOfNotNull(
                "Hotspot" to label,
                "Headline" to headline.takeIf { it.isNotBlank() },
                "Placed by" to "headline location (gazetteer heuristic)",
                "Position" to formatLatLon(lat, lon),
            ).map { (k, v) -> k to (v ?: "") }.filter { it.second.isNotBlank() }
    }

    data class GeoMapModel(
        val connected: Boolean,
        val stale: Boolean,
        val updated: String?,
        val status: String?,
        val error: String?,
        val spots: List<GeoSpot>,
        val items: List<GeoItem>,
        /** Headlines no gazetteer keyword matched — counted, never
         *  plotted. Null when the server supplied the hotspots
         *  (its own matching is not visible to the client). */
        val unplacedCount: Int?,
        val usedServerHotspots: Boolean,
    ) {
        val hasContent: Boolean get() = spots.isNotEmpty() || items.isNotEmpty()
    }

    /**
     * Gazetteer heuristic — a curated keyword → coordinates
     * table, mirroring the web feed's GEO_KEYWORDS (sentinel-live
     * feeds.py), extended with the seats the web table lacks
     * (Jerusalem, Iraq/Baghdad, India, Philippines, Guatemala,
     * Korea). Matching is a case-insensitive `contains` over the
     * headline + source, FIRST table entry wins per headline,
     * labels dedupe across headlines (the web's exact rules).
     * Unmatched headlines are NOT plotted — they are counted.
     */
    val GAZETTEER: List<Triple<String, MapPoint, String>> = listOf(
        Triple("ukraine", MapPoint(48.3, 31.1), "Ukraine"),
        Triple("kyiv", MapPoint(50.4, 30.5), "Kyiv"),
        Triple("russia", MapPoint(61.5, 90.0), "Russia"),
        Triple("moscow", MapPoint(55.7, 37.6), "Moscow"),
        Triple("kremlin", MapPoint(55.7, 37.6), "Moscow"),
        Triple("gaza", MapPoint(31.5, 34.4), "Gaza"),
        Triple("israel", MapPoint(31.0, 34.8), "Israel"),
        Triple("tel aviv", MapPoint(32.0, 34.7), "Tel Aviv"),
        Triple("palestine", MapPoint(31.9, 35.2), "West Bank"),
        Triple("lebanon", MapPoint(33.8, 35.8), "Lebanon"),
        Triple("beirut", MapPoint(33.8, 35.5), "Beirut"),
        Triple("iran", MapPoint(32.4, 53.6), "Iran"),
        Triple("tehran", MapPoint(35.6, 51.4), "Tehran"),
        Triple("syria", MapPoint(34.8, 38.0), "Syria"),
        Triple("damascus", MapPoint(33.5, 36.2), "Damascus"),
        Triple("yemen", MapPoint(15.5, 48.5), "Yemen"),
        Triple("red sea", MapPoint(20.0, 38.5), "Red Sea"),
        Triple("taiwan", MapPoint(23.6, 121.0), "Taiwan"),
        Triple("taipei", MapPoint(25.0, 121.5), "Taipei"),
        Triple("beijing", MapPoint(39.9, 116.4), "Beijing"),
        Triple("south china sea", MapPoint(12.0, 114.0), "South China Sea"),
        Triple("north korea", MapPoint(40.3, 127.5), "North Korea"),
        Triple("pyongyang", MapPoint(39.0, 125.7), "Pyongyang"),
        Triple("seoul", MapPoint(37.5, 127.0), "Seoul"),
        Triple("sudan", MapPoint(12.8, 30.2), "Sudan"),
        Triple("khartoum", MapPoint(15.5, 32.5), "Khartoum"),
        Triple("myanmar", MapPoint(21.9, 95.9), "Myanmar"),
        Triple("sahel", MapPoint(15.0, 0.0), "Sahel"),
        Triple("niger", MapPoint(17.6, 8.0), "Niger"),
        Triple("afghanistan", MapPoint(33.9, 67.7), "Afghanistan"),
        Triple("kabul", MapPoint(34.5, 69.1), "Kabul"),
        Triple("pakistan", MapPoint(30.3, 69.3), "Pakistan"),
        Triple("kashmir", MapPoint(34.0, 74.8), "Kashmir"),
        Triple("libya", MapPoint(28.0, 17.0), "Libya"),
        Triple("somalia", MapPoint(5.1, 46.1), "Somalia"),
        Triple("ethiopia", MapPoint(9.1, 40.4), "Ethiopia"),
        Triple("venezuela", MapPoint(6.4, -66.5), "Venezuela"),
        Triple("cuba", MapPoint(21.5, -79.5), "Cuba"),
        Triple("belarus", MapPoint(53.7, 27.9), "Belarus"),
        Triple("moldova", MapPoint(47.4, 28.3), "Moldova"),
        Triple("armenia", MapPoint(40.0, 45.0), "Armenia"),
        Triple("azerbaijan", MapPoint(40.1, 47.5), "Azerbaijan"),
        Triple("kosovo", MapPoint(42.6, 21.0), "Kosovo"),
        Triple("serbia", MapPoint(44.0, 20.8), "Serbia"),
        Triple("nato", MapPoint(50.8, 4.4), "Brussels (NATO)"),
        Triple("brussels", MapPoint(50.8, 4.4), "Brussels"),
        Triple("white house", MapPoint(38.9, -77.0), "Washington, D.C."),
        Triple("pentagon", MapPoint(38.8, -77.0), "Pentagon"),
        Triple("united nations", MapPoint(40.7, -74.0), "United Nations"),
        Triple("horn of africa", MapPoint(5.0, 46.0), "Horn of Africa"),
        Triple("arctic", MapPoint(75.0, -40.0), "Arctic"),
        Triple("brics", MapPoint(55.7, 37.6), "BRICS"),
        Triple("g7", MapPoint(50.8, 4.4), "G7"),
        Triple("g20", MapPoint(28.6, 77.2), "G20"),
        // Seats the web table lacks (brief's gazetteer list).
        Triple("jerusalem", MapPoint(31.8, 35.2), "Jerusalem"),
        Triple("iraq", MapPoint(33.0, 43.6), "Iraq"),
        Triple("baghdad", MapPoint(33.3, 44.4), "Baghdad"),
        Triple("india", MapPoint(21.0, 78.0), "India"),
        Triple("philippines", MapPoint(12.9, 121.8), "Philippines"),
        Triple("manila", MapPoint(14.6, 121.0), "Manila"),
        Triple("guatemala", MapPoint(15.5, -90.3), "Guatemala"),
        Triple("south korea", MapPoint(37.0, 127.9), "South Korea"),
        Triple("korea", MapPoint(36.5, 127.9), "Korea"),
    )

    /** The web's cap on plotted hotspots. */
    const val HOTSPOT_CAP: Int = 25

    /** Gazetteer fallback over [items]: first keyword match wins
     *  per headline, labels dedupe, unmatched are counted. */
    fun gazetteerSpots(items: List<GeoItem>): Pair<List<GeoSpot>, Int> {
        val spots = mutableListOf<GeoSpot>()
        val seenLabels = mutableSetOf<String>()
        var unplaced = 0
        for (item in items) {
            val haystack = (item.title + " " + item.source).lowercase(Locale.US)
            val hit = GAZETTEER.firstOrNull { (kw, _, _) -> haystack.contains(kw) }
            when {
                hit == null -> unplaced++
                hit.third in seenLabels -> Unit // placed already; not unplaced
                spots.size >= HOTSPOT_CAP -> unplaced++
                else -> {
                    seenLabels += hit.third
                    spots += GeoSpot(hit.third, item.title, hit.second.lat, hit.second.lon)
                }
            }
        }
        return spots to unplaced
    }

    fun parseGeo(envelope: JsonObject): GeoMapModel {
        val connected = boolOf(envelope, "connected") ?: false
        val stale = boolOf(envelope, "stale") ?: false
        val data = envelope["data"] as? JsonObject
            ?: return GeoMapModel(
                connected, stale, null, null, null,
                emptyList(), emptyList(), null, false,
            )
        val items = arrOf(data, "items").map { it ->
            GeoItem(
                title = strOf(it, "title"),
                source = strOf(it, "source"),
                published = strOf(it, "published"),
                impact = strOf(it, "impact"),
            )
        }
        val serverSpots = arrOf(data, "hotspots").mapNotNull { h ->
            val lat = dblOf(h, "lat") ?: return@mapNotNull null
            val lon = dblOf(h, "lon") ?: return@mapNotNull null
            GeoSpot(strOf(h, "label"), strOf(h, "title"), lat, lon)
        }
        val (spots, unplaced, usedServer) = if (serverSpots.isNotEmpty()) {
            Triple(serverSpots, null, true)
        } else {
            val (fallback, unplacedCount) = gazetteerSpots(items)
            Triple(fallback, unplacedCount, false)
        }
        return GeoMapModel(
            connected = connected,
            stale = stale,
            updated = strOf(data, "updated").takeIf { it.isNotBlank() },
            status = strOf(data, "status").takeIf { it.isNotBlank() },
            error = strOf(data, "error").takeIf { it.isNotBlank() },
            spots = spots,
            items = items,
            unplacedCount = unplaced,
            usedServerHotspots = usedServer,
        )
    }

    // ------------------------------------------------------------ flights

    data class FlightMarker(
        val hex: String,
        val callsign: String,
        val registration: String,
        val type: String,
        val altitudeFt: Int?,
        val onGround: Boolean,
        val speedKt: Double?,
        val headingDeg: Double?,
        val verticalRateFpm: Int?,
        val squawk: String,
        override val emergency: Boolean,
        val category: String,
        val seenS: Double?,
        override val lat: Double,
        override val lon: Double,
    ) : MapMarkerModel {
        override val markerId: String get() = "flight:$hex"
        override val title: String
            get() = callsign.ifBlank { hex }
        override val pulse: Boolean get() = emergency
        override val pulseBaseRadiusMeters: Double
            get() = if (emergency) EMERGENCY_PULSE_BASE_M else 0.0
        override val detailLines: List<Pair<String, String>>
            get() = listOfNotNull(
                "Callsign" to callsign.takeIf { it.isNotBlank() },
                "Type" to type.takeIf { it.isNotBlank() },
                "Registration" to registration.takeIf { it.isNotBlank() },
                "Altitude" to when {
                    onGround -> "On ground"
                    altitudeFt != null ->
                        String.format(Locale.US, "%,d ft", altitudeFt)
                    else -> null
                },
                "Ground speed" to speedKt?.let {
                    String.format(Locale.US, "%.0f kt", it)
                },
                "Heading" to headingDeg?.let {
                    String.format(Locale.US, "%03.0f°", it)
                },
                "Vertical rate" to verticalRateFpm?.let {
                    if (it == 0) "Level" else
                        String.format(Locale.US, "%,d fpm", it)
                },
                "Squawk" to squawk.takeIf { it.isNotBlank() }
                    ?.let { if (emergency) "$it — EMERGENCY" else it },
                "Category" to category.takeIf { it.isNotBlank() },
                "Position" to formatLatLon(lat, lon),
                "Seen" to seenS?.let {
                    String.format(Locale.US, "%.0f s ago", it)
                },
            ).map { (k, v) -> k to (v ?: "") }.filter { it.second.isNotBlank() }
    }

    data class FlightsMapModel(
        val connected: Boolean,
        val stale: Boolean,
        val total: Int,
        val center: MapPoint?,
        val distNm: Int?,
        val fetchedAt: String?,
        val source: String?,
        val note: String?,
        val ageS: Double?,
        val aircraft: List<FlightMarker>,
        /** One short heading line per moving aircraft — the
         *  ambient feed carries NO origin/destination, so a
         *  route line would be invented; the heading vector is
         *  what the payload honestly supports. */
        val headingVectors: List<MapPolylineModel>,
    ) {
        val hasContent: Boolean get() = aircraft.isNotEmpty()
    }

    /** Heading vectors project this far ahead of the aircraft. */
    const val HEADING_VECTOR_HORIZON_S: Double = 120.0

    /** Below this ground speed a vector is noise, not motion. */
    const val HEADING_VECTOR_MIN_SPEED_KT: Double = 20.0

    /** Nautical miles per degree of latitude (earth mean). */
    const val NM_PER_DEGREE: Double = 60.0

    /**
     * Dead-reckoning projection: the point [distanceNm] along
     * [headingDeg] (degrees true, 0 = north, 90 = east) from
     * [from], on the equirectangular approximation — more than
     * accurate enough for a two-minute vector at map scale.
     */
    fun projectPoint(from: MapPoint, headingDeg: Double, distanceNm: Double): MapPoint {
        val headingRad = Math.toRadians(headingDeg)
        val dLat = distanceNm * cos(headingRad) / NM_PER_DEGREE
        val dLon = distanceNm * sin(headingRad) /
            (NM_PER_DEGREE * cos(Math.toRadians(from.lat)))
        return MapPoint(from.lat + dLat, from.lon + dLon)
    }

    /**
     * The heading vector for one aircraft: a line from its
     * reported position to where its own heading + ground
     * speed carry it in [HEADING_VECTOR_HORIZON_S]. Null for
     * aircraft on the ground, parked-slow, or missing either
     * value — never an invented direction.
     */
    fun headingVectorFor(flight: FlightMarker): MapPolylineModel? {
        if (flight.onGround) return null
        val speed = flight.speedKt ?: return null
        val heading = flight.headingDeg ?: return null
        if (speed < HEADING_VECTOR_MIN_SPEED_KT) return null
        val start = MapPoint(flight.lat, flight.lon)
        val end = projectPoint(
            start, heading, speed * HEADING_VECTOR_HORIZON_S / 3600.0,
        )
        return MapPolylineModel(
            id = "vector:${flight.hex}",
            points = listOf(start, end),
            label = null,
            strokeWidth = 2f,
            alpha = 110,
        )
    }

    fun parseFlights(envelope: JsonObject): FlightsMapModel {
        val connected = boolOf(envelope, "connected") ?: false
        val envelopeStale = boolOf(envelope, "stale") ?: false
        val data = envelope["data"] as? JsonObject
            ?: return FlightsMapModel(
                connected, envelopeStale, 0, null, null, null, null,
                null, null, emptyList(), emptyList(),
            )
        val aircraft = arrOf(data, "aircraft").mapNotNull { a ->
            val lat = dblOf(a, "lat") ?: return@mapNotNull null
            val lon = dblOf(a, "lon") ?: return@mapNotNull null
            FlightMarker(
                hex = strOf(a, "hex"),
                callsign = strOf(a, "callsign").trim(),
                registration = strOf(a, "registration"),
                type = strOf(a, "type"),
                altitudeFt = intOf(a, "altitude_ft"),
                onGround = boolOf(a, "on_ground") ?: false,
                speedKt = dblOf(a, "speed_kt"),
                headingDeg = dblOf(a, "heading"),
                verticalRateFpm = intOf(a, "vertical_rate_fpm"),
                squawk = strOf(a, "squawk"),
                emergency = boolOf(a, "emergency") ?: false,
                category = strOf(a, "category"),
                seenS = dblOf(a, "seen_s"),
                lat = lat, lon = lon,
            )
        }
        val centerObj = data["center"] as? JsonObject
        val center = centerObj?.let {
            val la = dblOf(it, "lat"); val lo = dblOf(it, "lon")
            if (la != null && lo != null) MapPoint(la, lo) else null
        }
        return FlightsMapModel(
            connected = connected,
            stale = envelopeStale || (boolOf(data, "stale") ?: false),
            total = intOf(data, "total") ?: aircraft.size,
            center = center,
            distNm = intOf(data, "dist"),
            fetchedAt = strOf(data, "fetched_at").takeIf { it.isNotBlank() },
            source = strOf(data, "source").takeIf { it.isNotBlank() },
            note = strOf(data, "note").takeIf { it.isNotBlank() },
            ageS = dblOf(data, "age_s"),
            aircraft = aircraft,
            headingVectors = aircraft.mapNotNull { headingVectorFor(it) },
        )
    }

    /**
     * Airport hub coordinates — the web flights page's own
     * table (sentinel-live feeds.AIRPORT_COORDS), used to plot
     * a looked-up flight's route when BOTH endpoint IATA codes
     * are known majors. An unknown code means no route is
     * drawn — never an invented position.
     */
    val AIRPORT_COORDS: Map<String, MapPoint> = mapOf(
        "MCI" to MapPoint(39.2976, -94.7139), "STL" to MapPoint(38.7487, -90.3700),
        "DFW" to MapPoint(32.8998, -97.0403), "DEN" to MapPoint(39.8561, -104.6737),
        "ORD" to MapPoint(41.9742, -87.9073), "ATL" to MapPoint(33.6407, -84.4277),
        "LAX" to MapPoint(33.9416, -118.4085), "JFK" to MapPoint(40.6413, -73.7781),
        "EWR" to MapPoint(40.6895, -74.1745), "SFO" to MapPoint(37.6213, -122.3790),
        "SEA" to MapPoint(47.4502, -122.3088), "MIA" to MapPoint(25.7959, -80.2870),
        "BOS" to MapPoint(42.3656, -71.0096), "DCA" to MapPoint(38.8512, -77.0402),
        "IAD" to MapPoint(38.9531, -77.4565), "PHX" to MapPoint(33.4373, -112.0078),
        "LAS" to MapPoint(36.0840, -115.1537), "MSP" to MapPoint(44.8848, -93.2223),
        "DTW" to MapPoint(42.2124, -83.3534), "CLT" to MapPoint(35.2144, -80.9431),
        "IAH" to MapPoint(29.9844, -95.3414), "AUS" to MapPoint(30.1945, -97.6699),
        "BNA" to MapPoint(36.1263, -86.6774), "MDW" to MapPoint(41.7868, -87.7522),
        "LHR" to MapPoint(51.4700, -0.4543), "CDG" to MapPoint(49.0097, 2.5479),
        "FRA" to MapPoint(50.0379, 8.5622), "AMS" to MapPoint(52.3105, 4.7683),
        "NRT" to MapPoint(35.7720, 140.3929), "ICN" to MapPoint(37.4602, 126.4407),
    )

    data class FlightRoute(
        val fromIata: String,
        val toIata: String,
        val from: MapPoint,
        val to: MapPoint,
        val fromAirport: String,
        val toAirport: String,
    ) {
        val polyline: MapPolylineModel
            get() = MapPolylineModel(
                id = "route:$fromIata->$toIata",
                points = listOf(from, to),
                label = "$fromIata → $toIata",
            )

        fun endpointMarkers(): List<MapMarkerModel> = listOf(
            RouteEndpoint("route-dep:$fromIata", fromIata, fromAirport, from),
            RouteEndpoint("route-arr:$toIata", toIata, toAirport, to),
        )
    }

    private data class RouteEndpoint(
        override val markerId: String,
        val iata: String,
        val airport: String,
        val point: MapPoint,
    ) : MapMarkerModel {
        override val lat: Double get() = point.lat
        override val lon: Double get() = point.lon
        override val title: String get() = iata
        override val pulse: Boolean get() = false
        override val emergency: Boolean get() = false
        override val pulseBaseRadiusMeters: Double get() = 0.0
        override val detailLines: List<Pair<String, String>>
            get() = listOfNotNull(
                "Airport" to airport.takeIf { it.isNotBlank() },
                "Code" to iata,
                "Position" to formatLatLon(lat, lon),
            ).map { (k, v) -> k to (v ?: "") }.filter { it.second.isNotBlank() }
    }

    /** Route for a `getflightlookup` result object, or null when
     *  either endpoint IATA is missing or not a known major. */
    fun routeForLookup(result: JsonObject?): FlightRoute? {
        if (result == null) return null
        val dep = result["departure"] as? JsonObject ?: return null
        val arr = result["arrival"] as? JsonObject ?: return null
        val fromIata = strOf(dep, "iata").uppercase(Locale.US)
        val toIata = strOf(arr, "iata").uppercase(Locale.US)
        val from = AIRPORT_COORDS[fromIata] ?: return null
        val to = AIRPORT_COORDS[toIata] ?: return null
        return FlightRoute(
            fromIata = fromIata, toIata = toIata, from = from, to = to,
            fromAirport = strOf(dep, "airport"),
            toAirport = strOf(arr, "airport"),
        )
    }

    // ------------------------------------------------------------ helpers

    fun formatLatLon(lat: Double, lon: Double): String =
        String.format(
            Locale.US, "%.2f°, %.2f°",
            lat, lon,
        )

    private fun strOf(obj: JsonObject, key: String): String =
        (obj[key] as? JsonPrimitive)?.let {
            if (it.content == "null") "" else it.content
        } ?: ""

    private fun intOf(obj: JsonObject, key: String): Int? =
        (obj[key] as? JsonPrimitive)?.content?.toIntOrNull()
            ?: (obj[key] as? JsonPrimitive)?.content?.toDoubleOrNull()?.toInt()

    private fun dblOf(obj: JsonObject, key: String): Double? =
        (obj[key] as? JsonPrimitive)?.content?.toDoubleOrNull()

    private fun boolOf(obj: JsonObject, key: String): Boolean? =
        (obj[key] as? JsonPrimitive)?.content?.toBooleanStrictOrNull()

    private fun arrOf(obj: JsonObject, key: String): List<JsonObject> =
        (obj[key] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

    private fun strListOf(obj: JsonObject, key: String): List<String> =
        (obj[key] as? JsonArray)?.mapNotNull {
            (it as? JsonPrimitive)?.content?.takeIf { c -> c != "null" }
        } ?: emptyList()

    private fun pointOf(element: kotlinx.serialization.json.JsonElement?): MapPoint? {
        val arr = element as? JsonArray ?: return null
        if (arr.size < 2) return null
        val lat = (arr[0] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: return null
        val lon = (arr[1] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: return null
        return MapPoint(lat, lon)
    }
}

/**
 * The pulse the Live Intel maps breathe with: an expanding ring
 * whose radius grows and alpha decays over one period, looping.
 * Pure numbers, pinned by the suite; the view only renders
 * them. Under reduced motion the view never advances the phase
 * (phase stays 0 — a still, faint ring).
 */
object IntelMapPulse {
    const val PULSE_PERIOD_MS: Long = 1500L

    /** How far the ring expands past its base radius. */
    const val PULSE_EXPANSION: Double = 1.8

    /** Ring alpha at phase 0; decays linearly to 0. */
    const val PULSE_MAX_ALPHA: Float = 0.5f

    fun phaseAt(elapsedMs: Long): Float {
        val p = ((elapsedMs % PULSE_PERIOD_MS) + PULSE_PERIOD_MS) % PULSE_PERIOD_MS
        return p.toFloat() / PULSE_PERIOD_MS.toFloat()
    }

    fun radiusFactor(phase: Float): Double =
        1.0 + PULSE_EXPANSION * phase.coerceIn(0f, 1f).toDouble()

    fun alpha(phase: Float): Float =
        PULSE_MAX_ALPHA * (1f - phase.coerceIn(0f, 1f))
}

/** Basemap layers for the Sentinel map switcher. */
enum class MapLayer(val label: String) {
    STREETS("Streets"),
    SATELLITE("Satellite"),
    DARK("Dark"),
}

/**
 * Tile URL construction for the two custom layers (pinned by
 * the suite — the argument ORDER differs per provider and is
 * the classic silent-failure spot):
 *  - Esri World Imagery: /tile/{z}/{y}/{x}
 *  - CARTO dark_matter:  /dark_all/{z}/{x}/{y}.png
 */
object IntelMapTiles {
    const val ESRI_TEMPLATE: String =
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/%d/%d/%d"
    const val CARTO_TEMPLATE: String =
        "https://basemaps.cartocdn.com/dark_all/%d/%d/%d.png"

    fun esriUrl(zoom: Int, x: Int, y: Int): String =
        String.format(Locale.US, ESRI_TEMPLATE, zoom, y, x)

    fun cartoUrl(zoom: Int, x: Int, y: Int): String =
        String.format(Locale.US, CARTO_TEMPLATE, zoom, x, y)
}
