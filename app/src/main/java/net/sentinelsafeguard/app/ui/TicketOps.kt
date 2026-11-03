package net.sentinelsafeguard.app.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.Ticket
import net.sentinelsafeguard.app.data.api.applyPlaybook
import net.sentinelsafeguard.app.data.api.cannedResponses
import net.sentinelsafeguard.app.data.api.checklistRun
import net.sentinelsafeguard.app.data.api.checklists
import net.sentinelsafeguard.app.data.api.completeChecklistItem
import net.sentinelsafeguard.app.data.api.consumePart
import net.sentinelsafeguard.app.data.api.convertTimeToInvoice
import net.sentinelsafeguard.app.data.api.createExpense
import net.sentinelsafeguard.app.data.api.createTimeEntry
import net.sentinelsafeguard.app.data.api.deleteCannedResponse
import net.sentinelsafeguard.app.data.api.deleteExpense
import net.sentinelsafeguard.app.data.api.deletePhoto
import net.sentinelsafeguard.app.data.api.deletePlan
import net.sentinelsafeguard.app.data.api.deleteTimeEntry
import net.sentinelsafeguard.app.data.api.exportFull
import net.sentinelsafeguard.app.data.api.exportLabels
import net.sentinelsafeguard.app.data.api.exportPacket
import net.sentinelsafeguard.app.data.api.exportPriceBook
import net.sentinelsafeguard.app.data.api.exportWorkspace
import net.sentinelsafeguard.app.data.api.mergeTickets
import net.sentinelsafeguard.app.data.api.parts
import net.sentinelsafeguard.app.data.api.plans
import net.sentinelsafeguard.app.data.api.playbooks
import net.sentinelsafeguard.app.data.api.renderCannedResponse
import net.sentinelsafeguard.app.data.api.restockPart
import net.sentinelsafeguard.app.data.api.saveCannedResponse
import net.sentinelsafeguard.app.data.api.saveChecklistTemplate
import net.sentinelsafeguard.app.data.api.savePart
import net.sentinelsafeguard.app.data.api.savePlan
import net.sentinelsafeguard.app.data.api.savePlaybook
import net.sentinelsafeguard.app.data.api.saveSlaConfig
import net.sentinelsafeguard.app.data.api.setFollowUpState
import net.sentinelsafeguard.app.data.api.signoffChecklistRun
import net.sentinelsafeguard.app.data.api.slaConfig
import net.sentinelsafeguard.app.data.api.splitTicket
import net.sentinelsafeguard.app.data.api.startChecklistRun
import net.sentinelsafeguard.app.data.api.startTimer
import net.sentinelsafeguard.app.data.api.stopTimer
import net.sentinelsafeguard.app.data.api.ticketAudioNotes
import net.sentinelsafeguard.app.data.api.ticketExpenses
import net.sentinelsafeguard.app.data.api.ticketFollowUps
import net.sentinelsafeguard.app.data.api.ticketOps
import net.sentinelsafeguard.app.data.api.ticketPhotos
import net.sentinelsafeguard.app.data.api.timeEntries
import net.sentinelsafeguard.app.data.api.uploadAudio
import net.sentinelsafeguard.app.data.api.uploadPhoto
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Ticket Ops (Phase 3, mirror spec §6): the panels web injects
 * into the ticket profile (frontend inventory §4.4) — SLA,
 * follow-ups, expenses, photos/voice, canned responses, time
 * tracking, playbooks, checklist runs, plans, parts — plus
 * merge/split with web's confirm gates, rendered inside the
 * ticket profile where web puts them.
 */
@Composable
fun TicketOpsSections(viewModel: AppViewModel, nav: NavController, ticket: Ticket) {
    OpsSummaryCard(viewModel, ticket)
    FollowUpsCard(viewModel, ticket)
    ExpensesCard(viewModel, ticket)
    PhotosCard(viewModel, ticket)
    VoiceNotesCard(viewModel, ticket)
    CannedResponsesCard(viewModel, ticket)
    TimeTrackingCard(viewModel, ticket)
    PlaybooksCard(viewModel)
    ChecklistRunsCard(viewModel, ticket)
    PlansCard(viewModel, ticket)
    PartsCard(viewModel, ticket)
    MergeSplitCard(viewModel, nav, ticket)
}

@Composable
private fun OpsSummaryCard(viewModel: AppViewModel, ticket: Ticket) {
    AsyncCard("Ticket Ops", loader = { viewModel.api.ticketOps(ticket.id) }) { data, _ ->
        val ops = Phase3Logic.obj(data, "ops") ?: data
        val sla = Phase3Logic.obj(ops, "sla")
        if (sla != null) {
            Text("SLA", style = MaterialTheme.typography.titleSmall)
            DetailLine(
                "Response",
                "${Phase3Logic.str(sla, "responseState")} · target " +
                    Phase3Logic.formatTargetMinutes(Phase3Logic.int(sla, "responseTargetMin")),
            )
            DetailLine(
                "Resolution",
                "${Phase3Logic.str(sla, "resolutionState")} · target " +
                    Phase3Logic.formatTargetMinutes(Phase3Logic.int(sla, "resolutionTargetMin")),
            )
            val breaches = Phase3Logic.arr(sla, "breaches")
            if (breaches.isNotEmpty()) {
                Text("${breaches.size} breach(es) recorded.", color = SentinelMuted)
            }
        }
        val merges = Phase3Logic.arr(ops, "merges")
        if (merges.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("Merged tickets: ${merges.size}", color = SentinelMuted)
        }
        DetailLine(
            "Expenses total",
            MirrorVocab.formatCents(Phase3Logic.long(ops, "expensesTotalCents") ?: 0),
        )
    }
    SlaConfigCard(viewModel)
}

