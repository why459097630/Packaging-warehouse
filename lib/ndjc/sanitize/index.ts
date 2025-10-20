/**
 * Sanitizer：把 plan 归位到顶层五类锚点 + 白名单过滤 + 统计
 *
 * 重要变更（按你的要求）：
 *   - ❌ 取消所有“改键名/去前缀/别名改写”的步骤
 *   - ✅ 键名保持不变（包含 NDJC:/BLOCK:/LIST:/IF:/HOOK: 等前缀）
 *   - ✅ 白名单按原样（带前缀）匹配；aliases 仅扩充允许集，不做改名
 *
 * - 输入：02_plan.json（或任意路径）
 * - 输出：02_plan.sanitized.json；若 NDJC_SANITIZE_OVERWRITE=1 则同时覆盖 02_plan.json
 * - 环境变量：
 *    PLAN_JSON         指定输入文件（默认 requests/<RUN_ID>/02_plan.json）
 *    RUN_ID            用于默认路径
 *    REGISTRY_FILE     指定锚点注册表（默认 lib/ndjc/anchors/registry.circle-basic.json）
 *    NDJC_SANITIZE_OVERWRITE=1  覆盖原 02_plan.json
 */

import fs from "node:fs/promises";
import path from "node:path";

type Registry = {
  template: string;
  text: string[];
  block: string[];
  list: string[];
  if: string[];
  hook: string[];
  resources?: string[];
  aliases?: Record<string, string>;
};

type Plan = any;

function arr<T>(x: T | T[] | undefined | null): T[] {
  if (x == null) return [];
  return Array.isArray(x) ? x : [x];
}
function uniq<T>(xs: T[]): T[] {
  return Array.from(new Set(xs));
}
function isObj(x: any): x is Record<string, any> {
  return x && typeof x === "object" && !Array.isArray(x);
}

async function loadJson(p: string) {
  const txt = await fs.readFile(p, "utf8");
  return JSON.parse(txt);
}

async function saveJson(p: string, obj: any) {
  await fs.mkdir(path.dirname(p), { recursive: true });
  await fs.writeFile(p, JSON.stringify(obj, null, 2), "utf8");
}

/** 解析 aliases 目标，例如 "LIST:ROUTES" -> { kind:'list', name:'LIST:ROUTES' }（不改名） */
function parseAliasedTarget(
  s: string
): { kind: "block" | "list" | "if" | "hook" | null; name: string } {
  const m = s.match(/^(BLOCK|LIST|IF|HOOK):/i);
  if (!m) return { kind: null, name: s };
  const kind = m[1].toLowerCase() as "block" | "list" | "if" | "hook";
  return { kind, name: s }; // 保留原样（含前缀）
}

async function loadRegistry(): Promise<Registry | null> {
  const root = process.cwd();
  const hint =
    process.env.REGISTRY_FILE ||
    path.join(root, "lib/ndjc/anchors/registry.circle-basic.json");
  try {
    const j = await loadJson(hint);
    return j as Registry;
  } catch {
    return null;
  }
}

/** 列表项统一转字符串：字符串/数字/布尔转 String；对象/数组 JSON.stringify */
function toStringItem(x: any): string {
  if (x == null) return "";
  const t = typeof x;
  if (t === "string" || t === "number" || t === "boolean") return String(x);
  try {
    return JSON.stringify(x);
  } catch {
    return String(x);
  }
}

/**
 * 归位为 v1 分组的平面结构（不改键名）
 * - 保留 anchors (NDJC:*), blocks (BLOCK:*), lists (LIST:*), conditions (IF:*), hooks (HOOK:*)
 */
