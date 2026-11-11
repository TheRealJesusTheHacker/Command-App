package net.sentinelsafeguard.app.ui

import net.sentinelsafeguard.app.R

/**
 * The mirrored shell's navigation structure (COMMAND-MIRROR-SPEC
 * §2.1): Command's groups, exact labels, exact order, as production
 * renders them for the OWNER shell. As of Phase 9 (hunt14) every
 * view is built — the phasing-honesty placeholder rule has run
 * its course and NO placeholder screens remain in the app; the
 * [phase] field survives only as historical record on the type
 * and is null for every view.
 *
 * Access flags gate exactly like web: a flagged view renders only
 * when the matching flag rides the /api/app/me payload (B0).
 */
data class MirrorView(
    val key: String,
    val label: String,
    val phase: String? = null,
    val flag: String? = null,
)

data class MirrorGroup(val label: String, val views: List<MirrorView>)

object MirrorNav {
    const val PHASE2 = "Phase 2"
    const val PHASE3 = "Phase 3"
    const val PHASE4 = "Phase 4"
    const val PHASE5 = "Phase 5"
    const val PHASE6 = "Phase 6"
    const val PHASE7 = "Phase 7"
    const val PHASE8 = "Phase 8"
    const val PHASE9 = "Phase 9"

    val GROUPS: List<MirrorGroup> = listOf(
        MirrorGroup(
            "Today", listOf(
                MirrorView("today", "Today"),
                MirrorView("dashboard", "Dashboard"),
            )
        ),
        MirrorGroup(
            "Work", listOf(
                MirrorView("tickets", "Tickets"),
                MirrorView("cases", "Cases"),
                MirrorView("schedule", "Schedule"),
                // STAFF-only (Phase 9): web's STAFF shell carries
                // My Schedule instead of the full Schedule view;
                // visibleGroups enforces the swap per role.
                MirrorView("myschedule", "My Schedule"),
                MirrorView("engagements", "Engagements"),
                MirrorView("b4fdesk", "Runbooks & Sign-off"),
            )
        ),
        MirrorGroup(
            "Clients", listOf(
                MirrorView("clients", "Clients"),
                MirrorView("cf2assurance", "Documents & Assurance"),
            )
        ),
        MirrorGroup(
            "Money", listOf(
                MirrorView("invoices", "Invoices"),
                MirrorView("b3money", "Money Desk"),
                MirrorView("b4etax", "Tax & Profit", flag = "taxAccess"),
            )
        ),
        MirrorGroup(
            "Team & Assets", listOf(
                MirrorView("employees", "Employees"),
                MirrorView("assets", "Assets"),
                MirrorView("forms", "Forms"),
            )
        ),
        MirrorGroup(
            "Intel", listOf(
                MirrorView("liveintel", "Live Intel"),
                MirrorView("osint", "OSINT", flag = "osintAccess"),
                MirrorView("breachintel", "Breach Intel", flag = "breachAccess"),
                MirrorView("threatintel", "Threat Intel", flag = "threatAccess"),
                MirrorView("sentinelnews", "Sentinel News", flag = "newsAccess"),
                MirrorView("newsdesk", "News Desk", flag = "newsAccess"),
                MirrorView("reviews", "Reviews", flag = "reviewsAccess"),
                MirrorView("law", "Dogg and Associates", flag = "lawAccess"),
            )
        ),
        MirrorGroup(
            "Knowledge", listOf(
                MirrorView("knowledge", "Knowledge"),
                MirrorView("archive", "Archive"),
            )
        ),
        MirrorGroup(
            "Firm", listOf(
                MirrorView("b3ops", "Portal Settings"),
                MirrorView("cf2firmops", "Firm Ops"),
                MirrorView("b4assess", "Verified Desk"),
                MirrorView("activity", "Activity"),
                MirrorView("admin", "Admin"),
            )
        ),
    )

    /** The 12 nav-injected entries, flat after the groups, in
     *  production load order. Sheets mirror as full screens. */
    val INJECTED: List<MirrorView> = listOf(
        MirrorView("b3cops", "Ops Views"),
        MirrorView("b3dfield", "Pipeline & Field"),
        MirrorView("parts", "Parts & Inventory"),
        MirrorView("collections", "Collections"),
        MirrorView("staffgear", "Staff & Gear"),
        MirrorView("automation", "Automation"),
        MirrorView("clientops", "Client Ops"),
        MirrorView("pipelinepackets", "Pipeline & Packets"),
        MirrorView("cf2reports", "Reports & Feedback"),
        MirrorView("b4cadmin", "App & Integrations"),
        MirrorView("b4dimport", "Client Import"),
        MirrorView("b4jops", "Packets & Routes"),
    )

