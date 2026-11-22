package tech.huangsh.firstsight.data.repository

import tech.huangsh.firstsight.data.local.dao.ReminderDao
import tech.huangsh.firstsight.data.model.Reminder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao
) {
    val allReminders = reminderDao.getAllReminders()

    suspend fun getEnabledReminders(): List<Reminder> = reminderDao.getEnabledReminders()

    suspend fun getReminderById(id: Long): Reminder? = reminderDao.getReminderById(id)

    suspend fun insertReminder(reminder: Reminder): Long = reminderDao.insertReminder(reminder)

    suspend fun updateReminder(reminder: Reminder) {
        reminderDao.updateReminder(reminder.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteReminder(reminder: Reminder) {
        reminderDao.deleteReminder(reminder)
    }

    suspend fun markAcknowledged(id: Long, time: Long = System.currentTimeMillis()) {
        reminderDao.markAcknowledged(id, time)
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        reminderDao.setEnabled(id, enabled, System.currentTimeMillis())
    }
}
