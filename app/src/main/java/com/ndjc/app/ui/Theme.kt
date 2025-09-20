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

    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(

        )
    } else {
        lightColorScheme(

        )
    }

    // 动态色/系统栏等统一适配

    // 主题扩展：形状、排版、间距、EdgeToEdge、系统栏等
    // （生成器可在本段内安全注入到调用点下方）

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(),
        content = content
    )
}

// 组件预览/生成示例

