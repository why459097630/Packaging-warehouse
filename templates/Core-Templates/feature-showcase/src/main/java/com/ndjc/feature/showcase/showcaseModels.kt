package com.ndjc.feature.showcase

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * 同步状态：为“离线可用 + 云端同步 + 失败可重试 + merge”预留
 * - 当前 ViewModel/Repository 可以暂不使用；后续补齐同步引擎时直接接入。
 */
enum class SyncState {
    Synced,
    Pending,
    Failed
}

/**
 * 列表排序模式：展示型标配（搜索/筛选/排序）
 * - 具体排序规则后续在 ViewModel/QueryEngine 落地。
 */
enum class ShowcaseSortMode {
    Price,
    Name
}

enum class ShowcaseCloudPlanType {
    Trial,
    Paid,
    Unknown
}

enum class ShowcaseCloudServiceStatus {
    Active,
    ReadOnly,
    Deleted,
    Unknown
}

data class ShowcaseCloudStatus(
    val storeId: String = "",
    val planType: ShowcaseCloudPlanType = ShowcaseCloudPlanType.Unknown,
    val serviceStatus: ShowcaseCloudServiceStatus = ShowcaseCloudServiceStatus.Unknown,
    val serviceEndAt: String? = null,
    val deleteAt: String? = null,
    val canWrite: Boolean = true,
    val lastSyncAtMs: Long? = null
)

/**
 * 展示型“店铺/项目”信息：用于 About/Contact/Map/营业时间/社媒链接 等
 * - 当前只定义模型；后续 UI 包可选择是否展示。
 */
data class ExtraContact(
    val name: String = "",
    val value: String = ""
)

data class StoreProfile(
    val title: String = "",
    val subtitle: String = "",
    val description: String = "",
    val services: List<String> = emptyList(),
    val address: String = "",
    val hours: String = "",
    val mapUrl: String = "",
    // ✅ 新增：自定义联系方式（Name + Value）
    val extraContacts: List<ExtraContact> = emptyList(),

    val coverUrl: String = "",
    val logoUrl: String = "",
    val businessStatus: String = ""
)


fun encodeExtraContactsJson(items: List<ExtraContact>): String {
    return try {
        val arr = JSONArray()
        items.forEach {
            val o = JSONObject()
            o.put("name", it.name)
            o.put("value", it.value)
            arr.put(o)
        }
        arr.toString()
    } catch (_: Exception) {
        "[]"
    }
}

