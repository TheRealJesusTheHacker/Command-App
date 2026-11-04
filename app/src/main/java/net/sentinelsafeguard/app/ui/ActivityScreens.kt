package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import net.sentinelsafeguard.app.data.api.activityAll
import net.sentinelsafeguard.app.data.api.activityRevokeSession
import net.sentinelsafeguard.app.data.api.activitySessions
import net.sentinelsafeguard.app.data.api.activityVisitors
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Activity (Phase 8A, full) — the mirror of web's Activity
 * view: the audit-feed console with web's filters (Search over
 * person/action/detail/IP, the category chips, plus the
 * handler's ip / actor / days filters), the summary and
 * failed-by-IP and class-count surfaces, Active sessions with
 * per-session revoke, and the Public site panel.
 *
 * The feed is the COMPLETE filtered set: the console walks the
 * handler's limit+offset paging to the payload's own total
 * (see Phase8Api.activityAll), so the count the header shows
 * is the true filtered total, never a first page. Today's and
 * Dashboard's sign-in-alert links land here now.
 *
 * Sessions honesty: the app actor holds no web session, so no
 * row here is "your current session" — the backend resolves
 * isCurrent against no session for device tokens (Addendum
 * 24), and the mirror renders exactly that.
 *
 * Public visitors renders the backend's honest state: when
 * the feed is not wired the payload says connected: false and
 * the panel says so — no visitor list is ever invented.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ActivityScreen(viewModel: AppViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var cls by remember { mutableStateOf("all") }
    var ip by remember { mutableStateOf("") }
    var actorId by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val state = rememberJsonScreen {
        viewModel.api.activityAll(
            cls = cls,
            q = query.ifBlank { null },
            ip = ip.ifBlank { null },
            actorId = actorId.toIntOrNull(),
            days = days.toIntOrNull(),
        )
    }
    LaunchedEffect(cls, days) { state.reloadKey++ }
    LaunchedEffect(query, ip, actorId) {
        delay(400)
        state.reloadKey++
    }
    val sessionsState = rememberJsonScreen { viewModel.api.activitySessions() }
    val visitorsState = rememberJsonScreen { viewModel.api.activityVisitors() }
    var revokeTarget by remember { mutableStateOf<JsonObject?>(null) }

    Column {
        MirrorHeader("Activity", MirrorCopy.subtitle("activity"))
        when {
            state.error != null && state.data == null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null && sessionsState.data == null -> SkeletonList()
            else -> {
                val data = state.data
                val summary = data?.let { Phase3Logic.obj(it, "summary") }
                val classCounts = data?.let { Phase3Logic.obj(it, "classCounts") }
                val rows = data?.let { Phase3Logic.arr(it, "rows") } ?: emptyList()
                val total = data?.let { Phase3Logic.int(it, "total") } ?: rows.size
                LazyColumn {
                    item {
                        if (notice != null) {
                            Text(
                                notice!!, color = SentinelMuted,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                        if (error != null) {
                            Text(
                                error!!, color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                    if (summary != null) {
                        item {
                            Row(Modifier.padding(horizontal = 12.dp)) {
                                StatCard(
                                    "Events today",
                                    (Phase3Logic.int(summary, "eventsToday") ?: 0).toString(),
                                    Modifier.weight(1f),
                                )
                                StatCard(
                                    "Sign-ins (24h)",
                                    (Phase3Logic.int(summary, "signins24h") ?: 0).toString(),
                                    Modifier.weight(1f),
                                )
                                StatCard(
                                    "Failed (24h)",
                                    (Phase3Logic.int(summary, "failed24h") ?: 0).toString(),
                                    Modifier.weight(1f),
                                )
                            }
                            Row(Modifier.padding(horizontal = 12.dp)) {
                                StatCard(
                                    "Unique IPs (24h)",
                                    (Phase3Logic.int(summary, "uniqueIps24h") ?: 0).toString(),
                                    Modifier.weight(1f),
                                )
                                StatCard(
                                    "Downloads (24h)",
                                    (Phase3Logic.int(summary, "downloads24h") ?: 0).toString(),
                                    Modifier.weight(1f),
                                )
                                StatCard(
                                    "Active sessions",
                                    (Phase3Logic.int(summary, "activeSessions") ?: 0).toString(),
                                    Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    item {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            TextFieldRow("Search", query, { query = it })
                            Text(
                                "Search person, action, detail, or IP…",
                                color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
                            )
                            androidx.compose.foundation.layout.FlowRow {
                                Phase8Logic.CLASS_FILTERS.forEach { (key, label) ->
                                    val count = classCounts?.let { Phase3Logic.int(it, key) }
                                    FilterChip(
                                        selected = cls == key,
                                        onClick = { cls = key },
                                        label = { Text(if (count != null) "$label · $count" else label) },
                                        modifier = Modifier.padding(end = 6.dp, bottom = 4.dp),
                                    )
                                }
                            }
                            TextFieldRow("IP address", ip, { ip = it })
                            TextFieldRow("Actor ID", actorId, { actorId = it })
                            ChoiceMenu(
                                "Days",
                                Phase8Logic.daysLabel(days),
                                Phase8Logic.DAYS_FILTERS.map { it.second },
                            ) { days = Phase8Logic.daysValue(it) }
                            OutlinedButton(onClick = {
                                query = ""; cls = "all"; ip = ""; actorId = ""; days = ""
                            }) { Text("Reset filters") }
                        }
                    }
                    item {
                        MirrorCard("Audit feed", total) {
                            if (state.data == null) {
                                Text("Loading…", color = SentinelMuted)
                            } else if (rows.isEmpty()) {
                                Text("No activity matches.", color = SentinelMuted)
                            } else {
                                rows.forEach { row ->
                                    Text(Phase8Logic.activityHeadline(row))
                                    val detail = Phase3Logic.str(row, "detail")
                                    if (detail.isNotBlank()) {
                                        Text(
                                            detail, color = SentinelMuted,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    Text(
                                        Phase8Logic.activityMeta(row),
                                        color = SentinelMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                    item {
                        val failed = data?.let { Phase3Logic.arr(it, "failedByIp") } ?: emptyList()
                        MirrorCard("Failed sign-ins by IP", failed.size) {
                            if (failed.isEmpty()) {
                                Text("No failed sign-ins.", color = SentinelMuted)
                            }
                            failed.forEach { f ->
                                Text(Phase3Logic.str(f, "ip"))
                                val meta = listOf(
                                    "${Phase3Logic.int(f, "count") ?: 0} failed",
                                    Phase3Logic.str(f, "lastActionLabel"),
                                    (Phase3Logic.long(f, "lastAt") ?: 0L).let { at ->
                                        if (at > 0) formatEpochMs(at) else ""
                                    },
                                ).filter { it.isNotBlank() }.joinToString(" · ")
                                Text(meta, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                    item {
                        val sessions = sessionsState.data?.let { Phase3Logic.arr(it, "sessions") }
                            ?: emptyList()
                        MirrorCard("Active sessions", sessions.size) {
                            when {
                                sessionsState.error != null && sessionsState.data == null ->
                                    Text(sessionsState.error!!, color = MaterialTheme.colorScheme.error)
                                sessionsState.data == null -> SkeletonList(3)
                                sessions.isEmpty() -> Text("No active sessions.", color = SentinelMuted)
                                else -> sessions.forEach { s ->
                                    Text(
                                        Phase3Logic.str(s, "displayName")
                                            .ifBlank { Phase3Logic.str(s, "username") },
                                    )
                                    val meta = listOf(
                                        Phase3Logic.str(s, "role"),
                                        (Phase3Logic.long(s, "createdAt") ?: 0L).let { at ->
                                            if (at > 0) "Since " + formatEpochMs(at) else ""
                                        },
                                        if (Phase3Logic.bool(s, "isCurrent") == true) "Current session" else "",
                                    ).filter { it.isNotBlank() }.joinToString(" · ")
                                    if (meta.isNotBlank()) {
                                        Text(
                                            meta, color = SentinelMuted,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    TextButton(onClick = { revokeTarget = s }) { Text("Revoke") }
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                            OutlinedButton(onClick = { sessionsState.reloadKey++ }) { Text("Refresh") }
                        }
                    }
                    item {
                        MirrorCard("Public site") {
                            when {
                                visitorsState.error != null && visitorsState.data == null ->
                                    Text(visitorsState.error!!, color = MaterialTheme.colorScheme.error)
                                visitorsState.data == null -> SkeletonList(3)
                                else -> {
                                    val v = visitorsState.data!!
                                    if (Phase3Logic.bool(v, "connected") == true) {
                                        Phase6Logic.flatten(v).forEach { (k, value) ->
                                            if (k != "ok" && k != "connected") {
                                                DetailLine(k, value)
                                            }
                                        }
                                    } else {
                                        Text(
                                            "Public visitor feed is not connected.",
                                            color = SentinelMuted,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    revokeTarget?.let { s ->
        val sid = Phase3Logic.int(s, "id") ?: 0
        val name = Phase3Logic.str(s, "displayName").ifBlank { Phase3Logic.str(s, "username") }
        ConfirmGate(
            title = "Revoke this session?",
            body = "$name\n\nThat sign-in ends immediately.",
            confirmLabel = "Revoke",
            onDismiss = { revokeTarget = null },
            onConfirm = {
                revokeTarget = null
                scope.launch {
                    try {
                        val out = viewModel.api.activityRevokeSession(sid)
                        notice = Phase3Logic.str(out, "message").ifBlank { "Session revoked." }
                        error = null
                        sessionsState.reloadKey++
                        state.reloadKey++
                    } catch (e: Exception) {
                        error = plainError(e, "Could not revoke that session. Try again.")
                    }
                }
            },
        )
    }
}
