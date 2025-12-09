package com.ndjc.feature.restaurant

import android.net.Uri

/**
 * NDJC 餐厅模块 – UI 包 ↔ 逻辑模块 的按钮 & 状态契约。
 *
 * 约定：
 * - UI 包只依赖 XxxScreenUiState / XxxScreenActions，不直接访问 ViewModel / Repository。
 * - 模块壳子（RestaurantHomeScreen 等）负责把 Actions 对准 RestaurantViewModel。
 * - ViewModel 负责所有业务逻辑和状态修改。
 *
 * 注意：
 * - 以后新增页面 / 按钮：优先在这里追加 UiState 字段 / Actions 字段，再去绑 ViewModel。
 * - 尽量做到「只增不改」，保证旧 UI 包不被破坏。
 */

/* ------------------------- Home（首页） ------------------------- */

/**
 * 首页 UI 状态（壳子版）。
 * 后续可以从 RestaurantViewModel.uiState 中适配填充：
 * - dishes
 * - categories（可以由 dishes + manualCategories 推导）
 * - selectedCategory
 * - isLoading / statusMessage 等
 */
data class HomeScreenUiState(
    val isLoading: Boolean = false,
    val dishes: List<DemoDish> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val statusMessage: String? = null,
)

/**
 * 首页可用的业务动作。
 *
 * 对应 ViewModel 中典型函数：
 * - refresh(context)
 * - onCategorySelected(category)
 * - openDetail(dish)
 * - onAdminFabClicked()
 */
data class HomeScreenActions(
    /** 下拉刷新 / 刷新按钮 */
    val onRefresh: () -> Unit,

    /** 选择某个分类（含取消选中 = null） */
    val onCategorySelected: (String?) -> Unit,

    /** 点击某个菜品卡片，进入详情 */
    val onDishCardClick: (DemoDish) -> Unit,

    /** 点击 Admin 悬浮按钮：未登录→登录页，已登录→Admin 页 */
    val onAdminFabClick: () -> Unit,
)

/* ------------------------- Detail（详情页 / 浮层） ------------------------- */

/**
 * 详情页 UI 状态（壳子版）。
 * 一般只需要当前要展示的菜品。
 */
data class DetailScreenUiState(
    val dish: DemoDish? = null,
)

/**
 * 详情页业务动作。
 *
 * 对应 ViewModel：
 * - backFromDetail()
 */
data class DetailScreenActions(
    /** 从详情返回列表（清空 selectedDish，回到 Home/Admin） */
    val onBackClick: () -> Unit,
)

/* ------------------------- Login（登录页） ------------------------- */

/**
 * 登录页 UI 状态（壳子版）。
 */
data class LoginScreenUiState(
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * 登录页业务动作。
 *
 * 对应 ViewModel：
 * - backToHome()
 * - tryAdminLogin(username, password)
 */
data class LoginScreenActions(
    /** 登录页返回首页 */
    val onBackClick: () -> Unit,

    /** 提交用户名 + 密码进行 Admin 登录 */
    val onSubmitLogin: (String, String) -> Unit,
)

/* ------------------------- Admin（管理页） ------------------------- */

/**
 * Admin 管理页 UI 状态（壳子版）。
 * 可从 RestaurantViewModel.uiState 适配：
 * - isAdminLoggedIn
 * - dishes
 * - categories / selectedCategory
 * - pendingDeleteIndex
 */
data class AdminScreenUiState(
    val isAdminLoggedIn: Boolean = false,
    val dishes: List<DemoDish> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val pendingDeleteIndex: Int? = null,
    val statusMessage: String? = null,
)

/**
 * Admin 管理页业务动作。
 *
 * 对应 ViewModel：
 * - backToHome()
 * - openNewDishScreen()
 * - openEditScreen(dishId)
 * - requestDeleteDish(index)
 * - deleteDish(context, index)
 * - addCategory(name)
 * - removeCategory(context, category)
 */
data class AdminScreenActions(
    /** Admin 顶部返回按钮 → 回首页 */
    val onBackClick: () -> Unit,

    /** 点击“Add new dish” 按钮 → 进入新增菜品编辑页 */
    val onAddNewDishClick: () -> Unit,

    /** 点击某个菜品的编辑按钮 → 进入编辑页（通过 dishId） */
    val onEditDishClick: (String) -> Unit,

    /**
     * 点击某个菜品的删除图标 → 挂起待删除索引，用于弹出确认弹窗。
     * （对应 ViewModel.requestDeleteDish(index)）
     */
    val onRequestDeleteDish: (Int) -> Unit,

    /**
     * 在删除确认弹窗中点击“确认删除”。
     * （对应 ViewModel.deleteDish(context, index)，壳子层用闭包带入 context）
     */
    val onConfirmDeleteDish: (Int) -> Unit,

    /** 新增分类（输入框 + 按钮） */
    val onAddCategoryClick: (String) -> Unit,

    /** 删除分类按钮 */
    val onDeleteCategoryClick: (String) -> Unit,
)

/* ------------------------- Edit（编辑 / 新增菜品页） ------------------------- */

/**
 * 编辑 / 新增菜品页 UI 状态（壳子版）。
 * - dish：当前正在编辑的菜，如果是新增，可以为 null 或有默认草稿
 * - isNew：是否为“新增菜品”模式
 */
data class EditScreenUiState(
    val dish: DemoDish? = null,
    val isNew: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * 编辑 / 新增菜品页业务动作。
 *
 * 对应 ViewModel：
 * - backToAdminFromEdit()
 * - updateDish(context, index, updated)
 * - setDishImageFromPicker(context, index, sourceUri)
 */
data class EditScreenActions(
    /** 编辑页返回 Admin 页面 */
    val onBackClick: () -> Unit,

    /**
     * 点击“保存”按钮。
     * - index：要更新的菜品在列表中的索引；新增时可传 -1 或列表长度，由壳子层决定。
     * - dish：编辑后的完整 DemoDish。
     */
    val onSaveDish: (Int, DemoDish) -> Unit,

    /**
     * 在编辑页选择图片后的回调。
     * - index：菜品索引
     * - uri：原始选择的图片 Uri（壳子层负责传进来）
     */
    val onPickImage: (Int, Uri) -> Unit,
)
