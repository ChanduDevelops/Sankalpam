@file:OptIn(ExperimentalMaterial3Api::class)

package com.personal.sankalpam

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalTime

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var times by remember { mutableStateOf(ReminderKind.values().associateWith { Prefs.time(ctx, it) }) }
    var weeklyDays by remember { mutableStateOf(ReminderKind.values().filter { it.type == TodoType.WEEKLY }.associateWith { Prefs.weeklyDay(ctx, it) }) }
    var monthlyDays by remember { mutableStateOf(ReminderKind.values().filter { it.type == TodoType.MONTHLY }.associateWith { Prefs.monthlyDaysFromEnd(ctx, it) }) }

    var timerRingtoneTitle by remember { mutableStateOf(Prefs.getTimerRingtoneTitle(ctx)) }
    var notifToneTitle by remember { mutableStateOf(Prefs.getNotificationToneTitle(ctx)) }

    var pickingTime by remember { mutableStateOf<ReminderKind?>(null) }
    var pickingDay by remember { mutableStateOf<ReminderKind?>(null) }
    var currentTheme by remember { mutableStateOf(Prefs.getTheme(ctx)) }
    var pickingTheme by remember { mutableStateOf(false) }

    var currentAccent by remember { mutableStateOf(Prefs.getAccent(ctx)) }
    var pickingAccent by remember { mutableStateOf(false) }

    // Launcher for Timer Ringtone Picker
    val timerRingtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            val title = uri?.let { runCatching { RingtoneManager.getRingtone(ctx, it)?.getTitle(ctx) }.getOrNull() }
                ?: if (uri == null) "Silent" else "Default alarm tone"
            Prefs.setTimerRingtone(ctx, uri?.toString() ?: "", title)
            timerRingtoneTitle = title
        }
    }

    // Launcher for Notification Tone Picker
    val notifToneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            val title = uri?.let { runCatching { RingtoneManager.getRingtone(ctx, it)?.getTitle(ctx) }.getOrNull() }
                ?: if (uri == null) "Silent" else "Default notification tone"
            Prefs.setNotificationTone(ctx, uri?.toString() ?: "", title)
            notifToneTitle = title
            Notifier.ensureChannel(ctx)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsGroup("Appearance") {
                ListItem(
                    headlineContent = { Text("App Theme") },
                    supportingContent = {
                        val label = when (currentTheme) {
                            AppTheme.SYSTEM -> "System default"
                            AppTheme.LIGHT -> "Light"
                            AppTheme.DARK -> "Dark"
                            AppTheme.AMOLED -> "AMOLED"
                        }
                        Text(label)
                    },
                    leadingContent = {
                        Icon(
                            Icons.Outlined.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    modifier = Modifier.clickable { pickingTheme = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )

                // Accent color selector allows changing text/UI tint color across Light/Dark/AMOLED themes
                ListItem(
                    headlineContent = { Text("Accent Color") },
                    supportingContent = { Text(currentAccent.label) },
                    trailingContent = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(currentAccent.colorValue))
                        )
                    },
                    modifier = Modifier.clickable { pickingAccent = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }

            // Sounds & Tones: 1. Ringtone (for timers), 2. Notification tone (for reminders)
            SettingsGroup("Sounds & Tones") {
                ListItem(
                    headlineContent = { Text("Timer ringtone") },
                    supportingContent = { Text(timerRingtoneTitle) },
                    leadingContent = {
                        Icon(
                            Icons.Outlined.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    modifier = Modifier.clickable {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_TYPE,
                                RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE
                            )
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Timer Ringtone")
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Prefs.getTimerRingtoneUri(ctx))
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        }
                        timerRingtoneLauncher.launch(intent)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )

                ListItem(
                    headlineContent = { Text("Notification tone") },
                    supportingContent = { Text(notifToneTitle) },
                    leadingContent = {
                        Icon(
                            Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    modifier = Modifier.clickable {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Notification Tone")
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Prefs.getNotificationToneUri(ctx))
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                        }
                        notifToneLauncher.launch(intent)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }

            SettingsGroup("Weekly Reminders") {
                ReminderKind.values().filter { it.type == TodoType.WEEKLY }.forEach { kind ->
                    val dayName = weeklyDays.getValue(kind).name.lowercase().replaceFirstChar { it.titlecase() }
                    val hint = "Every $dayName" + (if (kind.isFinal) ", only if something is left" else "")
                    TimeRow(kind, times.getValue(kind), hint, onPickTime = { pickingTime = kind }, onPickDay = { pickingDay = kind })
                }
            }

            SettingsGroup("Monthly Reminders") {
                ReminderKind.values().filter { it.type == TodoType.MONTHLY }.forEach { kind ->
                    val daysFromEnd = monthlyDays.getValue(kind)
                    val dayText = if (daysFromEnd == 0L) "Last day of the month" else if (daysFromEnd == 1L) "Second-to-last day of the month" else "${daysFromEnd} days before end of month"
                    val hint = dayText + (if (kind.isFinal) ", only if something is left" else "")
                    TimeRow(kind, times.getValue(kind), hint, onPickTime = { pickingTime = kind }, onPickDay = { pickingDay = kind })
                }
            }

            SettingsGroup("Making sure reminders arrive") {
                Text(
                    "Some phones stop background apps to save battery. If a reminder doesn't show up, " +
                        "allow background activity and set this app's battery use to Unrestricted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { Notifier.show(ctx, ReminderKind.WEEKLY_REVIEW, force = true) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Send a test notification") }
                    OutlinedButton(onClick = { openNotificationSettings(ctx) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Notification settings")
                    }
                    OutlinedButton(onClick = { allowBackground(ctx) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Allow background activity")
                    }
                }
            }
        }
    }

    if (pickingTheme) {
        ThemeDialog(
            initial = currentTheme,
            onConfirm = { theme ->
                Prefs.setTheme(ctx, theme)
                currentTheme = theme
                if (ctx is MainActivity) ctx.recreate()
                pickingTheme = false
            },
            onDismiss = { pickingTheme = false },
        )
    }

    if (pickingAccent) {
        AccentDialog(
            initial = currentAccent,
            onConfirm = { accent ->
                Prefs.setAccent(ctx, accent)
                currentAccent = accent
                if (ctx is MainActivity) ctx.recreate()
                pickingAccent = false
            },
            onDismiss = { pickingAccent = false },
        )
    }

    pickingTime?.let { kind ->
        TimeDialog(
            title = kind.title,
            initial = times.getValue(kind),
            onConfirm = { time ->
                Prefs.setTime(ctx, kind, time)
                times = times + (kind to time)
                Scheduler.scheduleAll(ctx)
                pickingTime = null
            },
            onDismiss = { pickingTime = null },
        )
    }

    pickingDay?.let { kind ->
        if (kind.type == TodoType.WEEKLY) {
            WeeklyDayDialog(
                initial = weeklyDays.getValue(kind),
                onConfirm = { day ->
                    Prefs.setWeeklyDay(ctx, kind, day)
                    weeklyDays = weeklyDays + (kind to day)
                    Scheduler.scheduleAll(ctx)
                    pickingDay = null
                },
                onDismiss = { pickingDay = null },
            )
        } else {
            MonthlyDayDialog(
                initial = monthlyDays.getValue(kind),
                onConfirm = { days ->
                    Prefs.setMonthlyDaysFromEnd(ctx, kind, days)
                    monthlyDays = monthlyDays + (kind to days)
                    Scheduler.scheduleAll(ctx)
                    pickingDay = null
                },
                onDismiss = { pickingDay = null },
            )
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun TimeRow(kind: ReminderKind, time: LocalTime, hint: String, onPickTime: () -> Unit, onPickDay: () -> Unit) {
    ListItem(
        headlineContent = { Text(kind.title) },
        supportingContent = {
            Text(
                hint,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onPickDay() }
                    .padding(vertical = 4.dp),
            )
        },
        trailingContent = { FilledTonalButton(onClick = onPickTime) { Text(time.pretty()) } },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun TimeDialog(title: String, initial: LocalTime, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun WeeklyDayDialog(initial: DayOfWeek, onConfirm: (DayOfWeek) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select day") },
        text = {
            Column {
                DayOfWeek.values().forEach { day ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = day }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == day, onClick = { selected = day })
                        Spacer(Modifier.width(12.dp))
                        Text(day.name.lowercase().replaceFirstChar { it.titlecase() })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun MonthlyDayDialog(initial: Long, onConfirm: (Long) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(initial) }
    val options = listOf(
        0L to "Last day of month",
        1L to "1 day before end",
        2L to "2 days before end",
        3L to "3 days before end",
        4L to "4 days before end",
        5L to "5 days before end",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select day") },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = value }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == value, onClick = { selected = value })
                        Spacer(Modifier.width(12.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ThemeDialog(initial: AppTheme, onConfirm: (AppTheme) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(initial) }
    val options = listOf(
        AppTheme.SYSTEM to "System default",
        AppTheme.LIGHT to "Light",
        AppTheme.DARK to "Dark",
        AppTheme.AMOLED to "AMOLED",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select theme") },
        text = {
            Column {
                options.forEach { (theme, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = theme }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == theme, onClick = { selected = theme })
                        Spacer(Modifier.width(12.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentDialog(initial: AccentColor, onConfirm: (AccentColor) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select accent color") },
        text = {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AccentColor.values().forEach { accent ->
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(accent.colorValue))
                            .clickable { selected = accent },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected == accent) {
                            Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = Color.White)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun openNotificationSettings(ctx: Context) {
    runCatching {
        ctx.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun allowBackground(ctx: Context) {
    runCatching {
        ctx.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure {
        runCatching {
            ctx.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

// -------------------------------------------------------------
// COMPOSE PREVIEW FOR SETTINGS
// -------------------------------------------------------------

@Preview(showBackground = true, name = "Settings Screen Preview")
@Composable
private fun SettingsScreenPreview() {
    sankalpamTheme {
        SettingsScreen(onBack = {})
    }
}