@Composable
private fun SlaConfigCard(viewModel: AppViewModel) {
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    AsyncCard("SLA targets", loader = { viewModel.api.slaConfig() }) { data, reload ->
        val config = Phase3Logic.obj(data, "config") ?: data
        listOf("Critical", "High", "Normal", "Low").forEach { priority ->
            val row = Phase3Logic.obj(config, priority) ?: return@forEach
            var response by remember(priority) {
                mutableStateOf((Phase3Logic.int(row, "responseTargetMin") ?: 0).toString())
            }
            var resolution by remember(priority) {
                mutableStateOf((Phase3Logic.int(row, "resolutionTargetMin") ?: 0).toString())
            }
            Text(priority, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp))
            TextFieldRow("Response target (minutes)", response, { response = it })
            TextFieldRow("Resolution target (minutes)", resolution, { resolution = it })
            OutlinedButton(onClick = {
                scope.launch {
                    try {
                        viewModel.api.saveSlaConfig(
                            priority,
                            response.toIntOrNull() ?: 0,
                            resolution.toIntOrNull() ?: 0,
                        )
                        notice = "SLA targets saved."
                        reload()
                    } catch (e: Exception) {
                        error = plainError(e, "SLA targets could not be saved. Try again.")
                    }
                }
            }) { Text("Save $priority targets") }
        }
        if (notice != null) Text(notice!!, color = SentinelMuted)
        ActionErrorText(error)
    }
}

@Composable
private fun FollowUpsCard(viewModel: AppViewModel, ticket: Ticket) {
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    AsyncCard(
        "Follow-ups", "followUps",
        loader = { viewModel.api.ticketFollowUps("open") },
    ) { data, reload ->
        val mine = Phase3Logic.arr(data, "followUps")
            .filter { Phase3Logic.int(it, "ticketId") == ticket.id }
        if (mine.isEmpty()) Text("No open follow-ups for this ticket.", color = SentinelMuted)
        mine.forEach { followUp ->
            val fid = Phase3Logic.int(followUp, "id")
            Text(
                "${Phase3Logic.str(followUp, "kind")} · due ${Phase3Logic.str(followUp, "dueDate")}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            val draft = Phase3Logic.str(followUp, "draftText")
            if (draft.isNotBlank()) Text(draft, color = SentinelMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    if (fid != null) scope.launch {
                        try {
                            viewModel.api.setFollowUpState(fid, "done"); reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The follow-up could not be updated. Try again.")
                        }
                    }
                }) { Text("Mark done") }
                TextButton(onClick = {
                    if (fid != null) scope.launch {
                        try {
                            viewModel.api.setFollowUpState(fid, "dismissed"); reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The follow-up could not be updated. Try again.")
                        }
                    }
                }) { Text("Dismiss") }
            }
        }
        ActionErrorText(error)
    }
}

@Composable
private fun ExpensesCard(viewModel: AppViewModel, ticket: Ticket) {
    val scope = rememberCoroutineScope()
    var category by remember { mutableStateOf("mileage") }
    var description by remember { mutableStateOf("") }
    var miles by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var spentOn by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val isOwner = viewModel.me?.user?.role == "OWNER"
    AsyncCard(
        "Expenses", "expenses",
        loader = { viewModel.api.ticketExpenses(ticket.id) },
    ) { data, reload ->
        val expenses = Phase3Logic.arr(data, "expenses")
        if (expenses.isEmpty()) Text("No expenses logged.", color = SentinelMuted)
        expenses.forEach { expense ->
            val eid = Phase3Logic.int(expense, "id")
            Text(
                listOfNotNull(
                    Phase3Logic.str(expense, "category").takeIf { it.isNotBlank() },
                    Phase3Logic.str(expense, "description").takeIf { it.isNotBlank() },
                    Phase3Logic.displayValue("amountCents", expense["amountCents"]),
                    Phase3Logic.str(expense, "spentOn").takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 2.dp),
            )
            if (isOwner && eid != null) {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            viewModel.api.deleteExpense(eid); reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The expense could not be deleted. Try again.")
                        }
                    }
                }) { Text("Delete") }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Add expense", style = MaterialTheme.typography.titleSmall)
        TextFieldRow("Category", category, { category = it })
        TextFieldRow("Description", description, { description = it })
        TextFieldRow("Miles (mileage)", miles, { miles = it })
        TextFieldRow("Amount ($, other categories)", amount, { amount = it })
        TextFieldRow("Spent on (yyyy-MM-dd)", spentOn, { spentOn = it })
        Button(
            enabled = description.isNotBlank(),
            onClick = {
                scope.launch {
                    try {
                        viewModel.api.createExpense(buildJsonObject {
                            put("ticketId", ticket.id)
                            put("category", category)
                            put("description", description)
                            miles.toDoubleOrNull()?.let { put("miles", it) }
                            if (category == "mileage") put("rateCentsPerMile", 67)
                            amount.toDoubleOrNull()?.let { put("amountCents", (it * 100).toInt()) }
                            if (spentOn.isNotBlank()) put("spentOn", spentOn)
                        })
                        description = ""; miles = ""; amount = ""; reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The expense could not be logged. Try again.")
                    }
                }
            },
        ) { Text("Add expense") }
        ActionErrorText(error)
    }
}

