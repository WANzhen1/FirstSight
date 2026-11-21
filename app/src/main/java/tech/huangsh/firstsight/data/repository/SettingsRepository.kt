package tech.huangsh.firstsight.data.repository

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import tech.huangsh.firstsight.data.model.Settings
import tech.huangsh.firstsight.data.model.ThemeMode
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 扩展DataStore
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 设置数据仓库
 */
class SettingsRepository(private val context: Context) {
    private val dataStore = context.dataStore

    // 键定义
    companion object {
        val VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        val VOICE_VOLUME = floatPreferencesKey("voice_volume")
        val CALL_PROMPT_ENABLED = booleanPreferencesKey("call_prompt_enabled")
        val HOURLY_CHIME_ENABLED = booleanPreferencesKey("hourly_chime_enabled")
        val LOW_BATTERY_ALERT_ENABLED = booleanPreferencesKey("low_battery_alert_enabled")
        val LOW_BATTERY_THRESHOLD = intPreferencesKey("low_battery_threshold")
        val FULL_BATTERY_ALERT_ENABLED = booleanPreferencesKey("full_battery_alert_enabled")
        val WECHAT_AUTO_ANSWER_ENABLED = booleanPreferencesKey("wechat_auto_answer_enabled")
        val WECHAT_AUTO_ANSWER_CONTACTS_ONLY = booleanPreferencesKey("wechat_auto_answer_contacts_only")
        val WECHAT_AUTO_SPEAKERPHONE = booleanPreferencesKey("wechat_auto_speakerphone")
        val WECHAT_RETURN_HOME_AFTER_CALL = booleanPreferencesKey("wechat_return_home_after_call")
        val PHONE_RETURN_HOME_AFTER_CALL = booleanPreferencesKey("phone_return_home_after_call")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LAUNCHER_MODE = booleanPreferencesKey("launcher_mode")
        val FLOATING_BALL_ENABLED = booleanPreferencesKey("floating_ball_enabled")
        val AUTO_START_ENABLED = booleanPreferencesKey("auto_start_enabled")
        val IS_DEFAULT_LAUNCHER = booleanPreferencesKey("is_default_launcher")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val WEATHER_PROVINCE = stringPreferencesKey("weather_province")
        val WEATHER_CITY = stringPreferencesKey("weather_city")
        val PRESET_CONTACTS_SEEDED = booleanPreferencesKey("preset_contacts_seeded")
        val POWER_POLICY_CONFIGURED = booleanPreferencesKey("power_policy_configured")
        val AUTO_START_CONFIGURED = booleanPreferencesKey("auto_start_configured")
    }

    // 语音设置
    val voiceEnabled: Flow<Boolean> = dataStore.data.map { it[VOICE_ENABLED] ?: true }
    val voiceVolume: Flow<Float> = dataStore.data.map { it[VOICE_VOLUME] ?: 0.8f }
    val callPromptEnabled: Flow<Boolean> = dataStore.data.map { it[CALL_PROMPT_ENABLED] ?: true }
    val hourlyChimeEnabled: Flow<Boolean> = dataStore.data.map { it[HOURLY_CHIME_ENABLED] ?: false }

