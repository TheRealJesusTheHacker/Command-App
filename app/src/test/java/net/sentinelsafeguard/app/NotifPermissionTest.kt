package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.notify.NotifAllowAction
import net.sentinelsafeguard.app.notify.NotifPermission
import net.sentinelsafeguard.app.notify.NotifPermissionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hotfix 1.4.1 — pins the notification-permission state model
 * (notify/PermissionState.kt). The owner's 1.4.0 report: the
 * explainer's Allow tap produced no system prompt and no
 * permission — the decided-denied state these transitions exist
 * to detect and route around (via Android Settings).
 */
class NotifPermissionTest {

    @Test
    fun grantedWinsOverEverything() {
        assertEquals(
            NotifPermissionState.GRANTED,
            NotifPermission.state(granted = true, askedBefore = false, showRationale = false),
        )
        assertEquals(
            NotifPermissionState.GRANTED,
            NotifPermission.state(granted = true, askedBefore = true, showRationale = false),
        )
        assertEquals(
            NotifPermissionState.GRANTED,
            NotifPermission.state(granted = true, askedBefore = true, showRationale = true),
        )
    }

    @Test
    fun neverAskedIsAskable() {
        // Rationale is false before the first ask on Android —
        // the persisted asked flag is what keeps this ASKABLE
        // instead of misreading it as decided-denied.
        assertEquals(
            NotifPermissionState.ASKABLE,
            NotifPermission.state(granted = false, askedBefore = false, showRationale = false),
        )
    }

    @Test
    fun askedWithRationaleIsStillAskable() {
        assertEquals(
            NotifPermissionState.ASKABLE,
            NotifPermission.state(granted = false, askedBefore = true, showRationale = true),
        )
    }

    @Test
    fun askedNoRationaleDeniedIsFinal() {
        assertEquals(
            NotifPermissionState.DENIED_FINAL,
            NotifPermission.state(granted = false, askedBefore = true, showRationale = false),
        )
    }

    @Test
    fun allowActionRoutesByState() {
        assertEquals(
            NotifAllowAction.LAUNCH_SYSTEM_REQUEST,
            NotifPermission.allowAction(NotifPermissionState.ASKABLE),
        )
        assertEquals(
            NotifAllowAction.LAUNCH_SYSTEM_REQUEST,
            NotifPermission.allowAction(NotifPermissionState.GRANTED),
        )
        assertEquals(
            NotifAllowAction.OPEN_NOTIFICATION_SETTINGS,
            NotifPermission.allowAction(NotifPermissionState.DENIED_FINAL),
        )
    }

    @Test
    fun firstRunDialogHiddenWhenGrantedDismissedOrAfterSettingsTrip() {
        assertFalse(
            NotifPermission.shouldShowFirstRunDialog(
                NotifPermissionState.GRANTED,
                explainerDismissed = false, askedThisSession = false,
                settingsTripThisSession = false,
            ),
        )
        assertFalse(
            NotifPermission.shouldShowFirstRunDialog(
                NotifPermissionState.ASKABLE,
                explainerDismissed = true, askedThisSession = false,
                settingsTripThisSession = false,
            ),
        )
        assertFalse(
            NotifPermission.shouldShowFirstRunDialog(
                NotifPermissionState.DENIED_FINAL,
                explainerDismissed = false, askedThisSession = false,
                settingsTripThisSession = true,
            ),
        )
    }

    @Test
    fun firstRunDialogShowsOnFreshAskable() {
        assertTrue(
            NotifPermission.shouldShowFirstRunDialog(
                NotifPermissionState.ASKABLE,
                explainerDismissed = false, askedThisSession = false,
                settingsTripThisSession = false,
            ),
        )
    }

    @Test
    fun softDenialClosesDialogForTheSession() {
        // Asked, still ASKABLE (rationale offered): the 1.4.0
        // no-nag behavior — the dialog does not re-pop.
        assertFalse(
            NotifPermission.shouldShowFirstRunDialog(
                NotifPermissionState.ASKABLE,
                explainerDismissed = false, askedThisSession = true,
                settingsTripThisSession = false,
            ),
        )
    }

    @Test
    fun decidedDenialKeepsDialogUpInItsSettingsShape() {
        // The 1.4.1 morph case: a launch that lands in
        // DENIED_FINAL must not vanish — the same dialog now
        // carries the "Open Settings" route and the true state.
        assertTrue(
            NotifPermission.shouldShowFirstRunDialog(
                NotifPermissionState.DENIED_FINAL,
                explainerDismissed = false, askedThisSession = true,
                settingsTripThisSession = false,
            ),
        )
        assertTrue(
            NotifPermission.shouldShowFirstRunDialog(
                NotifPermissionState.DENIED_FINAL,
                explainerDismissed = false, askedThisSession = false,
                settingsTripThisSession = false,
            ),
        )
    }
}
