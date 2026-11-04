package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
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
import net.sentinelsafeguard.app.data.api.ScheduleResponse
import java.time.LocalDate

/**
 * My Schedule (Phase 9, STAFF shell): web's Schedule payload
 * scoped to the caller — GET /api/app/my-schedule rides the
 * same schedule handler server-side, which forces scope
 * "mine" (own jobs only) for STAFF. The screen renders that
 * scoped payload grouped by day, exactly as the Schedule view
 * renders its own window; tapping a job opens its profile.
 */
@Composable
fun MyScheduleScreen(viewModel: AppViewModel, nav: NavController) {
    var data by remember { mutableStateOf<ScheduleResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val start = remember { LocalDate.now().minusDays(7) }
    val end = remember { LocalDate.now().plusDays(60) }
    LaunchedEffect(Unit) {
        try {
            data = viewModel.api.myScheduleWindow(start.toString(), end.toString())
        } catch (e: Exception) {
            error = plainError(e, "Your schedule could not be loaded. Check your connection and try again.")
        }
    }
    Column {
        MirrorHeader("My Schedule", MirrorCopy.subtitle("myschedule"))
        when {
            error != null -> ErrorBox(error!!) { }
            data == null -> SkeletonList()
            else -> {
                val resp = data!!
                LazyColumn {
                    if (resp.jobs.isEmpty()) {
                        item {
                            EmptyBox(
                                "Nothing scheduled for you",
                                "Jobs assigned to you in this window will appear here.",
                            )
                        }
                    }
                    MirrorVocab.groupJobsByDate(resp.jobs).forEach { (date, jobs) ->
                        item { SectionHeader(date, jobs.size) }
                        jobs.forEach { job ->
                            item {
                                ItemRow(
                                    title = job.title.ifBlank { "Job #${job.id}" },
                                    subtitle = listOf(
                                        job.startTime,
                                        job.location,
                                        job.clientName ?: "",
                                        job.status,
                                    ).filter { it.isNotBlank() }.joinToString(" · "),
                                    onClick = { nav.navigate("job/${job.id}") },
                                )
                            }
                        }
                    }
                    // Personal schedule entries ride the same
                    // scoped payload; render them under their day.
                    resp.entries.forEach { entry ->
                        item { SectionHeader(entry.workDate) }
                        item {
                            ItemRow(
                                title = entry.entryType.ifBlank { "Schedule entry" },
                                subtitle = listOf(
                                    entry.startTime,
                                    entry.endTime,
                                    entry.notes,
                                ).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { null },
                            )
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}
