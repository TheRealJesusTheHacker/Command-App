package net.sentinelsafeguard.app.config

import net.sentinelsafeguard.app.BuildConfig

/**
 * The one place public endpoints and the Cloudflare Access gate values live.
 *
 * The Access service-auth values (spec §3.1) are a gate key proving "an
 * enrolled copy of the app is calling", never an identity — the backend
 * device token is the identity. They no longer travel as plaintext build
 * config: the build seals them into generated blobs and [GateConfig]
 * unseals them at runtime (see GateCrypto for the mechanism and its
 * honest limits). Until the Access layer lands, both values are "" and
 * the app behaves exactly as it did with empty placeholders.
 *
 * The base URL stays plain build config on purpose: it is a public DNS
 * name, not a secret, and hiding it would be theatre.
 */
data class ApiConfig(
    val baseUrl: String,
    val cfAccessClientId: String,
    val cfAccessClientSecret: String,
)

object AppConfig {
    val api = ApiConfig(
        baseUrl = BuildConfig.API_BASE_URL,
        cfAccessClientId = GateConfig.cfAccessClientId,
        cfAccessClientSecret = GateConfig.cfAccessClientSecret,
    )

    const val MAIL_HOST: String = BuildConfig.MAIL_HOST
    const val MAIL_IMAP_PORT: Int = 993
    const val MAIL_SMTP_PORT: Int = 587

    /** Header names Cloudflare Access expects for service authentication. */
    const val CF_ACCESS_CLIENT_ID_HEADER = "CF-Access-Client-Id"
    const val CF_ACCESS_CLIENT_SECRET_HEADER = "CF-Access-Client-Secret"
}
