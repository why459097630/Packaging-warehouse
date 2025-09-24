package com.ndjc.app.data

import org.json.JSONArray
import java.nio.charset.Charset
import com.ndjc.app.data.model.Comment
import com.ndjc.app.data.model.Post

object SeedRepository {
    private var cached: List<Post>? = null

    fun posts(): List<Post> = cached ?: loadFromRawOrFallback().also { cached = it }
    fun postById(id: String): Post? = posts().find { it.id == id }

    private fun loadFromRawOrFallback(): List<Post> = try {
        val resId = com.ndjc.app.R.raw.seed_data           // RES: raw/seed_data.json
        val text = AppCtx.app.resources
            .openRawResource(resId)
            .readBytes()
            .toString(Charset.forName("UTF-8"))
        parse(text)
    } catch (_: Throwable) { fallback() }

    private fun parse(json: String): List<Post> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val id = o.optString("id", i.toString())
            val author = o.optString("author", "User")
            val content = o.optString("content", "")
            val likes = o.optInt("likes", 0)
            val comments = buildList {
                val ca = o.optJSONArray("comments") ?: JSONArray()
                for (j in 0 until ca.length()) {
                    val c = ca.getJSONObject(j)
                    add(Comment(c.optString("author","User"), c.optString("content","")))
                }
            }
            Post(id, author, content, likes, comments)
        }
    }

    private fun fallback(): List<Post> = listOf(
        Post("1", "Alice", "Welcome to NDJC circle!", 3, listOf(Comment("Bob","Nice!"))),
        Post("2", "Carol", "Compose + M3 ready.", 5)
    )
}

/** Application 上下文（供读取 raw） */
object AppCtx { lateinit var app: android.app.Application }

/** 简单 Application 注入到 AppCtx（需在 Manifest 的 application 指定 android:name=".data.App"） */
class App : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        AppCtx.app = this
    }
}
