package net.sentinelsafeguard.app

import kotlinx.serialization.json.Json
import net.sentinelsafeguard.app.data.api.Client
import net.sentinelsafeguard.app.data.api.ClientWriteRequest
import net.sentinelsafeguard.app.ui.TierLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AUDIT-171 tier order: display derivation, charge gating, the
 * owner-only grant gate, and the T-1 contract that a generic
 * client save can never carry a tier field.
 */
class TierLogicTest {

    private fun client(family: Boolean = false, friends: Boolean = false) =
        Client(id = 1, legalName = "Tier Test LLC", familyLinked = family, friends = friends)

    @Test
    fun familyTakesPrecedenceOverFriends() {
        assertEquals(
            TierLogic.Tier.FAMILY,
            TierLogic.tierOf(client(family = true, friends = true)),
        )
    }

    @Test
    fun friendsAloneIsFriends() {
        assertEquals(TierLogic.Tier.FRIENDS, TierLogic.tierOf(client(friends = true)))
    }

    @Test
    fun neitherFlagIsStandard() {
        assertEquals(TierLogic.Tier.STANDARD, TierLogic.tierOf(client()))
    }

    @Test
    fun labelsNameAllThreeStates() {
        assertEquals("Family", TierLogic.label(TierLogic.Tier.FAMILY))
        assertEquals("Friends", TierLogic.label(TierLogic.Tier.FRIENDS))
        assertEquals("Standard", TierLogic.label(TierLogic.Tier.STANDARD))
    }

    @Test
    fun familyIsNeverChargeable() {
        assertFalse(TierLogic.canCharge(client(family = true)))
        assertFalse(TierLogic.canCharge(familyLinked = true))
        assertTrue(TierLogic.canCharge(client(friends = true)))
        assertTrue(TierLogic.canCharge(client()))
    }

    @Test
    fun grantGateIsOwnerOnlyFromTheServerRole() {
        assertTrue(TierLogic.isOwner("OWNER"))
        assertFalse(TierLogic.isOwner("ADMIN"))
        assertFalse(TierLogic.isOwner("STAFF"))
        assertFalse(TierLogic.isOwner("employee"))
        assertFalse(TierLogic.isOwner(null))
        assertFalse(TierLogic.isOwner(""))
    }

    @Test
    fun chargeNotesStateTheStandingRules() {
        assertTrue(TierLogic.chargeNote(TierLogic.Tier.FAMILY).contains("never charged"))
        assertTrue(TierLogic.chargeNote(TierLogic.Tier.FRIENDS).contains("20% off"))
    }

    // T-1 (security): the generic save payload must not be able to
    // grant OR clear a tier — no friends/family key may serialize.
    @Test
    fun clientWriteRequestCarriesNoTierField() {
        val json = Json { encodeDefaults = true }
            .encodeToString(ClientWriteRequest.serializer(), ClientWriteRequest(legalName = "X"))
        assertFalse(json.contains("\"friends\""))
        assertFalse(json.contains("\"family\""))
        assertFalse(json.contains("\"familyLinked\""))
    }

    // ---- TIER Phase B: tierState is the source of truth ---------

    private val wire = Json { ignoreUnknownKeys = true }

    private fun decodeClient(body: String): Client =
        wire.decodeFromString(Client.serializer(), body)

    @Test
    fun tierStateParsesFromThePhaseBPayload() {
        val c = decodeClient(
            """{"id": 7, "legalName": "State Co", "tier": "tier-b",
               "friends": true, "family": false, "familyLinked": false,
               "tierState": "friends"}""",
        )
        assertEquals("friends", c.tierState)
        assertTrue(c.friends)
        assertFalse(c.family)
        // The TRIAGE tag rides its own key and is not billing:
        assertEquals("tier-b", c.tier)
        assertEquals(TierLogic.Tier.FRIENDS, TierLogic.tierOf(c))
    }

    @Test
    fun tierStateWinsOverTheFlags() {
        // The server's computed state outranks any flag mix the
        // payload also carries — display and charge gating read
        // ONE derivation.
        val c = decodeClient(
            """{"id": 8, "legalName": "Wins Co", "friends": false,
               "family": false, "familyLinked": true,
               "tierState": "standard"}""",
        )
        assertEquals(TierLogic.Tier.STANDARD, TierLogic.tierOf(c))
        assertTrue(TierLogic.canCharge(c))
    }

    @Test
    fun familyStateIsUnchargeableEvenWithoutTheLinkedMarker() {
        val c = decodeClient(
            """{"id": 9, "legalName": "Fam Co", "friends": false,
               "family": true, "familyLinked": false,
               "tierState": "family"}""",
        )
        assertEquals(TierLogic.Tier.FAMILY, TierLogic.tierOf(c))
        assertFalse(TierLogic.canCharge(c))
    }

    @Test
    fun triageTagNeverDrivesBilling() {
        // A "tier-a" triage tag with no billing state is Standard.
        val c = client().copy(tier = "tier-a")
        assertEquals(TierLogic.Tier.STANDARD, TierLogic.tierOf(c))
        val fam = client(family = true).copy(tier = "tier-d")
        assertEquals(TierLogic.Tier.FAMILY, TierLogic.tierOf(fam))
    }

    @Test
    fun absentTierStateFallsBackToTheFlags() {
        // Pre-Phase-B payloads carry no tierState key at all.
        val c = decodeClient(
            """{"id": 10, "legalName": "Old Co", "friends": true,
               "familyLinked": false}""",
        )
        assertEquals("", c.tierState)
        assertEquals(TierLogic.Tier.FRIENDS, TierLogic.tierOf(c))
        val fam = decodeClient(
            """{"id": 11, "legalName": "Old Fam", "friends": true,
               "familyLinked": true}""",
        )
        assertEquals(TierLogic.Tier.FAMILY, TierLogic.tierOf(fam))
    }

    @Test
    fun unknownTierStateFallsBackRatherThanGuessing() {
        val c = client(friends = true).copy(tierState = "platinum")
        assertEquals(TierLogic.Tier.FRIENDS, TierLogic.tierOf(c))
    }

    @Test
    fun wireTokensAreTheContractTokens() {
        assertEquals("family", TierLogic.wireToken(TierLogic.Tier.FAMILY))
        assertEquals("friends", TierLogic.wireToken(TierLogic.Tier.FRIENDS))
        assertEquals("standard", TierLogic.wireToken(TierLogic.Tier.STANDARD))
        assertEquals(TierLogic.Tier.FAMILY, TierLogic.fromState("family"))
        assertEquals(TierLogic.Tier.FRIENDS, TierLogic.fromState(" Friends "))
        assertEquals(TierLogic.Tier.STANDARD, TierLogic.fromState("STANDARD"))
        assertEquals(null, TierLogic.fromState("platinum"))
        assertEquals(null, TierLogic.fromState(""))
        assertEquals(null, TierLogic.fromState(null))
    }
}
