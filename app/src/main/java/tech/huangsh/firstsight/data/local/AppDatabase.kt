package tech.huangsh.firstsight.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import tech.huangsh.firstsight.data.local.dao.AppInfoDao
import tech.huangsh.firstsight.data.local.dao.ContactDao
import tech.huangsh.firstsight.data.model.AppInfo
import tech.huangsh.firstsight.data.model.Contact

/**
 * 应用数据库
 */
@Database(
    entities = [
        Contact::class,
        AppInfo::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun appInfoDao(): AppInfoDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE contacts ADD COLUMN trustedForAutoAnswer INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
