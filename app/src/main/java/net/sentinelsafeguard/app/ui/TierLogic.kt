package net.sentinelsafeguard.app.ui

import net.sentinelsafeguard.app.data.api.Client

/**
 * Family / Friends tier logic (AUDIT-171 tier order, 2026-11-11).
 *
 * The tier rides the client record from the server (`familyLinked`
 * stamped from the family linkage, `friends` the record flag —
 * migration 0143). Family and Friends are mutually exclusive and
 * Family takes precedence (the server's rule; mirrored here for
 * display and charge gating):
 *
 *  - Family records are NEVER charged.
 *  - Friends records get 20% off, shown as an explicit discount
 *    line (the server computes the cents; the app renders them).
 *  - Everyone else is Standard.
 *
 * Tier ASSIGNMENT is owner-only: Skyler alone issues tier status.
 * The app displays and honors tier; the grant control renders only
 * for the owner account ([isOwner], from the server-issued role on
 * /api/app/me — never a client-side guess) and its write rides the
 * dedicated owner-gated server endpoint.
 */
object TierLogic {

    enum class Tier { FAMILY, FRIENDS, STANDARD }

    fun tierOf(client: Client): Tier = when {
        client.familyLinked -> Tier.FAMILY
        client.friends -> Tier.FRIENDS
        else -> Tier.STANDARD
    }

    fun tierOf(familyLinked: Boolean, friends: Boolean): Tier = when {
        familyLinked -> Tier.FAMILY
        friends -> Tier.FRIENDS
        else -> Tier.STANDARD
    }

    fun label(tier: Tier): String = when (tier) {
        Tier.FAMILY -> "Family"
        Tier.FRIENDS -> "Friends"
        Tier.STANDARD -> "Standard"
    }

    /** One honest line about what the tier means for charging. */
    fun chargeNote(tier: Tier): String = when (tier) {
        Tier.FAMILY -> "Family — never charged."
        Tier.FRIENDS -> "Friends — 20% off all prices, shown as a discount line."
        Tier.STANDARD -> "Standard pricing."
    }

    /**
     * The tier-grant control's render gate: the signed-in account
     * must BE the owner. The marker is the role the server issued
     * on the /api/app/me payload ("OWNER") — the same marker the
     * Archive/Employees/Knowledge owner surfaces already use.
     */
    fun isOwner(meRole: String?): Boolean = meRole == "OWNER"

    /** Charge-building flows (quotes, unbilled conversion) refuse
     * Family-linked records outright; the server refuses them too. */
    fun canCharge(client: Client): Boolean = !client.familyLinked

    fun canCharge(familyLinked: Boolean): Boolean = !familyLinked
}
