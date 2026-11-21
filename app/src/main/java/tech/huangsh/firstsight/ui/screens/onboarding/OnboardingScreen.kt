package tech.huangsh.firstsight.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import tech.huangsh.firstsight.ui.theme.FirstSightIcons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tech.huangsh.firstsight.R
import tech.huangsh.firstsight.ui.theme.FirstSightStyle
import tech.huangsh.firstsight.viewmodel.OnboardingState

/**
 * 首次启动引导页
 *
 * 权限分成四组：
 * - 基本权限：一次弹窗申请
 * - 无障碍、悬浮窗、电池优化：跳系统设置手动开启，返回后自动刷新
 */
@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onRequestRuntimePermissions: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenPowerPolicySettings: () -> Unit,
    onOpenAutoStartSettings: () -> Unit,
    onStart: () -> Unit
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FirstSightStyle.pageGradient())
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(48.dp))

                Icon(
                    imageVector = FirstSightIcons.PermDeviceInformation,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.onboarding_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.onboarding_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                PermissionGroupCard(
                    icon = Icons.Default.Lock,
                    title = stringResource(R.string.onboarding_group_basic),
                    description = stringResource(R.string.onboarding_group_basic_desc),
                    granted = state.runtimeGranted,
                    actionText = stringResource(R.string.onboarding_action_request),
                    onAction = onRequestRuntimePermissions
                )

                Spacer(Modifier.height(16.dp))

                PermissionGroupCard(
                    icon = Icons.Default.CheckCircle,
                    title = stringResource(R.string.onboarding_group_accessibility),
                    description = stringResource(R.string.onboarding_group_accessibility_desc),
                    granted = state.accessibilityEnabled,
                    actionText = stringResource(R.string.onboarding_action_request),
                    onAction = onOpenAccessibilitySettings,
                    hint = stringResource(R.string.onboarding_accessibility_how_to)
                )

                Spacer(Modifier.height(16.dp))

                GuideOnlyCard(
                    icon = FirstSightIcons.BatterySaver,
                    title = stringResource(R.string.onboarding_group_power),
                    description = stringResource(R.string.onboarding_group_power_desc),
                    howTo = stringResource(R.string.onboarding_power_how_to),
                    configured = state.powerPolicyConfigured,
                    onAction = onOpenPowerPolicySettings
                )

                Spacer(Modifier.height(16.dp))

                GuideOnlyCard(
                    icon = FirstSightIcons.Autorenew,
                    title = stringResource(R.string.onboarding_group_autostart),
                    description = stringResource(R.string.onboarding_group_autostart_desc),
                    howTo = stringResource(R.string.onboarding_autostart_how_to),
                    configured = state.autoStartConfigured,
                    onAction = onOpenAutoStartSettings
                )

                Spacer(Modifier.height(32.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Button(
                    onClick = onStart,
                    enabled = state.allGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_start),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(
                        if (state.allGranted) R.string.onboarding_ready_hint else R.string.onboarding_skip_hint
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (state.allGranted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PermissionGroupCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    actionText: String,
    onAction: () -> Unit,
    hint: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FirstSightStyle.cardMedium,
        colors = CardDefaults.cardColors(
            containerColor = if (granted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = if (granted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!granted && hint != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            if (granted) {
                Text(
                    text = stringResource(R.string.onboarding_granted),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(text = actionText, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun GuideOnlyCard(
    icon: ImageVector,
    title: String,
    description: String,
    howTo: String,
    configured: Boolean,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FirstSightStyle.cardMedium,
        colors = CardDefaults.cardColors(
            containerColor = if (configured) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (configured) Icons.Default.CheckCircle else icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = if (configured) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (configured) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.onboarding_configured),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!configured) {
                Spacer(Modifier.height(10.dp))

                Text(
                    text = howTo,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_action_request),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onAction) {
                        Text(
                            text = stringResource(R.string.onboarding_reconfigure),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
