package com.ndjc.app.feature.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource   // ✅ 补上
import com.ndjc.app.R                         // ✅ 补上

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
