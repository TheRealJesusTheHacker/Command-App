package net.sentinelsafeguard.app.data.api

import kotlinx.serialization.Serializable

/**
 * Wire models for the Command app API (/api/app/* endpoints).
 *
 * CONTRACT (hunt3, 2026-11-02): every shape below mirrors the
 * production backend's own response builders (server.py pub_user /
 * pub_self, pub_client, pub_ticket / pub_case, _pub_job, the
 * _app_* handlers, and the a_gettoday / a_getschedule / profile
 * payloads the app routes delegate to), verified field-by-field
 * against real response bodies captured from the byte-exact
 * production code. Ids and epoch-millisecond stamps are numbers on
 * the wire — never strings. Unknown fields are ignored so a backend
 * addition never breaks the app; sections the app does not render
 * (the Today cf*/b3* blocks, client-detail registers, cfOps) ride
 * the real bodies unparsed by design.
 */

// ---- Shared shapes ----

/** pub_user + pub_self: the account shape login and /me return. */
@Serializable
data class User(
    val id: Int,
    val displayName: String,
    val role: String,
    val portalLang: String = "en",
    val clientId: Int? = null,
    val jobTitle: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val active: Boolean = true,
    val createdAt: Long = 0,
    val mustReset: Boolean = false,
    val totpEnrolled: Boolean = false,
    val totpEnrollmentRequired: Boolean = false,
)

/** _app_device_shape: one enrolled device row. */
@Serializable
data class DeviceInfo(
    val id: Int,
    val deviceName: String,
    val createdAt: Long = 0,
    val lastSeenAt: Long = 0,
    val expiresAt: Long = 0,
    val revoked: Boolean = false,
    val revokedAt: Long? = null,
    val current: Boolean = false,
)

/** The paged section envelope the backend's list sections use. */
@Serializable
data class Section<T>(
    val total: Int = 0,
    val items: List<T> = emptyList(),
)

/** The plain {ok, message} body (logout, device revoke, errors). */
@Serializable
data class OkResponse(
    val ok: Boolean = false,
    val message: String? = null,
)

// ---- Auth ----

@Serializable
data class LoginRequest(
    val displayName: String,
    val passphrase: String,
    val deviceName: String,
)

/**
 * POST /api/app/auth/login, both legs. Success carries the device
 * token plus the nested user/device objects; a TOTP-confirmed
 * account first answers totpRequired + challenge (the completion
 * call posts [TotpCompleteRequest] to the same endpoint).
 */
@Serializable
data class LoginResponse(
    val ok: Boolean = false,
    val deviceToken: String? = null,
    val expiresAt: Long? = null,
    val user: User? = null,
    val device: DeviceInfo? = null,
    val totpRequired: Boolean = false,
    val challenge: String? = null,
    val challengeExpiresAt: Long? = null,
    val message: String? = null,
)

@Serializable
data class TotpCompleteRequest(
    val challenge: String,
    val code: String,
    val deviceName: String,
)

@Serializable
data class MeResponse(
    val ok: Boolean = true,
    val user: User,
    val serverTime: Long = 0,
    val device: DeviceInfo? = null,
)

// ---- Clients, cases, tickets (pub_client / pub_case / pub_ticket) ----

@Serializable
data class Client(
    val id: Int,
    val legalName: String = "",
    val dba: String = "",
    val type: String = "",
    val industry: String = "",
    val status: String = "",
    val stage: String = "Lead",
    val value: Double? = null,
    val nextFollowUp: String? = null,
    val source: String = "",
    val sourceDetail: String = "",
    val contactName: String = "",
    val contactTitle: String = "",
    val email: String = "",
    val phone: String = "",
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val zip: String = "",
    val notes: String = "",
    val tags: String? = null,
    val importBatchId: String? = null,
    val archivedAt: Long? = null,
    val archivedBy: Int? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    // Added by the app-surface builders (list / detail / Today).
    val tier: String = "",
    val familyLinked: Boolean = false,
)

