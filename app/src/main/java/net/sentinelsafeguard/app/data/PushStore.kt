package net.sentinelsafeguard.app.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Selfhost-push device state (1.4.0): in the self-hosted design
 * there is no third-party push token — the server delivers by the
 * device polling its own outbox, and registration stores a stable
 * per-install token the server can address this install by. This
 * store owns that token (generated once, then kept), the
 * registered-once flag, and the poll cursor (the highest outbox
 * position the device has consumed, so a poll never replays
 * history). Non-sensitive values only, following [SettingsStore];
 * the Command device token itself stays in [SecureStore].
 */
class PushStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    /**
     * The stable per-install push token. Generated on first read
     * and persisted immediately, so every registration for this
     * install presents the same token.
     */
    val pushToken: String
        get() {
            val existing = prefs.getString(KEY_PUSH_TOKEN, null)
            if (!existing.isNullOrBlank()) return existing
            val generated = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_PUSH_TOKEN, generated).apply()
            return generated
        }

    /** True once this install's token has been registered with the
     *  server. Reset on sign-out so the next sign-in re-registers. */
    var registered: Boolean
        get() = prefs.getBoolean(KEY_REGISTERED, false)
        set(value) = prefs.edit().putBoolean(KEY_REGISTERED, value).apply()

    /** Poll cursor: pass as `since` on the next poll. 0 = from the start. */
    var cursor: Long
        get() = prefs.getLong(KEY_CURSOR, 0L)
        set(value) = prefs.edit().putLong(KEY_CURSOR, value).apply()

    /** Forgets registration and cursor (sign-out); the token itself
     *  is kept — it identifies the install, not the session. */
    fun resetDeliveryState() {
        prefs.edit()
            .putBoolean(KEY_REGISTERED, false)
            .putLong(KEY_CURSOR, 0L)
            .apply()
    }

    companion object {
        const val FILE_NAME = "sentinel_push"
        private const val KEY_PUSH_TOKEN = "push_token"
        private const val KEY_REGISTERED = "push_registered"
        private const val KEY_CURSOR = "push_cursor"
    }
}
