package net.sentinelsafeguard.app

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.config.ApiConfig
import net.sentinelsafeguard.app.data.api.ApiException
import net.sentinelsafeguard.app.data.api.HttpResponse
import net.sentinelsafeguard.app.data.api.SentinelApi
import net.sentinelsafeguard.app.data.api.ctosSaveProfile
import net.sentinelsafeguard.app.ui.TierLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * TIER Phase B wiring pins (Command App 1.7.2): the tier write
 * rides POST /api/app/clients/{id}/tier with exactly one body
 * key, the success envelope's fresh record is what comes back,
 * and refusals surface the server's own message verbatim (the
 * owner lock and Family precedence are the server's sentences,
 * never a paraphrase). The OPSEC save (S-2) wraps its changed
 * fields under "profile" and nothing else.
 */
class TierPhaseBApiTest {

    private val config = ApiConfig(
        baseUrl = "https://command.example.test",
        cfAccessClientId = "test-client-id",
        cfAccessClientSecret = "test-client-secret",
    )

    private fun api(transport: FakeTransport): SentinelApi =
        SentinelApi(config, transport).also { it.deviceToken = "tok-test" }

    @Test
    fun setClientTierPostsTheOneKeyBodyToTheTwin() = runTest {
        val transport = FakeTransport {
            HttpResponse(
                200,
                """{"ok": true, "tierState": "friends",
                   "client": {"id": 42, "legalName": "Tier Co",
                              "tier": "tier-c", "friends": true,
                              "family": false, "familyLinked": false,
                              "tierState": "friends"}}""",
            )
        }
        val client = api(transport).setClientTier(42, "friends")
        val sent = transport.requests.single()
        assertEquals("POST", sent.method)
        assertEquals("/api/app/clients/42/tier", sent.path)
        assertEquals("""{"tier":"friends"}""", sent.body)
        assertEquals("Bearer tok-test", sent.headers["Authorization"])
        // The fresh record comes back whole: state + flags + the
        // untouched triage tag on its own key.
        assertEquals(42, client.id)
        assertEquals("friends", client.tierState)
        assertTrue(client.friends)
        assertEquals("tier-c", client.tier)
        assertEquals(TierLogic.Tier.FRIENDS, TierLogic.tierOf(client))
    }

    @Test
    fun setClientTierSurfacesTheOwnerLockVerbatim() = runTest {
        val lock = "Only the owner can change a client's tier — " +
            "Family and Friends status is the owner's alone to issue."
        val transport = FakeTransport {
            HttpResponse(
                403,
                """{"ok": false, "authorized": false, "message": "$lock"}""",
            )
        }
        try {
            api(transport).setClientTier(42, "family")
            fail("the refusal must throw")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertEquals(lock, e.message)
        }
    }

    @Test
    fun setClientTierSurfacesFamilyPrecedenceVerbatim() = runTest {
        val msg = "This record is Family — family accounts are never " +
            "charged, so the Friends tier can't apply. Family takes precedence."
        val transport = FakeTransport {
            HttpResponse(
                403,
                """{"ok": false, "authorized": false, "message": "$msg"}""",
            )
        }
        try {
            api(transport).setClientTier(42, "friends")
            fail("the refusal must throw")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertEquals(msg, e.message)
        }
    }

    @Test
    fun setClientTierMissingClientIsTheServerSentence() = runTest {
        val transport = FakeTransport {
            HttpResponse(404, """{"ok": false, "message": "Client not found."}""")
        }
        try {
            api(transport).setClientTier(999999, "standard")
            fail("the refusal must throw")
        } catch (e: ApiException) {
            assertEquals(404, e.status)
            assertEquals("Client not found.", e.message)
        }
    }

    @Test
    fun ctosSaveProfileWrapsOnlyTheProfileObject() = runTest {
        val transport = FakeTransport {
            HttpResponse(
                200,
                """{"ok": true,
                   "profile": {"doh": "quad9", "dohCustom": "",
                               "uaRotate": true, "jitterMs": 250},
                   "profileOptions": {"providers": ["google", "cloudflare", "quad9", "custom"],
                                      "wired": [], "unavailable": []}}""",
            )
        }
        val out = api(transport).ctosSaveProfile(
            buildJsonObject { put("doh", "quad9"); put("uaRotate", true) },
        )
        val sent = transport.requests.single()
        assertEquals("POST", sent.method)
        assertEquals("/api/app/osint/ctos/profile", sent.path)
        assertEquals("""{"profile":{"doh":"quad9","uaRotate":true}}""", sent.body)
        assertEquals("quad9", out["profile"]!!.let {
            (it as kotlinx.serialization.json.JsonObject)["doh"].toString().trim('"')
        })
    }

    @Test
    fun ctosSaveProfileSurfacesHandlerRefusalsVerbatim() = runTest {
        val transport = FakeTransport {
            HttpResponse(
                403,
                """{"ok": false, "message": "Unknown DoH provider."}""",
            )
        }
        try {
            api(transport).ctosSaveProfile(buildJsonObject { put("doh", "bogus") })
            fail("the refusal must throw")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertEquals("Unknown DoH provider.", e.message)
        }
    }
}
