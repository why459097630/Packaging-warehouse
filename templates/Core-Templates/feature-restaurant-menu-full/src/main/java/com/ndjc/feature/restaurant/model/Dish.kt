package com.ndjc.feature.restaurant.model

data class DishId(val value: String)

enum class DishCategory {
    HOT, COLD, DRINK, DESSERT, OTHER
}

data class Money(
    val amount: Long, // in minor units, e.g. cents
    val currency: String = "CNY"
)

data class Dish(
    val id: DishId,
    val nameZh: String,
    val nameEn: String,
    val category: DishCategory,
    val imageUrl: String?,
    val descZh: String,
    val descEn: String,
    val priceOriginal: Money,
    val priceDiscount: Money?,
    val isSoldOut: Boolean,
    val isFeatured: Boolean,
    val sortOrder: Int = 0
)
