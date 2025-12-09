# Required Capabilities (Template → must be provided by UI Pack + Modules)

# 本模板运行所需的最低能力（required）。缺任一项，构建必须失败。
# 本文件与 `sst.json` 的 `capabilities.required` / `capabilities.optional`
# 完全对齐。

# 说明：
# - required 列表：模板可以“强依赖”的基础能力，必须由 UI 包 + 模块共同提供。
# - optional 列表：模板可以选择性使用，但不得视为强依赖。
# - 实际对齐检查时，脚本会把下面所有条目视为一个集合进行校验。

## required （来自 SST:capabilities.required）

- routing.basic
- theme.base
- components.core

## optional （模板可以使用，但不得依赖）

- darkMode
- rtl
- density.compact
