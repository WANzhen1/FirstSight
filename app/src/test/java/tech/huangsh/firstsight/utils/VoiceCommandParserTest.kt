package tech.huangsh.firstsight.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.huangsh.firstsight.data.model.AppInfo
import tech.huangsh.firstsight.data.model.Contact
import tech.huangsh.firstsight.data.model.Reminder
import java.util.Calendar

class VoiceCommandParserTest {

    private val contacts = listOf(
        Contact(id = 1, name = "女儿", phone = "13800000000"),
        Contact(id = 2, name = "儿子", phone = "13900000000")
    )

    private val apps = listOf(
        AppInfo(packageName = "com.tencent.mm", appName = "微信"),
        AppInfo(packageName = "com.tencent.mobileqq", appName = "QQ")
    )

    @Test
    fun parsesPhoneCallByContactName() {
        val command = VoiceCommandParser.parse("给女儿打电话", contacts, apps)
        assertTrue(command is VoiceCommand.CallContact)
        assertEquals("女儿", (command as VoiceCommand.CallContact).contact.name)
    }

    @Test
    fun parsesOpenApp() {
        val command = VoiceCommandParser.parse("打开微信", contacts, apps)
        assertTrue(command is VoiceCommand.OpenApp)
        assertEquals("微信", (command as VoiceCommand.OpenApp).app.appName)
    }

    @Test
    fun parsesWeatherAndTime() {
        assertTrue(VoiceCommandParser.parse("今天天气怎么样", contacts, apps) is VoiceCommand.SpeakWeather)
        assertTrue(VoiceCommandParser.parse("现在几点", contacts, apps) is VoiceCommand.SpeakTime)
    }

    @Test
    fun parsesBuiltInTools() {
        assertTrue(VoiceCommandParser.parse("打开手电筒", contacts, apps) is VoiceCommand.ToggleFlashlight)
        assertTrue(VoiceCommandParser.parse("打开放大镜", contacts, apps) is VoiceCommand.OpenMagnifier)
        assertTrue(VoiceCommandParser.parse("打开计算器", contacts, apps) is VoiceCommand.OpenCalculator)
    }

    @Test
    fun unknownCommandDoesNothing() {
        assertTrue(VoiceCommandParser.parse("今天吃什么", contacts, apps) is VoiceCommand.Unknown)
    }

    @Test
    fun blankCallQueryDoesNotPickFirstContact() {
        assertTrue(VoiceCommandParser.parse("打电话", contacts, apps) is VoiceCommand.Unknown)
    }

    @Test
    fun blankOpenQueryDoesNotPickFirstApp() {
        assertTrue(VoiceCommandParser.parse("打开", contacts, apps) is VoiceCommand.Unknown)
    }
}

class ReminderTimeUtilsTest {

    @Test
    fun dailyReminderFindsNextOccurrence() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val reminder = Reminder(
            title = "吃降压药",
            hour = 8,
            minute = 30,
            repeatDays = Reminder.ALL_DAYS
        )

        val next = ReminderTimeUtils.nextTriggerAt(reminder, now)
        assertTrue(next != null && next > now)
        val nextCalendar = Calendar.getInstance().apply { timeInMillis = next!! }
        assertEquals(8, nextCalendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, nextCalendar.get(Calendar.MINUTE))
    }

    @Test
    fun pastOneTimeReminderIsNotScheduled() {
        val now = System.currentTimeMillis()
        val reminder = Reminder(
            title = "复诊",
            hour = 8,
            minute = 0,
            repeatDays = 0,
            onceAt = now - 60_000L
        )
        assertEquals(null, ReminderTimeUtils.nextTriggerAt(reminder, now))
    }

    @Test
    fun disabledReminderIsNotScheduled() {
        val reminder = Reminder(
            title = "吃药",
            hour = 8,
            minute = 0,
            enabled = false
        )
        assertEquals(null, ReminderTimeUtils.nextTriggerAt(reminder))
    }
}
