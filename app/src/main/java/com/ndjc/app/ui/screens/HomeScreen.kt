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
<!-- xml or kotlin snippet -->
// END_BLOCK
        },
        floatingActionButton = {
            // BLOCK:HOME_ACTIONS
<!-- xml or kotlin snippet -->
// END_BLOCK
        }
    ) { inner ->
        // BLOCK:HOME_BODY
<!-- xml or kotlin snippet -->
// END_BLOCK

        // BLOCK:EMPTY_STATE
<!-- xml or kotlin snippet -->
// END_BLOCK

        // BLOCK:ERROR_STATE
<!-- xml or kotlin snippet -->
// END_BLOCK

        // LIST:POST_FIELDS
        // ${ITEM}
        // END_LIST

        // LIST:COMMENT_FIELDS
        // ${ITEM}
        // END_LIST
    }
}
