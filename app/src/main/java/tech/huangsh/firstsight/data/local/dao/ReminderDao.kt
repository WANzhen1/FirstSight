package tech.huangsh.firstsight.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import tech.huangsh.firstsight.data.model.Reminder

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY hour ASC, minute ASC, id ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE enabled = 1")
    suspend fun getEnabledReminders(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): Reminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("UPDATE reminders SET lastAcknowledgedAt = :time, updatedAt = :time WHERE id = :id")
    suspend fun markAcknowledged(id: Long, time: Long)

    @Query("UPDATE reminders SET enabled = :enabled, updatedAt = :time WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean, time: Long)
}
