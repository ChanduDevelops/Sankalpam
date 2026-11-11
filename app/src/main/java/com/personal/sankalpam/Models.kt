package com.personal.sankalpam

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class TodoType { GENERAL, WEEKLY, MONTHLY, REGULAR }

enum class AppTheme { SYSTEM, LIGHT, DARK, AMOLED }

enum class AccentColor(val colorValue: Long, val label: String) {
    BLUE(0xFF005AC1, "Blue"),
    PURPLE(0xFF6750A4, "Purple"),
    RED(0xFFB3261E, "Red"),
    ORANGE(0xFF984803, "Orange"),
    PINK(0xFF8D156D, "Pink"),

    // New additions
    CYAN(0xFF00687A, "Cyan"),
    INDIGO(0xFF4355B9, "Indigo"),
    ROSE(0xFF9A254C, "Rose"),
    LIME(0xFF4E6700, "Olive Lime"),
    EMERALD(0xFF0B6D43, "Emerald"),
    SLATE(0xFF3C3C3C, "Slate"),
    BROWN(0xFF76563F, "Mocha Brown")
}

data class Todo(
    val id: Long,
    val title: String,
    val details: String = "",
    val type: TodoType,
    val done: Boolean = false,
    val isNote: Boolean = false,
    /** Weekly: ISO date of that week's Monday. Monthly: "2026-11". Regular/General: empty. */
    val cycleKey: String = "",
    /** How many cycles this item has been carried over unfinished. */
    val carried: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val doneAt: Long? = null,
    /** Epoch timestamp in milliseconds for reminder time (General reminders, Notes to-dos). */
    val reminderTime: Long? = null,
)

data class TimerItem(
    val id: Long,
    val label: String = "",
    val totalDurationSeconds: Long,
    val remainingSeconds: Long,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val endTimestamp: Long = 0L,
    val finishedAt: Long? = null,
    val isPinned: Boolean = false,
)

/** Keys of the week and month that are "current" right now. */
data class Cycles(val week: String, val month: String)

object Cycle {
    fun weekStart(d: LocalDate): LocalDate = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    fun weekKey(d: LocalDate = LocalDate.now()): String = weekStart(d).toString()
    fun monthKey(d: LocalDate = LocalDate.now()): String = YearMonth.from(d).toString()
    fun keyFor(type: TodoType, d: LocalDate = LocalDate.now()): String = when (type) {
        TodoType.WEEKLY -> weekKey(d)
        TodoType.MONTHLY -> monthKey(d)
        TodoType.GENERAL -> ""
        TodoType.REGULAR -> ""
    }
}

/**
 * Derives a title from description: first 2 words (or 1 word if only one word exists)
 * converted to Title Case. If empty, falls back to "Reminder".
 */
fun deriveGeneralReminderTitle(description: String): String {
    val trimmed = description.trim()
    if (trimmed.isEmpty()) return "Reminder"
    val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return "Reminder"
    val count = if (words.size >= 2) 2 else 1
    return words.take(count).joinToString(" ") { word ->
        word.lowercase().replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }
}
