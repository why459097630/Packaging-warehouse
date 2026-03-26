package com.ndjc.feature.showcase.ui

import androidx.compose.runtime.Composable
import com.ndjc.feature.showcase.*


/**
 * 逻辑模块与 UI 实现（内置 UI / UI 包）之间的唯一“剥离点”接口。
 * 任何 UI（放在逻辑模块内或 UI 包内）只要实现它，就能替换整个 showcase 的页面渲染。
 */
interface ShowcaseUiRenderer {

    @Composable
    fun Home(state: ShowcaseHomeUiState, actions: ShowcaseHomeActions)

    @Composable
    fun ChatThread(state: ShowcaseChatUiState, actions: ShowcaseChatActions)

    @Composable
    fun ChatMedia(state: ShowcaseChatUiState, actions: ShowcaseChatMediaActions)

    @Composable
    fun Login(state: ShowcaseLoginUiState, actions: ShowcaseLoginActions)

    @Composable
    fun Admin(state: ShowcaseAdminUiState, actions: ShowcaseAdminActions)

    @Composable
    fun AdminItems(state: ShowcaseAdminUiState, actions: ShowcaseAdminActions)

    @Composable
    fun AdminCategories(state: ShowcaseAdminUiState, actions: ShowcaseAdminActions)


    @Composable
    fun Detail(state: ShowcaseDetailUiState, actions: ShowcaseDetailActions)

    @Composable
    fun EditDish(state: ShowcaseEditDishUiState, actions: ShowcaseEditDishActions)

    @Composable
    fun StoreProfileView(state: ShowcaseStoreProfileUiState, actions: ShowcaseStoreProfileActions)

    @Composable
    fun StoreProfileEdit(state: ShowcaseStoreProfileUiState, actions: ShowcaseStoreProfileActions)

    @Composable
    fun ChangePassword(state: ShowcaseChangePasswordUiState, actions: ShowcaseChangePasswordActions)

    @Composable
    fun Favorites(state: ShowcaseFavoritesUiState, actions: ShowcaseFavoritesActions)

    @Composable
    fun Announcements(state: ShowcaseAnnouncementsUiState, actions: ShowcaseAnnouncementsActions)

    @Composable
    fun AdminAnnouncementEdit(state: ShowcaseAnnouncementEditUiState, actions: ShowcaseAnnouncementEditActions)
}

