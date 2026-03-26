package com.ndjc.feature.showcase

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID


class ShowcaseChatRepository(
    private val cloud: ShowcaseChatCloudRepository? = null
) {
    private fun dao(context: Context): ChatMessageDao =
        ShowcaseChatDb.get(context).chatDao()

    private fun metaDao(context: Context): ChatThreadMetaDao =
        ShowcaseChatDb.get(context).metaDao()


    fun isChatCloudEnabled(): Boolean =
        ShowcaseCloudConfig.ENABLE_CHAT_CLOUD &&
                !ShowcaseCloudConfig.ENABLE_CHAT_RELAY_ONLY &&
                cloud != null

    fun isChatRelayEnabled(): Boolean =
        ShowcaseCloudConfig.ENABLE_CHAT_CLOUD &&
                ShowcaseCloudConfig.ENABLE_CHAT_RELAY_ONLY &&
                cloud != null

    private fun effectiveTraceId(traceId: String?): String {
        val t = traceId?.trim().orEmpty()
        return if (t.isNotEmpty() && t != "-") t
        else "R${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(1000, 9999)}"
    }


    // --------------------
    // Local (Room)
    // --------------------

    suspend fun listLocal(context: Context, conversationId: String): List<ChatMessageEntity> {
        return dao(context).listByConversation(conversationId)
    }
    fun observeLocal(context: Context, conversationId: String): kotlinx.coroutines.flow.Flow<List<ChatMessageEntity>> {
        return dao(context).observeByConversation(conversationId)
    }

    fun observeUnread(context: Context, conversationId: String): kotlinx.coroutines.flow.Flow<Int> {
        return dao(context).observeUnread(conversationId)
    }

    fun observeLocalByStore(context: Context, storeId: String): kotlinx.coroutines.flow.Flow<List<ChatMessageEntity>> {
        return dao(context).observeByStore(storeId)
    }

    fun observeThreadMetaByStore(context: Context, storeId: String): kotlinx.coroutines.flow.Flow<List<ChatThreadMetaEntity>> {
        return metaDao(context).observeByStore(storeId)
    }

    suspend fun upsertLocal(context: Context, entity: ChatMessageEntity) {
        dao(context).upsert(entity)
    }

    suspend fun debugRoomSnapshot(context: Context): Pair<Int, ChatMessageEntity?> {
        val d = dao(context)
        return d.countAll() to d.latest()
    }

    suspend fun findLocalMessageById(context: Context, id: String): ChatMessageEntity? {
        return dao(context).findById(id)
    }

    // ✅ 新增：批量回写发送状态（sent/failed）
    suspend fun updateLocalStatus(context: Context, ids: List<String>, status: String) {
        if (ids.isEmpty()) return
        dao(context).updateStatusByIds(ids, status)
    }


    suspend fun clearLocal(context: Context, storeId: String) {
        dao(context).clearStore(storeId)
    }

    suspend fun markAllRead(context: Context, conversationId: String) {
        dao(context).markAllRead(conversationId)
    }

    suspend fun countUnread(context: Context, conversationId: String): Int {
        return dao(context).countUnread(conversationId)
    }

    suspend fun countUnreadForUserEntry(context: Context, conversationId: String): Int {
        return dao(context).countUnreadForUserEntry(conversationId)
    }

    suspend fun findLatestConversationIdByStoreAndClient(
        context: Context,
        storeId: String,
        clientId: String
    ): String? {
        return dao(context).findLatestConversationIdByStoreAndClient(
            storeId = storeId,
            clientId = clientId
        )
    }

    suspend fun countUnreadForUserEntryByStoreAndClient(
        context: Context,
        storeId: String,
        clientId: String
    ): Int {
        return dao(context).countUnreadForUserEntryByStoreAndClient(
            storeId = storeId,
            clientId = clientId
        )
    }

    suspend fun deleteLocalByIds(context: Context, storeId: String, ids: List<String>) {
        if (ids.isEmpty()) return
        dao(context).deleteByIds(storeId, ids)
    }

    suspend fun deleteLocalById(context: Context, id: String) {
        dao(context).deleteById(id)
    }

    suspend fun deleteLocalByIds(context: Context, ids: List<String>) {
        dao(context).deleteByIds(ids)
    }

    suspend fun listLocalByStore(context: Context, storeId: String): List<ChatMessageEntity> =
        withContext(Dispatchers.IO) {
            dao(context).listByStore(storeId)
        }

    // ✅ 会话内搜索：仅按 conversationId 搜消息内容（Room 查询）
    suspend fun searchLocalMessagesByConversationKeyword(
        context: Context,
        conversationId: String,
        keyword: String,
        limit: Int = 80
    ): List<ChatMessageEntity> = withContext(Dispatchers.IO) {
        dao(context).searchByConversationKeyword(conversationId, keyword.trim(), limit)
    }


// Local: Thread Meta (pin/delete)
// --------------------

    suspend fun listThreadMetaByStore(context: Context, storeId: String): List<ChatThreadMetaEntity> {
        return metaDao(context).listByStore(storeId)
    }
    suspend fun getThreadMeta(context: Context, storeId: String, conversationId: String): ChatThreadMetaEntity? {
        return metaDao(context).get(storeId, conversationId)
    }

    suspend fun syncMerchantThreadMetaFromCloud(
        context: Context,
        storeId: String,
        traceId: String? = null
    ): Int {
        val c = cloud ?: return 0
        val rows = c.fetchMerchantThreadMetaRows(storeId = storeId, traceId = traceId)
        if (rows.isEmpty()) return 0

        return withContext(Dispatchers.IO) {
            var count = 0
            for (row in rows) {
                if (row.conversationId.isBlank()) continue

                val old = metaDao(context).get(storeId, row.conversationId)
                val mergedAlias =
                    row.merchantAlias?.trim()?.takeIf { it.isNotBlank() }
                        ?: old?.alias?.trim()?.takeIf { it.isNotBlank() }

                metaDao(context).upsert(
                    ChatThreadMetaEntity(
                        storeId = storeId,
                        conversationId = row.conversationId,
                        pinnedAtMs = old?.pinnedAtMs ?: 0L,
                        isDeleted = row.merchantArchived,
                        deletedAtMs = row.merchantArchivedAtMs,
                        alias = mergedAlias
                    )
                )
                count++
            }
            count
        }
    }

    suspend fun isThreadPinned(context: Context, storeId: String, conversationId: String): Boolean {
        val meta = getThreadMeta(context, storeId, conversationId)
        return (meta?.pinnedAtMs ?: 0L) > 0L
    }
    suspend fun getThreadAlias(context: Context, storeId: String, conversationId: String): String? {
        return metaDao(context).get(storeId, conversationId)?.alias?.takeIf { it.isNotBlank() }
    }

    suspend fun resolveMerchantThreadDisplayName(
        context: Context,
        storeId: String,
        conversationId: String
    ): String {
        val alias = getThreadAlias(context, storeId, conversationId)?.trim()
        if (!alias.isNullOrBlank()) {
            return alias
        }

        val all = listLocalByStore(context, storeId)
        if (all.isEmpty()) {
            return "Customer"
        }

        val byConv = all.groupBy { it.conversationId }

        val ordered = byConv.entries
            .map { (cid, msgs) ->
                val firstMs = msgs.minOfOrNull { it.timeMs } ?: Long.MAX_VALUE
                cid to firstMs
            }
            .sortedBy { it.second }

        val idx = ordered.indexOfFirst { it.first == conversationId }
        return if (idx >= 0) "Customer #${idx + 1}" else "Customer"
    }

    suspend fun setThreadAlias(context: Context, storeId: String, conversationId: String, alias: String?) {
        val a = alias?.trim()?.takeIf { it.isNotBlank() }
        val old = metaDao(context).get(storeId, conversationId)

        metaDao(context).upsert(
            ChatThreadMetaEntity(
                storeId = storeId,
                conversationId = conversationId,
                pinnedAtMs = old?.pinnedAtMs ?: 0L,
                isDeleted = old?.isDeleted ?: false,
                deletedAtMs = old?.deletedAtMs ?: 0L,
                alias = a
            )
        )
        metaDao(context).updateAlias(storeId, conversationId, a)

        cloud?.upsertMerchantThreadMeta(
            storeId = storeId,
            conversationId = conversationId,
            merchantAlias = a,
            merchantArchived = old?.isDeleted ?: false,
            merchantArchivedAtMs = old?.deletedAtMs ?: 0L
        )
    }


    suspend fun setThreadPinned(context: Context, storeId: String, conversationId: String, pinned: Boolean) {
        val now = System.currentTimeMillis()
        val pinnedAt = if (pinned) now else 0L

        // 先 upsert 一条，保证存在
        val old = metaDao(context).get(storeId, conversationId)
        metaDao(context).upsert(
            ChatThreadMetaEntity(
                storeId = storeId,
                conversationId = conversationId,
                pinnedAtMs = pinnedAt,
                isDeleted = old?.isDeleted ?: false,
                deletedAtMs = old?.deletedAtMs ?: 0L,
                alias = old?.alias
            )
        )

        // 再 update（更稳）
        metaDao(context).updatePinned(storeId, conversationId, pinnedAt)
    }

    suspend fun markThreadDeleted(context: Context, storeId: String, conversationId: String, deleted: Boolean) {
        val old = metaDao(context).get(storeId, conversationId)
        val deletedAtMs = if (deleted) System.currentTimeMillis() else 0L

        metaDao(context).upsert(
            ChatThreadMetaEntity(
                storeId = storeId,
                conversationId = conversationId,
                pinnedAtMs = if (deleted) 0L else (old?.pinnedAtMs ?: 0L),
                isDeleted = deleted,
                deletedAtMs = deletedAtMs,
                alias = old?.alias
            )
        )
        metaDao(context).updateDeleted(storeId, conversationId, deleted, deletedAtMs)

        cloud?.upsertMerchantThreadMeta(
            storeId = storeId,
            conversationId = conversationId,
            merchantAlias = old?.alias,
            merchantArchived = deleted,
            merchantArchivedAtMs = deletedAtMs
        )
    }

    /**
     * ✅ 商家侧“删除聊天”改成：归档/隐藏会话
     * - 不删本地消息
     * - 不删云端消息
     * - 只把该会话在商家 ChatList 隐藏
     * - 删除后的新消息到来时，线程会重新出现
     */
    private fun extractLocalImageUrisFromMessageText(text: String): List<String> {
        val startToken = "⟪I⟫"
        val endToken = "⟪/I⟫"

        if (!text.startsWith(startToken)) {
            return emptyList()
        }

        val endIdx = text.indexOf(endToken)
        if (endIdx <= startToken.length) {
            return emptyList()
        }

        val urisRaw = text.substring(startToken.length, endIdx).trim()
        return urisRaw.split("|")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { it.startsWith("file://") || it.startsWith("/") }
            .distinct()
            .take(9)
    }

    private fun deleteOwnedLocalFileUri(context: Context, uriString: String) {
        runCatching {
            val uri = Uri.parse(uriString)
            val file =
                when {
                    uri.scheme == "file" -> {
                        uri.path?.let { File(it) }
                    }
                    uri.scheme.isNullOrBlank() && uriString.startsWith("/") -> {
                        File(uriString)
                    }
                    else -> null
                } ?: return@runCatching

            val target = file.canonicalFile
            val cacheRoot = context.cacheDir.canonicalFile
            val externalRoot = context.getExternalFilesDir(null)?.canonicalFile

            val ownedByApp =
                target.path.startsWith(cacheRoot.path) ||
                        (externalRoot != null && target.path.startsWith(externalRoot.path))

            if (ownedByApp && target.exists()) {
                target.delete()
            }
        }
    }

    suspend fun deleteThreadLocal(context: Context, storeId: String, conversationId: String) {
        withContext(Dispatchers.IO) {
            val oldMeta = metaDao(context).get(storeId, conversationId)
            val oldMessages = dao(context).listByConversation(conversationId)

            oldMessages.forEach { msg ->
                val localUris = extractLocalImageUrisFromMessageText(msg.text)
                localUris.forEach { uriString ->
                    deleteOwnedLocalFileUri(context, uriString)
                }
            }

            dao(context).deleteByConversation(conversationId)
            metaDao(context).deleteByConversation(storeId, conversationId)

            val deletedAtMs = System.currentTimeMillis()
            val keepAlias = oldMeta?.alias?.trim()?.takeIf { it.isNotBlank() }

            metaDao(context).upsert(
                ChatThreadMetaEntity(
                    storeId = storeId,
                    conversationId = conversationId,
                    pinnedAtMs = 0L,
                    isDeleted = true,
                    deletedAtMs = deletedAtMs,
                    alias = keepAlias
                )
            )

            cloud?.upsertMerchantThreadMeta(
                storeId = storeId,
                conversationId = conversationId,
                merchantAlias = keepAlias,
                merchantArchived = true,
                merchantArchivedAtMs = deletedAtMs
            )
        }
    }

    private suspend fun reviveDeletedThreadIfNeeded(
        context: Context,
        storeId: String,
        conversationId: String,
        latestNewMessageTimeMs: Long
    ) {
        val old = metaDao(context).get(storeId, conversationId) ?: return
        if (!old.isDeleted) return
        if (latestNewMessageTimeMs <= old.deletedAtMs) return

        metaDao(context).updateDeleted(
            storeId = storeId,
            conversationId = conversationId,
            isDeleted = false,
            deletedAtMs = old.deletedAtMs
        )

        cloud?.upsertMerchantThreadMeta(
            storeId = storeId,
            conversationId = conversationId,
            merchantAlias = old.alias,
            merchantArchived = false,
            merchantArchivedAtMs = old.deletedAtMs
        )
    }

    // --------------------
// Cloud (Supabase) -> Local Sync
// Phase-1: 先打通“写入 + 列表读取”
// --------------------
    data class CloudThreadSummary(
        val conversationId: String,
        val storeId: String,
        val clientId: String,
        val lastMessageAtIso: String?,
        val lastPreview: String?,
        val updatedAtIso: String?
    )

    suspend fun fetchCloudThreadSummaries(
        storeId: String,
        traceId: String? = null
    ): List<CloudThreadSummary> {
        if (!isChatCloudEnabled()) return emptyList()
        val tid = effectiveTraceId(traceId)
        android.util.Log.e("ChatTrace", "[$tid] repo fetchCloudThreadSummaries start store=$storeId")

        val rows = cloud!!.fetchThreadSummaries(storeId = storeId, limit = 30, traceId = tid)


        android.util.Log.e("ChatTrace", "[$tid] repo fetchCloudThreadSummaries end rows=${rows.size} store=$storeId")

        return rows.map {
            CloudThreadSummary(
                conversationId = it.conversationId,
                storeId = it.storeId,
                clientId = it.clientId,
                lastMessageAtIso = it.lastMessageAtIso,
                lastPreview = it.lastPreview,
                updatedAtIso = it.updatedAtIso
            )
        }
    }

    suspend fun syncConversationFromCloud(
        context: Context,
        storeId: String,
        conversationId: String,
        perspectiveRole: String,
        clientId: String? = null,
        traceId: String? = null
    ): Int {
        if (!isChatCloudEnabled()) return 0
        val tid = effectiveTraceId(traceId)
        android.util.Log.e("ChatTrace", "[$tid] repo syncConversationFromCloud start conv=$conversationId role=$perspectiveRole")

        val cloudMsgs = cloud!!.fetchMessagesByConversation(
            storeId = storeId,
            conversationId = conversationId,
            clientId = clientId,
            asMerchant = perspectiveRole == "merchant",
            limit = 120,
            traceId = tid
        )

        android.util.Log.e("ChatTrace", "[$tid] repo syncConversationFromCloud fetched msgs=${cloudMsgs.size} conv=$conversationId")

        if (cloudMsgs.isEmpty()) return 0

        val meta = withContext(Dispatchers.IO) {
            metaDao(context).get(storeId, conversationId)
        }

        val localCutoffMs = meta?.deletedAtMs ?: 0L

        val sourceMsgs = if (localCutoffMs > 0L) {
            cloudMsgs.filter { it.timeMs > localCutoffMs }
        } else {
            cloudMsgs
        }

        android.util.Log.e(
            "ChatTrace",
            "[$tid] repo syncConversationFromCloud sourceMsgs=${sourceMsgs.size} deleted=${meta?.isDeleted == true} deletedAtMs=$localCutoffMs conv=$conversationId"
        )

        if (sourceMsgs.isEmpty()) return 0

        // ✅ 本地仍保持“商家视角 canonical 语义”：
        // - cloud incoming (user -> merchant)  -> room "in"
        // - cloud outgoing (merchant -> user) -> room "out"
        fun mapDirectionCanonical(dir: String): String {
            val d = dir.lowercase()
            return if (d == "incoming") "in" else "out"
        }

        // ✅ 关键：
        // 云端 isRead 目前仍是“商家视角语义”。
        // 同步到本地时，需要按当前打开视角转换成本地可用语义：
        // 1) merchant 视角：沿用 cloud isRead
        // 2) client 视角：
        //    - 商家发给游客（canonical out）：
        //         a. 如果本地已经存在这条消息且本地已读=true，则保留 true
        //         b. 如果本地不存在这条消息，则作为新消息写 false
        //    - 游客自己发给商家（canonical in）=> 对自己来说不算未读，写 true
        fun mapLocalIsReadForPerspective(
            canonicalDirection: String,
            cloudIsRead: Boolean,
            perspectiveRole: String,
            existingLocal: ChatMessageEntity?
        ): Boolean {
            return if (perspectiveRole == "merchant") {
                cloudIsRead
            } else {
                if (canonicalDirection == "out") {
                    existingLocal?.isRead ?: false
                } else {
                    true
                }
            }
        }

        val upserted = withContext(Dispatchers.IO) {
            var cnt = 0
            for (m in sourceMsgs) {
                try {
                    val canonicalDirection = mapDirectionCanonical(m.direction)
                    val existingLocal = dao(context).findById(m.id)

                    val entity = ChatMessageEntity(
                        id = m.id,
                        conversationId = m.conversationId,
                        storeId = m.storeId,
                        clientId = m.clientId,
                        role = m.role,
                        direction = canonicalDirection,
                        text = m.text,
                        timeMs = m.timeMs,
                        status = "sent",
                        isRead = mapLocalIsReadForPerspective(
                            canonicalDirection = canonicalDirection,
                            cloudIsRead = m.isRead,
                            perspectiveRole = perspectiveRole,
                            existingLocal = existingLocal
                        )
                    )
                    dao(context).upsert(entity)
                    cnt++
                } catch (t: Throwable) {
                    android.util.Log.e(
                        "ChatTrace",
                        "[$tid] repo syncConversationFromCloud upsert FAIL id=${m.id} conv=$conversationId ${t.javaClass.simpleName}: ${t.message}"
                    )
                }
            }
            cnt
        }

        val latestNewMessageTimeMs = sourceMsgs.maxOfOrNull { it.timeMs } ?: 0L
        if (upserted > 0) {
            reviveDeletedThreadIfNeeded(
                context = context,
                storeId = storeId,
                conversationId = conversationId,
                latestNewMessageTimeMs = latestNewMessageTimeMs
            )
        }

        android.util.Log.e("ChatTrace", "[$tid] repo syncConversationFromCloud done upserted=$upserted conv=$conversationId")
        return upserted
    }
    // --------------------
// Relay-only: consume messages for merchant chatlist
// --------------------
    suspend fun consumeRelayForMerchant(
        context: Context,
        storeId: String,
        traceId: String? = null
    ): Int {
        if (!isChatRelayEnabled()) return 0
        val tid = effectiveTraceId(traceId)
        return withContext(Dispatchers.IO) {
            val rows = cloud!!.pullRelayByStore(
                storeId = storeId,
                clientId = null,
                asMerchant = true,
                limit = 200,
                traceId = tid
            )
            if (rows.isEmpty()) return@withContext 0

            val consumeIds = ArrayList<String>(rows.size)
            var inserted = 0
            val latestNewMessageByConversation = LinkedHashMap<String, Long>()

            for (r in rows) {
                // 商家端只消费“非商家发出的”（用户消息 + read_receipt）
                if (r.fromRole == "merchant") continue

                val payload = r.payload
                val type = payload.optString("type", "text")

                // ✅ read receipt：用户已打开并阅读，商家端把该会话所有 out 消息置为已读
                if (type == "read_receipt") {
                    dao(context).markAllOutgoingRead(r.conversationId)
                    consumeIds.add(r.id)
                    inserted++
                    continue
                }

                // ✅ 正常用户消息
                val text = payload.optString("text")
                val timeMs = payload.optLong("timeMs", System.currentTimeMillis())
                val status = "sent"

                val entity = ChatMessageEntity(
                    id = r.id,
                    storeId = r.storeId,
                    role = "client",
                    direction = "in", // canonical（商家语义）：游客发给商家 = in
                    text = text,
                    timeMs = timeMs,
                    status = status,
                    isRead = false,
                    conversationId = r.conversationId,
                    clientId = r.clientId
                )

                dao(context).upsert(entity)
                latestNewMessageByConversation[r.conversationId] =
                    maxOf(latestNewMessageByConversation[r.conversationId] ?: 0L, timeMs)

                consumeIds.add(r.id)
                inserted++
            }

            for ((conversationId, latestTimeMs) in latestNewMessageByConversation) {
                reviveDeletedThreadIfNeeded(
                    context = context,
                    storeId = storeId,
                    conversationId = conversationId,
                    latestNewMessageTimeMs = latestTimeMs
                )
            }

            if (consumeIds.isNotEmpty()) {
                cloud.deleteRelay(consumeIds, traceId = tid)
            }
            inserted
        }
    }
    suspend fun markAllOutgoingRead(context: Context, conversationId: String) {
        dao(context).markAllOutgoingRead(conversationId)
    }

    suspend fun enqueueReadReceiptForClient(
        context: Context,
        storeId: String,
        conversationId: String,
        clientId: String,
        traceId: String? = null
    ): Boolean {
        if (!isChatRelayEnabled()) return false
        val tid = effectiveTraceId(traceId)

        return withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val payload = JSONObject().apply {
                put("type", "read_receipt")
                put("timeMs", now)
            }

            val id = "rr_${now}_${UUID.randomUUID().toString().take(8)}"

            cloud!!.enqueueRelay(
                ShowcaseChatCloudRepository.RelayRow(
                    id = id,
                    conversationId = conversationId,
                    storeId = storeId,
                    clientId = clientId,
                    fromRole = "client",
                    payload = payload
                ),
                traceId = tid
            )
        }
    }
    // --------------------