    /** Admin console: 6 groups / 13 sections + injected DMARC.
     *  Sections are built mirrors as of Phase 8B (hunt13). */
    val ADMIN_GROUPS: List<MirrorGroup> = listOf(
        MirrorGroup(
            "Overview", listOf(
                MirrorView("overview", "Overview"),
                MirrorView("report", "Ops Report" ),
            )
        ),
        MirrorGroup(
            "People", listOf(
                MirrorView("roles", "Roles" ),
                MirrorView("members", "Members" ),
                MirrorView("invites", "Invites" ),
                MirrorView("sessions", "Sessions" ),
            )
        ),
        MirrorGroup(
            "Security", listOf(
                MirrorView("security", "Security" ),
                MirrorView("oversight", "Security Oversight" ),
                MirrorView("audit", "Audit" ),
            )
        ),
        MirrorGroup(
            "Law AI", listOf(
                MirrorView("lawai", "Law AI" ),
            )
        ),
        MirrorGroup(
            "Backups & Data", listOf(
                MirrorView("backups", "Backups" ),
                MirrorView("dmarc", "DMARC reports" ),
            )
        ),
        MirrorGroup(
            "Danger Zone", listOf(
                MirrorView("danger", "Danger Zone" ),
            )
        ),
    )

    /** STAFF shell subset (spec §2.1, verified Phase 9): the
     *  drawer shows EXACTLY these views for STAFF — Dashboard,
     *  Cases, Tickets, Assets, Forms, My Schedule, Knowledge —
     *  and every other Command view is absent (not disabled).
     *  My Schedule replaces the full Schedule view for STAFF
     *  (web's STAFF shell carries the caller-scoped payload). */
    val STAFF_KEYS: Set<String> =
        setOf("dashboard", "cases", "tickets", "assets", "forms", "myschedule", "knowledge")

    /** The portal shell (spec §2.1, Phase 9): client-structure
     *  sign-ins get these twelve views in web's names/order —
     *  a separate shell, never the staff drawer. The named
     *  backend deferrals (Addendum 28) are NOT views here and
     *  get no entries. */
    val PORTAL_VIEWS: List<MirrorView> = listOf(
        MirrorView("portal_profile", "My Profile"),
        MirrorView("portal_progress", "Progress"),
        MirrorView("portal_history", "History"),
        MirrorView("portal_documents", "Your Documents"),
        MirrorView("portal_files", "My Files & Packets"),
        MirrorView("portal_messages", "Messages"),
        MirrorView("portal_alert", "Critical Alert"),
        MirrorView("portal_signoffs", "My Sign-offs"),
        MirrorView("portal_contracts", "Contracts & Reports"),
        MirrorView("portal_knowledge", "Knowledge"),
        MirrorView("portal_status", "Status Board"),
        MirrorView("portal_feedback", "Feedback & Referrals"),
    )

    /** The employee-structure role names the app knows
     *  outright. Any OTHER employee role (the server's custom
     *  roles) is recognized through structureRole instead —
     *  see [shellKind]. */
    val KNOWN_EMPLOYEE_ROLES: Set<String> =
        setOf("OWNER", "STAFF", "Admin", "Subcontractor")

    /** Account roles the role-structure window RETIRED:
     *  production refuses to mint either, and no production
     *  user carries one. They are named here for exactly one
     *  purpose — the retirement guard in [shellKind]. The
     *  guard exists because the demo engine derives Tourist
     *  as employee-structure server-side, so structure alone
     *  must never admit the string; both retired strings fall
     *  into the same defensive default as every unrecognized
     *  role (the portal shell — least privilege). There is no
     *  per-role behavior left for either: the old
     *  portal-by-Family branch and refused-by-Tourist shell
     *  were removed in 1.5.4. */
    val RETIRED_ROLES: Set<String> = setOf("Family", "Tourist")

    enum class ShellKind { COMMAND, PORTAL }

