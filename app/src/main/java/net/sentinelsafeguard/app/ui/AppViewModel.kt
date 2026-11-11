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
import net.sentinelsafeguard.app.data.OfflineQueue
import net.sentinelsafeguard.app.data.PushStore
import net.sentinelsafeguard.app.data.SecureStore
import net.sentinelsafeguard.app.data.SettingsStore
import net.sentinelsafeguard.app.data.api.LoginResult
import net.sentinelsafeguard.app.data.api.MeResponse
import net.sentinelsafeguard.app.data.api.OkHttpTransport
import net.sentinelsafeguard.app.data.api.SentinelApi
import net.sentinelsafeguard.app.data.api.pushUnregister
import net.sentinelsafeguard.app.update.UpdateManager
import net.sentinelsafeguard.app.update.UpdateState

/**
 * App-wide state: the session, the lock, and the long-lived services.
 * Screens read from here; all secrets flow through [secure] only.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    val secure = SecureStore(application)
    val settings = SettingsStore(application)
    val pushStore = PushStore(application)

    val api = SentinelApi(AppConfig.api, OkHttpTransport(AppConfig.api.baseUrl))
    val updates = UpdateManager(api)

    var signedIn by mutableStateOf(secure.isSignedIn)
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

    /** Plain explanation waiting on the sign-in screen after a
     *  401 ended the session (hunt16): a clean return to sign-in
     *  WITH the reason, never a silent ejection. Declared before
     *  init: hunt17 restores a persisted notice there. */
    var sessionNotice by mutableStateOf<String?>(null)

    var updateAvailable by mutableStateOf<UpdateState.Available?>(null)
        private set

    var updateChecked by mutableStateOf(false)
        private set

    init {
        api.deviceToken = secure.deviceToken
        api.onSignedOut = { handleTokenDead() }
        // hunt17: restore a session-ended notice that survived
        // process death (set by handleTokenDead, cleared by the
        // next sign-in attempt) so the explanation is never lost
        // just because Android reclaimed the process first.
        if (settings.sessionEndedNoticePending) {
            sessionNotice = InteractionLogic.SESSION_ENDED_NOTICE
        }
        if (signedIn) {
            // Cold start with a live token: prove the human once,
            // if biometric unlock is on (spec §5).
            locked = settings.biometricEnabled
            checkForUpdate()
            flushOfflineQueue()
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
        settings.sessionEndedNoticePending = false
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
        flushOfflineQueue()
    }

    /**
     * AUDIT-171 B-08: the offline write queue finally has its
     * flush triggers. Queued ticket writes (made with no
     * connection) replay against POST /api/app/sync after every
     * sign-in and on every app start with a live session.
     * Best-effort by design: a failed flush leaves the entries
     * queued for the next trigger; it never blocks sign-in or
     * startup, and the queue's own receipts logic decides each
     * entry's fate.
     */
    fun flushOfflineQueue() {
        if (!signedIn) return
        viewModelScope.launch {
            try {
                OfflineQueue(getApplication()).flush(api)
            } catch (e: Exception) {
                // Still offline or the sync was refused — the
                // entries stay queued; nothing is lost.
            }
        }
    }

    fun unlock() {
        locked = false
    }

    fun lockNow() {
        if (signedIn && settings.biometricEnabled) locked = true
    }

    fun clearSessionNotice() {
        sessionNotice = null
        settings.sessionEndedNoticePending = false
    }

    /** A 401 from anywhere lands here: the token is dead, back to sign-in. */
    private fun handleTokenDead() {
        sessionNotice = InteractionLogic.SESSION_ENDED_NOTICE
        // Persist BEFORE teardown: if the process dies on the way
        // to the sign-in screen, the notice still shows (hunt17).
        settings.sessionEndedNoticePending = true
        viewModelScope.launch {
            secure.clearAll()
            // AUDIT-171 B-15: a forced sign-out now resets the same
            // push delivery state a manual sign-out does — the 401
            // path used to leave the cursor and registration latch
            // behind, so the next session inherited stale delivery
            // state. (The seen-event memory survives on purpose:
            // see PushStore.seenEventIds.)
            pushStore.resetDeliveryState()
            api.deviceToken = null
            signedIn = false
            locked = false
        }
    }

    /** Sign out = revoke this device (spec §2.1), then wipe local secrets. */
    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            // AUDIT-171 B-15: unregister this device's push
            // registration on the server BEFORE the token dies —
            // pushUnregister's own doc assigns it to sign-out, but
            // nothing ever called it, so the server kept a live
            // registration for a signed-out device. Best-effort:
            // a network failure must never block signing out.
            try {
                api.pushUnregister()
            } catch (ignored: Exception) {
                // Signing out proceeds regardless.
            }
            api.logout()
            secure.clearAll()
            // Push registration belongs to the session, not the
            // install: forget it (and its version record) so the
            // next sign-in's PushWorker run registers fresh —
            // the reset PushWorker's lifecycle has always assumed.
            pushStore.resetDeliveryState()
            // AUDIT-171 M-03: the token was not the whole session.
            // Form drafts, queued offline mutations and shared
            // export files belong to the signed-in account too —
            // a different person signing in on this phone must not
            // inherit them. (Today the draft/queue stores are not
            // yet wired into forms — AUDIT-171 B-08 — the wipe is
            // written against their real APIs so it stays true
            // when they are.)
            try {
                val drafts = net.sentinelsafeguard.app.data.DraftStore(getApplication())
                drafts.listFormIds().forEach { drafts.clear(it) }
                net.sentinelsafeguard.app.data.OfflineQueue(getApplication()).clear()
                java.io.File(getApplication<Application>().cacheDir, "updates")
                    .listFiles()?.forEach { it.delete() }
            } catch (ignored: Exception) {
                // Local hygiene never blocks sign-out.
            }
            me = null
            meLoaded = false
            signedIn = false
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

    val versionLabel: String get() = "Command App ${BuildConfig.VERSION_NAME}"
}