@Composable
private fun PhotosCard(viewModel: AppViewModel, ticket: Ticket) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var caption by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var viewPhoto by remember { mutableStateOf<ByteArray?>(null) }
    var reloadRef by remember { mutableStateOf<(() -> Unit)?>(null) }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                        ?: throw IllegalStateException("unreadable")
                    val name = uri.lastPathSegment?.substringAfterLast('/') ?: "photo.jpg"
                    viewModel.api.uploadPhoto(ticket.id, bytes, name, caption)
                    caption = ""
                    reloadRef?.invoke()
                } catch (e: Exception) {
                    error = plainError(e, "The photo could not be uploaded. Try again.")
                }
            }
        }
    }
    AsyncCard(
        "Photos", "photos",
        loader = { viewModel.api.ticketPhotos(ticket.id) },
    ) { data, reload ->
        reloadRef = reload
        val photos = Phase3Logic.arr(data, "photos")
        if (photos.isEmpty()) Text("No photos on this ticket.", color = SentinelMuted)
        photos.forEach { photo ->
            val pid = Phase3Logic.int(photo, "id")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    listOfNotNull(
                        Phase3Logic.str(photo, "fileName").takeIf { it.isNotBlank() }
                            ?: Phase3Logic.str(photo, "caption").takeIf { it.isNotBlank() },
                        Phase3Logic.str(photo, "kind").takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = {
                    if (pid != null) scope.launch {
                        try {
                            viewPhoto = viewModel.api.downloadAll("/api/app/photos/$pid/download")
                        } catch (e: Exception) {
                            error = plainError(e, "The photo could not be opened. Try again.")
                        }
                    }
                }) { Text("View") }
                TextButton(onClick = {
                    if (pid != null) scope.launch {
                        try {
                            viewModel.api.deletePhoto(pid); reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The photo could not be deleted. Try again.")
                        }
                    }
                }) { Text("Delete") }
            }
        }
        TextFieldRow("Caption for the next upload", caption, { caption = it })
        Button(onClick = { picker.launch("image/*") }) { Text("Upload photo") }
        ActionErrorText(error)
        viewPhoto?.let { bytes ->
            val bitmap = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
            AlertDialog(
                onDismissRequest = { viewPhoto = null },
                title = { Text("Photo") },
                text = {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Ticket photo",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text("This file is not an image the phone can preview.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        shareBytes(context, "photo.jpg", "image/jpeg", bytes)
                        viewPhoto = null
                    }) { Text("Share") }
                },
                dismissButton = {
                    TextButton(onClick = { viewPhoto = null }) { Text("Close") }
                },
            )
        }
    }
}

@Composable
private fun VoiceNotesCard(viewModel: AppViewModel, ticket: Ticket) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var caption by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadRef by remember { mutableStateOf<(() -> Unit)?>(null) }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                        ?: throw IllegalStateException("unreadable")
                    val name = uri.lastPathSegment?.substringAfterLast('/') ?: "note.wav"
                    viewModel.api.uploadAudio(ticket.id, bytes, name, caption)
                    caption = ""
                    reloadRef?.invoke()
                } catch (e: Exception) {
                    error = plainError(e, "The voice note could not be uploaded. Try again.")
                }
            }
        }
    }
    AsyncCard(
        "Voice notes", "audioNotes",
        loader = { viewModel.api.ticketAudioNotes(ticket.id) },
    ) { data, reload ->
        reloadRef = reload
        val notes = Phase3Logic.arr(data, "audioNotes")
        if (notes.isEmpty()) Text("No voice notes on this ticket.", color = SentinelMuted)
        notes.forEach { note ->
            val nid = Phase3Logic.int(note, "id")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    listOfNotNull(
                        Phase3Logic.str(note, "caption").takeIf { it.isNotBlank() },
                        Phase3Logic.str(note, "fileName").takeIf { it.isNotBlank() },
                    ).joinToString(" · ").ifBlank { "Voice note" },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = {
                    if (nid != null) scope.launch {
                        try {
                            val bytes = viewModel.api.downloadAll("/api/app/audio/$nid/download")
                            shareBytes(context, "voice-note.wav", "audio/wav", bytes)
                        } catch (e: Exception) {
                            error = plainError(e, "The voice note could not be downloaded. Try again.")
                        }
                    }
                }) { Text("Download") }
            }
        }
        TextFieldRow("Caption for the next upload", caption, { caption = it })
        Button(onClick = { picker.launch("audio/*") }) { Text("Upload voice note") }
        ActionErrorText(error)
    }
}

