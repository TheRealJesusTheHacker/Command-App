package net.sentinelsafeguard.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import net.sentinelsafeguard.app.ui.theme.SentinelGreen
import net.sentinelsafeguard.app.ui.theme.SentinelMuted
import net.sentinelsafeguard.app.web.WebDestination

private enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Filled.Home),
    TICKETS("Tickets", Icons.Filled.List),
    SCHEDULE("Schedule", Icons.Filled.DateRange),
    MAIL("Mail", Icons.Filled.Email),
    CLIENTS("Clients", Icons.Filled.Person),
}

/**
 * The app shell (spec §8): Command's entry splash on cold start →
 * sign-in → lock gate → five bottom tabs, one thumb away. Settings
 * hangs off the top bar so the five tabs stay exactly the five
 * tabs; the hunt4 embedded browser (Full Command / Full Webmail)
 * takes over the content area the same way Settings does.
 */
@Composable
fun AppRoot(viewModel: AppViewModel) {
    var splashDone by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        AppShell(viewModel)
        if (!splashDone) {
            SplashScreen { splashDone = true }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppShell(viewModel: AppViewModel) {
    if (!viewModel.signedIn) {
        SignInScreen(viewModel)
        return
    }
    if (viewModel.locked) {
        LockScreen(viewModel)
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var tab by remember { mutableStateOf(Tab.TODAY) }
    var showSettings by remember { mutableStateOf(false) }
    var webDestination by remember { mutableStateOf<WebDestination?>(null) }

    // Launch update prompt (spec §7): plain words, owner decides.
    viewModel.updateAvailable?.let { available ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdate() },
            title = { Text("A new version is ready") },
            text = {
                Text(
                    "Sentinel App ${available.info.version} is ready." +
                        (available.info.notes.takeIf { it.isNotBlank() }?.let { "\n\n$it" } ?: "") +
                        "\n\nUpdate now? The app will download it and Android will ask you to confirm the install."
                )
            },
            confirmButton = {
                Button(onClick = {
                    val info = available
                    viewModel.dismissUpdate()
                    scope.launch {
                        try {
                            val file = viewModel.updates.download(context)
                            viewModel.updates.install(context, file)
                        } catch (ignored: Exception) {
                            // The Settings screen's update check remains
                            // the retry path if the download fails.
                        }
                    }
                }) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdate() }) { Text("Later") }
            },
        )
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Text(
                        webDestination?.title
                            ?: if (showSettings) "Settings" else tab.label
                    )
                },
                actions = {
                    IconButton(onClick = {
                        if (webDestination != null) {
                            webDestination = null
                        } else {
                            showSettings = !showSettings
                        }
                    }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry && !showSettings && webDestination == null,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            tab = entry
                            showSettings = false
                            webDestination = null
                        },
                        icon = { Icon(entry.icon, contentDescription = entry.label) },
                        label = { Text(entry.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SentinelGreen,
                            selectedTextColor = SentinelGreen,
                            indicatorColor = SentinelGreen.copy(alpha = 0.16f),
                            unselectedIconColor = SentinelMuted,
                            unselectedTextColor = SentinelMuted,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                webDestination != null -> WebScreen(webDestination!!) {
                    webDestination = null
                }
                showSettings -> SettingsScreen(
                    viewModel,
                    onSignedOut = { showSettings = false },
                    onOpenWeb = { webDestination = it },
                )
                else -> AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 10 })
                            .togetherWith(fadeOut(tween(160)))
                    },
                    label = "tabs",
                ) { targetTab ->
                    when (targetTab) {
                        Tab.TODAY -> TodayScreen(
                            viewModel,
                            onOpenTickets = { tab = Tab.TICKETS },
                            onOpenClients = { tab = Tab.CLIENTS },
                            onOpenFullCommand = { webDestination = WebDestination.COMMAND },
                        )
                        Tab.TICKETS -> TicketsScreen(viewModel)
                        Tab.SCHEDULE -> ScheduleScreen(viewModel)
                        Tab.MAIL -> MailScreen(viewModel)
                        Tab.CLIENTS -> ClientsScreen(viewModel)
                    }
                }
            }
        }
    }
}
