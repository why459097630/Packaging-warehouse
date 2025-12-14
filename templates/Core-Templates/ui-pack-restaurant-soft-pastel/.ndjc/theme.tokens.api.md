# Theme & Tokens API Freeze
# 本文件描述 UI 包对外暴露的 Theme / Tokens API，并声明本包覆盖的核心 Design Tokens。
# 说明行全部以 `#` 开头，脚本只会读取下面的 Token 列表。

---

# 1. Kotlin API（冻结示意，仅文档，不参与脚本校验）
# - Kotlin 包名：com.ndjc.ui.m3
# - ThemePack:
#   @Composable NDJCThemePack(
#     dark: Boolean = false,
#     space: Spacing = Spacing(),
#     opacity: Opacity = Opacity(),
#     content: @Composable () -> Unit
#   )
# - Tokens:
#   object Tokens { @Composable fun current(): TokenSet }
# - TokenSet:
#   data class TokenSet(val space: Spacing, val opacity: Opacity)
# - Spacing:
#   data class Spacing(val xs:Int=4,val sm:Int=8,val md:Int=12,val lg:Int=16,val xl:Int=20)
# - Opacity:
#   data class Opacity(val disabled:Float=0.38f,val medium:Float=0.60f,val high:Float=1.00f)
# - Locals:
#   LocalSpacing, LocalOpacity
# - 组件 API（与 SST publicSurface.themeAndTokens.frozenAPI 对齐）

---

# 2. Tokens Domains & Required Keys（来自 SST）
# 以下为 UI 包必须至少覆盖的一组 Design Tokens（**Required**）。
# 列表与 `Core-Templates/.ndjc/tokens.requirements.md` 以及 sst.json 的 `tokens.requiredTokens` 一一对应。

- color.brand.primary
- color.brand.secondary
- color.surface.bg
- color.surface.card
- color.text.primary
- color.text.secondary
- color.success
- color.warn
- color.danger

- space.xs
- space.sm
- space.md
- space.lg
- space.xl

- radius.none
- radius.sm
- radius.md
- radius.lg
- radius.pill

- elevation.level0
- elevation.level1
- elevation.level2
- elevation.level3
- elevation.level4

- typography.display
- typography.headline
- typography.title
- typography.body
- typography.label

- opacity.disabled
- opacity.medium
- opacity.high

---

# 3. 说明
# - 上述列表 = SST.requiredTokens（本 UI 包全部实现）
# - UI 包可以在此基础上增加更多 Token（例如 hover、pressed 等），不会影响构建。
# - Token 命名需满足 SST.tokens.rules.naming = "kebab.or.dot" 要求。