    /**
     * Which shell an account lands in (1.5.4, on the
     * role-structure window's contracts). The server is
     * truth, in this order:
     *  1. a retired role string can never arrive from
     *     production; if one somehow does, it gets the
     *     defensive default, never a privileged shell;
     *  2. the portal flag on /api/app/me (Phase 9) is
     *     authoritative, and the "Client" role agrees with it;
     *  3. the known employee roles land in Command;
     *  4. structureRole resolves the roles the app does not
     *     know by name (the server's custom employee roles) —
     *     it is consulted ONLY for those: when structureRole
     *     and a KNOWN role ever disagree, the role wins
     *     (branches 2–3 run first);
     *  5. anything else — an unrecognized string with no
     *     structure to resolve it — takes the defensive
     *     default: the PORTAL shell (least privilege,
     *     client-safe, server-scoped), never Command. The
     *     shell logs that the default fired (see
     *     [shellDefaulted]).
     */
    fun shellKind(role: String, portalFlag: Boolean, structureRole: String? = null): ShellKind =
        when {
            role in RETIRED_ROLES -> ShellKind.PORTAL
            portalFlag || role == "Client" -> ShellKind.PORTAL
            role in KNOWN_EMPLOYEE_ROLES -> ShellKind.COMMAND
            structureRole == "client" -> ShellKind.PORTAL
            structureRole == "employee" -> ShellKind.COMMAND
            else -> ShellKind.PORTAL
        }

    /** True when [shellKind] reached the portal shell through
     *  the retirement guard or the unrecognized-role default
     *  (branches 1 and 5) rather than through an affirmative
     *  client signal — the shell logs those so a payload the
     *  app cannot classify is visible in diagnostics, never
     *  silently privileged. */
    fun shellDefaulted(role: String, portalFlag: Boolean, structureRole: String? = null): Boolean =
        role in RETIRED_ROLES ||
            (!portalFlag && role != "Client" && role !in KNOWN_EMPLOYEE_ROLES &&
                structureRole != "client" && structureRole != "employee")

    /** The Settings chip for a structure value, verbatim from
     *  the server's lowercase vocabulary; anything else (an
     *  older payload without the field, an unknown value)
     *  shows NO chip — the app derives nothing in its place. */
    fun structureLabel(structureRole: String?): String? = when (structureRole) {
        "employee" -> "Employee"
        "client" -> "Client"
        else -> null
    }

    fun portalLandingKey(): String = PORTAL_VIEWS.first().key

    /** Non-nav destinations reached from inside built views. */
    val EXTRA_VIEWS: List<MirrorView> = listOf(
        MirrorView("verified", "Sentinel Verified"),
        MirrorView("firmpulse", "Firm Pulse"),
        MirrorView("contract", "Contract"),
    )

    val ALL_VIEWS: Map<String, MirrorView> =
        (GROUPS.flatMap { it.views } + INJECTED + EXTRA_VIEWS + PORTAL_VIEWS +
            ADMIN_GROUPS.flatMap { it.views }).associateBy { it.key }

    fun flagOn(flag: String?, flags: Map<String, Boolean>): Boolean =
        flag == null || flags[flag] == true

    /**
     * AUDIT-171 B-26: may this account follow a notification
     * deep link to [route]? The NavHost registers every Command
     * route for every Command-shell account, and the drawer is
     * the only place role/flag restrictions were applied — an
     * intent-supplied route sailed past them. The link now lands
     * only when its section is one this account can actually see
     * (the same [visibleGroups] the drawer renders); anything
     * else is dropped, exactly like an unresolvable route.
     * (Server-side authorization still governs the DATA — this
     * guard is about not navigating a restricted account into a
     * surface its role never sees.)
     */
    fun deepLinkAllowed(
        route: String,
        role: String,
        flags: Map<String, Boolean>,
        taxOnly: Boolean,
    ): Boolean {
        val section = when (route.substringBefore("/")) {
            "ticket" -> "tickets"
            "invoice" -> "invoices"
            "client" -> "clients"
            else -> route.substringBefore("/")
        }
        return visibleGroups(role, flags, taxOnly)
            .any { group -> group.views.any { it.key == section } }
    }

    /** The views visible for a role + flag set, groups in order,
     *  empty groups dropped. taxOnly accounts see only Tax & Profit. */
    fun visibleGroups(
        role: String,
        flags: Map<String, Boolean>,
        taxOnly: Boolean,
    ): List<MirrorGroup> {
        if (taxOnly) {
            return listOf(
                MirrorGroup(
                    "Money",
                    listOf(MirrorView("b4etax", "Tax & Profit")),
                )
            )
        }
        val staff = role == "STAFF"
        return GROUPS.map { group ->
            group.copy(
                views = group.views.filter { view ->
                    flagOn(view.flag, flags) &&
                        // My Schedule is the STAFF shell's entry;
                        // the Command shell does not carry it.
                        (staff || view.key != "myschedule") &&
                        (!staff || view.key in STAFF_KEYS)
                }
            )
        }.filter { it.views.isNotEmpty() }
    }

