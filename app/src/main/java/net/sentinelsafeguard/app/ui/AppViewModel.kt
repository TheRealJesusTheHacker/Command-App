package net.sentinelsafeguard.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.sentinelsafeguard.app.BuildConfig
import net.sentinelsafeguard.app.config.AppConfig
import net.sentinelsafeguard.app.data.SecureStore
import net.sentinelsafeguard.app.data.SettingsStore
import net.sentinelsafeguard.app.data.api.LoginResult
import net.sentinelsafeguard.app.data.api.MeResponse
import net.sentinelsafeguard.app.data.api.OkHttpTransport
import net.sentinelsafeguard.app.data.api.SentinelApi
import net.sentinelsafeguard.app.data.mail.MailService
import net.sentinelsafeguard.app.update.UpdateManager
import net.sentinelsafeguard.app.update.UpdateState

/**
 * App-wide state: the session, the lock, and the long-lived services.
 * Screens read from here; all secrets flow through [secure] only.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    val secure = SecureStore(application)
    val settings = SettingsStore(application)
    val mail = MailService()

    val api = SentinelApi(AppConfig.api, OkHttpTransport(AppConfig.api.baseUrl))
    val updates = UpdateManager(api)

    var signedIn by mutableStateOf(secure.isSignedIn)
        private set

    var mailConnected by mutableStateOf(secure.hasMailCredentials)
        private set

    var locked by mutableStateOf(false)

    /** The /api/app/me payload (B0 flags ride it) — the mirrored
     *  shell gates its groups on these flags, exactly like web. */
    var me by mutableStateOf<MeResponse?>(null)
        private set

    /** True once a /me attempt has finished either way, so the
     *  shell can build even if the fetch failed (flags default
     *  off — fail-closed, like web without the flags). */
    var meLoaded by mutableStateOf(false)
        private set

    fun refreshMe() {
        viewModelScope.launch {
            try {
                me = api.me()
            } catch (e: Exception) {
                // Flags stay off; the shell still builds.
            } finally {
                meLoaded = true
            }
        }
    }

    var updateAvailable by mutableStateOf<UpdateState.Available?>(null)
        private set

    var updateChecked by mutableStateOf(false)
        private set

    init {
        api.deviceToken = secure.deviceToken
        api.onSignedOut = { handleTokenDead() }
        if (signedIn) {
            // Cold start with a live token: prove the human once,
            // if biometric unlock is on (spec §5).
            locked = settings.biometricEnabled
            checkForUpdate()
        }
    }

    fun deviceName(): String =
        "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim()

    /** The pending TOTP challenge from the first sign-in leg, held
     *  only in memory; the server challenge is single-use and
     *  short-lived, so nothing about it is persisted. */
    private var pendingTotpChallenge: String? = null
    private var pendingDisplayName: String? = null

    fun signIn(
        displayName: String,
        passphrase: String,
        onResult: (LoginResult) -> Unit,
    ) {
        sessionNotice = null
        viewModelScope.launch {
            val result = try {
                api.login(displayName, passphrase, deviceName())
            } catch (e: Exception) {
                LoginResult.Failed(InteractionLogic.NO_CONNECTION_MESSAGE)
            }
            if (result is LoginResult.TotpRequired) {
                pendingTotpChallenge = result.challenge
                pendingDisplayName = displayName
            }
            if (result is LoginResult.Success) {
                completeSignIn(result, displayName)
            }
            onResult(result)
        }
    }

    /** Second leg: answer the pending challenge with the typed code. */
    fun submitTotpCode(
        code: String,
        onResult: (LoginResult) -> Unit,
    ) {
        val challenge = pendingTotpChallenge
        if (challenge == null) {
            onResult(LoginResult.Failed("That sign-in challenge has expired — sign in again."))
            return
        }
        viewModelScope.launch {
            val result = try {
                api.completeTotp(challenge, code, deviceName())
            } catch (e: Exception) {
                LoginResult.Failed(InteractionLogic.NO_CONNECTION_MESSAGE)
            }
            if (result is LoginResult.Success) {
                completeSignIn(result, pendingDisplayName)
                pendingTotpChallenge = null
                pendingDisplayName = null
            }
            onResult(result)
        }
    }

    private fun completeSignIn(result: LoginResult.Success, fallbackName: String?) {
        secure.deviceToken = result.token
        secure.displayName = result.displayName ?: fallbackName
        signedIn = true
        locked = false
        checkForUpdate()
    }

    /** Mail sign-in is separate: the mailbox password, stored encrypted. */
    fun saveMailCredentials(username: String, password: String) {
        secure.mailUsername = username
        secure.mailPassword = password
        mailConnected = true
    }

    fun unlock() {
        locked = false
    }

    fun lockNow() {
        if (signedIn && settings.biometricEnabled) locked = true
    }

    /** Plain explanation waiting on the sign-in screen after a
     *  401 ended the session (hunt16): a clean return to sign-in
     *  WITH the reason, never a silent ejection. */
    var sessionNotice by mutableStateOf<String?>(null)

    fun clearSessionNotice() {
        sessionNotice = null
    }

    /** A 401 from anywhere lands here: the token is dead, back to sign-in. */
    private fun handleTokenDead() {
        sessionNotice = InteractionLogic.SESSION_ENDED_NOTICE
        viewModelScope.launch {
            secure.clearAll()
            mail.clearCache()
            api.deviceToken = null
            signedIn = false
            mailConnected = false
            locked = false
        }
    }

    /** Sign out = revoke this device (spec §2.1), then wipe local secrets. */
    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            api.logout()
            secure.clearAll()
            mail.clearCache()
            me = null
            meLoaded = false
            signedIn = false
            mailConnected = false
            locked = false
            updateAvailable = null
            onDone()
        }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            updateChecked = true
            updateAvailable = try {
                when (val state = updates.check()) {
                    is UpdateState.Available -> state
                    UpdateState.UpToDate -> null
                }
            } catch (e: Exception) {
                null // A failed update check never blocks the app.
            }
        }
    }

    fun dismissUpdate() {
        updateAvailable = null
    }

    val versionLabel: String get() = "Sentinel App ${BuildConfig.VERSION_NAME}"
}
