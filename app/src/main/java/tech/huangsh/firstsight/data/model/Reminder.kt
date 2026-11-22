package tech.huangsh.firstsight.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 提醒数据模型
 *
 * repeatDays 使用 bit0=周一 … bit6=周日；onceAt 不为空时表示只提醒一次。
 */
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val note: String = "",
    val isMedication: Boolean = true,
    val hour: Int,
    val minute: Int,
    val repeatDays: Int = ALL_DAYS,
    val onceAt: Long? = null,
    val enabled: Boolean = true,
    val lastAcknowledgedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val ALL_DAYS = 0b1111111
        const val WEEKDAYS = 0b0011111
        const val WEEKENDS = 0b1100000
    }
}
