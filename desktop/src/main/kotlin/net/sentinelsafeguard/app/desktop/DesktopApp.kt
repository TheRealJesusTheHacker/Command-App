package net.sentinelsafeguard.app.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.config.AppConfig
import net.sentinelsafeguard.app.data.DesktopStore
import net.sentinelsafeguard.app.data.api.ApiException
import net.sentinelsafeguard.app.data.api.LoginResult
import net.sentinelsafeguard.app.data.api.OkHttpTransport
import net.sentinelsafeguard.app.data.api.SentinelApi
import net.sentinelsafeguard.app.data.api.SignedOutException
import net.sentinelsafeguard.app.data.api.analytics
import net.sentinelsafeguard.app.data.api.bookingAdmin
import net.sentinelsafeguard.app.data.api.ctosCase
import net.sentinelsafeguard.app.data.api.ctosCreateCase
import net.sentinelsafeguard.app.data.api.ctosDeleteCase
import net.sentinelsafeguard.app.data.api.ctosWorkspace
import net.sentinelsafeguard.app.data.api.invoices
import net.sentinelsafeguard.app.data.api.liveFlights
import net.sentinelsafeguard.app.data.api.liveGeoData
import net.sentinelsafeguard.app.data.api.liveThreatData
import net.sentinelsafeguard.app.data.api.moneyDesk
import net.sentinelsafeguard.app.data.api.osintCreateSearch
import net.sentinelsafeguard.app.data.api.osintDetect
import net.sentinelsafeguard.app.data.api.osintWorkspace
import net.sentinelsafeguard.app.data.api.pushAck
import net.sentinelsafeguard.app.data.api.pushPoll
import net.sentinelsafeguard.app.data.api.wysAudio
import net.sentinelsafeguard.app.ui.AnalyticsLogic
import net.sentinelsafeguard.app.ui.IntelMapLogic
import net.sentinelsafeguard.app.ui.Phase3Logic
import net.sentinelsafeguard.app.ui.TileMap
import net.sentinelsafeguard.app.ui.Wave161Logic

// ---------------------------------------------------------------------------
// Command App Desktop — native Compose Desktop UI (desktop port #3).
// Navigation is the app's own small back-stack (decision of record:
// the phone's androidx.navigation-compose is Android-only; CMP's
// navigation artifact was not needed — screen parity is the
// requirement, and every screen below is reachable in <=2 taps from
// the rail, mirroring the phone's drawer sections). Screens speak
// to the SAME production host and the SAME /api/app/* twins as the
// phone, through the phone's own shared SentinelApi + logic layer
// compiled verbatim into this build.
// ---------------------------------------------------------------------------

enum class Screen {
    Today, Dashboard, Clients, ClientDetail, Tickets, TicketDetail,
    Money, Booking, Osint, Ctos, LiveIntel, Analytics, Settings, About,
}

class Session(val store: DesktopStore) {
    val api = SentinelApi(AppConfig.api, OkHttpTransport(AppConfig.api.baseUrl))
    var me: net.sentinelsafeguard.app.data.api.MeResponse? by mutableStateOf(null)
    var screen by mutableStateOf(Screen.Today)
    var backStack = mutableListOf<Screen>()
    var selectedClientId by mutableStateOf<Int?>(null)
    var selectedTicketId by mutableStateOf<Int?>(null)
    var notice by mutableStateOf<String?>(null)
    var pollEvents by mutableStateOf(0)

    fun go(s: Screen) {
        if (s != screen) { backStack.add(screen); screen = s }
    }

    fun back() {
        screen = if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex) else Screen.Today
    }

    fun signOutLocal() {
        api.deviceToken = null
        store.clearAll()
        me = null
        screen = Screen.Today
        backStack.clear()
    }
}

fun errText(e: Throwable): String = when (e) {
    is SignedOutException -> "Your sign-in has ended. Please sign in again."
    is ApiException -> e.message
    else -> e.message ?: "Something went wrong. Please try again."
}

