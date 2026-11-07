package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import net.sentinelsafeguard.app.ui.theme.SentinelGreen
import net.sentinelsafeguard.app.ui.theme.SentinelMuted
import net.sentinelsafeguard.app.ui.theme.SentinelSurfaceTop

/**
 * One transition pattern app-wide (hunt16, Polish Pass 2): a
 * short fade + a small horizontal settle for forward moves, the
 * mirror image on pop, plain fade out. Every NavHost (Command
 * shell + portal shell) uses exactly these, so no screen cuts
 * while a sibling animates.
 */
/**
 * Motion pass (2026-11-04): under the system reduced-motion
 * setting the settle disappears — every transition collapses
 * to a plain fade at the reduced-motion token (120 ms). The
 * standard 180/140 ms pattern above is otherwise untouched.
 */
private fun navEnter(reduced: Boolean = false): androidx.compose.animation.EnterTransition =
    if (reduced) {
        androidx.compose.animation.fadeIn(
            androidx.compose.animation.core.tween(MotionLogic.REDUCED_FADE_MS),
        )
    } else {
        androidx.compose.animation.fadeIn(
            androidx.compose.animation.core.tween(180),
        ) + androidx.compose.animation.slideInHorizontally(
            androidx.compose.animation.core.tween(180),
        ) { it / 14 }
    }

private fun navExit(reduced: Boolean = false): androidx.compose.animation.ExitTransition =
    androidx.compose.animation.fadeOut(
        androidx.compose.animation.core.tween(if (reduced) MotionLogic.REDUCED_FADE_MS else 140),
    )

private fun navPopEnter(reduced: Boolean = false): androidx.compose.animation.EnterTransition =
    if (reduced) {
        androidx.compose.animation.fadeIn(
            androidx.compose.animation.core.tween(MotionLogic.REDUCED_FADE_MS),
        )
    } else {
        androidx.compose.animation.fadeIn(
            androidx.compose.animation.core.tween(180),
        ) + androidx.compose.animation.slideInHorizontally(
            androidx.compose.animation.core.tween(180),
        ) { -it / 14 }
    }

private fun navPopExit(reduced: Boolean = false): androidx.compose.animation.ExitTransition =
    if (reduced) {
        androidx.compose.animation.fadeOut(
            androidx.compose.animation.core.tween(MotionLogic.REDUCED_FADE_MS),
        )
    } else {
        androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(140)) +
            androidx.compose.animation.slideOutHorizontally(
                androidx.compose.animation.core.tween(160),
            ) { it / 14 }
    }

/**
 * The mirrored shell (COMMAND-MIRROR-SPEC §2): Command's grouped
 * navigation as a drawer — exact groups, labels, and order —
 * over a navigation-compose back stack. Landing is Today (the
 * owner default in web Command; Dashboard for STAFF, whose
 * drawer is exactly the web STAFF subset). As of Phase 9 every
 * Command view is built — no placeholder screens remain.
 * Client/Family sign-ins get the separate portal shell
 * ([PortalShell]). Mail + Settings live in the Command drawer's
 * utility row: app-only entries, never Command views (§2.2).
 */
