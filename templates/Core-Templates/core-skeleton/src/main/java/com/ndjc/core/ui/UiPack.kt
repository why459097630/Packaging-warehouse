package com.ndjc.core.ui

import androidx.compose.runtime.Composable

/**
 * UI 抽象：所有 UI 包需要实现的最小接口
 */
interface UiPack {
  /** 根主题包装（把所有 M3/自定义样式注入到 content） */
  @Composable
  fun Theme(content: @Composable () -> Unit)

  /** 顶部栏（标题） */
  @Composable
  fun TopAppBar(title: String)

  /** TabBar（底部/顶部选项卡） */
  @Composable
  fun TabBar(
    routes: List<String>,
    selectedRoute: String,
    onSelected: (String) -> Unit
  )
}

/* ---------------- Provider：运行时注入入口（最简单的方式） ---------------- */

object UiPackProvider {

  /**
   * 当前生效的 UI 包实例。
   * 先给一个「安全的空实现」，避免调用处 NPE。
   */
  @JvmStatic
  var current: UiPack = object : UiPack {
    @Composable
    override fun Theme(content: @Composable () -> Unit) = content()

    @Composable
    override fun TopAppBar(title: String) { /* no-op */ }

    @Composable
    override fun TabBar(
      routes: List<String>,
      selectedRoute: String,
      onSelected: (String) -> Unit
    ) { /* no-op */ }
  }
}

/* ---------------- 便捷转发函数（调用处更简洁） ---------------- */

@Composable
fun UiPackTheme(content: @Composable () -> Unit) =
  UiPackProvider.current.Theme(content)

@Composable
fun UiPackTopAppBar(title: String) =
  UiPackProvider.current.TopAppBar(title)

@Composable
fun UiPackTabBar(
  routes: List<String>,
  selectedRoute: String,
  onSelected: (String) -> Unit
) = UiPackProvider.current.TabBar(routes, selectedRoute, onSelected)
