package net.sentinelsafeguard.app.ui

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Pure helpers for the Phase 3 (hunt7) mirrored surfaces — kept
 * free of Compose so the unit suite can pin them against the
 * real captured payloads (ContractPhase3Test).
 */
object Phase3Logic {

    // ---- tolerant JSON reads (web payloads are open-shaped) ----

    fun str(obj: JsonObject?, key: String): String =
        (obj?.get(key) as? JsonPrimitive)?.contentOrNullSafe() ?: ""

    fun strOrNull(obj: JsonObject?, key: String): String? =
        (obj?.get(key) as? JsonPrimitive)?.contentOrNullSafe()

    fun int(obj: JsonObject?, key: String): Int? =
        (obj?.get(key) as? JsonPrimitive)?.content?.toIntOrNull()

    fun long(obj: JsonObject?, key: String): Long? =
        (obj?.get(key) as? JsonPrimitive)?.content?.toLongOrNull()

    fun dbl(obj: JsonObject?, key: String): Double? =
        (obj?.get(key) as? JsonPrimitive)?.content?.toDoubleOrNull()

    fun bool(obj: JsonObject?, key: String): Boolean? =
        (obj?.get(key) as? JsonPrimitive)?.content?.toBooleanStrictOrNull()

    fun arr(obj: JsonObject?, key: String): List<JsonObject> =
        (obj?.get(key) as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

    fun arrRaw(obj: JsonObject?, key: String): List<JsonElement> =
        (obj?.get(key) as? JsonArray)?.toList() ?: emptyList()

    fun obj(obj: JsonObject?, key: String): JsonObject? = obj?.get(key) as? JsonObject

    private fun JsonPrimitive.contentOrNullSafe(): String? =
        if (this.isString) this.content
        else if (this.content == "null") null
        else this.content

    /** A display value for a payload field: strings as-is, epoch
     *  millis (13-digit) as a date-time, cents keys as money. */
    fun displayValue(key: String, element: JsonElement?): String? {
        if (element == null || element is kotlinx.serialization.json.JsonNull) return null
        val prim = element as? JsonPrimitive ?: return null
        val raw = prim.contentOrNullSafe() ?: return null
        if (raw.isBlank()) return null
        if (key.endsWith("Cents") || key == "totalCents" || key == "amountCents") {
            return raw.toLongOrNull()?.let { MirrorVocab.formatCents(it) } ?: raw
        }
        val asLong = raw.toLongOrNull()
        if (asLong != null && asLong > 1_000_000_000_000L && asLong < 10_000_000_000_000L &&
            (key.endsWith("At") || key == "at")
        ) {
            return formatEpochMs(asLong)
        }
        if (raw == "true") return "Yes"
        if (raw == "false") return "No"
        return raw
    }

    // ---- Sentinel Verified (web verificationsView semantics) ----

    /** Web computes Valid from dates; a revoked record stays
     *  Revoked; an active record past its expiry reads Expired. */
    fun verificationStatus(v: JsonObject, today: String): String {
        val stored = str(v, "status").ifBlank { str(v, "storedStatus") }
        if (stored.equals("revoked", ignoreCase = true)) return "Revoked"
        val expiry = str(v, "expiryDate")
        if (expiry.isNotBlank() && expiry < today) return "Expired"
        return "Valid"
    }

    // ---- Duplicates (web duplicatesView) ----

    val MERGE_FIELDS: List<Pair<String, String>> = listOf(
        "legalName" to "Legal name",
        "dba" to "DBA / company",
        "stage" to "Pipeline stage",
        "status" to "Status",
        "contactName" to "Contact",
        "email" to "Email",
        "phone" to "Phone",
        "value" to "Value",
    )

    fun suggestedWinnerId(pair: JsonObject): Int? =
        int(pair, "suggestedWinnerId")

    // ---- Saved views / presets ----

    /** The clients-directory stage a saved view's criteria names,
     *  when it names one (web applycfview semantics for the
     *  mirrored directory filter). */
    fun criteriaStage(criteria: JsonObject?): String? =
        criteria?.let { strOrNull(it, "stage") }?.takeIf { it.isNotBlank() }

    fun criteriaQuery(criteria: JsonObject?): String? =
        criteria?.let { strOrNull(it, "query") ?: strOrNull(it, "q") }
            ?.takeIf { it.isNotBlank() }

    // ---- Hygiene ----

    val HYGIENE_STATES: List<String> = listOf("yes", "no", "unknown")

    fun hygieneCounts(items: List<JsonObject>): Triple<Int, Int, Int> {
        var yes = 0
        var no = 0
        var unknown = 0
        items.forEach {
            when (str(it, "state")) {
                "yes" -> yes++
                "no" -> no++
                else -> unknown++
            }
        }
        return Triple(yes, no, unknown)
    }

    // ---- Time tracking ----

    fun formatDurationSeconds(totalSeconds: Long): String {
        val minutes = totalSeconds / 60
        val hours = minutes / 60
        val rem = minutes % 60
        return if (hours > 0) "${hours}h ${rem}m" else "${rem}m"
    }

    fun formatTargetMinutes(min: Int?): String = when {
        min == null -> ""
        min >= 60 && min % 60 == 0 -> "${min / 60}h"
        min >= 60 -> "${min / 60}h ${min % 60}m"
        else -> "${min}m"
    }

    // ---- Scan gate (web cf2assurance) ----

    /** Web's hard-gate refusal copy begins with this prefix; the
     *  app surfaces the server's message verbatim either way —
     *  this recognizer only lets the UI style a refusal as the
     *  gate (not a generic failure). */
    fun isGateRefusal(message: String): Boolean =
        message.contains("signed authorization", ignoreCase = true) ||
            message.contains("Hard gate", ignoreCase = true)

    // ---- Outreach channels (web profileView outreach panel) ----

    val OUTREACH_CHANNELS: List<String> = listOf("Email", "Phone script", "Text message")

    // ---- Booking admin ----

    val WEEKDAYS: List<String> = listOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday",
    )

    fun weekdayLabel(weekday: Int?): String =
        weekday?.let { WEEKDAYS.getOrNull(it) } ?: ""
}
