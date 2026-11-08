package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.notify.PushLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 1.5.1 — the push-registration lifecycle pins.
 *
 * The defect: registration fired only while `registered` was
 * false, so a registration made against the pre-activation
 * server latched forever and the device never re-registered
 * after push actually turned on. The lifecycle now runs on
 * (registered, registeredVersionCode) against the running
 * build's versionCode; PushStore.resetDeliveryState — wired
 * into AppViewModel.signOut in this release — leaves the store
 * at exactly (false, 0), which the first test pins as a
 * must-register state.
 */
class PushRegistrationTest {

    // The build that shipped the defect, and this hotfix.
    private val v150 = 106
    private val v151 = 107

    @Test
    fun signOutResetStateRegistersAgain() {
        // resetDeliveryState() leaves (registered=false,
        // version=0): the next sign-in's worker run must register.
        assertTrue(PushLogic.needsRegistration(false, 0, v151))
    }

    @Test
    fun neverRegisteredRegisters() {
        assertTrue(PushLogic.needsRegistration(false, 0, v150))
    }

    @Test
    fun staleVersionRegistrationReRegistersOnceThenLatches() {
        // A 1.5.0-era registration under a 1.5.1 build is stale.
        assertTrue(PushLogic.needsRegistration(true, v150, v151))
        // The successful heal records the current version…
        val healed = PushLogic.afterRegistrationAttempt(
            succeeded = true,
            registered = true,
            registeredVersionCode = v150,
            currentVersionCode = v151,
        )
        assertEquals(true to v151, healed)
        // …and from then on the same build stays latched.
        assertFalse(
            PushLogic.needsRegistration(healed.first, healed.second, v151),
        )
    }

    @Test
    fun missingVersionRecordHealsOnce() {
        // Every pre-1.5.1 install: registered, but no version was
        // ever recorded (0) — including the registration made
        // against the pre-activation server. It must heal.
        assertTrue(PushLogic.needsRegistration(true, 0, v151))
        val healed = PushLogic.afterRegistrationAttempt(
            succeeded = true,
            registered = true,
            registeredVersionCode = 0,
            currentVersionCode = v151,
        )
        assertFalse(
            PushLogic.needsRegistration(healed.first, healed.second, v151),
        )
    }

    @Test
    fun failedRegisterDoesNotLatch() {
        // A failed first attempt persists the incoming state, so
        // the next run still sees a must-register device.
        val after = PushLogic.afterRegistrationAttempt(
            succeeded = false,
            registered = false,
            registeredVersionCode = 0,
            currentVersionCode = v151,
        )
        assertEquals(false to 0, after)
        assertTrue(PushLogic.needsRegistration(after.first, after.second, v151))
    }

    @Test
    fun failedHealDoesNotRecordTheNewVersion() {
        // A failed heal keeps the OLD version record: the state
        // still reads stale, so the next run retries the heal
        // instead of believing it happened.
        val after = PushLogic.afterRegistrationAttempt(
            succeeded = false,
            registered = true,
            registeredVersionCode = v150,
            currentVersionCode = v151,
        )
        assertEquals(true to v150, after)
        assertTrue(PushLogic.needsRegistration(after.first, after.second, v151))
    }

    @Test
    fun sameVersionRunsDoNotReRegister() {
        assertFalse(PushLogic.needsRegistration(true, v151, v151))
        assertFalse(PushLogic.needsRegistration(true, v150, v150))
    }

    @Test
    fun successfulFirstRegistrationRecordsTheVersion() {
        val after = PushLogic.afterRegistrationAttempt(
            succeeded = true,
            registered = false,
            registeredVersionCode = 0,
            currentVersionCode = v151,
        )
        assertEquals(true to v151, after)
        assertFalse(PushLogic.needsRegistration(after.first, after.second, v151))
    }
}
