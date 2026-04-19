package com.ndjc.feature.showcase

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.TimeZone
import org.json.JSONObject
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow



/**
 * Chat 子域：状态机（逻辑模块内）
 *
 * 目标：
 * - UI 包只读 ShowcaseChatUiState，只调用“动作函数”
 * - 轮询/刷新/发送/失败回退/去重 在这里完成
 *
 * 注意：
 * - 这里做的是“最小可用 MVP”：
 *   1) ensureConversation
 *   2) fetchMessages
 *   3) sendMessage
 *   4) optional: pollLatest (UI 或 VM 定时触发)
 */
class ShowcaseChatDomain(
    private val repo: ShowcaseChatRepository,
    private val cloudRepo: ShowcaseChatCloudRepository? = null,
    private val logTag: String = "ShowcaseChatDomain"
)
{
    private fun isAppOwnedLocalFileUri(context: Context, uriString: String): Boolean {
        return runCatching {
            val uri = Uri.parse(uriString)
            val file =
                when {
                    uri.scheme == "file" -> uri.path?.let { File(it) }
                    uri.scheme.isNullOrBlank() && !uri.path.isNullOrBlank() -> File(uri.path!!)
                    else -> null
                } ?: return@runCatching false

            val target = file.canonicalFile
            val cacheRoot = context.cacheDir.canonicalFile
            val externalRoot = context.getExternalFilesDir(null)?.canonicalFile

            target.path.startsWith(cacheRoot.path) ||
                    (externalRoot != null && target.path.startsWith(externalRoot.path))
        }.getOrDefault(false)
    }

    private fun deleteAppOwnedLocalFileUri(context: Context, uriString: String) {
        runCatching {
            if (!isAppOwnedLocalFileUri(context, uriString)) return@runCatching
            val uri = Uri.parse(uriString)
            val path = uri.path ?: return@runCatching
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private fun isCloudOrRelay(): Boolean =
        (repo.isChatRelayEnabled() || repo.isChatCloudEnabled()) && cloudRepo != null


    private fun buildConversationId(storeId: String, clientId: String): String {
        return if (isCloud()) "cloud:$storeId:$clientId"
        else "local:$storeId:$clientId"
    }
    // 兼容旧代码：原来很多地方调用 isCloud()
// 现在 relay-only 也算“云端能力可用”
    private fun isCloud(): Boolean = isCloudOrRelay()



    private fun parseClientIdFromConversationId(conversationId: String): String {
        // cloud:<storeId>:<clientId>
        val parts = conversationId.split(":")
        return parts.getOrNull(2) ?: "unknown"
    }


    // 对 UI 暴露的状态
    // ✅ 对 UI 暴露的状态：Domain 不依赖 Compose（用 StateFlow）
    private val _uiState = MutableStateFlow(ShowcaseChatUiState())
    val uiStateFlow: StateFlow<ShowcaseChatUiState> = _uiState.asStateFlow()

    // 为了兼容现有写法：保持 uiState = uiState.copy(...) 可用
    var uiState: ShowcaseChatUiState
        get() = _uiState.value
        private set(value) { _uiState.value = value }

    // 当前身份
    private var identity: ShowcaseChatIdentity? = null

    // ✅ 当前打开会话的“视角角色”：仅用于把 canonical direction 映射成 UI direction
// - "merchant"：不反转
// - 其他（client/user）：反转
    private var perspectiveRoleForUi: String = "client"

    // 消息去重集合（避免轮询重复插入）
    private val seenIds = LinkedHashSet<String>(512)

    // 本地时间格式（UI 展示用）
    // ✅ Chat 时间展示约定：
    // - 必须包含日期：UI 侧要做“跨天显示日期”
    // - 跟随系统 12/24 小时制：
    //   24 小时制 -> yyyy-MM-dd HH:mm
    //   12 小时制 -> yyyy-MM-dd a hh:mm
    // - 精度到分钟：UI 侧做“同一分钟只显示一次”
    // 例：
    //   2026-02-24 19:23
    //   2026-02-24 PM 07:23
    private var timeFormatContext: Context? = null

    private fun isSystem24Hour(): Boolean {
        val ctx = timeFormatContext ?: return false
        return android.text.format.DateFormat.is24HourFormat(ctx)
    }

    private fun formatChatTime(date: Date): String {
        val pattern = if (isSystem24Hour()) {
            "yyyy-MM-dd HH:mm"
        } else {
            "yyyy-MM-dd a hh:mm"
        }
        return SimpleDateFormat(pattern, Locale.US).format(date)
    }


    /**
     * 最小身份生成：
     * - storeId：由业务给（比如当前店铺/配置）
     * - clientId：匿名访客本地持久化（SharedPreferences）
     */
    fun ensureIdentity(context: Context, storeId: String): ShowcaseChatIdentity {
        timeFormatContext = context.applicationContext
        val clientId = ensureClientId(context)
        val idt = ShowcaseChatIdentity(storeId = storeId, clientId = clientId)
        identity = idt
        return idt
    }

    fun resolveClientConversationId(context: Context, storeId: String): String {
        val idt = ensureIdentity(context, storeId)
        return buildConversationId(storeId = idt.storeId, clientId = idt.clientId)
    }

    fun restoreSnapshot(
        snapshot: ShowcaseChatUiState,
        perspectiveRole: String
    ) {
        perspectiveRoleForUi = perspectiveRole
        uiState = snapshot
    }

    /**
     * 打开会话：确保 conversation 存在，并拉取最近消息
     */
    suspend fun openLocal(context: Context, storeId: String) {
        // ✅ 客户视角打开：UI 方向需要反转 canonical direction
        perspectiveRoleForUi = "client"

        val idt = ensureIdentity(context, storeId)
        val conversationId = buildConversationId(storeId = idt.storeId, clientId = idt.clientId)

        try {
// ✅ 云端/relay 模式：只拉已有消息落本地；不要在“仅打开 chat”时就提前创建 conversation
            if (isCloud()) {
                withContext(Dispatchers.IO) {
                    if (repo.isChatRelayEnabled()) {
                        val traceId = "T${System.currentTimeMillis()}_openClient"
                        repo.consumeRelayForClient(
                            context = context,
                            storeId = idt.storeId,
                            clientId = idt.clientId,
                            traceId = traceId
                        )
                    } else if (repo.isChatCloudEnabled()) {
                        // 兼容：如果未来切回“云端保存 chat_messages”
                        repo.syncConversationFromCloud(
                            context = context,
                            storeId = idt.storeId,
                            conversationId = conversationId,
                            perspectiveRole = "client",
                            clientId = idt.clientId
                        )

                        cloudRepo?.markMerchantMessagesRead(
                            storeId = idt.storeId,
                            conversationId = conversationId,
                            clientId = idt.clientId
                        )
                    } else {
                        // no-op
                    }

                }
            }

            val result: Pair<List<ChatMessageEntity>, Int> = withContext(Dispatchers.IO) {
                // ✅ 用户视角打开：把“商家发来的消息”（canonical=out）标记为已读
                daoMarkClientRead(context, conversationId)

                val list = repo.listLocal(context, conversationId)
                val unread = repo.countUnreadForUserEntry(context, conversationId)
                list to unread
            }
            val pinned = repo.isThreadPinned(context, storeId, conversationId)

            uiState = uiState.copy(
                isConnecting = false,
                conversationId = conversationId,
                useStoreTitle = true,
                canTogglePinned = false,
                messages = result.first.map(::entityToUi),
                unreadCount = result.second,
                scrollToBottomSignal = System.currentTimeMillis(),
                errorMessage = null,

                isPinned = pinned,
                subtitle = if (pinned) "Pinned" else ""
            )


        } catch (t: Throwable) {
            uiState = uiState.copy(
                isConnecting = false,
                errorMessage = t.message ?: "openLocal failed"
            )
        }
    }

    suspend fun openMerchantConversation(
        context: Context,
        storeId: String,
        conversationId: String
    ) {
        // ✅ 商家视角打开：UI 方向不反转 canonical direction
        perspectiveRoleForUi = "merchant"

        try {
            withContext(Dispatchers.IO) {
                repo.syncMerchantThreadMetaFromCloud(
                    context = context,
                    storeId = storeId,
                    traceId = "T${System.currentTimeMillis()}_syncMetaBeforeOpen"
                )
            }

            // ✅ relay-only / cloud 转发模式：先把 relay 吃进本地，再读 Room
            withContext(Dispatchers.IO) {
                if (repo.isChatRelayEnabled()) {
                    val traceId = "T${System.currentTimeMillis()}_openMerchant"
                    repo.consumeRelayForMerchant(
                        context = context,
                        storeId = storeId,
                        traceId = traceId
                    )
                } else if (isCloud()) {
                    // 兼容：如果你未来切回“云端保存 chat_messages”
                    repo.syncConversationFromCloud(
                        context = context,
                        storeId = storeId,
                        conversationId = conversationId,
                        perspectiveRole = "merchant",
                        clientId = null
                    )

                    // 云端 messages 才需要标已读；relay-only 不需要
                    cloudRepo?.markUserMessagesRead(
                        storeId = storeId,
                        conversationId = conversationId
                    )
                } else {
                    // ✅ 必须有最终 else：避免 if 作为 withContext 最后一个表达式时触发编译错误
                }
            }

            var result: Pair<List<ChatMessageEntity>, Int> = withContext(Dispatchers.IO) {
                repo.markAllRead(context, conversationId)
                val list = repo.listLocal(context, conversationId)
                val unread = repo.countUnread(context, conversationId)
                list to unread
            }

            if (result.first.isEmpty() && isCloud()) {
                android.util.Log.e(
                    "ChatTrace",
                    "openMerchantConversation local empty after first sync, retry cloud sync conv=$conversationId"
                )

                withContext(Dispatchers.IO) {
                    repo.syncConversationFromCloud(
                        context = context,
                        storeId = storeId,
                        conversationId = conversationId,
                        perspectiveRole = "merchant",
                        clientId = null,
                        traceId = "T${System.currentTimeMillis()}_openMerchantRetry"
                    )
                }

                result = withContext(Dispatchers.IO) {
                    repo.markAllRead(context, conversationId)
                    val list = repo.listLocal(context, conversationId)
                    val unread = repo.countUnread(context, conversationId)
                    list to unread
                }
            }

            val clientId = parseClientIdFromConversationId(conversationId)
// ✅ 每次打开会话都从同一份数据源读取 pinned，避免沿用旧 uiState
            val pinned = repo.isThreadPinned(context, storeId, conversationId)

            val finalTitle: String = withContext(Dispatchers.IO) {
                repo.resolveMerchantThreadDisplayName(
                    context = context,
                    storeId = storeId,
                    conversationId = conversationId
                )
            }

            uiState = uiState.copy(
                isConnecting = false,
                conversationId = conversationId,
                title = finalTitle,
                useStoreTitle = false,
                canTogglePinned = true,
                messages = result.first.map(::entityToUi),
                unreadCount = result.second,
                scrollToBottomSignal = System.currentTimeMillis(),
                errorMessage = null,

                // ✅ 同源：来自 meta
                isPinned = pinned,
                subtitle = if (pinned) "Pinned" else ""
            )



        } catch (t: Throwable) {
            uiState = uiState.copy(
                isConnecting = false,
                errorMessage = t.message ?: "openMerchantConversation failed"
            )
        }
    }


    /**
     * 拉取最近消息（全量拉最近 N 条即可，MVP）
     */
    /**
     * 本地刷新：重新从 Room 读取（替代云端 fetchMessages）
     */
    /**
     * 本地模式：refresh 由 VM 调用 openLocal(context, storeId) 完成。
     * 这里保留函数签名，避免旧调用处编译失败。
     */
    suspend fun refreshLatest(limit: Int = 80) {
        uiState = uiState.copy(isRefreshing = false)
    }



    /**
     * 轮询拉最新（你可以在 VM 里每 3~5 秒触发一次，或页面 onResume 触发）
     * - MVP：简单全量拉最近 N 条，然后去重
     */
    /**
     * 本地模式：不需要云端轮询
     */
    suspend fun pollLatest(limit: Int = 50) {
        // no-op
    }

    // ---- Quote encoding (persisted in message.text, UI will parse & render) ----
// ---- Quote & Images encoding (persisted in message.text, UI will parse & render) ----
    private  val NDJC_QUOTE_START = "⟪Q⟫"
    private  val NDJC_QUOTE_END = "⟪/Q⟫"

    private  val NDJC_IMG_START = "⟪I⟫"
    private  val NDJC_IMG_END = "⟪/I⟫"

    private data class NdjcParsedQuote(
        val body: String,
        val quoteMessageId: String?,
        val quotePreview: String?
    )

    private fun parseNdjcQuotePayload(text: String): NdjcParsedQuote {
        if (!text.startsWith(NDJC_QUOTE_START)) {
            return NdjcParsedQuote(body = text, quoteMessageId = null, quotePreview = null)
        }

        val endIdx = text.indexOf(NDJC_QUOTE_END)
        if (endIdx <= NDJC_QUOTE_START.length) {
            return NdjcParsedQuote(body = text, quoteMessageId = null, quotePreview = null)
        }

        val quoteRaw = text.substring(NDJC_QUOTE_START.length, endIdx).trim()
        val rest = text.substring(endIdx + NDJC_QUOTE_END.length).trimStart('\n', ' ')

        // ✅ 新协议：<quotedMessageId>\n<preview...>
        // ✅ 兼容旧协议：只有 <preview>
        val firstNl = quoteRaw.indexOf('\n')
        val quoteMessageId =
            if (firstNl > 0) quoteRaw.substring(0, firstNl).trim().takeIf { it.isNotBlank() } else null
        val quotePreview =
            if (firstNl > 0) quoteRaw.substring(firstNl + 1).trimStart('\n', ' ') else quoteRaw

        return NdjcParsedQuote(
            body = rest,
            quoteMessageId = quoteMessageId,
            quotePreview = quotePreview.ifBlank { null }
        )
    }

    private fun buildNdjcQuotePayload(rawBody: String, quoteMessageId: String?, quotePreview: String?): String {
        val id = quoteMessageId?.trim().orEmpty()
        val preview = quotePreview?.trim().orEmpty()

        // ✅ 没有 preview 就不输出 quote block
        if (preview.isBlank()) return rawBody

        // ✅ id 不能带换行（否则破坏第一行协议）
        val safeId = id.replace("\n", " ").trim()

        // ✅ preview 允许包含 NDJC_PRODUCT payload（包含 '|' 与换行），不能替换
        val inner = if (safeId.isNotBlank()) "$safeId\n$preview" else preview
        return "$NDJC_QUOTE_START$inner$NDJC_QUOTE_END\n$rawBody"
    }

    private fun buildNdjcChatPayload(
        rawBody: String,
        quoteMessageId: String?,
        quotePreview: String?,
        imageUris: List<String>
    ): String {
        val inner = buildNdjcQuotePayload(
            rawBody = rawBody,
            quoteMessageId = quoteMessageId,
            quotePreview = quotePreview
        )

        val imgs = imageUris
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(9)

        return if (imgs.isEmpty()) {
            inner
        } else {
            "$NDJC_IMG_START${imgs.joinToString("|")}$NDJC_IMG_END\n$inner"
        }
    }
    // --- NDJC 内嵌标记：商品分享 ---
    private fun buildNdjcProductSharePayload(p: ShowcaseChatProductShare): String {
        // 格式：⟪P⟫\n<dishId>|<title>|<price>|<imageUrl?>\n⟪/P⟫
        val safe = listOf(p.dishId, p.title, p.price, p.imageUrl ?: "")
            .joinToString("|") { it.replace("\n", " ").replace("|", " ") }
        return "$NDJC_PRODUCT_START\n$safe\n$NDJC_PRODUCT_END"
    }

    /**
     * ✅ 给 UI/VM 用的“复制到剪贴板 payload”（协议由逻辑层定义）
     * UI 层只负责把返回的 String 写入剪贴板。
     */
    fun buildProductSharePayloadForClipboard(p: ShowcaseChatProductShare): String {
        return buildNdjcProductSharePayload(p)
    }

    private data class NdjcParsedProduct(
        val dishId: String,
        val title: String,
        val price: String,
        val imageUrl: String?
    )

    private fun parseNdjcProductSharePayload(text: String): NdjcParsedProduct? {
        val s = text.trim()

        val start = s.indexOf(NDJC_PRODUCT_START)
        val end = s.indexOf(NDJC_PRODUCT_END)

        if (start < 0 || end < 0 || end <= start) return null

        // buildNdjcProductSharePayload 是：⟪P⟫\n<dishId>|<title>|<price>|<imageUrl>\n⟪/P⟫
        val inner = s.substring(start + NDJC_PRODUCT_START.length, end)
            .trim('\n', ' ', '\t', '\r')

        // inner 可能是一行，也可能多行；取第一行有效数据
        val line = inner.lineSequence().firstOrNull { it.isNotBlank() } ?: return null
        val parts = line.split("|")

        val dishId = parts.getOrNull(0)?.trim().orEmpty()
        val title = parts.getOrNull(1)?.trim().orEmpty()
        val price = parts.getOrNull(2)?.trim().orEmpty()
        val imageUrl = parts.getOrNull(3)?.trim().takeUnless { it.isNullOrBlank() }

        if (dishId.isBlank() && title.isBlank()) return null
        return NdjcParsedProduct(dishId = dishId, title = title, price = price, imageUrl = imageUrl)
    }

    private data class NdjcParsedImages(
        val imageUris: List<String>,
        val innerText: String
    )

    private fun parseNdjcImages(text: String): NdjcParsedImages {
        if (!text.startsWith(NDJC_IMG_START)) return NdjcParsedImages(emptyList(), text)

        val endIdx = text.indexOf(NDJC_IMG_END)
        if (endIdx <= NDJC_IMG_START.length) return NdjcParsedImages(emptyList(), text)

        val urisRaw = text.substring(NDJC_IMG_START.length, endIdx).trim()
        val inner = text.substring(endIdx + NDJC_IMG_END.length).trimStart('\n', ' ')
        val uris = urisRaw.split("|")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(9)

        return NdjcParsedImages(uris, inner)
    }

    private fun rebuildNdjcImages(imageUris: List<String>, innerText: String): String {
        val imgs = imageUris
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(9)

        return if (imgs.isEmpty()) {
            innerText
        } else {
            "$NDJC_IMG_START${imgs.joinToString("|")}$NDJC_IMG_END\n$innerText"
        }
    }

    private fun isLocalImageUri(u: String): Boolean =
        u.startsWith("content://") || u.startsWith("file://")
    /**
     * UI 输入变化（UI 包只回调这个）
     *
     * ✅ 新逻辑：
     * - 如果输入文本里包含 NDJC 商品分享 payload（来自“复制→粘贴”），
     *   则把它转成 pendingProduct，并清空输入框，让 UI 显示“顶部待发送商品条（图1）”
     * - 否则按普通草稿处理
     */
    fun onDraftChange(text: String) {
        val parsed = parseNdjcProductSharePayload(text)
        if (parsed != null) {
            // ✅ 最后一次操作为准：进入“待发送商品”态时，必须清空引用态
            uiState = uiState.copy(
                pendingProduct = ShowcaseChatProductShare(
                    dishId = parsed.dishId,
                    title = parsed.title,
                    price = parsed.price,
                    imageUrl = parsed.imageUrl
                ),
                draftText = "",

                // ✅ 清空引用（避免引用卡片 + 待发送卡片同时存在）
                quoteMessageId = null,
                quotePreviewText = "",
                quote = null
            )
            return
        }

        uiState = uiState.copy(draftText = text)
    }
    fun setPendingProductShare(p: ShowcaseChatProductShare?) {
        // ✅ 最后一次操作为准：设置 pending 时清空引用态
        uiState = uiState.copy(
            pendingProduct = p,
            quoteMessageId = null,
            quotePreviewText = "",
            quote = null
        )
    }

    fun clearPendingProductShare() {
        uiState = uiState.copy(pendingProduct = null)
    }

    suspend fun sendPendingProductShareAsClientLocal(
        context: Context,
        storeId: String,
        role: String,
    ): Boolean {
        val p = uiState.pendingProduct ?: return false

        // ✅ 发送“待发送商品”时：以本次操作为准（清引用态），避免发完后引用卡片还残留在输入框上方
        uiState = uiState.copy(
            draftText = buildNdjcProductSharePayload(p),
            draftImageUris = emptyList(),

            // ✅ 清空引用态（关键修复问题一 + 问题二）
            quoteMessageId = null,
            quotePreviewText = "",
            quote = null,
        )

        val cloudSendOk = sendAsClientLocal(context, storeId = storeId, role = role)

        // sendAsClientLocal 内部会清空 draftText / draftImageUris；这里额外清掉 pending
        uiState = uiState.copy(pendingProduct = null)

        return cloudSendOk
    }


    fun onDraftImagesAdd(uriStrings: List<String>) {
        val merged = (uiState.draftImageUris + uriStrings)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(9)

        uiState = uiState.copy(
            draftImageUris = merged
        )
    }

    fun onRemoveDraftImage(uriString: String) {
        uiState = uiState.copy(
            draftImageUris = uiState.draftImageUris.filterNot { it == uriString }
        )
    }

    fun setPendingCameraUri(uriString: String?) {
        uiState = uiState.copy(pendingCameraUri = uriString)
    }
    /**
     * ✅ 发送时构造 quote payload（不污染 quotePreviewText）
     * - 引用的是商品卡片：返回 ⟪P⟫...⟪/P⟫
     * - 否则：返回人类可读的摘要文本（quotePreview）
     */
    private fun buildQuotePayloadForSend(quoteId: String?, quotePreview: String): String? {
        if (quoteId.isNullOrBlank()) return null

        val quotedMsg = uiState.messages.firstOrNull { it.id == quoteId } ?: return quotePreview.ifBlank { null }

        // ✅ 引用消息里如果包含商品 payload，则发送时把商品 payload塞进 ⟪Q⟫...⟪/Q⟫
        val product = parseNdjcProductSharePayload(quotedMsg.text)
        if (product != null) {
            return buildNdjcProductSharePayload(
                ShowcaseChatProductShare(
                    dishId = product.dishId,
                    title = product.title,
                    price = product.price,
                    imageUrl = product.imageUrl
                )
            )
        }

        // ✅ 不是商品卡片：退回文本摘要
        return quotePreview.ifBlank { null }
    }

    /**
     * 发送消息（以 client 身份发送）
     */
    suspend fun sendAsClientLocal(context: Context, storeId: String, role: String = "client"): Boolean {
        Log.e("ChatRoomTest", "Domain sendAsClientLocal reached")
        var cloudSendOk = false

        val newId: () -> String = {
            if (isCloud()) UUID.randomUUID().toString() else newLocalChatId()
        }

        val raw = uiState.draftText.trim()

        val imgs = uiState.draftImageUris
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(9)
        Log.e("ChatRoomTest", "raw='$raw' rawBlank=${raw.isBlank()} imgs=${imgs.size}")
        if (raw.isBlank() && imgs.isEmpty()) return false

        val traceId = "T${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
        Log.e("ChatTrace", "[$traceId] send start role=$role storeId=$storeId draftLen=${raw.length} imgs=${imgs.size}")
        val quoteId = uiState.quoteMessageId
        val quotePreview = uiState.quotePreviewText.trim() // UI 引用条展示用（人类可读）

        // ✅ 发送时用的 quote payload：如果引用的是商品卡片，就塞商品 payload；否则塞 quotePreview（文本摘要）
        val quotePayloadForSend: String? = buildQuotePayloadForSend(quoteId, quotePreview)

        val nowBase = System.currentTimeMillis()

        // 目标：一张图 = 一条消息（入库也是拆开的）
        // 如果同时有文字/引用，只挂在第一条（最小可用、规则明确）
        val newUiMsgs: List<ShowcaseChatMessageUi> =
            if (imgs.isEmpty()) {
                val persistedText = buildNdjcChatPayload(
                    rawBody = raw,
                    quoteMessageId = quoteId,
                    quotePreview = quotePayloadForSend, // ✅ 发送用：可能是商品 payload，也可能是纯文本
                    imageUris = emptyList()
                )


                listOf(
                    ShowcaseChatMessageUi(
                        id = newId(),
                        direction = ShowcaseChatDirection.Outgoing,
                        text = persistedText,
                        timeText = formatChatTime(Date(nowBase)),
                        status = ShowcaseChatSendStatus.Sending,
                        isRead = false,
                        quoteMessageId = quoteId,
                        quotePreviewText = quotePreview
                    )
                )
            } else {
                imgs.mapIndexed { idx, oneImg ->
                    val bodyForThis = if (idx == 0) raw else ""
                    val quoteForThis = if (idx == 0) quotePreview else ""
                    val persistedText = buildNdjcChatPayload(
                        rawBody = bodyForThis,
                        quoteMessageId = if (idx == 0) quoteId else null,
                        quotePreview = if (idx == 0) quotePayloadForSend else null, // ✅ 发送用
                        imageUris = listOf(oneImg) // ✅ 关键：每条消息只存 1 张图
                    )

                    ShowcaseChatMessageUi(
                        id = newId(),
                        direction = ShowcaseChatDirection.Outgoing,
                        text = persistedText,
                        timeText = formatChatTime(Date(nowBase + idx)),
                        status = ShowcaseChatSendStatus.Sending,
                        isRead = false,
                        quoteMessageId = if (idx == 0) quoteId else null,
                        quotePreviewText = quoteForThis
                    )
                }
            }

        // 先更新 UI（立刻可见）
        uiState = uiState.copy(
            draftText = "",
            draftImageUris = emptyList(),
            pendingCameraUri = null,
            isSending = true,

            // 发送后清空引用
            quoteMessageId = null,
            quotePreviewText = "",
            quote = null,

            messages = (uiState.messages + newUiMsgs).distinctBy { it.id },
            scrollToBottomSignal = nowBase,
            errorMessage = null
        )

        // 再写本地库
        val idSet = newUiMsgs.map { it.id }.toSet()
        try {
            withContext(Dispatchers.IO) {
                newUiMsgs.forEachIndexed { idx, msgUi ->
                    val ts = nowBase + idx
                    val idt = ensureIdentity(context, storeId)
                    val convId = uiState.conversationId ?: buildConversationId(idt.storeId, idt.clientId)

                    try {
                        repo.upsertLocal(
                            context = context,
                            entity = uiToEntity(
                                storeId = storeId,
                                role = role,
                                ui = msgUi,
                                timeMs = ts,
                                conversationId = convId,
                                clientId = idt.clientId
                            )
                        )
                        Log.e("ChatTrace", "[$traceId] room upsertLocal OK id=${msgUi.id} conv=$convId ts=$ts role=$role")
                    } catch (t: Throwable) {
                        Log.e(
                            "ChatRoomTest",
                            "upsertLocal THREW idx=$idx id=${msgUi.id} ${t.javaClass.simpleName}: ${t.message}",
                            t
                        )
                        // 继续下一条，保证“必出结果”，不会整段发送被干掉
                        return@forEachIndexed
                    }
                }


                // ✅ 关键：立刻读回 Room，验证 chat → Room 是否真的写进去了
                val (cnt, latest) = repo.debugRoomSnapshot(context)
                Log.d(
                    "ChatRoomTest",
                    "after upsertLocal count=$cnt latestId=${latest?.id} latestText=${latest?.text} latestStatus=${latest?.status} conv=${latest?.conversationId}"
                )
            }

            if (isCloud()) {
                val idt = ensureIdentity(context, storeId)
                val convIdFinal = uiState.conversationId ?: buildConversationId(idt.storeId, idt.clientId)

                val effectiveClientId = if (role == "merchant") {
                    parseClientIdFromConversationId(convIdFinal)
                } else {
                    idt.clientId
                }

                if (uiState.conversationId.isNullOrBlank()) {
                    uiState = uiState.copy(conversationId = convIdFinal)
                }

                val okMap: Map<String, Boolean> = try {
                    Log.e("ChatTrace", "cloud start conv=$convIdFinal role=$role effClient=$effectiveClientId cloudRepoNull=${cloudRepo == null}")

                    withContext(Dispatchers.IO) {
                        val okConv = cloudRepo!!.upsertConversation(
                            conversationId = convIdFinal,
                            storeId = idt.storeId,
                            clientId = effectiveClientId,
                            traceId = traceId
                        )

                        Log.e("ChatTrace", "cloud upsertConversation ok=$okConv conv=$convIdFinal")

                        newUiMsgs.mapIndexed { idx, msgUi ->
                            val ts = nowBase + idx

                            Log.e("ChatTrace", "[$traceId] cloud insertMessage -> start id=${msgUi.id} idx=$idx ts=$ts conv=$convIdFinal")

// 云端 direction：统一用“商家视角语义”
                            val cloudDirection = when (role) {
                                "client" -> "incoming"    // 游客发给商家
                                "merchant" -> "outgoing"  // 商家发给游客
                                else -> "incoming"
                            }

// is_read：统一改成“接收方是否已读”语义
// - 新发消息一律先是未读
// - 后续由用户打开会话 / 商家打开会话 / read_receipt 再把对应消息改成 true
                            val cloudIsRead = false

// 1) 解析 NDJC 图片段
                            val parsed = parseNdjcImages(msgUi.text)
                            var finalText = msgUi.text

// 2) 云端 chat 模式下：只要消息里带本地图片 URI（content:// / file://），都必须先上传 Storage，再替换成 publicUrl
                            if (isCloud() && parsed.imageUris.any { isLocalImageUri(it) }) {
                                val uploaded = parsed.imageUris.mapIndexed { idx, u ->
                                    if (!isLocalImageUri(u)) {
                                        u
                                    } else {
                                        val url = cloudRepo.uploadChatImageToPublicUrl(
                                            context = context,
                                            localUri = Uri.parse(u),
                                            storeId = idt.storeId,
                                            conversationId = convIdFinal,
                                            msgId = msgUi.id,
                                            clientId = if (role == "merchant") null else effectiveClientId,
                                            asMerchant = role == "merchant",
                                            index = idx,
                                            traceId = traceId
                                        )

                                        if (url.isNullOrBlank() &&
                                            role == "merchant" &&
                                            cloudRepo.isMerchantAuthExpired(
                                                code = cloudRepo.lastChatImageUploadCode,
                                                body = cloudRepo.lastChatImageUploadBody
                                            )
                                        ) {
                                            throw IllegalStateException("MERCHANT_CHAT_IMAGE_AUTH_EXPIRED")
                                        }

                                        url ?: u
                                    }
                                }

                                if (uploaded != parsed.imageUris) {
                                    finalText = rebuildNdjcImages(uploaded, parsed.innerText)

                                    // ✅ 同步写回本地库：保证之后重进页面也能打开（历史只保本地 Room）
                                    repo.upsertLocal(
                                        context = context,
                                        entity = uiToEntity(
                                            storeId = storeId,
                                            role = role,
                                            ui = msgUi.copy(text = finalText),
                                            timeMs = ts,
                                            conversationId = convIdFinal,
                                            clientId = effectiveClientId
                                        )
                                    )

                                    parsed.imageUris
                                        .filter { it.startsWith("file://") || it.startsWith("/") }
                                        .forEach { localUri ->
                                            deleteAppOwnedLocalFileUri(context, localUri)
                                        }
                                }
                            }

// 3) 非 relay-only：直接写 chat_messages；relay-only：才写 chat_relay
                            val payload = JSONObject().apply {
                                put("type", if (parsed.imageUris.isNotEmpty()) "image" else "text")
                                put("text", finalText)
                                put("timeMs", ts)
                                put("msgId", msgUi.id)
                            }

                            val ok = if (repo.isChatRelayEnabled()) {
                                cloudRepo.enqueueRelay(
                                    ShowcaseChatCloudRepository.RelayRow(
                                        id = msgUi.id,
                                        conversationId = convIdFinal,
                                        storeId = idt.storeId,
                                        clientId = effectiveClientId,
                                        fromRole = role,
                                        payload = payload
                                    ),
                                    traceId = traceId
                                )
                            } else {
                                cloudRepo.insertMessage(
                                    ShowcaseChatCloudRepository.CloudMsg(
                                        id = msgUi.id,
                                        conversationId = convIdFinal,
                                        storeId = idt.storeId,
                                        clientId = effectiveClientId,
                                        role = role,
                                        direction = cloudDirection,
                                        text = finalText,
                                        timeMs = ts,
                                        isRead = cloudIsRead
                                    ),
                                    traceId = traceId
                                )
                            }

                            Log.e("ChatTrace", "[$traceId] cloud insertMessage -> end ok=$ok id=${msgUi.id}")

                            msgUi.id to ok
                        }.toMap()

                    }
                } catch (t: Throwable) {
                    Log.e("ChatRoomTest", "cloud ERROR ${t.javaClass.simpleName}: ${t.message}", t)
                    // 全失败：让后面统一回写 failed
                    idSet.associateWith { false }
                }

                val sentIds = okMap.filterValues { it }.keys.toList()
                val failedIds = idSet.filterNot { sentIds.contains(it) }.toList()

                Log.e("ChatTrace", "[$traceId] cloud done sent=${sentIds.size} failed=${failedIds.size} conv=$convIdFinal")
                Log.e("ChatTrace", "[$traceId] room status writeback begin")

                withContext(Dispatchers.IO) {
                    repo.updateLocalStatus(context, sentIds, "sent")
                    repo.updateLocalStatus(context, failedIds, "failed")
                }

                Log.e("ChatTrace", "[$traceId] room status writeback done")

                cloudSendOk = sentIds.isNotEmpty()

                uiState = uiState.copy(
                    isSending = false,
                    messages = uiState.messages.map { m ->
                        if (!idSet.contains(m.id)) m
                        else if (okMap[m.id] == true) m.copy(status = ShowcaseChatSendStatus.Sent)
                        else m.copy(status = ShowcaseChatSendStatus.Failed)
                    }
                )
            } else {
                withContext(Dispatchers.IO) {
                    repo.updateLocalStatus(context, idSet.toList(), "sent")
                }

                cloudSendOk = true

                uiState = uiState.copy(
                    isSending = false,
                    messages = uiState.messages.map { m ->
                        if (idSet.contains(m.id)) m.copy(status = ShowcaseChatSendStatus.Sent) else m
                    }
                )
            }

        } catch (t: Throwable) {
            cloudSendOk = false
            uiState = uiState.copy(
                isSending = false,
                errorMessage = t.message ?: "local save failed",
                messages = uiState.messages.map { m ->
                    if (idSet.contains(m.id)) m.copy(status = ShowcaseChatSendStatus.Failed) else m
                }
            )
        }

        return cloudSendOk
    }
    fun quoteMessage(messageId: String) {
        val msg = uiState.messages.firstOrNull { it.id == messageId } ?: return

        // ✅ 商品卡片：引用预览用“商品：标题 价格”
        val product = parseNdjcProductSharePayload(msg.text)

        val preview = if (product != null) {
            buildString {
                append("商品：")
                append(product.title.ifBlank { product.dishId })
                if (product.price.isNotBlank()) {
                    append("  ")
                    append(product.price)
                }
            }.take(60)
        } else {
            val parsed = parseNdjcQuotePayload(msg.text)
            parsed.body.replace('\n', ' ').take(60)
        }

        // ✅ 新增：如果引用的是商品卡片，把商品结构放进 quoteProduct，供 UI 画“卡片引用条”（截图2）
        val quoteProduct = if (product != null) {
            ShowcaseChatProductShare(
                dishId = product.dishId,
                title = product.title,
                price = product.price,
                imageUrl = product.imageUrl
            )
        } else null
        uiState = uiState.copy(
            quoteMessageId = messageId,
            quotePreviewText = preview,

            // ✅ 最后一次操作为准：进入引用态时，清空待发送商品态
            pendingProduct = null,

            // ✅ 旧字段保持清空（你现在 UI 统一用 quotePreviewText）
            quote = null,

            isSelectionMode = false,
            selectedIds = emptySet()
        )

    }
    fun cancelQuote() {
        uiState = uiState.copy(
            quoteMessageId = null,
            quotePreviewText = "",
            quote = null
            // pendingProduct 不动：取消引用不应影响待发送商品
        )
    }

    fun enterSelection(messageId: String) {
        uiState = uiState.copy(
            isSelectionMode = true,
            selectedIds = setOf(messageId),
            // 进入多选时取消引用，避免 UI 同时出现两条工具条
            quote = null
        )
    }

    fun toggleSelection(messageId: String) {
        if (!uiState.isSelectionMode) {
            enterSelection(messageId)
            return
        }
        val next = uiState.selectedIds.toMutableSet()
        if (next.contains(messageId)) next.remove(messageId) else next.add(messageId)

        uiState = if (next.isEmpty()) {
            uiState.copy(isSelectionMode = false, selectedIds = emptySet())
        } else {
            uiState.copy(selectedIds = next)
        }
    }

    fun exitSelection() {
        uiState = uiState.copy(isSelectionMode = false, selectedIds = emptySet())
    }
    // ----------------------------
// ✅ 只负责 UIState 写回（不落库，不做业务）
    fun setPinnedUi(pinned: Boolean) {
        uiState = uiState.copy(
            isPinned = pinned,
            subtitle = if (pinned) "Pinned" else ""
        )
    }
    fun setSearchResultsQuery(q: String) {
        uiState = uiState.copy(findQuery = q)
    }

    fun setGlobalSearchResults(results: List<ShowcaseChatGlobalSearchResultUi>) {
        uiState = uiState.copy(globalSearchResults = results)
    }

    // （可留作本地调试备用，不建议业务入口再直接用它）
    fun togglePinned() {
        setPinnedUi(!uiState.isPinned)
    }
    // ✅ 只负责 UIState 写回（不落库，不做业务）
    fun openFind() {
        // ✅ 进入“查找聊天记录”独立页
        uiState = uiState.copy(
            isSearchResults = true,
            isFindOpen = true
        )
    }

    fun closeFind() {
        // ✅ 退出“查找聊天记录”独立页：清空 query/结果
        uiState = uiState.copy(
            isSearchResults = false,
            isFindOpen = false,
            findQuery = "",
            globalSearchResults = emptyList(),

            // 兼容旧：会话内 find 清理
            findMatchIds = emptyList(),
            findFocusedId = null,
            scrollToMessageId = null,
            scrollToMessageSignal = System.currentTimeMillis()
        )
    }
    fun hideFindKeepState() {
        // ✅ 临时离开“查找聊天记录页”但保留 query/结果
        // 用于：点击命中结果跳到 Chat，再后退回到搜索页时还要能看到原结果
        uiState = uiState.copy(
            isSearchResults = false,
            isFindOpen = false
        )
    }


    fun onFindQueryChange(q: String) {
        val query = q.trim()
        if (query.isBlank()) {
            uiState = uiState.copy(
                findQuery = q,
                findMatchIds = emptyList(),
                findFocusedId = null,
                scrollToMessageId = null
            )
            return
        }
        fun jumpToMessage(messageId: String) {
            if (messageId.isBlank()) return
            uiState = uiState.copy(
                scrollToMessageId = messageId,
                scrollToMessageSignal = System.currentTimeMillis(),
                // ✅ 触发闪烁
                flashMessageId = messageId,
                flashSignal = System.currentTimeMillis()
            )
        }


        val matches = uiState.messages
            .filter { it.text.contains(query, ignoreCase = true) }
            .map { it.id }

        val focused = matches.firstOrNull()

        uiState = uiState.copy(
            findQuery = q,
            findMatchIds = matches,
            findFocusedId = focused,
            scrollToMessageId = focused,
            scrollToMessageSignal = System.currentTimeMillis()
        )
    }

    fun findNext() {
        val ids = uiState.findMatchIds
        if (ids.isEmpty()) return

        val cur = uiState.findFocusedId
        val idx = if (cur == null) -1 else ids.indexOf(cur)
        val next = ids[(idx + 1).coerceAtLeast(0) % ids.size]

        uiState = uiState.copy(
            findFocusedId = next,
            scrollToMessageId = next,
            scrollToMessageSignal = System.currentTimeMillis()
        )
    }

    fun findPrev() {
        val ids = uiState.findMatchIds
        if (ids.isEmpty()) return

        val cur = uiState.findFocusedId
        val idx = if (cur == null) 0 else ids.indexOf(cur).coerceAtLeast(0)
        val prev = ids[(idx - 1 + ids.size) % ids.size]

        uiState = uiState.copy(
            findFocusedId = prev,
            scrollToMessageId = prev,
            scrollToMessageSignal = System.currentTimeMillis()
        )
    }
    /**
     * 点击“搜索结果”项后：发出滚动定位信号。
     * - 纯逻辑：UI 只消费 scrollToMessageId + scrollToMessageSignal 做滚动。
     */
    fun jumpToMessage(messageId: String) {
        if (messageId.isBlank()) return
        uiState = uiState.copy(
            scrollToMessageId = messageId,
            scrollToMessageSignal = System.currentTimeMillis()
        )
    }
    fun clearFlash() {
        if (uiState.flashMessageId == null) return
        uiState = uiState.copy(
            flashMessageId = null
        )
    }
    /**
     * ✅ 离开 Chat（Back/Home）时调用：
     * - 只清“定位/闪烁”相关信号，避免再次进入 Chat 时重复触发滚动/闪烁
     * - 不清 findQuery / globalSearchResults（避免破坏返回搜索结果页的体验）
     */
    fun clearJumpOnExit() {
        uiState = uiState.copy(
            scrollToMessageId = null,
            scrollToMessageSignal = System.currentTimeMillis(),
            flashMessageId = null
        )
    }
    suspend fun deleteSelectedLocal(context: Context, storeId: String) {
        val ids = uiState.selectedIds.toList()
        if (ids.isEmpty()) return
        withContext(Dispatchers.IO) {
            repo.deleteLocalByIds(context, storeId, ids)
        }
        uiState = uiState.copy(
            messages = uiState.messages.filterNot { ids.contains(it.id) },
            isSelectionMode = false,
            selectedIds = emptySet()
        )
    }

    suspend fun deleteOneLocal(context: Context, storeId: String, messageId: String) {
        withContext(Dispatchers.IO) {
            repo.deleteLocalByIds(context, storeId, listOf(messageId))
        }
        uiState = uiState.copy(
            messages = uiState.messages.filterNot { it.id == messageId }
        )
    }


    suspend fun retryLocal(context: Context, storeId: String, messageId: String, role: String = "client") {
        val msg = uiState.messages.firstOrNull { it.id == messageId } ?: return
        val now = System.currentTimeMillis()

        // 只允许重试 Outgoing 的 Failed 消息（避免误操作 Incoming）
        if (msg.direction != ShowcaseChatDirection.Outgoing) return
        if (msg.status != ShowcaseChatSendStatus.Failed) return

        // UI 先进入 Sending
        val sendingUi = msg.copy(status = ShowcaseChatSendStatus.Sending)
        uiState = uiState.copy(
            isSending = true,
            errorMessage = null,
            messages = uiState.messages.map { m -> if (m.id == messageId) sendingUi else m }
        )

        try {
            withContext(Dispatchers.IO) {
                val idt = ensureIdentity(context, storeId)
                val conversationId = uiState.conversationId ?: buildConversationId(idt.storeId, idt.clientId)


                repo.upsertLocal(
                    context,
                    uiToEntity(
                        storeId = storeId,
                        role = role,
                        ui = sendingUi,
                        timeMs = now,
                        conversationId = conversationId,
                        clientId = idt.clientId
                    )
                )


            }

            // 成功 -> Sent
            uiState = uiState.copy(
                isSending = false,
                messages = uiState.messages.map { m ->
                    if (m.id == messageId) m.copy(status = ShowcaseChatSendStatus.Sent) else m
                }
            )
        } catch (t: Throwable) {
            // 失败 -> Failed（仍保留重试入口）
            uiState = uiState.copy(
                isSending = false,
                errorMessage = t.message ?: "retry local save failed",
                messages = uiState.messages.map { m ->
                    if (m.id == messageId) m.copy(status = ShowcaseChatSendStatus.Failed) else m
                }
            )
        }
    }


    suspend fun clearLocal(context: Context, storeId: String) {
        withContext(Dispatchers.IO) { repo.clearLocal(context, storeId) }
        uiState = uiState.copy(messages = emptyList(), errorMessage = null, scrollToBottomSignal = System.currentTimeMillis())
    }




    /**
     * 失败重试：把 failed 的 outgoing 再发一次（简单实现）
     */
    /**
     * 兼容旧入口：本地模式下重试就是 retryLocal。
     * 注意：需要调用方（VM）传入 context/storeId 才能真正重试。
     */
    suspend fun retrySend(messageId: String) {
        uiState = uiState.copy(
            errorMessage = "LOCAL chat enabled. Call retryLocal(context, storeId, messageId, role)."
        )
    }


    // -------------------- internal helpers --------------------
    private fun entityToUi(e: ChatMessageEntity): ShowcaseChatMessageUi {
        // ✅ e.direction 是 canonical（商家视角语义）：
        // - "out" = 商家发给用户
        // - "in"  = 用户发给商家
        // UI 方向需要根据当前打开的视角决定是否反转
        val outgoingInMerchantPerspective: Boolean = (e.direction == "out")

        val isOutgoingForThisUi: Boolean =
            if (perspectiveRoleForUi == "merchant") outgoingInMerchantPerspective
            else !outgoingInMerchantPerspective

        val dir: ShowcaseChatDirection =
            if (isOutgoingForThisUi) ShowcaseChatDirection.Outgoing else ShowcaseChatDirection.Incoming

        val st = when (e.status) {
            "failed" -> ShowcaseChatSendStatus.Failed
            "sending" -> ShowcaseChatSendStatus.Sending
            else -> ShowcaseChatSendStatus.Sent
        }

        return ShowcaseChatMessageUi(
            id = e.id,
            direction = dir,
            text = e.text,
            timeText = formatChatTime(Date(e.timeMs)),
            status = st,
            isRead = e.isRead
        )
    }

    suspend fun refreshLocalConversation(
        context: Context,
        conversationId: String
    ) {
        try {
            val result: Pair<List<ChatMessageEntity>, Int> = withContext(Dispatchers.IO) {
                if (perspectiveRoleForUi == "merchant") {
                    repo.markAllRead(context, conversationId)
                    val list = repo.listLocal(context, conversationId)
                    val unread = repo.countUnread(context, conversationId)
                    list to unread
                } else {
                    repo.markMerchantMessagesRead(context, conversationId)
                    val list = repo.listLocal(context, conversationId)
                    val unread = repo.countUnreadForUserEntry(context, conversationId)
                    list to unread
                }
            }

            uiState = uiState.copy(
                conversationId = conversationId,
                messages = result.first.map(::entityToUi),
                unreadCount = result.second,
                errorMessage = null
            )
        } catch (t: Throwable) {
            uiState = uiState.copy(
                errorMessage = t.message ?: "refreshLocalConversation failed"
            )
        }
    }
    fun applyLocalSnapshot(
        conversationId: String,
        list: List<ChatMessageEntity>,
        unread: Int
    ) {
        val oldSize = uiState.messages.size
        val newMsgs = list.map(::entityToUi)
        val newSize = newMsgs.size
        val shouldScroll = newSize > oldSize

        uiState = uiState.copy(
            conversationId = conversationId,
            messages = newMsgs,
            unreadCount = unread,
            errorMessage = null,
            scrollToBottomSignal = if (shouldScroll) System.currentTimeMillis() else uiState.scrollToBottomSignal
        )

        android.util.Log.e(
            "ChatTrace",
            "UI_SNAP uiConv=${uiState.conversationId} size=${uiState.messages.size} unread=${uiState.unreadCount}"
        )

// ✅ 断言：新消息最终在列表哪里？（尾部/头部/中间）
// 只看这行就能判定：
// - size 增长但 last 不变 → 新消息被插到前面/中间（排序/插入位置逻辑）
// - last 在变 → 新消息确实在尾部（后面再看 UI 是否可见）
        val first = uiState.messages.firstOrNull()
        val last = uiState.messages.lastOrNull()
        val last5 = uiState.messages.takeLast(5).joinToString(" | ") { m ->
            val id6 = (m.id ?: "").takeLast(6)
            "$id6@${m.timeText}"
        }
        android.util.Log.e(
            "ChatTrace",
            "UI_RANGE size=${uiState.messages.size} " +
                    "first=${(first?.id ?: "").takeLast(6)}/${first?.timeText} " +
                    "last=${(last?.id ?: "").takeLast(6)}/${last?.timeText} " +
                    "last5=$last5"
        )

    }
    private fun uiToEntity(
        storeId: String,
        role: String,
        ui: ShowcaseChatMessageUi,
        timeMs: Long,
        conversationId: String,
        clientId: String
    ): ChatMessageEntity {
        // ✅ canonical direction（商家视角语义）只由 role 决定，不能由 UI 视角决定
        // - merchant 发出的消息：out
        // - user/client 发出的消息：in
        val dir = if (role == "merchant") "out" else "in"

        val st = when (ui.status) {
            ShowcaseChatSendStatus.Failed -> "failed"
            ShowcaseChatSendStatus.Sending -> "sending"
            ShowcaseChatSendStatus.Sent -> "sent"
            else -> "sent"
        }

        val readFlag = ui.isRead

        return ChatMessageEntity(
            id = ui.id,
            storeId = storeId,
            role = role,
            direction = dir,
            text = ui.text,
            timeMs = timeMs,
            status = st,
            isRead = readFlag,
            conversationId = conversationId,
            clientId = clientId
        )
    }



    private fun markLocalFailed(localId: String) {
        uiState = uiState.copy(
            messages = uiState.messages.map { m ->
                if (m.id == localId) m.copy(status = ShowcaseChatSendStatus.Failed) else m
            }
        )
    }




    private fun applyServerMessages(msgs: List<ShowcaseChatMessage>, replaceAll: Boolean) {
        if (replaceAll) {
            seenIds.clear()
        }

        val mapped = ArrayList<ShowcaseChatMessageUi>(msgs.size)
        var newest: String? = uiState.newestCreatedAt
        var oldest: String? = uiState.oldestCreatedAt

        for (m in msgs) {
            if (!seenIds.add(m.id)) continue

            // 更新游标：按 created_at 字符串即可（ISO8601 字典序与时间序一致）
            val ca = m.createdAt
            if (!ca.isNullOrBlank()) {
                newest = if (newest.isNullOrBlank() || ca > newest!!) ca else newest
                oldest = if (oldest.isNullOrBlank() || ca < oldest!!) ca else oldest
            }

            mapped.add(m.toUi())
        }

        // seenIds 上限（防止长会话无限增长）
        if (seenIds.size > 1200) {
            val it = seenIds.iterator()
            while (seenIds.size > 1000 && it.hasNext()) {
                it.next()
                it.remove()
            }
        }

        uiState = if (replaceAll) {
            uiState.copy(
                messages = mapped,
                newestCreatedAt = newest,
                oldestCreatedAt = oldest,
                canLoadOlder = mapped.isNotEmpty()
            )
        } else {
            val merged = (uiState.messages + mapped).distinctBy { it.id }
            uiState.copy(
                messages = merged,
                newestCreatedAt = newest,
                oldestCreatedAt = oldest
            )
        }
    }


    private fun replaceLocalWithServer(localId: String, server: ShowcaseChatMessage) {
        // 把 localId 从 seenIds 里移除（避免后续冲突）
        // localId 本来不是 server id，所以不一定存在
        val serverUi = server.toUi().copy(status = ShowcaseChatSendStatus.Sent)

        // 用 server.id 标记为已见
        seenIds.add(server.id)

        uiState = uiState.copy(
            messages = uiState.messages
                .filterNot { it.id == localId }
                .plus(serverUi)
                .distinctBy { it.id }
        )
    }

    private fun ShowcaseChatMessage.toUi(): ShowcaseChatMessageUi {
        val dir = if (sender == ShowcaseChatTables.SENDER_CLIENT) {
            ShowcaseChatDirection.Outgoing
        } else {
            ShowcaseChatDirection.Incoming
        }

        val timeText = formatServerTimeToHHmm(createdAt)

        return ShowcaseChatMessageUi(
            id = id,
            direction = dir,
            text = body,
            timeText = timeText,
            status = ShowcaseChatSendStatus.Idle
        )
    }

    private fun formatServerTimeToHHmm(createdAt: String?): String {
        if (createdAt.isNullOrBlank()) return ""

        // 输出统一格式：yyyy-MM-dd a hh:mm（本地时区，英文 AM/PM）
        return try {
            val patterns = arrayOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'"
            )
            val utc = TimeZone.getTimeZone("UTC")
            var parsed: Date? = null
            for (p in patterns) {
                try {
                    val sdf = SimpleDateFormat(p, Locale.US)
                    sdf.timeZone = utc
                    parsed = sdf.parse(createdAt)
                    if (parsed != null) break
                } catch (_: Throwable) { /* try next */ }
            }

            // 解析失败就做一个“稳妥降级”：仍然按系统 12/24 小时制输出
            if (parsed == null) {
                val datePart = if (createdAt.length >= 10) createdAt.substring(0, 10) else ""
                val tIndex = createdAt.indexOf('T')
                val hhmmRaw = if (tIndex >= 0 && createdAt.length >= tIndex + 6) {
                    createdAt.substring(tIndex + 1, tIndex + 6)
                } else {
                    ""
                }

                val formattedTime = try {
                    if (hhmmRaw.length == 5) {
                        val hour24 = hhmmRaw.substring(0, 2).toInt()
                        val minute = hhmmRaw.substring(3, 5)

                        if (isSystem24Hour()) {
                            "%02d:%s".format(Locale.US, hour24, minute)
                        } else {
                            val ampm = if (hour24 < 12) "AM" else "PM"
                            val hour12 = when {
                                hour24 == 0 -> 12
                                hour24 > 12 -> hour24 - 12
                                else -> hour24
                            }
                            "%s %02d:%s".format(Locale.US, ampm, hour12, minute)
                        }
                    } else {
                        ""
                    }
                } catch (_: Throwable) {
                    ""
                }

                return listOf(datePart, formattedTime).filter { it.isNotBlank() }.joinToString(" ")
            }

            // parsed 默认是 epoch 毫秒；按系统 12/24 小时制输出
            formatChatTime(parsed)
        } catch (_: Throwable) {
            ""
        }
    }
    suspend fun acknowledgeClientVisibleConversation(
        context: Context,
        conversationId: String
    ) {
        daoMarkClientRead(context, conversationId)
    }

    suspend fun acknowledgeMerchantVisibleConversation(
        context: Context,
        storeId: String,
        conversationId: String
    ) {
        daoMarkMerchantRead(context, storeId, conversationId)
    }

    private suspend fun daoMarkClientRead(context: Context, conversationId: String) {
        // 1) 本地：只把“商家发来的消息”置已读
        repo.markMerchantMessagesRead(context, conversationId)

        val idt = identity ?: return
        val storeId = idt.storeId
        val clientId = idt.clientId

        // 2) relay-only：发 read_receipt 给商家端
        if (repo.isChatRelayEnabled()) {
            val traceId = "T${System.currentTimeMillis()}_rr"
            repo.enqueueReadReceiptForClient(
                context = context,
                storeId = storeId,
                conversationId = conversationId,
                clientId = clientId,
                traceId = traceId
            )
        } else {
            cloudRepo?.markMerchantMessagesRead(
                storeId = storeId,
                conversationId = conversationId,
                clientId = clientId
            )
        }
    }

    private suspend fun daoMarkMerchantRead(
        context: Context,
        storeId: String,
        conversationId: String
    ) {
        // 1) 本地：把“游客发来的消息”置已读
        repo.markAllRead(context, conversationId)

        // 2) cloud 模式：把云端游客消息置已读，让游客自己发出的气泡能变 Read
        if (!repo.isChatRelayEnabled()) {
            cloudRepo?.markUserMessagesRead(
                storeId = storeId,
                conversationId = conversationId
            )
        }
    }

    fun ensureClientId(context: Context): String {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // ✅ 用 ANDROID_ID 作为“设备指纹”
        // 目的：即使你克隆了 AVD（SharedPreferences 被复制），两台模拟器的 ANDROID_ID 不同，
        //      也会触发重生成，保证 clientId 不冲突。
        val deviceId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull().orEmpty()

        val storedDeviceId = sp.getString(KEY_DEVICE_ID, null)
        val existing = sp.getString(KEY_CLIENT_ID, null)

        // 1) 既有 clientId 且 deviceId 未变化 → 直接复用
        if (!existing.isNullOrBlank() && !storedDeviceId.isNullOrBlank() && storedDeviceId == deviceId) {
            return existing
        }

        // 2) 首次安装（无 existing） 或 AVD 被克隆/迁移（deviceId 变了）→ 重生成
        val id = "client_" + UUID.randomUUID().toString()
        sp.edit()
            .putString(KEY_CLIENT_ID, id)
            .putString(KEY_DEVICE_ID, deviceId)
            .apply()

        Log.e("ChatTrace", "ensureClientId regen clientId=$id deviceId=$deviceId oldDeviceId=$storedDeviceId")
        return id
    }


    private companion object {
        private const val PREF_NAME = "Showcase_chat_prefs"
        private const val KEY_CLIENT_ID = "chat_client_id"
        private const val KEY_DEVICE_ID = "chat_device_id"
        private const val NDJC_PRODUCT_START = "⟪P⟫"
        private const val NDJC_PRODUCT_END = "⟪/P⟫"
    }

    // --------------------


}
