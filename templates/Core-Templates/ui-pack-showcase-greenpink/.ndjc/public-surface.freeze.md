# Public Surface Freeze（UI Pack）

本文件由 `sst.publicSurface` 自动生成，用于冻结 UI Pack 对外公共表面。

---

## Components

### AppBar
- slot: `header`
- required: `true`
- props: `title:string`, `navigationIcon:optional<Composable>`, `actions:list<Composable>`, `dense:boolean`

### NavBar
- alias: `Tabs`
- slot: `tabBar`
- required: `true`
- props: `items:list<NavItem{id:string,label:string}>`, `selectedId:string`, `onClick:(id:string)=>void`

### Button
- required: `true`
- props: `text:string`, `variant:enum(Filled|Tonal|Outlined|Elevated)`, `size:enum(Sm|Md|Lg)`, `intent:enum(Primary|Secondary|Neutral|Danger)`, `iconPlacement:enum(Start|End|Only)`, `enabled:boolean`, `onClick:()=>void`

### Card
- required: `true`
- props: `headline:string`, `subhead:optional<string>`, `clickable:boolean`

### ListItem
- required: `true`
- props: `title:string`, `subtitle:optional<string>`, `leading:optional<Composable>`, `trailing:optional<Composable>`

### FormField
- required: `true`
- props: `label:string`, `value:string`, `error:optional<string>`, `onChange:(v:string)=>void`

### Chip
- required: `false`
- props: `text:string`, `selected:boolean`, `onToggle:()=>void`

### Banner
- required: `false`
- props: `text:string`, `kind:enum(info|warn|error|success)`, `dismissible:boolean`

### Empty
- required: `false`
- props: `title:string`, `message:optional<string>`, `illustration:optional<resource.icon|image>`

### Skeleton
- required: `false`
- props: `shape:enum(rect|circle)`, `lines:int`

### BottomSheet
- required: `false`
- props: `open:boolean`, `title:optional<string>`, `content:Composable`, `onClose:()=>void`

### Toast
- required: `false`
- props: `message:string`, `durationMs:int`


## Theme & Tokens Frozen API

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

