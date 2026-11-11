package net.sentinelsafeguard.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import net.sentinelsafeguard.app.ui.CtosBuilders
import net.sentinelsafeguard.app.ui.CtosLogic
import net.sentinelsafeguard.app.ui.OsintLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AUDIT-171 OSINT fixes: builder port fidelity (B-16), workspace
 * gate/profile + gated-flag parsing (B-17), tool-result findings
 * and source URLs (B-19), and the URL scheme allowlist (M-05).
 * Builder expectations mirror the web shell's composition
 * functions byte-for-byte.
 */
class Audit171OsintTest {

    private fun obj(text: String): JsonObject =
        Json.parseToJsonElement(text) as JsonObject

    // ---- B-16: builders ------------------------------------------------

    @Test
    fun shqQuotesLikeTheWeb() {
        assertEquals("'plain'", CtosBuilders.shq("plain"))
        assertEquals("'a'\\''b'", CtosBuilders.shq("a'b"))
        assertEquals("''", CtosBuilders.shq(""))
    }

    @Test
    fun nmapBuilderMatchesWebDefaults() {
        val cmds = CtosBuilders.commands(
            "buildernmap", "scan.example.invalid",
            values = mapOf("scan" to "-sV", "ports" to "22,80,443"),
            checked = setOf("t4"),
        )
        assertEquals(
            listOf("nmap -sV -p '22,80,443' -T4 'scan.example.invalid'"),
            cmds,
        )
    }

    @Test
    fun nmapBuilderOmitsEmptyPortsAndUncheckedFlags() {
        val cmds = CtosBuilders.commands(
            "buildernmap", "host.example.invalid",
            values = mapOf("scan" to "-sn"),
            checked = emptySet(),
        )
        assertEquals(listOf("nmap -sn 'host.example.invalid'"), cmds)
    }

    @Test
    fun gobusterBuilderModesAndWordlistDefault() {
        val dir = CtosBuilders.commands(
            "buildergobuster", "https://x.example.invalid",
            values = mapOf("mode" to "dir", "extensions" to "php,html"),
            checked = emptySet(),
        )
        assertEquals(
            listOf(
                "gobuster dir -u 'https://x.example.invalid' " +
                    "-w '/usr/share/wordlists/dirb/common.txt' -x 'php,html'",
            ),
            dir,
        )
        val dns = CtosBuilders.commands(
            "buildergobuster", "example.invalid",
            values = mapOf("mode" to "dns", "wordlist" to "/tmp/w.txt"),
            checked = emptySet(),
        )
        assertEquals(
            listOf("gobuster dns -d 'example.invalid' -w '/tmp/w.txt'"),
            dns,
        )
        val fuzz = CtosBuilders.commands(
            "buildergobuster", "https://x.example.invalid/FUZZ",
            values = mapOf("mode" to "fuzz"),
            checked = emptySet(),
        )
        assertEquals(
            listOf(
                "gobuster fuzz -u 'https://x.example.invalid/FUZZ' " +
                    "-w '/usr/share/wordlists/dirb/common.txt' --fuzz -t 40",
            ),
            fuzz,
        )
    }

    @Test
    fun sqlmapAndSublist3rBuildersMatchWeb() {
        val sqlmap = CtosBuilders.commands(
            "buildersqlmap", "https://x.example.invalid/i.php?id=1",
            values = mapOf("level" to "3", "risk" to "2"),
            checked = setOf("forms", "batch"),
        )
        assertEquals(
            listOf("sqlmap -u 'https://x.example.invalid/i.php?id=1' --level=3 --risk=2 --forms --batch"),
            sqlmap,
        )
        val sub = CtosBuilders.commands(
            "buildersublist3r", "example.invalid",
            values = mapOf("output" to "subs.txt"),
            checked = setOf("brute", "verbose"),
        )
        assertEquals(
            listOf("sublist3r -d 'example.invalid' -b -o 'subs.txt' -v"),
            sub,
        )
    }

    @Test
    fun dorksBuilderExpandsCategoriesUnquoted() {
        val login = CtosBuilders.commands(
            "builderdorks", "example.invalid",
            values = mapOf("category" to "LOGIN"),
            checked = emptySet(),
        )
        assertEquals(
            listOf(
                "site:example.invalid inurl:login",
                "site:example.invalid inurl:admin",
                "site:example.invalid inurl:signin",
                "site:example.invalid intitle:\"login\"",
            ),
            login,
        )
        val all = CtosBuilders.commands(
            "builderdorks", "example.invalid",
            values = mapOf("category" to "ALL"),
            checked = emptySet(),
        )
        assertEquals(CtosBuilders.DORKS.values.sumOf { it.size }, all.size)
    }

