package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.config.GateCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

/**
 * The gate-config decode layer (config/GateCrypto.kt).
 *
 * The fixed vectors below were sealed by an INDEPENDENT
 * implementation of the generator's algorithm (a scratch JVM sealer
 * written for the hardening pass, implementing the "gate-config v1"
 * format from GateCrypto's own documentation and parsing the key
 * fragments out of GateCrypto.kt exactly as the Gradle generator
 * does). If either side's algorithm or fragment material changes,
 * these tests fail — that is what they are for.
 */
class GateCryptoTest {

    @Test
    fun unsealsTheGeneratorsVector() {
        assertEquals(
            "gate-config-test-vector-0123456789",
            GateCrypto.decrypt(VECTOR_BLOB),
        )
    }

    @Test
    fun unsealsAnEmptyValueToEmpty() {
        // The pre-Access-layer state: both blobs seal "".
        assertEquals("", GateCrypto.decrypt(EMPTY_BLOB))
    }

    @Test
    fun roundTripsThroughTheExposedKey() {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12) { (it * 7).toByte() }
        cipher.init(Cipher.ENCRYPT_MODE, GateCrypto.deriveKey(), GCMParameterSpec(128, iv))
        val blob = Base64.getEncoder()
            .encodeToString(iv + cipher.doFinal("round-trip-value".toByteArray(Charsets.UTF_8)))
        assertEquals("round-trip-value", GateCrypto.decrypt(blob))
    }

    @Test
    fun failsSafeOnMissingAndMalformedBlobs() {
        assertNull(GateCrypto.decrypt(null))
        assertNull(GateCrypto.decrypt(""))
        assertNull(GateCrypto.decrypt("   "))
        assertNull(GateCrypto.decrypt("not-base64!!!"))
        assertNull(GateCrypto.decrypt("AAAA")) // valid base64, far too short
    }

    @Test
    fun failsSafeOnATamperedBlob() {
        // Flip the tail of the vector: GCM integrity must reject it,
        // and the failure must surface as null ("not configured"),
        // never as an exception or garbled plaintext.
        val flipped = VECTOR_BLOB.dropLast(4) +
            if (VECTOR_BLOB.dropLast(4).endsWith("A")) "BBBB" else "AAAA"
        assertNull(GateCrypto.decrypt(flipped))
    }

    private companion object {
        const val EMPTY_BLOB = "bt28ZcTia0fRJYlU+MHNXQA/VaGoQPbh08lVlQ=="
        const val VECTOR_BLOB =
            "W97VQs6qt+Wk8C+hB45d9BDWYYM0aFpe1E2ZQ5MAdnX0HVJj1eJl/UzE8wPY6Zff7wNZz7qPsH2GOcWTlrM="
    }
}
