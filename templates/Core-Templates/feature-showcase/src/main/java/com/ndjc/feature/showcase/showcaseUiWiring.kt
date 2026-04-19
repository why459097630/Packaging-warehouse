package com.ndjc.feature.showcase

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import com.ndjc.feature.showcase.ui.NdjcFullscreenViewerRegistry
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding

import androidx.compose.material3.Scaffold

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

import androidx.core.content.ContextCompat

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

import com.ndjc.feature.showcase.ui.NdjcSyncErrorBanner
import com.ndjc.feature.showcase.ui.ShowcaseBottomBar
import com.ndjc.feature.showcase.ui.ShowcaseBottomBarTab
import com.ndjc.feature.showcase.ui.ShowcaseMerchantChatListScreen
import com.ndjc.feature.showcase.ui.ShowcaseUiRenderer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * feature-showcase 的 UI 接线层（wiring）
 * 目标：
 * - 只保留 VM 的状态机（ShowcaseUiState.screen / selectedDish / pendingDeleteIndex）
 * - wiring 层不再维护第二套 Screen/screenState，也不直接写 vm.uiState（因为 VM 是 private set）
 */
private fun parseCloudIsoMillis(raw: String): Long? {
    val value = raw.trim()
    if (value.isBlank()) return null

    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )

    for (pattern in patterns) {
        runCatching {
            val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val parsed = sdf.parse(value)
            if (parsed != null) return parsed.time
        }
    }

    return null
}

private fun formatCloudDateTimeLabel(raw: String): String {
    val millis = parseCloudIsoMillis(raw) ?: return raw.trim()
    val output = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }
    return output.format(millis)
}

private fun buildCloudDaysRemainingLabel(serviceEndAt: String): String {
    val endAtMs = parseCloudIsoMillis(serviceEndAt) ?: return ""
    val nowMs = System.currentTimeMillis()
    val diffMs = endAtMs - nowMs
    val oneDayMs = 24L * 60L * 60L * 1000L

    if (diffMs <= 0L) {
        return "Expired"
    }

    val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    val todayStartMs = dayFormat.parse(dayFormat.format(nowMs))?.time ?: nowMs
    val endDayStartMs = dayFormat.parse(dayFormat.format(endAtMs))?.time ?: endAtMs
    val days = ((endDayStartMs - todayStartMs) / oneDayMs).toInt()

    return when {
        days <= 0 -> "Expires today"
        days == 1 -> "1 day left"
        else -> "$days days left"
    }
}

private fun buildCloudStatusLabel(
    status: ShowcaseCloudServiceStatus,
    canWrite: Boolean
): String {
    return when (status) {
        ShowcaseCloudServiceStatus.Active -> {
            if (canWrite) "Running · Writable" else "Running · Not writable"
        }
        ShowcaseCloudServiceStatus.ReadOnly -> {
            "Read-only · Not writable"
        }
        ShowcaseCloudServiceStatus.Deleted -> {
            "Deleted · Not writable"
        }
        ShowcaseCloudServiceStatus.Unknown -> {
            if (canWrite) "Unknown · Writable" else "Unknown · Not writable"
        }
    }
}

private fun buildAdminSyncNoticeLabel(
    state: ShowcaseSyncOverviewState,
    pendingCount: Int,
    errorMessage: String?
): String {
    return when (state) {
        ShowcaseSyncOverviewState.Idle -> ""
        ShowcaseSyncOverviewState.HasPending -> {
            if (pendingCount > 0) {
                "Sync: $pendingCount changes pending"
            } else {
                ""
            }
        }
        ShowcaseSyncOverviewState.Syncing -> {
            if (pendingCount > 0) {
                "Syncing: $pendingCount changes pending"
            } else {
                "Syncing..."
            }
        }
        ShowcaseSyncOverviewState.Failed -> {
            errorMessage?.takeIf { it.isNotBlank() }?.let { "Sync failed: $it" } ?: "Sync failed"
        }
    }
}

