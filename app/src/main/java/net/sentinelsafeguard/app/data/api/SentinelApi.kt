package net.sentinelsafeguard.app.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sentinelsafeguard.app.config.ApiConfig
import net.sentinelsafeguard.app.config.AppConfig

/** The server refused the call. [message] is plain language, safe to show. */
class ApiException(val status: Int, override val message: String) : Exception(message)

/** The device token is gone or revoked — the app must return to sign-in. */
class SignedOutException : Exception("Your sign-in has ended. Please sign in again.")

sealed interface LoginResult {
    data class Success(val token: String, val displayName: String?, val role: String?) : LoginResult

    /** The passphrase verified; the server issued a single-use
     *  challenge that [SentinelApi.completeTotp] must answer. */
    data class TotpRequired(val challenge: String) : LoginResult
    data class Failed(val message: String) : LoginResult
}

/** The ticket-patch body as the backend reads it: only the fields
 *  being changed ride along (unset fields are omitted, never sent
 *  as nulls — the backend treats a present assignedUserId key,
 *  even null, as an assignment change). */
@Serializable
private data class TicketPatchWire(
    val status: String? = null,
    val priority: String? = null,
    val assignedUserId: Int? = null,
)

/**
 * Client for the Command app API. Every authenticated call
 * carries the device token in an Authorization header, plus the
 * Cloudflare Access service-auth headers when the Access layer has
 * supplied them. A 401 anywhere means the device token is dead:
 * [onSignedOut] fires and the call fails with [SignedOutException].
 */
