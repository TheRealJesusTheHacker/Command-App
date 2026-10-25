package net.sentinelsafeguard.app.util

/**
 * Version comparison for the update check (spec §7). Semantic-ish:
 * numeric dot parts compared in order; a suffix after '-' (for example
 * the hunt line's "-hunt1") is ignored for ordering, so 0.2.0-hunt1 is
 * newer than 0.1.0. Pure logic, unit-tested.
 */
object VersionCompare {

    fun parts(version: String): List<Int> =
        version.trim()
            .removePrefix("v")
            .substringBefore('-')
            .split('.')
            .map { segment -> segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }

    /** Returns > 0 when [a] is newer than [b], 0 when equal, < 0 when older. */
    fun compare(a: String, b: String): Int {
        val pa = parts(a)
        val pb = parts(b)
        val length = maxOf(pa.size, pb.size)
        for (i in 0 until length) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }

    fun isNewer(candidate: String, current: String): Boolean = compare(candidate, current) > 0
}
