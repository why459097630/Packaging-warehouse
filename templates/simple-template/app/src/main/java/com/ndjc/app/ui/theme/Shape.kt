package com.ndjc.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
  extraSmall = RoundedCornerShape({{NDJC_CORNER_RADIUS_DP}}.dp),
  small      = RoundedCornerShape({{NDJC_CORNER_RADIUS_DP}}.dp),
  medium     = RoundedCornerShape({{NDJC_CORNER_RADIUS_DP}}.dp),
  large      = RoundedCornerShape({{NDJC_CORNER_RADIUS_DP}}.dp),
  extraLarge = RoundedCornerShape({{NDJC_CORNER_RADIUS_DP}}.dp)
)
