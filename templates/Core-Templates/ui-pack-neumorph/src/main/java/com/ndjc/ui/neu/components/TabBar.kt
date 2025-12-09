package com.ndjc.ui.neu.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ndjc.ui.neu.R
import com.ndjc.ui.neu.theme.Tokens

@Composable
fun NDJCTabBar(
    items: List<String>,      // 只接收 routeId 列表
    selectedId: String,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = Tokens.current()

    // 外层：底部淡紫色光晕
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        t.color.primary.copy(alpha = 0.10f) // 底部淡淡的紫色
                    )
                )
            )
            .padding(bottom = 12.dp, top = 8.dp)
    ) {
        // 白色圆角导航条（悬浮在中间）
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 32.dp)
                .height(60.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = Color.White,
            shadowElevation = t.elevation.level2
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, id ->
                    val selected = id == selectedId

                    // 根据顺序挑图标：0=home，1=explore，2=clock，3+=person
                    val iconRes = when (index) {
                        0 -> R.drawable.home_24
                        1 -> R.drawable.explore_24
                        2 -> R.drawable.clock_24
                        else -> R.drawable.user_24
                    }

                    // 每个 tab：圆形按钮，选中时紫色实心圆 + 白色图标
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) t.color.primary else Color.Transparent
                            )
                            .clickable { onClick(id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = id,
                            modifier = Modifier.size(22.dp),
                            alpha = if (selected) 1f else 0.35f
                        )
                    }
                }
            }
        }
    }
}