@Composable
fun ShowcaseHomeScreen(
    nav: NavHostController,
    ui: ShowcaseUiRenderer,
) {
    val context = LocalContext.current
    val vm: ShowcaseViewModel = viewModel()
    val uiState by vm.uiStateFlow.collectAsState()
    val editDraft by vm.editDraftFlow.collectAsState()
    val favoriteIds by vm.favoriteIdsFlow.collectAsState()
    val chatEntryDot by vm.chatEntryDotFlow.collectAsState()
    val announcementsEntryDot by vm.announcementsEntryDotFlow.collectAsState()
    val pushRoute by ShowcasePushRouter.pendingRoute.collectAsState()

    LaunchedEffect(Unit) {
        vm.ensureLoaded(context)
    }

    LaunchedEffect(pushRoute) {
        val route = pushRoute ?: return@LaunchedEffect
        vm.handlePushRoute(
            context = context,
            route = route
        )
        ShowcasePushRouter.consume(route)
    }

    LaunchedEffect(uiState.screen, uiState.chat.conversationId, uiState.isAdminLoggedIn) {
        when (uiState.screen) {
            ShowcaseScreen.Chat -> {
                if (uiState.isAdminLoggedIn) {
                    vm.ensurePushRegistration(
                        context = context,
                        audience = "chat_merchant"
                    )
                } else {
                    if (!uiState.chat.conversationId.isNullOrBlank()) {
                        vm.ensurePushRegistration(
                            context = context,
                            audience = "chat_client"
                        )
                    }
                }
            }

            ShowcaseScreen.Admin,
            ShowcaseScreen.AdminItems,
            ShowcaseScreen.AdminCategories,
            ShowcaseScreen.StoreProfile,
            ShowcaseScreen.ChangePassword,
            ShowcaseScreen.MerchantChatList,
            ShowcaseScreen.AdminAnnouncementEdit -> {
                if (uiState.isAdminLoggedIn) {
                    vm.ensurePushRegistration(
                        context = context,
                        audience = "chat_merchant"
                    )
                }
            }

            ShowcaseScreen.Announcements,
            ShowcaseScreen.Home,
            ShowcaseScreen.Detail,
            ShowcaseScreen.StoreProfileView,
            ShowcaseScreen.Favorites -> {
                vm.ensurePushRegistration(
                    context = context,
                    audience = "announcement_subscriber"
                )
            }

            else -> Unit
        }
    }

    LaunchedEffect(uiState.screen) {
        if (uiState.screen == ShowcaseScreen.Home) {
            vm.ensureAnnouncementRegistrationOnHome(context)
        }
    }

// ✅ Chat 页面可见：启动轮询；离开 Chat：停止轮询（更稳，避免重组误停）
    DisposableEffect(uiState.screen) {
        val isChat = (uiState.screen == ShowcaseScreen.Chat)
        if (isChat) {
            vm.onChatScreenVisible(context)
        }
        onDispose {
            if (isChat) {
                vm.onChatScreenHidden()
            }
        }
    }

// ✅ Home 底栏 Chat 红点：独立轻量轮询
    DisposableEffect(uiState.screen) {
        val shouldPollChatEntry =
            (uiState.screen == ShowcaseScreen.Home
                    || uiState.screen == ShowcaseScreen.Detail
                    || uiState.screen == ShowcaseScreen.StoreProfileView
                    || uiState.screen == ShowcaseScreen.Favorites
                    || uiState.screen == ShowcaseScreen.Announcements)

        if (shouldPollChatEntry) {
            vm.startChatEntryPolling(context)
        }

        onDispose {
            if (shouldPollChatEntry) {
                vm.stopChatEntryPolling()
            }
        }
    }

// ✅ Home 底栏 Announcements 红点：独立轻量轮询
    DisposableEffect(uiState.screen) {
        val shouldPollAnnouncementsEntry =
            (uiState.screen == ShowcaseScreen.Home
                    || uiState.screen == ShowcaseScreen.Detail
                    || uiState.screen == ShowcaseScreen.StoreProfileView
                    || uiState.screen == ShowcaseScreen.Favorites
                    || uiState.screen == ShowcaseScreen.Announcements)

        if (shouldPollAnnouncementsEntry) {
            vm.startAnnouncementsEntryPolling(context)
        }

        onDispose {
            if (shouldPollAnnouncementsEntry) {
                vm.stopAnnouncementsEntryPolling()
            }
        }
    }
    val homeDishes: List<ShowcaseHomeDish> =
        vm.visibleDishes(includeHidden = false).map { d ->
            val id = d.id.toString()
            demoDishToHomeDish(d).copy(
                isFavorite = favoriteIds.contains(id)
            )
        }

// ✅ AdminItems：由 VM 统一做 search/filter/sort（UI 不再计算 list）
    val adminDishes: List<ShowcaseHomeDish> =
        vm.visibleAdminItems().map { d ->
            val id = d.id.toString()
            demoDishToHomeDish(d).copy(
                isFavorite = favoriteIds.contains(id)
            )
        }

    val categories = remember(uiState.dishes, uiState.manualCategories) {
        deriveCategories(uiState.dishes, uiState.manualCategories)
    }
    val allTags = remember(uiState.dishes) { deriveAllTags(uiState.dishes) }


    val homeState = remember(
        homeDishes,
        uiState.selectedCategory,
        categories,
        uiState.isLoading,
        uiState.statusMessage,
        uiState.searchQuery,
        uiState.sortMode,
        uiState.filterRecommendedOnly,
        uiState.filterOnSaleOnly,
        allTags,
        uiState.selectedTags,
        uiState.homeShowSortMenu,
        uiState.homeShowFilterMenu,
        uiState.homePriceMinDraft,
        uiState.homePriceMaxDraft,
        uiState.homeAppliedMinPrice,
        uiState.homeAppliedMaxPrice,
        uiState.homeShowPriceMenu

    ) {
        ShowcaseHomeUiState(
            dishes = homeDishes,
            selectedCategory = uiState.selectedCategory,
            manualCategories = categories,
            isLoading = uiState.isLoading,
            statusMessage = uiState.statusMessage,

            // search / sort / filters
            searchQuery = uiState.searchQuery,
            sortMode = uiState.sortMode,
            filterRecommendedOnly = uiState.filterRecommendedOnly,
            filterOnSaleOnly = uiState.filterOnSaleOnly,

            // ✅ tags
            allTags = allTags,
            selectedTags = uiState.selectedTags,

            showSortMenu = uiState.homeShowSortMenu,
            showFilterMenu = uiState.homeShowFilterMenu,
            showPriceMenu = uiState.homeShowPriceMenu,

            priceMinDraft = uiState.homePriceMinDraft,
            priceMaxDraft = uiState.homePriceMaxDraft,
            appliedMinPrice = uiState.homeAppliedMinPrice,
            appliedMaxPrice = uiState.homeAppliedMaxPrice,

            )

    }
    val loginState = remember(
        uiState.isLoading,
        uiState.loginError,
        uiState.loginUsernameDraft,
        uiState.loginPasswordDraft,
        uiState.loginRememberMeDraft
    ) {
        ShowcaseLoginUiState(
            isLoading = uiState.isLoading,
            loginError = uiState.loginError,
            usernameDraft = uiState.loginUsernameDraft,
            passwordDraft = uiState.loginPasswordDraft,
            rememberMe = uiState.loginRememberMeDraft,
            canLogin = vm.canLogin()
        )
    }

    val changePasswordState = remember(
        uiState.changePasswordCurrentDraft,
        uiState.changePasswordNewDraft,
        uiState.changePasswordConfirmDraft,
        uiState.isLoading,
        uiState.changePasswordError,
        uiState.changePasswordSuccess
    ) {
        ShowcaseChangePasswordUiState(
            current = uiState.changePasswordCurrentDraft,
            next = uiState.changePasswordNewDraft,
            confirm = uiState.changePasswordConfirmDraft,
            isSaving = uiState.isLoading,
            error = uiState.changePasswordError,
            success = uiState.changePasswordSuccess
        )
    }

    val changePasswordActions = ShowcaseChangePasswordActions(
        onBack = { vm.backFromChangePassword() },
        onCurrentChange = { vm.onChangePasswordCurrentDraftChange(it) },
        onNextChange = { vm.onChangePasswordNewDraftChange(it) },
        onConfirmChange = { vm.onChangePasswordConfirmDraftChange(it) },
        onSubmit = { vm.submitChangePassword(context) },
        onBackToHome = { vm.backToHome() }
    )

    val adminState = remember(
        uiState.isLoading,
        uiState.statusMessage,
        uiState.cloudStatus,

        // ✅ AdminItems 相关：必须加入，否则筛选/排序/搜索/价格区间输入不刷新
        uiState.adminItemsSortMode,
        uiState.adminItemsSortAscending,
        uiState.adminItemsSearchQuery,
        uiState.adminItemsFilterRecommended,
        uiState.adminItemsFilterHiddenOnly,
        uiState.adminItemsFilterDiscountOnly,
        uiState.adminItemsPriceMinDraft,
        uiState.adminItemsPriceMaxDraft,
        uiState.adminItemsAppliedMinPrice,
        uiState.adminItemsAppliedMaxPrice,
        uiState.adminSelectedDishIds,

        // ✅ Admin credentials drafts
        uiState.adminUsernameDraft,
        uiState.adminPasswordDraft,

        // ✅ AdminCategories 弹窗状态（你这里也在 adminState 里透出）
        uiState.adminPendingDeleteCategory,
        uiState.adminCannotDeleteCategory,

        // 其他你原本就有的
        uiState.selectedCategory,
        categories,
        adminDishes,
        uiState.pendingDeleteDishId,
        uiState.pendingSyncCount,
        uiState.syncOverviewState,
        uiState.syncErrorMessage,

        uiState.storeProfile,
        uiState.storeProfileDraft,
        uiState.isSavingStoreProfile,
        uiState.storeProfileSaveError,
        uiState.storeProfileSaveSuccess
    ) {
        val p = uiState.storeProfile ?: StoreProfile()
        val d = uiState.storeProfileDraft

        val adminProfile = ShowcaseStoreProfile(
            displayName = p.title,
            tagline = p.subtitle,
            address = p.address,
            businessHours = p.hours,
            mapUrl = p.mapUrl
        )

        val adminDraft = d?.let {
            ShowcaseStoreProfileDraft(
                displayName = it.title,
                tagline = it.subtitle,
                address = it.address,
                businessHours = it.hours,
                mapUrl = it.mapUrl,
                isDirty = true
            )
        }

        val adminCloudStatus = uiState.cloudStatus.takeIf { it.storeId.isNotBlank() }?.let { cloud ->
            val serviceEndAtRaw = cloud.serviceEndAt?.trim().orEmpty()
            val deleteAtRaw = cloud.deleteAt?.trim().orEmpty()

            ShowcaseCloudStatusUi(
                storeId = cloud.storeId,
                planLabel = when (cloud.planType) {
                    ShowcaseCloudPlanType.Trial -> "Trial"
                    ShowcaseCloudPlanType.Paid -> "Paid"
                    ShowcaseCloudPlanType.Unknown -> "Unknown"
                },
                statusLabel = buildCloudStatusLabel(
                    status = cloud.serviceStatus,
                    canWrite = cloud.canWrite
                ),
                daysRemainingLabel = buildCloudDaysRemainingLabel(serviceEndAtRaw),
                serviceEndAtLabel = if (serviceEndAtRaw.isNotBlank()) formatCloudDateTimeLabel(serviceEndAtRaw) else "",
                deleteAtLabel = if (deleteAtRaw.isNotBlank()) formatCloudDateTimeLabel(deleteAtRaw) else "",
                canWrite = cloud.canWrite
            )
        }

        ShowcaseAdminUiState(
            isLoading = uiState.isLoading,
            statusMessage = uiState.statusMessage,
            cloudStatus = adminCloudStatus,
            itemsSortMode = uiState.adminItemsSortMode,
            itemsSortAscending = uiState.adminItemsSortAscending,
            itemsSearchQuery = uiState.adminItemsSearchQuery,
            filterRecommended = uiState.adminItemsFilterRecommended,
            filterHiddenOnly = uiState.adminItemsFilterHiddenOnly,
            filterDiscountOnly = uiState.adminItemsFilterDiscountOnly,

            priceMinDraft = uiState.adminItemsPriceMinDraft,
            priceMaxDraft = uiState.adminItemsPriceMaxDraft,
            appliedMinPrice = uiState.adminItemsAppliedMinPrice,
            appliedMaxPrice = uiState.adminItemsAppliedMaxPrice,

            selectedCategory = uiState.selectedCategory,
            manualCategories = categories,
            dishes = adminDishes,
            pendingDeleteDishId = uiState.pendingDeleteDishId,
            selectedDishIds = uiState.adminSelectedDishIds,

            // ✅ sync overview
            pendingSyncCount = uiState.pendingSyncCount,
            syncOverviewState = mapSyncOverviewState(uiState.syncOverviewState),
            syncErrorMessage = uiState.syncErrorMessage,
            syncNoticeLabel = buildAdminSyncNoticeLabel(
                state = mapSyncOverviewState(uiState.syncOverviewState),
                pendingCount = uiState.pendingSyncCount,
                errorMessage = uiState.syncErrorMessage
            ),

            // ✅ store profile (admin path 2)
            storeProfile = adminProfile,
            storeProfileDraft = adminDraft,
            isSavingStoreProfile = uiState.isSavingStoreProfile,
            storeProfileSaveError = uiState.storeProfileSaveError,
            storeProfileSaveSuccess = uiState.storeProfileSaveSuccess,
            adminUsernameDraft = uiState.adminUsernameDraft,
            adminPasswordDraft = uiState.adminPasswordDraft,
            pendingDeleteCategory = uiState.adminPendingDeleteCategory,
            cannotDeleteCategory = uiState.adminCannotDeleteCategory
        )
    }






    val selectedDish = uiState.selectedDish
    val detailState = remember(
        selectedDish,
        uiState.detailImageIndex,
        vm.favoritesUiState // ✅ 关键：收藏变化会触发这里重新计算
    ) {

        if (selectedDish == null) {
            ShowcaseDetailUiState()
        } else {
            val img = vm.deriveDetailImages(selectedDish, uiState.detailImageIndex)
            val dishId = selectedDish.id.toString()

            ShowcaseDetailUiState(
                dishId = dishId,
                isFavorite = favoriteIds.contains(dishId),
                title = selectedDish.nameZh.ifBlank { selectedDish.nameEn },
                subtitle = selectedDish.descriptionEn.takeIf { it.isNotBlank() },
                price = formatPrice(selectedDish.originalPrice),
                discountPrice = selectedDish.discountPrice?.let(::formatPrice),
                description = selectedDish.descriptionEn,
                category = selectedDish.category.takeIf { it.isNotBlank() },
                isRecommended = selectedDish.isRecommended,
                isUnavailable = selectedDish.isSoldOut,

                imagePreviewUrl = img.preview,
                imageUrls = img.imageUrls,
                currentImageIndex = uiState.detailImageIndex,
                safeImageIndex = img.safeIndex,


                tags = selectedDish.tags,
                externalLink = selectedDish.externalLink,

                )

        }
    }
    val editState = remember(
        editDraft,
        categories,
        uiState.isLoading,
        uiState.statusMessage,
        uiState.editValidationError
    ) {

        val d = vm.deriveEditState(
            editDraft = editDraft,
            categories = categories,
            isLoading = uiState.isSavingEditDish
        )

        val draft = editDraft

        if (draft == null) {
            ShowcaseEditDishUiState(
                availableCategories = d.cleanedCategories,
                hasUnsavedChanges = false
            )
        } else {
            ShowcaseEditDishUiState(
                id = "",
                nameZh = draft.name,
                nameEn = "",
                descriptionEn = draft.description,
                category = draft.category,
                availableCategories = d.cleanedCategories,

                originalPrice = draft.price,
                discountPrice = draft.discountPrice,
                isRecommended = draft.isRecommended,
                isHidden = draft.isHidden,

                imageUrls = d.cleanedImages,

                isSaving = uiState.isSavingEditDish,
                isBlocking = uiState.isBlockingEditDish,
                statusMessage = uiState.statusMessage,
                isNew = draft.isNew,
                errorMessage = uiState.editValidationError,

                // Derived（全部来自 VM，不再在 wiring 计算）
                isDiscountInvalidNumber = d.isDiscountInvalidNumber,
                isDiscountGEPrice = d.isDiscountGEPrice,
                discountErrorText = d.discountErrorText,
                canSave = d.canSave,

                // Images derived
                canAddImageSlot = d.canAddImageSlot,
                maxImages = d.maxImages,
                hasUnsavedChanges = vm.hasUnsavedEditDraft()
            )
        }
    }



// -----------------------
// StoreProfile state 映射
// -----------------------
    val storeProfileState = remember(
        uiState.screen,
        uiState.storeProfile,
        uiState.storeProfileDraft, // ✅ 改这里
        uiState.isSavingStoreProfile,
        uiState.storeProfileSaveError,
        uiState.storeProfileSaveSuccess
    ) {
        val p = uiState.storeProfile ?: StoreProfile()
        val d = uiState.storeProfileDraft
        ShowcaseStoreProfileUiState(
            canEdit = (uiState.screen == ShowcaseScreen.StoreProfile),
            // 展示态（未编辑时显示）
            title = p.title,
            subtitle = p.subtitle,
            description = p.description,

            // ✅ 新：业务范围（展示态）
            services = p.services,
            // ✅ 新：自定义联系方式（展示态）
            extraContacts = p.extraContacts,


            address = p.address,
            hours = p.hours,
            mapUrl = p.mapUrl,
            coverUrl = p.coverUrl,
            logoUrl = p.logoUrl,
            businessStatus = p.businessStatus,

            // 编辑态（draft 优先；没有 draft 就给空/回落）
            isEditing = d != null,
            draftTitle = d?.title ?: p.title,
            draftSubtitle = d?.subtitle ?: p.subtitle,
            draftDescription = d?.description ?: p.description,
            draftAddress = d?.address ?: p.address,
            draftHours = d?.hours ?: p.hours,
            draftMapUrl = d?.mapUrl ?: p.mapUrl,
            draftLogoUrl = d?.logoUrl ?: p.logoUrl,
            draftCoverUrl = d?.coverUrl ?: p.coverUrl,
            draftBusinessStatus = d?.businessStatus ?: p.businessStatus,

// ✅ 新：业务范围（编辑态，d 优先）
// UI 用它渲染“已添加项列表”，Add 后会立刻多一行输入框
            draftServices = (d?.services ?: p.services),

// ✅ 新：自定义联系方式（编辑态，d 优先）
            draftExtraContacts = (d?.extraContacts ?: p.extraContacts).mapIndexed { index, c ->
                ExtraContactDraft(
                    id = index.toString(),
                    name = c.name,
                    value = c.value
                )
            },


            // ✅ Map URL 的 http/https 提示不走内联文案
            validationError = null,

            // 保存态
            // 保存态
            isSaving = uiState.isSavingStoreProfile,
            saveError = uiState.storeProfileSaveError,
            saveSuccess = uiState.storeProfileSaveSuccess,
            statusMessage = uiState.statusMessage,
            hasUnsavedChanges = vm.hasUnsavedStoreProfileDraft()
        )
    }

    // -----------------------
    // actions（不再直接写 vm.uiState）
    // -----------------------

    val homeActions = com.ndjc.feature.showcase.ShowcaseHomeActions(
        onRefresh = { vm.refresh(context) },
        onCategorySelected = { category -> vm.onCategorySelected(category) },
        onDishSelected = { dishId ->
            vm.openDetailById(dishId)
        },
        onProfileClick = { vm.onAdminFabClicked() },



        // ✅ 新增：打开 StoreProfile 展示页
        onOpenStoreProfileView = { vm.openStoreProfileView(context) },
        onOpenChat = ({ vm.openChatFromHome(context) } as () -> Unit),


        onSearchQueryChange = { q -> vm.onSearchQueryChange(q) },
        onSortModeChange = { mode -> vm.onSortModeChange(mode) },
        onFilterRecommendedOnlyChange = { enabled -> vm.onFilterRecommendedOnlyChange(enabled) },
        onFilterOnSaleOnlyChange = { enabled -> vm.onFilterOnSaleOnlyChange(enabled) },
        onSelectedTagsChange = { tags -> vm.onSelectedTagsChange(tags) },
        onClearSortAndFilters = { vm.clearHomeSortAndFilters() },
        onClearAll = { vm.clearHomeSortAndFilters() },
        onShowSortMenuChange = { show -> vm.onHomeShowSortMenuChange(show) },
        onShowFilterMenuChange = { show -> vm.onHomeShowFilterMenuChange(show) },

        onShowPriceMenuChange = { show -> vm.onHomeShowPriceMenuChange(show) },
        onPriceMinDraftChange = { v -> vm.onHomePriceMinDraftChange(v) },
        onPriceMaxDraftChange = { v -> vm.onHomePriceMaxDraftChange(v) },
        onApplyPriceRange = { vm.onHomeApplyPriceRange() },
        onClearPriceRange = { vm.onHomeClearPriceRange() },

        )
    val loginActions = ShowcaseLoginActions(
        onUsernameDraftChange = { vm.onLoginUsernameDraftChange(it) },
        onPasswordDraftChange = { vm.onLoginPasswordDraftChange(it) },

        // ✅ 自动登录（仅本机）
        onRememberMeChange = { checked -> vm.setLoginRememberMe(context, checked) },

        // ✅ 登录需要 context（成功时把“保持登录态”写入 prefs）
        onLogin = { u, p -> vm.tryAdminLogin(context, u, p) },
        onBackToHome = { vm.backToHome() }
    )

    val adminActions = ShowcaseAdminActions(
        onBackToHome = { vm.backToHome() },   // ✅ Home：永远回 Home
        onBack = { vm.backFromAdmin() },      // ✅ Back：按当前 Admin 层级回上一页
        onLogout = { vm.adminLogout(context) },
        onRetryPendingSync = { vm.retryPendingSync(context) },


        // ✅ Refresh（方案 B）
        onRefresh = { vm.refresh(context) },


        onSelectCategory = { category ->
            vm.onCategorySelected(category)
        },

        onAddCategory = { category ->
            vm.addCategory(category)
        },

        onDeleteCategory = { category ->
            vm.removeCategory(context, category)
        },

        // ✅ 分类重命名：接入 VM
        onRenameCategory = { oldName, newName ->
            vm.renameCategory(context, oldName, newName)
        },
        onOpenItemsManager = { vm.openAdminItemsScreen(context) },
        onOpenCategoriesManager = { vm.openAdminCategoriesScreen(context) },
        onOpenStoreProfile = { vm.openStoreProfile(context) },

        // ✅ 新增：Admin Home -> ChangePassword Screen
        onOpenChangePassword = { vm.openChangePasswordPage() },

// ✅ 新增：Admin 页入口 -> 商家聊天列表页
        onOpenMerchantChatList = { vm.openMerchantChatList(context) },
        onOpenAnnouncementPublisher = { vm.openAdminAnnouncementPublisher() },


        onAddNewDish = {
            vm.openNewDishScreen(context)
        },


        onEditDish = { dishId ->
            if (dishId.isBlank()) {
                vm.openNewDishScreen(context)
            } else {
                vm.openEditScreen(context, dishId)
            }
        },

        onDeleteDish = { dishId ->
            if (dishId.isNotBlank()) {
                vm.requestDeleteDish(dishId)
            }
        },

        // ✅ AdminItems：勾选/清空/批量删除
        onToggleSelectDish = { dishId ->
            vm.toggleAdminDishSelected(dishId)
        },
        onClearSelectedDishes = {
            vm.clearAdminDishSelection()
        },
        onDeleteSelectedDishes = {
            vm.deleteSelectedDishes(context)
        },

        onDismissPendingDelete = {
            vm.dismissPendingDelete()
        },

        onConfirmPendingDelete = {
            val id: String = uiState.pendingDeleteDishId?.trim().orEmpty()
            if (id.isNotEmpty()) {
                val deleteDishById: (android.content.Context, String) -> Unit = vm::deleteDish
                deleteDishById(context, id)
            }
        },

        onSetAdminCredentials = { username, password ->
            vm.setAdminCredentials(context, username, password)
        },
        onAdminUsernameDraftChange = { vm.onAdminUsernameDraftChange(it) },
        onAdminPasswordDraftChange = { vm.onAdminPasswordDraftChange(it) },
        onSaveAdminCredentials = { vm.saveAdminCredentialsFromDraft(context) },

        onRequestDeleteCategory = { cat -> vm.requestDeleteCategory(cat) },
        onDismissCategoryDeleteDialogs = { vm.dismissCategoryDeleteDialogs() },
        onConfirmPendingDeleteCategory = { vm.confirmPendingDeleteCategory(context) },

        onItemsSearchQueryChange = { vm.onAdminItemsSearchQueryChange(it) },
        onClearItemsSearchQuery = { vm.clearAdminItemsSearchQuery() },
        onItemsSortModeChange = { vm.onAdminItemsSortModeChange(it) },
        onItemsFilterRecommendedChange = { vm.onAdminItemsFilterRecommendedChange(it) },
        onItemsFilterHiddenOnlyChange = { vm.onAdminItemsFilterHiddenOnlyChange(it) },
        onItemsFilterDiscountOnlyChange = { vm.onAdminItemsFilterDiscountOnlyChange(it) },

        // ✅ AdminItems：price range（对齐 Home Filter Sheet）
        onPriceMinDraftChange = { vm.onAdminItemsPriceMinDraftChange(it) },
        onPriceMaxDraftChange = { vm.onAdminItemsPriceMaxDraftChange(it) },
        onApplyPriceRange = { vm.onAdminItemsApplyPriceRange() },
        onClearPriceRange = { vm.onAdminItemsClearPriceRange() },
// =========================
    )
    val detailActions =
        ShowcaseDetailActions(
            onBack = { vm.backFromDetail() },

            onEdit = {
                val id = uiState.selectedDish?.id?.toString()
                if (!id.isNullOrBlank()) {
                    vm.openEditScreen(context, id)
                }
            },
            onToggleFavorite = {
                val id = uiState.selectedDish?.id?.toString() ?: return@ShowcaseDetailActions
                vm.toggleFavorite(id)
            },
            // ✅ 统一入口：详情页 -> Chat（与底栏一致，带 pending 商品卡片）
            onOpenChat = {
                vm.openChatFromBottomBar(context)
            },

            onOpenImage = { _ -> },
            onImageIndexChanged = { idx ->
                vm.onDetailImageIndexChanged(idx)
            },

            // ✅ 新增：全屏预览长按保存（UI 只触发，VM 负责下载/落相册）
            onSavePreviewImage = { url ->
                vm.saveChatPreviewImage(context, url)
            },

            onBackToHome = { vm.backToHome() },
        )

    val chatRemainingImageSlots = (9 - uiState.chat.draftImageUris.size).coerceAtLeast(0)
    val chatMultiPickerMaxItems = chatRemainingImageSlots.coerceIn(2, 9)

    val pickChatImagesLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(chatMultiPickerMaxItems)
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        vm.onChatImagesSelected(context, uris)
    }

    val pickSingleChatImageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        vm.onChatImagesSelected(context, listOf(uri))
    }

    val takeChatPictureLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        vm.onChatCameraResult(context, success)
    }

    val launchChatCameraWithBestAvailablePath: () -> Unit = launch@{
        val uri = vm.prepareChatCameraCapture(context)
        if (uri == null) {
            vm.onChatFullCameraUnavailable()
            return@launch
        }

        takeChatPictureLauncher.launch(uri)
    }

    val requestChatCameraPermissionLauncher =
        androidx.activity.compose.rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted: Boolean ->
            if (!granted) {
                vm.onChatCameraPermissionDenied()
                return@rememberLauncherForActivityResult
            }

            launchChatCameraWithBestAvailablePath()
        }

    val editRemainingImageSlots = (9 - (editDraft?.imageUris?.size ?: 0)).coerceAtLeast(0)
    val editMultiPickerMaxItems = editRemainingImageSlots.coerceIn(2, 9)

    val pickEditDishImagesLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(editMultiPickerMaxItems)
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        vm.onEditImagesSelected(context, uris)
    }

    val pickSingleEditDishImageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        vm.onEditImagesSelected(context, listOf(uri))
    }
    val editActions = ShowcaseEditDishActions(
        onBackToHome = { vm.discardEditDraftAndGoHome() },
        onBack = {
            vm.backToAdminFromEdit()
        },

        onNameChange = { v -> vm.onEditNameChange(v) },
        onPriceChange = { v -> vm.onEditPriceChange(v) },
        onDiscountPriceChange = { v -> vm.onEditDiscountPriceChange(v) },
        onDescriptionChange = { v -> vm.onEditDescriptionChange(v) },

        onCategorySelected = { v -> vm.onEditCategorySelected(v) },
        onToggleRecommended = { v -> vm.onEditToggleRecommended(v) },
        onToggleHidden = { v -> vm.onEditToggleHidden(v) },

        onPickImage = {
            when {
                editRemainingImageSlots <= 0 -> {
                    vm.onEditImageLimitReached()
                }

                editRemainingImageSlots == 1 -> {
                    pickSingleEditDishImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }

                else -> {
                    pickEditDishImagesLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            }
        },

        onImagePicked = { uriString ->
            runCatching { Uri.parse(uriString) }
                .getOrNull()
                ?.let { uri -> vm.onEditImageSelected(context, uri) }
        },

        onRemoveImage = { uriOrUrl ->
            vm.onEditRemoveImage(uriOrUrl)
        },

        onMoveImage = { from, to ->
            android.util.Log.d("NDJC_DND", "WIRING onMoveImage from=$from to=$to")
            vm.onEditMoveImage(from, to)
        },

        onSave = {
            vm.onEditSave(context)
        },

        onDelete = vm.getEditDeleteAction(),
        onDismissError = {
            vm.dismissEditValidationError()
        }

    )





    val pickLogoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.toString()?.let { picked ->
            // 选择结果 -> 逻辑模块（VM）落地
            vm.onStoreProfileLogoPicked(picked)
        }
    }

    val storeProfileCoverCount = uiState.storeProfileDraft
        ?.coverUrl
        ?.lineSequence()
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?.distinct()
        ?.count()
        ?: 0

    val storeProfileRemainingCoverSlots = (9 - storeProfileCoverCount).coerceAtLeast(0)
    val storeProfileMultiPickerMaxItems = storeProfileRemainingCoverSlots.coerceIn(2, 9)

    val pickCoversLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = storeProfileMultiPickerMaxItems)
    ) { uris ->
        if (uris.isNullOrEmpty()) return@rememberLauncherForActivityResult
        vm.onStoreProfileCoversPicked(uris.map { it.toString() })
    }

    val pickSingleCoverLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        vm.onStoreProfileCoversPicked(listOf(uri.toString()))
    }

    val storeProfileActions = ShowcaseStoreProfileActions(
        onBackToHome = { vm.discardStoreProfileDraftAndGoHome() },
        onBack = { vm.backFromStoreProfile() },
        onOpenChat = { vm.openChatFromStoreProfile(context) },
        onStartEdit = { vm.startEditStoreProfile() },
        onCancelEdit = { vm.cancelEditStoreProfile() },

        onTitleChange = { v -> vm.onStoreProfileDraftTitleChange(v) },
        onSubtitleChange = { v -> vm.onStoreProfileDraftSubtitleChange(v) },
        onDescriptionChange = { v -> vm.onStoreProfileDraftDescriptionChange(v) },
        onAddressChange = { v -> vm.onStoreProfileDraftAddressChange(v) },
        onHoursChange = { v -> vm.onStoreProfileDraftHoursChange(v) },
        onMapUrlChange = { v -> vm.onStoreProfileDraftMapUrlChange(v) },
        onBusinessStatusChange = { v -> vm.onStoreProfileDraftBusinessStatusChange(v) },
        onServiceChange = { index, v -> vm.onStoreProfileServiceChange(index, v) },
        onServiceAdd = { v -> vm.onStoreProfileServiceAdd(v) },
        onServiceRemove = { index -> vm.onStoreProfileServiceRemove(index) },


        onExtraContactNameChange = { index, v -> vm.onStoreProfileExtraContactNameChange(index, v) },
        onExtraContactValueChange = { index, v -> vm.onStoreProfileExtraContactValueChange(index, v) },
        onExtraContactAdd = { name, value -> vm.onStoreProfileExtraContactAdd(name, value) },
        onExtraContactRemove = { index -> vm.onStoreProfileExtraContactRemove(index) },

        // ✅ UI 点按钮 -> wiring 拉系统选择器
        onPickLogo = {
            pickLogoLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onPickCovers = {
            when {
                storeProfileRemainingCoverSlots <= 0 -> {
                    vm.onStoreProfileCoverLimitReached()
                }

                storeProfileRemainingCoverSlots == 1 -> {
                    pickSingleCoverLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }

                else -> {
                    pickCoversLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            }
        },
        onRemoveLogo = {
            vm.onStoreProfileLogoRemove()
        },
        // ✅ UI 触发删除；逻辑模块（VM）执行更新
        onRemoveCover = { uriOrUrl ->
            vm.onStoreProfileCoverRemove(uriOrUrl)
        },
        onMoveCover = { from, to ->
            vm.onStoreProfileCoverMove(from, to)
        },
        onOpenMap = { url -> vm.openMap(context, url) },
        onCopy = { label, text -> vm.copyToClipboard(context, label, text) },
        onSavePreviewImage = { url -> vm.saveChatPreviewImage(context, url) },
        onChangePassword = { vm.openChangePasswordPage() },
        onSave = { vm.saveStoreProfile(context) },
    )


// -----------------------
// Chat state/actions 映射
// -----------------------
    val chatStoreTitle = uiState.storeProfile
        ?.title
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: "Chat"

    val resolvedChatTitle = if (uiState.chat.useStoreTitle) {
        chatStoreTitle
    } else {
        uiState.chat.title
            .trim()
            .takeIf { it.isNotEmpty() }
            ?: "Chat"
    }

    val chatState = uiState.chat.copy(
        title = resolvedChatTitle
    )

    val chatMediaActions = com.ndjc.feature.showcase.ShowcaseChatMediaActions(
        onBackToHome = { vm.backToHome() },
        onBack = { vm.chatCloseMediaGallery() },

        // ✅ 复用 Chat 页同一套保存逻辑：UI 只触发，VM 负责下载/落相册
        onSavePreviewImage = { url ->
            vm.saveChatPreviewImage(context, url)
        }
    )


    val chatActions = com.ndjc.feature.showcase.
    ShowcaseChatActions(
        onUseProductCardAsPending = { p ->
            vm.chatUseProductCardAsPending(p)
        },
        onJumpToMessage = { messageId ->
            vm.chatJumpToMessageFromQuote(messageId)
        },
        onBackToHome = { vm.backToHome() },
        onBack = { vm.backFromChat() },
        onDraftChange = { vm.onChatDraftChange(it) },
        onSend = { vm.sendChat() },
        onRetry = { id -> vm.retryChat(id) },
        onRefresh = { vm.refreshChatLatest() },

        onQuoteMessage = { id -> vm.chatQuoteMessage(id) },
        onCancelQuote = { vm.chatCancelQuote() },

        onEnterSelection = { id -> vm.chatEnterSelection(id) },
        onToggleSelection = { id -> vm.chatToggleSelection(id) },
        onExitSelection = { vm.chatExitSelection() },
        onDeleteMessage = { id -> vm.chatDeleteMessage(id) },
        onDeleteSelected = { vm.chatDeleteSelected() },

        onOpenSearchResults = { vm.chatOpenSearchResults() },
        onCloseSearchResults = { vm.chatCloseSearchResults() },
        onOpenMediaGallery = { vm.chatOpenMediaGallery() },

        onFindQueryChange = { q -> vm.chatSetFindQuery(q) },

        onJumpToFoundMessage = { id -> vm.chatJumpToFoundMessage(id) },

        onOpenThreadFromSearch = { conversationId, messageId ->
            vm.chatOpenThreadFromSearch(conversationId, messageId)
        },

        onTogglePinned = { vm.chatTogglePinned() },

        onPickImages = {
            when {
                chatRemainingImageSlots <= 0 -> {
                    vm.onChatImageLimitReached()
                }

                chatRemainingImageSlots == 1 -> {
                    pickSingleChatImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }

                else -> {
                    pickChatImagesLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            }
        },
        onOpenCamera = {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (granted) {
                launchChatCameraWithBestAvailablePath()
            } else {
                requestChatCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        onRemoveDraftImage = { uriString ->
            vm.chatRemoveDraftImage(uriString)
        },
        onSavePreviewImage = { url ->
            vm.saveChatPreviewImage(context, url)
        },
        onSendPendingProduct = { vm.sendPendingProductShare() },
        onClearPendingProduct = { vm.clearPendingProductShare() },

        onOpenProductDetail = { dishId ->
            vm.openDetailById(dishId)
        },

        buildProductClipboardPayload = { p ->
            vm.buildChatProductClipboardPayload(p)
        },

        )
    val merchantChatListActions = ShowcaseMerchantChatListActions(
        onBackToHome = { vm.backToHome() },
        onBack = { vm.backFromMerchantChatList() },
        onRefresh = { vm.refreshMerchantChatListByUser() },
        onOpenThread = { threadId, threadTitle ->
            vm.openChatFromMerchantList(context, threadId, threadTitle)
        },
        onOpenChatSearch = { vm.chatOpenSearchResults() },

        // ✅ 长按菜单：删除/置顶/已读
        onDeleteThread = { threadId -> vm.merchantChatListDeleteThread(threadId) },
        onTogglePin = { threadId, pinned -> vm.merchantChatListTogglePin(threadId, pinned) },
        onMarkRead = { threadId -> vm.merchantChatListMarkRead(threadId) },

        // ✅ 新增：改名（别名/备注）
        onRenameThread = { threadId, newName ->
            vm.merchantChatListRenameThread(threadId, newName)
        }
    )
    val favoritesState by vm.favoritesUiStateFlow.collectAsState()
    val favoritesActions = remember {
        ShowcaseFavoritesActions(
            onBackToHome = { vm.backToHome() },
            onBack = { vm.closeFavorites() },
            onQueryChange = { vm.onFavoritesQueryChange(it) },
            onOpenDetail = { dishId -> vm.openDetailById(dishId) },

            onToggleSelect = { dishId -> vm.toggleFavoritesSelection(dishId) },
            onClearSelection = { vm.clearFavoritesSelection() },
            onDeleteSelected = { vm.deleteSelectedFavorites() },

            onSortModeChange = { vm.onFavoritesSortModeChange(it) },
            onFilterRecommendedOnlyChange = { vm.onFavoritesFilterRecommendedOnlyChange(it) },
            onFilterOnSaleOnlyChange = { vm.onFavoritesFilterOnSaleOnlyChange(it) },
            onClearSortAndFilters = { vm.onFavoritesClearSortAndFilters() },
            onShowSortMenuChange = { vm.onFavoritesShowSortMenuChange(it) },
            onShowFilterMenuChange = { vm.onFavoritesShowFilterMenuChange(it) },

            onCategorySelected = { vm.onFavoritesCategorySelected(it) },
            onApplyPriceRange = { vm.onFavoritesApplyPriceRange() },
            onShowPriceMenuChange = { show -> vm.onFavoritesShowPriceMenuChange(show) },
            onPriceMinDraftChange = { v -> vm.onFavoritesPriceMinDraftChange(v) },
            onPriceMaxDraftChange = { v -> vm.onFavoritesPriceMaxDraftChange(v) },
            onClearPriceRange = { vm.onFavoritesClearPriceRange() },

            )
    }
    val announcementsState = remember(
        uiState.announcements,
        uiState.isLoading,
        uiState.statusMessage,
        uiState.pushTargetAnnouncementId
    ) {
        ShowcaseAnnouncementsUiState(
            items = uiState.announcements,
            isLoading = uiState.isLoading,
            statusMessage = uiState.statusMessage,
            focusedAnnouncementId = uiState.pushTargetAnnouncementId
        )
    }

    val announcementsActions = remember {
        ShowcaseAnnouncementsActions(
            onBackToHome = { vm.backToHome() },
            onBack = { vm.backFromAnnouncements() },
            onRefresh = { vm.refreshAnnouncements() },
            onOpenAnnouncement = { id -> vm.onAnnouncementExpanded(id) },
            onTrackAnnouncementView = { id -> vm.onAnnouncementExpanded(id) },
            onOpenAnnouncementImage = { id -> vm.onAnnouncementImageOpened(id) },
            onConsumeFocusedAnnouncement = { vm.consumePushAnnouncementTarget() }
        )
    }

// ------------------------- Admin Announcement Edit (state/actions) -------------------------

// ✅ 选择图片：wiring 负责调用系统 picker，VM 只收 url
    val pickAnnouncementCoverLauncher =
        androidx.activity.compose.rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                vm.onAdminAnnouncementCoverPicked(uri.toString())
            }
        }

    val adminDraftCards = uiState.adminAnnouncementDraftItems

    val hasSelection = uiState.adminAnnouncementSelectedIds.isNotEmpty()
    val hasComposerInput =
        uiState.adminAnnouncementCoverDraftUrl != null ||
                uiState.adminAnnouncementBodyDraft.trim().isNotBlank()

    val composerExpanded = uiState.adminAnnouncementComposerExpanded
    val draftItems = adminDraftCards
    val previewItem = draftItems.firstOrNull { it.id == uiState.adminAnnouncementPreviewId }

    val adminAnnouncementEditState = ShowcaseAnnouncementEditUiState(
        coverDraftUrl = uiState.adminAnnouncementCoverDraftUrl,
        bodyDraft = uiState.adminAnnouncementBodyDraft,
        editingId = uiState.adminAnnouncementEditingId,
        errorMessage = uiState.adminAnnouncementError,
        successMessage = uiState.adminAnnouncementSuccess,
        statusMessage = uiState.statusMessage,
        isSubmitting = uiState.adminAnnouncementIsSubmitting,
        isBlockingInput = uiState.adminAnnouncementIsBlocking,
        composerExpanded = composerExpanded,
        canStartNew = !composerExpanded && !uiState.adminAnnouncementIsSubmitting,
        canDeleteSelected = hasSelection && !uiState.adminAnnouncementIsSubmitting,
        canSaveDraft = composerExpanded && hasComposerInput && !uiState.adminAnnouncementIsSubmitting,
        canPublish = ((composerExpanded && hasComposerInput) || hasSelection) && !uiState.adminAnnouncementIsSubmitting,
        draftItems = draftItems,
        selectedIds = uiState.adminAnnouncementSelectedIds,
        previewItem = previewItem,
        previewVisible = previewItem != null,
        hasUnsavedChanges = vm.hasUnsavedAdminAnnouncementDraft()
    )
    val adminAnnouncementEditActions = remember {
        ShowcaseAnnouncementEditActions(
            onBackToHome = { vm.discardAdminAnnouncementDraftAndGoHome() },
            onBack = { vm.discardAdminAnnouncementDraftAndBack() },
            onStartNew = { vm.onAdminAnnouncementStartNew() },
            onPickCover = {
                pickAnnouncementCoverLauncher.launch("image/*")
            },
            onRemoveCover = { vm.onAdminAnnouncementClearCover() },
            onOpenCoverPreview = { },
            onBodyChange = { vm.onAdminAnnouncementBodyDraftChange(it) },
            onSaveDraft = { vm.onAdminAnnouncementSaveDraft() },
            onPushNow = { vm.onAdminAnnouncementPushNow() },
            onOpenItem = { id -> vm.onAdminAnnouncementOpenItem(id) },
            onPreviewItem = { id -> vm.onAdminAnnouncementPreviewItem(id) },
            onDismissPreview = { vm.onAdminAnnouncementDismissPreview() },
            onToggleSelect = { id -> vm.onAdminAnnouncementToggleSelect(id) },
            onClearSelection = { vm.onAdminAnnouncementClearSelection() },
            onDeleteSelected = { vm.onAdminAnnouncementDeleteSelected() }
        )
    }
// 渲染分发：Home/Detail/Store/Chat/Favorites/Announcements 统一显示底栏（Overlay 方案）
    val showBottomBar =
        (uiState.screen == ShowcaseScreen.Home
                || uiState.screen == ShowcaseScreen.Detail
                || uiState.screen == ShowcaseScreen.StoreProfileView
                || uiState.screen == ShowcaseScreen.Chat
                || uiState.screen == ShowcaseScreen.Favorites
                || uiState.screen == ShowcaseScreen.Announcements)

    if (showBottomBar) {
        val selectedTab: ShowcaseBottomBarTab? = when (uiState.screen) {
            ShowcaseScreen.StoreProfileView -> ShowcaseBottomBarTab.Store
            ShowcaseScreen.Chat -> ShowcaseBottomBarTab.Chat
            ShowcaseScreen.Favorites -> ShowcaseBottomBarTab.Favorites
            ShowcaseScreen.Announcements -> ShowcaseBottomBarTab.Announcements

            // Home/Detail 保持不强调选中
            ShowcaseScreen.Home -> null
            ShowcaseScreen.Detail -> null

            else -> null
        }

        val showChatEntryDot =
            chatEntryDot && uiState.screen != ShowcaseScreen.Chat
        val showAnnouncementsEntryDot =
            announcementsEntryDot && uiState.screen != ShowcaseScreen.Announcements

// ✅ Chat 页不显示底栏；全屏查看图片时也不显示底栏
        val isFullscreenViewerVisible by NdjcFullscreenViewerRegistry.visible
        val showBottomBarOverlay =
            (uiState.screen != ShowcaseScreen.Chat) && !isFullscreenViewerVisible

// ✅ Overlay 底栏高度测量（只有在 showBottomBarOverlay=true 才会生效）
        val density = LocalDensity.current
        val bottomBarHeightPxState = remember { mutableIntStateOf(0) }
        val bottomBarHeightDp = with(density) { bottomBarHeightPxState.intValue.toDp() }

// ✅ Chat 页 / 全屏看图时都不预留底栏空间
        val contentBottomPadding = if (showBottomBarOverlay) bottomBarHeightDp else 0.dp

        Box(modifier = Modifier.fillMaxSize()) {

            Box(modifier = Modifier.fillMaxSize().padding(bottom = contentBottomPadding)) {
                // ✅ 弱网/失败提示：全局叠加，不侵入 UI 包
                val err = uiState.syncErrorMessage
                if (!err.isNullOrBlank()) {
                    NdjcSyncErrorBanner(
                        message = err,
                        onRetry = { vm.retryLast(context) },
                        onDismiss = { vm.clearSyncError() }
                    )
                }

                when (uiState.screen) {
                    ShowcaseScreen.Home -> ui.Home(state = homeState, actions = homeActions)
                    ShowcaseScreen.Detail -> ui.Detail(state = detailState, actions = detailActions)
                    ShowcaseScreen.StoreProfileView -> ui.StoreProfileView(
                        state = storeProfileState,
                        actions = storeProfileActions
                    )
                    ShowcaseScreen.Favorites -> ui.Favorites(state = favoritesState, actions = favoritesActions)
                    ShowcaseScreen.Announcements -> ui.Announcements(
                        state = announcementsState,
                        actions = announcementsActions
                    )

                    ShowcaseScreen.Chat -> {
                        ui.ChatThread(state = chatState, actions = chatActions)
                    }

                    else -> { /* 不会走到 */ }
                }
            }

            // ✅ 关键：Chat 页时，这个 Box 整段不进入组合树（=完全不渲染底栏）
            if (showBottomBarOverlay) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { size -> bottomBarHeightPxState.intValue = size.height }
                ) {
                    ShowcaseBottomBar(
                        selected = selectedTab,
                        showChatDot = showChatEntryDot,
                        showAnnouncementsDot = showAnnouncementsEntryDot,
                        onOpenStoreProfile = { vm.openStoreProfileView(context) },
                        onOpenChat = { vm.openChatFromBottomBar(context) },
                        onOpenFavorites = { vm.openFavorites() },
                        onOpenAnnouncements = { vm.openAnnouncementsFromBottomBar() }
                    )
                }
            }
        }
    } else {
        when (uiState.screen) {
            ShowcaseScreen.Login -> ui.Login(state = loginState, actions = loginActions)
            ShowcaseScreen.Admin -> ui.Admin(state = adminState, actions = adminActions)

            // ✅ 方案 B：独立 Screen 分发
            ShowcaseScreen.AdminItems -> ui.AdminItems(state = adminState, actions = adminActions)
            ShowcaseScreen.AdminCategories -> ui.AdminCategories(state = adminState, actions = adminActions)
            ShowcaseScreen.AdminAnnouncementEdit -> ui.AdminAnnouncementEdit(
                state = adminAnnouncementEditState,
                actions = adminAnnouncementEditActions
            )
            ShowcaseScreen.Edit -> ui.EditDish(state = editState, actions = editActions)
            ShowcaseScreen.StoreProfile -> ui.StoreProfileEdit(state = storeProfileState, actions = storeProfileActions)

            ShowcaseScreen.ChangePassword -> ui.ChangePassword(
                state = changePasswordState,
                actions = changePasswordActions
            )

            ShowcaseScreen.MerchantChatList -> ShowcaseMerchantChatListScreen(
                state = uiState,
                actions = merchantChatListActions
            )

            // ✅ ChatSearchResults：不要再额外 statusBarsPadding（否则白卡整体下移）
            ShowcaseScreen.ChatSearchResults -> {
                ui.ChatThread(
                    state = chatState.copy(isSearchResults = true),
                    actions = chatActions
                )
            }

            // ✅ ChatMedia：不要再额外 statusBarsPadding（否则白卡整体下移）
            ShowcaseScreen.ChatMedia -> {
                ui.ChatMedia(state = chatState, actions = chatMediaActions)
            }

            ShowcaseScreen.Favorites -> ui.Favorites(state = favoritesState, actions = favoritesActions)

// Home / Detail / StoreProfileView / Chat / Announcements 已在上面分支处理
            ShowcaseScreen.Home,
            ShowcaseScreen.Detail,
            ShowcaseScreen.StoreProfileView,
            ShowcaseScreen.Chat,
            ShowcaseScreen.Announcements -> {}
        }
    }



}
private fun demoDishToHomeDish(d: DemoDish): ShowcaseHomeDish {
    val title = d.nameZh.ifBlank { d.nameEn }
    val subtitle = d.descriptionEn.takeIf { it.isNotBlank() }

    val original = d.originalPrice.toDouble()
    val discount = d.discountPrice?.toDouble()

    return ShowcaseHomeDish(
        id = d.id.toString(),
        title = title,
        subtitle = subtitle,
        category = d.category.takeIf { it.isNotBlank() },

        // ✅ 兼容：price 用“折后优先”
        price = (discount ?: original),

        // ✅ 新增：用于卡片同时展示
        originalPrice = original,
        discountPrice = discount,

        isRecommended = d.isRecommended,
        isSoldOut = d.isSoldOut,
        isHidden = d.isHidden,
        imagePreviewUrl = d.imageUrls.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: d.imageUri?.toString(),
        clickCount = d.clickCount
    )
}

private fun formatPrice(value: Float): String {
    return if (value % 1f == 0f) value.toInt().toString() else value.toString()
}
fun mapSyncOverviewState(v: SyncOverviewState): ShowcaseSyncOverviewState {
    return when (v) {
        SyncOverviewState.Idle -> ShowcaseSyncOverviewState.Idle
        SyncOverviewState.Syncing -> ShowcaseSyncOverviewState.Syncing
        SyncOverviewState.HasPending -> ShowcaseSyncOverviewState.HasPending
        SyncOverviewState.Failed -> ShowcaseSyncOverviewState.Failed
    }
}

