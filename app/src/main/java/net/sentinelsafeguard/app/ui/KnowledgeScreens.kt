package net.sentinelsafeguard.app.ui

import android.provider.OpenableColumns
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.knowledgeAudience
import net.sentinelsafeguard.app.data.api.knowledgeCreate
import net.sentinelsafeguard.app.data.api.knowledgeDelete
import net.sentinelsafeguard.app.data.api.knowledgeDeleteDocument
import net.sentinelsafeguard.app.data.api.knowledgeDetail
import net.sentinelsafeguard.app.data.api.knowledgeDownloadUrl
import net.sentinelsafeguard.app.data.api.knowledgeEdit
import net.sentinelsafeguard.app.data.api.knowledgeFileBytes
import net.sentinelsafeguard.app.data.api.knowledgeList
import net.sentinelsafeguard.app.data.api.knowledgeUpload
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Knowledge (Phase 7) — the mirror of web's Knowledge view:
 * the practice library directory with web's own search (over
 * title/category/description/uploader, server-side), category
 * chips with counts, the four-sort vocabulary, and the kbStats
 * header stats over the complete catalog; the reader with its
 * document served by the app file twin; and the editor with
 * web's fields plus the audience editor (web's two-step
 * saveknowledge → setknowledgeaudience flow, ridden as one
 * combined save exactly as the backend mirrors it).
 *
 * Web gates the write controls by OWNER in its own UI (the
 * server then enforces knowledge:write per role — STAFF's is
 * none, and its refusal surfaces in the server's words if a
 * call is ever made). The mirror keeps web's gate.
 */

/** Resolve a picked file's display name (the upload's
 *  X-File-Name and the client's own .pdf check both need it). */
private fun android.content.ContentResolver.displayName(uri: android.net.Uri): String {
    var name: String? = null
    runCatching {
        query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) name = c.getString(0)
        }
    }
    return name?.takeIf { it.isNotBlank() }
        ?: uri.lastPathSegment?.substringAfterLast('/') ?: "book.pdf"
}

/** The shared PDF picker + upload used by the list and reader:
 *  web's kbCheckFile runs first (its exact messages), then the
 *  bytes ride the binary twin and the server's own verdict —
 *  success message or refusal — is what the user reads. */
@Composable
private fun rememberKbUploader(
    viewModel: AppViewModel,
    onDone: (String) -> Unit,
    onError: (String) -> Unit,
): (Int) -> Unit {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var targetId by remember { mutableStateOf<Int?>(null) }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        val id = targetId
        targetId = null
        if (uri != null && id != null) {
            scope.launch {
                try {
                    val name = context.contentResolver.displayName(uri)
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                        ?: throw IllegalStateException("unreadable")
                    val problem = Phase7Logic.checkFile(name, bytes.size.toLong())
                    if (problem != null) {
                        onError(problem)
                        return@launch
                    }
                    val out = viewModel.api.knowledgeUpload(id, bytes, name)
                    onDone(Phase3Logic.str(out, "message"))
                } catch (e: Exception) {
                    onError(plainError(e, "The PDF could not be uploaded. Try again."))
                }
            }
        }
    }
    return { id -> targetId = id; picker.launch("application/pdf") }
}

