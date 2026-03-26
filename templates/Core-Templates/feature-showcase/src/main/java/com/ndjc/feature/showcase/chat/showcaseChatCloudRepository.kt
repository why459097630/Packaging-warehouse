package com.ndjc.feature.showcase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream

class ShowcaseChatCloudRepository(
    private val logTag: String = "ChatTrace"
) {


    data class CloudMsg(
        val id: String,
        val conversationId: String,
        val storeId: String,
        val clientId: String,
        val role: String,
        val direction: String,
        val text: String,
        val timeMs: Long,
        val isRead: Boolean
    )

    private data class ChatCloudConfig(
        val base: String,
        val apiKey: String
    )

    private fun requireConfig(): ChatCloudConfig? {
        val base = ShowcaseCloudConfig.SUPABASE_URL.trim().trimEnd('/')
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY.trim()
        if (base.isBlank() || apiKey.isBlank()) return null

        return ChatCloudConfig(
            base = base,
            apiKey = apiKey
        )
    }

    private fun readAll(conn: java.net.HttpURLConnection): String {
        return try {
            val stream = try {
                conn.inputStream
            } catch (_: Throwable) {
                conn.errorStream
            } ?: return ""

            stream.bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            ""
        }
    }

    private fun restUrl(path: String): String {
        val cfg = requireConfig()!!
        val clean = path.trimStart('/')
        return "${cfg.base}/rest/v1/$clean"
    }

    private fun openConn(
        url: String,
        method: String,
        actor: ShowcaseCloudConfig.AuthActor,
        scopeStoreId: String,
        scopeClientId: String? = null
    ): HttpURLConnection {
        val cfg = requireConfig()!!
        val token = when (actor) {
            ShowcaseCloudConfig.AuthActor.PUBLIC -> {
                ShowcaseCloudConfig.authToken(actor).trim().ifBlank { cfg.apiKey }
            }
            ShowcaseCloudConfig.AuthActor.MERCHANT -> {
                ShowcaseMerchantSessionManager.ensureValidMerchantAccessToken()
                    ?.trim()
                    .orEmpty()
                    .ifBlank {
                        ShowcaseStoreSession.currentMerchantAccessToken()?.trim().orEmpty()
                    }
                    .ifBlank { cfg.apiKey }
            }
        }

        return (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 10_000
            doOutput = method != "GET" && method != "DELETE"
            setRequestProperty("apikey", cfg.apiKey)
            setRequestProperty("Authorization", "Bearer $token")
            ShowcaseCloudConfig.requestScopeHeaders(
                storeId = scopeStoreId,
                clientId = scopeClientId
            ).forEach { (k, v) ->
                setRequestProperty(k, v)
            }
            setRequestProperty("Accept", "application/json")
            if (method != "GET" && method != "DELETE") {
                setRequestProperty("Content-Type", "application/json")
            }
        }
    }
    private fun readBody(conn: HttpURLConnection, code: Int): String? {
        return try {
            (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
        } catch (_: Throwable) {
            null
        }
    }

    @Volatile var lastChatImageUploadCode: Int? = null
        private set
    @Volatile var lastChatImageUploadBody: String? = null
        private set

    fun isMerchantAuthExpired(code: Int?, body: String?): Boolean {
        val c = code ?: return false
        val text = body?.trim().orEmpty()
        if (c == 401 || c == 403) return true
        return text.contains("JWT expired", ignoreCase = true) ||
                text.contains("\"exp\" claim timestamp check failed", ignoreCase = true) ||
                text.contains("Unauthorized", ignoreCase = true) ||
                text.contains("jwt expired", ignoreCase = true)
    }

    private fun effectiveTraceId(traceId: String?): String {
        val t = traceId?.trim().orEmpty()
        return if (t.isNotEmpty() && t != "-") t
        else "C${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(1000, 9999)}"
    }

    private fun isMerchantAuthExpired(code: Int, body: String?): Boolean {
        if (code != 401 && code != 403) return false
        val b = body?.lowercase().orEmpty()
        return b.contains("jwt") ||
                b.contains("token") ||
                b.contains("expired") ||
                b.contains("auth")
    }

    private fun refreshMerchantSessionIfNeeded(): Boolean {
        return try {
            val ok = ShowcaseMerchantSessionManager.forceRefreshMerchantSession()
            Log.d(logTag, "refreshMerchantSessionIfNeeded: success=$ok")
            ok
        } catch (t: Throwable) {
            Log.e(logTag, "refreshMerchantSessionIfNeeded failed", t)
            false
        }
    }

    suspend fun upsertConversation(conversationId: String, storeId: String, clientId: String): Boolean =
        withContext(Dispatchers.IO) {
            val cfg = requireConfig()
            if (cfg == null) {
                Log.e(logTag, "SKIP: SUPABASE_URL / SUPABASE_ANON_KEY blank")
                return@withContext false
            }

            val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
            if (conversationId.isBlank() || clientId.isBlank()) {
                Log.e(logTag, "SKIP blank fields conv=$conversationId store=$currentStoreId client=$clientId")
                return@withContext false
            }

            val url = restUrl("${ShowcaseCloudConfig.TABLE_CHAT_CONVERSATIONS}?on_conflict=conversation_id")
            val payload = JSONArray().apply {
                put(JSONObject().apply {
                    put("conversation_id", conversationId)
                    put("store_id", currentStoreId)
                    put("client_id", clientId)
                })
            }.toString()

            val conn = openConn(
                url = url,
                method = "POST",
                actor = ShowcaseCloudConfig.AuthActor.PUBLIC,
                scopeStoreId = currentStoreId,
                scopeClientId = clientId
            )
            conn.setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")

            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val body = readBody(conn, code)
            if (code !in 200..299) Log.e(logTag, "upsertConversation FAILED code=$code body=$body url=$url")
            code in 200..299
        }

    suspend fun upsertConversation(
        conversationId: String,
        storeId: String,
        clientId: String,
        traceId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val cfg = requireConfig()
        val tid = effectiveTraceId(traceId)
        if (cfg == null) {
            Log.e(logTag, "[$tid] SKIP: SUPABASE_URL / SUPABASE_ANON_KEY blank")
            return@withContext false
        }
        if (conversationId.isBlank() || storeId.isBlank() || clientId.isBlank()) {
            Log.e(logTag, "[$tid] SKIP blank fields conv=$conversationId store=$storeId client=$clientId")
            return@withContext false
        }

        val url = restUrl("${ShowcaseCloudConfig.TABLE_CHAT_CONVERSATIONS}?on_conflict=conversation_id")
        val payload = JSONArray().apply {
            put(JSONObject().apply {
                put("conversation_id", conversationId)
                put("store_id", storeId)
                put("client_id", clientId)
            })
        }.toString()

        Log.e(logTag, "[$tid] upsertConversation REQ url=$url body=$payload")

        val conn = openConn(
            url = url,
            method = "POST",
            actor = ShowcaseCloudConfig.AuthActor.PUBLIC,
            scopeStoreId = storeId,
            scopeClientId = clientId
        )
        conn.setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")

        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val body = readBody(conn, code)

        val ok = code in 200..299
        if (!ok) Log.e(logTag, "[$tid] upsertConversation FAILED code=$code body=$body url=$url")
        else Log.e(logTag, "[$tid] upsertConversation OK code=$code")
        ok
    }

    // --------------------
    // Cloud GET (Supabase) - Phase1: 读列表 + 读会话消息
    // --------------------
    data class CloudThreadSummaryRow(
        val conversationId: String,
        val storeId: String,
        val clientId: String,
        val lastMessageAtIso: String?,
        val lastPreview: String?,
        val updatedAtIso: String?
    )

    data class CloudThreadMetaRow(
        val conversationId: String,
        val storeId: String,
        val merchantAlias: String?,
        val merchantArchived: Boolean,
        val merchantArchivedAtMs: Long
    )

    private fun encode(v: String): String =
        java.net.URLEncoder.encode(v, "UTF-8")
    // --------------------
// Relay-only: chat_relay (no cloud history)
// --------------------
    data class RelayRow(
        val id: String,
        val conversationId: String,
        val storeId: String,
        val clientId: String,
        val fromRole: String,     // 'user' | 'merchant'
        val payload: JSONObject,
        val createdAt: String? = null
    )

    suspend fun enqueueRelay(row: RelayRow, traceId: String? = null): Boolean =
        withContext(Dispatchers.IO) {
            val cfg = requireConfig()
            val tid = effectiveTraceId(traceId)
            if (cfg == null) {
                Log.e(logTag, "[$tid] SKIP enqueueRelay: config blank")
                return@withContext false
            }

            val url = restUrl(ShowcaseCloudConfig.TABLE_CHAT_RELAY)
            val body = JSONObject().apply {
                put("id", row.id)
                put("conversation_id", row.conversationId)
                put("store_id", row.storeId)
                put("client_id", row.clientId)
                put("from_role", row.fromRole)
                put("payload", row.payload)
            }.toString()

            Log.e(logTag, "[$tid] relay enqueue REQ url=$url body=${body.take(800)}")

            val actor = if (
                row.fromRole.equals("merchant", ignoreCase = true) ||
                row.fromRole.equals("admin", ignoreCase = true)
            ) {
                ShowcaseCloudConfig.AuthActor.MERCHANT
            } else {
                ShowcaseCloudConfig.AuthActor.PUBLIC
            }

            val conn = openConn(
                url = url,
                method = "POST",
                actor = actor,
                scopeStoreId = row.storeId,
                scopeClientId = if (actor == ShowcaseCloudConfig.AuthActor.PUBLIC) row.clientId else null
            )
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val resp = readBody(conn, code)
            Log.e(logTag, "[$tid] relay enqueue RESP code=$code body=${(resp ?: "").take(800)}")
            code in 200..299
        }

    suspend fun pullRelayByStore(
        storeId: String,
        clientId: String? = null,
        asMerchant: Boolean = false,
        limit: Int = 200,
        traceId: String? = null
    ): List<RelayRow> = withContext(Dispatchers.IO) {
        val cfg = requireConfig()
        val tid = effectiveTraceId(traceId)
        if (cfg == null) return@withContext emptyList()
        if (storeId.isBlank()) return@withContext emptyList()

        val url = restUrl(
            "${ShowcaseCloudConfig.TABLE_CHAT_RELAY}" +
                    "?store_id=eq.${encode(storeId)}" +
                    "&order=created_at.asc" +
                    "&limit=${limit.coerceIn(1, 200)}"
        )

        Log.e(logTag, "[$tid] relay pullByStore REQ url=$url")
        val conn = openConn(
            url = url,
            method = "GET",
            actor = if (asMerchant) {
                ShowcaseCloudConfig.AuthActor.MERCHANT
            } else {
                ShowcaseCloudConfig.AuthActor.PUBLIC
            },
            scopeStoreId = storeId,
            scopeClientId = if (asMerchant) null else clientId
        )
        val code = conn.responseCode
        val body = readBody(conn, code) ?: "[]"
        Log.e(logTag, "[$tid] relay pullByStore RESP code=$code body=${body.take(1200)}")
        if (code !in 200..299) return@withContext emptyList()

        val arr = JSONArray(body)
        val out = ArrayList<RelayRow>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                RelayRow(
                    id = o.optString("id"),
                    conversationId = o.optString("conversation_id"),
                    storeId = o.optString("store_id"),
                    clientId = o.optString("client_id"),
                    fromRole = o.optString("from_role"),
                    payload = o.optJSONObject("payload") ?: JSONObject(),
                    createdAt = o.optString("created_at", null)
                )
            )
        }
        out
    }

    suspend fun deleteRelay(ids: List<String>, traceId: String? = null): Boolean =
        withContext(Dispatchers.IO) {
            val cfg = requireConfig()
            val tid = effectiveTraceId(traceId)
            if (cfg == null) return@withContext false
            if (ids.isEmpty()) return@withContext true

            val inList = ids.joinToString(",")
            val url = restUrl("${ShowcaseCloudConfig.TABLE_CHAT_RELAY}?id=in.(${encode(inList)})")

            Log.e(logTag, "[$tid] relay delete REQ url=$url ids=${ids.size}")
            val conn = openConn(
                url = url,
                method = "DELETE",
                actor = ShowcaseCloudConfig.AuthActor.MERCHANT,
                scopeStoreId = ShowcaseCloudConfig.currentStoreId()
            )
            val code = conn.responseCode
            val body = readBody(conn, code)
            Log.e(logTag, "[$tid] relay delete RESP code=$code body=${(body ?: "").take(800)}")
            code in 200..299
        }

    suspend fun fetchThreadSummaries(
        storeId: String,
        limit: Int = 30,
        traceId: String? = null
    ): List<CloudThreadSummaryRow> = withContext(Dispatchers.IO) {
        val cfg = requireConfig()
        val tid = effectiveTraceId(traceId)
        if (cfg == null) {
            Log.e(logTag, "[$tid] SKIP: SUPABASE_URL / SUPABASE_ANON_KEY blank")
            return@withContext emptyList()
        }

        fun parseRows(body: String?): List<CloudThreadSummaryRow> {
            val arr = JSONArray(body ?: "[]")
            return buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    add(
                        CloudThreadSummaryRow(
                            conversationId = o.optString("conversation_id"),
                            storeId = o.optString("store_id"),
                            clientId = o.optString("client_id"),
                            lastMessageAtIso = o.optString("last_message_at").takeIf { it.isNotBlank() },
                            lastPreview = o.optString("last_preview").takeIf { it.isNotBlank() },
                            updatedAtIso = o.optString("updated_at").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }

        val viewSelect = "conversation_id,store_id,client_id,last_message_at,last_preview,updated_at"
        val viewUrl = restUrl(
            "${ShowcaseCloudConfig.VIEW_CHAT_THREAD_SUMMARIES}" +
                    "?select=$viewSelect" +
                    "&store_id=eq.${encode(storeId)}" +
                    "&order=last_message_at.desc.nullslast" +
                    "&limit=$limit"
        )

        Log.e(logTag, "[$tid] fetchThreadSummaries VIEW REQ url=$viewUrl")

        if (ShowcaseStoreSession.currentMerchantAccessToken().isNullOrBlank()) {
            Log.e(logTag, "[$tid] fetchThreadSummaries VIEW SKIP merchant access token missing")
            return@withContext emptyList()
        }

        try {
            var conn = openConn(
                url = viewUrl,
                method = "GET",
                actor = ShowcaseCloudConfig.AuthActor.MERCHANT,
                scopeStoreId = storeId
            )
            var code = conn.responseCode
            var body = readBody(conn, code)

            if (isMerchantAuthExpired(code, body)) {
                Log.w(logTag, "[$tid] fetchThreadSummaries VIEW token expired, try refresh once")
                val refreshed = refreshMerchantSessionIfNeeded()
                if (refreshed) {
                    conn = openConn(
                        url = viewUrl,
                        method = "GET",
                        actor = ShowcaseCloudConfig.AuthActor.MERCHANT,
                        scopeStoreId = storeId
                    )
                    code = conn.responseCode
                    body = readBody(conn, code)
                }
            }

            if (code in 200..299) {
                val rows = parseRows(body)
                if (rows.isNotEmpty()) {
                    Log.e(logTag, "[$tid] fetchThreadSummaries VIEW OK rows=${rows.size}")
                    return@withContext rows
                }
                Log.e(logTag, "[$tid] fetchThreadSummaries VIEW EMPTY -> fallback conversations")
            } else {
                Log.e(logTag, "[$tid] fetchThreadSummaries VIEW FAILED code=$code body=$body -> fallback conversations")
            }
        } catch (t: Throwable) {
            Log.e(logTag, "[$tid] fetchThreadSummaries VIEW ERROR ${t.javaClass.simpleName}: ${t.message} -> fallback conversations", t)
        }

        val convSelect = "conversation_id,store_id,client_id,updated_at"
        val convUrl = restUrl(
            "${ShowcaseCloudConfig.TABLE_CHAT_CONVERSATIONS}" +
                    "?select=$convSelect" +
                    "&store_id=eq.${encode(storeId)}" +
                    "&order=updated_at.desc.nullslast" +
                    "&limit=$limit"
        )

        Log.e(logTag, "[$tid] fetchThreadSummaries FALLBACK REQ url=$convUrl")

        try {
            var conn = openConn(
                url = convUrl,
                method = "GET",
                actor = ShowcaseCloudConfig.AuthActor.MERCHANT,
                scopeStoreId = storeId
            )
            var code = conn.responseCode
            var body = readBody(conn, code)

            if (isMerchantAuthExpired(code, body)) {
                Log.w(logTag, "[$tid] fetchThreadSummaries FALLBACK token expired, try refresh once")
                val refreshed = refreshMerchantSessionIfNeeded()
                if (refreshed) {
                    conn = openConn(
                        url = convUrl,
                        method = "GET",
                        actor = ShowcaseCloudConfig.AuthActor.MERCHANT,
                        scopeStoreId = storeId
                    )
                    code = conn.responseCode
                    body = readBody(conn, code)
                }
            }

            if (code !in 200..299) {
                Log.e(logTag, "[$tid] fetchThreadSummaries FALLBACK FAILED code=$code body=$body url=$convUrl")
                return@withContext emptyList()
            }

            val arr = JSONArray(body ?: "[]")
            val rows = buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    add(
                        CloudThreadSummaryRow(
                            conversationId = o.optString("conversation_id"),
                            storeId = o.optString("store_id"),
                            clientId = o.optString("client_id"),
                            lastMessageAtIso = null,
                            lastPreview = null,
                            updatedAtIso = o.optString("updated_at").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }

            Log.e(logTag, "[$tid] fetchThreadSummaries FALLBACK OK rows=${rows.size}")
            rows
        } catch (t: Throwable) {
            Log.e(logTag, "[$tid] fetchThreadSummaries FALLBACK ERROR ${t.javaClass.simpleName}: ${t.message}", t)
            emptyList()
        }
    }
    suspend fun fetchMerchantThreadMetaRows(
        storeId: String,
        traceId: String? = null
    ): List<CloudThreadMetaRow> = withContext(Dispatchers.IO) {
        val cfg = requireConfig()
        val tid = effectiveTraceId(traceId)
        if (cfg == null) {
            Log.e(logTag, "[$tid] SKIP: SUPABASE_URL / SUPABASE_ANON_KEY blank")
            return@withContext emptyList()
        }

        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
        val select = "conversation_id,store_id,merchant_alias,merchant_archived,merchant_archived_at_ms"
        val url = restUrl(
            "${ShowcaseCloudConfig.TABLE_CHAT_THREAD_META}" +
                    "?select=$select" +
                    "&store_id=eq.${encode(currentStoreId)}"
        )

        Log.e(logTag, "[$tid] fetchMerchantThreadMetaRows REQ url=$url")

        try {
            var conn = openConn(
                url = url,
                method = "GET",
                actor = ShowcaseCloudConfig.AuthActor.MERCHANT,
                scopeStoreId = currentStoreId
            )
            var code = conn.responseCode
            var body = readBody(conn, code)

            if (isMerchantAuthExpired(code, body)) {
                Log.w(logTag, "[$tid] fetchMerchantThreadMetaRows token expired, try refresh once")
                val refreshed = refreshMerchantSessionIfNeeded()
                if (refreshed) {
                    conn = openConn(
                        url = url,
                        method = "GET",
                        actor = ShowcaseCloudConfig.AuthActor.MERCHANT,
                        scopeStoreId = currentStoreId
                    )
                    code = conn.responseCode
                    body = readBody(conn, code)
                }
            }

            if (code !in 200..299) {
                Log.e(logTag, "[$tid] fetchMerchantThreadMetaRows FAILED code=$code body=$body url=$url")
                return@withContext emptyList()
            }

            val arr = JSONArray(body ?: "[]")
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    add(
                        CloudThreadMetaRow(
                            conversationId = o.optString("conversation_id"),
                            storeId = o.optString("store_id"),
                            merchantAlias = o.optString("merchant_alias").takeIf { it.isNotBlank() },
                            merchantArchived = o.optBoolean("merchant_archived", false),
                            merchantArchivedAtMs = o.optLong("merchant_archived_at_ms", 0L)
                        )
                    )
                }
            }.also {
                Log.e(logTag, "[$tid] fetchMerchantThreadMetaRows OK rows=${it.size}")
            }
        } catch (t: Throwable) {
            Log.e(logTag, "[$tid] fetchMerchantThreadMetaRows ERROR ${t.javaClass.simpleName}: ${t.message}", t)
            emptyList()
        }
    }

    suspend fun upsertMerchantThreadMeta(
        storeId: String,
        conversationId: String,
        merchantAlias: String?,
        merchantArchived: Boolean,
        merchantArchivedAtMs: Long,
        traceId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val cfg = requireConfig()
        val tid = effectiveTraceId(traceId)
        if (cfg == null) {
            Log.e(logTag, "[$tid] SKIP: SUPABASE_URL / SUPABASE_ANON_KEY blank")
            return@withContext false
        }
        if (storeId.isBlank() || conversationId.isBlank()) {
            Log.e(logTag, "[$tid] SKIP blank fields store=$storeId conv=$conversationId")
            return@withContext false
        }

        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
        val url = restUrl("${ShowcaseCloudConfig.TABLE_CHAT_THREAD_META}?on_conflict=store_id,conversation_id")

        val payload = JSONArray().apply {
            put(
                JSONObject().apply {
                    put("store_id", currentStoreId)
                    put("conversation_id", conversationId)
                    put("merchant_alias", merchantAlias ?: JSONObject.NULL)
                    put("merchant_archived", merchantArchived)
                    put("merchant_archived_at_ms", merchantArchivedAtMs)
                }
            )
        }.toString()

        Log.e(logTag, "[$tid] upsertMerchantThreadMeta REQ url=$url body=$payload")

        var conn = openConn(
            url = url,
            method = "POST",
            actor = ShowcaseCloudConfig.AuthActor.MERCHANT,
            scopeStoreId = currentStoreId
        )
        conn.setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")
        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

        var code = conn.responseCode
        var body = readBody(conn, code)

        if (isMerchantAuthExpired(code, body)) {
            Log.w(logTag, "[$tid] upsertMerchantThreadMeta token expired, try refresh once")
            val refreshed = refreshMerchantSessionIfNeeded()
            if (refreshed) {
                conn = openConn(
                    url = url,
                    method = "POST",
                    actor = ShowcaseCloudConfig.AuthActor.MERCHANT,
                    scopeStoreId = currentStoreId
                )
                conn.setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

                code = conn.responseCode
                body = readBody(conn, code)
            }
        }

        Log.e(logTag, "[$tid] upsertMerchantThreadMeta RESP code=$code body=${(body ?: "").take(1200)}")

        val ok = code in 200..299
        if (!ok) {
            Log.e(logTag, "[$tid] upsertMerchantThreadMeta FAILED code=$code body=$body url=$url")
        }
        ok
    }

    suspend fun fetchMessagesByConversation(
        storeId: String,
        conversationId: String,
        clientId: String? = null,
        asMerchant: Boolean = false,
        limit: Int = 120,
        traceId: String? = null
    ): List<CloudMsg> = withContext(Dispatchers.IO) {
        val cfg = requireConfig()
        val tid = effectiveTraceId(traceId)
        if (cfg == null) {
            Log.e(logTag, "[$tid] SKIP: SUPABASE_URL / SUPABASE_ANON_KEY blank")
            return@withContext emptyList()
        }

        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
        val select = "id,conversation_id,store_id,client_id,role,direction,text,time_ms,is_read"
        val url = restUrl(
            "${ShowcaseCloudConfig.TABLE_CHAT_MESSAGES}" +
                    "?select=$select" +
                    "&store_id=eq.${encode(currentStoreId)}" +
                    "&conversation_id=eq.${encode(conversationId)}" +
                    "&order=time_ms.desc" +
                    "&limit=$limit"
        )

        Log.e(logTag, "[$tid] fetchMessagesByConversation REQ url=$url")

        if (asMerchant && ShowcaseStoreSession.currentMerchantAccessToken().isNullOrBlank()) {
            Log.e(logTag, "[$tid] fetchMessagesByConversation SKIP merchant access token missing")
            return@withContext emptyList()
        }

        try {
            var conn = openConn(
                url = url,
                method = "GET",
                actor = if (asMerchant) {
                    ShowcaseCloudConfig.AuthActor.MERCHANT
                } else {
                    ShowcaseCloudConfig.AuthActor.PUBLIC
                },
                scopeStoreId = currentStoreId,
                scopeClientId = if (asMerchant) null else clientId
            )
            var code = conn.responseCode
            var body = readBody(conn, code)

            if (asMerchant && isMerchantAuthExpired(code, body)) {
                Log.w(logTag, "[$tid] fetchMessagesByConversation token expired, try refresh once")
                val refreshed = refreshMerchantSessionIfNeeded()
                if (refreshed) {
                    conn = openConn(
                        url = url,
                        method = "GET",
                        actor = ShowcaseCloudConfig.AuthActor.MERCHANT,
                        scopeStoreId = currentStoreId,
                        scopeClientId = null
                    )
                    code = conn.responseCode
                    body = readBody(conn, code)
                }
            }

            if (code !in 200..299) {
                Log.e(logTag, "[$tid] fetchMessagesByConversation FAILED code=$code body=$body url=$url")
                return@withContext emptyList()
            }

            val arr = JSONArray(body ?: "[]")
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    add(
                        CloudMsg(
                            id = o.optString("id"),
                            conversationId = o.optString("conversation_id"),
                            storeId = o.optString("store_id"),
                            clientId = o.optString("client_id"),
                            role = o.optString("role"),
                            direction = o.optString("direction"),
                            text = o.optString("text"),
                            timeMs = o.optLong("time_ms"),
                            isRead = o.optBoolean("is_read", false)
                        )
                    )
                }
            }.also {
                Log.e(logTag, "[$tid] fetchMessagesByConversation OK rows=${it.size}")
            }
        } catch (t: Throwable) {
            Log.e(logTag, "[$tid] fetchMessagesByConversation ERROR ${t.javaClass.simpleName}: ${t.message}", t)
            emptyList()
        }
    }

    suspend fun insertMessage(msg: CloudMsg, traceId: String? = null): Boolean =
        withContext(Dispatchers.IO) {
            val cfg = requireConfig()
            val tid = effectiveTraceId(traceId)
            if (cfg == null) {
                Log.e(logTag, "[$tid] SKIP: SUPABASE_URL / SUPABASE_ANON_KEY blank")
                return@withContext false
            }

            if (msg.id.isBlank() || msg.conversationId.isBlank() || msg.storeId.isBlank() || msg.clientId.isBlank()) {
                Log.e(
                    logTag,
                    "[$tid] SKIP blank fields id=${msg.id} conv=${msg.conversationId} store=${msg.storeId} client=${msg.clientId}"
                )
                return@withContext false
            }

            val url = restUrl("${ShowcaseCloudConfig.TABLE_CHAT_MESSAGES}?on_conflict=id")

            val payload = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", msg.id)
                    put("conversation_id", msg.conversationId)
                    put("store_id", msg.storeId)
                    put("client_id", msg.clientId)
                    put("role", msg.role)
                    put("direction", msg.direction)

                    put("content", msg.text) // ✅ 兼容云端触发器/函数用 NEW.content
                    put("text", msg.text)    // ✅ 你现有读链路仍用 text

                    put("time_ms", msg.timeMs)
                    put("is_read", msg.isRead)
                })
            }.toString()


