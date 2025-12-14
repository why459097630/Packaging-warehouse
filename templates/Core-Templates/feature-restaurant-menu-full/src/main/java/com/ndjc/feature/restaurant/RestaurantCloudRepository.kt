package com.ndjc.feature.restaurant

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.UUID


/**
 * Supabase 云端访问仓库
 *
 * 依赖：
 *  - RestaurantCloudConfig.SUPABASE_URL
 *  - RestaurantCloudConfig.SUPABASE_ANON_KEY
 *
 * 表结构（示例）：
 *
 * 表：categories
 *   - id: uuid (pk)
 *   - name: text (unique)
 *
 * 表：dishes
 *   - id: uuid (pk)
 *   - name_en: text
 *   - name_zh: text
 *   - description_en: text?
 *   - description_zh: text?
 *   - category_id: uuid? (fk -> categories.id)
 *   - price: int4
 *   - discount_price: int4?
 *   - recommended: bool
 *   - sold_out: bool
 *   - image_url: text?
 */
class RestaurantCloudRepository {

    data class CloudCategory(
        val id: String,
        val name: String,
    )

    data class CloudDish(
        val id: String?,
        val nameEn: String,
        val nameZh: String,
        val descriptionEn: String?,
        val descriptionZh: String?,
        val categoryId: String?,
        val price: Int,
        val discountPrice: Int?,
        val recommended: Boolean,
        val soldOut: Boolean,
        val imageUrl: String?,
    )

    /*
     * -------------------- 读取：从 Supabase 拉取分类 & 菜品 --------------------
     */

    suspend fun fetchCategories(): List<CloudCategory> = withContext(Dispatchers.IO) {
        val apiKey = RestaurantCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) {
            Log.w("RestaurantCloud", "fetchCategories: SUPABASE_ANON_KEY is blank, skip")
            return@withContext emptyList()
        }

        val url = buildUrl("/rest/v1/categories?select=id,name&order=name.asc")
        Log.d("RestaurantCloud", "fetchCategories: GET $url")

        val (code, body) = httpGet(url)
        if (code !in 200..299 || body.isNullOrBlank()) {
            Log.w("RestaurantCloud", "fetchCategories: bad response code=$code body=$body")
            return@withContext emptyList()
        }

        val arr = JSONArray(body)
        val result = mutableListOf<CloudCategory>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = obj.optString("id", "")
            val name = obj.optString("name", "")
            if (id.isNotBlank() && name.isNotBlank()) {
                result.add(CloudCategory(id = id, name = name))
            }
        }
        result
    }

    suspend fun fetchDishes(): List<CloudDish> = withContext(Dispatchers.IO) {
        val apiKey = RestaurantCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) {
            Log.w("RestaurantCloud", "fetchDishes: SUPABASE_ANON_KEY is blank, skip")
            return@withContext emptyList()
        }

        val url = buildUrl(
            "/rest/v1/dishes" +
                "?select=id,name_en,name_zh,description_en,description_zh," +
                "category_id,price,discount_price,recommended,sold_out,image_url" +
                "&order=name_zh.asc"
        )
        Log.d("RestaurantCloud", "fetchDishes: GET $url")

        val (code, body) = httpGet(url)
        if (code !in 200..299 || body.isNullOrBlank()) {
            Log.w("RestaurantCloud", "fetchDishes: bad response code=$code body=$body")
            return@withContext emptyList()
        }

        val arr = JSONArray(body)
        val result = mutableListOf<CloudDish>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = obj.optString("id", null)
            val nameEn = obj.optString("name_en", "")
            val nameZh = obj.optString("name_zh", "")
            if (nameEn.isBlank() && nameZh.isBlank()) continue

            val descriptionEn = obj.optString("description_en", null)
            val descriptionZh = obj.optString("description_zh", null)
            val categoryId = obj.optString("category_id", null)
            val price = obj.optInt("price", 0)
            val discountPrice = if (!obj.isNull("discount_price")) obj.optInt("discount_price") else null
            val recommended = obj.optBoolean("recommended", false)
            val soldOut = obj.optBoolean("sold_out", false)
            val imageUrl = obj.optString("image_url", null)

            result.add(
                CloudDish(
                    id = id,
                    nameEn = nameEn,
                    nameZh = nameZh,
                    descriptionEn = descriptionEn,
                    descriptionZh = descriptionZh,
                    categoryId = categoryId,
                    price = price,
                    discountPrice = discountPrice,
                    recommended = recommended,
                    soldOut = soldOut,
                    imageUrl = imageUrl,
                )
            )
        }
        result
    }

    /*
     * -------------------- 写入：真正连 Supabase 的写回 --------------------
     */

    /**
     * 将本地 DemoDish 写回 Supabase 的 public.dishes 表。
     *
     * - 如果 id 是云端返回的 uuid（长度较长），则按 id 做 upsert（更新该行）
     * - 如果 id 是本地数字/"1"/"2" 等短 id，则视为新菜，插入新行（id 由 Supabase 自动生成）
     * - category:
     *    - 若 category 为空 → category_id = null
     *    - 若有分类名 → 在 categories 表中查找，不存在则自动新建后使用
     */
    // === Storage 上传（最小可用）===
