@file:OptIn(ExperimentalMaterial3Api::class)

package com.personal.sankalpam

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Asks the UI to jump to a tab and optional subtab. */
data class TabRequest(val tab: Int, val remindersTab: Int = 0, val nonce: Long = 0L)

private data class NavItem(val label: String, val icon: ImageVector)

// YouTube website style navigation icons: Notifications bell, Description library, Schedule timer
private val BOTTOM_NAV_ITEMS = listOf(
    NavItem("Reminders", Icons.Outlined.Notifications),
    NavItem("Notes", Icons.Outlined.Description),
    NavItem("Timers", Icons.Outlined.Schedule),
)

private val REMINDER_TABS = listOf("General", "Weekly", "Monthly")

@Composable
fun AppRoot(request: TabRequest?) {
    val ctx = LocalContext.current
    val repo = remember { TodoRepository.get(ctx) }
    val timerManager = remember { TimerManager.get(ctx) }

    val todos by repo.todos.collectAsState()
    val cycles by repo.cycles.collectAsState()
    val timers by timerManager.timers.collectAsState()

    var currentTab by rememberSaveable { mutableStateOf(0) }
    var currentRemindersTab by rememberSaveable { mutableStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(request) {
        if (request != null) {
            currentTab = request.tab
            currentRemindersTab = request.remindersTab
            showSettings = false
        }
    }
    BackHandler(enabled = showSettings) { showSettings = false }

    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
    } else {
        HomeScreen(
            currentTab = currentTab,
            onTabSelected = { currentTab = it },
            remindersTab = currentRemindersTab,
            onRemindersTabSelected = { currentRemindersTab = it },
            todos = todos,
            cycles = cycles,
            timers = timers,
            repo = repo,
            timerManager = timerManager,
            onSettings = { showSettings = true },
        )
    }
}