    fun visibleInjected(role: String, flags: Map<String, Boolean>, taxOnly: Boolean): List<MirrorView> {
        // Phase 9 STAFF verification: the STAFF drawer shows
        // exactly the STAFF subset — the injected entries are
        // absent for STAFF, as every other non-subset view is.
        if (taxOnly || role == "STAFF") return emptyList()
        return INJECTED
    }

    fun landingKey(role: String, taxOnly: Boolean): String = when {
        taxOnly -> "b4etax"
        role == "STAFF" -> "dashboard"
        else -> "today"
    }
}


/**
 * hunt18 drawer iconography: one Material Symbol per view,
 * shipped as in-repo vector drawables (res/drawable/ic_nav_*),
 * tinted muted at rest and emerald when the item is active.
 * The mapping is keyed by the same view keys GROUPS / INJECTED /
 * ADMIN_GROUPS / PORTAL_VIEWS / EXTRA_VIEWS already carry —
 * labels, groups, and order are untouched; the icons are
 * decoration beside the unchanged labels. Utility entries
 * (search / settings / signout) map by their route keys.
 * The full mapping is recorded in the hunt18 BUILD-RECORD.
 */
fun mirrorViewIcon(key: String): Int = when (key) {
    "today" -> R.drawable.ic_nav_today
    "dashboard" -> R.drawable.ic_nav_dashboard
    "tickets" -> R.drawable.ic_nav_tickets
    "cases" -> R.drawable.ic_nav_cases
    "schedule" -> R.drawable.ic_nav_schedule
    "myschedule" -> R.drawable.ic_nav_myschedule
    "engagements" -> R.drawable.ic_nav_engagements
    "b4fdesk" -> R.drawable.ic_nav_b4fdesk
    "clients" -> R.drawable.ic_nav_clients
    "cf2assurance" -> R.drawable.ic_nav_cf2assurance
    "invoices" -> R.drawable.ic_nav_invoices
    "b3money" -> R.drawable.ic_nav_b3money
    "b4etax" -> R.drawable.ic_nav_b4etax
    "employees" -> R.drawable.ic_nav_employees
    "assets" -> R.drawable.ic_nav_assets
    "forms" -> R.drawable.ic_nav_forms
    "liveintel" -> R.drawable.ic_nav_liveintel
    "osint" -> R.drawable.ic_nav_osint
    "breachintel" -> R.drawable.ic_nav_breachintel
    "threatintel" -> R.drawable.ic_nav_threatintel
    "sentinelnews" -> R.drawable.ic_nav_sentinelnews
    "newsdesk" -> R.drawable.ic_nav_newsdesk
    "reviews" -> R.drawable.ic_nav_reviews
    "law" -> R.drawable.ic_nav_law
    "knowledge" -> R.drawable.ic_nav_knowledge
    "archive" -> R.drawable.ic_nav_archive
    "b3ops" -> R.drawable.ic_nav_b3ops
    "cf2firmops" -> R.drawable.ic_nav_cf2firmops
    "b4assess" -> R.drawable.ic_nav_b4assess
    "activity" -> R.drawable.ic_nav_activity
    "admin" -> R.drawable.ic_nav_admin
    "b3cops" -> R.drawable.ic_nav_b3cops
    "b3dfield" -> R.drawable.ic_nav_b3dfield
    "parts" -> R.drawable.ic_nav_parts
    "collections" -> R.drawable.ic_nav_collections
    "staffgear" -> R.drawable.ic_nav_staffgear
    "automation" -> R.drawable.ic_nav_automation
    "clientops" -> R.drawable.ic_nav_clientops
    "pipelinepackets" -> R.drawable.ic_nav_pipelinepackets
    "cf2reports" -> R.drawable.ic_nav_cf2reports
    "b4cadmin" -> R.drawable.ic_nav_b4cadmin
    "b4dimport" -> R.drawable.ic_nav_b4dimport
    "b4jops" -> R.drawable.ic_nav_b4jops
    "overview" -> R.drawable.ic_nav_overview
    "report" -> R.drawable.ic_nav_report
    "roles" -> R.drawable.ic_nav_roles
    "members" -> R.drawable.ic_nav_members
    "invites" -> R.drawable.ic_nav_invites
    "sessions" -> R.drawable.ic_nav_sessions
    "security" -> R.drawable.ic_nav_security
    "oversight" -> R.drawable.ic_nav_oversight
    "audit" -> R.drawable.ic_nav_audit
    "lawai" -> R.drawable.ic_nav_lawai
    "backups" -> R.drawable.ic_nav_backups
    "dmarc" -> R.drawable.ic_nav_dmarc
    "danger" -> R.drawable.ic_nav_danger
    "portal_profile" -> R.drawable.ic_nav_portal_profile
    "portal_progress" -> R.drawable.ic_nav_portal_progress
    "portal_history" -> R.drawable.ic_nav_portal_history
    "portal_documents" -> R.drawable.ic_nav_portal_documents
    "portal_files" -> R.drawable.ic_nav_portal_files
    "portal_messages" -> R.drawable.ic_nav_portal_messages
    "portal_alert" -> R.drawable.ic_nav_portal_alert
    "portal_signoffs" -> R.drawable.ic_nav_portal_signoffs
    "portal_contracts" -> R.drawable.ic_nav_portal_contracts
    "portal_knowledge" -> R.drawable.ic_nav_portal_knowledge
    "portal_status" -> R.drawable.ic_nav_portal_status
    "portal_feedback" -> R.drawable.ic_nav_portal_feedback
    "verified" -> R.drawable.ic_nav_verified
    "firmpulse" -> R.drawable.ic_nav_firmpulse
    "contract" -> R.drawable.ic_nav_contract
    "search" -> R.drawable.ic_nav_search
    "settings" -> R.drawable.ic_nav_settings
    "signout" -> R.drawable.ic_nav_signout
    else -> R.drawable.ic_nav_dashboard
}

