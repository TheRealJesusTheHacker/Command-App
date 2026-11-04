package net.sentinelsafeguard.app.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.addSiteDiagram
import net.sentinelsafeguard.app.data.api.archiveRoadmapItem
import net.sentinelsafeguard.app.data.api.clientContacts
import net.sentinelsafeguard.app.data.api.clientDigests
import net.sentinelsafeguard.app.data.api.clientFiles
import net.sentinelsafeguard.app.data.api.clientDocExpiry
import net.sentinelsafeguard.app.data.api.clientDuplicates
import net.sentinelsafeguard.app.data.api.clientHealthTimeline
import net.sentinelsafeguard.app.data.api.clientHygiene
import net.sentinelsafeguard.app.data.api.clientMail
import net.sentinelsafeguard.app.data.api.clientMailThread
import net.sentinelsafeguard.app.data.api.clientNotes
import net.sentinelsafeguard.app.data.api.clientNotificationPrefs
import net.sentinelsafeguard.app.data.api.clientOffboard
import net.sentinelsafeguard.app.data.api.clientOutreachDraft
import net.sentinelsafeguard.app.data.api.clientPostureLetter
import net.sentinelsafeguard.app.data.api.clientReports
import net.sentinelsafeguard.app.data.api.clientScoreEvents
import net.sentinelsafeguard.app.data.api.clientSecurity
import net.sentinelsafeguard.app.data.api.clientSiteProfiles
import net.sentinelsafeguard.app.data.api.clientTraining
import net.sentinelsafeguard.app.data.api.createBreachWatch
import net.sentinelsafeguard.app.data.api.createClientNote
import net.sentinelsafeguard.app.data.api.createRisk
import net.sentinelsafeguard.app.data.api.createRoadmapItem
import net.sentinelsafeguard.app.data.api.deleteBreachWatch
import net.sentinelsafeguard.app.data.api.deleteClientContact
import net.sentinelsafeguard.app.data.api.deleteClientNote
import net.sentinelsafeguard.app.data.api.deleteSiteDiagram
import net.sentinelsafeguard.app.data.api.deleteSiteProfile
import net.sentinelsafeguard.app.data.api.discardClientDigest
import net.sentinelsafeguard.app.data.api.discardClientReport
import net.sentinelsafeguard.app.data.api.editClientNote
import net.sentinelsafeguard.app.data.api.exportVerifications
import net.sentinelsafeguard.app.data.api.firmPulse
import net.sentinelsafeguard.app.data.api.firmPulseDrilldown
import net.sentinelsafeguard.app.data.api.generateClientDigests
import net.sentinelsafeguard.app.data.api.generateClientReport
import net.sentinelsafeguard.app.data.api.generateRoadmap
import net.sentinelsafeguard.app.data.api.inviteClientContact
import net.sentinelsafeguard.app.data.api.issueVerification
import net.sentinelsafeguard.app.data.api.mergeClients
import net.sentinelsafeguard.app.data.api.pinClientNote
import net.sentinelsafeguard.app.data.api.revokeVerification
import net.sentinelsafeguard.app.data.api.runBreachMatch
import net.sentinelsafeguard.app.data.api.runOffboardStep
import net.sentinelsafeguard.app.data.api.saveClientContact
import net.sentinelsafeguard.app.data.api.saveClientOutreachDraft
import net.sentinelsafeguard.app.data.api.saveSiteProfile
import net.sentinelsafeguard.app.data.api.seedCompliance
import net.sentinelsafeguard.app.data.api.sendClientDigest
import net.sentinelsafeguard.app.data.api.sendClientReport
import net.sentinelsafeguard.app.data.api.sendPostureLetter
import net.sentinelsafeguard.app.data.api.setClientAcademyOrg
import net.sentinelsafeguard.app.data.api.setClientDocExpiry
import net.sentinelsafeguard.app.data.api.setClientNotificationPrefs
import net.sentinelsafeguard.app.data.api.setComplianceItem
import net.sentinelsafeguard.app.data.api.setHygieneItem
import net.sentinelsafeguard.app.data.api.setRiskState
import net.sentinelsafeguard.app.data.api.setRoadmapState
import net.sentinelsafeguard.app.data.api.startOffboard
import net.sentinelsafeguard.app.data.api.verifications
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Clients depth (Phase 3, mirror spec §6): the client profile's
 * sub-sections, in web's order, on the Phase 3 backend
 * (Addendum 14). Part 1 sits after Client details in the profile
 * (Draft first contact, Security score); Part 2 after the
 * Sentinel Verified card (Contacts & notifications editing,
 * Offboard, staff Notes, Health timeline, Site profiles,
 * Credential hygiene, Monthly posture letter, Client security,
 * Training/documents/transparency, Mail, Reports, Digests,
 * Firm Pulse). Every card loads independently.
 */

// ---------- Part 1 ----------

@Composable
fun ClientDepthPart1(viewModel: AppViewModel, clientId: Int) {
    OutreachCard(viewModel, clientId)
    SecurityScoreCard(viewModel, clientId)
}

@Composable
private fun OutreachCard(viewModel: AppViewModel, clientId: Int) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var channel by remember { mutableStateOf(Phase3Logic.OUTREACH_CHANNELS[0]) }
    var draft by remember { mutableStateOf("") }
    var framing by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    MirrorCard("Draft first contact") {
        ChoiceMenu("Channel", channel, Phase3Logic.OUTREACH_CHANNELS) { channel = it }
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                try {
                    val out = viewModel.api.clientOutreachDraft(clientId, channel)
                    draft = Phase3Logic.str(out, "draft")
                    framing = Phase3Logic.str(out, "framingLabel")
                    saved = false
                    error = null
                } catch (e: Exception) {
                    error = plainError(e, "The draft could not be created. Try again.")
                }
            }
        }) { Text("Draft first contact") }
        if (framing.isNotBlank()) {
            Text(framing, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
        }
        if (draft.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(draft, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { clipboard.setText(AnnotatedString(draft)) }) {
                    Text("Copy draft")
                }
                OutlinedButton(onClick = {
                    scope.launch {
                        try {
                            viewModel.api.saveClientOutreachDraft(clientId, channel, draft)
                            saved = true
                        } catch (e: Exception) {
                            error = plainError(e, "The draft could not be saved. Try again.")
                        }
                    }
                }) { Text("Save as draft note") }
            }
            if (saved) Text("Saved to the client's notes, marked as a draft.", color = SentinelMuted)
        }
        ActionErrorText(error)
    }
}

