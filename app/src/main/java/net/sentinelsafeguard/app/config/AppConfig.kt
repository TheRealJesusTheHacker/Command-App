package net.sentinelsafeguard.app.config

import net.sentinelsafeguard.app.BuildConfig

/**
 * The one place public endpoints and the Cloudflare Access gate values live.
 *
 * The Access service-auth values are build config (spec §3.1): the Client ID
 * and Secret are a gate key proving "an enrolled copy of the app is calling",
 * never an identity — the backend device token is the identity. The values
 * arrive with the Access layer; until then they are empty placeholders and
 * the app still compiles and its non-network tests still pass.
 */
data class ApiConfig(
    val baseUrl: String,
    val cfAccessClientId: String,
    val cfAccessClientSecret: String,
)

object AppConfig {
    val api = ApiConfig(
        baseUrl = BuildConfig.API_BASE_URL,
        cfAccessClientId = BuildConfig.CF_ACCESS_CLIENT_ID,
        cfAccessClientSecret = BuildConfig.CF_ACCESS_CLIENT_SECRET,
    )

    const val MAIL_HOST: String = BuildConfig.MAIL_HOST
    const val MAIL_IMAP_PORT: Int = 993
    const val MAIL_SMTP_PORT: Int = 587

    /** Header names Cloudflare Access expects for service authentication. */
    const val CF_ACCESS_CLIENT_ID_HEADER = "CF-Access-Client-Id"
    const val CF_ACCESS_CLIENT_SECRET_HEADER = "CF-Access-Client-Secret"
}
