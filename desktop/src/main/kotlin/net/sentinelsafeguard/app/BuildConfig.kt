package net.sentinelsafeguard.app

/**
 * Desktop shim for the Android-generated BuildConfig, carrying only
 * what the shared sources read: the production Command host (the
 * same value the Android build.gradle.kts bakes in) — read from the
 * tree, never invented. Desktop versioning is its own line (1.0.0);
 * these fields describe the API contract, not the Android app.
 */
object BuildConfig {
    const val API_BASE_URL: String = "https://command.sentinelsafeguard.net"
    const val VERSION_NAME: String = "1.0.0"
    const val VERSION_CODE: Int = 1
}
