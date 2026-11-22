package tech.huangsh.firstsight.viewmodel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tech.huangsh.firstsight.R
import tech.huangsh.firstsight.data.model.Contact
import tech.huangsh.firstsight.data.model.WeatherInfo
import tech.huangsh.firstsight.data.remote.WeatherService
import tech.huangsh.firstsight.data.repository.AppRepository
import tech.huangsh.firstsight.data.repository.ContactRepository
import tech.huangsh.firstsight.data.repository.ReminderRepository
import tech.huangsh.firstsight.data.repository.SettingsRepository
import tech.huangsh.firstsight.service.reminder.ReminderScheduler
import tech.huangsh.firstsight.service.wechat.FlowAbortReason
import tech.huangsh.firstsight.service.wechat.WeChatData
import tech.huangsh.firstsight.utils.BatteryStatus
import tech.huangsh.firstsight.utils.DateUtils
import tech.huangsh.firstsight.utils.FlashlightController
import tech.huangsh.firstsight.utils.FlashlightResult
import tech.huangsh.firstsight.utils.NetworkUtils
import tech.huangsh.firstsight.utils.PermissionUtils
import tech.huangsh.firstsight.utils.ToastUtils
import tech.huangsh.firstsight.utils.VoiceAssistant
import tech.huangsh.firstsight.utils.parseBatteryIntent
import tech.huangsh.firstsight.utils.readBatteryStatus
import java.util.*
import javax.inject.Inject

