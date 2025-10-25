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
 fun HomeHeader(){ Column{ Text("Welcome to Social Circle", style=MaterialTheme.typography.h5) Spacer(modifier=Modifier.height(8.dp)) } }
// END_BLOCK
        },
        floatingActionButton = {
            // BLOCK:HOME_ACTIONS
 fun HomeActions(onPostClick:()->Unit){ Row{ IconButton(onClick={ onPostClick() }){ Icon(Icons.Default.Add,"Add Post") } } }
// END_BLOCK
        }
    ) { inner ->

        // BLOCK:HEADER_AD_SLOT
        // 这里是首页内容区最顶部的广告位插槽（在列表/正文之前）
        // END_BLOCK

        // BLOCK:HOME_BODY
 fun HomeBody(posts: List<Post>){ LazyColumn{ items(posts){ post-> PostCard(post) } } }
// END_BLOCK

        // BLOCK:EMPTY_STATE
 fun EmptyState(){ Column(horizontalAlignment=Alignment.CenterHorizontally){ Icon(Icons.Default.Inbox,"Empty") Text("No content yet") } }
// END_BLOCK

        // BLOCK:ERROR_STATE
 fun ErrorState(message:String,onRetry:()->Unit){ Column(horizontalAlignment=Alignment.CenterHorizontally){ Icon(Icons.Default.Error,"Error") Text(message) Button(onClick={ onRetry() }){ Text("Retry") } } }
// END_BLOCK

        // LIST:POST_FIELDS
        // $ITEM
        // END_LIST

        // LIST:COMMENT_FIELDS
        // $ITEM
        // END_LIST
    }
}
