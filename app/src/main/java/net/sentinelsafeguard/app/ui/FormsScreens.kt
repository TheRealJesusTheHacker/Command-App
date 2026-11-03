package net.sentinelsafeguard.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.createForm
import net.sentinelsafeguard.app.data.api.deleteForm
import net.sentinelsafeguard.app.data.api.deleteFormFile
import net.sentinelsafeguard.app.data.api.downloadFormFile
import net.sentinelsafeguard.app.data.api.editForm
import net.sentinelsafeguard.app.data.api.formDetail
import net.sentinelsafeguard.app.data.api.formFiles
import net.sentinelsafeguard.app.data.api.forms
import net.sentinelsafeguard.app.data.api.updateFormFile
import net.sentinelsafeguard.app.data.api.uploadFormFile
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Forms (Phase 5): web's two sections — Form records and Form
 * documents (the uploaded-PDF vault). Upload/download ride the
 * backend's binary twins of POST /forms/upload and
 * GET /forms/download with web's rules (PDF only, 25 MB cap)
 * checked plainly before sending and enforced again server-side
 * in the server's own words. Form-file update/delete are the
 * web's OWNER-gated handlers — refusals surface verbatim.
 */

@Composable
fun FormsScreen(viewModel: AppViewModel, nav: NavController) {
    Column {
        MirrorHeader("Forms") {
            Button(onClick = { nav.navigate("formNew") }) { Text("+ New form record") }
        }
        FormRecordsSection(viewModel, nav)
    }
}

@Composable
private fun FormRecordsSection(viewModel: AppViewModel, nav: NavController) {
    var query by remember { mutableStateOf("") }
    var reloadKey by remember { mutableStateOf(0) }
    var data by remember { mutableStateOf<JsonObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(query, reloadKey) {
        try {
            data = viewModel.api.forms(q = query)
            error = null
        } catch (e: Exception) {
            error = plainError(e, "Couldn't load form records. Try again.")
        }
    }
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search form records") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            data == null -> SkeletonList()
            else -> {
                val rows = Phase3Logic.arr(data, "forms")
                LazyColumn {
                    item { SectionHeader("Form records", Phase3Logic.int(data, "total")) }
                    if (rows.isEmpty()) {
                        item { EmptyBox("No form records", "Form records linked to clients appear here.") }
                    }
                    items(rows) { form ->
                        val fid = Phase3Logic.int(form, "id")
                        ItemRow(
                            title = Phase3Logic.str(form, "title"),
                            subtitle = listOfNotNull(
                                Phase3Logic.strOrNull(form, "type"),
                                Phase3Logic.strOrNull(form, "clientName"),
                                Phase3Logic.displayValue("createdAt", form["createdAt"]),
                            ).joinToString(" · "),
                            onClick = { fid?.let { nav.navigate("formEdit/$it") } },
                        )
                    }
                    item { FormDocumentsSection(viewModel) { reloadKey++ } }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun FormDocumentsSection(viewModel: AppViewModel, onAnyChange: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var uploadName by remember { mutableStateOf("") }
    var uploadNote by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val state = rememberJsonScreen { viewModel.api.formFiles(q = query) }
    LaunchedEffect(query) { state.reloadKey++ }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                        ?: throw IllegalStateException("unreadable")
                    val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "form.pdf"
                    val precheck = Phase5Logic.uploadPrecheck(fileName, bytes)
                    if (precheck != null) {
                        error = precheck
                        return@launch
                    }
                    val out = viewModel.api.uploadFormFile(
                        bytes, fileName,
                        uploadName.ifBlank { fileName },
                        uploadNote,
                    )
                    notice = Phase3Logic.str(out, "message")
                    error = null
                    uploadName = ""
                    uploadNote = ""
                    state.reloadKey++
                } catch (e: Exception) {
                    error = plainError(e, "The document could not be uploaded. Try again.")
                }
            }
        }
    }
    Column {
        SectionHeader("Form documents", state.data?.let { Phase3Logic.int(it, "total") })
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search form documents…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Column(Modifier.padding(horizontal = 16.dp)) {
            TextFieldRow("Document name", uploadName, onChange = { uploadName = it })
            TextFieldRow("Note", uploadNote, onChange = { uploadNote = it })
            Text(
                "PDF files only, up to 25 MB — the same rule as web Command.",
                color = SentinelMuted,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(6.dp))
            Button(onClick = { picker.launch("application/pdf") }) { Text("Upload PDF") }
            if (notice != null) {
                Spacer(Modifier.height(6.dp))
                Text(notice!!)
            }
            ActionErrorText(error)
        }
        when {
            state.error != null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList(3)
            else -> {
                val files = Phase3Logic.arr(state.data, "files")
                if (files.isEmpty()) {
                    EmptyBox("No form documents", "Uploaded PDF form documents appear here.")
                }
                files.forEach { file ->
                    FormFileRow(viewModel, file) { state.reloadKey++ }
                }
            }
        }
    }
}

