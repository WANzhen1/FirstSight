package tech.huangsh.firstsight.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import tech.huangsh.firstsight.data.local.AppDatabase
import tech.huangsh.firstsight.data.local.dao.AppInfoDao
import tech.huangsh.firstsight.data.local.dao.ContactDao
import tech.huangsh.firstsight.data.local.dao.ReminderDao
import tech.huangsh.firstsight.data.remote.WeatherService
import tech.huangsh.firstsight.data.repository.AppRepository
import tech.huangsh.firstsight.data.repository.ContactRepository
import tech.huangsh.firstsight.data.repository.ReminderRepository
import tech.huangsh.firstsight.data.repository.SettingsRepository
import tech.huangsh.firstsight.utils.VoiceAssistant
import javax.inject.Singleton

/**
 * 应用模块 - 提供应用级别的依赖
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "one_tap_database"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideContactDao(database: AppDatabase): ContactDao = database.contactDao()

    @Provides
    fun provideAppInfoDao(database: AppDatabase): AppInfoDao = database.appInfoDao()

    @Provides
    fun provideReminderDao(database: AppDatabase): ReminderDao = database.reminderDao()

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob())

    @Provides
    @Singleton
    fun provideContactRepository(
        contactDao: ContactDao,
        @ApplicationContext context: Context
    ): ContactRepository {
        return ContactRepository(contactDao, context)
    }

    @Provides
    @Singleton
    fun provideAppRepository(
        appInfoDao: AppInfoDao,
        @ApplicationContext context: Context
    ): AppRepository {
        return AppRepository(appInfoDao, context)
    }

    @Provides
    @Singleton
    fun provideReminderRepository(
        reminderDao: ReminderDao
    ): ReminderRepository {
        return ReminderRepository(reminderDao)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository {
        return SettingsRepository(context)
    }

    @Provides
    @Singleton
    fun provideWeatherService(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository
    ): WeatherService {
        return WeatherService(context, settingsRepository)
    }

    /**
     * TTS 引擎是重资源，全应用共用一个实例
     *
     * 多个 ViewModel 各自 new 一份会重复绑定系统语音服务，
     * 且桌面 Activity 常驻不销毁，实例永远不会被释放。
     */
    @Provides
    @Singleton
    fun provideVoiceAssistant(
        @ApplicationContext context: Context
    ): VoiceAssistant {
        return VoiceAssistant(context)
    }
}