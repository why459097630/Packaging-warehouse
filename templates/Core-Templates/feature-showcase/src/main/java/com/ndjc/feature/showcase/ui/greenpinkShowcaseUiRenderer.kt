package com.ndjc.feature.showcase.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ndjc.feature.showcase.*



/**
 * 把 contracts 的 5 个页面入口，映射到 showcaseLayouts.kt 里的 Neu UI 实现。
 * 目的：UI 在逻辑模块内，但仍可通过替换 Renderer 实现“可剥离”。
 */
object GreenpowderShowcaseUiRenderer : ShowcaseUiRenderer {

    @Composable
    override fun Home(state: ShowcaseHomeUiState, actions: ShowcaseHomeActions) {
        ShowcaseHome(uiState = state, actions = actions, modifier = Modifier)
    }

    @Composable
    override fun Login(state: ShowcaseLoginUiState, actions: ShowcaseLoginActions) {
        ShowcaseLogin(uiState = state, actions = actions, modifier = Modifier)
    }

    @Composable
    override fun Admin(state: ShowcaseAdminUiState, actions: ShowcaseAdminActions) {
        ShowcaseAdmin(uiState = state, actions = actions, modifier = Modifier)
    }

    @Composable
    override fun AdminItems(state: ShowcaseAdminUiState, actions: ShowcaseAdminActions) {
        ShowcaseAdminItems(uiState = state, actions = actions, modifier = Modifier)
    }

    @Composable
    override fun AdminCategories(state: ShowcaseAdminUiState, actions: ShowcaseAdminActions) {
        ShowcaseAdminCategories(uiState = state, actions = actions, modifier = Modifier)
    }


    @Composable
    override fun Detail(state: ShowcaseDetailUiState, actions: ShowcaseDetailActions) {
        ShowcaseDishDetail(uiState = state, actions = actions, modifier = Modifier)
    }

    @Composable
    override fun EditDish(state: ShowcaseEditDishUiState, actions: ShowcaseEditDishActions) {
        ShowcaseEditDish(uiState = state, actions = actions, modifier = Modifier)
    }

    @Composable
    override fun StoreProfileView(state: ShowcaseStoreProfileUiState, actions: ShowcaseStoreProfileActions) {
        ShowcaseStoreProfileView(uiState = state, actions = actions, modifier = Modifier)
    }

    @Composable
    override fun StoreProfileEdit(state: ShowcaseStoreProfileUiState, actions: ShowcaseStoreProfileActions) {
        ShowcaseStoreProfileEdit(uiState = state, actions = actions, modifier = Modifier)
    }

    @Composable
    override fun ChangePassword(state: ShowcaseChangePasswordUiState, actions: ShowcaseChangePasswordActions) {
        ShowcaseChangePassword(state = state, actions = actions, modifier = Modifier)
    }


    @Composable
    override fun ChatThread(state: ShowcaseChatUiState, actions: ShowcaseChatActions) {
        ShowcaseChatThread(uiState = state, actions = actions, modifier = Modifier)
    }

    @Composable
    override fun ChatMedia(state: ShowcaseChatUiState, actions: ShowcaseChatMediaActions) {
        ShowcaseChatMedia(uiState = state, actions = actions, modifier = Modifier)
    }

    @Composable
    override fun Favorites(state: ShowcaseFavoritesUiState, actions: ShowcaseFavoritesActions) {
        ShowcaseFavoritesScreen(state = state, actions = actions)
    }

    @Composable
    override fun Announcements(
        state: ShowcaseAnnouncementsUiState,
        actions: ShowcaseAnnouncementsActions
    ) {
        ShowcaseAnnouncementsScreen(
            state = state,
            actions = actions
        )
    }

    @Composable
    override fun AdminAnnouncementEdit(
        state: ShowcaseAnnouncementEditUiState,
        actions: ShowcaseAnnouncementEditActions
    ) {
        ShowcaseAdminAnnouncementEdit(
            state = state,
            actions = actions
        )
    }

}




