package net.sentinelsafeguard.app.ui

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * In-app voice-note playback (Lane C, 1.4.0 item 10) — a thin
 * controller over android.media.MediaPlayer (no new dependency).
 * One note plays at a time; starting another stops the first.
 * The audio bytes arrive through the existing authenticated
 * download path (the caller fetches them); the controller
 * writes them to a temp file in cacheDir, plays from there,
 * and deletes the file on stop, on completion, and on dispose
 * — voice notes never accumulate on the device.
 *
 * The pure numbers (poll cadence, progress fraction, clock
 * text) live in [VoicePlayerLogic], pinned by the unit suite.
 */
object VoicePlayerLogic {
    /** Progress polling cadence while a note plays. */
    const val POLL_INTERVAL_MS: Long = 250L

    fun progressFraction(positionMs: Int, durationMs: Int): Float {
        if (durationMs <= 0) return 0f
        return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    /** m:ss clock text for a millisecond position/duration. */
    fun clockText(ms: Int): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }

    /** Temp-file extension from the note's file name — only a
     *  short alphanumeric suffix survives; anything else falls
     *  back to the card's long-standing "wav". */
    fun extensionFor(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "")
            .lowercase()
            .filter { it.isLetterOrDigit() }
            .take(5)
        return ext.ifBlank { "wav" }
    }
}

class VoicePlayerController internal constructor(private val cacheDir: File) {

    /** The note key currently loaded (playing or paused). */
    var activeKey by mutableStateOf<Any?>(null)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var positionMs by mutableStateOf(0)
        private set

    var durationMs by mutableStateOf(0)
        private set

    private var player: MediaPlayer? = null
    private var tempFile: File? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pollJob: Job? = null

    /**
     * Start [key] from [bytes]. Returns false when the player
     * could not be prepared — the caller surfaces the failure
     * in its own words; no partial state is left behind.
     */
    fun play(key: Any, bytes: ByteArray, extension: String): Boolean {
        stopInternal()
        val file = File(cacheDir, "voice-note-playing.$extension")
        return try {
            file.writeBytes(bytes)
            tempFile = file
            val mp = MediaPlayer()
            player = mp
            mp.setDataSource(file.absolutePath)
            mp.setOnCompletionListener { stopInternal() }
            mp.setOnErrorListener { _, _, _ ->
                stopInternal()
                true
            }
            mp.prepare()
            durationMs = mp.duration.coerceAtLeast(0)
            positionMs = 0
            activeKey = key
            mp.start()
            isPlaying = true
            startPolling()
            true
        } catch (e: Exception) {
            stopInternal()
            false
        }
    }

    /** Pause/resume the active note. No-op without one. */
    fun togglePause() {
        val mp = player ?: return
        try {
            if (isPlaying) {
                mp.pause()
                isPlaying = false
                pollJob?.cancel()
                positionMs = mp.currentPosition.coerceAtLeast(0)
            } else {
                mp.start()
                isPlaying = true
                startPolling()
            }
        } catch (e: Exception) {
            stopInternal()
        }
    }

    /** Stop playback and delete the cached temp file. */
    fun stop() = stopInternal()

    /** Release everything; the controller is unusable after. */
    fun release() {
        stopInternal()
        scope.cancel()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (true) {
                delay(VoicePlayerLogic.POLL_INTERVAL_MS)
                val mp = player ?: break
                if (!isPlaying) break
                try {
                    positionMs = mp.currentPosition.coerceAtLeast(0)
                    if (durationMs <= 0) durationMs = mp.duration.coerceAtLeast(0)
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    private fun stopInternal() {
        pollJob?.cancel()
        pollJob = null
        try {
            player?.stop()
        } catch (e: Exception) {
            // Already stopped/idle — release below is the point.
        }
        try {
            player?.release()
        } catch (e: Exception) {
            // Releasing a failed player must never throw onward.
        }
        player = null
        tempFile?.let { file ->
            try {
                file.delete()
            } catch (e: Exception) {
                // A leftover temp file in cacheDir is reclaimed
                // by the platform; nothing else to do.
            }
        }
        tempFile = null
        activeKey = null
        isPlaying = false
        positionMs = 0
        durationMs = 0
    }
}

/** The card-scoped player: remembered once, released on dispose. */
@Composable
fun rememberVoicePlayer(): VoicePlayerController {
    val context = LocalContext.current
    val controller = remember { VoicePlayerController(context.cacheDir) }
    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
    return controller
}
