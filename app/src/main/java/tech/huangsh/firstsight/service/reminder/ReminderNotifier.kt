package tech.huangsh.firstsight.service.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import tech.huangsh.firstsight.R
import tech.huangsh.firstsight.data.model.Reminder
import tech.huangsh.firstsight.ui.activity.ReminderActivity
import tech.huangsh.firstsight.utils.ReminderTimeUtils

/**
 * 提醒通知统一在这里构建，保证通知渠道、按钮和点击行为一致。
 */
object ReminderNotifier {

    const val CHANNEL_ID = "firstsight_reminders"

    fun show(context: Context, reminder: Reminder) {
        createChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return

        val contentIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            Intent(context, ReminderActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val ackIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt() * 10 + 1,
            Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_ACK
                putExtra(ReminderReceiver.EXTRA_ID, reminder.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt() * 10 + 2,
            Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_SNOOZE
                putExtra(ReminderReceiver.EXTRA_ID, reminder.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (reminder.isMedication) {
            context.getString(R.string.reminder_notification_medication_title)
        } else {
            context.getString(R.string.reminder_notification_title)
        }
        val timeText = ReminderTimeUtils.formatTime(reminder.hour, reminder.minute)
        val content = if (reminder.note.isBlank()) {
            "$timeText · ${ReminderTimeUtils.repeatLabel(reminder)}"
        } else {
            "$timeText · ${reminder.note}"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(reminder.title.ifBlank { title })
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n$content"))
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .addAction(0, context.getString(R.string.reminder_notification_taken), ackIntent)
            .addAction(0, context.getString(R.string.reminder_notification_snooze), snoozeIntent)
            .build()

        runCatching {
            notificationManager.notify(reminder.id.toInt(), notification)
        }
    }

    fun cancel(context: Context, reminderId: Long) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(reminderId.toInt())
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_notification_channel),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.reminder_notification_channel_desc)
            enableVibration(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }
}