function liftV1Grouped(plan: Plan) {
  const out = {
    template_key: plan.template_key || plan.template || "circle-basic",
    anchors: {} as Record<string, string>,
    blocks: {} as Record<string, string>,
    lists: {} as Record<string, string[]>,
    conditions: {} as Record<string, boolean>,
    hooks: {} as Record<string, string>,
    resources: plan.resources || {},
    companions: plan.companions || [],
    gradle: plan.gradle || {},
    meta: plan.meta || plan.metadata || {}
  };

  // 扁平已有的先拷贝 —— 不改 key
  if (isObj(plan.anchors)) {
    for (const [k, v] of Object.entries(plan.anchors)) {
      if (typeof v === "string" || typeof v === "number" || typeof v === "boolean") {
        out.anchors[k] = String(v);
      }
    }
  }
  if (isObj(plan.blocks)) {
    for (const [k, v] of Object.entries(plan.blocks)) {
      out.blocks[k] = String(v ?? "");
    }
  }
  if (isObj(plan.lists)) {
    for (const [k, v] of Object.entries(plan.lists)) {
      const items = Array.isArray(v) ? v.map(toStringItem).filter(Boolean) : [toStringItem(v)].filter(Boolean);
      if (items.length) out.lists[k] = items;
    }
  }
  if (isObj(plan.conditions)) {
    for (const [k, v] of Object.entries(plan.conditions)) {
      out.conditions[k] = !!v;
    }
  }
  if (isObj(plan.hooks)) {
    for (const [k, v] of Object.entries(plan.hooks)) {
      out.hooks[k] = String(v ?? "");
    }
  }

  // v1 分组归位 —— 不改 key
  const g = (plan.anchorsGrouped || plan.anchors || {}) as any;
  if (isObj(g.text)) {
    for (const [k, v] of Object.entries(g.text)) out.anchors[k] = String(v ?? "");
  }
  if (isObj(g.block)) {
    for (const [k, v] of Object.entries(g.block)) out.blocks[k] = String(v ?? "");
  }
  if (isObj(g.list)) {
    for (const [k, v] of Object.entries(g.list)) {
      const items = Array.isArray(v) ? v.map(toStringItem).filter(Boolean) : [toStringItem(v)].filter(Boolean);
      if (items.length) out.lists[k] = items;
    }
  }
  if (isObj(g.if)) {
    for (const [k, v] of Object.entries(g.if)) out.conditions[k] = !!v;
  }
  if (isObj(g.hook)) {
    for (const [k, v] of Object.entries(g.hook)) out.hooks[k] = String(v ?? "");
  }

  // gradle.applicationId → NDJC:PACKAGE_NAME 兜底（不影响键名）
  const appId =
    out.gradle?.applicationId ||
    plan.gradle?.applicationId ||
    out.meta?.packageId ||
    plan.meta?.packageId ||
    plan.metadata?.packageId;
  if (appId && !out.anchors["NDJC:PACKAGE_NAME"]) {
    out.anchors["NDJC:PACKAGE_NAME"] = String(appId);
  }

  // 常用兜底
  if (!out.lists["LIST:ROUTES"] && !out.lists["ROUTES"]) {
    // 兼容历史：若两者都没有，写入带前缀版本
    out.lists["LIST:ROUTES"] = ["home"];
  }
  if (!out.anchors["NDJC:PRIMARY_BUTTON_TEXT"]) out.anchors["NDJC:PRIMARY_BUTTON_TEXT"] = "Start";

  return out;
}

/** 扩展白名单：按原样（带前缀）比对；aliases 只扩充允许集，不做改名 */
function expandWhitelistWithAliases(reg: Registry) {
  const allow = {
    text: [...(reg.text || [])],
    block: [...(reg.block || [])],
    list: [...(reg.list || [])],
    if: [...(reg.if || [])],
    hook: [...(reg.hook || [])]
  };

  if (reg.aliases) {
    for (const [alias, target] of Object.entries(reg.aliases)) {
      const { kind, name } = parseAliasedTarget(target);
      if (!kind) continue;
      switch (kind) {
        case "block":
          allow.block.push(alias, name);
          break;
        case "list":
          allow.list.push(alias, name);
          break;
        case "if":
          allow.if.push(alias, name);
          break;
        case "hook":
          allow.hook.push(alias, name);
          break;
      }
    }
    allow.text = uniq(allow.text);
    allow.block = uniq(allow.block);
    allow.list = uniq(allow.list);
    allow.if = uniq(allow.if);
    allow.hook = uniq(allow.hook);
  }
  return allow;
}