// 你现在主要看 ChatTrace（而且只看 E），所以这里强制用 Log.e 打出来
            Log.e(logTag, "[$tid] cloud insertMessage REQ url=$url bodyHead=${payload.take(400)}")

            val actor = if (
                msg.role.equals("merchant", ignoreCase = true) ||
                msg.role.equals("admin", ignoreCase = true)
            ) {
                ShowcaseCloudConfig.AuthActor.MERCHANT
            } else {
                ShowcaseCloudConfig.AuthActor.PUBLIC
            }

            var conn = openConn(
                url = url,
                method = "POST",
                actor = actor,
                scopeStoreId = msg.storeId,
                scopeClientId = if (actor == ShowcaseCloudConfig.AuthActor.PUBLIC) msg.clientId else null
            )
            conn.setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")

            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            var code = conn.responseCode
            var body = readBody(conn, code)

            if (actor == ShowcaseCloudConfig.AuthActor.MERCHANT && isMerchantAuthExpired(code, body)) {
                Log.w(logTag, "[$tid] insertMessage token expired, try refresh once")
                val refreshed = refreshMerchantSessionIfNeeded()
                if (refreshed) {
                    conn = openConn(
                        url = url,
                        method = "POST",
                        actor = ShowcaseCloudConfig.AuthActor.MERCHANT,
                        scopeStoreId = msg.storeId,
                        scopeClientId = null
                    )
                    conn.setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")
                    conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

                    code = conn.responseCode
                    body = readBody(conn, code)
                }
            }

            Log.e(logTag, "[$tid] cloud insertMessage RESP code=$code body=${(body ?: "").take(1200)}")

            val ok = code in 200..299
            if (!ok) {
                Log.e(logTag, "[$tid] insertMessage FAILED code=$code body=$body url=$url")
                Log.e(logTag, "[$tid] cloud insertMessage FAILED code=$code")
            } else {
                Log.e(logTag, "[$tid] cloud insertMessage OK id=${msg.id} code=$code")
            }
            ok


        }
    suspend fun markUserMessagesRead(
        storeId: String,
        conversationId: String,
        traceId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val cfg = requireConfig()
        val tid = effectiveTraceId(traceId)
        if (cfg == null) {
            Log.e(logTag, "[$tid] SKIP: SUPABASE_URL / SUPABASE_ANON_KEY blank")
            return@withContext false
        }
        if (conversationId.isBlank()) {
            Log.e(logTag, "[$tid] SKIP blank conversationId")
            return@withContext false
        }

        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)

        val url = restUrl(
            "${ShowcaseCloudConfig.TABLE_CHAT_MESSAGES}" +
                    "?store_id=eq.${encode(currentStoreId)}" +
                    "&conversation_id=eq.${encode(conversationId)}" +
                    "&role=eq.client" +
                    "&is_read=eq.false"
        )

        val payload = JSONObject().apply {
            put("is_read", true)
        }.toString()

        Log.e(logTag, "[$tid] cloud markUserMessagesRead REQ url=$url body=$payload")

        var conn = openConn(
            url = url,
            method = "PATCH",
            actor = ShowcaseCloudConfig.AuthActor.MERCHANT,
            scopeStoreId = currentStoreId
        )
        conn.setRequestProperty("Prefer", "return=minimal")
        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

        var code = conn.responseCode
        var body = readBody(conn, code)

        if (isMerchantAuthExpired(code, body)) {
            Log.w(logTag, "[$tid] markUserMessagesRead token expired, try refresh once")
            val refreshed = refreshMerchantSessionIfNeeded()
            if (refreshed) {
                conn = openConn(
                    url = url,
                    method = "PATCH",
                    actor = ShowcaseCloudConfig.AuthActor.MERCHANT,
                    scopeStoreId = currentStoreId
                )
                conn.setRequestProperty("Prefer", "return=minimal")
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

                code = conn.responseCode
                body = readBody(conn, code)
            }
        }

        Log.e(logTag, "[$tid] cloud markUserMessagesRead RESP code=$code body=${(body ?: "").take(1200)}")

        val ok = code in 200..299
        if (!ok) Log.e(logTag, "[$tid] markUserMessagesRead FAILED code=$code body=$body url=$url")
        ok
    }
    // ✅ 用户侧：把“商家发来的未读”标记已读（用于商家侧看到已读回执）
    // ✅ 用户侧：把“商家发来的未读”标记已读（用于商家侧看到已读回执）
    suspend fun markMerchantMessagesRead(
        storeId: String,
        conversationId: String,
        clientId: String,
        traceId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val cfg = requireConfig()
        val tid = effectiveTraceId(traceId)
        if (cfg == null) {
            Log.e(logTag, "[$tid] SKIP: SUPABASE_URL / SUPABASE_ANON_KEY blank")
            return@withContext false
        }
        if (conversationId.isBlank()) {
            Log.e(logTag, "[$tid] SKIP blank conversationId")
            return@withContext false
        }

        // 只把“商家发来的未读”标记已读（用户视角）
        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)

        val url = restUrl(
            "${ShowcaseCloudConfig.TABLE_CHAT_MESSAGES}" +
                    "?store_id=eq.${encode(currentStoreId)}" +
                    "&conversation_id=eq.${encode(conversationId)}" +
                    "&role=eq.merchant" +
                    "&is_read=eq.false"
        )

        val payload = JSONObject().apply {
            put("is_read", true)
        }.toString()

        Log.e(logTag, "[$tid] cloud markMerchantMessagesRead REQ url=$url body=$payload")

        val conn = openConn(
            url = url,
            method = "PATCH",
            actor = ShowcaseCloudConfig.AuthActor.PUBLIC,
            scopeStoreId = currentStoreId,
            scopeClientId = clientId
        )
        conn.setRequestProperty("Prefer", "return=minimal")
        conn.outputStream.use { it.write(payload.toByteArray()) }

        val code = conn.responseCode
        val ok = code in 200..299

        val body = readAll(conn)
        Log.e(logTag, "[${tid}] cloud markMerchantMessagesRead RES code=$code ok=$ok body=$body url=$url")
        ok
    }

    // --------------------
