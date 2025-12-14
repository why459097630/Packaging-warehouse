package com.ndjc.ui.neu.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Contract-UI: opacity.* 设计令牌
 * - 暴露 disabled/medium/high 三档不透明度
 * - 提供 operator get 以支持 ["disabled"] 写法
 */
data class Opacity(
    val disabled: Float = 0.38f,
    val medium: Float = 0.60f,
    val high: Float = 1.00f
) {
    operator fun get(name: String): Float = when (name) {
        "disabled" -> disabled
        "medium" -> medium
        "high" -> high
        else -> high
    }
}

/** CompositionLocal：LocalOpacity（默认安全值） */
val LocalOpacity = staticCompositionLocalOf { Opacity() }
