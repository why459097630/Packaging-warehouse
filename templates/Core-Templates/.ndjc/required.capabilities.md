# Required Capabilities (Template → must be provided by UI Pack + Modules)

本模板运行所需的能力约束来自 `sst.capabilities`。
required 缺任一项，构建必须失败；optional 为可选能力声明。

---

## required（来自 SST·capabilities.required）

以下为模板**强依赖**的能力，UI 包和模块必须共同提供：

- routing.basic
- theme.base
- components.core

---

## optional（来自 SST·capabilities.optional）

以下能力若 UI 包提供，模板可以选择性使用，但不得把它们当成 required：

- darkMode
- rtl
- density.compact

---
## 说明
- required 列表 = `sst.capabilities.required`
- optional 列表 = `sst.capabilities.optional`
- checker 会据此检查：Template.required ⊆ UI.provided ∪ Modules.provided
