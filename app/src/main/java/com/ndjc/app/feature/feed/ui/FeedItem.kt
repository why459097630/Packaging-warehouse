package com.ndjc.app.feature.feed.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ndjc.app.data.model.Post

@Composable
fun FeedItem(post: Post, onClick: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(Modifier.padding(12.dp)) {
            Text(text = post.author, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(text = post.content, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))

            // BLOCK:FEED_ITEM_BADGES
            // 可注入标签/话题徽章
            // END_BLOCK

            Row {
                AssistChip(onClick = { /* like */ }, label = { Text("👍 ${post.likes}") })
                Spacer(Modifier.width(8.dp))
                AssistChip(onClick = { /* comment */ }, label = { Text("💬 ${post.comments.size}") })
            }

            // BLOCK:SPONSORED_CARD
            // 可注入赞助内容卡片
            // END_BLOCK
        }
    }
}
