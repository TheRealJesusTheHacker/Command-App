package net.sentinelsafeguard.app.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import net.sentinelsafeguard.app.BuildConfig
import net.sentinelsafeguard.app.data.api.DeviceInfo
import net.sentinelsafeguard.app.notify.QuietHours
import net.sentinelsafeguard.app.ui.theme.SentinelDanger
import net.sentinelsafeguard.app.ui.theme.SentinelMuted
import net.sentinelsafeguard.app.ui.theme.SentinelOutline
import net.sentinelsafeguard.app.ui.theme.SentinelSurface
import net.sentinelsafeguard.app.ui.theme.SentinelSurfaceTop
import net.sentinelsafeguard.app.update.UpdateState
import java.security.MessageDigest

/**
 * Settings (spec §2.1): fingerprint unlock, quiet hours, notification
 * switches, this-device info, sign out (revokes this device), and the
 * app version + update check. App-only by design (spec §2.2):
 * Command has no equivalent view; hunt5 removed the hunt4
 * embedded-browser entries — the app IS Command now.
 */
@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onSignedOut: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = viewModel.settings

    var biometric by remember { mutableStateOf(settings.biometricEnabled) }
    var notifyTickets by remember { mutableStateOf(settings.notifyTickets) }
    var notifyMail by remember { mutableStateOf(settings.notifyMail) }
    var notifyAlerts by remember { mutableStateOf(settings.notifySignInAlerts) }
    var quietStart by remember { mutableStateOf(QuietHours.toHhMm(settings.quietStartMinutes)) }
    var quietEnd by remember { mutableStateOf(QuietHours.toHhMm(settings.quietEndMinutes)) }
    var quietError by remember { mutableStateOf<String?>(null) }

    var devices by remember { mutableStateOf<List<DeviceInfo>?>(null) }
    var accountName by remember { mutableStateOf<String?>(null) }
    var confirmSignOut by remember { mutableStateOf(false) }

    var updateBusy by remember { mutableStateOf(false) }
    var updateNote by remember { mutableStateOf<String?>(null) }
    // Icon Morph Program (2026-11-07): M-18 (b) download
    // story-arc phase for the update download below — IDLE at
    // rest, ACTIVE while the APK streams (indeterminate: the
    // transport reports no byte total to this layer), DONE
    // when the download actually completes.
    var updatePhase by remember { mutableStateOf(DownloadPhase.IDLE) }

    LaunchedEffect(Unit) {
        try {
            accountName = viewModel.api.me().user.displayName
        } catch (ignored: Exception) {
        }
        try {
            devices = viewModel.api.devices()
        } catch (ignored: Exception) {
        }
    }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        SectionHeader("This device")
        // hunt18: the identity block becomes a SurfaceTop card
        // with a monogram disc — same strings, same order.
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, SentinelOutline),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(sentinelCardBrush(SentinelSurfaceTop), RoundedCornerShape(16.dp)),
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonogramDisc(accountName ?: viewModel.secure.displayName ?: "you", 40.dp)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        "Signed in as ${accountName ?: viewModel.secure.displayName ?: "you"} on " +
                            "${Build.MANUFACTURER} ${Build.MODEL}.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    devices?.firstOrNull { it.current }?.let { device ->
                        Text(
                            "Device name: ${device.deviceName}" +
                                (if (device.createdAt > 0) "\nAdded: ${formatEpochMs(device.createdAt)}" else "") +
                                (if (device.lastSeenAt > 0) "\nLast used: ${formatEpochMs(device.lastSeenAt)}" else ""),
                            color = SentinelMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        SectionHeader("Unlock")
        ToggleRow(
            "Fingerprint unlock",
            "Ask for your fingerprint when the app has been closed or in the background for about 5 minutes.",
            biometric,
        ) { picked ->
            biometric = picked
            settings.biometricEnabled = picked
        }

        SectionHeader("Notifications")
        Text(
            "The app checks for new tickets and mail about every 15 minutes.",
            color = SentinelMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        ToggleRow("New tickets", null, notifyTickets) {
            notifyTickets = it; settings.notifyTickets = it
        }
        ToggleRow("New mail", null, notifyMail) {
            notifyMail = it; settings.notifyMail = it
        }
        ToggleRow(
            "Sign-in alerts",
            "Someone signed in to Sentinel Command. These come through even during quiet hours.",
            notifyAlerts,
        ) {
            notifyAlerts = it; settings.notifySignInAlerts = it
        }

        Spacer(Modifier.height(12.dp))
        Text("Quiet hours", style = MaterialTheme.typography.titleMedium)
        Text(
            "Ticket and mail notifications stay silent during these hours. Sign-in alerts still come through.",
            color = SentinelMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = quietStart,
                onValueChange = { quietStart = it },
                label = { Text("From (24h)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = quietEnd,
                onValueChange = { quietEnd = it },
                label = { Text("To (24h)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        if (quietError != null) {
            Text(quietError!!, color = SentinelDanger, style = MaterialTheme.typography.bodyMedium)
        }
        OutlinedButton(onClick = {
            val start = QuietHours.parseTime(quietStart)
            val end = QuietHours.parseTime(quietEnd)
            if (start == null || end == null) {
                quietError = "Use times like 11:00 and 18:00."
            } else {
                settings.quietStartMinutes = start
                settings.quietEndMinutes = end
                quietError = null
                quietStart = QuietHours.toHhMm(start)
                quietEnd = QuietHours.toHhMm(end)
            }
        }) { Text("Save quiet hours") }
        Text(
            "Right now: silent from ${QuietHours.formatMinutes(settings.quietStartMinutes)} " +
                "to ${QuietHours.formatMinutes(settings.quietEndMinutes)}.",
            color = SentinelMuted,
            style = MaterialTheme.typography.bodyMedium,
        )

        SectionHeader("Mail")
        Text(
            "Messages stay in your mailbox until you delete them — nothing deletes itself. " +
                if (viewModel.secure.hasMailCredentials) {
                    "Mail is connected as ${viewModel.secure.mailUsername}."
                } else {
                    "Mail is not connected yet — open the Mail tab to connect it."
                },
            style = MaterialTheme.typography.bodyMedium,
        )

        SectionHeader("About this app")
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, SentinelOutline),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(sentinelCardBrush(SentinelSurface), RoundedCornerShape(16.dp)),
        ) {
            Column(Modifier.padding(16.dp)) {
                IdentityLine("App", "Sentinel App")
                IdentityLine("Version", viewModel.versionLabel.removePrefix("Sentinel App "))
                IdentityLine(
                    "Signed by",
                    "Sentinel Safeguard LLC — the owner's custodied release key",
                )
                signerFingerprint(context)?.let { fingerprint ->
                    Text(
                        fingerprint,
                        color = SentinelMuted,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                IdentityLine("This phone", "${Build.MANUFACTURER} ${Build.MODEL}")
                devices?.firstOrNull { it.current }?.let { device ->
                    IdentityLine("Device name", device.deviceName)
                }
            }
        }
        if (updateNote != null) {
            Text(updateNote!!, color = SentinelMuted, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            enabled = !updateBusy,
            onClick = {
                updateBusy = true
                updateNote = null
                updatePhase = DownloadPhase.IDLE
                scope.launch {
                    try {
                        when (val state = viewModel.updates.check()) {
                            UpdateState.UpToDate -> updateNote = "You have the newest version."
                            is UpdateState.Available -> {
                                updateNote = "Sentinel App ${state.info.version} is ready." +
                                    (state.info.notes.takeIf { it.isNotBlank() }?.let { " $it" } ?: "")
                                updatePhase = DownloadPhase.ACTIVE
                                val file = viewModel.updates.download(context)
                                updatePhase = DownloadPhase.DONE
                                viewModel.updates.install(context, file)
                            }
                        }
                    } catch (e: Exception) {
                        updatePhase = DownloadPhase.IDLE
                        updateNote = "The update check did not work. Check your connection and try again."
                    } finally {
                        updateBusy = false
                    }
                }
            },
        ) {
            // M-18 (b): the story-arc icon rides the button —
            // arrow at rest, ring while downloading, drawn
            // tick when the download completes.
            DownloadStoryIcon(phase = updatePhase, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.padding(start = 8.dp))
            Text(if (updateBusy) "Checking…" else "Check for updates")
        }

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = { confirmSignOut = true },
            colors = ButtonDefaults.buttonColors(containerColor = SentinelDanger),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Sign out") }
        Text(
            "Signing out revokes this phone's access right away. You will need your passphrase to sign in again.",
            color = SentinelMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(32.dp))
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out of this phone?") },
            text = {
                Text("This phone's access is revoked and its saved sign-in is wiped. Your account and mail are not changed.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmSignOut = false
                        viewModel.signOut(onSignedOut)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SentinelDanger),
                ) { Text("Sign out") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text("Stay signed in") }
            },
        )
    }
}

@Composable
private fun IdentityLine(label: String, value: String) {
    Text(label, color = SentinelMuted, style = MaterialTheme.typography.labelLarge)
    Text(value, style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(10.dp))
}

/**
 * The SHA-256 fingerprint of the certificate this installed copy
 * is actually signed with, read from the package manager — the
 * About block shows the real signer of the bits on the phone,
 * never a typed-in claim. Null if it cannot be read.
 */
private fun signerFingerprint(context: Context): String? = try {
    val packageManager = context.packageManager
    val packageName = context.packageName
    val certificates = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        info.signingInfo?.apkContentsSigners
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
    }
    val certificate = certificates?.firstOrNull()?.toByteArray()
    certificate?.let { bytes ->
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString(":") { "%02x".format(it) }
    }
} catch (e: Exception) {
    null
}

@Composable
private fun ToggleRow(
    title: String,
    body: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (!body.isNullOrBlank()) {
                Text(body, color = SentinelMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
