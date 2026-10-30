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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.sentinelsafeguard.app.data.mail.MailFolder
import net.sentinelsafeguard.app.data.mail.MailMessage
import net.sentinelsafeguard.app.data.mail.MailRules
import net.sentinelsafeguard.app.data.mail.MailSummary
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

private sealed interface MailRoute {
    data object Folders : MailRoute
    data class Messages(val folder: String) : MailRoute
    data class Read(val folder: String, val uid: Long) : MailRoute
}

/**
 * Mail (spec §4): skyler@ plus the shared sentinel@ folders, straight
 * over IMAP/SMTP. Triage-first: Archive (keeps a message for good),
 * Delete, mark unread, reply, basic search.
 */
@Composable
fun MailScreen(viewModel: AppViewModel) {
    if (!viewModel.mailConnected) {
        MailSignIn(viewModel)
        return
    }
    var route by remember { mutableStateOf<MailRoute>(MailRoute.Folders) }
    when (val r = route) {
        MailRoute.Folders -> FolderList(viewModel) { route = MailRoute.Messages(it) }
        is MailRoute.Messages -> MessageList(
            viewModel, r.folder,
            onBack = { route = MailRoute.Folders },
            onOpen = { uid -> route = MailRoute.Read(r.folder, uid) },
        )
        is MailRoute.Read -> MessageRead(
            viewModel, r.folder, r.uid,
            onBack = { route = MailRoute.Messages(r.folder) },
        )
    }
}

@Composable
private fun MailSignIn(viewModel: AppViewModel) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf(MailRules.PERSONAL_ADDRESS) }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }

    Column(
        Modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Connect your mail", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Enter your Sentinel mailbox password. It is stored on this phone only, locked with the phone's secure storage, and is only ever sent to the mail server.",
            color = SentinelMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Mailbox address") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mailbox password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !busy && username.isNotBlank() && password.isNotEmpty(),
            onClick = {
                busy = true
                error = null
                scope.launch {
                    try {
                        // Prove the password against the real server before saving it.
                        viewModel.mail.listFolders(username.trim(), password)
                        viewModel.saveMailCredentials(username.trim(), password)
                        done = true
                    } catch (e: Exception) {
                        error = "That password did not work for this mailbox. Check it and try again. " +
                            "If you changed it in webmail, use the new one."
                    } finally {
                        busy = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (busy) "Checking…" else "Connect mail") }
        if (done) {
            Spacer(Modifier.height(12.dp))
            Text("Connected. Your folders are ready — tap Mail again if this screen does not refresh.")
        }
    }
}

