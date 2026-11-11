@file:OptIn(ExperimentalMaterial3Api::class)

package com.personal.sankalpam

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DayStyle
import java.util.Locale

private val STAMP = DateTimeFormatter.ofPattern("EEE d MMM, h:mm a")
private val DAY = DateTimeFormatter.ofPattern("MMM d")
private val REMINDER_DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")
private val REMINDER_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a")

fun LocalTime.pretty(): String = format(DateTimeFormatter.ofPattern("h:mm a"))

fun deriveTitleFromDetails(details: String): String {
    val firstWord = details.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
    return firstWord.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}

fun formatDurationSeconds(totalSecs: Long): String {
    val hrs = totalSecs / 3600
    val mins = (totalSecs % 3600) / 60
    val secs = totalSecs % 60
    return if (hrs > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }
}

fun sanitizeNumberInput(input: String): String {
    val digits = input.filter { it.isDigit() }
    return if (digits.startsWith("0") && digits.length > 1) {
        digits.trimStart('0').take(2)
    } else {
        digits.take(2)
    }
}

/**
 * Top card of the Weekly / Monthly tab. For the week it is a seven-day strip: today is filled,
 * and the two days that send reminders (Saturday, Sunday) carry a ring.
 */
@Composable
fun CycleHeader(type: TodoType, done: Int, total: Int) {
    val ctx = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val today = LocalDate.now()
    val now = LocalDateTime.now()
    val weekly = type == TodoType.WEEKLY
    val review = if (weekly) ReminderKind.WEEKLY_REVIEW else ReminderKind.MONTHLY_REVIEW
    val last = if (weekly) ReminderKind.WEEKLY_FINAL else ReminderKind.MONTHLY_FINAL
    val nextReview = Scheduler.nextTrigger(ctx, review, Prefs.time(ctx, review), now).format(STAMP)
    val nextLast = Scheduler.nextTrigger(ctx, last, Prefs.time(ctx, last), now).format(STAMP)

    val title = if (weekly) "This week" else YearMonth.from(today).format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    val subtitle = if (weekly) {
        val start = Cycle.weekStart(today)
        "${start.format(DAY)} to ${start.plusDays(6).format(DAY)}"
    } else {
        "Day ${today.dayOfMonth} of ${today.lengthOfMonth()}"
    }
    val progress by animateFloatAsState(
        targetValue = if (total == 0) 0f else done.toFloat() / total,
        label = "todoProgress",
    )

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = scheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
                Text(
                    if (total == 0) "No to-dos yet" else "$done of $total done",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (weekly) {
                WeekStrip(today)
            } else {
                LinearProgressIndicator(
                    progress = { today.dayOfMonth.toFloat() / today.lengthOfMonth() },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = scheme.tertiary,
                    trackColor = scheme.surfaceContainerHighest,
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = scheme.primary,
                trackColor = scheme.surfaceContainerHighest,
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Next review: $nextReview", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                Text("Next last call: $nextLast", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WeekStrip(today: LocalDate) {
    val ctx = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val start = Cycle.weekStart(today)
    val reviewDay = Prefs.weeklyDay(ctx, ReminderKind.WEEKLY_REVIEW)
    val finalDay = Prefs.weeklyDay(ctx, ReminderKind.WEEKLY_FINAL)

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (i in 0..6) {
            val day = start.plusDays(i.toLong())
            val isToday = day == today
            val isPast = day.isBefore(today)
            val remindsToday = day.dayOfWeek == reviewDay || day.dayOfWeek == finalDay
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
                Text(
                    day.dayOfWeek.getDisplayName(DayStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isToday) scheme.primary else Color.Transparent)
                        .then(
                            if (remindsToday && !isToday) Modifier.border(1.5.dp, scheme.tertiary, CircleShape)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${day.dayOfMonth}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isToday -> scheme.onPrimary
                            isPast -> scheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else -> scheme.onSurface
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    )
}

@Composable
fun EmptyHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp),
    )
}

@Composable
fun TodoRow(
    todo: Todo,
    onToggle: (Long) -> Unit,
    onEdit: (Todo) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val textColor by animateColorAsState(
        targetValue = if (todo.done) scheme.onSurfaceVariant.copy(alpha = 0.6f) else scheme.onSurface,
        label = "todoText",
    )
    Card(
        onClick = { onEdit(todo) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (todo.done) scheme.surfaceContainerLow else scheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            Modifier.padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = todo.done, onCheckedChange = { onToggle(todo.id) }, enabled = !readOnly)
            Column(Modifier.weight(1f)) {
                Text(
                    todo.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None,
                )
                if (todo.details.isNotBlank()) {
                    Text(
                        todo.details,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                if (todo.carried > 0 && !todo.done) {
                    Text(
                        if (todo.carried == 1) "Carried over" else "Carried over ${todo.carried} times",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.tertiary,
                    )
                }
                if (todo.reminderTime != null && !todo.done) {
                    val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(todo.reminderTime), ZoneId.systemDefault())
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Alarm,
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            ldt.format(STAMP),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.primary,
                        )
                    }
                }
            }
        }
    }
}