/**
 * 主界面ViewModel
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
    private val voiceAssistant: VoiceAssistant,
    private val weatherService: WeatherService,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
    private val flashlightController: FlashlightController,
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    
    // 联系人数据
    private val _contacts = contactRepository.allContacts
    val contacts = _contacts.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    
    // 应用数据
    private val _apps = appRepository.enabledApps
    val apps = _apps.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val reminders = reminderRepository.allReminders.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val flashlightOn = flashlightController.isOn
    
    // 时间和日期
    private val _currentTime = MutableStateFlow("")
    val currentTime = _currentTime.asStateFlow()
    
    private val _currentDate = MutableStateFlow("")
    val currentDate = _currentDate.asStateFlow()

    private val _currentWeek = MutableStateFlow("")
    val currentWeek = _currentWeek.asStateFlow()

    private val _currentLunarDate = MutableStateFlow("")
    val currentLunarDate = _currentLunarDate.asStateFlow()
    // 天气信息（与 WeatherService 共享实时状态，天气设置页切换城市后首页立即同步）
    val weatherInfo: StateFlow<WeatherInfo?> = weatherService.currentWeather
    
    // 底部菜单状态
    private val _showBottomSheet = MutableStateFlow(false)
    val showBottomSheet = _showBottomSheet.asStateFlow()
    
    private val _selectedContact = MutableStateFlow<Contact?>(null)
    val selectedContact = _selectedContact.asStateFlow()

    // 无障碍服务是否开启，决定主界面是否显示微信一键通话提示
    // 初值 true 是有意的：状态读取完成前不闪一下警示条
    private val _accessibilityEnabled = MutableStateFlow(true)
    val accessibilityEnabled = _accessibilityEnabled.asStateFlow()

    // 是否已完成一次状态读取，避免初值造成误提示
    private val _accessibilityChecked = MutableStateFlow(false)
    val accessibilityChecked = _accessibilityChecked.asStateFlow()

    // 用户是否已在本次会话中忽略无障碍提示
    private val _accessibilityReminderDismissed = MutableStateFlow(false)
    val accessibilityReminderDismissed = _accessibilityReminderDismissed.asStateFlow()

    // 当前网络是否可用（断网时禁止发起微信音视频并语音提示）
    private val _isNetworkAvailable: MutableStateFlow<Boolean> =
        MutableStateFlow(NetworkUtils.isNetworkAvailable(appContext))
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    // 当前设备是否已插卡（未插卡时禁止拨打电话并语音提示）
    private val _isSimReady: MutableStateFlow<Boolean> =
        MutableStateFlow(NetworkUtils.isSimReady(appContext))
    val isSimReady: StateFlow<Boolean> = _isSimReady.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var batteryReceiver: BroadcastReceiver? = null
    private var lastAlertedBatteryLevel: Int = Int.MAX_VALUE
    private var lastLowBatteryAlertAt: Long = 0L
    private var hasAlertedFullBattery: Boolean = false
    private var fullBatteryAlertJob: Job? = null

    init {
        viewModelScope.launch {
            contactRepository.ensurePresetContacts(settingsRepository)
        }
        updateTime()
        // 启动时更新天气
        updateWeather()
        viewModelScope.launch { reminderScheduler.scheduleAll() }
        refreshAccessibilityStatus()
        observeAccessibilitySetting()
        registerBatteryReceiver()
        networkCallback = NetworkUtils.registerNetworkCallback(appContext) { available ->
            _isNetworkAvailable.value = available
        }

        // 无障碍服务中止流程时按原因播报，让用户知道自动操作已停下
        WeChatData.onFlowAborted = { reason ->
            val messageRes = when (reason) {
                FlowAbortReason.USER_TOUCH -> R.string.wechat_flow_user_interrupted
                FlowAbortReason.TIMEOUT -> R.string.wechat_flow_timeout
                FlowAbortReason.ERROR -> R.string.wechat_flow_error
            }
            speakSystemMessage(messageRes)
        }

        // 微信来电自动接听后语音播报提示
        WeChatData.onAutoAnswered = { callerName ->
            viewModelScope.launch {
                val current = settingsRepository.settings.first()
                if (!current.voiceEnabled) return@launch
                val text = if (!callerName.isNullOrBlank()) {
                    appContext.getString(R.string.wechat_auto_answered_speech, callerName)
                } else {
                    appContext.getString(R.string.wechat_auto_answered_unknown_speech)
                }
                voiceAssistant.speak(text = text, volume = current.voiceVolume)
            }
        }

        // 同步自动接听配置与联系人白名单到无障碍状态共享对象
        viewModelScope.launch {
            settingsRepository.settings.collect { current ->
                WeChatData.autoAnswerEnabled = current.wechatAutoAnswerEnabled
                WeChatData.autoAnswerContactsOnly = current.wechatAutoAnswerContactsOnly
                WeChatData.autoSpeakerphone = current.wechatAutoSpeakerphone
                WeChatData.returnHomeAfterWechatCall = current.wechatReturnHomeAfterCall
                WeChatData.returnHomeAfterPhoneCall = current.phoneReturnHomeAfterCall
            }
        }
        viewModelScope.launch {
            contactRepository.allContacts.collect { list ->
                val nicknames = linkedSetOf<String>()
                list.forEach { contact ->
                    if (contact.trustedForAutoAnswer) {
                        contact.wechatNickname?.trim()?.takeIf { it.isNotBlank() }?.let { nicknames.add(it) }
                    }
                }
                WeChatData.allowedCallers = nicknames
            }
        }

        // 定时更新天气（每小时）
        viewModelScope.launch {
            while (true) {
                delay(60 * 60 * 1000) // 1小时
                updateWeather()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        WeChatData.onFlowAborted = null
        WeChatData.onAutoAnswered = null
        unregisterAccessibilityObserver()
        unregisterBatteryReceiver()
        stopFullBatteryAlert()
        NetworkUtils.unregisterNetworkCallback(appContext, networkCallback)
        networkCallback = null
    }

    private fun registerBatteryReceiver() {
        if (batteryReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent ?: return
                val status = parseBatteryIntent(intent, readBatteryStatus(appContext))
                checkLowBatteryAlert(status)
                checkFullBatteryAlert(status)
            }
        }
        runCatching {
            appContext.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryReceiver = receiver
        }
    }

    private fun unregisterBatteryReceiver() {
        batteryReceiver?.let {
            runCatching { appContext.unregisterReceiver(it) }
        }
        batteryReceiver = null
    }

    /**
     * 低电量语音报警检测
     *
     * 仅在未充电、总语音开关及低电量提醒开关均开启且电量 <= 阈值时触发。
     * 同一低电量区间默认只提醒一次；若继续掉电 >= 5% 且间隔 >= 10 分钟才再次提醒，
     * 充上电或电量回升到阈值之上即自动重置。
     */
    private fun checkLowBatteryAlert(status: BatteryStatus) {
        viewModelScope.launch {
            val current = settingsRepository.settings.first()
            val threshold = current.lowBatteryThreshold
            if (status.isCharging || status.level > threshold) {
                lastAlertedBatteryLevel = Int.MAX_VALUE
                return@launch
            }
            if (!current.voiceEnabled || !current.lowBatteryAlertEnabled) return@launch
            if (status.level <= 0) return@launch

            val now = SystemClock.uptimeMillis()
            val firstAlert = lastAlertedBatteryLevel == Int.MAX_VALUE
            val droppedEnough = status.level <= lastAlertedBatteryLevel - 5 &&
                now - lastLowBatteryAlertAt >= LOW_BATTERY_REPEAT_INTERVAL_MS

            if (firstAlert || droppedEnough) {
                lastAlertedBatteryLevel = status.level
                lastLowBatteryAlertAt = now
                voiceAssistant.speak(
                    text = appContext.getString(R.string.low_battery_alert_speech, status.level),
                    volume = current.voiceVolume
                )
            }
        }
    }

    /**
     * 充满电语音提醒检测
     *
     * 仅在充电中且电量达到 100%、总语音开关及充满电提醒开关均开启时触发。
     * 每次充满仅触发一次，固定播报 1 分钟（不可调整时长），拔掉充电器或关闭开关立即停止。
     */
    private fun checkFullBatteryAlert(status: BatteryStatus) {
        if (!status.isCharging || status.level < 100) {
            hasAlertedFullBattery = false
            stopFullBatteryAlert()
            return
        }

        if (hasAlertedFullBattery || fullBatteryAlertJob?.isActive == true) return

        viewModelScope.launch {
            val current = settingsRepository.settings.first()
            if (!current.voiceEnabled || !current.fullBatteryAlertEnabled) return@launch
            if (hasAlertedFullBattery || fullBatteryAlertJob?.isActive == true) return@launch

            hasAlertedFullBattery = true
            startFullBatteryAlert()
        }
    }

    private fun startFullBatteryAlert() {
        stopFullBatteryAlert()
        fullBatteryAlertJob = viewModelScope.launch(Dispatchers.IO) {
            val endAt = SystemClock.uptimeMillis() + FULL_BATTERY_ALERT_DURATION_MS
            while (isActive && SystemClock.uptimeMillis() < endAt) {
                val latestSettings = settingsRepository.settings.first()
                val latestBattery = readBatteryStatus(appContext)
                if (!latestSettings.voiceEnabled ||
                    !latestSettings.fullBatteryAlertEnabled ||
                    !latestBattery.isCharging ||
                    latestBattery.level < 100
                ) {
                    break
                }
                voiceAssistant.speak(
                    text = appContext.getString(R.string.full_battery_alert_speech),
                    volume = latestSettings.voiceVolume
                )
                delay(FULL_BATTERY_REPEAT_INTERVAL_MS)
            }
            fullBatteryAlertJob = null
        }
    }

    private fun stopFullBatteryAlert() {
        fullBatteryAlertJob?.cancel()
        fullBatteryAlertJob = null
    }

    /**
     * 播报系统提示语
     *
     * 中止类提示属于流程反馈，不受“呼叫提示”开关控制，
     * 只受语音总开关约束。
     */
    private fun speakSystemMessage(stringRes: Int) {
        viewModelScope.launch {
            val current = settingsRepository.settings.first()
            if (!current.voiceEnabled) return@launch
            voiceAssistant.speak(
                text = appContext.getString(stringRes),
                volume = current.voiceVolume
            )
        }
    }
    
    /**
     * 更新时间
     *
     * 顺带承担整点报时的检测：每秒看一次当前是第几小时，
     * 与上次播报的小时不同且分钟为 0 时触发一次。
     */
    private fun updateTime() {
        viewModelScope.launch {
            while (true) {
                val now = Date()

                _currentTime.value = DateUtils.formatTime(now)
                _currentDate.value = DateUtils.formatDate(now)
                _currentWeek.value = DateUtils.getWeekDay(now)
                _currentLunarDate.value = DateUtils.getLunar(now)

                checkHourlyChime(now)

                delay(1000)
            }
        }
    }

    /** 上次整点报时的时刻，避免同一小时内重复播报 */
    private var lastChimedAt: Long = 0L

    /**
     * 整点报时
     *
     * 只在分钟为 0 且这一小时内尚未报过时触发。
     * 记的是绝对时刻而不是小时数——只记小时数会导致第二天同一小时
     * 被判定为"已报过"而漏报。
     */
    private fun checkHourlyChime(now: Date) {
        val calendar = Calendar.getInstance().apply { time = now }
        val minute = calendar.get(Calendar.MINUTE)
        if (minute != 0) return

        // 距离上次报时不足 30 分钟就跳过，覆盖"同一分钟内循环 60 次"的情况
        val elapsed = now.time - lastChimedAt
        if (elapsed in 0 until 30 * 60 * 1000L) return

        lastChimedAt = now.time
        viewModelScope.launch {
            val current = settingsRepository.settings.first()
            // 整点报时需要语音总开关和报时开关同时开启
            if (!current.voiceEnabled || !current.hourlyChimeEnabled) return@launch
            voiceAssistant.speak(
                text = "现在时间${DateUtils.formatHourForSpeech(now)}",
                volume = current.voiceVolume
            )
        }
    }

    /**
     * 朗读卡片内容：时间、日期、农历、天气
     *
     * 点首页顶部的时间卡时调用，把这一屏上的信息完整播报一遍。
     */
    fun speakDashboard() {
        viewModelScope.launch {
            val current = settingsRepository.settings.first()
            if (!current.voiceEnabled) return@launch

            val now = Date()
            val parts = mutableListOf(
                "现在时间${DateUtils.formatTimeForSpeech(now)}",
                "${DateUtils.formatDate(now)}${DateUtils.getWeekDay(now)}",
                DateUtils.getLunar(now)
            )

            // 天气为空时跳过，避免播报"0度"；播报时连同当前所选或定位的城市一起播报
            val weather = weatherInfo.value
            if (weather != null && weather.weather.isNotBlank()) {
                val cityPrefix = weather.city.takeIf { it.isNotBlank() } ?: ""
                parts.add("${cityPrefix}天气${weather.weather}，${weather.temperature}度")
            }

            voiceAssistant.speak(
                text = parts.joinToString("，"),
                volume = current.voiceVolume
            )
        }
    }
    
    /**
     * 供首页语音指令、天气提醒等外部场景复用同一套语音开关和音量。
     */
    fun speakText(text: String) {
        speak(text)
    }

    fun speakWeather() {
        val weather = weatherInfo.value ?: return
        if (weather.weather.isBlank()) {
            speakText(appContext.getString(R.string.weather_loading))
            return
        }
        val cityPrefix = weather.city.takeIf { it.isNotBlank() } ?: ""
        speakText("${cityPrefix}天气${weather.weather}，${weather.temperature}度")
    }

    fun speakTime() {
        speakText("现在时间${DateUtils.formatTimeForSpeech(Date())}")
    }

    fun toggleFlashlight(): Boolean {
        return when (val result = flashlightController.toggle()) {
            FlashlightResult.ON -> {
                ToastUtils.show(appContext, R.string.tool_flashlight_on)
                speakText(appContext.getString(R.string.tool_flashlight_on))
                true
            }
            FlashlightResult.OFF -> {
                ToastUtils.show(appContext, R.string.tool_flashlight_off)
                speakText(appContext.getString(R.string.tool_flashlight_off))
                true
            }
            FlashlightResult.NO_FLASH -> {
                ToastUtils.show(appContext, R.string.tool_flashlight_unavailable, Toast.LENGTH_LONG)
                speakText(appContext.getString(R.string.tool_flashlight_unavailable))
                false
            }
            FlashlightResult.PERMISSION_DENIED -> {
                ToastUtils.show(appContext, R.string.tool_camera_permission_needed, Toast.LENGTH_LONG)
                speakText(appContext.getString(R.string.tool_camera_permission_needed))
                false
            }
            FlashlightResult.ERROR -> {
                ToastUtils.show(appContext, R.string.tool_flashlight_unavailable, Toast.LENGTH_LONG)
                false
            }
        }
    }
    
    /**
     * 显示联系人操作菜单
     *
     * 网络/SIM 状态刷新与 TTS 播报均异步执行，避免阻塞弹窗首帧动画造成掉帧
     */
    fun showContactActions(contact: Contact) {
        _selectedContact.value = contact
        _showBottomSheet.value = true
        viewModelScope.launch(Dispatchers.IO) {
            _isNetworkAvailable.value = NetworkUtils.isNetworkAvailable(appContext)
            _isSimReady.value = NetworkUtils.isSimReady(appContext)
        }
        // 朗读姓名，让视弱用户确认点对了人
        speak(contact.name)
    }

    /**
     * 按当前语音设置朗读文本
     *
     * 语音反馈关闭时不播报，也不产生任何开销；在 IO 线程发起跨进程 TTS 调用避免阻塞主线程渲染。
     */
    private fun speak(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settingsRepository.settings.first()
            if (!current.voiceEnabled) return@launch
            voiceAssistant.speak(
                text = text,
                volume = current.voiceVolume
            )
        }
    }
    
    /**
     * 隐藏底部菜单
     */
    fun hideBottomSheet() {
        _showBottomSheet.value = false
        _selectedContact.value = null
    }
    
    /**
     * 启动应用
     */
    fun launchApp(packageName: String): Intent? {
        return appRepository.launchApp(packageName)
    }

    /**
     * 重新读取无障碍服务状态
     *
     * 从系统设置返回时调用
     */
    fun refreshAccessibilityStatus() {
        _accessibilityEnabled.value = PermissionUtils.isAccessibilityServiceEnabled(appContext)
        _accessibilityChecked.value = true
    }

    /** 监听系统无障碍开关的观察者，仅在注册期间非空 */
    private var accessibilityObserver: ContentObserver? = null

    /**
     * 监听系统无障碍开关变化
     *
     * 部分 ROM 的"一键清理"会把本应用的无障碍服务连带禁用，用户回到桌面时
     * 开关已经是关的。只在 onResume 检查会漏掉"清理后进程被杀、重新冷启动"
     * 这一路径，因此改为监听系统设置，变化时立即刷新界面状态。
     */
    private fun observeAccessibilitySetting() {
        if (accessibilityObserver != null) return
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                refreshAccessibilityStatus()
            }
        }
        val uriOfServices =
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val uriOfEnabled = Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_ENABLED)
        val registered = runCatching {
            appContext.contentResolver.registerContentObserver(uriOfServices, false, observer)
            appContext.contentResolver.registerContentObserver(uriOfEnabled, false, observer)
            true
        }.getOrDefault(false)
        if (registered) accessibilityObserver = observer
    }

    private fun unregisterAccessibilityObserver() {
        accessibilityObserver?.let {
            runCatching { appContext.contentResolver.unregisterContentObserver(it) }
        }
        accessibilityObserver = null
    }

    /**
     * 本次会话内忽略无障碍提示
     */
    fun dismissAccessibilityReminder() {
        _accessibilityReminderDismissed.value = true
    }

    /**
     * 跳转无障碍服务设置页
     */
    fun openAccessibilitySettings() {
        PermissionUtils.openAccessibilitySettings(appContext)
    }

    // 设置入口的连点计数
    private var tapCount = 0
    private var lastTapAt = 0L

    // 累计连点达到 7 次时触发一次事件
    private val _openSettingsRequested = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openSettingsRequested = _openSettingsRequested.asSharedFlow()

    /**
     * 记录一次设置入口点击
     *
     * 连续点到第 7 次时发起跳转。相邻两次点击超过间隔就重新计数，
     * 避免用户零散点击慢慢攒够次数误入设置。
     */
    fun onSettingsEntryTap() {
        val now = SystemClock.uptimeMillis()
        if (now - lastTapAt > TAP_RESET_INTERVAL_MS) {
            tapCount = 0
        }
        lastTapAt = now
        tapCount++

        if (tapCount >= SETTINGS_TAP_THRESHOLD) {
            tapCount = 0
            _openSettingsRequested.tryEmit(Unit)
        }
    }
    
    /**
     * 拨打普通电话
     *
     * 未插卡时直接拦截并语音+文字提示；
     * 先朗读提示再发起拨号，避免用户在等待接通时反复操作手机。
     */
    fun makePhoneCall(phoneNumber: String?, contactName: String? = null): Intent? {
        if (phoneNumber == null) return null
        if (!checkSimForPhoneCall()) return null
        speakCallPrompt(
            R.string.call_prompt_phone,
            contactName
        )
        return contactRepository.makePhoneCall(phoneNumber)
    }

    fun checkSimForPhoneCall(): Boolean {
        val ready = NetworkUtils.isSimReady(appContext)
        _isSimReady.value = ready
        if (!ready) {
            speakSystemMessage(R.string.phone_sim_unavailable)
            ToastUtils.show(appContext, R.string.phone_sim_unavailable)
            return false
        }
        return true
    }

    /**
     * 发起微信视频通话
     *
     * 断网时直接拦截，并语音+文字提示无法操作；
     * 呼叫真正启动后才播报呼叫提示，避免无障碍未开启或微信未安装时误导用户。
     */
    fun startWeChatVideoCall(wechatNickname: String?, contactName: String? = null) {
        if (!checkNetworkForWeChatCall()) return
        val started = contactRepository.startWeChatVideoCall(wechatNickname)
        if (!started) return
        speakCallPrompt(
            R.string.call_prompt_wechat_video,
            contactName
        )
    }

    /**
     * 发起微信语音通话
     */
    fun startWeChatVoiceCall(wechatNickname: String?, contactName: String? = null) {
        if (!checkNetworkForWeChatCall()) return
        val started = contactRepository.startWeChatVoiceCall(wechatNickname)
        if (!started) return
        speakCallPrompt(
            R.string.call_prompt_wechat_voice,
            contactName
        )
    }

    private fun checkNetworkForWeChatCall(): Boolean {
        val available = NetworkUtils.isNetworkAvailable(appContext)
        _isNetworkAvailable.value = available
        if (!available) {
            speakSystemMessage(R.string.wechat_network_disconnected)
            ToastUtils.show(appContext, R.string.wechat_network_disconnected)
            return false
        }
        return true
    }

    /**
     * 朗读呼叫提示
     *
     * 受语音反馈和呼叫提示两个开关共同控制，任一关闭都不播报。
     */
    private fun speakCallPrompt(stringRes: Int, contactName: String?) {
        viewModelScope.launch {
            val current = settingsRepository.settings.first()
            if (!current.voiceEnabled || !current.callPromptEnabled) return@launch
            val name = contactName?.takeIf { it.isNotBlank() }
                ?: appContext.getString(R.string.call_prompt_unknown_contact)
            voiceAssistant.speak(
                text = appContext.getString(stringRes, name),
                volume = current.voiceVolume
            )
        }
    }
    
    /**
     * 更新天气信息
     *
     * 取不到真实数据时 WeatherService 会返回上一次成功的结果或空值，
     * 这里不再填充假天气，避免把 25°C 晴 显示给任何地区的用户。
     */
    fun updateWeather() {
        viewModelScope.launch {
            weatherService.getWeatherInfo()
        }
    }

    /**
     * 当天气尚未成功加载时刷新天气（如从首次引导页授权返回桌面后）
     */
    fun refreshWeatherIfNeeded() {
        val current = weatherInfo.value
        if (current == null || current.weather.isBlank()) {
            updateWeather()
        }
    }

    private companion object {
        /** 进入设置需要的连点次数 */
        const val SETTINGS_TAP_THRESHOLD = 7

        /** 相邻两次点击的最长间隔，超过则重新计数 */
        const val TAP_RESET_INTERVAL_MS = 3_000L

        /** 低电量持续掉电时的最短重复播报间隔 */
        const val LOW_BATTERY_REPEAT_INTERVAL_MS = 10 * 60 * 1000L

        /** 充满电语音提醒固定播报总时长：1 分钟（不可调整） */
        const val FULL_BATTERY_ALERT_DURATION_MS = 60_000L

        /** 充满电语音提醒轮询播报间隔 */
        const val FULL_BATTERY_REPEAT_INTERVAL_MS = 5_000L
    }
}
