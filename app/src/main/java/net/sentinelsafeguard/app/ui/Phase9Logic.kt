package net.sentinelsafeguard.app.ui

import kotlinx.serialization.json.JsonObject

/**
 * Pure helpers for the Phase 9 (hunt14) surfaces — the portal
 * shell and the STAFF-shell verification — kept free of Compose
 * so the unit suite can pin them against the real captured
 * payloads (ContractPhase9Test) and the /api/app/me payloads
 * (role-shape tests). Vocabularies are web's own: quote
 * decisions are approve/decline, proposal decisions are
 * accept/decline (the handlers enforce exactly those sets),
 * portal languages are en/es (a_setportallang).
 */
object Phase9Logic {

    /** Web's quote-decision vocabulary (a_decidemyquote). */
    val QUOTE_DECISIONS: List<String> = listOf("approve", "decline")

    /** Web's proposal-decision vocabulary (a_cf2decidemyproposal). */
    val PROPOSAL_DECISIONS: List<String> = listOf("accept", "decline")

    /** Web's change-request vocabulary (a_cf2decidemychangerequest). */
    val CHANGE_DECISIONS: List<String> = listOf("approve", "decline")

    /** Portal languages (a_setportallang): en | es only. */
    val PORTAL_LANGS: List<Pair<String, String>> = listOf(
        "en" to "English",
        "es" to "Español",
    )

    /** Survey rating scale web's answer form offers. */
    val SURVEY_RATINGS: List<Int> = listOf(1, 2, 3, 4, 5)

    /** The approval-inbox total pending count from its counts
     *  object — the shell's badge for Contracts & Reports and
     *  My Sign-offs alike. */
    fun inboxTotal(inbox: JsonObject): Int =
        Phase3Logic.obj(inbox, "counts")?.let { Phase3Logic.int(it, "total") } ?: 0

    /** Progress rows carry web's staged progress fields; the
     *  "n of m" line renders from the payload's own stage list. */
    fun progressLine(row: JsonObject): String {
        val stage = Phase3Logic.str(row, "progressStage")
        val stages = Phase3Logic.arrRaw(row, "progressStages")
        val idx = Phase3Logic.int(row, "progressStageIndex")
        return when {
            stage.isBlank() -> Phase3Logic.str(row, "status")
            idx != null && stages.isNotEmpty() ->
                "$stage (${idx + 1} of ${stages.size})"
            else -> stage
        }
    }

    /** A document row's headline: portal documents ride the
     *  home payload's documents/library sections with web's
     *  title-first tables. */
    fun documentTitle(row: JsonObject): String =
        Phase8bLogic.rowTitle(row)

    /** Whether a contract row is still awaiting the client's
     *  signature (web's signable states in a_signcfcontract). */
    fun contractSignable(row: JsonObject): Boolean =
        Phase3Logic.str(row, "status") in setOf("sent", "awaiting", "partially-signed")

    /** Whether an authorization is awaiting signature
     *  (a_expsignauthorization's own gate word). */
    fun authorizationAwaiting(row: JsonObject): Boolean =
        Phase3Logic.str(row, "status") == "awaiting"
}
