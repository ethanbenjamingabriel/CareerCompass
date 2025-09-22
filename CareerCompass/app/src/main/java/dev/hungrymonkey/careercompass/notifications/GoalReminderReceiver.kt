package dev.hungrymonkey.careercompass.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class GoalReminderReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        
        val goalId = intent.getStringExtra("goal_id")
        val goalTitle = intent.getStringExtra("goal_title")
        val goalIcon = intent.getStringExtra("goal_icon") ?: "🎯"
        val targetDate = intent.getStringExtra("target_date")
        val daysBefore = intent.getIntExtra("days_before", 3)
        val notificationId = intent.getIntExtra("notification_id", goalId?.hashCode() ?: 0)
        
        if (goalId == null || goalTitle == null || targetDate == null) {
            return
        }
        
        val notificationManager = GoalNotificationManager(context)
        notificationManager.showGoalReminderNotification(
            goalId = goalId,
            goalTitle = goalTitle,
            goalIcon = goalIcon,
            targetDate = targetDate,
            daysBefore = daysBefore,
            notificationId = notificationId
        )
    }
}
