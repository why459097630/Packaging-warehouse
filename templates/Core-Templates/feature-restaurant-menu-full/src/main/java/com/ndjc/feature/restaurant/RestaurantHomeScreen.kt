package com.ndjc.feature.restaurant

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

// Neumorph UI 包：Login
import com.ndjc.ui.neu.components.restaurant.RestaurantLoginUI
import com.ndjc.ui.neu.components.restaurant.RestaurantLoginUiState
import com.ndjc.ui.neu.components.restaurant.RestaurantLoginActions

// Neumorph UI 包：首页
import com.ndjc.ui.neu.components.restaurant.RestaurantHomeUiState
import com.ndjc.ui.neu.components.restaurant.RestaurantHomeDish
import com.ndjc.ui.neu.components.restaurant.RestaurantHomeActions
import com.ndjc.ui.neu.components.restaurant.RestaurantHomeNeu

// Neumorph UI 包：Admin
import com.ndjc.ui.neu.components.restaurant.RestaurantAdminUiState
import com.ndjc.ui.neu.components.restaurant.RestaurantAdminActions
import com.ndjc.ui.neu.components.restaurant.RestaurantAdminNeu

// Neumorph UI 包：详情
import com.ndjc.ui.neu.components.restaurant.RestaurantDetailUiState
import com.ndjc.ui.neu.components.restaurant.RestaurantDetailActions
import com.ndjc.ui.neu.components.restaurant.RestaurantDishDetailNeu

// Neumorph UI 包：编辑菜品
import com.ndjc.ui.neu.components.restaurant.RestaurantEditDishUiState
import com.ndjc.ui.neu.components.restaurant.RestaurantEditDishActions
import com.ndjc.ui.neu.components.restaurant.RestaurantEditDishNeu
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.ndjc.feature.restaurant.AdminScreenActions
import com.ndjc.feature.restaurant.AdminScreenUiState


enum class RestaurantScreen {
    Home,
    Login,
    Admin,
    Edit,
}

data class RestaurantUiState(
    val screen: RestaurantScreen = RestaurantScreen.Home,
    val dishes: List<DemoDish> = emptyList(),
    val isAdminLoggedIn: Boolean = false,
    val selectedDish: DemoDish? = null,
    val selectedCategory: String? = null,
    val loginError: String? = null,
    val manualCategories: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val pendingDeleteIndex: Int? = null,
)

// ---------- 适配用扩展函数：把 DemoDish 映射到 UI 包自己的类型 ----------
private fun DemoDish.toUiDishForUiPack(): RestaurantHomeDish {
    return RestaurantHomeDish(
        id = id,
        title = nameZh.ifBlank { nameEn },
        subtitle = descriptionEn,
        category = category.ifBlank { null },
        price = originalPrice.toDouble(),
        isRecommended = isRecommended,
        isSoldOut = isSoldOut,
        imagePreviewUrl = imageUri?.toString()   // ✅ 新增
    )
}


