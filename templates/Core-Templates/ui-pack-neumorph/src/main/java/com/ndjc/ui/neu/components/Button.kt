package com.ndjc.ui.neu.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.ndjc.ui.neu.theme.Tokens
import androidx.compose.material3.Text

// 保持原有枚举，新增 Ghost 变体
enum class ButtonVariant { Filled, Tonal, Outlined, Elevated, Ghost }
enum class ButtonSize { Sm, Md, Lg }
enum class ButtonIntent { Primary, Neutral, Danger }
enum class IconPlacement { Start, End, None }

@Composable
fun NDJCButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ButtonVariant = ButtonVariant.Filled,
    size: ButtonSize = ButtonSize.Md,
    intent: ButtonIntent = ButtonIntent.Primary,
    iconPlacement: IconPlacement = IconPlacement.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val tokens = Tokens.current()
    val cs = tokens.color
    val op = tokens.opacity
    val sp = tokens.space
    val rd = tokens.radius

    // 拟物感：更大圆角 + 更宽松内边距
    val paddings = when (size) {
        ButtonSize.Sm -> PaddingValues(
            horizontal = sp.md,
            vertical = sp.sm
        )
        ButtonSize.Md -> PaddingValues(
            horizontal = sp.lg,
            vertical = sp.md
        )
        ButtonSize.Lg -> PaddingValues(
            horizontal = sp.xl,
            vertical = sp.md + 2.dp
        )
    }

    // intent → 语义底色
    val (baseBg, baseFg) = when (intent) {
        ButtonIntent.Primary -> cs.primary to cs.onPrimary
        ButtonIntent.Neutral -> cs.secondaryContainer to cs.onSecondaryContainer
        ButtonIntent.Danger  -> cs.error to cs.onError
    }

    val container: Color
    val content: Color

    if (!enabled) {
        container = when (variant) {
            ButtonVariant.Ghost -> Color.Transparent.copy(alpha = op.disabled)
            ButtonVariant.Outlined -> Color.Transparent
            ButtonVariant.Elevated, ButtonVariant.Tonal, ButtonVariant.Filled -> baseBg.copy(alpha = op.disabled)
        }
        content = MaterialTheme.colorScheme.onSurface.copy(alpha = op.disabled)
    } else {
        when (variant) {
            ButtonVariant.Filled -> {
                // 拟物取向：不是纯扁平实色，使用语义主色但保持柔和
                container = baseBg
                content   = baseFg
            }
            ButtonVariant.Tonal -> {
                container = cs.secondaryContainer
                content   = cs.onSecondaryContainer
            }
            ButtonVariant.Outlined -> {
                container = Color.Transparent
                content   = cs.primary
            }
            ButtonVariant.Elevated -> {
                // Elevated 走 surface 背景 + 低阴影，符合外凸观感
                container = cs.surface
                content   = cs.primary
            }
            ButtonVariant.Ghost -> {
                container = Color.Transparent
                content   = when (intent) {
                    ButtonIntent.Primary -> cs.primary
                    ButtonIntent.Neutral -> cs.onSurfaceVariant
                    ButtonIntent.Danger  -> cs.error
                }
            }
        }
    }

    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(rd.lg), // 圆角更大
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container,
            disabledContentColor   = content
        ),
        contentPadding = paddings,
        elevation = when (variant) {
            ButtonVariant.Elevated -> ButtonDefaults.buttonElevation(defaultElevation = tokens.elevation.level2)
            ButtonVariant.Filled, ButtonVariant.Tonal -> ButtonDefaults.buttonElevation(defaultElevation = tokens.elevation.level1)
            else -> ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        },
        interactionSource = interactionSource
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
