package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sentinelsafeguard.app.data.api.Job
import net.sentinelsafeguard.app.ui.theme.SentinelMuted
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Schedule (spec §2.1): Sentinel jobs only — today and this week,
 * with a "my jobs" filter and a job detail view. Read-only in v1.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(viewModel: AppViewModel) {
    var selectedId by remember { mutableStateOf<Int?>(null) }
    if (selectedId != null) {
        JobDetailScreen(viewModel, selectedId!!) { selectedId = null }
        return
    }

    var mineOnly by remember { mutableStateOf(false) }
    var jobs by remember { mutableStateOf<List<Job>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    val myName = viewModel.secure.displayName ?: ""

    LaunchedEffect(reloadKey) {
        error = null
        try {
            jobs = viewModel.api.schedule()
        } catch (e: Exception) {
            error = e.message ?: "The schedule could not be loaded. Check your connection and try again."
        }
    }

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = !mineOnly,
                onClick = { mineOnly = false },
                label = { Text("All jobs") },
            )
            FilterChip(
                selected = mineOnly,
                onClick = { mineOnly = true },
                label = { Text("My jobs") },
            )
        }
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            jobs == null -> LoadingBox("Loading schedule…")
            else -> {
                val visible = jobs!!.filter { job ->
                    !mineOnly || job.assignees.any {
                        it.displayName.equals(myName, ignoreCase = true)
                    }
                }
                val todayText = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val (todays, rest) = visible.partition { job ->
                    job.workDate == todayText
                }
                LazyColumn {
                    item { SectionHeader("Jobs today", todays.size) }
                    if (todays.isEmpty()) {
                        item { ItemRow("No jobs today", "Nothing is booked for today.") }
                    } else {
                        items(todays) { job -> JobRow(job) { selectedId = job.id } }
                    }
                    item { SectionHeader("This week", rest.size) }
                    if (rest.isEmpty()) {
                        item { ItemRow("Nothing else this week", "No more jobs are booked this week.") }
                    } else {
                        items(rest) { job -> JobRow(job) { selectedId = job.id } }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun JobRow(job: Job, onClick: () -> Unit) {
    ItemRow(
        title = job.title,
        subtitle = listOfNotNull(
            job.workDate.takeIf { it.isNotBlank() },
            job.startTime.takeIf { it.isNotBlank() }?.let { "at $it" },
            job.clientName?.takeIf { it.isNotBlank() },
        ).joinToString(" · "),
        onClick = onClick,
    )
}

@Composable
fun JobDetailScreen(viewModel: AppViewModel, jobId: Int, onBack: () -> Unit) {
    var job by remember { mutableStateOf<Job?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(jobId, reloadKey) {
        try {
            job = viewModel.api.job(jobId)
        } catch (e: Exception) {
            error = e.message ?: "This job could not be loaded. Go back and try again."
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        TextButton(onClick = onBack) { Text("‹ Back to schedule") }
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            job == null -> LoadingBox("Loading job…")
            else -> {
                val j = job!!
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(j.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    DetailLine(
                        "When",
                        listOfNotNull(
                            j.workDate.takeIf { it.isNotBlank() },
                            if (j.allDay) "All day" else j.startTime.takeIf { it.isNotBlank() },
                            if (j.allDay) null
                            else j.endTime.takeIf { it.isNotBlank() }?.let { "until $it" },
                        ).joinToString(" "),
                    )
                    DetailLine("Client", j.clientName)
                    DetailLine("Address", j.location)
                    DetailLine(
                        "Who is going",
                        if (j.assignees.isEmpty()) null
                        else j.assignees.joinToString(", ") { it.displayName },
                    )
                    DetailLine("Notes", j.notes)
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun DetailLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Spacer(Modifier.height(10.dp))
    Text(label, color = SentinelMuted, style = MaterialTheme.typography.labelLarge)
    Text(value, style = MaterialTheme.typography.bodyLarge)
}