@Composable
private fun SecurityScoreCard(viewModel: AppViewModel, clientId: Int) {
    AsyncCard("Security score", loader = { viewModel.api.clientSecurity(clientId) }) { sec, _ ->
        val score = Phase3Logic.obj(sec, "securityScore")
        if (score == null) {
            Text("No security score yet.", color = SentinelMuted)
        } else {
            Text(
                "Score ${Phase3Logic.str(score, "score")} of 100 · baseline ${Phase3Logic.str(score, "baseline")}",
                style = MaterialTheme.typography.titleMedium,
            )
            Phase3Logic.arr(score, "factors").forEach { factor ->
                val pts = Phase3Logic.int(factor, "points") ?: 0
                Text(
                    "${if (pts >= 0) "+" else ""}$pts · ${Phase3Logic.str(factor, "label")}",
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}

// ---------- Part 2 ----------

@Composable
fun ClientDepthPart2(viewModel: AppViewModel, nav: NavController, clientId: Int) {
    ContactsEditCard(viewModel, clientId)
    OffboardCard(viewModel, clientId)
    StaffNotesCard(viewModel, clientId)
    HealthTimelineCard(viewModel, clientId)
    SiteProfilesCard(viewModel, clientId)
    HygieneCard(viewModel, clientId)
    PostureLetterCard(viewModel, clientId)
    SecurityDetailCard(viewModel, clientId)
    TrainingDocsCard(viewModel, clientId)
    ClientMailCard(viewModel, clientId)
    ClientFilesCard(viewModel, clientId)
    ClientReportsCard(viewModel, clientId)
    ClientDigestsCard(viewModel, clientId)
    MirrorCard("Firm Pulse") {
        Text(
            "Revenue, aging, pipeline and posture for the whole firm, with drill-down.",
            color = SentinelMuted,
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { nav.navigate("firmpulse") }) { Text("Open Firm Pulse") }
    }
}

@Composable
private fun ContactsEditCard(viewModel: AppViewModel, clientId: Int) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<Int?>(null) }
    var inviteFor by remember { mutableStateOf<Int?>(null) }
    var inviteName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AsyncCard(
        "Contacts & notifications", "contacts",
        loader = { viewModel.api.clientContacts(clientId) },
    ) { data, reload ->
        val contacts = Phase3Logic.arr(data, "contacts")
        if (contacts.isEmpty()) Text("No additional contacts on file.", color = SentinelMuted)
        contacts.forEach { contact ->
            val cid = Phase3Logic.int(contact, "id")
            Text(
                listOfNotNull(
                    Phase3Logic.str(contact, "name").takeIf { it.isNotBlank() },
                    Phase3Logic.str(contact, "kind").takeIf { it.isNotBlank() },
                    Phase3Logic.str(contact, "email").takeIf { it.isNotBlank() },
                    Phase3Logic.str(contact, "phone").takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 2.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    editingId = cid
                    name = Phase3Logic.str(contact, "name")
                    email = Phase3Logic.str(contact, "email")
                    phone = Phase3Logic.str(contact, "phone")
                    kind = Phase3Logic.str(contact, "kind")
                }) { Text("Edit") }
                TextButton(onClick = { inviteFor = cid; inviteName = "" }) { Text("Invite to portal") }
                TextButton(onClick = {
                    if (cid != null) scope.launch {
                        try {
                            viewModel.api.deleteClientContact(clientId, cid); reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The contact could not be removed. Try again.")
                        }
                    }
                }) { Text("Delete") }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (editingId == null) "Add contact" else "Edit contact",
            style = MaterialTheme.typography.titleSmall,
        )
        TextFieldRow("Name", name, { name = it })
        TextFieldRow("Email", email, { email = it })
        TextFieldRow("Phone", phone, { phone = it })
        TextFieldRow("Kind", kind, { kind = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    try {
                        viewModel.api.saveClientContact(
                            clientId,
                            buildJsonObject {
                                put("name", name); put("email", email)
                                put("phone", phone); put("kind", kind)
                            },
                            editingId,
                        )
                        name = ""; email = ""; phone = ""; kind = ""; editingId = null
                        reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The contact could not be saved. Try again.")
                    }
                }
            }) { Text("Save contact") }
            if (editingId != null) {
                OutlinedButton(onClick = {
                    editingId = null; name = ""; email = ""; phone = ""; kind = ""
                }) { Text("Cancel") }
            }
        }
        if (inviteFor != null) {
            Spacer(Modifier.height(8.dp))
            TextFieldRow("Invite username", inviteName, { inviteName = it })
            Button(onClick = {
                val cid = inviteFor ?: return@Button
                scope.launch {
                    try {
                        viewModel.api.inviteClientContact(clientId, cid, inviteName.trim())
                        inviteFor = null
                    } catch (e: Exception) {
                        // Web's refusal copy (e.g. a username is
                        // required) is the server's message — the
                        // callJson path already carries it.
                        error = plainError(e, "The invite could not be sent. Try again.")
                    }
                }
            }) { Text("Send invite") }
        }
        ActionErrorText(error)
        Spacer(Modifier.height(10.dp))
        NotificationPrefsBlock(viewModel, clientId)
    }
}

@Composable
private fun NotificationPrefsBlock(viewModel: AppViewModel, clientId: Int) {
    val scope = rememberCoroutineScope()
    var prefs by remember { mutableStateOf<JsonObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    androidx.compose.runtime.LaunchedEffect(clientId) {
        try {
            prefs = Phase3Logic.obj(
                viewModel.api.clientNotificationPrefs(clientId), "prefs",
            ) ?: viewModel.api.clientNotificationPrefs(clientId)
        } catch (e: Exception) {
            error = plainError(e, "Notification preferences could not be loaded.")
        }
    }
    Text("Notification preferences", style = MaterialTheme.typography.titleSmall)
    val current = prefs
    if (current == null) {
        Text("Loading…", color = SentinelMuted)
    } else {
        listOf(
            "ticketUpdates" to "Ticket updates",
            "invoiceEvents" to "Invoice events",
            "monitoringAlerts" to "Monitoring alerts",
            "digest" to "Digest",
        ).forEach { (key, label) ->
            SwitchRow(label, Phase3Logic.bool(current, key) == true) { on ->
                val updated = JsonObject(current + (key to kotlinx.serialization.json.JsonPrimitive(on)))
                prefs = updated
                scope.launch {
                    try {
                        viewModel.api.setClientNotificationPrefs(clientId, updated)
                    } catch (e: Exception) {
                        error = plainError(e, "Preferences could not be saved. Try again.")
                    }
                }
            }
        }
    }
    ActionErrorText(error)
}

@Composable
private fun OffboardCard(viewModel: AppViewModel, clientId: Int) {
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    AsyncCard("Offboard client", loader = { viewModel.api.clientOffboard(clientId) }) { data, reload ->
        val off = Phase3Logic.obj(data, "offboard") ?: data
        val started = Phase3Logic.bool(off, "started") == true
        Text(
            if (started) "Offboarding started · status ${Phase3Logic.str(off, "status")}"
            else "Offboarding has not been started for this client.",
            color = SentinelMuted,
        )
        Spacer(Modifier.height(8.dp))
        if (!started) {
            Button(onClick = {
                scope.launch {
                    try {
                        viewModel.api.startOffboard(clientId); reload()
                    } catch (e: Exception) {
                        error = plainError(e, "Offboarding could not be started. Try again.")
                    }
                }
            }) { Text("Start offboard") }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("export" to "Export", "revoke" to "Revoke", "archive" to "Archive").forEach { (step, label) ->
                    OutlinedButton(onClick = {
                        scope.launch {
                            try {
                                viewModel.api.runOffboardStep(clientId, step); reload()
                            } catch (e: Exception) {
                                error = plainError(e, "That step could not be run. Try again.")
                            }
                        }
                    }) { Text(label) }
                }
            }
        }
        ActionErrorText(error)
    }
}

