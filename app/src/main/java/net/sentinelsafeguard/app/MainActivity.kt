package net.sentinelsafeguard.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import net.sentinelsafeguard.app.notify.Notifier
import net.sentinelsafeguard.app.ui.AppRoot
import net.sentinelsafeguard.app.ui.AppViewModel
import net.sentinelsafeguard.app.ui.theme.SentinelTheme

class MainActivity : FragmentActivity() {

    private val viewModel: AppViewModel by viewModels()
    private var backgroundedAtMillis: Long = 0L

    // Expansion A1 deep links: the route a notification tap asked
    // for, plus a nonce that bumps on every delivery so AppRoot
    // re-navigates even when the same route is tapped twice.
    private var deepLinkRoute by mutableStateOf<String?>(null)
    private var deepLinkNonce by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // AUDIT-171 M-06: the app renders client PII (names,
        // contacts, invoices) — keep it out of screenshots,
        // screen recordings and the recents thumbnail. (The
        // in-app update flow installs via a file intent and does
        // not depend on captures; nothing in the app shares its
        // own window.)
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
        )
        readDeepLink(intent)
        setContent {
            SentinelTheme {
                AppRoot(
                    viewModel,
                    deepLinkRoute = deepLinkRoute,
                    deepLinkNonce = deepLinkNonce,
                    onDeepLinkConsumed = { deepLinkRoute = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readDeepLink(intent)
    }

    private fun readDeepLink(intent: Intent?) {
        val route = intent?.getStringExtra(Notifier.EXTRA_ROUTE)?.takeIf { it.isNotBlank() }
        if (route != null) {
            deepLinkRoute = route
            deepLinkNonce++
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundedAtMillis = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        // Re-lock after about five minutes in the background (spec §5).
        if (backgroundedAtMillis > 0 &&
            System.currentTimeMillis() - backgroundedAtMillis > RELOCK_AFTER_MS
        ) {
            viewModel.lockNow()
        }
        backgroundedAtMillis = 0L
    }

    companion object {
        const val RELOCK_AFTER_MS = 5 * 60 * 1000L
    }
}
