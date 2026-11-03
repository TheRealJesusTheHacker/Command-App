package net.sentinelsafeguard.app.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
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


    // ---- Phase 2 (hunt6): Cases / Schedule writes / Engagements /
    // Runbooks & Sign-off / Today panels / global search ----

    /** Open-JSON GET for the Today panel family: the panel
     *  payloads are the web handlers' own section objects, and the
     *  Today screen renders them section by section. */
    suspend fun getJson(path: String): JsonObject =
        Json.parseToJsonElement(ensureOk(call("GET", path)).body).jsonObject

    suspend fun postJson(path: String, body: String = "{}"): JsonObject =
        Json.parseToJsonElement(ensureOk(call("POST", path, body)).body).jsonObject

    /** The case list with the B3 filter/search/sort vocabulary,
     *  paged through completely like every mirrored list. */
    suspend fun casesFiltered(
        q: String? = null,
        status: String? = null,
        priority: String? = null,
        category: String? = null,
        clientId: Int? = null,
        assignee: String? = null,
        overdue: Boolean = false,
        unassigned: Boolean = false,
        adMaterial: Boolean = false,
        sort: String? = null,
    ): Page<CaseItem> {
        val params = mutableListOf<String>()
        if (!q.isNullOrBlank()) params += "q=${urlEncode(q)}"
        if (!status.isNullOrBlank()) params += "status=${urlEncode(status)}"
        if (!priority.isNullOrBlank()) params += "priority=${urlEncode(priority)}"
        if (!category.isNullOrBlank()) params += "category=${urlEncode(category)}"
        if (clientId != null) params += "clientId=$clientId"
        if (!assignee.isNullOrBlank()) params += "assignee=${urlEncode(assignee)}"
        if (overdue) params += "overdue=1"
        if (unassigned) params += "unassigned=1"
        if (adMaterial) params += "adMaterial=1"
        if (!sort.isNullOrBlank()) params += "sort=${urlEncode(sort)}"
        return fetchAllPages(
            "/api/app/cases", params,
            decode = { json.decodeFromString<CaseListResponse>(it) },
            itemsOf = { it.cases },
            totalOf = { it.total },
        )
    }

    /** The full case profile envelope (case + client + tickets + activity). */
    suspend fun caseDetail(id: Int): CaseDetailResponse =
        json.decodeFromString(ensureOk(call("GET", "/api/app/cases/$id")).body)

    /** Full case write body (savecase field set). Nullables ride as
     *  explicit JSON nulls when cleared — savecase semantics. */
    private fun caseBody(edit: CaseFullEdit): String = buildJsonObject {
        if (edit.clientId != null) put("clientId", edit.clientId) else put("clientId", JsonNull)
        if (edit.assignedUserId != null) put("assignedUserId", edit.assignedUserId)
        else put("assignedUserId", JsonNull)
        put("title", edit.title)
        put("status", edit.status)
        put("priority", edit.priority)
        put("category", edit.category)
        if (edit.dueDate != null) put("dueDate", edit.dueDate) else put("dueDate", JsonNull)
        put("notes", edit.notes)
        put("adMaterial", edit.adMaterial)
    }.toString()

    suspend fun createCase(edit: CaseFullEdit): CaseItem =
        json.decodeFromString<CaseWriteResponse>(
            ensureOk(call("POST", "/api/app/cases", caseBody(edit))).body
        ).case

    suspend fun patchCase(id: Int, edit: CaseFullEdit): CaseItem =
        json.decodeFromString<CaseWriteResponse>(
            ensureOk(call("PATCH", "/api/app/cases/$id", caseBody(edit))).body
        ).case

    suspend fun setCaseStatus(id: Int, status: String): CaseItem =
        json.decodeFromString<CaseStatusResponse>(
            ensureOk(
                call(
                    "POST", "/api/app/cases/$id/status",
                    json.encodeToString(StageRequest(status)),
                )
            ).body
        ).case

    suspend fun archiveCase(id: Int): CaseItem =
        json.decodeFromString<CaseWriteResponse>(
            ensureOk(call("POST", "/api/app/cases/$id/archive", "{}")).body
        ).case

    suspend fun restoreCase(id: Int): CaseItem =
        json.decodeFromString<CaseWriteResponse>(
            ensureOk(call("POST", "/api/app/cases/$id/restore", "{}")).body
        ).case

    /** A schedule window with web date-window semantics: jobs,
     *  employees, and entries for [start]..[end] (yyyy-MM-dd). */
    suspend fun scheduleWindow(
        start: String,
        end: String,
        status: String? = null,
        employeeId: String? = null,
    ): ScheduleResponse {
        val params = mutableListOf("start=${urlEncode(start)}", "end=${urlEncode(end)}")
        if (!status.isNullOrBlank() && status != "All") params += "status=${urlEncode(status)}"
        if (!employeeId.isNullOrBlank() && employeeId != "All") {
            params += "employeeId=${urlEncode(employeeId)}"
        }
        return json.decodeFromString(
            ensureOk(call("GET", "/api/app/schedule?${params.joinToString("&")}")).body
        )
    }

    private fun jobBody(edit: JobFullEdit): String = buildJsonObject {
        put("title", edit.title)
        if (edit.clientId != null) put("clientId", edit.clientId) else put("clientId", JsonNull)
        if (edit.caseId != null) put("caseId", edit.caseId) else put("caseId", JsonNull)
        put("location", edit.location)
        put("workDate", edit.workDate)
        put("startTime", edit.startTime)
        put("endTime", edit.endTime)
        put("allDay", edit.allDay)
        put("status", edit.status)
        put("notes", edit.notes)
        put("assigneeIds", kotlinx.serialization.json.JsonArray(
            edit.assigneeIds.map { kotlinx.serialization.json.JsonPrimitive(it) }
        ))
    }.toString()

    suspend fun createJob(edit: JobFullEdit): Job =
        json.decodeFromString<JobWriteResponse>(
            ensureOk(call("POST", "/api/app/jobs", jobBody(edit))).body
        ).job

    suspend fun patchJob(id: Int, edit: JobFullEdit): Job =
        json.decodeFromString<JobWriteResponse>(
            ensureOk(call("PATCH", "/api/app/jobs/$id", jobBody(edit))).body
        ).job

    suspend fun setJobStatus(id: Int, status: String): Job =
        json.decodeFromString<JobStatusResponse>(
            ensureOk(
                call(
                    "POST", "/api/app/jobs/$id/status",
                    json.encodeToString(StageRequest(status)),
                )
            ).body
        ).job

    suspend fun deleteJob(id: Int) {
        ensureOk(call("DELETE", "/api/app/jobs/$id"))
    }

    suspend fun saveScheduleEntry(request: ScheduleEntryRequest): Int? {
        val body = buildJsonObject {
            if (request.id != null) put("id", request.id)
            put("userId", request.userId)
            put("entryType", request.entryType)
            put("workDate", request.workDate)
            put("startTime", request.startTime)
            put("endTime", request.endTime)
            put("notes", request.notes)
        }
        return json.decodeFromString<EntrySaveResponse>(
            ensureOk(call("POST", "/api/app/schedule-entries", body.toString())).body
        ).id
    }

    suspend fun deleteScheduleEntry(id: Int) {
        ensureOk(call("DELETE", "/api/app/schedule-entries/$id"))
    }

    suspend fun engagements(clientId: Int? = null): EngagementsListResponse {
        val path = if (clientId != null) "/api/app/engagements?clientId=$clientId"
        else "/api/app/engagements"
        return json.decodeFromString(ensureOk(call("GET", path)).body)
    }

    suspend fun engagement(id: Int): Engagement =
        json.decodeFromString<EngagementDetailResponse>(
            ensureOk(call("GET", "/api/app/engagements/$id")).body
        ).engagement

    private fun engagementBody(edit: EngagementEdit): String = buildJsonObject {
        if (edit.clientId != null) put("clientId", edit.clientId) else put("clientId", JsonNull)
        put("title", edit.title)
        put("scope", edit.scope)
        put("rulesOfEngagement", edit.rulesOfEngagement)
        put("status", edit.status)
        if (edit.startDate != null) put("startDate", edit.startDate) else put("startDate", JsonNull)
        if (edit.endDate != null) put("endDate", edit.endDate) else put("endDate", JsonNull)
    }.toString()

    suspend fun createEngagement(edit: EngagementEdit): Engagement =
        json.decodeFromString<EngagementWriteResponse>(
            ensureOk(call("POST", "/api/app/engagements", engagementBody(edit))).body
        ).engagement

    suspend fun patchEngagement(id: Int, edit: EngagementEdit): Engagement =
        json.decodeFromString<EngagementWriteResponse>(
            ensureOk(call("PATCH", "/api/app/engagements/$id", engagementBody(edit))).body
        ).engagement

    suspend fun rbProcedures(): List<RbProcedure> =
        json.decodeFromString<RbProceduresResponse>(
            ensureOk(call("GET", "/api/app/runbooks/procedures")).body
        ).procedures

    suspend fun rbSetProcedure(id: Int, isProcedure: Boolean, steps: List<RbStep>): RbProcedure? {
        val body = buildJsonObject {
            put("isProcedure", isProcedure)
            put("steps", kotlinx.serialization.json.JsonArray(steps.map { step ->
                buildJsonObject {
                    put("title", step.title)
                    put("detail", step.detail)
                }
            }))
        }
        return json.decodeFromString<RbProcedureSetResponse>(
            ensureOk(call("POST", "/api/app/runbooks/procedures/$id", body.toString())).body
        ).procedure
    }

    suspend fun rbRuns(ticketId: Int? = null): List<RbRun> {
        val path = if (ticketId != null) "/api/app/runbooks/runs?ticketId=$ticketId"
        else "/api/app/runbooks/runs"
        return json.decodeFromString<RbRunsResponse>(ensureOk(call("GET", path)).body).runs
    }

    suspend fun rbRun(id: Int): RbRun? {
        val out = json.decodeFromString<RbRunsResponse>(
            ensureOk(call("GET", "/api/app/runbooks/runs/$id")).body
        )
        return out.run ?: out.runs.firstOrNull { it.id == id }
    }

    suspend fun rbStartRun(kbId: Int, ticketId: Int?): RbRun? {
        val body = buildJsonObject {
            put("kbId", kbId)
            if (ticketId != null) put("ticketId", ticketId) else put("ticketId", JsonNull)
        }
        return json.decodeFromString<RbRunResponse>(
            ensureOk(call("POST", "/api/app/runbooks/runs", body.toString())).body
        ).run
    }

    suspend fun rbCheckStep(id: Int, stepIndex: Int, note: String = "", undo: Boolean = false): RbRun? {
        val body = buildJsonObject {
            put("stepIndex", stepIndex)
            put("note", note)
            put("undo", undo)
        }
        return json.decodeFromString<RbRunResponse>(
            ensureOk(call("POST", "/api/app/runbooks/runs/$id/check-step", body.toString())).body
        ).run
    }

    /** which: pause | resume | cancel (the run lifecycle verbs). */
    suspend fun rbRunState(id: Int, which: String): RbRun? =
        json.decodeFromString<RbRunResponse>(
            ensureOk(call("POST", "/api/app/runbooks/runs/$id/$which", "{}")).body
        ).run

    suspend fun rbExtraction(): RbExtractionResponse =
        json.decodeFromString(ensureOk(call("GET", "/api/app/runbooks/extraction-status")).body)

    suspend fun rbBackfill(): JsonObject = postJson("/api/app/runbooks/backfill")

    suspend fun ticketSignOff(id: Int): SignOffResponse =
        json.decodeFromString(ensureOk(call("GET", "/api/app/tickets/$id/signoff")).body)

    suspend fun postTicketSignOff(id: Int, signerName: String, clientId: Int?): SignOffResponse {
        val body = buildJsonObject {
            put("signerName", signerName)
            if (clientId != null) put("clientId", clientId)
        }
        return json.decodeFromString(
            ensureOk(call("POST", "/api/app/tickets/$id/signoff", body.toString())).body
        )
    }

    suspend fun todayPanels(): JsonObject = getJson("/api/app/today/panels")

    suspend fun todayAsk(question: String): AskResponse {
        val body = buildJsonObject { put("q", question) }
        return json.decodeFromString(
            ensureOk(call("POST", "/api/app/today/ask", body.toString())).body
        )
    }

    suspend fun todayAutomations(): JsonObject = getJson("/api/app/today/automations")

    suspend fun runAutomations(): JsonObject = postJson("/api/app/today/automations/run")

    suspend fun slaScan(): JsonObject = postJson("/api/app/today/sla-scan")

    suspend fun todayDigests(): JsonObject = getJson("/api/app/today/digests")

    suspend fun generateDigests(): JsonObject = postJson("/api/app/today/digests/generate")

    suspend fun sendDigest(id: Int): JsonObject = postJson("/api/app/today/digests/$id/send")

    suspend fun discardDigest(id: Int): JsonObject =
        postJson("/api/app/today/digests/$id/discard")

    suspend fun todayEstateHealth(): JsonObject = getJson("/api/app/today/estate-health")

    suspend fun todayPlatformDigest(): JsonObject = getJson("/api/app/today/platform-digest")

    suspend fun todayCalendarSync(): CalendarSyncResponse =
        json.decodeFromString(ensureOk(call("GET", "/api/app/today/calendar-sync")).body)

    suspend fun todayReportsSummary(): JsonObject = getJson("/api/app/today/reports-summary")

    suspend fun todayStaffCerts(): JsonObject = getJson("/api/app/today/staff-certs")

    suspend fun todayRegisters(clientId: Int? = null): JsonObject {
        val path = if (clientId != null) "/api/app/today/registers?clientId=$clientId"
        else "/api/app/today/registers"
        return getJson(path)
    }

    suspend fun todayUrgent(): JsonObject = getJson("/api/app/today/urgent")

    /** Global search (G-D4): web globalsearch, role-scoped server-side. */
    suspend fun search(q: String): SearchResponse =
        json.decodeFromString(
            ensureOk(call("GET", "/api/app/search?q=${urlEncode(q)}")).body
        )

    // ---- Phase 3 (hunt7) generic verbs: the Phase 3 surface is
    // web-handler payloads end to end, so its endpoints ride the
    // open-JSON pattern the Today panels established, extended to
    // the remaining verbs + binary up/downloads. Phase3Api.kt
    // holds the named endpoint methods on top of these. ----

    /** Open-JSON call for any verb. On refusal the server's own
     *  `message` is surfaced when it sent one — web's confirm and
     *  hard gates (merge, revoke, signed-authorization) explain
     *  themselves in that message, and the mirror shows web's
     *  words, not a paraphrase. Other refusals keep plainError. */
    suspend fun callJson(method: String, path: String, body: String? = null): JsonObject {
        val response = call(method, path, body)
        if (response.status !in 200..299) {
            val serverMessage = runCatching {
                Json.parseToJsonElement(response.body).jsonObject["message"]
                    ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
            }.getOrNull()
            throw ApiException(
                response.status,
                serverMessage?.takeIf { it.isNotBlank() }
                    ?: plainError(response.status, response.body),
            )
        }
        if (response.body.isBlank()) return JsonObject(emptyMap())
        return Json.parseToJsonElement(response.body).jsonObject
    }

    suspend fun patchJson(path: String, body: String): JsonObject =
        callJson("PATCH", path, body)

    suspend fun deleteJson(path: String): JsonObject = callJson("DELETE", path)

    /** Raw-byte upload (photos / voice notes): web's X-* header
     *  contract rides in [extraHeaders]; the response is the
     *  endpoint's JSON body. */
    suspend fun uploadBytes(
        path: String,
        bytes: ByteArray,
        extraHeaders: Map<String, String>,
    ): JsonObject {
        val response = transport.execute(
            HttpRequest(
                "POST", path, null,
                headers(authenticated = true) + extraHeaders,
                bodyBytes = bytes,
            )
        )
        if (response.status == 401) {
            onSignedOut?.invoke()
            throw SignedOutException()
        }
        val ok = ensureOk(response)
        if (ok.body.isBlank()) return JsonObject(emptyMap())
        return Json.parseToJsonElement(ok.body).jsonObject
    }

    /** Binary download to a sink (photo / diagram / voice bytes). */
    suspend fun downloadBytes(path: String, sink: (ByteArray) -> Unit): Int {
        val response = transport.download(
            HttpRequest("GET", path, null, headers(authenticated = true)),
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

    internal fun encodeQuery(value: String): String = urlEncode(value)

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

/** The full-edit case body (Phase 2) — see [SentinelApi.patchCase]. */
data class CaseFullEdit(
    val clientId: Int?,
    val assignedUserId: Int?,
    val title: String,
    val status: String,
    val priority: String,
    val category: String,
    val dueDate: String?,
    val notes: String,
    val adMaterial: Boolean,
)

/** The full job body (Phase 2, web savejob field set). */
data class JobFullEdit(
    val title: String,
    val clientId: Int?,
    val caseId: Int?,
    val location: String,
    val workDate: String,
    val startTime: String,
    val endTime: String,
    val allDay: Boolean,
    val status: String,
    val notes: String,
    val assigneeIds: List<Int>,
)

/** A schedule-entry save (Phase 2, web savescheduleentry fields). */
data class ScheduleEntryRequest(
    val id: Int? = null,
    val userId: Int,
    val entryType: String,
    val workDate: String,
    val startTime: String = "",
    val endTime: String = "",
    val notes: String = "",
)

/** The engagement save body (Phase 2, web savecfengagement fields). */
data class EngagementEdit(
    val clientId: Int?,
    val title: String,
    val scope: String,
    val rulesOfEngagement: String,
    val status: String,
    val startDate: String?,
    val endDate: String?,
)
