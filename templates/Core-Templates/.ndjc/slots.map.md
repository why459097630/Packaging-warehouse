# Slots Mapping (Standardized Slots Used by This Template)

本模板使用的页面插槽，必须与 SST 定义保持一致。

| Slot 名 | 用途描述 | 是否必填 | 备注 |
|--------|----------|---------|------|
| header | 顶部栏（AppBar） | ✅ 必需 | 负责标题、导航、操作 |
| hero | 顶部主展示区 | ✅ 建议 | 可做 Banner 或功能入口 |
| primary | 主内容区 | ✅ 必需 | 页面主体内容 |
| secondary | 辅助内容区 | 可选 | 侧栏或次级信息 |
| detail | 详情内容区 | 可选 | 用于详情页 |
| sheet | BottomSheet 占位位 | 可选 | 弹层内容 |
| tabBar | 底部 Tab 导航 | ✅ 必需（若为多页结构） | NavBar 展现位置 |

允许使用的可选插槽（如需支持需注明）：
- fab / dialog / settings
