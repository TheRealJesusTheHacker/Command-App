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
     * match the server's stated facts, or null when they match.
     *
     * AUDIT-171 H-01 (fail CLOSED): the SHA-256 is the integrity
     * anchor and is REQUIRED. The production version endpoint always
     * computes and sends it for a staged update (verified against
     * the server source), so a response without one is not a
     * legitimate update offer — previously this returned null
     * ("nothing to check against") and ANY bytes would have been
     * handed to the installer. A stated size is still checked when
     * present; the hash alone decides when it is absent.
     */
    fun mismatchReason(
        actualSizeBytes: Long,
        expectedSizeBytes: Long?,
        actualSha256: String,
        expectedSha256: String?,
    ): String? {
        if (expectedSha256.isNullOrBlank()) {
            return "The server did not provide this update's security " +
                "fingerprint, so the download cannot be verified. " +
                "Nothing was installed. Please try the update again later."
        }
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

    /**
     * True when BOTH signer-certificate digest sets are present and
     * identical (AUDIT-171 M-02). The app ships under one custody
     * signing key, so an update signed by anything else — or whose
     * signers cannot be read at all — is not this app's update, no
     * matter what its bytes hash to.
     */
    fun signerMatches(
        installedCertDigests: Set<String>,
        archiveCertDigests: Set<String>,
    ): Boolean =
        installedCertDigests.isNotEmpty() &&
            archiveCertDigests.isNotEmpty() &&
            installedCertDigests == archiveCertDigests
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
     * before handing it back: the SHA-256 of the downloaded bytes
     * must equal the server's stated sha256 (required — see
     * [mismatchReason]), the byte count must equal sizeBytes when
     * stated, and the APK must be signed by the same key as the
     * installed app. The hash is computed while streaming, so the
     * file is never re-read for it. On any mismatch the
     * partial/wrong file is deleted and an
     * [UpdateVerificationException] with a plain-language message is
     * thrown — a bad download is never offered to the installer.
     */
    suspend fun download(context: Context, expected: VersionResponse): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            // L-07: prune stale update downloads from the updates
            // cache dir so an old APK does not linger indefinitely.
            val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            dir.listFiles()?.forEach { stale ->
                if (stale.isFile && stale.lastModified() < cutoff) stale.delete()
            }
            val file = File(dir, "command-app-update.apk")
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
            // M-02: the hash proves the bytes are what the server
            // sent; the signer check proves they are OURS. Android
            // would refuse a differently-signed update at install
            // time anyway — refusing here gives the owner a clear
            // message instead of a system error, and never offers a
            // foreign APK to the installer.
            if (!signerMatchesInstalled(context, file)) {
                file.delete()
                throw UpdateVerificationException(
                    "The update was not signed with this app's key, so " +
                        "it cannot be verified as a genuine Command App " +
                        "update. Nothing was installed."
                )
            }
            file
        }

    /** SHA-256 digests of the signing certificates on [info]. */
    @Suppress("DEPRECATION")
    private fun signerDigests(info: android.content.pm.PackageInfo): Set<String> {
        val signatures = if (android.os.Build.VERSION.SDK_INT >= 28) {
            info.signingInfo?.apkContentsSigners?.toList()
        } else {
            info.signatures?.toList()
        } ?: return emptySet()
        return signatures.map { UpdateVerify.sha256Hex(it.toByteArray()) }.toSet()
    }

    private fun signerMatchesInstalled(context: Context, apk: File): Boolean {
        val pm = context.packageManager
        val flags = if (android.os.Build.VERSION.SDK_INT >= 28) {
            android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            android.content.pm.PackageManager.GET_SIGNATURES
        }
        val installed = try {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, flags)
        } catch (e: Exception) {
            null
        } ?: return false
        val archive = try {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(apk.absolutePath, flags)
        } catch (e: Exception) {
            null
        } ?: return false
        return UpdateVerify.signerMatches(signerDigests(installed), signerDigests(archive))
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
