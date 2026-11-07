package net.sentinelsafeguard.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * Form-draft preservation (1.4.0): when a form is left half-filled —
 * the app is backgrounded, the session ends, the phone dies — the
 * words already typed come back instead of vanishing. Two hard
 * rules, enforced in the pure logic below and never left to callers:
 *
 * 1. Sensitive fields are NEVER drafted. A field whose key names a
 *    secret (see [DraftLogic.DENIED_KEY_PARTS]) or whose input type
 *    the caller flags as sensitive is dropped before anything is
 *    stored. Drafts live in plain preferences, not the encrypted
 *    store, so a secret must never reach them.
 * 2. Drafts expire. Anything older than seven days is pruned on
 *    read and on list — a stale draft is worse than none, because
 *    it can quietly overwrite fresher server state.
 *
 * [DraftLogic] is pure and unit-tested; [DraftStore] is the thin
 * SharedPreferences shell over it, following [SettingsStore].
 */
object DraftLogic {

    /** Drafts older than this are pruned (7 days, in milliseconds). */
    const val MAX_AGE_MS: Long = 7L * 24 * 60 * 60 * 1000

    /**
     * Key fragments that mark a field as sensitive. Matched against
     * the lowercased key as a substring, so "passwordConfirm",
     * "apiToken" and "cardNumber" are all caught.
     */
    val DENIED_KEY_PARTS: List<String> = listOf(
        "password", "passphrase", "token", "secret",
        "totp", "code", "card", "cvv",
    )

    fun isSensitiveKey(key: String): Boolean {
        val normalized = key.lowercase()
        return DENIED_KEY_PARTS.any { normalized.contains(it) }
    }

    /**
     * True when a field must be excluded: its key is denied, or the
     * caller flagged it sensitive by key ([sensitiveKeys]) or the
     * field itself carries [sensitiveType] (password/secret input).
     */
    fun isSensitiveField(
        key: String,
        sensitiveType: Boolean = false,
        sensitiveKeys: Set<String> = emptySet(),
    ): Boolean = sensitiveType || key in sensitiveKeys || isSensitiveKey(key)

    /** [fields] minus every sensitive entry — the only map ever stored. */
    fun filterFields(
        fields: Map<String, String>,
        sensitiveKeys: Set<String> = emptySet(),
        sensitiveTypeKeys: Set<String> = emptySet(),
    ): Map<String, String> = fields.filterKeys { key ->
        !isSensitiveField(
            key = key,
            sensitiveType = key in sensitiveTypeKeys,
            sensitiveKeys = sensitiveKeys,
        )
    }

    fun isExpired(savedAtMs: Long, nowMs: Long): Boolean =
        savedAtMs > 0 && nowMs - savedAtMs > MAX_AGE_MS

    fun shouldKeep(savedAtMs: Long, nowMs: Long): Boolean = !isExpired(savedAtMs, nowMs)

    /** Serializes a field map to a JsonObject string. */
    fun serializeFields(fields: Map<String, String>): String =
        buildJsonObject {
            for ((key, value) in fields) put(key, value)
        }.toString()

    /** Parses a JsonObject string back to a field map; junk → empty. */
    fun deserializeFields(text: String?): Map<String, String> {
        if (text.isNullOrBlank()) return emptyMap()
        val obj = runCatching { Json.parseToJsonElement(text).jsonObject }
            .getOrNull() ?: return emptyMap()
        return obj.entries.mapNotNull { (key, value) ->
            val primitive = value as? JsonPrimitive ?: return@mapNotNull null
            val content = primitive.contentOrNull ?: return@mapNotNull null
            key to content
        }.toMap()
    }

    /** One stored draft: its fields plus when they were saved. */
    data class DraftEntry(
        val formId: String,
        val fields: Map<String, String>,
        val savedAtMs: Long,
    )

