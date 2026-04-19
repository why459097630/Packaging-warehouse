package com.ndjc.feature.showcase

object ShowcaseRuntimeState {

    private const val CHAT_PUSH_GRACE_MS = 2500L

    @Volatile
    private var chatVisible: Boolean = false

    @Volatile
    private var activeConversationId: String? = null

    @Volatile
    private var lastSeenConversationId: String? = null

    @Volatile
    private var lastSeenConversationAtMs: Long = 0L

    fun setChatVisible(visible: Boolean) {
        chatVisible = visible
    }

    fun isChatScreenVisible(): Boolean {
        return chatVisible
    }

    fun setActiveConversationId(conversationId: String?) {
        activeConversationId = conversationId
    }

    fun getActiveConversationId(): String? {
        return activeConversationId
    }

    fun markConversationVisible(conversationId: String?) {
        val normalized = conversationId?.trim()?.takeIf { it.isNotEmpty() } ?: return
        lastSeenConversationId = normalized
        lastSeenConversationAtMs = System.currentTimeMillis()
        activeConversationId = normalized
        chatVisible = true
    }

    fun markConversationRecentlySeen(conversationId: String?) {
        val normalized = conversationId?.trim()?.takeIf { it.isNotEmpty() } ?: return
        lastSeenConversationId = normalized
        lastSeenConversationAtMs = System.currentTimeMillis()
    }

    fun shouldSuppressChatPush(conversationId: String?): Boolean {
        val normalized = conversationId?.trim()?.takeIf { it.isNotEmpty() } ?: return false

        if (chatVisible && normalized == activeConversationId) {
            return true
        }

        val lastSeenId = lastSeenConversationId
        val lastSeenAt = lastSeenConversationAtMs
        if (normalized == lastSeenId && lastSeenAt > 0L) {
            val delta = System.currentTimeMillis() - lastSeenAt
            if (delta in 0..CHAT_PUSH_GRACE_MS) {
                return true
            }
        }

        return false
    }
}