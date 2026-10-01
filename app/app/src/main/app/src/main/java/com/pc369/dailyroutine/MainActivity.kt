package com.pc369.dailyroutine

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

data class Task(
    val name: String,
    var completed: Boolean = false
)

class MainActivity : AppCompatActivity() {

    private val tasks = mutableListOf<Task>()

    private lateinit var taskContainer: LinearLayout
    private lateinit var progressText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        taskContainer = findViewById(R.id.taskContainer)
        progressText = findViewById(R.id.progressText)
        progressBar = findViewById(R.id.progressBar)

        findViewById<Button>(R.id.addTaskButton).setOnClickListener {
            showAddTaskDialog()
        }

        updateTaskList()
    }

    private fun showAddTaskDialog() {
        val input = EditText(this)
        input.hint = "Enter task"

        AlertDialog.Builder(this)
            .setTitle("Add Task")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()

                if (name.isNotEmpty()) {
                    tasks.add(Task(name))
                    updateTaskList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateTaskList() {
        taskContainer.removeAllViews()

        for (task in tasks) {
            val checkBox = CheckBox(this)

            checkBox.text = task.name
            checkBox.textSize = 17f
            checkBox.isChecked = task.completed

            checkBox.setOnCheckedChangeListener { _, checked ->
                task.completed = checked
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
        val progress = (completed * 100) / tasks.size

        progressBar.progress = progress
        progressText.text = "Today's Progress: $progress%"
    }
}
