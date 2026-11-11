package com.personal.sankalpam

import android.content.Context
import java.io.File
import java.time.LocalDate
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Single source of truth. Keeps everything in memory (StateFlow) and mirrors it to
 * files/todos.json. Plenty for a personal list; no database needed.
 */
class TodoRepository private constructor(private val appContext: Context) {

    private val file = File(appContext.filesDir, "todos.json")
    private val io = Executors.newSingleThreadExecutor()

    private val _todos = MutableStateFlow(load())
    val todos: StateFlow<List<Todo>> = _todos.asStateFlow()

    private val _cycles = MutableStateFlow(cyclesFor(LocalDate.now()))
    val cycles: StateFlow<Cycles> = _cycles.asStateFlow()

    init {
        rollover()
    }

    /**
     * New week / new month: unfinished items move into the new cycle (so they are
     * reminded again next time); finished items stay behind as history.
     */
    fun rollover() {
        val c = cyclesFor(LocalDate.now())
        _cycles.value = c
        val current = _todos.value
        val next = current.map { t ->
            when {
                t.done || t.type == TodoType.REGULAR || t.type == TodoType.GENERAL -> t
                t.type == TodoType.WEEKLY && t.cycleKey != c.week ->
                    t.copy(cycleKey = c.week, carried = t.carried + 1)
                t.type == TodoType.MONTHLY && t.cycleKey != c.month ->
                    t.copy(cycleKey = c.month, carried = t.carried + 1)
                else -> t
            }
        }
        if (next != current) commit(next)
    }

    /** Items belonging to the current week / month. */
    fun cycleItems(type: TodoType): List<Todo> {
        val key = if (type == TodoType.WEEKLY) _cycles.value.week else _cycles.value.month
        return _todos.value.filter { it.type == type && it.cycleKey == key }
    }

    fun add(
        title: String,
        type: TodoType,
        details: String = "",
        isNote: Boolean = false,
        reminderTime: Long? = null,
    ) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        rollover()
        val todo = Todo(
            id = (_todos.value.maxOfOrNull { it.id } ?: 0L) + 1,
            title = clean,
            details = details.trim(),
            type = type,
            isNote = isNote,
            cycleKey = Cycle.keyFor(type),
            reminderTime = reminderTime,
        )
        commit(_todos.value + todo)
        if (reminderTime != null && !todo.done) {
            Scheduler.scheduleGeneralReminder(appContext, todo)
        }
    }

    fun update(todo: Todo) {
        commit(_todos.value.map { if (it.id == todo.id) todo else it })
        if (todo.reminderTime != null && !todo.done) {
            Scheduler.scheduleGeneralReminder(appContext, todo)
        } else {
            Scheduler.cancelGeneralReminder(appContext, todo.id)
        }
    }

    fun toggle(id: Long) {
        var updatedTodo: Todo? = null
        val updatedList = _todos.value.map {
            if (it.id == id) {
                val nextDone = !it.done
                val item = it.copy(done = nextDone, doneAt = if (nextDone) System.currentTimeMillis() else null)
                updatedTodo = item
                item
            } else it
        }
        commit(updatedList)
        updatedTodo?.let {
            if (it.done) {
                Scheduler.cancelGeneralReminder(appContext, it.id)
            } else if (it.reminderTime != null && it.reminderTime > System.currentTimeMillis()) {
                Scheduler.scheduleGeneralReminder(appContext, it)
            }
        }
    }

    fun delete(id: Long) {
        commit(_todos.value.filter { it.id != id })
        Scheduler.cancelGeneralReminder(appContext, id)
    }

    private fun cyclesFor(d: LocalDate) = Cycles(Cycle.weekKey(d), Cycle.monthKey(d))

    private fun commit(list: List<Todo>) {
        _todos.value = list
        val json = JSONArray().apply { list.forEach { put(it.toJson()) } }.toString()
        io.execute {
            runCatching {
                val tmp = File(file.parentFile, "todos.json.tmp")
                tmp.writeText(json)
                if (!tmp.renameTo(file)) file.writeText(json)
            }
        }
    }

    private fun load(): List<Todo> = runCatching {
        if (!file.exists()) {
            emptyList<Todo>()
        } else {
            val arr = JSONArray(file.readText())
            List(arr.length()) { arr.getJSONObject(it).toTodo() }
        }
    }.getOrDefault(emptyList<Todo>())

    companion object {
        @Volatile private var instance: TodoRepository? = null

        fun get(context: Context): TodoRepository =
            instance ?: synchronized(this) {
                instance ?: TodoRepository(context.applicationContext).also { instance = it }
            }
    }
}

private fun Todo.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("details", details)
    put("type", type.name)
    put("done", done)
    put("isNote", isNote)
    put("cycleKey", cycleKey)
    put("carried", carried)
    put("createdAt", createdAt)
    put("doneAt", doneAt ?: JSONObject.NULL)
    put("reminderTime", reminderTime ?: JSONObject.NULL)
}

private fun JSONObject.toTodo(): Todo = Todo(
    id = getLong("id"),
    title = getString("title"),
    details = optString("details", ""),
    type = runCatching { TodoType.valueOf(getString("type")) }.getOrDefault(TodoType.REGULAR),
    done = optBoolean("done", false),
    isNote = optBoolean("isNote", false),
    cycleKey = optString("cycleKey", ""),
    carried = optInt("carried", 0),
    createdAt = optLong("createdAt", 0L),
    doneAt = if (isNull("doneAt")) null else getLong("doneAt"),
    reminderTime = if (isNull("reminderTime")) null else getLong("reminderTime"),
)