/** Card for General Reminders */
@Composable
fun GeneralReminderCard(
    todo: Todo,
    onToggle: (Long) -> Unit,
    onEdit: (Todo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isPast = todo.reminderTime != null && todo.reminderTime < System.currentTimeMillis() && !todo.done
    val reminderLdt = todo.reminderTime?.let {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
    }

    Card(
        onClick = { onEdit(todo) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (todo.done) scheme.surfaceContainerLow else scheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            Modifier.padding(start = 6.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = todo.done, onCheckedChange = { onToggle(todo.id) })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    todo.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (todo.done) scheme.onSurfaceVariant.copy(alpha = 0.6f) else scheme.onSurface,
                )
                if (todo.details.isNotBlank()) {
                    Text(
                        todo.details,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
                if (reminderLdt != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            todo.done -> scheme.surfaceContainerHighest
                            isPast -> scheme.errorContainer
                            else -> scheme.primaryContainer
                        },
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Alarm,
                                contentDescription = null,
                                tint = when {
                                    todo.done -> scheme.onSurfaceVariant
                                    isPast -> scheme.onErrorContainer
                                    else -> scheme.onPrimaryContainer
                                },
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                reminderLdt.format(STAMP),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = when {
                                    todo.done -> scheme.onSurfaceVariant
                                    isPast -> scheme.onErrorContainer
                                    else -> scheme.onPrimaryContainer
                                },
                            )
                            if (isPast && !todo.done) {
                                Spacer(Modifier.width(4.dp))
                                Text("• Overdue", style = MaterialTheme.typography.labelSmall, color = scheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteCard(todo: Todo, onEdit: (Todo) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Card(
        onClick = { onEdit(todo) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                todo.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSecondaryContainer,
            )
            if (todo.details.isNotBlank()) {
                Text(
                    todo.details,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSecondaryContainer,
                )
            }
        }
    }
}

/**
 * Swipeable Timer Card supporting:
 * - Swipe left-to-right (StartToEnd): Pins/Unpins timer (pinned timers don't auto-delete after 12 hrs).
 * - Swipe right-to-left (EndToStart): Asks confirmation before delete.
 * - Card layout strictly implements the hand-drawn sketch (IMG_20261111_193036.jpg).
 */
@Composable
fun SwipeableTimerCard(
    timer: TimerItem,
    onStart: (Long) -> Unit,
    onPause: (Long) -> Unit,
    onReset: (Long) -> Unit,
    onRequestDelete: (TimerItem) -> Unit,
    onDismiss: (Long) -> Unit,
    onTogglePin: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onTogglePin(timer.id)
                    false // snap back
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onRequestDelete(timer)
                    false // snap back and open confirmation dialog
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.fillMaxWidth(),
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    else -> Color.Transparent
                },
                label = "swipeBg"
            )
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment,
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (timer.isPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                            contentDescription = "Pin",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (timer.isPinned) "Unpin" else "Pin",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Delete",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    ) {
        TimerCard(
            timer = timer,
            onStart = onStart,
            onPause = onPause,
            onReset = onReset,
            onRequestDelete = onRequestDelete,
            onDismiss = onDismiss,
            onTogglePin = onTogglePin,
        )
    }
}

/**
 * Hand-drawn Timer Card implementation:
 * - Rounded rectangle card
 * - Left:
 *   - Label text ("Timer (if no label)") + Pin badge
 *   - Primary pill button: Start / Pause / Dismiss
 *   - Secondary row: Reset button (YouTube Replay icon) + Delete icon button (YouTube Delete icon)
 * - Right:
 *   - Circular Countdown Ring ("A")
 *   - Time countdown (e.g. 05:00) with "mm ss" or "hh (if hours)"
 */
@Composable
fun TimerCard(
    timer: TimerItem,
    onStart: (Long) -> Unit,
    onPause: (Long) -> Unit,
    onReset: (Long) -> Unit,
    onRequestDelete: (TimerItem) -> Unit,
    onDismiss: (Long) -> Unit,
    onTogglePin: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    val hrs = timer.remainingSeconds / 3600
    val mins = (timer.remainingSeconds % 3600) / 60
    val secs = timer.remainingSeconds % 60
    val timeFormatted = if (hrs > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }
    val timeSubLabel = if (hrs > 0) "hh mm ss" else "mm ss"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (timer.isFinished) scheme.errorContainer else scheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // LEFT COLUMN (Label, Start/Pause/Dismiss pill, Reset & Delete buttons)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Label + Pin icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = timer.label.ifBlank { "Timer" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (timer.isFinished) scheme.onErrorContainer else scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (timer.isPinned) {
                        Icon(
                            Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = if (timer.isFinished) scheme.onErrorContainer else scheme.primary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onTogglePin(timer.id) },
                        )
                    }
                }

                // Primary pill button: Start / Pause / Dismiss (sketch: "Start/dismiss")
                if (timer.isFinished) {
                    Button(
                        onClick = { onDismiss(timer.id) },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = scheme.error),
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = "Dismiss", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Dismiss")
                    }
                } else if (timer.isRunning) {
                    FilledTonalButton(
                        onClick = { onPause(timer.id) },
                        shape = RoundedCornerShape(50),
                    ) {
                        Icon(Icons.Outlined.Pause, contentDescription = "Pause", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pause")
                    }
                } else {
                    Button(
                        onClick = { onStart(timer.id) },
                        shape = RoundedCornerShape(50),
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = "Start", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Start")
                    }
                }

                // Bottom row: "Reset" pill button + "del" icon button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = { onReset(timer.id) },
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Outlined.Replay, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset")
                    }

                    IconButton(
                        onClick = { onRequestDelete(timer) },
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = if (timer.isFinished) scheme.onErrorContainer else scheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            // RIGHT COLUMN: Circular Countdown Progress Ring (labeled "A" in sketch)
            Box(
                modifier = Modifier.size(126.dp),
                contentAlignment = Alignment.Center,
            ) {
                val ringColor = if (timer.isFinished) scheme.error else scheme.primary
                val trackColor = if (timer.isFinished) scheme.error.copy(alpha = 0.2f) else scheme.surfaceContainerHighest

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 8.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(diameter, diameter)

                    // Background track ring
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )

                    // Foreground countdown progress arc
                    val sweep = if (timer.totalDurationSeconds > 0) {
                        (timer.remainingSeconds.toFloat() / timer.totalDurationSeconds.toFloat()) * 360f
                    } else 0f

                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }

                // Time countdown text in center of ring
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = timeFormatted,
                        style = (if (hrs > 0) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall).copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = if (timer.isFinished) scheme.error else scheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = timeSubLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (timer.isFinished) scheme.onErrorContainer else scheme.onSurfaceVariant,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