fun decodeExtraContactsJson(json: String): List<ExtraContact> {
    return try {
        val arr = JSONArray(json.ifBlank { "[]" })
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("name", "").trim()
                val value = o.optString("value", "").trim()
                if (name.isNotBlank() && value.isNotBlank()) add(ExtraContact(name, value))
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

/**
 * 线索表单：展示型“转化”必备（最简）
 */
data class Lead(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val message: String = "",
    val createdAt: Long = 0L,
    val sourceDishId: String? = null,
)

data class DemoDish(
    val clickCount: Int = 0,
    val id: String,
    val nameZh: String,
    val nameEn: String,
    val descriptionEn: String,
    val category: String,
    val originalPrice: Float,
    val discountPrice: Float? = null,
    val isRecommended: Boolean = false,
    val isSoldOut: Boolean = false,
    val isHidden: Boolean = false,
    val imageResId: Int? = null,
    val imageUri: Uri? = null,
    val imageUrls: List<String> = emptyList(),

    /**
     * 展示型必备扩展字段（保持默认值，避免破坏现有调用）
     */
    val tags: List<String> = emptyList(),

    val externalLink: String? = null,

    /**
     * 同步/一致性所需字段（默认不启用也不影响）
     */
    val updatedAt: Long = 0L,
    val syncState: SyncState = SyncState.Synced,
    val dirty: Boolean = false,
)

/**
 * 兜底菜单：现在为空，不再强制插入 Demo Dish。
 */
val initialDishes: List<DemoDish> = emptyList()

/**
 * 统一计算分类：菜品里出现过的分类 + 纯手动分类
 */
internal fun deriveCategories(
    dishes: List<DemoDish>,
    manualCategories: List<String>
): List<String> {

    val fromDishes = dishes
        .mapNotNull { it.category.takeIf { c -> c.isNotBlank() } }
        .toSet()
    val extra = manualCategories
        .filter { it.isNotBlank() && it !in fromDishes }
        .toSet()
    return (fromDishes + extra).sorted()
}
/**
 * 统一计算 tags：去空/trim/去重/排序
 */
internal fun deriveAllTags(dishes: List<DemoDish>): List<String> {
    return dishes
        .asSequence()
        .flatMap { it.tags.asSequence() }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
        .toList()
}


private const val PREF_NAME = "Showcase_demo_prefs"
private const val KEY_DISHES_JSON = "dishes_json"
private const val KEY_STORE_PROFILE_JSON = "store_profile_json"
private const val KEY_MANUAL_CATEGORIES_JSON = "manual_categories_json"
private const val KEY_PUBLISHED_ANNOUNCEMENTS_JSON = "published_announcements_json"
private const val KEY_ADMIN_ANNOUNCEMENT_EDITOR_DRAFT_JSON = "admin_announcement_editor_draft_json"
private const val KEY_ITEM_EDITOR_DRAFT_JSON = "item_editor_draft_json"
private const val KEY_VIEWED_ANNOUNCEMENT_IDS_JSON = "viewed_announcement_ids_json"
private const val KEY_COUNTED_ANNOUNCEMENT_CLICK_IDS_JSON = "counted_announcement_click_ids_json"
private const val KEY_FAVORITE_IDS_JSON = "favorite_ids_json"
private const val KEY_FAVORITE_ADDED_AT_JSON = "favorite_added_at_json"

/**
 * 从 SharedPreferences 读取本地菜品缓存
 */
internal fun loadDishesFromStorage(context: Context): List<DemoDish> {
    return try {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_DISHES_JSON, null) ?: return emptyList()
        val root = JSONObject(json)
        val arr = root.optJSONArray("dishes") ?: return emptyList()
        val list = mutableListOf<DemoDish>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)

            val tags = mutableListOf<String>()
            val tagsArr = obj.optJSONArray("tags")
            if (tagsArr != null) {
                for (t in 0 until tagsArr.length()) {
                    val v = tagsArr.optString(t)
                    if (v.isNotBlank()) tags.add(v)
                }
            }

            val syncStateStr = obj.optString("syncState", SyncState.Synced.name)
            val syncState = try {
                SyncState.valueOf(syncStateStr)
            } catch (_: Exception) {
                SyncState.Synced
            }

            list.add(
                DemoDish(
                    clickCount = obj.optInt("clickCount", 0),
                    id = obj.optString("id"),
                    nameZh = obj.optString("nameZh"),
                    nameEn = obj.optString("nameEn"),
                    descriptionEn = obj.optString("descriptionEn"),
                    category = obj.optString("category"),
                    originalPrice = obj.optDouble("originalPrice", 0.0).toFloat(),
                    discountPrice = if (obj.isNull("discountPrice")) null
                    else obj.optDouble("discountPrice").toFloat(),
                    isRecommended = obj.optBoolean("isRecommended", false),
                    isSoldOut = obj.optBoolean("isSoldOut", false),
                    isHidden = obj.optBoolean("isHidden", false),
                    imageResId = null,

// ✅ 方案 B：多图优先（imageUrls），同时兼容旧字段 imageUri
                    imageUrls = run {
                        val arr = obj.optJSONArray("imageUrls")
                        val list = mutableListOf<String>()
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val v = arr.optString(i)
                                if (!v.isNullOrBlank() && v != "null") list.add(v)
                            }
                        }
                        if (list.isNotEmpty()) list
                        else {
                            // legacy fallback
                            obj.optString("imageUri", null)
                                ?.takeIf { it.isNotBlank() && it != "null" }
                                ?.let { listOf(it) }
                                ?: emptyList()
                        }
                    },

// legacy：保留第一张图用于旧 UI（未来可删除）
                    imageUri = run {
                        val first = obj.optJSONArray("imageUrls")?.optString(0)
                        if (!first.isNullOrBlank() && first != "null") Uri.parse(first)
                        else obj.optString("imageUri", null)
                            ?.takeIf { it.isNotBlank() && it != "null" }
                            ?.let { Uri.parse(it) }
                    },

                    tags = tags,

                    externalLink = obj.optString("externalLink", null)?.takeIf { it.isNotBlank() && it != "null" },
                    updatedAt = obj.optLong("updatedAt", 0L),
                    syncState = syncState,
                    dirty = obj.optBoolean("dirty", false),
                )
            )
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

/**
 * 把当前菜品列表写入 SharedPreferences
 */
