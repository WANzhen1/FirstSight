package tech.huangsh.firstsight.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import dagger.hilt.android.AndroidEntryPoint
import tech.huangsh.firstsight.data.model.Settings
import tech.huangsh.firstsight.ui.screens.onboarding.OnboardingScreen
import tech.huangsh.firstsight.ui.theme.FirstSightTheme
import tech.huangsh.firstsight.utils.PermissionUtils
import tech.huangsh.firstsight.viewmodel.OnboardingViewModel
import tech.huangsh.firstsight.viewmodel.SettingsViewModel

/**
 * 首次启动引导页
 *
 * 安装后第一次启动时展示，一次性完成电话、通讯录、相机、位置、通知等运行时权限申请，
 * 并引导用户开启无障碍服务、悬浮窗和忽略电池优化。
 */
@AndroidEntryPoint
class OnboardingActivity : ComponentActivity() {

    private val viewModel: OnboardingViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 未点击“开始使用”中途按返回键时，将整个应用退回后台而非关闭引导页，
        // 避免退到底下 MainActivity 的加载占位界面
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })

        setContent {
            val settings by settingsViewModel.settings.collectAsState(initial = Settings())
            val state by viewModel.state.collectAsState()

            FirstSightTheme(
                themeMode = settings.themeMode
            ) {
                OnboardingScreen(
                    state = state,
                    onRequestRuntimePermissions = { requestRuntimePermissions() },
                    onOpenAccessibilitySettings = { viewModel.openAccessibilitySettings() },
                    onOpenPowerPolicySettings = { viewModel.openPowerPolicySettings() },
                    onOpenAutoStartSettings = { viewModel.openAutoStartSettings() },
                    onStart = { finishOnboarding() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 从系统设置页返回时重新读取权限状态
        viewModel.refreshPermissions()
    }

    /**
     * 一次性申请所有运行时权限
     */
    private fun requestRuntimePermissions() {
        XXPermissions.with(this)
            .permission(PermissionUtils.runtimePermissions())
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                    viewModel.refreshPermissions()
                }

                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                    viewModel.refreshPermissions()
                }
            })
    }

    /**
     * 完成引导并进入主界面
     *
     * 即使还有权限没开也允许继续，未开启的项目会在主界面持续提示
     */
    private fun finishOnboarding() {
        viewModel.completeOnboarding {
            // MainActivity 已在引导页下方常驻，直接返回即可，无需重新启动
            finish()
        }
    }
}
