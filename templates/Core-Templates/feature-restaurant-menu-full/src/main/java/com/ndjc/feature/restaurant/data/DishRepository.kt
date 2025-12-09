package com.ndjc.feature.restaurant.data

import com.ndjc.feature.restaurant.model.Dish
import com.ndjc.feature.restaurant.model.DishId

interface DishRepository {
    suspend fun getAllDishes(): List<Dish>
    suspend fun getDish(id: DishId): Dish?
    suspend fun upsertDish(dish: Dish)
    suspend fun deleteDish(id: DishId)
}
