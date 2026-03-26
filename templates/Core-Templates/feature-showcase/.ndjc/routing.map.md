# Routing Map (Module: feature-showcase)

本模块与模板之间的路由映射关系，遵循 ndjc-sst 中的 routing.schema。  

## 1. 模板路由的对齐

根据单一真源，模板侧定义的标准路由为：  

- `home`   （无参数）
- `detail` （参数：id: string）  

在“Showcase（展示）”场景下，本模块对这两个路由的语义做如下实现：

| Template Route ID | Module Route ID | Params         | 使用能力         | 说明                          |
|-------------------|-----------------|----------------|------------------|-------------------------------|
| home              | home            | 无             | feature.showcase | 展示主页（内容区块 + CTA）    |
| detail            | detail          | id: string     | feature.showcase | 内容详情页（按 id 定位条目）  |

- `home`：作为应用入口页，对应“展示主页”，展示内容区块与引导转化入口；
- `detail`：展示单个内容条目的详情，通过 `id` 参数定位。

## 2. 管理侧相关路由与行为

管理侧不新增新的模板路由 ID，而是：

- 从 `home` 页面（或 UI 包提供的入口）触发留资表单入口；
- 留资表单通过 `sheet` 插槽呈现（底部弹出或半屏抽屉，由 UI 包决定）；
- 管理面板挂载在 `settings` 插槽中：

  - 管理展示内容（新增 / 编辑 / 删除 / 排序）；
  - 管理线索（查看 / 标记状态 / 导出）；
  - 其内部导航完全由模块内部管理，不扩展模板 routing.schema。

> 说明：
>
> - 模块严格遵循 ndjc-sst 中的 routing.schema，不新增顶层 routeId；
> - Deep Link 如 `app://detail/{id}` 仍按单一真源的约定工作，落在 `detail` 页；
> - 若未来模板扩展新的 routeId，本模块可选择增加对应实现并更新本文档。
