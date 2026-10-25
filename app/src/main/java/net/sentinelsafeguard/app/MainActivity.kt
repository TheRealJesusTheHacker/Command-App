package net.sentinelsafeguard.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import net.sentinelsafeguard.app.ui.AppRoot
import net.sentinelsafeguard.app.ui.AppViewModel
import net.sentinelsafeguard.app.ui.theme.SentinelTheme

class MainActivity : FragmentActivity() {

    private val viewModel: AppViewModel by viewModels()
    private var backgroundedAtMillis: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SentinelTheme {
                AppRoot(viewModel)
            }
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