// 你需要在 Supabase 创建 bucket，例如：dish-images，并设置为 public（否则 anon key 没法拿公开URL）
    private val DISH_IMAGES_BUCKET = "dish-images"

    suspend fun uploadDishImageBytes(
        bytes: ByteArray,
        fileExt: String = "jpg",
        contentType: String = "image/jpeg"
    ): String? = withContext(Dispatchers.IO) {
        val apiKey = RestaurantCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) return@withContext null

        try {
            // 用时间戳生成文件名，避免覆盖
            val objectPath = "dishes/${UUID.randomUUID()}.$fileExt"
            val url = buildUrl("/storage/v1/object/$DISH_IMAGES_BUCKET/$objectPath")

            val (code, body) = httpPutBytes(
                urlString = url,
                bytes = bytes,
                contentType = contentType
            )

            if (code !in 200..299) {
                Log.w("RestaurantCloud", "uploadDishImageBytes failed code=$code body=$body")
                return@withContext null
            }

            // bucket public 的情况下，public URL 这样拼
            val publicUrl = buildUrl("/storage/v1/object/public/$DISH_IMAGES_BUCKET/$objectPath")
            publicUrl
        } catch (e: Exception) {
            Log.e("RestaurantCloud", "uploadDishImageBytes failed", e)
            null
        }
    }

    suspend fun upsertDishFromDemo(
        id: String,
        nameZh: String,
        nameEn: String,
        descriptionEn: String,
        category: String,
        originalPrice: Int,
        discountPrice: Int?,
        isRecommended: Boolean,
        isSoldOut: Boolean,
        imageUri: String?
    ): Boolean = withContext(Dispatchers.IO) {
        val apiKey = RestaurantCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) {
            Log.w("RestaurantCloud", "upsertDishFromDemo: SUPABASE_ANON_KEY is blank, skip")
            return@withContext false
        }

        try {
            // 1) 解析 / 创建 category_id
            val categoryId = if (category.isBlank()) {
                null
            } else {
                resolveOrCreateCategoryId(category)
            }

            // 2) 准备 payload
            val payload = JSONObject().apply {
                put("name_zh", nameZh)
                put("name_en", nameEn)
                put("description_en", descriptionEn)
                put("category_id", categoryId)
                put("price", originalPrice)
                if (discountPrice != null) {
                    put("discount_price", discountPrice)
                } else {
                    put("discount_price", JSONObject.NULL)
                }
                put("recommended", isRecommended)
                put("sold_out", isSoldOut)

// 方案 A：image_url 只保存云端可访问的 URL（http/https）；不允许把 content:// 或 file:// 写入云端
                val imageUrl = imageUri?.trim()
                val isHttpUrl = !imageUrl.isNullOrBlank() &&
                        (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))

                if (isHttpUrl) {
                    put("image_url", imageUrl)
                } else {
                    if (!imageUrl.isNullOrBlank()) {
                        Log.w("RestaurantCloud", "upsertDishFromDemo: ignore non-http image url: $imageUrl")
                    }
                    put("image_url", JSONObject.NULL)
                }
            }

