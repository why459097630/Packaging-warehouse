# Routing Schema (Template-Level Contract)

定义模板可用的 routeId / params / entry / deep links，来自 `sst.routing`。

---
## 1. Runtime Abilities

- `navigate(routeId,params)`
- `openSheet(sheetId,args)`
- `back()`

## 2. Routes

### Route: `home`
**Params:** (none)

### Route: `detail`
**Params:**
- `id`: `string`

## 3. Deep Links

- pattern: `app://detail/{id}`

## 4. Entry & Return Policy

- entry: `home`
- returnPolicy: `backstack`

---
