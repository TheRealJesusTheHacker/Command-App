package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.sentinelsafeguard.app.data.api.RbExtractionResponse
import net.sentinelsafeguard.app.data.api.RbProcedure
import net.sentinelsafeguard.app.data.api.RbRun
import net.sentinelsafeguard.app.data.api.RbStep
import net.sentinelsafeguard.app.data.api.SignOffResponse
import net.sentinelsafeguard.app.data.api.Ticket
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Runbooks & Sign-off (b4fdesk, mirror spec §6 Phase 2): web's
 * desk — knowledge-base procedures (procedure flag + step edits
 * via b4fsetprocedure), recent runbook runs with the full run
 * lifecycle (start / check-step / pause / resume / cancel), the
 * attachment text extraction status + backfill pass, and the
 * ticket sign-off surface, which also renders inside the ticket
 * profile where web puts it.
 */

private fun jsonLine(obj: JsonObject): String =
    obj.entries.joinToString(" · ") { (k, v) ->
        val text = (v as? JsonPrimitive)?.content ?: v.toString()
        "$k: $text"
    }

@Composable
fun RunbooksScreen(viewModel: AppViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    var procedures by remember { mutableStateOf<List<RbProcedure>?>(null) }
    var runs by remember { mutableStateOf<List<RbRun>?>(null) }
    var extraction by remember { mutableStateOf<RbExtractionResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var openRunId by remember { mutableStateOf<Int?>(null) }
    var editing by remember { mutableStateOf<RbProcedure?>(null) }
    var startFor by remember { mutableStateOf<RbProcedure?>(null) }
    var backfillNote by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reloadKey) {
        error = null
        try {
            procedures = viewModel.api.rbProcedures()
            runs = viewModel.api.rbRuns()
            extraction = try {
                viewModel.api.rbExtraction()
            } catch (e: Exception) {
                null
            }
        } catch (e: Exception) {
            error = plainError(e, "Runbooks could not be loaded. Check your connection and try again.")
        }
    }

    if (editing != null) {
        ProcedureStepsEditor(
            viewModel = viewModel,
            procedure = editing!!,
            onDone = { editing = null; reloadKey++ },
            onCancel = { editing = null },
        )
        return
    }
    if (openRunId != null) {
        RunDetailScreen(
            viewModel = viewModel,
            runId = openRunId!!,
            onBack = { openRunId = null; reloadKey++ },
        )
        return
    }

    Column {
        MirrorHeader("Runbooks & Sign-off")
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            procedures == null || runs == null -> SkeletonList(6)
            else -> LazyColumn {
                if (actionError != null) {
                    item { Text(actionError!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
                }
                item {
                    MirrorCard("Procedures", procedures!!.size) {
                        if (procedures!!.isEmpty()) {
                            Text("No procedures yet.", color = SentinelMuted)
                        }
                        procedures!!.forEach { proc ->
                            Column(Modifier.padding(vertical = 4.dp)) {
                                Text(proc.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    listOfNotNull(
                                        proc.category.takeIf { it.isNotBlank() },
                                        "${proc.stepCount} step(s)",
                                        if (proc.isProcedure) "Procedure" else "Not a procedure",
                                        if (proc.clientVisible) "Client visible" else null,
                                    ).joinToString(" · "),
                                    color = SentinelMuted,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { startFor = proc }) { Text("Start run") }
                                    TextButton(onClick = { editing = proc }) { Text("Edit steps") }
                                    TextButton(onClick = {
                                        scope.launch {
                                            try {
                                                viewModel.api.rbSetProcedure(
                                                    proc.id, !proc.isProcedure, proc.steps,
                                                )
                                                reloadKey++
                                            } catch (e: Exception) {
                                                actionError = plainError(e, "That change could not be saved. Try again.")
                                            }
                                        }
                                    }) {
                                        Text(if (proc.isProcedure) "Unmark procedure" else "Mark as procedure")
                                    }
                                }
                            }
                        }
                        val procForStart = startFor
                        if (procForStart != null) {
                            StartRunPicker(
                                viewModel = viewModel,
                                procedure = procForStart,
                                onStarted = { run ->
                                    startFor = null
                                    if (run != null) openRunId = run.id else reloadKey++
                                },
                                onCancel = { startFor = null },
                                onError = { msg -> actionError = msg },
                            )
                        }
                    }
                }
                item {
                    MirrorCard("Recent runbook runs", runs!!.size) {
                        if (runs!!.isEmpty()) {
                            Text("No runbook runs yet.", color = SentinelMuted)
                        }
                        runs!!.forEach { run ->
                            ItemRow(
                                run.title,
                                listOfNotNull(
                                    run.status.takeIf { it.isNotBlank() },
                                    "${run.doneCount}/${run.totalSteps} step(s)",
                                    run.startedByName.takeIf { it.isNotBlank() },
                                    if (run.startedAt > 0) formatEpochMs(run.startedAt) else null,
                                    run.ticketId?.let { "Ticket #$it" },
                                ).joinToString(" · "),
                            ) { openRunId = run.id }
                        }
                    }
                }
                item {
                    MirrorCard("Attachment text extraction") {
                        val ex = extraction
                        if (ex == null) {
                            Text("Extraction status could not be loaded.", color = SentinelMuted)
                        } else {
                            if (ex.summary.isEmpty()) {
                                Text("No extraction activity yet.", color = SentinelMuted)
                            }
                            ex.summary.forEach { row ->
                                Text(jsonLine(row), style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                "${ex.attachments.size} attachment(s)",
                                color = SentinelMuted,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            scope.launch {
                                try {
                                    viewModel.api.rbBackfill()
                                    backfillNote = "Backfill pass run."
                                    reloadKey++
                                } catch (e: Exception) {
                                    actionError = plainError(e, "The backfill pass could not be run. Try again.")
                                }
                            }
                        }) { Text("Run backfill pass") }
                        if (backfillNote != null) {
                            Text(backfillNote!!, color = SentinelMuted)
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun StartRunPicker(
    viewModel: AppViewModel,
    procedure: RbProcedure,
    onStarted: (RbRun?) -> Unit,
    onCancel: () -> Unit,
    onError: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var tickets by remember { mutableStateOf<List<Ticket>?>(null) }
    var ticketId by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(procedure.id) {
        tickets = try {
            viewModel.api.ticketsFiltered().items
        } catch (e: Exception) {
            emptyList()
        }
    }

    Column(Modifier.padding(top = 8.dp)) {
        Text(
            "Start “${procedure.title}” on a ticket",
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(6.dp))
        val list = tickets
        if (list == null) {
            Text("Loading tickets…", color = SentinelMuted)
        } else if (list.isEmpty()) {
            Text("No tickets to run this on.", color = SentinelMuted)
        } else {
            val options = list.map { "#${it.id} · ${it.title}" }
            ChoiceMenu(
                "Ticket",
                options[
                    list.indexOfFirst { it.id == ticketId }.let { if (it < 0) 0 else it }
                ],
                options,
            ) { picked ->
                ticketId = list.firstOrNull { "#${it.id} · ${it.title}" == picked }?.id
            }
            if (ticketId == null && list.isNotEmpty()) ticketId = list.first().id
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !busy && ticketId != null,
                onClick = {
                    busy = true
                    scope.launch {
                        try {
                            onStarted(viewModel.api.rbStartRun(procedure.id, ticketId))
                        } catch (e: Exception) {
                            busy = false
                            onError(plainError(e, "The run could not be started. Try again."))
                        }
                    }
                },
            ) { Text(if (busy) "Starting…" else "Start run") }
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

@Composable
private fun ProcedureStepsEditor(
    viewModel: AppViewModel,
    procedure: RbProcedure,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var steps by remember {
        mutableStateOf(
            procedure.steps.ifEmpty { listOf(RbStep("", "")) }
                .map { it.title to it.detail }
        )
    }
    var isProcedure by remember { mutableStateOf(procedure.isProcedure) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Text(procedure.title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text("Procedure steps", color = SentinelMuted)
        Spacer(Modifier.height(12.dp))
        steps.forEachIndexed { index, (stepTitle, stepDetail) ->
            Text("Step ${index + 1}", style = MaterialTheme.typography.labelLarge, color = SentinelMuted)
            OutlinedTextField(
                value = stepTitle,
                onValueChange = { value ->
                    steps = steps.toMutableList().also { it[index] = value to stepDetail }
                },
                label = { Text("Step title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = stepDetail,
                onValueChange = { value ->
                    steps = steps.toMutableList().also { it[index] = stepTitle to value }
                },
                label = { Text("Detail") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row {
                TextButton(
                    enabled = steps.size > 1,
                    onClick = { steps = steps.toMutableList().also { it.removeAt(index) } },
                ) { Text("Remove step") }
            }
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(onClick = { steps = steps + ("" to "") }) { Text("+ Add step") }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            androidx.compose.material3.Checkbox(
                checked = isProcedure,
                onCheckedChange = { isProcedure = it },
            )
            Text("This knowledge entry is a procedure")
        }
        if (saveError != null) {
            Text(saveError!!, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !busy,
                onClick = {
                    busy = true
                    scope.launch {
                        try {
                            viewModel.api.rbSetProcedure(
                                procedure.id,
                                isProcedure,
                                steps.filter { it.first.isNotBlank() }
                                    .map { RbStep(it.first.trim(), it.second) },
                            )
                            onDone()
                        } catch (e: Exception) {
                            busy = false
                            saveError = plainError(e, "The procedure could not be saved. Try again.")
                        }
                    }
                },
            ) { Text(if (busy) "Saving…" else "Save procedure") }
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun RunDetailScreen(viewModel: AppViewModel, runId: Int, onBack: () -> Unit) {
    var run by remember { mutableStateOf<RbRun?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(runId, reloadKey) {
        error = null
        try {
            run = viewModel.api.rbRun(runId)
            if (run == null) {
                run = viewModel.api.rbRuns().firstOrNull { it.id == runId }
            }
        } catch (e: Exception) {
            error = plainError(e, "This run could not be loaded. Go back and try again.")
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        TextButton(onClick = onBack) { Text("← Back to Runbooks & Sign-off") }
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            run == null -> SkeletonList(4)
            else -> RunDetailContent(
                viewModel = viewModel,
                run = run!!,
                onChanged = { reloadKey++ },
            )
        }
    }
}

@Composable
fun RunDetailContent(viewModel: AppViewModel, run: RbRun, onChanged: () -> Unit) {
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf("") }
    var actionError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun act(block: suspend () -> Unit) {
        busy = true
        scope.launch {
            try {
                block()
                onChanged()
            } catch (e: Exception) {
                actionError = plainError(e, "That run action could not be saved. Try again.")
            } finally {
                busy = false
            }
        }
    }

    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(run.title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MirrorChip(run.status)
            MirrorChip("${run.doneCount}/${run.totalSteps} step(s)")
            if (run.ticketId != null) MirrorChip("Ticket #${run.ticketId}")
        }
        Text(
            listOfNotNull(
                run.startedByName.takeIf { it.isNotBlank() }?.let { "Started by $it" },
                if (run.startedAt > 0) formatEpochMs(run.startedAt) else null,
                run.finishedAt?.let { "Finished ${formatEpochMs(it)}" },
            ).joinToString(" · "),
            color = SentinelMuted,
        )
        if (actionError != null) {
            Spacer(Modifier.height(8.dp))
            Text(actionError!!, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (run.status == "active") {
                OutlinedButton(
                    enabled = !busy,
                    onClick = { act { viewModel.api.rbRunState(run.id, "pause") } },
                ) { Text("Pause") }
            }
            if (run.status == "paused") {
                OutlinedButton(
                    enabled = !busy,
                    onClick = { act { viewModel.api.rbRunState(run.id, "resume") } },
                ) { Text("Resume") }
            }
            if (run.status == "active" || run.status == "paused") {
                OutlinedButton(
                    enabled = !busy,
                    onClick = { act { viewModel.api.rbRunState(run.id, "cancel") } },
                ) { Text("Cancel run") }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Step note") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        run.steps.forEachIndexed { index, step ->
            val state = run.stepStates.getOrNull(index)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${index + 1}. ${step.title}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (step.detail.isNotBlank()) {
                        Text(step.detail, color = SentinelMuted)
                    }
                    Text(
                        listOfNotNull(
                            state?.state ?: "pending",
                            state?.byName?.takeIf { it.isNotBlank() },
                            state?.at?.let { formatEpochMs(it) },
                            state?.note?.takeIf { it.isNotBlank() },
                        ).joinToString(" · "),
                        color = SentinelMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (run.status == "active" || run.status == "paused") {
                    if (state?.state == "done") {
                        TextButton(
                            enabled = !busy,
                            onClick = {
                                act { viewModel.api.rbCheckStep(run.id, index, note, undo = true) }
                            },
                        ) { Text("Undo") }
                    } else {
                        Button(
                            enabled = !busy,
                            onClick = {
                                act { viewModel.api.rbCheckStep(run.id, index, note) }
                            },
                        ) { Text("Check step") }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** The runbook surface inside the ticket profile (web puts the
 *  start/check-step controls and the sign-off there). */
@Composable
fun TicketRunbookPanel(viewModel: AppViewModel, ticketId: Int) {
    val scope = rememberCoroutineScope()
    var runs by remember { mutableStateOf<List<RbRun>?>(null) }
    var procedures by remember { mutableStateOf<List<RbProcedure>>(emptyList()) }
    var openRunId by remember { mutableStateOf<Int?>(null) }
    var pendingStart by remember { mutableStateOf<RbProcedure?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var panelError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(ticketId, reloadKey) {
        try {
            runs = viewModel.api.rbRuns(ticketId)
            procedures = try {
                viewModel.api.rbProcedures().filter { it.isProcedure }
            } catch (e: Exception) {
                emptyList()
            }
        } catch (e: Exception) {
            panelError = plainError(e, "The runbook runs could not be loaded. Try again.")
        }
    }

    MirrorCard("Runbook runs", runs?.size) {
        when {
            panelError != null -> Text(panelError!!, color = MaterialTheme.colorScheme.error)
            runs == null -> Text("Loading…", color = SentinelMuted)
            else -> {
                val open = runs!!.firstOrNull { it.id == openRunId }
                if (open != null) {
                    RunDetailContent(viewModel, open) { reloadKey++ }
                    TextButton(onClick = { openRunId = null }) { Text("Close run") }
                } else {
                    if (runs!!.isEmpty()) {
                        Text("No runbook runs on this ticket yet.", color = SentinelMuted)
                    }
                    runs!!.forEach { run ->
                        ItemRow(
                            run.title,
                            listOfNotNull(
                                run.status.takeIf { it.isNotBlank() },
                                "${run.doneCount}/${run.totalSteps} step(s)",
                            ).joinToString(" \u00b7 "),
                        ) { openRunId = run.id }
                    }
                    if (procedures.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Start a run",
                            style = MaterialTheme.typography.labelLarge,
                            color = SentinelMuted,
                        )
                        procedures.forEach { proc ->
                            TextButton(onClick = { pendingStart = proc }) {
                                Text("Start: ${proc.title}")
                            }
                        }
                        val pending = pendingStart
                        if (pending != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    enabled = !busy,
                                    onClick = {
                                        busy = true
                                        scope.launch {
                                            try {
                                                val run = viewModel.api.rbStartRun(
                                                    pending.id, ticketId,
                                                )
                                                pendingStart = null
                                                reloadKey++
                                                if (run != null) openRunId = run.id
                                            } catch (e: Exception) {
                                                panelError = plainError(
                                                    e, "The run could not be started. Try again.",
                                                )
                                            } finally {
                                                busy = false
                                            }
                                        }
                                    },
                                ) { Text(if (busy) "Starting…" else "Confirm start") }
                                OutlinedButton(onClick = { pendingStart = null }) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The completion sign-off surface inside the ticket profile:
 *  read shows the signed record; write is write-once (the backend
 *  refuses a second sign-off, exactly as web). */
@Composable
fun TicketSignOffCard(viewModel: AppViewModel, ticketId: Int, clientId: Int?) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<SignOffResponse?>(null) }
    var signerName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(ticketId, reloadKey) {
        error = null
        try {
            state = viewModel.api.ticketSignOff(ticketId)
        } catch (e: Exception) {
            error = plainError(e, "The sign-off could not be loaded. Try again.")
        }
    }

    MirrorCard("Completion sign-off") {
        when {
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
            state == null -> Text("Loading…", color = SentinelMuted)
            else -> {
                val s = state!!
                val signed = s.signOff
                if (signed != null) {
                    DetailLine("Signed by", signed.signerName)
                    DetailLine("Status", signed.status)
                    DetailLine(
                        "Signed at",
                        if (signed.signedAt > 0) formatEpochMs(signed.signedAt) else null,
                    )
                    DetailLine("Document hash", signed.documentHash)
                    if (signed.summary.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(signed.summary, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Text(
                        "No sign-off on this ticket yet.",
                        color = SentinelMuted,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = signerName,
                        onValueChange = { signerName = it },
                        label = { Text("Signer name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (actionError != null) {
                        Text(actionError!!, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        enabled = !busy && signerName.isNotBlank(),
                        onClick = {
                            busy = true
                            scope.launch {
                                try {
                                    viewModel.api.postTicketSignOff(
                                        ticketId, signerName.trim(), clientId,
                                    )
                                    reloadKey++
                                } catch (e: Exception) {
                                    actionError = plainError(e, "The sign-off could not be recorded. Try again.")
                                } finally {
                                    busy = false
                                }
                            }
                        },
                    ) { Text(if (busy) "Signing…" else "Sign off job") }
                }
            }
        }
    }
}