@Composable
private fun StaffNotesCard(viewModel: AppViewModel, clientId: Int) {
    val scope = rememberCoroutineScope()
    var newBody by remember { mutableStateOf("") }
    var newPinned by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Int?>(null) }
    var editBody by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AsyncCard("Notes", "notes", loader = { viewModel.api.clientNotes(clientId) }) { data, reload ->
        val notes = Phase3Logic.arr(data, "notes")
        if (notes.isEmpty()) Text("No staff notes yet.", color = SentinelMuted)
        notes.forEach { note ->
            val nid = Phase3Logic.int(note, "id")
            val pinned = Phase3Logic.bool(note, "pinned") == true
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    (if (pinned) "📌 " else "") + Phase3Logic.str(note, "body"),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).padding(vertical = 2.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    if (nid != null) scope.launch {
                        try {
                            viewModel.api.pinClientNote(clientId, nid); reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The pin could not be changed. Try again.")
                        }
                    }
                }) { Text(if (pinned) "Unpin" else "Pin") }
                TextButton(onClick = { editingId = nid; editBody = Phase3Logic.str(note, "body") }) {
                    Text("Edit")
                }
                TextButton(onClick = {
                    if (nid != null) scope.launch {
                        try {
                            viewModel.api.deleteClientNote(clientId, nid); reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The note could not be deleted. Try again.")
                        }
                    }
                }) { Text("Delete") }
            }
            if (editingId == nid && nid != null) {
                TextFieldRow("Note", editBody, { editBody = it }, singleLine = false, minLines = 2)
                Button(onClick = {
                    scope.launch {
                        try {
                            viewModel.api.editClientNote(clientId, nid, editBody, pinned)
                            editingId = null; reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The note could not be saved. Try again.")
                        }
                    }
                }) { Text("Save note") }
            }
        }
        Spacer(Modifier.height(8.dp))
        TextFieldRow("New note", newBody, { newBody = it }, singleLine = false, minLines = 2)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = newPinned, onCheckedChange = { newPinned = it })
            Text("Pinned")
        }
        Button(
            enabled = newBody.isNotBlank(),
            onClick = {
                scope.launch {
                    try {
                        viewModel.api.createClientNote(clientId, newBody.trim(), newPinned)
                        newBody = ""; newPinned = false; reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The note could not be saved. Try again.")
                    }
                }
            },
        ) { Text("Save note") }
        ActionErrorText(error)
    }
}

