package com.ndjc.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ndjc.app.feature.feed.FeedScreen
import com.ndjc.app.feature.detail.PostDetailScreen
import com.ndjc.app.feature.post.PostEditorScreen

object Routes {
    // LIST:ROUTES
    const val Home = "home"
    const val Detail = "detail/{id}"
    const val Post = "post"
}

@Composable
fun NavGraph(nav: NavHostController) {
    // BLOCK:NAV_TRANSITIONS
    // 需要动画导航的话 这里换成 AnimatedNavHost
    // END_BLOCK

    NavHost(navController = nav, startDestination = Routes.Home) {

        // BLOCK:ROUTE_HOME
        composable(Routes.Home) {
            FeedScreen(
                { id -> nav.navigate("detail/$id") },     // 第1个参数：打开详情
                { nav.navigate(Routes.Post) }             // 第2个参数：去发帖页
            )
        }
        // END_BLOCK

        // BLOCK:ROUTE_DETAIL
        composable("detail/{id}") { backStack ->
            val id = backStack.arguments?.getString("id") ?: "0"
            PostDetailScreen(
                id = id,
                onBack = { nav.popBackStack() }
            )
        }
        // END_BLOCK

        // BLOCK:ROUTE_POST
        composable(Routes.Post) {
            PostEditorScreen(
                onBack = { nav.popBackStack() }
            )
        }
        // END_BLOCK
    }
}
