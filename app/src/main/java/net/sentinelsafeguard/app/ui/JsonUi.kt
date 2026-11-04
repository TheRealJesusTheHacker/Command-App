package net.sentinelsafeguard.app.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import net.sentinelsafeguard.app.data.api.SentinelApi
import net.sentinelsafeguard.app.ui.theme.SentinelMuted
import java.io.File

/**
 * Shared building blocks for the Phase 3 open-JSON surfaces.
 * Each Phase 3 card loads its own endpoint independently — one
 * failing panel can never take down its screen (the Today-panel
 * discipline from hunt6).
 */

/** A MirrorCard whose body is one endpoint's payload. [content]
 *  receives the payload and a reload trigger (post-action refresh). */
@Composable
fun AsyncCard(
    title: String,
    countKey: String? = null,
    loader: suspend () -> JsonObject,
    content: @Composable (JsonObject, () -> Unit) -> Unit,
) {
    var data by remember { mutableStateOf<JsonObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    LaunchedEffect(reloadKey) {
        error = null
        try {
            data = loader()
        } catch (e: Exception) {
            error = plainError(e, "This section could not be loaded. Try again.")
        }
    }
    val count = countKey?.let { key ->
        data?.let { Phase3Logic.arr(it, key).size }
    }
    MirrorCard(title, count) {
        when {
            // hunt16: a reload that fails mid-card keeps the
            // already-loaded content visible and shows the
            // failure as a line + Retry above it — never a
            // full-card replacement for data already on screen.
            data != null -> {
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = { reloadKey++ }) { Text("Retry") }
                    Spacer(Modifier.height(6.dp))
                }
                content(data!!) { reloadKey++ }
            }
            error != null -> {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { reloadKey++ }) { Text("Retry") }
            }
            // hunt16: the established skeleton pattern, not a
            // bare "Loading…" line (hunt4 premium standard).
            else -> SkeletonLines(3)
        }
    }
}

/** DetailLine rows for a payload's fields, in the given order,
 *  with the payload's own labels (web field order preserved by
 *  the caller). Blank / absent fields render nothing, as web's
 *  detail grids do. */
@Composable
fun JsonLines(obj: JsonObject, vararg fields: Pair<String, String>) {
    fields.forEach { (key, label) ->
        DetailLine(label, Phase3Logic.displayValue(key, obj[key]))
    }
}

/** Runs an action, then reloads (or reports the refusal in
 *  web's own words — ApiException already carries the server
 *  message for gate/confirm refusals). */
@Composable
fun rememberAction(
    onError: (String) -> Unit,
    onDone: () -> Unit,
): (suspend () -> Unit) -> Unit {
    val scope = rememberCoroutineScope()
    return { block ->
        scope.launch {
            try {
                block()
                onDone()
            } catch (e: Exception) {
                onError(plainError(e, "That could not be done. Try again."))
            }
        }
    }
}

/** Inline action-error line used across the Phase 3 cards. */
@Composable
fun ActionErrorText(error: String?) {
    if (error != null) {
        Spacer(Modifier.height(6.dp))
        Text(error, color = MaterialTheme.colorScheme.error)
    }
}

/** A web-style confirm gate: the dialog states the consequence
 *  in web's words; confirming runs the action (which sends the
 *  backend's confirm flag where the endpoint requires one). */