@Composable
private fun FolderList(viewModel: AppViewModel, onOpen: (String) -> Unit) {
    var folders by remember { mutableStateOf<List<MailFolder>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    val username = viewModel.secure.mailUsername!!
    val password = viewModel.secure.mailPassword!!

    LaunchedEffect(reloadKey) {
        error = null
        try {
            folders = viewModel.mail.listFolders(username, password)
        } catch (e: Exception) {
            error = "Mail could not be loaded. The mail server may be unreachable, or the mailbox password changed. " +
                "Check your connection and try again."
        }
    }

    Column {
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            folders == null -> LoadingBox("Loading mail folders…")
            else -> LazyColumn {
                items(folders!!) { folder ->
                    ItemRow(
                        title = folder.name.replace(MailRules.SHARED_PREFIX, "Shared business mail"),
                        subtitle = if (folder.shared) "Business mail — replies send as ${MailRules.SHARED_ADDRESS}"
                        else "Your mailbox",
                        badge = if (folder.unreadCount > 0) "${folder.unreadCount} new" else null,
                    ) { onOpen(folder.name) }
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Nothing here deletes itself — messages stay until you delete them.",
                        color = SentinelMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun MessageList(
    viewModel: AppViewModel,
    folder: String,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    var messages by remember { mutableStateOf<List<MailSummary>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var reloadKey by remember { mutableStateOf(0) }
    val username = viewModel.secure.mailUsername!!
    val password = viewModel.secure.mailPassword!!

    LaunchedEffect(folder, reloadKey) {
        error = null
        messages = null
        try {
            messages = viewModel.mail.listMessages(username, password, folder)
        } catch (e: Exception) {
            error = "This folder could not be loaded. Check your connection and try again."
        }
    }

    Column {
        TextButton(onClick = onBack) { Text("‹ All folders") }
        Text(
            folder.replace(MailRules.SHARED_PREFIX, "Shared business mail"),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search this folder") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(
                enabled = query.isNotBlank(),
                onClick = {
                    messages = null
                    viewModelScopeSearch(viewModel, username, password, folder, query) {
                        messages = it
                    }
                },
            ) { Text("Search") }
        }
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            messages == null -> LoadingBox("Loading messages…")
            messages!!.isEmpty() -> EmptyBox("No messages", "This folder is empty.")
            else -> LazyColumn {
                items(messages!!) { message ->
                    ItemRow(
                        title = message.subject,
                        subtitle = "${message.from} · ${message.dateText}",
                        badge = if (message.unread) "New" else null,
                    ) { onOpen(message.uid) }
                }
            }
        }
    }
}

private fun viewModelScopeSearch(
    viewModel: AppViewModel,
    username: String,
    password: String,
    folder: String,
    query: String,
    onResult: (List<MailSummary>) -> Unit,
) {
    kotlinx.coroutines.MainScope().launch {
        val results = try {
            viewModel.mail.search(username, password, folder, query)
        } catch (e: Exception) {
            emptyList()
        }
        onResult(results)
    }
}

@Composable
private fun MessageRead(
    viewModel: AppViewModel,
    folder: String,
    uid: Long,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<MailMessage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var replyText by remember { mutableStateOf("") }
    var actionNote by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    val username = viewModel.secure.mailUsername!!
    val password = viewModel.secure.mailPassword!!
    val identity = MailRules.identityForFolder(folder, username)

    LaunchedEffect(folder, uid, reloadKey) {
        error = null
        try {
            message = viewModel.mail.getMessage(username, password, folder, uid)
        } catch (e: Exception) {
            error = e.message ?: "This message could not be opened. Go back and try again."
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        TextButton(onClick = onBack) { Text("‹ Back to messages") }
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            message == null -> LoadingBox("Opening message…")
            else -> {
                val m = message!!
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(m.subject, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text("From ${m.from}", color = SentinelMuted)
                    Text(m.dateText, color = SentinelMuted)
                    Spacer(Modifier.height(14.dp))
                    Text(m.body, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            scope.launch {
                                try {
                                    viewModel.mail.archive(username, password, folder, uid)
                                    actionNote = "Archived. This message is kept for good."
                                } catch (e: Exception) {
                                    actionNote = "Archive did not work. Check your connection and try again."
                                }
                            }
                        }) { Text("Archive") }
                        OutlinedButton(onClick = {
                            scope.launch {
                                try {
                                    viewModel.mail.markUnread(username, password, folder, uid)
                                    actionNote = "Marked as unread."
                                } catch (e: Exception) {
                                    actionNote = "That did not work. Check your connection and try again."
                                }
                            }
                        }) { Text("Mark unread") }
                        OutlinedButton(onClick = {
                            scope.launch {
                                try {
                                    viewModel.mail.delete(username, password, folder, uid)
                                    onBack()
                                } catch (e: Exception) {
                                    actionNote = "Delete did not work. Check your connection and try again."
                                }
                            }
                        }) { Text("Delete") }
                    }
                    if (actionNote != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(actionNote!!, color = SentinelMuted)
                    }

                    Spacer(Modifier.height(24.dp))
                    Text("Reply", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "This reply sends as $identity.",
                        color = SentinelMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("Your reply") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        enabled = replyText.isNotBlank(),
                        onClick = {
                            val body = replyText.trim()
                            scope.launch {
                                try {
                                    viewModel.mail.reply(username, password, folder, m, body)
                                    replyText = ""
                                    actionNote = "Reply sent as $identity."
                                } catch (e: Exception) {
                                    actionNote = "The reply could not be sent. Check your connection and try again."
                                }
                            }
                        },
                    ) { Text("Send reply") }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}
