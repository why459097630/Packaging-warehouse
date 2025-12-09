# Preflight Checklist (MUST PASS Before Build)
构建前必须全部通过，否则构建失败。

---

## 1. slots.exist
- slots.exist

模板声明的全部槽位存在且命名正确  
- onFail: ❌ `[Preflight] Slot missing or mismatched`

---

## 2. resources.keysResolvable
- resources.keysResolvable

所有资源 key 可解析，无悬空  
- onFail: ❌ `[Preflight] Resource key unresolved`

---

## 3. tokens.complete
- tokens.complete

满足 `tokens.requirements.md` 中全部必需键  
- onFail: ❌ `[Preflight] Missing required Token keys`

---

## 4. routing.connected
- routing.connected

所有路由可达，entry 可达，deep link 合法  
- onFail: ❌ `[Preflight] Routing not fully connected`

---

## 执行顺序
按上述列出的顺序依次执行，一旦失败立即停止构建。
