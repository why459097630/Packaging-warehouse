package com.ndjc.app.ui.components  // NDJC:PACKAGE_NAME

import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*

@Composable
fun FeedItem(
  title: String,
  onClick: () -> Unit = {}
) {
  Card(onClick = onClick) {
    Column(Modifier.padding(16.dp)) {
      Text(text = title, style = MaterialTheme.typography.titleMedium)

      // BLOCK:FEED_ITEM_BADGES
      // 可注入：置顶/官方/新帖 等徽标
      // END_BLOCK

      // BLOCK:SPONSORED_CARD
      // 可注入：赞助内容卡片
      // END_BLOCK
    }
  }
}
