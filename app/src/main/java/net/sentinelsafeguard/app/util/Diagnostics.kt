package net.sentinelsafeguard.app.util

import android.util.Log

/**
 * Internal diagnostics (hunt4 premium pass — "error dignity").
 * Raw technical failures belong here, in the device log under the
 * SentinelDiag tag, never on the owner's screen: the UI shows
 * plain language and a way forward, while the detail stays one
 * `adb logcat -s SentinelDiag` away for the crew.
 */
object Diagnostics {
    fun log(where: String, error: Throwable) {
        try {
            Log.e("SentinelDiag", where, error)
        } catch (ignored: Exception) {
            // Logging must never take the app down with it.
        }
    }

    /** A noteworthy non-error event (1.5.4: the shell's
     *  defensive role default firing). Same tag, same rule —
     *  diagnostics belong in the log, not on a screen. */
    fun note(where: String, message: String) {
        try {
            Log.i("SentinelDiag", "$where: $message")
        } catch (ignored: Exception) {
            // Logging must never take the app down with it.
        }
    }
}
