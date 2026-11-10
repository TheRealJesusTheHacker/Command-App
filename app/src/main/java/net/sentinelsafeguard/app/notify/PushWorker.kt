package net.sentinelsafeguard.app.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import net.sentinelsafeguard.app.BuildConfig
import net.sentinelsafeguard.app.config.AppConfig
import net.sentinelsafeguard.app.data.PushStore
import net.sentinelsafeguard.app.data.SecureStore
import net.sentinelsafeguard.app.data.SettingsStore
import net.sentinelsafeguard.app.data.api.OkHttpTransport
import net.sentinelsafeguard.app.data.api.SentinelApi
import net.sentinelsafeguard.app.data.api.pushAck
import net.sentinelsafeguard.app.data.api.pushPoll
import net.sentinelsafeguard.app.data.api.pushRegister
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** One event from the device's Command outbox. */
data class PushEvent(
    val id: String,
    val kind: String,
    val title: String,
    val body: String,
    val refType: String? = null,
    val refId: String? = null,
)

/**
 * Pure push-consumption logic (kind → notification category, event
 * parsing, cursor advance), unit-tested without Android.
 */
object PushLogic {

    /**
     * Event kind → notification category. Critical alerts ride the
     * high-importance sign-in-alert channel (they are the one class
     * worth waking for); ticket events and everything else ride the
     * tickets channel, exactly as the poll engine categorizes them.
     */
    fun categoryFor(kind: String): NotifyCategory = when {
        kind == "critical_alert" -> NotifyCategory.SIGN_IN_ALERT
        kind.startsWith("ticket") -> NotifyCategory.TICKETS
        else -> NotifyCategory.TICKETS
    }

    /** Whether [category]'s per-category toggle is on in Settings. */
    fun categoryEnabled(category: NotifyCategory, settings: SettingsStore): Boolean =
        when (category) {
            NotifyCategory.TICKETS -> settings.notifyTickets
            NotifyCategory.SIGN_IN_ALERT -> settings.notifySignInAlerts
            // 1.4.0 enum growth (Lane D channels): push events never
            // map to these today (categoryFor above), but the when
            // must stay exhaustive — critical rides its own toggle,
            // estate-down rides the alerts toggle, money/digest
            // have no per-category toggle (quiet hours still gate
            // them inside QuietHours.shouldNotify).
            NotifyCategory.CRITICAL -> settings.notifyCritical
            NotifyCategory.ESTATE_DOWN -> settings.notifySignInAlerts
            NotifyCategory.MONEY -> true
            NotifyCategory.DIGEST -> true
        }

    private fun JsonObject.stringField(vararg names: String): String? {
        for (name in names) {
            val value = (this[name] as? JsonPrimitive)?.contentOrNull
            if (!value.isNullOrBlank()) return value
        }
        return null
    }

    /** Parses the events array of a push-poll response; junk → none. */
    fun parseEvents(response: JsonObject): List<PushEvent> {
        val array = (response["events"] as? JsonArray)
            ?: (response["outbox"] as? JsonArray)
            ?: (response["items"] as? JsonArray)
            ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val id = obj.stringField("id", "eventId", "outboxId")
                ?: return@mapNotNull null
            val kind = obj.stringField("kind", "type") ?: ""
            PushEvent(
                id = id,
                kind = kind,
                title = obj.stringField("title") ?: "",
                body = obj.stringField("body", "message", "text") ?: "",
                refType = obj.stringField("refType"),
                refId = obj.stringField("refId"),
            )
        }
    }

    /**
     * The cursor to store after a poll: the server's stated cursor
     * when it sent one, otherwise the highest numeric event id
     * seen, otherwise the current cursor (never backwards).
     */
    fun nextCursor(current: Long, response: JsonObject, events: List<PushEvent>): Long {
        val stated = listOf("cursor", "nextCursor", "nextSince", "since")
            .firstNotNullOfOrNull { name ->
                (response[name] as? JsonPrimitive)?.longOrNull
                    ?: (response[name] as? JsonPrimitive)?.contentOrNull?.toLongOrNull()
            }
        if (stated != null) return maxOf(current, stated)
        val maxId = events.mapNotNull { it.id.toLongOrNull() }.maxOrNull()
        return if (maxId != null) maxOf(current, maxId) else current
    }

    /**
     * Registration lifecycle (1.5.1): register when this install
     * has never registered, OR when the recorded registration
     * belongs to an older app version than the one running. A
     * recorded version of 0 means no version was ever recorded —
     * every pre-1.5.1 registration, including ones made against
     * the server before push activation existed — and heals the
     * same way: exactly once, by re-registering for the current
     * version. A registration recorded at the current version
     * latches: later runs do not re-register until sign-out
     * resets the store or the next version bump.
     */
    fun needsRegistration(
        registered: Boolean,
        registeredVersionCode: Int,
        currentVersionCode: Int,
    ): Boolean = !registered || registeredVersionCode < currentVersionCode

    /**
     * The (registered, registeredVersionCode) state to persist
     * after a registration attempt. Success records the current
     * version, which is what latches the heal above. Failure
     * persists the incoming state untouched — nothing latches,
     * so the next worker run retries.
     */
    fun afterRegistrationAttempt(
        succeeded: Boolean,
        registered: Boolean,
        registeredVersionCode: Int,
        currentVersionCode: Int,
    ): Pair<Boolean, Int> =
        if (succeeded) true to currentVersionCode
        else registered to registeredVersionCode

    /** Display title for an event whose payload left it blank. */
    fun fallbackTitle(kind: String): String = when {
        kind == "critical_alert" -> "Critical alert"
        kind.startsWith("ticket") -> "Ticket update"
        else -> "Sentinel update"
    }

    /** Display body for an event whose payload left it blank. */
    fun fallbackBody(kind: String): String = when {
        kind == "critical_alert" ->
            "A critical alert was raised in Sentinel Command. Open the app to check it."
        kind.startsWith("ticket") ->
            "A ticket changed in Sentinel Command. Open the app to see it."
        else -> "Something changed in Sentinel Command. Open the app to see it."
    }
}