@Composable
fun RestaurantHomeScreen(
    nav: NavHostController,
) {
    val context = LocalContext.current
    val vm: RestaurantViewModel = viewModel()
    val uiState = vm.uiState

    // 首次进入加载数据
    LaunchedEffect(Unit) {
        vm.ensureLoaded(context)
    }

    val allCategories = deriveCategories(uiState.dishes, uiState.manualCategories)

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ---------- 主路由：按当前 screen 切换不同页面 ----------
            when (uiState.screen) {
                RestaurantScreen.Home -> {
                    // ---------- Home 适配层：逻辑状态 → UI 包契约 ----------
                    val homeUiState = remember(uiState) {
                        RestaurantHomeUiState(
                            dishes = uiState.dishes.map { it.toUiDishForUiPack() },
                            selectedCategory = uiState.selectedCategory,
                            manualCategories = uiState.manualCategories,
                            isLoading = uiState.isLoading,
                            statusMessage = uiState.statusMessage
                        )
                    }

                    // 按钮规范：所有 Home 页按钮只通过 Actions 触发 VM 函数
                    val homeActions = remember(vm, context, uiState.dishes) {
                        RestaurantHomeActions(
                            onRefresh = { vm.refresh(context) },
                            onCategorySelected = { category ->
                                vm.onCategorySelected(category)
                            },
                            onDishSelected = { dishId ->
                                // UI 包只传 id，这里还原成 DemoDish，保持逻辑模块不变
                                val dish = vm.uiState.dishes.firstOrNull { it.id == dishId }
                                dish?.let { vm.openDetail(it) }
                            },
                            onProfileClick = {
                                vm.onAdminFabClicked()
                            }
                        )
                    }

                    RestaurantHomeNeu(
                        uiState = homeUiState,
                        actions = homeActions
                    )
                }

                RestaurantScreen.Login -> {
                    // ---------- Login 适配层 ----------
                    val loginUiState = remember(uiState) {
                        RestaurantLoginUiState(
                            isLoading = uiState.isLoading,
                            loginError = uiState.loginError
                        )
                    }

                    val loginActions = remember(vm) {
                        RestaurantLoginActions(
                            onLogin = { username, password ->
                                vm.tryAdminLogin(username, password)
                            },
                            onBackToHome = {
                                vm.backToHome()
                            }
                        )
                    }

                    RestaurantLoginUI(
                        uiState = loginUiState,
                        actions = loginActions
                    )
                }

                RestaurantScreen.Admin -> {
                    // ---------- Admin 适配层 ----------
                    val adminUiState = remember(uiState, allCategories) {
                        RestaurantAdminUiState(
                            categories = allCategories,
                            dishes = uiState.dishes.map { it.toUiDishForUiPack() },
                            isLoading = uiState.isLoading,
                            statusMessage = uiState.statusMessage
                        )
                    }

                    // 业务动作契约：VM ↔ AdminScreenActions
                    val adminContractActions = remember(vm, context, uiState.dishes) {
                        AdminScreenActions(
                            // 顶部 Back
                            onBackClick = { vm.backToHome() },

                            // “Add New Dish” 按钮
                            onAddNewDishClick = { vm.openNewDishScreen() },

                            // 列表中某个菜品的编辑
                            onEditDishClick = { dishId ->
                                vm.openEditScreen(dishId)
                            },

                            // 删除菜品：这里先简单直接删除（请求 / 确认都删同一个索引）
                            onRequestDeleteDish = { index ->
                                vm.deleteDish(context, index)
                            },
                            onConfirmDeleteDish = { index ->
                                vm.deleteDish(context, index)
                            },

                            // ★ 新增分类（业务动作汇总点）
                            onAddCategoryClick = { name ->
                                vm.addCategory(name)
                            },

                            // 删除分类
                            onDeleteCategoryClick = { category ->
                                vm.removeCategory(context, category)
                            }
                        )
                    }

                    // UI 包现在使用的动作：全部转发给 adminContractActions
                    val adminActions = remember(adminContractActions, uiState.dishes) {
                        RestaurantAdminActions(
                            onBack = { adminContractActions.onBackClick() },

                            // 刷新暂时不在契约里，先直接走 ViewModel
                            onRefresh = { vm.refresh(context) },

                            onEditDish = { dishId ->
                                // 约定：dishId 为空字符串表示“新增菜品”
                                if (dishId.isBlank()) {
                                    adminContractActions.onAddNewDishClick()
                                } else {
                                    adminContractActions.onEditDishClick(dishId)
                                }
                            },

                            onDeleteDish = { dishId ->
                                // 从 dishId 找到当前列表中的索引，再交给契约动作
                                val index = uiState.dishes.indexOfFirst { it.id == dishId }
                                if (index >= 0) {
                                    adminContractActions.onConfirmDeleteDish(index)
                                }
                            },

                            onDeleteCategory = { category ->
                                adminContractActions.onDeleteCategoryClick(category)
                            },

                            // ★ 新增分类：UI 包 → 契约 → ViewModel.addCategory
                            onAddCategory = { name ->
                                adminContractActions.onAddCategoryClick(name)
                            }
                        )
                    }

                    // 渲染 Admin 页面的内容
                    RestaurantAdminNeu(
                        uiState = adminUiState,
                        actions = adminActions,
                        modifier = Modifier.fillMaxSize()
                    )
                }


                RestaurantScreen.Edit -> {
                    val dish = uiState.selectedDish
                    val draft = vm.editDraft

                    // dish == null 说明是“新增菜品”；非空说明是“编辑已有”
                    val editUiState = remember(
                        draft,
                        dish,
                        uiState.manualCategories,
                        uiState.isLoading,
                        uiState.statusMessage
                    ) {
                        // 优先用 editDraft 的值，其次回落到 dish，最后回落到默认
                        val isNew = draft?.isNew ?: (dish == null)

                        val name = draft?.name
                            ?: dish?.let { d -> d.nameZh.ifBlank { d.nameEn } }
                            ?: ""

                        val price = draft?.price
                            ?: dish?.originalPrice?.toString()
                            ?: ""

                        val discountPrice = draft?.discountPrice
                            ?: dish?.discountPrice?.toString()
                            ?: ""

                        val description = draft?.description
                            ?: dish?.descriptionEn
                            ?: ""

                        val selectedCategory = draft?.category
                            ?: dish?.category?.ifBlank { null }

                        val isRecommended = draft?.isRecommended
                            ?: dish?.isRecommended
                            ?: false

                        val isSoldOut = draft?.isSoldOut
                            ?: dish?.isSoldOut
                            ?: false

                        val imagePreviewUrl = draft?.imageUri?.toString()
                            ?: dish?.imageUri?.toString()

                        RestaurantEditDishUiState(
                            name = name,
                            price = price,
                            discountPrice = discountPrice,
                            description = description,
                            availableCategories = uiState.manualCategories,
                            selectedCategory = selectedCategory,
                            isRecommended = isRecommended,
                            isSoldOut = isSoldOut,
                            imagePreviewUrl = imagePreviewUrl,
                            isSaving = uiState.isLoading,
                            errorMessage = uiState.statusMessage,
                            isNew = isNew
                        )
                    }
// 在 RestaurantScreen.Edit 分支内部，加在 val editActions 之前
                    val imagePickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.PickVisualMedia()
                    ) { uri ->
                        if (uri != null) {
                            // 选到图片后：压缩 + 写入 editDraft（不立刻上云）
                            vm.onEditImageSelected(context, uri)
                        }
                    }

                    val editActions = remember(vm, context, imagePickerLauncher) {
                        RestaurantEditDishActions(
                            onBack = { vm.backToAdminFromEdit() },
                            onNameChange = { vm.onEditNameChange(it) },
                            onPriceChange = { vm.onEditPriceChange(it) },
                            onDiscountPriceChange = { vm.onEditDiscountPriceChange(it) },
                            onDescriptionChange = { vm.onEditDescriptionChange(it) },
                            onCategorySelected = { vm.onEditCategorySelected(it) },
                            onToggleRecommended = { vm.onEditToggleRecommended(it) },
                            onToggleSoldOut = { vm.onEditToggleSoldOut(it) },
                            onPickImage = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            // ✅ 必填：补上这个参数（先占位，保证编译通过）
                            onRemoveImage = { _ ->
                                // TODO: 后续接入真正删除逻辑（清空 draft.imageUri 或从 draft.imageUris 移除）
                            },
                            onSave = {
                                vm.onEditSave(context)
                            }
                            // 注意：如果你工程里的 RestaurantEditDishActions 没有 onDelete 参数，就别写 onDelete
                            // 如果你工程里的契约有 onDelete（你上传的这份是有的），再按需加：, onDelete = null
                        )
                    }


                    RestaurantEditDishNeu(
                        uiState = editUiState,
                        actions = editActions,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // ---------- 详情页浮层：覆盖在 Home/Admin 最上层 ----------
            val selectedDish = uiState.selectedDish
            if (
                selectedDish != null &&
                (uiState.screen == RestaurantScreen.Home ||
                        uiState.screen == RestaurantScreen.Admin)
            ) {
                val detailUiState = RestaurantDetailUiState(
                    name = selectedDish.nameZh.ifBlank { selectedDish.nameEn },
                    price = selectedDish.originalPrice.toString(),
                    discountPrice = selectedDish.discountPrice?.toString(),
                    description = selectedDish.descriptionEn,
                    category = selectedDish.category,
                    isRecommended = selectedDish.isRecommended,
                    isUnavailable = selectedDish.isSoldOut,
                    imagePreviewUrl = null,
                    imageUrls = emptyList(),
                    currentImageIndex = 0
                )

                val detailActions = RestaurantDetailActions(
                    onBack = {
                        Log.d(
                            "NDJC_DETAIL",
                            "detailActions.onBack called, selectedDish=${uiState.selectedDish?.id}"
                        )
                        vm.backFromDetail()
                    },
                    // 从详情直接进入编辑页
                    onEdit = {
                        selectedDish.let { dish ->
                            vm.openEditScreen(dish.id)
                        }
                    },
                    onOpenImage = {
                        // TODO：之后接入图片预览逻辑
                        Log.d(
                            "NDJC_DETAIL",
                            "detailActions.onOpenImage clicked (待接入图片预览)"
                        )
                    }
                )

                RestaurantDishDetailNeu(
                    uiState = detailUiState,
                    actions = detailActions
                )
            }
        }
    }
}
