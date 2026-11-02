package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.web.WebDestination
import net.sentinelsafeguard.app.web.WebNav
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * hunt4 embedded-browser link policy: Sentinel hosts and the
 * Cloudflare Access sign-in domain stay inside the WebView;
 * everything else goes to the system browser. The destinations
 * themselves are pinned so an entry can never drift to a different
 * door.
 */
class WebNavTest {

    @Test
    fun destinationsPointAtTheRealDoors() {
        assertEquals("https://command.sentinelsafeguard.net", WebDestination.COMMAND.url)
        assertEquals("https://mail.sentinelsafeguard.net", WebDestination.WEBMAIL.url)
        assertEquals("Full Command", WebDestination.COMMAND.title)
        assertEquals("Full Webmail", WebDestination.WEBMAIL.title)
    }

    @Test
    fun sentinelHostsStayInside() {
        assertTrue(WebNav.staysInside("https://command.sentinelsafeguard.net"))
        assertTrue(WebNav.staysInside("https://command.sentinelsafeguard.net/tickets?id=3"))
        assertTrue(WebNav.staysInside("https://mail.sentinelsafeguard.net"))
        assertTrue(WebNav.staysInside("https://sentinelsafeguard.net"))
        assertTrue(WebNav.staysInside("http://command.sentinelsafeguard.net"))
        assertTrue(WebNav.isSentinelHost("command.sentinelsafeguard.net"))
        assertTrue(WebNav.isSentinelHost("SENTINELSafeguard.NET"))
    }

    @Test
    fun cloudflareAccessSignInStaysInside() {
        // The Access login redirect target — if this left the
        // WebView, the embedded sign-in would break.
        assertTrue(
            WebNav.staysInside(
                "https://tight-cherry-e81c.cloudflareaccess.com/cdn-cgi/access/login/example",
            ),
        )
    }

    @Test
    fun everythingElseGoesToTheSystemBrowser() {
        assertFalse(WebNav.staysInside("https://example.com"))
        assertFalse(WebNav.staysInside("https://sentinelsafeguard.net.evil.com"))
        assertFalse(WebNav.staysInside("https://evil-sentinelsafeguard.net"))
        assertFalse(WebNav.staysInside("https://cloudflareaccess.com.evil.com"))
        assertFalse(WebNav.staysInside("mailto:someone@example.com"))
        assertFalse(WebNav.staysInside("tel:+18165550100"))
        assertFalse(WebNav.staysInside("ftp://command.sentinelsafeguard.net/file"))
        assertFalse(WebNav.staysInside(""))
        assertFalse(WebNav.staysInside("   "))
        assertFalse(WebNav.staysInside(null))
    }

    @Test
    fun webViewInternalUrlsStayInside() {
        assertTrue(WebNav.staysInside("about:blank"))
        assertTrue(WebNav.staysInside("data:text/html,<p>hi</p>"))
        assertTrue(WebNav.staysInside("blob:https://command.sentinelsafeguard.net/1234"))
    }
}
