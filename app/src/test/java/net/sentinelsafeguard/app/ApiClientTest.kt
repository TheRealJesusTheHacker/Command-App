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
        val transport = FakeTransport {
            HttpResponse(200, """{"deviceToken":"tok-123","displayName":"Skyler","role":"OWNER"}""")
        }
        val api = SentinelApi(config, transport)
        val result = api.login("Skyler", "passphrase", "Test phone")
        assertTrue(result is LoginResult.Success)
        assertEquals("tok-123", (result as LoginResult.Success).token)
        assertEquals("tok-123", api.deviceToken)
        // Enrollment itself is unauthenticated, but the Access gate headers ride along.
        val sent = transport.requests.single()
        assertEquals("/api/app/auth/login", sent.path)
        assertEquals(null, sent.headers["Authorization"])
        assertEquals("test-client-id", sent.headers["CF-Access-Client-Id"])
    }

    @Test
    fun loginTotpBranchAsksForCode() = runTest {
        val transport = FakeTransport {
            HttpResponse(200, """{"requiresTotp":true}""")
        }
        val api = SentinelApi(config, transport)
        val result = api.login("Skyler", "passphrase", "Test phone")
        assertEquals(LoginResult.TotpRequired, result)
        assertEquals(null, api.deviceToken)
    }

    @Test
    fun loginWithTotpCodeSucceeds() = runTest {
        val transport = FakeTransport { request ->
            if (request.body?.contains("123456") == true) {
                HttpResponse(200, """{"deviceToken":"tok-totp"}""")
            } else {
                HttpResponse(200, """{"status":"totp_required"}""")
            }
        }
        val api = SentinelApi(config, transport)
        assertEquals(LoginResult.TotpRequired, api.login("Skyler", "pw", "phone"))
        val second = api.login("Skyler", "pw", "phone", totpCode = "123456")
        assertTrue(second is LoginResult.Success)
    }

    @Test
    fun loginBadPassphraseFailsWithMessage() = runTest {
        val transport = FakeTransport {
            HttpResponse(401, """{"message":"That name and passphrase did not match."}""")
        }
        val api = SentinelApi(config, transport)
        val result = api.login("Skyler", "wrong", "phone")
        assertTrue(result is LoginResult.Failed)
    }

    @Test
    fun tokenIsAttachedToAuthenticatedCalls() = runTest {
        val transport = FakeTransport { HttpResponse(200, """{"followUpsDue":1}""") }
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
        val transport = FakeTransport { HttpResponse(200, """{}""") }
        val api = SentinelApi(ApiConfig("https://x.test", "", ""), transport)
        api.deviceToken = "t"
        api.me()
        val sent = transport.requests.single()
        assertEquals(null, sent.headers["CF-Access-Client-Id"])
        assertEquals(null, sent.headers["CF-Access-Client-Secret"])
    }
}
