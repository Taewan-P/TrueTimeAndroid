package dev.chungjungsoo.truetime.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LiveTimeNotificationDismissReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        context.stopService(Intent(context, LiveTimeForegroundService::class.java))
    }
}
