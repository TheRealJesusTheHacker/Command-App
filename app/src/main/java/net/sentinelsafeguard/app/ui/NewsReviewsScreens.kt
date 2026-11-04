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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.newsBulkDeleteDrafts
import net.sentinelsafeguard.app.data.api.newsCreate
import net.sentinelsafeguard.app.data.api.newsDelete
import net.sentinelsafeguard.app.data.api.newsEdit
import net.sentinelsafeguard.app.data.api.newsFeed
import net.sentinelsafeguard.app.data.api.newsItems
import net.sentinelsafeguard.app.data.api.newsPublish
import net.sentinelsafeguard.app.data.api.newsUnpublish
import net.sentinelsafeguard.app.data.api.reviewFeatured
import net.sentinelsafeguard.app.data.api.reviewHide
import net.sentinelsafeguard.app.data.api.reviewPublish
import net.sentinelsafeguard.app.data.api.reviewResponsePublish
import net.sentinelsafeguard.app.data.api.reviewResponseSave
import net.sentinelsafeguard.app.data.api.reviewResponseUnpublish
import net.sentinelsafeguard.app.data.api.reviewResponses
import net.sentinelsafeguard.app.data.api.reviews
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Sentinel News + News Desk + Reviews (Phase 6). Sentinel News
 * is the published reader and News Desk is the editorial
 * workflow — kept visibly separate exactly as web separates
 * them (separate nav entries, separate endpoints; drafts never
 * appear in the reader). The Desk mirrors web's table + editor
 * with publish/unpublish/delete and bulk draft cleanup; web's
 * own refusal wordings (incomplete-publish, published-item bulk
 * refusal) surface verbatim. Reviews mirrors the queue with
 * publish/hide/featured and the owner review responses —
 * responses are OWNER-only inside the server handlers
 * (stricter than reviewsAccess) and refusals surface as-is.
 */

