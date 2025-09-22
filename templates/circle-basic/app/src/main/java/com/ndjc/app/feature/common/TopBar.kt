package com.ndjc.app.feature.common

import androidx.annotation.StringRes
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun TopBar(@StringRes titleRes: Int, actions: @Composable () -> Unit = {}) {
    CenterAlignedTopAppBar(title = { Text(stringResource(id = titleRes)) }, actions = { actions() })
}
