package com.personal.sankalpam

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * The four reminders. "Review" is the first nudge, "last call" is the second one that
 * only fires when something is still unfinished.
 */
enum class ReminderKind(
    val type: TodoType,
    val isFinal: Boolean,
    val prefKey: String,
    val defaultMinutes: Int,
    val title: String,
) {
    WEEKLY_REVIEW(TodoType.WEEKLY, false, "weekly_review", 9 * 60, "Weekly review"),
    WEEKLY_FINAL(TodoType.WEEKLY, true, "weekly_final", 18 * 60, "Weekly last call"),
    MONTHLY_REVIEW(TodoType.MONTHLY, false, "monthly_review", 9 * 60, "Monthly review"),
    MONTHLY_FINAL(TodoType.MONTHLY, true, "monthly_final", 18 * 60, "Monthly last call"),
}

object Prefs {
    private fun sp(ctx: Context) = ctx.getSharedPreferences("sankalpam", Context.MODE_PRIVATE)

    fun time(ctx: Context, kind: ReminderKind): LocalTime {
        val m = sp(ctx).getInt(kind.prefKey, kind.defaultMinutes)
        return LocalTime.of(m / 60, m % 60)
    }

    fun setTime(ctx: Context, kind: ReminderKind, time: LocalTime) {
        sp(ctx).edit().putInt(kind.prefKey, time.hour * 60 + time.minute).apply()
    }

    fun weeklyDay(ctx: Context, kind: ReminderKind): DayOfWeek {
        val def = if (kind == ReminderKind.WEEKLY_REVIEW) DayOfWeek.SATURDAY.value else DayOfWeek.SUNDAY.value
        return DayOfWeek.of(sp(ctx).getInt(kind.prefKey + "_day", def))
    }

    fun setWeeklyDay(ctx: Context, kind: ReminderKind, day: DayOfWeek) {
        sp(ctx).edit().putInt(kind.prefKey + "_day", day.value).apply()
    }

    fun monthlyDaysFromEnd(ctx: Context, kind: ReminderKind): Long {
        val def = if (kind == ReminderKind.MONTHLY_REVIEW) 1L else 0L
        return sp(ctx).getLong(kind.prefKey + "_days_end", def)
    }

    fun setMonthlyDaysFromEnd(ctx: Context, kind: ReminderKind, daysFromEnd: Long) {
        sp(ctx).edit().putLong(kind.prefKey + "_days_end", daysFromEnd).apply()
    }

    fun getTheme(ctx: Context): AppTheme {
        val name = sp(ctx).getString("app_theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        return runCatching { AppTheme.valueOf(name) }.getOrDefault(AppTheme.SYSTEM)
    }

    fun setTheme(ctx: Context, theme: AppTheme) {
        sp(ctx).edit().putString("app_theme", theme.name).apply()
    }

    fun getAccent(ctx: Context): AccentColor {
        val name = sp(ctx).getString("accent_color", AccentColor.BLUE.name) ?: AccentColor.BLUE.name
        return runCatching { AccentColor.valueOf(name) }.getOrDefault(AccentColor.BLUE)
    }

    fun setAccent(ctx: Context, color: AccentColor) {
        sp(ctx).edit().putString("accent_color", color.name).apply()
    }

    // --- Ringtone & Notification Tone Preferences ---

    fun getTimerRingtoneUri(ctx: Context): Uri {
        val uriStr = sp(ctx).getString("timer_ringtone_uri", null)
        return if (!uriStr.isNullOrEmpty()) {
            Uri.parse(uriStr)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: Settings.System.DEFAULT_ALARM_ALERT_URI
        }
    }

    fun getTimerRingtoneTitle(ctx: Context): String {
        val custom = sp(ctx).getString("timer_ringtone_title", null)
        if (!custom.isNullOrEmpty()) return custom
        val uri = getTimerRingtoneUri(ctx)
        return runCatching { RingtoneManager.getRingtone(ctx, uri)?.getTitle(ctx) }.getOrNull() ?: "Default alarm tone"
    }

    fun setTimerRingtone(ctx: Context, uriString: String, title: String) {
        sp(ctx).edit()
            .putString("timer_ringtone_uri", uriString)
            .putString("timer_ringtone_title", title)
            .apply()
    }

    fun getNotificationToneUri(ctx: Context): Uri {
        val uriStr = sp(ctx).getString("notification_tone_uri", null)
        return if (!uriStr.isNullOrEmpty()) {
            Uri.parse(uriStr)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: Settings.System.DEFAULT_NOTIFICATION_URI
        }
    }

    fun getNotificationToneTitle(ctx: Context): String {
        val custom = sp(ctx).getString("notification_tone_title", null)
        if (!custom.isNullOrEmpty()) return custom
        val uri = getNotificationToneUri(ctx)
        return runCatching { RingtoneManager.getRingtone(ctx, uri)?.getTitle(ctx) }.getOrNull() ?: "Default notification tone"
    }

    fun setNotificationTone(ctx: Context, uriString: String, title: String) {
        sp(ctx).edit()
            .putString("notification_tone_uri", uriString)
            .putString("notification_tone_title", title)
            .apply()
    }
}

object Scheduler {
    const val ACTION = "com.personal.sankalpam.REMINDER"
    const val ACTION_GENERAL_REMINDER = "com.personal.sankalpam.GENERAL_REMINDER"
    const val EXTRA_KIND = "kind"
    const val EXTRA_TODO_ID = "todo_id"

