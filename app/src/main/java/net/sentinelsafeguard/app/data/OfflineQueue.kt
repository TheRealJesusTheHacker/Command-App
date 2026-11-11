package net.sentinelsafeguard.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.ApiException
import net.sentinelsafeguard.app.data.api.SentinelApi
import net.sentinelsafeguard.app.data.api.syncMutations

/**
 * Offline write queue (1.4.0): ticket creates, notes and patches made
 * with no connection are kept on the phone and replayed against
 * POST /api/app/sync when the connection returns. The server is
 * idempotent and server-wins for scalar fields; its per-mutation
 * results decide each entry's fate here:
 *
 * - applied (or duplicate — the server already had it) → removed.
 * - conflict → the entry is marked SUPERSEDED and KEPT, with the
 *   server's copy stored beside it. A conflict is never silently
 *   dropped: the owner's words stay on the phone until a person
 *   resolves them.
 * - anything else / unknown → the entry stays pending for the
 *   next flush.
 *
 * [OfflineLogic] is pure and unit-tested; [OfflineQueue] is the
 * SharedPreferences-backed store plus the flush itself.
 */

enum class QueueEntryStatus { PENDING, SUPERSEDED }

data class QueueEntry(
    val key: String,
    val kind: String,
    val payload: JsonObject,
    val baseUpdatedAt: String? = null,
    val status: QueueEntryStatus = QueueEntryStatus.PENDING,
    val serverCopy: JsonObject? = null,
    val serverId: String? = null,
)

/** One server result entry from POST /api/app/sync. */
data class SyncResult(
    val key: String,
    val kind: String = "",
    val status: String = "",
    val id: String? = null,
    val duplicate: Boolean = false,
    val server: JsonObject? = null,
)

object OfflineLogic {

    /** Server contract: sync batches are at most this many mutations. */
    const val MAX_BATCH: Int = 50

    val KNOWN_KINDS: Set<String> = setOf("ticket_create", "ticket_note", "ticket_patch")

    /**
     * Enqueues [entry], deduped by key: an existing entry with the
     * same key is replaced in place (its newest payload wins) and
     * returns to PENDING — re-typing the same offline write must
     * never queue it twice.
     */
    fun enqueue(entries: List<QueueEntry>, entry: QueueEntry): List<QueueEntry> {
        val index = entries.indexOfFirst { it.key == entry.key }
        if (index < 0) return entries + entry
        val copy = entries.toMutableList()
        copy[index] = entry.copy(status = QueueEntryStatus.PENDING, serverCopy = null)
        return copy
    }

    /** The next batch to send: pending entries only, at most [MAX_BATCH]. */
    fun nextBatch(entries: List<QueueEntry>, limit: Int = MAX_BATCH): List<QueueEntry> =
        entries.filter { it.status == QueueEntryStatus.PENDING }.take(limit)

    /**
     * AUDIT-171 B-08: a write belongs in this queue ONLY when the
     * server never answered — a transport failure (no signal,
     * timeout, DNS). An [ApiException] means the server DID answer
     * and refused or failed the write on its merits; queueing a
     * refusal would replay a rejected write later and hide the
     * refusal from the person who made it.
     */
    fun shouldQueue(error: Throwable): Boolean = error !is ApiException

    /** The exact sync request body: {"mutations":[{key,kind,payload,baseUpdatedAt}]}. */
    fun buildSyncBody(batch: List<QueueEntry>): JsonObject = buildJsonObject {
        put("mutations", JsonArray(batch.map { entry ->
            buildJsonObject {
                put("key", entry.key)
                put("kind", entry.kind)
                put("payload", entry.payload)
                if (entry.baseUpdatedAt != null) {
                    put("baseUpdatedAt", entry.baseUpdatedAt)
                } else {
                    put("baseUpdatedAt", JsonNull)
                }
            }
        }))
    }

