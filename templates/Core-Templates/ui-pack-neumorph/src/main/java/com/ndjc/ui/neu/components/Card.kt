package com.ndjc.ui.neu.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ndjc.ui.neu.theme.Tokens

@Composable
fun NDJCCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    val t = Tokens.current()
    val shape = RoundedCornerShape(t.radius.lg)

    Card(
        modifier = modifier.drawBehind {
            val radiusPx = t.radius.lg.toPx()
            val blurPx = (t.blur * 1.3f).toPx()   // 放大一点模糊半径

            val width = size.width
            val height = size.height

            // 高光和阴影颜色调重一点，效果更明显
            val highlightColor = Color.White.copy(alpha = (t.lightAlpha * 2.2f).coerceAtMost(1f))
            val shadowColor = t.color.surfaceVariant.copy(alpha = (t.shadowAlpha * 1.5f).coerceAtMost(1f))

            // 底右暗影
            drawRoundRect(
                color = shadowColor,
                topLeft = Offset(blurPx, blurPx),
                size = Size(width, height),
                cornerRadius = CornerRadius(radiusPx, radiusPx)
            )

            // 顶左高光
            drawRoundRect(
                color = highlightColor,
                topLeft = Offset(-blurPx, -blurPx),
                size = Size(width, height),
                cornerRadius = CornerRadius(radiusPx, radiusPx)
            )
        },
        colors = CardDefaults.cardColors(
            // 内部底色稍微比整体背景深一点，方便看出浮起
            containerColor = t.color.surface,
            contentColor = t.color.onSurface
        ),
        // 拟物用自绘阴影，这里关掉 Material 的阴影
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = shape
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = t.space.lg,
                vertical = t.space.md
            )
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = t.color.onSurfaceVariant,
                modifier = Modifier.padding(top = t.space.sm)
            )
        }
    }
}
