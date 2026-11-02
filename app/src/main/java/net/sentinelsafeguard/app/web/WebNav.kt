package net.sentinelsafeguard.app.web

import java.net.URI

/**
 * The two embedded-browser destinations (hunt4): the complete
 * Sentinel Command web app and the full Roundcube webmail, hosted
 * inside the app so the owner gets the whole browser experience
 * without leaving it.
 *
 * The WebView deliberately carries NO Command API credentials: no
 * device token and none of the sealed Cloudflare Access service
 * headers (those stay on the API client only). Inside the browser
 * the owner signs in the normal browser way — Cloudflare Access
 * asks for an email code, then Command's own sign-in — and the
 * WebView's persisted cookies keep that session alive across app
 * restarts for as long as Access allows.
 */
enum class WebDestination(val title: String, val startUrl: String) {
    COMMAND("Full Command", "https://command.sentinelsafeguard.net"),
    WEBMAIL("Full Webmail", "https://mail.sentinelsafeguard.net"),
}

/**
 * Navigation policy for the embedded browser, kept free of Android
 * types so it is unit-testable: Sentinel hosts and the Cloudflare
 * Access sign-in domain stay inside the WebView; everything else
 * opens in the system browser.
 */
object WebNav {

    fun isSentinelHost(host: String?): Boolean {
        val h = host?.lowercase() ?: return false
        return h == "sentinelsafeguard.net" || h.endsWith(".sentinelsafeguard.net")
    }

    /** True when [url] should load inside the WebView. */
    fun staysInside(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lowered = url.lowercase()
        // Pages the WebView generates itself (error pages, blanks).
        if (lowered.startsWith("about:") || lowered.startsWith("data:") ||
            lowered.startsWith("blob:")
        ) {
            return true
        }
        val uri = try {
            URI(url)
        } catch (e: Exception) {
            return false
        }
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        val host = uri.host?.lowercase() ?: return false
        // The Access sign-in flow for command.sentinelsafeguard.net
        // runs on the account's *.cloudflareaccess.com domain; it is
        // part of the sign-in, so it stays inside too.
        return isSentinelHost(host) || host.endsWith(".cloudflareaccess.com")
    }
}