    /** The next moment (strictly after [now]) this reminder should fire. */
    fun nextTrigger(ctx: Context, kind: ReminderKind, time: LocalTime, now: LocalDateTime): LocalDateTime = when (kind.type) {
        TodoType.WEEKLY -> nextWeekly(Prefs.weeklyDay(ctx, kind), time, now)
        TodoType.MONTHLY -> nextMonthly(Prefs.monthlyDaysFromEnd(ctx, kind), time, now)
        TodoType.GENERAL -> now
        TodoType.REGULAR -> now
    }

    private fun nextWeekly(day: DayOfWeek, time: LocalTime, now: LocalDateTime): LocalDateTime {
        val date = now.toLocalDate().with(TemporalAdjusters.nextOrSame(day))
        val candidate = LocalDateTime.of(date, time)
        return if (candidate.isAfter(now)) candidate else candidate.plusWeeks(1)
    }

    /** [daysFromEnd] = 0 is the last day of the month, 1 is the day before it. */
    private fun nextMonthly(daysFromEnd: Long, time: LocalTime, now: LocalDateTime): LocalDateTime {
        var month = YearMonth.from(now)
        while (true) {
            val date = month.atEndOfMonth().minusDays(daysFromEnd)
            val candidate = LocalDateTime.of(date, time)
            if (candidate.isAfter(now)) return candidate
            month = month.plusMonths(1)
        }
    }

    fun scheduleAll(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = LocalDateTime.now()
        for (kind in ReminderKind.values()) {
            val at = nextTrigger(ctx, kind, Prefs.time(ctx, kind), now)
            val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val pi = pendingIntent(ctx, kind)
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            try {
                if (canExact) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
                }
            } catch (e: SecurityException) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            }
        }