// 3) upsert 逻辑（方案 A：本地 UUID 是事实主键；任何写入都必须带 id，并且幂等 UPSERT）
//    - 不允许走“让数据库生成 id”的分支，否则本地/云端会断链。
            val trimmedId = id.trim()
            require(trimmedId.isNotEmpty()) { "Scheme A requires local UUID id, but id is blank" }

            payload.put("id", trimmedId)

            val url = buildUrl("/rest/v1/dishes?on_conflict=id")
            Log.d("RestaurantCloud", "upsertDishFromDemo: UPSERT(POST) $url body=$payload")
            val (code, responseBody) = httpPost(
                url,
                payload,
                prefer = "resolution=merge-duplicates,return=minimal"
            )



            Log.d(
                "RestaurantCloud",
                "upsertDishFromDemo: response code=$code body=$responseBody"
            )

            code in 200..299
        } catch (e: Exception) {
            Log.e("RestaurantCloud", "upsertDishFromDemo: failed", e)
            false
        }
    }

    /**
     * 按 UUID 删除 Supabase public.dishes 的一条记录（方案 A：本地 UUID 与云端主键一致）。
     *
     * - 本地与云端使用同一主键：id（UUID）
     * - DELETE 是幂等的：云端不存在该 id 时，也视为删除成功（不会影响后续流程）
     */
    suspend fun deleteDishById(id: String): Boolean = withContext(Dispatchers.IO) {
        val apiKey = RestaurantCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) {
            Log.w("RestaurantCloud", "deleteDishById: SUPABASE_ANON_KEY is blank, skip")
            return@withContext false
        }

        val trimmed = id.trim()
        if (trimmed.isEmpty()) {
            Log.w("RestaurantCloud", "deleteDishById: id is blank, skip")
            return@withContext false
        }

        try {
            val encodedId = URLEncoder.encode(trimmed, "UTF-8")
            val url = buildUrl("/rest/v1/dishes?id=eq.$encodedId")
            Log.d("RestaurantCloud", "deleteDishById: DELETE $url")

            val (code, responseBody) = httpDelete(url)
            Log.d("RestaurantCloud", "deleteDishById: response code=$code body=$responseBody")

            code in 200..299
        } catch (e: Exception) {
            Log.e("RestaurantCloud", "deleteDishById: failed", e)
            false
        }
    }
    /**
     * 根据分类名获取 categories.id（uuid）。
     * - 不存在：返回 null
     */
    suspend fun getCategoryIdByName(name: String): String? = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@withContext null
        return@withContext try {
            fetchCategories().firstOrNull { it.name == trimmed }?.id
        } catch (e: Exception) {
            Log.e("RestaurantCloud", "getCategoryIdByName: failed", e)
            null
        }
    }

    /**
     * 检查是否存在任何菜品引用了某个分类 id（dishes.category_id）。
     * 说明：
     * - 这里用 fetchDishes() 拉全量后本地统计，数据量不大时最稳。
     * - 若你后续需要规模化，可改成服务端 count 查询或 RPC。
     */
    suspend fun hasAnyDishReferencingCategoryId(categoryId: String): Boolean = withContext(Dispatchers.IO) {
        val id = categoryId.trim()
        if (id.isEmpty()) return@withContext false
        return@withContext try {
            fetchDishes().any { it.categoryId == id }
        } catch (e: Exception) {
            Log.e("RestaurantCloud", "hasAnyDishReferencingCategoryId: failed", e)
            // 保守起见：出错时当作“有引用”，避免误删
            true
        }
    }


    /**
     * 删除指定名称的分类（如果存在）。
     *
     * - 若找不到该名称的分类：返回 false
     * - 若删除成功：返回 true
     */
    suspend fun deleteCategoryByName(name: String): Boolean = withContext(Dispatchers.IO) {
        val apiKey = RestaurantCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) {
            Log.w("RestaurantCloud", "deleteCategoryByName: SUPABASE_ANON_KEY is blank, skip")
            return@withContext false
        }

        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            Log.w("RestaurantCloud", "deleteCategoryByName: name is blank, skip")
            return@withContext false
        }

        try {
            // 先把所有分类拉一遍，找到这个名字对应的 id
            val categories = fetchCategories()
            val target = categories.find { it.name == trimmed }
            if (target == null) {
                Log.d("RestaurantCloud", "deleteCategoryByName: no category named $trimmed, skip")
                return@withContext false
            }

            val encodedId = URLEncoder.encode(target.id, "UTF-8")
            val url = buildUrl("/rest/v1/categories?id=eq.$encodedId")
            Log.d("RestaurantCloud", "deleteCategoryByName: DELETE $url")

            val (code, responseBody) = httpDelete(url)
            Log.d(
                "RestaurantCloud",
                "deleteCategoryByName: response code=$code body=$responseBody"
            )

            code in 200..299
        } catch (e: Exception) {
            Log.e("RestaurantCloud", "deleteCategoryByName: failed", e)
            false
        }
    }
    suspend fun ensureCategoryExists(name: String): Boolean = withContext(Dispatchers.IO) {
        val id = resolveOrCreateCategoryId(name)
        id != null && id.isNotBlank()
    }

    /**
     * 根据分类名称去 categories 表查找 id，不存在则自动创建该分类并返回新 id。
     * 用于把 DemoDish.category 映射到 Supabase 的 category_id。
     */
    private suspend fun resolveOrCreateCategoryId(name: String): String? =
        withContext(Dispatchers.IO) {
            val apiKey = RestaurantCloudConfig.SUPABASE_ANON_KEY
            if (apiKey.isBlank()) {
                Log.w("RestaurantCloud", "resolveOrCreateCategoryId: SUPABASE_ANON_KEY is blank, skip")
                return@withContext null
            }

            val trimmed = name.trim()
            if (trimmed.isEmpty()) {
                Log.w("RestaurantCloud", "resolveOrCreateCategoryId: name is blank")
                return@withContext null
            }

            try {
                // 1) 先查是否存在
                val encodedName = URLEncoder.encode(trimmed, "UTF-8")
                val urlSelect = buildUrl("/rest/v1/categories?name=eq.$encodedName&select=id&limit=1")
                Log.d("RestaurantCloud", "resolveOrCreateCategoryId: GET $urlSelect")

                val (codeSelect, bodySelect) = httpGet(urlSelect)
                if (codeSelect in 200..299 && !bodySelect.isNullOrBlank()) {
                    val arr = JSONArray(bodySelect)
                    if (arr.length() > 0) {
                        val first = arr.getJSONObject(0)
                        val existingId = first.optString("id", null)
                        if (!existingId.isNullOrBlank()) {
                            Log.d(
                                "RestaurantCloud",
                                "resolveOrCreateCategoryId: found existing id=$existingId for name=$trimmed"
                            )
                            return@withContext existingId
                        }
                    }
                }

                // 2) 不存在，则创建
                val urlInsert = buildUrl("/rest/v1/categories")
                val payload = JSONObject().apply {
                    put("name", trimmed)
                }
                Log.d("RestaurantCloud", "resolveOrCreateCategoryId: POST $urlInsert body=$payload")

                val (codeInsert, bodyInsert) = httpPost(
                    urlInsert,
                    payload,
                    prefer = "return=representation"
                )
                if (codeInsert !in 200..299 || bodyInsert.isNullOrBlank()) {
                    Log.w(
                        "RestaurantCloud",
                        "resolveOrCreateCategoryId: insert failed code=$codeInsert body=$bodyInsert"
                    )
                    return@withContext null
                }

                val arr2 = JSONArray(bodyInsert)
                if (arr2.length() == 0) {
                    Log.w("RestaurantCloud", "resolveOrCreateCategoryId: insert returned empty array")
                    return@withContext null
                }
                val obj = arr2.getJSONObject(0)
                val newId = obj.optString("id", null)
                if (newId.isNullOrBlank()) {
                    Log.w("RestaurantCloud", "resolveOrCreateCategoryId: insert returned no id")
                    return@withContext null
                }

                Log.d(
                    "RestaurantCloud",
                    "resolveOrCreateCategoryId: created new category id=$newId name=$trimmed"
                )
                newId
            } catch (e: Exception) {
                Log.e("RestaurantCloud", "resolveOrCreateCategoryId: failed", e)
                null
            }
        }

    /* -------------------- HTTP 辅助方法 -------------------- */

    private fun buildUrl(path: String): String {
        val base = RestaurantCloudConfig.SUPABASE_URL.trimEnd('/')
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return base + normalizedPath
    }

    private fun httpGet(urlString: String): Pair<Int, String?> {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("apikey", RestaurantCloudConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${RestaurantCloudConfig.SUPABASE_ANON_KEY}")
                setRequestProperty("Accept", "application/json")
            }

            val code = conn.responseCode
            val responseBody = try {
                (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()
                    ?.use { it.readText() }
            } catch (_: Exception) {
                null
            }

            code to responseBody
        } catch (e: Exception) {
            Log.e("RestaurantCloud", "httpGet: request failed for $urlString", e)
            0 to null
        } finally {
            conn?.disconnect()
        }
    }

    private fun httpPost(
        urlString: String,
        body: JSONObject,
        prefer: String? = null
    ): Pair<Int, String?> {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("apikey", RestaurantCloudConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${RestaurantCloudConfig.SUPABASE_ANON_KEY}")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json")
                if (!prefer.isNullOrBlank()) {
                    setRequestProperty("Prefer", prefer)
                }
            }

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            val responseBody = try {
                (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()
                    ?.use { it.readText() }
            } catch (_: Exception) {
                null
            }

            code to responseBody
        } catch (e: Exception) {
            Log.e("RestaurantCloud", "httpPost: request failed for $urlString", e)
            0 to null
        } finally {
            conn?.disconnect()
        }
    }

    // NOTE: Scheme A 禁止用 PATCH 做新增/更新 dishes；统一使用 UPSERT(POST on_conflict=id) 保证幂等落库。
