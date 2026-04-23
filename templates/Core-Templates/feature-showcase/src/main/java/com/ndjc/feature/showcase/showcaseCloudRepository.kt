package com.ndjc.feature.showcase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID


/**
 * Supabase 云端访问仓库
 *
 * 依赖：
 *  - ShowcaseCloudConfig.SUPABASE_URL
 *  - ShowcaseCloudConfig.SUPABASE_ANON_KEY
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
 *
 * （展示型必备扩展，可选启用）
 * 表：store_profile
 */
class ShowcaseCloudRepository {

    data class CloudCategory(
        val id: String,
        val name: String,
        // 可选：用于排序（若云端有 sort_order 字段）
        val sortOrder: Int? = null,
    )

    data class CloudDish(
        val id: String?,
        val nameEn: String,
        val nameZh: String,
        val descriptionEn: String?,
        val descriptionZh: String?,
        val categoryId: String?,
        val price: Double,
        val discountPrice: Double?,
        val recommended: Boolean,
        val soldOut: Boolean,
        val hidden: Boolean = false,
        val imageUrl: String?,
        val imageUrls: List<String> = emptyList(),
        val tags: List<String> = emptyList(),
        val externalLink: String? = null,
        val updatedAt: Long? = null,
        val clickCount: Int = 0,
    )

    /**
     * 展示型必备扩展：店铺/项目简介信息
     * - 只定义 Cloud DTO；真正映射到 StoreProfile 在 VM/Mapper 做也可以
     */
    data class CloudStoreProfile(
        val storeId: String,
        val title: String = "",
        val subtitle: String = "",
        val description: String = "",
        val address: String = "",
        val hours: String = "",
        val mapUrl: String = "",
        val extraContactsJson: String = "",
        val servicesJson: String = "",
        val coverUrl: String = "",
        val logoUrl: String = "",
        val businessStatus: String = "",
        val updatedAt: Long? = null,
    )



    data class CloudAnnouncement(
        val id: String,
        val storeId: String,
        val coverUrl: String?,
        val body: String,
        val status: String,
        val updatedAt: Long?,
        val createdAt: Long?,
        val viewCount: Int
    )
    data class MerchantAuthSession(
        val accessToken: String,
        val refreshToken: String?,
        val authUserId: String,
        val loginName: String,
        val expiresAt: Long
    )

    data class MerchantStoreMembership(
        val storeId: String,
        val authUserId: String,
        val loginName: String?,
    )
    data class CloudStoreServiceStatus(
        val storeId: String,
        val planType: String,
        val serviceStatus: String,
        val serviceEndAt: String?,
        val deleteAt: String?,
        val isWriteAllowed: Boolean
    )
    data class CategoryWriteResult(
        val ok: Boolean,
        val errorMessage: String? = null,
        val errorCode: Int = 0,
        val errorBody: String? = null
    )

    data class PushDeviceUpsert(
        val storeId: String,
        val audience: String,
        val token: String,
        val conversationId: String? = null,
        val clientId: String? = null,
        val platform: String = "android",
        val appVersion: String? = null
    )

    private companion object {
        // 已有
        private const val DISH_IMAGES_BUCKET = "dish-images"

        // ✅ 新增：店铺图片 bucket
        private const val STORE_IMAGES_BUCKET = "store-images"


    }

    private fun buildCategoryWriteErrorMessage(
        actionLabel: String,
        code: Int,
        body: String?
    ): String {
        val text = body?.trim().orEmpty()
        if (code == 0) {
            return "$actionLabel failed. Merchant session missing or network error."
        }
        if (code == 401) {
            return "$actionLabel failed. Merchant session expired. Please sign in again."
        }
        if (code == 403) {
            return "$actionLabel failed. Permission denied for current store."
        }
        if (text.contains("row-level security", ignoreCase = true) ||
            text.contains("permission denied", ignoreCase = true) ||
            text.contains("violates row-level security policy", ignoreCase = true)
        ) {
            return "$actionLabel failed. Store permission check was rejected by cloud policy."
        }
        if (text.contains("JWT", ignoreCase = true) && text.contains("expired", ignoreCase = true)) {
            return "$actionLabel failed. Merchant session expired. Please sign in again."
        }
        return "$actionLabel failed. Cloud code=$code."
    }

    private fun dishesTable(): String {
        // 兼容：若 Config 提供常量就用，否则退回旧表名
        return try {
            ShowcaseCloudConfig.TABLE_DISHES
        } catch (_: Throwable) {
            "dishes"
        }
    }
    private fun dishImagesTable(): String {
        return try {
            ShowcaseCloudConfig.TABLE_DISH_IMAGES
        } catch (_: Throwable) {
            "dish_images"
        }
    }
    private fun categoriesTable(): String {
        return try {
            ShowcaseCloudConfig.TABLE_CATEGORIES
        } catch (_: Throwable) {
            "categories"
        }
    }
    private fun storeProfileTable(): String {
        val t = runCatching { ShowcaseCloudConfig.TABLE_STORE_PROFILE }.getOrNull()?.trim().orEmpty()
        // ✅ 强校验：只允许正确表名，否则直接回退到 store_profiles
        return if (t.equals("store_profiles", ignoreCase = true)) "store_profiles" else "store_profiles"
    }



    private fun pushDevicesTable(): String {
        return try {
            ShowcaseCloudConfig.TABLE_PUSH_DEVICES
        } catch (_: Throwable) {
            "push_devices"
        }
    }

    private fun announcementsTable(): String {
        return try {
            ShowcaseCloudConfig.TABLE_ANNOUNCEMENTS
        } catch (_: Throwable) {
            "announcements"
        }
    }

    private fun storesTable(): String {
        return try {
            ShowcaseCloudConfig.TABLE_STORES
        } catch (_: Throwable) {
            "stores"
        }
    }

    private fun dishImagesBucket(): String {
        // ✅ 直接用本仓库常量，避免 ShowcaseCloudConfig 成员解析失败导致编译卡死
        return DISH_IMAGES_BUCKET
    }
    private fun storeImagesBucket(): String {
        return STORE_IMAGES_BUCKET
    }


    /*
     * -------------------- 读取：从 Supabase 拉取分类 & 菜品 --------------------
     */

    suspend fun fetchCategories(storeId: String): List<CloudCategory> = withContext(Dispatchers.IO) {
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) {
            Log.w("ShowcaseCloud", "fetchCategories: SUPABASE_ANON_KEY is blank, skip")
            return@withContext emptyList()
        }

        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
        val table = categoriesTable()
        val encodedStoreId = URLEncoder.encode("eq.$currentStoreId", "UTF-8")

        val url = ShowcaseCloudConfig.restUrl(
            "$table?select=id,store_id,name_zh,name_en,sort_order&store_id=$encodedStoreId&order=sort_order.asc"
        )

        Log.d("ShowcaseCloud", "fetchCategories: GET $url")

        val (code, body) = httpGet(url)

        if (code !in 200..299 || body.isNullOrBlank()) {
            Log.w("ShowcaseCloud", "fetchCategories: bad response code=$code body=$body")
            return@withContext emptyList()
        }

        val arr = JSONArray(body)
        val result = mutableListOf<CloudCategory>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = obj.optString("id", "")
            val nameZh = obj.optString("name_zh", "")
            val nameEn = obj.optString("name_en", "")
            val resolvedName = nameZh.ifBlank { nameEn }
            val sortOrder = if (obj.isNull("sort_order")) null else obj.optInt("sort_order")
            if (id.isNotBlank() && resolvedName.isNotBlank()) {
                result.add(
                    CloudCategory(
                        id = id,
                        name = resolvedName,
                        sortOrder = sortOrder
                    )
                )
            }
        }
        result
    }

    /**
     * 兼容旧行为：一次性全量拉取 dishes
     */
    suspend fun fetchDishes(storeId: String): List<CloudDish> = withContext(Dispatchers.IO) {
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) {
            Log.w("ShowcaseCloud", "fetchDishes: SUPABASE_ANON_KEY is blank, skip")
            return@withContext emptyList()
        }

        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
        val encodedStoreId = URLEncoder.encode("eq.$currentStoreId", "UTF-8")
        val table = dishesTable()
        val url = ShowcaseCloudConfig.restUrl(
            "$table" +
                    "?select=id,store_id,name_en,name_zh,description_en,description_zh," +
                    "category_id,price,discount_price,recommended,sold_out,hidden,image_url," +
                    "tags,external_link,updated_at,click_count" +
                    "&store_id=$encodedStoreId" +
                    "&order=name_zh.asc"
        )
        Log.d("ShowcaseCloud", "fetchDishes: GET $url")

        val (code, body) = httpGet(url)
        if (code !in 200..299 || body.isNullOrBlank()) {
            Log.w("ShowcaseCloud", "fetchDishes: bad response code=$code body=$body")
            return@withContext emptyList()
        }

        enrichDishesWithImages(
            storeId = currentStoreId,
            dishes = parseDishesArray(body)
        )
    }

    /**
     * 展示型必备：分页/增量加载（避免全量拉取）
     *
     * - offset/limit：Supabase PostgREST 支持 limit/offset
     * - order: 例 "updated_at.desc" / "name_zh.asc" / "price.desc"
     */
    suspend fun fetchDishesPaged(
        storeId: String,
        limit: Int = 50,
        offset: Int = 0,
        order: String = "name_zh.asc",
    ): List<CloudDish> = withContext(Dispatchers.IO) {
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) return@withContext emptyList()

        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
        val l = limit.coerceIn(1, 500)
        val o = offset.coerceAtLeast(0)
        val encodedStoreId = URLEncoder.encode("eq.$currentStoreId", "UTF-8")

        val table = dishesTable()
        val url = ShowcaseCloudConfig.restUrl(
            "$table" +
                    "?select=id,store_id,name_en,name_zh,description_en,description_zh," +
                    "category_id,price,discount_price,recommended,sold_out,hidden,image_url," +
                    "tags,external_link,updated_at,click_count" +
                    "&store_id=$encodedStoreId" +
                    "&order=${urlEncode(order)}" +
                    "&limit=$l&offset=$o"
        )
        Log.d("ShowcaseCloud", "fetchDishesPaged: GET $url")

        val (code, body) = httpGet(url)
        if (code !in 200..299 || body.isNullOrBlank()) return@withContext emptyList()
        enrichDishesWithImages(currentStoreId, parseDishesArray(body))
    }

    /**
     * 展示型必备：服务端搜索（可选）
     * - keyword 会在 name_zh/name_en 做 ilike 查询
     * - categoryId 可选
     *
     * 注意：PostgREST OR 语法需要 URL encode，这里做了最小实现。
     */
    // ==========================
