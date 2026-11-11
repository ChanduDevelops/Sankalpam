# Sankalpam

A modern, offline-first personal productivity and cycle reminder app for Android built with **Kotlin** and **Jetpack Compose (Material 3)**.

Sankalpam is designed to keep your commitments clear and manageable through structured review cycles, general time-based reminders, quick notes, and countdown timers—with zero cloud lock-in, no tracking, and no external dependencies.

---

## Features

### 1. Reminders
* **General Reminders**: Set one-off or scheduled reminders with precise date and time pickers. Supports auto-deriving titles from descriptions in Title Case when left blank.
* **Weekly Reviews**: Monday-to-Sunday cycles featuring a 7-day visual progress strip. Unfinished items automatically carry over to the next cycle with carry-over badges. Sends a Saturday review nudge and a Sunday last-call notification.
* **Monthly Reviews**: Track progress across the month with automated review notifications before month-end and rollover for unfinished goals.
* **Smart Floating Action Buttons**: Clean `[+ New]` button on every tab with dedicated transaction sheets featuring explicit **Cancel** and **Save** actions.

### 2. Timers
* **Custom Hand-Drawn Card Layout**: Features a rounded card with label, prominent pill control buttons (**Start**, **Pause**, **Dismiss**, and **Reset**), and a circular countdown progress ring displaying real-time `mm:ss` / `hh:mm:ss`.
* **Live Status Alerts & Ongoing Notification**: Active timers run a persistent notification in the notification shade using Android's native countdown chronometer (`CATEGORY_STOPWATCH`), supporting status bar live alert pills and Dynamic Island-style displays.
* **Swipe Gestures**:
  * **Swipe Left to Right**: Pin/unpin a timer. Pinned timers are never auto-deleted.
  * **Swipe Right to Left**: Delete timer with confirmation dialog.
* **Smart Alarm & 12-Hour Auto-Delete**:
  * Ringing stops immediately on either **Dismiss** or **Delete**.
  * Completed unpinned timers are automatically cleaned up 12 hours after stopping to keep your dashboard clutter-free.

### 3. Notes & To-Dos
* Maintain daily to-dos and long-form notes side by side.
* Attach optional date/time reminders to individual to-do items directly from the notes view.

### 4. Customizable Appearance & Audio Tones
* **Themes**: System default (dynamic wallpaper colors on Android 12+), Light, Dark, and true black AMOLED modes.
* **Custom Accent Colors**: Neutral surface styling with customizable accent palettes (**Blue**, **Green**, **Purple**, **Red**, **Orange**, **Pink**).
* **Tone Pickers**: Customize **Timer Ringtone** and **Notification Tone** directly from Android's system sound library.

### 5. Google Assistant & Gemini Integration
* Integrated with Android App Actions (`actions.intent.SET_TIMER`, `actions.intent.SHOW_TIMERS`, `actions.intent.CREATE_NOTE`, `actions.intent.CREATE_TODO_ITEM`).
* Hands-free voice commands to start timers or create reminders (e.g. *"Set a timer for 10 minutes"* or *"Add a reminder to buy milk"*).

---

## Tech Stack & Architecture

* **Language**: Kotlin 2.2+
* **UI Toolkit**: Jetpack Compose with Material 3 & Compose BOM
* **State Management**: Kotlin Coroutines & `StateFlow`
* **Scheduling**: Android `AlarmManager` (Exact Alarms) & `NotificationManager`
* **Storage**: Local private JSON (`todos.json`, `timers.json`) in internal app storage (no SQLite/Room overhead, instantaneous serialization)
* **Previews**: Full `@Preview` composables with mock data enabled for Android Studio UI design view without launching an emulator.

---

## Getting Started

### Prerequisites
* Android Studio (Ladybug / Koala / Meerkat or newer recommended)
* JDK 17
* Android SDK (API 35+ target, minimum API 26 / Android 8.0)

### Opening & Building in Android Studio
1. Clone this repository:
   ```bash
   git clone https://github.com/ChanduDevelops/sankalpam.git
   ```
2. Open Android Studio, select **File > Open**, and choose the cloned `Sankalpam` directory.
3. Allow Gradle to sync.
4. Run on a connected device or emulator by clicking the green **Run** button (or build the APK via **Build > Build Bundle(s) / APK(s) > Build APK(s)**).

---

## Permissions
* `POST_NOTIFICATIONS`: To show timer alerts and cycle reminders.
* `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`: For precise trigger times.
* `RECEIVE_BOOT_COMPLETED`: To automatically re-arm reminders after device reboot.
* `VIBRATE` & `WAKE_LOCK`: For alerting when a timer expires.

---

## License
Personal project. Feel free to use and adapt for personal productivity.
