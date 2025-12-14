package com.ndjc.ui.m3.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Contract-UI: space.* 设计令牌
 * - 使用 Int，方便外部以 .dp 转换（契合你现有代码 tokens.space.sm.dp 的写法）
 */
data class Spacing(
    val xs: Int = 4,
    val sm: Int = 8,
    val md: Int = 12,
    val lg: Int = 16,
    val xl: Int = 20
)

/** CompositionLocal：LocalSpacing（默认安全值） */
val LocalSpacing = staticCompositionLocalOf { Spacing() }
