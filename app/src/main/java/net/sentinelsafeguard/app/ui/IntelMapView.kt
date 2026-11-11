package net.sentinelsafeguard.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline

/**
 * The native Sentinel map (Lane C, 1.4.0 Expansion B) — OSMDroid
 * under a Compose surface, styled to the Sentinel HUD: emerald
 * on near-black, fine lines, no purple. Three basemap layers
 * (Streets = OSM MAPNIK, Satellite = Esri World Imagery,
 * Dark = CARTO dark_matter — all keyless), Sentinel-styled
 * layer chips and zoom buttons instead of the stock controls
 * (multi-touch stays on; the built-in zoom buttons are off).
 *
 * ALIVE, per the design correction: markers carry a pulsing
 * ring (expanding circle, alpha decay — the numbers live in
 * [IntelMapPulse], pinned by the suite), driven by a
 * withFrameMillis loop that only runs while the surface is
 * composed. Under the system reduced-motion setting the phase
 * never advances: the ring is a still, faint halo and nothing
 * moves.
 *
 * Data discipline: the caller owns last-good — this view simply
 * renders the model it is given, so a failed refresh never
 * blanks the map. The viewport frames ONCE (first model with a
 * frame center); later refreshes update markers in place and
 * never yank the operator's pan.
 */

/** Esri World Imagery — note the z/y/x argument order. */
private class EsriSatelliteSource : OnlineTileSourceBase(
    "Sentinel Satellite", 0, 18, 256, ".jpg",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/"),
) {
    override fun getTileURLString(pMapTileIndex: Long): String = IntelMapTiles.esriUrl(
        MapTileIndex.getZoom(pMapTileIndex),
        MapTileIndex.getX(pMapTileIndex),
        MapTileIndex.getY(pMapTileIndex),
    )
}

/** CARTO dark_matter — z/x/y argument order. */
private class CartoDarkSource : OnlineTileSourceBase(
    "Sentinel Dark", 0, 19, 256, ".png",
    arrayOf("https://basemaps.cartocdn.com/"),
) {
    override fun getTileURLString(pMapTileIndex: Long): String = IntelMapTiles.cartoUrl(
        MapTileIndex.getZoom(pMapTileIndex),
        MapTileIndex.getX(pMapTileIndex),
        MapTileIndex.getY(pMapTileIndex),
    )
}

/**
 * Field overlay drawn UNDER the markers: country volume circles
 * (fill + hairline stroke) and the pulsing rings around live
 * markers. Canvas-drawn so the circles never intercept taps —
 * marker taps always reach the markers.
 */
private class SentinelFieldOverlay : Overlay() {
    var circles: List<IntelMapLogic.MapCircleModel> = emptyList()
    var pulseTargets: List<IntelMapLogic.MapMarkerModel> = emptyList()
    var phase: Float = 0f
    var circleFillArgb: Int = 0
    var circleStrokeArgb: Int = 0
    var pulseArgb: Int = 0

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    override fun draw(canvas: AndroidCanvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val projection = mapView.projection ?: return
        val pt = Point()
        for (circle in circles) {
            projection.toPixels(GeoPoint(circle.center.lat, circle.center.lon), pt)
            val radiusPx = projection.metersToEquatorPixels(circle.radiusMeters.toFloat())
            if (radiusPx <= 0f) continue
            fillPaint.color = circleFillArgb
            canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), radiusPx, fillPaint)
            strokePaint.color = circleStrokeArgb
            strokePaint.strokeWidth = 1.5f
            canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), radiusPx, strokePaint)
        }
        val alpha = IntelMapPulse.alpha(phase)
        if (alpha <= 0.01f) return
        val factor = IntelMapPulse.radiusFactor(phase)
        for (target in pulseTargets) {
            projection.toPixels(GeoPoint(target.lat, target.lon), pt)
            val basePx = projection.metersToEquatorPixels(
                target.pulseBaseRadiusMeters.toFloat(),
            )
            val radiusPx = maxOf(basePx, 12f) * factor.toFloat()
            strokePaint.color = pulseArgb
            strokePaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
            strokePaint.strokeWidth = 2f
            canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), radiusPx, strokePaint)
        }
        strokePaint.alpha = 255
    }
}

