package net.sentinelsafeguard.app.config

import java.security.MessageDigest
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.Base64

/**
 * SPKI certificate pins (SHA-256 of the SubjectPublicKeyInfo, the
 * standard "sha256/…" pin format), and the code that checks a
 * presented chain against them.
 *
 * MAIL host — pinned, anchor-style. The pins are the two CA anchors
 * in the chain the live mail server actually presents, computed from
 * certificates fetched from mail.sentinelsafeguard.net:993 on
 * 2026-10-25 (Let's Encrypt "Generation Y" hierarchy):
 *
 *   leaf  CN=mail.sentinelsafeguard.net   (renews ~90 days — NOT pinned)
 *     ← YE2 intermediate                  (rotates by design — NOT pinned)
 *     ← ISRG Root YE                      PINNED
 *     ← ISRG Root X2 (cross-signed by X1) PINNED
 *
 * Anchor pinning means leaf renewals and intermediate rotations
 * change nothing: any future chain that still runs through either
 * root key matches. Both roots are in the presented chain today
 * (verified byte-for-byte pin agreement across two independent
 * fetches), so either alone is sufficient and the pair is the
 * backup for each other. Pinning ISRG Root X1 was considered and
 * rejected on evidence: the X1 certificate itself is never presented
 * in the chain, so its pin could never match a presented-chain
 * check. See HARDENING.md for the full provenance and the rotation
 * procedure.
 *
 * COMMAND host — deliberately NOT pinned (empty set). The
 * certificate on command.sentinelsafeguard.net is presented through
 * Cloudflare's layer, which the owner does not control: Cloudflare
 * can re-present that hostname under a different CA without any
 * change on the owner's server, and a stale pin there would brick
 * the app AND its token-gated update channel at the same moment.
 * The command channel's protections are the Access service-auth
 * gate, the revocable device token, and platform TLS — not a pin.
 * That exception is a decision, recorded in HARDENING.md; the empty
 * set here is pinned by a unit test so it cannot silently change.
 */
object CertificatePins {

    /** Pins for mail.sentinelsafeguard.net (SPKI SHA-256, base64). */
    val MAIL_PINS: Set<String> = setOf(
        // ISRG Root YE
        "sCkq5UWXjg+7mKu9lMhhYF5bGLsy7VI/UNW3tccdR7w=",
        // ISRG Root X2
        "diGVwiVYbubAI3RW4hB9xU8e/CH2GnkuvVFZE8zmgzI=",
    )

    /** Pins for command.sentinelsafeguard.net — none, BY DECISION. */
    val COMMAND_PINS: Set<String> = emptySet()

    /** The pin of one certificate: Base64(SHA-256(SPKI DER)). */
    fun spkiPin(certificate: Certificate): String {
        val spki = (certificate as X509Certificate).publicKey.encoded
        val digest = MessageDigest.getInstance("SHA-256").digest(spki)
        return Base64.getEncoder().encodeToString(digest)
    }

    /** True when any certificate in [chain] carries a pinned SPKI. */
    fun chainMatches(chain: Array<out Certificate>, pins: Set<String>): Boolean {
        if (pins.isEmpty()) return false
        return chain.any { spkiPin(it) in pins }
    }
}
