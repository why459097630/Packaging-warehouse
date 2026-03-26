# Golden Snapshots (Module: feature-demo)

本文件用于约定本模块在关键 UI/状态上的 Golden Snapshot 场景，方便回归对比。

## 建议快照场景

1. Home 页面（无数据/空态）
   - header：显示应用标题或模块标题
   - primary：显示空列表或占位提示
   - tabBar：正确高亮对应 Tab

2. Home 页面（有列表数据）
   - primary：展示若干列表项
   - 滚动与分隔线表现符合设计

3. Detail 页面（正常数据）
   - header：标题与返回操作正确
   - detail：展示完整详情内容

4. Detail 页面（异常状态，如数据缺失）
   - 展示错误提示 / 占位 Skeleton
   - 不导致崩溃

> Golden Snapshots 的具体实现（如 Screenshot Test / Compose Preview Capture）由实现方在测试工程中维护。
