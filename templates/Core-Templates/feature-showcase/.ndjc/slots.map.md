# Slots Map (Module: feature-showcase)

本模块在模板定义的各页面插槽（slots）中的挂载方式。  

模板侧 `slots` 在 ndjc-sst 中定义了：  

- 标准插槽：`header` / `hero` / `primary` / `secondary` / `detail` / `sheet` / `tabBar`  
- 可选插槽：`fab` / `dialog` / `settings`  

本模块主要使用 `primary` / `detail` / `sheet` / `settings` 四类插槽，实现通用展示与获客（Showcase + Lead Capture）业务。

## 1. 展示主页（routeId = home）

- 使用 `primary` 插槽作为展示主页的主体区域：

  - 展示内容区块（例如：主标题/副标题、卖点列表、图片/案例卡片、客户评价等，区块结构由模块输出，具体排版样式由 UI 包决定）；
  - 支持点击内容卡片跳转到 `detail` 路由（内容详情页）；
  - 提供“联系 / 留资”入口按钮（可位于列表顶部、底部 CTA、或悬浮按钮，由 UI 包决定），触发打开 `sheet` 插槽中的留资表单。

- `header` / `tabBar` 等插槽仍由模板 + UI 包提供（如 AppBar、底部导航），本模块不覆盖。

## 2. 内容详情页（routeId = detail）

- 使用 `detail` 插槽展示单个内容条目的详细信息：

  - 头图/画廊（可选）；
  - 标题、摘要、正文内容区块；
  - 可选的“联系 / 留资”入口（由 UI 包决定放置位置），触发 `sheet` 留资表单。

- `primary` 插槽在 `detail` 路由下不由本模块使用，避免语义混淆。

## 3. 留资表单（Lead Capture Form）

- 使用 `sheet` 插槽呈现留资表单界面：

  - 由 `home` / `detail` 页的“联系 / 留资”入口触发；
  - 提供表单字段（如：姓名/电话/邮箱/留言；字段开关与顺序由模块 configSchema 控制）；
  - 提交成功后关闭 `sheet`，并在管理端可见该条线索记录。

> 说明：
> - 具体呈现形式（底部弹出 / 半屏抽屉）由 UI 包的 `BottomSheet` 等组件实现；
> - 模块只负责打开/关闭与提交逻辑、状态管理与数据落库。

## 4. 管理面板（Admin Dashboard）

- 使用 `settings` 插槽作为管理面板的主体区域：

  - 展示内容管理：
    - 内容列表（可排序）；
    - 新增/编辑/删除内容条目（标题、摘要、正文、图片等）。
  - 线索管理：
    - 线索列表与详情；
    - 支持标记状态（新 / 已联系 / 已成交）；
    - 支持导出（如 CSV，具体入口由 UI 包决定）。

- 若 UI 包暂未实现 `settings` 插槽，也可以由模板降级为普通页面区域，但本模块将该能力声明为“优先挂载在 settings 插槽”。

## 5. 其他插槽

- 当前版本本模块不使用 `hero` / `secondary` / `fab` / `dialog` 等插槽；
- 若未来需要展示全屏 Banner / 轮播等，可在保持 SST 插槽语义的前提下扩展本文件。
