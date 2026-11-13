package net.sentinelsafeguard.app.desktop

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import net.sentinelsafeguard.app.data.DesktopStore

// Sentinel palette (near-black + emerald) — the phone's discipline,
// zero purple, in a desktop-native dark scheme.
private fun sentinelColors(): ColorScheme = darkColorScheme(
    primary = Color(0xFF2EE6A8),
    onPrimary = Color(0xFF04231A),
    secondary = Color(0xFF7BD8BB),
    onSecondary = Color(0xFF06231A),
    background = Color(0xFF0A0F0D),
    onBackground = Color(0xFFE6F2EC),
    surface = Color(0xFF121A17),
    onSurface = Color(0xFFE6F2EC),
    surfaceVariant = Color(0xFF1B2622),
    onSurfaceVariant = Color(0xFFB9CDC4),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF2B0B08),
)

@Composable
fun CommandDesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = sentinelColors(), content = content)
}

fun main() = application {
    val store = DesktopStore()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Command App",
        icon = painterResource("images/sentinel_logo.jpg"),
    ) {
        CommandDesktopTheme {
            DesktopApp(store)
        }
    }
}
