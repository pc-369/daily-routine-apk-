package com.pc369.dailyroutine

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class Task(
    val name: String,
    var completed: Boolean = false,
    var hour: Int = -1,
    var minute: Int = -1
)

class MainActivity : AppCompatActivity() {

    private val tasks = mutableListOf<Task>()

    private lateinit var taskContainer: LinearLayout
    private lateinit var progressText: TextView
    private lateinit var progressBar: ProgressBar

    private val prefs by lazy {
        getSharedPreferences("daily_routine", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        taskContainer = findViewById(R.id.taskContainer)
        progressText = findViewById(R.id.progressText)
        progressBar = findViewById(R.id.progressBar)

        loadTasks()

        findViewById<Button>(R.id.addTaskButton).setOnClickListener {
            showAddTaskDialog()
        }

        updateTaskList()
    }

    private fun showAddTaskDialog() {

        val input = EditText(this)
        input.hint = "Enter task"
        input.setPadding(30, 20, 30, 20)

        AlertDialog.Builder(this)
            .setTitle("Add Task")
            .setView(input)
            .setPositiveButton("Next") { _, _ ->

                val name = input.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(this, "Enter a task", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                showReminderDialog(name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showReminderDialog(name: String) {

        AlertDialog.Builder(this)
            .setTitle("Set Reminder?")
            .setMessage("Would you like to set a reminder for this task?")
            .setPositiveButton("Yes") { _, _ ->
                chooseReminderTime(name)
            }
            .setNegativeButton("No") { _, _ ->
                tasks.add(Task(name))
                saveTasks()
                updateTaskList()
            }
            .show()
    }

    private fun chooseReminderTime(name: String) {

        val now = Calendar.getInstance()

        TimePickerDialog(
            this,
            { _, hour, minute ->

                val task = Task(
                    name = name,
                    completed = false,
                    hour = hour,
                    minute = minute
                )

                tasks.add(task)

                scheduleReminder(task)

                saveTasks()
                updateTaskList()

                Toast.makeText(
                    this,
                    String.format("Reminder set: %02d:%02d", hour, minute),
                    Toast.LENGTH_SHORT
                ).show()

            },
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun scheduleReminder(task: Task) {

        if (task.hour < 0 || task.minute < 0) return

        val alarmManager =
            getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("taskName", task.name)
        }

        val requestCode =
            (task.name + task.hour + task.minute).hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, task.hour)
            set(Calendar.MINUTE, task.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    private fun updateTaskList() {

        taskContainer.removeAllViews()

        for (task in tasks) {

            val checkBox = CheckBox(this)

            val reminderText =
                if (task.hour >= 0)
                    String.format(
                        "   ⏰ %02d:%02d",
                        task.hour,
                        task.minute
                    )
                else
                    ""

            checkBox.text = task.name + reminderText
            checkBox.textSize = 17f
            checkBox.isChecked = task.completed

            checkBox.setOnCheckedChangeListener { _, checked ->

                task.completed = checked

                saveTasks()
                updateProgress()
            }

            taskContainer.addView(checkBox)
        }

        updateProgress()
    }

    private fun updateProgress() {

        if (tasks.isEmpty()) {
            progressBar.progress = 0
            progressText.text = "Today's Progress: 0%"
            return
        }

        val completed = tasks.count { it.completed }

        val progress =
            (completed * 100) / tasks.size

        progressBar.progress = progress

        progressText.text =
            "Today's Progress: $progress%"
    }

    private fun saveTasks() {

        val array = JSONArray()

        for (task in tasks) {

            val obj = JSONObject()

            obj.put("name", task.name)
            obj.put("completed", task.completed)
            obj.put("hour", task.hour)
            obj.put("minute", task.minute)

            array.put(obj)
        }

        prefs.edit()
            .putString("tasks", array.toString())
            .apply()
    }

    private fun loadTasks() {

        val saved = prefs.getString("tasks", null) ?: return

        try {

            val array = JSONArray(saved)

            for (i in 0 until array.length()) {

                val obj = array.getJSONObject(i)

                tasks.add(
                    Task(
                        name = obj.getString("name"),
                        completed = obj.getBoolean("completed"),
                        hour = obj.getInt("hour"),
                        minute = obj.getInt("minute")
                    )
                )
            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Could not load saved tasks",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
