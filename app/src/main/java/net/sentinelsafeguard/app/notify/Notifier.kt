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
    // Do All 1.4.0 (Expansion A1): the phone-automation channels.
    const val CHANNEL_CRITICAL = "critical_alerts"
    const val CHANNEL_ESTATE = "estate_alerts"
    const val CHANNEL_MONEY = "money_updates"
    const val CHANNEL_DIGEST = "digests"

    /** Intent extras a deep-link notification tap carries into
     *  MainActivity: the resolved in-app route and the raw record
     *  id it points at. AppRoot consumes them on launch and on
     *  onNewIntent — a tap lands on the exact record, never just
     *  the app's home screen (automations list rule). */
    const val EXTRA_ROUTE = "net.sentinelsafeguard.commandapp.extra.ROUTE"
    const val EXTRA_REF_ID = "net.sentinelsafeguard.commandapp.extra.REF_ID"

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
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_CRITICAL, "Critical alerts", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "A Critical Alert was sent in Sentinel Command. These arrive even during quiet hours." }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ESTATE, "Estate down", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "A Sentinel site or service is down. These arrive even during quiet hours." }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_MONEY, "Money updates", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Payments received and invoices that need attention." }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_DIGEST, "Digests", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Your morning brief and shift digests." }
        )
    }

    private fun channelAndBaseId(category: NotifyCategory): Pair<String, Int> = when (category) {
        NotifyCategory.TICKETS -> CHANNEL_TICKETS to 1001
        NotifyCategory.MAIL -> CHANNEL_MAIL to 1002
        NotifyCategory.SIGN_IN_ALERT -> CHANNEL_ALERTS to 1003
        NotifyCategory.CRITICAL -> CHANNEL_CRITICAL to 1004
        NotifyCategory.ESTATE_DOWN -> CHANNEL_ESTATE to 1005
        NotifyCategory.MONEY -> CHANNEL_MONEY to 1006
        NotifyCategory.DIGEST -> CHANNEL_DIGEST to 1007
    }

    fun notify(context: Context, category: NotifyCategory, title: String, body: String) {
        post(context, category, title, body, route = null, refId = null)
    }

    /**
     * A notification whose tap deep-links to the exact record:
     * [routeKey] is the automation/push kind (see [NotifyRouting])
     * and [refId] the record id it names. The PendingIntent carries
     * the resolved route as [EXTRA_ROUTE] plus the raw id as
     * [EXTRA_REF_ID]; MainActivity forwards both to AppRoot.
     */
    fun notifyDeepLink(
        context: Context,
        category: NotifyCategory,
        title: String,
        body: String,
        routeKey: String,
        refId: String?,
    ) {
        val route = NotifyRouting.targetFor(routeKey, refId).route
        post(context, category, title, body, route = route, refId = refId)
    }

    private fun post(
        context: Context,
        category: NotifyCategory,
        title: String,
        body: String,
        route: String?,
        refId: String?,
    ) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val (channel, baseId) = channelAndBaseId(category)
        // Deep-linked notifications get their own id slot per
        // record so two ticket alerts never overwrite each other;
        // plain notifications keep the historic per-category id.
        val id = if (route == null) {
            baseId
        } else {
            baseId * 1000 + ((refId?.hashCode() ?: route.hashCode()) and 0x3FF)
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (route != null) {
                putExtra(EXTRA_ROUTE, route)
                if (refId != null) putExtra(EXTRA_REF_ID, refId)
            }
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

/**
 * Pure routing for notification kinds (Expansion A1): an
 * automation/push kind string + record id maps to the
 * notification category and the exact in-app route the tap
 * opens. Route shapes follow MirrorNav / the AppRoot nav graph
 * ("ticket/{id}", "invoice/{id}", "client/{id}", section keys).
 * Kinds whose records have no detail route in the app land on
 * their section — noted per kind below. Unit-tested.
 */
object NotifyRouting {

    data class Target(val category: NotifyCategory, val route: String)

    private fun detailOrSection(refId: String?, detailPrefix: String, section: String): String {
        val id = refId?.trim()?.toIntOrNull()
        return if (id != null) "$detailPrefix$id" else section
    }

    fun targetFor(kind: String, refId: String?): Target = when (kind.trim().lowercase()) {
        // Tickets: Command's push kinds (ticket_assigned /
        // ticket_replied / critical enqueues) + the poll kinds.
        "ticket_created", "ticket_assigned", "ticket_replied",
        "new_ticket", "ticket", "tickets" ->
            Target(NotifyCategory.TICKETS, detailOrSection(refId, "ticket/", "tickets"))
        // Mail has no per-message route in the app shell — the
        // section IS the deepest honest target.
        "mail", "new_mail", "mail_intake" ->
            Target(NotifyCategory.MAIL, "mail")
        // Critical Alerts surface on Today (the urgent-intake
        // banner pins there); there is no alert detail route.
        "critical_alert", "critical" ->
            Target(NotifyCategory.CRITICAL, "today")
        "sign_in_alert", "signin_alert", "new_signin" ->
            Target(NotifyCategory.SIGN_IN_ALERT, "activity")
        // Estate health lives on Today's estate panel.
        "estate_down", "watchdog", "site_down", "estate" ->
            Target(NotifyCategory.ESTATE_DOWN, "today")
        "payment_received", "payment", "money",
        "invoice_overdue", "invoice_issued", "invoice" ->
            Target(NotifyCategory.MONEY, detailOrSection(refId, "invoice/", "invoices"))
        "lead", "new_lead" ->
            Target(NotifyCategory.TICKETS, detailOrSection(refId, "client/", "clients"))
        "digest", "platform_digest", "morning_brief", "shift_digest" ->
            Target(NotifyCategory.DIGEST, "today")
        else -> Target(NotifyCategory.TICKETS, "today")
    }
}
