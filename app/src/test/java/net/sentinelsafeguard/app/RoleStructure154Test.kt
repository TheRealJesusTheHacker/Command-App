package net.sentinelsafeguard.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import net.sentinelsafeguard.app.data.api.MeResponse
import net.sentinelsafeguard.app.data.api.User
import net.sentinelsafeguard.app.ui.MirrorNav
import net.sentinelsafeguard.app.ui.Phase5Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 1.5.4 pins for the role-structure window's app side:
 *
 *  - structureRole is parsed from pub_user/pub_self payloads
 *    (login + /me) and tolerated absent (older payloads);
 *  - shell routing follows the SERVER's role for every role
 *    the app knows; structureRole only resolves roles the
 *    app does NOT know (the server's custom employee roles);
 *  - the retired account roles "Family" / "Tourist" — and
 *    any unrecognized role with no structure to resolve it —
 *    fall into the ONE defensive default (PORTAL, least
 *    privilege), never a privileged path;
 *  - the directory classifier (Phase5Logic.isTeam /
 *    employeeSection) is the same decision as the shell's;
 *  - the assignable-role set and the directory filter set no
 *    longer offer the retired roles.
 *
 * The /me fixture is the rehearsal server's real pub_self
 * shape (2026-11-09, analytics-rehearsal/goldens/me_admin.json
 * — OwnerAX became OwnerRS here; the shape is byte-identical).
 */
class RoleStructure154Test {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    // ---- parsing ----

    @Test
    fun mePayloadCarriesStructureRole() {
        val me = json.decodeFromString<MeResponse>(ME_OWNER)
        assertEquals("OWNER", me.user.role)
        assertEquals("employee", me.user.structureRole)
        assertEquals("Employee", MirrorNav.structureLabel(me.user.structureRole))
    }

    @Test
    fun clientPayloadCarriesClientStructure() {
        val user = json.decodeFromString<User>(
            """{"id": 7, "displayName": "Portal Person", "role": "Client",
                "structureRole": "client", "clientId": 42}""",
        )
        assertEquals("client", user.structureRole)
        assertEquals("Client", MirrorNav.structureLabel(user.structureRole))
    }

    @Test
    fun olderPayloadWithoutStructureRoleParsesAndShowsNoChip() {
        val user = json.decodeFromString<User>(
            """{"id": 3, "displayName": "Legacy", "role": "STAFF"}""",
        )
        assertNull(user.structureRole)
        assertNull(MirrorNav.structureLabel(user.structureRole))
        assertNull(MirrorNav.structureLabel("something-else"))
        assertNull(MirrorNav.structureLabel(null))
    }

    // ---- shell routing ----

    @Test
    fun knownRolesRouteByRoleAlone() {
        assertEquals(MirrorNav.ShellKind.PORTAL, MirrorNav.shellKind("Client", false, null))
        assertEquals(MirrorNav.ShellKind.PORTAL, MirrorNav.shellKind("Client", false, "employee"))
        assertEquals(MirrorNav.ShellKind.COMMAND, MirrorNav.shellKind("OWNER", false, null))
        assertEquals(MirrorNav.ShellKind.COMMAND, MirrorNav.shellKind("Admin", false, "client"))
        assertEquals(MirrorNav.ShellKind.COMMAND, MirrorNav.shellKind("STAFF", false, null))
        assertEquals(MirrorNav.ShellKind.COMMAND, MirrorNav.shellKind("Subcontractor", false, null))
        assertFalse(MirrorNav.shellDefaulted("Client", false, null))
        assertFalse(MirrorNav.shellDefaulted("OWNER", false, null))
    }

    @Test
    fun portalFlagRemainsAuthoritative() {
        assertEquals(MirrorNav.ShellKind.PORTAL, MirrorNav.shellKind("OWNER", true, null))
        assertEquals(MirrorNav.ShellKind.PORTAL, MirrorNav.shellKind("STAFF", true, "employee"))
        assertFalse(MirrorNav.shellDefaulted("OWNER", true, null))
    }

    @Test
    fun structureRoleResolvesRolesTheAppDoesNotKnow() {
        // The server's custom employee roles arrive with
        // structure "employee" and keep their Command shell;
        // an unknown client-structure role lands in portal.
        assertEquals(
            MirrorNav.ShellKind.COMMAND,
            MirrorNav.shellKind("Accountant", false, "employee"),
        )
        assertEquals(
            MirrorNav.ShellKind.PORTAL,
            MirrorNav.shellKind("Accountant", false, "client"),
        )
        assertFalse(MirrorNav.shellDefaulted("Accountant", false, "employee"))
        assertFalse(MirrorNav.shellDefaulted("Accountant", false, "client"))
    }

    @Test
    fun retiredRolesFallIntoTheDefensiveDefault() {
        // Every combination — flag on or off, either structure
        // value, none — lands in PORTAL, and the default is
        // reported as defaulted (the shell logs it).
        for (role in listOf("Family", "Tourist")) {
            for (flag in listOf(false, true)) {
                for (structure in listOf(null, "employee", "client")) {
                    assertEquals(
                        "$role flag=$flag structure=$structure",
                        MirrorNav.ShellKind.PORTAL,
                        MirrorNav.shellKind(role, flag, structure),
                    )
                    assertTrue(
                        "$role flag=$flag structure=$structure",
                        MirrorNav.shellDefaulted(role, flag, structure),
                    )
                }
            }
        }
    }

    @Test
    fun unrecognizedRoleWithoutStructureDefaultsToPortal() {
        assertEquals(
            MirrorNav.ShellKind.PORTAL,
            MirrorNav.shellKind("Wizard", false, null),
        )
        assertTrue(MirrorNav.shellDefaulted("Wizard", false, null))
        assertEquals(
            MirrorNav.ShellKind.PORTAL,
            MirrorNav.shellKind("", false, null),
        )
        assertTrue(MirrorNav.shellDefaulted("", false, null))
    }

    // ---- the directory is the same decision ----

    private fun row(role: String, structure: String? = null) =
        Json.parseToJsonElement(
            if (structure == null) """{"role": "$role"}"""
            else """{"role": "$role", "structureRole": "$structure"}""",
        ).jsonObject

    @Test
    fun directoryTeamClassificationMatchesTheShell() {
        assertTrue(Phase5Logic.isTeam(row("OWNER")))
        assertTrue(Phase5Logic.isTeam(row("STAFF", "employee")))
        assertTrue(Phase5Logic.isTeam(row("Accountant", "employee")))
        assertFalse(Phase5Logic.isTeam(row("Client")))
        assertFalse(Phase5Logic.isTeam(row("Client", "client")))
        assertFalse(Phase5Logic.isTeam(row("Family", "client")))
        assertFalse(Phase5Logic.isTeam(row("Tourist", "employee")))
        assertFalse(Phase5Logic.isTeam(row("Wizard")))
        assertEquals("Team", Phase5Logic.employeeSection(row("OWNER")))
        assertEquals("Client access", Phase5Logic.employeeSection(row("Client", "client")))
        assertEquals("Client access", Phase5Logic.employeeSection(row("Tourist", "employee")))
    }

    // ---- the retired offers are gone ----

    @Test
    fun assignableRolesAndDirectoryFiltersExcludeTheRetired() {
        assertEquals(
            listOf("OWNER", "STAFF", "Admin", "Client"),
            Phase5Logic.ASSIGNABLE_ROLES,
        )
        assertFalse(Phase5Logic.ASSIGNABLE_ROLES.contains("Family"))
        assertFalse(Phase5Logic.ASSIGNABLE_ROLES.contains("Tourist"))
        assertFalse(Phase5Logic.EMPLOYEE_ROLE_FILTERS.any { it.second == "Demo" })
        assertFalse(Phase5Logic.EMPLOYEE_ROLE_FILTERS.any { it.first == "Demo access" })
        assertEquals(setOf("Family", "Tourist"), MirrorNav.RETIRED_ROLES)
        assertEquals(
            setOf("OWNER", "STAFF", "Admin", "Subcontractor"),
            MirrorNav.KNOWN_EMPLOYEE_ROLES,
        )
    }

    private companion object {
        // The staging rehearsal server's real /api/app/me body
        // (2026-11-09; display name normalized).
        const val ME_OWNER = """
            {"ok": true, "user": {"id": 1, "displayName": "OwnerRS", "role": "OWNER",
             "portalLang": "en", "structureRole": "employee", "clientId": null,
             "jobTitle": "", "email": "", "phone": "", "active": true,
             "createdAt": 1794208109522, "mustReset": false, "totpEnrolled": false,
             "totpEnrollmentRequired": false},
             "serverTime": 1794208110000, "device": null, "portal": false}
        """
    }
}
