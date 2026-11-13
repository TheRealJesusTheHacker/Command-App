package net.sentinelsafeguard.app.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Desktop twin of the shared logic layer's one helper from the
 * Android UI file ui/Common.kt (which is otherwise Compose/Android
 * UI and not shared): identical signature, identical format
 * ("yyyy-MM-dd HH:mm", Locale.US) — the Phase logic files call it
 * unqualified from this package.
 */
fun formatEpochMs(ms: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(ms))
