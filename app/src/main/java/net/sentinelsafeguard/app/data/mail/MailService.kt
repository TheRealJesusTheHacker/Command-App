package net.sentinelsafeguard.app.data.mail

import jakarta.mail.FetchProfile
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import jakarta.mail.search.BodyTerm
import jakarta.mail.search.FromTerm
import jakarta.mail.search.OrTerm
import jakarta.mail.search.SearchTerm
import jakarta.mail.search.SubjectTerm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sentinelsafeguard.app.config.AppConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

/**
 * Direct IMAP/SMTP to Sentinel Mail (spec §4.1) via Eclipse Angus Mail
 * (the maintained Jakarta Mail implementation). No hand-rolled protocol
 * code, no proxy through the Command backend, no server changes.
 *
 * The mailbox password is supplied per call from SecureStore by the
 * caller, is only ever sent to the mail server over TLS, and is never
 * logged or written anywhere by this class.
 *
 * Cache rule (spec §5): message lists live in memory only, for the one
 * open folder, and are dropped on sign-out via [clearCache]. Nothing
 * mail-shaped is written to disk.
 */
class MailService(
    private val host: String = AppConfig.MAIL_HOST,
) {
    private var cachedFolder: String? = null
    private var cachedSummaries: List<MailSummary> = emptyList()

    fun clearCache() {
        cachedFolder = null
        cachedSummaries = emptyList()
    }

    private fun imapSession(): Session {
        val props = Properties().apply {
            put("mail.imap.ssl.enable", "true")
            put("mail.imap.connectiontimeout", "20000")
            put("mail.imap.timeout", "30000")
            // Dovecot advertises the shared namespace under Shared.*
            put("mail.imap.ssl.trust", host)
        }
        return Session.getInstance(props)
    }

    private fun <T> withStore(username: String, password: String, block: (Store) -> T): T {
        val store = imapSession().getStore("imap")
        try {
            store.connect(host, AppConfig.MAIL_IMAP_PORT, username, password)
            return block(store)
        } finally {
            runCatching { store.close() }
        }
    }

    suspend fun listFolders(username: String, password: String): List<MailFolder> =
        withContext(Dispatchers.IO) {
            withStore(username, password) { store ->
                val folders = store.defaultFolder.list("*")
                    .filter { it.exists() }
                folders.map { folder ->
                    val unread = runCatching {
                        folder.open(Folder.READ_ONLY)
                        val count = folder.unreadMessageCount
                        folder.close(false)
                        count
                    }.getOrDefault(0)
                    MailFolder(
                        name = folder.fullName,
                        unreadCount = unread,
                        shared = MailRules.isSharedFolder(folder.fullName),
                    )
                }.sortedWith(compareBy({ !it.name.equals("INBOX", true) }, { it.name }))
            }
        }

    suspend fun totalUnread(username: String, password: String): Int =
        listFolders(username, password).sumOf { it.unreadCount }

    suspend fun listMessages(
        username: String,
        password: String,
        folderName: String,
        limit: Int = 50,
    ): List<MailSummary> = withContext(Dispatchers.IO) {
        if (cachedFolder == folderName && cachedSummaries.isNotEmpty()) {
            return@withContext cachedSummaries
        }
        withStore(username, password) { store ->
            val folder = store.getFolder(folderName)
            folder.open(Folder.READ_ONLY)
            try {
                val total = folder.messageCount
                if (total == 0) return@withStore emptyList()
                val start = maxOf(1, total - limit + 1)
                val messages = folder.getMessages(start, total)
                val profile = FetchProfile().apply {
                    add(FetchProfile.Item.ENVELOPE)
                    add(FetchProfile.Item.FLAGS)
                    add(FetchProfile.Item.CONTENT_INFO)
                }
                folder.fetch(messages, profile)
                val uidFolder = folder as jakarta.mail.UIDFolder
                val summaries = messages.reversed().map { message ->
                    MailSummary(
                        uid = uidFolder.getUID(message),
                        subject = message.subject ?: "(no subject)",
                        from = formatAddresses(message.from),
                        dateText = formatDate(message.receivedDate ?: message.sentDate),
                        unread = !message.isSet(Flags.Flag.SEEN),
                        snippet = "",
                    )
                }
                cachedFolder = folderName
                cachedSummaries = summaries
                summaries
            } finally {
                folder.close(false)
            }
        }
    }

    suspend fun getMessage(
        username: String,
        password: String,
        folderName: String,
        uid: Long,
    ): MailMessage = withContext(Dispatchers.IO) {
        withStore(username, password) { store ->
            val folder = store.getFolder(folderName)
            folder.open(Folder.READ_WRITE)
            try {
                val uidFolder = folder as jakarta.mail.UIDFolder
                val message = uidFolder.getMessageByUID(uid)
                    ?: throw IllegalStateException("That message could not be found. It may have been moved or deleted — go back and refresh the list.")
                val mail = MailMessage(
                    uid = uid,
                    subject = message.subject ?: "(no subject)",
                    from = formatAddresses(message.from),
                    to = formatAddresses(message.allRecipients),
                    dateText = formatDate(message.receivedDate ?: message.sentDate),
                    body = extractText(message),
                    unread = !message.isSet(Flags.Flag.SEEN),
                )
                message.setFlag(Flags.Flag.SEEN, true)
                mail
            } finally {
                folder.close(false)
            }
        }
    }

    suspend fun search(
        username: String,
        password: String,
        folderName: String,
        query: String,
        limit: Int = 50,
    ): List<MailSummary> = withContext(Dispatchers.IO) {
        withStore(username, password) { store ->
            val folder = store.getFolder(folderName)
            folder.open(Folder.READ_ONLY)
            try {
                val term: SearchTerm = OrTerm(
                    arrayOf(SubjectTerm(query), FromTerm(query), BodyTerm(query))
                )
                val found = folder.search(term).takeLast(limit).reversed()
                val uidFolder = folder as jakarta.mail.UIDFolder
                found.map { message ->
                    MailSummary(
                        uid = uidFolder.getUID(message),
                        subject = message.subject ?: "(no subject)",
                        from = formatAddresses(message.from),
                        dateText = formatDate(message.receivedDate ?: message.sentDate),
                        unread = !message.isSet(Flags.Flag.SEEN),
                        snippet = "",
                    )
                }
            } finally {
                folder.close(false)
            }
        }
    }

    /** Archive = move to the keep-forever folder (spec §4.2). */
    suspend fun archive(username: String, password: String, folderName: String, uid: Long) =
        move(username, password, folderName, uid, MailRules.archiveTarget())

    suspend fun delete(username: String, password: String, folderName: String, uid: Long) =
        move(username, password, folderName, uid, MailRules.trashTarget())

    private suspend fun move(
        username: String,
        password: String,
        fromFolder: String,
        uid: Long,
        toFolder: String,
    ) = withContext(Dispatchers.IO) {
        withStore(username, password) { store ->
            val source = store.getFolder(fromFolder)
            source.open(Folder.READ_WRITE)
            try {
                val uidFolder = source as jakarta.mail.UIDFolder
                val message = uidFolder.getMessageByUID(uid)
                    ?: throw IllegalStateException("That message could not be found. It may have been moved or deleted — go back and refresh the list.")
                val target = store.getFolder(toFolder)
                source.copyMessages(arrayOf<Message>(message), target)
                message.setFlag(Flags.Flag.DELETED, true)
                cachedFolder = null
                cachedSummaries = emptyList()
            } finally {
                source.close(true)
            }
        }
    }

    suspend fun markUnread(username: String, password: String, folderName: String, uid: Long) =
        withContext(Dispatchers.IO) {
            withStore(username, password) { store ->
                val folder = store.getFolder(folderName)
                folder.open(Folder.READ_WRITE)
                try {
                    val uidFolder = folder as jakarta.mail.UIDFolder
                    val message = uidFolder.getMessageByUID(uid)
                        ?: throw IllegalStateException("That message could not be found. It may have been moved or deleted.")
                    message.setFlag(Flags.Flag.SEEN, false)
                } finally {
                    folder.close(false)
                }
            }
        }

    /**
     * Reply over SMTP submission (587, STARTTLS). The From identity
     * follows the folder the original sits in (spec §4.1): shared
     * folders send as sentinel@, everything else as the account.
     */
    suspend fun reply(
        username: String,
        password: String,
        fromFolder: String,
        original: MailMessage,
        body: String,
    ) = withContext(Dispatchers.IO) {
        val identity = MailRules.identityForFolder(fromFolder, username)
        val props = Properties().apply {
            put("mail.smtp.host", host)
            put("mail.smtp.port", AppConfig.MAIL_SMTP_PORT.toString())
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.starttls.required", "true")
            put("mail.smtp.connectiontimeout", "20000")
            put("mail.smtp.timeout", "30000")
        }
        val session = Session.getInstance(props)
        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(identity))
            setRecipient(
                Message.RecipientType.TO,
                InternetAddress(extractAddress(original.from)),
            )
            subject = if (original.subject.startsWith("Re:", ignoreCase = true)) {
                original.subject
            } else {
                "Re: ${original.subject}"
            }
            setText("$body\n\n----- Original message -----\nFrom: ${original.from}\nDate: ${original.dateText}\nSubject: ${original.subject}\n\n${original.body}")
            setHeader("X-Sentinel-App", "Sentinel App")
        }
        val transport: Transport = session.getTransport("smtp")
        try {
            transport.connect(host, AppConfig.MAIL_SMTP_PORT, username, password)
            transport.sendMessage(message, message.allRecipients)
        } finally {
            runCatching { transport.close() }
        }
    }

    private fun extractText(part: Part): String {
        return try {
            when {
                part.isMimeType("text/plain") -> part.content?.toString() ?: ""
                part.isMimeType("multipart/*") -> {
                    val multipart = part.content as? Multipart ?: return ""
                    val texts = (0 until multipart.count).map { extractText(multipart.getBodyPart(it)) }
                    texts.firstOrNull { it.isNotBlank() } ?: ""
                }
                part.isMimeType("text/html") -> {
                    val html = part.content?.toString() ?: ""
                    html.replace(Regex("<[^>]+>"), " ")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                }
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun formatAddresses(addresses: Array<out jakarta.mail.Address>?): String =
        addresses?.joinToString(", ") { it.toString() } ?: ""

    private fun extractAddress(formatted: String): String {
        val match = Regex("<([^>]+)>").find(formatted)
        return match?.groupValues?.get(1) ?: formatted.trim()
    }

    private fun formatDate(date: Date?): String {
        if (date == null) return ""
        return SimpleDateFormat("EEE, MMM d, yyyy h:mm a", Locale.US).format(date)
    }
}