        // Schedule all upcoming General Reminders and to-dos with reminder times
        val repo = TodoRepository.get(ctx)
        val nowMillis = System.currentTimeMillis()
        repo.todos.value.forEach { todo ->
            if (todo.reminderTime != null && todo.reminderTime > nowMillis && !todo.done) {
                scheduleGeneralReminder(ctx, todo)
            }
        }
    }

    fun scheduleGeneralReminder(ctx: Context, todo: Todo) {
        val reminderTime = todo.reminderTime ?: return
        if (reminderTime <= System.currentTimeMillis() || todo.done) return

        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, ReminderReceiver::class.java).apply {
            action = ACTION_GENERAL_REMINDER
            putExtra(EXTRA_TODO_ID, todo.id)
        }
        val pi = PendingIntent.getBroadcast(
            ctx,
            (30000 + todo.id).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pi)
        }
    }

    fun cancelGeneralReminder(ctx: Context, todoId: Long) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, ReminderReceiver::class.java).apply {
            action = ACTION_GENERAL_REMINDER
        }
        val pi = PendingIntent.getBroadcast(
            ctx,
            (30000 + todoId).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) am.cancel(pi)
    }

    private fun pendingIntent(ctx: Context, kind: ReminderKind): PendingIntent {
        val intent = Intent(ctx, ReminderReceiver::class.java)
            .setAction(ACTION)
            .putExtra(EXTRA_KIND, kind.name)
        return PendingIntent.getBroadcast(
            ctx, kind.ordinal, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

object Notifier {
    fun getChannelId(ctx: Context): String {
        val soundUri = Prefs.getNotificationToneUri(ctx)
        return "sankalpam_reminders_${soundUri.toString().hashCode()}"
    }

    fun ensureChannel(ctx: Context): String {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val channelId = getChannelId(ctx)
        if (nm.getNotificationChannel(channelId) == null) {
            val soundUri = Prefs.getNotificationToneUri(ctx)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            val channel = NotificationChannel(channelId, "To-do & Reminder alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Weekly, monthly, and general reminders"
                setSound(soundUri, audioAttributes)
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }
        return channelId
    }

    /** [force] shows the notification even when there is nothing to say (used by the test button). */
    fun show(ctx: Context, kind: ReminderKind, force: Boolean = false) {
        val channelId = ensureChannel(ctx)
        val repo = TodoRepository.get(ctx)
        repo.rollover()
        val items = repo.cycleItems(kind.type)
        val pending = items.filter { !it.done }
        if (!force && (items.isEmpty() || (kind.isFinal && pending.isEmpty()))) return

        val weekly = kind.type == TodoType.WEEKLY
        val period = if (weekly) "week" else "month"
        val label = if (weekly) "Weekly" else "Monthly"

        val deadline = if (weekly) {
            Prefs.weeklyDay(ctx, ReminderKind.WEEKLY_FINAL).name.lowercase().replaceFirstChar { it.titlecase() }
        } else {
            val daysFromEnd = Prefs.monthlyDaysFromEnd(ctx, ReminderKind.MONTHLY_FINAL)
            if (daysFromEnd == 0L) "the end of the month" else "your deadline"
        }

        val (title, text) = when {
            items.isEmpty() -> "$label review" to "Nothing on your list for this $period yet."
            pending.isEmpty() -> "$label review: all done" to "All ${items.size} to-dos finished. Enjoy the rest of the $period!"
            kind.isFinal -> "Last call: ${pending.size} left this $period" to "Anything unfinished rolls into next $period."
            else -> "$label review: ${pending.size} left" to
                "${items.size - pending.size} of ${items.size} done. Wrap up the rest by $deadline."
        }

        val list = pending.take(10).joinToString("\n") { "• ${it.title}" } +
            if (pending.size > 10) "\n…and ${pending.size - 10} more" else ""

        // Tab 0 is Reminders (with subtab 1 for Weekly, 2 for Monthly)
        val open = PendingIntent.getActivity(
            ctx, 100 + kind.ordinal,
            Intent(ctx, MainActivity::class.java)
                .putExtra("tab", 0)
                .putExtra("reminders_tab", if (weekly) 1 else 2)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = Notification.Builder(ctx, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(if (list.isBlank()) text else "$text\n$list"))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()

        ctx.getSystemService(NotificationManager::class.java).notify(1000 + kind.ordinal, notification)
    }

    fun showGeneralReminder(ctx: Context, todo: Todo) {
        val channelId = ensureChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx,
            (40000 + todo.id).toInt(),
            Intent(ctx, MainActivity::class.java)
                .putExtra("tab", 0)
                .putExtra("reminders_tab", 0) // General reminders tab
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayText = if (todo.details.isNotBlank()) todo.details else "Reminder from Sankalpam"

        val notification = Notification.Builder(ctx, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(todo.title)
            .setContentText(displayText)
            .setStyle(Notification.BigTextStyle().bigText(displayText))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()

        ctx.getSystemService(NotificationManager::class.java).notify((50000 + todo.id).toInt(), notification)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Scheduler.ACTION_GENERAL_REMINDER -> {
                val todoId = intent.getLongExtra(Scheduler.EXTRA_TODO_ID, -1L)
                if (todoId != -1L) {
                    val repo = TodoRepository.get(context)
                    val todo = repo.todos.value.find { it.id == todoId }
                    if (todo != null && !todo.done) {
                        Notifier.showGeneralReminder(context, todo)
                    }
                }
            }
            TimerManager.ACTION_TIMER_EXPIRED -> {
                val label = intent.getStringExtra("timer_label") ?: ""
                TimerNotificationManager.cancelRunningNotification(context)
                TimerSoundManager.startAlarm(context, label)
            }
            TimerSoundManager.ACTION_DISMISS_TIMER -> {
                TimerSoundManager.stopAlarm(context)
            }
            TimerNotificationManager.ACTION_PAUSE_TIMER -> {
                val timerId = intent.getLongExtra("timer_id", -1L)
                if (timerId != -1L) TimerManager.get(context).pauseTimer(timerId)
            }
            TimerNotificationManager.ACTION_RESET_TIMER -> {
                val timerId = intent.getLongExtra("timer_id", -1L)
                if (timerId != -1L) TimerManager.get(context).resetTimer(timerId)
            }
            Scheduler.ACTION -> {
                val kind = runCatching { ReminderKind.valueOf(intent.getStringExtra(Scheduler.EXTRA_KIND) ?: "") }.getOrNull()
                if (kind != null) Notifier.show(context, kind)
                Scheduler.scheduleAll(context)
            }
            else -> {
                Scheduler.scheduleAll(context)
            }
        }
    }
}

/** Re-arms the alarms after reboot, app update, clock change or time-zone change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Scheduler.scheduleAll(context)
    }
}
