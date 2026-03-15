package dev.chungjungsoo.truetime.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.chungjungsoo.truetime.MainActivity
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
            corrected: Boolean,
            chipText: String,
        ) {
            manager.notify(
                NOTIFICATION_ID,
                createLiveTimeNotification(
                    corrected = corrected,
                    chipText = chipText,
                ),
            )
        }

        fun createLiveTimeNotification(
            corrected: Boolean,
            chipText: String,
        ): Notification {
            ensureChannel()
            val contentIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val deleteIntent =
                PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent(context, LiveTimeNotificationDismissReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val statusTextResId =
                if (corrected) {
                    R.string.live_time_notification_corrected
                } else {
                    R.string.live_time_notification_uncorrected
                }

            if (Build.VERSION.SDK_INT >= 36) {
                val builder =
                    Notification
                        .Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(context.getString(R.string.live_time_notification_title))
                        .setContentText(context.getString(R.string.live_time_notification_content))
                        .setSubText(context.getString(statusTextResId))
                        .setContentIntent(contentIntent)
                        .setDeleteIntent(deleteIntent)
                        .setCategory(Notification.CATEGORY_STATUS)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setOnlyAlertOnce(true)
                        .setOngoing(true)
                        .setShowWhen(false)
                        .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
                        .setShortCriticalText(chipText)

                requestPromotedOngoing(builder)
                return builder.build()
            }

            return NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.live_time_notification_title))
                .setContentText(context.getString(R.string.live_time_notification_content))
                .setSubText(context.getString(statusTextResId))
                .setContentIntent(contentIntent)
                .setDeleteIntent(deleteIntent)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setShowWhen(false)
                .setSilent(true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build()
        }

        private fun ensureChannel() {
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.live_time_notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }

        private fun requestPromotedOngoing(builder: Notification.Builder) {
            runCatching {
                builder::class.java
                    .getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                    .invoke(builder, true)
            }
        }

        companion object {
            const val CHANNEL_ID = "truetime_live_channel"
            const val NOTIFICATION_ID = 10_001
        }
    }
