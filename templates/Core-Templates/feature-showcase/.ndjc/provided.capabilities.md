# Provided Capabilities (Module: feature-showcase)

功能模块对外声明的业务能力列表。  
注意：模块不重复声明 `routing.basic` / `theme.base` / `components.core` 等基础能力，  
这些由模板和 UI 包负责提供，对齐 ndjc-sst。  

## feature capabilities

- `feature.showcase`  
  - 提供“展示（Showcase）”相关的整套页面与操作能力，包含：

    - 使用模板定义的 `home` 路由作为**展示主页**：
      - 展示内容区块（如：Hero 标题、亮点列表、图片/案例卡片、CTA 按钮等，具体样式由 UI 包决定）；
      - 提供“联系 / 留资”入口（按钮位置由 UI 包决定），触发打开留资表单。

    - 使用模板定义的 `detail` 路由作为**内容详情页**：
      - 展示单个内容条目的详情（图文、画廊、信息块等）；
      - 通过 `id` 参数定位具体内容；
      - 返回主页的导航行为遵循模板 routing 约定。

    - 通过 `sheet` / `settings` 插槽提供**管理侧操作能力**：
      - `sheet`：留资表单面板（姓名/电话/邮箱/留言等字段，字段开关与顺序由 configSchema 控制）；
      - `settings`：管理面板（管理展示内容：新增/编辑/删除；管理线索：查看/标记状态/导出）。

> 说明：
>
> - 本模块面向“展示 + 获客（Lead Capture）”场景，适合作为多行业 App 的通用展示入口模块；
> - 本模块不包含下单/支付与预约履约能力；
> - 具体 UI 呈现（卡片、列表、按钮样式、弹层形态）由 UI 包与模板在约定插槽内实现。