/**
 * EditorSheet for standard To-dos and Notes.
 * Features explicit Cancel and Save buttons for transaction.
 */
@Composable
fun EditorSheet(
    initial: Todo?,
    type: TodoType,
    startAsNote: Boolean,
    onSave: (title: String, details: String, isNote: Boolean, reminderTime: Long?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var details by remember { mutableStateOf(initial?.details ?: "") }
    var isNote by remember { mutableStateOf(initial?.isNote ?: startAsNote) }
    var reminderTime by remember { mutableStateOf(initial?.reminderTime) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val canSave = if (isNote) {
        title.isNotBlank() || details.isNotBlank()
    } else {
        title.isNotBlank()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                if (initial == null) {
                    when {
                        isNote -> "New note"
                        type == TodoType.WEEKLY -> "New weekly to-do"
                        type == TodoType.MONTHLY -> "New monthly to-do"
                        else -> "New to-do"
                    }
                } else "Edit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            if (type == TodoType.REGULAR) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !isNote, onClick = { isNote = false }, label = { Text("To-do") })
                    FilterChip(selected = isNote, onClick = { isNote = true }, label = { Text("Note") })
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (isNote) "Title (optional)" else "What needs doing?") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (isNote) "Note" else "Details (optional)") },
                minLines = if (isNote) 4 else 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            // Optional reminder for to-dos
            if (type == TodoType.REGULAR && !isNote) {
                val reminderLdt = reminderTime?.let {
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (reminderLdt == null) {
                        OutlinedButton(
                            onClick = {
                                reminderTime = System.currentTimeMillis() + 3600_000L
                                showDatePicker = true
                            },
                        ) {
                            Icon(Icons.Outlined.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add reminder")
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Outlined.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(reminderLdt.format(STAMP))
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { reminderTime = null }) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete")
                    }
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val finalTitle = if (isNote && title.isBlank()) {
                            deriveTitleFromDetails(details).ifBlank { "Untitled" }
                        } else {
                            title.trim()
                        }
                        onSave(finalTitle, details.trim(), isNote, reminderTime)
                    },
                    enabled = canSave,
                ) {
                    Text("Save")
                }
            }
        }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = reminderTime ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = dateState.selectedDateMillis ?: System.currentTimeMillis()
                        val newDate = Instant.ofEpochMilli(selectedDate).atZone(ZoneId.of("UTC")).toLocalDate()
                        val currentLocalTime = reminderTime?.let {
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()).toLocalTime()
                        } ?: LocalTime.now().plusHours(1).withMinute(0).withSecond(0)
                        val combined = LocalDateTime.of(newDate, currentLocalTime)
                        reminderTime = combined.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) { Text("Next (Time)") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        val currentLdt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(reminderTime ?: System.currentTimeMillis()),
            ZoneId.systemDefault()
        )
        val timeState = rememberTimePickerState(
            initialHour = currentLdt.hour,
            initialMinute = currentLdt.minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select reminder time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newLdt = currentLdt.withHour(timeState.hour).withMinute(timeState.minute).withSecond(0)
                        reminderTime = newLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        showTimePicker = false
                    }
                ) { Text("Done") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }
}