@Composable
fun ConfirmGate(
    title: String,
    body: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            Button(onClick = { onConfirm() }) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/** A labeled text field — the Phase 3 form row.
 *  hunt16: the keyboard follows the field's meaning (email /
 *  phone / number / decimal pads inferred from the label by
 *  [InteractionLogic.fieldKindForLabel], overridable via
 *  [kind]) and the IME action advances instead of dismissing. */
@Composable
fun TextFieldRow(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    singleLine: Boolean = true,
    minLines: Int = 1,
    kind: InteractionLogic.FieldKind? = null,
) {
    val effective = kind ?: InteractionLogic.fieldKindForLabel(label)
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = when (effective) {
                InteractionLogic.FieldKind.EMAIL ->
                    androidx.compose.ui.text.input.KeyboardType.Email
                InteractionLogic.FieldKind.PHONE ->
                    androidx.compose.ui.text.input.KeyboardType.Phone
                InteractionLogic.FieldKind.NUMBER ->
                    androidx.compose.ui.text.input.KeyboardType.Number
                InteractionLogic.FieldKind.DECIMAL ->
                    androidx.compose.ui.text.input.KeyboardType.Decimal
                InteractionLogic.FieldKind.TEXT ->
                    androidx.compose.ui.text.input.KeyboardType.Text
            },
            imeAction = androidx.compose.ui.text.input.ImeAction.Next,
            capitalization = if ("name" in label.lowercase()) {
                androidx.compose.ui.text.input.KeyboardCapitalization.Words
            } else {
                androidx.compose.ui.text.input.KeyboardCapitalization.Sentences
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    )
}

/**
 * A date field (hunt16, Polish Pass 2 item 6a): the owner picks
 * the date — never types yyyy-MM-dd. The field shows the pretty
 * date; tapping it (or its calendar button) opens the Material 3
 * date picker that ships INSIDE the already-locked material3
 * 1.3.1 artifact, so this needed no new dependency and no Gradle
 * relock. The value on the wire stays the backend's exact
 * yyyy-MM-dd ([InteractionLogic] converts both ways). A stored
 * value that does not parse passes through visibly, untouched.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DateFieldRow(
    label: String,
    value: String,
    onChange: (String) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val tick = rememberHapticTick()
    OutlinedTextField(
        value = InteractionLogic.prettyYmd(value),
        onValueChange = { },
        label = { Text(label) },
        singleLine = true,
        readOnly = true,
        trailingIcon = {
            Row {
                if (value.isNotBlank()) {
                    androidx.compose.material3.TextButton(onClick = { onChange("") }) {
                        Text("Clear")
                    }
                }
                androidx.compose.material3.IconButton(onClick = { showPicker = true }) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Filled.DateRange,
                        contentDescription = "Pick a date for $label",
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    )
    if (showPicker) {
        val pickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = InteractionLogic.pickerMillisFor(value),
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onChange(InteractionLogic.pickerMillisToYmd(millis))
                    }
                    tick()
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) {
            androidx.compose.material3.DatePicker(state = pickerState)
        }
    }
}

/** A labeled switch row (notification prefs, active flags). */
@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Share text through Android's share sheet (JSON exports,
 *  rendered documents, posture letters — Android norms). */
fun shareText(context: Context, subject: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, subject))
}

/** Share bytes as a file (PDF documents, photos, voice notes)
 *  through the app's FileProvider cache share path. */
fun shareBytes(context: Context, fileName: String, mimeType: String, bytes: ByteArray) {
    // file_paths.xml exposes only the "updates" cache path to the
    // FileProvider; shares ride that same exposed cache directory.
    val dir = File(context.cacheDir, "updates").apply { mkdirs() }
    val file = File(dir, fileName)
    file.writeBytes(bytes)
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context, context.packageName + ".files", file,
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, fileName))
}

/** Collect a binary download fully in memory (photos, diagrams —
 *  all server-capped at sizes a phone holds comfortably). */
suspend fun SentinelApi.downloadAll(path: String): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    downloadBytes(path) { chunk -> out.write(chunk) }
    return out.toByteArray()
}

/** A small self-contained loader for full screens (not cards):
 *  returns (data, error, reload) as a triple of state holders. */
class JsonScreenState {
    var data by mutableStateOf<JsonObject?>(null)
    var error by mutableStateOf<String?>(null)
    var reloadKey by mutableStateOf(0)
}

@Composable
fun rememberJsonScreen(loader: suspend () -> JsonObject): JsonScreenState {
    val state = remember { JsonScreenState() }
    LaunchedEffect(state.reloadKey) {
        state.error = null
        try {
            state.data = loader()
        } catch (e: Exception) {
            state.error = plainError(e, "This could not be loaded. Check your connection and try again.")
        }
    }
    return state
}

/** The section note for a payload `message` line, when present. */
@Composable
fun MessageLine(obj: JsonObject) {
    val msg = Phase3Logic.str(obj, "message")
    if (msg.isNotBlank()) {
        Text(msg, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))
    }
}