/** A HUD dot marker: bright core, hairline ring, soft glow. */
private fun markerIcon(context: Context, fillArgb: Int): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (26 * density).toInt().coerceAtLeast(26)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val center = sizePx / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    // Glow.
    paint.color = fillArgb
    paint.alpha = 46
    canvas.drawCircle(center, center, center, paint)
    // Ring.
    paint.alpha = 255
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.6f * density
    canvas.drawCircle(center, center, center - paint.strokeWidth, paint)
    // Core.
    paint.style = Paint.Style.FILL
    canvas.drawCircle(center, center, 4.2f * density, paint)
    return BitmapDrawable(context.resources, bitmap)
}

@Composable
fun SentinelMap(
    markers: List<IntelMapLogic.MapMarkerModel>,
    circles: List<IntelMapLogic.MapCircleModel> = emptyList(),
    polylines: List<IntelMapLogic.MapPolylineModel> = emptyList(),
    onMarkerTap: (String) -> Unit,
    accent: Color,
    frameCenter: IntelMapLogic.MapPoint,
    frameZoom: Double,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val reduced = rememberReducedMotion()
    val tick = rememberHapticTick()
    var layer by remember { mutableStateOf(MapLayer.DARK) }

    val accentArgb = accent.toArgb()
    val emergencyArgb = net.sentinelsafeguard.app.ui.theme.SentinelDanger.toArgb()
    val circleFillArgb = accent.copy(alpha = 0.10f).toArgb()
    val circleStrokeArgb = accent.copy(alpha = 0.45f).toArgb()

    val fieldOverlay = remember { SentinelFieldOverlay() }
    val icons = remember(context, accentArgb, emergencyArgb) {
        mapOf(
            false to markerIcon(context, accentArgb),
            true to markerIcon(context, emergencyArgb),
        )
    }

    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(CartoDarkSource())
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            controller.setZoom(frameZoom)
            controller.setCenter(GeoPoint(frameCenter.lat, frameCenter.lon))
        }
    }

    // Framing happens once: the first frame center that differs
    // from the creation viewport wins (e.g. the flights payload
    // center arriving after first composition); after that the
    // operator owns the viewport.
    var framed by remember { mutableStateOf(false) }
    LaunchedEffect(frameCenter, frameZoom) {
        if (!framed) {
            mapView.controller.setCenter(GeoPoint(frameCenter.lat, frameCenter.lon))
            mapView.controller.setZoom(frameZoom)
            framed = true
        }
    }

    // The pulse heartbeat: advance the phase and repaint while
    // visible. Reduced motion never starts the loop — the ring
    // rests at phase 0.
    LaunchedEffect(reduced, mapView) {
        if (!reduced) {
            var startMs = -1L
            while (true) {
                withFrameMillis { frameMs ->
                    if (startMs < 0L) startMs = frameMs
                    fieldOverlay.phase = IntelMapPulse.phaseAt(frameMs - startMs)
                }
                mapView.invalidate()
            }
        } else {
            fieldOverlay.phase = 0f
            mapView.invalidate()
        }
    }

    DisposableEffect(mapView) {
        onDispose { mapView.onDetach() }
    }

    Box(
        modifier
            .background(net.sentinelsafeguard.app.ui.theme.SentinelBlack)
            .border(1.dp, net.sentinelsafeguard.app.ui.theme.SentinelOutline),
    ) {
        AndroidView(
            factory = { mapView },
            update = { mv ->
                mv.setTileSource(
                    when (layer) {
                        MapLayer.STREETS -> TileSourceFactory.MAPNIK
                        MapLayer.SATELLITE -> EsriSatelliteSource()
                        MapLayer.DARK -> CartoDarkSource()
                    },
                )
                fieldOverlay.circles = circles
                fieldOverlay.pulseTargets = markers.filter { it.pulse }
                fieldOverlay.circleFillArgb = circleFillArgb
                fieldOverlay.circleStrokeArgb = circleStrokeArgb
                fieldOverlay.pulseArgb = accentArgb
                mv.overlays.clear()
                mv.overlays.add(fieldOverlay)
                for (line in polylines) {
                    val polyline = Polyline(mv)
                    polyline.setPoints(line.points.map { GeoPoint(it.lat, it.lon) })
                    polyline.outlinePaint.color = accentArgb
                    polyline.outlinePaint.strokeWidth = line.strokeWidth
                    polyline.outlinePaint.alpha = line.alpha
                    mv.overlays.add(polyline)
                }
                for (markerModel in markers) {
                    val marker = Marker(mv)
                    marker.position = GeoPoint(markerModel.lat, markerModel.lon)
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    marker.icon = icons[markerModel.emergency]
                    marker.title = markerModel.title
                    val id = markerModel.markerId
                    marker.setOnMarkerClickListener { _, _ ->
                        onMarkerTap(id)
                        true
                    }
                    mv.overlays.add(marker)
                }
                mv.invalidate()
            },
            modifier = Modifier.matchParentSize(),
        )

        // Layer switcher — Sentinel chips, top start.
        Row(
            Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(
                    net.sentinelsafeguard.app.ui.theme.SentinelSurface.copy(alpha = 0.88f),
                    RoundedCornerShape(10.dp),
                )
                .border(
                    1.dp,
                    net.sentinelsafeguard.app.ui.theme.SentinelOutline,
                    RoundedCornerShape(10.dp),
                )
                .padding(4.dp),
        ) {
            for (option in MapLayer.entries) {
                val selected = layer == option
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = if (selected) {
                        net.sentinelsafeguard.app.ui.theme.SentinelGreenDim
                    } else {
                        Color.Transparent
                    },
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        // AUDIT-171 M-10: the layer chips were
                        // ~26dp tall — under the 48dp touch floor.
                        .defaultMinSize(minHeight = 48.dp)
                        .sentinelClickable { tick(); layer = option },
                ) {
                    Text(
                        option.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) {
                            net.sentinelsafeguard.app.ui.theme.SentinelBlack
                        } else {
                            net.sentinelsafeguard.app.ui.theme.SentinelMuted
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    )
                }
            }
        }

        // Zoom controls — Sentinel discs, bottom end.
        Column(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
        ) {
            for ((label, zoomIn) in listOf("+" to true, "−" to false)) {
                Surface(
                    shape = CircleShape,
                    color = net.sentinelsafeguard.app.ui.theme.SentinelSurface.copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, net.sentinelsafeguard.app.ui.theme.SentinelOutline,
                    ),
                    modifier = Modifier
                        .padding(vertical = 3.dp)
                        // AUDIT-171 M-10: 38dp was under the 48dp
                        // touch floor — the discs are 48dp now,
                        // with real names for screen readers.
                        .size(48.dp)
                        .semantics {
                            contentDescription = if (zoomIn) "Zoom in" else "Zoom out"
                        }
                        .sentinelClickable {
                            tick()
                            if (zoomIn) mapView.controller.zoomIn() else mapView.controller.zoomOut()
                        },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            label,
                            color = net.sentinelsafeguard.app.ui.theme.SentinelGreen,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // Attribution — the tile providers' credit, bottom start.
        Text(
            when (layer) {
                MapLayer.STREETS -> "© OpenStreetMap contributors"
                MapLayer.SATELLITE -> "Imagery © Esri, Maxar, Earthstar Geographics"
                MapLayer.DARK -> "© OpenStreetMap contributors © CARTO"
            },
            color = net.sentinelsafeguard.app.ui.theme.SentinelMuted,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .background(
                    net.sentinelsafeguard.app.ui.theme.SentinelBlack.copy(alpha = 0.6f),
                    RoundedCornerShape(4.dp),
                )
                .padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}
