package dev.hungrymonkey.careercompass.models

data class Subtask(
    val title: String = "",
    val isCompleted: Boolean = false
)

data class ReminderSettings(
    val isEnabled: Boolean = false,
    val daysBefore: Int = 3,
    val notificationId: Int = 0
)

data class Goal(
    val id: String,
    val title: String,
    val targetDate: String,
    val subtasks: List<Subtask>,
    val icon: String,
    val priority: String,
    val reminderSettings: ReminderSettings = ReminderSettings()
)