package net.sentinelsafeguard.app

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.OfflineLogic
import net.sentinelsafeguard.app.data.QueueEntry
import net.sentinelsafeguard.app.data.QueueEntryStatus
import net.sentinelsafeguard.app.data.SyncResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineQueueLogicTest {

    private fun entry(key: String, kind: String = "ticket_note", body: String = "hi") =
        QueueEntry(
            key = key,
            kind = kind,
            payload = buildJsonObject { put("body", body) },
            baseUpdatedAt = "2026-11-07T10:00:00Z",
        )

    @Test
    fun enqueueAppendsNewKeys() {
        val out = OfflineLogic.enqueue(listOf(entry("a")), entry("b"))
        assertEquals(listOf("a", "b"), out.map { it.key })
    }

    @Test
    fun enqueueDedupesByKeyKeepingNewestPayload() {
        val first = OfflineLogic.enqueue(emptyList(), entry("a", body = "old"))
        val second = OfflineLogic.enqueue(first, entry("a", body = "new"))
        assertEquals(1, second.size)
        val payload = second[0].payload["body"] as JsonPrimitive
        assertEquals("new", payload.content)
        assertEquals(QueueEntryStatus.PENDING, second[0].status)
    }

    @Test
    fun nextBatchCapsAtFiftyAndSkipsSuperseded() {
        var entries = (1..60).map { entry("k$it") }
        entries = entries + entry("done").copy(status = QueueEntryStatus.SUPERSEDED)
        val batch = OfflineLogic.nextBatch(entries)
        assertEquals(50, batch.size)
        assertTrue(batch.all { it.status == QueueEntryStatus.PENDING })
    }

    @Test
    fun syncBodyMatchesContractShape() {
        val body = OfflineLogic.buildSyncBody(listOf(entry("a", kind = "ticket_create")))
        val mutations = body["mutations"]!!.jsonArray
        assertEquals(1, mutations.size)
        val first = mutations[0].jsonObject
        assertEquals("a", (first["key"] as JsonPrimitive).content)
        assertEquals("ticket_create", (first["kind"] as JsonPrimitive).content)
        assertEquals("2026-11-07T10:00:00Z", (first["baseUpdatedAt"] as JsonPrimitive).content)
        assertTrue(first["payload"] is JsonObject)
    }

    @Test
    fun appliedAndDuplicateResultsRemoveEntries() {
        val entries = listOf(entry("a"), entry("b"), entry("c"))
        val out = OfflineLogic.applyResults(
            entries,
            listOf(
                SyncResult(key = "a", status = "applied", id = "41"),
                SyncResult(key = "b", status = "applied", duplicate = true),
            ),
        )
        assertEquals(listOf("c"), out.map { it.key })
    }

    @Test
    fun conflictMarksSupersededAndKeepsServerCopy() {
        val server = buildJsonObject { put("title", "Server title") }
        val out = OfflineLogic.applyResults(
            listOf(entry("a")),
            listOf(SyncResult(key = "a", status = "conflict", id = "9", server = server)),
        )
        assertEquals(1, out.size)
        assertEquals(QueueEntryStatus.SUPERSEDED, out[0].status)
        assertEquals(server, out[0].serverCopy)
        assertEquals("9", out[0].serverId)
    }

    @Test
    fun unknownStatusAndMissingResultKeepEntryPending() {
        val out = OfflineLogic.applyResults(
            listOf(entry("a"), entry("b")),
            listOf(SyncResult(key = "a", status = "mystery")),
        )
        assertEquals(2, out.size)
        assertTrue(out.all { it.status == QueueEntryStatus.PENDING })
    }

    @Test
    fun parseResultsReadsContractEntries() {
        val response = buildJsonObject {
            put("results", kotlinx.serialization.json.JsonArray(listOf(
                buildJsonObject {
                    put("key", "a"); put("kind", "ticket_note")
                    put("status", "conflict"); put("id", 7)
                    put("server", buildJsonObject { put("title", "S") })
                },
                buildJsonObject {
                    put("key", "b"); put("kind", "ticket_patch")
                    put("status", "applied"); put("duplicate", true)
                },
            )))
        }
        val results = OfflineLogic.parseResults(response)
        assertEquals(2, results.size)
        assertEquals("7", results[0].id)
        assertEquals("conflict", results[0].status)
        assertTrue(results[1].duplicate)
    }

    @Test
    fun entriesRoundTripThroughSerialization() {
        val entries = listOf(
            entry("a"),
            entry("b", kind = "ticket_patch").copy(
                status = QueueEntryStatus.SUPERSEDED,
                serverCopy = buildJsonObject { put("x", 1) },
                serverId = "3",
            ),
        )
        val restored = OfflineLogic.deserializeEntries(OfflineLogic.serializeEntries(entries))
        assertEquals(entries, restored)
    }
}
