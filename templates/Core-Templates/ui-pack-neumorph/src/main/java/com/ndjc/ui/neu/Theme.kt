package com.ndjc.ui.m3

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api

/**
 * NDJCThemePack（M3 标准 / Neumorph 桥接）
 *
 * - 仅承担外观主题，不依赖具体导航/布局类型
 * - 后续可通过 TokenBridge 注入配色
 */
@Composable
fun NDJCThemePack(content: @Composable () -> Unit) {
    val light = lightColorScheme()
    // 简化：默认用亮色；可按系统切换或由 Token 控制
    MaterialTheme(
        colorScheme = light,
        typography = MaterialTheme.typography,
        content = content
    )
}

/**
 * NDJCAppBar
 *
 * 统一对外暴露的顶栏组件。
 * - 模块侧统一 import com.ndjc.ui.m3.NDJCAppBar
 * - 具体视觉由当前启用的 UI 包实现（这里用 M3/Neumorph 皮肤）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NDJCAppBar(
    title: String,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
        },
        navigationIcon = {
            navigationIcon?.invoke()
        },
        actions = {
            actions()
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
