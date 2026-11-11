package com.personal.sankalpam

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object TimerSoundManager {
    private const val CHANNEL_TIMER_ID = "sankalpam_timer_alerts"
    private const val NOTIFICATION_ID = 9001
    const val ACTION_DISMISS_TIMER = "com.personal.sankalpam.DISMISS_TIMER"

    private var activeRingtone: Ringtone? = null
    private var isRinging = false

    fun startAlarm(ctx: Context, timerLabel: String) {
        if (isRinging) return
        isRinging = true

        val uri = Prefs.getTimerRingtoneUri(ctx)
        runCatching {
            val ringtone = RingtoneManager.getRingtone(ctx, uri)
            ringtone?.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone?.isLooping = true
            }
            ringtone?.play()
            activeRingtone = ringtone
        }

        // Vibrate
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            val pattern = longArrayOf(0, 600, 400, 600, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }

        // Show High Priority Alert Notification
        showTimerFinishedNotification(ctx, timerLabel)
    }

    fun stopAlarm(ctx: Context) {
        isRinging = false
        runCatching {
            activeRingtone?.stop()
            activeRingtone = null
        }
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.cancel()
        }
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.cancel(NOTIFICATION_ID)
    }

    private fun showTimerFinishedNotification(ctx: Context, label: String) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_TIMER_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_TIMER_ID,
                "Timer alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when timers finish"
                setSound(null, null) // Sound is handled by RingtoneManager loop
                enableVibration(false) // Vibration handled explicitly
            }
            nm.createNotificationChannel(channel)
        }

        val dismissIntent = Intent(ctx, ReminderReceiver::class.java).apply {
            action = ACTION_DISMISS_TIMER
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            ctx,
            9002,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(ctx, MainActivity::class.java).apply {
            putExtra("tab", 2) // Timers tab
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            ctx,
            9003,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayText = if (label.isNotBlank()) "Timer \"$label\" has finished!" else "Timer has finished!"

        val notification = Notification.Builder(ctx, CHANNEL_TIMER_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Timer Finished")
            .setContentText(displayText)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(
                    null,
                    "Stop",
                    dismissPendingIntent
                ).build()
            )
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }
}