// 该方法保留仅用于历史代码/特定场景，业务写入请勿调用。
    private fun httpPatch(
        urlString: String,
        body: JSONObject,
    ): Pair<Int, String?> {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("apikey", RestaurantCloudConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${RestaurantCloudConfig.SUPABASE_ANON_KEY}")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=minimal")
            }

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            val responseBody = try {
                (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()
                    ?.use { it.readText() }
            } catch (_: Exception) {
                null
            }

            code to responseBody
        } catch (e: Exception) {
            Log.e("RestaurantCloud", "httpPatch: request failed for $urlString", e)
            0 to null
        } finally {
            conn?.disconnect()
        }
    }

    private fun httpDelete(urlString: String): Pair<Int, String?> {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("apikey", RestaurantCloudConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${RestaurantCloudConfig.SUPABASE_ANON_KEY}")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json")
            }

            val code = conn.responseCode
            val responseBody = try {
                (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()
                    ?.use { it.readText() }
            } catch (_: Exception) {
                null
            }

            code to responseBody
        } catch (e: Exception) {
            Log.e("RestaurantCloud", "httpDelete: request failed for $urlString", e)
            0 to null
        } finally {
            conn?.disconnect()
        }
    }
}
private fun httpPutBytes(
    urlString: String,
    bytes: ByteArray,
    contentType: String
): Pair<Int, String?> {
    var conn: HttpURLConnection? = null
    return try {
        val url = URL(urlString)
        conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("apikey", RestaurantCloudConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${RestaurantCloudConfig.SUPABASE_ANON_KEY}")
            setRequestProperty("Content-Type", contentType)
            setRequestProperty("x-upsert", "true") // 同路径时允许覆盖（可选）
        }

        conn.outputStream.use { os ->
            os.write(bytes)
            os.flush()
        }

        val code = conn.responseCode
        val responseBody = try {
            (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
        } catch (_: Exception) {
            null
        }
        code to responseBody
    } catch (e: Exception) {
        Log.e("RestaurantCloud", "httpPutBytes failed for $urlString", e)
        0 to null
    } finally {
        conn?.disconnect()
    }
}
