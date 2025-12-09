# Provided Capabilities (Module: feature-restaurant-menu)

功能模块对外声明的业务能力列表。  
注意：模块不重复声明 `routing.basic` / `theme.base` / `components.core` 等基础能力，  
这些由模板和 UI 包负责提供，对齐 ndjc-sst。  

## feature capabilities

- `feature.restaurant.menu`  
  - 提供“餐厅菜单”相关的整套页面与操作能力，包含：

    - 使用模板定义的 `home` 路由作为**菜单列表页**：
      - 按分类展示菜品（中/英文名称、当前价格、推荐标记等）；
      - 支持从列表导航到菜品详情页；
      - 从列表进入管理员登录入口（底部按钮 / 右上角按钮，具体样式由 UI 包决定）。

    - 使用模板定义的 `detail` 路由作为**菜品详情页**：
      - 展示大图、中英文菜名、描述、原价/折扣价、售罄状态等；
      - 返回菜单列表的导航行为遵循模板 routing 约定。

    - 通过 `sheet` / `settings` 插槽提供**管理员侧操作能力**：
      - `sheet`：管理员登录面板（输入密码 / PIN 后进入管理模式）；
      - `settings`：菜单管理界面（新增 / 编辑 / 删除菜品，调整推荐与售罄状态）。

> 说明：
>
> - 本模块面向“餐厅菜单 AppCore”场景，适合作为整套 App 的核心业务模块使用；
> - 顾客仅浏览菜单，不涉及下单/支付能力；
> - 管理员登录与管理界面的具体 UI 由 UI 包和模板在约定插槽内实现。
