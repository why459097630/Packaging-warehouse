/**
 * Sanitizer：把 plan 归位到顶层五类锚点 + 白名单过滤 + 统计
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

function liftV1Grouped(plan: Plan) {
  // 支持两种输入：
  //  a) v1 分组：plan.anchors.{text,block,list,if,hook}
  //  b) 扁平：plan.anchors / plan.blocks / plan.lists / plan.conditions / plan.hooks
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

  // 扁平已有的先拷贝
  if (isObj(plan.anchors)) {
    for (const [k, v] of Object.entries(plan.anchors)) {
      if (typeof v === "string" || typeof v === "number" || typeof v === "boolean") {
        out.anchors[k] = String(v);
      }
    }
  }
  if (isObj(plan.blocks)) {
    for (const [k, v] of Object.entries(plan.blocks)) out.blocks[k] = String(v ?? "");
  }
  if (isObj(plan.lists)) {
    for (const [k, v] of Object.entries(plan.lists)) {
      const arrv = Array.isArray(v) ? v.map(String).filter(Boolean) : [String(v ?? "")].filter(Boolean);
      if (arrv.length) out.lists[k] = arrv;
    }
  }
  if (isObj(plan.conditions)) {
    for (const [k, v] of Object.entries(plan.conditions)) out.conditions[k] = !!v;
  }
  if (isObj(plan.hooks)) {
    for (const [k, v] of Object.entries(plan.hooks)) out.hooks[k] = String(v ?? "");
  }

  // v1 分组归位
  const g = (plan.anchorsGrouped || plan.anchors || {}) as any;
  if (isObj(g.text)) {
    for (const [k, v] of Object.entries(g.text)) out.anchors[k] = String(v ?? "");
  }
  if (isObj(g.block)) {
    for (const [k, v] of Object.entries(g.block)) out.blocks[k] = String(v ?? "");
  }
  if (isObj(g.list)) {
    for (const [k, v] of Object.entries(g.list)) {
      const arrv = Array.isArray(v) ? v.map(String).filter(Boolean) : [String(v ?? "")].filter(Boolean);
      if (arrv.length) out.lists[k] = arrv;
    }
  }
  if (isObj(g.if)) {
    for (const [k, v] of Object.entries(g.if)) out.conditions[k] = !!v;
  }
  if (isObj(g.hook)) {
    for (const [k, v] of Object.entries(g.hook)) out.hooks[k] = String(v ?? "");
  }

  // gradle.applicationId → NDJC:PACKAGE_NAME 兜底
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
  if (!out.lists["LIST:ROUTES"]) out.lists["LIST:ROUTES"] = ["home"];
  if (!out.anchors["NDJC:PRIMARY_BUTTON_TEXT"]) out.anchors["NDJC:PRIMARY_BUTTON_TEXT"] = "Start";

  return out;
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
  const keep = <T extends string>(m: Record<string, any>, allow: T[]) => {
    const out: Record<string, any> = {};
    let drop = 0;
    for (const [k, v] of Object.entries(m)) {
      if (allow.includes(k)) out[k] = v;
      else drop++;
    }
    return { out, drop };
  };

  const a = keep(flat.anchors, reg.text);
  const b = keep(flat.blocks, reg.block);
  // list 允许“列表名匹配”，不校验每个元素
  const l = keep(flat.lists, reg.list);
  const c = keep(flat.conditions, reg.if);
  const h = keep(flat.hooks, reg.hook);

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

/** 额外安全过滤：仅允许 TEXT 锚点使用 NDJC: 前缀，防止把源码里的 `R.` 等符号替空 */
function filterDangerousTextKeys(
  sanitized: ReturnType<typeof liftV1Grouped>
) {
  const anchors = sanitized.anchors || {};
  const kept: Record<string, string> = {};
  let droppedDanger = 0;

  for (const [k, v] of Object.entries(anchors)) {
    // 仅保留 NDJC: 前缀（模板中的文本锚点均为该前缀）
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

  // 关键新增：对 TEXT 锚点做 NDJC: 白名单过滤，屏蔽危险键（如 R. / R.string）
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
