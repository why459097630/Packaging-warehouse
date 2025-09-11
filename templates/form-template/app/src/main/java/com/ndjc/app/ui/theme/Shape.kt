package com.ndjc.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(NdjcCornerRadius.extraSmall),
    small      = RoundedCornerShape(NdjcCornerRadius.small),
    medium     = RoundedCornerShape(NdjcCornerRadius.medium),
    large      = RoundedCornerShape(NdjcCornerRadius.large),
    extraLarge = RoundedCornerShape(NdjcCornerRadius.extraLarge),
)
