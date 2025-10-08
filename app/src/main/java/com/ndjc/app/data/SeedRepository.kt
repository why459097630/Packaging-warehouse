package com.ndjc.app.data

import org.json.JSONArray
import com.ndjc.app.data.model.Comment
import com.ndjc.app.data.model.Post

/**
 * 数据来源优先级：
 * 1) res/raw/seed_posts.json（新标准）
 * 2) res/raw/seed_data.json（兼容旧命名）
 * 若都不存在/解析失败则使用 fallback 内置示例。
 */
object SeedRepository {
    private var cached: List<Post>? = null

    fun posts(): List<Post> = cached ?: loadFromRawOrFallback().also { cached = it }

    fun postById(id: String): Post? = posts().find { it.id == id }

    private fun loadFromRawOrFallback(): List<Post> {
        return try {
            val app = AppCtx.app ?: return fallback()
            val res = app.resources
            val pkg = app.packageName

            // 先找 seed_posts（新标准），找不到再回退到 seed_data（兼容旧命名）
            val idPosts = res.getIdentifier("seed_posts", "raw", pkg)
            val idData = res.getIdentifier("seed_data", "raw", pkg)
            val rawId = when {
                idPosts != 0 -> idPosts
                idData != 0 -> idData
                else -> 0
            }

            if (rawId == 0) return fallback()

            val json = res.openRawResource(rawId).use { it.readBytes().toString(Charsets.UTF_8) }
            parse(json)
        } catch (_: Throwable) {
            fallback()
        }
    }

    private fun parse(json: String): List<Post> {
        val arr = JSONArray(json)
        val list = mutableListOf<Post>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue

            val id = o.optString("id", (i + 1).toString())
            val author = o.optString("author", "User")
            val content = o.optString("content", "")
            val likes = o.optInt("likes", 0)

            val comments = buildList {
                val ca = o.optJSONArray("comments") ?: JSONArray()
                for (j in 0 until ca.length()) {
                    val c = ca.optJSONObject(j) ?: continue
                    add(Comment(c.optString("author", "User"), c.optString("content", "")))
                }
            }

            list += Post(id, author, content, likes, comments)
        }
        return list
    }

    private fun fallback(): List<Post> = listOf(
        Post("1", "Alice", "Welcome to NDJC circle!", 3, listOf(Comment("Bob", "Nice!"))),
        Post("2", "Carol", "Compose + M3 ready.", 5, emptyList())
    )
}

/** Application 上下文（保持轻量无引库） */
object AppCtx {
    @Volatile internal var app: android.app.Application? = null
}
