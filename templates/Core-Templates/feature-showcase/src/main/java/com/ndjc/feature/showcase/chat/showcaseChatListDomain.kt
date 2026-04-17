package com.ndjc.feature.showcase

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * 商家聊天列表页逻辑
 * 只做聚合规则，不做 UI
 */
class ShowcaseChatListDomain(
    private val repo: ShowcaseChatRepository
) {

    suspend fun buildMerchantThreads(
        context: Context,
        storeId: String
    ): List<ShowcaseChatThreadSummaryUi> {

        val metaTraceId = "M${System.currentTimeMillis()}_${storeId.takeLast(4)}"
        try {
            repo.syncMerchantThreadMetaFromCloud(
                context = context,
                storeId = storeId,
                traceId = metaTraceId
            )
        } catch (t: Throwable) {
            android.util.Log.e("ChatTrace", "[$metaTraceId] chatlist syncMerchantThreadMeta FAILED: ${t.message}")
        }

// 1) Cloud -> Room：先把“列表所需的最新消息”同步进本地（两种模式）
//
// A) Relay-only：从云端 relay 拉取并落 Room
        if (repo.isChatRelayEnabled()) {
            val traceId = "R${System.currentTimeMillis()}_${storeId.takeLast(4)}"
            try {
                val n = repo.consumeRelayForMerchant(context, storeId, traceId = traceId)
                android.util.Log.e("ChatTrace", "[$traceId] chatlist consumeRelay inserted=$n")
            } catch (t: Throwable) {
                android.util.Log.e("ChatTrace", "chatlist consumeRelay FAILED: ${t.message}")
            }
        }

// B) Direct cloud（非 relay）：先拉 threads summary，再逐个把对应会话同步到 Room
        if (repo.isChatCloudEnabled()) {
            val traceId = "L${System.currentTimeMillis()}_${storeId.takeLast(4)}"
            try {
                val summaries = repo.fetchCloudThreadSummaries(storeId = storeId, traceId = traceId)
                val top = summaries.take(20)
                var upsertedTotal = 0
                for (s in top) {
                    upsertedTotal += repo.syncConversationFromCloud(
                        context = context,
                        storeId = storeId,
                        conversationId = s.conversationId,
                        perspectiveRole = "merchant",
                        traceId = traceId
                    )
                }
                android.util.Log.e("ChatTrace", "[$traceId] chatlist cloudSync threads=${top.size} upserted=$upsertedTotal")

                if (top.isNotEmpty()) {
                    val cloudUi = buildMerchantThreadsFromCloudSummaries(
                        context = context,
                        storeId = storeId,
                        summaries = top
                    )
                    if (cloudUi.isNotEmpty()) {
                        android.util.Log.e("ChatTrace", "[$traceId] chatlist useCloudSummaries threads=${cloudUi.size}")
                        return cloudUi
                    }
                }
            } catch (t: Throwable) {
                android.util.Log.e("ChatTrace", "chatlist cloudSync FAILED: ${t.message}")
            }
        }

        return buildMerchantThreadsFromLocal(context, storeId)
    }

    suspend fun buildMerchantThreadsFromLocal(
        context: Context,
        storeId: String
    ): List<ShowcaseChatThreadSummaryUi> {
        val all = repo.listLocalByStore(context, storeId)
        if (all.isEmpty()) return emptyList()

        val metaMap = repo.listThreadMetaByStore(context, storeId)
            .associateBy { it.conversationId }

        val byConv = all.groupBy { it.conversationId }

        val seqMap: Map<String, Int> = byConv.entries
            .map { (conversationId, msgs) ->
                val firstMs = msgs.minOfOrNull { it.timeMs } ?: Long.MAX_VALUE
                conversationId to firstMs
            }
            .sortedBy { it.second }
            .mapIndexed { idx, pair -> pair.first to (idx + 1) }
            .toMap()

        val triples: List<Triple<Pair<Long, Long>, ShowcaseChatThreadSummaryUi, Unit>> =
            byConv.mapNotNull { (conversationId, msgs) ->

                val meta = metaMap[conversationId]
                if (meta?.isDeleted == true) {
                    return@mapNotNull null
                }

                val last = msgs.maxByOrNull { it.timeMs }
                val lastMs = last?.timeMs ?: 0L

                val previewRaw = last?.text.orEmpty()
                val preview = buildThreadPreview(previewRaw)

                val timeText = if (lastMs > 0L) formatYmdAmpmHm(context, lastMs) else ""
                val unread = repo.countUnread(context, conversationId)

                val alias = meta?.alias?.trim()?.takeIf { it.isNotBlank() }
                val seq = seqMap[conversationId] ?: 0
                val title = alias ?: if (seq > 0) "Customer #$seq" else "Customer"

                val pinnedAt = meta?.pinnedAtMs ?: 0L
                val isPinned = pinnedAt > 0L

                val ui = ShowcaseChatThreadSummaryUi(
                    threadId = conversationId,
                    title = title,
                    lastPreview = preview,
                    lastTimeText = timeText,
                    unreadCount = unread,
                    isPinned = isPinned
                )

                Triple(pinnedAt to lastMs, ui, Unit)
            }

        return triples
            .sortedWith(
                compareByDescending<Triple<Pair<Long, Long>, ShowcaseChatThreadSummaryUi, Unit>> { it.first.first }
                    .thenByDescending { it.first.second }
            )
            .map { it.second }
    }
    private suspend fun buildMerchantThreadsFromCloudSummaries(
        context: Context,
        storeId: String,
        summaries: List<ShowcaseChatRepository.CloudThreadSummary>
    ): List<ShowcaseChatThreadSummaryUi> {
        if (summaries.isEmpty()) return emptyList()

        val metaMap = repo.listThreadMetaByStore(context, storeId)
            .associateBy { it.conversationId }

        val seqMap = summaries
            .mapIndexed { index, s -> s.conversationId to (index + 1) }
            .toMap()

        val triples = summaries.mapNotNull { s ->
            val conversationId = s.conversationId
            if (conversationId.isBlank()) return@mapNotNull null

            val meta = metaMap[conversationId]
            if (meta?.isDeleted == true) return@mapNotNull null

            val lastMs = parseCloudIsoToMs(s.lastMessageAtIso)
                ?: parseCloudIsoToMs(s.updatedAtIso)
                ?: 0L

            val preview = buildThreadPreview(s.lastPreview.orEmpty())
            val timeText = if (lastMs > 0L) formatYmdAmpmHm(context, lastMs) else ""
            val unread = repo.countUnread(context, conversationId)

            val alias = meta?.alias?.trim()?.takeIf { it.isNotBlank() }
            val seq = seqMap[conversationId] ?: 0
            val title = alias ?: if (seq > 0) "Customer #$seq" else "Customer"

            val pinnedAt = meta?.pinnedAtMs ?: 0L
            val isPinned = pinnedAt > 0L

            val ui = ShowcaseChatThreadSummaryUi(
                threadId = conversationId,
                title = title,
                lastPreview = preview,
                lastTimeText = timeText,
                unreadCount = unread,
                isPinned = isPinned
            )

            Triple(pinnedAt to lastMs, ui, Unit)
        }

        return triples
            .sortedWith(
                compareByDescending<Triple<Pair<Long, Long>, ShowcaseChatThreadSummaryUi, Unit>> { it.first.first }
                    .thenByDescending { it.first.second }
            )
            .map { it.second }
    }

    private fun parseCloudIsoToMs(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null

        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss.SSSXXX",
            "yyyy-MM-dd HH:mm:ssXXX"
        )

        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val parsed = sdf.parse(iso)
                if (parsed != null) return parsed.time
            } catch (_: Throwable) {
            }
        }

        return null
    }
    // --- 列表预览净化：把 NDJC 编码（图片/引用）剥掉，列表上显示干净文案 ---
    private fun buildThreadPreview(text: String): String {
        if (text.isBlank()) return ""
        if (text.contains("⟪P⟫")) return ""
        var s = text.trim()

        // ✅ 兼容 "{⟪Q⟫...}" / "({⟪I⟫...}"：如果前 3 个字符内出现 "⟪"，从它开始对齐
        val firstMarker = s.indexOf("⟪")
        if (firstMarker >= 0 && firstMarker <= 3) {
            s = s.substring(firstMarker)
        }

        // 1) 去掉图片头：⟪I⟫...⟪/I⟫
        if (s.startsWith("⟪I⟫")) {
            val end = s.indexOf("⟪/I⟫")
            if (end > 0) {
                s = s.substring(end + "⟪/I⟫".length).trimStart('\n', ' ')
            } else {
                return ""
            }
        }

        // 2) 去掉引用头：⟪Q⟫...⟪/Q⟫
        if (s.startsWith("⟪Q⟫")) {
            val end = s.indexOf("⟪/Q⟫")
            if (end > 0) {
                s = s.substring(end + "⟪/Q⟫".length).trimStart('\n', ' ')
            } else {
                return ""
            }
        }

        // ✅ 3) 去掉商品卡头：⟪P⟫...⟪/P⟫（你现在缺的就是这个）
        if (s.startsWith("⟪P⟫")) {
            val end = s.indexOf("⟪/P⟫")
            if (end > 0) {
                s = s.substring(end + "⟪/P⟫".length).trimStart('\n', ' ')
            } else {
                return ""
            }
        }

        return s
            .replace('\n', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(60)
    }

    private fun formatHHmm(ms: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = ms }
        val hh = cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val mm = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
        return "$hh:$mm"
    }

    private fun formatYmdAmpmHm(context: Context, ms: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = ms }

        val y = cal.get(Calendar.YEAR)
        val mo = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val d = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')

        val h24 = cal.get(Calendar.HOUR_OF_DAY)
        val mm = cal.get(Calendar.MINUTE).toString().padStart(2, '0')

        return if (android.text.format.DateFormat.is24HourFormat(context)) {
            val hh = h24.toString().padStart(2, '0')
            "$y-$mo-$d $hh:$mm"
        } else {
            val ampm = if (h24 < 12) "AM" else "PM"
            val h12 = (h24 % 12).let { if (it == 0) 12 else it }.toString().padStart(2, '0')
            "$y-$mo-$d $ampm $h12:$mm"
        }
    }
    suspend fun deleteThread(context: Context, storeId: String, conversationId: String) {
        repo.deleteThreadLocal(context, storeId, conversationId)
    }

    suspend fun setPinned(context: Context, storeId: String, conversationId: String, pinned: Boolean) {
        repo.setThreadPinned(context, storeId, conversationId, pinned)
    }

    suspend fun markThreadRead(context: Context, conversationId: String) {
        repo.markAllRead(context, conversationId)
    }
    suspend fun setThreadAlias(context: Context, storeId: String, conversationId: String, alias: String?) {
        repo.setThreadAlias(context, storeId, conversationId, alias)
    }
    // ✅ 全局搜索：同时搜「聊天内容」+「用户名称（alias/客户 #N）」