@Composable
fun HomeScreen(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    remindersTab: Int,
    onRemindersTabSelected: (Int) -> Unit,
    todos: List<Todo>,
    cycles: Cycles,
    timers: List<TimerItem>,
    repo: TodoRepository? = null,
    timerManager: TimerManager? = null,
    onSettings: () -> Unit,
) {
    var editingTodo by remember { mutableStateOf<Todo?>(null) }
    var creatingGeneralReminder by remember { mutableStateOf(false) }
    var creatingWeeklyTodo by remember { mutableStateOf(false) }
    var creatingMonthlyTodo by remember { mutableStateOf(false) }
    var creatingNoteOrTodo by remember { mutableStateOf(false) }
    var creatingTimer by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(2.dp))
                        Text("Sankalpam", fontWeight = FontWeight.SemiBold)
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                BOTTOM_NAV_ITEMS.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = currentTab == index,
                        onClick = { onTabSelected(index) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            when (currentTab) {
                0 -> {
                    // Reminders destination
                    ExtendedFloatingActionButton(
                        onClick = {
                            when (remindersTab) {
                                0 -> creatingGeneralReminder = true
                                1 -> creatingWeeklyTodo = true
                                else -> creatingMonthlyTodo = true
                            }
                        },
                        shape = RoundedCornerShape(30.dp),
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        text = {
                            Text(
                                when (remindersTab) {
                                    0 -> "New reminder"
                                    1 -> "New to-do"
                                    else -> "New to-do"
                                }
                            )
                        },
                    )
                }
                1 -> {
                    // Notes destination
                    ExtendedFloatingActionButton(
                        onClick = { creatingNoteOrTodo = true },
                        shape = RoundedCornerShape(30.dp),
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        text = { Text("New") },
                    )
                }
                2 -> {
                    // Timers destination
                    ExtendedFloatingActionButton(
                        onClick = { creatingTimer = true },
                        shape = RoundedCornerShape(30.dp),
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        text = { Text("Add timer") },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (currentTab) {
                0 -> RemindersSection(
                    selectedSubTab = remindersTab,
                    onSubTabSelected = onRemindersTabSelected,
                    todos = todos,
                    cycles = cycles,
                    repo = repo,
                    onEdit = { editingTodo = it },
                )
                1 -> NotesScreen(todos = todos, repo = repo, onEdit = { editingTodo = it })
                else -> TimersScreen(timers = timers, timerManager = timerManager)
            }
        }
    }

    // --- DIALOGS AND SHEETS ---

    // 1. Editing any existing Todo / Reminder
    editingTodo?.let { todo ->
        if (todo.type == TodoType.GENERAL) {
            GeneralReminderEditorSheet(
                initial = todo,
                onSave = { title, desc, reminderTime ->
                    repo?.update(todo.copy(title = title, details = desc, reminderTime = reminderTime))
                    editingTodo = null
                },
                onDelete = {
                    repo?.delete(todo.id)
                    editingTodo = null
                },
                onDismiss = { editingTodo = null },
            )
        } else {
            EditorSheet(
                initial = todo,
                type = todo.type,
                startAsNote = todo.isNote,
                onSave = { title, details, isNote, reminderTime ->
                    repo?.update(
                        todo.copy(
                            title = title,
                            details = details,
                            isNote = isNote,
                            reminderTime = reminderTime
                        )
                    )
                    editingTodo = null
                },
                onDelete = {
                    repo?.delete(todo.id)
                    editingTodo = null
                },
                onDismiss = { editingTodo = null },
            )
        }
    }

    // 2. Creating a new General Reminder
    if (creatingGeneralReminder) {
        GeneralReminderEditorSheet(
            initial = null,
            onSave = { title, desc, reminderTime ->
                repo?.add(
                    title = title,
                    type = TodoType.GENERAL,
                    details = desc,
                    isNote = false,
                    reminderTime = reminderTime
                )
                creatingGeneralReminder = false
            },
            onDelete = null,
            onDismiss = { creatingGeneralReminder = false },
        )
    }

    // 3. Creating Weekly To-do
    if (creatingWeeklyTodo) {
        EditorSheet(
            initial = null,
            type = TodoType.WEEKLY,
            startAsNote = false,
            onSave = { title, details, isNote, reminderTime ->
                repo?.add(title, TodoType.WEEKLY, details, isNote, reminderTime)
                creatingWeeklyTodo = false
            },
            onDelete = null,
            onDismiss = { creatingWeeklyTodo = false },
        )
    }

    // 4. Creating Monthly To-do
    if (creatingMonthlyTodo) {
        EditorSheet(
            initial = null,
            type = TodoType.MONTHLY,
            startAsNote = false,
            onSave = { title, details, isNote, reminderTime ->
                repo?.add(title, TodoType.MONTHLY, details, isNote, reminderTime)
                creatingMonthlyTodo = false
            },
            onDelete = null,
            onDismiss = { creatingMonthlyTodo = false },
        )
    }

    // 5. Creating Note / To-do in Notes tab
    if (creatingNoteOrTodo) {
        EditorSheet(
            initial = null,
            type = TodoType.REGULAR,
            startAsNote = true,
            onSave = { title, details, isNote, reminderTime ->
                repo?.add(title, TodoType.REGULAR, details, isNote, reminderTime)
                creatingNoteOrTodo = false
            },
            onDelete = null,
            onDismiss = { creatingNoteOrTodo = false },
        )
    }

    // 6. Creating a Timer
    if (creatingTimer) {
        AddTimerSheet(
            onSave = { h, m, s, label ->
                timerManager?.addTimer(h, m, s, label, autoStart = true)
                creatingTimer = false
            },
            onDismiss = { creatingTimer = false },
        )
    }
}

/**
 * Reminders Section hosting top tabs: General, Weekly, Monthly.
 */
@Composable
fun RemindersSection(
    selectedSubTab: Int,
    onSubTabSelected: (Int) -> Unit,
    todos: List<Todo>,
    cycles: Cycles,
    repo: TodoRepository? = null,
    onEdit: (Todo) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = selectedSubTab,
            modifier = Modifier.fillMaxWidth(),
        ) {
            REMINDER_TABS.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { onSubTabSelected(index) },
                    text = { Text(title, fontWeight = if (selectedSubTab == index) FontWeight.SemiBold else FontWeight.Normal) },
                )
            }
        }

        Box(Modifier.weight(1f)) {
            when (selectedSubTab) {
                0 -> GeneralRemindersScreen(todos = todos, repo = repo, onEdit = onEdit)
                1 -> CycleScreen(type = TodoType.WEEKLY, all = todos, cycleKey = cycles.week, repo = repo, onEdit = onEdit)
                else -> CycleScreen(type = TodoType.MONTHLY, all = todos, cycleKey = cycles.month, repo = repo, onEdit = onEdit)
            }
        }
    }
}

/**
 * Screen displaying General Reminders (individual date & time adjustable reminders).
 */