@Composable
private fun FormFileRow(viewModel: AppViewModel, file: JsonObject, onChanged: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fileId = Phase3Logic.int(file, "id") ?: return
    var editing by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf(Phase3Logic.str(file, "displayName")) }
    var note by remember { mutableStateOf(Phase3Logic.str(file, "note")) }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val run: (suspend () -> Unit) -> Unit = { block ->
        scope.launch {
            try {
                block()
                onChanged()
            } catch (e: Exception) {
                error = plainError(e, "That could not be done. Try again.")
            }
        }
    }
    MirrorCard(Phase3Logic.str(file, "displayName")) {
        JsonLines(
            file,
            "originalFilename" to "Original filename",
            "uploadedByName" to "Uploaded by",
            "note" to "Note",
        )
        DetailLine(
            "Size",
            Phase3Logic.long(file, "sizeBytes")?.let { Phase5Logic.formatBytes(it) },
        )
        DetailLine("Uploaded", Phase3Logic.displayValue("createdAt", file["createdAt"]))
        ActionErrorText(error)
        Spacer(Modifier.height(6.dp))
        Row {
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val bytes = viewModel.api.downloadFormFile(fileId)
                            shareBytes(
                                context,
                                Phase3Logic.str(file, "originalFilename").ifBlank { "form.pdf" },
                                "application/pdf",
                                bytes,
                            )
                        } catch (e: Exception) {
                            error = plainError(e, "The document could not be downloaded. Try again.")
                        }
                    }
                },
                modifier = Modifier.padding(end = 8.dp),
            ) { Text("Download") }
            OutlinedButton(
                onClick = { editing = !editing },
                modifier = Modifier.padding(end = 8.dp),
            ) { Text("Edit") }
            OutlinedButton(onClick = { confirmDelete = true }) { Text("Delete") }
        }
        if (editing) {
            Spacer(Modifier.height(6.dp))
            TextFieldRow("Display name", displayName, onChange = { displayName = it })
            TextFieldRow("Note", note, onChange = { note = it })
            Button(onClick = {
                run {
                    viewModel.api.updateFormFile(
                        fileId,
                        buildJsonObject {
                            put("displayName", displayName)
                            put("note", note)
                        },
                    )
                    editing = false
                }
            }) { Text("Save") }
        }
        Text(
            "Update and delete are OWNER-gated, as in web Command.",
            color = SentinelMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (confirmDelete) {
        ConfirmGate(
            title = "Delete form document",
            body = "This permanently deletes this form document. This cannot be undone.",
            confirmLabel = "Delete",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                run { viewModel.api.deleteFormFile(fileId) }
            },
        )
    }
}

@Composable
fun FormRecordFormScreen(viewModel: AppViewModel, nav: NavController, formId: Int?) {
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(formId == null) }
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var clientIdText by remember { mutableStateOf("") }
    var dataJson by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(formId) {
        if (formId != null) {
            try {
                val data = viewModel.api.formDetail(formId)
                val form = Phase3Logic.obj(data, "form") ?: data
                title = Phase3Logic.str(form, "title")
                type = Phase3Logic.str(form, "type")
                clientIdText = Phase3Logic.int(form, "clientId")?.toString() ?: ""
                dataJson = Phase3Logic.str(form, "dataJson")
                clientName = Phase3Logic.str(form, "clientName")
            } catch (e: Exception) {
                error = plainError(e, "Couldn't load this form record. Try again.")
            } finally {
                loaded = true
            }
        }
    }
    LazyColumn {
        item { MirrorHeader(if (formId == null) "New form record" else "Edit form record") }
        if (!loaded) {
            item { SkeletonList(3) }
        } else {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    if (clientName.isNotBlank()) {
                        DetailLine("Client", clientName)
                    }
                    TextFieldRow("Title", title, onChange = { title = it })
                    TextFieldRow("Type", type, onChange = { type = it })
                    TextFieldRow("Client ID (optional)", clientIdText, onChange = { clientIdText = it })
                    TextFieldRow("Data (JSON)", dataJson, onChange = { dataJson = it }, singleLine = false, minLines = 4)
                    ActionErrorText(error)
                    Spacer(Modifier.height(10.dp))
                    Row {
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        val body = buildJsonObject {
                                            put("title", title)
                                            put("type", type)
                                            clientIdText.toIntOrNull()?.let { put("clientId", it) }
                                            put("dataJson", dataJson)
                                        }
                                        if (formId == null) viewModel.api.createForm(body)
                                        else viewModel.api.editForm(formId, body)
                                        nav.popBackStack()
                                    } catch (e: Exception) {
                                        error = plainError(e, "That could not be saved. Try again.")
                                    }
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp),
                        ) { Text("Save") }
                        if (formId != null) {
                            OutlinedButton(onClick = { confirmDelete = true }) { Text("Delete") }
                        }
                    }
                    if (formId != null) {
                        Text(
                            "Delete is OWNER-only, as in web Command.",
                            color = SentinelMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
    if (confirmDelete && formId != null) {
        ConfirmGate(
            title = "Delete form record",
            body = "This permanently deletes this form record. This cannot be undone.",
            confirmLabel = "Delete",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                scope.launch {
                    try {
                        viewModel.api.deleteForm(formId)
                        nav.popBackStack()
                    } catch (e: Exception) {
                        error = plainError(e, "That could not be deleted. Try again.")
                    }
                }
            },
        )
    }
}