    @Test
    fun builderRegistryCoversTheFiveServerBuilders() {
        assertTrue(CtosBuilders.isBuilder("buildernmap"))
        assertTrue(CtosBuilders.isBuilder("builderdorks"))
        assertFalse(CtosBuilders.isBuilder("dns"))
        assertFalse(CtosBuilders.isBuilder("encsettings"))
    }

    // ---- B-17: gated flag, profile, gate -------------------------------

    @Test
    fun gatedFlagParsesFromToolMeta() {
        val ws = obj(
            """{"tools": [
                {"name": "pentest", "label": "Pentest", "gated": true, "kind": "active"},
                {"name": "dns", "label": "DNS", "kind": "server"}
            ]}""",
        )
        val tools = CtosLogic.parseTools(ws)
        assertTrue(tools.first { it.name == "pentest" }.gated)
        assertFalse(tools.first { it.name == "dns" }.gated)
    }

    @Test
    fun profileAndOptionsParse() {
        val ws = obj(
            """{"profile": {"doh": "cloudflare", "dohCustom": "", "uaRotate": true, "jitterMs": 250},
                "profileOptions": {
                  "wired": [{"key": "doh", "label": "DNS-over-HTTPS provider", "detail": "d"}],
                  "unavailable": [{"label": "Auto proxy chain", "reason": "r"}],
                  "providers": ["google", "cloudflare", "custom"]}}""",
        )
        val profile = CtosLogic.parseProfile(ws)!!
        assertEquals("cloudflare", profile.doh)
        assertTrue(profile.uaRotate)
        assertEquals(250, profile.jitterMs)
        assertEquals("Cloudflare", CtosLogic.profileValueLine(profile, "doh"))
        assertEquals("Up to 250 ms", CtosLogic.profileValueLine(profile, "jitterMs"))
        val options = CtosLogic.parseProfileOptions(ws)!!
        assertEquals(1, options.wired.size)
        assertEquals("Auto proxy chain", options.unavailable[0].label)
        assertEquals(listOf("google", "cloudflare", "custom"), options.providers)
    }

    @Test
    fun gatePanelParses() {
        val ws = obj(
            """{"gate": {"rule": "RULE", "canManage": true,
                "entries": [{"id": 3, "target": "lab.example.invalid",
                             "targetKind": "domain", "basisKind": "owner",
                             "clientLabel": "", "status": "active"}],
                "audit": [{"id": 1}, {"id": 2}], "pool": []}}""",
        )
        val gate = CtosLogic.parseGate(ws)!!
        assertTrue(gate.canManage)
        assertEquals(1, gate.entries.size)
        assertEquals("lab.example.invalid", gate.entries[0].target)
        assertEquals(2, gate.auditCount)
        assertEquals(0, gate.poolCount)
    }

    // ---- B-19: result findings + source url -----------------------------

    @Test
    fun toolResultKeepsTopLevelFindings() {
        val payload = obj(
            """{"ok": true, "tool": "dns", "recorded": 2,
                "result": {"sources": [], "data": {},
                           "findings": [{"type": "ip", "value": "192.0.2.1",
                                         "detail": "A record", "url": ""}]}}""",
        )
        val parsed = CtosLogic.parseToolResult(payload)
        assertEquals(1, parsed.findings.size)
        assertEquals("192.0.2.1", parsed.findings[0].value)
        assertEquals("dns", parsed.findings[0].tool)
    }

    @Test
    fun sourceEntryKeepsItsUrl() {
        val entry = obj(
            """{"source": " crt.sh ", "status": "found", "summary": "s",
                "url": "https://crt.sh/?q=example.invalid", "findings": []}""",
        )
        val parsed = OsintLogic.parseSourceEntry(entry)
        assertEquals("https://crt.sh/?q=example.invalid", parsed.url)
    }

    // ---- M-05: URL allowlist ---------------------------------------------

    @Test
    fun openableUrlAllowsHttpAndHttpsOnly() {
        assertEquals("https://example.invalid/x", OsintLogic.openableUrl(" https://example.invalid/x "))
        assertEquals("http://example.invalid", OsintLogic.openableUrl("http://example.invalid"))
        assertNull(OsintLogic.openableUrl("intent://scan#Intent;scheme=zxing;end"))
        assertNull(OsintLogic.openableUrl("file:///etc/passwd"))
        assertNull(OsintLogic.openableUrl("javascript:alert(1)"))
        assertNull(OsintLogic.openableUrl("example.invalid/no-scheme"))
        assertNull(OsintLogic.openableUrl("https://"))
        assertNull(OsintLogic.openableUrl(""))
        assertNull(OsintLogic.openableUrl(null))
    }
}
