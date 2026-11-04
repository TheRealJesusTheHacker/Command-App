package net.sentinelsafeguard.app.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Non-sensitive preferences only: toggles, quiet hours, and the last
 * counts the notification poll saw (so it can tell what rose). No
 * credentials ever live here — those are in [SecureStore] only.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()

    var notifyTickets: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_TICKETS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_TICKETS, value).apply()

    var notifyMail: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_MAIL, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_MAIL, value).apply()

    var notifySignInAlerts: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_ALERTS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_ALERTS, value).apply()

    /** Quiet hours default to the owner's sleep window (spec §6). */
    var quietStartMinutes: Int
        get() = prefs.getInt(KEY_QUIET_START, 11 * 60)
        set(value) = prefs.edit().putInt(KEY_QUIET_START, value).apply()

    var quietEndMinutes: Int
        get() = prefs.getInt(KEY_QUIET_END, 18 * 60)
        set(value) = prefs.edit().putInt(KEY_QUIET_END, value).apply()

    var lastNewTicketCount: Int
        get() = prefs.getInt(KEY_LAST_TICKETS, -1)
        set(value) = prefs.edit().putInt(KEY_LAST_TICKETS, value).apply()

    var lastMailUnreadCount: Int
        get() = prefs.getInt(KEY_LAST_MAIL, -1)
        set(value) = prefs.edit().putInt(KEY_LAST_MAIL, value).apply()

    var lastSignInAlertCount: Int
        get() = prefs.getInt(KEY_LAST_ALERTS, -1)
        set(value) = prefs.edit().putInt(KEY_LAST_ALERTS, value).apply()

    /** hunt17: the "your sign-in has ended" notice survives
     *  process death. The 401 handler sets this flag BEFORE the
     *  session is torn down; the sign-in screen reads it on the
     *  next cold start and the next sign-in attempt clears it.
     *  It is a boolean only — the notice text itself is a
     *  constant, never stored. */
    var sessionEndedNoticePending: Boolean
        get() = prefs.getBoolean(KEY_SESSION_ENDED, false)
        set(value) = prefs.edit().putBoolean(KEY_SESSION_ENDED, value).apply()

    companion object {
        const val FILE_NAME = "sentinel_settings"
        private const val KEY_BIOMETRIC = "biometric_enabled"
        private const val KEY_NOTIFY_TICKETS = "notify_tickets"
        private const val KEY_NOTIFY_MAIL = "notify_mail"
        private const val KEY_NOTIFY_ALERTS = "notify_signin_alerts"
        private const val KEY_QUIET_START = "quiet_start_minutes"
        private const val KEY_QUIET_END = "quiet_end_minutes"
        private const val KEY_LAST_TICKETS = "last_new_ticket_count"
        private const val KEY_LAST_MAIL = "last_mail_unread_count"
        private const val KEY_LAST_ALERTS = "last_signin_alert_count"
        private const val KEY_SESSION_ENDED = "session_ended_notice_pending"
    }
}
