package tech.huangsh.firstsight.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import com.google.android.accessibility.selecttospeak.SelectToSpeakService
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions

/**
 * 权限统一管理工具
 *
 * 把权限分成两类：
 * - 运行时权限（电话、通讯录、相机、位置、通知）：可以通过系统弹窗申请
 * - 特殊权限（无障碍服务、悬浮窗、忽略电池优化）：只能跳系统设置由用户手动开启
 */
object PermissionUtils {

    /**
     * 首次启动引导中通过弹窗申请的运行时权限
     *
     * 通知权限单独处理，因为它只在 Android 13+ 才存在
     */
    fun runtimePermissions(): Array<String> {
        val permissions = mutableListOf(
            Permission.CALL_PHONE,
            Permission.READ_CONTACTS,
            Permission.CAMERA,
            Permission.ACCESS_FINE_LOCATION,
            Permission.ACCESS_COARSE_LOCATION,
            Permission.GET_INSTALLED_APPS
        )
        return permissions.toTypedArray()
    }

    /**
     * 运行时权限是否全部授予
     */
    fun areRuntimePermissionsGranted(context: Context): Boolean {
        return deniedRuntimePermissions(context).isEmpty()
    }

    /**
     * 未授予的运行时权限
     */
    fun deniedRuntimePermissions(context: Context): List<String> {
        return XXPermissions.getDenied(context, runtimePermissions())
    }

    /**
     * 无障碍服务是否已开启
     *
     * 优先读 ENABLED_ACCESSIBILITY_SERVICES，同时兼容部分 ROM 上 ACCESSIBILITY_ENABLED 为 0
     * 但服务实际已启用的情形，因此两个条件取或。
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val serviceId = ComponentName(context, SelectToSpeakService::class.java)
        val flatServiceId = serviceId.flattenToString()

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()

        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabledServices) }
        var found = false
        while (splitter.hasNext()) {
            val service = splitter.next()
            if (service.equals(flatServiceId, ignoreCase = true) ||
                service.equals(serviceId.flattenToShortString(), ignoreCase = true)
            ) {
                found = true
                break
            }
        }
        if (!found) return false

        // 有些 ROM 即使服务已在列表中，ACCESSIBILITY_ENABLED 也可能为 0
        val accessibilityEnabled = try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            0
        }
        return accessibilityEnabled == 1 || enabledServices.isNotEmpty()
    }

    /**
     * 是否已忽略电池优化
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * 跳转无障碍服务设置页
     */
    fun openAccessibilitySettings(context: Context) {
        startSafely(
            context,
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    /**
     * 跳转电池优化忽略页，失败时退回电池优化列表
     */
    fun openBatteryOptimizationSettings(context: Context) {
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (!startSafely(context, direct)) {
            val list = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (!startSafely(context, list)) {
                startSafely(context, appDetailsIntent(context))
            }
        }
    }

    /**
     * 跳转省电策略设置页（优先尝试小米省电配置页，失败时回退到系统忽略电池优化/应用详情页）
     */
    fun openPowerPolicySettings(context: Context) {
        val miuiPowerIntent = Intent().apply {
            setClassName(
                "com.miui.powerkeeper",
                "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
            )
            putExtra("package_name", context.packageName)
            putExtra("package_label", context.applicationInfo.loadLabel(context.packageManager).toString())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (!startSafely(context, miuiPowerIntent)) {
            openBatteryOptimizationSettings(context)
        }
    }

    /**
     * 跳转自启动管理页（兼容主流国产 ROM，失败时退回本应用详情页）
     */
    fun openAutoStartSettings(context: Context) {
        val candidates = listOf(
            // 小米 / Redmi / HyperOS
            Intent().setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            ),
            // 华为 / 荣耀
            Intent().setClassName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            ),
            Intent().setClassName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
            ),
            // OPPO / 一加 / realme
            Intent().setClassName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            ),
            Intent().setClassName(
                "com.oplus.safecenter",
                "com.oplus.safecenter.permission.startup.StartupAppListActivity"
            ),
            // vivo / iQOO
            Intent().setClassName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )
        )
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (startSafely(context, intent)) return
        }
        startSafely(context, appDetailsIntent(context))
    }

    /**
     * 本应用详情页
     */
    fun appDetailsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun startSafely(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
