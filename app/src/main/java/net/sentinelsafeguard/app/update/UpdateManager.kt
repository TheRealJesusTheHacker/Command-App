package net.sentinelsafeguard.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sentinelsafeguard.app.BuildConfig
import net.sentinelsafeguard.app.data.api.SentinelApi
import net.sentinelsafeguard.app.data.api.VersionResponse
import net.sentinelsafeguard.app.util.VersionCompare
import java.io.File
import java.security.MessageDigest

sealed interface UpdateState {
    data object UpToDate : UpdateState
    data class Available(val info: VersionResponse) : UpdateState
}

/** The downloaded update did not match what the server said it sent. */
class UpdateVerificationException(message: String) : Exception(message)

/**
 * Pure update decisions + integrity checks (U-04), kept free of
 * Android and network types so they are unit-tested directly.
 */
object UpdateVerify {

    /**
     * An update is offered only when the signals the server sent
     * agree it is newer. When the server sends a versionCode it is
     * authoritative for ordering (a name alone can be relabelled):
     * the code must be greater than the installed code, and the
     * name — when also sent — must not be older than the installed
     * name (a staged rollback is never offered as an "update").
     * When no code is sent, the name comparison decides, as before.
     */
    fun isUpdateAvailable(
        remoteVersionCode: Int?,
        remoteVersion: String?,
        currentVersionCode: Int,
        currentVersion: String,
    ): Boolean {
        if (remoteVersionCode != null) {
            if (remoteVersionCode <= currentVersionCode) return false
            if (remoteVersion != null &&
                VersionCompare.isNewer(currentVersion, remoteVersion)
            ) {
                return false
            }
            return true
        }
        return remoteVersion != null &&
            VersionCompare.isNewer(remoteVersion, currentVersion)
    }

    /** Lowercase hex SHA-256 of [data]. */
    fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Returns a plain-language reason the downloaded bytes do NOT
     * match the server's stated facts, or null when they match (or
     * when the server stated nothing to check against).
     */
    fun mismatchReason(
        actualSizeBytes: Long,
        expectedSizeBytes: Long?,
        actualSha256: String,
        expectedSha256: String?,
    ): String? {
        if (expectedSizeBytes != null && actualSizeBytes != expectedSizeBytes) {
            return "The update download did not finish correctly " +
                "(got $actualSizeBytes of $expectedSizeBytes bytes). " +
                "Nothing was installed. Please try the update again."
        }
        if (expectedSha256 != null &&
            !actualSha256.equals(expectedSha256.trim(), ignoreCase = true)
        ) {
            return "The update download did not match the file the server sent " +
                "(its security fingerprint differs). Nothing was installed. " +
                "Please try the update again."
        }
        return null
    }
}

/**
 * In-app updates (spec §7): at launch and from Settings, ask the Command
 * backend what the newest version is; if it is newer, say so in plain
 * words and offer the download. The APK comes from the backend itself
 * over the device-token channel — no public link, no store.
 */
class UpdateManager(private val api: SentinelApi) {

    suspend fun check(): UpdateState {
        val remote = api.version()
        return if (remote.available &&
            UpdateVerify.isUpdateAvailable(
                remoteVersionCode = remote.versionCode,
                remoteVersion = remote.version,
                currentVersionCode = BuildConfig.VERSION_CODE,
                currentVersion = BuildConfig.VERSION_NAME,
            )
        ) {
            UpdateState.Available(remote)
        } else {
            UpdateState.UpToDate
        }
    }

    /**
     * Downloads the update APK into app-private cache and verifies it
     * against [expected] (the VersionResponse check() returned)
     * before handing it back: the byte count must equal sizeBytes
     * and the SHA-256 of the downloaded bytes must equal sha256,
     * whenever the server stated them. The hash is computed while
     * streaming, so the file is never re-read. On any mismatch the
     * partial/wrong file is deleted and an
     * [UpdateVerificationException] with a plain-language message is
     * thrown — a bad download is never offered to the installer.
     */
    suspend fun download(context: Context, expected: VersionResponse): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val file = File(dir, "sentinel-app-update.apk")
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            try {
                file.outputStream().use { out ->
                    api.downloadUpdate { chunk ->
                        digest.update(chunk)
                        total += chunk.size
                        out.write(chunk)
                    }
                }
            } catch (e: Exception) {
                file.delete()
                throw e
            }
            val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
            val reason = UpdateVerify.mismatchReason(
                actualSizeBytes = total,
                expectedSizeBytes = expected.sizeBytes,
                actualSha256 = actualSha256,
                expectedSha256 = expected.sha256,
            )
            if (reason != null) {
                file.delete()
                throw UpdateVerificationException(reason)
            }
            file
        }

    /** Hands the downloaded APK to the Android installer. */
    fun install(context: Context, apk: File) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            // Android needs the owner's one-time OK for this app to
            // install updates. Send them to the exact settings page.
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settingsIntent)
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
