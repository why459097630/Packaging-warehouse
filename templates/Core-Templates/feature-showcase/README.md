# feature-restaurant-menu (AppCore + 契约骨架)

这是 NDJC 的 **餐厅菜单整套逻辑模块（无 UI AppCore）**，包含：

- 契约 / 对齐文件：
  - `feature-restaurant-menu.ndjc.json`
  - `module.manifest.json`
- 逻辑层 Kotlin：
  - `model/` 菜品数据结构
  - `data/` 仓库接口 + 内存实现（可替换为 Room/local-db）
  - `state/` 列表 / 详情 / 管理状态
  - `core/RestaurantMenuAppCore` 统一封装所有操作逻辑
  - `navigation/RestaurantRoutes` 与模板 routeId 对齐

❗ 注意：
- 这里只提供 **逻辑 + 契约骨架**，不包含 Compose UI。
- UI 由模板 / UI 包根据 state + 事件再接上。
- 需要在你的 Core-Templates 仓库里：
  - 配好 Gradle（参考其他 feature module 的 build.gradle.kts）
  - 在 ModuleRegistry / ModuleScreenRegistry 里注册这些 routeId。
