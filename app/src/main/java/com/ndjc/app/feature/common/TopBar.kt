package com.ndjc.app.feature.common

import androidx.annotation.StringRes           // ✅ 补上
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
