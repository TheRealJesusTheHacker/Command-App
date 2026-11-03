package net.sentinelsafeguard.app.ui

/**
 * The mirrored shell's navigation structure (COMMAND-MIRROR-SPEC
 * §2.1): Command's groups, exact labels, exact order, as production
 * renders them for the OWNER shell. A view not yet built in the
 * current phase still appears here under its exact Command name
 * and opens a placeholder naming its phase (phasing-honesty rule)
 * — [phase] is null only for views with a real native screen.
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
                MirrorView("engagements", "Engagements"),
                MirrorView("b4fdesk", "Runbooks & Sign-off"),
            )
        ),
        MirrorGroup(
            "Clients", listOf(
                MirrorView("clients", "Clients"),
                MirrorView("cf2assurance", "Documents & Assurance", PHASE3),
            )
        ),
        MirrorGroup(
            "Money", listOf(
                MirrorView("invoices", "Invoices", PHASE4),
                MirrorView("b3money", "Money Desk", PHASE4),
                MirrorView("b4etax", "Tax & Profit", PHASE4, flag = "taxAccess"),
            )
        ),
        MirrorGroup(
            "Team & Assets", listOf(
                MirrorView("employees", "Employees", PHASE5),
                MirrorView("assets", "Assets", PHASE5),
                MirrorView("forms", "Forms", PHASE5),
            )
        ),
        MirrorGroup(
            "Intel", listOf(
                MirrorView("liveintel", "Live Intel", PHASE6),
                MirrorView("osint", "OSINT", PHASE6, flag = "osintAccess"),
                MirrorView("breachintel", "Breach Intel", PHASE6, flag = "breachAccess"),
                MirrorView("threatintel", "Threat Intel", PHASE6, flag = "threatAccess"),
                MirrorView("sentinelnews", "Sentinel News", PHASE6, flag = "newsAccess"),
                MirrorView("newsdesk", "News Desk", PHASE6, flag = "newsAccess"),
                MirrorView("reviews", "Reviews", PHASE6, flag = "reviewsAccess"),
                MirrorView("law", "Dogg and Associates", PHASE6, flag = "lawAccess"),
            )
        ),
        MirrorGroup(
            "Knowledge", listOf(
                MirrorView("knowledge", "Knowledge", PHASE7),
                MirrorView("archive", "Archive", PHASE7),
            )
        ),
        MirrorGroup(
            "Firm", listOf(
                MirrorView("b3ops", "Portal Settings", PHASE8),
                MirrorView("cf2firmops", "Firm Ops", PHASE8),
                MirrorView("b4assess", "Verified Desk", PHASE8),
                MirrorView("activity", "Activity", PHASE8),
                MirrorView("admin", "Admin"),
            )
        ),
    )

    /** The 12 nav-injected entries, flat after the groups, in
     *  production load order. Sheets mirror as full screens. */
    val INJECTED: List<MirrorView> = listOf(
        MirrorView("b3cops", "Ops Views", PHASE8),
        MirrorView("b3dfield", "Pipeline & Field", PHASE8),
        MirrorView("parts", "Parts & Inventory", PHASE8),
        MirrorView("collections", "Collections", PHASE8),
        MirrorView("staffgear", "Staff & Gear", PHASE8),
        MirrorView("automation", "Automation", PHASE8),
        MirrorView("clientops", "Client Ops", PHASE8),
        MirrorView("pipelinepackets", "Pipeline & Packets", PHASE8),
        MirrorView("cf2reports", "Reports & Feedback", PHASE8),
        MirrorView("b4cadmin", "App & Integrations", PHASE8),
        MirrorView("b4dimport", "Client Import", PHASE8),
        MirrorView("b4jops", "Packets & Routes", PHASE8),
    )

    /** Admin console: 6 groups / 13 sections + injected DMARC.
     *  The console structure renders in hunt5; its sections are
     *  placeholders until Phase 8. */
    val ADMIN_GROUPS: List<MirrorGroup> = listOf(
        MirrorGroup(
            "Overview", listOf(
                MirrorView("overview", "Overview", PHASE8),
                MirrorView("report", "Ops Report", PHASE8),
            )
        ),
        MirrorGroup(
            "People", listOf(
                MirrorView("roles", "Roles", PHASE8),
                MirrorView("members", "Members", PHASE8),
                MirrorView("invites", "Invites", PHASE8),
                MirrorView("sessions", "Sessions", PHASE8),
            )
        ),
        MirrorGroup(
            "Security", listOf(
                MirrorView("security", "Security", PHASE8),
                MirrorView("oversight", "Security Oversight", PHASE8),
                MirrorView("audit", "Audit", PHASE8),
            )
        ),
        MirrorGroup(
            "Law AI", listOf(
                MirrorView("lawai", "Law AI", PHASE8),
            )
        ),
        MirrorGroup(
            "Backups & Data", listOf(
                MirrorView("backups", "Backups", PHASE8),
                MirrorView("dmarc", "DMARC reports", PHASE8),
            )
        ),
        MirrorGroup(
            "Danger Zone", listOf(
                MirrorView("danger", "Danger Zone", PHASE8),
            )
        ),
    )

    /** STAFF shell subset (spec §2.1): same screens, matrix-gated. */
    val STAFF_KEYS: Set<String> =
        setOf("dashboard", "cases", "tickets", "assets", "forms", "schedule", "knowledge")

    /** Non-nav destinations reached from inside built views. */
    val EXTRA_VIEWS: List<MirrorView> = listOf(
        MirrorView("verified", "Sentinel Verified", PHASE3),
    )

    val ALL_VIEWS: Map<String, MirrorView> =
        (GROUPS.flatMap { it.views } + INJECTED + EXTRA_VIEWS +
            ADMIN_GROUPS.flatMap { it.views }).associateBy { it.key }

    fun flagOn(flag: String?, flags: Map<String, Boolean>): Boolean =
        flag == null || flags[flag] == true

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
                    listOf(MirrorView("b4etax", "Tax & Profit", PHASE4)),
                )
            )
        }
        val staff = role == "STAFF"
        return GROUPS.map { group ->
            group.copy(
                views = group.views.filter { view ->
                    flagOn(view.flag, flags) && (!staff || view.key in STAFF_KEYS)
                }
            )
        }.filter { it.views.isNotEmpty() }
    }

    fun visibleInjected(role: String, flags: Map<String, Boolean>, taxOnly: Boolean): List<MirrorView> {
        if (taxOnly || role == "STAFF") {
            // Bundle-injected staff buttons still apply for STAFF in
            // web; OWNER-only ones (Client Ops, App & Integrations)
            // stay owner-only.
            val ownerOnly = setOf("clientops", "b4cadmin")
            return if (taxOnly) emptyList()
            else INJECTED.filter { it.key !in ownerOnly }
        }
        return INJECTED
    }

    fun landingKey(role: String, taxOnly: Boolean): String = when {
        taxOnly -> "b4etax"
        role == "STAFF" -> "dashboard"
        else -> "today"
    }
}
