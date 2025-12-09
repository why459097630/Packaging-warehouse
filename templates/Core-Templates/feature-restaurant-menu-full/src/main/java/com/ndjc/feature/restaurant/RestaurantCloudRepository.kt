package com.ndjc.feature.restaurant

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

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
                if (!imageUri.isNullOrBlank()) {
                    put("image_url", imageUri)
                } else {
                    put("image_url", JSONObject.NULL)
                }
            }

            // 3) upsert 逻辑：
            //    - 如果 id 像 uuid（长度 > 10），按 id=eq.<id> 做 PATCH
            //    - 否则视作新建：POST 插入一行
            val looksLikeUuid = id.length > 10

            val (code, responseBody) = if (looksLikeUuid) {
                val encodedId = URLEncoder.encode(id, "UTF-8")
                val url = buildUrl("/rest/v1/dishes?id=eq.$encodedId")
                Log.d("RestaurantCloud", "upsertDishFromDemo: PATCH $url body=$payload")
                httpPatch(url, payload)
            } else {
                val url = buildUrl("/rest/v1/dishes")
                Log.d("RestaurantCloud", "upsertDishFromDemo: POST $url body=$payload")
                httpPost(url, payload, prefer = "return=minimal")
            }

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
     * 按 id 删除 Supabase public.dishes 里的一条记录。
     *
     * - 仅当 id 为云端返回的 uuid 且 UI 已刷新后，删除才会命中那条记录。
     * - 如果是本地临时的数字 id（"1" / "2" 等），云端压根没有这条记录，DELETE 只是不起作用。
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
