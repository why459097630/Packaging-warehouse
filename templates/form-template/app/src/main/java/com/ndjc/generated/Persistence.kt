package com.ndjc.generated

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.*
import java.nio.charset.StandardCharsets

object Persistence {
    private const val USER_FILE = "catalog_user.json"
    private val gson = Gson()

    private fun readAll(`in`: InputStream): String {
        BufferedReader(InputStreamReader(`in`, StandardCharsets.UTF_8)).use { br ->
            val sb = StringBuilder()
            var line: String?
            while (br.readLine().also { line = it } != null) sb.append(line).append('\n')
            return sb.toString()
        }
    }

    private fun readAsset(ctx: Context, path: String): String? = try {
        ctx.assets.open(path).use { readAll(it) }
    } catch (_: Exception) { null }

    private fun userFile(ctx: Context) = File(ctx.filesDir, USER_FILE)

    fun loadModels(ctx: Context): MutableList<Model> {
        return try {
            val f = userFile(ctx)
            if (!f.exists()) {
                var json = readAsset(ctx, "generated/catalog.json")
                if (json == null) json = readAsset(ctx, "generated/spec.json")
                if (json != null) FileOutputStream(f).use { it.write(json.toByteArray(StandardCharsets.UTF_8)) }
            }
            if (f.exists()) {
                FileInputStream(f).use { input ->
                    val json = readAll(input)
                    // 优先按 Catalog 解析
                    val cat = gson.fromJson(json, Catalog::class.java)
                    if (cat != null && cat.models != null) return cat.models.toMutableList()
                    // 兼容只存 models 数组的情况
                    val t = object : TypeToken<MutableList<Model>>() {}.type
                    return gson.fromJson(json, t)
                }
            }
            mutableListOf()
        } catch (_: Exception) { mutableListOf() }
    }

    fun saveModels(ctx: Context, list: MutableList<Model>) {
        try {
            val outCat = Catalog(title = "My Catalog", models = list)
            val bytes = gson.toJson(outCat).toByteArray(StandardCharsets.UTF_8)
            FileOutputStream(userFile(ctx)).use { it.write(bytes) }
        } catch (_: Exception) { }
    }
}
