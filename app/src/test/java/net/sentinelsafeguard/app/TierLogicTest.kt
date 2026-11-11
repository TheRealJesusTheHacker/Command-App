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
}