@Composable
fun GeneralRemindersScreen(
    todos: List<Todo>,
    repo: TodoRepository? = null,
    onEdit: (Todo) -> Unit,
) {
    val generalItems = todos.filter { it.type == TodoType.GENERAL }
    val pending = generalItems.filter { !it.done }.sortedBy { it.reminderTime ?: Long.MAX_VALUE }
    val done = generalItems.filter { it.done }.sortedByDescending { it.doneAt ?: 0L }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (generalItems.isEmpty()) {
            item(key = "empty") {
                EmptyHint("No general reminders yet.\nTap [+ New reminder] to set a reminder with date and time.")
            }
        }

        items(pending, key = { it.id }) { todo ->
            GeneralReminderCard(
                todo = todo,
                onToggle = { repo?.toggle(it) },
                onEdit = onEdit,
                modifier = Modifier.animateItem(),
            )
        }

        if (done.isNotEmpty()) {
            item(key = "doneLabel") { SectionLabel("Completed (${done.size})") }
            items(done, key = { it.id }) { todo ->
                GeneralReminderCard(
                    todo = todo,
                    onToggle = { repo?.toggle(it) },
                    onEdit = onEdit,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

/**
 * Weekly and Monthly cycle screen. Input horizontal bar replaced by bottom floating [+ New] button.
 */
@Composable
fun CycleScreen(
    type: TodoType,
    all: List<Todo>,
    cycleKey: String,
    repo: TodoRepository? = null,
    onEdit: (Todo) -> Unit,
) {
    val weekly = type == TodoType.WEEKLY
    val items = all.filter { it.type == type && it.cycleKey == cycleKey }
    val pending = items.filter { !it.done }
    val done = items.filter { it.done }
    val earlier = all
        .filter { it.type == type && it.cycleKey != cycleKey && it.done }
        .sortedByDescending { it.doneAt ?: 0L }
    var showEarlier by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") { CycleHeader(type, done.size, items.size) }

        if (items.isEmpty()) {
            item(key = "empty") {
                EmptyHint(
                    if (weekly) "Nothing yet. Tap [+ New to-do] to add to-dos for this week; you get a review on Saturday."
                    else "Nothing yet. Tap [+ New to-do] to add to-dos for this month; you get a review near month end."
                )
            }
        }

        items(pending, key = { it.id }) { todo ->
            TodoRow(todo, onToggle = { repo?.toggle(it) }, onEdit = onEdit, modifier = Modifier.animateItem())
        }

        if (done.isNotEmpty()) {
            item(key = "doneLabel") { SectionLabel("Completed (${done.size})") }
            items(done, key = { it.id }) { todo ->
                TodoRow(todo, onToggle = { repo?.toggle(it) }, onEdit = onEdit, modifier = Modifier.animateItem())
            }
        }

        if (earlier.isNotEmpty()) {
            item(key = "earlierToggle") {
                TextButton(onClick = { showEarlier = !showEarlier }) {
                    Text(if (showEarlier) "Hide earlier" else "Earlier completed (${earlier.size})")
                }
            }
            if (showEarlier) {
                items(earlier, key = { it.id }) { todo ->
                    TodoRow(todo, onToggle = {}, onEdit = onEdit, modifier = Modifier.animateItem(), readOnly = true)
                }
            }
        }
    }
}

/**
 * Notes screen hosting notes and to-dos (including individual to-dos with reminders).
 * Input horizontal bar replaced by bottom floating [+ New] button.
 */
@Composable
fun NotesScreen(
    todos: List<Todo> = emptyList(),
    repo: TodoRepository? = null,
    onEdit: (Todo) -> Unit = {}
) {
    val regular = todos.filter { it.type == TodoType.REGULAR }
    val open = regular.filter { !it.isNote && !it.done }
    val notes = regular.filter { it.isNote }.sortedByDescending { it.createdAt }
    val done = regular.filter { !it.isNote && it.done }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (regular.isEmpty()) {
            item(key = "empty") {
                EmptyHint("Quick notes and to-dos live here.\nTap [+ New] below to add one.")
            }
        }

        items(open, key = { it.id }) { todo ->
            TodoRow(todo, onToggle = { repo?.toggle(it) }, onEdit = onEdit, modifier = Modifier.animateItem())
        }

        if (notes.isNotEmpty()) {
            item(key = "notesLabel") { SectionLabel("Notes") }
            items(notes, key = { it.id }) { todo ->
                NoteCard(todo, onEdit, Modifier.animateItem())
            }
        }

        if (done.isNotEmpty()) {
            item(key = "doneLabel") { SectionLabel("Completed (${done.size})") }
            items(done, key = { it.id }) { todo ->
                TodoRow(todo, onToggle = { repo?.toggle(it) }, onEdit = onEdit, modifier = Modifier.animateItem())
            }
        }
    }
}

/**
 * Timers screen:
 * - Features the new hand-drawn TimerCard layout with circular countdown ring.
 * - Swipe left to right: Pins timer (pinned timers don't auto-delete after 12 hrs).
 * - Swipe right to left: Deletes timer after asking confirmation.
 * - Delete icon on card also asks confirmation before delete.
 * - Deleting stops any active ringing sound immediately.
 */
@Composable
fun TimersScreen(
    timers: List<TimerItem> = emptyList(),
    timerManager: TimerManager? = null,
) {
    var timerToDelete by remember { mutableStateOf<TimerItem?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (timers.isEmpty()) {
            item(key = "empty") {
                EmptyHint("No active timers.\nTap [+ Add timer] below to start a timer.")
            }
        }

        items(timers, key = { it.id }) { timer ->
            SwipeableTimerCard(
                timer = timer,
                onStart = { timerManager?.startTimer(it) },
                onPause = { timerManager?.pauseTimer(it) },
                onReset = { timerManager?.resetTimer(it) },
                onRequestDelete = { timerToDelete = it },
                onDismiss = { timerManager?.dismissFinishedTimer(it) },
                onTogglePin = { timerManager?.togglePinTimer(it) },
            )
        }
    }

    // Confirmation dialog before deleting timer
    timerToDelete?.let { timer ->
        AlertDialog(
            onDismissRequest = { timerToDelete = null },
            title = { Text("Delete timer?") },
            text = {
                Text(
                    if (timer.label.isNotBlank()) "Are you sure you want to delete \"${timer.label}\"?"
                    else "Are you sure you want to delete this timer?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        timerManager?.deleteTimer(timer.id)
                        timerToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { timerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// COMPOSE PREVIEWS FOR ANDROID STUDIO DESIGN PANEL
// -------------------------------------------------------------

@Preview(showBackground = true, name = "Reminders Screen - General")
@Composable
private fun RemindersGeneralPreview() {
    val sampleTodos = listOf(
        Todo(1L, "Pick up laundry", "From Cleaners on 5th Ave", TodoType.GENERAL, false, reminderTime = System.currentTimeMillis() + 3600000),
        Todo(2L, "Call electrician", "Fix kitchen light switch", TodoType.GENERAL, false, reminderTime = System.currentTimeMillis() + 7200000),
        Todo(3L, "Submit timesheet", "", TodoType.GENERAL, true, reminderTime = System.currentTimeMillis() - 3600000),
    )
    sankalpamTheme {
        GeneralRemindersScreen(todos = sampleTodos, repo = null, onEdit = {})
    }
}

@Preview(showBackground = true, name = "Notes Screen Preview")
@Composable
private fun NotesScreenPreview() {
    val sampleTodos = listOf(
        Todo(1L, "Call back plumber", "", TodoType.REGULAR, done = false),
        Todo(2L, "Grocery list", "Milk, Eggs, Spinach, Almonds", TodoType.REGULAR, isNote = true),
        Todo(3L, "Pay credit card bill", "Due date is tomorrow", TodoType.REGULAR, done = false, reminderTime = System.currentTimeMillis() + 3600000),
        Todo(4L, "Finished reading chapter 3", "", TodoType.REGULAR, done = true),
    )
    sankalpamTheme {
        NotesScreen(todos = sampleTodos, repo = null, onEdit = {})
    }
}

@Preview(showBackground = true, name = "Timers Screen Preview")
@Composable
private fun TimersScreenPreview() {
    val sampleTimers = listOf(
        TimerItem(1L, "Pasta timer", 600L, 342L, isRunning = true, isFinished = false, isPinned = true),
        TimerItem(2L, "Tea brewing", 180L, 0L, isRunning = false, isFinished = true),
        TimerItem(3L, "Pomodoro Focus", 1500L, 1500L, isRunning = false, isFinished = false),
    )
    sankalpamTheme {
        TimersScreen(timers = sampleTimers, timerManager = null)
    }
}
