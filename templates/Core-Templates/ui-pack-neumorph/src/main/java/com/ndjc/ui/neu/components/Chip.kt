package com.ndjc.ui.neu.components

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.ndjc.ui.neu.theme.Tokens

@Composable
fun NDJCChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val t = Tokens.current()
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        modifier = modifier,
        shape = RoundedCornerShape(t.radius.pill), // 拟物：胶囊更柔
        colors = FilterChipDefaults.filterChipColors(
            containerColor           = t.color.surface,
            selectedContainerColor   = t.color.primaryContainer,
            labelColor               = t.color.onSurface,
            selectedLabelColor       = t.color.onPrimaryContainer,
            iconColor                = t.color.onSurfaceVariant,
            selectedLeadingIconColor = t.color.onPrimaryContainer
        )
    )
}
