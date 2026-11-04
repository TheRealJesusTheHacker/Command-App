package net.sentinelsafeguard.app.ui

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Pure helpers for the Phase 6 (hunt10) Intel surfaces — kept
 * free of Compose so the unit suite can pin them against the
 * real captured payloads (ContractPhase6Test). Vocabularies are
 * web Command's exact sets where the payloads carry them; the
 * rest ride the payloads themselves (sectors, venue types,
 * categories, tags, templates all arrive in the workspaces).
 */
object Phase6Logic {

    /** Flatten an open-shaped payload value into display lines
     *  ("key: value", nested values joined plainly) — Live Intel's
     *  upstream payloads are open-shaped exactly as web composes
     *  them, so nothing is dropped. */
    fun flatten(element: JsonElement?, prefix: String = ""): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        when (element) {
            is JsonObject -> element.forEach { (k, v) ->
                val label = if (prefix.isBlank()) k else "$prefix · $k"
                out += flatten(v, label)
            }
            is JsonArray -> element.forEachIndexed { i, v ->
                out += flatten(v, if (prefix.isBlank()) "${i + 1}" else "$prefix ${i + 1}")
            }
            is JsonPrimitive -> {
                val v = if (element.content == "null") "" else element.content
                if (v.isNotBlank()) out += prefix to v
            }
            else -> Unit
        }
        return out
    }

    /** Threat Intel severity chips (payload vocabulary). */
    val THREAT_SEVERITIES: List<String> = listOf("Critical", "High", "Medium", "Low")

    /** News Desk bulk cleanup takes DRAFT ids only — published
     *  items can never be selected (web's own rule; the server
     *  refuses in its own words when asked anyway). */
    fun draftIds(items: List<JsonObject>): List<Int> =
        items.filter { Phase3Logic.str(it, "state") == "draft" }
            .mapNotNull { Phase3Logic.int(it, "id") }

    /** A review's response status line, from the responses
     *  payload's nested response object. */
    fun responseStatus(review: JsonObject): String {
        val response = Phase3Logic.obj(review, "response") ?: return "No response"
        val status = Phase3Logic.str(response, "status")
        return if (status.isBlank()) "No response" else "Response: $status"
    }

    /** Law template lookup by key across the workspace's
     *  categories (the docgen + fast-form surfaces key off it). */
    fun lawTemplate(workspace: JsonObject, key: String): JsonObject? {
        for (category in Phase3Logic.arr(workspace, "categories")) {
            for (template in Phase3Logic.arr(category, "templates")) {
                if (Phase3Logic.str(template, "key") == key) return template
            }
        }
        return null
    }

    /** The research jurisdiction rides as free text: the
     *  workspace payload carries no jurisdiction list, so none
     *  is invented — the handler remains the authority and its
     *  wording ("Pick a jurisdiction from the list.") surfaces
     *  verbatim when it disagrees. */
}
