package com.ndjc.app.feature.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ndjc.app.R
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
          headlineContent = { Text(text = p.content) },         // ✅ 显示 text 参数
          supportingContent = {
            Text(text = stringResource(R.string.comments_count, p.comments.size))
          },
          modifier = Modifier.clickable { onPostClick(p.id) }
        )

        // BLOCK:FEED_ITEM_BADGES
        // 可注入：在列表行底部渲染角标/打点信息
        // END_BLOCK

        Divider()
      }

      // BLOCK:EMPTY_STATE
      // 可注入：当 posts 为空时渲染的空状态组件
      // END_BLOCK
    }
  }
}