// Storage upload (WeChat/WhatsApp style): upload -> send url/key
// --------------------
    suspend fun uploadChatImageToPublicUrl(
        context: Context,
        localUri: Uri,
        storeId: String,
        conversationId: String,
        msgId: String,
        clientId: String? = null,
        asMerchant: Boolean = false,
        index: Int = 0,
        traceId: String? = null
    ): String? = withContext(Dispatchers.IO) {
        val cfg = requireConfig()
        val tid = effectiveTraceId(traceId)
        if (cfg == null) {
            Log.e(logTag, "[$tid] uploadChatImage SKIP: config blank")
            return@withContext null
        }

        val (base, _) = cfg

        // 1) 读取 bytes
        val bytes = try {
            context.contentResolver.openInputStream(localUri)?.use { input ->
                val bos = ByteArrayOutputStream()
                val buf = ByteArray(16 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    bos.write(buf, 0, n)
                }
                bos.toByteArray()
            }
        } catch (t: Throwable) {
            Log.e(logTag, "[$tid] uploadChatImage read FAILED: ${t.message}")
            null
        }

        if (bytes == null || bytes.isEmpty()) {
            Log.e(logTag, "[$tid] uploadChatImage empty bytes uri=$localUri")
            return@withContext null
        }

        // 2) mime/后缀（尽量保守：默认 jpg）
        val mime = try { context.contentResolver.getType(localUri) } catch (_: Throwable) { null }
        val ext = when {
            mime?.contains("png", true) == true -> "png"
            mime?.contains("webp", true) == true -> "webp"
            else -> "jpg"
        }
        val contentType = mime ?: when (ext) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }

        // 3) 生成对象路径（key）
        val safeStore = storeId.replace(":", "_")
        val safeConv = conversationId.replace(":", "_")
        val objectPath = "chat/$safeStore/$safeConv/${msgId}_${index}.$ext"

        // 4) 上传到 Storage
        val uploadUrl = ShowcaseCloudConfig.storageObjectUrl(
            bucket = ShowcaseCloudConfig.BUCKET_CHAT_IMAGES,
            objectPath = objectPath
        )

        Log.e(logTag, "[$tid] uploadChatImage REQ url=$uploadUrl bytes=${bytes.size} ct=$contentType uri=$localUri")

        val actor = if (asMerchant) {
            ShowcaseCloudConfig.AuthActor.MERCHANT
        } else {
            ShowcaseCloudConfig.AuthActor.PUBLIC
        }
        val token = when (actor) {
            ShowcaseCloudConfig.AuthActor.PUBLIC -> {
                ShowcaseCloudConfig.authToken(actor).trim()
            }
            ShowcaseCloudConfig.AuthActor.MERCHANT -> {
                ShowcaseMerchantSessionManager.ensureValidMerchantAccessToken()
                    ?.trim()
                    .orEmpty()
                    .ifBlank {
                        ShowcaseStoreSession.currentMerchantAccessToken()?.trim().orEmpty()
                    }
            }
        }
        val conn = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("apikey", ShowcaseCloudConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            ShowcaseCloudConfig.requestScopeHeaders(
                storeId = storeId,
                clientId = if (asMerchant) null else clientId
            ).forEach { (k, v) ->
                setRequestProperty(k, v)
            }
            setRequestProperty("Content-Type", contentType)
            setRequestProperty("x-upsert", "true")
            setRequestProperty("Accept", "application/json")
        }

        try {
            conn.outputStream.use { it.write(bytes) }
        } catch (t: Throwable) {
            Log.e(logTag, "[$tid] uploadChatImage write FAILED: ${t.message}")
            return@withContext null
        }

        val code = conn.responseCode
        val body = readBody(conn, code)
        lastChatImageUploadCode = code
        lastChatImageUploadBody = body
        Log.e(logTag, "[$tid] uploadChatImage RESP code=$code body=${(body ?: "").take(600)}")

        if (code !in 200..299) return@withContext null

        // 5) public URL（因为 bucket 设为 Public）
        ShowcaseCloudConfig.storagePublicObjectUrl(
            bucket = ShowcaseCloudConfig.BUCKET_CHAT_IMAGES,
            objectPath = objectPath
        )
    }

}
