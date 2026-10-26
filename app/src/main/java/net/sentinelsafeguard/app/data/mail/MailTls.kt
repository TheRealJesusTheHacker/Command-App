package net.sentinelsafeguard.app.data.mail

import net.sentinelsafeguard.app.config.CertificatePins
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory

/**
 * The mail stack's TLS entry point: an [SSLSocketFactory] whose
 * trust manager is the platform default PLUS the SPKI anchor pins
 * for the mail host ([SpkiPinningTrustManager]).
 *
 * MailService hands this factory to Angus through the
 * `mail.<protocol>.ssl.socketFactory` session property as an object.
 * That property is the one Angus consults first for both kinds of
 * connection this app makes — implicit TLS (IMAP 993) and the
 * STARTTLS upgrade (SMTP 587) — verified against the Angus 2.0.3
 * SocketFetcher source when this was written (see HARDENING.md).
 */
object MailTls {

    private val factory: SSLSocketFactory by lazy {
        val context = SSLContext.getInstance("TLS")
        context.init(
            null,
            arrayOf(SpkiPinningTrustManager(CertificatePins.MAIL_PINS)),
            null,
        )
        context.socketFactory
    }

    fun socketFactory(): SSLSocketFactory = factory
}
