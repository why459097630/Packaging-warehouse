package com.ndjc.ui.neu.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.dp
import com.ndjc.ui.neu.theme.Tokens

@Composable
fun NDJCToast(
    message: String,
    modifier: Modifier = Modifier
) {
    val t = Tokens.current()
    Popup(
        alignment = Alignment.BottomCenter,
        properties = PopupProperties(excludeFromSystemGesture = true)
    ) {
        Surface(
            modifier = modifier
                .wrapContentSize()
                .padding(bottom = t.space.xl),
            color = t.color.inverseSurface,
            contentColor = t.color.inverseOnSurface,
            shape = RoundedCornerShape(t.radius.xl), // 更大圆角
            tonalElevation = t.elevation.level2,
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(
                    horizontal = t.space.lg,
                    vertical = t.space.sm
                ),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
        }
    }
}
