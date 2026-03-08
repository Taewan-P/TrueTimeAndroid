package dev.chungjungsoo.truetime

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.chungjungsoo.truetime.controller.MainController
import dev.chungjungsoo.truetime.notification.LiveTimeNotificationManager
import dev.chungjungsoo.truetime.ui.TimeScreen
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var liveTimeNotificationManager: LiveTimeNotificationManager

    private val controller: MainController by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                val state = controller.uiState.value
                liveTimeNotificationManager.showLiveTimeNotification(state.currentTime, state.corrected)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        controller.initialize()

        setContent {
            MaterialTheme {
                Surface {
                    val state by controller.uiState.collectAsState()
                    TimeScreen(
                        state = state,
                        onRefresh = controller::refresh,
                    )

                    LaunchedEffect(state.currentTime, state.corrected) {
                        liveTimeNotificationManager.showLiveTimeNotification(
                            timeText = state.currentTime,
                            corrected = state.corrected,
                        )
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
