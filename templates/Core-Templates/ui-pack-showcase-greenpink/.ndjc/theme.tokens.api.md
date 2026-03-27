# Theme Tokens API（UI Pack）

本文件由 `sst.json` 自动生成，用于声明 UI Pack 对主题 / Tokens / Frozen API 的实现契约。

---

## 1. Required Token Domains & Keys

本 UI Pack 至少需要提供以下 Design Tokens。可扩展更多键，但不能缺少这些键。

## color

- brand.primary
- brand.secondary
- danger
- success
- surface.bg
- surface.card
- text.primary
- text.secondary
- warn

## space

- lg
- md
- sm
- xl
- xs

## radius

- lg
- md
- none
- pill
- sm

## elevation

- level0
- level1
- level2
- level3
- level4

## typography

- body
- display
- headline
- label
- title

## opacity

- disabled
- high
- medium

## 2. Frozen Theme API

### kotlinPackage
- `com.ndjc.ui.m3`

### ThemePack
- `@Composable NDJCThemePack(dark: Boolean = false, space: Spacing = Spacing(), opacity: Opacity = Opacity(), content: @Composable () -> Unit)`

### Tokens
- `object Tokens { @Composable fun current(): TokenSet }`

### TokenSet
- `data class TokenSet(val space: Spacing, val opacity: Opacity)`

### Spacing
- `data class Spacing(val xs:Int=4,val sm:Int=8,val md:Int=12,val lg:Int=16,val xl:Int=20)`

### Opacity
- `data class Opacity(val disabled:Float=0.38f,val medium:Float=0.60f,val high:Float=1.00f)`

### Locals
- `LocalSpacing`
- `LocalOpacity`

### AppBar
- `@Composable NDJCAppBar(title:String, modifier:Modifier=Modifier, navigationIcon:(@Composable ()->Unit)?=null, actions:@Composable RowScope.()->Unit = {})`

### TabBar
- `@Composable NDJCTabBar(items: List<NavItem>, selectedId: String, onClick: (String)->Unit, modifier: Modifier = Modifier)`

### NavItem
- `data class NavItem(val id:String, val label:String)`

### ButtonEnums
- `enum ButtonVariant { Filled, Tonal, Outlined, Elevated }`
- `enum ButtonSize { Sm, Md, Lg }`
- `enum ButtonIntent { Primary, Secondary, Neutral, Danger }`
- `enum IconPlacement { Start, End, Only }`

### ButtonComposable
- `@Composable NDJCButton(text:String, modifier:Modifier=Modifier, onClick:()->Unit={}, variant:ButtonVariant=Filled, size:ButtonSize=Md, intent:ButtonIntent=Primary, iconPlacement:IconPlacement=Start, enabled:Boolean=true)`

## 3. Token Rules

- naming: `kebab.or.dot`
- validation: `all required domains & keys must exist; enums must match; values must be parseable`

---

## 说明
- 此处 domain & key 与 `Core-Templates/.ndjc/tokens.requirements.md` 保持一一对应。
- UI Pack 可以提供更多 Token，但不得比 required 列表少。
- Frozen API 来自 `sst.publicSurface.themeAndTokens.frozenAPI`，UI 层实现不得私自漂移。
