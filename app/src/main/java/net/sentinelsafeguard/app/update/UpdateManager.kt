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

sealed interface UpdateState {
    data object UpToDate : UpdateState
    data class Available(val info: VersionResponse) : UpdateState
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
        val staged = remote.version
        return if (remote.available && staged != null &&
            VersionCompare.isNewer(staged, BuildConfig.VERSION_NAME)
        ) {
            UpdateState.Available(remote)
        } else {
            UpdateState.UpToDate
        }
    }

    /** Downloads the update APK into app-private cache. Returns the file. */
    suspend fun download(context: Context): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val file = File(dir, "sentinel-app-update.apk")
        file.outputStream().use { out ->
            api.downloadUpdate { chunk -> out.write(chunk) }
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
