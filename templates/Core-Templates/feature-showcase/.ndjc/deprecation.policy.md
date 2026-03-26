# Deprecation Policy (Module: feature-demo)

本模块对外暴露的契约（.ndjc 中描述的能力、slots、路由 ID 等）遵循以下废弃策略：

1. **能力 ID（capability id）**
   - 一旦发布并被模板引用（如 `feature.home`、`feature.detail`），不得直接删除。
   - 若需要替换，需新增新的能力 ID，并在文档中标注旧能力为 deprecated。

2. **路由 ID**
   - `home` / `detail` 为模板约定的 routeId，不得变更。
   - 如需新增其它 routeId，必须保证模板可识别或通过路由扩展机制接入。

3. **Slots 使用**
   - `header` / `primary` / `detail` / `tabBar` 的使用语义需保持稳定。
   - 若修改布局方式，仅允许在保持插槽语义的前提下调整内部 UI 实现。

4. **Tokens 使用**
   - 模块使用的 token 必须来自 SST 中定义的 domains & keys。
   - 不得在模块内部“发明”新的 token key。

> 如需进行 Breaking Change，建议先升级 SST 或模板版本，再对模块进行适配。
