@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ndjc.app.ui.screens  // 固定源码包名

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
 fun HomeHeader() { Text("菜单") }
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
 fun HomeBody() { LazyColumn { items = listOf( "菜品1", "菜品2", "菜品3") { item -> Text(item) } } }
// END_BLOCK

        // BLOCK:EMPTY_STATE
        // 当空态时渲染内容的占位 UI
        // END_BLOCK

        // BLOCK:ERROR_STATE
        // 当错误态时渲染内容的占位 UI
        // END_BLOCK

        // LIST:POST_FIELDS
        // ${ITEM}
        // END_LIST

        // LIST:COMMENT_FIELDS
        // ${ITEM}
        // END_LIST
    }
}