    /** Parses the results array of a sync response; junk → none. */
    fun parseResults(response: JsonObject): List<SyncResult> {
        val array = (response["results"] as? JsonArray)
            ?: (response["outcomes"] as? JsonArray)
            ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val key = (obj["key"] as? JsonPrimitive)?.contentOrNull
                ?: return@mapNotNull null
            SyncResult(
                key = key,
                kind = (obj["kind"] as? JsonPrimitive)?.contentOrNull ?: "",
                status = (obj["status"] as? JsonPrimitive)?.contentOrNull ?: "",
                id = (obj["id"] as? JsonPrimitive)?.contentOrNull,
                duplicate = (obj["duplicate"] as? JsonPrimitive)?.booleanOrNull ?: false,
                server = obj["server"] as? JsonObject,
            )
        }
    }

    fun isApplied(result: SyncResult): Boolean =
        result.duplicate ||
            result.status.lowercase() in setOf("applied", "ok", "created", "updated", "duplicate")

    fun isConflict(result: SyncResult): Boolean =
        result.status.lowercase() in setOf("conflict", "superseded", "stale")

    /**
     * Applies [results] to [entries]: applied/duplicate entries are
     * removed; conflicts are marked SUPERSEDED and kept with the
     * server copy recorded; everything else is left untouched.
     */
    fun applyResults(entries: List<QueueEntry>, results: List<SyncResult>): List<QueueEntry> {
        if (results.isEmpty()) return entries
        val byKey = results.associateBy { it.key }
        val out = ArrayList<QueueEntry>(entries.size)
        for (entry in entries) {
            val result = byKey[entry.key]
            when {
                result == null -> out.add(entry)
                isApplied(result) -> Unit // Server has it — the entry's job is done.
                isConflict(result) -> out.add(
                    entry.copy(
                        status = QueueEntryStatus.SUPERSEDED,
                        serverCopy = result.server ?: entry.serverCopy,
                        serverId = result.id ?: entry.serverId,
                    )
                )
                else -> out.add(entry)
            }
        }
        return out
    }

    fun serializeEntry(entry: QueueEntry): JsonObject = buildJsonObject {
        put("key", entry.key)
        put("kind", entry.kind)
        put("payload", entry.payload)
        if (entry.baseUpdatedAt != null) {
            put("baseUpdatedAt", entry.baseUpdatedAt)
        } else {
            put("baseUpdatedAt", JsonNull)
        }
        put("status", entry.status.name)
        entry.serverCopy?.let { put("serverCopy", it) }
        entry.serverId?.let { put("serverId", it) }
    }

    fun deserializeEntry(obj: JsonObject): QueueEntry? {
        val key = (obj["key"] as? JsonPrimitive)?.contentOrNull ?: return null
        val kind = (obj["kind"] as? JsonPrimitive)?.contentOrNull ?: return null
        val payload = obj["payload"] as? JsonObject ?: JsonObject(emptyMap())
        val status = runCatching {
            QueueEntryStatus.valueOf(
                (obj["status"] as? JsonPrimitive)?.contentOrNull ?: "PENDING"
            )
        }.getOrDefault(QueueEntryStatus.PENDING)
        return QueueEntry(
            key = key,
            kind = kind,
            payload = payload,
            baseUpdatedAt = (obj["baseUpdatedAt"] as? JsonPrimitive)?.contentOrNull,
            status = status,
            serverCopy = obj["serverCopy"] as? JsonObject,
            serverId = (obj["serverId"] as? JsonPrimitive)?.contentOrNull,
        )
    }

    fun serializeEntries(entries: List<QueueEntry>): String =
        buildJsonArray { for (entry in entries) add(serializeEntry(entry)) }.toString()

    fun deserializeEntries(text: String?): List<QueueEntry> {
        if (text.isNullOrBlank()) return emptyList()
        val array = runCatching { Json.parseToJsonElement(text).jsonArray }
            .getOrNull() ?: return emptyList()
        return array.mapNotNull { element ->
            (element as? JsonObject)?.let { deserializeEntry(it) }
        }
    }
}

/** What one [OfflineQueue.flush] did, in plain counts. */
data class FlushReport(
    val sent: Int,
    val removed: Int,
    val superseded: Int,
    val remaining: Int,
)

/**
 * SharedPreferences-backed offline queue. The whole queue is one
 * JSON array under one key — the queue is small (a phone's worth of
 * unsent ticket writes), and a single value keeps save/load atomic.
 */
class OfflineQueue(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun entries(): List<QueueEntry> =
        OfflineLogic.deserializeEntries(prefs.getString(KEY_ENTRIES, null))

    fun pending(): List<QueueEntry> =
        entries().filter { it.status == QueueEntryStatus.PENDING }

    fun superseded(): List<QueueEntry> =
        entries().filter { it.status == QueueEntryStatus.SUPERSEDED }

    fun enqueue(entry: QueueEntry) {
        saveAll(OfflineLogic.enqueue(entries(), entry))
    }

    fun enqueue(
        key: String,
        kind: String,
        payload: JsonObject,
        baseUpdatedAt: String? = null,
    ) {
        enqueue(QueueEntry(key = key, kind = kind, payload = payload, baseUpdatedAt = baseUpdatedAt))
    }

    fun saveAll(entries: List<QueueEntry>) {
        prefs.edit().putString(KEY_ENTRIES, OfflineLogic.serializeEntries(entries)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_ENTRIES).apply()
    }

    /**
     * Sends pending entries in batches of at most
     * [OfflineLogic.MAX_BATCH] until the queue drains or a batch
     * makes no progress (its results are all unknown — those entries
     * stay pending for the next flush rather than looping forever).
     * Network failure propagates to the caller; entries already
     * resolved by earlier batches stay resolved.
     */
    suspend fun flush(api: SentinelApi): FlushReport {
        var sent = 0
        var removed = 0
        var superseded = 0
        while (true) {
            val current = entries()
            val batch = OfflineLogic.nextBatch(current)
            if (batch.isEmpty()) break
            val response = api.syncMutations(OfflineLogic.buildSyncBody(batch))
            sent += batch.size
            val results = OfflineLogic.parseResults(response)
            val updated = OfflineLogic.applyResults(current, results)
            saveAll(updated)
            val batchKeys = batch.map { it.key }.toSet()
            removed += batch.count { entry ->
                updated.none { it.key == entry.key } && entry.key in batchKeys
            }
            superseded += updated.count { entry ->
                entry.key in batchKeys && entry.status == QueueEntryStatus.SUPERSEDED &&
                    current.firstOrNull { it.key == entry.key }?.status != QueueEntryStatus.SUPERSEDED
            }
            val progressed = results.any {
                OfflineLogic.isApplied(it) || OfflineLogic.isConflict(it)
            }
            if (!progressed) break
        }
        return FlushReport(
            sent = sent,
            removed = removed,
            superseded = superseded,
            remaining = entries().count { it.status == QueueEntryStatus.PENDING },
        )
    }

    companion object {
        const val FILE_NAME = "sentinel_offline_queue"
        private const val KEY_ENTRIES = "entries"
    }
}
