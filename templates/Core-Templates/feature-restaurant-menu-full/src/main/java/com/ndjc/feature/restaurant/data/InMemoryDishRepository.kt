package com.ndjc.feature.restaurant.data

import com.ndjc.feature.restaurant.model.*
import kotlinx.coroutines.delay

class InMemoryDishRepository : DishRepository {
    // in real app you will replace this with Room / local db
    private val storage: MutableMap<String, Dish> = LinkedHashMap()

    override suspend fun getAllDishes(): List<Dish> {
        delay(30) // simulate IO
        return storage.values.sortedWith(
            compareBy<Dish> { it.sortOrder }.thenBy { it.nameZh }
        )
    }

    override suspend fun getDish(id: DishId): Dish? {
        delay(10)
        return storage[id.value]
    }

    override suspend fun upsertDish(dish: Dish) {
        storage[dish.id.value] = dish
    }

    override suspend fun deleteDish(id: DishId) {
        storage.remove(id.value)
    }
}
