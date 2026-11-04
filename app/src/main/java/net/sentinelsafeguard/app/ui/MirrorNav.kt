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

    /** STAFF shell subset (spec §2.1): same screens, matrix-gated. */
    val STAFF_KEYS: Set<String> =
        setOf("dashboard", "cases", "tickets", "assets", "forms", "schedule", "knowledge")

    /** Non-nav destinations reached from inside built views. */
    val EXTRA_VIEWS: List<MirrorView> = listOf(
        MirrorView("verified", "Sentinel Verified"),
        MirrorView("firmpulse", "Firm Pulse"),
        MirrorView("contract", "Contract"),
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
                    listOf(MirrorView("b4etax", "Tax & Profit")),
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
