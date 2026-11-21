package tech.huangsh.firstsight.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tech.huangsh.firstsight.data.repository.SettingsRepository
import tech.huangsh.firstsight.utils.PermissionUtils
import javax.inject.Inject

/**
 * 首次启动引导的权限状态
 */
data class OnboardingState(
    val runtimeGranted: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val powerPolicyConfigured: Boolean = false,
    val autoStartConfigured: Boolean = false
) {
    val allGranted: Boolean
        get() = runtimeGranted &&
            accessibilityEnabled &&
            powerPolicyConfigured &&
            autoStartConfigured
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    /** 记录本次跳转的系统设置类型，返回时自动更新对应卡片状态 */
    private var pendingGuideAction: String? = null

    init {
        refreshPermissions()
    }

    /**
     * 重新读取所有权限状态
     *
     * 从系统设置页返回时会调用，保证界面显示的是最新状态
     */
    fun refreshPermissions() {
        viewModelScope.launch {
            when (pendingGuideAction) {
                ACTION_POWER -> {
                    settingsRepository.updatePowerPolicyConfigured(true)
                    pendingGuideAction = null
                }
                ACTION_AUTOSTART -> {
                    settingsRepository.updateAutoStartConfigured(true)
                    pendingGuideAction = null
                }
            }

            val powerConfigured = settingsRepository.powerPolicyConfigured.first()
            val autoStartConfigured = settingsRepository.autoStartConfigured.first()
            val batteryIgnored = PermissionUtils.isIgnoringBatteryOptimizations(context)

            _state.value = OnboardingState(
                runtimeGranted = PermissionUtils.areRuntimePermissionsGranted(context),
                accessibilityEnabled = PermissionUtils.isAccessibilityServiceEnabled(context),
                powerPolicyConfigured = powerConfigured || batteryIgnored,
                autoStartConfigured = autoStartConfigured
            )
        }
    }

    fun openAccessibilitySettings() {
        PermissionUtils.openAccessibilitySettings(context)
    }

    fun openPowerPolicySettings() {
        pendingGuideAction = ACTION_POWER
        PermissionUtils.openPowerPolicySettings(context)
    }

    fun openAutoStartSettings() {
        pendingGuideAction = ACTION_AUTOSTART
        PermissionUtils.openAutoStartSettings(context)
    }

    /**
     * 标记引导完成
     */
    fun completeOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.updateOnboardingCompleted(true)
            onDone()
        }
    }

    private companion object {
        const val ACTION_POWER = "power"
        const val ACTION_AUTOSTART = "autostart"
    }
}
