package com.personal.sankalpam

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class TimerManager private constructor(private val appContext: Context) {

    private val file = File(appContext.filesDir, "timers.json")
    private val io = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _timers = MutableStateFlow(load())
    val timers: StateFlow<List<TimerItem>> = _timers.asStateFlow()

    private var tickerJob: Job? = null

    init {
        // Clean up any unpinned completed timers older than 12 hours
        cleanupExpiredCompletedTimers()

        // Recalculate any timers that were running when app was closed
        val now = System.currentTimeMillis()
        var anyFinished = false
        val restored = _timers.value.map { timer ->
            if (timer.isRunning) {
                val remaining = maxOf(0L, (timer.endTimestamp - now + 999) / 1000)
                if (remaining == 0L) {
                    anyFinished = true
                    timer.copy(
                        remainingSeconds = 0,
                        isRunning = false,
                        isFinished = true,
                        finishedAt = timer.finishedAt ?: now
                    )
                } else {
                    timer.copy(remainingSeconds = remaining)
                }
            } else timer
        }
        _timers.value = restored
        if (anyFinished) {
            TimerSoundManager.startAlarm(appContext, "")
        }
        startTickerIfNeeded()
        TimerNotificationManager.updateRunningNotification(appContext, restored)
    }

    /**
     * Deletes finished or dismissed timers older than 24 hours, unless pinned.
     */
    fun cleanupExpiredCompletedTimers() {
        val now = System.currentTimeMillis()
        val twentyFourHours = 24 * 60 * 60 * 1000L
        val filtered = _timers.value.filterNot { timer ->
            !timer.isRunning && !timer.isPinned && timer.finishedAt != null && (now - timer.finishedAt >= twentyFourHours)
        }
        if (filtered.size != _timers.value.size) {
            _timers.value = filtered
            commit(filtered)
        }
    }

    private fun startTickerIfNeeded() {
        val hasRunning = _timers.value.any { it.isRunning }
        if (!hasRunning) {
            tickerJob?.cancel()
            tickerJob = null
            TimerNotificationManager.updateRunningNotification(appContext, _timers.value)
            return
        }
        if (tickerJob?.isActive == true) return

        tickerJob = scope.launch {
            var tickCount = 0
            while (isActive) {
                delay(250)
                tickCount++
                val now = System.currentTimeMillis()
                var updated = false
                var anyFinishedNow = false
                val next = _timers.value.map { timer ->
                    if (timer.isRunning) {
                        val remaining = maxOf(0L, (timer.endTimestamp - now + 999) / 1000)
                        if (remaining == 0L && !timer.isFinished) {
                            updated = true
                            anyFinishedNow = true
                            TimerSoundManager.startAlarm(appContext, timer.label)
                            timer.copy(
                                remainingSeconds = 0,
                                isRunning = false,
                                isFinished = true,
                                finishedAt = now
                            )
                        } else if (remaining != timer.remainingSeconds) {
                            updated = true
                            timer.copy(remainingSeconds = remaining)
                        } else timer
                    } else timer
                }

                // Periodic 12-hr cleanup check every 60 seconds (240 ticks of 250ms)
                if (tickCount % 240 == 0) {
                    cleanupExpiredCompletedTimers()
                }

                if (updated) {
                    _timers.value = next
                    commit(next)
                    if (anyFinishedNow) {
                        TimerNotificationManager.updateRunningNotification(appContext, next)
                    }
                    if (!next.any { it.isRunning }) {
                        tickerJob?.cancel()
                        tickerJob = null
                        break
                    }
                }
            }
        }
    }

    fun addTimer(hours: Int, minutes: Int, seconds: Int, label: String, autoStart: Boolean = false): TimerItem {
        val totalSecs = (hours * 3600L) + (minutes * 60L) + seconds.toLong()
        if (totalSecs <= 0) return _timers.value.firstOrNull() ?: TimerItem(1, "", 60, 60)

        val id = (_timers.value.maxOfOrNull { it.id } ?: 0L) + 1
        val end = if (autoStart) System.currentTimeMillis() + (totalSecs * 1000L) else 0L
        val item = TimerItem(
            id = id,
            label = label.trim(),
            totalDurationSeconds = totalSecs,
            remainingSeconds = totalSecs,
            isRunning = autoStart,
            isFinished = false,
            endTimestamp = end,
            finishedAt = null,
            isPinned = false,
        )
        val next = _timers.value + item
        _timers.value = next
        commit(next)

        if (autoStart) {
            scheduleExactAlarm(item)
            startTickerIfNeeded()
            TimerNotificationManager.updateRunningNotification(appContext, next)
        }
        return item
    }

    fun startTimer(id: Long) {
        val now = System.currentTimeMillis()
        val next = _timers.value.map {
            if (it.id == id && !it.isRunning) {
                val secsLeft = if (it.remainingSeconds <= 0L) it.totalDurationSeconds else it.remainingSeconds
                val end = now + (secsLeft * 1000L)
                val updated = it.copy(
                    remainingSeconds = secsLeft,
                    isRunning = true,
                    isFinished = false,
                    endTimestamp = end,
                    finishedAt = null
                )
                scheduleExactAlarm(updated)
                updated
            } else it
        }
        _timers.value = next
        commit(next)
        startTickerIfNeeded()
        TimerNotificationManager.updateRunningNotification(appContext, next)
    }

    fun pauseTimer(id: Long) {
        val now = System.currentTimeMillis()
        val next = _timers.value.map {
            if (it.id == id && it.isRunning) {
                cancelExactAlarm(it.id)
                val remaining = maxOf(0L, (it.endTimestamp - now + 999) / 1000)
                it.copy(remainingSeconds = remaining, isRunning = false)
            } else it
        }
        _timers.value = next
        commit(next)
        startTickerIfNeeded()
        TimerNotificationManager.updateRunningNotification(appContext, next)
    }

    fun resetTimer(id: Long) {
        cancelExactAlarm(id)
        val next = _timers.value.map {
            if (it.id == id) {
                it.copy(
                    remainingSeconds = it.totalDurationSeconds,
                    isRunning = false,
                    isFinished = false,
                    endTimestamp = 0L,
                    finishedAt = null
                )
            } else it
        }
        _timers.value = next
        commit(next)
        startTickerIfNeeded()
        TimerNotificationManager.updateRunningNotification(appContext, next)
    }

    fun dismissFinishedTimer(id: Long) {
        TimerSoundManager.stopAlarm(appContext)
        val next = _timers.value.map {
            if (it.id == id) {
                it.copy(
                    remainingSeconds = it.totalDurationSeconds,
                    isRunning = false,
                    isFinished = false,
                    endTimestamp = 0L,
                    finishedAt = it.finishedAt ?: System.currentTimeMillis()
                )
            } else it
        }
        _timers.value = next
        commit(next)
        TimerNotificationManager.updateRunningNotification(appContext, next)
    }

    fun togglePinTimer(id: Long) {
        val next = _timers.value.map {
            if (it.id == id) it.copy(isPinned = !it.isPinned) else it
        }
        _timers.value = next
        commit(next)
    }

    fun deleteTimer(id: Long) {
        cancelExactAlarm(id)
        // Ensure ringing alarm is stopped when deleting a timer
        TimerSoundManager.stopAlarm(appContext)
        val next = _timers.value.filter { it.id != id }
        _timers.value = next
        commit(next)
        startTickerIfNeeded()
        TimerNotificationManager.updateRunningNotification(appContext, next)
    }

    private fun scheduleExactAlarm(timer: TimerItem) {
        val am = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(appContext, ReminderReceiver::class.java).apply {
            action = ACTION_TIMER_EXPIRED
            putExtra("timer_id", timer.id)
            putExtra("timer_label", timer.label)
        }
        val pi = PendingIntent.getBroadcast(
            appContext,
            (20000 + timer.id).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.endTimestamp, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.endTimestamp, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.endTimestamp, pi)
        }
    }

    private fun cancelExactAlarm(id: Long) {
        val am = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(appContext, ReminderReceiver::class.java).apply {
            action = ACTION_TIMER_EXPIRED
        }
        val pi = PendingIntent.getBroadcast(
            appContext,
            (20000 + id).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) am.cancel(pi)
    }

    private fun commit(list: List<TimerItem>) {
        val json = JSONArray().apply { list.forEach { put(it.toJson()) } }.toString()
        io.execute {
            runCatching {
                val tmp = File(file.parentFile, "timers.json.tmp")
                tmp.writeText(json)
                if (!tmp.renameTo(file)) file.writeText(json)
            }
        }
    }

    private fun load(): List<TimerItem> = runCatching {
        if (!file.exists()) {
            emptyList<TimerItem>()
        } else {
            val arr = JSONArray(file.readText())
            List(arr.length()) { arr.getJSONObject(it).toTimerItem() }
        }
    }.getOrDefault(emptyList())

    companion object {
        const val ACTION_TIMER_EXPIRED = "com.personal.sankalpam.TIMER_EXPIRED"

        @Volatile private var instance: TimerManager? = null

        fun get(context: Context): TimerManager =
            instance ?: synchronized(this) {
                instance ?: TimerManager(context.applicationContext).also { instance = it }
            }
    }
}

private fun TimerItem.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("label", label)
    put("totalDurationSeconds", totalDurationSeconds)
    put("remainingSeconds", remainingSeconds)
    put("isRunning", isRunning)
    put("isFinished", isFinished)
    put("endTimestamp", endTimestamp)
    put("finishedAt", finishedAt ?: JSONObject.NULL)
    put("isPinned", isPinned)
}

private fun JSONObject.toTimerItem(): TimerItem = TimerItem(
    id = getLong("id"),
    label = optString("label", ""),
    totalDurationSeconds = getLong("totalDurationSeconds"),
    remainingSeconds = getLong("remainingSeconds"),
    isRunning = optBoolean("isRunning", false),
    isFinished = optBoolean("isFinished", false),
    endTimestamp = optLong("endTimestamp", 0L),
    finishedAt = if (isNull("finishedAt")) null else getLong("finishedAt"),
    isPinned = optBoolean("isPinned", false),
)
