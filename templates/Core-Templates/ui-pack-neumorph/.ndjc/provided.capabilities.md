# Provided Capabilities (UI Pack: ui-pack-neumorph)

# 本文件声明此 UI 包对外提供的能力列表。
# 至少需要覆盖模板的 required 能力，以及 `sst.json` 中声明的 optional 能力。

# 说明：
# - “Template Required” 覆盖 `sst.capabilities.required`。
# - “Optional” 覆盖 `sst.capabilities.optional`；UI 包提供后，模板可以选择性使用。
# - 如果未来 UI 包有额外能力，可在文末追加条目。

## 1. Template Required （SST:capabilities.required）

- routing.basic
- theme.base
- components.core

## 2. Optional （SST:capabilities.optional）

- darkMode
- rtl
- density.compact
