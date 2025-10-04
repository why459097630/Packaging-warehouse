@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ndjc.app.feature.post

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PostEditorScreen(onBack: () -> Unit) {
  var text by remember { mutableStateOf("") }

  // HOOK:POST_EDITOR_SIDE_EFFECTS
  // 可注入：编辑器生命周期相关副作用（如埋点）

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(title = { Text("New Post") })
    }
  ) { inner ->
    Column(Modifier.padding(inner).padding(16.dp)) {

      OutlinedTextField(
        value = text,                // ✅ 使用 value（不是 text）
        onValueChange = { text = it },
        modifier = Modifier
          .fillMaxWidth(),
        minLines = 4,
        placeholder = { Text("Say something…") }
      )

      Spacer(Modifier.height(12.dp))

      // BLOCK:POST_EDITOR_TOAST
      // 可注入：字段校验结果/保存后的提醒等 UI 片段
      // END_BLOCK

      Button(onClick = { /* todo: persist */ onBack() }) { Text("Publish") }
    }
  }
}
