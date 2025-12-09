# Theme Tokens Usage (Module: feature-demo)

本模块在 UI 实现中**预计会使用**的设计 Token 集合（消费方视角）。
所有 Token 必须来自 SST 定义的 domains & keys，并由 UI 包实际提供。

## 1. 颜色（color.*）

- color.brand.primary
- color.brand.secondary
- color.surface.bg
- color.surface.card
- color.text.primary
- color.text.secondary
- color.success
- color.warn
- color.danger

## 2. 间距（space.*）

- space.xs
- space.sm
- space.md
- space.lg
- space.xl

## 3. 圆角（radius.*）

- radius.none
- radius.sm
- radius.md
- radius.lg
- radius.pill

## 4. 阴影（elevation.*）

- elevation.level0
- elevation.level1
- elevation.level2
- elevation.level3
- elevation.level4

## 5. 排版（typography.*）

- typography.display
- typography.headline
- typography.title
- typography.body
- typography.label

## 6. 透明度（opacity.*）

- opacity.disabled
- opacity.medium
- opacity.high

> 实际使用时，模块代码应通过 UI 包提供的 `Tokens.current()` / `LocalSpacing` / `LocalOpacity`
> 等 API 获取上述 Token 的具体值。
