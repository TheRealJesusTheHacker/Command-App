package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.util.VersionCompare
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCompareTest {

    @Test
    fun newerPatchIsNewer() {
        assertTrue(VersionCompare.isNewer("0.1.1", "0.1.0"))
        assertTrue(VersionCompare.compare("0.1.1", "0.1.0") > 0)
    }

    @Test
    fun olderIsNotNewer() {
        assertFalse(VersionCompare.isNewer("0.1.0", "0.2.0"))
        assertTrue(VersionCompare.compare("0.9.9", "1.0.0") < 0)
    }

    @Test
    fun equalIsNotNewer() {
        assertEquals(0, VersionCompare.compare("1.0.0", "1.0.0"))
        assertFalse(VersionCompare.isNewer("1.0.0", "1.0.0"))
    }

    @Test
    fun huntSuffixDoesNotBreakOrdering() {
        // The hunt line ships names like 0.1.0-hunt1; the suffix is a
        // label, not an ordering input.
        assertTrue(VersionCompare.isNewer("0.2.0-hunt1", "0.1.0"))
        assertEquals(0, VersionCompare.compare("0.1.0-hunt2", "0.1.0-hunt1"))
    }

    @Test
    fun missingPartsCountAsZero() {
        assertEquals(0, VersionCompare.compare("1.0", "1.0.0"))
        assertTrue(VersionCompare.isNewer("1.0.1", "1.0"))
    }
}
