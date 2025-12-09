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
import com.ndjc.core.skeleton.resolveTabRoutesFromModules

/**
 * NDJC 核心 NavHost。
 *
 * 真正的“模块 → 具体页面”映射不再直接依赖 feature 模块，
 * 而是通过上层传入的 resolveScreen 回调来完成：
 *
 * - CoreNavHost 只负责：
 *   - 读取 assembly.modules
 *   - 决定是否启用某个 route（"home" / "about"）
 * - 具体用哪个 Screen（HomeScreen / AboutScreen），由 app 层实现 resolveScreen。
 */
@Composable
fun CoreNavHost(
    nav: NavHostController,
    startRoute: String,
    assembly: Assembly,
    modifier: Modifier = Modifier,
    resolveScreen: @Composable (routeId: String, nav: NavHostController) -> Unit,
) {
    val modules = assembly.modules

    NavHost(
        navController = nav,
        startDestination = startRoute,
        modifier = modifier
    ) {
        // ---------- home ----------
        if ("feature-home-basic" in modules) {
            composable("home") {
                resolveScreen("home", nav)
            }
        }

        // ---------- about ----------
        if ("feature-about-basic" in modules) {
            composable("about") {
                resolveScreen("about", nav)
            }
        }

        // ---------- fallback ----------
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