/**
 * Polish Pass 1 (hunt15) — visual fidelity: the exact view
 * titles and header subtitles production Command renders, as
 * extracted from the staging static tree (hash-recorded in the
 * hunt15 build record; staging index.html has drifted from the
 * Phase 0 production hash, so these strings were re-read from
 * the staging files themselves, not assumed from the inventory).
 * Web's <h1> sometimes differs from its nav label (Operations
 * Views, Pipeline & Field Jobs, Client CSV Import, Firm
 * Operations, OSINT Toolkit) — [VIEW_TITLES] carries those.
 */
object MirrorCopy {
    val VIEW_TITLES: Map<String, String> = mapOf(
        "b3cops" to "Operations Views",
        "b3dfield" to "Pipeline & Field Jobs",
        "b4dimport" to "Client CSV Import",
        "cf2firmops" to "Firm Operations",
        "osint" to "OSINT Toolkit",
    )

    fun title(key: String, fallback: String): String = VIEW_TITLES[key] ?: fallback

    val SUBTITLES: Map<String, String> = mapOf(
        "cases" to "Every engagement as a working case — board, directory, and full case files.",
        "assets" to "Two registers, kept separate: Sentinel's own equipment first, then the gear tracked for clients.",
        "invoices" to "Real billing documents — line items, tax, a true lifecycle, and a printable invoice for every client.",
        "employees" to "The team directory — who is here, what each person is carrying, and the work still waiting for an owner.",
        "schedule" to "Every job and every person on one calendar — pick a person to see only their jobs.",
        "myschedule" to "Only the jobs assigned to you appear here.",
        "activity" to "Everything your team has done in Command, newest first. The security console — IP addresses, devices, and sign-in attempts — is owner-only.",
        "law" to "Legal research workspace and document drafting — for Sentinel's own use.",
        "sentinelnews" to "What the public sees on the Sentinel News page — published items only, newest first. Drafts are worked on the News Desk tab.",
        "newsdesk" to "The desk behind Sentinel News on the public site — drafts live here, automation-filed stories arrive already published, and anything live can be unpublished with a tap.",
        "reviews" to "Client reviews submitted through one-time review-request links. A review appears on the public website only when you publish it here — pending and hidden reviews are never exported.",
        "b3money" to "Build quotes from the price book, convert unbilled work to draft invoices, review purchase drafts, and work the payment board. Automations create DRAFTS only — nothing is sent or ordered.",
        "b4etax" to "The tax-year pack (invoices, expenses, mileage, payments, summary PDF) and Firm Pulse profit rollups. Figures are computed live from Command records, America/Chicago.",
        "b3ops" to "Subcontractor assignments, one-time secrets, client one-pagers, portal tips and the review showcase.",
        "engagements" to "Contracts & e-signature, pentest engagements, findings, and the findings library. Findings cannot be added until a pentest authorization for that client is fully signed (client signature + owner countersignature).",
        "cf2firmops" to "Job costing, Sentinel's own vendors, the internal task board, and staff availability. Internal only — none of this is ever shown to clients.",
        "cf2assurance" to "Document packets, client file requests, the backup restore-test log, and the vulnerability scan register. A scan can only be recorded for a client with a fully signed pentest authorization on file.",
        "b4assess" to "Guided security assessments, Sentinel Verified badge issuance, review responses, and generated documents in the client's language.",
        "osint" to "Public-source checks on identifiers already in hand — every run needs a purpose note and is written to the activity log. Client and demo accounts never see this tool.",
        "breachintel" to "A living register of recent data breaches and where the stolen data surfaced — as publicly reported. This register cites reporting only: it never links to a leak or download, and never stores leaked data.",
        "threatintel" to "One fused feed from the free sources verified live for this workspace — CISA's Known Exploited Vulnerabilities catalog, NVD critical CVEs, CISA advisories, and reputable security reporting — deduped by CVE, URL, and headline, and scored by a documented deterministic rule (source class, KEV ransomware/due-date pressure, exploitation signals, corroboration). The score is a triage sort, not a risk measurement. Nothing here auto-publishes: filing an item to the News Desk is a deliberate tap and always lands as a draft.",
        "b3cops" to "Read-only planning surfaces computed live from the existing record. Staff only. Family-linked clients never show money figures here.",
        "b3dfield" to "Service playbooks, today's field jobs, staged asset import, and transparent lead scoring with source ROI. Nothing here sends anything.",
        "cf2reports" to "Monthly client reports, satisfaction surveys, the referral tracker, and per-client threat briefs. Everything here is a draft until the owner sends it — threat briefs are never emailed by Command.",
        "b4cadmin" to "Command App devices, partner lead API keys, and outbound webhooks. Keys and signing secrets are shown once, at creation only. Webhook deliveries run on the automation tick with backoff (60s, 5m, 30m, 2h; 5 attempts, then dead-letter) — never inline.",
        "b4dimport" to "Bulk-import clients into the onboarding review queue. A dry run reports every row first — duplicates flagged with their match reasons, invalid rows with line numbers — and confirming only STAGES rows in the review queue. No client is created by an import: a person merges each staged row in the wizard, and a row that matches an existing client is staged against that client so approving updates it instead of creating a second record. A family flag in the file is a suggestion for the reviewer only — the import never sets a Family link.",
        "b4jops" to "Paper invoice packets for clients with no email on file, the day route planner, warranty claims, retainer statement drafts and urgent-alert routing. Nothing here sends anything: packets are posted by hand, statements are drafts.",
        "search" to "Grouped by module. A module your role cannot open never appears — not even as a count.",
        "portal_profile" to "Your Sentinel Safeguard client portal — your profile, and the work we hold for you.",
        "portal_progress" to "Where your work stands — your tickets and cases on one timeline: Received, In Progress, Resolved. Newest first.",
        "portal_history" to "Your work with Sentinel Safeguard in one timeline, plus your current security tip and any one-time secrets waiting for you.",
        "portal_documents" to "Your invoices and the documents we hold for you — only yours, from your own client record.",
        "portal_files" to "Document packets Sentinel has issued to you, and files Sentinel has asked you to upload (PDF).",
        "portal_messages" to "Message Sentinel Safeguard about a ticket — the thread lives here in your portal; email is only a notification.",
        "portal_alert" to "For an emergency happening right now — a Critical Alert files a ticket at Critical priority, the highest in our system.",
        "portal_contracts" to "Contracts sent to you for signature, and your published engagement reports.",
        "portal_feedback" to "Surveys Sentinel has asked you about, and the referrals you have given.",
        "portal_knowledge" to "The practice library — codes, standards, and law, housed on site and readable here.",
    )

    fun subtitle(key: String): String? = SUBTITLES[key]

    /** Web Today's header line: "<Weekday, Month D, YYYY> — what
     *  needs you first." in America/Chicago (production fmt). */
    fun todaySubtitle(
        now: java.time.ZonedDateTime =
            java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Chicago")),
    ): String {
        val fmt = java.time.format.DateTimeFormatter.ofPattern(
            "EEEE, MMMM d, yyyy", java.util.Locale.US,
        )
        return "${now.format(fmt)} — what needs you first."
    }
}
