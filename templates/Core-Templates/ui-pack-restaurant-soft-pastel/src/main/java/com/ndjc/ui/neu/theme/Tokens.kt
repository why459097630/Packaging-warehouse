package com.ndjc.ui.neu.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 主题模式（按需接你项目里的枚举） */
enum class ThemeMode { Light, Dark }

/** Neumorph 主题 Token（颜色 / 半径 / 阴影 / 间距等） */
object NeuTokens {

    /** —— 颜色：柔和、低对比、冷紫调 —— */
    object Light {
        val primary          = Color(0xFF6E73FF)
        val onPrimary        = Color(0xFFFFFFFF)
        val surface          = Color(0xFFF5F6FA)
        val onSurface        = Color(0xFF1F2230)
        val surfaceVariant   = Color(0xFFE8EAF3)
        val outline          = Color(0xFFCED2E1)
        val inverseSurface   = Color(0xFF1C1E26)
        val inverseOnSurface = Color(0xFFEDEFFC)
        val secondary        = Color(0xFF9FA4FF)
        val tertiary         = Color(0xFFB3B8FF)
        val error            = Color(0xFFEF5350)
    }

    object Dark {
        val primary          = Color(0xFF9AA0FF)
        val onPrimary        = Color(0xFF121212)
        val surface          = Color(0xFF111318)
        val onSurface        = Color(0xFFE8EAFF)
        val surfaceVariant   = Color(0xFF1A1D24)
        val outline          = Color(0xFF2A2E3A)
        val inverseSurface   = Color(0xFFF5F6FA)
        val inverseOnSurface = Color(0xFF16181E)
        val secondary        = Color(0xFFB9BDFF)
        val tertiary         = Color(0xFFC6CAFF)
        val error            = Color(0xFFEF9A9A)
    }

    /** 圆角半径（柔和） */
    object Radius {
        val xs: Dp = 8.dp
        val sm: Dp = 12.dp
        val md: Dp = 16.dp
        val lg: Dp = 20.dp
        val xl: Dp = 28.dp
        val pill: Dp = 999.dp
    }

    /** 间距密度（拟态留白更大） */
    object Space {
        val xs: Dp = 4.dp
        val sm: Dp = 8.dp
        val md: Dp = 12.dp
        val lg: Dp = 16.dp
        val xl: Dp = 24.dp
        val xxl: Dp = 32.dp
    }

    /** 阴影/层级（轻度） */
    object Elevation {
        val level0: Dp = 0.dp
        val level1: Dp = 1.dp
        val level2: Dp = 3.dp
        val level3: Dp = 6.dp
    }
    /** 透明度（按钮等组件用） */
    object Opacity {
        val enabled: Float = 1.0f      // 正常
        val disabled: Float = 0.38f    // 禁用
        val pressed: Float = 0.90f     // 按下
    }
}

/* -------------------------------------------------------------------------- */
/*                              Tokens / Composition                          */
/* -------------------------------------------------------------------------- */

/**
 * 对外暴露给 UI 组件用的 Token 规格：
 * - color: 用 Material3 的 ColorScheme
 * - radius/space/elevation: 保持对象结构，支持 t.radius.lg / t.space.sm / t.elevation.level1
 */
data class TokensSpec(
    val color: ColorScheme,
    val radius: NeuTokens.Radius = NeuTokens.Radius,
    val space: NeuTokens.Space = NeuTokens.Space,
    val elevation: NeuTokens.Elevation = NeuTokens.Elevation,
    val blur: Dp = 12.dp,          // 模糊半径基准
    val lightAlpha: Float = 0.06f, // 高光透明度
    val shadowAlpha: Float = 0.25f, // 阴影透明度
    val opacity: NeuTokens.Opacity = NeuTokens.Opacity
)

/** CompositionLocal：在 Theme 里注入 TokensSpec */
val LocalTokens = staticCompositionLocalOf {
    // 默认给一个 Light 配置，避免空指针；真正使用时会在 Theme 中覆盖
    TokensSpec(color = lightColorScheme())
}

/** 供组件使用：val t = Tokens.current() */

/** 如果需要在 Theme 里按模式选择配色，可用此函数 */
fun colorSchemeFor(mode: ThemeMode): ColorScheme {
    return when (mode) {
        ThemeMode.Light -> lightColorScheme(
            primary = NeuTokens.Light.primary,
            onPrimary = NeuTokens.Light.onPrimary,
            secondary = NeuTokens.Light.secondary,
            onSecondary = NeuTokens.Light.onSurface,
            tertiary = NeuTokens.Light.tertiary,
            onTertiary = NeuTokens.Light.onSurface,
            error = NeuTokens.Light.error,
            onError = NeuTokens.Light.onPrimary,
            background = NeuTokens.Light.surface,
            onBackground = NeuTokens.Light.onSurface,
            surface = NeuTokens.Light.surface,
            onSurface = NeuTokens.Light.onSurface,
            surfaceVariant = NeuTokens.Light.surfaceVariant,
            outline = NeuTokens.Light.outline,
            inverseSurface = NeuTokens.Light.inverseSurface,
            inverseOnSurface = NeuTokens.Light.inverseOnSurface
        )

        ThemeMode.Dark -> darkColorScheme(
            primary = NeuTokens.Dark.primary,
            onPrimary = NeuTokens.Dark.onPrimary,
            secondary = NeuTokens.Dark.secondary,
            onSecondary = NeuTokens.Dark.onSurface,
            tertiary = NeuTokens.Dark.tertiary,
            onTertiary = NeuTokens.Dark.onSurface,
            error = NeuTokens.Dark.error,
            onError = NeuTokens.Dark.onPrimary,
            background = NeuTokens.Dark.surface,
            onBackground = NeuTokens.Dark.onSurface,
            surface = NeuTokens.Dark.surface,
            onSurface = NeuTokens.Dark.onSurface,
            surfaceVariant = NeuTokens.Dark.surfaceVariant,
            outline = NeuTokens.Dark.outline,
            inverseSurface = NeuTokens.Dark.inverseSurface,
            inverseOnSurface = NeuTokens.Dark.inverseOnSurface
        )
    }
}
