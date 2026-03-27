# Routes Provided Map（UI Pack）

本文件由 `sst.routing.schema` 自动生成，用于声明 UI Pack 可承载的路由与基础导航能力。

---

## Runtime Abilities

- `navigate(routeId,params)`
- `openSheet(sheetId,args)`
- `back()`

## Routes

### route: home
- id: home
- params: (none)
- entry: true

### route: detail
- id: detail
- params: `id:string`
- entry: false


## Deep Links

- `app://detail/{id}`
