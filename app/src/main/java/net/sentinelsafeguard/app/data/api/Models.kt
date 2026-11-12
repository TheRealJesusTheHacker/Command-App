package net.sentinelsafeguard.app.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

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
    // Role-structure window (server W4, 2026-11-08): the
    // account's STRUCTURE — "employee" or "client", derived
    // server-side from the role and carried additively on
    // pub_user/pub_self. Nullable: payloads from before the
    // window simply omit it, and the app derives nothing in
    // its place (see MirrorNav.shellKind / structureLabel).
    val structureRole: String? = null,
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
    // B0 (hunt5): the boot access-flag set, mirroring the web
    // workspace payload so the mirrored shell gates exactly
    // like web Command.
    val lawAccess: Boolean = false,
    val newsAccess: Boolean = false,
    val osintAccess: Boolean = false,
    val breachAccess: Boolean = false,
    val threatAccess: Boolean = false,
    val reviewsAccess: Boolean = false,
    val taxAccess: Boolean = false,
    val taxOnly: Boolean = false,
    // Phase 9 (Addendum 28): true for the web portal accounts
    // (client structure) — the app shell routes those accounts
    // to the portal shell, never the staff drawer. (Family is
    // a client-RECORD tier since the role-structure window,
    // never an account role.)
    val portal: Boolean = false,
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
    // ⚠️ [tier] is the TRIAGE tag token ("tier-a"…"tier-d" or "")
    // — never read it for billing. The billing tier is [tierState].
    val tier: String = "",
    val familyLinked: Boolean = false,
    /** Friends tier (migration 0143, live in production): 20% off;
     *  Family-linked records are never charged and take precedence. */
    val friends: Boolean = false,
    /** The record's Family flag (TIER Phase B): the stored tier
     *  fact [familyLinked] mirrors on the staff surface. */
    val family: Boolean = false,
    /** The server's three-state billing tier ("family" |
     *  "friends" | "standard", Family precedence — TIER Phase B).
     *  The source of truth for tier display and charge gating
     *  (see TierLogic). Empty when a payload predates the field;
     *  TierLogic then derives from the flags, as it always has. */
    val tierState: String = "",
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
    // Added by the Phase 2 app case builders (list / write rows).
    val ticketCount: Int = 0,
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
    /** Phase 2 backend (Addendum 12): list rows now carry the
     *  linked case's title additively — the hunt5 deviation
     *  (board cards showed "Case #<id>" with no title) closes. */
    val caseTitle: String? = null,
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
    // hunt5 mirror: the sections the hunt-era screen dropped.
    val assetsFlagged: Section<AssetItem> = Section(),
    val assetRenewals: AssetRenewals = AssetRenewals(),
    val planRenewals: PlanRenewals = PlanRenewals(),
    val platformDigest: PlatformDigest? = null,
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
    // FEATUREWAVE Item 23 (1.6.1): the stage-ordered photo
    // timeline (pub_cf_photo rows + stage/stageLabel), kept as
    // open JSON like every other web-shaped section — the
    // parsing/rendering lives in Wave161Logic/Wave161Screens.
    val photoTimeline: List<JsonObject> = emptyList(),
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
    val adMaterial: Boolean? = null,
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

/** GET /clients/{id}: the profile envelope (web getclientprofile
 *  verbatim). hunt5 renders the client record plus the linked
 *  registers, contacts/notification prefs, and verifications;
 *  the remaining register sections (roadmap/risks/breach/
 *  compliance/security score/site profiles) stay unparsed —
 *  they are Phase 3 surfaces. */
@Serializable
data class ClientDetailResponse(
    val ok: Boolean = true,
    val client: Client,
    val cases: List<CaseItem> = emptyList(),
    val tickets: List<Ticket> = emptyList(),
    val assets: List<AssetItem> = emptyList(),
    val invoices: List<InvoiceLite> = emptyList(),
    val forms: List<FormLite> = emptyList(),
    val verifications: List<Verification> = emptyList(),
    val contacts: List<ClientContact> = emptyList(),
    val notificationPrefs: NotificationPrefs? = null,
)

