package com.ndjc.app.navigation  // 固定源码包名

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

    const val Home = "home"
    const val Detail = "detail/{id}"
    const val Post = "post"
}

@Composable
fun NavGraph(navController: NavHostController) {

    
    // 这里可切换为 AnimatedNavHost 并注入 enter/exit/popEnter/popExit 转场动画
    

    NavHost(navController = navController, startDestination = Routes.Home) {
        // BLOCK:ROUTE_HOME
        composable(Routes.Home) {
            HomeScreen(
                onOpenDetail = { id -> navController.navigate("detail/$id") },
                onCreatePost = { navController.navigate(Routes.Post) }
            )
        }
        // END_BLOCK

        // BLOCK:ROUTE_DETAIL
        composable("detail/{id}") { backStack ->
            val id = backStack.arguments?.getString("id") ?: "0"
            PostDetailScreen(
                id = id,
                onBack = { navController.popBackStack() }
            )
        }
        // END_BLOCK

        // BLOCK:ROUTE_POST
        composable(Routes.Post) {
            PostEditorScreen(
                onBack = { navController.popBackStack() }
            )
        }
        // END_BLOCK
    }
}
