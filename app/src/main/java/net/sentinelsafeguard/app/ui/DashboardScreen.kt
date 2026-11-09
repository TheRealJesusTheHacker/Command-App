package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import net.sentinelsafeguard.app.data.api.DashboardResponse
import net.sentinelsafeguard.app.ui.theme.SentinelGreen
import net.sentinelsafeguard.app.ui.theme.SentinelGreenDim
import net.sentinelsafeguard.app.ui.theme.SentinelSurfaceTop
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Dashboard (mirror spec §3.1): web paintDashboard() — header +
 * Operational pill, attention chips, canWrite-gated quick-create,
 * the 6 tiles in web order, Recent activity. Data: B1
 * GET /api/app/dashboard (cents stay integers until format).
 */
@Composable
fun DashboardScreen(viewModel: AppViewModel, nav: NavController) {
    var dash by remember { mutableStateOf<DashboardResponse?>(null) }
    var failed by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        failed = false
        try {
            dash = viewModel.api.dashboard()
        } catch (e: Exception) {
            plainError(e, "Couldn't load the dashboard")
            failed = true
        }
    }

    when {
        failed -> ErrorBox("Couldn't load the dashboard") { reloadKey++ }
        dash == null -> SkeletonList(6)
        else -> {
            val d = dash!!
            LazyColumn {
                item {
                    MirrorHeader("Dashboard") {
                        // hunt18: the Operational chip becomes a
                        // filled pill — SurfaceTop fill, gradient
                        // hairline, same string.
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SentinelSurfaceTop),
                            border = BorderStroke(1.dp, gradientHairlineBrush()),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(
                                "● Operational",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                // Attention chips — only kinds with count > 0.
                val attention = d.attention.filter { it.count > 0 }
                if (attention.isNotEmpty()) {
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            attention.forEach { chip ->
                                val (label, route) = when (chip.kind) {
                                    "followups" -> "Follow-ups due" to "clients"
                                    "overdueCases" -> "Overdue cases" to "cases"
                                    "unassignedTickets" -> "Unassigned tickets" to "tickets"
                                    "signinAlerts" -> "Sign-in alerts" to "activity"
                                    else -> chip.kind to "dashboard"
                                }
                                FilterChip(
                                    selected = false,
                                    onClick = { nav.navigate(route) },
                                    label = { Text("$label · ${chip.count}") },
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                            }
                        }
                    }
                }

                // Quick-create row (canWrite-gated, web order).
                val cw = d.canWrite
                if (cw.clients || cw.tickets || cw.cases || cw.invoices) {
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            if (cw.clients) {
                                Button(
                                    onClick = { nav.navigate("clientNew") },
                                    modifier = Modifier.padding(end = 8.dp),
                                ) { Text("+ New client") }
                            }
                            if (cw.tickets) {
                                OutlinedButton(
                                    onClick = { nav.navigate("ticketNew") },
                                    modifier = Modifier.padding(end = 8.dp),
                                ) { Text("+ New ticket") }
                            }
                            if (cw.cases) {
                                OutlinedButton(
                                    onClick = { nav.navigate("caseNew") },
                                    modifier = Modifier.padding(end = 8.dp),
                                ) { Text("+ New case") }
                            }
                            if (cw.invoices) {
                                OutlinedButton(onClick = { nav.navigate("invoiceNew") }) {
                                    Text("+ New invoice")
                                }
                            }
                        }
                    }
                }

                d.tickets?.let { t ->
                    item {
                        MirrorCard("Tickets — open", t.openTotal) {
                            t.byPriority.forEach { DashRow(it.priority, it.count.toString()) }
                            DashRow("Unassigned", t.unassigned.toString())
                            t.byStatus.forEach { DashRow(it.status, it.count.toString()) }
                        }
                    }
                }
                d.cases?.let { c ->
                    item {
                        MirrorCard("Cases", c.total) {
                            SegmentBar(c.byStage.map { it.status to it.count })
                            c.byStage.forEach { DashRow(it.status, it.count.toString()) }
                            DashRow("Overdue", c.overdue.toString())
                            DashRow("Unassigned", c.unassigned.toString())
                        }
                    }
                }
                d.clients?.let { c ->
                    item {
                        MirrorCard("Clients", c.total) {
                            SegmentBar(
                                MirrorVocab.TIERS.map { tier ->
                                    (MirrorVocab.TIER_LABELS[tier] ?: tier) to (c.byTier[tier] ?: 0)
                                } + ("Untiered" to (c.byTier["untiered"] ?: 0))
                            )
                            MirrorVocab.TIERS.forEach { tier ->
                                DashRow(MirrorVocab.TIER_LABELS[tier] ?: tier, (c.byTier[tier] ?: 0).toString())
                            }
                            DashRow("Untiered", (c.byTier["untiered"] ?: 0).toString())
                            DashRow("Pipeline value", MirrorVocab.formatCents(c.pipelineValueCents))
                            c.byStage.forEach { stage ->
                                DashRow(
                                    stage.stage,
                                    if (stage.valuedCount > 0) {
                                        "${stage.count} · ${MirrorVocab.formatCents(stage.valueCents)}"
                                    } else {
                                        stage.count.toString()
                                    },
                                )
                            }
                        }
                    }
                }
                d.invoices?.let { inv ->
                    item {
                        MirrorCard("Invoices", inv.count) {
                            DashRow(
                                "Outstanding (Sent)",
                                "${inv.outstanding.count} · ${MirrorVocab.formatCents(inv.outstanding.totalCents)}",
                            )
                            DashRow(
                                "Overdue",
                                "${inv.overdue.count} · ${MirrorVocab.formatCents(inv.overdue.totalCents)}",
                            )
                            DashRow("Drafts", inv.draftCount.toString())
                            DashRow("Paid to date", MirrorVocab.formatCents(inv.paidTotalCents))
                        }
                    }
                }
                d.assets?.let { a ->
                    item {
                        MirrorCard("Assets", a.total) {
                            a.byStatus.forEach { DashRow(it.status, it.count.toString()) }
                            DashRow(
                                "Scope",
                                "Sentinel ${a.byScope["Sentinel"] ?: 0} · Client ${a.byScope["Client"] ?: 0}",
                            )
                        }
                    }
                }
                d.presence?.let { p ->
                    item {
                        MirrorCard("Team presence", p.activeSessionCount) {
                            if (p.team.isEmpty()) {
                                Text("No one is signed in right now.", color = SentinelMuted)
                            }
                            p.team.forEach { member ->
                                DashRow(
                                    "${member.displayName} · ${member.role}",
                                    "${member.sessions} session(s)",
                                )
                            }
                        }
                    }
                }

                // 1.5.4: the website-analytics card (web's
                // dashboard widget, mirrored). It self-gates:
                // accounts without admin:read never see it —
                // the composable renders nothing on the 403.
                item { AnalyticsDashboardCard(viewModel, nav, reloadKey) }

                item {
                    MirrorCard("Recent activity") {
                        if (d.recentActivity.isEmpty()) {
                            Text("None", color = SentinelMuted)
                        }
                        d.recentActivity.forEach { row ->
                            Text(
                                "${formatEpochMs(row.createdAt)} — ${row.action}" +
                                    (row.detail?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun DashRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum",
            ),
        )
    }
}

/** The web segmented bar, proportional segments over the counts. */
@Composable
private fun SegmentBar(segments: List<Pair<String, Int>>) {
    val total = segments.sumOf { it.second }
    if (total <= 0) return
    Row(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .padding(bottom = 4.dp),
    ) {
        segments.filter { it.second > 0 }.forEachIndexed { idx, (_, count) ->
            Box(
                Modifier
                    .weight(count.toFloat())
                    .height(6.dp)
                    .padding(end = if (idx == segments.lastIndex) 0.dp else 2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                SentinelGreen.copy(alpha = 1f - (idx % 4) * 0.18f),
                                SentinelGreenDim.copy(alpha = 1f - (idx % 4) * 0.18f),
                            ),
                        ),
                        RoundedCornerShape(50),
                    ),
            )
        }
    }
}
