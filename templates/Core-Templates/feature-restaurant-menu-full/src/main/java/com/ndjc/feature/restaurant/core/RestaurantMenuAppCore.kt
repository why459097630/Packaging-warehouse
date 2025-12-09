package com.ndjc.feature.restaurant.core

import com.ndjc.feature.restaurant.data.DishRepository
import com.ndjc.feature.restaurant.model.*
import com.ndjc.feature.restaurant.state.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 无 UI 的整套餐厅菜单 AppCore：
 * - 管菜单列表
 * - 管菜品详情
 * - 管管理员登录 + 编辑
 *
 * UI / 模板只需要订阅 state，并通过这些方法触发事件。
 */
class RestaurantMenuAppCore(
    private val repo: DishRepository,
    private val adminPassword: String = "1234"
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _menuListState = MutableStateFlow(MenuListState())
    val menuListState: StateFlow<MenuListState> = _menuListState

    private val _detailState = MutableStateFlow(DishDetailState())
    val detailState: StateFlow<DishDetailState> = _detailState

    private val _adminState = MutableStateFlow(AdminState())
    val adminState: StateFlow<AdminState> = _adminState

    init {
        refreshMenu()
    }

    fun refreshMenu() {
        scope.launch {
            _menuListState.value = _menuListState.value.copy(isLoading = true, errorMessage = null)
            try {
                val dishes = repo.getAllDishes()
                _menuListState.value = _menuListState.value.copy(
                    isLoading = false,
                    dishes = dishes
                )
            } catch (t: Throwable) {
                _menuListState.value = _menuListState.value.copy(
                    isLoading = false,
                    errorMessage = t.message ?: "加载菜单失败"
                )
            }
        }
    }

    fun selectDish(id: DishId) {
        scope.launch {
            _detailState.value = _detailState.value.copy(
                isLoading = true,
                dishId = id,
                errorMessage = null
            )
            try {
                val dish = repo.getDish(id)
                _detailState.value = _detailState.value.copy(
                    isLoading = false,
                    dish = dish
                )
            } catch (t: Throwable) {
                _detailState.value = _detailState.value.copy(
                    isLoading = false,
                    errorMessage = t.message ?: "加载菜品失败"
                )
            }
        }
    }

    fun adminLogin(password: String) {
        if (password == adminPassword) {
            _adminState.value = _adminState.value.copy(
                isLoggedIn = true,
                errorMessage = null
            )
        } else {
            _adminState.value = _adminState.value.copy(
                isLoggedIn = false,
                errorMessage = "密码错误"
            )
        }
    }

    fun startEditDish(dish: Dish?) {
        _adminState.value = _adminState.value.copy(
            editingDish = dish,
            errorMessage = null
        )
    }

    fun saveDish(dish: Dish) {
        scope.launch {
            _adminState.value = _adminState.value.copy(isLoading = true, errorMessage = null)
            try {
                repo.upsertDish(dish)
                _adminState.value = _adminState.value.copy(
                    isLoading = false,
                    editingDish = null
                )
                refreshMenu()
            } catch (t: Throwable) {
                _adminState.value = _adminState.value.copy(
                    isLoading = false,
                    errorMessage = t.message ?: "保存失败"
                )
            }
        }
    }

    fun deleteDish(id: DishId) {
        scope.launch {
            _adminState.value = _adminState.value.copy(isLoading = true, errorMessage = null)
            try {
                repo.deleteDish(id)
                _adminState.value = _adminState.value.copy(isLoading = false)
                refreshMenu()
            } catch (t: Throwable) {
                _adminState.value = _adminState.value.copy(
                    isLoading = false,
                    errorMessage = t.message ?: "删除失败"
                )
            }
        }
    }
}
