# Tokens Requirements for Template
本模板所需的 Design Tokens 域与必备键（**Required**）。

---
### color
- brand.primary
- brand.secondary
- danger
- success
- surface.bg
- surface.card
- text.primary
- text.secondary
- warn

### space
- lg
- md
- sm
- xl
- xs

### radius
- lg
- md
- none
- pill
- sm

### elevation
- level0
- level1
- level2
- level3
- level4

### typography
- body
- display
- headline
- label
- title

### opacity
- disabled
- high
- medium
---
## 使用规则（SST 合规说明）
- 上述 required 列表完全来自 sst.json 的 `required.tokens`，缺任意 key → 构建失败。
- UI 包可提供更多 Token，但不得比 required 列表少。
