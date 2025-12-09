package com.ndjc.ui.neu.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * 轻量 Neumorph 工具：
 *  - [neumorphRaised]  ：外凸的高光+投影
 *  - [neumorphSunken]  ：内凹的高光+投影
 *
 * 说明：
 *  1) 统一从 Tokens 衍生高光/阴影色（不再写死值）
 *  2) 仅用于按钮/卡片/滑块等小面积元素；大面积建议用 elevation
 */

private fun highLight(t: TokensSpec): Color =
    t.color.onSurface.copy(alpha = 0.06f)

private fun lowShadow(t: TokensSpec): Color =
    t.color.outline.copy(alpha = 0.25f)

/** 外凸（Raised）：元素边缘上方高光、下方投影 */
fun Modifier.neumorphRaised(tokens: TokensSpec): Modifier = this.then(
    Modifier.drawBehind {
        val t = tokens
        val w = size.width
        val h = size.height
        val r = min(w, h) * 0.08f

        // 上方高光
        drawIntoCanvas {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(highLight(t), Color.Transparent),
                    start = Offset(0f, 0f),
                    end = Offset(0f, r),
                    tileMode = TileMode.Clamp
                ),
                size = Size(w, r)
            )
        }
        // 下方投影
        drawIntoCanvas {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, lowShadow(t)),
                    start = Offset(0f, h - r),
                    end   = Offset(0f, h),
                    tileMode = TileMode.Clamp
                ),
                topLeft = Offset(0f, h - r),
                size = Size(w, r)
            )
        }
    }
)

/** 内凹（Sunken）：容器内部上缘高光、下缘投影，模拟“凹槽” */
fun Modifier.neumorphSunken(tokens: TokensSpec): Modifier = this.then(
    Modifier.drawBehind {
        val t = tokens
        val w = size.width
        val h = size.height
        val r = min(w, h) * 0.08f

        // 内部上缘高光
        drawIntoCanvas {
            withTransform({
                translate(0f, 0f)
            }) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(highLight(t), Color.Transparent),
                        start = Offset(0f, 0f),
                        end = Offset(0f, r),
                        tileMode = TileMode.Clamp
                    ),
                    size = Size(w, r)
                )
            }
        }
        // 内部下缘阴影
        drawIntoCanvas {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, lowShadow(t)),
                    start = Offset(0f, h - r),
                    end   = Offset(0f, h),
                    tileMode = TileMode.Clamp
                ),
                topLeft = Offset(0f, h - r),
                size = Size(w, r)
            )
        }
    }
)
