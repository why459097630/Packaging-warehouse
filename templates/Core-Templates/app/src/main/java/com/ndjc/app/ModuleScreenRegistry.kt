package com.ndjc.app

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.compose.material3.Text
import com.ndjc.feature.restaurant.RestaurantHomeScreen

/**
 * app 层的「routeId → 具体 Screen」映射。
 *
 * - core-skeleton 只认识 "home" / "about" 这类 routeId
 * - 真正调用哪个 Composable，由这里决定
 */
@Composable
fun ResolveCoreScreen(
    routeId: String,
    nav: NavHostController,
) {
    when (routeId) {
        // 首页 → 餐厅首页
        "home" -> RestaurantHomeScreen(
            nav = nav,
        )

        // 关于页（占位版，本地简单实现，不再依赖 feature-about-basic 模块）
        "about" -> LocalAboutScreen()

        // 兜底（未知 route）
        else -> {
            // 先留空，或者后面统一跳到一个 missing-route 页面
        }
    }
}

/**
 * 本地占位 About 页面：
 * 只是为了满足 "about" 这个 route 存在，不再依赖 feature-about-basic 模块。
 */
@Composable
private fun LocalAboutScreen() {
    Text("NDJC demo about (placeholder)")
}