@Composable
fun DesktopApp(store: DesktopStore) {
    val session = remember { Session(store) }
    val scope = rememberCoroutineScope()
    var locked by remember { mutableStateOf(store.hasPin() && store.deviceToken != null) }

    LaunchedEffect(Unit) {
        val token = store.deviceToken
        if (!token.isNullOrBlank()) {
            session.api.deviceToken = token
            session.api.onSignedOut = { session.signOutLocal() }
            runCatching { session.me = session.api.me() }
                .onFailure { if (it is SignedOutException) session.signOutLocal() }
        }
    }

    // SELFHOST push: long-poll while the app is OPEN only. Nothing
    // is delivered while the app is closed and nothing claims it.
    LaunchedEffect(session.me) {
        if (session.me == null) return@LaunchedEffect
        var since = store.pref("push_since")?.toLongOrNull() ?: 0L
        while (true) {
            runCatching {
                val out = session.api.pushPoll(since, 0)
                val events = Phase3Logic.arr(out, "events")
                if (events.isNotEmpty()) {
                    session.pollEvents += events.size
                    val ids = events.mapNotNull { Phase3Logic.int(it, "id")?.toString() }
                    val maxId = events.mapNotNull { Phase3Logic.long(it, "id") }.maxOrNull()
                    if (maxId != null) { since = maxId; store.setPref("push_since", since.toString()) }
                    if (ids.isNotEmpty()) runCatching { session.api.pushAck(ids) }
                }
            }
            delay(60_000)
        }
    }

    if (locked && store.hasPin()) {
        PinUnlock(store) { locked = false }
        return
    }

    if (session.me == null) {
        SignIn(session) { /* me set inside */ }
        return
    }

    Row(Modifier.fillMaxSize()) {
        // Rail — the phone's drawer sections, same order family.
        Column(
            Modifier.width(210.dp).padding(10.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Command App", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                session.me?.user?.displayName ?: "",
                style = MaterialTheme.typography.labelMedium, color = Color(0xFF9FB8AD),
            )
            Spacer(Modifier.height(8.dp))
            for ((label, s) in listOf(
                "Today" to Screen.Today,
                "Dashboard" to Screen.Dashboard,
                "Clients" to Screen.Clients,
                "Tickets" to Screen.Tickets,
                "Money desk" to Screen.Money,
                "Booking" to Screen.Booking,
                "OSINT" to Screen.Osint,
                "CToS tools" to Screen.Ctos,
                "Live Intel" to Screen.LiveIntel,
                "Analytics" to Screen.Analytics,
                "Settings" to Screen.Settings,
                "About" to Screen.About,
            )) {
                if (session.screen == s) {
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text(label) }
                } else {
                    OutlinedButton(onClick = { session.go(s) }, modifier = Modifier.fillMaxWidth()) { Text(label) }
                }
            }
            if (session.pollEvents > 0) {
                Text(
                    "${session.pollEvents} update(s) arrived while the app is open (self-hosted poll).",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                scope.launch { runCatching { session.api.logout() }; session.signOutLocal() }
            }) { Text("Sign out") }
        }
        Box(Modifier.fillMaxSize().padding(12.dp)) {
            Column(Modifier.fillMaxSize()) {
                session.notice?.let {
                    Card(
                        Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF123528)),
                    ) { Text(it, Modifier.padding(10.dp)) }
                }
                when (session.screen) {
                    Screen.Today -> TodayScreen(session)
                    Screen.Dashboard -> DashboardScreen(session)
                    Screen.Clients -> ClientsScreen(session)
                    Screen.ClientDetail -> ClientDetailScreen(session)
                    Screen.Tickets -> TicketsScreen(session)
                    Screen.TicketDetail -> TicketDetailScreen(session)
                    Screen.Money -> MoneyScreen(session)
                    Screen.Booking -> BookingScreen(session)
                    Screen.Osint -> OsintScreen(session)
                    Screen.Ctos -> CtosScreen(session)
                    Screen.LiveIntel -> LiveIntelScreen(session)
                    Screen.Analytics -> AnalyticsScreen(session)
                    Screen.Settings -> SettingsScreen(session, store)
                    Screen.About -> AboutScreen(store)
                }
            }
        }
    }
}

@Composable
fun PinUnlock(store: DesktopStore, onUnlock: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Unlock Command App", style = MaterialTheme.typography.headlineSmall)
        Text("Enter the PIN for this desktop install. (Desktop unlock is PIN only — no biometrics on desktop.)")
        OutlinedTextField(pin, { pin = it }, label = { Text("PIN") }, visualTransformation = PasswordVisualTransformation())
        error?.let { Text(it, color = Color(0xFFFF8A80)) }
        Button(onClick = {
            if (store.checkPin(pin)) onUnlock() else error = "That PIN does not match."
        }) { Text("Unlock") }
    }
}