@Composable
fun AppRoot(
    viewModel: AppViewModel,
    deepLinkRoute: String? = null,
    deepLinkNonce: Int = 0,
    onDeepLinkConsumed: () -> Unit = {},
) {
    var splashDone by remember { mutableStateOf(false) }
    // hunt17: AppShell composes UNDER the splash from the first
    // frame — sign-in state resolves and (when signed in) the /me
    // fetch + Today loads run DURING the splash, so the splash's
    // own timing (Command's 3840/4700 ms) is the only wait; it
    // never serializes ahead of data. Tap dismisses it at once.
    // The shared skeleton pulse also lives here: one infinite
    // transition for every loading skeleton in the app.
    SkeletonPulseHost {
        Box(Modifier.fillMaxSize()) {
            AppShell(viewModel, deepLinkRoute, deepLinkNonce, onDeepLinkConsumed)
            if (!splashDone) {
                SplashScreen { splashDone = true }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppShell(
    viewModel: AppViewModel,
    deepLinkRoute: String? = null,
    deepLinkNonce: Int = 0,
    onDeepLinkConsumed: () -> Unit = {},
) {
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
    val drawerTick = rememberHapticTick()
    val reducedMotion = rememberReducedMotion()

    LaunchedEffect(viewModel.signedIn) {
        if (!viewModel.meLoaded) viewModel.refreshMe()
    }

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
                            val file = viewModel.updates.download(context, info.info)
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

    if (!viewModel.meLoaded) {
        LoadingBox("Loading Command…")
        return
    }

    val me = viewModel.me
    val user = me?.user

    // Forced flows resolve on web (spec §2.3): the app refuses
    // with web's plain wording rather than improvising them.
    if (user?.mustReset == true) {
        ForcedFlowNotice(
            "Passphrase change required",
            "Your passphrase must be changed before Command opens. " +
                "That step happens in Sentinel Command on the web — sign in there, " +
                "change the passphrase, then come back here.",
            viewModel,
        )
        return
    }
    if (user?.totpEnrollmentRequired == true) {
        ForcedFlowNotice(
            "Two-factor enrollment required",
            "This account must finish two-factor enrollment before Command opens. " +
                "That step happens in Sentinel Command on the web — sign in there, " +
                "finish enrollment, then come back here.",
            viewModel,
        )
        return
    }

    // Do All 1.4.0 (item 6) — first run: the notification
    // permission is explained in Sentinel's own words BEFORE
    // Android's system prompt is ever shown, and only once.
    // "Not now" is remembered (SettingsStore) and the explanation
    // stays re-openable from Settings → Notifications. This is
    // the only place in the app that requests the permission.
    val notifSettings = viewModel.settings
    var notifGranted by remember { mutableStateOf(notificationsPermissionGranted(context)) }
    var notifPromptDismissed by remember {
        mutableStateOf(notifSettings.notifyPermissionExplainerDismissed)
    }
    var notifAskedThisSession by remember { mutableStateOf(false) }
    val notifLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notifGranted = granted
        notifAskedThisSession = true
        if (granted) {
            notifSettings.notifyPermissionExplainerDismissed = false
            notifPromptDismissed = false
        }
    }
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
        !notifGranted && !notifPromptDismissed && !notifAskedThisSession
    ) {
        NotificationPermissionExplainerDialog(
            onAllow = {
                notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            },
            onNotNow = {
                notifSettings.notifyPermissionExplainerDismissed = true
                notifPromptDismissed = true
            },
        )
    }

    val role = user?.role ?: "OWNER"
    when (MirrorNav.shellKind(role, me?.portal ?: false)) {
        MirrorNav.ShellKind.TOURIST_REFUSED -> {
            // By design the demo is a separate instance: Tourist
            // accounts never receive a production device token,
            // so a Tourist sign-in attempt is refused plainly
            // with the server's own refusal, never a shell.
            ForcedFlowNotice(
                "Demo accounts use the demo",
                "This is a demo (Tourist) account. The demo lives on its own " +
                    "instance, so it can't sign in to the production app — " +
                    "the server refused this sign-in. Sign in with your real " +
                    "account here, or open the demo the way you usually do.",
                viewModel,
            )
            return
        }
        MirrorNav.ShellKind.PORTAL -> {
            // Phase 9: Client/Family sign-ins get the portal
            // shell — web's twelve portal views, never the
            // staff drawer.
            PortalShell(viewModel)
            return
        }
        MirrorNav.ShellKind.COMMAND -> Unit
    }

    val flags = mapOf(
        "lawAccess" to (me?.lawAccess ?: false),
        "newsAccess" to (me?.newsAccess ?: false),
        "osintAccess" to (me?.osintAccess ?: false),
        "breachAccess" to (me?.breachAccess ?: false),
        "threatAccess" to (me?.threatAccess ?: false),
        "reviewsAccess" to (me?.reviewsAccess ?: false),
        "taxAccess" to (me?.taxAccess ?: false),
    )
    val taxOnly = me?.taxOnly ?: false
    val landing = MirrorNav.landingKey(role, taxOnly)
    // taxOnly's only view is Tax & Profit (b4etax) — built in
    // hunt8 (Phase 4), so its route is the built destination.
    val startRoute = landing

    val nav = rememberNavController()

    // Expansion A1: a notification tap's route arrives via
    // MainActivity's intent extras — navigate to the exact
    // record, then consume it so a recomposition never
    // re-navigates. An unresolvable route is dropped quietly.
    LaunchedEffect(deepLinkNonce) {
        val route = deepLinkRoute
        if (!route.isNullOrBlank()) {
            try {
                nav.navigate(route) { launchSingleTop = true }
            } catch (ignored: Exception) {
            }
            onDeepLinkConsumed()
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: landing
    val currentKey = currentRoute.substringBefore("/")

    fun titleFor(route: String): String = when {
        route.startsWith("ticket/") || route.startsWith("ticketEdit") -> "Tickets"
        route == "ticketNew" -> "New ticket"
        route.startsWith("client/") || route.startsWith("clientEdit") -> "Clients"
        route == "clientNew" -> "New client"
        route.startsWith("case/") || route.startsWith("caseEdit") -> "Cases"
        route == "caseNew" -> "New case"
        route.startsWith("engagement/") || route.startsWith("engagementEdit") -> "Engagements"
        route == "engagementNew" -> "New engagement"
        route.startsWith("contract/") -> "Contracts"
        route.startsWith("invoice/") || route.startsWith("invoiceEdit") -> "Invoices"
        route == "invoiceNew" -> "New invoice"
        route.startsWith("quote/") || route.startsWith("quoteEdit") -> "Quotes"
        route == "quoteNew" -> "New quote"
        route.startsWith("knowledge/") || route.startsWith("knowledgeEdit") -> "Knowledge"
        route == "knowledgeNew" -> "New catalog entry"
        route == "knowledge" -> "Knowledge"
        route == "archive" -> "Archive"
        route.startsWith("employee/") || route.startsWith("employeeEdit") -> "Employees"
        route == "employeeNew" -> "New employee"
        route.startsWith("asset/") || route.startsWith("assetEdit") -> "Assets"
        route == "assetNew" -> "New asset"
        route == "formNew" -> "New form record"
        route.startsWith("formEdit") -> "Forms"
        route == "b3money" -> "Money Desk"
        route == "b4etax" -> "Tax & Profit"
        route == "b3ops" -> "Portal Settings"
        route == "cf2firmops" -> "Firm Ops"
        route == "b4assess" -> "Verified Desk"
        route.startsWith("assessment/") -> "Verified Desk"
        route == "activity" -> "Activity"
        route == "b4fdesk" -> "Runbooks & Sign-off"
        route.startsWith("job/") || route.startsWith("jobEdit") -> "Schedule"
        route == "jobNew" -> "New job"
        route == "search" -> "Search"
        route == "mail" -> "Mail"
        route == "settings" -> "Settings"
        route == "admin" -> "Admin"
        route.startsWith("adminsec/") ->
            MirrorNav.ALL_VIEWS[route.removePrefix("adminsec/")]?.label ?: "Admin"
        route == "verified" -> "Sentinel Verified"
        route == "firmpulse" -> "Firm Pulse"
        else -> MirrorNav.ALL_VIEWS[route]?.label ?: "Command"
    }

    fun go(route: String) {
        drawerTick()
        scope.launch { drawerState.close() }
        if (currentRoute != route) {
            nav.navigate(route) { launchSingleTop = true }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
                ) {
                    Text(
                        "SENTINEL SAFEGUARD",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    if (user != null) {
                        Text(
                            "${user.displayName} · ${user.role}",
                            color = SentinelMuted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (role != "STAFF") {
                        // Phase 9 STAFF verification: the STAFF
                        // drawer is exactly the STAFF subset plus
                        // the §2.2 utility row — no Search entry.
                        NavigationDrawerItem(
                            label = { Text("Search") },
                            selected = currentKey == "search",
                            onClick = { go("search") },
                            icon = { DrawerIcon("search", currentKey == "search") },
                            colors = drawerItemColors(),
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                    MirrorNav.visibleGroups(role, flags, taxOnly).forEach { group ->
                        Text(
                            group.label,
                            color = SentinelMuted,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 4.dp),
                        )
                        group.views.forEach { view ->
                            NavigationDrawerItem(
                                label = { Text(view.label) },
                                selected = currentKey == view.key,
                                onClick = {
                                    go(if (view.key == "admin") "admin" else viewRoute(view))
                                },
                                icon = { DrawerIcon(view.key, currentKey == view.key) },
                                colors = drawerItemColors(),
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                    }
                    MirrorNav.visibleInjected(role, flags, taxOnly).forEach { view ->
                        NavigationDrawerItem(
                            label = { Text(view.label) },
                            selected = currentKey == view.key,
                            onClick = { go(viewRoute(view)) },
                            icon = { DrawerIcon(view.key, currentKey == view.key) },
                            colors = drawerItemColors(),
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    NavigationDrawerItem(
                        label = { Text("Mail") },
                        selected = currentKey == "mail",
                        onClick = { go("mail") },
                        icon = { DrawerIcon("mail", currentKey == "mail") },
                        colors = drawerItemColors(),
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    NavigationDrawerItem(
                        label = { Text("Settings") },
                        selected = currentKey == "settings",
                        onClick = { go("settings") },
                        icon = { DrawerIcon("settings", currentKey == "settings") },
                        colors = drawerItemColors(),
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(titleFor(currentRoute)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Command menu")
                        }
                    },
                    actions = {
                        if (role != "STAFF") {
                            TextButton(onClick = { go("search") }) { Text("Search") }
                        }
                        IconButton(onClick = { go("settings") }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    },
                )
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                NavHost(navController = nav, startDestination = startRoute, enterTransition = { navEnter(reducedMotion) }, exitTransition = { navExit(reducedMotion) }, popEnterTransition = { navPopEnter(reducedMotion) }, popExitTransition = { navPopExit(reducedMotion) }) {
                    composable("today") { TodayScreen(viewModel, nav) }
                    composable("dashboard") { DashboardScreen(viewModel, nav) }
                    composable("tickets") { TicketsScreen(viewModel, nav) }
                    composable(
                        "ticket/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        TicketProfileScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id") ?: 0,
                        )
                    }
                    composable("ticketNew") { TicketFormScreen(viewModel, nav, null) }
                    composable(
                        "ticketEdit/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        TicketFormScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id"),
                        )
                    }
                    composable("clients") { ClientsScreen(viewModel, nav) }
                    composable(
                        "client/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        ClientProfileScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id") ?: 0,
                        )
                    }
                    composable("clientNew") { ClientFormScreen(viewModel, nav, null) }
                    composable(
                        "clientEdit/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        ClientFormScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id"),
                        )
                    }
                    composable("cases") { CasesScreen(viewModel, nav) }
                    composable(
                        "case/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        CaseProfileScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id") ?: 0,
                        )
                    }
                    composable("caseNew") { CaseFormScreen(viewModel, nav, null) }
                    composable(
                        "caseEdit/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        CaseFormScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id"),
                        )
                    }
                    composable("schedule") { ScheduleScreen(viewModel, nav) }
                    composable("myschedule") { MyScheduleScreen(viewModel, nav) }
                    composable(
                        "job/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        JobProfileScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id") ?: 0,
                        )
                    }
                    composable("jobNew") { JobFormScreen(viewModel, nav, null, null) }
                    composable(
                        "jobNew/{date}",
                        arguments = listOf(navArgument("date") { type = NavType.StringType }),
                    ) { entry ->
                        JobFormScreen(
                            viewModel, nav, null,
                            entry.arguments?.getString("date"),
                        )
                    }
                    composable(
                        "jobEdit/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        JobFormScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id"), null,
                        )
                    }
                    composable("engagements") { EngagementsScreen(viewModel, nav) }
                    composable(
                        "engagement/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        EngagementProfileScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id") ?: 0,
                        )
                    }
                    composable("engagementNew") { EngagementFormScreen(viewModel, nav, null) }
                    composable(
                        "engagementEdit/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        EngagementFormScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id"),
                        )
                    }
                    composable("b4fdesk") { RunbooksScreen(viewModel, nav) }
                    composable("invoices") { InvoicesScreen(viewModel, nav) }
                    composable(
                        "invoice/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        InvoiceProfileScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id") ?: 0,
                        )
                    }
                    composable("invoiceNew") { InvoiceFormScreen(viewModel, nav, null) }
                    composable(
                        "invoiceEdit/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        InvoiceFormScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id"),
                        )
                    }
                    composable(
                        "quote/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        QuoteProfileScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id") ?: 0,
                        )
                    }
                    composable("quoteNew") { QuoteFormScreen(viewModel, nav, null) }
                    composable(
                        "quoteEdit/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        QuoteFormScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id"),
                        )
                    }
                    composable("employees") { EmployeesScreen(viewModel, nav) }
                    composable(
                        "employee/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        EmployeeProfileScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id") ?: 0,
                        )
                    }
                    composable("employeeNew") { EmployeeFormScreen(viewModel, nav, null) }
                    composable(
                        "employeeEdit/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        EmployeeFormScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id"),
                        )
                    }
                    composable("assets") { AssetsScreen(viewModel, nav) }
                    composable(
                        "asset/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        AssetProfileScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id") ?: 0,
                        )
                    }
                    composable("assetNew") { AssetFormScreen(viewModel, nav, null) }
                    composable(
                        "assetEdit/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        AssetFormScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id"),
                        )
                    }
                    composable("forms") { FormsScreen(viewModel, nav) }
                    composable("formNew") { FormRecordFormScreen(viewModel, nav, null) }
                    composable(
                        "formEdit/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        FormRecordFormScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id"),
                        )
                    }
                    composable("knowledge") { KnowledgeScreen(viewModel, nav) }
                    composable(
                        "knowledge/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        KnowledgeDetailScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id") ?: 0,
                        )
                    }
                    composable("knowledgeNew") { KnowledgeFormScreen(viewModel, nav, null) }
                    composable(
                        "knowledgeEdit/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        KnowledgeFormScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id"),
                        )
                    }
                    composable("archive") { ArchiveScreen(viewModel, nav) }
                    composable("b3ops") { PortalSettingsScreen(viewModel, nav) }
                    composable("cf2firmops") { FirmOpsScreen(viewModel, nav) }
                    composable("b4assess") { VerifiedDeskScreen(viewModel, nav) }
                    composable(
                        "assessment/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        AssessmentDetailScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id") ?: 0,
                        )
                    }
                    composable("activity") { ActivityScreen(viewModel, nav) }
                    composable("liveintel") { LiveIntelScreen(viewModel, nav) }
                    composable("osint") { OsintScreen(viewModel, nav) }
                    composable("breachintel") { BreachIntelScreen(viewModel, nav) }
                    composable("threatintel") { ThreatIntelScreen(viewModel, nav) }
                    composable("sentinelnews") { SentinelNewsScreen(viewModel, nav) }
                    composable("newsdesk") { NewsDeskScreen(viewModel, nav) }
                    composable("reviews") { ReviewsScreen(viewModel, nav) }
                    composable("law") { LawScreen(viewModel, nav) }
                    composable("b3money") { MoneyDeskScreen(viewModel, nav) }
                    composable("b4etax") { TaxProfitScreen(viewModel, nav) }
                    composable("verified") { VerifiedScreen(viewModel, nav) }
                    composable("firmpulse") { FirmPulseScreen(viewModel, nav) }
                    composable("cf2assurance") { AssuranceScreen(viewModel, nav) }
                    composable(
                        "contract/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        ContractDetailScreen(
                            viewModel, nav,
                            entry.arguments?.getInt("id") ?: 0,
                        )
                    }
                    composable("search") { SearchScreen(viewModel, nav) }
                    composable("mail") { MailScreen(viewModel) }
                    composable("settings") {
                        SettingsScreen(viewModel, onSignedOut = { go(startRoute) })
                    }
                    composable("admin") { AdminConsoleScreen(nav) }
                    composable(
                        "adminsec/{key}",
                        arguments = listOf(navArgument("key") { type = NavType.StringType }),
                    ) { entry ->
                        AdminSectionScreen(
                            viewModel, nav,
                            entry.arguments?.getString("key") ?: "overview",
                        )
                    }
                    MirrorNav.INJECTED.forEach { injected ->
                        composable(injected.key) { InjectedScreen(viewModel, nav, injected.key) }
                    }
                    // Phase 9 closes the placeholder ledger: every
                    // Command view has a real route above, so the
                    // old view/{key} placeholder route is GONE —
                    // there is no placeholder destination left in
                    // the nav graph.
                }
            }
        }
    }
}


