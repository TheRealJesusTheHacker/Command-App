package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.bookingAdmin
import net.sentinelsafeguard.app.data.api.deleteBookingAvailability
import net.sentinelsafeguard.app.data.api.deleteBookingService
import net.sentinelsafeguard.app.data.api.manageBooking
import net.sentinelsafeguard.app.data.api.saveBookingAvailability
import net.sentinelsafeguard.app.data.api.saveBookingService
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Schedule booking admin (Phase 2 remainder, on the Phase 3
 * backend): web's booking-admin panels — weekly availability,
 * services, and bookings manage (cancel / reschedule) — rendered
 * inside the Schedule view where web puts them.
 */
@Composable
fun BookingAdminPanel(viewModel: AppViewModel) {
    val scope = rememberCoroutineScope()
    var weekday by remember { mutableStateOf(0) }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("17:00") }
    var serviceName by remember { mutableStateOf("") }
    var serviceMinutes by remember { mutableStateOf("60") }
    var rescheduleDate by remember { mutableStateOf("") }
    var managingId by remember { mutableStateOf<Int?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    AsyncCard("Booking admin", "admin", loader = { viewModel.api.bookingAdmin() }) { data, reload ->
        val warnings = Phase3Logic.arrRaw(data, "staffAvailabilityWarnings")
            .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
        warnings.forEach { warning ->
            Text(warning, color = MaterialTheme.colorScheme.error)
        }

        Text("Availability", style = MaterialTheme.typography.titleSmall)
        val availability = Phase3Logic.arr(data, "availability")
        if (availability.isEmpty()) Text("No availability set.", color = SentinelMuted)
        availability.forEach { slot ->
            val sid = Phase3Logic.int(slot, "id")
            Row {
                Text(
                    "${Phase3Logic.str(slot, "weekdayLabel")} · " +
                        "${Phase3Logic.str(slot, "startTime")}–${Phase3Logic.str(slot, "endTime")}" +
                        " · ${Phase3Logic.str(slot, "staffName")}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (sid != null) {
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                viewModel.api.deleteBookingAvailability(sid); reload()
                            } catch (e: Exception) {
                                error = plainError(e, "The slot could not be removed. Try again.")
                            }
                        }
                    }) { Text("Delete") }
                }
            }
        }
        ChoiceMenu("Weekday", Phase3Logic.weekdayLabel(weekday), Phase3Logic.WEEKDAYS) { picked ->
            weekday = Phase3Logic.WEEKDAYS.indexOf(picked).coerceAtLeast(0)
        }
        TextFieldRow("Start (HH:MM)", startTime, { startTime = it })
        TextFieldRow("End (HH:MM)", endTime, { endTime = it })
        Button(
            enabled = startTime.isNotBlank() && endTime.isNotBlank(),
            onClick = {
                scope.launch {
                    try {
                        viewModel.api.saveBookingAvailability(
                            buildJsonObject {
                                put("weekday", weekday)
                                put("startTime", startTime)
                                put("endTime", endTime)
                                put("active", true)
                            },
                            null,
                        )
                        reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The availability could not be saved. Try again.")
                    }
                }
            },
        ) { Text("Add availability") }

        Spacer(Modifier.height(12.dp))
        Text("Services", style = MaterialTheme.typography.titleSmall)
        val services = Phase3Logic.arr(data, "services")
        services.forEach { service ->
            val sid = Phase3Logic.int(service, "id")
            Row {
                Text(
                    "${Phase3Logic.str(service, "name")} · ${Phase3Logic.str(service, "durationMinutes")} min" +
                        if (Phase3Logic.bool(service, "active") == false) " (inactive)" else "",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (sid != null) {
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                viewModel.api.deleteBookingService(sid); reload()
                            } catch (e: Exception) {
                                error = plainError(e, "The service could not be removed. Try again.")
                            }
                        }
                    }) { Text("Delete") }
                }
            }
        }
        TextFieldRow("Service name", serviceName, { serviceName = it })
        TextFieldRow("Duration (minutes)", serviceMinutes, { serviceMinutes = it })
        Button(
            enabled = serviceName.isNotBlank(),
            onClick = {
                scope.launch {
                    try {
                        viewModel.api.saveBookingService(
                            buildJsonObject {
                                put("name", serviceName)
                                put("durationMinutes", serviceMinutes.toIntOrNull() ?: 60)
                                put("active", true)
                            },
                            null,
                        )
                        serviceName = ""; reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The service could not be saved. Try again.")
                    }
                }
            },
        ) { Text("Add service") }

        Spacer(Modifier.height(12.dp))
        Text("Bookings", style = MaterialTheme.typography.titleSmall)
        val bookings = Phase3Logic.arr(data, "bookings")
        if (bookings.isEmpty()) Text("No bookings.", color = SentinelMuted)
        bookings.forEach { booking ->
            val bid = Phase3Logic.int(booking, "id")
            Text(
                "${Phase3Logic.str(booking, "name")} · ${Phase3Logic.str(booking, "serviceName")} · " +
                    "${Phase3Logic.str(booking, "date")} ${Phase3Logic.str(booking, "startTime")} · " +
                    Phase3Logic.str(booking, "status"),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (bid != null) {
                Row {
                    TextButton(onClick = { managingId = bid }) { Text("Manage") }
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                val out = viewModel.api.manageBooking(bid, "cancel", null)
                                notice = Phase3Logic.str(out, "message"); reload()
                            } catch (e: Exception) {
                                error = plainError(e, "The booking could not be cancelled. Try again.")
                            }
                        }
                    }) { Text("Cancel booking") }
                }
            }
        }
        if (managingId != null) {
            TextFieldRow("Reschedule to (yyyy-MM-dd)", rescheduleDate, { rescheduleDate = it })
            Button(
                enabled = rescheduleDate.isNotBlank(),
                onClick = {
                    val bid = managingId ?: return@Button
                    scope.launch {
                        try {
                            val out = viewModel.api.manageBooking(
                                bid, "reschedule", rescheduleDate,
                            )
                            notice = Phase3Logic.str(out, "message")
                            managingId = null; rescheduleDate = ""; reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The booking could not be rescheduled. Try again.")
                        }
                    }
                },
            ) { Text("Reschedule") }
        }
        if (notice != null) Text(notice!!, color = SentinelMuted)
        ActionErrorText(error)
    }
}
