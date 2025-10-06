@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ndjc.app.ui.screens  // com.mississippi.restaurant

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier

@Composable
fun HomeScreen(
  onOpenDetail: (String) -> Unit,
  onCreatePost: () -> Unit
) {
  Scaffold(
    topBar = {
      // BLOCK:HOME_HEADER
      TopAppBar(title = { Text(text = "NDJC Home") })
      // END_BLOCK
    },
    floatingActionButton = {
      // BLOCK:HOME_ACTIONS
      ExtendedFloatingActionButton(onClick = { onCreatePost() }) {
        Text(text = "Create")
      }
      // END_BLOCK
    }
  ) { inner ->
    // BLOCK:HOME_BODY
    Column(Modifier.padding(inner).fillMaxSize()) {
      Text(text = "Feed placeholder")
    }
    // END_BLOCK

    // BLOCK:EMPTY_STATE
    // 当空数据时展示的占位 UI
    // END_BLOCK

    // BLOCK:ERROR_STATE
    // 当错误时展示的占位 UI
    // END_BLOCK

    // LIST:POST_FIELDS
    // ${ITEM}
    // END_LIST

    // LIST:COMMENT_FIELDS
    // ${ITEM}
    // END_LIST
  }
}
