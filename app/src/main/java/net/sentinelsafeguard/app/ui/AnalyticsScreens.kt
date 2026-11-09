package net.sentinelsafeguard.app.ui

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import net.sentinelsafeguard.app.data.api.SignedOutException
import net.sentinelsafeguard.app.data.api.analytics
import net.sentinelsafeguard.app.ui.theme.SentinelMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 1.5.4: native website analytics — the mirror of Command's
 * web dashboard widget (analyticssummary), over
 * GET /api/app/analytics. The web widget's own presentation
 * is the spec: the six rows in web order, counts with
 * thousands separators, avg. visit as m:ss, the "(direct)"
 * referrer label, rolling headline numbers (M-03-style,
 * reduced-motion snaps), and the "self-hosted analytics —
 * no third party" footer.
 *
 * Gate discipline (the gate itself is server-side,
 * admin:read): the Dashboard card attempts the call and on
 * the gate's 403 renders NOTHING — no card, no error noise;
 * the feature simply is not this account's. The full view is
 * reached only through the card, so its own Unauthorized
 * state is a belt-and-braces quiet line for a direct route.
 * Unavailable reasons render AnalyticsLogic's plain copy;
 * a genuinely empty period renders as the empty state, not
 * a grid of zeros (AnalyticsLogic.isEmptyPeriod).
 */

/** One load of the endpoint, classified. A null [state] with
 *  [failed] set is a transport/other failure. */
private data class AnalyticsOutcome(
    val state: AnalyticsLogic.State? = null,
    val failed: Boolean = false,
)

private suspend fun AppViewModel.loadAnalytics(): AnalyticsOutcome =
    try {
        AnalyticsOutcome(state = AnalyticsLogic.parse(api.analytics()))
    } catch (e: SignedOutException) {
        // The shell is already returning to sign-in.
        AnalyticsOutcome()
    } catch (e: Exception) {
        val classified = AnalyticsLogic.stateForError(e)
        if (classified != null) AnalyticsOutcome(state = classified)
        else AnalyticsOutcome(failed = true)
    }

/**
 * The Dashboard card. [refreshSignal] is the dashboard's own
 * reload counter, so a dashboard retry re-asks the gate too.
 * Renders nothing at all until the server has answered, and
 * nothing ever for accounts the gate refuses.
 */
