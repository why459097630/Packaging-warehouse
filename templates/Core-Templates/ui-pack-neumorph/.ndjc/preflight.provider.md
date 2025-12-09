\# Preflight (UI Pack Provider Checks)



在发布/构建前，UI 包必须通过以下自检：



---



\## 1. Public Surface 对齐

所有组件（AppBar / NavBar / Button / Card / ListItem / FormField / Chip / Banner / Empty / Skeleton / BottomSheet / Toast）

的签名、字段名称、默认值均必须与 SST 的 `publicSurface.components` 保持一致；不得新增或缺失字段。



---



\## 2. Tokens 完整性

UI 包提供的 TokenSet 必须覆盖 SST 所有 required domains \& keys：

来自 `sst.tokens.requiredTokens`，例如：



\- color.brand.primary

\- ...

\- opacity.high



---



\## 3. 资源键命名规范

资源键必须符合 SST 规则：  

\- strings: `ui.\* | feature.\*`  

\- media: `img.\* | icon.\* | font.\*`  

不得出现重复或冲突。



---



\## 4. 可访问性（A11y）

所有核心组件（按钮/导航/表单）必须具备语义与焦点可达。



---



\## 说明

任何未通过上述自检项的 UI 包不得发布。



