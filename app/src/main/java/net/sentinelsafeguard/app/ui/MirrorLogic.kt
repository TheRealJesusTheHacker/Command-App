package net.sentinelsafeguard.app.ui

import net.sentinelsafeguard.app.data.api.Client
import net.sentinelsafeguard.app.data.api.Ticket
import java.text.NumberFormat
import java.util.Locale

/**
 * Pure mirror logic: vocabularies, board grouping/sorting, money
 * formatting, and directory filter mapping. Kept free of Compose
 * so the unit tests pin it directly. Vocabularies are web
 * Command's exact sets (mirror inventory §4/§5).
 */
object MirrorVocab {
    val TICKET_STAGES = listOf("New", "Open", "In Progress", "On Hold", "Resolved", "Closed")
    val PRIORITIES = listOf("Low", "Normal", "High", "Critical")
    val CATEGORIES = listOf("Assessment", "Incident Response", "Install", "Support", "Audit", "Other")
    val CLIENT_STAGES = listOf(
        "Lead", "Contacted", "Qualified", "Proposal", "Negotiation",
        "Active Client", "Past Client", "Lost",
    )
    val CLIENT_STATUSES = listOf("Prospect", "Active", "Inactive", "Former")
    val CLIENT_TYPES = listOf("Corporate", "Individual")
    val CLIENT_SOURCES = listOf(
        "Google", "Facebook", "Referral — person", "KC clinic", "Academy", "Other",
    )
    val TIERS = listOf("tier-a", "tier-b", "tier-c", "tier-d", "partner")
    val TIER_LABELS = mapOf(
        "tier-a" to "Tier A", "tier-b" to "Tier B", "tier-c" to "Tier C",
        "tier-d" to "Tier D", "partner" to "Partner",
    )

    /** Board columns: canonical stages in order, then any legacy
     *  statuses present in the data as extra columns (web behavior). */
    fun boardStages(tickets: List<Ticket>): List<String> {
        val present = tickets.map { it.status }.distinct()
        val legacy = present.filter { it !in TICKET_STAGES }.sorted()
        return TICKET_STAGES + legacy
    }

    /** Select options for a status value: the canonical stages,
     *  plus the current value itself when legacy (web renders it
     *  as an extra "<status> (legacy)" option). */
    fun stageOptions(current: String): List<String> =
        if (current in TICKET_STAGES) TICKET_STAGES else TICKET_STAGES + current

    fun priorityRank(priority: String): Int = PRIORITIES.indexOf(priority)

    /** Web board sort: priority ↓, due ↑ (undated last), newest first. */
    fun sortBoardCards(tickets: List<Ticket>): List<Ticket> =
        tickets.sortedWith(
            compareByDescending<Ticket> { priorityRank(it.priority) }
                .thenBy { it.dueDate?.takeIf { d -> d.isNotBlank() } ?: "9999-12-31" }
                .thenByDescending { it.createdAt }
        )

    fun groupBoard(tickets: List<Ticket>): List<Pair<String, List<Ticket>>> =
        boardStages(tickets).map { stage ->
            stage to sortBoardCards(tickets.filter { it.status == stage })
        }

    /** Stepper move: one stage back/forward in canonical order;
     *  null at the ends (or for legacy statuses). */
    fun stepStage(current: String, delta: Int): String? {
        val idx = TICKET_STAGES.indexOf(current)
        if (idx < 0) return null
        val next = idx + delta
        return if (next in TICKET_STAGES.indices) TICKET_STAGES[next] else null
    }

    fun isOverdue(dueDate: String?, today: String, done: Boolean): Boolean =
        !done && !dueDate.isNullOrBlank() && dueDate < today

    fun isDone(status: String): Boolean = status == "Resolved" || status == "Closed"

