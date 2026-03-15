package dev.chungjungsoo.truetime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.chungjungsoo.truetime.R
import dev.chungjungsoo.truetime.controller.MainController

@Suppress("FunctionName")
@Composable
fun TimeScreen(
    state: MainController.UiState,
    inPipMode: Boolean,
    onRefresh: () -> Unit,
    onActivateLiveNotification: () -> Unit,
    onEnterPip: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(if (inPipMode) 12.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = state.currentTime,
            style = if (inPipMode) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
        )
        if (inPipMode) {
            Text(
                text = if (state.corrected) "NTP corrected" else "TrustedTime",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
            )
            return
        }
        Text(text = "Second: ${state.secondInMinute}")
        LinearProgressIndicator(
            progress = { state.minuteProgress },
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
        )
        Text(text = "Minute progress: ${(state.minuteProgress * 100).toInt()}%")
        Text(text = "Last updated: ${state.lastUpdated}", modifier = Modifier.padding(top = 16.dp))
        Text(text = "Offset: ${state.offsetMillis} ms")
        Text(text = "Estimated Error: ${state.estimatedErrorMillis} ms")
        Text(text = "NTP drift: ${state.driftMillis} ms")
        Text(text = if (state.corrected) "Source: corrected with NTP" else "Source: TrustedTime")
        Button(onClick = onActivateLiveNotification, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.activate_live_notification))
        }
        Button(onClick = onEnterPip, modifier = Modifier.padding(top = 16.dp)) {
            Text("Enter PiP")
        }
        Button(onClick = onRefresh, modifier = Modifier.padding(top = 16.dp)) {
            Text("Refresh")
        }
    }
}
