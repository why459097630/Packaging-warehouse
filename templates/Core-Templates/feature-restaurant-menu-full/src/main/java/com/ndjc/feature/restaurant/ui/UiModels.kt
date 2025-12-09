package com.ndjc.feature.restaurant.ui

data class UiCategory(
    val name: String,
    val count: Int
)

data class UiDish(
    val id: String,
    val title: String,
    val desc: String,
    val price: Double,
    val image: String?,
    val category: String?
)
