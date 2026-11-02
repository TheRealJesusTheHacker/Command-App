package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sentinelsafeguard.app.data.api.TodayResponse

/**
 * Today (spec §2.1): the same five sections as the web Today view,
 * glance-sized — counts first, then the top items. Reads in seconds.
 */
@Composable
fun TodayScreen(
    viewModel: AppViewModel,
    onOpenTickets: () -> Unit,
    onOpenClients: () -> Unit,
    onOpenFullCommand: () -> Unit,
) {
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
            val data = today!!
            LazyColumn {
                item {
                    ItemRow(
                        "Open full Command",
                        "The complete Sentinel Command — every view, exactly like the browser.",
                        badge = "Full",
                    ) { onOpenFullCommand() }
                }

                item { SectionHeader("Follow-ups due", data.followUps.total) }
                if (data.followUps.items.isEmpty()) {
                    item { ItemRow("Nothing due", "No follow-ups are waiting today.") }
                } else {
                    items(data.followUps.items) { client ->
                        ItemRow(
                            client.legalName,
                            listOfNotNull(
                                client.nextFollowUp?.takeIf { it.isNotBlank() }
                                    ?.let { "Due $it" },
                                client.contactName.takeIf { it.isNotBlank() },
                            ).joinToString(" · "),
                        ) { onOpenClients() }
                    }
                }

                item { SectionHeader("New tickets", data.newTickets.total) }
                if (data.newTickets.items.isEmpty()) {
                    item { ItemRow("No new tickets", "You are all caught up.") }
                } else {
                    items(data.newTickets.items) { ticket ->
                        ItemRow(
                            ticket.title,
                            listOfNotNull(
                                ticket.clientName?.takeIf { it.isNotBlank() },
                                ticket.priority.takeIf { it.isNotBlank() },
                            ).joinToString(" · "),
                        ) { onOpenTickets() }
                    }
                }

                item { SectionHeader("Overdue cases", data.overdueCases.total) }
                if (data.overdueCases.items.isEmpty()) {
                    item { ItemRow("No overdue cases", "Every case is on track.") }
                } else {
                    items(data.overdueCases.items) { entry ->
                        ItemRow(
                            entry.title,
                            listOfNotNull(
                                entry.clientName?.takeIf { it.isNotBlank() },
                                entry.dueDate?.takeIf { it.isNotBlank() }
                                    ?.let { "Due $it" },
                            ).joinToString(" · "),
                            badge = "Overdue",
                        )
                    }
                }

                item { SectionHeader("Best clients not contacted in a while", data.tierA.total) }
                if (data.tierA.items.isEmpty()) {
                    item { ItemRow("All caught up", "Your best clients have all been contacted recently.") }
                } else {
                    items(data.tierA.items) { client ->
                        ItemRow(
                            client.legalName,
                            listOfNotNull(
                                client.stage.takeIf { it.isNotBlank() },
                                client.contactName.takeIf { it.isNotBlank() },
                            ).joinToString(" · "),
                        ) { onOpenClients() }
                    }
                }

                item { SectionHeader("Sign-in alerts", data.signinAlerts.total) }
                if (data.signinAlerts.items.isEmpty()) {
                    item { ItemRow("Nothing new", "No new sign-ins to check.") }
                } else {
                    items(data.signinAlerts.items) { alert ->
                        ItemRow(
                            alert.actorName.ifBlank { "Sign-in" },
                            listOfNotNull(
                                alert.detail?.takeIf { it.isNotBlank() },
                                alert.ip.takeIf { it.isNotBlank() },
                                if (alert.createdAt > 0) formatEpochMs(alert.createdAt) else null,
                            ).joinToString(" · "),
                            badge = "Sign-in",
                        )
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