@Composable
fun SignIn(session: Session, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(session.store.displayName ?: "") }
    var pass by remember { mutableStateOf("") }
    var challenge by remember { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val deviceName = remember { "Command App Desktop (" + System.getProperty("os.name") + ")" }

    fun finish(result: LoginResult) {
        busy = false
        when (result) {
            is LoginResult.Success -> {
                session.store.deviceToken = result.token
                session.store.displayName = result.displayName ?: name
                session.api.onSignedOut = { session.signOutLocal() }
                scope.launch {
                    runCatching { session.me = session.api.me() }
                        .onFailure { error = errText(it) }
                }
                onDone()
            }
            is LoginResult.TotpRequired -> { challenge = result.challenge; error = null }
            is LoginResult.Failed -> error = result.message
        }
    }

    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Command App", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Sign in to Sentinel Command — the same account and device-token sign-in as the phone app.")
        if (challenge == null) {
            OutlinedTextField(name, { name = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                pass, { pass = it }, label = { Text("Passphrase") },
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
            )
            error?.let { Text(it, color = Color(0xFFFF8A80)) }
            Button(
                enabled = !busy && name.isNotBlank() && pass.isNotBlank(),
                onClick = {
                    busy = true; error = null
                    scope.launch {
                        runCatching { session.api.login(name.trim(), pass, deviceName) }
                            .onSuccess { finish(it) }
                            .onFailure { busy = false; error = errText(it) }
                    }
                },
            ) { Text(if (busy) "Signing in…" else "Sign in") }
        } else {
            Text("Two-factor: enter the 6-digit code from your authenticator.")
            OutlinedTextField(code, { code = it }, label = { Text("Code") }, modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = Color(0xFFFF8A80)) }
            Button(
                enabled = !busy && code.isNotBlank(),
                onClick = {
                    busy = true; error = null
                    val ch = challenge!!
                    scope.launch {
                        runCatching { session.api.completeTotp(ch, code.trim(), deviceName) }
                            .onSuccess { finish(it) }
                            .onFailure { busy = false; error = errText(it) }
                    }
                },
            ) { Text(if (busy) "Verifying…" else "Verify code") }
            TextButton(onClick = { challenge = null; code = "" }) { Text("Back") }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
}

@Composable
fun LoadError(error: String?, retry: () -> Unit) {
    if (error != null) {
        Card(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1712)),
        ) {
            Column(Modifier.padding(10.dp)) {
                Text(error, color = Color(0xFFFFC9C0))
                TextButton(onClick = retry) { Text("Retry") }
            }
        }
    }
}

@Composable
fun TodayScreen(session: Session) {
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<net.sentinelsafeguard.app.data.api.TodayResponse?>(null) }
    var wys by remember { mutableStateOf<Wave161Logic.WysAudio?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    fun load() {
        loading = true
        scope.launch {
            runCatching { session.api.today() }.onSuccess { data = it; error = null }
                .onFailure { error = errText(it) }
            runCatching {
                // The WYS card rides the dashboard payload's
                // whileYouSlept field (owner-only server-side);
                // absence is the gate — no card, no Listen row.
                val dash = session.api.dashboard()
                wys = Wave161Logic.parseWys(dash.whileYouSlept)?.audio
            }
            loading = false
        }
    }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize()) {
        SectionTitle("Today")
        LoadError(error) { load() }
        if (loading) { CircularProgressIndicator(); return@Column }
        val t = data ?: return@Column
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (t.briefing.isNotBlank()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Briefing", fontWeight = FontWeight.Bold)
                            Text(t.briefing)
                            wys?.let { WysListen(session, it) }
                        }
                    }
                }
            } else {
                item { WysStandalone(session) }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Money", fontWeight = FontWeight.Bold)
                        Text("Outstanding: ${t.money.outstanding.count} invoice(s), ${t.money.outstanding.totalCents / 100}.${(t.money.outstanding.totalCents % 100).toString().padStart(2, '0')} USD")
                        Text("Overdue: ${t.money.overdue.count} invoice(s)")
                        Text("Due this week: ${t.money.dueThisWeek.count} invoice(s)")
                    }
                }
            }
            if (t.newTickets.items.isNotEmpty()) {
                item { Text("New tickets", fontWeight = FontWeight.Bold) }
                items(t.newTickets.items) { tk ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Text("#${tk.id} ${tk.title}", fontWeight = FontWeight.Bold)
                            Text("${tk.status} · ${tk.priority}${tk.clientName?.let { " · $it" } ?: ""}")
                        }
                    }
                }
            }
            if (t.overdueCases.items.isNotEmpty()) {
                item { Text("Overdue cases", fontWeight = FontWeight.Bold) }
                items(t.overdueCases.items) { c ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Text(c.title, fontWeight = FontWeight.Bold)
                            Text("${c.status} · due ${c.dueDate ?: "—"}")
                        }
                    }
                }
            }
            if (t.followUps.items.isNotEmpty()) {
                item { Text("Follow-ups due", fontWeight = FontWeight.Bold) }
                items(t.followUps.items) { c ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Text(c.legalName.ifBlank { c.dba }, fontWeight = FontWeight.Bold)
                            Text("Next follow-up: ${c.nextFollowUp ?: "—"} · ${c.stage}")
                        }
                    }
                }
            }
            if (t.signinAlerts.items.isNotEmpty()) {
                item { Text("Sign-in alerts", fontWeight = FontWeight.Bold) }
                items(t.signinAlerts.items) { a ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Text("${a.actorName} — ${a.action}")
                            Text(a.detail ?: "", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            if (t.newTickets.items.isEmpty() && t.overdueCases.items.isEmpty() && t.followUps.items.isEmpty()) {
                item { Text("Nothing urgent in today's summary.") }
            }
        }
    }
}