/**
 * Input Sheet for General Reminders:
 * 1. Title (optional) - if empty set to first 2 words or 1 word from description in Title case
 * 2. Description
 * 3. Date and Time (for reminder)
 * E. Cancel and Save buttons for transaction
 */
@Composable
fun GeneralReminderEditorSheet(
    initial: Todo?,
    onSave: (title: String, description: String, reminderTime: Long) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.details ?: "") }

    val defaultTarget = remember {
        val now = LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0)
        now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    var reminderTime by remember {
        mutableStateOf(initial?.reminderTime ?: defaultTarget)
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(reminderTime), ZoneId.systemDefault())

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                if (initial == null) "New reminder" else "Edit reminder",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title (optional - auto from description)") },
                placeholder = { Text("e.g. Doctor Appointment") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description") },
                placeholder = { Text("What would you like to be reminded of?") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            // Date & Time Picker Selector Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Remind at",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FilledTonalButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(ldt.format(REMINDER_DATE_FORMAT))
                        }
                        FilledTonalButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(ldt.format(REMINDER_TIME_FORMAT))
                        }
                    }

                    // Quick presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AssistChip(
                            onClick = {
                                val target = LocalDateTime.now().plusHours(1)
                                reminderTime = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            },
                            label = { Text("In 1 hour") },
                        )
                        AssistChip(
                            onClick = {
                                val target = LocalDate.now().atTime(18, 0)
                                reminderTime = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            },
                            label = { Text("Today 6 PM") },
                        )
                        AssistChip(
                            onClick = {
                                val target = LocalDate.now().plusDays(1).atTime(9, 0)
                                reminderTime = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            },
                            label = { Text("Tomorrow 9 AM") },
                        )
                    }
                }
            }

            // Transaction buttons: Cancel and Save
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete")
                    }
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val finalTitle = if (title.trim().isNotEmpty()) {
                            title.trim()
                        } else {
                            deriveGeneralReminderTitle(description)
                        }
                        onSave(finalTitle, description.trim(), reminderTime)
                    },
                    enabled = title.isNotBlank() || description.isNotBlank(),
                ) {
                    Text("Save")
                }
            }
        }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = reminderTime)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = dateState.selectedDateMillis ?: System.currentTimeMillis()
                        val newDate = Instant.ofEpochMilli(selectedDate).atZone(ZoneId.of("UTC")).toLocalDate()
                        val combined = LocalDateTime.of(newDate, ldt.toLocalTime())
                        reminderTime = combined.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = ldt.hour,
            initialMinute = ldt.minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select reminder time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val combined = ldt.withHour(timeState.hour).withMinute(timeState.minute).withSecond(0)
                        reminderTime = combined.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        showTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }
}

