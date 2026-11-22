package tech.huangsh.firstsight.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import tech.huangsh.firstsight.data.local.dao.AppInfoDao
import tech.huangsh.firstsight.data.local.dao.ContactDao
import tech.huangsh.firstsight.data.local.dao.ReminderDao
import tech.huangsh.firstsight.data.model.AppInfo
import tech.huangsh.firstsight.data.model.Contact
import tech.huangsh.firstsight.data.model.Reminder

/**
 * 应用数据库
 */
@Database(
    entities = [
        Contact::class,
        AppInfo::class,
        Reminder::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun appInfoDao(): AppInfoDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE contacts ADD COLUMN trustedForAutoAnswer INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reminders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        note TEXT NOT NULL,
                        isMedication INTEGER NOT NULL,
                        hour INTEGER NOT NULL,
                        minute INTEGER NOT NULL,
                        repeatDays INTEGER NOT NULL,
                        onceAt INTEGER,
                        enabled INTEGER NOT NULL,
                        lastAcknowledgedAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
