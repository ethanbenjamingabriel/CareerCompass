package dev.hungrymonkey.careercompass.models

import com.google.firebase.Timestamp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class GoalFirestore(
    val title: String = "",
    val targetDate: Timestamp = Timestamp.now(),
    val icon: String = "",
    val priority: String = "",
    val subtasks: List<Map<String,Any>> = emptyList(),
    val reminderSettings: Map<String, Any> = emptyMap()
) {
    fun toUiModel(id: String): Goal {
        val date = Instant.ofEpochSecond(targetDate.seconds)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))

        val uiSubs = subtasks.map { map ->
            Subtask(
                title = map["title"] as? String ?: "",
                isCompleted = map["isCompleted"] as? Boolean == true
            )
        }
        
        val reminderData = ReminderSettings(
            isEnabled = reminderSettings["isEnabled"] as? Boolean ?: false,
            daysBefore = (reminderSettings["daysBefore"] as? Long)?.toInt() ?: 3,
            notificationId = (reminderSettings["notificationId"] as? Long)?.toInt() ?: 0
        )
        
        return Goal(id, title, date, uiSubs, icon, priority, reminderData)
    }
}

fun Goal.toFirestoreModel(): Map<String,Any> = mapOf(
    "title" to title,
    "targetDate" to Timestamp(
        java.time.LocalDate.parse(targetDate, DateTimeFormatter.ofPattern("MM/dd/yyyy"))
            .atStartOfDay(ZoneId.systemDefault())
            .toEpochSecond(), 0
    ),
    "icon" to icon,
    "priority" to priority,
    "subtasks" to subtasks.map { sub ->
        mapOf(
            "title" to sub.title,
            "isCompleted" to sub.isCompleted
        )
    },
    "reminderSettings" to mapOf(
        "isEnabled" to reminderSettings.isEnabled,
        "daysBefore" to reminderSettings.daysBefore,
        "notificationId" to reminderSettings.notificationId
    )
)