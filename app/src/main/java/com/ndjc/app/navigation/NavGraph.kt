package com.ndjc.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ndjc.app.feature.feed.FeedScreen
import com.ndjc.app.feature.detail.PostDetailScreen
import com.ndjc.app.feature.post.PostEditorScreen

object Routes {

    const val Home = "home"
    const val Detail = "detail/{id}"
    const val Post = "post"
}

@Composable
fun NavGraph(nav: NavHostController) {

    // 可注入导航转场或 AnimatedNavHost
    // END_BLOCK

    NavHost(navController = nav, startDestination = Routes.Home) {

        composable(Routes.Home) {
            FeedScreen(
                onOpenDetail = { id -> nav.navigate("detail/$id") },
                onCreatePost = { nav.navigate(Routes.Post) }
            )
        }
        // END_BLOCK

        composable("detail/{id}") { backStack ->
            val id = backStack.arguments?.getString("id") ?: "0"
            PostDetailScreen(id = id, onBack = { nav.popBackStack() })
        }
        // END_BLOCK

        composable(Routes.Post) {
            PostEditorScreen(onBack = { nav.popBackStack() })
        }
        // END_BLOCK
    }
}
