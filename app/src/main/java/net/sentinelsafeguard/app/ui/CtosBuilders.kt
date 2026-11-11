package net.sentinelsafeguard.app.ui

/**
 * CToS command builders (AUDIT-171 B-16) — a faithful port of the
 * web shell's client-side builders (static/index.html, the
 * `ctosBuilderPanel` composition functions). Command NEVER executes
 * these: a builder composes a command string on the device for the
 * operator's own terminal, exactly like the web. Before this port
 * the app sent builder tiles to the server run endpoint, which
 * refuses them ("Unknown CToS tool") — every Run failed.
 *
 * Inputs ride two maps, mirroring the web form: [values] for
 * selects/text fields by key, [checked] for checkbox keys. The
 * keys and defaults are the web's:
 *  - buildernmap:     scan (select, "-sV"), ports (text),
 *                     t4 (checked), scripts, osdetect
 *  - buildergobuster: mode (select, "dir"), wordlist (text,
 *                     default dirb common.txt), extensions (text)
 *  - buildersqlmap:   level ("1"), risk ("1"),
 *                     forms, dbs, batch (checked)
 *  - buildersublist3r: output (text), brute, verbose
 *  - builderdorks:    category (select, "ALL")
 */
object CtosBuilders {

    val BUILDER_TOOLS = setOf(
        "buildernmap", "buildergobuster", "buildersqlmap",
        "buildersublist3r", "builderdorks",
    )

    fun isBuilder(toolName: String): Boolean = toolName in BUILDER_TOOLS

    /** The web's `_ctosShq`: POSIX single-quote a shell argument. */
    fun shq(s: String): String = "'" + s.replace("'", "'\\''") + "'"

    const val DEFAULT_WORDLIST = "/usr/share/wordlists/dirb/common.txt"

    /** The Google-dork catalogue, verbatim from the web builder. */
    val DORKS: Map<String, List<String>> = linkedMapOf(
        "LOGIN" to listOf("inurl:login", "inurl:admin", "inurl:signin", "intitle:\"login\""),
        "FILES" to listOf("filetype:pdf", "filetype:xls", "filetype:doc", "filetype:sql", "filetype:bak"),
        "CONFIG" to listOf("ext:env", "ext:cfg", "ext:conf", "ext:ini", "inurl:config"),
        "EXPOSED" to listOf("inurl:phpinfo", "inurl:server-status", "inurl:.git", "intitle:\"index of\""),
        "SQL" to listOf("inurl:id=", "inurl:product.php?id=", "inurl:news.php?id="),
        "ERRORS" to listOf("\"mysql_fetch\"", "\"ORA-01756\"", "\"Microsoft OLE DB\"", "intitle:\"error occurred\""),
        "CAMERAS" to listOf("inurl:/view/view.shtml", "intitle:\"Live View / - AXIS\"", "inurl:ViewerFrame?Mode="),
    )

    fun commands(
        tool: String,
        target: String,
        values: Map<String, String>,
        checked: Set<String>,
    ): List<String> {
        val t = target.trim()
        return when (tool) {
            "buildernmap" -> {
                val parts = mutableListOf("nmap", values["scan"] ?: "-sV")
                val ports = (values["ports"] ?: "").trim()
                if (ports.isNotEmpty()) parts += "-p ${shq(ports)}"
                if ("t4" in checked) parts += "-T4"
                if ("scripts" in checked) parts += "-sC"
                if ("osdetect" in checked) parts += "-O"
                parts += shq(t)
                listOf(parts.joinToString(" "))
            }
            "buildergobuster" -> {
                val mode = values["mode"] ?: "dir"
                val flag = if (mode == "dir" || mode == "fuzz") "-u" else "-d"
                val parts = mutableListOf(
                    "gobuster", mode, flag, shq(t),
                    "-w", shq((values["wordlist"] ?: "").ifBlank { DEFAULT_WORDLIST }),
                )
                val ext = (values["extensions"] ?: "").trim()
                if (ext.isNotEmpty() && mode == "dir") parts += "-x ${shq(ext)}"
                if (mode == "fuzz") parts += "--fuzz -t 40"
                listOf(parts.joinToString(" "))
            }
            "buildersqlmap" -> {
                val parts = mutableListOf(
                    "sqlmap", "-u", shq(t),
                    "--level=${values["level"] ?: "1"}",
                    "--risk=${values["risk"] ?: "1"}",
                )
                if ("forms" in checked) parts += "--forms"
                if ("dbs" in checked) parts += "--dbs"
                if ("batch" in checked) parts += "--batch"
                listOf(parts.joinToString(" "))
            }
            "buildersublist3r" -> {
                val parts = mutableListOf("sublist3r", "-d", shq(t))
                if ("brute" in checked) parts += "-b"
                val out = (values["output"] ?: "").trim()
                if (out.isNotEmpty()) parts += "-o ${shq(out)}"
                if ("verbose" in checked) parts += "-v"
                listOf(parts.joinToString(" "))
            }
            "builderdorks" -> {
                val cat = values["category"] ?: "ALL"
                val keys = if (cat == "ALL") DORKS.keys.toList() else listOf(cat)
                val out = mutableListOf<String>()
                for (k in keys) {
                    for (d in DORKS[k].orEmpty()) {
                        // Search queries, not shell commands: the
                        // web does not shell-quote these.
                        out += "site:$t $d"
                    }
                }
                out
            }
            else -> emptyList()
        }
    }
}
