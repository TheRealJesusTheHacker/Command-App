package net.sentinelsafeguard.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import net.sentinelsafeguard.app.MainActivity
import net.sentinelsafeguard.app.R

/** Local notifications only — no push service, no third parties (spec §6). */
object Notifier {

    const val CHANNEL_TICKETS = "tickets"
    const val CHANNEL_MAIL = "mail"
    const val CHANNEL_ALERTS = "signin_alerts"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_TICKETS, "New tickets", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "A new ticket needs attention." }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_MAIL, "New mail", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "New mail in your Sentinel mailbox." }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Sign-in alerts", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Someone signed in to Sentinel Command. These arrive even during quiet hours." }
        )
    }

    fun notify(context: Context, category: NotifyCategory, title: String, body: String) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val (channel, id) = when (category) {
            NotifyCategory.TICKETS -> CHANNEL_TICKETS to 1001
            NotifyCategory.MAIL -> CHANNEL_MAIL to 1002
            NotifyCategory.SIGN_IN_ALERT -> CHANNEL_ALERTS to 1003
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_sentinel)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (ignored: SecurityException) {
            // Notification permission not granted — the toggles in Settings
            // are the user's control; never crash a background poll for it.
        }
    }
}
