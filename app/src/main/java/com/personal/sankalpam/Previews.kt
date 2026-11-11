@file:OptIn(ExperimentalMaterial3Api::class)

package com.personal.sankalpam

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true, name = "Full Reminders Screen - Light")
@Composable
fun FullRemindersScreenLightPreview() {
    val sampleTodos = listOf(
        Todo(1L, "File quarterly taxes", "Gather receipts and form 16", TodoType.GENERAL, false, reminderTime = System.currentTimeMillis() + 3600000),
        Todo(2L, "Doctor consultation", "Hospital visit at 4 PM", TodoType.GENERAL, false, reminderTime = System.currentTimeMillis() + 7200000),
        Todo(3L, "Pay electricity bill", "Online bill pay", TodoType.GENERAL, true, reminderTime = System.currentTimeMillis() - 3600000),
    )
    val cycles = Cycles("2026-11-09", "2026-11")
    sankalpamTheme(appTheme = AppTheme.LIGHT) {
        HomeScreen(
            currentTab = 0,
            onTabSelected = {},
            remindersTab = 0,
            onRemindersTabSelected = {},
            todos = sampleTodos,
            cycles = cycles,
            timers = emptyList(),
            repo = null,
            timerManager = null,
            onSettings = {},
        )
    }
}

@Preview(showBackground = true, name = "Full Timers Screen - Dark")
@Composable
fun FullTimersScreenDarkPreview() {
    val sampleTimers = listOf(
        TimerItem(1L, "Focus Pomodoro", 1500L, 840L, isRunning = true, isFinished = false),
        TimerItem(2L, "Tea Steeping", 180L, 0L, isRunning = false, isFinished = true),
        TimerItem(3L, "Plank Workout", 60L, 60L, isRunning = false, isFinished = false),
    )
    sankalpamTheme(appTheme = AppTheme.DARK) {
        HomeScreen(
            currentTab = 2,
            onTabSelected = {},
            remindersTab = 0,
            onRemindersTabSelected = {},
            todos = emptyList(),
            cycles = Cycles("2026-11-09", "2026-11"),
            timers = sampleTimers,
            repo = null,
            timerManager = null,
            onSettings = {},
        )
    }
}

@Preview(showBackground = true, name = "General Reminder Editor Preview")
@Composable
fun GeneralReminderEditorSheetPreview() {
    sankalpamTheme {
        Box(Modifier.padding(16.dp)) {
            GeneralReminderEditorSheet(
                initial = Todo(
                    id = 1L,
                    title = "Dentist appointment",
                    details = "Annual cleaning and checkup",
                    type = TodoType.GENERAL,
                    reminderTime = System.currentTimeMillis() + 86400000L,
                ),
                onSave = { _, _, _ -> },
                onDelete = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "Add Timer Sheet Preview")
@Composable
fun AddTimerSheetPreview() {
    sankalpamTheme {
        Box(Modifier.padding(16.dp)) {
            AddTimerSheet(
                onSave = { _, _, _, _ -> },
                onDismiss = {},
            )
        }
    }
}
