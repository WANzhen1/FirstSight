package tech.huangsh.firstsight.data.repository

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tech.huangsh.firstsight.data.local.dao.ContactDao
import tech.huangsh.firstsight.data.model.Contact
import tech.huangsh.firstsight.service.wechat.WeChatData
import tech.huangsh.firstsight.utils.ChineseUtils
import tech.huangsh.firstsight.utils.ImageUtils
import tech.huangsh.firstsight.utils.PermissionUtils
import tech.huangsh.firstsight.utils.ToastUtils

/**
 * 联系人数据仓库
 */
class ContactRepository(
    private val contactDao: ContactDao,
    private val context: Context
) {
    private val seedMutex = Mutex()
    val allContacts: Flow<List<Contact>> = contactDao.getAllContacts()
    
    fun getContactsCount(): Flow<Int> = contactDao.getContactCount()

    suspend fun getContactById(id: Long): Contact? = contactDao.getContactById(id)

    suspend fun insertContact(contact: Contact): Long {
        val maxOrder = contactDao.getMaxOrder() ?: -1
        val newContact = contact.copy(order = maxOrder + 1)
        return contactDao.insertContact(newContact)
    }

    suspend fun updateContact(contact: Contact) {
        contactDao.updateContact(contact.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteContact(contact: Contact) {
        // 删除头像文件
        ImageUtils.deleteImageFile(contact.avatarUri)
        contactDao.deleteContact(contact)
    }

    suspend fun deleteContactById(id: Int) {
        // 先获取联系人信息以删除头像文件
        val contact = contactDao.getContactById(id.toLong())
        contact?.let {
            ImageUtils.deleteImageFile(it.avatarUri)
        }
        contactDao.deleteContactById(id)
    }

    suspend fun moveContact(contactId: Long, fromPosition: Int, toPosition: Int) {
        if (fromPosition == toPosition) return

        // 获取所有联系人并更新顺序
        val contacts = contactDao.getAllContacts().map { it }.first()

        if (fromPosition < toPosition) {
            // 向下移动：将中间的联系人向上移动
            for (i in fromPosition + 1..toPosition) {
                contactDao.updateContactOrder(contacts[i].id, i - 1)
            }
        } else {
            // 向上移动：将中间的联系人向下移动
            for (i in toPosition until fromPosition) {
                contactDao.updateContactOrder(contacts[i].id, i + 1)
            }
        }

        // 更新被拖动的联系人的位置
        contactDao.updateContactOrder(contactId, toPosition)
    }

    /**
     * 批量持久化联系人排序（按列表顺序写入 order = 0..n-1）
     */
    suspend fun updateContactOrders(orderedContacts: List<Contact>) {
        contactDao.updateContactOrders(orderedContacts.map { it.id })
    }

    fun searchContacts(query: String): Flow<List<Contact>> {
        return contactDao.searchContacts(query)
    }

    /**
     * 首次启动时预置 5 位默认联系人（仅执行一次，用户主动删空后不再重新插入）
     */
    suspend fun ensurePresetContacts(settingsRepository: SettingsRepository) {
        seedMutex.withLock {
            if (settingsRepository.presetContactsSeeded.first()) return
            if (contactDao.getContactCount().first() > 0) {
                settingsRepository.updatePresetContactsSeeded(true)
                return
            }

            withContext(Dispatchers.IO) {
                val presets = listOf(
                    Triple("陈冠希", "13800000001", "avatars/chen_guanxi.jpg"),
                    Triple("彭于晏", "13800000002", "avatars/peng_yuyan.jpg"),
                    Triple("金城武", "13800000003", "avatars/jin_chengwu.jpg"),
                    Triple("吴彦祖", "13800000004", "avatars/wu_yanzu.jpg"),
                    Triple("刘亦菲", "13800000005", "avatars/liu_yifei.jpg")
                )

                var nextOrder = (contactDao.getMaxOrder() ?: -1) + 1
                presets.forEach { (name, phone, assetPath) ->
                    val avatarPath = ImageUtils.copyAssetToAppDirectory(context, assetPath)
                    contactDao.insertContact(
                        Contact(
                            name = name,
                            phone = phone,
                            wechatNickname = name,
                            avatarUri = avatarPath,
                            order = nextOrder++,
                            hasVideoCall = true,
                            hasVoiceCall = true,
                            hasPhoneCall = true
                        )
                    )
                }
            }

            settingsRepository.updatePresetContactsSeeded(true)
        }
    }

    /**
     * 从系统通讯录获取联系人
     */
    suspend fun getSystemContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return@withContext emptyList()

        val contacts = mutableListOf<Contact>()
        val contentResolver = context.contentResolver

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )

        val cursor = try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
        } catch (e: Exception) {
            null
        }

        cursor?.use { c ->
            val nameIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

            while (c.moveToNext()) {
                val name = if (nameIndex >= 0) c.getString(nameIndex)?.trim().orEmpty() else ""
                val rawPhone = if (phoneIndex >= 0) c.getString(phoneIndex)?.trim().orEmpty() else ""
                val photoUri = if (photoIndex >= 0) c.getString(photoIndex) else null

                if (name.isBlank() || rawPhone.isBlank()) continue
                val normalizedPhone = normalizePhone(rawPhone)
                if (normalizedPhone.isBlank()) continue

                contacts.add(
                    Contact(
                        name = name,
                        phone = normalizedPhone,
                        wechatNickname = name,
                        avatarUri = photoUri,
                        hasVideoCall = true,
                        hasVoiceCall = true,
                        hasPhoneCall = true
                    )
                )
            }
        }

        // 去重并按中文拼音A-Z排序
        contacts
            .distinctBy { "${it.name}|${normalizePhone(it.phone)}" }
            .sortedWith(compareBy(ChineseUtils.pinyinComparator) { it.name })
    }

    /**
     * 批量导入系统通讯录联系人（复制头像到本地并去重）
     * @return 实际新导入的联系人数量
     */
    suspend fun importContacts(selectedContacts: List<Contact>): Int = withContext(Dispatchers.IO) {
        if (selectedContacts.isEmpty()) return@withContext 0

        val existing = contactDao.getAllContacts().first()
        val existingKeys = existing.map { "${it.name.trim()}|${normalizePhone(it.phone)}" }.toMutableSet()
        var nextOrder = (contactDao.getMaxOrder() ?: -1) + 1
        var importedCount = 0

        selectedContacts.forEach { contact ->
            val key = "${contact.name.trim()}|${normalizePhone(contact.phone)}"
            if (key in existingKeys) return@forEach

            val localAvatarPath = contact.avatarUri?.let { uriStr ->
                runCatching {
                    val uri = Uri.parse(uriStr)
                    if (uri.scheme == "content" || uri.scheme == "file") {
                        ImageUtils.copyImageToAppDirectory(context, uri)
                    } else {
                        uriStr
                    }
                }.getOrNull()
            }

            contactDao.insertContact(
                contact.copy(
                    id = 0,
                    avatarUri = localAvatarPath,
                    order = nextOrder++,
                    wechatNickname = contact.wechatNickname?.takeIf { it.isNotBlank() } ?: contact.name
                )
            )
            existingKeys.add(key)
            importedCount++
        }

        importedCount
    }

    private fun normalizePhone(phone: String?): String {
        return phone.orEmpty().replace(Regex("[\\s\\-()]+"), "")
    }

    /**
     * 发起电话呼叫
     */
    fun makePhoneCall(phoneNumber: String): Intent {
        val cleaned = phoneNumber.trim()
        val action = if (cleaned.any { it == '*' || it == '#' || it == ';' || it == ',' || it == '%' }) {
            Intent.ACTION_DIAL
        } else {
            Intent.ACTION_CALL
        }
        return Intent(action).apply {
            data = Uri.fromParts("tel", cleaned, null)
            flags = FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * 发起微信视频通话
     * @return 操作是否成功启动
     */
    fun startWeChatVideoCall(wechatNickname: String?): Boolean {
        if (wechatNickname.isNullOrEmpty()) {
            return false
        }

        // 检查无障碍服务是否启用
        if (!isAccessibilityServiceEnabled()) {
            // 跳转到无障碍服务设置页面
            PermissionUtils.openAccessibilitySettings(context)
            return false
        }

        // 检查微信应用是否安装弄璋之喜
        if (!isWeChatInstalled()) {
            return false
        }

        // 设置微信视频通话参数
        WeChatData.updateValue(wechatNickname)
        WeChatData.updateVideo(true)
        WeChatData.startFlow()

        // 启动微信应用
        return startWechat()
    }

    /**
     * 发起微信语音通话
     * @return 操作是否成功启动
     */
    fun startWeChatVoiceCall(wechatNickname: String?): Boolean {
        if (wechatNickname.isNullOrEmpty()) {
            return false
        }

        // 检查无障碍服务是否启用
        if (!isAccessibilityServiceEnabled()) {
            // 跳转到无障碍服务设置页面
            PermissionUtils.openAccessibilitySettings(context)
            return false
        }

        // 检查微信应用是否安装
        if (!isWeChatInstalled()) {
            return false
        }

        // 设置微信语音通话参数
        WeChatData.updateValue(wechatNickname)
        WeChatData.updateVideo(false)
        WeChatData.startFlow()

        // 启动微信应用
        return startWechat()
    }

    /**
     * 启动微信应用
     * @return 是否成功启动
     */
    private fun startWechat(): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.tencent.mm")
            if (launchIntent != null) {
                launchIntent.flags = FLAG_ACTIVITY_NEW_TASK
                launchIntent.setClassName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI")
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查微信应用是否安装
     * @return 微信是否已安装
     */
    private fun isWeChatInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.tencent.mm", 0)
            true
        } catch (e: Exception) {
            ToastUtils.show(context, "未找到微信应用")
            false
        }
    }

    /**
     * 检查无障碍服务是否启用
     */
    private fun isAccessibilityServiceEnabled(): Boolean =
        PermissionUtils.isAccessibilityServiceEnabled(context)

    suspend fun getContactCount(): Int = contactDao.getContactCount().first()

    suspend fun deleteAllContacts() {
        // 先获取所有联系人以删除头像文件
        val contacts = contactDao.getAllContacts().map { it }.first()
        contacts.forEach { contact ->
            ImageUtils.deleteImageFile(contact.avatarUri)
        }
        contactDao.deleteAllContacts()
    }
}