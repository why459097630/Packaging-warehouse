# Module Manifest Contract

来自 `sst.moduleManifest` 的模块侧契约定义，所有功能模块的 .ndjc / manifest.json 必须遵守。

---
## 1. Fields

- `moduleId`: `string`
- `version`: `semver`
- `type`: `enum(ui|feature|tooling)`
- `supportedSlots`: `list<slotId>`
- `routes`: `list<route>`
- `capabilities`: `list<string>`
- `permissions`: `list<string>`
- `resources`: `map<string,resourceKey>`
- `hooks`: `map<string,handler>`
- `configSchema`: `json-schema`

## 2. Rules

- module must not depend on a concrete template implementation; only depend on this contract

---
## 说明
- 模块注册表和模块 .ndjc 应与本契约保持一一对应。
