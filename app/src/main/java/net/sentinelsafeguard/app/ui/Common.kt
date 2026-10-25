package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.sentinelsafeguard.app.ui.theme.SentinelDanger
import net.sentinelsafeguard.app.ui.theme.SentinelMuted
import net.sentinelsafeguard.app.ui.theme.SentinelSurface
import net.sentinelsafeguard.app.ui.theme.SentinelSurfaceHigh

@Composable
fun LoadingBox(message: String = "Loading…") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(message, color = SentinelMuted)
    }
}

/** Every error says what happened and what to do next (spec §8). */
@Composable
fun ErrorBox(message: String, onRetry: (() -> Unit)? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SentinelSurface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Something went wrong", style = MaterialTheme.typography.titleMedium, color = SentinelDanger)
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
            if (onRetry != null) {
                TextButton(onClick = onRetry) { Text("Try again") }
            }
        }
    }
}

@Composable
fun EmptyBox(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(body, color = SentinelMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SectionHeader(title: String, count: Int? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (count != null) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = if (count > 0) MaterialTheme.colorScheme.primary else SentinelMuted,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun ItemRow(
    title: String,
    subtitle: String? = null,
    badge: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SentinelSurface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, color = SentinelMuted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (!badge.isNullOrBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = SentinelSurfaceHigh)) {
                    Text(
                        badge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
