package tech.huangsh.firstsight.data.model

/**
 * 设置项数据模型
 */
data class Settings(
    val voiceEnabled: Boolean = true,
    val voiceVolume: Float = 0.8f,
    // 发起通话时朗读提示语，默认开启
    val callPromptEnabled: Boolean = true,
    // 每到整点播报当前时间，默认关闭，避免打扰
    val hourlyChimeEnabled: Boolean = false,
    // 低电量语音报警，默认开启，阈值默认 20%
    val lowBatteryAlertEnabled: Boolean = true,
    val lowBatteryThreshold: Int = 20,
    // 充满电语音提醒，默认关闭，触发后固定播报 1 分钟
    val fullBatteryAlertEnabled: Boolean = false,
    // 微信来电自动接听，默认关闭；开启时默认仅自动接听已添加的联系人，自动免提默认关闭
    val wechatAutoAnswerEnabled: Boolean = false,
    val wechatAutoAnswerContactsOnly: Boolean = true,
    val wechatAutoSpeakerphone: Boolean = false,
    // 通话挂断自动返回首页，默认关闭
    val wechatReturnHomeAfterCall: Boolean = false,
    val phoneReturnHomeAfterCall: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val launcherMode: Boolean = true,
    val floatingBallEnabled: Boolean = true,
    val autoStartEnabled: Boolean = true,
    val isDefaultLauncher: Boolean = false // 是否为默认桌面
)

/**
 * 主题模式
 *
 * 只保留深浅色维度：跟随系统 / 始终浅色 / 始终深色。
 * 原来的蓝橙配色方案已移除，避免同一语义出现四套色板。
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * 应用分类枚举
 */
enum class AppCategory {
    SOCIAL,
    TOOLS,
    ENTERTAINMENT,
    LIFE,
    OTHER
}

/**
 * 联系人操作类型
 */
enum class ContactAction {
    VIDEO_CALL,
    VOICE_CALL,
    PHONE_CALL
}