// dish_images（方案 B：多图）
// ==========================

    private suspend fun fetchDishImagesByDishIds(
        storeId: String,
        dishIds: List<String>
    ): Map<String, List<String>> =
        withContext(Dispatchers.IO) {
            val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
            if (apiKey.isBlank()) return@withContext emptyMap()

            val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
            val ids = dishIds.filter { it.isNotBlank() }.distinct()
            if (ids.isEmpty()) return@withContext emptyMap()

            val inList = ids.joinToString(",") { it }
            val encodedStoreId = URLEncoder.encode("eq.$currentStoreId", "UTF-8")
            val table = dishImagesTable()
            val url = ShowcaseCloudConfig.restUrl(
                "$table?select=dish_id,image_url,sort_order&store_id=$encodedStoreId&dish_id=in.($inList)&order=sort_order.asc"
            )

            val (code, body) = httpGet(url)
            if (code !in 200..299 || body.isNullOrBlank()) {
                Log.w("ShowcaseCloud", "fetchDishImagesByDishIds failed: $code $body")
                return@withContext emptyMap()
            }

            val arr = JSONArray(body)
            val map = linkedMapOf<String, MutableList<Pair<Int, String>>>()

            for (i in 0 until arr.length()) {
                // 兼容：某些 org.json 版本没有 optJSONObject
                val obj = try { arr.getJSONObject(i) } catch (_: Throwable) { null } ?: continue

                val dishId = obj.optString("dish_id")
                val urlStr = obj.optString("image_url")
                val sortOrder = if (obj.isNull("sort_order")) 0 else obj.optInt("sort_order", 0)
                if (dishId.isBlank() || urlStr.isBlank()) continue

                val list = map.getOrPut(dishId) { mutableListOf() }
                list.add(sortOrder to urlStr)
            }

            map.mapValues { (_, list) -> list.sortedBy { it.first }.map { it.second } }
        }


    /**
     * 方案 B：用 (dish_id, url) 唯一约束做 upsert；并用 id(uuid) 作为主键。
     *
     * 你需要在 Supabase 侧执行：
     *  - dish_images.id uuid primary key default gen_random_uuid()
     *  - unique(dish_id, url)
     *  - (可选) sort_order int4
     *  - 开启 RLS 后：商家写操作需携带当前登录商家的 access token
     */
    suspend fun replaceDishImages(
        storeId: String,
        dishId: String,
        imageUrls: List<String>,
    ): Boolean = withContext(Dispatchers.IO) {
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) {
            Log.w("ShowcaseCloud", "replaceDishImages: SUPABASE_ANON_KEY is blank")
            return@withContext false
        }
        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
        if (dishId.isBlank()) {
            Log.w("ShowcaseCloud", "replaceDishImages: dishId is blank")
            return@withContext false
        }
        val urls = imageUrls.filter { it.isNotBlank() }.distinct().take(9)
        val table = dishImagesTable()
        try {
            Log.d(
                "ShowcaseCloud",
                "replaceDishImages:start storeId=$currentStoreId dishId=$dishId urls=$urls"
            )
            val encodedStoreId = URLEncoder.encode("eq.$currentStoreId", "UTF-8")
            val encodedDishId = URLEncoder.encode("eq.$dishId", "UTF-8")
            val deleteUrl = ShowcaseCloudConfig.restUrl(
                "$table?store_id=$encodedStoreId&dish_id=$encodedDishId"
            )
            Log.d(
                "ShowcaseCloud",
                "replaceDishImages:deleteUrl=$deleteUrl"
            )
            val (deleteCode, deleteBody) = httpDelete(deleteUrl)
            Log.d(
                "ShowcaseCloud",
                "replaceDishImages:delete response code=$deleteCode body=$deleteBody"
            )
            if (deleteCode !in 200..299) {
                Log.w("ShowcaseCloud", "replaceDishImages delete failed: $deleteCode $deleteBody")
                return@withContext false
            }
            if (urls.isEmpty()) {
                Log.d("ShowcaseCloud", "replaceDishImages:no urls left after filter, return true")
                return@withContext true
            }

            val insertUrl = ShowcaseCloudConfig.restUrl(table)
            val arr = JSONArray()
            urls.forEachIndexed { idx, u ->
                val obj = JSONObject()
                obj.put("id", UUID.randomUUID().toString())
                obj.put("store_id", currentStoreId)
                obj.put("dish_id", dishId)
                obj.put("image_url", u)
                obj.put("sort_order", idx)
                arr.put(obj)
            }
            Log.d(
                "ShowcaseCloud",
                "replaceDishImages:insertUrl=$insertUrl body=$arr"
            )
            val (insertCode, insertBody) = httpPost(
                insertUrl,
                JSONObject().apply { put("__raw_array__", arr.toString()) },
                prefer = "return=minimal"
            )
            Log.d(
                "ShowcaseCloud",
                "replaceDishImages:insert response code=$insertCode body=$insertBody"
            )
            if (insertCode !in 200..299) {
                Log.w("ShowcaseCloud", "replaceDishImages insert failed: $insertCode $insertBody")
                return@withContext false
            }
            Log.d(
                "ShowcaseCloud",
                "replaceDishImages:success dishId=$dishId count=${urls.size}"
            )
            true
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "replaceDishImages failed", e)
            false
        }
    }

    suspend fun searchDishes(
        storeId: String,
        keyword: String,
        categoryId: String? = null,
        limit: Int = 50,
        offset: Int = 0,
        order: String = "name_zh.asc",
    ): List<CloudDish> = withContext(Dispatchers.IO) {
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) return@withContext emptyList()

        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
        val kw = keyword.trim()
        if (kw.isBlank()) return@withContext emptyList()

        val l = limit.coerceIn(1, 200)
        val o = offset.coerceAtLeast(0)
        val encodedStoreId = URLEncoder.encode("eq.$currentStoreId", "UTF-8")

        val orExpr = "or=(name_zh.ilike.*$kw*,name_en.ilike.*$kw*)"
        val table = dishesTable()

        val categoryFilter = categoryId?.trim()?.takeIf { it.isNotBlank() }?.let {
            "category_id=eq.${urlEncode(it)}&"
        } ?: ""

        val url = ShowcaseCloudConfig.restUrl(
            "$table" +
                    "?select=id,store_id,name_en,name_zh,description_en,description_zh," +
                    "category_id,price,discount_price,recommended,sold_out,hidden,image_url," +
                    "tags,external_link,updated_at" +
                    "&store_id=$encodedStoreId" +
                    "&$categoryFilter${urlEncode(orExpr)}" +
                    "&order=${urlEncode(order)}" +
                    "&limit=$l&offset=$o"
        )
        Log.d("ShowcaseCloud", "searchDishes: GET $url")

        val (code, body) = httpGet(url)
        if (code !in 200..299 || body.isNullOrBlank()) return@withContext emptyList()
        enrichDishesWithImages(currentStoreId, parseDishesArray(body))
    }

    private fun parseDishesArray(body: String): List<CloudDish> {
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
            val price = obj.optDouble("price", 0.0)
            val discountPrice = if (!obj.isNull("discount_price")) obj.optDouble("discount_price") else null
            val recommended = obj.optBoolean("recommended", false)
            val soldOut = obj.optBoolean("sold_out", false)
// 兼容字段名：hidden / is_hidden
            val hidden = when {
                obj.has("hidden") -> obj.optBoolean("hidden", false)
                obj.has("is_hidden") -> obj.optBoolean("is_hidden", false)
                else -> false
            }

            val imageUrl = obj.optString("image_url", null)


            val tags = mutableListOf<String>()
            val tagsArr = obj.optJSONArray("tags")
            if (tagsArr != null) {
                for (t in 0 until tagsArr.length()) {
                    val v = tagsArr.optString(t)
                    if (!v.isNullOrBlank()) tags.add(v)
                }
            }
            val externalLink = obj.optString("external_link", null)
            val updatedAt = if (obj.isNull("updated_at")) null else obj.optLong("updated_at")
            val clickCount = obj.optInt("click_count", 0)

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
                    hidden = hidden,
                    imageUrl = imageUrl,
                    tags = tags,
                    externalLink = externalLink,
                    updatedAt = updatedAt,
                    clickCount = clickCount
                )
            )
        }

        return result

    }
    private suspend fun enrichDishesWithImages(
        storeId: String,
        dishes: List<CloudDish>
    ): List<CloudDish> = withContext(Dispatchers.IO) {
        val ids = dishes.mapNotNull { it.id }.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return@withContext dishes

        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
        val imagesMap = fetchDishImagesByDishIds(currentStoreId, ids)

        dishes.map { d ->
            val imgs = d.id?.let { imagesMap[it] }.orEmpty()
            when {
                imgs.isNotEmpty() -> d.copy(imageUrls = imgs)
                !d.imageUrl.isNullOrBlank() -> d.copy(imageUrls = listOf(d.imageUrl))
                else -> d
            }
        }
    }
    // ✅ 记录最近一次 upsert store_profile 的返回（用于 VM/UI 显示）
    @Volatile var lastUpsertCode: Int? = null
        private set
    @Volatile var lastUpsertBody: String? = null
        private set

    @Volatile var lastAnnouncementUpsertCode: Int? = null
        private set
    @Volatile var lastAnnouncementUpsertBody: String? = null
        private set

    @Volatile var lastAnnouncementPushCode: Int? = null
        private set
    @Volatile var lastAnnouncementPushBody: String? = null
        private set

    @Volatile var lastMerchantAuthCode: Int? = null
        private set
    @Volatile var lastMerchantAuthBody: String? = null
        private set

    @Volatile var lastDeleteCode: Int? = null
        private set
    @Volatile var lastDeleteBody: String? = null
        private set

    @Volatile var lastDishImageUploadCode: Int? = null
        private set
    @Volatile var lastDishImageUploadBody: String? = null
        private set

    @Volatile var lastStoreImageUploadCode: Int? = null
        private set
    @Volatile var lastStoreImageUploadBody: String? = null
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

    /*
     * -------------------- 写入：分类 & 菜品（Scheme A：本地 UUID 为主键）--------------------
     */
    // 让 VM 继续调用 upsertDishFromDemo，不改 VM
    suspend fun upsertDishFromDemo(
        storeId: String,
        id: String,
        nameZh: String,
        nameEn: String,
        descriptionEn: String,
        category: String,
        originalPrice: Double,
        discountPrice: Double?,
        isRecommended: Boolean,
        isSoldOut: Boolean,
        imageUri: String?
    ): Boolean {
        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)

        val dish = DemoDish(
            id = id,
            nameZh = nameZh,
            nameEn = nameEn,
            descriptionEn = descriptionEn,
            category = category,
            originalPrice = originalPrice.toFloat(),
            discountPrice = discountPrice?.toFloat(),
            isRecommended = isRecommended,
            isSoldOut = isSoldOut,
            imageUri = imageUri?.let { android.net.Uri.parse(it) }
        )
        return upsertDishSchemeA(
            dish = dish,
            storeId = currentStoreId
        )
    }

    suspend fun getCategoryIdByName(
        storeId: String,
        name: String
    ): String? {
        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
        val n = name.trim()
        if (n.isBlank()) return null
        val categories = fetchCategories(currentStoreId)
        return categories.firstOrNull { it.name == n }?.id
    }

    suspend fun hasAnyDishReferencingCategoryId(
        storeId: String,
        categoryId: String
    ): Boolean {
        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
        val id = categoryId.trim()
        if (id.isBlank()) return false
        val dishes = fetchDishes(currentStoreId)
        return dishes.any { it.categoryId == id }
    }

    suspend fun upsertDishSchemeA(
        dish: DemoDish,
        storeId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) {
            Log.w("ShowcaseCloud", "upsertDishSchemeA: SUPABASE_ANON_KEY is blank, skip")
            return@withContext false
        }

        try {
            val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
            val categoryId = resolveOrCreateCategoryId(currentStoreId, dish.category)

            val table = dishesTable()
            val url = ShowcaseCloudConfig.restUrl("$table?on_conflict=id")
            val payload = JSONObject().apply {
                put("id", dish.id)
                put("store_id", currentStoreId)
                put("name_en", dish.nameEn)
                put("name_zh", dish.nameZh)
                put("description_en", dish.descriptionEn ?: JSONObject.NULL)
                put("description_zh", JSONObject.NULL)
                put("category_id", categoryId ?: JSONObject.NULL)
                put("price", dish.originalPrice.toDouble())
                put("discount_price", dish.discountPrice?.toDouble() ?: JSONObject.NULL)
                put("recommended", dish.isRecommended)
                put("sold_out", dish.isSoldOut)
                put("hidden", dish.isHidden)
                put("image_url", dish.imageUri?.toString() ?: JSONObject.NULL)

                val tagsArr = JSONArray()
                dish.tags.forEach { t ->
                    if (t.isNotBlank()) tagsArr.put(t)
                }
                put("tags", tagsArr)
                put("external_link", dish.externalLink ?: JSONObject.NULL)
                if (dish.updatedAt > 0L) {
                    put("updated_at", dish.updatedAt)
                }
            }

            val bodyArr = JSONArray().apply { put(payload) }

            Log.d("ShowcaseCloud", "upsertDishSchemeA: POST $url body=$bodyArr")
            val (code, responseBody) = httpPost(
                url,
                JSONObject().apply { put("__raw_array__", bodyArr.toString()) },
                prefer = "resolution=merge-duplicates,return=minimal"
            )
            lastUpsertCode = code
            lastUpsertBody = responseBody
            Log.d(
                "ShowcaseCloud",
                "upsertDishSchemeA: response code=$code body=$responseBody imageUrl=${dish.imageUri?.toString()}"
            )
            code in 200..299
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "upsertDishSchemeA: failed", e)
            false
        }
    }

    suspend fun deleteDishById(
        storeId: String,
        id: String
    ): Boolean = withContext(Dispatchers.IO) {
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) {
            Log.w("ShowcaseCloud", "deleteDishById: SUPABASE_ANON_KEY is blank, skip")
            return@withContext false
        }

        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)

        if (id.isBlank()) {
            Log.w("ShowcaseCloud", "deleteDishById: id is blank, skip")
            return@withContext false
        }

        try {
            val encodedId = URLEncoder.encode("eq.$id", "UTF-8")
            val encodedStoreId = URLEncoder.encode("eq.$currentStoreId", "UTF-8")
            val table = dishesTable()
            val url = ShowcaseCloudConfig.restUrl("$table?id=$encodedId&store_id=$encodedStoreId")
            Log.d("ShowcaseCloud", "deleteDishById: DELETE $url")

            val (code, responseBody) = httpDelete(url)
            lastDeleteCode = code
            lastDeleteBody = responseBody
            Log.d("ShowcaseCloud", "deleteDishById: response code=$code body=$responseBody")

            code in 200..299
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "deleteDishById: failed", e)
            false
        }
    }

    /**
     * 根据分类名称删除分类（先查 id，再 delete）。
     *
     * - 若找不到该名称的分类：返回 false
     * - 若删除成功：返回 true
     */
    suspend fun deleteCategoryByName(
        storeId: String,
        name: String
    ): CategoryWriteResult = withContext(Dispatchers.IO) {
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) {
            Log.w("ShowcaseCloud", "deleteCategoryByName: SUPABASE_ANON_KEY is blank, skip")
            return@withContext CategoryWriteResult(
                ok = false,
                errorMessage = "Delete category failed. SUPABASE_ANON_KEY is blank."
            )
        }

        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            Log.w("ShowcaseCloud", "deleteCategoryByName: name is blank, skip")
            return@withContext CategoryWriteResult(
                ok = false,
                errorMessage = "Delete category failed. Category name is blank."
            )
        }

        try {
            val categories = fetchCategories(currentStoreId)
            val target = categories.find { it.name == trimmed }
            if (target == null) {
                Log.d("ShowcaseCloud", "deleteCategoryByName: no category named $trimmed, skip")
                return@withContext CategoryWriteResult(
                    ok = false,
                    errorMessage = "Delete category failed. Category was not found in cloud."
                )
            }

            val encodedId = URLEncoder.encode("eq.${target.id}", "UTF-8")
            val encodedStoreId = URLEncoder.encode("eq.$currentStoreId", "UTF-8")
            val table = categoriesTable()
            val url = ShowcaseCloudConfig.restUrl("$table?id=$encodedId&store_id=$encodedStoreId")
            Log.d("ShowcaseCloud", "deleteCategoryByName: DELETE $url")

            val (code, responseBody) = httpDelete(url)
            Log.d(
                "ShowcaseCloud",
                "deleteCategoryByName: response code=$code body=$responseBody"
            )

            if (code in 200..299) {
                CategoryWriteResult(ok = true)
            } else {
                CategoryWriteResult(
                    ok = false,
                    errorMessage = buildCategoryWriteErrorMessage(
                        actionLabel = "Delete category",
                        code = code,
                        body = responseBody
                    ),
                    errorCode = code,
                    errorBody = responseBody
                )
            }
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "deleteCategoryByName: failed", e)
            CategoryWriteResult(
                ok = false,
                errorMessage = "Delete category failed. ${e.message ?: "Unknown error."}"
            )
        }
    }

    suspend fun ensureCategoryExists(
        storeId: String,
        name: String
    ): CategoryWriteResult = withContext(Dispatchers.IO) {
        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            return@withContext CategoryWriteResult(
                ok = false,
                errorMessage = "Add category failed. Category name is blank."
            )
        }

        val existingBinding = fetchMerchantBindingForCurrentStore(currentStoreId)
        val currentAuthUserId = ShowcaseStoreSession.currentMerchantAuthUserId()?.trim().orEmpty()
        if (existingBinding == null || currentAuthUserId.isBlank() || !existingBinding.authUserId.equals(currentAuthUserId, ignoreCase = true)) {
            return@withContext CategoryWriteResult(
                ok = false,
                errorMessage = "Add category failed. Current account is not bound to this store."
            )
        }

        try {
            val encodedStoreId = URLEncoder.encode("eq.$currentStoreId", "UTF-8")
            val table = categoriesTable()
            val orExpr = "or=(name_zh.eq.${urlEncode(trimmed)},name_en.eq.${urlEncode(trimmed)})"
            val urlSelect = ShowcaseCloudConfig.restUrl(
                "$table?store_id=$encodedStoreId&select=id&$orExpr&limit=1"
            )
            Log.d("ShowcaseCloud", "ensureCategoryExists: GET $urlSelect")

            val (codeSelect, bodySelect) = httpGet(urlSelect)
            Log.d("ShowcaseCloud", "ensureCategoryExists: select code=$codeSelect body=$bodySelect")
            if (codeSelect in 200..299 && !bodySelect.isNullOrBlank()) {
                val arr = JSONArray(bodySelect)
                if (arr.length() > 0) {
                    val first = arr.getJSONObject(0)
                    val existingId = first.optString("id", null)
                    if (!existingId.isNullOrBlank()) {
                        return@withContext CategoryWriteResult(ok = true)
                    }
                }
            }

            val urlInsert = ShowcaseCloudConfig.restUrl(table)
            val payload = JSONObject().apply {
                put("id", UUID.randomUUID().toString())
                put("store_id", currentStoreId)
                put("name", trimmed)
                put("name_zh", trimmed)
                put("name_en", trimmed)
            }
            Log.d("ShowcaseCloud", "ensureCategoryExists: POST $urlInsert body=$payload")

            val (codeInsert, bodyInsert) = httpPost(
                urlInsert,
                payload,
                prefer = "return=representation"
            )
            Log.d("ShowcaseCloud", "ensureCategoryExists: insert code=$codeInsert body=$bodyInsert")

            if (codeInsert !in 200..299 || bodyInsert.isNullOrBlank()) {
                return@withContext CategoryWriteResult(
                    ok = false,
                    errorMessage = buildCategoryWriteErrorMessage(
                        actionLabel = "Add category",
                        code = codeInsert,
                        body = bodyInsert
                    ),
                    errorCode = codeInsert,
                    errorBody = bodyInsert
                )
            }

            val arr2 = JSONArray(bodyInsert)
            if (arr2.length() == 0) {
                return@withContext CategoryWriteResult(
                    ok = false,
                    errorMessage = "Add category failed. Cloud returned an empty insert result."
                )
            }

            val obj = arr2.getJSONObject(0)
            val newId = obj.optString("id", null)
            if (newId.isNullOrBlank()) {
                return@withContext CategoryWriteResult(
                    ok = false,
                    errorMessage = "Add category failed. Cloud returned no category id."
                )
            }

            CategoryWriteResult(ok = true)
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "ensureCategoryExists failed", e)
            CategoryWriteResult(
                ok = false,
                errorMessage = "Add category failed. ${e.message ?: "Unknown error."}"
            )
        }
    }

    /**
     * 展示型必备：分类重命名（运营常用）
     * - 仅改 categories.name，不自动级联 dishes（dishes 关联的是 category_id，不需要级联）
     */
    suspend fun renameCategoryById(
        storeId: String,
        categoryId: String,
        newName: String
    ): CategoryWriteResult = withContext(Dispatchers.IO) {
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) {
            return@withContext CategoryWriteResult(
                ok = false,
                errorMessage = "Update category failed. SUPABASE_ANON_KEY is blank."
            )
        }

        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
        val id = categoryId.trim()
        val nn = newName.trim()
        if (id.isBlank() || nn.isBlank()) {
            return@withContext CategoryWriteResult(
                ok = false,
                errorMessage = "Update category failed. Category id or name is blank."
            )
        }

        try {
            val encodedId = URLEncoder.encode("eq.$id", "UTF-8")
            val encodedStoreId = URLEncoder.encode("eq.$currentStoreId", "UTF-8")
            val table = categoriesTable()
            val url = ShowcaseCloudConfig.restUrl("$table?id=$encodedId&store_id=$encodedStoreId")

            val payload = JSONObject().apply {
                put("name", nn)
                put("name_zh", nn)
                put("name_en", nn)
            }

            Log.d("ShowcaseCloud", "renameCategoryById: PATCH $url body=$payload")
            val (code, body) = httpPatch(url, payload)
            Log.d("ShowcaseCloud", "renameCategoryById: response code=$code body=$body")

            if (code in 200..299) {
                CategoryWriteResult(ok = true)
            } else {
                CategoryWriteResult(
                    ok = false,
                    errorMessage = buildCategoryWriteErrorMessage(
                        actionLabel = "Update category",
                        code = code,
                        body = body
                    ),
                    errorCode = code,
                    errorBody = body
                )
            }
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "renameCategoryById failed", e)
            CategoryWriteResult(
                ok = false,
                errorMessage = "Update category failed. ${e.message ?: "Unknown error."}"
            )
        }
    }
    /**
     * 展示型必备：分类排序（可选启用）
     * - 需要云端 categories 表存在 sort_order:int4 字段
     */
    suspend fun setCategorySortOrder(
        storeId: String,
        categoryId: String,
        sortOrder: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) return@withContext false

        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
        val id = categoryId.trim()
        if (id.isBlank()) return@withContext false

        try {
            val encodedId = URLEncoder.encode("eq.$id", "UTF-8")
            val encodedStoreId = URLEncoder.encode("eq.$currentStoreId", "UTF-8")
            val table = categoriesTable()
            val url = ShowcaseCloudConfig.restUrl("$table?id=$encodedId&store_id=$encodedStoreId")

            val payload = JSONObject().apply {
                put("sort_order", sortOrder)
            }

            Log.d("ShowcaseCloud", "setCategorySortOrder: PATCH $url body=$payload")
            val (code, body) = httpPatch(url, payload)
            Log.d("ShowcaseCloud", "setCategorySortOrder: response code=$code body=$body")

            code in 200..299
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "setCategorySortOrder failed", e)
            false
        }
    }

    /**
     * 根据分类名称去 categories 表查找 id，不存在则自动创建该分类并返回新 id。
     * 用于把 DemoDish.category 映射到 Supabase 的 category_id。
     */
    private suspend fun resolveOrCreateCategoryId(
        storeId: String,
        name: String
    ): String? =
        withContext(Dispatchers.IO) {
            val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
            if (apiKey.isBlank()) {
                Log.w("ShowcaseCloud", "resolveOrCreateCategoryId: SUPABASE_ANON_KEY is blank, skip")
                return@withContext null
            }

            val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
            val trimmed = name.trim()
            if (trimmed.isEmpty()) {
                Log.w("ShowcaseCloud", "resolveOrCreateCategoryId: name is blank")
                return@withContext null
            }

            try {
                val encodedStoreId = URLEncoder.encode("eq.$currentStoreId", "UTF-8")
                val table = categoriesTable()
                val orExpr = "or=(name_zh.eq.${urlEncode(trimmed)},name_en.eq.${urlEncode(trimmed)})"
                val urlSelect = ShowcaseCloudConfig.restUrl(
                    "$table?store_id=$encodedStoreId&select=id&$orExpr&limit=1"
                )
                Log.d("ShowcaseCloud", "resolveOrCreateCategoryId: GET $urlSelect")

                val (codeSelect, bodySelect) = httpGet(urlSelect)
                if (codeSelect in 200..299 && !bodySelect.isNullOrBlank()) {
                    val arr = JSONArray(bodySelect)
                    if (arr.length() > 0) {
                        val first = arr.getJSONObject(0)
                        val existingId = first.optString("id", null)
                        if (!existingId.isNullOrBlank()) {
                            Log.d(
                                "ShowcaseCloud",
                                "resolveOrCreateCategoryId: found existing category id=$existingId name=$trimmed storeId=$currentStoreId"
                            )
                            return@withContext existingId
                        }
                    }
                }

                val urlInsert = ShowcaseCloudConfig.restUrl(table)
                val payload = JSONObject().apply {
                    put("id", UUID.randomUUID().toString())
                    put("store_id", currentStoreId)
                    put("name_zh", trimmed)
                    put("name_en", trimmed)
                }
                Log.d("ShowcaseCloud", "resolveOrCreateCategoryId: POST $urlInsert body=$payload")

                val (codeInsert, bodyInsert) = httpPost(
                    urlInsert,
                    payload,
                    prefer = "return=representation"
                )
                if (codeInsert !in 200..299 || bodyInsert.isNullOrBlank()) {
                    Log.w(
                        "ShowcaseCloud",
                        "resolveOrCreateCategoryId: insert failed code=$codeInsert body=$bodyInsert"
                    )
                    return@withContext null
                }

                val arr2 = JSONArray(bodyInsert)
                if (arr2.length() == 0) {
                    Log.w("ShowcaseCloud", "resolveOrCreateCategoryId: insert returned empty array")
                    return@withContext null
                }
                val obj = arr2.getJSONObject(0)
                val newId = obj.optString("id", null)
                if (newId.isNullOrBlank()) {
                    Log.w("ShowcaseCloud", "resolveOrCreateCategoryId: insert returned no id")
                    return@withContext null
                }

                Log.d(
                    "ShowcaseCloud",
                    "resolveOrCreateCategoryId: created new category id=$newId name=$trimmed storeId=$currentStoreId"
                )
                return@withContext newId
            } catch (e: Exception) {
                Log.e("ShowcaseCloud", "resolveOrCreateCategoryId: failed", e)
                null
            }
        }

    /**
     * 上传菜品图片（bytes），返回可访问的 public URL（bucket 需设置为 public）。
     */
    suspend fun uploadDishImageBytes(
        bytes: ByteArray,
        fileExt: String = "jpg",
        contentType: String = "image/jpeg"
    ): String? = withContext(Dispatchers.IO) {
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) {
            Log.w("ShowcaseCloud", "uploadDishImageBytes: SUPABASE_ANON_KEY is blank")
            return@withContext null
        }
        try {
            val safeStoreId = ShowcaseCloudConfig.currentStoreId().replace(":", "_")
            val objectPath = "dishes/$safeStoreId/${UUID.randomUUID()}.$fileExt"
            val bucket = dishImagesBucket()
            val putUrl = ShowcaseCloudConfig.storageObjectUrl(
                bucket = bucket,
                objectPath = objectPath
            )
            Log.d(
                "ShowcaseCloud",
                "uploadDishImageBytes:start bucket=$bucket objectPath=$objectPath size=${bytes.size} contentType=$contentType"
            )
            Log.d(
                "ShowcaseCloud",
                "uploadDishImageBytes:putUrl=$putUrl"
            )

            val (code, body) = httpPutBytes(
                urlString = putUrl,
                bytes = bytes,
                contentType = contentType
            )
            lastDishImageUploadCode = code
            lastDishImageUploadBody = body
            Log.d(
                "ShowcaseCloud",
                "uploadDishImageBytes:response code=$code body=$body"
            )
            if (code !in 200..299) {
                Log.w("ShowcaseCloud", "uploadDishImageBytes failed code=$code body=$body")
                return@withContext null
            }

            val publicUrl = ShowcaseCloudConfig.storagePublicObjectUrl(
                bucket = bucket,
                objectPath = objectPath
            )
            Log.d(
                "ShowcaseCloud",
                "uploadDishImageBytes:success publicUrl=$publicUrl"
            )
            publicUrl
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "uploadDishImageBytes failed", e)
            null
        }
    }
    /**
     * 上传店铺图片（cover/logo），返回可访问的 public URL（bucket 需设置为 public）。
     * kind: "cover" | "logo"
     */
    suspend fun uploadStoreImageBytes(
        bytes: ByteArray,
        kind: String,
        fileExt: String = "jpg",
        contentType: String = "image/jpeg"
    ): String? = withContext(Dispatchers.IO) {
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) return@withContext null

        try {
            val safeKind = kind.trim().ifBlank { "cover" }
            // ✅ 用随机 UUID，避免覆盖；路径分目录，避免与 dish/chat 混
            val safeStoreId = ShowcaseCloudConfig.currentStoreId().replace(":", "_")
            val objectPath = "store/$safeStoreId/$safeKind/${UUID.randomUUID()}.$fileExt"

            val bucket = storeImagesBucket()
            val putUrl = ShowcaseCloudConfig.storageObjectUrl(bucket = bucket, objectPath = objectPath)

            val (code, body) = httpPutBytes(
                urlString = putUrl,
                bytes = bytes,
                contentType = contentType
            )
            lastStoreImageUploadCode = code
            lastStoreImageUploadBody = body

            if (code !in 200..299) {
                Log.w("ShowcaseCloud", "uploadStoreImageBytes failed code=$code body=$body")
                return@withContext null
            }

            // ✅ public bucket 的公网 URL
            ShowcaseCloudConfig.storagePublicObjectUrl(bucket = bucket, objectPath = objectPath)
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "uploadStoreImageBytes failed", e)
            null
        }
    }

    /**
     * 展示型必备：删除图片（可选启用）
     *
     * - 仅删除 Storage Object；不会自动更新 dishes.image_url（那一步通常由 VM/Repo 的 dish upsert 完成）
     * - 传入的 imageUrl 需要是 Supabase storage object URL 或 public URL
     */
    suspend fun deleteDishImageByUrl(imageUrl: String): Boolean = withContext(Dispatchers.IO) {
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) return@withContext false

        val url = imageUrl.trim()
        if (url.isBlank()) return@withContext false

        try {
            // 解析 object path：优先从 "/storage/v1/object/" 后截取
            val marker = "/storage/v1/object/"
            val idx = url.indexOf(marker)
            if (idx < 0) {
                Log.w("ShowcaseCloud", "deleteDishImageByUrl: not a supabase storage url, skip url=$url")
                return@withContext false
            }

            val after = url.substring(idx + marker.length).trimStart('/')
            // after 形如: "<bucket>/<path...>"
            val slash = after.indexOf('/')
            if (slash <= 0) {
                Log.w("ShowcaseCloud", "deleteDishImageByUrl: invalid storage url, skip url=$url")
                return@withContext false
            }

            val bucket = after.substring(0, slash)
            val objectPath = after.substring(slash + 1)
            if (bucket.isBlank() || objectPath.isBlank()) return@withContext false

            val deleteUrl = ShowcaseCloudConfig.storageObjectUrl(bucket = bucket, objectPath = objectPath)
            Log.d("ShowcaseCloud", "deleteDishImageByUrl: DELETE $deleteUrl")

            val (code, body) = httpDelete(deleteUrl)
            Log.d("ShowcaseCloud", "deleteDishImageByUrl: response code=$code body=$body")

            code in 200..299
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "deleteDishImageByUrl failed", e)
            false
        }
    }

    /**
     * -------------------- 展示型必备扩展：StoreProfile --------------------
     */

    /**
     * 拉取店铺信息（约定：只取一条，按 updated_at desc）
     * - 需要你云端 store_profile 表存在相应字段（字段名参考 payload）
     */
    suspend fun fetchStoreServiceStatus(
        storeId: String
    ): CloudStoreServiceStatus? = withContext(Dispatchers.IO) {
        try {
            val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
            val table = storesTable()
            val url = ShowcaseCloudConfig.restUrl(
                "$table?select=store_id,plan_type,service_status,service_end_at,delete_at,is_write_allowed&store_id=eq.${urlEncode(currentStoreId)}&limit=1"
            )

            val (code, body) = httpGet(
                urlString = url,
                actor = ShowcaseCloudConfig.AuthActor.PUBLIC,
                scopeStoreId = currentStoreId
            )
            if (code !in 200..299 || body.isNullOrBlank()) return@withContext null

            val arr = JSONArray(body)
            if (arr.length() <= 0) return@withContext null

            val obj = arr.optJSONObject(0) ?: return@withContext null
            CloudStoreServiceStatus(
                storeId = obj.optString("store_id", "").trim(),
                planType = obj.optString("plan_type", "").trim(),
                serviceStatus = obj.optString("service_status", "").trim(),
                serviceEndAt = obj.optString("service_end_at", "").trim().ifBlank { null },
                deleteAt = obj.optString("delete_at", "").trim().ifBlank { null },
                isWriteAllowed = obj.optBoolean("is_write_allowed", true)
            )
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "fetchStoreServiceStatus failed", e)
            null
        }
    }

    suspend fun isStoreWriteAllowed(
        storeId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val status = fetchStoreServiceStatus(storeId) ?: return@withContext true
        status.isWriteAllowed &&
                !status.serviceStatus.equals("read_only", ignoreCase = true) &&
                !status.serviceStatus.equals("deleted", ignoreCase = true)
    }

    suspend fun fetchStoreProfile(
        storeId: String
    ): CloudStoreProfile? = withContext(Dispatchers.IO) {
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) return@withContext null

        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
        val encodedStoreId = URLEncoder.encode("eq.$currentStoreId", "UTF-8")
        val table = storeProfileTable()
        val url = ShowcaseCloudConfig.restUrl(
            "$table" +
                    "?select=store_id,title,subtitle,description,address,hours,map_url,cover_url,logo_url,business_status,extra_contacts_json,services_json" +
                    "&store_id=$encodedStoreId" +
                    "&limit=1"
        )

        Log.d("ShowcaseCloud", "fetchStoreProfile: GET $url")

        val (code, body) = httpGet(url)
        if (code !in 200..299 || body.isNullOrBlank()) return@withContext null

        return@withContext try {
            val arr = JSONArray(body)
            if (arr.length() == 0) null
            else parseStoreProfile(arr.getJSONObject(0))
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "fetchStoreProfile parse failed", e)
            null
        }
    }
    /**
     * StoreProfile：参数版 upsert（给 VM 直接用，最小改动）
     * - 内部复用已有的 upsertStoreProfile(profile: CloudStoreProfile)
     * - updatedAt 默认写当前时间戳（毫秒）；若你云端用触发器自动更新时间，也可改为 null
     */
    suspend fun upsertStoreProfile(
        storeId: String,
        title: String,
        subtitle: String,
        description: String,
        address: String,
        hours: String,
        mapUrl: String,
        coverUrl: String,
        logoUrl: String,
        businessStatus: String,
        extraContactsJson: String,
        servicesJson: String,
    ): Boolean {
        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
        val profile = CloudStoreProfile(
            storeId = currentStoreId,
            title = title,
            subtitle = subtitle,
            description = description,
            address = address,
            hours = hours,
            mapUrl = mapUrl,
            extraContactsJson = extraContactsJson,
            servicesJson = servicesJson,
            coverUrl = coverUrl,
            logoUrl = logoUrl,
            businessStatus = businessStatus,
            updatedAt = System.currentTimeMillis()
        )
        return upsertStoreProfile(profile)
    }

    /**
     * upsert 店铺信息（Scheme：本地提供 id 或自动生成）
     * - on_conflict=id
     */
    suspend fun upsertStoreProfile(profile: CloudStoreProfile): Boolean = withContext(Dispatchers.IO) {
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) return@withContext false

        try {
            val currentStoreId = ShowcaseCloudConfig.requireStoreId(profile.storeId)

            val safeLogoUrl = profile.logoUrl
                .orEmpty()
                .trim()
                .takeIf { it.startsWith("http://") || it.startsWith("https://") }
                .orEmpty()

            val safeCoverUrl = profile.coverUrl
                .orEmpty()
                .trim()
                .replace("\\n", "\n")
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotBlank() && (it.startsWith("http://") || it.startsWith("https://")) }
                .distinct()
                .joinToString("\n")

            val payload = JSONObject().apply {
                put("store_id", currentStoreId)
                put("title", profile.title)
                put("subtitle", profile.subtitle)
                put("description", profile.description)
                put("address", profile.address)
                put("hours", profile.hours)
                put("map_url", profile.mapUrl)
                put("extra_contacts_json", profile.extraContactsJson)
                put("services_json", profile.servicesJson)
                put("cover_url", safeCoverUrl)
                put("logo_url", safeLogoUrl)
                put("business_status", profile.businessStatus)
            }

            val table = storeProfileTable()
            val checkUrl = ShowcaseCloudConfig.restUrl(
                "$table?select=store_id&store_id=eq.${urlEncode(currentStoreId)}&limit=1"
            )
            val (checkCode, checkBody) = httpGet(
                urlString = checkUrl,
                actor = ShowcaseCloudConfig.AuthActor.MERCHANT,
                scopeStoreId = currentStoreId
            )

            val exists = if (checkCode in 200..299 && !checkBody.isNullOrBlank()) {
                val arr = JSONArray(checkBody)
                arr.length() > 0
            } else {
                false
            }

            val (code, resp) = if (exists) {
                val patchUrl = ShowcaseCloudConfig.restUrl(
                    "$table?store_id=eq.${urlEncode(currentStoreId)}"
                )
                Log.d("ShowcaseCloud", "upsertStoreProfile: PATCH $patchUrl body=$payload")
                httpPatch(
                    urlString = patchUrl,
                    body = payload,
                    prefer = "return=representation",
                    actor = ShowcaseCloudConfig.AuthActor.MERCHANT,
                    scopeStoreId = currentStoreId
                )
            } else {
                val postUrl = ShowcaseCloudConfig.restUrl(table)
                val bodyArr = JSONArray().apply { put(payload) }
                Log.d("ShowcaseCloud", "upsertStoreProfile: POST $postUrl body=$bodyArr")
                httpPost(
                    postUrl,
                    JSONObject().apply { put("__raw_array__", bodyArr.toString()) },
                    prefer = "return=representation",
                    actor = ShowcaseCloudConfig.AuthActor.MERCHANT
                )
            }

            Log.d("ShowcaseCloud", "upsertStoreProfile: response code=$code body=$resp")

            lastUpsertCode = code
            lastUpsertBody = resp

            code in 200..299

        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "upsertStoreProfile failed", e)
            false
        }
    }

    private fun parseIsoMillis(raw: String?): Long? {
        val v = raw?.trim().orEmpty()
        if (v.isBlank()) return null

        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )

        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                    isLenient = false
                }
                val date = sdf.parse(v)
                if (date != null) return date.time
            } catch (_: Throwable) {
            }
        }

        return null
    }

    private fun formatIsoUtcMillis(value: Long?): String? {
        if (value == null) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }.format(java.util.Date(value))
        } catch (_: Throwable) {
            null
        }
    }

    private fun parseAnnouncement(obj: JSONObject): CloudAnnouncement {
        val updatedRaw = if (obj.has("updated_at")) obj.opt("updated_at") else null
        val createdRaw = if (obj.has("created_at")) obj.opt("created_at") else null

        val updatedAt = when (updatedRaw) {
            null, JSONObject.NULL -> null
            is Number -> updatedRaw.toLong()
            is String -> updatedRaw.trim().toLongOrNull() ?: parseIsoMillis(updatedRaw)
            else -> null
        }

        val createdAt = when (createdRaw) {
            null, JSONObject.NULL -> null
            is Number -> createdRaw.toLong()
            is String -> createdRaw.trim().toLongOrNull() ?: parseIsoMillis(createdRaw)
            else -> null
        }

        return CloudAnnouncement(
            id = obj.optString("id", ""),
            storeId = obj.optString("store_id", ""),
            coverUrl = obj.optString("cover_url", "").ifBlank { null },
            body = obj.optString("body", ""),
            status = obj.optString("status", "draft"),
            updatedAt = updatedAt,
            createdAt = createdAt,
            viewCount = obj.optInt("view_count", 0)
        )
    }

    suspend fun fetchAnnouncements(
        storeId: String,
        includeDrafts: Boolean = false,
        actor: ShowcaseCloudConfig.AuthActor = if (includeDrafts) {
            ShowcaseCloudConfig.AuthActor.MERCHANT
        } else {
            ShowcaseCloudConfig.AuthActor.PUBLIC
        }
    ): List<CloudAnnouncement> = withContext(Dispatchers.IO) {
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) return@withContext emptyList()

        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)

        try {
            val table = announcementsTable()
            val encodedStoreId = URLEncoder.encode("eq.$currentStoreId", "UTF-8")
            val statusFilter = if (includeDrafts) {
                ""
            } else {
                "&status=${URLEncoder.encode("in.(sent,published)", "UTF-8")}"
            }

            val url = ShowcaseCloudConfig.restUrl(
                "$table?select=id,store_id,cover_url,body,status,updated_at,created_at,view_count&store_id=$encodedStoreId$statusFilter&order=updated_at.desc"
            )
            Log.d("ShowcaseCloud", "fetchAnnouncements: GET $url")
            val (code, body) = httpGet(
                urlString = url,
                actor = actor,
                scopeStoreId = currentStoreId
            )
            Log.d("ShowcaseCloud", "fetchAnnouncements: response code=$code body=$body")
            if (code !in 200..299 || body.isNullOrBlank()) return@withContext emptyList()

            val arr = JSONArray(body)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = try {
                        arr.getJSONObject(i)
                    } catch (_: Throwable) {
                        null
                    } ?: continue
                    add(parseAnnouncement(obj))
                }
            }
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "fetchAnnouncements failed", e)
            emptyList()
        }
    }

    suspend fun upsertAnnouncement(announcement: CloudAnnouncement): Boolean = withContext(Dispatchers.IO) {
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) return@withContext false
        try {
            val table = announcementsTable()
            val url = ShowcaseCloudConfig.restUrl("$table?on_conflict=id")
            val payload = JSONObject().apply {
                put("id", announcement.id)
                put("store_id", announcement.storeId)
                put("cover_url", announcement.coverUrl ?: JSONObject.NULL)
                put("body", announcement.body)
                put("status", announcement.status)
                put("updated_at", formatIsoUtcMillis(announcement.updatedAt) ?: JSONObject.NULL)
                put("view_count", announcement.viewCount)
            }
            val bodyArr = JSONArray().apply {
                put(payload)
            }
            Log.d("ShowcaseCloud", "upsertAnnouncement: POST $url body=$bodyArr")
            val (code, resp) = httpPost(
                url,
                JSONObject().apply { put("__raw_array__", bodyArr.toString()) },
                prefer = "resolution=merge-duplicates,return=representation"
            )
            Log.d("ShowcaseCloud", "upsertAnnouncement: response code=$code body=$resp")
            lastAnnouncementUpsertCode = code
            lastAnnouncementUpsertBody = resp
            code in 200..299
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "upsertAnnouncement failed", e)
            false
        }
    }

    suspend fun deleteAnnouncements(
        storeId: String,
        ids: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext true
        try {
            val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
            val table = announcementsTable()
            val inValue = ids.joinToString(separator = ",", prefix = "in.(", postfix = ")")
            val encodedIds = URLEncoder.encode(inValue, "UTF-8")
            val encodedStoreId = URLEncoder.encode("eq.$currentStoreId", "UTF-8")
            val url = ShowcaseCloudConfig.restUrl("$table?id=$encodedIds&store_id=$encodedStoreId")
            Log.d("ShowcaseCloud", "deleteAnnouncements: DELETE $url")
            val (code, resp) = httpDelete(url)
            lastDeleteCode = code
            lastDeleteBody = resp
            Log.d("ShowcaseCloud", "deleteAnnouncements: response code=$code body=$resp")
            code in 200..299
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "deleteAnnouncements failed", e)
            false
        }
    }

    private fun parseStoreProfile(obj: JSONObject): CloudStoreProfile {
        return CloudStoreProfile(
            storeId = obj.optString("store_id", ""),
            title = obj.optString("title", ""),
            subtitle = obj.optString("subtitle", ""),
            description = obj.optString("description", ""),
            address = obj.optString("address", ""),
            hours = obj.optString("hours", ""),
            mapUrl = obj.optString("map_url", ""),
            coverUrl = obj.optString("cover_url", ""),
            logoUrl = obj.optString("logo_url", ""),
            businessStatus = obj.optString("business_status", ""),
            extraContactsJson = obj.optString("extra_contacts_json", ""),
            servicesJson = obj.optString("services_json", ""),
            updatedAt = null,
        )
    }
    // =====================================================
    // Chat Cloud (Supabase) - 双模拟器对聊最小闭环
    // =====================================================

    data class CloudChatThreadSummary(
        val conversationId: String,
        val storeId: String,
        val clientId: String,
        val lastMessageId: String?,
        val lastText: String?,
        val lastTimeMs: Long?,
        val unreadCount: Int,
    )

    data class CloudChatMessage(
        val id: String,
        val conversationId: String,
        val storeId: String,
        val clientId: String,
        val role: String,        // "client" / "merchant"
        val direction: String,   // 你表里存的字符串（如 "Outgoing"/"Incoming"）
        val text: String,
        val timeMs: Long,
        val isRead: Boolean,
    )

    private fun chatConversationsTable(): String = try {
        ShowcaseCloudConfig.TABLE_CHAT_CONVERSATIONS
    } catch (_: Throwable) {
        "chat_conversations"
    }

    private fun chatMessagesTable(): String = try {
        ShowcaseCloudConfig.TABLE_CHAT_MESSAGES
    } catch (_: Throwable) {
        "chat_messages"
    }

    private fun chatThreadSummariesView(): String = try {
        ShowcaseCloudConfig.VIEW_CHAT_THREAD_SUMMARIES
    } catch (_: Throwable) {
        "chat_thread_summaries"
    }

    /**
     * 云端会话 ID（稳定 threadId）
     * 约定：conversationId = "cloud:<storeId>:<clientId>"
     */
    fun buildConversationId(storeId: String, clientId: String): String {
        val s = storeId.trim()
        val c = clientId.trim()
        return "cloud:$s:$c"
    }

    /**
     * upsert 会话：chat_conversations(on_conflict=conversation_id)
     */
    suspend fun upsertChatConversation(
        conversationId: String,
        storeId: String,
        clientId: String,
    ): Boolean = withContext(Dispatchers.IO) {
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) return@withContext false

        val cid = conversationId.trim()
        val sid = storeId.trim()
        val clid = clientId.trim()
        if (cid.isBlank() || sid.isBlank() || clid.isBlank()) return@withContext false

        val table = chatConversationsTable()
        val url = ShowcaseCloudConfig.restUrl("$table?on_conflict=conversation_id")

        val payload = JSONObject().apply {
            put("conversation_id", cid)
            put("store_id", sid)
            put("client_id", clid)
        }
        val arr = JSONArray().apply { put(payload) }

        val (code, _) = httpPost(
            url,
            JSONObject().apply { put("__raw_array__", arr.toString()) },
            prefer = "resolution=merge-duplicates,return=minimal"
        )
        code in 200..299
    }

    /**
     * 插入消息：chat_messages(on_conflict=id)
     */
    suspend fun insertChatMessage(
        msg: CloudChatMessage,
    ): Boolean = withContext(Dispatchers.IO) {
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) return@withContext false

        if (msg.id.isBlank() || msg.conversationId.isBlank() || msg.storeId.isBlank() || msg.clientId.isBlank()) {
            return@withContext false
        }

        val table = chatMessagesTable()
        val url = ShowcaseCloudConfig.restUrl("$table?on_conflict=id")

        val payload = JSONObject().apply {
            put("id", msg.id)
            put("conversation_id", msg.conversationId)
            put("store_id", msg.storeId)
            put("client_id", msg.clientId)
            put("role", msg.role)
            put("direction", msg.direction)
            put("text", msg.text)
            put("time_ms", msg.timeMs)
            put("is_read", msg.isRead)
        }
        val arr = JSONArray().apply { put(payload) }

        val (code, _) = httpPost(
            url,
            JSONObject().apply { put("__raw_array__", arr.toString()) },
            prefer = "resolution=merge-duplicates,return=minimal"
        )
        code in 200..299
    }

    /**
     * 拉取某个会话的消息列表（Chat 页）
     */
    suspend fun fetchChatMessages(
        storeId: String,
        conversationId: String,
        limit: Int = 200,
    ): List<CloudChatMessage> = withContext(Dispatchers.IO) {
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) return@withContext emptyList()

        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
        val cid = conversationId.trim()
        if (cid.isBlank()) return@withContext emptyList()

        val l = limit.coerceIn(1, 500)
        val table = chatMessagesTable()

        val url = ShowcaseCloudConfig.restUrl(
            "$table" +
                    "?store_id=eq.${urlEncode(currentStoreId)}" +
                    "&conversation_id=eq.${urlEncode(cid)}" +
                    "&select=id,conversation_id,store_id,client_id,role,direction,text,time_ms,is_read" +
                    "&order=time_ms.asc" +
                    "&limit=$l"
        )

        val (code, body) = httpGet(url)
        if (code !in 200..299 || body.isNullOrBlank()) return@withContext emptyList()

        val arr = JSONArray(body)
        val out = mutableListOf<CloudChatMessage>()
        for (i in 0 until arr.length()) {
            val obj = try { arr.getJSONObject(i) } catch (_: Throwable) { null } ?: continue
            out.add(
                CloudChatMessage(
                    id = obj.optString("id", ""),
                    conversationId = obj.optString("conversation_id", ""),
                    storeId = obj.optString("store_id", ""),
                    clientId = obj.optString("client_id", ""),
                    role = obj.optString("role", ""),
                    direction = obj.optString("direction", ""),
                    text = obj.optString("text", ""),
                    timeMs = obj.optLong("time_ms", 0L),
                    isRead = obj.optBoolean("is_read", false),
                )
            )
        }
        out
    }

    /**
     * 拉取商家的 ChatList（线程摘要视图）
     * 依赖 view：chat_thread_summaries
     *
     * 你的 view 当前列：
     *  - conversation_id, store_id, client_id, last_message_at, last_preview, updated_at
     */
    suspend fun fetchChatThreadSummaries(
        storeId: String,
        limit: Int = 200,
    ): List<CloudChatThreadSummary> = withContext(Dispatchers.IO) {
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) return@withContext emptyList()

        val sid = storeId.trim()
        if (sid.isBlank()) return@withContext emptyList()

        val l = limit.coerceIn(1, 500)
        val view = chatThreadSummariesView()

        val url = ShowcaseCloudConfig.restUrl(
            "$view" +
                    "?store_id=eq.${urlEncode(sid)}" +
                    "&select=conversation_id,store_id,client_id,last_message_at,last_preview,updated_at" +
                    "&order=last_message_at.desc.nullslast" +
                    "&limit=$l"
        )

        val (code, body) = httpGet(url)
        if (code !in 200..299 || body.isNullOrBlank()) return@withContext emptyList()

        val arr = JSONArray(body)
        val out = mutableListOf<CloudChatThreadSummary>()
        for (i in 0 until arr.length()) {
            val obj = try { arr.getJSONObject(i) } catch (_: Throwable) { null } ?: continue

            // 兼容：last_message_at 是 timestamptz 字符串；我们 MVP 不在这里转 time_ms
            val lastAtIso = obj.optString("last_message_at", "").takeIf { it.isNotBlank() }
            val lastPreview = obj.optString("last_preview", "").takeIf { it.isNotBlank() }

            out.add(
                CloudChatThreadSummary(
                    conversationId = obj.optString("conversation_id", ""),
                    storeId = obj.optString("store_id", ""),
                    clientId = obj.optString("client_id", ""),
                    lastMessageId = null,          // 你的 view 没有这个列（MVP 先空）
                    lastText = lastPreview,        // 用 last_preview 充当 lastText
                    lastTimeMs = null,             // 你的 view 没有 last_time_ms（MVP 先空）
                    unreadCount = 0,               // 你的 view 没有 unread_count（MVP 先 0）
                )
            )
        }
        out
    }


    /**
     * 商家打开某会话后：把该会话里“用户发来的未读”标记为已读
     * PATCH chat_messages set is_read=true where conversation_id=... and role='user' and is_read=false
     */
    suspend fun markUserMessagesRead(
        storeId: String,
        conversationId: String,
    ): Boolean = withContext(Dispatchers.IO) {
        val apiKey = ShowcaseCloudConfig.SUPABASE_ANON_KEY
        if (apiKey.isBlank()) return@withContext false

        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
        val cid = conversationId.trim()
        if (cid.isBlank()) return@withContext false

        val table = chatMessagesTable()
        val url = ShowcaseCloudConfig.restUrl(
            "$table" +
                    "?store_id=eq.${urlEncode(currentStoreId)}" +
                    "&conversation_id=eq.${urlEncode(cid)}" +
                    "&role=eq.client" +
                    "&is_read=eq.false"
        )

        val payload = JSONObject().apply { put("is_read", true) }
        val (code, _) = httpPatch(url, payload)
        code in 200..299
    }

    // ===== Header 组装逻辑统一收敛 =====

    private fun openConnection(
        urlString: String,
        method: String,
        isWrite: Boolean = false,
        doOutput: Boolean = false,
        connectTimeoutMs: Int = 10_000,
        readTimeoutMs: Int = 10_000,
        acceptJson: Boolean = true,
        contentType: String? = null,
        prefer: String? = null,
        extraHeaders: Map<String, String> = emptyMap(),
        actor: ShowcaseCloudConfig.AuthActor =
            if (method.equals("GET", ignoreCase = true)) {
                ShowcaseCloudConfig.AuthActor.PUBLIC
            } else {
                ShowcaseCloudConfig.AuthActor.MERCHANT
            },
        scopeStoreId: String = ShowcaseCloudConfig.currentStoreId(),
        scopeClientId: String? = null,
    ): HttpURLConnection {
        val url = URL(urlString)
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            this.doOutput = doOutput
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs

            val token = when (actor) {
                ShowcaseCloudConfig.AuthActor.PUBLIC -> ShowcaseCloudConfig.authToken(actor)
                ShowcaseCloudConfig.AuthActor.MERCHANT -> {
                    ShowcaseMerchantSessionManager.ensureValidMerchantAccessToken()
                        ?.trim()
                        .orEmpty()
                        .ifBlank {
                            ShowcaseStoreSession.currentMerchantAccessToken()?.trim().orEmpty()
                        }
                }
            }
            setRequestProperty(ShowcaseCloudConfig.Headers.API_KEY, ShowcaseCloudConfig.SUPABASE_ANON_KEY)

            if (actor == ShowcaseCloudConfig.AuthActor.MERCHANT && token.isNotBlank()) {
                setRequestProperty(
                    ShowcaseCloudConfig.Headers.AUTHORIZATION,
                    "Bearer $token"
                )
            }

            ShowcaseCloudConfig.requestScopeHeaders(
                storeId = scopeStoreId,
                clientId = scopeClientId
            ).forEach { (k, v) ->
                setRequestProperty(k, v)
            }

            if (acceptJson) {
                setRequestProperty("Accept", "application/json")
            }
            if (!contentType.isNullOrBlank()) {
                setRequestProperty("Content-Type", contentType)
            }
            if (!prefer.isNullOrBlank()) {
                setRequestProperty(ShowcaseCloudConfig.Headers.PREFER_RETURN_REPRESENTATION, prefer)
            }

            for ((k, v) in extraHeaders) {
                setRequestProperty(k, v)
            }
        }
    }

    private fun readResponseBody(conn: HttpURLConnection, code: Int): String? {
        return try {
            (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }

    private fun isJwtExpiredBody(body: String?): Boolean {
        val text = body?.trim().orEmpty()
        if (text.isBlank()) return false
        return text.contains("JWT expired", ignoreCase = true) ||
                text.contains("\"exp\" claim timestamp check failed", ignoreCase = true) ||
                text.contains("jwt expired", ignoreCase = true) ||
                text.contains("expired", ignoreCase = true) && text.contains("jwt", ignoreCase = true)
    }

    private fun shouldRetryWithMerchantRefresh(
        actor: ShowcaseCloudConfig.AuthActor,
        code: Int,
        body: String?
    ): Boolean {
        if (actor != ShowcaseCloudConfig.AuthActor.MERCHANT) return false
        if (code !in listOf(400, 401, 403)) return false
        return isJwtExpiredBody(body)
    }

    private fun refreshMerchantSessionIfNeeded(): Boolean {
        return try {
            val ok = ShowcaseMerchantSessionManager.forceRefreshMerchantSession()
            Log.d("ShowcaseCloud", "refreshMerchantSessionIfNeeded: success=$ok")
            ok
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "refreshMerchantSessionIfNeeded failed", e)
            false
        }
    }

    private fun openAuthConnection(
        urlString: String,
        method: String,
        bearerToken: String? = null,
        doOutput: Boolean = false,
        connectTimeoutMs: Int = 10_000,
        readTimeoutMs: Int = 10_000
    ): HttpURLConnection {
        val url = URL(urlString)
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            this.doOutput = doOutput
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("apikey", ShowcaseCloudConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            val token = bearerToken?.trim().orEmpty()
            if (token.isNotEmpty()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
        }
    }

    private fun httpAuthRequest(
        urlString: String,
        method: String,
        body: JSONObject?,
        bearerToken: String?
    ): Pair<Int, String?> {
        var conn: HttpURLConnection? = null
        return try {
            conn = openAuthConnection(
                urlString = urlString,
                method = method,
                bearerToken = bearerToken,
                doOutput = body != null
            )
            if (body != null) {
                conn.outputStream.use { os ->
                    os.write(body.toString().toByteArray(Charsets.UTF_8))
                    os.flush()
                }
            }
            val code = conn.responseCode
            val responseBody = readResponseBody(conn, code)
            code to responseBody
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "httpAuthRequest failed for $urlString", e)
            0 to null
        } finally {
            conn?.disconnect()
        }
    }
    private fun httpGet(
        urlString: String,
        actor: ShowcaseCloudConfig.AuthActor = ShowcaseCloudConfig.AuthActor.PUBLIC,
        scopeStoreId: String = ShowcaseCloudConfig.currentStoreId(),
        scopeClientId: String? = null
    ): Pair<Int, String?> {
        var conn: HttpURLConnection? = null
        return try {
            fun executeOnce(): Pair<Int, String?> {
                conn?.disconnect()
                conn = openConnection(
                    urlString = urlString,
                    method = "GET",
                    doOutput = false,
                    acceptJson = true,
                    contentType = null,
                    prefer = null,
                    actor = actor,
                    scopeStoreId = scopeStoreId,
                    scopeClientId = scopeClientId
                )
                val code = conn!!.responseCode
                val responseBody = readResponseBody(conn!!, code)
                return code to responseBody
            }

            var result = executeOnce()
            if (shouldRetryWithMerchantRefresh(actor, result.first, result.second)) {
                Log.w("ShowcaseCloud", "httpGet: merchant token expired, try refresh once. url=$urlString")
                val refreshed = refreshMerchantSessionIfNeeded()
                if (refreshed) {
                    result = executeOnce()
                }
            }
            result
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "httpGet: request failed for $urlString", e)
            0 to null
        } finally {
            conn?.disconnect()
        }
    }

    private fun httpPost(
        urlString: String,
        body: JSONObject,
        prefer: String? = null,
        actor: ShowcaseCloudConfig.AuthActor = ShowcaseCloudConfig.AuthActor.MERCHANT,
        scopeStoreId: String = ShowcaseCloudConfig.currentStoreId(),
        scopeClientId: String? = null
    ): Pair<Int, String?> {
        var conn: HttpURLConnection? = null
        return try {
            val rawArray = body.optString("__raw_array__", null)
            val payloadBytes = if (!rawArray.isNullOrBlank()) {
                rawArray.toByteArray(Charsets.UTF_8)
            } else {
                body.toString().toByteArray(Charsets.UTF_8)
            }

            fun executeOnce(): Pair<Int, String?> {
                conn?.disconnect()
                conn = openConnection(
                    urlString = urlString,
                    method = "POST",
                    isWrite = true,
                    doOutput = true,
                    acceptJson = true,
                    contentType = "application/json",
                    prefer = prefer,
                    actor = actor,
                    scopeStoreId = scopeStoreId,
                    scopeClientId = scopeClientId
                )

                conn!!.outputStream.use { os ->
                    os.write(payloadBytes)
                    os.flush()
                }

                val code = conn!!.responseCode
                val responseBody = readResponseBody(conn!!, code)
                return code to responseBody
            }

            var result = executeOnce()
            if (shouldRetryWithMerchantRefresh(actor, result.first, result.second)) {
                Log.w("ShowcaseCloud", "httpPost: merchant token expired, try refresh once. url=$urlString")
                val refreshed = refreshMerchantSessionIfNeeded()
                if (refreshed) {
                    result = executeOnce()
                }
            }
            result
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "httpPost: request failed for $urlString", e)
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
        prefer: String? = "return=minimal",
        actor: ShowcaseCloudConfig.AuthActor = ShowcaseCloudConfig.AuthActor.MERCHANT,
        scopeStoreId: String = ShowcaseCloudConfig.currentStoreId(),
        scopeClientId: String? = null
    ): Pair<Int, String?> {
        var conn: HttpURLConnection? = null
        return try {
            val payloadBytes = body.toString().toByteArray(Charsets.UTF_8)

            fun executeOnce(): Pair<Int, String?> {
                conn?.disconnect()
                conn = openConnection(
                    urlString = urlString,
                    method = "PATCH",
                    isWrite = true,
                    doOutput = true,
                    acceptJson = true,
                    contentType = "application/json",
                    prefer = prefer,
                    actor = actor,
                    scopeStoreId = scopeStoreId,
                    scopeClientId = scopeClientId
                )

                conn!!.outputStream.use { os ->
                    os.write(payloadBytes)
                    os.flush()
                }

                val code = conn!!.responseCode
                val responseBody = readResponseBody(conn!!, code)
                return code to responseBody
            }

            var result = executeOnce()
            if (shouldRetryWithMerchantRefresh(actor, result.first, result.second)) {
                Log.w("ShowcaseCloud", "httpPatch: merchant token expired, try refresh once. url=$urlString")
                val refreshed = refreshMerchantSessionIfNeeded()
                if (refreshed) {
                    result = executeOnce()
                }
            }
            result
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "httpPatch: request failed for $urlString", e)
            0 to null
        } finally {
            conn?.disconnect()
        }
    }

    private fun httpDelete(
        urlString: String,
        actor: ShowcaseCloudConfig.AuthActor = ShowcaseCloudConfig.AuthActor.MERCHANT
    ): Pair<Int, String?> {
        var conn: HttpURLConnection? = null
        return try {
            fun executeOnce(): Pair<Int, String?> {
                conn?.disconnect()
                conn = openConnection(
                    urlString = urlString,
                    method = "DELETE",
                    isWrite = true,
                    doOutput = false,
                    acceptJson = true,
                    contentType = "application/json",
                    prefer = null,
                    actor = actor
                )

                val code = conn!!.responseCode
                val responseBody = readResponseBody(conn!!, code)
                return code to responseBody
            }

            var result = executeOnce()
            if (shouldRetryWithMerchantRefresh(actor, result.first, result.second)) {
                Log.w("ShowcaseCloud", "httpDelete: merchant token expired, try refresh once. url=$urlString")
                val refreshed = refreshMerchantSessionIfNeeded()
                if (refreshed) {
                    result = executeOnce()
                }
            }
            result
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "httpDelete: request failed for $urlString", e)
            0 to null
        } finally {
            conn?.disconnect()
        }
    }
    suspend fun upsertPushDevice(device: PushDeviceUpsert): Boolean = withContext(Dispatchers.IO) {
        val table = pushDevicesTable()

        val usePublicActor =
            when (device.audience) {
                "announcement_subscriber" -> true
                "chat_client" -> !device.clientId.isNullOrBlank()
                else -> false
            }

        val url = if (usePublicActor) {
            ShowcaseCloudConfig.restUrl(table)
        } else {
            ShowcaseCloudConfig.restUrl(
                "$table?on_conflict=store_id,token,audience,conversation_scope"
            )
        }

        val body = JSONObject().apply {
            put("store_id", device.storeId)
            put("audience", device.audience)
            put("token", device.token)
            put("conversation_id", device.conversationId)
            put("client_id", device.clientId)
            put("platform", device.platform)
            put("app_version", device.appVersion)
        }

        Log.d("NDJC_PUSH", "upsertPushDevice url=$url")
        Log.d("NDJC_PUSH", "upsertPushDevice body=$body")

        val codeAndBody = httpPost(
            urlString = url,
            body = body,
            prefer = if (usePublicActor) {
                "return=minimal"
            } else {
                "resolution=merge-duplicates,return=minimal"
            },
            actor = if (usePublicActor) {
                ShowcaseCloudConfig.AuthActor.PUBLIC
            } else {
                ShowcaseCloudConfig.AuthActor.MERCHANT
            },
            scopeStoreId = device.storeId,
            scopeClientId = if (device.audience == "chat_client" && usePublicActor) {
                device.clientId
            } else {
                null
            }
        )

        val code = codeAndBody.first
        val resp = codeAndBody.second

        Log.d(
            "NDJC_PUSH",
            "upsertPushDevice actor=${if (usePublicActor) "PUBLIC" else "MERCHANT"} scopeStoreId=${device.storeId} scopeClientId=${if (device.audience == "chat_client" && usePublicActor) device.clientId else null}"
        )
        Log.d("NDJC_PUSH", "upsertPushDevice code=$code")
        Log.d("NDJC_PUSH", "upsertPushDevice resp=$resp")

        code in 200..299 || code == 409
    }

    suspend fun dispatchChatPush(
        storeId: String,
        conversationId: String,
        targetAudience: String,
        senderRole: String,
        senderName: String,
        bodyPreview: String,
        senderClientId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val url = ShowcaseCloudConfig.functionUrl(ShowcaseCloudConfig.EDGE_PUSH_DISPATCH)
        val openAs =
            when (targetAudience.trim().lowercase()) {
                "chat_merchant" -> "merchant"
                "chat_client" -> "client"
                else -> ""
            }

        val payload = JSONObject().apply {
            put("type", "chat")
            put("audience", targetAudience)
            put("store_id", storeId)
            put("conversation_id", conversationId)
            put("sender_role", senderRole)
            if (openAs.isNotBlank()) {
                put("open_as", openAs)
            }
            put("title", senderName.ifBlank { "New message" })
            put("body", bodyPreview.ifBlank { "Sent you a new message" })
        }

        val actor =
            if (senderRole == "merchant") {
                ShowcaseCloudConfig.AuthActor.MERCHANT
            } else {
                ShowcaseCloudConfig.AuthActor.PUBLIC
            }

        val codeAndBody = httpPost(
            urlString = url,
            body = payload,
            prefer = null,
            actor = actor,
            scopeStoreId = storeId,
            scopeClientId = if (actor == ShowcaseCloudConfig.AuthActor.PUBLIC) senderClientId else null
        )
        val code = codeAndBody.first
        val resp = codeAndBody.second
        Log.d("NDJC_PUSH", "dispatchChatPush actor=$actor scopeStoreId=$storeId scopeClientId=${if (actor == ShowcaseCloudConfig.AuthActor.PUBLIC) senderClientId else null}")
        Log.d("NDJC_PUSH", "dispatchChatPush url=$url")
        Log.d("NDJC_PUSH", "dispatchChatPush payload=$payload")
        Log.d("NDJC_PUSH", "dispatchChatPush code=$code")
        Log.d("NDJC_PUSH", "dispatchChatPush resp=$resp")
        code in 200..299
    }
    suspend fun incrementDishClickCount(
        storeId: String,
        dishId: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (dishId.isBlank()) return@withContext false
        try {
            val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
            val url = ShowcaseCloudConfig.restUrl("rpc/ndjc_inc_dish_click_count")
            val payload = JSONObject().apply {
                put("p_store_id", currentStoreId)
                put("p_dish_id", dishId)
            }
            val (code, body) = httpPost(
                urlString = url,
                body = payload,
                prefer = "return=minimal",
                actor = ShowcaseCloudConfig.AuthActor.PUBLIC
            )
            Log.d("ShowcaseCloud", "incrementDishClickCount: code=$code body=$body")
            code in 200..299
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "incrementDishClickCount failed", e)
            false
        }
    }

    suspend fun incrementAnnouncementViewCount(
        storeId: String,
        announcementId: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (announcementId.isBlank()) return@withContext false
        try {
            val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
            val trimmedAnnouncementId = announcementId.trim()
            val url = ShowcaseCloudConfig.restUrl("rpc/ndjc_inc_announcement_view_count")
            val payload = JSONObject().apply {
                put("p_store_id", currentStoreId)
                put("p_announcement_id", trimmedAnnouncementId)
            }

            Log.d(
                "NDJC_CLICK",
                "increment start storeId=$currentStoreId announcementId=$trimmedAnnouncementId url=$url payload=$payload"
            )

            val (code, body) = httpPost(
                urlString = url,
                body = payload,
                prefer = "return=minimal",
                actor = ShowcaseCloudConfig.AuthActor.PUBLIC
            )

            Log.d(
                "NDJC_CLICK",
                "increment result code=$code body=$body storeId=$currentStoreId announcementId=$trimmedAnnouncementId"
            )

            val success = code in 200..299
            if (!success) {
                Log.e(
                    "NDJC_CLICK",
                    "increment FAILED code=$code body=$body storeId=$currentStoreId announcementId=$trimmedAnnouncementId"
                )
            }

            success
        } catch (e: Exception) {
            Log.e("NDJC_CLICK", "incrementAnnouncementViewCount failed", e)
            false
        }
    }
    suspend fun signInMerchant(
        loginName: String,
        password: String
    ): MerchantAuthSession? = withContext(Dispatchers.IO) {
        val email = loginName.trim()
        val pwd = password.trim()
        if (email.isBlank() || pwd.isBlank()) return@withContext null

        try {
            val url = ShowcaseCloudConfig.authUrl("token?grant_type=password")
            val payload = JSONObject().apply {
                put("email", email)
                put("password", pwd)
            }

            val (code, body) = httpAuthRequest(
                urlString = url,
                method = "POST",
                body = payload,
                bearerToken = null
            )
            if (code !in 200..299 || body.isNullOrBlank()) return@withContext null

            val obj = JSONObject(body)
            val accessToken = obj.optString("access_token", "").trim()
            val refreshToken = obj.optString("refresh_token", "").trim().ifBlank { null }
            val authUserId = obj.optJSONObject("user")
                ?.optString("id", "")
                ?.trim()
                .orEmpty()
            val expiresIn = obj.optLong("expires_in", 3600L).coerceAtLeast(1L)
            val expiresAt = (System.currentTimeMillis() / 1000L) + expiresIn

            if (accessToken.isBlank() || authUserId.isBlank()) return@withContext null

            MerchantAuthSession(
                accessToken = accessToken,
                refreshToken = refreshToken,
                authUserId = authUserId,
                loginName = email,
                expiresAt = expiresAt
            )
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "signInMerchant failed", e)
            null
        }
    }

    suspend fun fetchMerchantBindingForCurrentStore(
        storeId: String
    ): MerchantStoreMembership? = withContext(Dispatchers.IO) {
        try {
            val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
            val table = "store_memberships"
            val url = ShowcaseCloudConfig.restUrl(
                "$table?select=store_id,auth_user_id,login_name&store_id=eq.${urlEncode(currentStoreId)}&limit=1"
            )

            val (code, body) = httpGet(
                urlString = url,
                actor = ShowcaseCloudConfig.AuthActor.MERCHANT,
                scopeStoreId = currentStoreId
            )
            if (code !in 200..299 || body.isNullOrBlank()) return@withContext null

            val arr = JSONArray(body)
            if (arr.length() <= 0) return@withContext null

            val obj = arr.optJSONObject(0) ?: return@withContext null

            MerchantStoreMembership(
                storeId = obj.optString("store_id", "").trim(),
                authUserId = obj.optString("auth_user_id", "").trim(),
                loginName = obj.optString("login_name", "").trim().ifBlank { null }
            )
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "fetchMerchantBindingForCurrentStore failed", e)
            null
        }
    }

    suspend fun fetchMerchantBindingForStoreAndAuthUser(
        storeId: String,
        authUserId: String
    ): MerchantStoreMembership? = withContext(Dispatchers.IO) {
        val currentStoreId = storeId.trim()
        val currentAuthUserId = authUserId.trim()
        if (currentStoreId.isBlank() || currentAuthUserId.isBlank()) return@withContext null

        try {
            val table = "store_memberships"
            val url = ShowcaseCloudConfig.restUrl(
                "$table?select=store_id,auth_user_id,login_name&store_id=eq.${urlEncode(currentStoreId)}&auth_user_id=eq.${urlEncode(currentAuthUserId)}&limit=1"
            )

            val (code, body) = httpGet(
                urlString = url,
                actor = ShowcaseCloudConfig.AuthActor.MERCHANT,
                scopeStoreId = currentStoreId
            )
            if (code !in 200..299 || body.isNullOrBlank()) return@withContext null

            val arr = JSONArray(body)
            if (arr.length() <= 0) return@withContext null

            val obj = arr.optJSONObject(0) ?: return@withContext null

            MerchantStoreMembership(
                storeId = obj.optString("store_id", "").trim(),
                authUserId = obj.optString("auth_user_id", "").trim(),
                loginName = obj.optString("login_name", "").trim().ifBlank { null }
            )
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "fetchMerchantBindingForStoreAndAuthUser failed", e)
            null
        }
    }

    suspend fun updateMerchantPassword(
        newPassword: String
    ): Boolean = withContext(Dispatchers.IO) {
        val next = newPassword.trim()
        if (next.length < 4) return@withContext false

        try {
            val url = ShowcaseCloudConfig.authUrl("user")
            val payload = JSONObject().apply {
                put("password", next)
            }

            val bearerToken = ShowcaseMerchantSessionManager.ensureValidMerchantAccessToken()
                ?: return@withContext false

            val (code, body) = httpAuthRequest(
                urlString = url,
                method = "PUT",
                body = payload,
                bearerToken = bearerToken
            )
            lastMerchantAuthCode = code
            lastMerchantAuthBody = body
            Log.d("ShowcaseCloud", "updateMerchantPassword code=$code body=$body")
            code in 200..299
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "updateMerchantPassword failed", e)
            false
        }
    }

    suspend fun updateMerchantLoginName(
        storeId: String,
        newLoginName: String
    ): Boolean = withContext(Dispatchers.IO) {
        val next = newLoginName.trim()
        if (next.isBlank()) return@withContext false

        val currentStoreId = ShowcaseCloudConfig.requireStoreId(storeId)
        val authUserId = ShowcaseStoreSession.currentMerchantAuthUserId()?.trim().orEmpty()
        if (authUserId.isBlank()) return@withContext false

        try {
            val authUrl = ShowcaseCloudConfig.authUrl("user")
            val authPayload = JSONObject().apply {
                put("email", next)
            }
            val bearerToken = ShowcaseMerchantSessionManager.ensureValidMerchantAccessToken()
                ?: return@withContext false

            val (authCode, authBody) = httpAuthRequest(
                urlString = authUrl,
                method = "PUT",
                body = authPayload,
                bearerToken = bearerToken
            )
            lastMerchantAuthCode = authCode
            lastMerchantAuthBody = authBody
            Log.d("ShowcaseCloud", "updateMerchantLoginName auth code=$authCode body=$authBody")
            if (authCode !in 200..299) return@withContext false

            val restUrl = ShowcaseCloudConfig.restUrl(
                "store_memberships?auth_user_id=eq.${urlEncode(authUserId)}&store_id=eq.${urlEncode(currentStoreId)}"
            )
            val payload = JSONObject().apply {
                put("login_name", next)
            }
            val (code, body) = httpPatch(
                urlString = restUrl,
                body = payload,
                prefer = "return=minimal",
                actor = ShowcaseCloudConfig.AuthActor.MERCHANT
            )
            lastMerchantAuthCode = code
            lastMerchantAuthBody = body
            Log.d("ShowcaseCloud", "updateMerchantLoginName membership code=$code body=$body")
            code in 200..299
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "updateMerchantLoginName failed", e)
            false
        }
    }
    suspend fun dispatchAnnouncementPush(
        storeId: String,
        announcementId: String,
        bodyPreview: String
    ): Boolean = withContext(Dispatchers.IO) {
        val url = ShowcaseCloudConfig.functionUrl(ShowcaseCloudConfig.EDGE_PUSH_DISPATCH)
        val payload = JSONObject().apply {
            put("type", "announcement")
            put("audience", "announcement_subscriber")
            put("store_id", storeId)
            put("announcement_id", announcementId)
            put("title", "New announcement")
            put("body", bodyPreview.ifBlank { "Posted a new announcement" })
        }
        val codeAndBody = httpPost(
            urlString = url,
            body = payload,
            prefer = null
        )
        val code = codeAndBody.first
        val resp = codeAndBody.second
        lastAnnouncementPushCode = code
        lastAnnouncementPushBody = resp
        Log.d("NDJC_PUSH", "dispatchAnnouncementPush url=$url")
        Log.d("NDJC_PUSH", "dispatchAnnouncementPush payload=$payload")
        Log.d("NDJC_PUSH", "dispatchAnnouncementPush code=$code")
        Log.d("NDJC_PUSH", "dispatchAnnouncementPush resp=$resp")
        code in 200..299
    }
    private fun httpPutBytes(
        urlString: String,
        bytes: ByteArray,
        contentType: String,
        actor: ShowcaseCloudConfig.AuthActor = ShowcaseCloudConfig.AuthActor.MERCHANT
    ): Pair<Int, String?> {
        var conn: HttpURLConnection? = null
        return try {
            fun executeOnce(): Pair<Int, String?> {
                conn?.disconnect()
                conn = openConnection(
                    urlString = urlString,
                    method = "PUT",
                    isWrite = true,
                    doOutput = true,
                    connectTimeoutMs = 15_000,
                    readTimeoutMs = 15_000,
                    acceptJson = false,
                    contentType = contentType,
                    prefer = null,
                    extraHeaders = mapOf(
                        "x-upsert" to "true"
                    ),
                    actor = actor
                )

                conn!!.outputStream.use { os ->
                    os.write(bytes)
                    os.flush()
                }

                val code = conn!!.responseCode
                val responseBody = readResponseBody(conn!!, code)
                return code to responseBody
            }

            var result = executeOnce()
            if (shouldRetryWithMerchantRefresh(actor, result.first, result.second)) {
                Log.w("ShowcaseCloud", "httpPutBytes: merchant token expired, try refresh once. url=$urlString")
                val refreshed = refreshMerchantSessionIfNeeded()
                if (refreshed) {
                    result = executeOnce()
                }
            }
            result
        } catch (e: Exception) {
            Log.e("ShowcaseCloud", "httpPutBytes failed for $urlString", e)
            0 to null
        } finally {
            conn?.disconnect()
        }
    }

    private fun urlEncode(v: String): String {
        return try {
            URLEncoder.encode(v, "UTF-8")
        } catch (_: Exception) {
            v
        }
    }
}
