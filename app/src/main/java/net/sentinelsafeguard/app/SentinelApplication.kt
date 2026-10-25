package net.sentinelsafeguard.app

import android.app.Application
import net.sentinelsafeguard.app.notify.Notifier
import net.sentinelsafeguard.app.notify.PollWorker

class SentinelApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannels(this)
        PollWorker.schedule(this)
    }
}
