package com.ndjc.feature.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ndjc.feature.demo.Routes.DETAIL
import com.ndjc.feature.demo.Routes.DEMO
import com.ndjc.feature.demo.Routes.HOME
import com.ndjc.ui.neu.demo.DemoScreen

/**
 * App 的页面树入口：这里把 DemoScreen 正式注册为 /demo 路由。
 * MainActivity 已经把 startRoute 传进来，这里直接用。
 */
@Composable
fun FeatureDemoEntry(
    navController: NavHostController,
    startRoute: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startRoute,
        modifier = modifier
    ) {
        // ——现有页面（占位写法，若你已有同名页面可保留自己的实现）——
        composable(HOME) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Home")
            }
        }
        composable(DETAIL) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Detail")
            }
        }

        // ——新增 Demo 组件总览页——
        composable(DEMO) {
            DemoScreen()
        }
    }
}
