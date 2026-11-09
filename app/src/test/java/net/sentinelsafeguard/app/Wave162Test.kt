package net.sentinelsafeguard.app

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import net.sentinelsafeguard.app.config.ApiConfig
import net.sentinelsafeguard.app.data.api.ApiException
import net.sentinelsafeguard.app.data.api.HttpResponse
import net.sentinelsafeguard.app.data.api.SentinelApi
import net.sentinelsafeguard.app.data.api.SignedOutException
import net.sentinelsafeguard.app.data.api.wysAudio
import net.sentinelsafeguard.app.ui.Wave161Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * 1.6.2 pins for the briefing audio's CANONICAL app route
 * (GET /api/app/wysaudio).
 *
 * The fixtures below are BYTE-VERBATIM captures from the
 * 2026-11-09 wave162 rehearsal against the staging Command
 * server (wave162-rehearsal/goldens/ — a throwaway DB; the
 * staging tree is the same build Live shipped to production,
 * server.py sha256 3f9493e1…; the route exercised exactly
 * as the client calls it; production never contacted;
 * 13/13 rehearsal checks PASS).
 *
 * What is pinned, and why:
 *  - the call rides GET on the canonical route with the
 *    device token — the 1.6.1 POST /actions arm call is
 *    gone from the client (the arm itself stays live
 *    server-side, rehearsal-proven, and is not ours to
 *    remove);
 *  - the available body parses through the unchanged
 *    Wave161Logic.parseWysAudio to Ready — playback path
 *    untouched since its 1.6.1 unit proof;
 *  - the gate's 403 surfaces as an ApiException carrying
 *    the server's own message (the row degrades it to the
 *    quiet line; it is never rendered as an error);
 *  - a dead token's 401 stays the standard session-expiry
 *    path: onSignedOut fires and the call fails with
 *    SignedOutException.
 */
class Wave162Test {

    private fun obj(text: String) = Json.parseToJsonElement(text).jsonObject

    private fun apiWith(status: Int, responseBody: String): SentinelApi {
        val transport = FakeTransport { HttpResponse(status, responseBody) }
        return SentinelApi(ApiConfig("https://command.example.test", "", ""), transport)
            .also { it.deviceToken = "device-token" }
    }

    @Test
    fun wysAudioRidesTheCanonicalRouteWithTheDeviceToken() = runTest {
        val transport = FakeTransport { HttpResponse(200, AVAILABLE) }
        val api = SentinelApi(ApiConfig("https://command.example.test", "", ""), transport)
        api.deviceToken = "device-token"
        val body = api.wysAudio()
        // The full loop: wire body -> the 1.6.1-proven parser
        // -> playback-ready bytes.
        val parsed = Wave161Logic.parseWysAudio(body)
        assertTrue(parsed is Wave161Logic.WysAudioResult.Ready)
        parsed as Wave161Logic.WysAudioResult.Ready
        assertEquals("wav", parsed.extension)
        assertEquals(1644, parsed.bytes.size)
        val sent = transport.requests.single()
        assertEquals("GET", sent.method)
        assertEquals("/api/app/wysaudio", sent.path)
        assertTrue(sent.headers["Authorization"]?.startsWith("Bearer ") == true)
        assertEquals(null, sent.body)
    }

    @Test
    fun wysAudioUnavailableParsesNotGenerated() = runTest {
        val parsed = Wave161Logic.parseWysAudio(apiWith(200, UNAVAILABLE).wysAudio())
        assertTrue(parsed is Wave161Logic.WysAudioResult.NotGenerated)
    }

    @Test
    fun wysAudioGate403SurfacesTheServersMessage() = runTest {
        var caught: ApiException? = null
        try {
            apiWith(403, FORBIDDEN).wysAudio()
        } catch (e: ApiException) {
            caught = e
        }
        assertTrue("expected ApiException", caught != null)
        val error = caught!!
        assertEquals(403, error.status)
        assertEquals("Not authorized.", error.message)
        // And the body itself, parsed, is the quiet Denied —
        // the row's degradation and the parser agree.
        assertTrue(
            Wave161Logic.parseWysAudio(obj(FORBIDDEN)) is Wave161Logic.WysAudioResult.Denied,
        )
    }

    @Test
    fun wysAudioDeadToken401StaysTheSessionExpiryPath() = runTest {
        val api = apiWith(401, UNAUTHENTICATED)
        var signOutFired = false
        api.onSignedOut = { signOutFired = true }
        try {
            api.wysAudio()
            fail("expected SignedOutException")
        } catch (e: SignedOutException) {
            // The standard path: SentinelApi fired onSignedOut
            // and the shell returns to sign-in.
        }
        assertTrue(signOutFired)
    }

    private companion object {
        // Byte-verbatim from wave162-rehearsal/goldens/
        // (2026-11-09, staging server, canonical route).
        const val AVAILABLE =
            """{"authorized": true, "available": true, "hash": "eb3395b953a76afe", "contentType": "audio/wav", "audioB64": "UklGRmQGAABXQVZFZm10IBAAAAABAAEAQB8AAIA+AAACABAAZGF0YUAGAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"}"""
        const val UNAVAILABLE =
            """{"authorized": true, "available": false, "hash": "eb3395b953a76afe"}"""
        const val FORBIDDEN =
            """{"ok": false, "authorized": false, "message": "Not authorized."}"""
        const val UNAUTHENTICATED =
            """{"ok": false, "authorized": false, "message": "Sign-in required."}"""
    }
}
