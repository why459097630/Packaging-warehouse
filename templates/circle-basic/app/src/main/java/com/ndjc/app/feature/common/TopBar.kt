@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class) // ✅ 为 M3 顶栏开白名单

package com.ndjc.app.feature.common

import android.annotation.StringRes
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource  // ✅ 补上

@Composable
fun TopBar(@StringRes titleRes: Int, actions: @Composable () -> Unit = {}) {
    CenterAlignedTopAppBar(
        title = { Text(text = stringResource(id = titleRes)) },
        actions = { actions() }
    )
}
