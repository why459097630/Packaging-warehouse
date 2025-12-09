@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ndjc.ui.neu.components

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ndjc.ui.neu.theme.Tokens

@Composable
fun NDJCTopAppBar(
    title: String,
    modifier: Modifier = Modifier
) {
    val t = Tokens.current()
    val colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
        containerColor = t.color.surface,
        titleContentColor = t.color.onSurface,
        navigationIconContentColor = t.color.onSurface,
        actionIconContentColor = t.color.onSurface
    )
    CenterAlignedTopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        modifier = modifier,
        colors = colors,
        scrollBehavior = null // 需要时可接入 pinned/enterAlways 等
    )
}
