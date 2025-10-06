package com.ndjc.app.feature.common  // 固定源码包名，保证 R 归属稳定

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ndjc.app.R

@Composable
fun EmptyState() {
    // BLOCK:EMPTY_STATE
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = stringResource(id = R.string.empty_hint))
    }
    // END_BLOCK
}
