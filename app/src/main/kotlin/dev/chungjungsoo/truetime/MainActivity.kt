package dev.chungjungsoo.truetime

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import dagger.hilt.android.AndroidEntryPoint
import dev.chungjungsoo.truetime.controller.MainController
import dev.chungjungsoo.truetime.notification.LiveTimeForegroundService
import dev.chungjungsoo.truetime.ui.TimeScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val controller: MainController by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startLiveTimeService()
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
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            startLiveTimeService()
            return
        }

        val granted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            startLiveTimeService()
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun startLiveTimeService() {
        startForegroundService(
            this,
            Intent(this, LiveTimeForegroundService::class.java),
        )
    }
}
