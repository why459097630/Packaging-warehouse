# Preflight Provider (Module: feature-demo)

本模块在集成到 NDJC 流水线时，建议在构建前执行以下自检（preflight）步骤：

1. **能力对齐检查**
   - 确认本模块 `.ndjc/provided.capabilities.md` 中声明的能力与实现代码一致。
   - 典型能力：`feature.home`、`feature.detail`。

2. **路由对齐检查**
   - 确认模块实际注册的 routeId 覆盖模板 `routing.schema.md` 中要求的 `home` / `detail`。
   - 确认 `detail` 路由能接受 `id: String` 参数。

3. **Slots 使用检查**
   - Home：至少使用 `header` + `primary`，可选使用 `tabBar`。
   - Detail：至少使用 `header` + `detail`。
   - 不使用模板未定义的 slot 名。

4. **Tokens 使用检查**
   - 扫描模块 UI 代码中使用的 token key，确保均属于 SST 中定义的 domains & keys。
   - 例如：`color.brand.primary`、`color.surface.bg`、`typography.body` 等。

> 以上检查可通过脚本或单元测试自动化完成，此文件仅作为契约级说明。
