# Tokens Requirements for Template
# 本模板所需的 Design Tokens 域与必备键（**Required**）。
# 本文件完全由 scripts/sst.json 的 `tokens.requiredTokens` 推导而来；缺任意一项 → 构建失败。

---

### required tokens (from SST.requiredTokens)

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

# 使用规则（SST 合规说明）
# - 以上 required 列表完全来自 sst.json 的 `requiredTokens`
# - UI 包可以提供更多 Token，但不得比 required 列表少
