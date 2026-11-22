package tech.huangsh.firstsight.service.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.huangsh.firstsight.data.model.Reminder
import tech.huangsh.firstsight.data.repository.ReminderRepository
import tech.huangsh.firstsight.ui.activity.ReminderActivity
import tech.huangsh.firstsight.utils.ReminderTimeUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用 setAlarmClock 排期：
 * 1. 不需要 SCHEDULE_EXACT_ALARM，系统允许闹钟类精确闹钟；
 * 2. 到点会显示闹钟图标，符合服药/复诊提醒的语义；
 * 3. 开机后由 ReminderBootReceiver 重新排期。
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderRepository: ReminderRepository
) {
    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminder: Reminder) {
        cancel(reminder.id)
        val triggerAt = ReminderTimeUtils.nextTriggerAt(reminder) ?: return
        setAlarm(reminder.id, triggerAt, snooze = false)
    }

    fun scheduleSnooze(reminder: Reminder, minutes: Int = 10) {
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        setAlarm(reminder.id, triggerAt, snooze = true)
    }

    fun cancel(reminderId: Long) {
        alarmManager.cancel(triggerPendingIntent(reminderId, snooze = false))
        alarmManager.cancel(triggerPendingIntent(reminderId, snooze = true))
    }

    fun cancelSnooze(reminderId: Long) {
        alarmManager.cancel(triggerPendingIntent(reminderId, snooze = true))
    }

    suspend fun scheduleAll() {
        reminderRepository.getEnabledReminders().forEach { schedule(it) }
    }

    private fun setAlarm(reminderId: Long, triggerAt: Long, snooze: Boolean) {
        val pendingIntent = triggerPendingIntent(reminderId, snooze)
        val showIntent = PendingIntent.getActivity(
            context,
            requestCode(reminderId, snooze),
            Intent(context, ReminderActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        runCatching {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), pendingIntent)
        }
    }

    private fun triggerPendingIntent(reminderId: Long, snooze: Boolean): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TRIGGER
            putExtra(ReminderReceiver.EXTRA_ID, reminderId)
            putExtra(ReminderReceiver.EXTRA_SNOOZE, snooze)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(reminderId, snooze),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(reminderId: Long, snooze: Boolean): Int {
        val base = (reminderId % 1_000_000L).toInt()
        return base * 2 + if (snooze) 1 else 0
    }
}
