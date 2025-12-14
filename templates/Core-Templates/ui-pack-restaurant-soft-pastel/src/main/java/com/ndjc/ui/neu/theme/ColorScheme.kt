package com.ndjc.ui.neu.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** 将 Tokens → Material3 ColorScheme（确保真的用的是 neu 的色系） */
fun neuLightColorScheme(): ColorScheme = lightColorScheme(
    primary = NeuTokens.Light.primary,
    onPrimary = NeuTokens.Light.onPrimary,
    secondary = NeuTokens.Light.secondary,
    tertiary = NeuTokens.Light.tertiary,

    surface = NeuTokens.Light.surface,
    onSurface = NeuTokens.Light.onSurface,
    surfaceVariant = NeuTokens.Light.surfaceVariant,
    outline = NeuTokens.Light.outline,

    background = NeuTokens.Light.surface,
    onBackground = NeuTokens.Light.onSurface,

    error = NeuTokens.Light.error,
)

fun neuDarkColorScheme(): ColorScheme = darkColorScheme(
    primary = NeuTokens.Dark.primary,
    onPrimary = NeuTokens.Dark.onPrimary,
    secondary = NeuTokens.Dark.secondary,
    tertiary = NeuTokens.Dark.tertiary,

    surface = NeuTokens.Dark.surface,
    onSurface = NeuTokens.Dark.onSurface,
    surfaceVariant = NeuTokens.Dark.surfaceVariant,
    outline = NeuTokens.Dark.outline,

    background = NeuTokens.Dark.surface,
    onBackground = NeuTokens.Dark.onSurface,

    error = NeuTokens.Dark.error,
)

/** 便于在 Theme.kt 里按模式选择 */
fun colorSchemeOf(mode: ThemeMode): ColorScheme =
    if (mode == ThemeMode.Dark) neuDarkColorScheme() else neuLightColorScheme()
