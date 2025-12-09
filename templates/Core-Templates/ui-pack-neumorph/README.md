# ui-pack-m3-standard (Contract-UI v1)

## 引入步骤
1) 将本模块放到仓库（推荐：`/ui-packs/ui-pack-m3-standard`）。
2) `settings.gradle.kts`：
```
include(":ui-pack-m3-standard")
project(":ui-pack-m3-standard").projectDir = file("ui-packs/ui-pack-m3-standard")
```
3) `app/build.gradle.kts`：
```
implementation(project(":ui-pack-m3-standard"))
```
4) `MainActivity.kt` 导入：
```kotlin
import com.ndjc.ui.m3.NDJCThemePack
import com.ndjc.ui.m3.components.NDJCTabBar
import com.ndjc.ui.m3.components.NavItem
```
5) 通过 `assembly.json` 选择 UI 包 id。