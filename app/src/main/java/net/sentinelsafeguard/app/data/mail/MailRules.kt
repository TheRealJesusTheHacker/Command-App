package net.sentinelsafeguard.app.data.mail

/**
 * Pure mail rules (spec §4) — kept free of any mail library so they are
 * unit-tested directly.
 *
 * Folder identity rule: mail lives in skyler@sentinelsafeguard.net, and
 * the shared business folders arrive as Shared.sentinel.* under the
 * scoped sharing grant. A reply written from a shared folder is sent
 * as sentinel@sentinelsafeguard.net; a reply from anywhere else is
 * sent as the signed-in mailbox itself.
 */
object MailRules {

    const val PERSONAL_ADDRESS = "skyler@sentinelsafeguard.net"
    const val SHARED_ADDRESS = "sentinel@sentinelsafeguard.net"
    const val SHARED_PREFIX = "Shared.sentinel"
    const val ARCHIVE_FOLDER = "Archive"
    const val TRASH_FOLDER = "Trash"

    fun isSharedFolder(folderName: String): Boolean =
        folderName.equals(SHARED_PREFIX, ignoreCase = true) ||
            folderName.startsWith("$SHARED_PREFIX.", ignoreCase = true)

    /** The From address a reply from [folderName] must use. */
    fun identityForFolder(folderName: String, accountAddress: String = PERSONAL_ADDRESS): String =
        if (isSharedFolder(folderName)) SHARED_ADDRESS else accountAddress

    /** Archive is the keep-forever folder (spec §4.2) — always the same target. */
    fun archiveTarget(): String = ARCHIVE_FOLDER

    fun trashTarget(): String = TRASH_FOLDER

    /** True for the folders the folder list shows first-class. */
    fun isSystemFolder(folderName: String): Boolean {
        val leaf = folderName.substringAfterLast('.').substringAfterLast('/')
        return leaf.equals("INBOX", ignoreCase = true) ||
            leaf.equals(ARCHIVE_FOLDER, ignoreCase = true) ||
            leaf.equals(TRASH_FOLDER, ignoreCase = true) ||
            leaf.equals("Sent Items", ignoreCase = true) ||
            leaf.equals("Drafts", ignoreCase = true) ||
            leaf.equals("Junk", ignoreCase = true)
    }
}

data class MailFolder(
    val name: String,
    val unreadCount: Int,
    val shared: Boolean,
)

data class MailSummary(
    val uid: Long,
    val subject: String,
    val from: String,
    val dateText: String,
    val unread: Boolean,
    val snippet: String,
)

data class MailMessage(
    val uid: Long,
    val subject: String,
    val from: String,
    val to: String,
    val dateText: String,
    val body: String,
    val unread: Boolean,
)
