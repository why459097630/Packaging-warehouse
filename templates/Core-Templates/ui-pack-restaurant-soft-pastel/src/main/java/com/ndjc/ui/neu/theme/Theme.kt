package com.ndjc.ui.neu.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun NDJCTheme(
    mode: ThemeMode = ThemeMode.Light,
    content: @Composable () -> Unit
) {
    val colorScheme = colorSchemeOf(mode)
    val shapes = neuShapes()
    val typography = neuTypography()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF000000), // 顶：纯黑
                        Color(0xFFFF0000), // 中：大红
                        Color(0xFF0000FF)  // 底：纯蓝
                    )
                )
            )
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = shapes,
            typography = typography,
        ) {
            content()
        }
    }
}
