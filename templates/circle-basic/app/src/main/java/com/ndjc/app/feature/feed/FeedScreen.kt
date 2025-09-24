package com.ndjc.app.feature.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ndjc.app.R
import com.ndjc.app.feature.common.TopBar
import com.ndjc.app.data.SeedRepository
import com.ndjc.app.feature.feed.ui.FeedItem

@Composable
fun FeedScreen(
    onOpenDetail: (String) -> Unit,
    onCreatePost: () -> Unit
) {
    val posts by remember { mutableStateOf(SeedRepository.posts()) }

    Scaffold(
        topBar = {
            TopBar(
                titleRes = R.string.home_title,
                actions = {
                    // BLOCK:HOME_ACTIONS
                    // 可注入搜索/设置按钮
                    // END_BLOCK
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreatePost,
                value = { Text(stringResource(id = R.string.feed_post)) }
            )
        }
    ) { inner ->
        Column(Modifier.padding(inner)) {
            // BLOCK:HOME_HEADER
            // 可注入公告/横幅
            // END_BLOCK

            // BLOCK:HOME_BODY
            if (posts.isEmpty()) {
                com.ndjc.app.feature.common.EmptyState()
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
                    items(posts) { p ->
                        // BLOCK:HEADER_AD_SLOT
                        // 可注入信息流广告
                        // END_BLOCK

                        FeedItem(post = p, onClick = { onOpenDetail(p.id) })
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            // END_BLOCK
        }
    }
}
