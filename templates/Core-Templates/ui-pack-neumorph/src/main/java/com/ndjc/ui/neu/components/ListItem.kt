package com.ndjc.ui.neu.components

import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ndjc.ui.neu.theme.Tokens

@Composable
fun NDJCListItem(
    headline: String,
    supporting: String? = null,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val t = Tokens.current()
    ListItem(
        headlineContent = { Text(headline) },
        supportingContent = supporting?.let { { Text(it) } },
        leadingContent = leading,
        trailingContent = trailing,
        modifier = modifier,
        colors = ListItemDefaults.colors(
            containerColor   = t.color.surface,
            headlineColor    = t.color.onSurface,
            leadingIconColor = t.color.onSurfaceVariant,
            overlineColor    = t.color.onSurfaceVariant,
            supportingColor  = t.color.onSurfaceVariant,
            trailingIconColor= t.color.onSurfaceVariant
        )
    )
}
