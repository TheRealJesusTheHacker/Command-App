package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import net.sentinelsafeguard.app.data.api.lawDocCreate
import net.sentinelsafeguard.app.data.api.lawDocDelete
import net.sentinelsafeguard.app.data.api.lawDocEdit
import net.sentinelsafeguard.app.data.api.lawDocgen
import net.sentinelsafeguard.app.data.api.lawResearch
import net.sentinelsafeguard.app.data.api.lawWorkspace
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Dogg and Associates (Phase 6): web's legal workspace —
 * document library (list/detail/CRUD incl. research notes and
 * fast-path standard forms), Law Lookup (research queue),
 * Doc Gen (docgen queue), History (assistant requests),
 * Practice areas, and the Template catalog. There is NO
 * config/key UI here: Law AI configuration lives in the Admin
 * console (Phase 8), exactly as web separates it. Handler
 * refusals (non-fast template, jurisdiction) surface verbatim.
 */

@Composable
fun LawScreen(viewModel: AppViewModel, nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = rememberJsonScreen { viewModel.api.lawWorkspace() }
    var openDoc by remember { mutableStateOf<JsonObject?>(null) }
    var noteTitle by remember { mutableStateOf("") }
    var noteQuestion by remember { mutableStateOf("") }
    var researchQuestion by remember { mutableStateOf("") }
    var researchJurisdiction by remember { mutableStateOf("") }
    var docgenTemplate by remember { mutableStateOf("") }
    var docgenFields by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var fastTemplate by remember { mutableStateOf("") }
    var fastFields by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<Int?>(null) }
    val run: (suspend () -> String?) -> Unit = { block ->
        scope.launch {
            try {
                notice = block(); error = null; state.reloadKey++
            } catch (e: Exception) {
                error = plainError(e, "That could not be done. Try again.")
            }
        }
    }
    Column {
        MirrorHeader("Dogg and Associates", MirrorCopy.subtitle("law"))
        Text(
            "Legal intelligence, not legal advice.",
            color = SentinelMuted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Column(Modifier.padding(horizontal = 16.dp)) {
            if (notice != null) Text(notice!!)
            ActionErrorText(error)
        }
        when {
            state.error != null && state.data == null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList()
            else -> {
                val data = state.data!!
                val templates = Phase3Logic.arr(data, "categories")
                    .flatMap { Phase3Logic.arr(it, "templates") }
                LazyColumn {
                    Phase3Logic.strOrNull(data, "aiNotice")?.let { aiNotice ->
                        item {
                            MirrorCard("Legal assistant") {
                                Text(aiNotice, color = SentinelMuted)
                                DetailLine("Configured",
                                    if (Phase3Logic.bool(data, "aiConfigured") == true) "Yes" else "No")
                            }
                        }
                    }
                    item {
                        MirrorCard("Research note") {
                            TextFieldRow("Title", noteTitle, onChange = { noteTitle = it })
                            TextFieldRow("Question", noteQuestion, onChange = { noteQuestion = it },
                                singleLine = false, minLines = 2)
                            Button(onClick = {
                                run {
                                    val out = viewModel.api.lawDocCreate(buildJsonObject {
                                        put("kind", "research"); put("question", noteQuestion)
                                        put("title", noteTitle)
                                    })
                                    Phase3Logic.str(out, "message")
                                }
                            }) { Text("Save research note") }
                        }
                    }
                    item {
                        MirrorCard("Law Lookup") {
                            TextFieldRow("Question", researchQuestion, onChange = { researchQuestion = it },
                                singleLine = false, minLines = 2)
                            TextFieldRow("Jurisdiction", researchJurisdiction,
                                onChange = { researchJurisdiction = it })
                            Button(onClick = {
                                run {
                                    viewModel.api.lawResearch(buildJsonObject {
                                        put("question", researchQuestion)
                                        put("jurisdiction", researchJurisdiction)
                                    })
                                    "Research queued."
                                }
                            }) { Text("Submit research") }
                        }
                    }
                    item {
                        MirrorCard("Doc Gen") {
                            if (templates.isNotEmpty()) {
                                ChoiceMenu("Template",
                                    templates.firstOrNull { Phase3Logic.str(it, "key") == docgenTemplate }
                                        ?.let { Phase3Logic.str(it, "name") } ?: "Pick a template",
                                    templates.map { Phase3Logic.str(it, "name") },
                                ) { picked ->
                                    docgenTemplate = templates.firstOrNull {
                                        Phase3Logic.str(it, "name") == picked
                                    }?.let { Phase3Logic.str(it, "key") } ?: docgenTemplate
                                    docgenFields = emptyMap()
                                }
                            }
                            Phase6Logic.lawTemplate(data, docgenTemplate)?.let { template ->
                                Phase3Logic.arr(template, "fields").forEach { field ->
                                    val key = Phase3Logic.str(field, "key")
                                    TextFieldRow(
                                        Phase3Logic.str(field, "label") +
                                            if (Phase3Logic.bool(field, "required") == true) " (required)" else "",
                                        docgenFields[key] ?: "",
                                        onChange = { v -> docgenFields = docgenFields + (key to v) },
                                    )
                                }
                                Button(onClick = {
                                    run {
                                        viewModel.api.lawDocgen(buildJsonObject {
                                            put("templateKey", docgenTemplate)
                                            put("fields", buildJsonObject {
                                                docgenFields.forEach { (k, v) -> put(k, v) }
                                            })
                                        })
                                        "Document generation queued."
                                    }
                                }) { Text("Submit doc-gen") }
                            }
                        }
                    }
                    item {
                        MirrorCard("Standard form (fast path)") {
                            val fast = templates.filter { Phase3Logic.bool(it, "fastPath") == true }
                            if (fast.isNotEmpty()) {
                                ChoiceMenu("Template",
                                    fast.firstOrNull { Phase3Logic.str(it, "key") == fastTemplate }
                                        ?.let { Phase3Logic.str(it, "name") } ?: "Pick a template",
                                    fast.map { Phase3Logic.str(it, "name") },
                                ) { picked ->
                                    fastTemplate = fast.firstOrNull {
                                        Phase3Logic.str(it, "name") == picked
                                    }?.let { Phase3Logic.str(it, "key") } ?: fastTemplate
                                    fastFields = emptyMap()
                                }
                            }
                            Phase6Logic.lawTemplate(data, fastTemplate)?.let { template ->
                                Phase3Logic.arr(template, "fields").forEach { field ->
                                    val key = Phase3Logic.str(field, "key")
                                    TextFieldRow(
                                        Phase3Logic.str(field, "label") +
                                            if (Phase3Logic.bool(field, "required") == true) " (required)" else "",
                                        fastFields[key] ?: "",
                                        onChange = { v -> fastFields = fastFields + (key to v) },
                                    )
                                }
                                Button(onClick = {
                                    run {
                                        val out = viewModel.api.lawDocCreate(buildJsonObject {
                                            put("kind", "document"); put("templateKey", fastTemplate)
                                            put("fields", buildJsonObject {
                                                fastFields.forEach { (k, v) -> put(k, v) }
                                            })
                                        })
                                        Phase3Logic.str(out, "message")
                                    }
                                }) { Text("Save document") }
                            }
                            Text(
                                "Non-fast templates are refused here in the server's words — draft those with Doc Gen instead.",
                                color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    val docs = Phase3Logic.arr(data, "documents")
                    item { SectionHeader("Documents", docs.size) }
                    if (docs.isEmpty()) {
                        item { EmptyBox("No documents", "Law documents appear here.") }
                    }
                    items(docs) { doc ->
                        ItemRow(
                            title = Phase3Logic.str(doc, "title"),
                            subtitle = listOfNotNull(
                                Phase3Logic.strOrNull(doc, "kind"),
                                Phase3Logic.strOrNull(doc, "category"),
                                Phase3Logic.strOrNull(doc, "createdByName"),
                            ).joinToString(" · "),
                            onClick = { openDoc = doc },
                        )
                    }
                    val requests = Phase3Logic.arr(data, "aiRequests")
                    item { SectionHeader("History — assistant requests", requests.size) }
                    items(requests) { request ->
                        ItemRow(
                            title = Phase3Logic.str(request, "label"),
                            subtitle = listOfNotNull(
                                Phase3Logic.strOrNull(request, "kind"),
                                Phase3Logic.strOrNull(request, "status"),
                            ).joinToString(" · "),
                        )
                    }
                    item { SectionHeader("Practice areas") }
                    items(Phase3Logic.arr(data, "practiceAreas")) { area ->
                        MirrorCard(Phase3Logic.str(area, "name")) {
                            Phase3Logic.arrRaw(area, "topics")
                                .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                                .forEach { Text("• $it") }
                        }
                    }
                    item { SectionHeader("Template catalog") }
                    items(Phase3Logic.arr(data, "categories")) { category ->
                        MirrorCard(Phase3Logic.str(category, "name")) {
                            Phase3Logic.arr(category, "templates").forEach { template ->
                                DetailLine(
                                    Phase3Logic.str(template, "name"),
                                    listOfNotNull(
                                        Phase3Logic.strOrNull(template, "blurb"),
                                        if (Phase3Logic.bool(template, "fastPath") == true) "Fast path" else null,
                                    ).joinToString(" · "),
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
                openDoc?.let { doc ->
                    LawDocSheet(viewModel, doc,
                        onClose = { openDoc = null },
                        onDelete = { confirmDelete = Phase3Logic.int(doc, "id"); openDoc = null },
                        // AUDIT-171 B-02: the post-share hook was an
                        // empty lambda — a share now closes the
                        // sheet, like every other completed action.
                        onShared = { openDoc = null },
                        share = { title, body -> shareText(context, title, body) })
                }
            }
        }
    }
    confirmDelete?.let { id ->
        ConfirmGate("Delete document", "This permanently deletes this law document.", "Delete",
            onDismiss = { confirmDelete = null },
            onConfirm = {
                confirmDelete = null
                run {
                    val out = viewModel.api.lawDocDelete(id)
                    Phase3Logic.str(out, "message")
                }
            })
    }
}

@Composable
private fun LawDocSheet(
    viewModel: AppViewModel, doc: JsonObject,
    onClose: () -> Unit, onDelete: () -> Unit, onShared: () -> Unit,
    share: (String, String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val docId = Phase3Logic.int(doc, "id")
    var title by remember { mutableStateOf(Phase3Logic.str(doc, "title")) }
    var question by remember { mutableStateOf(Phase3Logic.str(doc, "question")) }
    var notes by remember { mutableStateOf(Phase3Logic.str(doc, "notes")) }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf<String?>(null) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        title = { Text(Phase3Logic.str(doc, "title")) },
        text = {
            Column {
                JsonLines(doc, "kind" to "Kind", "category" to "Category",
                    "jurisdiction" to "Jurisdiction", "createdByName" to "Created by")
                DetailLine("Body", Phase3Logic.strOrNull(doc, "body"))
                Spacer(Modifier.height(8.dp))
                TextFieldRow("Title", title, onChange = { title = it })
                TextFieldRow("Question", question, onChange = { question = it },
                    singleLine = false, minLines = 2)
                TextFieldRow("Notes", notes, onChange = { notes = it },
                    singleLine = false, minLines = 2)
                if (saved != null) Text(saved!!)
                ActionErrorText(error)
                Row {
                    Button(onClick = {
                        scope.launch {
                            try {
                                val out = viewModel.api.lawDocEdit(docId ?: 0, buildJsonObject {
                                    put("kind", Phase3Logic.str(doc, "kind"))
                                    put("title", title); put("question", question); put("notes", notes)
                                })
                                saved = Phase3Logic.str(out, "message")
                            } catch (e: Exception) {
                                error = plainError(e, "That could not be saved. Try again.")
                            }
                        }
                    }, modifier = Modifier.padding(end = 8.dp)) { Text("Save") }
                    OutlinedButton(onClick = {
                        share(Phase3Logic.str(doc, "title"), Phase3Logic.str(doc, "body"))
                        onShared()
                    }, modifier = Modifier.padding(end = 8.dp)) { Text("Share") }
                    OutlinedButton(onClick = onDelete) { Text("Delete") }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onClose) { Text("Close") }
        },
    )
}
