package com.ndjc.app.navigation  // 需与实际包名保持一致

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ndjc.app.ui.screens.HomeScreen
import com.ndjc.app.feature.detail.PostDetailScreen
import com.ndjc.app.feature.post.PostEditorScreen

object Routes {
    // LIST:ROUTES
    // ${ITEM}
    // END_LIST

    // 文本锚点（由 NDJC 在物化时替换为真实字符串）
    const val Home: String = "home"
    const val Detail: String = "detail"

    // 其它路由保持模板默认值
    const val Post: String = "post"
}

@Composable
fun NavGraph(navController: NavHostController) {

    // IF:NAV_TRANSITIONS
    // 这里可切换为 AnimatedNavHost 并注入 enter/exit/popEnter/popExit 转场动画
    // END_IF

    NavHost(navController = navController, startDestination = Routes.Home) {

        // home
        composable(Routes.Home) {
            HomeScreen(
                onOpenDetail = { id ->
                    val path = Routes.Detail.replace("{id}", id)
                    navController.navigate(path)
                },
                onCreatePost = { navController.navigate(Routes.Post) }
            )
        }

        // detail
        composable(Routes.Detail) { backStack ->
            val id = backStack.arguments?.getString("id") ?: "0"
            PostDetailScreen(
                id = id,
                onBack = { navController.popBackStack() }
            )
        }

        // BLOCK:ROUTE_POST
        composable(Routes.Post) {
            PostEditorScreen(
                onBack = { navController.popBackStack() }
            )
        }
        // END_BLOCK
    }
}