@Composable
private fun HealthTimelineCard(viewModel: AppViewModel, clientId: Int) {
    AsyncCard(
        "Health timeline", "events",
        loader = { viewModel.api.clientHealthTimeline(clientId) },
    ) { data, _ ->
        val events = Phase3Logic.arr(data, "events")
        if (events.isEmpty()) Text("No health events yet.", color = SentinelMuted)
        events.forEach { event ->
            Text(
                Phase3Logic.str(event, "title"),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            val detail = Phase3Logic.str(event, "detail")
            if (detail.isNotBlank()) Text(detail, color = SentinelMuted)
            Phase3Logic.long(event, "at")?.let {
                Text(formatEpochMs(it), color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SiteProfilesCard(viewModel: AppViewModel, clientId: Int) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var siteName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var isp by remember { mutableStateOf("") }
    var circuitType by remember { mutableStateOf("") }
    var circuitSpeed by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var diagramFor by remember { mutableStateOf<Int?>(null) }
    var viewDiagram by remember { mutableStateOf<ByteArray?>(null) }
    AsyncCard(
        "Site profiles — survey & network", "siteProfiles",
        loader = { viewModel.api.clientSiteProfiles(clientId) },
    ) { data, reload ->
        val profiles = Phase3Logic.arr(data, "siteProfiles")
        if (profiles.isEmpty()) Text("No site profiles yet.", color = SentinelMuted)
        profiles.forEach { profile ->
            val pid = Phase3Logic.int(profile, "id")
            Text(
                Phase3Logic.str(profile, "siteName"),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 6.dp),
            )
            JsonLines(
                profile,
                "addressLabel" to "Address", "isp" to "ISP",
                "circuitType" to "Circuit type", "circuitSpeed" to "Circuit speed",
                "notes" to "Notes",
            )
            val diagrams = Phase3Logic.arr(profile, "diagrams")
            diagrams.forEach { diagram ->
                val did = Phase3Logic.int(diagram, "id")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Diagram · ${Phase3Logic.str(diagram, "fileName")}",
                        color = SentinelMuted,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        if (did != null) scope.launch {
                            try {
                                viewDiagram = viewModel.api.downloadAll("/api/app/site-diagrams/$did/download")
                            } catch (e: Exception) {
                                error = plainError(e, "The diagram could not be opened. Try again.")
                            }
                        }
                    }) { Text("View") }
                    TextButton(onClick = {
                        if (did != null) scope.launch {
                            try {
                                viewModel.api.deleteSiteDiagram(did); reload()
                            } catch (e: Exception) {
                                error = plainError(e, "The diagram could not be removed. Try again.")
                            }
                        }
                    }) { Text("Delete") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { diagramFor = pid }) { Text("Add diagram") }
                TextButton(onClick = {
                    if (pid != null) scope.launch {
                        try {
                            viewModel.api.deleteSiteProfile(clientId, pid); reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The site profile could not be deleted. Try again.")
                        }
                    }
                }) { Text("Delete") }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Add site profile", style = MaterialTheme.typography.titleSmall)
        TextFieldRow("Site name", siteName, { siteName = it })
        TextFieldRow("Address", address, { address = it })
        TextFieldRow("ISP", isp, { isp = it })
        TextFieldRow("Circuit type", circuitType, { circuitType = it })
        TextFieldRow("Circuit speed", circuitSpeed, { circuitSpeed = it })
        TextFieldRow("Notes", notes, { notes = it })
        Button(
            enabled = siteName.isNotBlank(),
            onClick = {
                scope.launch {
                    try {
                        viewModel.api.saveSiteProfile(
                            clientId,
                            buildJsonObject {
                                put("clientId", clientId); put("siteName", siteName)
                                put("addressLabel", address); put("isp", isp)
                                put("circuitType", circuitType); put("circuitSpeed", circuitSpeed)
                                put("notes", notes)
                            },
                            null,
                        )
                        siteName = ""; address = ""; isp = ""
                        circuitType = ""; circuitSpeed = ""; notes = ""
                        reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The site profile could not be saved. Try again.")
                    }
                }
            },
        ) { Text("Add site profile") }
        ActionErrorText(error)

        // Diagram picker: any document/image the server accepts
        // (its type list rides the site-profiles payload).
        val picker = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { uri ->
            val target = diagramFor
            diagramFor = null
            if (uri != null && target != null) {
                scope.launch {
                    try {
                        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                            ?: throw IllegalStateException("unreadable")
                        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "diagram"
                        val type = context.contentResolver.getType(uri) ?: "application/octet-stream"
                        viewModel.api.addSiteDiagram(
                            clientId, target, name, type,
                            Base64.encodeToString(bytes, Base64.NO_WRAP), bytes.size,
                        )
                        reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The diagram could not be attached. Try again.")
                    }
                }
            }
        }
        if (diagramFor != null) {
            androidx.compose.runtime.LaunchedEffect(diagramFor) { picker.launch("*/*") }
        }
        viewDiagram?.let { bytes ->
            val bitmap = remember(bytes) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { viewDiagram = null },
                title = { Text("Diagram") },
                text = {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Site diagram",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text("This diagram is not an image the phone can preview. Use Share to open it in another app.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        shareBytes(context, "diagram", "application/octet-stream", bytes)
                        viewDiagram = null
                    }) { Text("Share") }
                },
                dismissButton = {
                    TextButton(onClick = { viewDiagram = null }) { Text("Close") }
                },
            )
        }
    }
}

@Composable
private fun HygieneCard(viewModel: AppViewModel, clientId: Int) {
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    AsyncCard(
        "Credential hygiene checklist",
        loader = { viewModel.api.clientHygiene(clientId) },
    ) { data, reload ->
        val hygiene = Phase3Logic.obj(data, "hygiene") ?: data
        val items = Phase3Logic.arr(hygiene, "items")
        val (yes, no, unknown) = Phase3Logic.hygieneCounts(items)
        Text("$yes yes · $no no · $unknown unknown", color = SentinelMuted)
        items.forEach { item ->
            val key = Phase3Logic.str(item, "key")
            Spacer(Modifier.height(6.dp))
            Text(Phase3Logic.str(item, "label"), style = MaterialTheme.typography.bodyMedium)
            ChoiceMenu("State", Phase3Logic.str(item, "state"), Phase3Logic.HYGIENE_STATES) { picked ->
                scope.launch {
                    try {
                        viewModel.api.setHygieneItem(
                            clientId, key, picked, Phase3Logic.str(item, "note"),
                        )
                        reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The checklist could not be saved. Try again.")
                    }
                }
            }
        }
        ActionErrorText(error)
    }
}

@Composable
private fun PostureLetterCard(viewModel: AppViewModel, clientId: Int) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    var sent by remember { mutableStateOf<String?>(null) }
    AsyncCard(
        "Monthly posture letter",
        loader = { viewModel.api.clientPostureLetter(clientId) },
    ) { data, _ ->
        val letter = Phase3Logic.obj(data, "letter") ?: data
        JsonLines(letter, "monthLabel" to "Month", "subject" to "Subject")
        val text = Phase3Logic.str(letter, "text")
        if (text.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    try {
                        val out = viewModel.api.sendPostureLetter(clientId)
                        sent = Phase3Logic.str(out, "message")
                    } catch (e: Exception) {
                        error = plainError(e, "The letter could not be sent. Try again.")
                    }
                }
            }) { Text("Send posture letter") }
            if (text.isNotBlank()) {
                OutlinedButton(onClick = {
                    shareText(context, Phase3Logic.str(letter, "subject"), text)
                }) { Text("Share") }
            }
        }
        if (sent != null) Text(sent!!, color = SentinelMuted)
        ActionErrorText(error)
    }
}

