package com.ndjc.ui.neu.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ndjc.ui.neu.theme.Tokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NDJCModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable () -> Unit
) {
    val t = Tokens.current()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = t.color.surface,
        contentColor = t.color.onSurface,
        dragHandle = null // 拟物风格可弱化抓手
    ) {
        Surface(tonalElevation = t.elevation.level1) {
            Column(
                modifier = Modifier.padding(
                    horizontal = t.space.lg,
                    vertical = t.space.md
                )
            ) {
                content()
            }
        }
    }
}