class SentinelApi(
    private val config: ApiConfig,
    private val transport: HttpTransport,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = false },
) {
    @Volatile
    var deviceToken: String? = null

    /** The signed-in account as the backend last described it
     *  (login / me responses); used to resolve "assign to me". */
    @Volatile
    private var cachedUser: User? = null

    /** Set by the app shell; invoked once per 401 so the UI can sign out. */
    var onSignedOut: (() -> Unit)? = null

    private fun headers(authenticated: Boolean): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (config.cfAccessClientId.isNotBlank()) {
            map[AppConfig.CF_ACCESS_CLIENT_ID_HEADER] = config.cfAccessClientId
        }
        if (config.cfAccessClientSecret.isNotBlank()) {
            map[AppConfig.CF_ACCESS_CLIENT_SECRET_HEADER] = config.cfAccessClientSecret
        }
        if (authenticated) {
            val token = deviceToken
            if (!token.isNullOrBlank()) map["Authorization"] = "Bearer $token"
        }
        return map
    }

    private suspend fun call(
        method: String,
        path: String,
        body: String? = null,
        authenticated: Boolean = true,
    ): HttpResponse {
        val response = transport.execute(
            HttpRequest(method, path, body, headers(authenticated))
        )
        if (response.status == 401 && authenticated) {
            onSignedOut?.invoke()
            throw SignedOutException()
        }
        return response
    }

    private fun ensureOk(response: HttpResponse): HttpResponse {
        if (response.status !in 200..299) {
            throw ApiException(
                response.status,
                plainError(response.status, response.body),
            )
        }
        return response
    }

    suspend fun login(
        displayName: String,
        passphrase: String,
        deviceName: String,
    ): LoginResult {
        val payload = json.encodeToString(
            LoginRequest(displayName, passphrase, deviceName)
        )
        val response = transport.execute(
            HttpRequest("POST", "/api/app/auth/login", payload, headers(authenticated = false))
        )
        val parsed = runCatching { json.decodeFromString<LoginResponse>(response.body) }.getOrNull()
        // The backend's real TOTP shape: HTTP 200, totpRequired=true,
        // plus the single-use challenge the completion call answers.
        val challenge = parsed?.challenge
        if (parsed?.totpRequired == true && !challenge.isNullOrBlank()) {
            return LoginResult.TotpRequired(challenge)
        }
        val token = parsed?.deviceToken
        if (response.status in 200..299 && !token.isNullOrBlank()) {
            deviceToken = token
            cachedUser = parsed.user
            return LoginResult.Success(token, parsed.user?.displayName, parsed.user?.role)
        }
        return LoginResult.Failed(
            parsed?.message ?: plainError(response.status, response.body)
        )
    }

    /** Second leg of enrollment sign-in: answer the challenge from
     *  [login] with the 6-digit code. The backend's completion shape
     *  is {challenge, code, deviceName} on the same endpoint — the
     *  passphrase is NOT resent. */
    suspend fun completeTotp(
        challenge: String,
        code: String,
        deviceName: String,
    ): LoginResult {
        val payload = json.encodeToString(
            TotpCompleteRequest(challenge, code, deviceName)
        )
        val response = transport.execute(
            HttpRequest("POST", "/api/app/auth/login", payload, headers(authenticated = false))
        )
        val parsed = runCatching { json.decodeFromString<LoginResponse>(response.body) }.getOrNull()
        val token = parsed?.deviceToken
        if (response.status in 200..299 && !token.isNullOrBlank()) {
            deviceToken = token
            cachedUser = parsed.user
            return LoginResult.Success(token, parsed.user?.displayName, parsed.user?.role)
        }
        return LoginResult.Failed(
            parsed?.message ?: plainError(response.status, response.body)
        )
    }

    suspend fun logout() {
        try {
            call("POST", "/api/app/auth/logout")
        } catch (ignored: Exception) {
            // Signing out locally matters even if the server is unreachable;
            // the token is wiped by the caller either way.
        } finally {
            deviceToken = null
            cachedUser = null
        }
    }

    suspend fun me(): MeResponse {
        val out = json.decodeFromString<MeResponse>(ensureOk(call("GET", "/api/app/me")).body)
        cachedUser = out.user
        return out
    }

    /** The signed-in account's id, from the cached login/me user —
     *  fetching /me once if nothing is cached yet. */
    private suspend fun myUserId(): Int =
        cachedUser?.id ?: me().user.id

    suspend fun today(): TodayResponse =
        json.decodeFromString(ensureOk(call("GET", "/api/app/today")).body)

    /** The backend's filters are a stage name (?status=) and an
     *  assignee selector (?assignee=me / unassigned / a user id). */
    suspend fun tickets(status: String? = null, assignee: String? = null): List<Ticket> {
        val params = mutableListOf<String>()
        if (!status.isNullOrBlank()) params += "status=${urlEncode(status)}"
        if (!assignee.isNullOrBlank()) params += "assignee=${urlEncode(assignee)}"
        val path = "/api/app/tickets" +
            if (params.isEmpty()) "" else "?" + params.joinToString("&")
        return json.decodeFromString<TicketListResponse>(ensureOk(call("GET", path)).body).tickets
    }

    /** The detail envelope's ticket has no clientName; the name is
     *  backfilled from the embedded client so callers see the same
     *  Ticket shape the list returns. */
    suspend fun ticket(id: Int): Ticket {
        val detail = json.decodeFromString<TicketDetailResponse>(
            ensureOk(call("GET", "/api/app/tickets/$id")).body
        )
        return detail.ticket.copy(
            clientName = detail.ticket.clientName ?: detail.client?.legalName
        )
    }

    suspend fun patchTicket(id: Int, patch: TicketPatchRequest): Ticket {
        val wire = TicketPatchWire(
            status = patch.status,
            priority = patch.priority,
            assignedUserId = patch.assignedUserId
                ?: if (patch.assignToMe == true) myUserId() else null,
        )
        return json.decodeFromString<TicketWriteResponse>(
            ensureOk(call("PATCH", "/api/app/tickets/$id", json.encodeToString(wire))).body
        ).ticket
    }

    suspend fun addTicketNote(id: Int, body: String): Ticket =
        json.decodeFromString<TicketWriteResponse>(
            ensureOk(
                call("POST", "/api/app/tickets/$id/notes", json.encodeToString(NoteRequest(body)))
            ).body
        ).ticket

    suspend fun createTicket(request: TicketCreateRequest): Ticket =
        json.decodeFromString<TicketWriteResponse>(
            ensureOk(call("POST", "/api/app/tickets", json.encodeToString(request))).body
        ).ticket

    suspend fun clients(query: String? = null): List<Client> {
        val path = if (query.isNullOrBlank()) "/api/app/clients"
        else "/api/app/clients?q=${urlEncode(query)}"
        return json.decodeFromString<ClientListResponse>(ensureOk(call("GET", path)).body).clients
    }

    suspend fun client(id: Int): Client =
        json.decodeFromString<ClientDetailResponse>(
            ensureOk(call("GET", "/api/app/clients/$id")).body
        ).client

    suspend fun pipeline(): PipelineResponse =
        json.decodeFromString(ensureOk(call("GET", "/api/app/pipeline")).body)

    /** No range argument: with no window parameters the backend
     *  serves its default window — today through six days out —
     *  which is exactly what the schedule screen renders. */
    suspend fun schedule(): List<Job> =
        json.decodeFromString<ScheduleResponse>(
            ensureOk(call("GET", "/api/app/schedule")).body
        ).jobs

    suspend fun job(id: Int): Job =
        json.decodeFromString<JobDetailResponse>(
            ensureOk(call("GET", "/api/app/jobs/$id")).body
        ).job

    suspend fun devices(): List<DeviceInfo> =
        json.decodeFromString<DeviceListResponse>(ensureOk(call("GET", "/api/app/devices")).body).devices

    suspend fun revokeDevice(id: Int) {
        ensureOk(call("DELETE", "/api/app/devices/$id"))
    }

    suspend fun version(): VersionResponse =
        json.decodeFromString(ensureOk(call("GET", "/api/app/version")).body)

    /** Streams the signed update APK (device-token gated, spec §7). */
    suspend fun downloadUpdate(sink: (ByteArray) -> Unit): Int {
        val response = transport.download(
            HttpRequest("GET", "/api/app/update.apk", null, headers(authenticated = true)),
            sink,
        )
        if (response.status == 401) {
            onSignedOut?.invoke()
            throw SignedOutException()
        }
        if (response.status !in 200..299) {
            throw ApiException(response.status, plainError(response.status, ""))
        }
        return response.status
    }

    private fun urlEncode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8")

    companion object {
        /** Every error the user sees says what happened and what to do. */
        fun plainError(status: Int, body: String): String = when (status) {
            0 -> "No connection. Check your internet and try again."
            400 -> "The server could not use what was sent. Check the details and try again."
            403 -> "This account is not allowed to do that. Ask the owner if that seems wrong."
            404 -> "That item could not be found. It may have been removed — go back and refresh."
            429 -> "Too many tries in a row. Wait a minute, then try again."
            in 500..599 -> "The server hit a problem. Try again in a little while."
            else -> "Something went wrong (server said $status). Try again."
        }
    }
}