@Composable
private fun SecurityDetailCard(viewModel: AppViewModel, clientId: Int) {
    val scope = rememberCoroutineScope()
    var rmTitle by remember { mutableStateOf("") }
    var rmDetail by remember { mutableStateOf("") }
    var riskTitle by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    AsyncCard(
        "Client security — roadmap, risks, breach watch, compliance",
        loader = { viewModel.api.clientSecurity(clientId) },
    ) { sec, reload ->
        Text("Roadmap", style = MaterialTheme.typography.titleSmall)
        val roadmap = Phase3Logic.arr(sec, "roadmap")
        if (roadmap.isEmpty()) Text("No roadmap items yet.", color = SentinelMuted)
        roadmap.forEach { item ->
            val iid = Phase3Logic.int(item, "id")
            Text(
                "${Phase3Logic.str(item, "title")} · ${Phase3Logic.str(item, "state")}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 2.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("planned", "in_progress", "done", "accepted_risk").forEach { state ->
                    TextButton(onClick = {
                        if (iid != null) scope.launch {
                            try {
                                viewModel.api.setRoadmapState(clientId, iid, state); reload()
                            } catch (e: Exception) {
                                error = plainError(e, "The roadmap item could not be updated. Try again.")
                            }
                        }
                    }) { Text(state.replace('_', ' ')) }
                }
                TextButton(onClick = {
                    if (iid != null) scope.launch {
                        try {
                            viewModel.api.archiveRoadmapItem(clientId, iid); reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The roadmap item could not be archived. Try again.")
                        }
                    }
                }) { Text("Archive") }
            }
        }
        TextFieldRow("Roadmap item title", rmTitle, { rmTitle = it })
        TextFieldRow("Detail", rmDetail, { rmDetail = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = rmTitle.isNotBlank(),
                onClick = {
                    scope.launch {
                        try {
                            viewModel.api.createRoadmapItem(clientId, rmTitle, rmDetail, "Normal")
                            rmTitle = ""; rmDetail = ""; reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The roadmap item could not be added. Try again.")
                        }
                    }
                },
            ) { Text("Add roadmap item") }
            OutlinedButton(onClick = {
                scope.launch {
                    try {
                        viewModel.api.generateRoadmap(clientId); reload()
                    } catch (e: Exception) {
                        error = plainError(e, "Suggestions could not be generated. Try again.")
                    }
                }
            }) { Text("Generate roadmap suggestions") }
        }

        Spacer(Modifier.height(12.dp))
        Text("Risks", style = MaterialTheme.typography.titleSmall)
        val risks = Phase3Logic.arr(sec, "risks")
        if (risks.isEmpty()) Text("No risks recorded.", color = SentinelMuted)
        risks.forEach { risk ->
            val rid = Phase3Logic.int(risk, "id")
            Text(
                "${Phase3Logic.str(risk, "title")} · ${Phase3Logic.str(risk, "state")}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 2.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("open", "accepted", "mitigated", "closed").forEach { state ->
                    TextButton(onClick = {
                        if (rid != null) scope.launch {
                            try {
                                viewModel.api.setRiskState(clientId, rid, state); reload()
                            } catch (e: Exception) {
                                error = plainError(e, "The risk could not be updated. Try again.")
                            }
                        }
                    }) { Text(state) }
                }
            }
        }
        TextFieldRow("Risk title", riskTitle, { riskTitle = it })
        Button(
            enabled = riskTitle.isNotBlank(),
            onClick = {
                scope.launch {
                    try {
                        viewModel.api.createRisk(clientId, riskTitle, "", "open")
                        riskTitle = ""; reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The risk could not be added. Try again.")
                    }
                }
            },
        ) { Text("Add risk") }

        Spacer(Modifier.height(12.dp))
        Text("Breach watch", style = MaterialTheme.typography.titleSmall)
        val watches = Phase3Logic.arr(sec, "breachWatches")
        if (watches.isEmpty()) Text("No breach watches.", color = SentinelMuted)
        watches.forEach { watch ->
            val wid = Phase3Logic.int(watch, "id")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Phase3Logic.str(watch, "domain"),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = {
                    if (wid != null) scope.launch {
                        try {
                            viewModel.api.deleteBreachWatch(clientId, wid); reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The watch could not be removed. Try again.")
                        }
                    }
                }) { Text("Delete") }
            }
        }
        val matches = Phase3Logic.arr(sec, "breachMatches")
        if (matches.isNotEmpty()) {
            Text("${matches.size} breach match(es) on file.", color = SentinelMuted)
        }
        TextFieldRow("Watch domain", domain, { domain = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = domain.isNotBlank(),
                onClick = {
                    scope.launch {
                        try {
                            viewModel.api.createBreachWatch(clientId, domain.trim())
                            domain = ""; reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The watch could not be added. Try again.")
                        }
                    }
                },
            ) { Text("Add watch") }
            OutlinedButton(onClick = {
                scope.launch {
                    try {
                        val out = viewModel.api.runBreachMatch(clientId)
                        notice = "Matcher run: ${Phase3Logic.str(out, "matches")} match(es) " +
                            "across ${Phase3Logic.str(out, "watches")} watch(es)."
                        reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The matcher could not run. Try again.")
                    }
                }
            }) { Text("Run breach matcher") }
        }

        Spacer(Modifier.height(12.dp))
        Text("Compliance", style = MaterialTheme.typography.titleSmall)
        val compliance = Phase3Logic.arr(sec, "compliance")
        if (compliance.isEmpty()) Text("No compliance tracker seeded yet.", color = SentinelMuted)
        compliance.forEach { item ->
            val iid = Phase3Logic.int(item, "id")
            Text(
                "${Phase3Logic.str(item, "controlId")} · ${Phase3Logic.str(item, "title")} · ${Phase3Logic.str(item, "state")}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 2.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("not_started", "in_progress", "done", "not_applicable").forEach { state ->
                    TextButton(onClick = {
                        if (iid != null) scope.launch {
                            try {
                                viewModel.api.setComplianceItem(clientId, iid, state); reload()
                            } catch (e: Exception) {
                                error = plainError(e, "The item could not be updated. Try again.")
                            }
                        }
                    }) { Text(state.replace('_', ' ')) }
                }
            }
        }
        val frameworks = Phase3Logic.arrRaw(sec, "complianceFrameworks")
            .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
        if (frameworks.isNotEmpty()) {
            var framework by remember { mutableStateOf(frameworks.first()) }
            ChoiceMenu("Framework", framework, frameworks) { framework = it }
            Button(onClick = {
                scope.launch {
                    try {
                        viewModel.api.seedCompliance(clientId, framework); reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The tracker could not be seeded. Try again.")
                    }
                }
            }) { Text("Seed compliance tracker") }
        }
        if (notice != null) Text(notice!!, color = SentinelMuted)
        ActionErrorText(error)
    }
}

