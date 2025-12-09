package com.ndjc.ui.neu.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ndjc.ui.neu.theme.Tokens
import com.ndjc.ui.neu.theme.neumorphSunken
import kotlin.math.roundToInt

/**
 * NDJC Slider（Neumorph 版）
 *
 * - 背景凹槽：使用 neumorphSunken(tokens) 实现“内凹”效果
 * - 进度条：自绘一条柔和的填充（低不透明度主色）
 * - Material3 Slider：轨道设为透明，仅负责拇指与手势；拇指是圆形高光色
 *
 * 说明：
 * - 这个实现既保持了 M3 Slider 的可访问性与手势，也能得到拟物凹槽视觉
 * - valueRange 默认 0f..1f；你也可以传 0f..100f 等，进度条会按比例计算
 */
@Composable
fun NDJCSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
) {
    val t = Tokens.current()
    // 轨道高度和圆角 —— 拟物的凹槽通常更厚一点
    val trackHeight = 14.dp
    val shape = RoundedCornerShape(t.radius.pill)

    // 将 value 映射为 0..1，用于绘制进度条长度
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
        .coerceIn(0f, 1f)

    Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
        // 凹槽容器（先画内凹，再叠加进度填充）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(shape)
                .background(t.color.surface)           // 凹槽基底
                .neumorphSunken(Tokens.current())      // 内凹效果
        ) {
            // 进度填充（柔和主色，圆角胶囊）
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(
                        color = t.color.primary.copy(alpha = 0.22f),
                        shape = shape
                    )
            )
        }

        // 将真正可交互的 Slider 叠在凹槽上方（透明轨道，仅显示拇指）
        Slider(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp), // 提供额外触控空间
            colors = SliderDefaults.colors(
                thumbColor = t.color.primary,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            )
        )
    }
}

/* —— 可选：带百分比标签的包装（示例） —— */
@Composable
fun NDJCSliderWithLabel(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    toPercent: (Float) -> Int = { (it * 100).roundToInt() }
) {
    Column(modifier = modifier) {
        NDJCSlider(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps
        )
        Spacer(Modifier.height(Tokens.current().space.sm))
        // 这里你可替换成自己的排版/对齐
        androidx.compose.material3.Text(
            text = "${toPercent(((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f))}%",
            color = Tokens.current().color.onSurfaceVariant,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium
        )
    }
}