/**
 * Add Timer Sheet:
 * 1. hrs, mns, secs
 * 2. label (optional)
 * E. Cancel and Save buttons for transaction
 */
@Composable
fun AddTimerSheet(
    onSave: (hours: Int, minutes: Int, seconds: Int, label: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var hoursText by rememberSaveable { mutableStateOf("") }
    var minutesText by rememberSaveable { mutableStateOf("") }
    var secondsText by rememberSaveable { mutableStateOf("") }
    var label by rememberSaveable { mutableStateOf("") }

    val h = hoursText.toIntOrNull() ?: 0
    val m = minutesText.toIntOrNull() ?: 0
    val s = secondsText.toIntOrNull() ?: 0
    val canSave = (h > 0 || m > 0 || s > 0)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Add timer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

            // Duration input fields: Hours, Minutes, Seconds
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = sanitizeNumberInput(it)},
                    modifier = Modifier.weight(1f),
                    label = { Text("Hours") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = sanitizeNumberInput(it)},
                    modifier = Modifier.weight(1f),
                    label = { Text("Mins") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = secondsText,
                    onValueChange = { secondsText = sanitizeNumberInput(it)},
                    modifier = Modifier.weight(1f),
                    label = { Text("Secs") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }

            // Quick preset chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = { hoursText = "0"; minutesText = "1"; secondsText = "0" },
                    label = { Text("1 min") },
                )
                AssistChip(
                    onClick = { hoursText = "0"; minutesText = "5"; secondsText = "0" },
                    label = { Text("5 mins") },
                )
                AssistChip(
                    onClick = { hoursText = "0"; minutesText = "10"; secondsText = "0" },
                    label = { Text("10 mins") },
                )
                AssistChip(
                    onClick = { hoursText = "0"; minutesText = "25"; secondsText = "0" },
                    label = { Text("25 mins") },
                )
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Label (optional)") },
                placeholder = { Text("e.g. Focus, Cooking, Workout") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            // Transaction buttons: Cancel and Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = { onSave(h, m, s, label.trim()) },
                    enabled = canSave,
                ) {
                    Text("Save")
                }
            }
        }
    }
}

