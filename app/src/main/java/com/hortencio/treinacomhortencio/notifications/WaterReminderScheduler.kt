package com.hortencio.treinacomhortencio.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hortencio.treinacomhortencio.MainActivity
import com.hortencio.treinacomhortencio.R
import com.hortencio.treinacomhortencio.data.TrainingProfileStore
import java.util.Calendar

object WaterReminderScheduler {
    private const val CHANNEL_ID = "water_reminders"
    private const val NOTIFICATION_ID = 2048
    internal const val EXTRA_INTERVAL_MINUTES = "interval_minutes"
    internal const val ACTION_WATER_REMINDER = "com.hortencio.treinacomhortencio.WATER_REMINDER"

    fun schedule(context: Context, intervalMinutes: Int) {
        if (intervalMinutes == 0) {
            cancel(context)
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reminderPendingIntent(context, intervalMinutes)
        alarmManager.cancel(pendingIntent)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextReminderTime(intervalMinutes, System.currentTimeMillis()),
            pendingIntent
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(reminderPendingIntent(context, 0))
    }

    internal fun nextReminderTime(intervalMinutes: Int, now: Long): Long {
        require(intervalMinutes > 0)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.MINUTE, intervalMinutes)
        }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        if (hour < WAKE_START_HOUR) {
            calendar.set(Calendar.HOUR_OF_DAY, WAKE_START_HOUR)
            calendar.set(Calendar.MINUTE, 0)
        } else if (hour >= WAKE_END_HOUR) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, WAKE_START_HOUR)
            calendar.set(Calendar.MINUTE, 0)
        }
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    internal fun showReminder(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Lembretes de hidratação",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Lembretes opcionais para fazer uma pausa e beber água."
                }
            )
        }
        val openApp = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_water_notification)
            .setContentTitle("Uma pausa para hidratar")
            .setContentText("Se te apetecer, bebe um pouco de água e continua ao teu ritmo.")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun reminderPendingIntent(context: Context, intervalMinutes: Int): PendingIntent {
        val intent = Intent(context, WaterReminderReceiver::class.java)
            .setAction(ACTION_WATER_REMINDER)
            .putExtra(EXTRA_INTERVAL_MINUTES, intervalMinutes)
        return PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private const val WAKE_START_HOUR = 9
    private const val WAKE_END_HOUR = 21
}

class WaterReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                val interval = TrainingProfileStore(context).waterReminderIntervalMinutes()
                if (interval > 0) WaterReminderScheduler.schedule(context, interval)
            }
            WaterReminderScheduler.ACTION_WATER_REMINDER -> {
                WaterReminderScheduler.showReminder(context)
                val interval = intent.getIntExtra(WaterReminderScheduler.EXTRA_INTERVAL_MINUTES, 0)
                if (interval > 0) WaterReminderScheduler.schedule(context, interval)
            }
        }
    }
}
