package net.sentinelsafeguard.app.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.sentinelsafeguard.app.BuildConfig
import net.sentinelsafeguard.app.data.api.DeviceInfo
import net.sentinelsafeguard.app.data.api.pushPrefs
import net.sentinelsafeguard.app.data.api.pushPrefsSet
import net.sentinelsafeguard.app.notify.NotifAllowAction
import net.sentinelsafeguard.app.notify.NotifPermission
import net.sentinelsafeguard.app.notify.NotifPermissionState
import net.sentinelsafeguard.app.notify.PushPrefsLogic
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

    // Do All 1.4.0 (item 6): the notification permission is only
    // ever requested from the explanation surface below (first
    // run shows the same explanation in AppShell) — never cold.
    // Hotfix 1.4.1: the surface now runs on the permission state
    // model (notify/PermissionState.kt). The platform's answers
    // are re-read on every resume — a grant made in Android
    // Settings takes effect here at once, no restart — and when
    // the permission is in its decided-denied state the card's
    // button opens Android's notification settings instead of
    // launching a prompt that will never appear.
    var notifPermissionGranted by remember {
        mutableStateOf(notificationsPermissionGranted(context))
    }
    var notifAsked by remember { mutableStateOf(settings.notifyPermissionAsked) }
    var notifRationale by remember { mutableStateOf(notificationsShowRationale(context)) }
    val notifState = NotifPermission.state(notifPermissionGranted, notifAsked, notifRationale)
    var showNotifyExplainer by remember {
        mutableStateOf(!settings.notifyPermissionExplainerDismissed)
    }
    OnResumeEffect {
        val grantedNow = notificationsPermissionGranted(context)
        notifPermissionGranted = grantedNow
        notifRationale = notificationsShowRationale(context)
        notifAsked = settings.notifyPermissionAsked
        // A grant reached via Android Settings clears the stale
        // dismissal too, so the granted line below is what shows.
        if (grantedNow) settings.notifyPermissionExplainerDismissed = false
    }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        settings.notifyPermissionAsked = true
        notifAsked = true
        notifPermissionGranted = granted
        notifRationale = notificationsShowRationale(context)
        if (granted) settings.notifyPermissionExplainerDismissed = false
    }
    var notifyCritical by remember { mutableStateOf(settings.notifyCritical) }

    // 1.5.2 (Elevation F4): the server-side push event
    // preferences. These are read from GET /api/app/push/prefs
    // on entry and written back per toggle — the values shown
    // are always the SERVER's, never a local copy that could
    // drift from Command on the web (one store, migration
    // 0169). A failed load shows the failure and offers no
    // toggles rather than rendering invented defaults.
    var serverPushPrefs by remember { mutableStateOf<PushPrefsLogic.Prefs?>(null) }
    var pushPrefsLoading by remember { mutableStateOf(true) }
    var pushPrefsError by remember { mutableStateOf<String?>(null) }
    var pushPrefsSaving by remember { mutableStateOf(false) }
    var pushPrefsReload by remember { mutableStateOf(0) }
    LaunchedEffect(pushPrefsReload) {
        pushPrefsLoading = true
        try {
            val parsed = PushPrefsLogic.parse(viewModel.api.pushPrefs())
            if (parsed == null) {
                pushPrefsError = "The server answered, but its push choices " +
                    "could not be read. Nothing is shown rather than a guess."
            } else {
                serverPushPrefs = parsed
                pushPrefsError = null
            }
        } catch (e: Exception) {
            pushPrefsError = plainError(
                e, "Your server push choices could not be loaded.")
        } finally {
            pushPrefsLoading = false
        }
    }

    fun onServerPushToggle(key: String, value: Boolean) {
        if (serverPushPrefs == null || pushPrefsSaving) return
        scope.launch {
            pushPrefsSaving = true
            try {
                val out = viewModel.api.pushPrefsSet(
                    PushPrefsLogic.writeBody(key, value))
                val parsed = PushPrefsLogic.parse(out)
                if (parsed == null) {
                    // The write may have landed; re-read the
                    // server's truth instead of guessing.
                    pushPrefsError = "That change could not be confirmed — " +
                        "reloading the server's values."
                    pushPrefsReload++
                } else {
                    serverPushPrefs = parsed
                    pushPrefsError = null
                }
            } catch (e: Exception) {
                pushPrefsError = plainError(
                    e, "That change could not be saved. " +
                        "The switches still show the server's values.")
            } finally {
                pushPrefsSaving = false
            }
        }
    }

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (notifPermissionGranted) {
                Text(
                    "Android notifications are on for this app.",
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
            } else if (showNotifyExplainer) {
                NotificationPermissionExplainerCard(
                    deniedFinal = notifState == NotifPermissionState.DENIED_FINAL,
                    onAllow = {
                        when (NotifPermission.allowAction(notifState)) {
                            NotifAllowAction.LAUNCH_SYSTEM_REQUEST ->
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            NotifAllowAction.OPEN_NOTIFICATION_SETTINGS ->
                                openAppNotificationSettings(context)
                        }
                    },
                    onNotNow = {
                        settings.notifyPermissionExplainerDismissed = true
                        showNotifyExplainer = false
                    },
                )
                Spacer(Modifier.height(8.dp))
            } else {
                Text(
                    "Android notifications are off for this app, so the alerts below " +
                        "cannot reach you until the permission is on.",
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = { showNotifyExplainer = true }) {
                    Text("Turn on notifications")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Push notifications", style = MaterialTheme.typography.titleMedium)
        Text(
            "These choices live on your Sentinel server and apply " +
                "everywhere you sign in — Command on the web shows the " +
                "same switches. They decide which events the server " +
                "sends to your devices at all. The switches further " +
                "down are this phone's own display choices and are " +
                "not changed by these.",
            color = SentinelMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(4.dp))
        when {
            pushPrefsLoading && serverPushPrefs == null -> Text(
                "Loading your server choices…",
                color = SentinelMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            serverPushPrefs == null -> {
                if (pushPrefsError != null) {
                    Text(
                        pushPrefsError!!,
                        color = SentinelDanger,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                OutlinedButton(onClick = { pushPrefsReload++ }) {
                    Text("Try again")
                }
            }
            else -> {
                val prefs = serverPushPrefs!!
                PushPrefsLogic.KEYS.forEach { key ->
                    ToggleRow(
                        PushPrefsLogic.label(key),
                        PushPrefsLogic.note(key),
                        prefs.value(key),
                        enabled = !pushPrefsSaving,
                    ) { picked -> onServerPushToggle(key, picked) }
                }
                if (pushPrefsError != null) {
                    Text(
                        pushPrefsError!!,
                        color = SentinelDanger,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
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
        ToggleRow(
            "Critical alerts",
            "A Critical Alert in Command, or a Sentinel site going down. " +
                "These come through even during quiet hours.",
            notifyCritical,
        ) {
            notifyCritical = it; settings.notifyCritical = it
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
                                val file = viewModel.updates.download(context, state.info)
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

/** True when Android will actually show this app's notifications:
 *  before Android 13 there is no runtime permission to ask for. */
fun notificationsPermissionGranted(context: Context): Boolean =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        true
    } else {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

/** The Activity behind a Compose context, or null when there is
 *  none. Every app screen is hosted by MainActivity; the chain
 *  is walked because LocalContext may hand back a wrapper. */
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Hotfix 1.4.1 — Android's own answer to "would the system
 * prompt still show?": true after a soft denial, false when
 * the permission sits in its decided-denied state (the state
 * where RequestPermission returns instantly and shows nothing).
 * Before Android 13 there is no prompt to reason about.
 */
fun notificationsShowRationale(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    val activity = context.findActivity() ?: return false
    return ActivityCompat.shouldShowRequestPermissionRationale(
        activity, Manifest.permission.POST_NOTIFICATIONS,
    )
}

/**
 * Hotfix 1.4.1 — opens Android's per-app notification settings,
 * the only place the switch exists once the permission is
 * decided-denied. Falls back to the app's details page (which
 * links to the same switch) when the direct screen is absent.
 */
fun openAppNotificationSettings(context: Context) {
    val direct = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(direct)
    } catch (ignored: Exception) {
        try {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (ignoredAgain: Exception) {
        }
    }
}

/**
 * Hotfix 1.4.1 — runs [onResume] on every ON_RESUME of the host
 * lifecycle. The 1.4.0 permission surfaces read the grant once
 * at composition, so a grant made in Android Settings kept
 * reporting "off" until the app restarted; this is the re-read.
 * (ON_RESUME also fires right after composition — re-reading
 * the same true state is harmless.)
 */
@Composable
fun OnResumeEffect(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnResume by rememberUpdatedState(onResume)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) latestOnResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/**
 * Do All 1.4.0 (item 6): what each notification channel is for,
 * in plain words — the body shared by the Settings card and the
 * first-run dialog. The privacy line is literally true of this
 * app: notifications are built on the phone from the app's own
 * poll of the owner's self-hosted Command + mailbox; nothing
 * leaves the phone and no third-party service is involved.
 */
@Composable
private fun NotificationExplainerBody(deniedFinal: Boolean = false) {
    Column {
        NotifyChannelLine(
            "New tickets",
            "A ticket lands, or a client replies to one.",
        )
        NotifyChannelLine(
            "New mail",
            "New messages in your Sentinel mailbox.",
        )
        NotifyChannelLine(
            "Sign-in alerts",
            "Someone signs in to Sentinel Command. These come through even during quiet hours.",
        )
        NotifyChannelLine(
            "Critical alerts",
            "A Critical Alert in Command, or a Sentinel site going down. These also come through during quiet hours.",
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Quiet hours hold the routine ones — tickets, mail, money, digests — " +
                "until the window ends. Sign-in alerts, critical alerts, and " +
                "estate-down alerts still come through.",
            color = SentinelMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Notifications are made on this phone. The app checks your own " +
                "Sentinel server about every 15 minutes — nothing leaves your " +
                "phone and no third party is involved.",
            color = SentinelMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (deniedFinal) {
            // Hotfix 1.4.1: the platform will not show its pop-up
            // for this app anymore, so say so plainly and point
            // at the one place the switch still lives.
            Spacer(Modifier.height(8.dp))
            Text(
                "Android has stopped showing the pop-up for this app — " +
                    "that can happen after an earlier no. The switch now " +
                    "lives in Android Settings, on this app's page. The " +
                    "button below opens it: turn Notifications on there, " +
                    "then come back — this screen updates itself.",
                color = SentinelMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun NotifyChannelLine(name: String, what: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        SectionTick()
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(what, color = SentinelMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** The explanation as a Settings card (top-tier Sentinel plate:
 *  SurfaceTop gradient, emerald gradient hairline, top highlight). */
@Composable
fun NotificationPermissionExplainerCard(
    onAllow: () -> Unit,
    onNotNow: () -> Unit,
    deniedFinal: Boolean = false,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, gradientHairlineBrush()),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(sentinelCardBrush(SentinelSurfaceTop), RoundedCornerShape(16.dp)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Notifications, in plain words", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            NotificationExplainerBody(deniedFinal)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onAllow) { Text(if (deniedFinal) "Open Settings" else "Allow") }
                OutlinedButton(onClick = onNotNow) { Text("Not now") }
            }
        }
    }
}

/** The same explanation as the first-run dialog (AppShell shows
 *  this before the system prompt, once, until it is answered). */
@Composable
fun NotificationPermissionExplainerDialog(
    onAllow: () -> Unit,
    onNotNow: () -> Unit,
    deniedFinal: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onNotNow,
        title = { Text("Notifications, in plain words") },
        text = { NotificationExplainerBody(deniedFinal) },
        confirmButton = {
            Button(onClick = onAllow) { Text(if (deniedFinal) "Open Settings" else "Allow") }
        },
        dismissButton = {
            TextButton(onClick = onNotNow) { Text("Not now") }
        },
    )
}

@Composable
private fun ToggleRow(
    title: String,
    body: String?,
    checked: Boolean,
    enabled: Boolean = true,
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
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
