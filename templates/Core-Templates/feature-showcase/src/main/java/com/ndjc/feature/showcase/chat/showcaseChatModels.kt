package com.ndjc.feature.showcase

import java.util.UUID

/**
 * Chat 子域：模型 & UI DTO & 状态
 *
 * 设计目标：
 * - UI 包只拿 ChatUiState 渲染，不碰云/实时/重连。
 * - 逻辑模块内部负责拉取、发送、轮询、去重、状态机。
 *
 * 云端表结构（建议最小字段）：
 * 1) conversations
 *    - id (uuid, pk)
 *    - store_id (text)
 *    - client_id (text)
 *    - created_at (timestamptz)
 *    - last_message_at (timestamptz, nullable)
 *
 * 2) messages
 *    - id (uuid, pk)
 *    - conversation_id (uuid, fk)
 *    - sender (text)  // "store" | "client"
 *    - body (text)
 *    - created_at (timestamptz)
 *
 * 你也可以把 store_id/client_id 直接放 messages 表，减少 join，
 * Repository 里只需要改 select/where 即可。
 */
object ShowcaseChatTables {
    const val TABLE_CONVERSATIONS: String = ShowcaseCloudConfig.TABLE_CHAT_CONVERSATIONS
    const val TABLE_MESSAGES: String = ShowcaseCloudConfig.TABLE_CHAT_MESSAGES

    // sender 约定
    const val SENDER_STORE: String = "store"
    const val SENDER_CLIENT: String = "client"
}
data class ShowcaseChatGlobalSearchResultUi(
    val conversationId: String,
    val messageId: String?,
    val displayName: String,
    val senderLabel: String,
    val snippet: String,
    val timeMs: Long,
    val timeText: String,
    val matchedInName: Boolean
)

/** 会话身份 */
data class ShowcaseChatIdentity(
    val storeId: String,
    val clientId: String
)

/** 云端会话 */
data class ShowcaseChatConversation(
    val id: String,
    val storeId: String,
    val clientId: String,
    val createdAt: String? = null,
    val lastMessageAt: String? = null
)

/** 云端消息 */
data class ShowcaseChatMessage(
    val id: String,
    val conversationId: String,
    val sender: String,         // "store" | "client"
    val body: String,
    val createdAt: String? = null
)

/** UI 方向 */
enum class ShowcaseChatDirection { Incoming, Outgoing }

/** UI 发送状态 */
enum class ShowcaseChatSendStatus { Idle, Sending, Sent, Failed }

/** UI DTO：渲染用消息（不包含任何云端依赖） */
// ✅ 商家“会话列表”每一行的 UI DTO（只给 UI 包用）
data class ShowcaseChatThreadSummaryUi(
    val threadId: String, // ✅ conversationId（稳定且唯一）
    val title: String,
    val lastPreview: String,
    val lastTimeText: String,
    val unreadCount: Int,
    val isPinned: Boolean = false  // ✅ 新增：置顶标记（不破坏旧调用，给默认值）
)


data class ShowcaseChatMessageUi(
    val id: String,
    val direction: ShowcaseChatDirection,
    val text: String,
    val timeText: String = "",
    val status: ShowcaseChatSendStatus = ShowcaseChatSendStatus.Idle,
    val isRead: Boolean = false,

    // ---- 引用（reply）----
    // 被引用的消息 id（可选）
    val quoteMessageId: String? = null,
    val quotePreviewText: String = "",

// ---- 置顶 ----
    val isPinned: Boolean = false,

// ---- 查找（聊天记录搜索）----
    val isFindOpen: Boolean = false,
    val findQuery: String = "",
    val findMatchIds: List<String> = emptyList(),
    val findFocusedId: String? = null,

// UI 只负责滚动，逻辑模块发信号
    val scrollToMessageId: String? = null,
    val scrollToMessageSignal: Long = 0L,

)

data class ShowcaseChatQuoteUi(
    val messageId: String,
    val preview: String
)


/** UI 状态：UI 包只消费这个 */
data class ShowcaseChatUiState(
    val title: String = "Chat",
    val subtitle: String = "",
    val useStoreTitle: Boolean = true,
    val canTogglePinned: Boolean = false,
    val isPinned: Boolean = false,
    val isConnecting: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSending: Boolean = false,

    // 可选：上拉加载更早消息（UI 包如果没做入口也不影响）
    val isLoadingOlder: Boolean = false,
    val canLoadOlder: Boolean = true,

    val errorMessage: String? = null,

    val conversationId: String? = null,
    val draftText: String = "",

    /** ✅ Chat 草稿图片（最多 9 张，存 uriString） */
    val draftImageUris: List<String> = emptyList(),
    /** ✅ 相机拍照临时 Uri（用于 TakePicture） */
    val pendingCameraUri: String? = null,

    val messages: List<ShowcaseChatMessageUi> = emptyList(),
// ---- 引用（reply）----
    val quote: ShowcaseChatQuoteUi? = null,


    // ---- 多选 ----
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    // ---- Quote（引用）----
    val quoteMessageId: String? = null,
    val quotePreviewText: String = "",

    /**
     * ✅ 新增：引用的是“商品卡片”时，把商品结构放这里，供 UI 渲染截图2那种“卡片引用条”
     * （注意：这是“引用回复上下文”，不是 pendingProduct，不会出现“发送商品”按钮）
     */
    val quoteProduct: ShowcaseChatProductShare? = null,
// ---- 查找（聊天记录搜索）----

    val isSearchResults: Boolean = false,
    val isFindOpen: Boolean = false,
    val findQuery: String = "",

// ✅ 新增：全局搜索结果（跨会话：名称/消息）
    val globalSearchResults: List<ShowcaseChatGlobalSearchResultUi> = emptyList(),

// 兼容旧：会话内 find（仍保留）
    val findMatchIds: List<String> = emptyList(),
    val findFocusedIndex: Int = 0,

    // Flash (UI only rendering signal; decision made by logic)
    val flashMessageId: String? = null,
    val flashSignal: Long = 0L,

    val scrollToMessageId: String? = null,
    val scrollToMessageSignal: Long = 0L,
    val pendingProduct: ShowcaseChatProductShare? = null,





    /**
     * 游标（逻辑模块维护，UI 包只读）：
     * - newestCreatedAt：目前已知最新消息 created_at（用于增量拉取）
     * - oldestCreatedAt：目前已知最早消息 created_at（用于加载更早）
     */
    val newestCreatedAt: String? = null,
    val oldestCreatedAt: String? = null,
    val unreadCount: Int = 0,
    // 给 UI 的“滚动信号”（UI 包只负责滚动，不负责策略）
    val scrollToBottomSignal: Long = 0L,
    // ---- Find（查找聊天记录）----
    val findFocusedId: String? = null,
    val findScrollSignal: Long = 0L

) {
    val hasConversation: Boolean get() = !conversationId.isNullOrBlank()
}


/** 生成本地临时消息 id（用于 sending 状态占位） */
fun newLocalChatId(): String = "local_" + UUID.randomUUID().toString()

// -------------------------
// Merchant Chat List DTO
// -------------------------

