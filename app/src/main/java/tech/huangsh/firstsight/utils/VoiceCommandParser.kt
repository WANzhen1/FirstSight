package tech.huangsh.firstsight.utils

import tech.huangsh.firstsight.data.model.AppInfo
import tech.huangsh.firstsight.data.model.Contact

sealed interface VoiceCommand {
    data class CallContact(val contact: Contact) : VoiceCommand
    data class OpenApp(val app: AppInfo) : VoiceCommand
    data object SpeakWeather : VoiceCommand
    data object SpeakTime : VoiceCommand
    data object OpenReminders : VoiceCommand
    data object OpenTools : VoiceCommand
    data object ToggleFlashlight : VoiceCommand
    data object OpenMagnifier : VoiceCommand
    data object OpenCalculator : VoiceCommand
    data object SetAlarm : VoiceCommand
    data object Unknown : VoiceCommand
}

/**
 * 只解析固定指令，不接开放式大模型，避免老人说错时执行不可预期的操作。
 */
object VoiceCommandParser {

    fun parse(rawText: String, contacts: List<Contact>, apps: List<AppInfo>): VoiceCommand {
        val text = rawText.trim().replace(" ", "")
        if (text.isBlank()) return VoiceCommand.Unknown

        if (text.contains("天气")) return VoiceCommand.SpeakWeather
        if (text.contains("几点") || text.contains("时间")) return VoiceCommand.SpeakTime
        if (text.contains("提醒") || text.contains("吃药")) return VoiceCommand.OpenReminders
        if (text.contains("手电")) return VoiceCommand.ToggleFlashlight
        if (text.contains("放大")) return VoiceCommand.OpenMagnifier
        if (text.contains("计算")) return VoiceCommand.OpenCalculator
        if (text.contains("闹钟")) return VoiceCommand.SetAlarm
        if (text.contains("工具")) return VoiceCommand.OpenTools

        if (text.contains("打电话") || text.contains("呼叫") || text.startsWith("打给")) {
            val query = text
                .replace("打电话给", "")
                .replace("打电话", "")
                .replace("呼叫", "")
                .replace("打给", "")
                .replace("给", "")
            val contact = query.takeIf { it.isNotBlank() }?.let { nameQuery ->
                contacts.firstOrNull {
                    it.name.isNotBlank() && (nameQuery.contains(it.name) || it.name.contains(nameQuery))
                }
            }
            if (contact != null) return VoiceCommand.CallContact(contact)
        }

        if (text.startsWith("打开") || text.startsWith("启动")) {
            val query = text.removePrefix("打开").removePrefix("启动")
            val app = query.takeIf { it.isNotBlank() }?.let { appQuery ->
                apps.firstOrNull {
                    it.appName.isNotBlank() && (appQuery.contains(it.appName) || it.appName.contains(appQuery))
                }
            }
            if (app != null) return VoiceCommand.OpenApp(app)
        }

        return VoiceCommand.Unknown
    }
}
