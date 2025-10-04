@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ndjc.app.feature.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ndjc.app.data.SeedRepository

@Composable
fun PostDetailScreen(
  id: String,
  onBack: () -> Unit
) {
  val post = remember(id) { SeedRepository.postById(id) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(post?.author ?: "Detail") },
        navigationIcon = {
          TextButton(onClick = onBack) { Text("Back") }
        }
      )
    }
  ) { inner ->
    Column(Modifier.padding(inner).padding(16.dp)) {

      Text(
        text = post?.content ?: "N/A",
        style = MaterialTheme.typography.bodyLarge
      )

      Divider()

      Spacer(Modifier.height(12.dp))
      Text("Comments: ${post?.comments?.size ?: 0}")

      // HOOK:DETAIL_FAB
      // 可注入：浮动按钮/分享等入口
    }
  }
}
