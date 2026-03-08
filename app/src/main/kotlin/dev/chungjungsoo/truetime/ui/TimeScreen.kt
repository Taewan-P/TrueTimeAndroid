package dev.chungjungsoo.truetime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.chungjungsoo.truetime.controller.MainController

@Composable
fun TimeScreen(
    state: MainController.UiState,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = state.currentTime, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(text = "Last updated: ${state.lastUpdated}")
        Text(text = "Offset: ${state.offsetMillis} ms")
        Text(text = "Estimated Error: ${state.estimatedErrorMillis} ms")
        Text(text = "NTP drift: ${state.driftMillis} ms")
        Text(text = if (state.corrected) "Source: corrected with NTP" else "Source: TrustedTime")
        Button(onClick = onRefresh, modifier = Modifier.padding(top = 16.dp)) {
            Text("Refresh")
        }
    }
}