    // 显示设置
    // 旧版本存的是 BLUE/ORANGE，读取时容错回退到跟随系统
    val themeMode: Flow<ThemeMode> = dataStore.data.map {
        runCatching { ThemeMode.valueOf(it[THEME_MODE] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    // 系统设置
    val launcherMode: Flow<Boolean> = dataStore.data.map { it[LAUNCHER_MODE] ?: true }
    val floatingBallEnabled: Flow<Boolean> = dataStore.data.map { it[FLOATING_BALL_ENABLED] ?: true }
    val autoStartEnabled: Flow<Boolean> = dataStore.data.map { it[AUTO_START_ENABLED] ?: true }
    
    // 桌面启动器设置
    val isDefaultLauncher: Flow<Boolean> = dataStore.data.map { it[IS_DEFAULT_LAUNCHER] ?: false }

    // 首次启动引导是否已完成
    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }

    // 天气城市设置（空串表示根据定位自动选择）
    val weatherProvince: Flow<String> = dataStore.data.map { it[WEATHER_PROVINCE] ?: "" }
    val weatherCity: Flow<String> = dataStore.data.map { it[WEATHER_CITY] ?: "" }

    // 是否已初始化内置默认联系人
    val presetContactsSeeded: Flow<Boolean> = dataStore.data.map { it[PRESET_CONTACTS_SEEDED] ?: false }

    // 省电策略与自启动是否已完成引导设置（厂商私有开关无公开读取 API，记录用户前往设置后的确认状态）
    val powerPolicyConfigured: Flow<Boolean> = dataStore.data.map { it[POWER_POLICY_CONFIGURED] ?: false }
    val autoStartConfigured: Flow<Boolean> = dataStore.data.map { it[AUTO_START_CONFIGURED] ?: false }

    // 获取所有设置
    val settings: Flow<Settings> = dataStore.data.map { preferences ->
        Settings(
            voiceEnabled = preferences[VOICE_ENABLED] ?: true,
            voiceVolume = preferences[VOICE_VOLUME] ?: 0.8f,
            callPromptEnabled = preferences[CALL_PROMPT_ENABLED] ?: true,
            hourlyChimeEnabled = preferences[HOURLY_CHIME_ENABLED] ?: false,
            lowBatteryAlertEnabled = preferences[LOW_BATTERY_ALERT_ENABLED] ?: true,
            lowBatteryThreshold = (preferences[LOW_BATTERY_THRESHOLD] ?: 20).coerceIn(10, 50),
            fullBatteryAlertEnabled = preferences[FULL_BATTERY_ALERT_ENABLED] ?: false,
            wechatAutoAnswerEnabled = preferences[WECHAT_AUTO_ANSWER_ENABLED] ?: false,
            wechatAutoAnswerContactsOnly = preferences[WECHAT_AUTO_ANSWER_CONTACTS_ONLY] ?: true,
            wechatAutoSpeakerphone = preferences[WECHAT_AUTO_SPEAKERPHONE] ?: false,
            wechatReturnHomeAfterCall = preferences[WECHAT_RETURN_HOME_AFTER_CALL] ?: false,
            phoneReturnHomeAfterCall = preferences[PHONE_RETURN_HOME_AFTER_CALL] ?: false,
            themeMode = runCatching {
                ThemeMode.valueOf(preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            launcherMode = preferences[LAUNCHER_MODE] ?: true,
            floatingBallEnabled = preferences[FLOATING_BALL_ENABLED] ?: true,
            autoStartEnabled = preferences[AUTO_START_ENABLED] ?: true,
            isDefaultLauncher = preferences[IS_DEFAULT_LAUNCHER] ?: false
        )
    }

    // 更新设置
    suspend fun updateVoiceEnabled(enabled: Boolean) {
        dataStore.edit { it[VOICE_ENABLED] = enabled }
    }

    suspend fun updateVoiceVolume(volume: Float) {
        dataStore.edit { it[VOICE_VOLUME] = volume }
    }

    suspend fun updateCallPromptEnabled(enabled: Boolean) {
        dataStore.edit { it[CALL_PROMPT_ENABLED] = enabled }
    }

    suspend fun updateHourlyChimeEnabled(enabled: Boolean) {
        dataStore.edit { it[HOURLY_CHIME_ENABLED] = enabled }
    }

    suspend fun updateLowBatteryAlertEnabled(enabled: Boolean) {
        dataStore.edit { it[LOW_BATTERY_ALERT_ENABLED] = enabled }
    }

    suspend fun updateLowBatteryThreshold(threshold: Int) {
        dataStore.edit { it[LOW_BATTERY_THRESHOLD] = threshold.coerceIn(10, 50) }
    }

    suspend fun updateFullBatteryAlertEnabled(enabled: Boolean) {
        dataStore.edit { it[FULL_BATTERY_ALERT_ENABLED] = enabled }
    }

    suspend fun updateWechatAutoAnswerEnabled(enabled: Boolean) {
        dataStore.edit { it[WECHAT_AUTO_ANSWER_ENABLED] = enabled }
    }

    suspend fun updateWechatAutoAnswerContactsOnly(contactsOnly: Boolean) {
        dataStore.edit { it[WECHAT_AUTO_ANSWER_CONTACTS_ONLY] = contactsOnly }
    }

    suspend fun updateWechatAutoSpeakerphone(enabled: Boolean) {
        dataStore.edit { it[WECHAT_AUTO_SPEAKERPHONE] = enabled }
    }

    suspend fun updateWechatReturnHomeAfterCall(enabled: Boolean) {
        dataStore.edit { it[WECHAT_RETURN_HOME_AFTER_CALL] = enabled }
    }

    suspend fun updatePhoneReturnHomeAfterCall(enabled: Boolean) {
        dataStore.edit { it[PHONE_RETURN_HOME_AFTER_CALL] = enabled }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun updateLauncherMode(enabled: Boolean) {
        dataStore.edit { it[LAUNCHER_MODE] = enabled }
    }

    suspend fun updateFloatingBallEnabled(enabled: Boolean) {
        dataStore.edit { it[FLOATING_BALL_ENABLED] = enabled }
    }

    suspend fun updateAutoStartEnabled(enabled: Boolean) {
        dataStore.edit { it[AUTO_START_ENABLED] = enabled }
    }

    suspend fun updateIsDefaultLauncher(isDefault: Boolean) {
        dataStore.edit { it[IS_DEFAULT_LAUNCHER] = isDefault }
    }

    suspend fun updateOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    suspend fun updateWeatherCity(province: String, city: String) {
        dataStore.edit {
            it[WEATHER_PROVINCE] = province
            it[WEATHER_CITY] = city
        }
    }

    suspend fun updatePresetContactsSeeded(seeded: Boolean) {
        dataStore.edit { it[PRESET_CONTACTS_SEEDED] = seeded }
    }

    suspend fun updatePowerPolicyConfigured(configured: Boolean) {
        dataStore.edit { it[POWER_POLICY_CONFIGURED] = configured }
    }

    suspend fun updateAutoStartConfigured(configured: Boolean) {
        dataStore.edit { it[AUTO_START_CONFIGURED] = configured }
    }

    /**
     * 同步读取引导完成标记，供 Launcher Activity 启动时判断
     */
    suspend fun isOnboardingCompleted(): Boolean = onboardingCompleted.first()

    /**
     * 格式化时间显示
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun formatTimeForDisplay(time: String): String {
        return try {
            val localTime = LocalTime.parse(time, DateTimeFormatter.ISO_LOCAL_TIME)
            localTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            time
        }
    }
}