/** WYS Listen — owner-only briefing audio. Real MP3 playback via
 *  the desktop JLayer player; refusals render as the server states
 *  them (Denied = not the owner / not authorized). */
@Composable
fun WysStandalone(session: Session) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf("Listen to the latest While You Slept briefing (owner only).") }
    var playing by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("While You Slept — Listen", fontWeight = FontWeight.Bold)
            Text(state, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !playing, onClick = {
                    state = "Loading audio…"
                    scope.launch {
                        runCatching { session.api.wysAudio() }
                            .onSuccess { body ->
                                when (val r = Wave161Logic.parseWysAudio(body)) {
                                    is Wave161Logic.WysAudioResult.Ready -> {
                                        state = "Playing briefing audio…"
                                        playing = true
                                        Mp3Player.play(r.bytes) { playing = false; state = "Finished." }
                                    }
                                    Wave161Logic.WysAudioResult.NotGenerated ->
                                        state = "No briefing audio has been generated yet."
                                    Wave161Logic.WysAudioResult.Denied ->
                                        state = "Listen is owner-only — the server refused this account."
                                }
                            }
                            .onFailure { state = errText(it) }
                    }
                }) { Text("Listen") }
                if (playing) {
                    OutlinedButton(onClick = { Mp3Player.stop(); playing = false; state = "Stopped." }) { Text("Stop") }
                }
            }
        }
    }
}

@Composable
fun WysListen(session: Session, audio: Wave161Logic.WysAudio) {
    if (!audio.available) {
        Text("Briefing audio has not been generated yet.", style = MaterialTheme.typography.bodySmall)
        return
    }
    WysStandalone(session)
}

object Mp3Player {
    private var thread: Thread? = null
    @Volatile private var player: javazoom.jl.player.Player? = null

    fun play(bytes: ByteArray, onEnd: () -> Unit) {
        stop()
        thread = Thread {
            try {
                val p = javazoom.jl.player.Player(java.io.ByteArrayInputStream(bytes))
                player = p
                p.play()
            } catch (ignored: Exception) {
            } finally {
                onEnd()
            }
        }.also { it.isDaemon = true; it.start() }
    }

    fun stop() {
        runCatching { player?.close() }
        player = null
    }
}

@Composable
fun DashboardScreen(session: Session) {
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<net.sentinelsafeguard.app.data.api.DashboardResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    fun load() {
        scope.launch {
            runCatching { session.api.dashboard() }.onSuccess { data = it; error = null }
                .onFailure { error = errText(it) }
        }
    }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize()) {
        SectionTitle("Dashboard")
        LoadError(error) { load() }
        val d = data ?: run { CircularProgressIndicator(); return@Column }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            d.tickets?.let { tk ->
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Tickets", fontWeight = FontWeight.Bold)
                            Text("Open total: ${tk.openTotal} · Unassigned: ${tk.unassigned}")
                            for (p in tk.byPriority) Text("${p.priority}: ${p.count}")
                            for (st in tk.byStatus) Text("${st.status}: ${st.count}")
                        }
                    }
                }
            }
            d.clients?.let { cl ->
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Clients", fontWeight = FontWeight.Bold)
                            Text("Total: ${cl.total} · Pipeline value (cents): ${cl.pipelineValueCents}")
                            for (st in cl.byStage) Text("${st.stage}: ${st.count}")
                        }
                    }
                }
            }
            d.cases?.let { cs ->
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Cases", fontWeight = FontWeight.Bold)
                            Text("Total: ${cs.total} · Overdue: ${cs.overdue} · Unassigned: ${cs.unassigned}")
                        }
                    }
                }
            }
            d.invoices?.let { inv ->
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Invoices", fontWeight = FontWeight.Bold)
                            Text("Count: ${inv.count} · Drafts: ${inv.draftCount}")
                            Text("Outstanding: ${inv.outstanding.count} · Overdue: ${inv.overdue.count}")
                        }
                    }
                }
            }
            if (d.attention.isNotEmpty()) {
                item { Text("Needs attention", fontWeight = FontWeight.Bold) }
                items(d.attention) { a -> Text("${a.kind}: ${a.count}") }
            }
        }
    }
}

