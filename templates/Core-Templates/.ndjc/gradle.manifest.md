# Gradle / AndroidManifest Contract

来自 `sst.gradleManifest` 的构建级约束，仅描述“形态 & 冲突策略”，不绑定具体实现。

---
## 1. ApplicationId

- applicationId: `string`

## 2. Permissions（合并策略：union）

- `list<string>`

## 3. Dependencies（合并策略：union + conflictResolution）

- `list<maven:group:artifact:version>`

## 4. resConfigs

- `list<string>`

## 5. Conflict Resolution

- strategy: `platformBOM; unresolved -> fail`

---
