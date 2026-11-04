package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * The mirrored shell (COMMAND-MIRROR-SPEC §2): Command's grouped
 * navigation as a drawer — exact groups, labels, and order —
 * over a navigation-compose back stack. Landing is Today (the
 * owner default in web Command). Unbuilt Command views appear
 * under their exact names and open a placeholder naming their
 * phase; they are never renamed, hidden, or silently cut.
 * Mail + Settings live in the drawer's utility row: they are
 * app-only entries, never presented as Command views (§2.2).
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

    val role = user?.role ?: "OWNER"
    if (role == "Client" || role == "Family" || role == "Tourist") {
        // The portal shell is Phase 9 — an honest placeholder
        // shell, never the staff shell with data it cannot show.
        Column {
            MirrorHeader("Sentinel Command")
            PlaceholderScreen("Client portal", MirrorNav.PHASE9)
            Button(
                onClick = { viewModel.signOut {} },
                modifier = Modifier.padding(horizontal = 16.dp),
            ) { Text("Sign out") }
        }
        return
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
        route.startsWith("employee/") || route.startsWith("employeeEdit") -> "Employees"
        route == "employeeNew" -> "New employee"
        route.startsWith("asset/") || route.startsWith("assetEdit") -> "Assets"
        route == "assetNew" -> "New asset"
        route == "formNew" -> "New form record"
        route.startsWith("formEdit") -> "Forms"
        route == "b3money" -> "Money Desk"
        route == "b4etax" -> "Tax & Profit"
        route == "b4fdesk" -> "Runbooks & Sign-off"
        route.startsWith("job/") || route.startsWith("jobEdit") -> "Schedule"
        route == "jobNew" -> "New job"
        route == "search" -> "Search"
        route == "mail" -> "Mail"
        route == "settings" -> "Settings"
        route == "admin" -> "Admin"
        route == "verified" -> "Sentinel Verified"
        route == "firmpulse" -> "Firm Pulse"
        route.startsWith("view/") ->
            MirrorNav.ALL_VIEWS[route.removePrefix("view/")]?.label ?: "Command"
        else -> MirrorNav.ALL_VIEWS[route]?.label ?: "Command"
    }

    fun go(route: String) {
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
                    NavigationDrawerItem(
                        label = { Text("Search") },
                        selected = currentKey == "search",
                        onClick = { go("search") },
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
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
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                    }
                    MirrorNav.visibleInjected(role, flags, taxOnly).forEach { view ->
                        NavigationDrawerItem(
                            label = { Text(view.label) },
                            selected = currentKey == view.key,
                            onClick = { go(viewRoute(view)) },
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    NavigationDrawerItem(
                        label = { Text("Mail") },
                        selected = currentKey == "mail",
                        onClick = { go("mail") },
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    NavigationDrawerItem(
                        label = { Text("Settings") },
                        selected = currentKey == "settings",
                        onClick = { go("settings") },
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
                        TextButton(onClick = { go("search") }) { Text("Search") }
                        IconButton(onClick = { go("settings") }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    },
                )
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                NavHost(navController = nav, startDestination = startRoute) {
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
                        "view/{key}",
                        arguments = listOf(navArgument("key") { type = NavType.StringType }),
                    ) { entry ->
                        val view = MirrorNav.ALL_VIEWS[entry.arguments?.getString("key")]
                        if (view != null) {
                            PlaceholderScreen(view.label, view.phase ?: MirrorNav.PHASE8)
                        } else {
                            PlaceholderScreen("Command", MirrorNav.PHASE8)
                        }
                    }
                }
            }
        }
    }
}

/** Drawer route for a nav view: built views use their key as the
 *  route; unbuilt views land on the named placeholder. */
private fun viewRoute(view: MirrorView): String =
    if (view.phase == null) view.key else "view/${view.key}"

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
 * 6 groups / 13 sections + injected DMARC. The structure renders
 * in hunt5; every section is a placeholder until Phase 8.
 */
@Composable
fun AdminConsoleScreen(nav: androidx.navigation.NavController) {
    androidx.compose.foundation.lazy.LazyColumn {
        item { MirrorHeader("Admin") }
        MirrorNav.ADMIN_GROUPS.forEach { group ->
            item { SectionHeader(group.label) }
            group.views.forEach { section ->
                item {
                    ItemRow(section.label) { nav.navigate("view/${section.key}") }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
