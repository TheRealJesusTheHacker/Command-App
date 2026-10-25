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

    fun signIn(
        displayName: String,
        passphrase: String,
        totpCode: String?,
        onResult: (LoginResult) -> Unit,
    ) {
        viewModelScope.launch {
            val result = try {
                api.login(displayName, passphrase, deviceName(), totpCode)
            } catch (e: Exception) {
                LoginResult.Failed("No connection. Check your internet and try again.")
            }
            if (result is LoginResult.Success) {
                secure.deviceToken = result.token
                secure.displayName = result.displayName ?: displayName
                signedIn = true
                locked = false
                checkForUpdate()
            }
            onResult(result)
        }
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

    /** A 401 from anywhere lands here: the token is dead, back to sign-in. */
    private fun handleTokenDead() {
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
