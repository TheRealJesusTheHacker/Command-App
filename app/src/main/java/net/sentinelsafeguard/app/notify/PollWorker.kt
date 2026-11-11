package net.sentinelsafeguard.app.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import net.sentinelsafeguard.app.BuildConfig
import net.sentinelsafeguard.app.config.AppConfig
import net.sentinelsafeguard.app.data.PushStore
import net.sentinelsafeguard.app.data.SecureStore
import net.sentinelsafeguard.app.data.SettingsStore
import net.sentinelsafeguard.app.data.api.OkHttpTransport
import net.sentinelsafeguard.app.data.api.SentinelApi
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * The v1 notification engine (spec §6): WorkManager wakes the app about
 * every 15 minutes (the platform floor — "within about 15 minutes",
 * never "instant") and compares two cheap numbers against last time:
 * new-ticket / sign-in-alert counts from the Command API. Anything
 * that rose posts a local notification, subject to the per-category
 * toggles and quiet hours. (1.6.3: the former mailbox unread
 * check is gone with the mail engine — mail lives in the
 * Sentinel Mail App.)
 */
class PollWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val secure = SecureStore(applicationContext)
        if (!secure.isSignedIn) return Result.success()
        val settings = SettingsStore(applicationContext)
        Notifier.ensureChannels(applicationContext)

        val api = SentinelApi(
            AppConfig.api,
            OkHttpTransport(AppConfig.api.baseUrl),
        )
        api.deviceToken = secure.deviceToken

        val now = LocalTime.now()
        try {
            val today = api.today()

            // AUDIT-171 B-12: once the self-host push channel is
            // registered for the running version, IT owns ticket
            // alerts (per event, deep-linked). The poll worker's
            // count-based ticket alert then stands down — before
            // this gate, the same ticket change alerted twice, once
            // per worker. Sign-in alerts are unaffected: the outbox
            // carries no sign-in kind, so the poll worker remains
            // their only producer.
            val pushStore = PushStore(applicationContext)
            val pushOwnsTickets = pushStore.registered &&
                pushStore.registeredVersionCode >= BuildConfig.VERSION_CODE

            if (!pushOwnsTickets &&
                today.newTickets.total > settings.lastNewTicketCount &&
                settings.lastNewTicketCount >= 0 &&
                QuietHours.shouldNotify(
                    NotifyCategory.TICKETS, now,
                    settings.quietStartMinutes, settings.quietEndMinutes,
                    settings.notifyTickets,
                )
            ) {
                val rose = today.newTickets.total - settings.lastNewTicketCount
                Notifier.notify(
                    applicationContext, NotifyCategory.TICKETS,
                    "New tickets",
                    if (rose == 1) "One new ticket is waiting." else "$rose new tickets are waiting.",
                )
            }
            settings.lastNewTicketCount = today.newTickets.total

            if (today.signinAlerts.total > settings.lastSignInAlertCount &&
                settings.lastSignInAlertCount >= 0 &&
                QuietHours.shouldNotify(
                    NotifyCategory.SIGN_IN_ALERT, now,
                    settings.quietStartMinutes, settings.quietEndMinutes,
                    settings.notifySignInAlerts,
                )
            ) {
                Notifier.notify(
                    applicationContext, NotifyCategory.SIGN_IN_ALERT,
                    "Sign-in alert",
                    "There was a new sign-in to Command. Open the app to check it.",
                )
            }
            settings.lastSignInAlertCount = today.signinAlerts.total
        } catch (ignored: Exception) {
            // A failed poll is silent by design; the next one retries.
            // A dead token surfaces the next time the app is opened.
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "sentinel_poll"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PollWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
