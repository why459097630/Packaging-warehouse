package com.ndjc.feature.restaurant

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class DemoDish(
    val id: String,
    val nameZh: String,
    val nameEn: String,
    val descriptionEn: String,
    val category: String,
    val originalPrice: Float,
    val discountPrice: Float? = null,
    val isRecommended: Boolean = false,
    val isSoldOut: Boolean = false,
    val imageResId: Int? = null,
    val imageUri: Uri? = null,
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

private const val PREF_NAME = "restaurant_demo_prefs"
private const val KEY_DISHES_JSON = "dishes_json"

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
            list.add(
                DemoDish(
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
                    imageResId = null,
                    imageUri = obj.optString("imageUri", null)
                        ?.takeIf { it.isNotBlank() && it != "null" }
                        ?.let { Uri.parse(it) }
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
            obj.put("imageUri", dish.imageUri?.toString() ?: JSONObject.NULL)
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
