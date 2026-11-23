package tech.huangsh.firstsight.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import java.util.Calendar

/**
 * 打开系统闹钟。
 *
 * 部分厂商时钟没有注册 AlarmClock.ACTION_SET_ALARM，或只注册了 SHOW_ALARMS，
 * 因此按“创建闹钟 → 打开闹钟列表 → 已知时钟包 → 已安装 clock/alarm 包”的顺序回退。
 */
object AlarmLauncher {

    private val knownClockPackages = listOf(
        "com.android.deskclock",
        "com.google.android.deskclock",
        "com.sec.android.app.clockpackage",
        "com.miui.clock",
        "com.huawei.deskclock",
        "com.hihonor.deskclock",
        "com.coloros.alarmclock",
        "com.oplus.alarmclock",
        "com.oneplus.deskclock",
        "com.vivo.alarmclock",
        "com.meizu.flyme.alarmclock",
        "com.transsion.clock",
        "com.mediatek.deskclock"
    )

    private val clockKeywords = listOf("clock", "alarm", "deskclock")

    fun openAlarm(context: Context): Boolean {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val packageManager = context.packageManager

        val packageCandidates = LinkedHashSet<String>()
        packageCandidates.addAll(knownClockPackages)

        runCatching {
            packageManager.getInstalledPackages(0)
                .asSequence()
                .map { it.packageName }
                .filter { it != context.packageName }
                .filter { packageName ->
                    clockKeywords.any { keyword -> packageName.contains(keyword, ignoreCase = true) }
                }
                .forEach { packageCandidates.add(it) }
        }

        val intents = mutableListOf<Intent>()
        intents += createAlarmIntent(hour, minute)
        intents += Intent(AlarmClock.ACTION_SHOW_ALARMS)

        packageCandidates.forEach { packageName ->
            intents += createAlarmIntent(hour, minute).setPackage(packageName)
            intents += Intent(AlarmClock.ACTION_SHOW_ALARMS).setPackage(packageName)
            packageManager.getLaunchIntentForPackage(packageName)?.let { intents += it }
        }

        for (intent in intents) {
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val started = runCatching {
                context.startActivity(intent)
                true
            }.getOrDefault(false)
            if (started) return true
        }
        return false
    }

    private fun createAlarmIntent(hour: Int, minute: Int): Intent {
        return Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        }
    }
}