@Composable
private fun CannedResponsesCard(viewModel: AppViewModel, ticket: Ticket) {
    val scope = rememberCoroutineScope()
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var body by remember { mutableStateOf("") }
    var rendered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AsyncCard(
        "Canned responses", "responses",
        loader = { viewModel.api.cannedResponses() },
    ) { data, reload ->
        val responses = Phase3Logic.arr(data, "responses")
        if (responses.isEmpty()) Text("No canned responses yet.", color = SentinelMuted)
        responses.forEach { response ->
            val rid = Phase3Logic.int(response, "id")
            Text(
                "${Phase3Logic.str(response, "title")} · ${Phase3Logic.str(response, "category")}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(Phase3Logic.str(response, "body"), color = SentinelMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    if (rid != null) scope.launch {
                        try {
                            val out = viewModel.api.renderCannedResponse(rid, ticket.id)
                            rendered = Phase3Logic.str(out, "text")
                        } catch (e: Exception) {
                            error = plainError(e, "The response could not be rendered. Try again.")
                        }
                    }
                }) { Text("Render for this ticket") }
                TextButton(onClick = {
                    if (rid != null) scope.launch {
                        try {
                            viewModel.api.deleteCannedResponse(rid); reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The response could not be deleted. Try again.")
                        }
                    }
                }) { Text("Delete") }
            }
        }
        if (rendered.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(rendered, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = {
                clipboard.setText(androidx.compose.ui.text.AnnotatedString(rendered))
            }) { Text("Copy rendered text") }
        }
        Spacer(Modifier.height(8.dp))
        Text("New canned response", style = MaterialTheme.typography.titleSmall)
        TextFieldRow("Title", title, { title = it })
        TextFieldRow("Category", category, { category = it })
        TextFieldRow("Body", body, { body = it }, singleLine = false, minLines = 2)
        Button(
            enabled = title.isNotBlank() && body.isNotBlank(),
            onClick = {
                scope.launch {
                    try {
                        viewModel.api.saveCannedResponse(
                            buildJsonObject {
                                put("title", title); put("category", category); put("body", body)
                            },
                            null,
                        )
                        title = ""; body = ""; reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The response could not be saved. Try again.")
                    }
                }
            },
        ) { Text("Save canned response") }
        ActionErrorText(error)
    }
}

@Composable
private fun TimeTrackingCard(viewModel: AppViewModel, ticket: Ticket) {
    val scope = rememberCoroutineScope()
    var description by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var billable by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val selected = remember { mutableStateOf(setOf<Int>()) }
    AsyncCard(
        "Time tracking", "entries",
        loader = { viewModel.api.timeEntries(ticket.id) },
    ) { data, reload ->
        val entries = Phase3Logic.arr(data, "entries")
        val running = Phase3Logic.obj(data, "running")
        if (running != null) {
            val rid = Phase3Logic.int(running, "id")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Timer running · ${Phase3Logic.str(running, "description")}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = {
                    if (rid != null) scope.launch {
                        try {
                            viewModel.api.stopTimer(rid); reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The timer could not be stopped. Try again.")
                        }
                    }
                }) { Text("Stop timer") }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = description.isNotBlank(),
                    onClick = {
                        scope.launch {
                            try {
                                viewModel.api.startTimer(ticket.id, description, billable)
                                description = ""; reload()
                            } catch (e: Exception) {
                                error = plainError(e, "The timer could not be started. Try again.")
                            }
                        }
                    },
                ) { Text("Start timer") }
            }
        }
        if (entries.isEmpty()) Text("No time entries yet.", color = SentinelMuted)
        entries.forEach { entry ->
            val eid = Phase3Logic.int(entry, "id")
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (eid != null && Phase3Logic.bool(entry, "billed") != true) {
                    Checkbox(
                        checked = eid in selected.value,
                        onCheckedChange = { on ->
                            selected.value = if (on) selected.value + eid else selected.value - eid
                        },
                    )
                }
                Text(
                    listOfNotNull(
                        Phase3Logic.str(entry, "description").takeIf { it.isNotBlank() },
                        Phase3Logic.formatDurationSeconds(
                            Phase3Logic.long(entry, "durationSeconds") ?: 0,
                        ),
                        if (Phase3Logic.bool(entry, "running") == true) "running" else null,
                        if (Phase3Logic.bool(entry, "billed") == true) "billed" else null,
                    ).joinToString(" · "),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (eid != null) {
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                viewModel.api.deleteTimeEntry(eid); reload()
                            } catch (e: Exception) {
                                error = plainError(e, "The entry could not be deleted. Try again.")
                            }
                        }
                    }) { Text("Delete") }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Add entry by hand", style = MaterialTheme.typography.titleSmall)
        TextFieldRow("Description", description, { description = it })
        TextFieldRow("Duration (minutes)", minutes, { minutes = it })
        TextFieldRow("Rate ($/hour, optional)", rate, { rate = it })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = billable, onCheckedChange = { billable = it })
            Text("Billable")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = description.isNotBlank() && minutes.toIntOrNull() != null,
                onClick = {
                    scope.launch {
                        try {
                            viewModel.api.createTimeEntry(buildJsonObject {
                                put("ticketId", ticket.id)
                                ticket.clientId?.let { put("clientId", it) }
                                put("description", description)
                                put("durationMinutes", minutes.toInt())
                                put("billable", billable)
                                rate.toDoubleOrNull()?.let { put("rateCents", (it * 100).toInt()) }
                            })
                            description = ""; minutes = ""; reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The entry could not be saved. Try again.")
                        }
                    }
                },
            ) { Text("Add entry") }
            OutlinedButton(
                enabled = selected.value.isNotEmpty() && ticket.clientId != null,
                onClick = {
                    val cid = ticket.clientId ?: return@OutlinedButton
                    scope.launch {
                        try {
                            val out = viewModel.api.convertTimeToInvoice(cid, selected.value.toList())
                            notice = "Converted to invoice " +
                                (Phase3Logic.obj(out, "invoice")?.let { Phase3Logic.str(it, "invoiceNumber") } ?: "")
                            selected.value = emptySet()
                            reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The entries could not be converted. Try again.")
                        }
                    }
                },
            ) { Text("Convert to invoice") }
        }
        if (notice != null) Text(notice!!, color = SentinelMuted)
        ActionErrorText(error)
    }
}