@Composable
fun ClientsScreen(session: Session) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<net.sentinelsafeguard.app.data.api.Client>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    fun load() {
        scope.launch {
            runCatching { session.api.clients(query.ifBlank { null }) }
                .onSuccess { items = it.items; total = it.total; error = null }
                .onFailure { error = errText(it) }
        }
    }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize()) {
        SectionTitle("Clients ($total)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(query, { query = it }, label = { Text("Search clients") }, modifier = Modifier.weight(1f))
            Button(onClick = { load() }) { Text("Search") }
        }
        Spacer(Modifier.height(8.dp))
        LoadError(error) { load() }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { c ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text(c.legalName.ifBlank { c.dba.ifBlank { "Client #${c.id}" } }, fontWeight = FontWeight.Bold)
                        Text("${c.stage} · ${c.status}${if (c.tierState.isNotBlank()) " · Tier: ${c.tierState}" else ""}")
                        if (c.contactName.isNotBlank()) Text(c.contactName + if (c.email.isNotBlank()) " · ${c.email}" else "")
                        TextButton(onClick = { session.selectedClientId = c.id; session.go(Screen.ClientDetail) }) {
                            Text("Open")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClientDetailScreen(session: Session) {
    val scope = rememberCoroutineScope()
    val id = session.selectedClientId
    var client by remember { mutableStateOf<net.sentinelsafeguard.app.data.api.Client?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var tierMsg by remember { mutableStateOf<String?>(null) }
    fun load() {
        if (id == null) return
        scope.launch {
            runCatching { session.api.client(id) }.onSuccess { client = it; error = null }
                .onFailure { error = errText(it) }
        }
    }
    LaunchedEffect(id) { load() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TextButton(onClick = { session.back() }) { Text("← Back") }
        SectionTitle("Client")
        LoadError(error) { load() }
        val c = client ?: run { CircularProgressIndicator(); return@Column }
        Text(c.legalName.ifBlank { c.dba }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Stage: ${c.stage} · Status: ${c.status} · Type: ${c.type} · Industry: ${c.industry}")
        if (c.contactName.isNotBlank()) Text("Contact: ${c.contactName}${if (c.contactTitle.isNotBlank()) " (${c.contactTitle})" else ""}")
        if (c.email.isNotBlank()) Text("Email: ${c.email}")
        if (c.phone.isNotBlank()) Text("Phone: ${c.phone}")
        val addr = listOf(c.street, c.city, c.state, c.zip).filter { it.isNotBlank() }.joinToString(", ")
        if (addr.isNotBlank()) Text("Address: $addr")
        if (c.notes.isNotBlank()) Text("Notes: ${c.notes}")
        Spacer(Modifier.height(12.dp))
        // Tier grant — OWNER only, same gate as the phone: the
        // control renders only for the OWNER role, writes via
        // POST /api/app/clients/{id}/tier, and the server's
        // refusal message renders verbatim.
        val role = session.me?.user?.role ?: ""
        if (role.equals("OWNER", ignoreCase = true)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tier (owner only)", fontWeight = FontWeight.Bold)
                    Text("Current tier: ${c.tierState.ifBlank { "standard" }} — Family is never charged; Friends get 20% off, shown as an explicit discount line.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (tier in listOf("family", "friends", "standard")) {
                            OutlinedButton(onClick = {
                                scope.launch {
                                    runCatching { session.api.setClientTier(c.id, tier) }
                                        .onSuccess { client = it; tierMsg = "Tier set to $tier." }
                                        .onFailure { tierMsg = errText(it) }
                                }
                            }) { Text(tier.replaceFirstChar { it.uppercase() }) }
                        }
                    }
                    tierMsg?.let { Text(it) }
                }
            }
        }
    }
}

@Composable
fun TicketsScreen(session: Session) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<net.sentinelsafeguard.app.data.api.Ticket>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    fun load() {
        scope.launch {
            runCatching { session.api.tickets() }.onSuccess { items = it.items; total = it.total; error = null }
                .onFailure { error = errText(it) }
        }
    }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize()) {
        SectionTitle("Tickets ($total)")
        LoadError(error) { load() }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { t ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text("#${t.id} ${t.title}", fontWeight = FontWeight.Bold)
                        Text("${t.status} · ${t.priority} · ${t.category}${t.clientName?.let { " · $it" } ?: ""}")
                        TextButton(onClick = { session.selectedTicketId = t.id; session.go(Screen.TicketDetail) }) { Text("Open") }
                    }
                }
            }
        }
    }
}

