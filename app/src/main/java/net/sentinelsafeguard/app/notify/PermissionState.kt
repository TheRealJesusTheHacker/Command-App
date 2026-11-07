package net.sentinelsafeguard.app.notify

/**
 * Hotfix 1.4.1 — the notification-permission state model.
 *
 * Why this exists: on Android 13+ the POST_NOTIFICATIONS
 * permission can sit in a DECIDED-DENIED state (denied once, or
 * burned by the platform before the app ever asked — creating
 * notification channels from Application.onCreate / a background
 * worker before any explicit request does exactly that). In
 * that state the system's RequestPermission prompt returns
 * instantly with granted=false and shows NO dialog, so 1.4.0's
 * "Allow" tap looked dead ("I got this but no real
 * permission", owner report 2026-11-07). Permission decisions
 * persist across in-place upgrades, so the denial predates the
 * explainer that first asked.
 *
 * The model is pure (booleans in, state out) and unit-tested:
 * the UI feeds it the platform's live answers — the grant check,
 * the persisted "we launched a request that completed" flag
 * (SettingsStore.notifyPermissionAsked), and Android's
 * shouldShowRequestPermissionRationale — and never guesses.
 */
enum class NotifPermissionState {
    /** Android will show this app's notifications. */
    GRANTED,

    /** Never asked (per our own bookkeeping), or asked and the
     *  platform still offers a rationale — the system prompt
     *  will appear if we launch it. */
    ASKABLE,

    /** Asked, no rationale, not granted: the system prompt
     *  will NOT appear anymore. The only remaining route is
     *  Android's per-app notification settings screen. */
    DENIED_FINAL,
}

/** What the explainer's Allow button must do in each state. */
enum class NotifAllowAction {
    LAUNCH_SYSTEM_REQUEST,
    OPEN_NOTIFICATION_SETTINGS,
}

object NotifPermission {

    fun state(
        granted: Boolean,
        askedBefore: Boolean,
        showRationale: Boolean,
    ): NotifPermissionState = when {
        granted -> NotifPermissionState.GRANTED
        !askedBefore || showRationale -> NotifPermissionState.ASKABLE
        else -> NotifPermissionState.DENIED_FINAL
    }

    fun allowAction(state: NotifPermissionState): NotifAllowAction = when (state) {
        NotifPermissionState.DENIED_FINAL ->
            NotifAllowAction.OPEN_NOTIFICATION_SETTINGS
        NotifPermissionState.GRANTED,
        NotifPermissionState.ASKABLE ->
            NotifAllowAction.LAUNCH_SYSTEM_REQUEST
    }

    /**
     * First-run dialog visibility. Beyond the 1.4.0 rules
     * (never when granted, never after "Not now", never again
     * this session after a settings trip), one deliberate
     * change: when a launch completes and lands the permission
     * in DENIED_FINAL, the dialog STAYS up — it has just
     * changed shape (its button is now "Open Settings" with the
     * plain-words reason), so the surface shows the current
     * true state instead of vanishing on a dead-looking tap. A
     * launch that leaves the state ASKABLE (a soft first
     * denial) still closes the dialog for the session, exactly
     * as 1.4.0 behaved — the explanation is never a nag loop.
     */
    fun shouldShowFirstRunDialog(
        state: NotifPermissionState,
        explainerDismissed: Boolean,
        askedThisSession: Boolean,
        settingsTripThisSession: Boolean,
    ): Boolean {
        if (state == NotifPermissionState.GRANTED) return false
        if (explainerDismissed) return false
        if (settingsTripThisSession) return false
        if (askedThisSession && state == NotifPermissionState.ASKABLE) return false
        return true
    }
}