@Composable
private fun PlaybooksCard(viewModel: AppViewModel) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var ticketTitle by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var applyFor by remember { mutableStateOf<Int?>(null) }
    var clients by remember { mutableStateOf<List<net.sentinelsafeguard.app.data.api.Client>>(emptyList()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        clients = try { viewModel.api.clients().items } catch (e: Exception) { emptyList() }
    }
    AsyncCard("Playbooks", "playbooks", loader = { viewModel.api.playbooks() }) { data, reload ->
        val playbooks = Phase3Logic.arr(data, "playbooks")
        if (playbooks.isEmpty()) Text("No playbooks yet.", color = SentinelMuted)
        playbooks.forEach { playbook ->
            val pid = Phase3Logic.int(playbook, "id")
            Text(
                "${Phase3Logic.str(playbook, "name")} · ${Phase3Logic.str(playbook, "ticketTitle")}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 2.dp),
            )
            TextButton(onClick = { applyFor = pid }) { Text("Apply to client") }
        }
        TextFieldRow("Playbook name", name, { name = it })
        TextFieldRow("Ticket title", ticketTitle, { ticketTitle = it })
        Button(
            enabled = name.isNotBlank(),
            onClick = {
                scope.launch {
                    try {
                        viewModel.api.savePlaybook(
                            buildJsonObject {
                                put("name", name); put("ticketTitle", ticketTitle)
                                put("ticketPriority", "Normal"); put("ticketCategory", "Install")
                            },
                            null,
                        )
                        name = ""; ticketTitle = ""; reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The playbook could not be saved. Try again.")
                    }
                }
            },
        ) { Text("Save playbook") }
        if (notice != null) Text(notice!!, color = SentinelMuted)
        ActionErrorText(error)
        applyFor?.let { pid ->
            var chosen by remember { mutableStateOf(clients.firstOrNull()) }
            AlertDialog(
                onDismissRequest = { applyFor = null },
                title = { Text("Apply playbook") },
                text = {
                    androidx.compose.foundation.layout.Column {
                        Text("Pick the client this playbook runs for. Applying spawns the playbook's ticket, checklist, and site survey, as on web.")
                        if (clients.isEmpty()) {
                            Text("Loading clients…", color = SentinelMuted)
                        } else {
                            ChoiceMenu(
                                "Client",
                                chosen?.legalName ?: clients.first().legalName,
                                clients.map { it.legalName },
                            ) { picked ->
                                chosen = clients.firstOrNull { it.legalName == picked }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val cid = chosen?.id ?: clients.firstOrNull()?.id
                        applyFor = null
                        if (cid != null) scope.launch {
                            try {
                                val out = viewModel.api.applyPlaybook(pid, cid)
                                notice = Phase3Logic.str(out, "message")
                            } catch (e: Exception) {
                                error = plainError(e, "The playbook could not be applied. Try again.")
                            }
                        }
                    }) { Text("Apply playbook") }
                },
                dismissButton = {
                    TextButton(onClick = { applyFor = null }) { Text("Cancel") }
                },
            )
        }
    }
}

@Composable
private fun ChecklistRunsCard(viewModel: AppViewModel, ticket: Ticket) {
    val scope = rememberCoroutineScope()
    var templateName by remember { mutableStateOf("") }
    var templateItems by remember { mutableStateOf("") }
    var openRun by remember { mutableStateOf<JsonObject?>(null) }
    var signoffName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AsyncCard("Checklist runs", loader = { viewModel.api.checklists() }) { data, reload ->
        val templates = Phase3Logic.arr(data, "templates")
        val runs = Phase3Logic.arr(data, "runs")
            .filter { Phase3Logic.int(it, "ticketId") == ticket.id }
        Text("Templates", style = MaterialTheme.typography.titleSmall)
        if (templates.isEmpty()) Text("No checklist templates yet.", color = SentinelMuted)
        templates.forEach { template ->
            val tid = Phase3Logic.int(template, "id")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Phase3Logic.str(template, "name"),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = {
                    if (tid != null) scope.launch {
                        try {
                            val out = viewModel.api.startChecklistRun(tid, ticket.id)
                            val rid = Phase3Logic.int(out, "id")
                            if (rid != null) openRun = viewModel.api.checklistRun(rid)
                            reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The run could not be started. Try again.")
                        }
                    }
                }) { Text("Start run") }
            }
        }
        TextFieldRow("New template name", templateName, { templateName = it })
        TextFieldRow("Items (one per line)", templateItems, { templateItems = it }, singleLine = false, minLines = 3)
        Button(
            enabled = templateName.isNotBlank() && templateItems.isNotBlank(),
            onClick = {
                scope.launch {
                    try {
                        val items = JsonArray(
                            templateItems.lines().filter { it.isNotBlank() }.map { line ->
                                buildJsonObject {
                                    put("kind", "check"); put("label", line.trim()); put("required", true)
                                }
                            },
                        )
                        viewModel.api.saveChecklistTemplate(templateName, items)
                        templateName = ""; templateItems = ""; reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The template could not be saved. Try again.")
                    }
                }
            },
        ) { Text("Save template") }
        Spacer(Modifier.height(10.dp))
        Text("Runs on this ticket", style = MaterialTheme.typography.titleSmall)
        if (runs.isEmpty()) Text("No runs on this ticket yet.", color = SentinelMuted)
        runs.forEach { run ->
            val rid = Phase3Logic.int(run, "id")
            ItemRow(
                Phase3Logic.str(run, "templateName").ifBlank { "Checklist run" },
                Phase3Logic.str(run, "status"),
            ) {
                if (rid != null) scope.launch {
                    try {
                        openRun = viewModel.api.checklistRun(rid)
                    } catch (e: Exception) {
                        error = plainError(e, "The run could not be opened. Try again.")
                    }
                }
            }
        }
        openRun?.let { run ->
            val rid = Phase3Logic.int(run, "id")
            Spacer(Modifier.height(8.dp))
            Text(
                "Run · ${Phase3Logic.str(run, "templateName")} · ${Phase3Logic.str(run, "status")}",
                style = MaterialTheme.typography.titleSmall,
            )
            Phase3Logic.arr(run, "items").forEachIndexed { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = Phase3Logic.bool(item, "done") == true,
                        onCheckedChange = { done ->
                            if (rid != null) scope.launch {
                                try {
                                    viewModel.api.completeChecklistItem(rid, index, done)
                                    openRun = viewModel.api.checklistRun(rid)
                                } catch (e: Exception) {
                                    error = plainError(e, "The item could not be updated. Try again.")
                                }
                            }
                        },
                    )
                    Text(Phase3Logic.str(item, "label"), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (Phase3Logic.bool(run, "signed") != true && rid != null) {
                TextFieldRow("Sign-off name", signoffName, { signoffName = it })
                Button(
                    enabled = signoffName.isNotBlank(),
                    onClick = {
                        scope.launch {
                            try {
                                viewModel.api.signoffChecklistRun(rid, signoffName.trim())
                                openRun = viewModel.api.checklistRun(rid)
                                reload()
                            } catch (e: Exception) {
                                error = plainError(e, "The sign-off could not be recorded. Try again.")
                            }
                        }
                    },
                ) { Text("Sign off run") }
            } else if (rid != null) {
                Text(
                    "Signed off by ${Phase3Logic.str(run, "signoffName")}",
                    color = SentinelMuted,
                )
            }
        }
        ActionErrorText(error)
    }
}

