# 安全修复说明

修复日期：2026-11-21

本提交基于上游 `44dfeaf` 的安全审查结果，重点修复自动接听、无障碍自动化、敏感数据备份和权限范围问题。

## 高风险修复

- **微信自动接听白名单**
  - 联系人新增 `trustedForAutoAnswer` 字段，并加入 Room v1→v2 迁移。
  - 只有显式标记为“可信家人”的联系人，才会进入自动接听白名单。
  - 已有联系人、预置联系人和导入联系人默认都不可信，避免“联系人列表等于家人列表”。
  - 由于微信界面只提供昵称，仍存在昵称被冒充的残余风险；需要微信唯一 ID 或人工确认才能彻底消除。

- **锁屏自动接听**
  - 自动接听前检查 `KeyguardManager.isKeyguardLocked`，锁屏状态直接跳过，避免锁屏时自动接听并开启免提。

- **无障碍点击与悬浮窗劫持**
  - 所有微信操作前校验当前活动窗口属于 `com.tencent.mm`。
  - 坐标点击前检查目标位置是否被非微信、非 SystemUI 窗口遮挡。
  - 来电横幅只接受 `com.android.systemui` / `com.miui.systemui`，不再扫描所有第三方应用窗口。
  - 无障碍服务只遍历微信、SystemUI、拨号器等必要窗口，减少读取其他应用内容。

## 中风险修复

- **云备份与敏感数据**
  - `android:allowBackup` 改为 `false`，避免联系人、电话号码、微信昵称、头像和应用设置进入云备份或设备迁移。
  - 删除未使用的明文密码字段、默认密码 `123456` 和 `verifyPassword` 死代码。

- **设置入口认证**
  - 设备已设置锁屏时，进入隐藏设置前要求系统锁屏密码/指纹验证。
  - 无锁屏设备仍保留“连点 7 次 + 确认”的原有入口。

- **未使用危险权限**
  - 移除 `WRITE_CONTACTS`、`READ_EXTERNAL_STORAGE`、`WRITE_EXTERNAL_STORAGE`、`SYSTEM_ALERT_WINDOW`、`RECEIVE_BOOT_COMPLETED`、`VIBRATE`、`SCHEDULE_EXACT_ALARM`、`POST_NOTIFICATIONS`。
  - 移除引导页中未实际使用的悬浮窗授权步骤。
  - `QUERY_ALL_PACKAGES` 仍保留，用于桌面启动器的应用列表功能。

- **电话拨打**
  - 拨打前增加二次确认，显示联系人和实际号码。
  - 含 `*`、`#`、`;`、`,`、`%` 等特殊字符的号码改走 `ACTION_DIAL`，不再直接拨出。
  - `tel:` URI 改用 `Uri.fromParts` 构造，避免特殊字符被错误解析。

- **FileProvider 与日志**
  - FileProvider 从整个外部存储收窄到外部缓存目录。
  - 删除联系人姓名、头像 URI 等调试日志。

## 构建与 lint

- `:app:assembleDebug` 构建成功。
- `:app:lintDebug` 通过，0 errors。
- 修复 XXPermissions 使用 `Manifest.permission.*` 导致的 lint 类型错误。
- 未修改应用版本号和签名配置。

## 行为变化

- 升级后已有联系人默认不是“可信家人”，需要在联系人详情中手动打开对应开关。
- 开启“仅接听可信家人”时，如果没有标记任何可信联系人，不会自动接听微信来电。
- 锁屏状态不会自动接听。
- 电话拨打会增加一次确认步骤。
- 设备有锁屏时，进入设置需要先通过系统锁屏验证。
