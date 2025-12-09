package com.ndjc.feature.restaurant.ui

data class RestaurantUiActions(
    val onRefresh: () -> Unit,
    val onCategorySelected: (String?) -> Unit,
    val onDishSelected: (UiDish) -> Unit,
    val onProfileClick: () -> Unit,
    val onSearchChanged: (String) -> Unit
)
