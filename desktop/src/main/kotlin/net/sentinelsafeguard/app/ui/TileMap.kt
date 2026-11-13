package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.File
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.skia.Image as SkiaImage

/**
 * Desktop tile map — the honest desktop equivalent of the phone's
 * osmdroid SentinelMap (desktop port, 2026-11-13). osmdroid is an
 * Android-view library and cannot run here, so this renders REAL
 * OpenStreetMap tiles (fetched over HTTPS with an identifying
 * user-agent, disk-cached in the per-user config dir) in Compose,
 * plots the same markers the shared IntelMapLogic parses from the
 * same payloads, and supports pan (drag) and zoom (buttons).
 * Attribution is rendered on the map, as OSM requires.
 */
object TileCache {
    private val client = OkHttpClient()
    private val memory = java.util.concurrent.ConcurrentHashMap<String, ImageBitmap>()

    suspend fun tile(z: Int, x: Int, y: Int, cacheDir: File): ImageBitmap? {
        val key = "$z/$x/$y"
        memory[key]?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(cacheDir, "map-cache/$z/$x")
                val f = File(dir, "$y.png")
                val bytes: ByteArray = if (f.exists() && f.length() > 0) {
                    f.readBytes()
                } else {
                    val req = Request.Builder()
                        .url("https://tile.openstreetmap.org/$z/$x/$y.png")
                        .header("User-Agent", "CommandAppDesktop/1.0.0 (Sentinel Safeguard; contact: sentinel@sentinelsafeguard.net)")
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) return@withContext null
                        val b = resp.body?.bytes() ?: return@withContext null
                        dir.mkdirs(); f.writeBytes(b); b
                    }
                }
                val bmp = SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                memory[key] = bmp
                bmp
            }.getOrNull()
        }
    }
}

private fun lonToWorldX(lon: Double, zoom: Int): Double =
    (lon + 180.0) / 360.0 * 256.0 * 2.0.pow(zoom)

private fun latToWorldY(lat: Double, zoom: Int): Double {
    val rad = lat * PI / 180.0
    return (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0 * 256.0 * 2.0.pow(zoom)
}

private fun worldXToLon(x: Double, zoom: Int): Double =
    x / (256.0 * 2.0.pow(zoom)) * 360.0 - 180.0

private fun worldYToLat(y: Double, zoom: Int): Double {
    val n = PI - 2.0 * PI * y / (256.0 * 2.0.pow(zoom))
    return 180.0 / PI * atan(sinh(n))
}

@Composable
fun TileMap(
    markers: List<IntelMapLogic.MapMarkerModel>,
    center: IntelMapLogic.MapPoint,
    initialZoom: Int,
    cacheDir: File,
    modifier: Modifier = Modifier,
) {
    var zoom by remember(center, initialZoom) { mutableStateOf(initialZoom) }
    var centerLat by remember(center) { mutableStateOf(center.lat) }
    var centerLon by remember(center) { mutableStateOf(center.lon) }
    var tiles by remember { mutableStateOf<Map<Triple<Int, Int, Int>, ImageBitmap>>(emptyMap()) }
    var viewW by remember { mutableStateOf(900) }
    var viewH by remember { mutableStateOf(420) }

    // (Re)fetch the visible tile set whenever the view moves.
    LaunchedEffect(zoom, centerLat, centerLon, viewW, viewH) {
        val cx = lonToWorldX(centerLon, zoom)
        val cy = latToWorldY(centerLat, zoom)
        val x0 = floor((cx - viewW / 2) / 256).toInt()
        val x1 = floor((cx + viewW / 2) / 256).toInt()
        val y0 = floor((cy - viewH / 2) / 256).toInt()
        val y1 = floor((cy + viewH / 2) / 256).toInt()
        val max = 2.0.pow(zoom).toInt()
        val loaded = mutableMapOf<Triple<Int, Int, Int>, ImageBitmap>()
        for (x in x0..x1) for (y in y0..y1) {
            if (x in 0 until max && y in 0 until max) {
                TileCache.tile(zoom, x, y, cacheDir)?.let { loaded[Triple(zoom, x, y)] = it }
            }
        }
        tiles = loaded
    }

    Box(modifier.fillMaxWidth().height(420.dp).background(Color(0xFF0B1512))) {
        Canvas(
            Modifier.fillMaxWidth().height(420.dp)
                .pointerInput(zoom) {
                    viewW = size.width; viewH = size.height
                    detectDragGestures { change, drag ->
                        change.consume()
                        val cx = lonToWorldX(centerLon, zoom) - drag.x
                        val cy = latToWorldY(centerLat, zoom) - drag.y
                        centerLon = worldXToLon(cx, zoom)
                        centerLat = worldYToLat(cy, zoom)
                    }
                }
        ) {
            viewW = size.width.toInt(); viewH = size.height.toInt()
            val cx = lonToWorldX(centerLon, zoom)
            val cy = latToWorldY(centerLat, zoom)
            val originX = cx - size.width / 2
            val originY = cy - size.height / 2
            for ((coord, bmp) in tiles) {
                val dx = (coord.second * 256 - originX).toInt()
                val dy = (coord.third * 256 - originY).toInt()
                drawImage(bmp, dstOffset = IntOffset(dx, dy), dstSize = IntSize(256, 256))
            }
            for (m in markers) {
                val px = (lonToWorldX(m.lon, zoom) - originX).toFloat()
                val py = (latToWorldY(m.lat, zoom) - originY).toFloat()
                if (px in -20f..size.width + 20f && py in -20f..size.height + 20f) {
                    drawCircle(Color(0xFF2EE6A8), radius = 14f, center = Offset(px, py), alpha = 0.35f)
                    drawCircle(Color(0xFF2EE6A8), radius = 6f, center = Offset(px, py))
                    drawCircle(Color(0xFF06231A), radius = 2.5f, center = Offset(px, py))
                }
            }
        }
        Row(Modifier.padding(8.dp)) {
            Button(onClick = { if (zoom < 18) zoom++ }) { Text("+") }
            Button(onClick = { if (zoom > 2) zoom-- }) { Text("−") }
        }
        Text(
            "© OpenStreetMap contributors — desktop tile map (pan: drag, zoom: buttons). " +
                "${markers.size} marker(s) from the live payload.",
            Modifier.padding(start = 8.dp, top = 380.dp),
            color = Color.White, style = MaterialTheme.typography.labelSmall,
        )
    }
}