function applyRegistryWhitelist(
  flat: ReturnType<typeof liftV1Grouped>,
  reg: Registry | null
) {
  if (!reg) {
    return {
      sanitized: flat,
      dropped: { anchors: 0, blocks: 0, lists: 0, conditions: 0, hooks: 0 }
    };
  }
  const allow = expandWhitelistWithAliases(reg);

  const keep = <T extends string>(m: Record<string, any>, allowList: T[]) => {
    const out: Record<string, any> = {};
    let drop = 0;
    for (const [k, v] of Object.entries(m)) {
      if (allowList.includes(k)) out[k] = v;
      else drop++;
    }
    return { out, drop };
  };

  const a = keep(flat.anchors, allow.text);   // NDJC:* 文本锚点按原样比对
  const b = keep(flat.blocks, allow.block);   // BLOCK:* 原样比对
  const l = keep(flat.lists, allow.list);     // LIST:* 原样比对
  const c = keep(flat.conditions, allow.if);  // IF:* 原样比对
  const h = keep(flat.hooks, allow.hook);     // HOOK:* 原样比对

  return {
    sanitized: {
      ...flat,
      anchors: a.out,
      blocks: b.out,
      lists: l.out,
      conditions: c.out,
      hooks: h.out
    },
    dropped: {
      anchors: a.drop,
      blocks: b.drop,
      lists: l.drop,
      conditions: c.drop,
      hooks: h.drop
    }
  };
}

/** 额外安全过滤：仅允许 TEXT 键使用 NDJC: 前缀，防止把源码里的 `R.` 等符号替空 */
function filterDangerousTextKeys(
  sanitized: ReturnType<typeof liftV1Grouped>
) {
  const anchors = sanitized.anchors || {};
  const kept: Record<string, string> = {};
  let droppedDanger = 0;

  for (const [k, v] of Object.entries(anchors)) {
    if (k.startsWith("NDJC:")) {
      kept[k] = v;
    } else {
      droppedDanger++;
    }
  }
  return { ...sanitized, anchors: kept, droppedDanger };
}

export async function sanitizePlanFile() {
  const runId = process.env.RUN_ID;
  const inPath =
    process.env.PLAN_JSON ||
    (runId ? path.join("requests", runId, "02_plan.json") : "");
  if (!inPath) {
    throw new Error("PLAN_JSON or RUN_ID required");
  }
  const reg = await loadRegistry();

  const plan = await loadJson(inPath);
  const flat = liftV1Grouped(plan);
  const { sanitized: whitelisted, dropped } = applyRegistryWhitelist(flat, reg);

  // 对 TEXT 锚点做 NDJC: 白名单过滤，屏蔽危险键（如 R. / R.string）
  const afterSafe = filterDangerousTextKeys(whitelisted);

  const outPath = inPath.replace(/02_plan\.json$/, "02_plan.sanitized.json");
  await saveJson(outPath, afterSafe);

  const overwrite = (process.env.NDJC_SANITIZE_OVERWRITE || "0").trim() === "1";
  if (overwrite) {
    await saveJson(inPath, afterSafe);
  }

  // 统计打印（供 CI grep）
  const counts = {
    anchors: Object.keys(afterSafe.anchors || {}).length,
    blocks: Object.keys(afterSafe.blocks || {}).length,
    lists: Object.keys(afterSafe.lists || {}).length,
    conditions: Object.keys(afterSafe.conditions || {}).length,
    hooks: Object.keys(afterSafe.hooks || {}).length
  };
  console.log(
    `NDJC sanitize: anchors=${counts.anchors} blocks=${counts.blocks} lists=${counts.lists} if=${counts.conditions} hooks=${counts.hooks} dropped=${JSON.stringify(
      dropped
    )} dropped_danger_text=${afterSafe.droppedDanger || 0}`
  );

  // 空计划可选择直接失败
  const allEmpty =
    counts.anchors + counts.blocks + counts.lists + counts.conditions + counts.hooks === 0;
  if (allEmpty && (process.env.NDJC_SANITIZE_FAIL_ON_EMPTY || "0") === "1") {
    throw new Error("Sanitized plan is empty (no anchors/blocks/lists/conditions/hooks).");
  }

  return { inPath, outPath, counts, dropped, droppedDanger: afterSafe.droppedDanger || 0 };
}

// 允许作为脚本运行：node dist/lib/ndjc/sanitize/index.js
if (require.main === module) {
  sanitizePlanFile().catch((e) => {
    console.error(e?.stack || e?.message || String(e));
    process.exit(1);
  });
}
