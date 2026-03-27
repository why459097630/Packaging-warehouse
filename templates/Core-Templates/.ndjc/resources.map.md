# Resources Map（Template）

来自 `sst.resources` 的模板资源映射声明，用于资源解析与对齐检查。

---

## Namespaces

### namespace: strings
- pattern: `ui.* | feature.*`
- examples: `ui.common.ok`, `ui.empty.title`, `feature.feed.title`

### namespace: media
- pattern: `img.* | icon.* | font.*`
- examples: `img.logo.primary`, `icon.action.add`


## Conflict Resolution Order

1. template
2. uiPack
3. featureModule
