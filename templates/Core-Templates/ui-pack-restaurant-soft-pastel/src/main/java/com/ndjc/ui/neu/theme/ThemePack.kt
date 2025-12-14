package com.ndjc.ui.neu.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 密度枚举：Regular / Compact
 * （现在先不区分具体数值，将来需要可以在 buildTokens 里做差异）
 */
enum class Density {
    Regular,
    Compact
}

/**
 * 主题包入口：
 * - mode：Light / Dark
 * - density：Regular / Compact
 * - tokens：当前模式下的 TokensSpec
 */
object ThemePack {

    @Volatile
    var mode: ThemeMode = ThemeMode.Light

    @Volatile
    var density: Density = Density.Regular

    @Volatile
    internal var currentTokens: TokensSpec? = null

    /** 对外暴露：当前 Tokens（如果 currentTokens 为 null，则按 mode/density 现算一份） */
    val tokens: TokensSpec
        get() = buildTokens(mode, density)

    fun useLight() {
        mode = ThemeMode.Light
    }

    fun useDark() {
        mode = ThemeMode.Dark
    }

    fun useRegular() {
        density = Density.Regular
    }

    fun useCompact() {
        density = Density.Compact
    }
}

/**
 * 实际构建 TokensSpec 的地方：
 * - 现在先只根据 ThemeMode 切换颜色；
 * - Radius / Space / Elevation / Opacity 直接走 NeuTokens 里的默认值；
 * - 将来如果想让 Compact 更紧凑，可以在这里对 Space 做一份“压缩版”再塞进去。
 */
private fun buildTokens(
    mode: ThemeMode,
    density: Density
): TokensSpec {
    val colors = colorSchemeFor(mode)

    // 目前先不区分 Regular / Compact，用同一套空间/圆角/阴影
    val radius = NeuTokens.Radius
    val space = NeuTokens.Space
    val elevation = NeuTokens.Elevation
    val opacity = NeuTokens.Opacity

    return TokensSpec(
        color = colors,
        radius = radius,
        space = space,
        elevation = elevation,
        blur = 12.dp,
        lightAlpha = 0.06f,
        shadowAlpha = 0.25f,
        opacity = opacity
    )
}

/** 兼容层：支持 Tokens.current() 与 Tokens.color/... 直达属性 */
object Tokens {

    private val v: TokensSpec
        get() = ThemePack.currentTokens ?: ThemePack.tokens

    fun current(): TokensSpec = v

    val color get() = v.color
    val space get() = v.space
    val radius get() = v.radius
    val elevation get() = v.elevation
    val blur: Dp get() = v.blur
    val lightAlpha get() = v.lightAlpha
    val shadowAlpha get() = v.shadowAlpha
    val opacity get() = v.opacity
}
