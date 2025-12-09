package com.ndjc.ui.neu.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/** 圆角整体更“软”一些，卡片/按钮/输入框观感统一 */
fun neuShapes(): Shapes = Shapes(
    extraSmall = RoundedCornerShape(NeuTokens.Radius.xs),
    small      = RoundedCornerShape(NeuTokens.Radius.sm),
    medium     = RoundedCornerShape(NeuTokens.Radius.md),
    large      = RoundedCornerShape(NeuTokens.Radius.lg),
    extraLarge = RoundedCornerShape(NeuTokens.Radius.xl),
)
