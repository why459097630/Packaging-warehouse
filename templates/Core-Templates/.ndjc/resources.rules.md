# Resources Rules (命名空间 & 冲突顺序)

来自 `sst.resources` 的资源命名规则约定。

---
## 1. Namespaces

### strings
- pattern: `ui.* | feature.*`

### media
- pattern: `img.* | icon.* | font.*`

## 2. Examples

### string
- `ui.common.ok`
- `ui.empty.title`
- `feature.feed.title`

### media
- `img.logo.primary`
- `icon.action.add`

## 3. Conflict Resolution Order

1. template
2. uiPack
3. featureModule

---
## 说明
- 冲突顺序用于当同一个资源 key 在模板 / UI / 模块多处声明时的决策顺序。