@Composable
fun SentinelNewsScreen(viewModel: AppViewModel, nav: NavController) {
    val state = rememberJsonScreen { viewModel.api.newsFeed() }
    var openId by remember { mutableStateOf<Int?>(null) }
    Column {
        MirrorHeader("Sentinel News", MirrorCopy.subtitle("sentinelnews"))
        when {
            state.error != null && state.data == null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList()
            else -> {
                val items = Phase3Logic.arr(state.data, "items")
                LazyColumn {
                    item { SectionHeader("Published", items.size) }
                    if (items.isEmpty()) {
                        item { EmptyBox("No published news", "Published Sentinel News items appear here.") }
                    }
                    items(items) { item ->
                        ItemRow(
                            title = Phase3Logic.str(item, "title"),
                            subtitle = listOfNotNull(
                                Phase3Logic.strOrNull(item, "date"),
                                Phase3Logic.strOrNull(item, "tag"),
                                Phase3Logic.strOrNull(item, "summary"),
                            ).joinToString(" · "),
                            onClick = { openId = Phase3Logic.int(item, "id") },
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
                openId?.let { id ->
                    val item = items.firstOrNull { Phase3Logic.int(it, "id") == id }
                    if (item != null) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { openId = null },
                            title = { Text(Phase3Logic.str(item, "title")) },
                            text = {
                                Column {
                                    JsonLines(item, "date" to "Date", "tag" to "Tag",
                                        "sourceName" to "Source", "sourceUrl" to "Source URL",
                                        "summary" to "Summary", "body" to "Story")
                                }
                            },
                            confirmButton = {
                                androidx.compose.material3.TextButton(onClick = { openId = null }) { Text("Close") }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewsDeskScreen(viewModel: AppViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    val state = rememberJsonScreen { viewModel.api.newsItems() }
    var editing by remember { mutableStateOf<JsonObject?>(null) }
    var creating by remember { mutableStateOf(false) }
    var selectedDrafts by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var confirmBulk by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Int?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val run: (suspend () -> String?) -> Unit = { block ->
        scope.launch {
            try {
                notice = block(); error = null; selectedDrafts = emptySet(); state.reloadKey++
            } catch (e: Exception) {
                error = plainError(e, "That could not be done. Try again.")
            }
        }
    }
    Column {
        MirrorHeader("News Desk", MirrorCopy.subtitle("newsdesk")) {
            Button(onClick = { creating = true }) { Text("New item") }
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            if (notice != null) Text(notice!!)
            ActionErrorText(error)
        }
        when {
            state.error != null && state.data == null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList()
            else -> {
                val data = state.data!!
                val triage = Phase3Logic.obj(data, "triage")
                val items = Phase3Logic.arr(data, "items")
                LazyColumn {
                    if (triage != null) {
                        item {
                            MirrorCard("Triage") {
                                JsonLines(triage, "drafts" to "Drafts", "published" to "Published",
                                    "today" to "Today", "thisWeek" to "This week", "older" to "Older")
                            }
                        }
                    }
                    item { SectionHeader("Items", items.size) }
                    if (items.isEmpty()) {
                        item { EmptyBox("No news items", "News Desk items appear here.") }
                    }
                    items(items) { item ->
                        val id = Phase3Logic.int(item, "id")
                        val isDraft = Phase3Logic.str(item, "state") == "draft"
                        MirrorCard(Phase3Logic.str(item, "title")) {
                            JsonLines(item, "date" to "Date", "tag" to "Tag", "state" to "State",
                                "origin" to "Origin", "summary" to "Summary")
                            if (Phase3Logic.bool(item, "isStale") == true) {
                                Text("Stale draft", color = SentinelMuted)
                            }
                            Row {
                                OutlinedButton(onClick = { editing = item },
                                    modifier = Modifier.padding(end = 8.dp)) { Text("Edit") }
                                if (isDraft) {
                                    Button(onClick = {
                                        id?.let { run { Phase3Logic.str(viewModel.api.newsPublish(it), "message") } }
                                    }, modifier = Modifier.padding(end = 8.dp)) { Text("Publish") }
                                } else {
                                    OutlinedButton(onClick = {
                                        id?.let { run { Phase3Logic.str(viewModel.api.newsUnpublish(it), "message") } }
                                    }, modifier = Modifier.padding(end = 8.dp)) { Text("Unpublish") }
                                }
                                OutlinedButton(onClick = { confirmDelete = id }) { Text("Delete") }
                            }
                            if (isDraft && id != null) {
                                Row {
                                    Switch(
                                        checked = id in selectedDrafts,
                                        onCheckedChange = { on ->
                                            selectedDrafts = if (on) selectedDrafts + id else selectedDrafts - id
                                        },
                                    )
                                    Text("Select draft for cleanup", Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    }
                    if (selectedDrafts.isNotEmpty()) {
                        item {
                            Column(Modifier.padding(16.dp)) {
                                Button(onClick = { confirmBulk = true }) {
                                    Text("Delete selected drafts (${selectedDrafts.size})")
                                }
                                Text(
                                    "Published items can never be selected — web's rule.",
                                    color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
    if (creating || editing != null) {
        NewsItemFormSheet(viewModel, editing, state.data,
            onClose = { creating = false; editing = null },
            onSaved = { msg ->
                creating = false; editing = null; notice = msg; state.reloadKey++
            })
    }
    if (confirmBulk) {
        ConfirmGate("Delete selected drafts",
            "This permanently deletes the selected draft items. Published items are never included.",
            "Delete drafts",
            onDismiss = { confirmBulk = false },
            onConfirm = {
                confirmBulk = false
                val ids = selectedDrafts.toList()
                run { Phase3Logic.str(viewModel.api.newsBulkDeleteDrafts(ids), "message") }
            })
    }
    confirmDelete?.let { id ->
        ConfirmGate("Delete news item", "This permanently deletes this news item.", "Delete",
            onDismiss = { confirmDelete = null },
            onConfirm = {
                confirmDelete = null
                run { Phase3Logic.str(viewModel.api.newsDelete(id), "message") }
            })
    }
}

@Composable
private fun NewsItemFormSheet(
    viewModel: AppViewModel, existing: JsonObject?, workspace: JsonObject?,
    onClose: () -> Unit, onSaved: (String?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val itemId = existing?.let { Phase3Logic.int(it, "id") }
    var title by remember { mutableStateOf(existing?.let { Phase3Logic.str(it, "title") } ?: "") }
    var summary by remember { mutableStateOf(existing?.let { Phase3Logic.str(it, "summary") } ?: "") }
    var body by remember { mutableStateOf(existing?.let { Phase3Logic.str(it, "body") } ?: "") }
    var tag by remember { mutableStateOf(existing?.let { Phase3Logic.str(it, "tag") } ?: "") }
    var date by remember { mutableStateOf(existing?.let { Phase3Logic.str(it, "date") } ?: "") }
    var scopeText by remember { mutableStateOf(existing?.let { Phase3Logic.str(it, "scope") } ?: "") }
    var sourceName by remember { mutableStateOf(existing?.let { Phase3Logic.str(it, "sourceName") } ?: "") }
    var sourceUrl by remember { mutableStateOf(existing?.let { Phase3Logic.str(it, "sourceUrl") } ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    val tags = workspace?.let {
        Phase3Logic.arrRaw(it, "tags")
            .mapNotNull { el -> (el as? kotlinx.serialization.json.JsonPrimitive)?.content }
    } ?: emptyList()
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (itemId == null) "New item" else "Edit item") },
        text = {
            Column {
                TextFieldRow("Headline", title, onChange = { title = it })
                if (tags.isNotEmpty()) {
                    ChoiceMenu("Tag", tag.ifBlank { tags.first() }, tags) { tag = it }
                } else {
                    TextFieldRow("Tag", tag, onChange = { tag = it })
                }
                DateFieldRow("Date", date, onChange = { date = it })
                TextFieldRow("Scope", scopeText, onChange = { scopeText = it })
                TextFieldRow("Summary", summary, onChange = { summary = it }, singleLine = false, minLines = 2)
                TextFieldRow("Body", body, onChange = { body = it }, singleLine = false, minLines = 4)
                TextFieldRow("Source name", sourceName, onChange = { sourceName = it })
                TextFieldRow("Source URL", sourceUrl, onChange = { sourceUrl = it })
                Text(
                    "Items are born drafts — publish from the desk when ready.",
                    color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
                )
                ActionErrorText(error)
            }
        },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    try {
                        val item = buildJsonObject {
                            put("title", title); put("summary", summary); put("body", body)
                            put("tag", tag.ifBlank { tags.firstOrNull() ?: "" }); put("date", date)
                            put("scope", scopeText); put("sourceName", sourceName); put("sourceUrl", sourceUrl)
                        }
                        val out = if (itemId == null) viewModel.api.newsCreate(item)
                        else viewModel.api.newsEdit(itemId, item)
                        onSaved(Phase3Logic.str(out, "message"))
                    } catch (e: Exception) {
                        error = plainError(e, "That could not be saved. Try again.")
                    }
                }
            }) { Text("Save draft") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onClose) { Text("Cancel") }
        },
    )
}

// ---- Reviews ----

@Composable
fun ReviewsScreen(viewModel: AppViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    val queueState = rememberJsonScreen { viewModel.api.reviews() }
    val responseState = rememberJsonScreen { viewModel.api.reviewResponses() }
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var responseDrafts by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    val run: (suspend () -> String?) -> Unit = { block ->
        scope.launch {
            try {
                notice = block(); error = null
                queueState.reloadKey++; responseState.reloadKey++
            } catch (e: Exception) {
                error = plainError(e, "That could not be done. Try again.")
            }
        }
    }
    Column {
        MirrorHeader("Reviews", MirrorCopy.subtitle("reviews"))
        Column(Modifier.padding(horizontal = 16.dp)) {
            if (notice != null) Text(notice!!)
            ActionErrorText(error)
        }
        LazyColumn {
            item { SectionHeader("Queue") }
            when {
                queueState.error != null && queueState.data == null -> item { ErrorBox(queueState.error!!) { queueState.reloadKey++ } }
                queueState.data == null -> item { SkeletonList(3) }
                else -> {
                    val rows = Phase3Logic.arr(queueState.data, "reviews")
                    if (rows.isEmpty()) {
                        item { EmptyBox("No reviews", "Submitted reviews appear here.") }
                    }
                    items(rows) { review ->
                        val id = Phase3Logic.int(review, "id") ?: return@items
                        val status = Phase3Logic.str(review, "status")
                        val featured = Phase3Logic.bool(review, "featured") == true
                        MirrorCard(Phase3Logic.str(review, "businessName")) {
                            DetailLine("Rating", Phase3Logic.int(review, "rating")?.let { "$it / 5" })
                            JsonLines(review, "text" to "Review", "status" to "Status")
                            DetailLine("Submitted", Phase3Logic.displayValue("createdAt", review["createdAt"]))
                            if (featured) {
                                DetailLine("Featured", "Order ${Phase3Logic.int(review, "featuredOrder") ?: 0}")
                            }
                            Row {
                                if (status != "published") {
                                    Button(onClick = {
                                        run { Phase3Logic.str(viewModel.api.reviewPublish(id), "message") }
                                    }, modifier = Modifier.padding(end = 8.dp)) { Text("Publish") }
                                } else {
                                    OutlinedButton(onClick = {
                                        run { Phase3Logic.str(viewModel.api.reviewHide(id), "message") }
                                    }, modifier = Modifier.padding(end = 8.dp)) { Text("Hide") }
                                }
                                OutlinedButton(onClick = {
                                    run {
                                        Phase3Logic.str(
                                            viewModel.api.reviewFeatured(id, !featured, if (featured) null else 1),
                                            "message")
                                    }
                                }) { Text(if (featured) "Unfeature" else "Feature") }
                            }
                        }
                    }
                }
            }
            item { SectionHeader("Responses") }
            item {
                Text(
                    "Responses are owner-only, as in web Command — the server enforces it.",
                    color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            when {
                responseState.error != null && responseState.data == null -> item { ErrorBox(responseState.error!!) { responseState.reloadKey++ } }
                responseState.data == null -> item { SkeletonList(3) }
                else -> {
                    val rows = Phase3Logic.arr(responseState.data, "reviews")
                    if (rows.isEmpty()) {
                        item { EmptyBox("No reviews", "Reviews with response controls appear here.") }
                    }
                    items(rows) { review ->
                        val id = Phase3Logic.int(review, "id") ?: return@items
                        val response = Phase3Logic.obj(review, "response")
                        val draft = responseDrafts[id]
                            ?: response?.let { Phase3Logic.str(it, "body") } ?: ""
                        MirrorCard(Phase3Logic.str(review, "businessName")) {
                            DetailLine("Response status", Phase6Logic.responseStatus(review))
                            response?.let {
                                DetailLine("Response", Phase3Logic.strOrNull(it, "body"))
                            }
                            TextFieldRow("Response", draft, onChange = { v ->
                                responseDrafts = responseDrafts + (id to v)
                            }, singleLine = false, minLines = 2)
                            Row {
                                Button(onClick = {
                                    run { Phase3Logic.str(viewModel.api.reviewResponseSave(id, draft), "message") }
                                }, modifier = Modifier.padding(end = 8.dp)) { Text("Save draft response") }
                                if (response != null && Phase3Logic.str(response, "status") != "published") {
                                    OutlinedButton(onClick = {
                                        run { Phase3Logic.str(viewModel.api.reviewResponsePublish(id), "message") }
                                    }, modifier = Modifier.padding(end = 8.dp)) { Text("Publish response") }
                                }
                                if (response != null && Phase3Logic.str(response, "status") == "published") {
                                    OutlinedButton(onClick = {
                                        run { Phase3Logic.str(viewModel.api.reviewResponseUnpublish(id), "message") }
                                    }) { Text("Unpublish response") }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