// -------------------------------------------------------------
// COMPOSE PREVIEWS FOR ANDROID STUDIO DESIGN PANEL
// -------------------------------------------------------------

@Preview(showBackground = true, name = "Cycle Header - Weekly")
@Composable
private fun CycleHeaderWeeklyPreview() {
    sankalpamTheme {
        Box(Modifier.padding(16.dp)) {
            CycleHeader(type = TodoType.WEEKLY, done = 4, total = 7)
        }
    }
}

@Preview(showBackground = true, name = "Cycle Header - Monthly")
@Composable
private fun CycleHeaderMonthlyPreview() {
    sankalpamTheme {
        Box(Modifier.padding(16.dp)) {
            CycleHeader(type = TodoType.MONTHLY, done = 12, total = 18)
        }
    }
}

@Preview(showBackground = true, name = "Todo Row - Active")
@Composable
private fun TodoRowActivePreview() {
    sankalpamTheme {
        Box(Modifier.padding(16.dp)) {
            TodoRow(
                todo = Todo(
                    id = 1L,
                    title = "File quarterly taxes",
                    details = "Gather receipts and form 16",
                    type = TodoType.WEEKLY,
                    done = false,
                    carried = 1,
                ),
                onToggle = {},
                onEdit = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "Todo Row - Completed")
@Composable
private fun TodoRowCompletedPreview() {
    sankalpamTheme {
        Box(Modifier.padding(16.dp)) {
            TodoRow(
                todo = Todo(
                    id = 2L,
                    title = "Grocery shopping",
                    details = "Buy vegetables, milk, coffee",
                    type = TodoType.WEEKLY,
                    done = true,
                ),
                onToggle = {},
                onEdit = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "General Reminder Card")
@Composable
private fun GeneralReminderCardPreview() {
    sankalpamTheme {
        Box(Modifier.padding(16.dp)) {
            GeneralReminderCard(
                todo = Todo(
                    id = 3L,
                    title = "Doctor Appointment",
                    details = "Consult Dr. Rao regarding regular checkup",
                    type = TodoType.GENERAL,
                    done = false,
                    reminderTime = System.currentTimeMillis() + 7200_000L,
                ),
                onToggle = {},
                onEdit = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "Note Card")
@Composable
private fun NoteCardPreview() {
    sankalpamTheme {
        Box(Modifier.padding(16.dp)) {
            NoteCard(
                todo = Todo(
                    id = 4L,
                    title = "Book recommendations",
                    details = "1. Atomic Habits\n2. Deep Work\n3. The Psychology of Money",
                    type = TodoType.REGULAR,
                    isNote = true,
                ),
                onEdit = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "Timer Card - Running (New Sketch)")
@Composable
private fun TimerCardRunningPreview() {
    sankalpamTheme {
        Box(Modifier.padding(16.dp)) {
            TimerCard(
                timer = TimerItem(
                    id = 1L,
                    label = "Deep Work",
                    totalDurationSeconds = 1500L,
                    remainingSeconds = 942L,
                    isRunning = true,
                    isFinished = false,
                    isPinned = true,
                ),
                onStart = {},
                onPause = {},
                onReset = {},
                onRequestDelete = {},
                onDismiss = {},
                onTogglePin = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "Timer Card - Finished (New Sketch)")
@Composable
private fun TimerCardFinishedPreview() {
    sankalpamTheme {
        Box(Modifier.padding(16.dp)) {
            TimerCard(
                timer = TimerItem(
                    id = 2L,
                    label = "Tea Brewing",
                    totalDurationSeconds = 180L,
                    remainingSeconds = 0L,
                    isRunning = false,
                    isFinished = true,
                ),
                onStart = {},
                onPause = {},
                onReset = {},
                onRequestDelete = {},
                onDismiss = {},
                onTogglePin = {},
            )
        }
    }
}
