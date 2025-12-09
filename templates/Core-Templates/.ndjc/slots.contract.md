# Slots Contract (Template Layout Slots)

来自 `sst.slots` 的插槽约定，模板与 UI / 模块均需遵守。

---
## 1. Standard Slots（必须实现）

- header
- hero
- primary
- secondary
- detail
- sheet
- tabBar

## 2. Optional Slots（可选实现）

- fab
- dialog
- settings

## 3. Meta 信息

### layoutIntent
- stack
- list
- grid
- staggered
- tabs

### scrollable
- none
- vertical
- horizontal

### priority
- low
- normal
- high

### gestures
- pullToRefresh
- swipeToDismiss
- dragHandle

## 4. Override 顺序

1. template（优先级 1）
2. uiPack（优先级 2）
3. featureModule（优先级 3）

---
## 说明
- 本文件为 slots 单一契约来源的 markdown 视图，仅由 sst.json 生成，不手动修改。
