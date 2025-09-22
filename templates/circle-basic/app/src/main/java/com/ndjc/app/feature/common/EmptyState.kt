package com.ndjc.app.feature.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ndjc.app.R

@Composable
fun EmptyState() {
    // BLOCK:EMPTY_STATE
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = stringResource(id = R.string.empty_hint))
    }
    // END_BLOCK
}
