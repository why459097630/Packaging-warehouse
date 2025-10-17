@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ndjc.app.feature.common  // 固定源码包名

import androidx.annotation.StringRes
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun TopBar(
    @StringRes titleRes: Int,
    actions: @Composable () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = { Text(text = stringResource(id = titleRes)) },
        actions = { actions() }
    )
}
