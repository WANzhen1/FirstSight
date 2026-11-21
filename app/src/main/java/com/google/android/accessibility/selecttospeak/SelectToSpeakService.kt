package com.google.android.accessibility.selecttospeak

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Path
import android.graphics.Rect
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import tech.huangsh.firstsight.service.wechat.FlowAbortReason
import tech.huangsh.firstsight.service.wechat.WeChatActivity
import tech.huangsh.firstsight.service.wechat.WeChatData
import tech.huangsh.firstsight.service.wechat.WeChatId
import tech.huangsh.firstsight.ui.activity.MainActivity

/**
 * 微信一键通话的无障碍驱动
 *
 * 用轮询重试而不是依赖窗口事件推进：
 * 搜索结果加载、菜单弹出等只是同一个 Activity 内的 View 变化，不会产生
 * WindowStateChanged 事件，只靠事件驱动会永久卡在等待某一步。
 *
 * 每一步在 [STEP_TIMEOUT_MS] 内反复尝试，超时则复位并提示。
 */
class SelectToSpeakService : AccessibilityService() {

    private val tag: String = "WechatAccessibilityTag"

    private val handler = Handler(Looper.getMainLooper())

    /** 当前步骤开始的时间，用于超时判断 */
    private var stepStartedAt = 0L

    /** 上次按返回键的时间，避免连续后退把微信一路退出去 */
    private var lastBackAt = 0L

    private var tickerRunning = false

    /** 最近一次窗口事件的类名，用于快速判断弹窗是否出现 */
    private var lastClassName: String = ""

    /** 最近一次 TYPE_WINDOW_STATE_CHANGED 的窗口类名（不会被点击事件覆盖） */
    private var currentWindowClassName: String = ""

    /**
     * 本服务自己操作的时间
     *
     * 用 dispatchGesture 派发的手势会被系统视作一次触摸交互，同样产生
     * TYPE_TOUCH_INTERACTION_START。不排除的话，index=6 手势点击菜单的
     * 瞬间就会被误判成用户介入。
     */
    private var lastSelfActionAt = 0L

    /** 自动接听防抖、冷却时间与接通确认状态 */
    private var lastAutoAnswerScanAt = 0L
    private var lastAutoAnswerAt = 0L
    private var lastBannerExpandAt = 0L
    private var pendingAutoAnswerRunnable: Runnable? = null
    private var pendingBannerExpandRunnable: Runnable? = null
    private var pendingConnectedConfirmCaller: String? = null
    private var pendingConnectedConfirmUntil = 0L
    private var connectedCheckRunnable: Runnable? = null
    private var pendingSpeakerphoneRunnable: Runnable? = null

    /** 通话挂断自动返回首页状态 */
    private var inWeChatVoipCall = false
    private var inPhoneCall = false
    private var lastCallStateScanAt = 0L
    private var lastReturnHomeAt = 0L
    private var pendingReturnHomeRunnable: Runnable? = null

    /**
     * 处理用户触摸
     *
     * 只在自己操作后的短窗口内忽略，其余触碰都视为用户介入。
     * 微信一键通话全程约 3 秒，用户中途点屏幕基本意味着想自己操作。
     */
    private fun handleUserTouch() {
        if (SystemClock.uptimeMillis() - lastSelfActionAt < SELF_ACTION_GRACE_MS) return

        // 如果正在等待延迟自动接听，用户触碰屏幕则取消本次自动接听，交由用户自己操作
        cancelPendingAutoAnswer()

        if (WeChatData.index == 0) return

        Log.d(tag, "user touch detected, abort flow at step ${WeChatData.index}")
        abortFlow(FlowAbortReason.USER_TOUCH)
    }

    /** 记录一次自身操作，供触摸判定排除 */
    private fun markSelfAction() {
        lastSelfActionAt = SystemClock.uptimeMillis()
    }

