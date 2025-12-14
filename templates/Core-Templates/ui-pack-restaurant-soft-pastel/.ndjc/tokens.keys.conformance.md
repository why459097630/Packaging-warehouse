# Tokens Keys Conformance

UI 包 TokenSet 必须覆盖 SST `tokens.domains` 中的所有 **required** 域与 key；命名规则必须一致。

---

## 必填（必须全部存在）

### color
- brand.primary
- brand.secondary
- surface.bg
- surface.card
- text.primary
- text.secondary
- success
- warn
- danger

### space
- xs
- sm
- md
- lg
- xl

### radius
- none
- sm
- md
- lg
- pill

### elevation
- level0
- level1
- level2
- level3
- level4

### typography
- display
- headline
- title
- body
- label

### opacity
- disabled
- medium
- high

---

## 自检项目
- [ ] 命名与域完全一致（无差异/无拼写错误）
- [ ] 值类型正确（颜色/尺寸/数字可解析）
- [ ] UI 包全部覆盖 required 名单
