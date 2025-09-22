package dev.hungrymonkey.careercompass.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.hungrymonkey.careercompass.MainActivity
import dev.hungrymonkey.careercompass.R
import dev.hungrymonkey.careercompass.models.Goal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.ZoneId

class GoalNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "goal_reminders"
        const val CHANNEL_NAME = "Goal Reminders"
        const val CHANNEL_DESCRIPTION = "Notifications for upcoming goal deadlines"
    }

    private val notificationManager = NotificationManagerCompat.from(context)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleGoalReminder(goal: Goal) {
        if (!goal.reminderSettings.isEnabled) {
            return
        }


        try {
            val targetDate = LocalDate.parse(goal.targetDate, DateTimeFormatter.ofPattern("MM/dd/yyyy"))
            val reminderDate = targetDate.minusDays(goal.reminderSettings.daysBefore.toLong())


            if (reminderDate.isBefore(LocalDate.now())) {
                return
            }

            val reminderTime = reminderDate.atTime(9, 0)
            val reminderMillis = reminderTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()


            val intent = Intent(context, GoalReminderReceiver::class.java).apply {
                putExtra("goal_id", goal.id)
                putExtra("goal_title", goal.title)
                putExtra("goal_icon", goal.icon)
                putExtra("target_date", goal.targetDate)
                putExtra("days_before", goal.reminderSettings.daysBefore)
                putExtra("notification_id", goal.reminderSettings.notificationId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                goal.reminderSettings.notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminderMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelGoalReminder(notificationId: Int) {
        val intent = Intent(context, GoalReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun showGoalReminderNotification(
        goalId: String,
        goalTitle: String,
        goalIcon: String,
        targetDate: String,
        daysBefore: Int,
        notificationId: Int
    ) {

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }


        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to_goals", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pluralDays = if (daysBefore == 1) "day" else "days"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$goalIcon Goal Reminder")
            .setContentText("\"$goalTitle\" is due in $daysBefore $pluralDays (on $targetDate)")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Your goal \"$goalTitle\" is due in $daysBefore $pluralDays on $targetDate. Don't forget to work on it!"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()


        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: Exception) {
            println("Error posting notification: ${e.message}")
        }
    }
}
