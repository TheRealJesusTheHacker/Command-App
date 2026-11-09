package net.sentinelsafeguard.app.data.api

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 1.4.0 ("Do All") endpoint methods: the offline-sync replay, the
 * selfhost push consumption surface, and the backup-bundle file
 * download. Like Phases 3–9 these ride the open-JSON verbs on
 * [SentinelApi]; refusals surface the server's own message via
 * [SentinelApi.callJson] — never a fake empty state.
 * 1.5.4 adds the website-analytics read (below).
 */

private fun SentinelApi.doAllQs(params: Map<String, String?>): String {
    val parts = params.entries.filter { !it.value.isNullOrBlank() }
        .map { "${it.key}=${encodeQuery(it.value!!)}" }
    return if (parts.isEmpty()) "" else "?" + parts.joinToString("&")
}

// ---- Offline sync (POST /api/app/sync) ----

/**
 * Replays queued offline mutations. [body] is the exact contract
 * shape {"mutations":[{key,kind,payload,baseUpdatedAt}]} — built by
 * OfflineLogic.buildSyncBody — and the response carries the
 * per-mutation results ({key,kind,status,id?,duplicate?,server?})
 * that OfflineLogic.parseResults reads.
 */
suspend fun SentinelApi.syncMutations(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/sync", body.toString())

// ---- Selfhost push (device long-poll + ACK) ----

/**
 * Registers this install's push token. In the selfhost design the
 * server stores the token (hash + last4, plus an envelope-encrypted
 * copy) and delivers by the device polling its outbox — no
 * third-party push service is involved. [token] is the stable
 * per-install token from PushStore.
 */
suspend fun SentinelApi.pushRegister(token: String): JsonObject =
    callJson(
        "POST", "/api/app/push/register",
        buildJsonObject {
            put("platform", "android")
            put("token", token)
        }.toString(),
    )

/** Removes this device's push registration (sign-out). */
suspend fun SentinelApi.pushUnregister(): JsonObject =
    callJson("POST", "/api/app/push/unregister", "{}")

/**
 * Polls this device's outbox for events after [since] (the PushStore
 * cursor). [wait] is the server's long-poll hold in seconds; the
 * WorkManager consumer passes 0 — see PushWorker for why.
 */
suspend fun SentinelApi.pushPoll(since: Long, wait: Int = 0): JsonObject =
    callJson(
        "GET", "/api/app/push/poll" + doAllQs(
            mapOf("since" to since.toString(), "wait" to wait.toString())
        )
    )

/**
 * 1.5.2 (Elevation F4): reads this user's server-side push
 * event preferences ({ok, prefs:{ticket_assigned,
 * ticket_replied, critical_alert}}). One store backs the web
 * actions and these endpoints (migration 0169), so what this
 * returns is what Command on the web shows — no local copy.
 */
suspend fun SentinelApi.pushPrefs(): JsonObject =
    callJson("GET", "/api/app/push/prefs")

/**
 * Writes a partial prefs change ({"prefs": {…}} — omitted
 * kinds keep their value) and returns the same readback
 * shape as [pushPrefs]: the server's full current state.
 */
suspend fun SentinelApi.pushPrefsSet(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/push/prefs", body.toString())

/**
 * Acknowledges delivery of the outbox events [ids]; the server marks
 * them sent so they are not redelivered. Numeric ids are sent as
 * numbers, matching the server's outbox id type.
 */
suspend fun SentinelApi.pushAck(ids: List<String>): JsonObject =
    callJson(
        "POST", "/api/app/push/ack",
        buildJsonObject {
            put("ids", JsonArray(ids.map { id ->
                id.toLongOrNull()?.let { JsonPrimitive(it) } ?: JsonPrimitive(id)
            }))
        }.toString(),
    )

// ---- Website analytics (GET /api/app/analytics) ----

/**
 * 1.5.4: the app twin of the web dashboard's analyticssummary
 * payload — the server reuses the web handler itself, so the
 * body is that payload byte-for-byte ({authorized, available,
 * visitors, pageviews, visits, bounceRate, avgVisitSecs,
 * activeVisitors, topPages, topReferrers, periodDays}, or
 * {authorized:true, available:false, reason}). The admin:read
 * gate lives server-side: a non-admin device token is refused
 * 403 (surfacing here as an [ApiException] with status 403 —
 * AnalyticsLogic.stateForError classifies it), and a dead
 * token is the standard 401 → SignedOutException path inside
 * [SentinelApi.call]. Parsing lives in AnalyticsLogic; this
 * method only rides the wire.
 */
suspend fun SentinelApi.analytics(): JsonObject =
    callJson("GET", "/api/app/analytics")

// ---- Briefing audio (FEATUREWAVE Item 22) ----

/**
 * 1.6.1: the morning-briefing audio, through the server's
 * getwysaudio action — the caller NEVER names a file: the
 * server recomputes the CURRENT While You Slept card's hash
 * and serves the cached clip for exactly that hash
 * ({authorized, available, hash} or {…, contentType,
 * audioB64}). The action is OWNER-only in-handler; every
 * other actor gets the denied shape {authorized:false}.
 *
 * Route honesty (verified against the shipped staging
 * source, 2026-11-09): no /api/app/* route wraps this
 * action — it is served on the web shell's POST /actions
 * surface, whose actor resolution reads a session token the
 * app does not hold. This call therefore rides that surface
 * with the app's standard device-auth headers and an EMPTY
 * args object (the action called bare, exactly as the
 * contract prescribes); on current server builds the answer
 * is the denied shape, and Wave161Logic + the card degrade
 * to the quiet "not generated yet" line — never an error.
 * The moment the server accepts device auth for the action
 * (or adds an app wrap), the same call returns audioB64 and
 * playback lights up with no further client change. The
 * response is parsed by Wave161Logic.parseWysAudio.
 */
suspend fun SentinelApi.wysAudio(): JsonObject =
    callJson(
        "POST", "/actions",
        buildJsonObject {
            put("action", "getwysaudio")
            put("args", buildJsonObject { })
        }.toString(),
    )

// ---- Backup bundle download ----

/**
 * Downloads the backup bundle file to [sink]; returns the HTTP
 * status. [urlPath] is the downloadUrl from adminBackupStatus —
 * a path like "/backup/download?t=..." carrying a single-use token.
 * Honest gate note: that single-use token IS the gate for this
 * file route (it is minted by the OWNER-only status handler and
 * dies after one use); this call adds the device-token headers via
 * [SentinelApi.downloadBytes] and no other credential.
 */
suspend fun SentinelApi.backupDownload(urlPath: String, sink: (ByteArray) -> Unit): Int =
    downloadBytes(urlPath, sink)
