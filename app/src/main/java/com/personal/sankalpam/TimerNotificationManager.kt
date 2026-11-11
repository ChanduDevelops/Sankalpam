package com.personal.sankalpam

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object TimerNotificationManager {
    const val CHANNEL_RUNNING_TIMER = "sankalpam_running_timer"
    const val RUNNING_NOTIFICATION_ID = 9005

    const val ACTION_PAUSE_TIMER = "com.personal.sankalpam.PAUSE_TIMER"
    const val ACTION_RESUME_TIMER = "com.personal.sankalpam.RESUME_TIMER"
    const val ACTION_RESET_TIMER = "com.personal.sankalpam.RESET_TIMER"

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_RUNNING_TIMER) == null) {
            val channel = NotificationChannel(
                CHANNEL_RUNNING_TIMER,
                "Active Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live countdown for running timers"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun updateRunningNotification(ctx: Context, timers: List<TimerItem>) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val runningTimer = timers.filter { it.isRunning }.minByOrNull { it.remainingSeconds }

        if (runningTimer == null) {
            nm.cancel(RUNNING_NOTIFICATION_ID)
            return
        }

        ensureChannel(ctx)

        val openAppIntent = Intent(ctx, MainActivity::class.java).apply {
            putExtra("tab", 2) // Timers tab
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            ctx,
            9006,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(ctx, ReminderReceiver::class.java).apply {
            action = ACTION_PAUSE_TIMER
            putExtra("timer_id", runningTimer.id)
        }
        val pausePendingIntent = PendingIntent.getBroadcast(
            ctx,
            (9010 + runningTimer.id).toInt(),
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resetIntent = Intent(ctx, ReminderReceiver::class.java).apply {
            action = ACTION_RESET_TIMER
            putExtra("timer_id", runningTimer.id)
        }
        val resetPendingIntent = PendingIntent.getBroadcast(
            ctx,
            (9020 + runningTimer.id).toInt(),
            resetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = runningTimer.label.ifBlank { "Timer" }
        val remainingText = formatDurationSeconds(runningTimer.remainingSeconds)

        val builder = Notification.Builder(ctx, CHANNEL_RUNNING_TIMER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("Remaining: $remainingText")
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(runningTimer.endTimestamp)
            .setUsesChronometer(true)
            .setCategory(Notification.CATEGORY_STOPWATCH)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setChronometerCountDown(true)
        }

        builder.addAction(
            Notification.Action.Builder(
                null,
                "Pause",
                pausePendingIntent
            ).build()
        )
        builder.addAction(
            Notification.Action.Builder(
                null,
                "Reset",
                resetPendingIntent
            ).build()
        )

        nm.notify(RUNNING_NOTIFICATION_ID, builder.build())
    }

    fun cancelRunningNotification(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.cancel(RUNNING_NOTIFICATION_ID)
    }
}
