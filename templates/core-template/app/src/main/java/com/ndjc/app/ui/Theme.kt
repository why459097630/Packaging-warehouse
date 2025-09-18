package com.ndjc.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    // 主题预设（可注入不同风格/品牌主色）
    // NDJC:BLOCK:APP_THEME_PRESETS

    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(
            // NDJC:BLOCK:COLORS
        )
    } else {
        lightColorScheme(
            // NDJC:BLOCK:COLORS
        )
    }

    // 动态色/壁纸色等策略关与适配
    // NDJC:BLOCK:DYNAMIC_COLOR

    // 主题调整：形状、排版、间距、EdgeToEdge、系统栏等
    // （生成器可把复杂内容注入到该点之下）
    // NDJC:BLOCK:THEME_OVERRIDES

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(),
        content = content
    )
}

// 组件预览/主题示例
// NDJC:BLOCK:COMPOSE_PREVIEWS
