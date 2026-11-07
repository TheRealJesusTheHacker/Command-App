package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.data.DraftLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftLogicTest {

    private val now = 1_700_000_000_000L

    @Test
    fun deniedKeysAreSensitive() {
        assertTrue(DraftLogic.isSensitiveKey("password"))
        assertTrue(DraftLogic.isSensitiveKey("newPassphrase"))
        assertTrue(DraftLogic.isSensitiveKey("apiToken"))
        assertTrue(DraftLogic.isSensitiveKey("clientSecret"))
        assertTrue(DraftLogic.isSensitiveKey("totpCode"))
        assertTrue(DraftLogic.isSensitiveKey("cardNumber"))
        assertTrue(DraftLogic.isSensitiveKey("cvv"))
        assertFalse(DraftLogic.isSensitiveKey("title"))
        assertFalse(DraftLogic.isSensitiveKey("description"))
    }

    @Test
    fun filterDropsSensitiveKeepsOrdinary() {
        val kept = DraftLogic.filterFields(
            mapOf(
                "title" to "Printer is down",
                "password" to "hunter2",
                "notes" to "Second floor",
            )
        )
        assertEquals(
            mapOf("title" to "Printer is down", "notes" to "Second floor"),
            kept,
        )
    }

    @Test
    fun filterHonorsCallerFlaggedKeysAndTypes() {
        val kept = DraftLogic.filterFields(
            fields = mapOf("title" to "T", "pin" to "1234", "hint" to "blue"),
            sensitiveKeys = setOf("hint"),
            sensitiveTypeKeys = setOf("pin"),
        )
        assertEquals(mapOf("title" to "T"), kept)
    }

    @Test
    fun sensitiveTypeFlagAloneExcludes() {
        assertTrue(DraftLogic.isSensitiveField("anything", sensitiveType = true))
        assertFalse(DraftLogic.isSensitiveField("anything"))
    }

    @Test
    fun sevenDayPruneRule() {
        assertFalse(DraftLogic.isExpired(now, now))
        assertFalse(DraftLogic.isExpired(now, now + DraftLogic.MAX_AGE_MS))
        assertTrue(DraftLogic.isExpired(now, now + DraftLogic.MAX_AGE_MS + 1))
        assertTrue(DraftLogic.shouldKeep(now, now + 1000))
    }

    @Test
    fun fieldsRoundTripThroughJson() {
        val fields = mapOf("title" to "A \"quoted\" title", "body" to "line1\nline2")
        val restored = DraftLogic.deserializeFields(DraftLogic.serializeFields(fields))
        assertEquals(fields, restored)
    }

    @Test
    fun deserializeJunkIsEmpty() {
        assertEquals(emptyMap<String, String>(), DraftLogic.deserializeFields(null))
        assertEquals(emptyMap<String, String>(), DraftLogic.deserializeFields("not json"))
        assertEquals(emptyMap<String, String>(), DraftLogic.deserializeFields("[1,2]"))
    }

    @Test
    fun entryRoundTrip() {
        val entry = DraftLogic.DraftEntry(
            formId = "ticket-new",
            fields = mapOf("title" to "T"),
            savedAtMs = now,
        )
        val restored = DraftLogic.deserializeEntry("ticket-new", DraftLogic.serializeEntry(entry))
        assertEquals(entry, restored)
    }

    @Test
    fun deserializeEntryJunkIsNull() {
        assertNull(DraftLogic.deserializeEntry("x", null))
        assertNull(DraftLogic.deserializeEntry("x", "{broken"))
    }
}