    fun serializeEntry(entry: DraftEntry): String =
        buildJsonObject {
            put("formId", entry.formId)
            put("savedAtMs", entry.savedAtMs)
            put("fields", buildJsonObject {
                for ((key, value) in entry.fields) put(key, value)
            })
        }.toString()

    fun deserializeEntry(formId: String, text: String?): DraftEntry? {
        if (text.isNullOrBlank()) return null
        val obj = runCatching { Json.parseToJsonElement(text).jsonObject }
            .getOrNull() ?: return null
        val savedAt = (obj["savedAtMs"] as? JsonPrimitive)?.longOrNull ?: 0L
        val fieldsObj = obj["fields"] as? JsonObject
        val fields = if (fieldsObj != null) {
            deserializeFields(fieldsObj.toString())
        } else {
            emptyMap()
        }
        return DraftEntry(formId = formId, fields = fields, savedAtMs = savedAt)
    }
}

/**
 * SharedPreferences-backed draft storage. Keys are namespaced per
 * form id ("draft.<formId>") so forms never collide, and expired
 * drafts are removed whenever they are encountered.
 */
class DraftStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    /**
     * Saves [fields] for [formId], with sensitive fields removed by
     * [DraftLogic.filterFields] first. [sensitiveKeys] names extra
     * keys to exclude; [sensitiveTypeKeys] names keys whose input
     * type is sensitive (password/secret fields).
     */
    fun save(
        formId: String,
        fields: Map<String, String>,
        sensitiveKeys: Set<String> = emptySet(),
        sensitiveTypeKeys: Set<String> = emptySet(),
        nowMs: Long = System.currentTimeMillis(),
    ) {
        val kept = DraftLogic.filterFields(fields, sensitiveKeys, sensitiveTypeKeys)
        if (kept.isEmpty()) {
            clear(formId)
            return
        }
        val entry = DraftLogic.DraftEntry(formId = formId, fields = kept, savedAtMs = nowMs)
        prefs.edit().putString(keyFor(formId), DraftLogic.serializeEntry(entry)).apply()
    }

    /** The saved fields for [formId], or an empty map when there is
     *  no live draft. An expired draft is deleted, not returned. */
    fun load(formId: String, nowMs: Long = System.currentTimeMillis()): Map<String, String> =
        loadEntry(formId, nowMs)?.fields ?: emptyMap()

    fun loadEntry(formId: String, nowMs: Long = System.currentTimeMillis()): DraftLogic.DraftEntry? {
        val entry = DraftLogic.deserializeEntry(formId, prefs.getString(keyFor(formId), null))
            ?: return null
        if (DraftLogic.isExpired(entry.savedAtMs, nowMs)) {
            clear(formId)
            return null
        }
        return entry
    }

    fun clear(formId: String) {
        prefs.edit().remove(keyFor(formId)).apply()
    }

    /** Form ids with a live (unexpired) draft. Listing prunes the rest. */
    fun listFormIds(nowMs: Long = System.currentTimeMillis()): List<String> {
        pruneExpired(nowMs)
        return prefs.all.keys
            .filter { it.startsWith(KEY_PREFIX) }
            .map { it.removePrefix(KEY_PREFIX) }
            .sorted()
    }

    /** Removes every expired draft; returns how many were removed. */
    fun pruneExpired(nowMs: Long = System.currentTimeMillis()): Int {
        var removed = 0
        val editor = prefs.edit()
        for (key in prefs.all.keys) {
            if (!key.startsWith(KEY_PREFIX)) continue
            val entry = DraftLogic.deserializeEntry(
                key.removePrefix(KEY_PREFIX),
                prefs.getString(key, null),
            )
            if (entry == null || DraftLogic.isExpired(entry.savedAtMs, nowMs)) {
                editor.remove(key)
                removed++
            }
        }
        if (removed > 0) editor.apply()
        return removed
    }

    private fun keyFor(formId: String): String = KEY_PREFIX + formId

    companion object {
        const val FILE_NAME = "sentinel_drafts"
        private const val KEY_PREFIX = "draft."
    }
}
