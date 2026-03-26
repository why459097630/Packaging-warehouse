package com.ndjc.core.skeleton

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

/**
 * NDJC 核心 NavHost。
 *
 * 规则：
 * - startDestination 必须是 NavGraph 的 direct child，否则会直接崩溃。
 * - 因为模块/UI 包是可装配的，CoreNavHost 不能用 “modules 条件注册 home/about” 的方式来注册 startRoute，
 *   否则一旦装配的 modules 不包含某个模块，startRoute 就会缺失导致闪退。
 *
 * 结论：
 * - 永远注册 startRoute（以及你希望稳定存在的公共路由，如 about）
 * - 具体页面由上层 resolveScreen 决定；如果上层找不到 renderer，应自行跳 missing 或显示占位，但不应 crash。
 */
@Composable
fun CoreNavHost(
    nav: NavHostController,
    startRoute: String,
    assembly: Assembly,
    modifier: Modifier = Modifier,
    resolveScreen: @Composable (routeId: String, nav: NavHostController) -> Unit,
) {
    NavHost(
        navController = nav,
        startDestination = startRoute,
        modifier = modifier
    ) {
        // ✅ 必须：永远注册 startRoute，避免 “startDestination 不是 direct child” 崩溃
        composable(startRoute) {
            resolveScreen(startRoute, nav)
        }

        // ✅ 可选：如果你希望 about 作为稳定公共路由存在，也永远注册（由 resolveScreen 决定显示什么）
        composable("about") {
            resolveScreen("about", nav)
        }

        // ✅ fallback
        composable("missing") {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No page registered for route.")
            }
        }
    }
}
