package net.sentinelsafeguard.app.config

/**
 * Runtime access to the Cloudflare Access gate values.
 *
 * The values exist in the app only as sealed blobs in the generated
 * [GeneratedGateConfig] (written by the build, never committed) and
 * are unsealed here, lazily, through [GateCrypto]. Until the Access
 * layer lands (owner-side Cloudflare step), the sealed values are
 * encryptions of the empty string and both properties below are "" —
 * the API client then omits the Access headers entirely, exactly as
 * it did before this plumbing existed.
 *
 * An unseal failure also yields "": a damaged blob degrades to
 * "not configured", never to a crash and never to a garbage header.
 */
object GateConfig {

    val cfAccessClientId: String by lazy {
        GateCrypto.decrypt(GeneratedGateConfig.CF_ACCESS_CLIENT_ID_BLOB) ?: ""
    }

    val cfAccessClientSecret: String by lazy {
        GateCrypto.decrypt(GeneratedGateConfig.CF_ACCESS_CLIENT_SECRET_BLOB) ?: ""
    }
}
