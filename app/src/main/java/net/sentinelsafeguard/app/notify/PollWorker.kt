package net.sentinelsafeguard.app.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import net.sentinelsafeguard.app.config.AppConfig
import net.sentinelsafeguard.app.data.SecureStore
import net.sentinelsafeguard.app.data.SettingsStore
import net.sentinelsafeguard.app.data.api.OkHttpTransport
import net.sentinelsafeguard.app.data.api.SentinelApi
import net.sentinelsafeguard.app.data.mail.MailService
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * The v1 notification engine (spec §6): WorkManager wakes the app about
 * every 15 minutes (the platform floor — "within about 15 minutes",
 * never "instant") and compares two cheap numbers against last time:
 * new-ticket / sign-in-alert counts from the Command API, and the mail
 * unread count over IMAP. Anything that rose posts a local
 * notification, subject to the per-category toggles and quiet hours.
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

            if (today.newTickets.total > settings.lastNewTicketCount &&
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
                    "There was a new sign-in to Sentinel Command. Open the app to check it.",
                )
            }
            settings.lastSignInAlertCount = today.signinAlerts.total
        } catch (ignored: Exception) {
            // A failed poll is silent by design; the next one retries.
            // A dead token surfaces the next time the app is opened.
        }

        if (secure.hasMailCredentials) {
            try {
                val unread = MailService().totalUnread(
                    secure.mailUsername!!, secure.mailPassword!!,
                )
                if (unread > settings.lastMailUnreadCount &&
                    settings.lastMailUnreadCount >= 0 &&
                    QuietHours.shouldNotify(
                        NotifyCategory.MAIL, now,
                        settings.quietStartMinutes, settings.quietEndMinutes,
                        settings.notifyMail,
                    )
                ) {
                    val rose = unread - settings.lastMailUnreadCount
                    Notifier.notify(
                        applicationContext, NotifyCategory.MAIL,
                        "New mail",
                        if (rose == 1) "One new message arrived." else "$rose new messages arrived.",
                    )
                }
                settings.lastMailUnreadCount = unread
            } catch (ignored: Exception) {
                // Mail server unreachable this round — stay silent, retry next poll.
            }
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
