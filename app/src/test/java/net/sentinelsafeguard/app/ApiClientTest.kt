package net.sentinelsafeguard.app

import kotlinx.coroutines.test.runTest
import net.sentinelsafeguard.app.config.ApiConfig
import net.sentinelsafeguard.app.data.api.HttpResponse
import net.sentinelsafeguard.app.data.api.LoginResult
import net.sentinelsafeguard.app.data.api.SentinelApi
import net.sentinelsafeguard.app.data.api.SignedOutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ApiClientTest {

    private val config = ApiConfig(
        baseUrl = "https://command.example.test",
        cfAccessClientId = "test-client-id",
        cfAccessClientSecret = "test-client-secret",
    )

    @Test
    fun loginSuccessReturnsTokenAndStoresIt() = runTest {
        // The REAL backend success body (rehearsal capture): the
        // account rides nested under "user" — the flat
        // {deviceToken, displayName, role} shape never existed
        // server-side, which is why hunt2's Success carried nulls.
        val transport = FakeTransport {
            HttpResponse(200, RealBodies.LOGIN_SUCCESS)
        }
        val api = SentinelApi(config, transport)
        val result = api.login("Reh Owner", "passphrase", "Rehearsal phone")
        assertTrue(result is LoginResult.Success)
        assertEquals("tok-rehearsal-a", (result as LoginResult.Success).token)
        assertEquals("Reh Owner", (result as LoginResult.Success).displayName)
        assertEquals("OWNER", (result as LoginResult.Success).role)
        assertEquals("tok-rehearsal-a", api.deviceToken)
        // Enrollment itself is unauthenticated, but the Access gate headers ride along.
        val sent = transport.requests.single()
        assertEquals("/api/app/auth/login", sent.path)
        assertEquals(null, sent.headers["Authorization"])
        assertEquals("test-client-id", sent.headers["CF-Access-Client-Id"])
    }

    @Test
    fun loginTotpBranchAsksForCode() = runTest {
        // The backend's real shape (rehearsal capture): HTTP 200
        // with totpRequired + the single-use challenge, NOT a
        // status flag.
        val transport = FakeTransport {
            HttpResponse(200, RealBodies.LOGIN_TOTP_REQUIRED)
        }
        val api = SentinelApi(config, transport)
        val result = api.login("Reh Totp", "passphrase", "Second phone")
        assertTrue(result is LoginResult.TotpRequired)
        assertEquals("ch-rehearsal", (result as LoginResult.TotpRequired).challenge)
        assertEquals(null, api.deviceToken)
    }

    @Test
    fun completeTotpSendsChallengeShapeAndSucceeds() = runTest {
        val transport = FakeTransport { request ->
            if (request.body?.contains(""""challenge"""") == true) {
                HttpResponse(200, RealBodies.LOGIN_TOTP_SUCCESS)
            } else {
                HttpResponse(200, RealBodies.LOGIN_TOTP_REQUIRED)
            }
        }
        val api = SentinelApi(config, transport)
        val first = api.login("Reh Totp", "pw", "Second phone")
        assertTrue(first is LoginResult.TotpRequired)
        val second = api.completeTotp(
            (first as LoginResult.TotpRequired).challenge, "123456", "Second phone")
        assertTrue(second is LoginResult.Success)
        assertEquals("Reh Totp", (second as LoginResult.Success).displayName)
        assertEquals("tok-rehearsal-b", api.deviceToken)
        // The completion call answers {challenge, code, deviceName} —
        // and must NOT resend the passphrase.
        val completion = transport.requests.last()
        assertEquals("/api/app/auth/login", completion.path)
        assertTrue(completion.body!!.contains(""""challenge":"ch-rehearsal""""))
        assertTrue(completion.body!!.contains(""""code":"123456""""))
        assertTrue(!completion.body!!.contains("passphrase"))
    }

    @Test
    fun completeTotpWrongCodeFailsWithServerMessage() = runTest {
        // Verbatim backend refusal (server.py Denied, HTTP 403).
        val transport = FakeTransport {
            HttpResponse(403, """{"ok":false,"message":"That code doesn't match — check the time-based code and try again."}""")
        }
        val api = SentinelApi(config, transport)
        val result = api.completeTotp("ch-rehearsal", "000000", "phone")
        assertTrue(result is LoginResult.Failed)
        assertTrue((result as LoginResult.Failed).message.contains("That code doesn't match"))
        assertEquals(null, api.deviceToken)
    }

    @Test
    fun loginBadPassphraseFailsWithMessage() = runTest {
        // Real refusal (rehearsal capture): HTTP 403, the
        // backend's own "Invalid credentials." body.
        val transport = FakeTransport {
            HttpResponse(403, RealBodies.LOGIN_DENIED)
        }
        val api = SentinelApi(config, transport)
        val result = api.login("Reh Owner", "wrong", "phone")
        assertTrue(result is LoginResult.Failed)
        assertEquals("Invalid credentials.", (result as LoginResult.Failed).message)
    }

    @Test
    fun tokenIsAttachedToAuthenticatedCalls() = runTest {
        val transport = FakeTransport { HttpResponse(200, RealBodies.TODAY) }
        val api = SentinelApi(config, transport)
        api.deviceToken = "tok-attach"
        api.today()
        val sent = transport.requests.single()
        assertEquals("Bearer tok-attach", sent.headers["Authorization"])
        assertEquals("test-client-secret", sent.headers["CF-Access-Client-Secret"])
    }

    @Test
    fun unauthorizedResponseSignsOut() = runTest {
        val transport = FakeTransport { HttpResponse(401, """{"error":"revoked"}""") }
        val api = SentinelApi(config, transport)
        api.deviceToken = "tok-dead"
        var signedOutFired = false
        api.onSignedOut = { signedOutFired = true }
        try {
            api.today()
            fail("A 401 must surface as SignedOutException")
        } catch (expected: SignedOutException) {
            // expected
        }
        assertTrue(signedOutFired)
    }

    @Test
    fun accessHeadersOmittedWhenNotConfigured() = runTest {
        val transport = FakeTransport { HttpResponse(200, RealBodies.ME) }
        val api = SentinelApi(ApiConfig("https://x.test", "", ""), transport)
        api.deviceToken = "t"
        api.me()
        val sent = transport.requests.single()
        assertEquals(null, sent.headers["CF-Access-Client-Id"])
        assertEquals(null, sent.headers["CF-Access-Client-Secret"])
    }
}