/**
 * Selfhost push consumption (1.4.0): the device long-polls its own
 * Command outbox and ACKs what it received — no third-party push
 * service, no Firebase. WorkManager runs this on the same ~15-minute
 * periodic floor as [PollWorker], which stays scheduled as the
 * fallback until push is proven on the owner's phone.
 *
 * Honest timing note: the poll passes wait=0 rather than holding a
 * long-poll open, because a WorkManager worker must finish well
 * inside its execution window — holding a connection for the
 * server's full long-poll would be killed mid-wait. Delivery is
 * therefore "within about 15 minutes", the same promise PollWorker
 * makes, never "instant".
 */
class PushWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val secure = SecureStore(applicationContext)
        if (!secure.isSignedIn) return Result.success()
        val settings = SettingsStore(applicationContext)
        val pushStore = PushStore(applicationContext)
        Notifier.ensureChannels(applicationContext)

        val api = SentinelApi(
            AppConfig.api,
            OkHttpTransport(AppConfig.api.baseUrl),
        )
        api.deviceToken = secure.deviceToken

        try {
            // Registration lifecycle (PushLogic.needsRegistration):
            // register on first run, once more after sign-out
            // (AppViewModel.signOut calls
            // PushStore.resetDeliveryState), and exactly once per
            // app version when the recorded registration predates
            // the running build — the heal that lets a device
            // registered against the pre-activation server
            // re-register now that push is on (the server
            // self-populates live tokens from re-registration).
            // A failed attempt throws before any state is written,
            // so nothing latches and the next run retries.
            if (PushLogic.needsRegistration(
                    pushStore.registered,
                    pushStore.registeredVersionCode,
                    BuildConfig.VERSION_CODE,
                )
            ) {
                api.pushRegister(pushStore.pushToken)
                val newState = PushLogic.afterRegistrationAttempt(
                    succeeded = true,
                    registered = pushStore.registered,
                    registeredVersionCode = pushStore.registeredVersionCode,
                    currentVersionCode = BuildConfig.VERSION_CODE,
                )
                pushStore.registered = newState.first
                pushStore.registeredVersionCode = newState.second
            }

            val response = api.pushPoll(since = pushStore.cursor, wait = 0)
            val events = PushLogic.parseEvents(response)
            if (events.isEmpty()) {
                pushStore.cursor = PushLogic.nextCursor(pushStore.cursor, response, events)
                return Result.success()
            }

            val now = LocalTime.now()
            for (event in events) {
                val category = PushLogic.categoryFor(event.kind)
                val allowed = QuietHours.shouldNotify(
                    category, now,
                    settings.quietStartMinutes, settings.quietEndMinutes,
                    PushLogic.categoryEnabled(category, settings),
                )
                if (allowed) {
                    Notifier.notify(
                        applicationContext, category,
                        event.title.ifBlank { PushLogic.fallbackTitle(event.kind) },
                        event.body.ifBlank { PushLogic.fallbackBody(event.kind) },
                    )
                }
                // Silenced events (quiet hours / toggle off) are
                // still consumed: they were delivered to the device,
                // and ACKing them below is what stops redelivery.
            }
            // ACK every id actually received and processed above —
            // notified or silenced alike — so nothing redelivers.
            api.pushAck(events.map { it.id })
            pushStore.cursor = PushLogic.nextCursor(pushStore.cursor, response, events)
        } catch (ignored: Exception) {
            // A failed poll is silent by design, like PollWorker; the
            // next periodic run retries, and un-ACKed events simply
            // redeliver then. PollWorker remains the fallback.
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "sentinel_push"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PushWorker>(15, TimeUnit.MINUTES)
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
