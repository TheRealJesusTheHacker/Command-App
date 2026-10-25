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
) {
    var today by remember { mutableStateOf<TodayResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        error = null
        try {
            today = viewModel.api.today()
        } catch (e: Exception) {
            error = e.message ?: "The server could not be reached. Check your connection and try again."
        }
    }

    when {
        error != null -> ErrorBox(error!!) { reloadKey++ }
        today == null -> LoadingBox("Loading today…")
        else -> {
            val data = today!!
            LazyColumn {
                item { SectionHeader("Follow-ups due", data.followUpsDue) }
                if (data.followUps.isEmpty()) {
                    item { ItemRow("Nothing due", "No follow-ups are waiting today.") }
                } else {
                    items(data.followUps) { entry ->
                        ItemRow(entry.title, entry.subtitle) { onOpenClients() }
                    }
                }

                item { SectionHeader("New tickets", data.newTickets) }
                if (data.tickets.isEmpty()) {
                    item { ItemRow("No new tickets", "You are all caught up.") }
                } else {
                    items(data.tickets) { entry ->
                        ItemRow(entry.title, entry.subtitle) { onOpenTickets() }
                    }
                }

                item { SectionHeader("Overdue cases", data.overdueCases) }
                if (data.cases.isEmpty()) {
                    item { ItemRow("No overdue cases", "Every case is on track.") }
                } else {
                    items(data.cases) { entry ->
                        ItemRow(entry.title, entry.subtitle, badge = "Overdue")
                    }
                }

                item { SectionHeader("Best clients not contacted in a while", data.tierAUntouched) }
                if (data.tierA.isEmpty()) {
                    item { ItemRow("All caught up", "Your best clients have all been contacted recently.") }
                } else {
                    items(data.tierA) { entry ->
                        ItemRow(entry.title, entry.subtitle) { onOpenClients() }
                    }
                }

                item { SectionHeader("Sign-in alerts", data.signInAlerts) }
                if (data.alerts.isEmpty()) {
                    item { ItemRow("Nothing new", "No new sign-ins to check.") }
                } else {
                    items(data.alerts) { entry ->
                        ItemRow(entry.title, entry.subtitle, badge = "Sign-in")
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
