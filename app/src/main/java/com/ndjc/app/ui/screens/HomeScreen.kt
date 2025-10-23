@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ndjc.app.ui.screens   // 屏幕/页面集合

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
 fun HomeHeader() { Row { Text(text = "Welcome") } }
// END_BLOCK
        },
        floatingActionButton = {
            // BLOCK:HOME_ACTIONS
 fun HomeActions() { Row { Button(onClick = {}) { Text("New Post") } Button(onClick = {}) { Text("Search") } } }
// END_BLOCK
        }
    ) { inner ->

        // BLOCK:HEADER_AD_SLOT
        // 这里是首页内容区最顶部的广告位插槽（在列表/正文之前）
        // END_BLOCK

        // BLOCK:HOME_BODY
 fun HomeBody() { Column { Text(text = "Latest posts") Button(onClick = {}) { Text("Refresh") } } }
// END_BLOCK

        // BLOCK:EMPTY_STATE
        // 当空数据集/网络空内容的占位 UI
        // END_BLOCK

        // BLOCK:ERROR_STATE
        // 错误态/刷新失败时的应对 UI
        // END_BLOCK

        // LIST:POST_FIELDS
        // $ITEM
        // END_LIST

        // LIST:COMMENT_FIELDS
        // $ITEM
        // END_LIST
    }
}
