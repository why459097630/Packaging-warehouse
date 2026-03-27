# Preflight Provider（UI Pack）

本文件由 `sst.preflight.requiredChecks` 自动生成，用于声明 UI Pack 参与实现的预检能力。

---

## Required Checks

- slots.exist
- resources.keysResolvable
- tokens.complete
- routing.connected

## UI Pack Provider Notes

- 本 UI Pack 至少要能配合模板 / 模块完成上述 requiredChecks。
- 若某项主要由模块承担，也必须在联调时可被 checker 识别为“已覆盖”。
