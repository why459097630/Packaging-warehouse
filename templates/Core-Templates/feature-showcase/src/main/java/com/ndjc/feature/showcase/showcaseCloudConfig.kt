package com.ndjc.feature.showcase

/**
 * 云端配置（Supabase）
 *
 * 说明：
 * - 只在 feature-Showcase 内部使用
 * - 不动模板 / app / UI 包
 * - 后续 Repository 只从这里取 URL / Key / 表名 / bucket
 *
 * ⚠️ 注意：
 * 1. SUPABASE_URL 填“Project URL”（形如 https://xxxxxx.supabase.co）
 * 2. SUPABASE_ANON_KEY 填 API Keys 里 “Publishable key”（sb_publishable_xxx 开头）
 * 3. 如果不想把密钥提交到仓库，可以后面再改成 BuildConfig / 环境注入，这里先写死方便开发验证。
 */
object ShowcaseCloudConfig {

    enum class AuthActor {
        PUBLIC,
        MERCHANT
    }

    fun authToken(actor: AuthActor): String {
        return when (actor) {
            AuthActor.PUBLIC -> ""
            AuthActor.MERCHANT -> {
                ShowcaseStoreSession.requireMerchantAccessToken()
            }
        }
    }

    fun requestScopeHeaders(
        storeId: String = currentStoreId(),
        clientId: String? = null
    ): Map<String, String> {
        val result = linkedMapOf<String, String>()
        val sid = requireStoreId(storeId)
        result[Headers.NDJC_STORE_ID] = sid

        val cid = clientId?.trim().orEmpty()
        if (cid.isNotEmpty()) {
            result[Headers.NDJC_CLIENT_ID] = cid
        }
        return result
    }

    fun restUrl(path: String): String {
        val base = SUPABASE_URL.trimEnd('/')
        val clean = path.trimStart('/')
        return "$base/rest/v1/$clean"
    }

    fun functionUrl(path: String): String {
        val base = SUPABASE_URL.trimEnd('/')
        val clean = path.trimStart('/')
        return "$base/functions/v1/$clean"
    }

    fun authUrl(path: String): String {
        val base = SUPABASE_URL.trimEnd('/')
        val clean = path.trimStart('/')
        return "$base/auth/v1/$clean"
    }

    fun storageObjectUrl(bucket: String, objectPath: String): String {
        val base = SUPABASE_URL.trimEnd('/')
        val b = bucket.trim().trim('/')
        val p = objectPath.trim().trimStart('/')
        return "$base/storage/v1/object/$b/$p"
    }

    fun storagePublicObjectUrl(bucket: String, objectPath: String): String {
        val base = SUPABASE_URL.trimEnd('/')
        val b = bucket.trim().trim('/')
        val p = objectPath.trim().trimStart('/')
        return "$base/storage/v1/object/public/$b/$p"
    }

    fun currentStoreId(): String {
        return requireStoreId(ShowcaseStoreSession.requireStoreId())
    }

    fun requireStoreId(storeId: String?): String {
        val value = storeId?.trim().orEmpty()
        require(value.isNotEmpty()) { "storeId is required" }
        return value
    }

    /**
     * Supabase Project URL
     *
     * 示例（请替换为你自己的）：
     * "https://xxxxxx.supabase.co"
     */
    const val SUPABASE_URL: String = "https://lqnbjtnsobovpivuijci.supabase.co"

    /**
     * Supabase anon 公钥（Publishable key）
     *
     * 示例（请替换为你自己的）：
     * "sb_publishable_xxxxxxxxxxxxxxxxx"
     */
    const val SUPABASE_ANON_KEY: String = "sb_publishable_sk9Sy_nL5C38e0ZisMedlA_LUcT3QZb"

    /**
     * 正式版不再把商家 JWT 写死在代码里。
     * 商家登录成功后，运行时 access token 统一放在 ShowcaseStoreSession 中。
     */

    const val SUPABASE_SCHEMA: String = "public"

    const val TABLE_STORES: String = "stores"
    const val TABLE_STORE_PROFILE: String = "store_profiles"
    const val TABLE_CATEGORIES: String = "categories"
    const val TABLE_DISHES: String = "dishes"
    const val TABLE_DISH_IMAGES: String = "dish_images"
    const val TABLE_ANNOUNCEMENTS: String = "announcements"
    const val TABLE_CHAT_CONVERSATIONS: String = "chat_conversations"
    const val TABLE_CHAT_THREAD_META: String = "chat_thread_meta"
    const val TABLE_CHAT_MESSAGES: String = "chat_messages"
    const val TABLE_CHAT_RELAY: String = "chat_relay"
    const val TABLE_PUSH_DEVICES: String = "push_devices"
    const val TABLE_ASSETS_MANIFEST: String = "assets_manifest"
    const val TABLE_APP_PROJECTS: String = "app_projects"
    const val TABLE_APP_BUILDS: String = "app_builds"
    const val VIEW_CHAT_THREAD_SUMMARIES: String = "chat_thread_summaries"
    const val EDGE_PUSH_DISPATCH: String = "send_push"

    // ✅ Chat 云端总开关
    const val ENABLE_CHAT_CLOUD: Boolean = true
    const val ENABLE_CHAT_RELAY_ONLY: Boolean = false

    // ✅ Chat 图片 bucket（微信式：先上传 Storage，再发 URL/key）
    const val BUCKET_CHAT_IMAGES: String = "chat-images"

    /**
     * 后面 Repository 会用到的一些固定路径/头信息 key
     */
    object Headers {
        const val API_KEY = "apikey"
        const val AUTHORIZATION = "Authorization"
        const val PREFER_RETURN_REPRESENTATION = "Prefer"
        const val CONTENT_TYPE = "Content-Type"
        const val ACCEPT = "Accept"

        const val NDJC_STORE_ID = "x-ndjc-store-id"
        const val NDJC_CLIENT_ID = "x-ndjc-client-id"
    }

    /**
     * REST 常用查询参数（可选）
     */
    object Query {
        const val SELECT = "select"
        const val ORDER = "order"
        const val LIMIT = "limit"
        const val OFFSET = "offset"
    }
}