// Relay-only: consume messages for client chat
// --------------------
    suspend fun consumeRelayForClient(
        context: Context,
        storeId: String,
        clientId: String,
        traceId: String? = null
    ): Int {
        if (!isChatRelayEnabled()) return 0
        val tid = effectiveTraceId(traceId)

        return withContext(Dispatchers.IO) {
            // 目前云端只提供 pullRelayByStore，所以先按 store 拉，再本地过滤 clientId/fromRole
            val rows = cloud!!.pullRelayByStore(
                storeId = storeId,
                clientId = clientId,
                asMerchant = false,
                limit = 200,
                traceId = tid
            )
            if (rows.isEmpty()) return@withContext 0

            val consumeIds = ArrayList<String>(rows.size)
            var inserted = 0

            for (r in rows) {
                // 普通用户端只消费“商家发来的”
                if (r.fromRole != "merchant") continue
                if (r.clientId != clientId) continue

                val payload = r.payload
                val text = payload.optString("text")
                val timeMs = payload.optLong("timeMs", System.currentTimeMillis())
                val status = "sent"

                val entity = ChatMessageEntity(
                    id = r.id,
                    storeId = r.storeId,
                    role = "merchant",
                    direction = "out", // ✅ canonical（商家语义）：商家发给用户 = out
                    text = text,
                    timeMs = timeMs,
                    status = status,
                    isRead = false,
                    conversationId = r.conversationId,
                    clientId = r.clientId
                )

                dao(context).upsert(entity)
                consumeIds.add(r.id)
                inserted++
            }

            if (consumeIds.isNotEmpty()) {
                cloud.deleteRelay(consumeIds, traceId = tid)
            }
            inserted
        }
    }




}
