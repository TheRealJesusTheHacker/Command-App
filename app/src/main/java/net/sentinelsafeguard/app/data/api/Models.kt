package net.sentinelsafeguard.app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire models for the Command app API (spec §3.3, /api/app/ endpoints).
 * Unknown fields are ignored so a backend addition never breaks the app.
 */

// ---- Auth ----

@Serializable
data class LoginRequest(
    val displayName: String,
    val passphrase: String,
    val deviceName: String,
    val totpCode: String? = null,
)

@Serializable
data class LoginResponse(
    val deviceToken: String? = null,
    val requiresTotp: Boolean = false,
    val status: String? = null,
    val displayName: String? = null,
    val role: String? = null,
    val message: String? = null,
)

@Serializable
data class MeResponse(
    val displayName: String = "",
    val role: String = "",
    val serverTime: String? = null,
)

// ---- Today ----

@Serializable
data class TodayItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
)

@Serializable
data class TodayResponse(
    val followUpsDue: Int = 0,
    val newTickets: Int = 0,
    val overdueCases: Int = 0,
    val tierAUntouched: Int = 0,
    val signInAlerts: Int = 0,
    val followUps: List<TodayItem> = emptyList(),
    val tickets: List<TodayItem> = emptyList(),
    val cases: List<TodayItem> = emptyList(),
    val tierA: List<TodayItem> = emptyList(),
    val alerts: List<TodayItem> = emptyList(),
)

// ---- Tickets ----

@Serializable
data class TicketNote(
    val id: String,
    val author: String? = null,
    val body: String,
    val createdAt: String? = null,
)

@Serializable
data class Ticket(
    val id: String,
    val title: String,
    val message: String? = null,
    val reporter: String? = null,
    val status: String = "new",
    val priority: String = "normal",
    val assignee: String? = null,
    val source: String? = null,
    val clientId: String? = null,
    val clientName: String? = null,
    val caseId: String? = null,
    val createdAt: String? = null,
    val notes: List<TicketNote> = emptyList(),
)

@Serializable
data class TicketListResponse(
    val tickets: List<Ticket> = emptyList(),
)

@Serializable
data class TicketPatchRequest(
    val status: String? = null,
    val priority: String? = null,
    val assignee: String? = null,
    @SerialName("assignToMe") val assignToMe: Boolean? = null,
)

@Serializable
data class NoteRequest(val body: String)

@Serializable
data class TicketCreateRequest(
    val title: String,
    val message: String,
    val priority: String = "normal",
    val clientId: String? = null,
)

// ---- Clients & pipeline ----

@Serializable
data class Client(
    val id: String,
    val name: String,
    val business: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val tier: String? = null,
    val tags: List<String> = emptyList(),
    val stage: String? = null,
    val followUpDate: String? = null,
    val notes: String? = null,
)

@Serializable
data class ClientListResponse(
    val clients: List<Client> = emptyList(),
)

@Serializable
data class PipelineCard(
    val id: String,
    val name: String,
)

@Serializable
data class PipelineStage(
    val name: String,
    val count: Int = 0,
    val clients: List<PipelineCard> = emptyList(),
)

@Serializable
data class PipelineResponse(
    val stages: List<PipelineStage> = emptyList(),
)

// ---- Schedule ----

@Serializable
data class Job(
    val id: String,
    val title: String,
    val clientId: String? = null,
    val clientName: String? = null,
    val address: String? = null,
    val start: String? = null,
    val end: String? = null,
    val date: String? = null,
    val assignees: List<String> = emptyList(),
    val notes: String? = null,
)

@Serializable
data class ScheduleResponse(
    val jobs: List<Job> = emptyList(),
)

// ---- Devices & version ----

@Serializable
data class DeviceInfo(
    val id: String,
    val name: String,
    val createdAt: String? = null,
    val lastSeenAt: String? = null,
    val current: Boolean = false,
)

@Serializable
data class DeviceListResponse(
    val devices: List<DeviceInfo> = emptyList(),
)

@Serializable
data class VersionResponse(
    val latestVersion: String,
    val latestVersionCode: Int? = null,
    val notes: String? = null,
)
