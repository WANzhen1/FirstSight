package tech.huangsh.firstsight.ui.screens.home

import android.app.Activity
import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import tech.huangsh.firstsight.ui.theme.FirstSightIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.sp
import tech.huangsh.firstsight.R
import tech.huangsh.firstsight.data.model.AppInfo
import tech.huangsh.firstsight.data.model.Reminder
import tech.huangsh.firstsight.ui.activity.ReminderActivity
import tech.huangsh.firstsight.ui.activity.SettingsActivity
import tech.huangsh.firstsight.ui.activity.ToolsActivity
import tech.huangsh.firstsight.ui.screens.components.ContactActionBottomSheet
import tech.huangsh.firstsight.ui.screens.components.ContactItem
import tech.huangsh.firstsight.ui.theme.FirstSightStyle
import tech.huangsh.firstsight.ui.theme.FirstSightTheme
import tech.huangsh.firstsight.utils.AlarmLauncher
import tech.huangsh.firstsight.utils.ImageUtils
import tech.huangsh.firstsight.utils.ReminderTimeUtils
import tech.huangsh.firstsight.utils.ToastUtils
import tech.huangsh.firstsight.utils.VoiceCommand
import tech.huangsh.firstsight.utils.VoiceCommandParser
import tech.huangsh.firstsight.utils.WeatherAlert
import tech.huangsh.firstsight.utils.WeatherAlertUtils
import tech.huangsh.firstsight.utils.rememberBatteryStatus
import tech.huangsh.firstsight.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsState()
    val apps by viewModel.apps.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState() // 时间
    val currentDate by viewModel.currentDate.collectAsState() // 日期
    val currentWeek by viewModel.currentWeek.collectAsState() // 星期
    val currentLunarDate by viewModel.currentLunarDate.collectAsState() // 农历日期
    val weatherInfo by viewModel.weatherInfo.collectAsState() // 天气
    val showBottomSheet by viewModel.showBottomSheet.collectAsState()
    val selectedContact by viewModel.selectedContact.collectAsState()
    val accessibilityEnabled by viewModel.accessibilityEnabled.collectAsState()
    val accessibilityChecked by viewModel.accessibilityChecked.collectAsState()
    val accessibilityDismissed by viewModel.accessibilityReminderDismissed.collectAsState()
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState()
    val isSimReady by viewModel.isSimReady.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val flashlightOn by viewModel.flashlightOn.collectAsState()

    val weatherAlert = remember(weatherInfo) { WeatherAlertUtils.fromWeather(weatherInfo) }
    val alertPrefs = remember { context.getSharedPreferences("weather_alert", Context.MODE_PRIVATE) }
    var dismissedWeatherAlert by remember { mutableStateOf(alertPrefs.getString("dismissed", null)) }
    val todayKey = remember(currentDate) { SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(Date()) }
    val weatherAlertKey = weatherAlert?.let { "$todayKey|${it.key}" }
    val nextReminder = remember(reminders, currentTime) {
        val now = System.currentTimeMillis()
        reminders.asSequence()
            .filter { it.enabled }
            .mapNotNull { reminder ->
                ReminderTimeUtils.nextTriggerAt(reminder, now)?.let { triggerAt -> reminder to triggerAt }
            }
            .minByOrNull { it.second }
    }

    LaunchedEffect(weatherAlertKey) {
        val alert = weatherAlert
        if (alert != null && weatherAlertKey != null) {
            val spoken = alertPrefs.getString("spoken", null)
            if (spoken != weatherAlertKey) {
                viewModel.speakText(alert.message)
                alertPrefs.edit().putString("spoken", weatherAlertKey).apply()
            }
        }
    }

    // 连点 7 次后先弹窗确认，确认后才进入设置
    var showSettingsConfirm by remember { mutableStateOf(false) }
    var pendingPhoneNumber by remember { mutableStateOf<String?>(null) }
    var pendingPhoneName by remember { mutableStateOf<String?>(null) }
    val settingsAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            context.startActivity(Intent(context, SettingsActivity::class.java))
        }
    }
    fun openSystemAlarm() {
        if (!AlarmLauncher.openAlarm(context)) {
            ToastUtils.show(context, R.string.tool_alarm_unavailable, Toast.LENGTH_LONG)
        }
    }

    fun handleVoiceCommand(rawText: String) {
        when (val command = VoiceCommandParser.parse(rawText, contacts, apps)) {
            is VoiceCommand.CallContact -> {
                val phone = command.contact.phone?.trim()
                if (phone.isNullOrBlank()) {
                    viewModel.speakText(context.getString(R.string.voice_command_no_phone, command.contact.name))
                    ToastUtils.show(context, context.getString(R.string.voice_command_no_phone, command.contact.name))
                } else {
                    pendingPhoneNumber = phone
                    pendingPhoneName = command.contact.name
                    ToastUtils.show(context, context.getString(R.string.voice_command_call_confirm, command.contact.name))
                }
            }
            is VoiceCommand.OpenApp -> {
                val intent = viewModel.launchApp(command.app.packageName)
                if (intent != null) {
                    runCatching { context.startActivity(intent) }
                } else {
                    viewModel.speakText(context.getString(R.string.voice_command_unknown))
                }
            }
            VoiceCommand.SpeakWeather -> viewModel.speakWeather()
            VoiceCommand.SpeakTime -> viewModel.speakTime()
            VoiceCommand.OpenReminders -> context.startActivity(Intent(context, ReminderActivity::class.java))
            VoiceCommand.OpenTools -> context.startActivity(ToolsActivity.createIntent(context))
            VoiceCommand.ToggleFlashlight -> viewModel.toggleFlashlight()
            VoiceCommand.OpenMagnifier -> context.startActivity(
                ToolsActivity.createIntent(context, ToolsActivity.TOOL_MAGNIFIER)
            )
            VoiceCommand.OpenCalculator -> context.startActivity(
                ToolsActivity.createIntent(context, ToolsActivity.TOOL_CALCULATOR)
            )
            VoiceCommand.SetAlarm -> openSystemAlarm()
            VoiceCommand.Unknown -> {
                viewModel.speakText(context.getString(R.string.voice_command_unknown))
                ToastUtils.show(context, R.string.voice_command_unknown)
            }
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                handleVoiceCommand(spokenText)
            }
        }
    }

    fun startVoiceCommand() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.voice_command_prompt))
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            ToastUtils.show(context, R.string.voice_command_unavailable, Toast.LENGTH_LONG)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.openSettingsRequested.collect {
            showSettingsConfirm = true
        }
    }
    
    // 使用XXPermissions申请电话权限并拨打电话
    fun requestPhonePermissionAndCall(phoneNumber: String, contactName: String?) {
        XXPermissions.with(context as androidx.activity.ComponentActivity)
            .permission(Permission.CALL_PHONE)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                    if (allGranted) {
                        // 权限已授予，执行打电话
                        val intent = viewModel.makePhoneCall(phoneNumber, contactName)
                        intent?.let { context.startActivity(it) }
                    }
                }
                
                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                    // 权限被拒绝，显示提示
                    if (doNotAskAgain) {
                        ToastUtils.show(context, "请在设置中开启电话权限才能拨打电话", Toast.LENGTH_LONG)
                        // 可以引导用户到设置页面
                        XXPermissions.startPermissionActivity(context, permissions)
                    } else {
                        ToastUtils.show(context, "需要电话权限才能拨打电话")
                    }
                }
            })
    }

    fun openSettingsWithAuth() {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguard?.isDeviceSecure == true) {
            val intent = keyguard.createConfirmDeviceCredentialIntent(
                context.getString(R.string.settings_auth_title),
                context.getString(R.string.settings_auth_message)
            )
            if (intent != null) {
                settingsAuthLauncher.launch(intent)
                return
            }
        }
        context.startActivity(Intent(context, SettingsActivity::class.java))
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FirstSightStyle.pageGradient()),
        // 透明容器色，否则会盖住上面的渐变背景
        containerColor = Color.Transparent,
        // 不自动注入系统状态栏和导航栏安全区留白，使顶部卡片贴近顶部、底部列表延伸至屏幕底边
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 顶部时间 + 天气 + 电量（电量同时是隐藏的设置入口）
                Card(
                    shape = FirstSightStyle.cardLarge,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 16.dp,
                            shape = FirstSightStyle.cardLarge,
                            ambientColor = FirstSightStyle.primaryShadow(),
                            spotColor = FirstSightStyle.primaryShadow()
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FirstSightStyle.primaryGradient())
                            .clip(FirstSightStyle.cardLarge)
                    ) {
                        // 两层柔光：右上大光斑 + 左下小光斑，替代纯色板带来的平塌感
                        // 光晕强度走 FirstSightStyle，深色模式下会自动减弱
                        val glowStrong = FirstSightStyle.heroGlowColor(strong = true)
                        val glowSoft = FirstSightStyle.heroGlowColor(strong = false)
                        Canvas(modifier = Modifier.matchParentSize()) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(glowStrong, Color.Transparent),
                                    center = Offset(size.width * 0.92f, size.height * 0.06f),
                                    radius = size.width * 0.55f
                                ),
                                radius = size.width * 0.55f,
                                center = Offset(size.width * 0.92f, size.height * 0.06f)
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(glowSoft, Color.Transparent),
                                    center = Offset(size.width * 0.04f, size.height * 1.02f),
                                    radius = size.width * 0.34f
                                ),
                                radius = size.width * 0.34f,
                                center = Offset(size.width * 0.04f, size.height * 1.02f)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                // 点击卡片朗读时间、日期、农历和天气
                                // 放在内容区而不是外层 Box，避免盖住右上角的电量按钮
                                .clickable { viewModel.speakDashboard() }
                                .padding(22.dp)
                        ) {
                            Text(
                                currentTime,
                                fontSize = 56.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-2.5).sp
                            )
                            Spacer(Modifier.height(9.dp))
                            Text(
                                "$currentDate $currentWeek",
                                // 用主题字号才能在“大字号”设置下同步放大
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.94f),
                                fontWeight = FontWeight.SemiBold
                            )
                            // 天气拿不到时只显示农历，避免出现孤零零的 "°C"
                            val weather = weatherInfo
                            val weatherText = if (weather != null && weather.weather.isNotBlank()) {
                                "$currentLunarDate  ${weather.weatherIcon} ${weather.weather} ${weather.temperature}°C"
                            } else {
                                currentLunarDate
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                weatherText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }

                        BatteryIndicator(
                            onClick = { viewModel.onSettingsEntryTap() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val alert = weatherAlert
            if (alert != null && weatherAlertKey != dismissedWeatherAlert) {
                WeatherAlertCard(
                    alert = alert,
                    onDismiss = {
                        weatherAlertKey?.let { key ->
                            dismissedWeatherAlert = key
                            alertPrefs.edit().putString("dismissed", key).apply()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            val next = nextReminder
            if (next != null) {
                HomeReminderCard(
                    reminder = next.first,
                    triggerAt = next.second,
                    onClick = { context.startActivity(Intent(context, ReminderActivity::class.java)) },
                    onSpeak = { viewModel.speakText(ReminderTimeUtils.buildTextForSpeech(next.first)) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 无障碍服务未开启时持续提示，否则微信一键通话无法使用
            if (accessibilityChecked && !accessibilityEnabled && !accessibilityDismissed) {
                AccessibilityReminderCard(
                    onOpenSettings = { viewModel.openAccessibilitySettings() },
                    onDismiss = { viewModel.dismissAccessibilityReminder() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 常用APP（最多5个）
            // 没有选中任何常用应用时整块隐藏，避免留下空白占位
            if (apps.isNotEmpty()) {
                Card(
                    shape = FirstSightStyle.cardLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        apps.take(5).forEach {
                            AppCard(it, onClick = { packageName ->
                                val intent = viewModel.launchApp(packageName)
                                intent?.let { app -> context.startActivity(app) }
                            })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            QuickToolsCard(
                flashlightOn = flashlightOn,
                onVoice = { startVoiceCommand() },
                onFlashlight = { viewModel.toggleFlashlight() },
                onMagnifier = {
                    context.startActivity(ToolsActivity.createIntent(context, ToolsActivity.TOOL_MAGNIFIER))
                },
                onCalculator = {
                    context.startActivity(ToolsActivity.createIntent(context, ToolsActivity.TOOL_CALCULATOR))
                },
                onAlarm = { openSystemAlarm() }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 联系人网格 (每行2个)，延伸至屏幕最底部不留白
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(13.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                contentPadding = PaddingValues(bottom = 0.dp)
            ) {
                itemsIndexed(
                    items = contacts,
                    key = { _, contact -> contact.id }
                ) { index, contact ->
                    ContactItem(
                        contact = contact,
                        onClick = { viewModel.showContactActions(contact) },
                        isHomeScreen = true,
                        avatarIndex = index
                    )
                }
            }
        }

        // 底部菜单（使用同窗口内联动画层，避免 ModalBottomSheet 每次新建 DialogWindow 导致掉帧）
        ContactActionBottomSheet(
            visible = showBottomSheet && selectedContact != null,
            contact = selectedContact,
            isNetworkAvailable = isNetworkAvailable,
            isSimReady = isSimReady,
            onDismiss = { viewModel.hideBottomSheet() },
            onVideoCall = {
                val contact = selectedContact ?: return@ContactActionBottomSheet
                viewModel.startWeChatVideoCall(
                    contact.wechatNickname,
                    contact.name
                )
                if (isNetworkAvailable) {
                    viewModel.hideBottomSheet()
                }
            },
            onVoiceCall = {
                val contact = selectedContact ?: return@ContactActionBottomSheet
                viewModel.startWeChatVoiceCall(
                    contact.wechatNickname,
                    contact.name
                )
                if (isNetworkAvailable) {
                    viewModel.hideBottomSheet()
                }
            },
            onPhoneCall = { phone ->
                val contact = selectedContact ?: return@ContactActionBottomSheet
                if (viewModel.checkSimForPhoneCall()) {
                    pendingPhoneNumber = phone
                    pendingPhoneName = contact.name
                    viewModel.hideBottomSheet()
                }
            },
            onCancelCall = {
                viewModel.hideBottomSheet()
            }
        )

        // 连点 7 次后的进入设置确认弹窗
        if (showSettingsConfirm) {
            AlertDialog(
                onDismissRequest = { showSettingsConfirm = false },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                title = {
                    Text(
                        text = stringResource(R.string.settings_confirm_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.settings_confirm_message),
                        fontSize = 18.sp,
                        lineHeight = 27.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSettingsConfirm = false
                            openSettingsWithAuth()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.confirm),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSettingsConfirm = false }) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontSize = 18.sp
                        )
                    }
                }
            )
        }

        if (pendingPhoneNumber != null) {
            val number = pendingPhoneNumber ?: ""
            val contactName = pendingPhoneName ?: ""
            AlertDialog(
                onDismissRequest = {
                    pendingPhoneNumber = null
                    pendingPhoneName = null
                },
                title = {
                    Text(
                        text = stringResource(R.string.phone_confirm_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.phone_confirm_message, contactName, number),
                        fontSize = 20.sp,
                        lineHeight = 30.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingPhoneNumber = null
                            pendingPhoneName = null
                            requestPhonePermissionAndCall(number, contactName)
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.confirm),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            pendingPhoneNumber = null
                            pendingPhoneName = null
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontSize = 20.sp
                        )
                    }
                }
            )
        }
    }
}

/**
 * 右上角电量显示
 *
 * 同时是隐藏的设置入口：连点 7 次才会跳转设置页。
 */
@Composable
fun BatteryIndicator(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val battery by rememberBatteryStatus()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(Color.White.copy(alpha = 0.20f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BatteryGlyph(
            level = battery.level,
            isCharging = battery.isCharging,
            modifier = Modifier.size(width = 34.dp, height = 16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${battery.level}%",
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp
        )
    }
}

/**
 * 横向电池图形
 *
 * 用 Canvas 手绘而不是用系统图标：需要按真实电量填充比例，
 * 且要在充电时叠加闪电标记，现成图标无法同时表达这两件事。
 */
@Composable
private fun BatteryGlyph(
    level: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val outline = MaterialTheme.colorScheme.onPrimary
    Canvas(modifier = modifier) {
        val capWidth = size.width * 0.09f
        val bodyWidth = size.width - capWidth - 1.dp.toPx()
        val bodyHeight = size.height
        val corner = bodyHeight * 0.28f
        val strokeWidth = 1.6.dp.toPx()
        val inset = strokeWidth / 2f

        // 电池外壳
        drawRoundRect(
            color = outline,
            topLeft = Offset(inset, inset),
            size = Size(bodyWidth - strokeWidth, bodyHeight - strokeWidth),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = strokeWidth)
        )

        // 正极凸起
        drawRoundRect(
            color = outline,
            topLeft = Offset(bodyWidth, bodyHeight * 0.30f),
            size = Size(capWidth, bodyHeight * 0.40f),
            cornerRadius = CornerRadius(capWidth * 0.6f, capWidth * 0.6f)
        )

        // 电量填充，留出内边距避免压住外壳描边
        val padding = strokeWidth * 1.6f
        val fillMaxWidth = bodyWidth - padding * 2
        val ratio = (level.coerceIn(0, 100)) / 100f
        val fillWidth = (fillMaxWidth * ratio).coerceAtLeast(if (ratio > 0f) padding else 0f)

        // 低电量转红，充电时转绿，其余保持白色
        val fillColor = when {
            isCharging -> Color(0xFF7BF1A8)
            level <= 20 -> Color(0xFFFF8A80)
            else -> outline
        }

        if (fillWidth > 0f) {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(padding, padding),
                size = Size(fillWidth, bodyHeight - padding * 2),
                cornerRadius = CornerRadius(corner * 0.5f, corner * 0.5f)
            )
        }

        // 充电时的闪电标记
        if (isCharging) {
            val cx = bodyWidth * 0.52f
            val cy = bodyHeight / 2f
            val w = bodyWidth * 0.16f
            val h = bodyHeight * 0.30f
            val bolt = Path().apply {
                moveTo(cx + w * 0.30f, cy - h)
                lineTo(cx - w * 0.55f, cy + h * 0.12f)
                lineTo(cx - w * 0.02f, cy + h * 0.12f)
                lineTo(cx - w * 0.32f, cy + h)
                lineTo(cx + w * 0.55f, cy - h * 0.10f)
                lineTo(cx + w * 0.04f, cy - h * 0.10f)
                close()
            }
            drawPath(bolt, color = Color(0xFF0B2B16))
        }
    }
}

@Composable
fun AccessibilityReminderCard(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FirstSightStyle.cardMedium,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FirstSightStyle.alertGradient())
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(FirstSightStyle.small)
                    .background(FirstSightStyle.alertAccent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "!",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.accessibility_reminder_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = FirstSightStyle.alertTextColor()
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.accessibility_reminder_desc),
                    fontSize = 12.5.sp,
                    color = FirstSightStyle.alertTextSubColor()
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(FirstSightStyle.small)
                        .background(FirstSightStyle.alertAccent)
                        .clickable { onOpenSettings() }
                        .padding(horizontal = 15.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = stringResource(R.string.accessibility_reminder_action),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(2.dp))
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.accessibility_reminder_later),
                        color = FirstSightStyle.alertTextSubColor(),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun QuickToolsCard(
    flashlightOn: Boolean,
    onVoice: () -> Unit,
    onFlashlight: () -> Unit,
    onMagnifier: () -> Unit,
    onCalculator: () -> Unit,
    onAlarm: () -> Unit
) {
    Card(
        shape = FirstSightStyle.cardLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickToolItem(
                icon = FirstSightIcons.Mic,
                label = stringResource(R.string.tool_voice),
                onClick = onVoice
            )
            QuickToolItem(
                icon = FirstSightIcons.Flashlight,
                label = stringResource(R.string.tool_flashlight),
                active = flashlightOn,
                onClick = onFlashlight
            )
            QuickToolItem(
                icon = FirstSightIcons.Search,
                label = stringResource(R.string.tool_magnifier),
                onClick = onMagnifier
            )
            QuickToolItem(
                icon = FirstSightIcons.Calculate,
                label = stringResource(R.string.tool_calculator),
                onClick = onCalculator
            )
            QuickToolItem(
                icon = FirstSightIcons.Schedule,
                label = stringResource(R.string.tool_alarm),
                onClick = onAlarm
            )
        }
    }
}

@Composable
private fun QuickToolItem(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(FirstSightStyle.small)
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(
                    if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(26.dp),
                tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun WeatherAlertCard(
    alert: WeatherAlert,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FirstSightStyle.cardMedium,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FirstSightStyle.alertGradient())
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(FirstSightStyle.small)
                    .background(FirstSightStyle.alertAccent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "!",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = FirstSightStyle.alertTextColor()
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = alert.message,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = FirstSightStyle.alertTextSubColor()
                )
            }

            Spacer(Modifier.width(6.dp))

            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.weather_alert_dismiss),
                    color = FirstSightStyle.alertTextColor(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun HomeReminderCard(
    reminder: Reminder,
    triggerAt: Long,
    onClick: () -> Unit,
    onSpeak: () -> Unit
) {
    Card(
        shape = FirstSightStyle.cardMedium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FirstSightIcons.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (reminder.isMedication) R.string.reminder_medication_card
                        else R.string.reminder_schedule_card
                    ),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = reminder.title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${ReminderTimeUtils.formatNextTrigger(triggerAt)} · ${ReminderTimeUtils.repeatLabel(reminder)}",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onSpeak) {
                Text(
                    text = stringResource(R.string.reminder_speak),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun AppCard(app: AppInfo, onClick: (String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick(app.packageName) }
    ) {
        Card(
            shape = FirstSightStyle.cardMedium,
            modifier = Modifier.size(56.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FirstSightStyle.appIconGradient())
            ) {
                when {
                    app.iconBytes != null -> {
                        val bitmap = ImageUtils.byteArrayToBitmap(app.iconBytes)
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = app.appName,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    else -> {
                        Icon(
                            imageVector = FirstSightIcons.Android,
                            contentDescription = app.appName,
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = app.appName,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // 用主题字号而不是硬编码，才能跟随设置里的字体大小缩放
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
fun contactCardPreview() {
    FirstSightTheme {
        val apps = listOf(
            AppInfo(
                packageName = "tech.huangsh.assistant",
                appName = "微信",
                iconBytes = null,
                isEnabled = true,
                order = 1,
                installTime = 1,
                lastUpdateTime = 1,
            )
        )
        apps.forEach { app ->
            AppCard(app) {
            }
        }
    }
}