@Serializable
data class PipelineStage(
    val stage: String,
    val count: Int = 0,
    /** B7 (hunt5): full pub_client cards (+ familyLinked/friends/tier). */
    val clients: List<Client> = emptyList(),
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

/** pub_schedule (+ userName from the schedule read): a personal
 *  schedule entry riding the same window as the jobs. */
@Serializable
data class ScheduleEntry(
    val id: Int,
    val userId: Int? = null,
    val entryType: String = "",
    val workDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val notes: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val userName: String = "",
)

/** Phase 2: the full schedule window body — jobs, the employee
 *  roster the filters and the People mode render from, and the
 *  personal schedule entries (the hunt-era model dropped the
 *  last two; the full Schedule mirror needs them). */
@Serializable
data class ScheduleResponse(
    val ok: Boolean = true,
    val scope: String = "",
    val jobs: List<Job> = emptyList(),
    val employees: List<User> = emptyList(),
    val entries: List<ScheduleEntry> = emptyList(),
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

// ---- hunt5: assets (Today cards + client linked grid) ----

@Serializable
data class AssetFlag(
    val field: String = "",
    val date: String = "",
    val state: String = "",
)

@Serializable
data class AssetRenewal(
    val field: String = "",
    val date: String = "",
    val daysLeft: Int = 0,
    val bucket: String = "",
    val state: String = "",
)

/** pub_asset as Today's flagged/renewal sections carry it. */
@Serializable
data class AssetItem(
    val id: Int,
    val clientId: Int? = null,
    val assignedUserId: Int? = null,
    val name: String = "",
    val type: String = "",
    val identifier: String? = null,
    val reviewDueDate: String? = null,
    val scope: String = "",
    val status: String = "",
    val warrantyUntil: String? = null,
    val warrantyExpires: String? = null,
    val supportExpires: String? = null,
    val notes: String = "",
    val archivedAt: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val flags: List<AssetFlag> = emptyList(),
    val renewals: List<AssetRenewal> = emptyList(),
    val earliest: AssetRenewal? = null,
)

@Serializable
data class RenewalBucket(
    val total: Int = 0,
    val items: List<AssetItem> = emptyList(),
)

@Serializable
data class AssetRenewals(
    val total: Int = 0,
    val within30: RenewalBucket = RenewalBucket(),
    val within60: RenewalBucket = RenewalBucket(),
    val within90: RenewalBucket = RenewalBucket(),
    val items: List<AssetItem> = emptyList(),
)

@Serializable
data class PlanRenewals(
    val total: Int = 0,
    val items: List<kotlinx.serialization.json.JsonObject> = emptyList(),
)

/** The platform digest block when the today payload carries one. */
@Serializable
data class PlatformDigest(
    val day: String = "",
    val title: String = "",
    val lines: List<String> = emptyList(),
    val emailed: Boolean? = null,
    val demo: Boolean = false,
)

// ---- hunt5: client profile linked registers ----

@Serializable
data class InvoiceLite(
    val id: Int,
    val invoiceNumber: String = "",
    val clientId: Int? = null,
    val status: String = "",
    val issueDate: String = "",
    val dueDate: String = "",
    val totalCents: Long = 0,
    val overdue: Boolean = false,
)

@Serializable
data class FormLite(
    val id: Int,
    val clientId: Int? = null,
    val type: String = "",
    val title: String = "",
    val createdAt: Long = 0,
)

@Serializable
data class Verification(
    val id: Int? = null,
    val category: String = "",
    val status: String = "",
    val issuedDate: String? = null,
    val expiresDate: String? = null,
    val notes: String = "",
)

@Serializable
data class ClientContact(
    val id: Int? = null,
    val name: String = "",
    val title: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "",
)

@Serializable
data class NotificationPrefs(
    val ticketUpdates: Boolean = false,
    val invoiceEvents: Boolean = false,
    val monitoringAlerts: Boolean = false,
    val digest: Boolean = false,
)

// ---- hunt5: dashboard (B1, web getdashboard semantics) ----

@Serializable
data class DashPriorityCount(val priority: String = "", val count: Int = 0)

@Serializable
data class DashStatusCount(val status: String = "", val count: Int = 0)

@Serializable
data class DashTickets(
    val openTotal: Int = 0,
    val byPriority: List<DashPriorityCount> = emptyList(),
    val byStatus: List<DashStatusCount> = emptyList(),
    val unassigned: Int = 0,
)

@Serializable
data class DashCases(
    val total: Int = 0,
    val byStage: List<DashStatusCount> = emptyList(),
    val overdue: Int = 0,
    val unassigned: Int = 0,
)

@Serializable
data class DashStageValue(
    val stage: String = "",
    val count: Int = 0,
    val valueCents: Long = 0,
    val valuedCount: Int = 0,
)

@Serializable
data class DashClients(
    val total: Int = 0,
    val byTier: Map<String, Int> = emptyMap(),
    val byStage: List<DashStageValue> = emptyList(),
    val pipelineValueCents: Long = 0,
)

@Serializable
data class DashInvoices(
    val count: Int = 0,
    val outstanding: MoneyBucket = MoneyBucket(),
    val overdue: MoneyBucket = MoneyBucket(),
    val draftCount: Int = 0,
    val paidTotalCents: Long = 0,
)

@Serializable
data class DashAssets(
    val total: Int = 0,
    val byStatus: List<DashStatusCount> = emptyList(),
    val byScope: Map<String, Int> = emptyMap(),
)

@Serializable
data class DashTeamMember(
    val id: Int = 0,
    val displayName: String = "",
    val role: String = "",
    val sessions: Int = 0,
    val lastExpiresAt: Long = 0,
)

@Serializable
data class DashPresence(
    val activeSessionCount: Int = 0,
    val team: List<DashTeamMember> = emptyList(),
)

@Serializable
data class DashAttention(val kind: String = "", val count: Int = 0)

@Serializable
data class DashCanWrite(
    val clients: Boolean = false,
    val cases: Boolean = false,
    val tickets: Boolean = false,
    val invoices: Boolean = false,
)

/** GET /api/app/dashboard. Module blocks are present only when
 *  the role can read the module (web getdashboard scoping). */
@Serializable
data class DashboardResponse(
    val ok: Boolean = true,
    val today: String = "",
    val tickets: DashTickets? = null,
    val cases: DashCases? = null,
    val clients: DashClients? = null,
    val invoices: DashInvoices? = null,
    val assets: DashAssets? = null,
    val presence: DashPresence? = null,
    val attention: List<DashAttention> = emptyList(),
    val recentActivity: List<ActivityItem> = emptyList(),
    val canWrite: DashCanWrite = DashCanWrite(),
    // FEATUREWAVE Item 7 (1.6.1): the "While You Slept"
    // overnight card — OWNER-only server-side (staff/client
    // payloads never carry the key). Open JSON by design; the
    // parse + honesty rails live in Wave161Logic. Absence is
    // the gate: null → no card renders.
    val whileYouSlept: JsonObject? = null,
)

// ---- hunt5: roster (B2), portal thread (B6), timeline (B8), writes ----

@Serializable
data class RosterResponse(
    val ok: Boolean = true,
    val users: List<User> = emptyList(),
)

@Serializable
data class PortalMessage(
    val id: Int,
    val ticketId: Int = 0,
    val clientId: Int? = null,
    val senderKind: String = "",
    val senderUserId: Int? = null,
    val senderName: String = "",
    val body: String = "",
    val createdAt: Long = 0,
    val readByStaffAt: Long? = null,
    val readByClientAt: Long? = null,
)

@Serializable
data class PortalMessagesResponse(
    val ok: Boolean = true,
    val ticketId: Int = 0,
    val clientId: Int? = null,
    val messages: List<PortalMessage> = emptyList(),
    val unreadForStaff: Int = 0,
    val unreadForClient: Int = 0,
)

@Serializable
data class PortalMessagePostRequest(val body: String)

@Serializable
data class PortalMessagePostResponse(
    val ok: Boolean = true,
    val id: Int? = null,
    val message: PortalMessage? = null,
)

@Serializable
data class TimelineEntry(
    val type: String = "",
    val at: Long = 0,
    val date: String = "",
    val title: String = "",
    val detail: String = "",
    val view: String? = null,
    val refId: Int? = null,
)

@Serializable
data class ClientTimelineResponse(
    val ok: Boolean = true,
    val clientId: Int = 0,
    val timeline: List<TimelineEntry> = emptyList(),
    val total: Int = 0,
    val truncated: Boolean = false,
)

/** Create / patch / archive / follow-up responses carry the record. */
@Serializable
data class ClientWriteResponse(
    val ok: Boolean = true,
    val id: Int? = null,
    val message: String? = null,
    val client: Client,
)

@Serializable
data class ClientStageResponse(
    val ok: Boolean = true,
    val stage: String = "",
    val client: Client,
)

/** POST /api/app/clients/{id}/tier success envelope (TIER
 *  Phase B app twin of setclienttier): the fresh three-state
 *  tier plus the full client record as the server now holds it.
 *  Refusals ride the shared {ok:false, message} envelope and
 *  surface through callJson's verbatim message path. */
@Serializable
data class ClientTierResponse(
    val ok: Boolean = true,
    val tierState: String = "",
    val client: Client,
)

/** The full web saveclient field set (B7). PATCH seeds from the
 *  stored record server-side, so the form's full state is safe. */
@Serializable
data class ClientWriteRequest(
    val legalName: String,
    val dba: String = "",
    val type: String = "",
    val status: String = "",
    val stage: String = "Lead",
    val value: Double? = null,
    val nextFollowUp: String? = null,
    val source: String = "",
    val sourceDetail: String = "",
    val industry: String = "",
    val contactName: String = "",
    val contactTitle: String = "",
    val email: String = "",
    val phone: String = "",
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val zip: String = "",
    val tags: String = "",
    // AUDIT-171 T-1: NO tier field rides this request, by design.
    // Family/Friends status is owner-granted state (Skyler alone
    // issues it); a generic save must never carry it. The server
    // treats an absent flag as "leave as-is", so saves cannot clear
    // a tier either. Granting rides the dedicated owner-gated tier
    // endpoint (see TierLogic).
    val notes: String = "",
)

// ---- Phase 2 (hunt6): Cases API ----

@Serializable
data class CaseListResponse(
    val ok: Boolean = true,
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
    val cases: List<CaseItem> = emptyList(),
)

/** GET /cases/{id}: the web getcaseprofile envelope verbatim. */
@Serializable
data class CaseDetailResponse(
    val ok: Boolean = true,
    val case: CaseItem,
    val client: Client? = null,
    val tickets: List<Ticket> = emptyList(),
    val activity: List<ActivityItem> = emptyList(),
)

@Serializable
data class CaseWriteResponse(
    val ok: Boolean = true,
    val id: Int? = null,
    val message: String? = null,
    val case: CaseItem,
)

@Serializable
data class CaseStatusResponse(
    val ok: Boolean = true,
    val status: String = "",
    val case: CaseItem,
)

// ---- Phase 2 (hunt6): Schedule writes ----

@Serializable
data class JobWriteResponse(
    val ok: Boolean = true,
    val id: Int? = null,
    val job: Job,
)

@Serializable
data class JobStatusResponse(
    val ok: Boolean = true,
    val status: String = "",
    val job: Job,
)

@Serializable
data class EntrySaveResponse(
    val ok: Boolean = true,
    val id: Int? = null,
    val message: String? = null,
)

// ---- Phase 2 (hunt6): Engagements API (pub_cf_engagement) ----

@Serializable
data class EngFinding(
    val id: Int? = null,
    val engagementId: Int? = null,
    val title: String = "",
    val severity: String = "",
    val status: String = "",
    val description: String = "",
    val detail: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

@Serializable
data class Engagement(
    val id: Int,
    val clientId: Int? = null,
    val title: String = "",
    val scope: String = "",
    val rulesOfEngagement: String = "",
    val status: String = "Planned",
    val startDate: String? = null,
    val endDate: String? = null,
    val reportPublished: Boolean = false,
    val reportGeneratedAt: Long? = null,
    val hasSignedAuthorization: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val findings: List<EngFinding> = emptyList(),
)

@Serializable
data class EngagementsListResponse(
    val ok: Boolean = true,
    val engagements: List<Engagement> = emptyList(),
    val severities: List<String> = emptyList(),
    val findingStatuses: List<String> = emptyList(),
    val engagementStatuses: List<String> = emptyList(),
)

@Serializable
data class EngagementDetailResponse(
    val ok: Boolean = true,
    val engagement: Engagement,
)

@Serializable
data class EngagementWriteResponse(
    val ok: Boolean = true,
    val id: Int? = null,
    val message: String? = null,
    val engagement: Engagement,
)

// ---- Phase 2 (hunt6): Runbooks & Sign-off (b4fdesk) ----

@Serializable
data class RbStep(
    val title: String = "",
    val detail: String = "",
)

@Serializable
data class RbStepState(
    val state: String = "pending",
    val by: Int? = null,
    val byName: String = "",
    val at: Long? = null,
    val note: String = "",
)

@Serializable
data class RbProcedure(
    val id: Int,
    val title: String = "",
    val category: String = "",
    val isProcedure: Boolean = false,
    val steps: List<RbStep> = emptyList(),
    val stepCount: Int = 0,
    val clientVisible: Boolean = false,
    val updatedAt: Long = 0,
)

@Serializable
data class RbProceduresResponse(
    val ok: Boolean = true,
    val procedures: List<RbProcedure> = emptyList(),
)

@Serializable
data class RbProcedureSetResponse(
    val ok: Boolean = true,
    val message: String? = null,
    val procedure: RbProcedure? = null,
)

@Serializable
data class RbRun(
    val id: Int,
    val kbId: Int? = null,
    val ticketId: Int? = null,
    val clientId: Int? = null,
    val title: String = "",
    val steps: List<RbStep> = emptyList(),
    val stepStates: List<RbStepState> = emptyList(),
    val status: String = "active",
    val doneCount: Int = 0,
    val totalSteps: Int = 0,
    val startedBy: Int? = null,
    val startedByName: String = "",
    val startedAt: Long = 0,
    val finishedAt: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

@Serializable
data class RbRunsResponse(
    val ok: Boolean = true,
    val runs: List<RbRun> = emptyList(),
    val run: RbRun? = null,
)

@Serializable
data class RbRunResponse(
    val ok: Boolean = true,
    val message: String? = null,
    val id: Int? = null,
    val completed: Boolean? = null,
    val run: RbRun? = null,
)

/** Attachment text extraction status: summary + attachment rows
 *  ride as open objects (their shape is the extractor's own). */
@Serializable
data class RbExtractionResponse(
    val ok: Boolean = true,
    val summary: List<kotlinx.serialization.json.JsonObject> = emptyList(),
    val attachments: List<kotlinx.serialization.json.JsonObject> = emptyList(),
)

@Serializable
data class SignOff(
    val contractId: Int? = null,
    val ticketId: Int? = null,
    val clientId: Int? = null,
    val title: String = "",
    val summary: String = "",
    val documentHash: String = "",
    val status: String = "",
    val signerName: String = "",
    val signedAt: Long = 0,
    val signatureId: Int? = null,
    val signerIp: String = "",
)

@Serializable
data class SignOffResponse(
    val ok: Boolean = true,
    val message: String? = null,
    val ticketId: Int = 0,
    val ticketStatus: String = "",
    val completedWithSignOff: Boolean = false,
    val signOff: SignOff? = null,
)

// ---- Phase 2 (hunt6): Today ask + calendar sync + search ----

/** POST /api/app/today/ask (web b3fask): a deterministic answer. */
@Serializable
data class AskResponse(
    val ok: Boolean = true,
    val answered: Boolean = false,
    val kind: String = "",
    val entity: String = "",
    val text: String = "",
)

/** GET /api/app/today/calendar-sync — the READ surface only. */
@Serializable
data class CalendarSyncResponse(
    val ok: Boolean = true,
    val enabled: Boolean = false,
    val calendarId: String = "",
    val prepared: Boolean = false,
    val live: Boolean = false,
    val helperLastSeen: Long? = null,
    val lastRunMs: Long? = null,
    val lastSummary: String = "",
    val note: String = "",
)

@Serializable
data class SearchResultItem(
    val id: Int,
    val title: String = "",
    val subtitle: String = "",
    val type: String = "",
    val snippet: String = "",
)

/** GET /api/app/search (web globalsearch verbatim, role-scoped). */
@Serializable
data class SearchResponse(
    val ok: Boolean = true,
    val q: String = "",
    val groups: Map<String, List<SearchResultItem>> = emptyMap(),
)
