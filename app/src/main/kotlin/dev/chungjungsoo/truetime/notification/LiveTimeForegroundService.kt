package dev.chungjungsoo.truetime.notification

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import dev.chungjungsoo.truetime.model.TrustedClockModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class LiveTimeForegroundService : Service() {
    @Inject
    lateinit var trustedClockModel: TrustedClockModel

    @Inject
    lateinit var liveTimeNotificationManager: LiveTimeNotificationManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var updateJob: Job? = null
    private var offsetMillis: Long = 0L
    private var estimatedErrorMillis: Long = 0L
    private var corrected: Boolean = false

    override fun onCreate() {
        super.onCreate()
        startForeground(
            LiveTimeNotificationManager.NOTIFICATION_ID,
            liveTimeNotificationManager.createLiveTimeNotification(
                timeText = "--:--:--.---",
                corrected = false,
                secondInMinute = 0,
            ),
        )
        startUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        updateJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startUpdates() {
        updateJob?.cancel()
        updateJob =
            serviceScope.launch {
                runCatching { trustedClockModel.initialize() }
                refreshSnapshot()

                while (isActive) {
                    val adjustedMillis = System.currentTimeMillis() + offsetMillis - estimatedErrorMillis
                    val secondInMinute = ((adjustedMillis / 1000L) % 60L).toInt()
                    liveTimeNotificationManager.showLiveTimeNotification(
                        timeText = formatLiveTime(adjustedMillis),
                        corrected = corrected,
                        secondInMinute = secondInMinute,
                    )

                    if (secondInMinute == 0) {
                        refreshSnapshot()
                    }
                    delay(100L)
                }
            }
    }

    private fun refreshSnapshot() {
        val snapshot = trustedClockModel.refreshSnapshot() ?: return
        offsetMillis = snapshot.offsetMillis
        estimatedErrorMillis = snapshot.estimatedErrorMillis
        corrected = snapshot.corrected
    }

    private fun formatLiveTime(epochMillis: Long): String =
        LIVE_TIME_FORMATTER.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

    companion object {
        private val LIVE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    }
}
