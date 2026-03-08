package dev.chungjungsoo.truetime.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.chungjungsoo.truetime.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveTimeNotificationManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val manager = context.getSystemService(NotificationManager::class.java)

        fun showLiveTimeNotification(
            timeText: String,
            corrected: Boolean,
            secondInMinute: Int,
        ) {
            ensureChannel()
            val suffix = if (corrected) " (NTP corrected)" else ""
            val notification =
                if (Build.VERSION.SDK_INT >= 36) {
                    Notification
                        .Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("TrueTime Live")
                        .setContentText("$timeText$suffix")
                        .setSubText("Live Update")
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setStyle(
                            Notification
                                .ProgressStyle()
                                .setStyledByProgress(false)
                                .setProgressSegments(listOf(Notification.ProgressStyle.Segment(60)))
                                .setProgress(secondInMinute),
                        ).build()
                } else {
                    NotificationCompat
                        .Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("TrueTime Live")
                        .setContentText("$timeText$suffix")
                        .setSubText("Live Update")
                        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setProgress(60, secondInMinute, false)
                        .build()
                }
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
