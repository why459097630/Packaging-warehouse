package com.ndjc.feature.restaurant.state

import com.ndjc.feature.restaurant.model.Dish
import com.ndjc.feature.restaurant.model.DishId

data class MenuListState(
    val isLoading: Boolean = false,
    val dishes: List<Dish> = emptyList(),
    val errorMessage: String? = null
)

data class DishDetailState(
    val isLoading: Boolean = false,
    val dishId: DishId? = null,
    val dish: Dish? = null,
    val errorMessage: String? = null
)

data class AdminState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val editingDish: Dish? = null,
    val errorMessage: String? = null
)