@Composable
private fun PlansCard(viewModel: AppViewModel, ticket: Ticket) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var cadence by remember { mutableStateOf("monthly") }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AsyncCard("Recurring service plans", "plans", loader = { viewModel.api.plans() }) { data, reload ->
        val cadences = Phase3Logic.arrRaw(data, "cadences")
            .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
        if (cadences.isNotEmpty() && cadence !in cadences) cadence = cadences.first()
        val mine = Phase3Logic.arr(data, "plans")
            .filter { Phase3Logic.int(it, "clientId") == ticket.clientId }
        if (mine.isEmpty()) Text("No plans for this client.", color = SentinelMuted)
        mine.forEach { plan ->
            val pid = Phase3Logic.int(plan, "id")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${Phase3Logic.str(plan, "name")} · ${Phase3Logic.str(plan, "cadence")} · next ${Phase3Logic.str(plan, "nextDueDate")}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (pid != null) {
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                viewModel.api.deletePlan(pid); reload()
                            } catch (e: Exception) {
                                error = plainError(e, "The plan could not be deleted. Try again.")
                            }
                        }
                    }) { Text("Delete") }
                }
            }
        }
        if (ticket.clientId != null) {
            TextFieldRow("Plan name", name, { name = it })
            if (cadences.isNotEmpty()) {
                ChoiceMenu("Cadence", cadence, cadences) { cadence = it }
            }
            TextFieldRow("Amount ($)", amount, { amount = it })
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    val cid = ticket.clientId ?: return@Button
                    scope.launch {
                        try {
                            viewModel.api.savePlan(
                                buildJsonObject {
                                    put("clientId", cid); put("name", name)
                                    put("cadence", cadence)
                                    amount.toDoubleOrNull()?.let { put("amountCents", (it * 100).toInt()) }
                                },
                                null,
                            )
                            name = ""; amount = ""; reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The plan could not be saved. Try again.")
                        }
                    }
                },
            ) { Text("Save plan") }
        }
        ActionErrorText(error)
    }
}

