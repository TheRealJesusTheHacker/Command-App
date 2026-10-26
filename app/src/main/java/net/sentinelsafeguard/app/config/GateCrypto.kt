package net.sentinelsafeguard.app.config

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Decode layer for the Cloudflare Access gate values (spec §3.1).
 *
 * The values travel: GitHub Secrets → build environment → the Gradle
 * generator (see app/build.gradle.kts), which seals them and writes
 * them into the generated [GeneratedGateConfig] as opaque blobs →
 * this object unseals them at runtime. Neither the repo nor a plain
 * `strings` dump of the APK shows them in the clear, because the
 * plaintext never exists in any file: only AES-GCM ciphertext does.
 *
 * DOCTRINE — read before trusting this: client-side obfuscation is a
 * SPEED BUMP, not a vault. The key fragments below are public by
 * design (they are in the source, on purpose, in the open); anyone
 * with the APK and an afternoon can unseal the blobs. That is
 * accepted because the values are a GATE KEY, not an identity: the
 * Cloudflare Access service-auth pair only proves "an enrolled copy
 * of the app is calling". The backend device token is the identity —
 * it is issued per device, hashed server-side, and revocable. A
 * leaked gate key alone authenticates no one. This layer exists to
 * keep the values out of casual view (repo browsers, strings dumps,
 * log scrapers), nothing more. See HARDENING.md.
 *
 * Format ("gate-config v1"):
 *   key  = SHA-256( UTF-8( FRAGMENT_1 + ":" + FRAGMENT_2 + ":" +
 *                          FRAGMENT_3 ) ), used as an AES-256 key.
 *   blob = Base64( IV(12 bytes) || AES-GCM ciphertext+tag(128 bit) )
 *
 * The fragments are parsed out of THIS file by the Gradle generator
 * (exact `FRAGMENT_n = "..."` lines), so the sealer and this unsealer
 * can never drift apart on fragment material. The generator also
 * round-trip-checks every blob it seals. GateCryptoTest pins this
 * side of the contract with a fixed vector sealed by the generator's
 * algorithm, so an algorithm change on either side fails CI.
 *
 * JDK crypto only — no third-party dependency is involved.
 */
object GateCrypto {

    // Key fragments. PUBLIC BY DESIGN (see header). Keep them on
    // their own lines in exactly this shape: the build reads them.
    private const val FRAGMENT_1 = "U2VudGluZWwtQXBw"
    private const val FRAGMENT_2 = "Z2F0ZS1rZXktdjE="
    private const val FRAGMENT_3 = "c2hhLTI1Ni1rZGY="

    private const val IV_BYTES = 12
    private const val GCM_TAG_BITS = 128

    internal fun deriveKey(): SecretKeySpec {
        val material = "$FRAGMENT_1:$FRAGMENT_2:$FRAGMENT_3".toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(material)
        return SecretKeySpec(digest, "AES")
    }

    /**
     * Unseal one generated blob. Returns the plaintext, or null when
     * the blob is null/blank, malformed, or fails the GCM integrity
     * check — every failure lands in the same place, and callers
     * treat null as "not configured" (the safe direction: the Access
     * headers are simply omitted).
     */
    fun decrypt(blob: String?): String? {
        if (blob.isNullOrBlank()) return null
        return try {
            val raw = Base64.getDecoder().decode(blob.trim())
            if (raw.size <= IV_BYTES) return null
            val iv = raw.copyOfRange(0, IV_BYTES)
            val ciphertext = raw.copyOfRange(IV_BYTES, raw.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
