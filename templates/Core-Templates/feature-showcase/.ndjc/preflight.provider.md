# Preflight Provider（Feature Module）

本文件由 `sst.preflight.requiredChecks` 自动生成，用于声明模块参与实现的预检能力。

---

## Required Checks

- slots.exist
- resources.keysResolvable
- tokens.complete
- routing.connected

## Module Provider Notes

- 模块需与模板 / UI Pack 共同覆盖上述 requiredChecks。
- 若某项主要由 UI 承担，模块侧也应声明其已知的协作边界。
