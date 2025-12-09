# CDC Runner (Module: feature-demo)

本文件用于说明功能模块 **feature-demo** 的契约驱动测试（CDC, Contract Driven Check）入口约定。

- 模块类型：功能模块（Feature Module）
- 适配模板：Core-Templates（home/detail 双路由模板）
- 适配 UI 包：ui-pack-neumorph
- 适配 SST：ndjc-sst-v1@1.0.0

## 目标

- 确认模块提供的能力（`feature.home` / `feature.detail`）在目标模板中可达。
- 确认模块挂载到的 slots（`header` / `primary` / `detail` / `tabBar`）在运行时均有 UI 包实现。
- 确认模块依赖的 tokens 均在 SST + UI 包中定义。

## 建议 CDC 场景

1. Home 页面：
   - 正常渲染 header（标题 / 操作区）。
   - primary 区展示主内容。
   - tabBar 中存在该模块对应的入口 Tab。

2. Detail 页面：
   - 能通过路由参数 `id` 正确进入。
   - header 展示与当前详情相关的标题。
   - detail 区渲染详情内容。

> 实际 CDC 实现可以在本模块的 `test/` 目录中完成；此文件仅描述契约级别的自检范围。
