package tech.huangsh.firstsight.ui.screens.reminder

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import tech.huangsh.firstsight.R
import tech.huangsh.firstsight.data.model.Reminder
import tech.huangsh.firstsight.ui.screens.components.CommonTopBar
import tech.huangsh.firstsight.ui.theme.FirstSightIcons
import tech.huangsh.firstsight.ui.theme.FirstSightStyle
import tech.huangsh.firstsight.utils.ReminderTimeUtils
import tech.huangsh.firstsight.utils.ToastUtils
import tech.huangsh.firstsight.viewmodel.ReminderViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    onBack: () -> Unit,
    viewModel: ReminderViewModel
) {
    val context = LocalContext.current
    val reminders by viewModel.reminders.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }
    var deleteTarget by remember { mutableStateOf<Reminder?>(null) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            ToastUtils.show(context, R.string.reminder_notification_permission_hint, android.widget.Toast.LENGTH_LONG)
        }
        pendingAction?.invoke()
        pendingAction = null
    }

    fun ensureNotificationPermission(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingAction = action
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            action()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FirstSightStyle.pageGradient()),
        containerColor = Color.Transparent,
        topBar = {
            CommonTopBar(
                title = stringResource(R.string.reminder_management),
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingReminder = null
                    showEditor = true
                },
                icon = {
                    Icon(
                        imageVector = FirstSightIcons.Schedule,
                        contentDescription = null
                    )
                },
                text = { Text(stringResource(R.string.reminder_add), fontSize = 17.sp) }
            )
        }
    ) { padding ->
        if (reminders.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = FirstSightIcons.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.reminder_empty),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.reminder_empty_hint),
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
            ) {
                items(reminders, key = { it.id }) { reminder ->
                    ReminderRow(
                        reminder = reminder,
                        onEdit = {
                            editingReminder = reminder
                            showEditor = true
                        },
                        onToggle = { enabled -> viewModel.setEnabled(reminder, enabled) },
                        onDelete = { deleteTarget = reminder }
                    )
                }
            }
        }
    }

    if (showEditor) {
        ReminderEditorDialog(
            initial = editingReminder,
            onDismiss = { showEditor = false },
            onSave = { reminder ->
                ensureNotificationPermission {
                    viewModel.saveReminder(reminder) {
                        showEditor = false
                    }
                }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.reminder_delete_title), fontSize = 22.sp) },
            text = { Text(stringResource(R.string.reminder_delete_confirm, target.title), fontSize = 17.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteReminder(target)
                        deleteTarget = null
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error, fontSize = 17.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.cancel), fontSize = 17.sp)
                }
            }
        )
    }
}

@Composable
private fun ReminderRow(
    reminder: Reminder,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = FirstSightStyle.cardMedium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.width(86.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = ReminderTimeUtils.formatTime(reminder.hour, reminder.minute),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (reminder.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = ReminderTimeUtils.repeatLabel(reminder),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (reminder.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = reminder.note,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (reminder.isMedication) stringResource(R.string.reminder_type_medication) else stringResource(R.string.reminder_type_appointment),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Switch(
                    checked = reminder.enabled,
                    onCheckedChange = onToggle
                )
                TextButton(onClick = onDelete) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderEditorDialog(
    initial: Reminder?,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit
) {
    val context = LocalContext.current
    val now = remember { Calendar.getInstance() }
    val initialOnceAt = initial?.onceAt

    var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
    var note by remember(initial) { mutableStateOf(initial?.note ?: "") }
    var isMedication by remember(initial) { mutableStateOf(initial?.isMedication ?: true) }
    var hour by remember(initial) { mutableIntStateOf(initial?.hour ?: 8) }
    var minute by remember(initial) { mutableIntStateOf(initial?.minute ?: 0) }
    var repeatMode by remember(initial) {
        mutableStateOf(
            when {
                initial == null -> RepeatMode.DAILY
                initial.onceAt != null -> RepeatMode.ONCE
                initial.repeatDays == Reminder.WEEKDAYS -> RepeatMode.WEEKDAYS
                initial.repeatDays == Reminder.WEEKENDS -> RepeatMode.WEEKENDS
                else -> RepeatMode.DAILY
            }
        )
    }
    var dateMillis by remember(initial) {
        mutableLongStateOf(
            initialOnceAt ?: Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    fun pickTime() {
        TimePickerDialog(context, { _, pickedHour, pickedMinute ->
            hour = pickedHour
            minute = pickedMinute
        }, hour, minute, true).show()
    }

    fun pickDate() {
        val current = Calendar.getInstance().apply { timeInMillis = dateMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                dateMillis = picked.timeInMillis
            },
            current.get(Calendar.YEAR),
            current.get(Calendar.MONTH),
            current.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(if (initial == null) R.string.reminder_add else R.string.reminder_edit),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        error = null
                    },
                    label = { Text(stringResource(R.string.reminder_name)) },
                    placeholder = { Text(stringResource(R.string.reminder_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = isMedication,
                        onClick = { isMedication = true },
                        label = { Text(stringResource(R.string.reminder_type_medication)) }
                    )
                    FilterChip(
                        selected = !isMedication,
                        onClick = { isMedication = false },
                        label = { Text(stringResource(R.string.reminder_type_appointment)) }
                    )
                }

                Spacer(Modifier.height(14.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pickTime() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.reminder_time),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = ReminderTimeUtils.formatTime(hour, minute),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.reminder_repeat),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RepeatMode.values().forEach { mode ->
                        FilterChip(
                            selected = repeatMode == mode,
                            onClick = { repeatMode = mode },
                            label = { Text(mode.label) }
                        )
                    }
                }

                if (repeatMode == RepeatMode.ONCE) {
                    Spacer(Modifier.height(14.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pickDate() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.reminder_date),
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = java.text.SimpleDateFormat("yyyy年MM月dd日", java.util.Locale.CHINA)
                                    .format(java.util.Date(dateMillis)),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.reminder_note)) },
                    placeholder = { Text(stringResource(R.string.reminder_note_hint)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), fontSize = 16.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                error = context.getString(R.string.reminder_name_required)
                                return@Button
                            }
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = if (repeatMode == RepeatMode.ONCE) {
                                    dateMillis
                                } else {
                                    System.currentTimeMillis()
                                }
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val onceAt = if (repeatMode == RepeatMode.ONCE) {
                                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                                    error = context.getString(R.string.reminder_time_invalid)
                                    return@Button
                                }
                                calendar.timeInMillis
                            } else {
                                null
                            }
                            val repeatDays = when (repeatMode) {
                                RepeatMode.DAILY -> Reminder.ALL_DAYS
                                RepeatMode.WEEKDAYS -> Reminder.WEEKDAYS
                                RepeatMode.WEEKENDS -> Reminder.WEEKENDS
                                RepeatMode.ONCE -> 0
                            }
                            val base = initial ?: Reminder(title = title, hour = hour, minute = minute)
                            onSave(
                                base.copy(
                                    title = title.trim(),
                                    note = note.trim(),
                                    isMedication = isMedication,
                                    hour = hour,
                                    minute = minute,
                                    repeatDays = repeatDays,
                                    onceAt = onceAt,
                                    enabled = true
                                )
                            )
                        }
                    ) {
                        Text(stringResource(R.string.reminder_save), fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

private enum class RepeatMode(val label: String) {
    DAILY("每天"),
    WEEKDAYS("周一至周五"),
    WEEKENDS("周末"),
    ONCE("仅一次")
}
