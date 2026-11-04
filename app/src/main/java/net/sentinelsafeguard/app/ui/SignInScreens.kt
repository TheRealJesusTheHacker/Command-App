package net.sentinelsafeguard.app.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import net.sentinelsafeguard.app.data.api.LoginResult
import net.sentinelsafeguard.app.ui.theme.SentinelDanger
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Enrollment sign-in (spec §2.1): the same display name + passphrase as
 * the web sign-in. The code field appears only when the server asks
 * for it (the account has the extra code step turned on).
 */
@Composable
fun SignInScreen(viewModel: AppViewModel) {
    var displayName by remember { mutableStateOf(viewModel.secure.displayName ?: "") }
    var passphrase by remember { mutableStateOf("") }
    var totpCode by remember { mutableStateOf("") }
    var needsTotp by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val totpFocus = remember { FocusRequester() }
    // hunt16: when the second leg appears, the keyboard lands on
    // the code field — no hunting for the field mid-sign-in.
    LaunchedEffect(needsTotp) {
        if (needsTotp) {
            try {
                totpFocus.requestFocus()
            } catch (ignored: Exception) {
                // Focus is garnish; the field is on screen either way.
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "SENTINEL SAFEGUARD",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text("Sign in", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Sign in with the same name and passphrase you use for Sentinel Command.",
            color = SentinelMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))

        // hunt16: an expired session returns here WITH its plain
        // explanation (set by the shell when a 401 ended it).
        val notice = viewModel.sessionNotice
        if (notice != null && !needsTotp) {
            Text(notice, color = SentinelDanger, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = passphrase,
            onValueChange = { passphrase = it },
            label = { Text("Passphrase") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        if (needsTotp) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Your account also asks for the 6-digit code from your authenticator app.",
                color = SentinelMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = totpCode,
                onValueChange = { totpCode = it.filter { c -> c.isDigit() }.take(6) },
                label = { Text("6-digit code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().focusRequester(totpFocus),
            )
        }

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error!!, color = SentinelDanger, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                busy = true
                error = null
                val callback: (LoginResult) -> Unit = { result ->
                    busy = false
                    when (result) {
                        is LoginResult.Success -> Unit // shell switches screens
                        is LoginResult.TotpRequired -> {
                            needsTotp = true
                            error = null
                        }
                        is LoginResult.Failed -> {
                            error = when {
                                needsTotp -> {
                                    // hunt16: a failed code clears
                                    // for a clean retype and says
                                    // how to recover — the leg
                                    // stays open, Start over below
                                    // restarts the whole sign-in.
                                    totpCode = ""
                                    InteractionLogic.TOTP_RETRY_MESSAGE
                                }
                                result.message.contains("passphrase", true) ||
                                    result.message.contains("401") ->
                                    "That name and passphrase did not match. Check them and try again."
                                else -> result.message
                            }
                        }
                    }
                }
                if (needsTotp) {
                    // Second leg answers the server challenge with
                    // the typed code; the passphrase is not resent.
                    viewModel.submitTotpCode(totpCode, callback)
                } else {
                    viewModel.signIn(displayName.trim(), passphrase, callback)
                }
            },
            enabled = !busy && displayName.isNotBlank() && passphrase.isNotEmpty() &&
                (!needsTotp || totpCode.length == 6),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (busy) "Signing in…" else "Sign in")
        }
        if (needsTotp) {
            TextButton(onClick = {
                // Start over: drop the challenge and the typed
                // secrets, back to a fresh first leg.
                needsTotp = false
                totpCode = ""
                passphrase = ""
                error = null
            }) { Text("Start over") }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "This phone is enrolled as its own device. You can revoke it any time from Settings here, or from Sentinel Command on the web.",
            color = SentinelMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * The lock screen (spec §5): fingerprint unlocks; the passphrase
 * fallback signs out to the sign-in screen for a fresh enrollment.
 */
@Composable
fun LockScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }

    fun promptBiometric() {
        val activity = context as? FragmentActivity ?: return
        val manager = BiometricManager.from(context)
        val can = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            error = "Fingerprint unlock is not set up on this phone. Use the passphrase instead."
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.unlock()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        error = "Unlock did not work: $errString Try again, or use the passphrase."
                    }
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Sentinel App")
            .setSubtitle("Use your fingerprint to continue")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(Unit) { promptBiometric() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Sentinel App is locked", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your sign-in is still saved on this phone. Unlock to continue.",
            color = SentinelMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error!!, color = SentinelDanger, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { promptBiometric() }, modifier = Modifier.fillMaxWidth()) {
            Text("Unlock with fingerprint")
        }
        TextButton(onClick = { viewModel.signOut { } }) {
            Text("Use passphrase instead (signs this device out first)")
        }
    }
}
