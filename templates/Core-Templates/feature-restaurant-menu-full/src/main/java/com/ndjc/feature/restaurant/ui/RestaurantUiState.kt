package com.ndjc.feature.restaurant.ui

data class RestaurantUiState(
    val categories: List<UiCategory> = emptyList(),
    val dishes: List<UiDish> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false
)