internal fun saveDishesToStorage(context: Context, dishes: List<DemoDish>) {
    try {
        val root = JSONObject()
        val arr = JSONArray()
        dishes.forEach { dish ->
            val obj = JSONObject()
            obj.put("id", dish.id)
            obj.put("nameZh", dish.nameZh)
            obj.put("nameEn", dish.nameEn)
            obj.put("descriptionEn", dish.descriptionEn)
            obj.put("category", dish.category)
            obj.put("originalPrice", dish.originalPrice)
            if (dish.discountPrice != null) {
                obj.put("discountPrice", dish.discountPrice)
            } else {
                obj.put("discountPrice", JSONObject.NULL)
            }
            obj.put("isRecommended", dish.isRecommended)
            obj.put("isSoldOut", dish.isSoldOut)
            obj.put("isHidden", dish.isHidden)
            obj.put("clickCount", dish.clickCount)

// ✅ 方案 B：多图优先写入 imageUrls；同时保留旧字段 imageUri（兼容旧版本）
            val urls = dish.imageUrls.filter { it.isNotBlank() }.distinct().take(9)
            val urlArr = JSONArray()
            urls.forEach { urlArr.put(it) }
            obj.put("imageUrls", urlArr)

// legacy：第一张图写入 imageUri，确保旧读取逻辑还能显示
            obj.put("imageUri", urls.firstOrNull() ?: dish.imageUri?.toString() ?: JSONObject.NULL)

// 新增字段（保持兼容）
            val tagsArr = JSONArray()

            dish.tags.forEach { t ->
                if (t.isNotBlank()) tagsArr.put(t)
            }
            obj.put("tags", tagsArr)
            obj.put("externalLink", dish.externalLink ?: JSONObject.NULL)
            obj.put("updatedAt", dish.updatedAt)
            obj.put("syncState", dish.syncState.name)
            obj.put("dirty", dish.dirty)

            arr.put(obj)
        }
        root.put("dishes", arr)

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_DISHES_JSON, root.toString())
            .apply()
    } catch (_: Exception) {
        // 忽略持久化错误，不影响运行
    }
}

/**
 * StoreProfile 本地缓存：展示型“About/Contact”基础信息
 * - 当前不影响现有逻辑；后续 ViewModel/Repo 接入即可。
 */
internal fun loadStoreProfileFromStorage(context: Context): StoreProfile? {
    return try {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_STORE_PROFILE_JSON, null) ?: return null
        val obj = JSONObject(json)
        StoreProfile(
            title = obj.optString("title", ""),
            subtitle = obj.optString("subtitle", ""),
            description = obj.optString("description", ""),
            address = obj.optString("address", ""),
            hours = obj.optString("hours", ""),
            mapUrl = obj.optString("mapUrl", ""),
            extraContacts = decodeExtraContactsJson(obj.optString("extraContactsJson", "[]")),
            coverUrl = obj.optString("coverUrl", ""),
            logoUrl = obj.optString("logoUrl", ""),
            businessStatus = obj.optString("businessStatus", "")
        )
    } catch (_: Exception) {
        null
    }
}

internal fun saveStoreProfileToStorage(context: Context, profile: StoreProfile) {
    try {
        val obj = JSONObject()
        obj.put("title", profile.title)
        obj.put("subtitle", profile.subtitle)
        obj.put("description", profile.description)
        obj.put("address", profile.address)
        obj.put("hours", profile.hours)
        obj.put("mapUrl", profile.mapUrl)
        obj.put("extraContactsJson", encodeExtraContactsJson(profile.extraContacts))
        obj.put("coverUrl", profile.coverUrl)
        obj.put("logoUrl", profile.logoUrl)
        obj.put("businessStatus", profile.businessStatus)

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_STORE_PROFILE_JSON, obj.toString())
            .apply()
    } catch (_: Exception) {
        // 忽略持久化错误，不影响运行
    }
}
internal fun loadManualCategoriesFromStorage(context: Context): List<String> {
    return try {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_MANUAL_CATEGORIES_JSON, null) ?: return emptyList()
        val arr = JSONArray(json)
        buildList {
            for (i in 0 until arr.length()) {
                val v = arr.optString(i, "").trim()
                if (v.isNotBlank()) add(v)
            }
        }.distinct()
    } catch (_: Exception) {
        emptyList()
    }
}

