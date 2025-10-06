package com.ndjc.app.feature.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp  // ✅ 补上 dp

import com.ndjc.app.R            // ✅ 补上
import com.ndjc.app.data.SeedRepository
import com.ndjc.app.feature.common.TopBar

@Composable
fun FeedScreen(
    onPostClick: (String) -> Unit,
    onCreatePost: () -> Unit = {}
) {
    val posts = remember { SeedRepository.posts() }

    Scaffold(
        topBar = { TopBar(titleRes = R.string.feed_title) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreatePost) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            }
        }
    ) { inner ->
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.padding(inner)
        ) {
            items(posts) { p ->
                ListItem(
                    headlineContent = { Text(text = p.content) },        // ✅ 显式 text 参数
                    supportingContent = {
                        Text(text = stringResource(R.string.comments_count, p.comments.size))
                    },
                    modifier = Modifier.clickable { onPostClick(p.id) }
                )

                // BLOCK:FEED_ITEM_BADGES
                // 可注入：为每条云标/标签/评分徽章
                // END_BLOCK

                Divider()

                // BLOCK:EMPTY_STATE
                // 可注入：无数据时显示的占位 UI
                // END_BLOCK
            }
        }
    }
}
