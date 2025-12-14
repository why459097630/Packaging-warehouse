@file:OptIn(ExperimentalMaterial3Api::class)

package com.ndjc.ui.neu

import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ndjc.core.ui.UiPack
import com.ndjc.core.ui.UiPackProvider
import com.ndjc.ui.neu.theme.NDJCTheme
import com.ndjc.ui.neu.theme.NeuTokens
import com.ndjc.ui.neu.theme.ThemeMode

/** 你的 Neumorph UI 包实现 */
class UiPackNeu : UiPack {

    /** 套用 neu 主题（按需你也可以把 mode 外抛） */
    @Composable
    override fun Theme(content: @Composable () -> Unit) {
        NDJCTheme(mode = ThemeMode.Light) {
            // 整个 App 的紫→蓝渐变背景
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFBBA4FF), // 顶部：偏紫
                                Color(0xFF9FB4FF), // 中段：紫蓝过渡
                                Color(0xFF8EC5FF)  // 底部：偏蓝
                            )
                        )
                    )
            ) {
                // 透明 Surface 承载内容，不再盖住渐变
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    content()
                }
            }
        }
    }

    /** 顶栏：更平/更软，浅描边/轻阴影可在组件层继续做 */
    @Composable
    override fun TopAppBar(title: String) {
        CenterAlignedTopAppBar(
            title = { Text(title, style = MaterialTheme.typography.titleMedium) },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }

    /** TabBar：药丸指示器 + 更圆的触感 */
    @Composable
    override fun TabBar(
        routes: List<String>,
        selectedRoute: String,
        onSelected: (String) -> Unit
    ) {
        val selectedIndex = routes.indexOf(selectedRoute).coerceAtLeast(0)
        TabRow(
            selectedTabIndex = selectedIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = { tabPositions ->
                if (selectedIndex in tabPositions.indices) {
                    val pos = tabPositions[selectedIndex]
                    Box(
                        Modifier
                            .tabIndicatorOffset(pos)
                            .height(3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            },
            divider = {} // 去掉底部分割线，观感更“软”
        ) {
            routes.forEachIndexed { index, title ->
                Tab(
                    selected = index == selectedIndex,
                    onClick = { onSelected(routes[index]) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                )
            }
        }
    }
}

/** 默认把当前 UiPack 切到 Neumorph；如果你已有装配逻辑，可移到 AppRoot */
@Suppress("unused")
val UseNeumorphUiPackOnce: Unit = run {
    UiPackProvider.current = UiPackNeu()
}
