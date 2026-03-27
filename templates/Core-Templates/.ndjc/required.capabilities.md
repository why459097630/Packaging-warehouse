# Required Capabilities (Template → must be provided by UI Pack + Modules)

本模板运行所需的最低能力（required）。缺任一项，构建必须失败。

---

## required（来自 SST·requiredCapabilities）

以下为模板**强依赖**的能力，UI 包和模块必须共同提供：

- (none)

---

## optional（模板可以使用，但不得依赖）

以下能力若 UI 包提供，模板可以选择性使用，但不得依赖：

- layout.responsive
- components.card
- components.modal

---

## 说明
- required 列表 = 完全取自 `sst.json` 的 `requiredCapabilities`。
- optional 仅作提示，不影响构建结果。