/** pub_case — also the shape of Today's overdue-case rows. */
@Serializable
data class CaseItem(
    val id: Int,
    val clientId: Int? = null,
    val assignedUserId: Int? = null,
    val title: String = "",
    val status: String = "Open",
    val priority: String = "Normal",
    val category: String = "",
    val dueDate: String? = null,
    val openedDate: String = "",
    val closedDate: String = "",
    val notes: String = "",
    val adMaterial: Boolean = false,
    val archivedAt: Long? = null,
    val archivedBy: Int? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    // Added by the Today builder only.
    val clientName: String? = null,
)

/**
 * pub_ticket (+ clientName on the app list/write shapes). A
 * ticket's notes are ONE string on the wire — added notes are
 * appended into it by the backend; there is no note list.
 */
@Serializable
data class Ticket(
    val id: Int,
    val clientId: Int? = null,
    val assignedUserId: Int? = null,
    val title: String = "",
    val status: String = "Open",
    val priority: String = "Normal",
    val category: String = "",
    val dueDate: String? = null,
    val openedDate: String = "",
    val closedDate: String = "",
    val notes: String = "",
    val adMaterial: Boolean = false,
    val archivedAt: Long? = null,
    val archivedBy: Int? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val caseId: Int? = null,
    val source: String = "Internal",
    val sourceRef: String? = null,
    val mergedIntoId: Int? = null,
    val clientName: String? = null,
)

/** pub_activity — the timeline rows inside a ticket profile. */
@Serializable
data class ActivityItem(
    val id: Int,
    val actorId: Int? = null,
    val action: String = "",
    val entityType: String = "",
    val entityId: Int? = null,
    val detail: String? = null,
    val createdAt: Long = 0,
)

// ---- Today (GET /api/app/today → a_gettoday payload + ok) ----

@Serializable
data class SignInAlert(
    val id: Int,
    val actorId: Int? = null,
    val actorName: String = "",
    val action: String = "",
    val detail: String? = null,
    val ip: String = "",
    val createdAt: Long = 0,
)

@Serializable
data class MoneyBucket(
    val count: Int = 0,
    val totalCents: Long = 0,
)

@Serializable
data class TodayMoney(
    val outstanding: MoneyBucket = MoneyBucket(),
    val overdue: MoneyBucket = MoneyBucket(),
    val dueThisWeek: MoneyBucket = MoneyBucket(),
)

@Serializable
data class DueThisWeek(
    val cases: Section<CaseItem> = Section(),
    val followUps: Section<Client> = Section(),
)

@Serializable
data class TodayResponse(
    val ok: Boolean = true,
    val today: String = "",
    val followUps: Section<Client> = Section(),
    val tierA: Section<Client> = Section(),
    val newTickets: Section<Ticket> = Section(),
    val overdueCases: Section<CaseItem> = Section(),
    val signinAlerts: Section<SignInAlert> = Section(),
    val money: TodayMoney = TodayMoney(),
    val dueThisWeek: DueThisWeek = DueThisWeek(),
    val websiteTickets: Section<Ticket> = Section(),
    val briefing: String = "",
)

// ---- Tickets API ----

@Serializable
data class TicketListResponse(
    val ok: Boolean = true,
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
    val tickets: List<Ticket> = emptyList(),
)

/** GET /tickets/{id}: the profile envelope around the ticket.
 *  Its ticket carries no clientName; the name rides the embedded
 *  client (full pub_client, or the lite {id, legalName, name}
 *  shape for callers without clients:read — Client parses both). */
@Serializable
data class TicketDetailResponse(
    val ok: Boolean = true,
    val ticket: Ticket,
    val client: Client? = null,
    val case: CaseItem? = null,
    val activity: List<ActivityItem> = emptyList(),
)

