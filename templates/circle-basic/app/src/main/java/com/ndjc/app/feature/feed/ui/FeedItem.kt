package com.ndjc.app.feature.feed.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ndjc.app.R

/**
 * 通用的 Feed 列表项。
 * 为了不与具体数据模型耦合（避免额外 import/类型依赖导致编译失败），这里用基础字段而不是 Post 类型。
 * 这样即使当前项目暂未引用该组件，文件也能独立通过编译；后续若需要，可在调用处自适配包装。
 */
@Composable
fun FeedItem(
    title: String,
    subtitle: String? = null,
    commentsCount: Int = 0,
    sponsored: Boolean = false,
    onClick: () -> Unit = {}
) {
    ListItem(
        headlineContent = { Text(text = title) },
        supportingContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 评论数量
                AssistChip(
                    onClick = { onClick() },
                    label = {
                        Text(
                            text = stringResource(
                                id = R.string.comments_count,
                                commentsCount
                            )
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors()
                )

                // BLOCK:FEED_ITEM_BADGES
                // 这里可注入：标签、置顶、作者等级等徽标
                // END_BLOCK

                if (sponsored) {
                    // BLOCK:SPONSORED_CARD
                    AssistChip(
                        onClick = { /* no-op */ },
                        label = { Text(text = "Ad") },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                    // END_BLOCK
                }
            }

            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )

    Divider(modifier = Modifier.padding(start = 8.dp, end = 8.dp))
}