@Composable
private fun PartsCard(viewModel: AppViewModel, ticket: Ticket) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AsyncCard("Parts", "parts", loader = { viewModel.api.parts() }) { data, reload ->
        val parts = Phase3Logic.arr(data, "parts")
        if (parts.isEmpty()) Text("No parts in inventory.", color = SentinelMuted)
        parts.forEach { part ->
            val pid = Phase3Logic.int(part, "id")
            Text(
                "${Phase3Logic.str(part, "name")} · ${Phase3Logic.str(part, "sku")} · on hand ${Phase3Logic.str(part, "qtyOnHand")}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    if (pid != null) scope.launch {
                        try {
                            viewModel.api.consumePart(pid, ticket.id, 1); reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The part could not be consumed. Try again.")
                        }
                    }
                }) { Text("Consume 1 onto this ticket") }
                TextButton(onClick = {
                    if (pid != null) scope.launch {
                        try {
                            viewModel.api.restockPart(pid, 1); reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The part could not be restocked. Try again.")
                        }
                    }
                }) { Text("Restock 1") }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Add part", style = MaterialTheme.typography.titleSmall)
        TextFieldRow("Name", name, { name = it })
        TextFieldRow("SKU", sku, { sku = it })
        TextFieldRow("Qty on hand", qty, { qty = it })
        TextFieldRow("Unit cost ($)", cost, { cost = it })
        Button(
            enabled = name.isNotBlank(),
            onClick = {
                scope.launch {
                    try {
                        viewModel.api.savePart(
                            buildJsonObject {
                                put("name", name); put("sku", sku)
                                put("qtyOnHand", qty.toIntOrNull() ?: 0)
                                cost.toDoubleOrNull()?.let { put("unitCostCents", (it * 100).toInt()) }
                                put("lowStockThreshold", 1)
                            },
                            null,
                        )
                        name = ""; sku = ""; qty = ""; cost = ""; reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The part could not be saved. Try again.")
                    }
                }
            },
        ) { Text("Add part") }
        ActionErrorText(error)
    }
}

