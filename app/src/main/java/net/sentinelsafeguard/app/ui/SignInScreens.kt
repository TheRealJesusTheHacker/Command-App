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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import net.sentinelsafeguard.app.data.api.LoginResult
import net.sentinelsafeguard.app.ui.theme.SentinelDanger
import net.sentinelsafeguard.app.ui.theme.SentinelDangerBright
import net.sentinelsafeguard.app.ui.theme.SentinelMuted
import net.sentinelsafeguard.app.ui.theme.SentinelSurfaceLow

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
    // Motion pass (2026-11-04): M-13 — each failed leg bumps the
    // shake key; the form shakes, the error text stays exactly
    // as generic as it was.
    var shakeKey by remember { mutableStateOf(0) }
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
            .padding(24.dp)
            .errorShake(shakeKey),
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
            // hunt17 (accessibility): the notice and errors are
            // live regions — TalkBack announces them as they
            // appear instead of leaving the owner to discover
            // them by swiping.
            Text(
                notice,
                color = SentinelDangerBright,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics {
                    liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
                },
            )
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display name") },
            singleLine = true,
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = SentinelSurfaceLow,
                focusedContainerColor = SentinelSurfaceLow,
            ),
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
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = SentinelSurfaceLow,
                focusedContainerColor = SentinelSurfaceLow,
            ),
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
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = SentinelSurfaceLow,
                focusedContainerColor = SentinelSurfaceLow,
            ),
                modifier = Modifier.fillMaxWidth().focusRequester(totpFocus),
            )
        }

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                error!!,
                color = SentinelDangerBright,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics {
                    liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
                },
            )
        }

        Spacer(Modifier.height(20.dp))
        GradientPrimaryButton(
            text = if (busy) "Signing in…" else "Sign in",
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
                            shakeKey++
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
        )
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
    // Motion pass (2026-11-04): M-13 shake on unlock failure too.
    var shakeKey by remember { mutableStateOf(0) }
    // Icon Morph Program (2026-11-07): M-18 (f) unlock spring —
    // the lock state genuinely changes on this screen. Biometric
    // success flips the visual state, the shackle springs open
    // (34% overshoot), and only then does the shell unlock.
    // Under reduced motion the swap + unlock are instant.
    var unlockedVisual by remember { mutableStateOf(false) }
    val reducedMotion = rememberReducedMotion()
    LaunchedEffect(unlockedVisual) {
        if (unlockedVisual) {
            unlockSpringSettle(reducedMotion)
            viewModel.unlock()
        }
    }

    fun promptBiometric() {
        val activity = context as? FragmentActivity ?: return
        val manager = BiometricManager.from(context)
        val can = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            shakeKey++
            error = "Fingerprint unlock is not set up on this phone. Use the passphrase instead."
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    // M-18 (f): play the unlock spring, then the
                    // LaunchedEffect above unlocks the shell.
                    if (!unlockedVisual) unlockedVisual = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        shakeKey++
                        error = "Unlock did not work: $errString Try again, or use the passphrase."
                    }
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Command App")
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
            .padding(24.dp)
            .errorShake(shakeKey),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        UnlockSpringIcon(unlocked = unlockedVisual)
        Spacer(Modifier.height(16.dp))
        Text("Command App is locked", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your sign-in is still saved on this phone. Unlock to continue.",
            color = SentinelMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error!!, color = SentinelDangerBright, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(24.dp))
        GradientPrimaryButton(
            text = "Unlock with fingerprint",
            onClick = { promptBiometric() },
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = { viewModel.signOut { } }) {
            Text("Use passphrase instead (signs this device out first)")
        }
    }
}