internal fun saveManualCategoriesToStorage(context: Context, categories: List<String>) {
    try {
        val arr = JSONArray()
        categories
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { arr.put(it) }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_MANUAL_CATEGORIES_JSON, arr.toString())
            .apply()
    } catch (_: Exception) {
        // ignore
    }
}
internal data class CachedPublishedAnnouncement(
    val id: String,
    val coverUrl: String?,
    val body: String,
    val updatedAt: Long,
    val viewCount: Int
)

internal fun loadPublishedAnnouncementsFromStorage(context: Context): List<CachedPublishedAnnouncement> {
    return try {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PUBLISHED_ANNOUNCEMENTS_JSON, null) ?: return emptyList()
        val arr = JSONArray(json)
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").trim()
                val body = obj.optString("body", "").trim()
                if (id.isBlank() || body.isBlank()) continue
                add(
                    CachedPublishedAnnouncement(
                        id = id,
                        coverUrl = obj.optString("coverUrl", "").ifBlank { null },
                        body = body,
                        updatedAt = obj.optLong("updatedAt", 0L),
                        viewCount = obj.optInt("viewCount", 0)
                    )
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

internal fun savePublishedAnnouncementsToStorage(
    context: Context,
    items: List<CachedPublishedAnnouncement>
) {
    try {
        val arr = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("coverUrl", item.coverUrl ?: JSONObject.NULL)
            obj.put("body", item.body)
            obj.put("updatedAt", item.updatedAt)
            obj.put("viewCount", item.viewCount)
            arr.put(obj)
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PUBLISHED_ANNOUNCEMENTS_JSON, arr.toString())
            .apply()
    } catch (_: Exception) {
        // ignore
    }
}

internal fun loadViewedAnnouncementIdsFromStorage(context: Context): Set<String> {
    return try {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_VIEWED_ANNOUNCEMENT_IDS_JSON, null) ?: return emptySet()
        val arr = JSONArray(json)
        buildSet {
            for (i in 0 until arr.length()) {
                val id = arr.optString(i, "").trim()
                if (id.isNotBlank()) {
                    add(id)
                }
            }
        }
    } catch (_: Exception) {
        emptySet()
    }
}

internal fun saveViewedAnnouncementIdsToStorage(
    context: Context,
    ids: Set<String>
) {
    try {
        val arr = JSONArray()
        ids.sorted().forEach { id ->
            if (id.isNotBlank()) {
                arr.put(id)
            }
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_VIEWED_ANNOUNCEMENT_IDS_JSON, arr.toString())
            .apply()
    } catch (_: Exception) {
        // ignore
    }
}

internal fun loadCountedAnnouncementClickIdsFromStorage(context: Context): Set<String> {
    return try {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_COUNTED_ANNOUNCEMENT_CLICK_IDS_JSON, null) ?: return emptySet()
        val arr = JSONArray(json)
        buildSet {
            for (i in 0 until arr.length()) {
                val id = arr.optString(i, "").trim()
                if (id.isNotBlank()) {
                    add(id)
                }
            }
        }
    } catch (_: Exception) {
        emptySet()
    }
}

internal fun saveCountedAnnouncementClickIdsToStorage(
    context: Context,
    ids: Set<String>
) {
    try {
        val arr = JSONArray()
        ids.sorted().forEach { id ->
            if (id.isNotBlank()) {
                arr.put(id)
            }
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_COUNTED_ANNOUNCEMENT_CLICK_IDS_JSON, arr.toString())
            .apply()
    } catch (_: Exception) {
        // ignore
    }
}

internal fun loadFavoriteIdsFromStorage(context: Context): Set<String> {
    return try {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_FAVORITE_IDS_JSON, null) ?: return emptySet()
        val arr = JSONArray(json)
        buildSet {
            for (i in 0 until arr.length()) {
                val id = arr.optString(i, "").trim()
                if (id.isNotBlank()) {
                    add(id)
                }
            }
        }
    } catch (_: Exception) {
        emptySet()
    }
}

internal fun saveFavoriteIdsToStorage(
    context: Context,
    ids: Set<String>
) {
    try {
        val arr = JSONArray()
        ids.sorted().forEach { id ->
            if (id.isNotBlank()) {
                arr.put(id)
            }
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_FAVORITE_IDS_JSON, arr.toString())
            .apply()
    } catch (_: Exception) {
        // ignore
    }
}

