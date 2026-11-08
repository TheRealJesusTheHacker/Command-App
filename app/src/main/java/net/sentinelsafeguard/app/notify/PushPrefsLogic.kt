package net.sentinelsafeguard.app.notify

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.ui.Phase3Logic

/**
 * 1.5.2 (Elevation F4): the pure shape of Command's per-user
 * push event preferences, mirrored in the app's Settings.
 *
 * The prefs live on the SERVER — migration 0169's
 * user_push_prefs store, shared by the web actions and
 * GET/POST /api/app/push/prefs — so this logic only parses
 * and shapes. It NEVER invents a value: a response that does
 * not carry all three boolean kinds is unparseable, and the
 * surface shows an honest error instead of guessed defaults.
 * The server's own default (no row = all ON) is only ever
 * seen because the server itself reports it that way.
 *
 * These toggles govern which event types the SERVER enqueues
 * for this user (honored at enqueue — a disabled type creates
 * no outbox row). They do not change anything on this device:
 * channels, quiet hours and the Android permission live in
 * the local notification settings, untouched by this surface.
 */
object PushPrefsLogic {

    const val TICKET_ASSIGNED = "ticket_assigned"
    const val TICKET_REPLIED = "ticket_replied"
    const val CRITICAL_ALERT = "critical_alert"

    /** The three event kinds, in the server's order. */
    val KEYS: List<String> = listOf(TICKET_ASSIGNED, TICKET_REPLIED, CRITICAL_ALERT)

    /** Plain-words Settings labels for the server kinds. */
    fun label(key: String): String = when (key) {
        TICKET_ASSIGNED -> "A ticket is assigned to me"
        TICKET_REPLIED -> "A client replies to a ticket"
        CRITICAL_ALERT -> "Critical alerts"
        else -> key
    }

    /** One line on what each server event type is. */
    fun note(key: String): String = when (key) {
        TICKET_ASSIGNED ->
            "Command sends a push when a ticket lands on you."
        TICKET_REPLIED ->
            "Command sends a push when a client answers a ticket."
        CRITICAL_ALERT ->
            "Command sends a push for a Critical Alert."
        else -> ""
    }

    /** The three server values, exactly as reported. */
    data class Prefs(
        val ticketAssigned: Boolean,
        val ticketReplied: Boolean,
        val criticalAlert: Boolean,
    ) {
        fun value(key: String): Boolean = when (key) {
            TICKET_ASSIGNED -> ticketAssigned
            TICKET_REPLIED -> ticketReplied
            CRITICAL_ALERT -> criticalAlert
            else -> false
        }

        fun with(key: String, value: Boolean): Prefs = when (key) {
            TICKET_ASSIGNED -> copy(ticketAssigned = value)
            TICKET_REPLIED -> copy(ticketReplied = value)
            CRITICAL_ALERT -> copy(criticalAlert = value)
            else -> this
        }
    }

    /**
     * Parse a GET/POST /api/app/push/prefs response
     * ({ok, prefs:{ticket_assigned, ticket_replied,
     * critical_alert}}) — all three kinds must be present as
     * booleans; anything else returns null so the caller can
     * fail honestly instead of rendering invented state.
     */
    fun parse(response: JsonObject): Prefs? {
        val prefs = Phase3Logic.obj(response, "prefs") ?: return null
        return Prefs(
            ticketAssigned = Phase3Logic.bool(prefs, TICKET_ASSIGNED) ?: return null,
            ticketReplied = Phase3Logic.bool(prefs, TICKET_REPLIED) ?: return null,
            criticalAlert = Phase3Logic.bool(prefs, CRITICAL_ALERT) ?: return null,
        )
    }

    /**
     * The partial write body for one toggle:
     * {"prefs": {"<kind>": <value>}} — omitted kinds keep
     * their server value; the response is the full readback.
     */
    fun writeBody(key: String, value: Boolean): JsonObject = buildJsonObject {
        put("prefs", buildJsonObject { put(key, value) })
    }
}
