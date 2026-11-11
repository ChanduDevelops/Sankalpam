package com.personal.sankalpam

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    private val tabRequest = mutableStateOf<TabRequest?>(null)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    var currentTheme by mutableStateOf(AppTheme.SYSTEM)
        private set

    var currentAccent by mutableStateOf(AccentColor.BLUE)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifier.ensureChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        handleIntent(intent)

        setContent {
            LaunchedEffect(Unit) {
                currentTheme = Prefs.getTheme(this@MainActivity)
                currentAccent = Prefs.getAccent(this@MainActivity)
            }
            sankalpamTheme(currentTheme, currentAccent) { AppRoot(tabRequest.value) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        currentTheme = Prefs.getTheme(this)
        currentAccent = Prefs.getAccent(this)
        TodoRepository.get(this).rollover()                       // new week or month? carry unfinished items over
        Scheduler.scheduleAll(this)                               // keep alarms armed
        TimerManager.get(this).cleanupExpiredCompletedTimers()    // auto-delete completed timers after 12 hrs
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            // Google Assistant / Gemini: Set a timer
            AlarmClock.ACTION_SET_TIMER -> {
                val lengthSecs = intent.getIntExtra(AlarmClock.EXTRA_LENGTH, 0)
                val label = intent.getStringExtra(AlarmClock.EXTRA_MESSAGE) ?: ""
                val totalSecs = if (lengthSecs > 0) lengthSecs else 60
                val h = totalSecs / 3600
                val m = (totalSecs % 3600) / 60
                val s = totalSecs % 60

                TimerManager.get(this).addTimer(h, m, s, label, autoStart = true)
                tabRequest.value = TabRequest(tab = 2, nonce = System.nanoTime())
                Toast.makeText(this, "Timer started for ${formatDurationSeconds(totalSecs.toLong())}", Toast.LENGTH_SHORT).show()
            }

            // Google Assistant / Gemini: Show timers
            AlarmClock.ACTION_SHOW_TIMERS -> {
                tabRequest.value = TabRequest(tab = 2, nonce = System.nanoTime())
            }

            // Google Assistant / Gemini / Share: Create note or reminder
            "android.intent.action.CREATE_NOTE",
            "com.google.android.gms.actions.CREATE_NOTE",
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                val title = intent.getStringExtra(Intent.EXTRA_TITLE) ?: ""
                val combined = if (title.isNotBlank()) title else text

                // If user said "remind" or text suggests a reminder, add to General Reminders
                val isReminder = combined.contains("remind", ignoreCase = true)
                if (isReminder) {
                    val desc = text.ifBlank { title }
                    val finalTitle = if (title.isNotBlank() && !title.equals("remind", ignoreCase = true)) {
                        title
                    } else {
                        deriveGeneralReminderTitle(desc)
                    }
                    val reminderTime = System.currentTimeMillis() + 3600_000L // 1 hour from now
                    TodoRepository.get(this).add(finalTitle, TodoType.GENERAL, desc, isNote = false, reminderTime = reminderTime)
                    tabRequest.value = TabRequest(tab = 0, remindersTab = 0, nonce = System.nanoTime())
                    Toast.makeText(this, "Reminder added: $finalTitle", Toast.LENGTH_SHORT).show()
                } else {
                    val finalTitle = title.ifBlank { deriveTitleFromDetails(text).ifBlank { "New item" } }
                    TodoRepository.get(this).add(finalTitle, TodoType.REGULAR, text, isNote = false)
                    tabRequest.value = TabRequest(tab = 1, nonce = System.nanoTime())
                    Toast.makeText(this, "Added to Notes", Toast.LENGTH_SHORT).show()
                }
            }

            // Internal navigation from notifications
            else -> {
                if (intent.hasExtra("tab")) {
                    val tab = intent.getIntExtra("tab", 0)
                    val remindersTab = intent.getIntExtra("reminders_tab", 0)
                    tabRequest.value = TabRequest(tab = tab, remindersTab = remindersTab, nonce = System.nanoTime())
                }
            }
        }
    }
}
