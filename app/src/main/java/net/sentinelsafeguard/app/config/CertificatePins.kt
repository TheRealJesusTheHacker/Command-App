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
 *
 * History: this file once also carried the mail host's anchor pins
 * for the app's former direct mail client. 1.6.3 removed the mail
 * engine from this app entirely (mail lives in the Sentinel Mail
 * App), so the mail pins went with it.
 */
object CertificatePins {

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
