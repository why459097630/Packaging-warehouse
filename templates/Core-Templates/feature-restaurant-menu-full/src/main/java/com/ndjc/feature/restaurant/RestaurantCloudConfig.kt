package com.ndjc.feature.restaurant

/**
 * 云端配置（Supabase）
 *
 * 说明：
 * - 只在 feature-restaurant-menu-full 内部使用
 * - 不动模板 / app / UI 包
 * - 后续 Repository 只从这里取 URL / Key / 表名
 *
 * ⚠️ 注意：
 * 1. SUPABASE_URL 填“Project URL”（形如 https://xxxxxx.supabase.co）
 * 2. SUPABASE_ANON_KEY 填 API Keys 里 “Publishable key”（sb_publishable_xxx 开头）
 * 3. 如果不想把密钥提交到仓库，可以后面再改成 BuildConfig / 环境注入，这里先写死方便开发验证。
 */
object RestaurantCloudConfig {

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
     * 使用的 schema，默认 public
     */
    const val SUPABASE_SCHEMA: String = "public"

    /**
     * 表名约定：分类表 & 菜品表
     * 已在 Supabase 里建好的那两个表名，保持和云端一致即可。
     */
    const val TABLE_CATEGORIES: String = "categories"
    const val TABLE_DISHES: String = "dishes"

    /**
     * 后面 Repository 会用到的一些固定路径/头信息 key
     */
    object Headers {
        const val API_KEY = "apikey"
        const val AUTHORIZATION = "Authorization" // 形如 "Bearer <anon key>"
        const val PREFER_RETURN_REPRESENTATION = "Prefer" // eg. "return=representation"
    }

    /**
     * 简单的工具方法，后面 Repository 可以少拼一点字符串
     */
    fun restUrl(path: String): String {
        // 最终类似：https://xxxxxx.supabase.co/rest/v1/dishes
        val base = SUPABASE_URL.trimEnd('/')
        val clean = path.trimStart('/')
        return "$base/rest/v1/$clean"
    }
}