@Composable
fun KnowledgeScreen(viewModel: AppViewModel, nav: NavController) {
    val isOwner = viewModel.me?.user?.role == "OWNER"
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var sort by remember { mutableStateOf("title") }
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val state = rememberJsonScreen {
        viewModel.api.knowledgeList(query, category, sort)
    }
    LaunchedEffect(query, category, sort) {
        delay(300)
        state.reloadKey++
    }
    val pickUpload = rememberKbUploader(
        viewModel,
        onDone = { msg -> notice = msg.ifBlank { null }; state.reloadKey++ },
        onError = { error = it },
    )
    var deleteTarget by remember { mutableStateOf<JsonObject?>(null) }

    Column {
        MirrorHeader(
            "Knowledge",
            subtitle = "The practice library — codes, standards, and law, housed on site and readable here.",
        ) {
            if (isOwner) {
                Button(onClick = { nav.navigate("knowledgeNew") }) { Text("+ New catalog entry") }
            }
        }
        when {
            state.error != null && state.data == null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList()
            else -> {
                val data = state.data!!
                val stats = Phase3Logic.obj(data, "stats")
                val categories = Phase3Logic.arr(data, "categories")
                val entries = Phase3Logic.arr(data, "entries")
                val catalogTotal = Phase3Logic.int(stats, "total") ?: 0
                LazyColumn {
                    item {
                        Row(Modifier.padding(horizontal = 12.dp)) {
                            StatCard("Catalog entries", catalogTotal.toString(), Modifier.weight(1f))
                            StatCard(
                                "Ready to read",
                                (Phase3Logic.int(stats, "readyToRead") ?: 0).toString(),
                                Modifier.weight(1f),
                            )
                            StatCard(
                                "Awaiting file",
                                (Phase3Logic.int(stats, "awaitingFile") ?: 0).toString(),
                                Modifier.weight(1f),
                            )
                        }
                        Row(Modifier.padding(horizontal = 12.dp)) {
                            StatCard(
                                "Total size",
                                Phase7Logic.kbSize(Phase3Logic.long(stats, "totalSize") ?: 0),
                                Modifier.weight(1f),
                            )
                            StatCard("Total pages", Phase7Logic.totalPagesLabel(stats), Modifier.weight(1f))
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("Search the library") },
                            placeholder = { Text("Search title, category, description…") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilterChip(
                                selected = category == "All",
                                onClick = { category = "All" },
                                label = { Text("All") },
                            )
                            categories.forEach { cat ->
                                val name = Phase3Logic.str(cat, "category")
                                Spacer(Modifier.padding(horizontal = 3.dp))
                                FilterChip(
                                    selected = category == name,
                                    onClick = { category = name },
                                    label = { Text("$name · ${Phase3Logic.int(cat, "count") ?: 0}") },
                                )
                            }
                        }
                    }
                    item {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ChoiceMenu(
                                "Sort",
                                Phase7Logic.sortLabel(sort),
                                Phase7Logic.SORTS.map { it.second },
                            ) { sort = Phase7Logic.sortKey(it) }
                        }
                    }
                    if (notice != null) {
                        item {
                            Text(
                                notice!!,
                                color = SentinelMuted,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                    if (error != null) {
                        item { ActionErrorText(error) }
                    }
                    if (catalogTotal == 0) {
                        item {
                            EmptyBox(
                                "Knowledge",
                                "The library is empty. Add a catalog entry, then upload its PDF — the book becomes readable here, on site.",
                            )
                        }
                    } else if (entries.isEmpty()) {
                        item {
                            Column(Modifier.padding(horizontal = 16.dp)) {
                                Text("No books match your search.", color = SentinelMuted)
                                OutlinedButton(onClick = { query = ""; category = "All" }) {
                                    Text("Clear search")
                                }
                            }
                        }
                    }
                    items(entries) { entry ->
                        val id = Phase3Logic.int(entry, "id") ?: 0
                        val hasFile = Phase3Logic.bool(entry, "hasFile") == true
                        MirrorCard(Phase3Logic.str(entry, "title")) {
                            Row {
                                MirrorChip(Phase3Logic.str(entry, "category").ifBlank { "Uncategorized" })
                                Spacer(Modifier.padding(horizontal = 3.dp))
                                MirrorChip(Phase7Logic.fileStateChip(entry))
                                Spacer(Modifier.padding(horizontal = 3.dp))
                                MirrorChip(Phase7Logic.audienceChip(entry))
                            }
                            val desc = Phase3Logic.str(entry, "description")
                            if (desc.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(desc, color = SentinelMuted)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(Phase7Logic.entryMeta(entry), color = SentinelMuted)
                            Row {
                                Button(onClick = { nav.navigate("knowledge/$id") }) {
                                    Text(if (hasFile) "Read" else "Open")
                                }
                                if (isOwner) {
                                    TextButton(onClick = { pickUpload(id) }) {
                                        Text(if (hasFile) "Replace PDF" else "Upload PDF")
                                    }
                                    TextButton(onClick = { nav.navigate("knowledgeEdit/$id") }) {
                                        Text("Edit")
                                    }
                                    TextButton(onClick = { deleteTarget = entry }) {
                                        Text("Delete")
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

    deleteTarget?.let { entry ->
        val id = Phase3Logic.int(entry, "id") ?: 0
        val scope = rememberCoroutineScope()
        ConfirmGate(
            title = "Delete this entry?",
            body = Phase3Logic.str(entry, "title") + "\n\n" +
                (if (Phase3Logic.bool(entry, "hasFile") == true) "Its stored PDF is removed with it. " else "") +
                "This cannot be undone.",
            confirmLabel = "Delete",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                scope.launch {
                    try {
                        val out = viewModel.api.knowledgeDelete(id)
                        notice = Phase3Logic.str(out, "message").ifBlank { null }
                        state.reloadKey++
                    } catch (e: Exception) {
                        error = plainError(e, "Could not delete this entry.")
                    }
                }
            },
        )
    }
}

@Composable
fun KnowledgeDetailScreen(viewModel: AppViewModel, nav: NavController, id: Int) {
    val isOwner = viewModel.me?.user?.role == "OWNER"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = rememberJsonScreen { viewModel.api.knowledgeDetail(id) }
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmRemove by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val pickUpload = rememberKbUploader(
        viewModel,
        onDone = { msg -> notice = msg.ifBlank { null }; state.reloadKey++ },
        onError = { error = it },
    )

    Column {
        TextButton(onClick = { nav.popBackStack() }) { Text("← Library") }
        when {
            state.error != null && state.data == null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList()
            else -> {
                val entry = Phase3Logic.obj(state.data, "entry") ?: JsonObject(emptyMap())
                val document = Phase3Logic.obj(state.data, "document")
                val hasFile = Phase3Logic.bool(entry, "hasFile") == true
                val fileUrl = Phase3Logic.strOrNull(document, "url")
                LazyColumn {
                    item { MirrorHeader(Phase3Logic.str(entry, "title")) }
                    item {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            Text(Phase7Logic.entryMeta(entry), color = SentinelMuted)
                            JsonLines(
                                entry,
                                "category" to "Category",
                                "fileName" to "File",
                                "uploadedByName" to "Uploaded by",
                                "createdAt" to "Created",
                                "updatedAt" to "Last updated",
                            )
                            DetailLine("Audience", Phase7Logic.audienceChip(entry))
                            val desc = Phase3Logic.str(entry, "description")
                            if (desc.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text(desc)
                            }
                        }
                    }
                    if (notice != null) {
                        item {
                            Text(
                                notice!!,
                                color = SentinelMuted,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                    item { ActionErrorText(error) }
                    if (hasFile && fileUrl != null) {
                        item {
                            MirrorCard("Document") {
                                val docMsg = Phase3Logic.str(document, "message")
                                if (docMsg.isNotBlank()) Text(docMsg, color = SentinelMuted)
                                Row {
                                    Button(onClick = {
                                        scope.launch {
                                            try {
                                                val bytes = viewModel.api.knowledgeFileBytes(fileUrl)
                                                shareBytes(
                                                    context,
                                                    Phase3Logic.str(entry, "fileName").ifBlank { "book.pdf" },
                                                    "application/pdf",
                                                    bytes,
                                                )
                                            } catch (e: Exception) {
                                                error = plainError(e, "The book could not be opened. Try again.")
                                            }
                                        }
                                    }) { Text("Full screen") }
                                    Spacer(Modifier.padding(horizontal = 4.dp))
                                    OutlinedButton(onClick = {
                                        scope.launch {
                                            try {
                                                val bytes = viewModel.api.knowledgeFileBytes(
                                                    knowledgeDownloadUrl(fileUrl)
                                                )
                                                shareBytes(
                                                    context,
                                                    Phase3Logic.str(entry, "fileName").ifBlank { "book.pdf" },
                                                    "application/pdf",
                                                    bytes,
                                                )
                                            } catch (e: Exception) {
                                                error = plainError(e, "The book could not be downloaded. Try again.")
                                            }
                                        }
                                    }) { Text("Download") }
                                }
                            }
                        }
                    } else {
                        item {
                            MirrorCard("Document") {
                                Text(
                                    "This entry is catalog-only — no PDF is housed yet. Upload the book and it becomes readable here, on site.",
                                    color = SentinelMuted,
                                )
                                if (isOwner) {
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = { pickUpload(id) }) { Text("Upload PDF") }
                                }
                            }
                        }
                    }
                    if (isOwner) {
                        item {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                OutlinedButton(onClick = { nav.navigate("knowledgeEdit/$id") }) {
                                    Text("Edit entry")
                                }
                                if (hasFile) {
                                    TextButton(onClick = { pickUpload(id) }) { Text("Replace PDF") }
                                    TextButton(onClick = { confirmRemove = true }) { Text("Remove PDF") }
                                }
                                TextButton(onClick = { confirmDelete = true }) { Text("Delete entry") }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }

                if (confirmRemove) {
                    ConfirmGate(
                        title = "Remove this PDF?",
                        body = Phase3Logic.str(entry, "title") +
                            "\n\nThe stored PDF is permanently removed. The catalog entry stays, marked Awaiting file. This cannot be undone.",
                        confirmLabel = "Remove PDF",
                        onDismiss = { confirmRemove = false },
                        onConfirm = {
                            confirmRemove = false
                            scope.launch {
                                try {
                                    val out = viewModel.api.knowledgeDeleteDocument(id)
                                    notice = Phase3Logic.str(out, "message").ifBlank { null }
                                    state.reloadKey++
                                } catch (e: Exception) {
                                    error = plainError(e, "Could not remove this PDF.")
                                }
                            }
                        },
                    )
                }
                if (confirmDelete) {
                    ConfirmGate(
                        title = "Delete this entry?",
                        body = Phase3Logic.str(entry, "title") + "\n\n" +
                            (if (hasFile) "Its stored PDF is removed with it. " else "") +
                            "This cannot be undone.",
                        confirmLabel = "Delete",
                        onDismiss = { confirmDelete = false },
                        onConfirm = {
                            confirmDelete = false
                            scope.launch {
                                try {
                                    viewModel.api.knowledgeDelete(id)
                                    nav.popBackStack()
                                } catch (e: Exception) {
                                    error = plainError(e, "Could not delete this entry.")
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun KnowledgeFormScreen(viewModel: AppViewModel, nav: NavController, editId: Int?) {
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(editId == null) }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var audience by remember { mutableStateOf("none") }
    var selectedClients by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var clients by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(editId) {
        try {
            clients = viewModel.api.clients().items.map { c ->
                // The audience checklist works in the open-JSON
                // shape the rest of Phase 7 uses.
                kotlinx.serialization.json.buildJsonObject {
                    put("id", c.id)
                    put("legalName", c.legalName)
                }
            }
        } catch (e: Exception) {
            // The checklist is an aid; ids can still be saved
            // through the dedicated audience endpoint later.
        }
        if (editId != null) {
            try {
                val detail = viewModel.api.knowledgeDetail(editId)
                val entry = Phase3Logic.obj(detail, "entry")
                if (entry != null) {
                    title = Phase3Logic.str(entry, "title")
                    category = Phase3Logic.str(entry, "category")
                    description = Phase3Logic.str(entry, "description")
                    audience = Phase7Logic.entryAudience(entry)
                }
            } catch (e: Exception) {
                error = plainError(e, "That entry could not be loaded. Try again.")
            }
        }
        loaded = true
    }

    Column {
        MirrorHeader(if (editId == null) "New catalog entry" else "Edit catalog entry")
        if (!loaded) {
            SkeletonList()
            return@Column
        }
        LazyColumn {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    TextFieldRow("Title", title, { title = it })
                    TextFieldRow("Category", category, { category = it })
                    Row {
                        Phase7Logic.CATEGORIES.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) },
                            )
                            Spacer(Modifier.padding(horizontal = 2.dp))
                        }
                    }
                    TextFieldRow("Description", description, { description = it }, singleLine = false, minLines = 3)
                    Spacer(Modifier.height(8.dp))
                    ChoiceMenu(
                        "Client-safe publishing (CF #23)",
                        Phase7Logic.audienceLabel(audience),
                        Phase7Logic.AUDIENCES.map { it.second },
                    ) { audience = Phase7Logic.audienceValue(it) }
                }
            }
            if (audience == "specific") {
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(
                            "Client ids for a specific audience",
                            color = SentinelMuted,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        if (clients.isEmpty()) {
                            Text("The client list could not be loaded here.", color = SentinelMuted)
                        }
                        clients.forEach { client ->
                            val cid = Phase3Logic.int(client, "id") ?: return@forEach
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = cid in selectedClients,
                                    onCheckedChange = { checked ->
                                        selectedClients = if (checked) selectedClients + cid
                                        else selectedClients - cid
                                    },
                                )
                                Text("${Phase3Logic.str(client, "legalName")} (#$cid)")
                            }
                        }
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        "Default is internal only. Publishing is set in the query, not by hiding: a client portal only ever receives items whose audience includes it.",
                        color = SentinelMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    ActionErrorText(error)
                    Spacer(Modifier.height(12.dp))
                    Row {
                        Button(
                            enabled = !saving,
                            onClick = {
                                if (title.isBlank()) {
                                    error = "Title is required."
                                    return@Button
                                }
                                saving = true
                                scope.launch {
                                    try {
                                        // Web's editor flow, two steps:
                                        // saveknowledge, then
                                        // setknowledgeaudience on the
                                        // dedicated endpoint.
                                        val body = kotlinx.serialization.json.buildJsonObject {
                                            put("title", title.trim())
                                            put("category", category)
                                            put("description", description)
                                        }
                                        val saved = if (editId == null) {
                                            viewModel.api.knowledgeCreate(body)
                                        } else {
                                            viewModel.api.knowledgeEdit(editId, body)
                                        }
                                        val kid = Phase3Logic.int(saved, "id") ?: editId
                                        if (kid != null) {
                                            try {
                                                viewModel.api.knowledgeAudience(
                                                    kid, audience, selectedClients.toList(),
                                                )
                                            } catch (e: Exception) {
                                                error = plainError(
                                                    e,
                                                    "Entry saved, but its audience could not be set.",
                                                )
                                                saving = false
                                                return@launch
                                            }
                                        }
                                        nav.popBackStack()
                                    } catch (e: Exception) {
                                        error = plainError(e, "Could not save this entry.")
                                    } finally {
                                        saving = false
                                    }
                                }
                            },
                        ) { Text(if (editId == null) "Save entry" else "Save changes") }
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        OutlinedButton(onClick = { nav.popBackStack() }) { Text("Cancel") }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
