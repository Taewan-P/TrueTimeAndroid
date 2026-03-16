package dev.chungjungsoo.truetime.notification

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import dagger.hilt.android.AndroidEntryPoint
import dev.chungjungsoo.truetime.model.TrustedClockModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LiveTimeForegroundService : Service() {
    @Inject
    lateinit var trustedClockModel: TrustedClockModel

    @Inject
    lateinit var liveTimeNotificationManager: LiveTimeNotificationManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var updateJob: Job? = null
    private var offsetMillis: Long = 0L
    private var corrected: Boolean = false
    private var lastSnapshotRefreshElapsedRealtime: Long = 0L

    override fun onCreate() {
        super.onCreate()
        val notification =
            liveTimeNotificationManager.createLiveTimeNotification(
                corrected = false,
                chipText = "--:--"
            )
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                LiveTimeNotificationManager.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(
                LiveTimeNotificationManager.NOTIFICATION_ID,
                notification
            )
        }
        startUpdates()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int = START_STICKY

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
                refreshSnapshot(force = true)

                while (isActive) {
                    val adjustedMillis = System.currentTimeMillis() + offsetMillis
                    liveTimeNotificationManager.showLiveTimeNotification(
                        corrected = corrected,
                        chipText = formatStatusChip(adjustedMillis)
                    )

                    refreshSnapshot()
                    val delayMillis = MILLIS_PER_SECOND - (adjustedMillis % MILLIS_PER_SECOND)
                    delay(delayMillis.coerceAtLeast(MIN_NOTIFICATION_UPDATE_INTERVAL_MS))
                }
            }
    }

    private suspend fun refreshSnapshot(force: Boolean = false) {
        val refreshElapsedRealtime = SystemClock.elapsedRealtime()
        if (!force && refreshElapsedRealtime - lastSnapshotRefreshElapsedRealtime < SNAPSHOT_REFRESH_INTERVAL_MS) {
            return
        }
        val snapshot = trustedClockModel.refreshSnapshot() ?: return
        offsetMillis = snapshot.offsetMillis
        corrected = snapshot.corrected
        lastSnapshotRefreshElapsedRealtime = refreshElapsedRealtime
    }

    private fun formatStatusChip(epochMillis: Long): String = STATUS_CHIP_FORMATTER.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

    companion object {
        private val STATUS_CHIP_FORMATTER = DateTimeFormatter.ofPattern("mm:ss")
        private const val MILLIS_PER_SECOND = 1_000L
        private const val MIN_NOTIFICATION_UPDATE_INTERVAL_MS = 50L
        private const val SNAPSHOT_REFRESH_INTERVAL_MS = 60_000L
    }
}
