package net.sentinelsafeguard.app.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.config.ApiConfig
import net.sentinelsafeguard.app.config.AppConfig

/** The server refused the call. [message] is plain language, safe to show. */
class ApiException(val status: Int, override val message: String) : Exception(message)

/**
 * One complete list from a paged backend envelope: every item the
 * envelope holds (all pages fetched), plus the backend's own
 * [total] so screens can show the true count. Before hunt4 the app
 * read only the first page (50) and called it the list — the CRM
 * holds ~266 clients, so 216 were silently invisible on the phone.
 */
data class Page<T>(val items: List<T>, val total: Int)

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
    val adMaterial: Boolean? = null,
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

    /**
     * Walk a paged list envelope to the end: request pages at the
     * backend's maximum page size until everything the envelope's
     * [totalOf] reports has been collected. A short or empty page
     * also ends the walk, so a total that moves mid-fetch can never
     * loop forever.
     */
    private suspend fun <T, R> fetchAllPages(
        basePath: String,
        params: List<String>,
        decode: (String) -> R,
        itemsOf: (R) -> List<T>,
        totalOf: (R) -> Int,
    ): Page<T> {
        val all = mutableListOf<T>()
        var total = Int.MAX_VALUE
        var offset = 0
        while (all.size < total) {
            val query = (params + "limit=$PAGE_SIZE" + "offset=$offset")
                .joinToString("&")
            val page = decode(ensureOk(call("GET", "$basePath?$query")).body)
            val items = itemsOf(page)
            total = totalOf(page)
            if (items.isEmpty()) break
            all += items
            offset += items.size
        }
        return Page(all, if (total == Int.MAX_VALUE) all.size else total)
    }

    /** The backend's filters are a stage name (?status=) and an
     *  assignee selector (?assignee=me / unassigned / a user id).
     *  The full filtered set comes back — every page, not just the
     *  first — with the backend's true filtered total. */
    suspend fun tickets(status: String? = null, assignee: String? = null): Page<Ticket> {
        val params = mutableListOf<String>()
        if (!status.isNullOrBlank()) params += "status=${urlEncode(status)}"
        if (!assignee.isNullOrBlank()) params += "assignee=${urlEncode(assignee)}"
        return fetchAllPages(
            "/api/app/tickets", params,
            decode = { json.decodeFromString<TicketListResponse>(it) },
            itemsOf = { it.tickets },
            totalOf = { it.total },
        )
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
            adMaterial = patch.adMaterial,
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

    /**
     * The complete client list — every page of the envelope, with
     * the backend's true total. Search rides the backend's own ?q=
     * (it matches name, business, contact, email and phone across
     * the whole client base server-side), and the matches are paged
     * through completely too, so search is never limited to page 1.
     */
    suspend fun clients(query: String? = null): Page<Client> {
        val params = mutableListOf<String>()
        if (!query.isNullOrBlank()) params += "q=${urlEncode(query)}"
        return fetchAllPages(
            "/api/app/clients", params,
            decode = { json.decodeFromString<ClientListResponse>(it) },
            itemsOf = { it.clients },
            totalOf = { it.total },
        )
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


    // ---- hunt5 mirror endpoints (B1–B8) ----

    suspend fun dashboard(): DashboardResponse =
        json.decodeFromString(ensureOk(call("GET", "/api/app/dashboard")).body)

    /** Active accounts for assignee pickers (B2). */
    suspend fun roster(): List<User> =
        json.decodeFromString<RosterResponse>(ensureOk(call("GET", "/api/app/roster")).body).users

    /**
     * The ticket list with the B3 filter/search/sort surface.
     * Null/blank parameters are omitted; the full filtered set is
     * paged through completely, never capped.
     */
    suspend fun ticketsFiltered(
        q: String? = null,
        status: String? = null,
        priority: String? = null,
        category: String? = null,
        clientId: Int? = null,
        caseId: Int? = null,
        overdue: Boolean = false,
        unassigned: Boolean = false,
        noCase: Boolean = false,
        adMaterial: Boolean = false,
        sort: String? = null,
    ): Page<Ticket> {
        val params = mutableListOf<String>()
        if (!q.isNullOrBlank()) params += "q=${urlEncode(q)}"
        if (!status.isNullOrBlank()) params += "status=${urlEncode(status)}"
        if (!priority.isNullOrBlank()) params += "priority=${urlEncode(priority)}"
        if (!category.isNullOrBlank()) params += "category=${urlEncode(category)}"
        if (clientId != null) params += "clientId=$clientId"
        if (caseId != null) params += "caseId=$caseId"
        if (overdue) params += "overdue=1"
        if (unassigned) params += "unassigned=1"
        if (noCase) params += "noCase=1"
        if (adMaterial) params += "adMaterial=1"
        if (!sort.isNullOrBlank()) params += "sort=${urlEncode(sort)}"
        return fetchAllPages(
            "/api/app/tickets", params,
            decode = { json.decodeFromString<TicketListResponse>(it) },
            itemsOf = { it.tickets },
            totalOf = { it.total },
        )
    }

    /** The full profile envelope: ticket + client + case + activity. */
    suspend fun ticketDetail(id: Int): TicketDetailResponse =
        json.decodeFromString(ensureOk(call("GET", "/api/app/tickets/$id")).body)

    /**
     * Full edit (B3): rides the web saveticket path server-side;
     * a status in the same body then rides setticketstatus, so
     * the closure follow-up pair and the other web side effects
     * fire exactly once, server-side. clientId / caseId /
     * assignedUserId / dueDate are sent as explicit JSON nulls
     * when cleared — saveticket semantics need the key present.
     */
    suspend fun patchTicketFull(id: Int, edit: TicketFullEdit): Ticket {
        val body = buildJsonObject {
            put("title", edit.title)
            put("status", edit.status)
            put("priority", edit.priority)
            if (edit.assignedUserId != null) put("assignedUserId", edit.assignedUserId)
            else put("assignedUserId", JsonNull)
            put("category", edit.category)
            if (edit.dueDate != null) put("dueDate", edit.dueDate) else put("dueDate", JsonNull)
            if (edit.clientId != null) put("clientId", edit.clientId) else put("clientId", JsonNull)
            if (edit.caseId != null) put("caseId", edit.caseId) else put("caseId", JsonNull)
            put("adMaterial", edit.adMaterial)
            put("notes", edit.notes)
        }
        return json.decodeFromString<TicketWriteResponse>(
            ensureOk(call("PATCH", "/api/app/tickets/$id", body.toString())).body
        ).ticket
    }

    suspend fun archiveTicket(id: Int): Ticket =
        json.decodeFromString<TicketWriteResponse>(
            ensureOk(call("POST", "/api/app/tickets/$id/archive", "{}")).body
        ).ticket

    suspend fun restoreTicket(id: Int): Ticket =
        json.decodeFromString<TicketWriteResponse>(
            ensureOk(call("POST", "/api/app/tickets/$id/restore", "{}")).body
        ).ticket

    /** B6: Closed + OWNER gate lives server-side (web requestreview). */
    suspend fun requestReview(id: Int) {
        ensureOk(call("POST", "/api/app/tickets/$id/request-review", "{}"))
    }

    suspend fun portalMessages(id: Int): PortalMessagesResponse =
        json.decodeFromString(ensureOk(call("GET", "/api/app/tickets/$id/portal-messages")).body)

    suspend fun postPortalMessage(id: Int, text: String): PortalMessage? =
        json.decodeFromString<PortalMessagePostResponse>(
            ensureOk(
                call(
                    "POST", "/api/app/tickets/$id/portal-messages",
                    json.encodeToString(PortalMessagePostRequest(text)),
                )
            ).body
        ).message

    /** The full profile envelope (linked registers included). */
    suspend fun clientDetail(id: Int): ClientDetailResponse =
        json.decodeFromString(ensureOk(call("GET", "/api/app/clients/$id")).body)

    suspend fun createClient(request: ClientWriteRequest): Client =
        json.decodeFromString<ClientWriteResponse>(
            ensureOk(call("POST", "/api/app/clients", json.encodeToString(request))).body
        ).client

    suspend fun patchClient(id: Int, request: ClientWriteRequest): Client =
        json.decodeFromString<ClientWriteResponse>(
            ensureOk(call("PATCH", "/api/app/clients/$id", json.encodeToString(request))).body
        ).client

    /** B7 stage move — web setclientstage semantics + logging. */
    suspend fun setClientStage(id: Int, stage: String): Client =
        json.decodeFromString<ClientStageResponse>(
            ensureOk(
                call(
                    "POST", "/api/app/clients/$id/stage",
                    json.encodeToString(StageRequest(stage)),
                )
            ).body
        ).client

    suspend fun archiveClient(id: Int): Client =
        json.decodeFromString<ClientWriteResponse>(
            ensureOk(call("POST", "/api/app/clients/$id/archive", "{}")).body
        ).client

    suspend fun restoreClient(id: Int): Client =
        json.decodeFromString<ClientWriteResponse>(
            ensureOk(call("POST", "/api/app/clients/$id/restore", "{}")).body
        ).client

    /**
     * B5 follow-up: [days] snoozes from today (Today's +7 days);
     * otherwise [nextFollowUp] sets the date, or clears it when null.
     */
    suspend fun clientFollowup(id: Int, days: Int? = null, nextFollowUp: String? = null): Client {
        val body = buildJsonObject {
            if (days != null) put("days", days)
            else if (nextFollowUp != null) put("nextFollowUp", nextFollowUp)
            else put("nextFollowUp", JsonNull)
        }
        return json.decodeFromString<ClientWriteResponse>(
            ensureOk(call("POST", "/api/app/clients/$id/followup", body.toString())).body
        ).client
    }

    /** B8: the profile Timeline card. */
    suspend fun clientTimeline(id: Int): ClientTimelineResponse =
        json.decodeFromString(ensureOk(call("GET", "/api/app/clients/$id/timeline")).body)

    private fun urlEncode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8")

    companion object {
        /** The backend's own maximum page size (_app_page caps
         *  limit at 200) — fewest round trips, no invented cap. */
        private const val PAGE_SIZE = 200

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

/** The full-edit ticket body (B3) — see [SentinelApi.patchTicketFull]. */
data class TicketFullEdit(
    val title: String,
    val status: String,
    val priority: String,
    val assignedUserId: Int?,
    val category: String,
    val dueDate: String?,
    val clientId: Int?,
    val caseId: Int?,
    val adMaterial: Boolean,
    val notes: String,
)

@Serializable
private data class StageRequest(val stage: String)
