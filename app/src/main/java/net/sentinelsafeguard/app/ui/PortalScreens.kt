package net.sentinelsafeguard.app.ui

import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.portalApprovalInbox
import net.sentinelsafeguard.app.data.api.portalAuthorizationSign
import net.sentinelsafeguard.app.data.api.portalAuthorizations
import net.sentinelsafeguard.app.data.api.portalChangeRequestDecide
import net.sentinelsafeguard.app.data.api.portalChangeRequests
import net.sentinelsafeguard.app.data.api.portalContacts
import net.sentinelsafeguard.app.data.api.portalContract
import net.sentinelsafeguard.app.data.api.portalContractDocument
import net.sentinelsafeguard.app.data.api.portalContractSign
import net.sentinelsafeguard.app.data.api.portalContracts
import net.sentinelsafeguard.app.data.api.portalCriticalAlert
import net.sentinelsafeguard.app.data.api.portalFileRequestUpload
import net.sentinelsafeguard.app.data.api.portalFileRequests
import net.sentinelsafeguard.app.data.api.portalHealthTimeline
import net.sentinelsafeguard.app.data.api.portalHistory
import net.sentinelsafeguard.app.data.api.portalHome
import net.sentinelsafeguard.app.data.api.portalKnowledge
import net.sentinelsafeguard.app.data.api.portalLang
import net.sentinelsafeguard.app.data.api.portalLangSet
import net.sentinelsafeguard.app.data.api.portalNotificationPrefs
import net.sentinelsafeguard.app.data.api.portalNotificationPrefsSet
import net.sentinelsafeguard.app.data.api.portalPacketDocument
import net.sentinelsafeguard.app.data.api.portalPackets
import net.sentinelsafeguard.app.data.api.portalProposal
import net.sentinelsafeguard.app.data.api.portalProposalDecide
import net.sentinelsafeguard.app.data.api.portalProposalDocument
import net.sentinelsafeguard.app.data.api.portalProposals
import net.sentinelsafeguard.app.data.api.portalQuoteDecide
import net.sentinelsafeguard.app.data.api.portalReferrals
import net.sentinelsafeguard.app.data.api.portalReportDocument
import net.sentinelsafeguard.app.data.api.portalSignOffs
import net.sentinelsafeguard.app.data.api.portalStatusBoard
import net.sentinelsafeguard.app.data.api.portalSurveyAnswer
import net.sentinelsafeguard.app.data.api.portalSurveys
import net.sentinelsafeguard.app.data.api.portalTicketMessagePost
import net.sentinelsafeguard.app.data.api.portalTicketMessages
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * The portal shell (COMMAND-MIRROR-SPEC §2.1, Phase 9): the
 * twelve web portal views for Client/Family sign-ins, in web's
 * names and order, wired to the /api/app/portal/ endpoints
 * (Addendum 28). Every payload is the web portal handler's own;
 * the client identity comes from the token server-side — no
 * screen here names a client. Signing / deciding / answering
 * carry web's confirm semantics: a ConfirmGate states the
 * consequence first, and typed-name signatures are collected
 * where the handler requires them.
 */

/** One generic portal row: web's title-first tables, rendered
 *  from the payload's own fields. */
@Composable
private fun PortalRow(row: JsonObject, onClick: (() -> Unit)? = null) {
    ItemRow(
        title = Phase8bLogic.rowTitle(row),
        subtitle = Phase8bLogic.rowMeta(row).ifBlank { null },
        onClick = onClick,
    )
}