// 返回给 ChatSearchResults 页使用（UI 只渲染）
    // ✅ 新增：只抽取“主信息”用于搜索（不匹配引用块内容、不匹配图片块、不匹配商品块）
    private fun extractMainBodyForSearch(raw: String): String {
        if (raw.isBlank()) return ""
        if (raw.contains("⟪P⟫")) return "" // 商品卡整条消息：不参与

        var s = raw.trim()

        val firstMarker = s.indexOf("⟪")
        if (firstMarker >= 0 && firstMarker <= 3) {
            s = s.substring(firstMarker)
        }

        // 1) 剥离图片块
        while (true) {
            val si = s.indexOf("⟪I⟫")
            if (si < 0) break
            val ei = s.indexOf("⟪/I⟫", startIndex = si + "⟪I⟫".length)
            if (ei < 0) break
            s = (s.substring(0, si) + s.substring(ei + "⟪/I⟫".length)).trim()
        }

        // 2) 剥离引用块
        while (true) {
            val sq = s.indexOf("⟪Q⟫")
            if (sq < 0) break
            val eq = s.indexOf("⟪/Q⟫", startIndex = sq + "⟪Q⟫".length)
            if (eq < 0) break
            s = (s.substring(0, sq) + s.substring(eq + "⟪/Q⟫".length)).trim()
        }

        // 3) 剥离商品块
        while (true) {
            val sp = s.indexOf("⟪P⟫")
            if (sp < 0) break
            val ep = s.indexOf("⟪/P⟫", startIndex = sp + "⟪P⟫".length)
            if (ep < 0) break
            s = (s.substring(0, sp) + s.substring(ep + "⟪/P⟫".length)).trim()
        }

        return s
            .replace('\n', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    suspend fun searchMessagesAndNames(
        context: Context,
        storeId: String,
        keyword: String,
        merchantSenderLabel: String,
        allowedConversationIds: Set<String>? = null
    ): List<ShowcaseChatGlobalSearchResultUi> {

        val q = keyword.trim()
        if (q.isBlank()) return emptyList()
        val all = repo.listLocalByStore(context, storeId)
        if (all.isEmpty()) return emptyList()

        val metaMap = repo.listThreadMetaByStore(context, storeId).associateBy { it.conversationId }

        val allow = allowedConversationIds?.takeIf { it.isNotEmpty() }
        val byConv = all.groupBy { it.conversationId }
            .let { grouped ->
                if (allow == null) grouped else grouped.filterKeys { it in allow }
            }

        if (byConv.isEmpty()) return emptyList()

        // 会话编号：按最早消息时间升序 => 客户 #1, #2...
        val seqMap: Map<String, Int> = byConv.entries
            .map { (conversationId, msgs) ->
                val firstMs = msgs.minOfOrNull { it.timeMs } ?: Long.MAX_VALUE
                conversationId to firstMs
            }
            .sortedBy { it.second }
            .mapIndexed { idx, pair -> pair.first to (idx + 1) }
            .toMap()

        data class ThreadIndex(
            val conversationId: String,
            val title: String,
            val lastPreview: String,
            val lastMs: Long,
            val lastTimeText: String
        )

        val indexMap: Map<String, ThreadIndex> = byConv.mapNotNull { (conversationId, msgs) ->
            val meta = metaMap[conversationId]
            if (meta?.isDeleted == true) return@mapNotNull null

            val last = msgs.maxByOrNull { it.timeMs }
            val lastMs = last?.timeMs ?: 0L
            val preview = buildThreadPreview(last?.text.orEmpty())
            val timeText = if (lastMs > 0L) formatYmdAmpmHm(context, lastMs) else ""

            val alias = meta?.alias?.trim()?.takeIf { it.isNotBlank() }
            val seq = seqMap[conversationId] ?: 0
            val title = alias ?: if (seq > 0) "Customer #$seq" else "Customer"

            ThreadIndex(
                conversationId = conversationId,
                title = title,
                lastPreview = preview,
                lastMs = lastMs,
                lastTimeText = timeText
            )
        }.associateBy { it.conversationId }
        val msgHits = all
            .asSequence()
            .filter { m -> (allow == null || m.conversationId in allow) && m.conversationId in indexMap }
            .map { m -> m to extractMainBodyForSearch(m.text) }
            .filter { (_, body) -> body.contains(q, ignoreCase = true) }
            .sortedByDescending { (m, _) -> m.timeMs }
            .take(80)
            .map { (m, body) ->
                val customerTitle = indexMap[m.conversationId]?.title ?: "Customer"
                val senderLabel = if (m.direction == "out") {
                    merchantSenderLabel.trim().ifBlank { "Merchant" }
                } else {
                    customerTitle
                }

                ShowcaseChatGlobalSearchResultUi(
                    conversationId = m.conversationId,
                    messageId = m.id,
                    displayName = customerTitle,
                    senderLabel = senderLabel,
                    snippet = body.take(60),
                    timeMs = m.timeMs,
                    timeText = formatYmdAmpmHm(context, m.timeMs),
                    matchedInName = false
                )
            }
            .toList()


        // 3) 命中「用户名称」
        val nameHits = indexMap.values
            .filter { it.title.contains(q, ignoreCase = true) }
            .map { t ->
                ShowcaseChatGlobalSearchResultUi(
                    conversationId = t.conversationId,
                    messageId = null,
                    displayName = t.title,
                    senderLabel = t.title,
                    snippet = t.lastPreview,
                    timeMs = t.lastMs,
                    timeText = t.lastTimeText,
                    matchedInName = true
                )
            }

        return (msgHits + nameHits)
            .distinctBy { it.conversationId to (it.messageId ?: "") }
            .sortedByDescending { it.timeMs }
    }


}
