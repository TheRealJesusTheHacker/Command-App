package net.sentinelsafeguard.app.config

/**
 * Desktop counterpart of the Android build's generated gate config.
 * The Cloudflare Access service-auth values are NOT configured for
 * the desktop build (they were never issued for it), so both blobs
 * are empty: GateCrypto.decrypt("") yields null and GateConfig
 * degrades to "not configured" — the API client omits the Access
 * headers entirely, exactly the phone's pre-Access behavior. The
 * device token remains the identity. No gate key is fabricated.
 */
internal object GeneratedGateConfig {
    const val CF_ACCESS_CLIENT_ID_BLOB: String = ""
    const val CF_ACCESS_CLIENT_SECRET_BLOB: String = ""
}