@Composable
private fun TrainingDocsCard(viewModel: AppViewModel, clientId: Int) {
    val scope = rememberCoroutineScope()
    var orgId by remember { mutableStateOf("") }
    var docLabel by remember { mutableStateOf("") }
    var docExpiry by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    MirrorCard("Training, documents & transparency") {
        var training by remember { mutableStateOf<JsonObject?>(null) }
        var expiry by remember { mutableStateOf<JsonObject?>(null) }
        var events by remember { mutableStateOf<JsonObject?>(null) }
        androidx.compose.runtime.LaunchedEffect(clientId) {
            training = try { viewModel.api.clientTraining(clientId) } catch (e: Exception) { null }
            expiry = try { viewModel.api.clientDocExpiry(clientId) } catch (e: Exception) { null }
            events = try { viewModel.api.clientScoreEvents(clientId) } catch (e: Exception) { null }
        }
        Text("Training", style = MaterialTheme.typography.titleSmall)
        val tr = training
        if (tr == null) {
            Text("Loading…", color = SentinelMuted)
        } else {
            Text(
                "Academy state: ${Phase3Logic.str(tr, "state")}" +
                    (Phase3Logic.str(tr, "orgId").takeIf { it.isNotBlank() }?.let { " · org $it" } ?: ""),
                color = SentinelMuted,
            )
            val members = Phase3Logic.arr(tr, "members")
            members.forEach { member ->
                Text(
                    listOfNotNull(
                        Phase3Logic.str(member, "name").takeIf { it.isNotBlank() },
                        Phase3Logic.str(member, "email").takeIf { it.isNotBlank() },
                        Phase3Logic.str(member, "progress").takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
            TextFieldRow("Academy org id", orgId, { orgId = it })
            Button(onClick = {
                scope.launch {
                    try {
                        viewModel.api.setClientAcademyOrg(clientId, orgId.trim())
                        orgId = ""
                    } catch (e: Exception) {
                        error = plainError(e, "The Academy org could not be set. Try again.")
                    }
                }
            }) { Text("Set Academy org") }
        }
        Spacer(Modifier.height(10.dp))
        Text("Document expiry", style = MaterialTheme.typography.titleSmall)
        val ex = expiry
        if (ex == null) {
            Text("Loading…", color = SentinelMuted)
        } else {
            val docs = Phase3Logic.arr(ex, "documents")
            if (docs.isEmpty()) Text("No documents tracked.", color = SentinelMuted)
            docs.forEach { doc ->
                Text(
                    "${Phase3Logic.str(doc, "label")} · expires ${Phase3Logic.str(doc, "expiresDate")}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
            TextFieldRow("Document label", docLabel, { docLabel = it })
            DateFieldRow("Expiry date", docExpiry, { docExpiry = it })
            Button(
                enabled = docLabel.isNotBlank(),
                onClick = {
                    scope.launch {
                        try {
                            viewModel.api.setClientDocExpiry(
                                clientId,
                                buildJsonObject {
                                    put("label", docLabel); put("expiresDate", docExpiry)
                                },
                            )
                            docLabel = ""; docExpiry = ""
                            expiry = viewModel.api.clientDocExpiry(clientId)
                        } catch (e: Exception) {
                            error = plainError(e, "The document expiry could not be saved. Try again.")
                        }
                    }
                },
            ) { Text("Set document expiry") }
        }
        Spacer(Modifier.height(10.dp))
        Text("Score events", style = MaterialTheme.typography.titleSmall)
        val ev = events
        if (ev == null) {
            Text("Loading…", color = SentinelMuted)
        } else {
            val rows = Phase3Logic.arr(ev, "events")
            if (rows.isEmpty()) Text("No score-change events yet.", color = SentinelMuted)
            rows.forEach { row ->
                Text(
                    listOfNotNull(
                        Phase3Logic.str(row, "detail").takeIf { it.isNotBlank() }
                            ?: Phase3Logic.str(row, "title").takeIf { it.isNotBlank() },
                        Phase3Logic.str(row, "score").takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
            val note = Phase3Logic.str(ev, "note")
            if (note.isNotBlank()) Text(note, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
        }
        ActionErrorText(error)
    }
}

@Composable
private fun ClientMailCard(viewModel: AppViewModel, clientId: Int) {
    val scope = rememberCoroutineScope()
    var thread by remember { mutableStateOf<JsonObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    AsyncCard("Mail with this client", loader = { viewModel.api.clientMail(clientId) }) { data, _ ->
        val note = Phase3Logic.str(data, "note")
        if (note.isNotBlank()) Text(note, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
        if (Phase3Logic.bool(data, "configured") != true) {
            // The bridge's honest unconfigured state, in web's own
            // words (the payload's message), exactly as web shows.
            val message = Phase3Logic.str(data, "message")
            if (message.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        }
        val threads = Phase3Logic.arr(data, "threads")
        if (threads.isEmpty() && Phase3Logic.bool(data, "configured") == true) {
            Text("No mail threads with this client yet.", color = SentinelMuted)
        }
        threads.forEach { row ->
            val key = Phase3Logic.str(row, "key").ifBlank { Phase3Logic.str(row, "threadKey") }
            ItemRow(
                Phase3Logic.str(row, "subject").ifBlank { "Mail thread" },
                listOfNotNull(
                    Phase3Logic.str(row, "counterparty").takeIf { it.isNotBlank() },
                    Phase3Logic.str(row, "lastAt").takeIf { it.isNotBlank() },
                    Phase3Logic.str(row, "count").takeIf { it.isNotBlank() }?.let { "$it message(s)" },
                ).joinToString(" · "),
            ) {
                if (key.isNotBlank()) scope.launch {
                    try {
                        thread = viewModel.api.clientMailThread(clientId, key)
                    } catch (e: Exception) {
                        error = plainError(e, "The thread could not be opened. Try again.")
                    }
                }
            }
        }
        thread?.let { opened ->
            Spacer(Modifier.height(8.dp))
            Phase3Logic.arr(opened, "messages").forEach { msg ->
                Text(
                    "${Phase3Logic.str(msg, "from")} — ${Phase3Logic.str(msg, "subject")}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                val body = Phase3Logic.str(msg, "body").ifBlank { Phase3Logic.str(msg, "snippet") }
                if (body.isNotBlank()) Text(body, color = SentinelMuted)
            }
        }
        ActionErrorText(error)
    }
}

@Composable
private fun ClientReportsCard(viewModel: AppViewModel, clientId: Int) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var period by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDiscard by remember { mutableStateOf<Int?>(null) }
    AsyncCard("Client reports", "reports", loader = { viewModel.api.clientReports(clientId) }) { data, reload ->
        val reports = Phase3Logic.arr(data, "reports")
        if (reports.isEmpty()) Text("No reports yet.", color = SentinelMuted)
        reports.forEach { report ->
            val rid = Phase3Logic.int(report, "id")
            Text(
                "${Phase3Logic.str(report, "period")} · ${Phase3Logic.str(report, "state")}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 2.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    val body = Phase3Logic.str(report, "body")
                    if (body.isNotBlank()) {
                        shareText(context, "Client report ${Phase3Logic.str(report, "period")}", body)
                    }
                }) { Text("Share") }
                TextButton(onClick = {
                    if (rid != null) scope.launch {
                        try {
                            viewModel.api.sendClientReport(rid); reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The report could not be sent. Try again.")
                        }
                    }
                }) { Text("Send") }
                TextButton(onClick = { confirmDiscard = rid }) { Text("Discard") }
            }
        }
        TextFieldRow("Period (yyyy-MM)", period, { period = it })
        Button(
            enabled = period.isNotBlank(),
            onClick = {
                scope.launch {
                    try {
                        viewModel.api.generateClientReport(clientId, period.trim())
                        period = ""; reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The report could not be generated. Try again.")
                    }
                }
            },
        ) { Text("Generate report draft") }
        ActionErrorText(error)
        confirmDiscard?.let { rid ->
            ConfirmGate(
                "Discard report",
                "Discard this report draft? It will be removed from the client's reports.",
                "Discard",
                onDismiss = { confirmDiscard = null },
                onConfirm = {
                    confirmDiscard = null
                    scope.launch {
                        try {
                            viewModel.api.discardClientReport(rid); reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The report could not be discarded. Try again.")
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun ClientDigestsCard(viewModel: AppViewModel, clientId: Int) {
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    AsyncCard("Weekly digests", "digests", loader = { viewModel.api.clientDigests() }) { data, reload ->
        val mine = Phase3Logic.arr(data, "digests")
            .filter { Phase3Logic.int(it, "clientId") == clientId }
        if (mine.isEmpty()) Text("No digests queued for this client.", color = SentinelMuted)
        mine.forEach { digest ->
            val did = Phase3Logic.int(digest, "id")
            Text(
                "${Phase3Logic.str(digest, "weekStart")} · ${Phase3Logic.str(digest, "state")}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 2.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    if (did != null) scope.launch {
                        try {
                            viewModel.api.sendClientDigest(did); reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The digest could not be sent. Try again.")
                        }
                    }
                }) { Text("Send") }
                TextButton(onClick = {
                    if (did != null) scope.launch {
                        try {
                            viewModel.api.discardClientDigest(did); reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The digest could not be discarded. Try again.")
                        }
                    }
                }) { Text("Discard") }
            }
        }
        OutlinedButton(onClick = {
            scope.launch {
                try {
                    viewModel.api.generateClientDigests(); reload()
                } catch (e: Exception) {
                    error = plainError(e, "Digests could not be generated. Try again.")
                }
            }
        }) { Text("Generate weekly digests") }
        ActionErrorText(error)
    }
}

// ---------- Duplicates (Clients tab, OWNER) ----------

@Composable
fun DuplicatesMode(viewModel: AppViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var confirmPair by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val state = rememberJsonScreen { viewModel.api.clientDuplicates() }
    Column {
        Text(
            "Owner-only.",
            color = SentinelMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        when {
            state.error != null && state.data == null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList(4)
            else -> {
                val pairs = Phase3Logic.arr(state.data!!, "pairs")
                if (notice != null) {
                    Text(notice!!, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
                if (pairs.isEmpty()) {
                    EmptyBox("No duplicates", "No client records share a phone, email, or business name.")
                }
                pairs.forEach { pair ->
                    val a = Phase3Logic.obj(pair, "a")
                    val b = Phase3Logic.obj(pair, "b")
                    if (a == null || b == null) return@forEach
                    val aId = Phase3Logic.int(a, "id") ?: return@forEach
                    val bId = Phase3Logic.int(b, "id") ?: return@forEach
                    var winner by remember(pair.toString()) {
                        mutableStateOf(Phase3Logic.suggestedWinnerId(pair) ?: aId)
                    }
                    MirrorCard(
                        "Duplicate pair",
                        footerLabel = null, onFooter = null,
                    ) {
                        val reasons = Phase3Logic.arrRaw(pair, "reasons")
                            .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                        Text(
                            "Shared: ${reasons.joinToString(", ")}",
                            color = SentinelMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Phase3Logic.MERGE_FIELDS.forEach { (key, label) ->
                            val av = Phase3Logic.displayValue(key, a[key]) ?: "—"
                            val bv = Phase3Logic.displayValue(key, b[key]) ?: "—"
                            Text(
                                "$label: $av  |  $bv",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 1.dp),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceMenu(
                                "Survivor",
                                if (winner == aId) Phase3Logic.str(a, "legalName") else Phase3Logic.str(b, "legalName"),
                                listOf(Phase3Logic.str(a, "legalName"), Phase3Logic.str(b, "legalName")),
                            ) { picked ->
                                winner = if (picked == Phase3Logic.str(a, "legalName")) aId else bId
                            }
                            OutlinedButton(onClick = { winner = if (winner == aId) bId else aId }) {
                                Text("Flip survivor")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val loser = if (winner == aId) bId else aId
                                confirmPair = winner to loser
                            }) { Text("Merge") }
                            TextButton(onClick = { nav.navigate("client/$aId") }) { Text("Open A") }
                            TextButton(onClick = { nav.navigate("client/$bId") }) { Text("Open B") }
                        }
                    }
                }
                ActionErrorText(error)
                confirmPair?.let { (winnerId, loserId) ->
                    ConfirmGate(
                        "Merge clients",
                        "Merge the duplicate into the survivor? The duplicate record is " +
                            "archived and every linked record moves to the survivor.",
                        "Merge",
                        onDismiss = { confirmPair = null },
                        onConfirm = {
                            confirmPair = null
                            scope.launch {
                                try {
                                    val out = viewModel.api.mergeClients(winnerId, loserId)
                                    notice = Phase3Logic.str(out, "message")
                                    state.reloadKey++
                                } catch (e: Exception) {
                                    error = plainError(e, "The merge could not be done. Try again.")
                                }
                            }
                        },
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ---------- Sentinel Verified register ----------

@Composable
fun VerifiedScreen(viewModel: AppViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var issueClient by remember { mutableStateOf<Int?>(null) }
    var issueCategory by remember { mutableStateOf("") }
    var confirmIssue by remember { mutableStateOf(false) }
    var confirmRevoke by remember { mutableStateOf<Int?>(null) }
    val state = rememberJsonScreen { viewModel.api.verifications() }
    var clients by remember { mutableStateOf<List<net.sentinelsafeguard.app.data.api.Client>>(emptyList()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        clients = try { viewModel.api.clients().items } catch (e: Exception) { emptyList() }
    }
    val today = remember { java.time.LocalDate.now().toString() }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        TextButton(onClick = { nav.popBackStack() }) { Text("← Back to directory") }
        MirrorHeader("Sentinel Verified — register")
        when {
            state.error != null && state.data == null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList(4)
            else -> {
                val data = state.data!!
                val verifications = Phase3Logic.arr(data, "verifications")
                val categories = Phase3Logic.arrRaw(data, "categories")
                    .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                if (issueCategory.isBlank() && categories.isNotEmpty()) {
                    issueCategory = categories.first()
                }
                if (notice != null) {
                    Text(notice!!, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
                MirrorCard("Verifications", verifications.size) {
                    if (verifications.isEmpty()) {
                        Text("No verifications issued yet.", color = SentinelMuted)
                    }
                    verifications.forEach { v ->
                        val vid = Phase3Logic.int(v, "id")
                        Text(
                            "${Phase3Logic.str(v, "businessName")} · ${Phase3Logic.str(v, "code")}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            "${Phase3Logic.str(v, "category")} · ${Phase3Logic.verificationStatus(v, today)} · " +
                                "issued ${Phase3Logic.str(v, "issuedDate")} · expires ${Phase3Logic.str(v, "expiryDate")}",
                            color = SentinelMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (Phase3Logic.verificationStatus(v, today) != "Revoked" && vid != null) {
                            TextButton(onClick = { confirmRevoke = vid }) { Text("Revoke") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        scope.launch {
                            try {
                                val out = viewModel.api.exportVerifications()
                                shareText(context, "Sentinel Verified — public list", out.toString())
                                notice = Phase3Logic.str(out, "message")
                            } catch (e: Exception) {
                                error = plainError(e, "The export could not be created. Try again.")
                            }
                        }
                    }) { Text("Export public verification list") }
                }
                MirrorCard("Issue verification") {
                    if (clients.isEmpty()) {
                        Text("Loading clients…", color = SentinelMuted)
                    } else {
                        val selected = clients.firstOrNull { it.id == issueClient } ?: clients.first()
                        ChoiceMenu(
                            "Client",
                            selected.legalName,
                            clients.map { it.legalName },
                        ) { picked ->
                            issueClient = clients.firstOrNull { it.legalName == picked }?.id
                        }
                    }
                    if (categories.isNotEmpty()) {
                        ChoiceMenu("Category", issueCategory, categories) { issueCategory = it }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        enabled = clients.isNotEmpty() && issueCategory.isNotBlank(),
                        onClick = { confirmIssue = true },
                    ) { Text("Issue verification") }
                    ActionErrorText(error)
                }
                if (confirmIssue) {
                    ConfirmGate(
                        "Issue verification",
                        "Issue this Sentinel Verified verification? It appears on the " +
                            "public register with its category and dates.",
                        "Issue verification",
                        onDismiss = { confirmIssue = false },
                        onConfirm = {
                            confirmIssue = false
                            val cid = issueClient ?: clients.firstOrNull()?.id
                            if (cid != null) scope.launch {
                                try {
                                    val out = viewModel.api.issueVerification(cid, issueCategory)
                                    notice = Phase3Logic.str(out, "message")
                                    state.reloadKey++
                                } catch (e: Exception) {
                                    error = plainError(e, "The verification could not be issued. Try again.")
                                }
                            }
                        },
                    )
                }
                confirmRevoke?.let { vid ->
                    ConfirmGate(
                        "Revoke verification",
                        "Revocation requires confirmation — it is permanent and cannot be undone.",
                        "Revoke",
                        onDismiss = { confirmRevoke = null },
                        onConfirm = {
                            confirmRevoke = null
                            scope.launch {
                                try {
                                    val out = viewModel.api.revokeVerification(vid, confirm = true)
                                    notice = Phase3Logic.str(out, "message")
                                    state.reloadKey++
                                } catch (e: Exception) {
                                    error = plainError(e, "The verification could not be revoked. Try again.")
                                }
                            }
                        },
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ---------- Firm Pulse ----------

@Composable
fun FirmPulseScreen(viewModel: AppViewModel, nav: NavController) {
    var drill by remember { mutableStateOf<JsonObject?>(null) }
    var drillTitle by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val state = rememberJsonScreen { viewModel.api.firmPulse() }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        TextButton(onClick = { nav.popBackStack() }) { Text("← Back") }
        MirrorHeader("Firm Pulse")
        when {
            state.error != null && state.data == null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList(4)
            else -> {
                val pulse = state.data!!
                MirrorCard("Aging") {
                    Phase3Logic.arr(pulse, "aging").forEach { bucket ->
                        ItemRow(
                            Phase3Logic.str(bucket, "bucket"),
                            MirrorVocab.formatCents(Phase3Logic.long(bucket, "totalCents") ?: 0),
                        ) {
                            scope.launch {
                                try {
                                    drillTitle = "Aging · ${Phase3Logic.str(bucket, "bucket")}"
                                    drill = viewModel.api.firmPulseDrilldown(
                                        "aging", Phase3Logic.str(bucket, "bucket"),
                                    )
                                } catch (e: Exception) {
                                    // Drill-down failure leaves the pulse readable.
                                }
                            }
                        }
                    }
                }
                MirrorCard("Pipeline & plans") {
                    val pipeline = Phase3Logic.obj(pulse, "pipeline")
                    if (pipeline != null) {
                        DetailLine("Pipeline", MirrorVocab.formatCents(Phase3Logic.long(pipeline, "totalCents") ?: 0))
                    }
                    val mrr = Phase3Logic.obj(pulse, "planMrr")
                    if (mrr != null) {
                        DetailLine("Plan MRR", MirrorVocab.formatCents(Phase3Logic.long(mrr, "totalCents") ?: 0))
                    }
                    Phase3Logic.arr(pulse, "revenueByMonth").forEach { row ->
                        Text(
                            "${Phase3Logic.str(row, "month")} · ${MirrorVocab.formatCents(Phase3Logic.long(row, "totalCents") ?: 0)}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
                MirrorCard("Posture distribution") {
                    Phase3Logic.arr(pulse, "postureDistribution").forEach { band ->
                        Text(
                            "${Phase3Logic.str(band, "band")} · ${Phase3Logic.str(band, "count")} client(s)",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
                MirrorCard("Ticket throughput") {
                    Phase3Logic.arr(pulse, "ticketThroughput").forEach { week ->
                        Text(
                            "${Phase3Logic.str(week, "weekStart")} · opened ${Phase3Logic.str(week, "opened")} · closed ${Phase3Logic.str(week, "closed")}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
                drill?.let { d ->
                    MirrorCard(drillTitle.ifBlank { "Drill-down" }) {
                        DetailLine(
                            "Total",
                            MirrorVocab.formatCents(Phase3Logic.long(d, "totalCents") ?: 0),
                        )
                        val records = Phase3Logic.arr(d, "records")
                        if (records.isEmpty()) Text("No records in this figure.", color = SentinelMuted)
                        records.forEach { record ->
                            Text(
                                listOfNotNull(
                                    Phase3Logic.str(record, "invoiceNumber").takeIf { it.isNotBlank() }
                                        ?: Phase3Logic.str(record, "legalName").takeIf { it.isNotBlank() }
                                        ?: Phase3Logic.str(record, "title").takeIf { it.isNotBlank() },
                                    Phase3Logic.displayValue("totalCents", record["totalCents"]),
                                    Phase3Logic.str(record, "status").takeIf { it.isNotBlank() },
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/**
 * hunt17 (final contract census): the client's vault files —
 * expansion item 36's list, with each file opening through the
 * form-files download route (shared as a file, like every other
 * document surface in the app).
 */
@Composable
private fun ClientFilesCard(viewModel: AppViewModel, clientId: Int) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    AsyncCard(
        "Client files", "files",
        loader = { viewModel.api.clientFiles(clientId) },
    ) { data, _ ->
        val files = Phase3Logic.arr(data, "files")
        if (files.isEmpty()) Text("No files in this client's vault.", color = SentinelMuted)
        files.forEach { file ->
            val fid = Phase3Logic.int(file, "id")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Phase3Logic.str(file, "displayName").ifBlank {
                        Phase3Logic.str(file, "originalFilename")
                    },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = {
                    if (fid != null) scope.launch {
                        try {
                            val bytes = viewModel.api.downloadAll(
                                "/api/app/form-files/$fid/download",
                            )
                            shareBytes(
                                context,
                                Phase3Logic.str(file, "originalFilename")
                                    .ifBlank { "client-file-$fid" },
                                "application/octet-stream",
                                bytes,
                            )
                        } catch (e: Exception) {
                            error = plainError(e, "The file could not be opened. Try again.")
                        }
                    }
                }) { Text("Open") }
            }
        }
        ActionErrorText(error)
    }
}
