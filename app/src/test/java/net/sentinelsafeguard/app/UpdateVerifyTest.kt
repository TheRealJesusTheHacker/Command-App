package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.update.UpdateVerify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVerifyTest {

    // sha256("abc"), the well-known NIST test vector.
    private val abcSha256 =
        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"

    @Test
    fun sha256HexMatchesKnownVector() {
        assertEquals(abcSha256, UpdateVerify.sha256Hex("abc".toByteArray()))
    }

    @Test
    fun higherVersionCodeIsAnUpdate() {
        assertTrue(UpdateVerify.isUpdateAvailable(104, "1.4.0", 103, "1.3.0"))
    }

    @Test
    fun equalOrLowerVersionCodeIsNotAnUpdate() {
        assertFalse(UpdateVerify.isUpdateAvailable(103, "1.3.0", 103, "1.3.0"))
        assertFalse(UpdateVerify.isUpdateAvailable(102, "1.2.0", 103, "1.3.0"))
    }

    @Test
    fun higherCodeWithOlderNameIsNotAnUpdate() {
        // A staged rollback must never be offered as an update.
        assertFalse(UpdateVerify.isUpdateAvailable(104, "1.2.0", 103, "1.3.0"))
    }

    @Test
    fun higherCodeWithSameNameIsAnUpdate() {
        // A rebuilt hotfix can bump the code without a name change.
        assertTrue(UpdateVerify.isUpdateAvailable(104, "1.3.0", 103, "1.3.0"))
    }

    @Test
    fun missingCodeFallsBackToNameComparison() {
        assertTrue(UpdateVerify.isUpdateAvailable(null, "1.4.0", 103, "1.3.0"))
        assertFalse(UpdateVerify.isUpdateAvailable(null, "1.3.0", 103, "1.3.0"))
        assertFalse(UpdateVerify.isUpdateAvailable(null, null, 103, "1.3.0"))
    }

    @Test
    fun matchingSizeAndHashPass() {
        assertNull(
            UpdateVerify.mismatchReason(
                actualSizeBytes = 3L,
                expectedSizeBytes = 3L,
                actualSha256 = abcSha256,
                expectedSha256 = abcSha256,
            )
        )
    }

    @Test
    fun sizeMismatchIsRejected() {
        val reason = UpdateVerify.mismatchReason(
            actualSizeBytes = 2L,
            expectedSizeBytes = 3L,
            actualSha256 = abcSha256,
            expectedSha256 = abcSha256,
        )
        assertTrue(reason!!.contains("did not finish"))
    }

    @Test
    fun hashMismatchIsRejected() {
        val reason = UpdateVerify.mismatchReason(
            actualSizeBytes = 3L,
            expectedSizeBytes = 3L,
            actualSha256 = abcSha256,
            expectedSha256 = "0".repeat(64),
        )
        assertTrue(reason!!.contains("did not match"))
    }

    @Test
    fun hashComparisonIgnoresCaseAndWhitespace() {
        assertNull(
            UpdateVerify.mismatchReason(
                actualSizeBytes = 3L,
                expectedSizeBytes = null,
                actualSha256 = abcSha256,
                expectedSha256 = "  " + abcSha256.uppercase() + " ",
            )
        )
    }

    @Test
    fun unstatedFactsAreNotChecked() {
        assertNull(
            UpdateVerify.mismatchReason(
                actualSizeBytes = 999L,
                expectedSizeBytes = null,
                actualSha256 = abcSha256,
                expectedSha256 = null,
            )
        )
    }
}