/**
 * hunt18 drawer icon: the view's Material Symbol at 20dp,
 * muted at rest, emerald on the active item (whose pill fill
 * is refined to SurfaceTop). Decoration beside the unchanged
 * label — never a replacement for it.
 */
@Composable
private fun DrawerIcon(key: String, selected: Boolean) {
    Icon(
        painter = androidx.compose.ui.res.painterResource(mirrorViewIcon(key)),
        contentDescription = null,
        tint = if (selected) SentinelGreen else SentinelMuted,
        modifier = Modifier.size(20.dp),
    )
}

@Composable
private fun drawerItemColors() = NavigationDrawerItemDefaults.colors(
    selectedContainerColor = SentinelSurfaceTop,
)

/** Drawer route for a nav view: every view is built (Phase 9
 *  closed the ledger), so the key IS the route. */
private fun viewRoute(view: MirrorView): String = view.key

@Composable
private fun ForcedFlowNotice(title: String, body: String, viewModel: AppViewModel) {
    Column(Modifier.padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(body, color = SentinelMuted)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { viewModel.signOut {} }) { Text("Sign out") }
    }
}

/**
 * The Admin console (spec §2.1): its own grouped structure —
 * 6 groups / 13 sections + injected DMARC. The sections are the
 * built Phase 8B mirrors (hunt13) and open as full screens.
 */