internal fun loadFavoriteAddedAtFromStorage(context: Context): Map<String, Long> {
    return try {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_FAVORITE_ADDED_AT_JSON, null) ?: return emptyMap()
        val obj = JSONObject(json)
        buildMap {
            val keys = obj.keys()
            while (keys.hasNext()) {
                val id = keys.next().trim()
                if (id.isBlank()) continue
                val ts = obj.optLong(id, 0L)
                if (ts > 0L) {
                    put(id, ts)
                }
            }
        }
    } catch (_: Exception) {
        emptyMap()
    }
}

internal fun saveFavoriteAddedAtToStorage(
    context: Context,
    value: Map<String, Long>
) {
    try {
        val obj = JSONObject()
        value.forEach { (id, ts) ->
            val key = id.trim()
            if (key.isNotBlank() && ts > 0L) {
                obj.put(key, ts)
            }
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_FAVORITE_ADDED_AT_JSON, obj.toString())
            .apply()
    } catch (_: Exception) {
        // ignore
    }
}

internal data class CachedAdminAnnouncementEditorDraft(
    val editingId: String?,
    val body: String
)

internal fun loadAdminAnnouncementEditorDraftFromStorage(
    context: Context
): CachedAdminAnnouncementEditorDraft? {
    return try {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ADMIN_ANNOUNCEMENT_EDITOR_DRAFT_JSON, null) ?: return null
        val obj = JSONObject(json)

        val body = obj.optString("body", "")
        val editingId = obj.optString("editingId", "").ifBlank { null }

        if (body.isBlank() && editingId == null) {
            null
        } else {
            CachedAdminAnnouncementEditorDraft(
                editingId = editingId,
                body = body
            )
        }
    } catch (_: Exception) {
        null
    }
}

internal fun saveAdminAnnouncementEditorDraftToStorage(
    context: Context,
    draft: CachedAdminAnnouncementEditorDraft
) {
    try {
        val obj = JSONObject()
        obj.put("editingId", draft.editingId ?: JSONObject.NULL)
        obj.put("body", draft.body)

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_ADMIN_ANNOUNCEMENT_EDITOR_DRAFT_JSON, obj.toString())
            .commit()
    } catch (_: Exception) {
        // ignore
    }
}



internal fun clearAdminAnnouncementEditorDraftFromStorage(context: Context) {
    try {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_ADMIN_ANNOUNCEMENT_EDITOR_DRAFT_JSON)
            .commit()
    } catch (_: Exception) {
        // ignore
    }
}
internal data class CachedItemEditorDraft(
    val editingId: String?,
    val isNew: Boolean,
    val name: String,
    val price: String,
    val discountPrice: String,
    val description: String,
    val category: String?
)

internal fun loadItemEditorDraftFromStorage(
    context: Context
): CachedItemEditorDraft? {
    return try {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ITEM_EDITOR_DRAFT_JSON, null) ?: return null
        val obj = JSONObject(json)

        val editingId = obj.optString("editingId", "").ifBlank { null }
        val isNew = obj.optBoolean("isNew", false)
        val name = obj.optString("name", "")
        val price = obj.optString("price", "")
        val discountPrice = obj.optString("discountPrice", "")
        val description = obj.optString("description", "")
        val categoryRaw = obj.optString("category", "")
        val category = categoryRaw
            .trim()
            .takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }

        if (
            editingId == null &&
            name.isBlank() &&
            price.isBlank() &&
            discountPrice.isBlank() &&
            description.isBlank() &&
            category == null
        ) {
            null
        } else {
            CachedItemEditorDraft(
                editingId = editingId,
                isNew = isNew,
                name = name,
                price = price,
                discountPrice = discountPrice,
                description = description,
                category = category
            )
        }
    } catch (_: Exception) {
        null
    }
}

internal fun saveItemEditorDraftToStorage(
    context: Context,
    draft: CachedItemEditorDraft
) {
    try {
        val obj = JSONObject()
        obj.put("editingId", draft.editingId ?: JSONObject.NULL)
        obj.put("isNew", draft.isNew)
        obj.put("name", draft.name)
        obj.put("price", draft.price)
        obj.put("discountPrice", draft.discountPrice)
        obj.put("description", draft.description)
        obj.put("category", draft.category?.trim().orEmpty())

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_ITEM_EDITOR_DRAFT_JSON, obj.toString())
            .commit()
    } catch (_: Exception) {
        // ignore
    }
}

internal fun clearItemEditorDraftFromStorage(context: Context) {
    try {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_ITEM_EDITOR_DRAFT_JSON)
            .commit()
    } catch (_: Exception) {
        // ignore
    }
}