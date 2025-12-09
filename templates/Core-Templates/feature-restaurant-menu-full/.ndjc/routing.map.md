# Routing Map (Module: feature-restaurant-menu)

本模块与模板之间的路由映射关系，遵循 ndjc-sst 中的 routing.schema。  

## 1. 模板路由的对齐

根据单一真源，模板侧定义的标准路由为：  

- `home`   （无参数）
- `detail` （参数：id: string）  

在“餐厅菜单 App”场景下，本模块对这两个路由的语义做如下实现：

| Template Route ID | Module Route ID | Params         | 使用能力                | 说明                         |
|-------------------|-----------------|----------------|-------------------------|------------------------------|
| home              | home            | 无             | feature.restaurant.menu | 菜单列表页（按分类展示菜品） |
| detail            | detail          | id: string     | feature.restaurant.menu | 菜品详情页                   |

- `home`：作为应用入口页，对应“餐厅菜单列表”，展示所有菜品及分类；
- `detail`：展示单个菜品的详情，通过 `id` 参数定位具体菜品。

## 2. 管理员相关路由与行为

管理员登录与管理界面不新增新的模板路由 ID，而是：

- 从 `home` 页面触发管理员登录入口；
- 登录表单通过 `sheet` 插槽呈现（底部弹出或半屏抽屉，由 UI 包决定）；
- 登录成功后进入管理界面，挂载在 `settings` 插槽中：

  - 显示菜品列表（带编辑、删除、添加操作）；
  - 其内部导航完全由模块内部管理，不扩展模板 routing.schema。

> 说明：
>
> - 模块严格遵循 ndjc-sst 中的 routing.schema，不新增顶层 routeId；
> - Deep Link 如 `app://detail/{id}` 仍按单一真源的约定工作，落在 `detail` 页；
> - 若未来模板扩展新的 routeId，本模块可选择增加对应实现并更新本文档。