/** Create (201, with id), patch, and note responses. */
@Serializable
data class TicketWriteResponse(
    val ok: Boolean = true,
    val id: Int? = null,
    val ticket: Ticket,
)

@Serializable
data class TicketPatchRequest(
    val status: String? = null,
    val priority: String? = null,
    val assignedUserId: Int? = null,
    /** UI-level only: SentinelApi resolves this to the signed-in
     *  account's id and sends assignedUserId. Never on the wire. */
    val assignToMe: Boolean? = null,
)

/** The backend reads the note text from "note" (or "text"). */
@Serializable
data class NoteRequest(val note: String)

@Serializable
data class TicketCreateRequest(
    val title: String,
    val notes: String? = null,
    val priority: String = "Normal",
    val clientId: Int? = null,
    val caseId: Int? = null,
    val assignedUserId: Int? = null,
    val category: String? = null,
    val status: String? = null,
    val dueDate: String? = null,
)

// ---- Clients & pipeline API ----

@Serializable
data class ClientListResponse(
    val ok: Boolean = true,
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
    val clients: List<Client> = emptyList(),
)

/** GET /clients/{id}: the profile envelope. The app renders the
 *  client record; the cases/tickets/assets/invoices/forms and
 *  register sections in the same body are not modeled (unknown
 *  keys, ignored by the parser). */
@Serializable
data class ClientDetailResponse(
    val ok: Boolean = true,
    val client: Client,
)

@Serializable
data class PipelineCard(
    val id: Int,
    val legalName: String = "",
    val stage: String = "",
    val value: Double? = null,
    val nextFollowUp: String? = null,
    val tier: String = "",
)

@Serializable
data class PipelineStage(
    val stage: String,
    val count: Int = 0,
    val clients: List<PipelineCard> = emptyList(),
)

@Serializable
data class PipelineResponse(
    val ok: Boolean = true,
    val stages: List<PipelineStage> = emptyList(),
    val totalClients: Int = 0,
)

// ---- Schedule API (_pub_job via a_getschedule / a_getjob) ----

@Serializable
data class JobAssignee(
    val id: Int,
    val displayName: String = "",
    val role: String = "",
)

@Serializable
data class Job(
    val id: Int,
    val title: String = "",
    val clientId: Int? = null,
    val caseId: Int? = null,
    val clientName: String? = null,
    val clientContactName: String? = null,
    val clientContactTitle: String? = null,
    val clientEmail: String? = null,
    val clientPhone: String? = null,
    val caseTitle: String? = null,
    val location: String = "",
    val workDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val allDay: Boolean = false,
    val status: String = "",
    val notes: String = "",
    val assignees: List<JobAssignee> = emptyList(),
    val assigneeIds: List<Int> = emptyList(),
    val createdBy: Int? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

@Serializable
data class ScheduleRange(
    val startDate: String? = null,
    val endDate: String? = null,
)

/** The employees / entries arrays in the same body are not
 *  modeled; the schedule screen renders jobs only. */
@Serializable
data class ScheduleResponse(
    val ok: Boolean = true,
    val scope: String = "",
    val jobs: List<Job> = emptyList(),
    val range: ScheduleRange? = null,
)

@Serializable
data class JobDetailResponse(
    val ok: Boolean = true,
    val job: Job,
)

// ---- Devices & version ----

@Serializable
data class DeviceListResponse(
    val ok: Boolean = true,
    val devices: List<DeviceInfo> = emptyList(),
)

/** GET /api/app/version: the staged-release facts. version and
 *  versionCode are null (and available false) when nothing is
 *  staged — an honest "no update", never an invented version. */
@Serializable
data class VersionResponse(
    val ok: Boolean = true,
    val available: Boolean = false,
    val version: String? = null,
    val versionCode: Int? = null,
    val notes: String = "",
    val fileName: String? = null,
    val sizeBytes: Long? = null,
    val sha256: String? = null,
    val downloadPath: String? = null,
    val message: String? = null,
)
