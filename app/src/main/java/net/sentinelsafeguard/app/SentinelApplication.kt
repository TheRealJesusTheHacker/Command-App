package net.sentinelsafeguard.app

import android.app.Application
import net.sentinelsafeguard.app.notify.Notifier
import net.sentinelsafeguard.app.notify.PollWorker
import net.sentinelsafeguard.app.notify.PushWorker

class SentinelApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannels(this)
        PollWorker.schedule(this)
        // 1.4.0: selfhost push consumption (long-poll + ACK of the
        // Command outbox) alongside PollWorker, which stays as the
        // fallback until push is proven on the owner's phone.
        PushWorker.schedule(this)
    }
}
