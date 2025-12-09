package com.ndjc.ui.neu.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.ndjc.ui.neu.theme.Tokens

@Composable
fun NDJCBanner(
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val t = Tokens.current()
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = t.color.primaryContainer,
        contentColor = t.color.onPrimaryContainer,
        tonalElevation = t.elevation.level1,
        shape = RoundedCornerShape(t.radius.lg) // 柔和圆角的条幅
    ) {
        Row(
            modifier = Modifier.padding(horizontal = t.space.lg, vertical = t.space.sm),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = text, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.width(t.space.md))
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}