@Composable
fun AnalyticsDashboardCard(viewModel: AppViewModel, nav: NavController, refreshSignal: Int) {
    var outcome by remember { mutableStateOf<AnalyticsOutcome?>(null) }
    LaunchedEffect(refreshSignal) {
        outcome = viewModel.loadAnalytics()
    }
    val state = outcome?.state ?: return
    when (state) {
        is AnalyticsLogic.State.Available -> {
            val stats = state.stats
            MirrorCard(
                "Website analytics",
                footerLabel = "Open analytics",
                onFooter = { nav.navigate("analytics") },
            ) {
                if (AnalyticsLogic.isEmptyPeriod(stats)) {
                    Text(
                        AnalyticsLogic.emptyPeriodCopy(stats),
                        color = SentinelMuted,
                    )
                } else {
                    AnalyticsStatRows(stats)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    analyticsFooterLine(stats),
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        is AnalyticsLogic.State.Unavailable -> {
            MirrorCard(
                "Website analytics",
                footerLabel = "Open analytics",
                onFooter = { nav.navigate("analytics") },
            ) {
                Text(AnalyticsLogic.reasonCopy(state.reason), color = SentinelMuted)
            }
        }
        // Unauthorized / Malformed: no card, no noise — the
        // gate's answer is the whole story on the dashboard.
        else -> Unit
    }
}

/** The full native analytics view (route "analytics"). */
@Composable
fun AnalyticsScreen(viewModel: AppViewModel, nav: NavController) {
    var outcome by remember { mutableStateOf<AnalyticsOutcome?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        failure = null
        val loaded = viewModel.loadAnalytics()
        outcome = loaded
        if (loaded.failed) failure = "Couldn't load analytics. Check your connection and try again."
        refreshing = false
    }

    val state = outcome?.state
    when {
        outcome == null -> SkeletonList(6)
        failure != null -> Column {
            MirrorHeader("Website analytics")
            ErrorBox(failure!!) { reloadKey++ }
        }
        state is AnalyticsLogic.State.Unauthorized -> Column {
            MirrorHeader("Website analytics")
            MirrorCard("Website analytics") {
                Text(
                    "Website analytics is limited to accounts with admin access in Command.",
                    color = SentinelMuted,
                )
            }
        }
        state is AnalyticsLogic.State.Malformed -> Column {
            MirrorHeader("Website analytics")
            ErrorBox("Analytics sent back something the app couldn't read.") { reloadKey++ }
        }
        state is AnalyticsLogic.State.Unavailable -> Column {
            MirrorHeader("Website analytics")
            MirrorCard("Website analytics") {
                Text(AnalyticsLogic.reasonCopy(state.reason), color = SentinelMuted)
                TextButton(onClick = { reloadKey++ }) { Text("Try again") }
            }
        }
        state is AnalyticsLogic.State.Available -> {
            val stats = state.stats
            val pullState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { refreshing = true; reloadKey++ },
                state = pullState,
                indicator = {
                    DampedPullIndicator(pullState, refreshing, Modifier.align(Alignment.TopCenter))
                },
            ) {
                LazyColumn {
                    item { MirrorHeader("Website analytics") }
                    item {
                        MirrorCard("Website analytics") {
                            if (AnalyticsLogic.isEmptyPeriod(stats)) {
                                Text(
                                    AnalyticsLogic.emptyPeriodCopy(stats),
                                    color = SentinelMuted,
                                )
                            } else {
                                AnalyticsStatRows(stats)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                analyticsFooterLine(stats),
                                color = SentinelMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    if (stats.topPages.isNotEmpty()) {
                        item {
                            MirrorCard("Top pages") {
                                stats.topPages.forEach { row ->
                                    AnalyticsRow(row.label, AnalyticsLogic.formatCount(row.count))
                                }
                            }
                        }
                    }
                    if (stats.topReferrers.isNotEmpty()) {
                        item {
                            MirrorCard("Top referrers") {
                                stats.topReferrers.forEach { row ->
                                    AnalyticsRow(
                                        AnalyticsLogic.referrerLabel(row.label),
                                        AnalyticsLogic.formatCount(row.count),
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

/** The six headline rows, in the web widget's order. Visitors
 *  and Pageviews roll (web's M-03 pair); the rest render as
 *  served — Active now in particular never ticks on its own:
 *  it is the server's number, refreshed only by a reload. */
@Composable
private fun AnalyticsStatRows(stats: AnalyticsLogic.Stats) {
    AnalyticsRollingRow("Visitors", stats.visitors)
    AnalyticsRollingRow("Pageviews", stats.pageviews)
    AnalyticsRow("Visits", AnalyticsLogic.formatCount(stats.visits))
    AnalyticsRow("Bounce rate", AnalyticsLogic.formatBounce(stats.bounceRate))
    AnalyticsRow("Avg. visit", AnalyticsLogic.formatAvgVisit(stats.avgVisitSecs))
    AnalyticsRow(
        AnalyticsLogic.ACTIVE_LABEL,
        AnalyticsLogic.formatCount(stats.activeVisitors),
    )
}

@Composable
private fun AnalyticsRow(label: String, value: String) {
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

@Composable
private fun AnalyticsRollingRow(label: String, value: Long) {
    val reduced = rememberReducedMotion()
    val animated by animateIntAsState(
        targetValue = value.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
        animationSpec = if (reduced) snap() else tween(durationMillis = 600),
        label = "analyticsRollingCount",
    )
    AnalyticsRow(label, AnalyticsLogic.formatCount(animated.toLong()))
}

/** "Last 30 days · as of 2:41 PM · self-hosted analytics — no
 *  third party" — web's footer, with the stale marker web
 *  uses when the server serves a cached copy. */
private fun analyticsFooterLine(stats: AnalyticsLogic.Stats): String = buildString {
    append(AnalyticsLogic.periodLabel(stats.periodDays))
    stats.asOfEpochSec?.let { asOf ->
        append(" · as of ")
        append(SimpleDateFormat("h:mm a", Locale.US).format(Date(asOf * 1000)))
    }
    if (stats.stale) append(" · cached copy")
    append(" · self-hosted analytics — no third party")
}
