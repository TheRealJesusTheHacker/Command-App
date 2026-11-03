package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import net.sentinelsafeguard.app.data.api.AssetItem
import net.sentinelsafeguard.app.data.api.CaseItem
import net.sentinelsafeguard.app.data.api.Client
import net.sentinelsafeguard.app.data.api.Ticket
import net.sentinelsafeguard.app.data.api.TodayResponse
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Today (mirror spec §3.2): web paintToday()'s FULL core
 * composition from the existing /api/app/today payload — the
 * hunt-era screen rendered 5 sections and dropped the rest.
 * Rows navigate to the RECORD, as web rows do. The ~16 injected
 * bundle panels are Phase 2 (see the hunt5 placeholder ledger).
 */
@Composable
fun TodayScreen(viewModel: AppViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    var today by remember { mutableStateOf<TodayResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        error = null
        try {
            today = viewModel.api.today()
        } catch (e: Exception) {
            error = plainError(e, "Today could not be loaded. Check your connection and try again.")
        }
    }

    when {
        error != null -> ErrorBox(error!!) { reloadKey++ }
        today == null -> SkeletonList(6)
        else -> {
            val d = today!!
            LazyColumn {
                item { MirrorHeader("Today") }
                item { UrgentBanner(viewModel, nav) }
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        StatCard("Follow-ups due", d.followUps.total.toString(), Modifier.weight(1f))
                        StatCard("Tier A waiting", d.tierA.total.toString(), Modifier.weight(1f))
                        StatCard("New tickets", d.newTickets.total.toString(), Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        StatCard("Overdue cases", d.overdueCases.total.toString(), Modifier.weight(1f))
                        StatCard("Sign-in alerts", d.signinAlerts.total.toString(), Modifier.weight(1f))
                        Spacer(Modifier.weight(1f))
                    }
                }
                if (d.briefing.isNotBlank()) {
                    item {
                        Text(
                            d.briefing,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }

                // 1 Follow-ups due
                item {
                    MirrorCard(
                        "Follow-ups due", d.followUps.total,
                        footerLabel = "Show all → Clients",
                        onFooter = { nav.navigate("clients") },
                    ) {
                        if (d.followUps.items.isEmpty()) {
                            Text("No follow-ups are due.", color = SentinelMuted)
                        }
                        d.followUps.items.forEach { client ->
                            TodayClientRow(
                                client = client,
                                todayStr = d.today,
                                actionLabel = "+7 days",
                                onAction = {
                                    scope.launch {
                                        try {
                                            viewModel.api.clientFollowup(client.id, days = 7)
                                            reloadKey++
                                        } catch (e: Exception) {
                                            error = plainError(e, "The follow-up could not be moved. Try again.")
                                        }
                                    }
                                },
                                onOpen = { nav.navigate("client/${client.id}") },
                            )
                        }
                    }
                }

                // 2 Tier A leads — untouched longest
                item {
                    MirrorCard(
                        "Tier A leads — untouched longest", d.tierA.total,
                        footerLabel = "Show all → Clients",
                        onFooter = { nav.navigate("clients") },
                    ) {
                        if (d.tierA.items.isEmpty()) {
                            Text(
                                "No Tier A leads yet. Tag a client tier-a to watch them here.",
                                color = SentinelMuted,
                            )
                        }
                        d.tierA.items.forEach { client ->
                            ItemRow(
                                client.legalName,
                                listOfNotNull(
                                    client.stage.takeIf { it.isNotBlank() },
                                    MirrorVocab.TIER_LABELS[MirrorVocab.tierOf(client)],
                                    "Last touched ${formatEpochMs(client.updatedAt)}",
                                ).joinToString(" · "),
                            ) { nav.navigate("client/${client.id}") }
                        }
                    }
                }

                // 3 New tickets — unassigned
                item {
                    MirrorCard(
                        "New tickets — unassigned", d.newTickets.total,
                        footerLabel = "Show all → Tickets",
                        onFooter = { nav.navigate("tickets") },
                    ) {
                        if (d.newTickets.items.isEmpty()) {
                            Text("No new tickets are waiting.", color = SentinelMuted)
                        }
                        d.newTickets.items.forEach { ticket ->
                            TodayTicketRow(
                                ticket = ticket,
                                actionLabel = "Assign to me",
                                onAction = {
                                    scope.launch {
                                        try {
                                            viewModel.api.patchTicket(
                                                ticket.id,
                                                net.sentinelsafeguard.app.data.api.TicketPatchRequest(assignToMe = true),
                                            )
                                            reloadKey++
                                        } catch (e: Exception) {
                                            error = plainError(e, "The ticket could not be assigned. Try again.")
                                        }
                                    }
                                },
                                onOpen = { nav.navigate("ticket/${ticket.id}") },
                            )
                        }
                    }
                }

                // 4 Overdue cases
                item {
                    MirrorCard(
                        "Overdue cases", d.overdueCases.total,
                        footerLabel = "Show all → Cases",
                        onFooter = { nav.navigate("cases") },
                    ) {
                        if (d.overdueCases.items.isEmpty()) {
                            Text("No cases are overdue.", color = SentinelMuted)
                        }
                        d.overdueCases.items.forEach { case ->
                            TodayCaseRow(case) { nav.navigate("case/${case.id}") }
                        }
                    }
                }

                // 5 Sign-in alerts
                item {
                    MirrorCard(
                        "Sign-in alerts", d.signinAlerts.total,
                        footerLabel = "Show all → Activity",
                        onFooter = { nav.navigate("view/activity") },
                    ) {
                        if (d.signinAlerts.items.isEmpty()) {
                            Text("No sign-in alerts.", color = SentinelMuted)
                        }
                        d.signinAlerts.items.forEach { alert ->
                            ItemRow(
                                "${alert.actorName} signed in from a new IP address",
                                listOfNotNull(
                                    alert.ip.takeIf { it.isNotBlank() },
                                    formatEpochMs(alert.createdAt),
                                ).joinToString(" · "),
                            )
                        }
                    }
                }

                // 6 Money — invoices
                item {
                    MirrorCard(
                        "Money — invoices", null,
                        footerLabel = "Show all → Invoices",
                        onFooter = { nav.navigate("view/invoices") },
                    ) {
                        MoneyLine("Outstanding", d.money.outstanding)
                        MoneyLine("Overdue", d.money.overdue)
                        MoneyLine("Due this week", d.money.dueThisWeek)
                    }
                }

                // 7 Due this week
                item {
                    MirrorCard("Due this week") {
                        Text(
                            "Cases · ${d.dueThisWeek.cases.total}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        d.dueThisWeek.cases.items.forEach { case ->
                            TodayCaseRow(case) { nav.navigate("case/${case.id}") }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Follow-ups · ${d.dueThisWeek.followUps.total}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        d.dueThisWeek.followUps.items.forEach { client ->
                            TodayClientRow(
                                client = client,
                                todayStr = d.today,
                                onOpen = { nav.navigate("client/${client.id}") },
                            )
                        }
                    }
                }

                // 8 Website tickets — since yesterday
                item {
                    MirrorCard(
                        "Website tickets — since yesterday", d.websiteTickets.total,
                        footerLabel = "Show all → Tickets",
                        onFooter = { nav.navigate("tickets") },
                    ) {
                        if (d.websiteTickets.items.isEmpty()) {
                            Text("No website tickets since yesterday.", color = SentinelMuted)
                        }
                        d.websiteTickets.items.forEach { ticket ->
                            TodayTicketRow(ticket) { nav.navigate("ticket/${ticket.id}") }
                        }
                    }
                }

                // 9 Assets flagged — warranty / review
                item {
                    MirrorCard(
                        "Assets flagged — warranty / review", d.assetsFlagged.total,
                        footerLabel = "Show all → Assets",
                        onFooter = { nav.navigate("view/assets") },
                    ) {
                        if (d.assetsFlagged.items.isEmpty()) {
                            Text("No assets are flagged.", color = SentinelMuted)
                        }
                        d.assetsFlagged.items.forEach { asset ->
                            TodayAssetRow(asset) { nav.navigate("view/assets") }
                        }
                    }
                }

                // 10 Asset renewals
                item {
                    MirrorCard(
                        "Asset renewals", d.assetRenewals.total,
                        footerLabel = "Show all → Assets",
                        onFooter = { nav.navigate("view/assets") },
                    ) {
                        if (d.assetRenewals.items.isEmpty()) {
                            Text("No asset renewals are coming up.", color = SentinelMuted)
                        }
                        d.assetRenewals.items.forEach { asset ->
                            val e = asset.earliest
                            ItemRow(
                                asset.name,
                                listOfNotNull(
                                    asset.type.takeIf { it.isNotBlank() },
                                    e?.let {
                                        "${it.field.replaceFirstChar { c -> c.uppercase() }} ${it.state} ${it.date} (${it.daysLeft}d)"
                                    },
                                ).joinToString(" · "),
                            ) { nav.navigate("view/assets") }
                        }
                    }
                }

                // 11 Plan renewals
                item {
                    MirrorCard(
                        "Plan renewals", d.planRenewals.total,
                        footerLabel = "Show all → Assets",
                        onFooter = { nav.navigate("view/assets") },
                    ) {
                        if (d.planRenewals.items.isEmpty()) {
                            Text("No plan renewals are coming up.", color = SentinelMuted)
                        }
                        d.planRenewals.items.forEach { obj ->
                            val name = listOf("name", "title", "planName", "clientName")
                                .firstNotNullOfOrNull { k -> obj[k]?.toString()?.trim('"')?.takeIf { it.isNotBlank() } }
                                ?: "Plan renewal"
                            val date = listOf("date", "renewalDate", "nextRenewal", "expires")
                                .firstNotNullOfOrNull { k -> obj[k]?.toString()?.trim('"')?.takeIf { it.isNotBlank() } }
                            ItemRow(name, date) { nav.navigate("view/assets") }
                        }
                    }
                }

                // Platform digest
                d.platformDigest?.let { digest ->
                    item {
                        MirrorCard("Platform digest ${digest.day}".trim()) {
                            digest.lines.forEach { line -> Text(line) }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (digest.emailed == true) "Digest emailed to the owner mailbox."
                                else "Digest stored — not yet emailed.",
                                color = SentinelMuted,
                            )
                            if (digest.demo) {
                                Text("Sample data — demo workspace.", color = SentinelMuted)
                            }
                        }
                    }
                }

                // Your notifications
                item {
                    MirrorCard("Your notifications") {
                        Text(
                            "Choose which events reach this phone, and the quiet hours " +
                                "that hold the routine ones until morning.",
                            color = SentinelMuted,
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { nav.navigate("settings") }) {
                            Text("Open notification preferences")
                        }
                    }
                }
                // Phase 2: Today's injected panels, in web's order.
                item { TodayPanels(viewModel, nav) }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun MoneyLine(label: String, bucket: net.sentinelsafeguard.app.data.api.MoneyBucket) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        Text("${MirrorVocab.formatCents(bucket.totalCents)} · ${bucket.count} invoice(s)")
    }
}

@Composable
private fun TodayClientRow(
    client: Client,
    todayStr: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onOpen: () -> Unit,
) {
    val followUp = client.nextFollowUp?.takeIf { it.isNotBlank() }
    val overdue = followUp != null && todayStr.isNotBlank() && followUp < todayStr
    Column {
        ItemRow(
            client.legalName,
            listOfNotNull(
                client.stage.takeIf { it.isNotBlank() },
                MirrorVocab.TIER_LABELS[MirrorVocab.tierOf(client)],
                followUp?.let { if (overdue) "Overdue · $it" else "Due $it" },
            ).joinToString(" · "),
        ) { onOpen() }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun TodayTicketRow(
    ticket: Ticket,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onOpen: () -> Unit,
) {
    Column {
        ItemRow(
            ticket.title,
            listOfNotNull(
                ticket.clientName?.takeIf { it.isNotBlank() },
                ticket.status.takeIf { it.isNotBlank() },
                ticket.priority.takeIf { it.isNotBlank() },
                ticket.source.takeIf { it.isNotBlank() }?.let { "via $it" },
                formatEpochMs(ticket.createdAt),
            ).joinToString(" · "),
        ) { onOpen() }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun TodayCaseRow(case: CaseItem, onOpen: () -> Unit) {
    ItemRow(
        case.title,
        listOfNotNull(
            case.clientName?.takeIf { it.isNotBlank() },
            case.status.takeIf { it.isNotBlank() },
            case.priority.takeIf { it.isNotBlank() },
            case.dueDate?.takeIf { it.isNotBlank() }?.let { "Due $it" },
        ).joinToString(" · "),
    ) { onOpen() }
}

@Composable
private fun TodayAssetRow(asset: AssetItem, onOpen: () -> Unit) {
    ItemRow(
        asset.name,
        listOfNotNull(
            asset.scope.takeIf { it.isNotBlank() },
            asset.type.takeIf { it.isNotBlank() },
            asset.flags.joinToString(" · ") { flag ->
                "${flag.field.replaceFirstChar { c -> c.uppercase() }} ${flag.state} ${flag.date}"
            }.takeIf { it.isNotBlank() },
        ).joinToString(" · "),
    ) { onOpen() }
}