@Composable
fun TicketDetailScreen(session: Session) {
    val scope = rememberCoroutineScope()
    val id = session.selectedTicketId
    var ticket by remember { mutableStateOf<net.sentinelsafeguard.app.data.api.Ticket?>(null) }
    var note by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    fun load() {
        if (id == null) return
        scope.launch {
            runCatching { session.api.ticket(id) }.onSuccess { ticket = it; error = null }
                .onFailure { error = errText(it) }
        }
    }
    LaunchedEffect(id) { load() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TextButton(onClick = { session.back() }) { Text("← Back") }
        SectionTitle("Ticket")
        LoadError(error) { load() }
        val t = ticket ?: run { CircularProgressIndicator(); return@Column }
        Text("#${t.id} ${t.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("${t.status} · ${t.priority} · ${t.category}")
        Text("Opened: ${t.openedDate}${t.dueDate?.let { " · Due: $it" } ?: ""}")
        if (t.notes.isNotBlank()) { Spacer(Modifier.height(6.dp)); Text(t.notes) }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(note, { note = it }, label = { Text("Add a note") }, modifier = Modifier.fillMaxWidth())
        Button(enabled = note.isNotBlank(), onClick = {
            scope.launch {
                runCatching { session.api.addTicketNote(t.id, note) }
                    .onSuccess { ticket = it; note = ""; msg = "Note added." }
                    .onFailure { msg = errText(it) }
            }
        }) { Text("Add note") }
        msg?.let { Text(it) }
    }
}

fun jsonPreview(obj: JsonObject?, limit: Int = 4000): String {
    if (obj == null) return ""
    val s = obj.toString()
    return if (s.length > limit) s.substring(0, limit) + "…" else s
}

/** Generic honest JSON surface for API-driven desks whose phone
 *  rendering is a large mirror tree (money desk, booking admin):
 *  the payload is fetched live from the same twin and rendered as
 *  structured sections — server values only, nothing invented. */
@Composable
fun JsonDeskScreen(session: Session, title: String, loader: suspend (SentinelApi) -> JsonObject) {
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<JsonObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    fun load() {
        scope.launch {
            runCatching { loader(session.api) }.onSuccess { data = it; error = null }
                .onFailure { error = errText(it) }
        }
    }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize()) {
        SectionTitle(title)
        LoadError(error) { load() }
        val d = data ?: run { CircularProgressIndicator(); return@Column }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((key, value) in d.entries) {
                if (key == "ok") continue
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Text(key, fontWeight = FontWeight.Bold)
                            Text(
                                when (value) {
                                    is JsonObject -> jsonPreview(value, 2000)
                                    else -> value.toString().take(2000)
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoneyScreen(session: Session) {
    val scope = rememberCoroutineScope()
    var invoices by remember { mutableStateOf<JsonObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    fun load() {
        scope.launch {
            runCatching { session.api.invoices() }.onSuccess { invoices = it; error = null }
                .onFailure { error = errText(it) }
            runCatching { session.api.moneyDesk() }
        }
    }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize()) {
        SectionTitle("Money desk — invoices")
        LoadError(error) { load() }
        val d = invoices ?: run { CircularProgressIndicator(); return@Column }
        val rows = Phase3Logic.arr(d, "invoices")
        if (rows.isEmpty()) {
            Text("No invoices in the current payload.")
            Text(jsonPreview(d, 1500), style = MaterialTheme.typography.bodySmall)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rows) { inv ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Text(
                                Phase3Logic.str(inv, "number").ifBlank { "Invoice #${Phase3Logic.int(inv, "id")}" },
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "${Phase3Logic.str(inv, "status")} · ${Phase3Logic.str(inv, "clientName")} · " +
                                    "total ${(Phase3Logic.long(inv, "totalCents") ?: 0) / 100} USD",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingScreen(session: Session) {
    JsonDeskScreen(session, "Booking") { it.bookingAdmin() }
}

@Composable
fun OsintScreen(session: Session) {
    val scope = rememberCoroutineScope()
    var workspace by remember { mutableStateOf<JsonObject?>(null) }
    var value by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        runCatching { session.api.osintWorkspace() }.onSuccess { workspace = it }
            .onFailure { error = errText(it) }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionTitle("OSINT")
        LoadError(error) {}
        val ws = workspace
        if (ws != null) {
            val keySources = Phase3Logic.obj(ws, "keySources")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text("Source API keys (status only — values are set in web Command settings, never here)", fontWeight = FontWeight.Bold)
                    if (keySources == null) {
                        Text("Status unavailable")
                    } else {
                        for ((k, v) in keySources.entries) {
                            val configured = (v as? kotlinx.serialization.json.JsonPrimitive)?.content == "true"
                            val isBool = (v as? kotlinx.serialization.json.JsonPrimitive)?.isString == false
                            Text("$k: ${if (!isBool) "Status unavailable" else if (configured) "Configured" else "Not configured"}")
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            val types = Phase3Logic.arrRaw(ws, "types")
            Text("Tool types from the workspace: ${types.size}", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value, { value = it }, label = { Text("Identifier (email, domain, username, IP…)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(purpose, { purpose = it }, label = { Text("Purpose note (required by the server)") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = value.isNotBlank(), onClick = {
                scope.launch {
                    runCatching { session.api.osintDetect(value.trim()) }
                        .onSuccess { result = jsonPreview(it) }
                        .onFailure { result = errText(it) }
                }
            }) { Text("Detect type") }
            Button(enabled = value.isNotBlank() && purpose.isNotBlank(), onClick = {
                scope.launch {
                    val body = buildJsonObject {
                        put("type", "domain")
                        put("value", value.trim())
                        put("purpose", purpose.trim())
                    }
                    runCatching { session.api.osintCreateSearch(body) }
                        .onSuccess { result = "Search queued: " + jsonPreview(it) }
                        .onFailure { result = errText(it) }
                }
            }) { Text("Run search") }
        }
        Text(
            "Searches run through the server's authorization gate; refusals render in the server's own words. " +
                "Hits are possible matches — identity is not verified.",
            style = MaterialTheme.typography.bodySmall,
        )
        result?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
fun CtosScreen(session: Session) {
    val scope = rememberCoroutineScope()
    var workspace by remember { mutableStateOf<JsonObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<String?>(null) }
    var newCaseTitle by remember { mutableStateOf("") }
    fun load() {
        scope.launch {
            runCatching { session.api.ctosWorkspace() }.onSuccess { workspace = it; error = null }
                .onFailure { error = errText(it) }
        }
    }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize()) {
        SectionTitle("CToS tools + cases")
        LoadError(error) { load() }
        val ws = workspace ?: run { CircularProgressIndicator(); return@Column }
        val casesTotal = Phase3Logic.int(ws, "casesTotal")
        val findingsTotal = Phase3Logic.int(ws, "findingsTotal")
        Text("Cases: ${casesTotal ?: "—"} · Findings: ${findingsTotal ?: "—"} (server totals, creator-scoped as the server sends them)")
        val toolMetas = net.sentinelsafeguard.app.ui.CtosLogic.parseTools(ws)
        if (toolMetas.isNotEmpty()) {
            Text(
                "Tools in the server registry (${toolMetas.size}): " +
                    toolMetas.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(newCaseTitle, { newCaseTitle = it }, label = { Text("New case title") }, modifier = Modifier.weight(1f))
            Button(enabled = newCaseTitle.isNotBlank(), onClick = {
                scope.launch {
                    runCatching {
                        session.api.ctosCreateCase(buildJsonObject { put("title", newCaseTitle.trim()) })
                    }.onSuccess { newCaseTitle = ""; detail = "Case created."; load() }
                        .onFailure { detail = errText(it) }
                }
            }) { Text("Create case") }
        }
        Spacer(Modifier.height(8.dp))
        val cases = Phase3Logic.arr(ws, "cases")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cases) { c ->
                val cid = Phase3Logic.int(c, "id") ?: 0
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text(Phase3Logic.str(c, "title").ifBlank { "Case #$cid" }, fontWeight = FontWeight.Bold)
                        Text(Phase3Logic.str(c, "status"))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = {
                                scope.launch {
                                    runCatching { session.api.ctosCase(cid) }
                                        .onSuccess { detail = jsonPreview(it) }
                                        .onFailure {
                                            // 1.7.4 case-scoping dialect: an unknown /
                                            // not-yours case answers 403 with the
                                            // server's sentence — render it as the
                                            // deleted-case end state, not an error dump.
                                            detail = errText(it); load()
                                        }
                                }
                            }) { Text("Open") }
                            TextButton(onClick = {
                                scope.launch {
                                    runCatching { session.api.ctosDeleteCase(cid) }
                                        .onSuccess { detail = "Case deleted."; load() }
                                        .onFailure { detail = errText(it) }
                                }
                            }) { Text("Delete") }
                        }
                    }
                }
            }
            detail?.let { d ->
                item { Text(d, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
fun LiveIntelScreen(session: Session) {
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf("Threat") }
    var markers by remember { mutableStateOf<List<IntelMapLogic.MapMarkerModel>>(emptyList()) }
    var caption by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    fun load(which: String) {
        tab = which
        scope.launch {
            when (which) {
                "Threat" -> runCatching { session.api.liveThreatData() }
                    .onSuccess {
                        val m = IntelMapLogic.parseThreat(it)
                        markers = m.markers
                        caption = "Threat: ${m.attacksToday} attacks today from ${m.sourcesToday} sources" +
                            if (m.stale) " · STALE" else ""
                        error = null
                    }.onFailure { error = errText(it) }
                "Geo" -> runCatching { session.api.liveGeoData() }
                    .onSuccess {
                        val m = IntelMapLogic.parseGeo(it)
                        markers = m.spots
                        caption = "Geo hotspots placed by headline location (the payload carries no coordinates — labeled, as on the phone)."
                        error = null
                    }.onFailure { error = errText(it) }
                else -> runCatching { session.api.liveFlights() }
                    .onSuccess {
                        val m = IntelMapLogic.parseFlights(it)
                        markers = m.aircraft
                        caption = "Flights: aircraft from the live ambient feed."
                        error = null
                    }.onFailure { error = errText(it) }
            }
        }
    }
    LaunchedEffect(Unit) { load("Threat") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionTitle("Live Intel")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (t in listOf("Threat", "Geo", "Flights")) {
                if (tab == t) Button(onClick = {}) { Text(t) }
                else OutlinedButton(onClick = { load(t) }) { Text(t) }
            }
        }
        Spacer(Modifier.height(8.dp))
        LoadError(error) { load(tab) }
        if (caption.isNotBlank()) Text(caption)
        TileMap(
            markers = markers,
            center = if (tab == "Flights") IntelMapLogic.KC_CENTER else IntelMapLogic.WORLD_CENTER,
            initialZoom = if (tab == "Flights") 8 else 2,
            cacheDir = DesktopStore.configDir(),
        )
        if (markers.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            for (m in markers.take(12)) {
                Text("• ${m.title}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun AnalyticsScreen(session: Session) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    fun load() {
        scope.launch {
            runCatching { session.api.analytics() }
                .onSuccess { body ->
                    val state = AnalyticsLogic.parse(body)
                    text = when (state) {
                        is AnalyticsLogic.State.Available ->
                            "Visitors: ${state.stats.visitors} · Pageviews: ${state.stats.pageviews} · " +
                                "Visits: ${state.stats.visits} · Active now: ${state.stats.activeVisitors} · " +
                                "Period: ${state.stats.periodDays} day(s)" +
                                state.stats.topPages.joinToString("") { "\nTop page: ${it.label} (${it.count})" }
                        is AnalyticsLogic.State.Unavailable -> "Analytics unavailable: ${state.reason}"
                        AnalyticsLogic.State.Unauthorized -> "Analytics is admin-only — the server refused this account."
                        AnalyticsLogic.State.Malformed -> "Analytics payload could not be read."
                    }
                    error = null
                }
                .onFailure {
                    text = if (it is ApiException && it.status == 403) {
                        "Analytics is admin-only — the server refused this account (403)."
                    } else null
                    error = if (text == null) errText(it) else null
                }
        }
    }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize()) {
        SectionTitle("Website analytics")
        LoadError(error) { load() }
        Text(text ?: "Loading…")
    }
}

@Composable
fun SettingsScreen(session: Session, store: DesktopStore) {
    var pin by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Settings")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Account", fontWeight = FontWeight.Bold)
                Text("${session.me?.user?.displayName ?: ""} · Role: ${session.me?.user?.role ?: ""}")
                Text("Signed in to ${AppConfig.api.baseUrl} with a device token (same model as the phone).")
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Desktop PIN lock", fontWeight = FontWeight.Bold)
                Text(
                    if (store.hasPin()) "A PIN is set for this desktop install. Unlock is PIN only — there is no biometric row on desktop."
                    else "No PIN set. Set one to lock this desktop install at launch (PIN only — no biometrics on desktop).",
                )
                OutlinedTextField(pin, { pin = it }, label = { Text("New PIN") }, visualTransformation = PasswordVisualTransformation())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = pin.length >= 4, onClick = { store.setPin(pin); pin = ""; msg = "PIN set." }) { Text("Set PIN") }
                    if (store.hasPin()) {
                        OutlinedButton(onClick = { store.clearPin(); msg = "PIN removed." }) { Text("Remove PIN") }
                    }
                }
                msg?.let { Text(it) }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Updates", fontWeight = FontWeight.Bold)
                Text("This desktop edition has no in-app updater — by design. New desktop versions are published on the Sentinel downloads page.")
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Notifications", fontWeight = FontWeight.Bold)
                Text("While this app is open it checks for Command updates about once a minute (self-hosted poll). Nothing is delivered while the app is closed.")
            }
        }
    }
}

@Composable
fun AboutScreen(store: DesktopStore) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("About")
        Text("Command App — Desktop edition 1.0.0", fontWeight = FontWeight.Bold)
        Text("Sentinel Safeguard LLC. The desktop client of Sentinel Command, speaking to the same production host and the same /api/app/* device-token API as the phone app.")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Credential storage", fontWeight = FontWeight.Bold)
                Text(
                    if (DesktopStore.osKeyStorageActive) {
                        "OS key storage is ACTIVE on this machine (Windows DPAPI / Linux Secret Service): your device token is protected by the operating system."
                    } else {
                        "OS key storage is NOT available on this machine: your device token is encrypted with AES-256-GCM under a key kept in a permissions-locked (0600) file in this user's config folder (${DesktopStore.configDir()})."
                    },
                )
            }
        }
        Text("Maps: OpenStreetMap tiles, © OpenStreetMap contributors. Briefing audio playback: JLayer (pure-Java MP3).")
    }
}
