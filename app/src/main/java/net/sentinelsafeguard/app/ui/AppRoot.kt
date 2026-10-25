package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

private enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Filled.Home),
    TICKETS("Tickets", Icons.Filled.List),
    SCHEDULE("Schedule", Icons.Filled.DateRange),
    MAIL("Mail", Icons.Filled.Email),
    CLIENTS("Clients", Icons.Filled.Person),
}

/**
 * The app shell (spec §8): sign-in → lock gate → five bottom tabs,
 * one thumb away. Settings hangs off the top bar so the five tabs
 * stay exactly the five tabs.
 */
@Composable
fun AppRoot(viewModel: AppViewModel) {
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
    var tab by remember { mutableStateOf(Tab.TODAY) }
    var showSettings by remember { mutableStateOf(false) }

    // Launch update prompt (spec §7): plain words, owner decides.
    viewModel.updateAvailable?.let { available ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdate() },
            title = { Text("A new version is ready") },
            text = {
                Text(
                    "Sentinel App ${available.info.latestVersion} is ready." +
                        (available.info.notes?.let { "\n\n$it" } ?: "") +
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
                title = { Text(if (showSettings) "Settings" else tab.label) },
                actions = {
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry && !showSettings,
                        onClick = {
                            tab = entry
                            showSettings = false
                        },
                        icon = { Icon(entry.icon, contentDescription = entry.label) },
                        label = { Text(entry.label) },
                    )
                }
            }
        },
    ) { padding ->
        androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
            if (showSettings) {
                SettingsScreen(viewModel) { showSettings = false }
            } else {
                when (tab) {
                    Tab.TODAY -> TodayScreen(
                        viewModel,
                        onOpenTickets = { tab = Tab.TICKETS },
                        onOpenClients = { tab = Tab.CLIENTS },
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