@Composable
fun AdminConsoleScreen(nav: androidx.navigation.NavController) {
    // Do All 1.4.0 tool deck (Skyler: "More functional tools, not
    // flat"): the App Command Center lands as grouped tool cards —
    // one deck card per group, one tile per tool with its purpose
    // in a line. Every tool is ONE tap from this deck (two from
    // the drawer), with the M-09 stagger + M-01 press-sink the
    // rest of the app uses. No tile is decorative: every tile
    // opens its section.
    androidx.compose.foundation.lazy.LazyColumn {
        item { MirrorHeader("Admin") }
        MirrorNav.ADMIN_GROUPS.forEach { group ->
            item {
                MirrorCard(group.label) {
                    group.views.forEachIndexed { index, section ->
                        Column(Modifier.listEntrance(index)) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .sentinelClickable {
                                        nav.navigate("adminsec/${section.key}")
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        section.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Text(
                                        adminToolPurpose(section.key),
                                        color = SentinelMuted,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                Text(
                                    "›",
                                    color = SentinelMuted,
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

/** One plain line per admin tool: what it does, from what the
 *  section itself does (AdminScreens.kt) — no invented scope. */
private fun adminToolPurpose(key: String): String = when (key) {
    "overview" -> "Users by role, workspace settings, estate health, recent admin activity."
    "report" -> "The monthly operations report, ready to share."
    "roles" -> "Roles and tab permissions — who can do what."
    "members" -> "Member accounts: create, invite, set passphrases."
    "invites" -> "Open invitations and when they expire."
    "sessions" -> "Active sessions, and revoke-all in one tap."
    "security" -> "Sign-in protection, lockdown, and recovery codes."
    "oversight" -> "Retention, the audit explorer, exports, and login security."
    "audit" -> "The full audit feed and Security Oversight."
    "lawai" -> "Law AI provider, daily cap, and its API key."
    "backups" -> "Server backup bundles — request, download, discard."
    "dmarc" -> "DMARC reports by source and domain. Read-only."
    "danger" -> "Workspace exports and revoke-all. Handle with care."
    else -> "Open this admin tool."
}

/**
 * The portal shell (Phase 9): Client/Family sign-ins land here
 * — web's twelve portal views in web's names/order as a simple
 * drawer, landing on My Profile. This is a separate shell from
 * Command's: no staff groups, no injected entries, no Search.
 * The drawer's only utility entry is Sign out (the portal token
 * is refused on the staff utility surfaces server-side, so the
 * app-only Settings/Mail entries do not belong here).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalShell(viewModel: AppViewModel) {
    val scope = rememberCoroutineScope()
    val drawerTick = rememberHapticTick()
    val reducedMotion = rememberReducedMotion()
    val nav = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: MirrorNav.portalLandingKey()
    val user = viewModel.me?.user

    fun go(route: String) {
        drawerTick()
        scope.launch { drawerState.close() }
        if (currentRoute != route) {
            nav.navigate(route) { launchSingleTop = true }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
                ) {
                    Text(
                        "SENTINEL SAFEGUARD",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    if (user != null) {
                        Text(
                            "${user.displayName} · ${user.role}",
                            color = SentinelMuted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    MirrorNav.PORTAL_VIEWS.forEach { view ->
                        NavigationDrawerItem(
                            label = { Text(view.label) },
                            selected = currentRoute == view.key,
                            onClick = { go(view.key) },
                            icon = { DrawerIcon(view.key, currentRoute == view.key) },
                            colors = drawerItemColors(),
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    NavigationDrawerItem(
                        label = { Text("Sign out") },
                        selected = false,
                        onClick = { viewModel.signOut {} },
                        icon = { DrawerIcon("signout", false) },
                        colors = drawerItemColors(),
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            MirrorNav.ALL_VIEWS[currentRoute]?.label ?: "My Profile",
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Portal menu")
                        }
                    },
                )
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                NavHost(
                    navController = nav,
                    startDestination = MirrorNav.portalLandingKey(),
                    enterTransition = { navEnter(reducedMotion) },
                    exitTransition = { navExit(reducedMotion) },
                    popEnterTransition = { navPopEnter(reducedMotion) },
                    popExitTransition = { navPopExit(reducedMotion) },
                ) {
                    composable("portal_profile") { PortalProfileScreen(viewModel, nav) }
                    composable("portal_progress") { PortalProgressScreen(viewModel, nav) }
                    composable("portal_history") { PortalHistoryScreen(viewModel, nav) }
                    composable("portal_documents") { PortalDocumentsScreen(viewModel, nav) }
                    composable("portal_files") { PortalFilesScreen(viewModel, nav) }
                    composable("portal_messages") { PortalMessagesScreen(viewModel, nav) }
                    composable("portal_alert") { PortalAlertScreen(viewModel, nav) }
                    composable("portal_signoffs") { PortalSignOffsScreen(viewModel, nav) }
                    composable("portal_contracts") { PortalContractsScreen(viewModel, nav) }
                    composable("portal_knowledge") { PortalKnowledgeScreen(viewModel, nav) }
                    composable("portal_status") { PortalStatusScreen(viewModel, nav) }
                    composable("portal_feedback") { PortalFeedbackScreen(viewModel, nav) }
                }
            }
        }
    }
}
