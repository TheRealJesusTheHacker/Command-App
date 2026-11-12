package net.sentinelsafeguard.app.ui

import net.sentinelsafeguard.app.data.api.Client

/**
 * Family / Friends tier logic (AUDIT-171 tier order, 2026-11-11;
 * TIER Phase B wiring, 1.7.2).
 *
 * The tier rides the client record from the server. Since Phase B
 * the record carries the server's own three-state computation,
 * `tierState` ("family" | "friends" | "standard", Family
 * precedence) — that field is the source of truth for display and
 * charge gating whenever the payload carries it. Payloads that
 * predate it leave [Client.tierState] empty, and the derivation
 * falls back to the record flags (`familyLinked` / `family` /
 * `friends` — migration 0143), exactly as AUDIT-171 derived it.
 * Family and Friends are mutually exclusive and Family takes
 * precedence (the server's rule):
 *
 *  - Family records are NEVER charged.
 *  - Friends records get 20% off, shown as an explicit discount
 *    line (the server computes the cents; the app renders them).
 *  - Everyone else is Standard.
 *
 * ⚠️ The client record's `tier` key is the TRIAGE tag
 * ("tier-a"…"tier-d") — billing logic never reads it.
 *
 * Tier ASSIGNMENT is owner-only: Skyler alone issues tier status.
 * The grant control renders only for the owner account ([isOwner],
 * from the server-issued role on /api/app/me — never a client-side
 * guess) and its write rides the dedicated owner-gated endpoint
 * (SentinelApi.setClientTier).
 */
object TierLogic {

    enum class Tier { FAMILY, FRIENDS, STANDARD }

    /** The Phase B state token for a tier — the exact string the
     *  tier write endpoint and the record's tierState carry. */
    fun wireToken(tier: Tier): String = when (tier) {
        Tier.FAMILY -> "family"
        Tier.FRIENDS -> "friends"
        Tier.STANDARD -> "standard"
    }

    /** Parse a tierState value; null when the payload carries no
     *  usable state (absent/empty/unknown) — callers then derive
     *  from the flags instead of guessing. */
    fun fromState(state: String?): Tier? = when (state?.trim()?.lowercase()) {
        "family" -> Tier.FAMILY
        "friends" -> Tier.FRIENDS
        "standard" -> Tier.STANDARD
        else -> null
    }

    fun tierOf(client: Client): Tier =
        fromState(client.tierState)
            ?: tierOf(client.familyLinked || client.family, client.friends)

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
     * Family records outright; the server refuses them too. Reads
     * the same derivation as the display, so a record the server
     * states is Family is unchargeable however the flags ride. */
    fun canCharge(client: Client): Boolean = tierOf(client) != Tier.FAMILY

    fun canCharge(familyLinked: Boolean): Boolean = !familyLinked
}
