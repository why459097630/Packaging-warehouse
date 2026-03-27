# Tokens Keys Conformance（UI Pack）

本文件由 `sst.tokens` 自动生成，用于说明 UI Pack Token 键命名与校验规则。

---

## Rules

- naming: `kebab.or.dot`
- validation: `all required domains & keys must exist; enums must match; values must be parseable`

## Domains

### color
- required: `true`
- keys: `brand.primary`, `brand.secondary`, `surface.bg`, `surface.card`, `text.primary`, `text.secondary`, `success`, `warn`, `danger`

### space
- required: `true`
- keys: `xs`, `sm`, `md`, `lg`, `xl`

### radius
- required: `true`
- keys: `none`, `sm`, `md`, `lg`, `pill`

### elevation
- required: `true`
- keys: `level0`, `level1`, `level2`, `level3`, `level4`

### typography
- required: `true`
- keys: `display`, `headline`, `title`, `body`, `label`

### opacity
- required: `true`
- keys: `disabled`, `medium`, `high`

### duration
- required: `false`
- keys: `fast`, `normal`, `slow`

### density
- required: `false`
- enum: `comfortable`, `compact`, `auto`

### themeMode
- required: `false`
- enum: `light`, `dark`, `system`

