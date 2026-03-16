package dev.chungjungsoo.truetime

import android.Manifest
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import dagger.hilt.android.AndroidEntryPoint
import dev.chungjungsoo.truetime.controller.MainController
import dev.chungjungsoo.truetime.notification.LiveTimeForegroundService
import dev.chungjungsoo.truetime.notification.LiveTimeNotificationManager
import dev.chungjungsoo.truetime.ui.TimeScreen
import dev.chungjungsoo.truetime.ui.theme.TrueTimeTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var liveTimeNotificationManager: LiveTimeNotificationManager

    private val controller: MainController by viewModels()
    private var inPipMode by mutableStateOf(false)
    private var liveNotificationActive by mutableStateOf(false)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startLiveTimeService()
            } else {
                syncLiveNotificationState()
            }
        }

    private val pipRefreshReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent
            ) {
                if (intent.action == ACTION_PIP_REFRESH) {
                    controller.refresh()
                }
            }
        }

    private val liveNotificationStateReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent
            ) {
                if (intent.action == LiveTimeForegroundService.ACTION_LIVE_NOTIFICATION_STATE_CHANGED) {
                    liveNotificationActive =
                        intent.getBooleanExtra(
                            LiveTimeForegroundService.EXTRA_LIVE_NOTIFICATION_ACTIVE,
                            false
                        )
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        controller.initialize()
        updatePipParams()

        setContent {
            TrueTimeTheme {
                Surface {
                    val state by controller.uiState.collectAsState()
                    TimeScreen(
                        state = state,
                        inPipMode = inPipMode,
                        onRefresh = controller::refresh,
                        liveNotificationActive = liveNotificationActive,
                        onToggleLiveNotification = ::toggleLiveNotification,
                        onEnterPip = ::enterClockPipMode
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        controller.startTicker()
        ContextCompat.registerReceiver(
            this,
            pipRefreshReceiver,
            IntentFilter(ACTION_PIP_REFRESH),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ContextCompat.registerReceiver(
            this,
            liveNotificationStateReceiver,
            IntentFilter(LiveTimeForegroundService.ACTION_LIVE_NOTIFICATION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        syncLiveNotificationState()
    }

    override fun onStop() {
        runCatching { unregisterReceiver(pipRefreshReceiver) }
        runCatching { unregisterReceiver(liveNotificationStateReceiver) }
        controller.stopTicker()
        super.onStop()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPipMode = isInPictureInPictureMode
    }

    private fun toggleLiveNotification() {
        if (liveNotificationActive) {
            stopLiveTimeService()
        } else {
            requestNotificationPermissionIfNeeded()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (liveNotificationActive || liveTimeNotificationManager.isLiveTimeNotificationActive()) {
            liveNotificationActive = true
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            startLiveTimeService()
            return
        }

        val granted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            startLiveTimeService()
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun startLiveTimeService() {
        if (liveNotificationActive || liveTimeNotificationManager.isLiveTimeNotificationActive()) {
            liveNotificationActive = true
            return
        }
        startForegroundService(
            this,
            Intent(this, LiveTimeForegroundService::class.java)
        )
    }

    private fun stopLiveTimeService() {
        stopService(Intent(this, LiveTimeForegroundService::class.java))
        liveTimeNotificationManager.cancelLiveTimeNotification()
        liveNotificationActive = false
    }

    private fun syncLiveNotificationState() {
        liveNotificationActive = liveTimeNotificationManager.isLiveTimeNotificationActive()
    }

    private fun enterClockPipMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !supportsPip()) return
        enterPictureInPictureMode(buildPipParams())
    }

    private fun updatePipParams() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !supportsPip()) return
        setPictureInPictureParams(buildPipParams())
    }

    private fun buildPipParams(): PictureInPictureParams {
        val rootView = window.decorView
        val sourceRectHint = Rect(0, 0, rootView.width.coerceAtLeast(1), rootView.height.coerceAtLeast(1))
        val builder =
            PictureInPictureParams
                .Builder()
                .setAspectRatio(Rational(16, 9))
                .setSourceRectHint(sourceRectHint)
                .setActions(listOf(createRefreshAction()))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(false)
        }
        return builder.build()
    }

    private fun createRefreshAction(): RemoteAction = RemoteAction(
        Icon.createWithResource(this, android.R.drawable.ic_popup_sync),
        getString(R.string.pip_refresh),
        getString(R.string.pip_refresh),
        PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_PIP_REFRESH).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    )

    private fun supportsPip(): Boolean = packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    companion object {
        private const val ACTION_PIP_REFRESH = "dev.chungjungsoo.truetime.action.PIP_REFRESH"
    }
}
