package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.data.mail.MailRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MailRulesTest {

    @Test
    fun sharedFolderRepliesSendAsSentinel() {
        assertEquals(
            "sentinel@sentinelsafeguard.net",
            MailRules.identityForFolder("Shared.sentinel.INBOX"),
        )
        assertEquals(
            "sentinel@sentinelsafeguard.net",
            MailRules.identityForFolder("Shared.sentinel.Sent"),
        )
        assertTrue(MailRules.isSharedFolder("Shared.sentinel"))
    }

    @Test
    fun personalFolderRepliesSendAsAccount() {
        assertEquals(
            "skyler@sentinelsafeguard.net",
            MailRules.identityForFolder("INBOX"),
        )
        assertEquals(
            "skyler@sentinelsafeguard.net",
            MailRules.identityForFolder("Archive"),
        )
        assertFalse(MailRules.isSharedFolder("INBOX"))
        assertFalse(MailRules.isSharedFolder("Shared.other.INBOX"))
    }

    @Test
    fun archiveAlwaysTargetsTheKeepForeverFolder() {
        assertEquals("Archive", MailRules.archiveTarget())
        assertEquals("Trash", MailRules.trashTarget())
    }

    @Test
    fun identityHonoursACustomAccountAddress() {
        assertEquals(
            "me@example.com",
            MailRules.identityForFolder("INBOX", "me@example.com"),
        )
        // ...but a shared folder still wins over the account address.
        assertEquals(
            "sentinel@sentinelsafeguard.net",
            MailRules.identityForFolder("Shared.sentinel.INBOX", "me@example.com"),
        )
    }
}
