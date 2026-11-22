package tech.huangsh.firstsight.ui.screens.tools

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.huangsh.firstsight.R
import tech.huangsh.firstsight.ui.activity.ToolsActivity
import tech.huangsh.firstsight.ui.screens.components.CommonTopBar
import tech.huangsh.firstsight.ui.theme.FirstSightIcons
import tech.huangsh.firstsight.ui.theme.FirstSightStyle
import tech.huangsh.firstsight.utils.FlashlightResult
import tech.huangsh.firstsight.utils.ToastUtils
import tech.huangsh.firstsight.viewmodel.ToolsViewModel
import java.util.Calendar

@Composable
fun ToolsScreen(
    initialTool: String?,
    onBack: () -> Unit,
    viewModel: ToolsViewModel
) {
    val context = LocalContext.current
    val flashlightOn by viewModel.flashlightOn.collectAsState()
    var activeTool by remember(initialTool) { mutableStateOf(initialTool.toToolType()) }
    val currentTool = activeTool

    fun toggleFlashlight() {
        when (viewModel.toggleFlashlight()) {
            FlashlightResult.ON -> ToastUtils.show(context, R.string.tool_flashlight_on)
            FlashlightResult.OFF -> ToastUtils.show(context, R.string.tool_flashlight_off)
            FlashlightResult.NO_FLASH -> ToastUtils.show(context, R.string.tool_flashlight_unavailable, android.widget.Toast.LENGTH_LONG)
            FlashlightResult.PERMISSION_DENIED -> ToastUtils.show(context, R.string.tool_camera_permission_needed, android.widget.Toast.LENGTH_LONG)
            FlashlightResult.ERROR -> ToastUtils.show(context, R.string.tool_flashlight_unavailable, android.widget.Toast.LENGTH_LONG)
        }
    }

    fun openAlarm() {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, calendar.get(Calendar.HOUR_OF_DAY))
            putExtra(AlarmClock.EXTRA_MINUTES, calendar.get(Calendar.MINUTE))
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            ToastUtils.show(context, R.string.tool_alarm_unavailable)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FirstSightStyle.pageGradient())
    ) {
        CommonTopBar(
            title = stringResource(R.string.tool_title),
            onBack = onBack
        )

        if (currentTool == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.tool_hint),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToolTile(
                        icon = FirstSightIcons.Flashlight,
                        title = stringResource(R.string.tool_flashlight),
                        subtitle = if (flashlightOn) stringResource(R.string.tool_flashlight_on_short) else stringResource(R.string.tool_flashlight_off_short),
                        active = flashlightOn,
                        modifier = Modifier.weight(1f),
                        onClick = { toggleFlashlight() }
                    )
                    ToolTile(
                        icon = FirstSightIcons.Search,
                        title = stringResource(R.string.tool_magnifier),
                        subtitle = stringResource(R.string.tool_magnifier_desc),
                        modifier = Modifier.weight(1f),
                        onClick = { activeTool = ToolType.MAGNIFIER }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToolTile(
                        icon = FirstSightIcons.Calculate,
                        title = stringResource(R.string.tool_calculator),
                        subtitle = stringResource(R.string.tool_calculator_desc),
                        modifier = Modifier.weight(1f),
                        onClick = { activeTool = ToolType.CALCULATOR }
                    )
                    ToolTile(
                        icon = FirstSightIcons.Schedule,
                        title = stringResource(R.string.tool_alarm),
                        subtitle = stringResource(R.string.tool_alarm_desc),
                        modifier = Modifier.weight(1f),
                        onClick = { openAlarm() }
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { activeTool = null }) {
                        Text(stringResource(R.string.tool_back), fontSize = 16.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = currentTool.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                when (currentTool) {
                    ToolType.FLASHLIGHT -> FlashlightPanel(
                        isOn = flashlightOn,
                        onToggle = { toggleFlashlight() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    ToolType.MAGNIFIER -> MagnifierView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    ToolType.CALCULATOR -> CalculatorView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    null -> Unit
                }
            }
        }
    }
}

@Composable
private fun ToolTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        shape = FirstSightStyle.cardLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .height(148.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (active) Color.White.copy(alpha = 0.20f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = if (active) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FlashlightPanel(
    isOn: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .clip(RoundedCornerShape(38.dp))
                .background(
                    if (isOn) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = FirstSightIcons.Flashlight,
                contentDescription = null,
                modifier = Modifier.size(68.dp),
                tint = if (isOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = if (isOn) stringResource(R.string.tool_flashlight_on) else stringResource(R.string.tool_flashlight_off),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onToggle,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        ) {
            Text(
                text = if (isOn) stringResource(R.string.tool_flashlight_off) else stringResource(R.string.tool_flashlight_on),
                fontSize = 19.sp
            )
        }
    }
}

private enum class ToolType(val title: String, val key: String) {
    FLASHLIGHT("手电筒", ToolsActivity.TOOL_FLASHLIGHT),
    MAGNIFIER("放大镜", ToolsActivity.TOOL_MAGNIFIER),
    CALCULATOR("计算器", ToolsActivity.TOOL_CALCULATOR)
}

private fun String?.toToolType(): ToolType? {
    return when (this) {
        ToolsActivity.TOOL_FLASHLIGHT -> ToolType.FLASHLIGHT
        ToolsActivity.TOOL_MAGNIFIER -> ToolType.MAGNIFIER
        ToolsActivity.TOOL_CALCULATOR -> ToolType.CALCULATOR
        else -> null
    }
}
