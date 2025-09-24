package com.ndjc.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
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
    // 可注入动画或换成 AnimatedNavHost
    // END_BLOCK

    NavHost(navController = nav, startDestination = Routes.Home) {

        // BLOCK:ROUTE_HOME
        composable(Routes.Home) {
            FeedScreen(
                onOpenDetail = { id: String ->   // 显式声明类型，避免推断失败
                    nav.navigate("detail/$id")
                },
                onPostClick = {                   // 统一参数名
                    nav.navigate(Routes.Post)
                }
            )
        }
        // END_BLOCK

        // BLOCK:ROUTE_DETAIL
        composable(
            route = Routes.Detail,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStack ->
            val id = backStack.arguments?.getString("id") ?: "0"
            PostDetailScreen(
                id = id,
                onBack = { nav.popBackStack() }
            )
        }
        // END_BLOCK

        // BLOCK:ROUTE_POST
        composable(Routes.Post) {
            PostEditorScreen(onBack = { nav.popBackStack() })
        }
        // END_BLOCK
    }
}