@Composable
private fun PortalScreenShell(
    title: String,
    subtitleKey: String,
    content: @Composable () -> Unit,
) {
    LazyColumn {
        item {
            MirrorHeader(title, MirrorCopy.subtitle(subtitleKey)) {
                MirrorChip("● Client portal")
            }
        }
        item { content() }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

/** The honest unlinked shape: the account signed in but is not
 *  linked to a client record — web's portal says the same. */
@Composable
private fun UnlinkedNotice() {
    EmptyBox(
        "Not linked yet",
        "This account isn't linked to a client record yet. " +
            "Ask Sentinel Safeguard to link it, then sign in again.",
    )
}

// ---- My Profile ----

@Composable
fun PortalProfileScreen(viewModel: AppViewModel, nav: NavController) {
    PortalScreenShell("My Profile", "portal_profile") {
        // Lane B / scope generalization (1.4.0): the portal's
        // distinct tools were only reachable through the
        // drawer; the portal home now breaks the main ones
        // out as a deck of Sentinel cards, one tap away.
        // Labels and routes are MirrorNav's own.
        ToolDeck(
            entries = listOf(
                ToolDeckEntry("Messages") { nav.navigate("portal_messages") },
                ToolDeckEntry("Critical Alert") { nav.navigate("portal_alert") },
                ToolDeckEntry("Contracts & Reports") { nav.navigate("portal_contracts") },
                ToolDeckEntry("My Files & Packets") { nav.navigate("portal_files") },
                ToolDeckEntry("Progress") { nav.navigate("portal_progress") },
                ToolDeckEntry("Your Documents") { nav.navigate("portal_documents") },
            ),
        )
        AsyncCard("Profile", loader = { viewModel.api.portalHome() }) { home, reload ->
            if (Phase3Logic.bool(home, "linked") == false) {
                UnlinkedNotice()
            } else {
                val profile = Phase3Logic.obj(home, "profile")
                if (profile != null) {
                    JsonLines(
                        profile,
                        "legalName" to "Business",
                        "dba" to "DBA",
                        "contactName" to "Contact",
                        "contactTitle" to "Title",
                        "email" to "Email",
                        "phone" to "Phone",
                        "street" to "Street",
                        "city" to "City",
                        "state" to "State",
                        "zip" to "ZIP",
                    )
                }
                val score = Phase3Logic.obj(home, "securityScore")
                if (score != null) {
                    Spacer(Modifier.height(10.dp))
                    DetailLine("Security score", Phase3Logic.displayValue("score", score["score"]))
                    DetailLine("Score state", Phase3Logic.str(score, "state").ifBlank { null })
                }
                val unread = Phase3Logic.int(home, "portalMessageUnread") ?: 0
                if (unread > 0) {
                    Spacer(Modifier.height(10.dp))
                    Text("$unread unread message(s) — see Messages.", color = SentinelMuted)
                }
            }
        }
        AsyncCard("Contacts", loader = { viewModel.api.portalContacts() }) { data, _ ->
            val rows = Phase3Logic.arr(data, "contacts")
            if (rows.isEmpty()) {
                Text("No contacts on file.", color = SentinelMuted)
            }
            rows.forEach { PortalRow(it) }
            Text(
                "Contact changes are made by Sentinel Safeguard — send a message if one is wrong.",
                color = SentinelMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        NotificationPrefsCard(viewModel)
        PortalLangCard(viewModel)
    }
}

@Composable
private fun NotificationPrefsCard(viewModel: AppViewModel) {
    AsyncCard("Notification preferences", loader = { viewModel.api.portalNotificationPrefs() }) { data, reload ->
        val prefs = Phase3Logic.obj(data, "prefs") ?: data
        var ticketUpdates by remember(prefs) { mutableStateOf(Phase3Logic.bool(prefs, "ticketUpdates") == true) }
        var invoiceEvents by remember(prefs) { mutableStateOf(Phase3Logic.bool(prefs, "invoiceEvents") == true) }
        var monitoringAlerts by remember(prefs) { mutableStateOf(Phase3Logic.bool(prefs, "monitoringAlerts") == true) }
        var digest by remember(prefs) { mutableStateOf(Phase3Logic.bool(prefs, "digest") == true) }
        var error by remember { mutableStateOf<String?>(null) }
        var saved by remember { mutableStateOf(false) }
        SwitchRow("Ticket updates", ticketUpdates) { ticketUpdates = it }
        SwitchRow("Invoice events", invoiceEvents) { invoiceEvents = it }
        SwitchRow("Monitoring alerts", monitoringAlerts) { monitoringAlerts = it }
        SwitchRow("Digest", digest) { digest = it }
        // Motion pass (2026-11-04): M-08 submit morph — the pill
        // carries idle → spinner → drawn tick in one shape; the
        // saved line below still confirms in words, and failures
        // keep the same plain error the old button produced.
        MorphSubmitButton(text = "Save preferences") {
            try {
                viewModel.api.portalNotificationPrefsSet(
                    buildJsonObject {
                        put("ticketUpdates", ticketUpdates)
                        put("invoiceEvents", invoiceEvents)
                        put("monitoringAlerts", monitoringAlerts)
                        put("digest", digest)
                    },
                )
                saved = true
                reload()
                true
            } catch (e: Exception) {
                error = plainError(e, "That could not be done. Try again.")
                false
            }
        }
        if (saved) Text("Notification preferences saved.", color = SentinelMuted)
        ActionErrorText(error)
    }
}

@Composable
private fun PortalLangCard(viewModel: AppViewModel) {
    AsyncCard("Language", loader = { viewModel.api.portalLang() }) { data, reload ->
        var lang by remember(data) { mutableStateOf(Phase3Logic.str(data, "portalLang").ifBlank { "en" }) }
        var error by remember { mutableStateOf<String?>(null) }
        val run = rememberAction({ error = it }, { reload() })
        ChoiceMenu("Portal language", Phase9Logic.PORTAL_LANGS.firstOrNull { it.first == lang }?.second ?: lang,
            Phase9Logic.PORTAL_LANGS.map { it.second }) { picked ->
            val code = Phase9Logic.PORTAL_LANGS.firstOrNull { it.second == picked }?.first ?: "en"
            lang = code
            run { viewModel.api.portalLangSet(code) }
        }
        ActionErrorText(error)
    }
}

// ---- Progress ----

@Composable
fun PortalProgressScreen(viewModel: AppViewModel, nav: NavController) {
    PortalScreenShell("Progress", "portal_progress") {
        AsyncCard("Your work in progress", loader = { viewModel.api.portalHome() }) { home, _ ->
            if (Phase3Logic.bool(home, "linked") == false) {
                UnlinkedNotice()
            } else {
                val rows = Phase3Logic.arr(home, "progress")
                if (rows.isEmpty()) Text("Nothing in progress right now.", color = SentinelMuted)
                rows.forEach { row ->
                    ItemRow(
                        title = Phase8bLogic.rowTitle(row),
                        subtitle = Phase9Logic.progressLine(row),
                    )
                }
                val roadmap = Phase3Logic.obj(home, "cfRoadmap")
                if (roadmap != null) {
                    Spacer(Modifier.height(10.dp))
                    val done = Phase3Logic.int(roadmap, "doneCount") ?: 0
                    val total = Phase3Logic.int(roadmap, "total") ?: 0
                    val pct = Phase3Logic.int(roadmap, "percent") ?: 0
                    DetailLine("Roadmap", "$done of $total done · $pct%")
                    Phase3Logic.arr(roadmap, "items").forEach { PortalRow(it) }
                }
            }
        }
    }
}

// ---- History (incl. the health timeline) ----

@Composable
fun PortalHistoryScreen(viewModel: AppViewModel, nav: NavController) {
    PortalScreenShell("History", "portal_history") {
        AsyncCard("History", loader = { viewModel.api.portalHistory() }) { data, _ ->
            val rows = Phase3Logic.arr(data, "history")
            if (rows.isEmpty()) Text("No history yet.", color = SentinelMuted)
            rows.forEach { row ->
                ItemRow(
                    title = Phase8bLogic.rowTitle(row),
                    subtitle = listOf(
                        Phase3Logic.str(row, "kind"),
                        Phase3Logic.str(row, "detail"),
                    ).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { null },
                )
            }
        }
        AsyncCard("Health timeline", loader = { viewModel.api.portalHealthTimeline() }) { data, _ ->
            val rows = Phase3Logic.arr(data, "events")
            if (rows.isEmpty()) Text("No health events yet.", color = SentinelMuted)
            rows.forEach { row ->
                ItemRow(
                    title = Phase8bLogic.rowTitle(row),
                    subtitle = Phase3Logic.str(row, "detail").ifBlank { null },
                )
            }
            val score = Phase3Logic.obj(data, "securityScore")
            if (score != null) {
                DetailLine("Security score", Phase3Logic.displayValue("score", score["score"]))
            }
        }
    }
}

// ---- Your Documents ----

@Composable
fun PortalDocumentsScreen(viewModel: AppViewModel, nav: NavController) {
    PortalScreenShell("Your Documents", "portal_documents") {
        AsyncCard("Documents", loader = { viewModel.api.portalHome() }) { home, _ ->
            if (Phase3Logic.bool(home, "linked") == false) {
                UnlinkedNotice()
            } else {
                val docs = Phase3Logic.arr(home, "documents")
                if (docs.isEmpty()) Text("No documents yet.", color = SentinelMuted)
                docs.forEach { PortalRow(it) }
            }
        }
        AsyncCard("Library", loader = { viewModel.api.portalHome() }) { home, _ ->
            val rows = Phase3Logic.arr(home, "library")
            if (rows.isEmpty()) Text("The library is empty.", color = SentinelMuted)
            rows.forEach { row ->
                ItemRow(
                    title = Phase8bLogic.rowTitle(row),
                    subtitle = listOf(
                        Phase3Logic.str(row, "category"),
                        Phase3Logic.str(row, "fileName"),
                    ).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { null },
                )
            }
        }
    }
}

// ---- My Files & Packets ----

@Composable
fun PortalFilesScreen(viewModel: AppViewModel, nav: NavController) {
    val context = LocalContext.current
    PortalScreenShell("My Files & Packets", "portal_files") {
        AsyncCard("Packets", loader = { viewModel.api.portalPackets() }) { data, _ ->
            var error by remember { mutableStateOf<String?>(null) }
            val run = rememberAction({ error = it }, {})
            val rows = Phase3Logic.arr(data, "packets")
            if (rows.isEmpty()) Text("No packets yet.", color = SentinelMuted)
            rows.forEach { row ->
                val id = Phase3Logic.int(row, "id")
                ItemRow(
                    title = Phase8bLogic.rowTitle(row),
                    subtitle = Phase3Logic.int(row, "itemCount")?.let { "$it item(s)" },
                    onClick = if (id == null) null else {
                        {
                            run {
                                val doc = viewModel.api.portalPacketDocument(id)
                                val b64 = Phase3Logic.str(doc, "pdfBase64")
                                if (b64.isBlank()) throw IllegalStateException("empty")
                                shareBytes(
                                    context, "packet-$id.pdf", "application/pdf",
                                    Base64.decode(b64, Base64.DEFAULT),
                                )
                            }
                        }
                    },
                )
            }
            ActionErrorText(error)
        }
        FileRequestsCard(viewModel)
    }
}

@Composable
private fun FileRequestsCard(viewModel: AppViewModel) {
    val context = LocalContext.current
    var pendingItem by remember { mutableStateOf<Int?>(null) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var uploaded by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        val itemId = pendingItem
        pendingItem = null
        if (uri != null && itemId != null) {
            scope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalStateException("empty")
                    var name = "upload.pdf"
                    runCatching {
                        context.contentResolver.query(
                            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null,
                        )?.use { c -> if (c.moveToFirst()) name = c.getString(0) }
                    }
                    viewModel.api.portalFileRequestUpload(
                        itemId, name, Base64.encodeToString(bytes, Base64.NO_WRAP),
                    )
                    uploaded = true
                } catch (e: Exception) {
                    uploadError = plainError(e, "That upload could not be sent. Try again.")
                }
            }
        }
    }
    AsyncCard("File requests", loader = { viewModel.api.portalFileRequests() }) { data, reload ->
        val rows = Phase3Logic.arr(data, "requests")
        if (rows.isEmpty()) Text("No file requests right now.", color = SentinelMuted)
        rows.forEach { req ->
            PortalRow(req)
            Phase3Logic.arr(req, "items").forEach { item ->
                val itemId = Phase3Logic.int(item, "id")
                val status = Phase3Logic.str(item, "status")
                Row(Modifier.padding(start = 16.dp)) {
                    Column {
                        Text(
                            Phase3Logic.str(item, "label").ifBlank { "File" },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(status, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                        if (itemId != null && status != "uploaded") {
                            OutlinedButton(onClick = {
                                pendingItem = itemId
                                picker.launch("application/pdf")
                            }) { Text("Upload file") }
                        }
                    }
                }
            }
        }
        if (uploaded) {
            Text("File uploaded.", color = SentinelMuted)
            OutlinedButton(onClick = { uploaded = false; reload() }) { Text("Refresh") }
        }
        ActionErrorText(uploadError)
    }
}

// ---- Messages ----

@Composable
fun PortalMessagesScreen(viewModel: AppViewModel, nav: NavController) {
    PortalScreenShell("Messages", "portal_messages") {
        AsyncCard("Your tickets", loader = { viewModel.api.portalHome() }) { home, _ ->
            var selected by remember { mutableStateOf<Int?>(null) }
            val tickets = Phase3Logic.arr(home, "tickets")
            if (tickets.isEmpty()) {
                Text("No tickets yet — Messages opens a thread on one of your tickets.", color = SentinelMuted)
            }
            tickets.forEach { t ->
                val id = Phase3Logic.int(t, "id")
                ItemRow(
                    title = Phase8bLogic.rowTitle(t),
                    subtitle = Phase3Logic.str(t, "status").ifBlank { null },
                    badge = if (selected == id) "Open" else null,
                    onClick = if (id == null) null else ({ selected = id }),
                )
            }
            val ticketId = selected
            if (ticketId != null) {
                PortalThread(viewModel, ticketId)
            }
        }
    }
}

@Composable
private fun PortalThread(viewModel: AppViewModel, ticketId: Int) {
    var reloadKey by remember(ticketId) { mutableStateOf(0) }
    var thread by remember(ticketId) { mutableStateOf<JsonObject?>(null) }
    var error by remember(ticketId) { mutableStateOf<String?>(null) }
    var draft by remember(ticketId) { mutableStateOf("") }
    var sendError by remember { mutableStateOf<String?>(null) }
    // M-18 (e) Send arc: bumped only on a successful send
    // (rememberAction's onSuccess), never on failure.
    var sendKey by remember { mutableStateOf(0) }
    val run = rememberAction({ sendError = it }, { draft = ""; sendKey++; reloadKey++ })
    androidx.compose.runtime.LaunchedEffect(ticketId, reloadKey) {
        error = null
        try {
            thread = viewModel.api.portalTicketMessages(ticketId)
        } catch (e: Exception) {
            error = plainError(e, "This thread could not be loaded. Try again.")
        }
    }
    Column(Modifier.padding(top = 12.dp)) {
        Text("Thread", style = MaterialTheme.typography.titleMedium)
        when {
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
            thread == null -> Text("Loading…", color = SentinelMuted)
            else -> {
                val msgs = Phase3Logic.arr(thread!!, "messages")
                if (msgs.isEmpty()) Text("No messages yet.", color = SentinelMuted)
                msgs.forEach { m ->
                    ItemRow(
                        title = Phase3Logic.str(m, "senderName").ifBlank { "Message" },
                        subtitle = Phase3Logic.str(m, "body").ifBlank { null },
                    )
                }
            }
        }
        TextFieldRow("Reply", draft, { draft = it }, singleLine = false, minLines = 2)
        Button(
            onClick = { if (draft.isNotBlank()) run { viewModel.api.portalTicketMessagePost(ticketId, draft) } },
            enabled = draft.isNotBlank(),
        ) {
            SendArcIcon(sendKey)
            Spacer(Modifier.padding(start = 8.dp))
            Text("Send reply")
        }
        ActionErrorText(sendError)
    }
}

// ---- Critical Alert ----

@Composable
fun PortalAlertScreen(viewModel: AppViewModel, nav: NavController) {
    PortalScreenShell("Critical Alert", "portal_alert") {
        MirrorCard("Send a Critical Alert") {
            var title by remember { mutableStateOf("") }
            var details by remember { mutableStateOf("") }
            var confirm by remember { mutableStateOf(false) }
            var sent by remember { mutableStateOf<String?>(null) }
            var error by remember { mutableStateOf<String?>(null) }
            val run = rememberAction({ error = it }, { /* set below */ })
            Text(
                "A Critical Alert opens a Critical-priority ticket with Sentinel Safeguard right away. " +
                    "Use it when something is actively wrong.",
                color = SentinelMuted,
            )
            TextFieldRow("What is happening?", title, { title = it })
            TextFieldRow("Details", details, { details = it }, singleLine = false, minLines = 3)
            Button(
                onClick = { confirm = true },
                enabled = title.isNotBlank(),
            ) { Text("Send Critical Alert") }
            if (sent != null) Text(sent!!, color = SentinelMuted)
            ActionErrorText(error)
            if (confirm) {
                ConfirmGate(
                    title = "Send this Critical Alert?",
                    body = "This files a Critical-priority ticket on your record immediately.",
                    confirmLabel = "Send alert",
                    onDismiss = { confirm = false },
                    onConfirm = {
                        confirm = false
                        run {
                            val out = viewModel.api.portalCriticalAlert(title, details)
                            sent = Phase3Logic.str(out, "message").ifBlank { "Critical Alert sent." }
                            title = ""
                            details = ""
                        }
                    },
                )
            }
        }
    }
}

// ---- My Sign-offs ----

@Composable
fun PortalSignOffsScreen(viewModel: AppViewModel, nav: NavController) {
    PortalScreenShell("My Sign-offs", "portal_signoffs") {
        AsyncCard("Sign-offs", loader = { viewModel.api.portalSignOffs() }) { data, _ ->
            val rows = Phase3Logic.arr(data, "signOffs")
            if (rows.isEmpty()) {
                Text("No sign-off contracts right now. Ones awaiting your signature appear under Contracts & Reports.", color = SentinelMuted)
            }
            rows.forEach { PortalRow(it) }
        }
        AsyncCard("Authorizations", loader = { viewModel.api.portalAuthorizations() }) { data, reload ->
            var signerName by remember { mutableStateOf("") }
            var pendingId by remember { mutableStateOf<Int?>(null) }
            var error by remember { mutableStateOf<String?>(null) }
            val run = rememberAction({ error = it }, { pendingId = null; reload() })
            val rows = Phase3Logic.arr(data, "authorizations")
            if (rows.isEmpty()) Text("No authorizations awaiting you.", color = SentinelMuted)
            rows.forEach { row ->
                PortalRow(row)
                if (Phase9Logic.authorizationAwaiting(row)) {
                    val id = Phase3Logic.int(row, "id")
                    if (id != null) {
                        OutlinedButton(onClick = { pendingId = id }) { Text("Sign") }
                    }
                }
            }
            ActionErrorText(error)
            val pid = pendingId
            if (pid != null) {
                Column {
                    TextFieldRow("Type your full name — it is your signature", signerName, { signerName = it })
                    Row {
                        Button(
                            onClick = { run { viewModel.api.portalAuthorizationSign(pid, signerName) } },
                            enabled = signerName.isNotBlank(),
                        ) { Text("Sign authorization") }
                        OutlinedButton(onClick = { pendingId = null }) { Text("Cancel") }
                    }
                }
            }
        }
    }
}

// ---- Contracts & Reports ----

@Composable
fun PortalContractsScreen(viewModel: AppViewModel, nav: NavController) {
    val context = LocalContext.current
    PortalScreenShell("Contracts & Reports", "portal_contracts") {
        AsyncCard("Approval inbox", loader = { viewModel.api.portalApprovalInbox() }) { data, _ ->
            val counts = Phase3Logic.obj(data, "counts")
            if (counts != null) {
                DetailLine("Waiting on you", (Phase9Logic.inboxTotal(data)).toString())
                DetailLine("Quotes", Phase3Logic.displayValue("quote", counts["quote"]))
                DetailLine("Proposals", Phase3Logic.displayValue("proposal", counts["proposal"]))
                DetailLine("Change requests", Phase3Logic.displayValue("changeRequest", counts["changeRequest"]))
            }
            Phase3Logic.arr(data, "items").forEach { PortalRow(it) }
        }
        PortalContractsCard(viewModel)
        PortalQuotesCard(viewModel)
        PortalProposalsCard(viewModel)
        PortalChangeRequestsCard(viewModel)
        AsyncCard("Reports", loader = { viewModel.api.portalContracts() }) { data, _ ->
            var error by remember { mutableStateOf<String?>(null) }
            val run = rememberAction({ error = it }, {})
            val engagements = Phase3Logic.arr(data, "engagements")
            if (engagements.isEmpty()) Text("No reports yet.", color = SentinelMuted)
            engagements.forEach { row ->
                val id = Phase3Logic.int(row, "id")
                ItemRow(
                    title = Phase8bLogic.rowTitle(row),
                    subtitle = Phase8bLogic.rowMeta(row).ifBlank { null },
                    onClick = if (id == null) null else {
                        {
                            run {
                                val doc = viewModel.api.portalReportDocument(id)
                                val b64 = Phase3Logic.str(doc, "pdfBase64")
                                if (b64.isBlank()) throw IllegalStateException("empty")
                                shareBytes(
                                    context, "report-$id.pdf", "application/pdf",
                                    Base64.decode(b64, Base64.DEFAULT),
                                )
                            }
                        }
                    },
                )
            }
            ActionErrorText(error)
        }
    }
}

@Composable
private fun PortalContractsCard(viewModel: AppViewModel) {
    val context = LocalContext.current
    AsyncCard("Contracts", loader = { viewModel.api.portalContracts() }) { data, reload ->
        var signerName by remember { mutableStateOf("") }
        var signTarget by remember { mutableStateOf<Int?>(null) }
        var expanded by remember { mutableStateOf<Int?>(null) }
        var detail by remember { mutableStateOf<JsonObject?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        val run = rememberAction({ error = it }, { signTarget = null; reload() })
        val loadRun = rememberAction({ error = it }, {})
        val rows = Phase3Logic.arr(data, "contracts")
        if (rows.isEmpty()) Text("No contracts yet.", color = SentinelMuted)
        rows.forEach { row ->
            val id = Phase3Logic.int(row, "id") ?: return@forEach
            ItemRow(
                title = Phase8bLogic.rowTitle(row),
                subtitle = Phase3Logic.str(row, "status").ifBlank { null },
                onClick = {
                    expanded = id
                    loadRun { detail = viewModel.api.portalContract(id) }
                },
            )
            if (expanded == id && detail != null) {
                val contract = Phase3Logic.obj(detail!!, "contract") ?: detail!!
                val body = Phase3Logic.str(contract, "body")
                if (body.isNotBlank()) {
                    Text(body, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                }
                Row {
                    OutlinedButton(onClick = {
                        loadRun {
                            val doc = viewModel.api.portalContractDocument(id)
                            val b64 = Phase3Logic.str(doc, "pdfBase64")
                            if (b64.isNotBlank()) {
                                shareBytes(
                                    context, "contract-$id.pdf", "application/pdf",
                                    Base64.decode(b64, Base64.DEFAULT),
                                )
                            } else {
                                shareText(context, Phase3Logic.str(contract, "title"), Phase3Logic.str(doc, "html"))
                            }
                        }
                    }) { Text("Document") }
                    if (Phase9Logic.contractSignable(row)) {
                        OutlinedButton(onClick = { signTarget = id }) { Text("Sign") }
                    }
                }
            }
        }
        ActionErrorText(error)
        val target = signTarget
        if (target != null) {
            Column {
                TextFieldRow("Type your full name — it is your signature", signerName, { signerName = it })
                Row {
                    Button(
                        onClick = { run { viewModel.api.portalContractSign(target, signerName) } },
                        enabled = signerName.isNotBlank(),
                    ) { Text("Sign contract") }
                    OutlinedButton(onClick = { signTarget = null }) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
private fun PortalQuotesCard(viewModel: AppViewModel) {
    AsyncCard("Quotes", loader = { viewModel.api.portalHome() }) { home, reload ->
        var pending by remember { mutableStateOf<Pair<Int, String>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        val run = rememberAction({ error = it }, { pending = null; reload() })
        val rows = Phase3Logic.arr(home, "quotes")
        if (rows.isEmpty()) Text("No quotes right now.", color = SentinelMuted)
        rows.forEach { row ->
            val id = Phase3Logic.int(row, "id")
            PortalRow(row)
            if (id != null && Phase3Logic.str(row, "status") == "sent") {
                Row {
                    Button(onClick = { pending = id to "approve" }) { Text("Approve") }
                    OutlinedButton(onClick = { pending = id to "decline" }) { Text("Decline") }
                }
            }
        }
        ActionErrorText(error)
        val p = pending
        if (p != null) {
            ConfirmGate(
                title = if (p.second == "approve") "Approve this quote?" else "Decline this quote?",
                body = "Your decision is recorded on the quote right away.",
                confirmLabel = if (p.second == "approve") "Approve" else "Decline",
                onDismiss = { pending = null },
                onConfirm = { run { viewModel.api.portalQuoteDecide(p.first, p.second) } },
            )
        }
    }
}

@Composable
private fun PortalProposalsCard(viewModel: AppViewModel) {
    val context = LocalContext.current
    AsyncCard("Proposals", loader = { viewModel.api.portalProposals() }) { data, reload ->
        var pending by remember { mutableStateOf<Pair<Int, String>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        val run = rememberAction({ error = it }, { pending = null; reload() })
        val docRun = rememberAction({ error = it }, {})
        val rows = Phase3Logic.arr(data, "proposals")
        if (rows.isEmpty()) Text("No proposals right now.", color = SentinelMuted)
        rows.forEach { row ->
            val id = Phase3Logic.int(row, "id")
            PortalRow(row)
            if (id != null) {
                Row {
                    OutlinedButton(onClick = {
                        docRun {
                            val doc = viewModel.api.portalProposalDocument(id)
                            val b64 = Phase3Logic.str(doc, "pdfBase64")
                            if (b64.isNotBlank()) {
                                shareBytes(
                                    context, "proposal-$id.pdf", "application/pdf",
                                    Base64.decode(b64, Base64.DEFAULT),
                                )
                            }
                        }
                    }) { Text("Document") }
                    val status = Phase3Logic.str(row, "status")
                    if (status == "issued" || status == "viewed") {
                        Button(onClick = { pending = id to "accept" }) { Text("Accept") }
                        OutlinedButton(onClick = { pending = id to "decline" }) { Text("Decline") }
                    }
                }
            }
        }
        ActionErrorText(error)
        val p = pending
        if (p != null) {
            ConfirmGate(
                title = if (p.second == "accept") "Accept this proposal?" else "Decline this proposal?",
                body = "Your decision is recorded on the proposal right away.",
                confirmLabel = if (p.second == "accept") "Accept" else "Decline",
                onDismiss = { pending = null },
                onConfirm = { run { viewModel.api.portalProposalDecide(p.first, p.second) } },
            )
        }
    }
}

@Composable
private fun PortalChangeRequestsCard(viewModel: AppViewModel) {
    AsyncCard("Change requests", loader = { viewModel.api.portalChangeRequests() }) { data, reload ->
        var pending by remember { mutableStateOf<Pair<Int, String>?>(null) }
        var signerName by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        val run = rememberAction({ error = it }, { pending = null; reload() })
        val rows = Phase3Logic.arr(data, "changeRequests")
        if (rows.isEmpty()) Text("No change requests right now.", color = SentinelMuted)
        rows.forEach { row ->
            val id = Phase3Logic.int(row, "id")
            PortalRow(row)
            if (id != null && Phase3Logic.str(row, "status") == "sent") {
                Row {
                    Button(onClick = { pending = id to "approve" }) { Text("Approve") }
                    OutlinedButton(onClick = { pending = id to "decline" }) { Text("Decline") }
                }
            }
        }
        ActionErrorText(error)
        val p = pending
        if (p != null && p.second == "approve") {
            Column {
                Text(
                    "Approving signs this change request — type your full name as your signature.",
                    color = SentinelMuted,
                )
                TextFieldRow("Your full name", signerName, { signerName = it })
                Row {
                    Button(
                        onClick = {
                            run {
                                viewModel.api.portalChangeRequestDecide(p.first, "approve", signerName)
                            }
                        },
                        enabled = signerName.isNotBlank(),
                    ) { Text("Approve & sign") }
                    OutlinedButton(onClick = { pending = null }) { Text("Cancel") }
                }
            }
        } else if (p != null) {
            ConfirmGate(
                title = "Decline this change request?",
                body = "Your decision is recorded right away.",
                confirmLabel = "Decline",
                onDismiss = { pending = null },
                onConfirm = { run { viewModel.api.portalChangeRequestDecide(p.first, "decline") } },
            )
        }
    }
}

// ---- Knowledge (portal audience only) ----

@Composable
fun PortalKnowledgeScreen(viewModel: AppViewModel, nav: NavController) {
    PortalScreenShell("Knowledge", "portal_knowledge") {
        AsyncCard("Knowledge", loader = { viewModel.api.portalKnowledge() }) { data, _ ->
            val rows = Phase3Logic.arr(data, "library")
            if (rows.isEmpty()) Text("No knowledge entries for you yet.", color = SentinelMuted)
            rows.forEach { row ->
                ItemRow(
                    title = Phase8bLogic.rowTitle(row),
                    subtitle = listOf(
                        Phase3Logic.str(row, "category"),
                        Phase3Logic.str(row, "description"),
                    ).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { null },
                )
            }
        }
    }
}

// ---- Status Board ----

@Composable
fun PortalStatusScreen(viewModel: AppViewModel, nav: NavController) {
    PortalScreenShell("Status Board", "portal_status") {
        AsyncCard("Status Board", loader = { viewModel.api.portalStatusBoard() }) { data, _ ->
            val board = Phase3Logic.obj(data, "board") ?: data
            val clientName = Phase3Logic.str(board, "clientName")
            if (clientName.isNotBlank()) {
                DetailLine("Client", clientName)
            }
            listOf(
                "locations" to "Locations",
                "assets" to "Assets",
                "warranties" to "Warranties",
                "licenses" to "Licenses",
                "retainers" to "Retainers",
                "maintUpcoming" to "Maintenance coming up",
                "emergencyContacts" to "Emergency contacts",
            ).forEach { (key, label) ->
                val rows = Phase3Logic.arr(board, key)
                if (rows.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(label, style = MaterialTheme.typography.titleSmall)
                    rows.forEach { PortalRow(it) }
                }
            }
        }
    }
}

// ---- Feedback & Referrals ----

@Composable
fun PortalFeedbackScreen(viewModel: AppViewModel, nav: NavController) {
    PortalScreenShell("Feedback & Referrals", "portal_feedback") {
        AsyncCard("Surveys", loader = { viewModel.api.portalSurveys() }) { data, reload ->
            var answerTarget by remember { mutableStateOf<Int?>(null) }
            var rating by remember { mutableStateOf(5) }
            var recommend by remember { mutableStateOf(true) }
            var comment by remember { mutableStateOf("") }
            var confirm by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }
            val run = rememberAction({ error = it }, { confirm = false; answerTarget = null; reload() })
            val rows = Phase3Logic.arr(data, "surveys")
            if (rows.isEmpty()) Text("No surveys waiting.", color = SentinelMuted)
            rows.forEach { row ->
                val id = Phase3Logic.int(row, "id")
                PortalRow(row)
                if (id != null && Phase3Logic.str(row, "status") == "pending") {
                    OutlinedButton(onClick = { answerTarget = id }) { Text("Answer") }
                }
            }
            val target = answerTarget
            if (target != null) {
                Column {
                    Text("How did we do?", style = MaterialTheme.typography.titleSmall)
                    Row {
                        Phase9Logic.SURVEY_RATINGS.forEach { r ->
                            FilterChip(
                                selected = rating == r,
                                onClick = { rating = r },
                                label = { Text(r.toString()) },
                                modifier = Modifier.padding(end = 4.dp),
                            )
                        }
                    }
                    SwitchRow("Would you recommend us?", recommend) { recommend = it }
                    TextFieldRow("Anything to add?", comment, { comment = it }, singleLine = false, minLines = 2)
                    Button(onClick = { confirm = true }) { Text("Send answer") }
                }
            }
            ActionErrorText(error)
            if (confirm && target != null) {
                ConfirmGate(
                    title = "Send this answer?",
                    body = "Your answer goes to Sentinel Safeguard as your survey response.",
                    confirmLabel = "Send",
                    onDismiss = { confirm = false },
                    onConfirm = {
                        run { viewModel.api.portalSurveyAnswer(target, rating, recommend, comment) }
                    },
                )
            }
        }
        AsyncCard("Referrals", loader = { viewModel.api.portalReferrals() }) { data, _ ->
            val rows = Phase3Logic.arr(data, "referrals")
            if (rows.isEmpty()) Text("No referrals recorded yet.", color = SentinelMuted)
            rows.forEach { row ->
                ItemRow(
                    title = Phase3Logic.str(row, "referredName").ifBlank { Phase8bLogic.rowTitle(row) },
                    subtitle = Phase3Logic.str(row, "status").ifBlank { null },
                )
            }
        }
    }
}
