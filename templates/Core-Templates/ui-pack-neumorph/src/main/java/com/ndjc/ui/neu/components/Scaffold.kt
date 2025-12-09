package com.ndjc.ui.neu.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * NDJC Neumorph Bus 风格的页面骨架：
 * - 整体淡紫背景
 * - 顶部大块紫色渐变 + 半圆弧形
 * - 内部内容区域统一 padding
 *
 * DemoScreen / 未来的 Bus 风格页面，都建议用这个骨架包一层。
 */
@Composable
fun NDJCBusScaffold(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F2FF)) // 整体淡紫背景
    ) {
        // 顶部紫色渐变块（Bus 风格的半圆形顶部）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0xFFB97CFF),
                            Color(0xFF8A72FF)
                        )
                    ),
                    shape = RoundedCornerShape(
                        topStart = 32.dp,
                        topEnd = 32.dp,
                        bottomStart = 120.dp,
                        bottomEnd = 180.dp
                    )
                )
        )

        // 页面内容区域：统一内边距 + 垂直布局
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            content = content
        )
    }
}
