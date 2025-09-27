package com.ndjc.app.data

import org.json.JSONArray
import com.ndjc.app.data.model.Comment
import com.ndjc.app.data.model.Post

/**
 * 运行时优先读取 res/raw/seed_posts.json；
 * 若不存在则回退到 res/raw/seed_data.json；
 * 都不存在/解析失败则使用 fallback 内置示例。
 */
object SeedRepository {
    private var cached: List<Post>? = null

    fun posts(): List<Post> = cached ?: loadFromRawOrFallback().also { cached = it }

    fun postById(id: String): Post? = posts().find { it.id == id }

    // ✅ 改为“块函数体”，允许在 try 中使用 return
    private fun loadFromRawOrFallback(): List<Post> {
        return try {
            val app = AcpCtx.app ?: return fallback()
            val res = app.resources
            val pkg = app.packageName

            // 先找 seed_posts（新标准），找不到再回退 seed_data（兼容旧命名）
            val idPosts = res.getIdentifier("seed_posts", "raw", pkg)
            val idData  = res.getIdentifier("seed_data",  "raw", pkg)
            val rawId   = when {
                idPosts != 0 -> idPosts
                idData  != 0 -> idData
                else         -> 0
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

            val id       = o.optString("id", (i + 1).toString())
            val author   = o.optString("author", "User")
            val content  = o.optString("content", "")
            val likes    = o.optInt("likes", 0)

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

/** Application 上下文（保持你现有写法） */
object AcpCtx { @Volatile internal var app: android.app.Application? = null }

/** 在 Application 注入（Manifest 里把 application 声明成 android:name=".data.App"） */
class App : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        AcpCtx.app = this
    }
}
