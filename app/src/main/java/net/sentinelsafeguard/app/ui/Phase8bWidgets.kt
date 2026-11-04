package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Rendering helpers for the Phase 8B surfaces. The Phase 8B
 * payloads are the web handlers' own open shapes; these helpers
 * render them generically but faithfully — every scalar field
 * the handler returns appears under its own name, arrays render
 * as rows, and nothing is invented when a payload is empty
 * (web's empty states ride the payloads' own counts).
 */

/** Every scalar field of [obj] as a DetailLine, in payload
 *  order, skipping [skip] keys and nested structures (callers
 *  render those as their own sections). */
@Composable
fun AnyLines(obj: JsonObject, vararg skip: String) {
    val skipSet = skip.toSet()
    obj.entries.forEach { (key, value) ->
        if (key in skipSet) return@forEach
        if (value is JsonPrimitive) {
            DetailLine(key, Phase3Logic.displayValue(key, value))
        }
    }
}

/** Rows of [key]'s array: headline + meta + scalar detail lines. */
@Composable
fun AnyRows(obj: JsonObject, key: String, detailKeys: List<String> = emptyList()) {
    val rows = Phase3Logic.arr(obj, key)
    if (rows.isEmpty()) {
        Text("None", color = SentinelMuted)
        return
    }
    rows.forEach { row ->
        Column {
            Text(Phase8bLogic.rowTitle(row), style = MaterialTheme.typography.bodyLarge)
            val meta = Phase8bLogic.rowMeta(row)
            if (meta.isNotBlank()) {
                Text(meta, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
            }
            detailKeys.forEach { dk ->
                DetailLine(dk, Phase3Logic.displayValue(dk, row[dk]))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** A nested object's scalar fields under a small heading. */
@Composable
fun NestedLines(title: String, obj: JsonObject?, vararg skip: String) {
    if (obj == null) return
    Text(title, style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    AnyLines(obj, *skip)
    Spacer(Modifier.height(8.dp))
}

/** Counts of a payload's named arrays as stat-ish lines. */
fun arrayCount(obj: JsonObject, key: String): Int =
    (obj[key] as? JsonArray)?.size ?: 0

/** First nested JsonObject under any of [keys]. */
fun firstObj(obj: JsonObject, vararg keys: String): JsonObject? {
    for (key in keys) {
        (obj[key] as? JsonObject)?.let { return it }
    }
    return null
}

/** All elements of a JsonElement when it is an array of objects. */
fun objList(element: JsonElement?): List<JsonObject> =
    (element as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

/** A small "open the reused surface" link row: Phase 8B reuses
 *  earlier phases' screens exactly where the web console itself
 *  composes shared handlers (Addendum 26 reuse accounting). */
@Composable
fun ReuseLink(label: String, note: String, onOpen: () -> Unit) {
    Text(note, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
    Spacer(Modifier.height(6.dp))
    OutlinedButton(onClick = onOpen) { Text(label) }
}
