package tech.huangsh.firstsight.utils

import tech.huangsh.firstsight.data.model.Reminder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 提醒时间计算与展示。
 */
object ReminderTimeUtils {

    fun nextTriggerAt(
        reminder: Reminder,
        now: Long = System.currentTimeMillis()
    ): Long? {
        if (!reminder.enabled) return null

        reminder.onceAt?.let { onceAt ->
            return onceAt.takeIf { it > now }
        }

        if (reminder.repeatDays == 0) return null

        val calendar = Calendar.getInstance()
        for (offset in 0..7) {
            calendar.timeInMillis = now
            calendar.add(Calendar.DAY_OF_YEAR, offset)
            val dayBit = dayBitOf(calendar.get(Calendar.DAY_OF_WEEK))
            if (reminder.repeatDays and (1 shl dayBit) == 0) continue

            calendar.set(Calendar.HOUR_OF_DAY, reminder.hour)
            calendar.set(Calendar.MINUTE, reminder.minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            if (calendar.timeInMillis > now) return calendar.timeInMillis
        }
        return null
    }

    fun dayBitOf(calendarDay: Int): Int = (calendarDay + 5) % 7

    fun formatTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

    fun formatNextTrigger(triggerAt: Long, now: Long = System.currentTimeMillis()): String {
        val trigger = Calendar.getInstance().apply { timeInMillis = triggerAt }
        val today = Calendar.getInstance().apply { timeInMillis = now }
        val time = SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(triggerAt))

        val sameDay = trigger.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            trigger.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        if (sameDay) return "今天 $time"

        val tomorrow = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val isTomorrow = trigger.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
            trigger.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR)
        if (isTomorrow) return "明天 $time"

        return SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA).format(Date(triggerAt))
    }

    fun repeatLabel(reminder: Reminder): String {
        if (reminder.onceAt != null) return "仅一次"
        return when (reminder.repeatDays) {
            Reminder.ALL_DAYS -> "每天"
            Reminder.WEEKDAYS -> "周一至周五"
            Reminder.WEEKENDS -> "周末"
            0 -> "仅一次"
            else -> {
                val dayNames = arrayOf("一", "二", "三", "四", "五", "六", "日")
                val selected = dayNames.filterIndexed { index, _ ->
                    reminder.repeatDays and (1 shl index) != 0
                }
                if (selected.isEmpty()) "仅一次" else "每周" + selected.joinToString("、")
            }
        }
    }

    fun buildTextForSpeech(reminder: Reminder): String {
        return if (reminder.isMedication) {
            "该吃药了，${reminder.title}"
        } else {
            "提醒时间到了，${reminder.title}"
        }
    }
}