@Composable
private fun MergeSplitCard(viewModel: AppViewModel, nav: NavController, ticket: Ticket) {
    val scope = rememberCoroutineScope()
    var others by remember { mutableStateOf<List<Ticket>>(emptyList()) }
    var mergeTarget by remember { mutableStateOf<Ticket?>(null) }
    var confirmMerge by remember { mutableStateOf(false) }
    var splitTitle by remember { mutableStateOf("") }
    var activity by remember { mutableStateOf<List<net.sentinelsafeguard.app.data.api.ActivityItem>>(emptyList()) }
    var pivotId by remember { mutableStateOf<Int?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    androidx.compose.runtime.LaunchedEffect(ticket.id) {
        others = try {
            viewModel.api.ticketsFiltered(clientId = ticket.clientId).items
                .filter { it.id != ticket.id }
        } catch (e: Exception) { emptyList() }
        activity = try {
            viewModel.api.ticketDetail(ticket.id).activity
        } catch (e: Exception) { emptyList() }
    }
    MirrorCard("Merge / split") {
        Text("Merge selected INTO this ticket", style = MaterialTheme.typography.titleSmall)
        if (others.isEmpty()) {
            Text("No other tickets to merge in.", color = SentinelMuted)
        } else {
            val chosen = mergeTarget ?: others.first()
            ChoiceMenu(
                "Duplicate ticket",
                "#${chosen.id} ${chosen.title}",
                others.map { "#${it.id} ${it.title}" },
            ) { picked ->
                mergeTarget = others.firstOrNull { "#${it.id} ${it.title}" == picked }
            }
            Button(onClick = { confirmMerge = true }) { Text("Merge selected INTO this ticket") }
        }
        Spacer(Modifier.height(12.dp))
        Text("Split this ticket", style = MaterialTheme.typography.titleSmall)
        Text(
            "Everything from the chosen timeline entry onward moves to the new ticket.",
            color = SentinelMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        TextFieldRow("New ticket title", splitTitle, { splitTitle = it })
        if (activity.isNotEmpty()) {
            val chosenRow = activity.firstOrNull { it.id == pivotId } ?: activity.first()
            ChoiceMenu(
                "Pivot timeline entry",
                "${chosenRow.action} — ${chosenRow.detail ?: ""}",
                activity.map { "${it.action} — ${it.detail ?: ""}" },
            ) { picked ->
                pivotId = activity.firstOrNull { "${it.action} — ${it.detail ?: ""}" == picked }?.id
            }
        }
        Button(
            enabled = splitTitle.isNotBlank() && (pivotId != null || activity.isNotEmpty()),
            onClick = {
                val pivot = pivotId ?: activity.firstOrNull()?.id ?: return@Button
                scope.launch {
                    try {
                        val out = viewModel.api.splitTicket(ticket.id, splitTitle.trim(), pivot)
                        notice = Phase3Logic.str(out, "message")
                        splitTitle = ""
                    } catch (e: Exception) {
                        error = plainError(e, "The ticket could not be split. Try again.")
                    }
                }
            },
        ) { Text("Split ticket") }
        if (notice != null) Text(notice!!, color = SentinelMuted)
        ActionErrorText(error)
        if (confirmMerge) {
            val duplicate = mergeTarget ?: others.firstOrNull()
            ConfirmGate(
                "Merge tickets",
                "Merging is permanent and cannot be undone. The duplicate ticket's " +
                    "history moves into this ticket and the duplicate is closed out.",
                "Merge",
                onDismiss = { confirmMerge = false },
                onConfirm = {
                    confirmMerge = false
                    if (duplicate != null) scope.launch {
                        try {
                            val out = viewModel.api.mergeTickets(ticket.id, duplicate.id, confirm = true)
                            notice = Phase3Logic.str(out, "message")
                            nav.popBackStack()
                        } catch (e: Exception) {
                            error = plainError(e, "The tickets could not be merged. Try again.")
                        }
                    }
                },
            )
        }
    }
}

/**
 * Exports (G-C9): web's JSON-returning exports download/share as
 * JSON files; the print views ride as their data payloads
 * (labels data; the packet's stored document). This card lives
 * in Documents & Assurance, the mirrored view whose packets and
 * documents these exports describe; the register's own export
 * lives on the Verified screen.
 */
@Composable
fun ExportsCard(viewModel: AppViewModel) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var batchId by remember { mutableStateOf("") }
    var labels by remember { mutableStateOf<JsonObject?>(null) }
    var packet by remember { mutableStateOf<JsonObject?>(null) }
    MirrorCard("Exports") {
        fun runExport(name: String, loader: suspend () -> JsonObject) {
            scope.launch {
                try {
                    val out = loader()
                    shareText(context, name, out.toString())
                    notice = "$name ready to share."
                } catch (e: Exception) {
                    error = plainError(e, "The export could not be created. Try again.")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { runExport("Full export") { viewModel.api.exportFull() } }) {
                Text("Full export")
            }
            OutlinedButton(onClick = { runExport("Workspace export") { viewModel.api.exportWorkspace() } }) {
                Text("Workspace export")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { runExport("Price book export") { viewModel.api.exportPriceBook() } }) {
                Text("Price book export")
            }
            OutlinedButton(onClick = {
                scope.launch {
                    try {
                        labels = viewModel.api.exportLabels()
                    } catch (e: Exception) {
                        error = plainError(e, "Labels could not be loaded. Try again.")
                    }
                }
            }) { Text("Asset labels data") }
        }
        labels?.let { data ->
            val rows = Phase3Logic.arr(data, "labels")
            if (rows.isEmpty()) Text("No labels.", color = SentinelMuted)
            rows.forEach { label ->
                Text(
                    listOfNotNull(
                        Phase3Logic.str(label, "name").takeIf { it.isNotBlank() },
                        Phase3Logic.str(label, "identifier").takeIf { it.isNotBlank() },
                        Phase3Logic.str(label, "qrCode").takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        TextFieldRow("Packet batch id", batchId, { batchId = it })
        Button(
            enabled = batchId.toIntOrNull() != null,
            onClick = {
                scope.launch {
                    try {
                        packet = viewModel.api.exportPacket(batchId.toInt())
                    } catch (e: Exception) {
                        error = plainError(e, "That packet could not be found. Check the batch id and try again.")
                    }
                }
            },
        ) { Text("View packet document") }
        packet?.let { doc ->
            val html = Phase3Logic.str(doc, "documentHtml")
                .ifBlank { Phase3Logic.str(doc, "document_html") }
                .ifBlank { doc.toString() }
            Spacer(Modifier.height(6.dp))
            Text(html.take(2000), color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { shareText(context, "Packet document", html) }) {
                Text("Share packet document")
            }
        }
        if (notice != null) Text(notice!!, color = SentinelMuted)
        ActionErrorText(error)
    }
}
