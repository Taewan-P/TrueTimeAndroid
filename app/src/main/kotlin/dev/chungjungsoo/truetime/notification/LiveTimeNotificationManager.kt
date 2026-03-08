package dev.chungjungsoo.truetime.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.chungjungsoo.truetime.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveTimeNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun showLiveTimeNotification(timeText: String, corrected: Boolean) {
        ensureChannel()
        val suffix = if (corrected) " (NTP corrected)" else ""
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("TrueTime Live")
            .setContentText("$timeText$suffix")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "TrueTime Live Updates", NotificationManager.IMPORTANCE_LOW),
        )
    }

    companion object {
        private const val CHANNEL_ID = "truetime_live_channel"
        private const val NOTIFICATION_ID = 10_001
    }
}
