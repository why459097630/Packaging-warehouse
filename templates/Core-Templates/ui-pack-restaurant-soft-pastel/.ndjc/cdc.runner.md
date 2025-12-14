# CDC Runner (Consume Template Test Cases)

UI 包发布前必须拉取模板仓的 CDC 用例并全部通过。

建议覆盖：
- AppBar 标题渲染
- Tab 切换事件触发
- Button 四种变体渲染与点击
- 从 home → detail(id) 跳转渲染

> 任一失败不得发布。若由于模板用例变更失败，应先更新实现或在 SST 中进行协同演进。