    /** Cents → web money text ($1,234 / $1,234.56). */
    fun formatCents(cents: Long): String {
        val fmt = NumberFormat.getNumberInstance(Locale.US)
        val dollars = cents / 100
        val rem = Math.abs(cents % 100)
        return if (rem == 0L) "$" + fmt.format(dollars)
        else "$" + fmt.format(dollars) + "." + rem.toString().padStart(2, '0')
    }

    /** Client pipeline/contract value (dollars on the wire) → $. */
    fun formatDollars(value: Double?): String {
        if (value == null) return ""
        val cents = Math.round(value * 100)
        return formatCents(cents)
    }

    fun tierOf(client: Client): String =
        client.tier.ifBlank {
            client.tags?.split(",")?.map { it.trim().lowercase() }
                ?.firstOrNull { it in TIERS } ?: ""
        }

    fun stageChipCounts(clients: List<Client>): List<Pair<String, Int>> =
        CLIENT_STAGES.map { stage -> stage to clients.count { it.stage == stage } }

    fun tierChipCounts(clients: List<Client>): List<Pair<String, Int>> =
        TIERS.map { tier -> tier to clients.count { tierOf(it) == tier } }

    /** Top-3 sources for the Clients stat row: "source · count". */
    fun topSources(clients: List<Client>, limit: Int = 3): List<Pair<String, Int>> =
        clients.mapNotNull { it.source.takeIf { s -> s.isNotBlank() } }
            .groupingBy { it }.eachCount()
            .entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(limit).map { it.key to it.value }

    fun pipelineValue(clients: List<Client>): Double =
        clients.filter { it.stage != "Lost" }.sumOf { it.value ?: 0.0 }

    fun inPipelineCount(clients: List<Client>): Int =
        clients.count { it.stage in CLIENT_STAGES.subList(0, 5) }

    /** Directory card order for the pipeline board: value ↓, name. */
    fun sortPipelineCards(clients: List<Client>): List<Client> =
        clients.sortedWith(
            compareByDescending<Client> { it.value ?: 0.0 }
                .thenBy { it.legalName.lowercase() }
        )

    /** The B3 query-parameter mapping for the ticket directory:
     *  which server filters a directory state sends. */
    fun ticketDirectoryParams(
        q: String,
        stage: String?,
        priority: String?,
        overdueOnly: Boolean,
        unassignedOnly: Boolean,
        noCaseOnly: Boolean,
        adOnly: Boolean,
        sortLabel: String,
    ): Map<String, String> {
        val sort = when (sortLabel) {
            "Priority high → low" -> "priority"
            "Due soonest" -> "due"
            "Stage order" -> "stage"
            else -> "id_desc"
        }
        val out = linkedMapOf<String, String>()
        if (q.isNotBlank()) out["q"] = q
        if (stage != null) out["status"] = stage
        if (priority != null) out["priority"] = priority
        if (overdueOnly) out["overdue"] = "1"
        if (unassignedOnly) out["unassigned"] = "1"
        if (noCaseOnly) out["noCase"] = "1"
        if (adOnly) out["adMaterial"] = "1"
        out["sort"] = sort
        return out
    }

    val TICKET_SORTS = listOf("Newest", "Priority high → low", "Due soonest", "Stage order")
    val CLIENT_SORTS = listOf("Name A–Z", "Newest", "Stage order", "Value high → low")

    fun sortClients(clients: List<Client>, sortLabel: String): List<Client> = when (sortLabel) {
        "Newest" -> clients.sortedByDescending { it.createdAt }
        "Stage order" -> clients.sortedWith(
            compareBy<Client> { CLIENT_STAGES.indexOf(it.stage).let { i -> if (i < 0) 99 else i } }
                .thenBy { it.legalName.lowercase() }
        )
        "Value high → low" -> clients.sortedWith(
            compareByDescending<Client> { it.value ?: 0.0 }.thenBy { it.legalName.lowercase() }
        )
        else -> clients.sortedBy { it.legalName.lowercase() }
    }
}
