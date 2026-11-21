package tech.huangsh.firstsight.ui.screens.contact

import android.widget.Toast
import androidx.activity.compose.BackHandler
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import tech.huangsh.firstsight.ui.theme.FirstSightIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.*
import tech.huangsh.firstsight.R
import tech.huangsh.firstsight.data.model.Contact
import tech.huangsh.firstsight.ui.activity.ContactDetailActivity
import tech.huangsh.firstsight.ui.screens.components.CommonTopBar
import tech.huangsh.firstsight.ui.theme.FirstSightStyle
import tech.huangsh.firstsight.ui.screens.components.ContactItem
import tech.huangsh.firstsight.utils.ToastUtils
import tech.huangsh.firstsight.viewmodel.ContactViewModel

@Composable
fun ContactManagementScreen(
    onBack: () -> Unit,
    onAddContact: () -> Unit,
    viewModel: ContactViewModel = hiltViewModel()
) {
    val contacts by viewModel.contacts.collectAsState(emptyList())
    val systemContacts by viewModel.systemContacts.collectAsState(emptyList())
    val isLoadingSystemContacts by viewModel.isLoadingSystemContacts.collectAsState()
    val context = LocalContext.current

    // 本地即时排序列表，避免拖动过程中被数据库异步流回写打断
    var localContacts by remember { mutableStateOf(contacts) }
    var isDraggingAny by remember { mutableStateOf(false) }

    LaunchedEffect(contacts, isDraggingAny) {
        if (!isDraggingAny) {
            localContacts = contacts
        }
    }

    // ComposeReorderable 状态：拖动时即时更新 localContacts，松手后一次性持久化到数据库
    val state = rememberReorderableLazyListState(
        onMove = { from, to ->
            isDraggingAny = true
            localContacts = localContacts.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
            true
        },
        onDragEnd = { _, _ ->
            isDraggingAny = false
            viewModel.saveContactOrder(localContacts)
        }
    )

    // 导入联系人底部弹出框状态
    var showImportBottomSheet by remember { mutableStateOf(false) }
    var selectedContacts by remember { mutableStateOf<Set<Contact>>(emptySet()) }
    var searchQuery by remember { mutableStateOf("") }

    // 批量删除状态
    var isBatchDeleteMode by remember { mutableStateOf(false) }
    var selectedDeleteIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    fun exitBatchDeleteMode() {
        isBatchDeleteMode = false
        selectedDeleteIds = emptySet()
    }

    BackHandler(enabled = isBatchDeleteMode) {
        exitBatchDeleteMode()
    }

    // 检查并请求通讯录权限后打开导入弹窗
    fun requestContactsAndOpenImport() {
        val activity = context as? androidx.activity.ComponentActivity ?: return
        XXPermissions.with(activity)
            .permission(Permission.READ_CONTACTS)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                    if (allGranted) {
                        showImportBottomSheet = true
                    }
                }

                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                    if (doNotAskAgain) {
                        ToastUtils.show(
                            context,
                            "请在系统设置中开启通讯录权限以导入联系人",
                            Toast.LENGTH_LONG
                        )
                        XXPermissions.startPermissionActivity(context, permissions)
                    } else {
                        ToastUtils.show(
                            context,
                            "需要通讯录权限才能读取手机联系人"
                        )
                    }
                }
            })
    }

    // 显示导入联系人底部弹出框时加载系统联系人
    LaunchedEffect(showImportBottomSheet) {
        if (showImportBottomSheet) {
            viewModel.loadSystemContacts()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FirstSightStyle.pageGradient())
    ) {
        // 顶部栏 - 使用统一的样式
        CommonTopBar(
            title = if (isBatchDeleteMode) {
                "已选 ${selectedDeleteIds.size} 人"
            } else {
                stringResource(R.string.contact_management)
            },
            onBack = {
                if (isBatchDeleteMode) {
                    exitBatchDeleteMode()
                } else {
                    onBack()
                }
            },
            actions = {
                if (isBatchDeleteMode) {
                    val allSelected = localContacts.isNotEmpty() &&
                        localContacts.all { it.id in selectedDeleteIds }
                    TextButton(
                        onClick = {
                            selectedDeleteIds = if (allSelected) {
                                emptySet()
                            } else {
                                localContacts.map { it.id }.toSet()
                            }
                        }
                    ) {
                        Text(if (allSelected) "取消全选" else "全选")
                    }
                    TextButton(onClick = { exitBatchDeleteMode() }) {
                        Text(stringResource(R.string.cancel))
                    }
                } else {
                    IconButton(onAddContact) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_contact),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = { requestContactsAndOpenImport() }
                    ) {
                        Icon(
                            imageVector = FirstSightIcons.ImportContacts,
                            contentDescription = stringResource(R.string.import_contacts),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    if (contacts.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                isBatchDeleteMode = true
                                selectedDeleteIds = emptySet()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "批量删除",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        )

        // 联系人列表
        if (contacts.isEmpty()) {
            // 空状态
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = FirstSightIcons.People,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.no_contacts),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.add_first_contact),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = if (isBatchDeleteMode) {
                    "点击联系人进行勾选，确认后可一键批量删除"
                } else {
                    "按住右侧三杠或长按卡片上下拖动排序，排在前面的将优先显示在首页"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            LazyColumn(
                state = state.listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .let { if (isBatchDeleteMode) it else it.reorderable(state) },
                verticalArrangement = Arrangement.spacedBy(15.dp),
                contentPadding = PaddingValues(bottom = if (isBatchDeleteMode) 16.dp else 80.dp)
            ) {
                items(localContacts, key = { it.id }) { contact ->
                    val isSelected = contact.id in selectedDeleteIds
                    ReorderableItem(state, key = contact.id) { isDragging ->
                        DraggableContactItem(
                            contact = contact,
                            isDragging = isDragging && !isBatchDeleteMode,
                            isSelectionMode = isBatchDeleteMode,
                            isSelected = isSelected,
                            onClick = {
                                if (isBatchDeleteMode) {
                                    selectedDeleteIds = if (isSelected) {
                                        selectedDeleteIds - contact.id
                                    } else {
                                        selectedDeleteIds + contact.id
                                    }
                                } else {
                                    ContactDetailActivity.start(context, contact, true)
                                }
                            },
                            reorderableState = state
                        )
                    }
                }
            }

            if (isBatchDeleteMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Button(
                            onClick = { showBatchDeleteConfirm = true },
                            enabled = selectedDeleteIds.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF5350),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = if (selectedDeleteIds.isEmpty()) {
                                    "请选择要删除的联系人"
                                } else {
                                    "删除所选联系人 (${selectedDeleteIds.size})"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }

    // 导入联系人底部弹出框
    if (showImportBottomSheet) {
        val existingKeys = remember(contacts) {
            contacts.map { "${it.name.trim()}|${it.phone.orEmpty().replace(Regex("[\\s\\-()]+"), "")}" }.toSet()
        }
        ImportContactsBottomSheet(
            systemContacts = systemContacts,
            existingKeys = existingKeys,
            isLoading = isLoadingSystemContacts,
            selectedContacts = selectedContacts,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onContactSelect = { contact, isSelected ->
                selectedContacts = if (isSelected) {
                    selectedContacts + contact
                } else {
                    selectedContacts - contact
                }
            },
            onSelectAll = { importable ->
                selectedContacts = if (importable.all { it in selectedContacts }) {
                    selectedContacts - importable.toSet()
                } else {
                    selectedContacts + importable
                }
            },
            onImport = { orderedContacts ->
                viewModel.importContacts(orderedContacts) { count ->
                    val msg = if (count > 0) "已成功导入 $count 位联系人" else "所选联系人已存在"
                    ToastUtils.show(context, msg)
                }
                selectedContacts = emptySet()
                searchQuery = ""
                showImportBottomSheet = false
            },
            onDismiss = {
                selectedContacts = emptySet()
                searchQuery = ""
                showImportBottomSheet = false
            }
        )
    }

    if (showBatchDeleteConfirm) {
        val count = selectedDeleteIds.size
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text("确认批量删除") },
            text = { Text("确定要删除选中的 $count 位联系人吗？删除后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = localContacts.filter { it.id in selectedDeleteIds }
                        showBatchDeleteConfirm = false
                        viewModel.deleteContacts(toDelete) { deletedCount ->
                            ToastUtils.show(context, "已删除 $deletedCount 位联系人")
                            exitBatchDeleteMode()
                        }
                    }
                ) {
                    Text("删除", color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportContactsBottomSheet(
    systemContacts: List<Contact>,
    existingKeys: Set<String>,
    isLoading: Boolean,
    selectedContacts: Set<Contact>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onContactSelect: (Contact, Boolean) -> Unit,
    onSelectAll: (List<Contact>) -> Unit,
    onImport: (List<Contact>) -> Unit,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val bottomSheetHeight = screenHeight * 0.8f
    
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = 150f // 向下拖动超过这个阈值就关闭
    
    // 动画偏移量
    val animatedOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dragOffset"
    )
    
    // 过滤联系人
    val filteredContacts = remember(systemContacts, searchQuery) {
        if (searchQuery.isBlank()) {
            systemContacts
        } else {
            systemContacts.filter { contact ->
                contact.name.contains(searchQuery, ignoreCase = true) ||
                contact.phone?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }
    
    // 简化：直接使用过滤后的联系人，不需要拖动排序
    
    Dialog(
        onDismissRequest = { }, // 不允许点击外部关闭
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false // 禁用点击外部关闭
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            // 底部弹出内容 - 向上偏移一些
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bottomSheetHeight)
                    .align(Alignment.BottomCenter)
                    .offset(y = (-20).dp)
                    .graphicsLayer {
                        translationY = maxOf(0f, animatedOffset)
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                // 开始拖动时停止动画
                            },
                            onDragEnd = {
                                if (dragOffset > dismissThreshold) {
                                    onDismiss()
                                } else {
                                    // 回弹到原位置
                                    dragOffset = 0f
                                }
                            }
                        ) { _, dragAmount ->
                            // 只允许向下拖动
                            if (dragAmount > 0 || dragOffset > 0) {
                                dragOffset = maxOf(0f, dragOffset + dragAmount)
                            }
                        }
                    },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // 顶部拖动指示器
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(2.dp)
                            )
                            .align(Alignment.CenterHorizontally)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 标题
                    Text(
                        text = stringResource(R.string.import_contacts),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 搜索框 - 圆角样式
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        label = { Text("搜索联系人") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "清除")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        singleLine = true,
                        shape = FirstSightStyle.cardLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 联系人列表
                    val importableFiltered = remember(filteredContacts, existingKeys) {
                        filteredContacts.filter { contact ->
                            val key = "${contact.name.trim()}|${contact.phone.orEmpty().replace(Regex("[\\s\\-()]+"), "")}"
                            key !in existingKeys
                        }
                    }

                    if (!isLoading && importableFiltered.isNotEmpty()) {
                        val allSelected = importableFiltered.all { it in selectedContacts }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "共 ${filteredContacts.size} 位联系人（可选 ${importableFiltered.size} 位）",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { onSelectAll(importableFiltered) }) {
                                Text(text = if (allSelected) "取消全选" else "全选")
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        if (isLoading) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(stringResource(R.string.loading_contacts))
                            }
                        } else if (systemContacts.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "手机通讯录暂无联系人",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (filteredContacts.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "未找到匹配的联系人",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn {
                                items(filteredContacts) { contact ->
                                    val key = "${contact.name.trim()}|${contact.phone.orEmpty().replace(Regex("[\\s\\-()]+"), "")}"
                                    val alreadyAdded = key in existingKeys
                                    val isSelected = selectedContacts.contains(contact)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !alreadyAdded) {
                                                onContactSelect(contact, !isSelected)
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = alreadyAdded || isSelected,
                                            enabled = !alreadyAdded,
                                            onCheckedChange = { checked ->
                                                if (!alreadyAdded) {
                                                    onContactSelect(contact, checked)
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = contact.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = if (alreadyAdded) {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                            contact.phone?.let { phone ->
                                                Text(
                                                    text = phone,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        if (alreadyAdded) {
                                            Text(
                                                text = "已添加",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // 底部按钮 - 减少间距，向上移动
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.cancel),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        
                        Button(
                            onClick = { 
                                // 传递选中的联系人
                                val selectedContactsList = selectedContacts.toList()
                                onImport(selectedContactsList)
                            },
                            enabled = selectedContacts.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                text = if (selectedContacts.isEmpty()) {
                                    "导入"
                                } else {
                                    "导入 (${selectedContacts.size})"
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun DraggableContactItem(
    contact: Contact,
    isDragging: Boolean,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    reorderableState: ReorderableLazyListState
) {
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        label = "dragScale"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .let { if (isSelectionMode) it else it.detectReorderAfterLongPress(reorderableState) },
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isDragging -> MaterialTheme.colorScheme.primaryContainer
                isSelectionMode && isSelected -> lerp(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.primaryContainer,
                    0.55f
                )
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = when {
            isDragging -> CardDefaults.cardElevation(defaultElevation = 10.dp)
            else -> CardDefaults.cardElevation(defaultElevation = 2.dp)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isDragging) { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            ContactItem(
                contact = contact,
                modifier = Modifier.size(50.dp),
                onClick = onClick,
                isHomeScreen = false
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 姓名
            Text(
                text = contact.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
            } else {
                // 三杠拖动手柄（按住直接拖动，无需长按等待）
                Icon(
                    imageVector = FirstSightIcons.DragIndicator,
                    contentDescription = stringResource(R.string.drag_to_sort),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(48.dp)
                        .detectReorder(reorderableState)
                        .padding(10.dp)
                )
            }
        }
    }
}
