package com.ndjc.feature.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ndjc.core.ui.UiPackProvider

/**
 * AppRoot：唯一依赖 UiPack 的地方
 */
@Composable
fun FeatureAppRoot(startRoute: String) {
  // 约定的三个路由；你也可以接入自己已有的 Routes 常量
  val routes = listOf("/home", "/detail", "/demo")

  // 选中项：初始为 startRoute
  var selected by remember {
    mutableStateOf(
      if (routes.contains(startRoute)) startRoute else routes.first()
    )
  }
  val nav = rememberNavController()

  // 包一层当前 UI 包提供的主题
  UiPackProvider.current.Theme {
    Column(
      modifier = Modifier
        .fillMaxSize()
        // 全局背景：竖向柔和紫蓝渐变
        .background(
          brush = Brush.verticalGradient(
            colors = listOf(
              Color(0xFFBBA4FF), // 顶部稍亮、偏冷
              Color(0xFF9FB4FF), // 中段过渡
              Color(0xFF8EC5FF)  // 底部略偏蓝
            )
          )
        )
    ) {

      // 顶栏：标题用当前路由名（可按需换成 map）
      UiPackProvider.current.TopAppBar(title = titleOf(selected))

      // TabBar：来自当前 UI 包
      UiPackProvider.current.TabBar(
        routes = routes,
        selectedRoute = selected
      ) { route ->
        selected = route
        if (nav.currentDestination?.route != route) {
          nav.navigate(route) {
            launchSingleTop = true
            popUpTo(nav.graph.startDestinationId) { saveState = true }
            restoreState = true
          }
        }
      }

      // 内联 NavHost（不再依赖丢失的 com.ndjc.feature.demo.nav.FeatureNavHost）
      NavHost(
        navController = nav,
        startDestination = selected,
        modifier = Modifier.fillMaxSize()
      ) {
        composable("/home")   { HomeScreen() }
        composable("/detail") { DetailScreen() }
        composable("/demo")   { DemoScreenPlaceholder() }
      }
    }
  }
}

/* 你可以把下面三个占位页换成你已有的页面；
 * 之所以放在当前文件里，是为了去掉对“丢失包装器/包名”的依赖，让先编译通过。
 */
@Composable
private fun HomeScreen() { Text("Home") }

@Composable
private fun DetailScreen() { Text("Detail") }

/** 如果你已有 ui 包里的 DemoScreen，可把这里替换成真实引用 */
@Composable
private fun DemoScreenPlaceholder() { Text("Demo") }

private fun titleOf(route: String): String = when (route) {
  "/home" -> "Home"
  "/detail" -> "Detail"
  "/demo" -> "Demo"
  else -> route
}