    private val ticker = object : Runnable {
        override fun run() {
            if (WeChatData.index == 0) {
                stopTicker()
                return
            }
            if (SystemClock.uptimeMillis() - stepStartedAt > STEP_TIMEOUT_MS) {
                Log.d(tag, "step ${WeChatData.index} timeout")
                abortFlow(FlowAbortReason.TIMEOUT)
                return
            }
            try {
                step()
            } catch (e: Exception) {
                // 步骤抛异常时继续轮询只会反复失败，直接中止并告知用户
                Log.e(tag, "step failed", e)
                abortFlow(FlowAbortReason.ERROR)
                return
            }
            if (WeChatData.index != 0) {
                handler.postDelayed(this, TICK_INTERVAL_MS)
            } else {
                stopTicker()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        WeChatData.onFlowStarted = { startTicker() }
    }

    override fun onInterrupt() {
        // 服务被系统回收，属于外部原因，不向用户播报
        WeChatData.updateIndex(0)
        stopTicker()
    }

    override fun onDestroy() {
        WeChatData.onFlowStarted = null
        WeChatData.onFlowAborted = null
        cancelPendingAutoAnswer()
        cancelPendingSpeakerphone()
        cancelPendingReturnHome()
        stopWaitingForConnectedScreen()
        stopTicker()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // 用户开始一次触摸交互时中止自动流程。
        // 触摸事件不参与类名记录，否则会把 lastClassName 覆盖成具体控件，
        // 破坏 index=7 的弹窗判断。
        if (event.eventType == AccessibilityEvent.TYPE_TOUCH_INTERACTION_START) {
            handleUserTouch()
            return
        }

        if (!isRelevantWindowPackage(event.packageName?.toString()) &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            currentWindowClassName = event.className?.toString().orEmpty()
        }
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            lastClassName = event.className?.toString().orEmpty()
        }

        // 兜底：万一启动回调没触发，靠窗口事件把轮询拉起来
        if (WeChatData.index != 0 && !tickerRunning) {
            startTicker()
        }

        // 无主动呼出任务时，检查待确认接通状态、微信来电自动接听或挂断自动返回首页
        if (WeChatData.index == 0) {
            checkPendingConnectedConfirmation()
            if (WeChatData.autoAnswerEnabled) {
                val forceScan = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                checkIncomingWeChatCall(forceScan = forceScan)
            }
            if (WeChatData.returnHomeAfterWechatCall || WeChatData.returnHomeAfterPhoneCall) {
                checkCallHangupReturnHome(event)
            }
        }
    }

    private fun startTicker() {
        tickerRunning = true
        stepStartedAt = SystemClock.uptimeMillis()
        lastBackAt = 0L
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    private fun stopTicker() {
        tickerRunning = false
        handler.removeCallbacks(ticker)
    }

    /**
     * 复位状态机并通知上层播报
     *
     * 先复位再回调，避免回调里的播报期间状态机还停在半途。
     */
    private fun abortFlow(reason: FlowAbortReason) {
        WeChatData.updateIndex(0)
        stopTicker()
        WeChatData.onFlowAborted?.invoke(reason)
    }

    private fun advance(next: Int) {
        WeChatData.updateIndex(next)
        stepStartedAt = SystemClock.uptimeMillis()
    }

    private fun step() {
        when (WeChatData.index) {
            1 -> stepBackToIndex()
            2 -> stepOpenSearch()
            3 -> stepInputText()
            4 -> stepOpenContact()
            5 -> stepOpenMore()
            6 -> stepTapCallMenu()
            7 -> stepTapCallOption()
        }
    }

    /**
     * 回到微信首页，并点底部「微信」标签
     *
     * 微信还没启动或不在前台时什么都不做，避免误按返回键
     */
    private fun stepBackToIndex() {
        if (!isInWeChat()) return

        val root = weChatRoot() ?: return
        val tabs = root
            .findAccessibilityNodeInfosByViewId(WeChatId.TABLES.id)
            .orEmpty()
        if (tabs.isNotEmpty()) {
            tabs.first().click()
            advance(2)
            return
        }
        pressBack()
    }

    private fun stepOpenSearch() {
        val searchIcon = findById(WeChatId.SEARCH.id) ?: return
        if (searchIcon.click()) advance(3)
    }

    private fun stepInputText() {
        val input = findById(WeChatId.INPUT.id) ?: return
        if (input.input(WeChatData.value)) advance(4)
    }

    /**
     * 点搜索结果中精确匹配目标微信昵称的条目
     *
     * 输入文字后结果列表是异步加载的，按目标昵称精确校验后再点击，
     * 避免旧搜索结果未刷新、或群聊/近似名称排在第一条时误打错人。
     */
    private fun stepOpenContact() {
        val target = normalizeForMatch(WeChatData.value)
        val root = weChatRoot() ?: return
        val candidates = root.findAccessibilityNodeInfosByViewId(WeChatId.LIST.id)
        if (candidates.isNullOrEmpty()) return

        val matched = if (target.isNotEmpty()) {
            candidates.firstOrNull { row ->
                collectAllNodes(row).any { node ->
                    normalizeForMatch(nodeLabel(node).orEmpty()) == target
                }
            }
        } else {
            candidates.firstOrNull()
        }

        if (matched == null) {
            Log.d(tag, "stepOpenContact waiting for exact match: target=$target, candidates=${candidates.size}")
            return
        }
        if (matched.click()) advance(5)
    }

    private fun stepOpenMore() {
        val more = findById(WeChatId.MORE.id) ?: return
        if (more.click()) advance(6)
    }

    /**
     * 点「更多」面板里的通话入口
     *
     * 该菜单项是自绘控件，节点不可点，只能按屏幕坐标发手势
     */
    private fun stepTapCallMenu() {
        val menu = findByText(WeChatData.findText(false)) ?: return
        val rect = Rect()
        menu.getBoundsInScreen(rect)
        if (rect.width() <= 0 || rect.height() <= 0) return

        performClick(rect.exactCenterX(), rect.exactCenterY())
        advance(7)
    }

    /**
     * 点弹窗里的「视频通话」/「语音通话」
     *
     * 弹窗出现前不能点，否则会重复命中上一步的菜单项
     */
    private fun stepTapCallOption() {
        if (!isDialogVisible()) return

        val option = findByText(WeChatData.findText(true)) ?: return
        if (option.click()) {
            WeChatData.updateIndex(0)
            stopTicker()
        }
    }

    private fun isInWeChat(): Boolean =
        rootInActiveWindow?.packageName == WECHAT_PACKAGE

    /**
     * 是否处于通话选择弹窗
     *
     * 窗口事件可能尚未到达，因此同时看节点树里有没有弹窗容器
     */
    private fun isDialogVisible(): Boolean {
        if (isDialogClass(lastClassName)) return true

        val root = weChatRoot() ?: return false
        // 弹窗容器存在，且里面确实有通话选项，才算弹窗已就绪
        val hasDialogContainer = root
            .findAccessibilityNodeInfosByViewId(WeChatId.DIALOG.id)
            ?.isNotEmpty() == true
        return hasDialogContainer &&
            root.findAccessibilityNodeInfosByText(WeChatData.findText(true))?.isNotEmpty() == true
    }

    private fun isDialogClass(className: String): Boolean =
        className.contains(WeChatActivity.DIALOG.id) ||
            className == WeChatActivity.DIALOG_OLD.id

    private fun weChatRoot(): AccessibilityNodeInfo? =
        rootInActiveWindow?.takeIf { it.packageName?.toString() == WECHAT_PACKAGE }

    private fun findById(viewId: String): AccessibilityNodeInfo? =
        weChatRoot()?.findAccessibilityNodeInfosByViewId(viewId)?.firstOrNull()

    private fun findByText(text: String): AccessibilityNodeInfo? =
        weChatRoot()?.findAccessibilityNodeInfosByText(text)?.firstOrNull()

    private fun pressBack() {
        val now = SystemClock.uptimeMillis()
        if (now - lastBackAt < BACK_INTERVAL_MS) return
        lastBackAt = now
        markSelfAction()
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun AccessibilityNodeInfo?.click(): Boolean {
        this ?: return false
        markSelfAction()
        return if (isClickable) {
            performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            parent?.click() == true
        }
    }

    private fun AccessibilityNodeInfo?.input(text: String): Boolean {
        this ?: return false
        markSelfAction()
        return if (isEditable) {
            val arguments: Bundle = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        } else {
            parent?.input(text) == true
        }
    }

    private fun performClick(x: Float, y: Float) {
        if (hasForeignWindowAt(x, y)) {
            Log.w(tag, "skip click blocked by foreign window")
            return
        }
        markSelfAction()
        val gestureBuilder = GestureDescription.Builder()
        val path = Path()
        path.moveTo(x, y)
        gestureBuilder.addStroke(StrokeDescription(path, 0, 1))
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    /**
     * 检测微信视频/语音来电
     *
     * 兼容三种微信来电形态：
     * 1. 全屏来电界面（拒绝/接听两个圆形大按钮）
     * 2. 桌面/其他应用前台时的顶部通知横幅（无接听按钮，需先点击横幅展开为全屏来电界面）
     * 3. 微信 App 内部顶部的悬浮来电条（自带红色挂断与绿色接听按钮）
     */
    private fun checkIncomingWeChatCall(forceScan: Boolean = false) {
        val keyguard = getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguard?.isKeyguardLocked == true) return
        val now = SystemClock.uptimeMillis()
        if (now - lastAutoAnswerAt < AUTO_ANSWER_COOLDOWN_MS) return
        if (!forceScan && now - lastAutoAnswerScanAt < AUTO_ANSWER_SCAN_INTERVAL_MS) return
        lastAutoAnswerScanAt = now

        if (pendingAutoAnswerRunnable != null) return

        // 优先检查带“接听/拒绝”按钮的界面（图 1 全屏来电 或 图 3 微信内悬浮条）
        val directCandidate = findDirectAnswerCandidate()
        if (directCandidate != null) {
            cancelPendingBannerExpand()
            val runnable = Runnable {
                pendingAutoAnswerRunnable = null
                if (!WeChatData.autoAnswerEnabled || WeChatData.index != 0) return@Runnable
                if (SystemClock.uptimeMillis() - lastAutoAnswerAt < AUTO_ANSWER_COOLDOWN_MS) return@Runnable

                val confirmed = findDirectAnswerCandidate() ?: return@Runnable
                val clicked = tapAnswerNode(confirmed.answerNode)
                if (clicked) {
                    lastAutoAnswerAt = SystemClock.uptimeMillis()
                    Log.d(tag, "tapped answer button, waiting for connected screen, caller=${confirmed.matchedCaller}")
                    startWaitingForConnectedScreen(confirmed.matchedCaller)
                }
            }
            pendingAutoAnswerRunnable = runnable
            handler.postDelayed(runnable, AUTO_ANSWER_DELAY_MS)
            return
        }

        // 若无直接接听按钮，检查是否为图 2 顶部通知横幅（“邀请你语音通话/视频通话”），先点击展开进入全屏
        if (pendingBannerExpandRunnable != null) return
        if (now - lastBannerExpandAt < BANNER_EXPAND_COOLDOWN_MS) return

        val bannerNode = findIncomingCallBanner() ?: return
        val expandRunnable = Runnable {
            pendingBannerExpandRunnable = null
            if (!WeChatData.autoAnswerEnabled || WeChatData.index != 0) return@Runnable
            val confirmedBanner = findIncomingCallBanner() ?: return@Runnable
            if (tapBannerNode(confirmedBanner)) {
                lastBannerExpandAt = SystemClock.uptimeMillis()
                Log.d(tag, "expanded WeChat incoming call banner to full screen")
            }
        }
        pendingBannerExpandRunnable = expandRunnable
        handler.postDelayed(expandRunnable, BANNER_EXPAND_DELAY_MS)
    }

    private fun cancelPendingAutoAnswer() {
        pendingAutoAnswerRunnable?.let { handler.removeCallbacks(it) }
        pendingAutoAnswerRunnable = null
        cancelPendingBannerExpand()
    }

    private fun cancelPendingBannerExpand() {
        pendingBannerExpandRunnable?.let { handler.removeCallbacks(it) }
        pendingBannerExpandRunnable = null
    }

    /**
     * 点击接听后轮询检测是否真正进入已接通通话界面，确认接通后再触发语音播报
     */
    private fun startWaitingForConnectedScreen(caller: String?) {
        stopWaitingForConnectedScreen()
        cancelPendingSpeakerphone()
        pendingConnectedConfirmCaller = caller
        pendingConnectedConfirmUntil = SystemClock.uptimeMillis() + CONNECTED_CONFIRM_TIMEOUT_MS

        val poller = object : Runnable {
            override fun run() {
                if (pendingConnectedConfirmUntil == 0L ||
                    SystemClock.uptimeMillis() > pendingConnectedConfirmUntil
                ) {
                    stopWaitingForConnectedScreen()
                    return
                }
                if (isWeChatCallConnectedScreen()) {
                    val confirmedCaller = pendingConnectedConfirmCaller
                    stopWaitingForConnectedScreen()
                    onWeChatCallConnected(confirmedCaller, source = "poller")
                    return
                }
                handler.postDelayed(this, CONNECTED_CHECK_INTERVAL_MS)
            }
        }
        connectedCheckRunnable = poller
        handler.postDelayed(poller, CONNECTED_CHECK_INTERVAL_MS)
    }

    private fun checkPendingConnectedConfirmation() {
        if (pendingConnectedConfirmUntil == 0L) return
        if (SystemClock.uptimeMillis() > pendingConnectedConfirmUntil) {
            stopWaitingForConnectedScreen()
            return
        }
        if (isWeChatCallConnectedScreen()) {
            val confirmedCaller = pendingConnectedConfirmCaller
            stopWaitingForConnectedScreen()
            onWeChatCallConnected(confirmedCaller, source = "event")
        }
    }

    private fun onWeChatCallConnected(confirmedCaller: String?, source: String) {
        Log.d(tag, "confirmed WeChat call connected via $source, caller=$confirmedCaller")
        WeChatData.onAutoAnswered?.invoke(confirmedCaller)
        if (WeChatData.autoSpeakerphone) {
            scheduleAutoSpeakerphone()
        }
    }

    private fun cancelPendingSpeakerphone() {
        pendingSpeakerphoneRunnable?.let { handler.removeCallbacks(it) }
        pendingSpeakerphoneRunnable = null
    }

    private fun scheduleAutoSpeakerphone() {
        cancelPendingSpeakerphone()
        var attempts = 0
        val runnable = object : Runnable {
            override fun run() {
                if (!WeChatData.autoSpeakerphone) {
                    pendingSpeakerphoneRunnable = null
                    return
                }
                when (tryEnableWeChatSpeakerphone()) {
                    SpeakerphoneResult.TAPPED, SpeakerphoneResult.ALREADY_ON -> {
                        pendingSpeakerphoneRunnable = null
                    }
                    SpeakerphoneResult.NOT_FOUND -> {
                        attempts++
                        if (attempts < SPEAKERPHONE_MAX_ATTEMPTS) {
                            handler.postDelayed(this, SPEAKERPHONE_RETRY_INTERVAL_MS)
                        } else {
                            Log.d(tag, "auto speakerphone skipped: speaker-off control not found")
                            pendingSpeakerphoneRunnable = null
                        }
                    }
                }
            }
        }
        pendingSpeakerphoneRunnable = runnable
        handler.postDelayed(runnable, SPEAKERPHONE_INITIAL_DELAY_MS)
    }

    private enum class SpeakerphoneResult {
        TAPPED,
        ALREADY_ON,
        NOT_FOUND
    }

    private fun tryEnableWeChatSpeakerphone(): SpeakerphoneResult {
        val roots = getRelevantWindowRoots()
        for (root in roots) {
            if (root.packageName?.toString() != WECHAT_PACKAGE) continue
            val nodes = collectAllNodes(root)
            val alreadyOn = nodes.any { isSpeakerOnLabel(nodeLabel(it)) }
            if (alreadyOn) {
                Log.d(tag, "auto speakerphone already on")
                return SpeakerphoneResult.ALREADY_ON
            }
            val speakerOffNode = nodes.firstOrNull { isSpeakerOffLabel(nodeLabel(it)) } ?: continue
            if (tapBottomCircleOrParent(speakerOffNode, "speakerphone")) {
                Log.d(tag, "auto speakerphone enabled via ${nodeLabel(speakerOffNode)}")
                return SpeakerphoneResult.TAPPED
            }
        }
        return SpeakerphoneResult.NOT_FOUND
    }

    private fun isSpeakerOnLabel(label: String?): Boolean {
        if (label.isNullOrBlank()) return false
        return label.contains("扬声器已开") ||
            label.contains("关闭扬声器") ||
            label.contains("免提已开") ||
            label.contains("关闭免提")
    }

    private fun isSpeakerOffLabel(label: String?): Boolean {
        if (label.isNullOrBlank()) return false
        if (isSpeakerOnLabel(label)) return false
        return label.contains("扬声器已关") ||
            label.contains("打开扬声器") ||
            label.contains("开启扬声器") ||
            label.contains("免提已关") ||
            label.contains("开启免提") ||
            label.contains("打开免提") ||
            label == "免提" ||
            label == "扬声器"
    }

    private fun stopWaitingForConnectedScreen() {
        connectedCheckRunnable?.let { handler.removeCallbacks(it) }
        connectedCheckRunnable = null
        pendingConnectedConfirmCaller = null
        pendingConnectedConfirmUntil = 0L
    }

    /**
     * 判断当前是否已进入微信通话接通页面（含计时器/麦克风等已接通控件，且不再有接听按钮）
     */
    private fun isWeChatCallConnectedScreen(): Boolean {
        val roots = getRelevantWindowRoots()
        for (root in roots) {
            if (root.packageName?.toString() != WECHAT_PACKAGE) continue
            val nodes = collectAllNodes(root)
            val texts = nodes.mapNotNull { nodeLabel(it)?.takeIf { s -> s.isNotBlank() } }

            val hasAnswerButton = nodes.any { isPrimaryAnswerNode(it) }
            if (hasAnswerButton) continue

            val hasHangup = texts.any { it == "挂断" || it.contains("挂断") }
            if (!hasHangup) continue

            val hasCallTimer = texts.any { CALL_TIMER_REGEX.matches(it) }
            val hasConnectedControls = texts.any {
                it.contains("麦克风") ||
                    it.contains("扬声器") ||
                    it.contains("免提") ||
                    it.contains("静音") ||
                    it.contains("翻转镜头") ||
                    it.contains("切到语音") ||
                    it.contains("模糊背景")
            }
            if (hasCallTimer || hasConnectedControls) {
                return true
            }
        }
        return false
    }

    private data class IncomingCallCandidate(
        val answerNode: AccessibilityNodeInfo,
        val matchedCaller: String?
    )

    private fun findDirectAnswerCandidate(): IncomingCallCandidate? {
        val roots = getRelevantWindowRoots()
        for (root in roots) {
            val pkg = root.packageName?.toString().orEmpty()
            if (pkg != WECHAT_PACKAGE) continue

            val nodes = collectAllNodes(root)
            val hasRejectButton = nodes.any { isRejectCallNode(it) }
            val answerNode = nodes.firstOrNull { isPrimaryAnswerNode(it) }
            if (!hasRejectButton || answerNode == null) {
                if (lastClassName == WeChatActivity.VOIP.id) {
                    val sampleTexts = nodes.mapNotNull { nodeLabel(it) }.distinct().take(15)
                    Log.d(tag, "VOIP screen detected but buttons missing: hasReject=$hasRejectButton, answer=$answerNode, texts=$sampleTexts")
                }
                continue
            }

            val texts = nodes.mapNotNull { nodeLabel(it)?.takeIf { s -> s.isNotBlank() } }
            val matchedCaller = matchAllowedCaller(texts)
            if (WeChatData.autoAnswerContactsOnly && matchedCaller == null) {
                Log.d(tag, "incoming call ignored (not in contacts): texts=${texts.take(10)}")
                continue
            }
            return IncomingCallCandidate(answerNode = answerNode, matchedCaller = matchedCaller)
        }
        return null
    }

    /**
     * 查找图 2 的顶部来电通知横幅（无直接接听按钮，包含“邀请你语音通话/视频通话”）
     */
    private fun findIncomingCallBanner(): AccessibilityNodeInfo? {
        val statusBarHeight = getStatusBarHeight()
        val minBannerWidth = (resources.displayMetrics.density * 48f).toInt()
        val screenHeight = resources.displayMetrics.heightPixels
        val roots = getRelevantWindowRoots()
        for (root in roots) {
            val pkg = root.packageName?.toString().orEmpty()
            if (!isSystemUiPackage(pkg)) continue

            val nodes = collectAllNodes(root)
            val inviteNode = nodes.firstOrNull { node ->
                val label = nodeLabel(node) ?: return@firstOrNull false
                if (!label.contains("邀请你语音通话") && !label.contains("邀请你视频通话")) {
                    return@firstOrNull false
                }
                val rect = Rect()
                node.getBoundsInScreen(rect)
                // 排除状态栏小图标（如 Rect(181, 20 - 231, 56)），只匹配状态栏下方的真实悬浮横幅
                rect.top >= statusBarHeight &&
                    rect.bottom <= screenHeight * 0.35f &&
                    rect.width() >= minBannerWidth
            } ?: continue

            val texts = nodes.mapNotNull { nodeLabel(it)?.takeIf { s -> s.isNotBlank() } }
            if (WeChatData.autoAnswerContactsOnly && matchAllowedCaller(texts) == null) {
                continue
            }
            return inviteNode
        }
        return null
    }

    private fun getStatusBarHeight(): Int {
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) {
            resources.getDimensionPixelSize(resId)
        } else {
            (resources.displayMetrics.density * 28f).toInt()
        }
    }

    private fun getRelevantWindowRoots(): List<AccessibilityNodeInfo> {
        return buildList {
            windows?.mapNotNullTo(this) { window ->
                window.root?.takeIf { isRelevantWindowPackage(it.packageName?.toString()) }
            }
            rootInActiveWindow
                ?.takeIf { isRelevantWindowPackage(it.packageName?.toString()) }
                ?.let { if (it !in this) add(it) }
        }
    }

    private fun isSystemUiPackage(pkg: String): Boolean =
        pkg == "com.android.systemui" || pkg == "com.miui.systemui"

    private fun isRelevantWindowPackage(pkg: String?): Boolean {
        if (pkg.isNullOrBlank()) return false
        return pkg == WECHAT_PACKAGE ||
            pkg == packageName ||
            isSystemUiPackage(pkg) ||
            isInCallPackage(pkg)
    }

    private fun hasForeignWindowAt(x: Float, y: Float): Boolean {
        val windowList = windows ?: return false
        for (window in windowList) {
            val root = window.root ?: continue
            val pkg = root.packageName?.toString().orEmpty()
            if (pkg == WECHAT_PACKAGE || pkg == packageName || isSystemUiPackage(pkg)) continue
            val bounds = Rect()
            if (runCatching { root.getBoundsInScreen(bounds) }.isFailure) continue
            if (bounds.contains(x.toInt(), y.toInt())) return true
        }
        return false
    }

    private fun collectAllNodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val result = ArrayList<AccessibilityNodeInfo>(64)
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty() && result.size < 500) {
            val node = queue.removeFirst()
            result.add(node)
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.addLast(it) }
            }
        }
        return result
    }

    private fun nodeLabel(node: AccessibilityNodeInfo): String? {
        val text = node.text?.toString()?.trim().orEmpty()
        if (text.isNotEmpty()) return text
        return node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun isRejectCallNode(node: AccessibilityNodeInfo): Boolean {
        val label = nodeLabel(node) ?: return false
        return label == "挂断" || label == "拒绝" || label.contains("拒绝") || label.contains("挂断")
    }

    private fun isPrimaryAnswerNode(node: AccessibilityNodeInfo): Boolean {
        val label = nodeLabel(node) ?: return false
        if (label.contains("语音接听") || label.contains("切到语音") || label.contains("切换")) {
            return false
        }
        return label == "接听" || label == "接受" ||
            label.contains("接听视频") || label.contains("接听语音")
    }

    private fun normalizeForMatch(value: String): String {
        return value.replace(Regex("[\\s\\u200B-\\u200F\\uFEFF\\u00AD]"), "")
    }

    private fun matchAllowedCaller(texts: List<String>): String? {
        val allowed = WeChatData.allowedCallers
        if (allowed.isEmpty()) return null
        val normalizedTexts = texts.map(::normalizeForMatch).filter { it.isNotEmpty() }
        return allowed.firstOrNull { caller ->
            val normalizedCaller = normalizeForMatch(caller)
            normalizedCaller.isNotEmpty() &&
                normalizedTexts.any { text -> text == normalizedCaller }
        }
    }

    /**
     * 点击接听按钮
     *
     * 图 1 全屏界面中“接听”可能是位于绿色圆形图标正下方的独立 TextView：
     * 1. 先查找尺寸合理的局部可点击父容器（避免误点占满整屏的根布局）
     * 2. 若无局部可点击父容器，查找位于“接听”文字正上方的可点击圆形按钮节点
     * 3. 最后兜底使用屏幕坐标手势点击
     */
    private fun tapAnswerNode(node: AccessibilityNodeInfo): Boolean {
        return tapBottomCircleOrParent(node, "answer")
    }

    private fun tapBottomCircleOrParent(node: AccessibilityNodeInfo, actionName: String): Boolean {
        val activeRoot = rootInActiveWindow ?: return false
        if (activeRoot.packageName?.toString() != WECHAT_PACKAGE) return false
        val metrics = resources.displayMetrics
        val maxW = (metrics.widthPixels * 0.48f).toInt()
        val maxH = (metrics.heightPixels * 0.35f).toInt()

        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.width() <= 0 || rect.height() <= 0) return false

        // 全屏来电/通话页（控件位于屏幕下半区）：
        // 微信节点可能本身就是包含圆形图标+底部文字的整块按钮（高约 90dp），
        // 也可能只是圆形图标正下方的独立文字节点。统一使用真实手势点击圆形区域。
        val isFullScreenBottomLabel = rect.top > metrics.heightPixels * 0.65f
        if (isFullScreenBottomLabel) {
            if (rect.height() >= metrics.density * 56f) {
                val clickX = rect.exactCenterX()
                val clickY = rect.top + rect.height() * 0.40f
                Log.d(tag, "tapped fullscreen $actionName via button-top gesture ($clickX, $clickY), labelRect=$rect")
                performClick(clickX, clickY)
                return true
            }

            val root: AccessibilityNodeInfo? = activeRoot
            if (root != null) {
                val maxHorizOffset = metrics.density * 60f
                val minVertGap = metrics.density * 4f
                val maxVertGap = metrics.density * 140f
                val iconButton = collectAllNodes(root).firstOrNull { candidate ->
                    if (!candidate.isClickable) return@firstOrNull false
                    val cr = Rect()
                    candidate.getBoundsInScreen(cr)
                    cr.width() in 1..maxW &&
                        cr.height() in 1..maxH &&
                        kotlin.math.abs(cr.exactCenterX() - rect.exactCenterX()) <= maxHorizOffset &&
                        rect.top - cr.bottom >= -minVertGap &&
                        rect.top - cr.centerY() <= maxVertGap
                }
                if (iconButton != null) {
                    val cr = Rect()
                    iconButton.getBoundsInScreen(cr)
                    Log.d(tag, "tapped fullscreen $actionName via gesture on icon bounds=$cr")
                    performClick(cr.exactCenterX(), cr.exactCenterY())
                    return true
                }
            }

            val clickY = (rect.top - (metrics.density * 52f)).coerceAtLeast(0f)
            Log.d(tag, "tapped fullscreen $actionName via offset gesture (${rect.exactCenterX()}, $clickY), labelRect=$rect")
            performClick(rect.exactCenterX(), clickY)
            return true
        }

        // 悬浮横幅或常规可点击父容器：先找尺寸合理的局部可点击父容器
        var curr: AccessibilityNodeInfo? = node
        var depth = 0
        while (curr != null && depth < 8) {
            if (curr.isClickable) {
                val r = Rect()
                curr.getBoundsInScreen(r)
                if (r.width() in 1..maxW && r.height() in 1..maxH) {
                    markSelfAction()
                    if (curr.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        Log.d(tag, "tapped $actionName via clickable parent depth=$depth bounds=$r")
                        return true
                    }
                }
            }
            curr = curr.parent
            depth++
        }

        Log.d(tag, "tapped $actionName via center gesture (${rect.exactCenterX()}, ${rect.exactCenterY()})")
        performClick(rect.exactCenterX(), rect.exactCenterY())
        return true
    }

    private fun tapBannerNode(node: AccessibilityNodeInfo): Boolean {
        if (!isSystemUiPackage(node.packageName?.toString().orEmpty())) return false
        val metrics = resources.displayMetrics
        val statusBarHeight = getStatusBarHeight()
        val maxBannerH = (metrics.heightPixels * 0.35f).toInt()
        var curr: AccessibilityNodeInfo? = node
        var depth = 0
        while (curr != null && depth < 12) {
            if (curr.isClickable) {
                val r = Rect()
                curr.getBoundsInScreen(r)
                if (r.height() in 20..maxBannerH && r.top >= statusBarHeight) {
                    markSelfAction()
                    if (curr.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        Log.d(tag, "tapped banner via clickable parent depth=$depth bounds=$r")
                        return true
                    }
                }
            }
            curr = curr.parent
            depth++
        }

        // 部分系统通知横幅（如小米悬浮通知）用 OnTouchListener 响应点击而不暴露 isClickable，
        // 在确认横幅文本仍在顶部可见区域时，使用手势点击横幅中心将其展开为全屏来电界面
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.width() <= 0 || rect.height() < 15 || rect.top < statusBarHeight || rect.bottom > maxBannerH) {
            return false
        }
        Log.d(tag, "tapped banner via gesture (${rect.exactCenterX()}, ${rect.exactCenterY()}), bounds=$rect")
        performClick(rect.exactCenterX(), rect.exactCenterY())
        return true
    }

    /**
     * 监听微信语音/视频与系统电话的挂断状态，满足条件时自动返回一见首页
     */
    private fun checkCallHangupReturnHome(event: AccessibilityEvent) {
        val now = SystemClock.uptimeMillis()
        val isWindowChanged = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        if (!isWindowChanged && now - lastCallStateScanAt < CALL_STATE_SCAN_INTERVAL_MS) return
        lastCallStateScanAt = now

        val eventPkg = event.packageName?.toString().orEmpty()
        if (WeChatData.returnHomeAfterWechatCall) {
            checkWeChatHangup(eventPkg, isWindowChanged)
        } else {
            inWeChatVoipCall = false
        }

        if (WeChatData.returnHomeAfterPhoneCall) {
            checkPhoneHangup(eventPkg, isWindowChanged)
        } else {
            inPhoneCall = false
        }
    }

    private fun checkWeChatHangup(eventPkg: String, isWindowChanged: Boolean) {
        val voipActiveNow = isWeChatVoipWindowActive()
        if (voipActiveNow) {
            if (hasWeChatCallEndedText()) {
                inWeChatVoipCall = false
                scheduleReturnHome(delayMs = RETURN_HOME_END_TEXT_DELAY_MS, reason = "wechat_end_text")
                return
            }
            if (!inWeChatVoipCall) {
                Log.d(tag, "entered WeChat VoIP call state")
            }
            inWeChatVoipCall = true
            cancelPendingReturnHome()
            return
        }

        if (!inWeChatVoipCall) return
        if (hasWeChatFloatingCallTimer()) return

        if ((isWindowChanged || currentWindowClassName != WeChatActivity.VOIP.id) && eventPkg != packageName) {
            inWeChatVoipCall = false
            scheduleReturnHome(delayMs = RETURN_HOME_WINDOW_EXIT_DELAY_MS, reason = "wechat_window_closed")
        }
    }

    private fun checkPhoneHangup(eventPkg: String, isWindowChanged: Boolean) {
        val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
        val isAudioInCall = audioManager?.mode == AudioManager.MODE_IN_CALL
        val isInCallWindow = isInCallPackage(eventPkg) || isInCallUiVisibleOnScreen()

        if (isInCallWindow) {
            if (hasPhoneCallEndedText()) {
                inPhoneCall = false
                scheduleReturnHome(delayMs = RETURN_HOME_END_TEXT_DELAY_MS, reason = "phone_end_text")
                return
            }
            if (!inPhoneCall) {
                Log.d(tag, "entered system phone call state (pkg=$eventPkg)")
            }
            inPhoneCall = true
            cancelPendingReturnHome()
            return
        }

        if (isAudioInCall) {
            inPhoneCall = true
            cancelPendingReturnHome()
            return
        }

        if (!inPhoneCall) return
        if (isWindowChanged || !isAudioInCall) {
            inPhoneCall = false
            scheduleReturnHome(delayMs = RETURN_HOME_WINDOW_EXIT_DELAY_MS, reason = "phone_call_ended")
        }
    }

    private fun isWeChatVoipWindowActive(): Boolean {
        if (currentWindowClassName == WeChatActivity.VOIP.id &&
            rootInActiveWindow?.packageName?.toString() == WECHAT_PACKAGE
        ) {
            return true
        }
        if (isWeChatCallConnectedScreen()) return true

        val roots = getRelevantWindowRoots()
        for (root in roots) {
            if (root.packageName?.toString() != WECHAT_PACKAGE) continue
            val texts = collectAllNodes(root).mapNotNull { nodeLabel(it) }
            val hasWaitingText = texts.any {
                it.contains("等待对方接受邀请") ||
                    it.contains("等待对方接听")
            }
            val hasOutboundVoipControls = texts.any { it == "取消" } &&
                texts.any {
                    it == "最小化" ||
                        it.contains("麦克风") ||
                        it.contains("扬声器") ||
                        it.contains("切到语音")
                }
            if (hasWaitingText || hasOutboundVoipControls) {
                return true
            }
        }
        return false
    }

    private fun hasWeChatCallEndedText(): Boolean {
        val roots = getRelevantWindowRoots()
        for (root in roots) {
            if (root.packageName?.toString() != WECHAT_PACKAGE) continue
            val texts = collectAllNodes(root).mapNotNull { nodeLabel(it) }
            if (texts.any { isCallEndedLabel(it) }) return true
        }
        return false
    }

    private fun hasWeChatFloatingCallTimer(): Boolean {
        val roots = getRelevantWindowRoots()
        for (root in roots) {
            if (root.packageName?.toString() != WECHAT_PACKAGE) continue
            val nodes = collectAllNodes(root)
            val texts = nodes.mapNotNull { nodeLabel(it) }
            if (texts.any { CALL_TIMER_REGEX.matches(it) || it == "等待接听" }) return true
        }
        return false
    }

    private fun isInCallPackage(pkg: String): Boolean {
        if (pkg.isBlank()) return false
        return pkg.contains("incallui", ignoreCase = true) ||
            pkg == "com.google.android.dialer" ||
            pkg == "com.android.dialer"
    }

    private fun isInCallUiVisibleOnScreen(): Boolean {
        val roots = getRelevantWindowRoots()
        for (root in roots) {
            val pkg = root.packageName?.toString().orEmpty()
            if (!isInCallPackage(pkg)) continue
            val texts = collectAllNodes(root).mapNotNull { nodeLabel(it) }
            if (texts.any {
                    it.contains("挂断") ||
                        it.contains("结束通话") ||
                        it.contains("正在通话") ||
                        it.contains("通话中") ||
                        CALL_TIMER_REGEX.matches(it)
                }
            ) {
                return true
            }
        }
        return false
    }

    private fun hasPhoneCallEndedText(): Boolean {
        val roots = getRelevantWindowRoots()
        for (root in roots) {
            val pkg = root.packageName?.toString().orEmpty()
            if (!isInCallPackage(pkg)) continue
            val texts = collectAllNodes(root).mapNotNull { nodeLabel(it) }
            if (texts.any { isCallEndedLabel(it) }) return true
        }
        return false
    }

    private fun isCallEndedLabel(label: String): Boolean {
        return label.contains("通话结束") ||
            label.contains("通话已结束") ||
            label.contains("对方已挂断") ||
            label.contains("对方已拒绝") ||
            label.contains("对方已取消") ||
            label.contains("对方无应答") ||
            label.contains("无人接听") ||
            label.contains("对方忙") ||
            label.contains("忙线中") ||
            label == "已挂断" ||
            label == "已拒绝" ||
            label == "已取消" ||
            label == "已结束"
    }

    private fun scheduleReturnHome(delayMs: Long, reason: String) {
        val now = SystemClock.uptimeMillis()
        if (now - lastReturnHomeAt < RETURN_HOME_COOLDOWN_MS) return
        cancelPendingReturnHome()
        val runnable = Runnable {
            pendingReturnHomeRunnable = null
            if (reason.startsWith("wechat") && !WeChatData.returnHomeAfterWechatCall) return@Runnable
            if (reason.startsWith("phone") && !WeChatData.returnHomeAfterPhoneCall) return@Runnable
            returnHomeToFirstSight(reason)
        }
        pendingReturnHomeRunnable = runnable
        handler.postDelayed(runnable, delayMs)
    }

    private fun cancelPendingReturnHome() {
        pendingReturnHomeRunnable?.let { handler.removeCallbacks(it) }
        pendingReturnHomeRunnable = null
    }

    private fun returnHomeToFirstSight(reason: String) {
        val now = SystemClock.uptimeMillis()
        if (now - lastReturnHomeAt < RETURN_HOME_COOLDOWN_MS) return
        lastReturnHomeAt = now

        val isDefaultHome = isFirstSightDefaultHome()
        if (isDefaultHome) {
            val homeOk = performGlobalAction(GLOBAL_ACTION_HOME)
            Log.d(tag, "returning to FirstSight via GLOBAL_ACTION_HOME=$homeOk, reason=$reason")
            if (homeOk) return
        }

        Log.d(tag, "returning to FirstSight via startActivity, isDefaultHome=$isDefaultHome, reason=$reason")
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        runCatching { startActivity(intent) }
            .onFailure { Log.e(tag, "failed to return to FirstSight home screen", it) }
    }

    private fun isFirstSightDefaultHome(): Boolean {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolved = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == packageName
    }

    private companion object {
        const val WECHAT_PACKAGE = "com.tencent.mm"
        const val TICK_INTERVAL_MS = 400L
        const val STEP_TIMEOUT_MS = 5_000L
        const val BACK_INTERVAL_MS = 800L
        const val AUTO_ANSWER_SCAN_INTERVAL_MS = 250L
        const val AUTO_ANSWER_DELAY_MS = 800L
        const val AUTO_ANSWER_COOLDOWN_MS = 3_500L
        const val BANNER_EXPAND_DELAY_MS = 200L
        const val BANNER_EXPAND_COOLDOWN_MS = 2_500L
        const val CONNECTED_CHECK_INTERVAL_MS = 300L
        const val CONNECTED_CONFIRM_TIMEOUT_MS = 10_000L
        const val SPEAKERPHONE_INITIAL_DELAY_MS = 350L
        const val SPEAKERPHONE_RETRY_INTERVAL_MS = 500L
        const val SPEAKERPHONE_MAX_ATTEMPTS = 4
        const val CALL_STATE_SCAN_INTERVAL_MS = 250L
        const val RETURN_HOME_END_TEXT_DELAY_MS = 700L
        const val RETURN_HOME_WINDOW_EXIT_DELAY_MS = 500L
        const val RETURN_HOME_COOLDOWN_MS = 2_500L
        val CALL_TIMER_REGEX = Regex("^\\d{2}:\\d{2}(:\\d{2})?$")

        /**
         * 自身操作后的忽略窗口
         *
         * 只用于吸收事件派发到抵达系统的那点延迟。窗口必须明显小于
         * [TICK_INTERVAL_MS]，否则每轮都处在忽略期，用户真实触摸会被漏检。
         */
        const val SELF_ACTION_GRACE_MS = 200L
    }
}
