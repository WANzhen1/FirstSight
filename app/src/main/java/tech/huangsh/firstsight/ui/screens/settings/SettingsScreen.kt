package tech.huangsh.firstsight.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import tech.huangsh.firstsight.data.model.Settings
import tech.huangsh.firstsight.ui.theme.FirstSightIcons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import tech.huangsh.firstsight.R
import tech.huangsh.firstsight.ui.activity.AboutActivity
import tech.huangsh.firstsight.ui.activity.AppManagementActivity
import tech.huangsh.firstsight.ui.activity.ContactManagementActivity
import tech.huangsh.firstsight.ui.activity.DisplaySettingsActivity
import tech.huangsh.firstsight.ui.activity.ReminderActivity
import tech.huangsh.firstsight.ui.activity.ToolsActivity
import tech.huangsh.firstsight.ui.activity.VoiceSettingsActivity
import tech.huangsh.firstsight.ui.activity.WeatherSettingsActivity
import tech.huangsh.firstsight.ui.screens.components.CommonTopBar
import tech.huangsh.firstsight.ui.theme.FirstSightStyle
import tech.huangsh.firstsight.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FirstSightStyle.pageGradient())
    ) {
        // 顶部栏 - 使用统一的样式
        CommonTopBar(
            title = stringResource(R.string.settings),
            onBack = onBack
        )

        // 设置列表 - 与首页保持一致的白色卡片风格
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsItem(
                icon = FirstSightIcons.People,
                title = stringResource(R.string.contact_management),
                onClick = {
                    val intent = Intent(context, ContactManagementActivity::class.java)
                    context.startActivity(intent)
                }
            )

            SettingsItem(
                icon = FirstSightIcons.Apps,
                title = stringResource(R.string.app_settings),
                onClick = {
                    val intent = Intent(context, AppManagementActivity::class.java)
                    context.startActivity(intent)
                }
            )

            // 提醒管理
            SettingsItem(
                icon = FirstSightIcons.Schedule,
                title = stringResource(R.string.reminder_management),
                onClick = {
                    val intent = Intent(context, ReminderActivity::class.java)
                    context.startActivity(intent)
                }
            )

            // 快捷工具
            SettingsItem(
                icon = FirstSightIcons.Apps,
                title = stringResource(R.string.quick_tools),
                onClick = {
                    val intent = Intent(context, ToolsActivity::class.java)
                    context.startActivity(intent)
                }
            )

            // 天气设置
            SettingsItem(
                icon = FirstSightIcons.WbSunny,
                title = stringResource(R.string.weather_settings),
                onClick = {
                    val intent = Intent(context, WeatherSettingsActivity::class.java)
                    context.startActivity(intent)
                }
            )
            
            // 显示设置
            SettingsItem(
                icon = FirstSightIcons.DisplaySettings,
                title = stringResource(R.string.display_settings),
                onClick = {
                    val intent = Intent(context, DisplaySettingsActivity::class.java)
                    context.startActivity(intent)
                }
            )

            // 语音设置
            SettingsItem(
                icon = FirstSightIcons.VolumeUp,
                title = stringResource(R.string.voice_settings),
                onClick = {
                    val intent = Intent(context, VoiceSettingsActivity::class.java)
                    context.startActivity(intent)
                }
            )

            // 微信来电自动接听设置
            WeChatAutoAnswerSection(settingsViewModel)

            // 通话挂断自动返回首页设置
            CallAutoReturnSection(settingsViewModel)

            // 桌面启动器设置
            LauncherSettingsSection(settingsViewModel)

            // 关于我们
            SettingsItem(
                icon = Icons.Default.Info,
                title = stringResource(R.string.about_us),
                onClick = {
                    val intent = Intent(context, AboutActivity::class.java)
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    trailing: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = FirstSightStyle.cardMedium,
                ambientColor = FirstSightStyle.cardShadow(),
                spotColor = FirstSightStyle.cardShadow()
            )
            .clickable { onClick() },
        shape = FirstSightStyle.cardMedium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标背景 - 使用与首页一致的蓝色主题
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(FirstSightStyle.small)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // 标题
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // 尾部内容
            trailing()
        }
    }
}

@Composable
fun WeChatAutoAnswerSection(settingsViewModel: SettingsViewModel) {
    val settings by settingsViewModel.settings.collectAsState(initial = Settings())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = FirstSightStyle.cardMedium,
                ambientColor = FirstSightStyle.cardShadow(),
                spotColor = FirstSightStyle.cardShadow()
            ),
        shape = FirstSightStyle.cardMedium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(FirstSightStyle.small)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.wechat_auto_answer),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.wechat_auto_answer_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = settings.wechatAutoAnswerEnabled,
                    onCheckedChange = { settingsViewModel.updateWechatAutoAnswerEnabled(it) }
                )
            }

            if (settings.wechatAutoAnswerEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.wechat_auto_answer_contacts_only),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.wechat_auto_answer_contacts_only_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = settings.wechatAutoAnswerContactsOnly,
                        onCheckedChange = { settingsViewModel.updateWechatAutoAnswerContactsOnly(it) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.wechat_auto_speakerphone),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.wechat_auto_speakerphone_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = settings.wechatAutoSpeakerphone,
                        onCheckedChange = { settingsViewModel.updateWechatAutoSpeakerphone(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun CallAutoReturnSection(settingsViewModel: SettingsViewModel) {
    val settings by settingsViewModel.settings.collectAsState(initial = Settings())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = FirstSightStyle.cardMedium,
                ambientColor = FirstSightStyle.cardShadow(),
                spotColor = FirstSightStyle.cardShadow()
            ),
        shape = FirstSightStyle.cardMedium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(FirstSightStyle.small)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Text(
                    text = stringResource(R.string.call_auto_return_home),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.wechat_return_home_after_call),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.wechat_return_home_after_call_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = settings.wechatReturnHomeAfterCall,
                    onCheckedChange = { settingsViewModel.updateWechatReturnHomeAfterCall(it) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.phone_return_home_after_call),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.phone_return_home_after_call_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = settings.phoneReturnHomeAfterCall,
                    onCheckedChange = { settingsViewModel.updatePhoneReturnHomeAfterCall(it) }
                )
            }
        }
    }
}

@Composable
fun LauncherSettingsSection(settingsViewModel: SettingsViewModel) {
    val isDefaultLauncher by settingsViewModel.isDefaultLauncher.collectAsState()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 默认桌面状态
        // 无论当前是否已是默认桌面，点击都跳系统设置页，
        // 否则设成本应用后就再也无法从这里切回系统桌面。
        SettingsItemWithDescription(
            icon = Icons.Default.Home,
            title = stringResource(R.string.default_launcher_status),
            description = if (isDefaultLauncher) {
                stringResource(R.string.is_default_launcher)
            } else {
                stringResource(R.string.not_default_launcher) + "\n" + stringResource(R.string.set_as_default_launcher)
            },
            descriptionColor = if (isDefaultLauncher) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            onClick = { settingsViewModel.openDefaultAppSettings() }
        )
    }
}

@Composable
fun SettingsItemWithDescription(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    descriptionColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit = {},
    trailing: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = FirstSightStyle.cardMedium,
                ambientColor = FirstSightStyle.cardShadow(),
                spotColor = FirstSightStyle.cardShadow()
            )
            .clickable { onClick() },
        shape = FirstSightStyle.cardMedium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标背景 - 使用与首页一致的蓝色主题
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(FirstSightStyle.small)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // 标题和描述
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = descriptionColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 尾部内容
            trailing()
        }
    }
}