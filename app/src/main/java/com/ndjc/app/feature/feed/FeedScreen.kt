package com.ndjc.app.feature.feed   // 固定源码包名

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ndjc.app.R
import com.ndjc.app.data.SeedRepository
import com.ndjc.app.feature.common.EmptyState
import com.ndjc.app.feature.feed.ui.FeedItem

@Composable
fun FeedScreen(
    onPostClick: (String) -> Unit,
    onCreatePost: () -> Unit = {}
) {
    // 固定数据来源；remember 避免每次重组都重新解析
    val posts = remember { SeedRepository.posts() }

    Scaffold(
        topBar = { SimpleTopBar(titleRes = R.string.feed_title) },
        floatingActionButton = {
            // 避免依赖 icons 扩展库，直接显示字符
            FloatingActionButton(onClick = onCreatePost) {
                Text(text = "+", style = MaterialTheme.typography.titleMedium)
            }
        }
    ) { innerPadding ->
        if (posts.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyState()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(posts) { p ->
                    // 这里不直接耦合具体数据模型到 UI 组件（FeedItem 是通用的）
                    FeedItem(
                        title = p.content,
                        subtitle = p.author,
                        commentsCount = p.comments.size,
                        sponsored = false,
                        onClick = { onPostClick(p.id) }
                    )
                }
            }
        }
    }
}

/**
 * 稳定版自绘顶栏（避免使用 TopAppBar 实验 API）
 */
@Composable
private fun SimpleTopBar(titleRes: Int) {
    Surface(tonalElevation = 3.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = stringResource(id = titleRes),
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
