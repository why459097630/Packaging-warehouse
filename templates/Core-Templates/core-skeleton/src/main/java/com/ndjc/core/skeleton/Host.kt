package com.ndjc.core.skeleton

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ndjc.core.skeleton.Assembly
import com.ndjc.core.skeleton.Hooks
import kotlinx.coroutines.launch

/**
 * NDJC 核心 AppHost：负责把 Assembly + Hook + 模板 UI 串起来。
 *
 * 注意：这里不直接依赖具体模块，只处理 routeId 字符串。
 */
@Composable
fun NDJCAppHost(
    assembly: Assembly,
    hooks: Hooks,

    // header / tabBar / content 这些插槽由模板实现注入
    header: @Composable () -> Unit = {},
    tabBar: @Composable (navigator: Navigator, currentRouteId: String) -> Unit = { _, _ -> },
    content: @Composable (nav: NavHostController, startRoute: String, modifier: Modifier) -> Unit,
) {
    val nav = rememberNavController()

    // 入口 route：优先用 ModuleRegistry 里声明的 entryRouteId，找不到就回退到 assembly.routingEntry
    val entryRoute = remember(assembly) {
        resolveEntryRouteFromModules(
            modules = assembly.modules,
            fallback = assembly.routingEntry
                .orEmpty()
                .ifBlank { "home" }
        )
    }

    // 当前路由追踪（供 Hook 与 tabBar 高亮）
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRouteId = backStackEntry?.destination?.route ?: entryRoute
    var previousRouteId by remember { mutableStateOf<String?>(null) }

    // 导航实现（对上层暴露 Navigator 接口，不泄漏 NavHostController）
val navigator = object : Navigator {
    override val currentRouteId: String
        get() = backStackEntry?.destination?.route ?: entryRoute

    override fun navigate(routeId: String) {
        if (routeId == currentRouteId) return

        nav.navigate(routeId) {
            popUpTo(entryRoute) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

    // Hook：进入 / 离开 route 的回调
    LaunchedEffect(currentRouteId) {
        if (previousRouteId != currentRouteId) {
            previousRouteId?.let { hooks.onLeaveRoute(it) }
            hooks.onEnterRoute(currentRouteId, emptyMap())
            previousRouteId = currentRouteId
        }
    }

    // Scaffold：顶部 header，底部 tabBar（语义化参数）
    Scaffold(
        topBar = { },
        bottomBar = { }
    ) { padding ->
        // 模板负责 content 的布局，这里保证内容至少能显示出来
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            content(nav, entryRoute, Modifier)
        }
    }
}
