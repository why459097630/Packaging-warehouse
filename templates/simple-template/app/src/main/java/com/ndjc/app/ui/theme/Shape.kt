package com.ndjc.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// 如果你用了上面的 NdjcCornerRadius（类型是 Dp），直接传即可
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(NdjcCornerRadius),
    small      = RoundedCornerShape(NdjcCornerRadius),
    medium     = RoundedCornerShape(NdjcCornerRadius),
    large      = RoundedCornerShape(NdjcCornerRadius),
    extraLarge = RoundedCornerShape(NdjcCornerRadius),
)
