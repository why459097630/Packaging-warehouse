// scripts/ndjc-sst-checker.js
// NDJC SST checker (Node.js version)
//
// 覆盖：
//  1) Capabilities / Tokens / Slots / Routing / Resources / sst.lock
//  2) 软线：基于 sst.json 的能力定义 / 预检策略 / Token 命名规范 / CDC / 弃用策略 对齐情况

const fs = require("fs");
const path = require("path");

/* ---------------- 配置区：根据 NDJC 实际命名可自行调整 ---------------- */

const FILE_NAMES = {
  template: {
    capabilities: "required.capabilities.md",
    slots: "slots.map.md",
    tokens: "tokens.requirements.md",
    routes: "routes.map.md",               // 模板声明的路由清单（可选）
    resources: "resources.map.md",         // 模板声明的资源锚点清单（可选）
    sstLock: "sst.lock.json",

    // 软线 / 规划项
    preflightChecklist: "preflight.checklist.md",
    routingSchema: "routing.schema.md",
    resourcesRules: "resources.rules.md",
    cdcCases: "cdc.cases.md",
    deprecationAudit: "deprecation.audit.md",
  },
  ui: {
    capabilities: "provided.capabilities.md",
    tokens: "theme.tokens.api.md",
    routes: "routes.provided.map.md",      // 如果 UI 也声明路由，可用
    resources: "resources.provided.map.md",
    sstLock: "sst.lock.json",

    // 软线 / 规划项
    preflightProvider: "preflight.provider.md",
    publicSurfaceFreeze: "public-surface.freeze.md",
    tokensConformance: "tokens.keys.conformance.md",
    cdcRunner: "cdc.runner.md",
    deprecationPolicy: "deprecation.policy.md",
    goldenSnapshots: "golden.snapshots.md",
  },
  module: {
    capabilities: "provided.capabilities.md",
    tokens: "theme.tokens.api.md",
    slotsUsage: "slots.usage.map.md",        // 模块实际使用的 slot 列表
    routesProvided: "routes.provided.map.md",// 模块提供的路由 / Screen 名称
    resourcesUsage: "resources.usage.map.md",// 模块使用/提供的资源锚点
    sstLock: "sst.lock.json",

    // 软线 / 规划项（可选）
    preflightProvider: "preflight.provider.md",
    cdcRunner: "cdc.runner.md",
  },
};

/* ---------------- 参数解析 ---------------- */

const args = process.argv.slice(2);
let mode = "--warn";

// 按你目前项目的默认路径写死（可通过参数覆盖）
let templateDir = "E:/NDJC/Packaging-warehouse/templates/Core-Templates/.ndjc";
let uiDir       = "E:/NDJC/Packaging-warehouse/templates/Core-Templates/ui-pack-neumorph/.ndjc";
let moduleDir   = "E:/NDJC/Packaging-warehouse/templates/Core-Templates/feature-demo/.ndjc";

// sst.json 默认放在 scripts 旁边（同目录）
let sstPath     = path.join(__dirname, "sst.json");

for (let i = 0; i < args.length; i++) {
  const a = args[i];
  if (a === "--strict" || a === "--warn") mode = a;
  if (a === "--template" && args[i + 1]) templateDir = args[++i];
  if (a === "--ui" && args[i + 1])       uiDir       = args[++i];
  if (a === "--module" && args[i + 1])   moduleDir   = args[++i];
  if (a === "--sst" && args[i + 1])      sstPath     = args[++i];
}

let ErrorCount = 0;
let WarnCount  = 0;

/* ---------------- 工具函数 ---------------- */

function readFileSafe(p) {
  try {
    return fs.readFileSync(p, "utf8");
  } catch {
    return null;
  }
}

/**
 * 读取 markdown 列表文件：
 * - 跳过空行、# 注释、``` 代码块标记
 * - 支持 "- item" / "* item" / "item1, item2" 这种写法
 * - 返回去重 + 排序后的字符串数组
 */
function readMdList(p, label) {
  const txt = readFileSafe(p);
  if (txt === null) {
    WarnCount++;
    console.log(`WARN - ${label || "markdown list"} file not found: ${p}`);
    return [];
  }

  const items = [];
  const lines = txt.split(/\r?\n/);

  for (let raw of lines) {
    let line = raw.trim();
    if (!line) continue;
    if (line.startsWith("#")) continue;
    if (line.startsWith("```")) continue;

    let payload = line;
    if (line.startsWith("-") || line.startsWith("*")) {
      payload = line.slice(1).trim();
    }

    const parts = payload.split(",");
    for (let part of parts) {
      const tok = part.trim();
      if (tok) items.push(tok);
    }
  }

  return Array.from(new Set(items)).sort();
}

function readJsonFile(p, label) {
  const txt = readFileSafe(p);
  if (txt === null) {
    WarnCount++;
    console.log(`WARN - ${label || "JSON"} file not found: ${p}`);
    return null;
  }
  try {
    return JSON.parse(txt);
  } catch (e) {
    ErrorCount++;
    console.log(`ERROR - failed to parse JSON: ${p}`);
    console.log(String(e));
    return null;
  }
}

function readTextNonEmpty(p, label) {
  const txt = readFileSafe(p);
  if (txt === null) {
    WarnCount++;
    console.log(`WARN - ${label || "text"} file not found: ${p}`);
    return null;
  }
  const trimmed = txt.trim();
  if (!trimmed) {
    WarnCount++;
    console.log(`WARN - ${label || "text"} file is empty: ${p}`);
    return null;
  }
  return trimmed;
}

/* ---------------- 读取 SST 规范（sst.json） ---------------- */

console.log("NDJC SST checker (Node.js version)");
console.log(`Mode           : ${mode}`);
console.log(`TemplateNdjcDir: ${templateDir}`);
console.log(`UiNdjcDir      : ${uiDir}`);
console.log(`ModuleNdjcDir  : ${moduleDir}`);
console.log(`SST Spec       : ${sstPath}`);
console.log("");

const SstSpec = readJsonFile(sstPath, "SST spec (sst.json)");

if (!SstSpec) {
  console.log("WARN - SST spec not loaded; only基础对齐检查可用");
}

/* ---------------- 1) Template (.ndjc) ---------------- */

console.log("---- 1) Template (.ndjc) ----");
let TemplateInfo = null;

if (fs.existsSync(templateDir)) {
  console.log(`INFO - template dir exists: ${templateDir}`);

  const tplCaps = readMdList(
    path.join(templateDir, FILE_NAMES.template.capabilities),
    "template capabilities"
  );
  const tplSlots = readMdList(
    path.join(templateDir, FILE_NAMES.template.slots),
    "template slots"
  );
  const tplTokens = readMdList(
    path.join(templateDir, FILE_NAMES.template.tokens),
    "template token requirements"
  );
  const tplRoutes = readMdList(
    path.join(templateDir, FILE_NAMES.template.routes),
    "template routes"
  );
  const tplResources = readMdList(
    path.join(templateDir, FILE_NAMES.template.resources),
    "template resources"
  );
  const tplLock = readJsonFile(
    path.join(templateDir, FILE_NAMES.template.sstLock),
    "template sst.lock"
  );

  const tplPreflight = readMdList(
    path.join(templateDir, FILE_NAMES.template.preflightChecklist),
    "template preflight.checklist"
  );
  const tplRoutingSchema = readTextNonEmpty(
    path.join(templateDir, FILE_NAMES.template.routingSchema),
    "template routing.schema"
  );
  const tplResourcesRules = readTextNonEmpty(
    path.join(templateDir, FILE_NAMES.template.resourcesRules),
    "template resources.rules"
  );
  const tplCdcCases = readMdList(
    path.join(templateDir, FILE_NAMES.template.cdcCases),
    "template cdc.cases"
  );
  const tplDeprecationAudit = readTextNonEmpty(
    path.join(templateDir, FILE_NAMES.template.deprecationAudit),
    "template deprecation.audit"
  );

  console.log(`INFO - template capabilities count : ${tplCaps.length}`);
  console.log(`INFO - template slots count        : ${tplSlots.length}`);
  console.log(`INFO - template token requirements : ${tplTokens.length}`);
  if (tplRoutes.length) {
    console.log(`INFO - template routes count       : ${tplRoutes.length}`);
  }
  if (tplResources.length) {
    console.log(`INFO - template resources count    : ${tplResources.length}`);
  }
  if (tplPreflight.length) {
    console.log(`INFO - template preflight checks   : ${tplPreflight.length}`);
  }

  if (tplLock) {
    console.log(
      `INFO - template sst.lock version: ${tplLock.version}, sstId: ${tplLock.sstId}`
    );
  } else {
    WarnCount++;
    console.log("WARN - template sst.lock.json missing or invalid");
  }

  TemplateInfo = {
    caps: tplCaps,
    slots: tplSlots,
    tokens: tplTokens,
    routes: tplRoutes,
    resources: tplResources,
    lock: tplLock,

    preflight: tplPreflight,
    routingSchema: tplRoutingSchema,
    resourcesRules: tplResourcesRules,
    cdcCases: tplCdcCases,
    deprecationAudit: tplDeprecationAudit,
  };
} else {
  ErrorCount++;
  console.log(`ERROR - template .ndjc dir NOT found: ${templateDir}`);
}

console.log("");

/* ---------------- 2) UI pack (.ndjc) ---------------- */

console.log("---- 2) UI pack (.ndjc) ----");
let UiInfo = null;

if (fs.existsSync(uiDir)) {
  console.log(`INFO - UI dir exists: ${uiDir}`);

  const uiCaps = readMdList(
    path.join(uiDir, FILE_NAMES.ui.capabilities),
    "UI provided capabilities"
  );
  const uiTokens = readMdList(
    path.join(uiDir, FILE_NAMES.ui.tokens),
    "UI token api"
  );
  const uiRoutes = readMdList(
    path.join(uiDir, FILE_NAMES.ui.routes),
    "UI routes"
  );
  const uiResources = readMdList(
    path.join(uiDir, FILE_NAMES.ui.resources),
    "UI resources"
  );
  const uiLock = readJsonFile(
    path.join(uiDir, FILE_NAMES.ui.sstLock),
    "UI sst.lock"
  );

  const uiPreflightProvider = readMdList(
    path.join(uiDir, FILE_NAMES.ui.preflightProvider),
    "UI preflight.provider"
  );
  const uiPublicSurfaceFreeze = readTextNonEmpty(
    path.join(uiDir, FILE_NAMES.ui.publicSurfaceFreeze),
    "UI public-surface.freeze"
  );
  const uiTokensConformance = readTextNonEmpty(
    path.join(uiDir, FILE_NAMES.ui.tokensConformance),
    "UI tokens.keys.conformance"
  );
  const uiCdcRunner = readTextNonEmpty(
    path.join(uiDir, FILE_NAMES.ui.cdcRunner),
    "UI cdc.runner"
  );
  const uiDeprecationPolicy = readTextNonEmpty(
    path.join(uiDir, FILE_NAMES.ui.deprecationPolicy),
    "UI deprecation.policy"
  );
  const uiGoldenSnapshots = readTextNonEmpty(
    path.join(uiDir, FILE_NAMES.ui.goldenSnapshots),
    "UI golden.snapshots"
  );

  console.log(`INFO - UI provided capabilities : ${uiCaps.length}`);
  console.log(`INFO - UI token api count       : ${uiTokens.length}`);
  if (uiRoutes.length) {
    console.log(`INFO - UI routes count          : ${uiRoutes.length}`);
  }
  if (uiResources.length) {
    console.log(`INFO - UI resources count       : ${uiResources.length}`);
  }
  if (uiPreflightProvider.length) {
    console.log(`INFO - UI preflight providers   : ${uiPreflightProvider.length}`);
  }

  if (uiLock) {
    console.log(
      `INFO - UI sst.lock version: ${uiLock.version}, sstId: ${uiLock.sstId}`
    );
  } else {
    WarnCount++;
    console.log("WARN - UI sst.lock.json missing or invalid");
  }

  UiInfo = {
    caps: uiCaps,
    tokens: uiTokens,
    routes: uiRoutes,
    resources: uiResources,
    lock: uiLock,

    preflightProvider: uiPreflightProvider,
    publicSurfaceFreeze: uiPublicSurfaceFreeze,
    tokensConformance: uiTokensConformance,
    cdcRunner: uiCdcRunner,
    deprecationPolicy: uiDeprecationPolicy,
    goldenSnapshots: uiGoldenSnapshots,
  };
} else {
  ErrorCount++;
  console.log(`ERROR - UI .ndjc dir NOT found: ${uiDir}`);
}

console.log("");

/* ---------------- 3) Module (.ndjc) ---------------- */

console.log("---- 3) Module (.ndjc) ----");
let ModuleInfo = null;

if (fs.existsSync(moduleDir)) {
  console.log(`INFO - module dir exists: ${moduleDir}`);

  const modCaps = readMdList(
    path.join(moduleDir, FILE_NAMES.module.capabilities),
    "module provided capabilities"
  );
  const modTokens = readMdList(
    path.join(moduleDir, FILE_NAMES.module.tokens),
    "module token usage"
  );
  const modSlotsUsage = readMdList(
    path.join(moduleDir, FILE_NAMES.module.slotsUsage),
    "module slots usage"
  );
  const modRoutesProvided = readMdList(
    path.join(moduleDir, FILE_NAMES.module.routesProvided),
    "module routes provided"
  );
  const modResourcesUsage = readMdList(
    path.join(moduleDir, FILE_NAMES.module.resourcesUsage),
    "module resources usage"
  );
  const modLock = readJsonFile(
    path.join(moduleDir, FILE_NAMES.module.sstLock),
    "module sst.lock"
  );

  const modPreflightProvider = readMdList(
    path.join(moduleDir, FILE_NAMES.module.preflightProvider),
    "module preflight.provider"
  );
  const modCdcRunner = readTextNonEmpty(
    path.join(moduleDir, FILE_NAMES.module.cdcRunner),
    "module cdc.runner"
  );

  console.log(`INFO - module provided capabilities : ${modCaps.length}`);
  console.log(`INFO - module token usage (declared) : ${modTokens.length}`);
  if (modSlotsUsage.length) {
    console.log(`INFO - module slots usage count      : ${modSlotsUsage.length}`);
  }
  if (modRoutesProvided.length) {
    console.log(`INFO - module routes provided count  : ${modRoutesProvided.length}`);
  }
  if (modResourcesUsage.length) {
    console.log(`INFO - module resources usage count  : ${modResourcesUsage.length}`);
  }
  if (modPreflightProvider.length) {
    console.log(`INFO - module preflight providers    : ${modPreflightProvider.length}`);
  }

  if (modLock) {
    console.log(
      `INFO - module sst.lock version: ${modLock.version}, sstId: ${modLock.sstId}`
    );
  } else {
    WarnCount++;
    console.log("WARN - module sst.lock.json missing or invalid");
  }

  ModuleInfo = {
    caps: modCaps,
    tokens: modTokens,
    slotsUsage: modSlotsUsage,
    routesProvided: modRoutesProvided,
    resourcesUsage: modResourcesUsage,
    lock: modLock,

    preflightProvider: modPreflightProvider,
    cdcRunner: modCdcRunner,
  };
} else {
  ErrorCount++;
  console.log(`ERROR - module .ndjc dir NOT found: ${moduleDir}`);
}

console.log("");

/* ---------------- 4) Capability alignment (模板要求 vs UI+模块提供) ---------------- */

console.log("---- 4) Capability alignment ----");

if (TemplateInfo && UiInfo && ModuleInfo) {
  const tplCaps = TemplateInfo.caps;
  const uiCaps  = UiInfo.caps || [];
  const modCaps = ModuleInfo.caps || [];

  const providedCaps = Array.from(
    new Set([...uiCaps, ...modCaps])
  ).sort();

  const missing = tplCaps.filter((c) => c && !providedCaps.includes(c));

  if (missing.length) {
    ErrorCount++;
    console.log("ERROR - required capabilities NOT satisfied by UI+module:");
    console.log("  → " + missing.join(", "));
    console.log("");
    console.log("DETAIL - capability presence by file:");
    console.log(`  template file : ${path.join(templateDir, FILE_NAMES.template.capabilities)}`);
    console.log(`  ui-pack file  : ${path.join(uiDir, FILE_NAMES.ui.capabilities)}`);
    console.log(`  module file   : ${path.join(moduleDir, FILE_NAMES.module.capabilities)}`);
    console.log("");

    for (const cap of missing) {
      console.log(`  Capability: ${cap}`);
      console.log(`    template: ${tplCaps.includes(cap) ? "✔ 包含" : "✖ 不包含"}`);
      console.log(`    ui-pack : ${uiCaps.includes(cap)  ? "✔ 包含" : "✖ 不包含"}`);
      console.log(`    module  : ${modCaps.includes(cap) ? "✔ 包含" : "✖ 不包含"}`);
      console.log("");
    }
  } else {
    console.log("INFO - all template capabilities are satisfied by UI + module");
  }
} else {
  WarnCount++;
  console.log("WARN - capability alignment skipped (missing template/UI/module info)");
}

console.log("");

/* ---------------- 5) Token alignment (template vs UI) ---------------- */

console.log("---- 5) Token alignment (template vs UI) ----");

if (TemplateInfo && UiInfo) {
  const required = TemplateInfo.tokens || [];
  const uiTokens = UiInfo.tokens || [];

  const missing = required.filter((t) => t && !uiTokens.includes(t));

  if (missing.length) {
    ErrorCount++;
    console.log("ERROR - token requirements NOT satisfied by UI:");
    console.log("  → " + missing.join(", "));
    console.log("");
    console.log("DETAIL - token presence by file:");
    console.log(`  template file : ${path.join(templateDir, FILE_NAMES.template.tokens)}`);
    console.log(`  ui-pack file  : ${path.join(uiDir, FILE_NAMES.ui.tokens)}`);
    console.log("");

    for (const tok of missing) {
      console.log(`  Token: ${tok}`);
      console.log(`    template: ${required.includes(tok) ? "✔ 包含" : "✖ 不包含"}`);
      console.log(`    ui-pack : ${uiTokens.includes(tok)  ? "✔ 包含" : "✖ 不包含"}`);
      console.log("");
    }
  } else {
    console.log("INFO - all template token requirements are satisfied by UI pack");
  }
} else {
  WarnCount++;
  console.log("WARN - token alignment skipped (missing template/UI info)");
}

console.log("");

/* ---------------- 6) Slots alignment (template vs module usage) ---------------- */

console.log("---- 6) Slots alignment (template vs module) ----");

if (TemplateInfo && ModuleInfo) {
  const tplSlots = TemplateInfo.slots || [];
  const modSlots = ModuleInfo.slotsUsage || [];

  if (!tplSlots.length && !modSlots.length) {
    console.log("INFO - no slot information found on template & module; skipped");
  } else if (!tplSlots.length) {
    WarnCount++;
    console.log("WARN - template has no slots.map.md but module has slots usage");
  } else if (!modSlots.length) {
    WarnCount++;
    console.log("WARN - module has no slots.usage map; slot alignment skipped");
  } else {
    const unknownSlots = modSlots.filter((s) => s && !tplSlots.includes(s));
    const unusedSlots = tplSlots.filter((s) => s && !modSlots.includes(s));

    if (unknownSlots.length) {
      ErrorCount++;
      console.log("ERROR - module uses slots not declared in template:");
      console.log(unknownSlots.join(", "));
    } else {
      console.log("INFO - all module slots are declared in template slots.map.md");
    }

    if (unusedSlots.length) {
      WarnCount++;
      console.log("WARN - template declares slots that are not used by module:");
      console.log(unusedSlots.join(", "));
    }
  }
} else {
  WarnCount++;
  console.log("WARN - slots alignment skipped (missing template/module info)");
}

console.log("");

/* ---------------- 7) Routing alignment (template vs module) ---------------- */

console.log("---- 7) Routing alignment (template vs module) ----");

if (TemplateInfo && ModuleInfo) {
  const tplRoutes = TemplateInfo.routes || [];
  const modRoutes = ModuleInfo.routesProvided || [];

  if (!tplRoutes.length && !modRoutes.length) {
    console.log("INFO - no routing map found on template & module; skipped");
  } else if (!tplRoutes.length) {
    WarnCount++;
    console.log("WARN - template routes.map.md missing but module provides routes");
  } else if (!modRoutes.length) {
    WarnCount++;
    console.log("WARN - module routes.provided.map.md missing; routing alignment skipped");
  } else {
    const unknownRoutes = modRoutes.filter(
      (r) => r && !tplRoutes.includes(r)
    );
    const missingRoutes = tplRoutes.filter(
      (r) => r && !modRoutes.includes(r)
    );

    if (unknownRoutes.length) {
      ErrorCount++;
      console.log("ERROR - module provides routes not declared in template routes:");
      console.log(unknownRoutes.join(", "));
    } else {
      console.log("INFO - all module routes are declared in template routes map");
    }

    if (missingRoutes.length) {
      WarnCount++;
      console.log("WARN - template defines routes without module implementation:");
      console.log(missingRoutes.join(", "));
    }
  }
} else {
  WarnCount++;
  console.log("WARN - routing alignment skipped (missing template/module info)");
}

console.log("");

/* ---------------- 8) Resources alignment (template vs module) ---------------- */

console.log("---- 8) Resources alignment (template vs module) ----");

if (TemplateInfo && ModuleInfo) {
  const tplRes = TemplateInfo.resources || [];
  const modRes = ModuleInfo.resourcesUsage || [];

  if (!tplRes.length && !modRes.length) {
    console.log("INFO - no resources map found on template & module; skipped");
  } else if (!tplRes.length) {
    WarnCount++;
    console.log("WARN - template resources.map.md missing but module uses resources");
  } else if (!modRes.length) {
    WarnCount++;
    console.log("WARN - module resources.usage.map.md missing; resources alignment skipped");
  } else {
    const unknownRes = modRes.filter((r) => r && !tplRes.includes(r));
    const missingRes = tplRes.filter((r) => r && !modRes.includes(r));

    if (unknownRes.length) {
      ErrorCount++;
      console.log("ERROR - module uses resources not declared in template resources:");
      console.log(unknownRes.join(", "));
    } else {
      console.log("INFO - all module resources anchors are declared in template");
    }

    if (missingRes.length) {
      WarnCount++;
      console.log("WARN - template declares resources that module never uses:");
      console.log(missingRes.join(", "));
    }
  }
} else {
  WarnCount++;
  console.log("WARN - resources alignment skipped (missing template/module info)");
}

console.log("");

/* ---------------- 9) SST lock alignment ---------------- */

console.log("---- 9) SST lock alignment ----");

if (
  TemplateInfo &&
  UiInfo &&
  ModuleInfo &&
  TemplateInfo.lock &&
  UiInfo.lock &&
  ModuleInfo.lock
) {
  const tpl = TemplateInfo.lock;
  const ui = UiInfo.lock;
  const mod = ModuleInfo.lock;

  let allOk =
    tpl.version === ui.version &&
    tpl.version === mod.version &&
    tpl.sstId === ui.sstId &&
    tpl.sstId === mod.sstId;

  if (SstSpec && SstSpec.contract && SstSpec.contract.hash) {
    if (tpl.sstId !== SstSpec.contract.hash) {
      allOk = false;
      ErrorCount++;
      console.log(
        `ERROR - template sst.lock.sstId (${tpl.sstId}) does not match sst.json hash (${SstSpec.contract.hash})`
      );
    }
  }

  if (allOk) {
    console.log("INFO - sst.lock aligned:");
    console.log(`  version: ${tpl.version}`);
    console.log(`  sstId  : ${tpl.sstId}`);
  } else {
    ErrorCount++;
    console.log("ERROR - sst.lock mismatch:");
    console.log(`  template=(${tpl.version},${tpl.sstId})`);
    console.log(`  ui      =(${ui.version},${ui.sstId})`);
    console.log(`  module  =(${mod.version},${mod.sstId})`);
  }
} else {
  WarnCount++;
  console.log(
    "WARN - sst.lock alignment skipped (missing template/UI/module lock)"
  );
}

console.log("");

/* ---------------- 10) SST vs Template 能力 / Tokens / 预检 对齐 ---------------- */

console.log("---- 10) SST vs Template contract alignment ----");

if (SstSpec && TemplateInfo) {
  // 10.1 能力定义：SST.required/optional 应该都出现在模板 required.capabilities.md 里
  if (Array.isArray(SstSpec.capabilities && SstSpec.capabilities.required)) {
    const expectedCaps = SstSpec.capabilities.required.concat(
      SstSpec.capabilities.optional || []
    );
    const missingCaps = expectedCaps.filter(
      (c) => c && !TemplateInfo.caps.includes(c)
    );
    if (missingCaps.length) {
      WarnCount++;
      console.log(
        "WARN - template required.capabilities.md does not cover all SST capabilities (required+optional):"
      );
      console.log(missingCaps.join(", "));
    } else {
      console.log("INFO - template capabilities cover SST required+optional");
    }
  }

  // 10.2 Tokens：SST tokens.domains 中 required=true 的 key 必须都在 tokens.requirements.md 里
  if (SstSpec.tokens && SstSpec.tokens.domains) {
    const expectedTokens = [];
    for (const [domain, def] of Object.entries(SstSpec.tokens.domains)) {
      if (!def || !def.required) continue;
      if (Array.isArray(def.keys)) {
        for (const k of def.keys) {
          expectedTokens.push(`${domain}.${k}`);
        }
      }
    }
    const missingTokens = expectedTokens.filter(
      (t) => t && !TemplateInfo.tokens.includes(t)
    );
    if (missingTokens.length) {
      WarnCount++;
      console.log(
        "WARN - template tokens.requirements.md does not cover all SST required token domains/keys:"
      );
      console.log(missingTokens.join(", "));
    } else {
      console.log("INFO - template token requirements cover SST required domains/keys");
    }
  }

  // 10.3 预检策略：SST.preflight.requiredChecks vs template preflight.checklist
  if (
    SstSpec.preflight &&
    Array.isArray(SstSpec.preflight.requiredChecks)
  ) {
    const expectedChecks = SstSpec.preflight.requiredChecks;
    const tplChecks = TemplateInfo.preflight || [];
    if (!tplChecks.length) {
      WarnCount++;
      console.log(
        "WARN - template preflight.checklist.md missing or empty while SST defines requiredChecks"
      );
    } else {
      const missing = expectedChecks.filter(
        (c) => c && !tplChecks.includes(c)
      );
      if (missing.length) {
        ErrorCount++;
        console.log(
          "ERROR - template preflight.checklist does not contain all SST requiredChecks:"
        );
        console.log(missing.join(", "));
      } else {
        console.log("INFO - template preflight.checklist covers SST requiredChecks");
      }
    }
  }
} else {
  WarnCount++;
  console.log("WARN - SST vs template contract alignment skipped (missing SstSpec or TemplateInfo)");
}

console.log("");

/* ---------------- 11) 预检提供方（UI / 模块）对齐 ---------------- */

console.log("---- 11) Preflight providers alignment (UI / Module) ----");

if (SstSpec && (UiInfo || ModuleInfo)) {
  const expectedChecks =
    (SstSpec.preflight && SstSpec.preflight.requiredChecks) || [];
  const uiChecks = (UiInfo && UiInfo.preflightProvider) || [];
  const modChecks = (ModuleInfo && ModuleInfo.preflightProvider) || [];
  const union = Array.from(new Set([...uiChecks, ...modChecks]));

  if (!expectedChecks.length) {
    console.log("INFO - SST has no preflight.requiredChecks; providers check skipped");
  } else if (!union.length) {
    WarnCount++;
    console.log(
      "WARN - neither UI nor Module provide preflight.provider.md entries while SST defines requiredChecks"
    );
  } else {
    const missing = expectedChecks.filter((c) => c && !union.includes(c));
    if (missing.length) {
      WarnCount++;
      console.log(
        "WARN - some SST requiredChecks are not implemented by UI or Module preflight providers:"
      );
      console.log(missing.join(", "));
    } else {
      console.log("INFO - UI+Module preflight.provider cover SST requiredChecks (at least声明层面)");
    }
  }
} else {
  WarnCount++;
  console.log("WARN - preflight providers alignment skipped (missing SstSpec/UI/Module)");
}

console.log("");

/* ---------------- 12) Token 命名规范 / Conformance ---------------- */

console.log("---- 12) Tokens naming & conformance ----");

if (SstSpec && SstSpec.tokens && SstSpec.tokens.rules && UiInfo) {
  const namingRule = SstSpec.tokens.rules.naming || "";
  const uiText = UiInfo.tokensConformance || "";

  if (!namingRule) {
    console.log("INFO - SST.tokens.rules.naming not specified; skip naming check");
  } else if (!uiText) {
    WarnCount++;
    console.log(
      "WARN - UI tokens.keys.conformance.md missing or empty while SST specifies naming rule"
    );
  } else if (!uiText.includes(namingRule)) {
    WarnCount++;
    console.log(
      `WARN - UI tokens.keys.conformance.md does not mention SST naming rule "${namingRule}" explicitly`
    );
  } else {
    console.log(
      `INFO - UI tokens.keys.conformance.md acknowledges SST naming rule "${namingRule}"`
    );
  }
} else {
  WarnCount++;
  console.log("WARN - tokens naming & conformance check skipped (missing SstSpec or UiInfo)");
}

console.log("");

/* ---------------- 13) CDC / Golden Snapshots / 弃用策略 ---------------- */

console.log("---- 13) CDC & deprecation policy alignment ----");

if (SstSpec && TemplateInfo && UiInfo) {
  // 13.1 CDC cases：模板 cdc.cases.md 覆盖 SST.cdc.cases
  if (SstSpec.cdc && Array.isArray(SstSpec.cdc.cases)) {
    const expectedCases = SstSpec.cdc.cases;
    const tplCases = TemplateInfo.cdcCases || [];
    if (!tplCases.length) {
      WarnCount++;
      console.log(
        "WARN - template cdc.cases.md missing or empty while SST defines CDC cases"
      );
    } else {
      const missing = expectedCases.filter((c) => c && !tplCases.includes(c));
      if (missing.length) {
        WarnCount++;
        console.log(
          "WARN - template cdc.cases.md does not cover all SST CDC cases:"
        );
        console.log(missing.join(", "));
      } else {
        console.log("INFO - template CDC cases cover SST CDC cases");
      }
    }
  }

  // 13.2 Golden snapshots：若 SST 定义了 golden.baselineVersion，则 UI golden.snapshots.md 必须存在
  if (SstSpec.cdc && SstSpec.cdc.golden && SstSpec.cdc.golden.baselineVersion) {
    if (!UiInfo.goldenSnapshots) {
      WarnCount++;
      console.log(
        "WARN - UI golden.snapshots.md missing while SST.cdc.golden.baselineVersion is defined"
      );
    } else {
      console.log("INFO - UI golden.snapshots.md exists (CDC golden baseline present)");
    }
  }

  // 13.3 Deprecation policy：只做存在性/开关检查
  if (SstSpec.deprecationPolicy) {
    if (!TemplateInfo.deprecationAudit) {
      WarnCount++;
      console.log(
        "WARN - template deprecation.audit.md missing while SST.deprecationPolicy is defined"
      );
    }
    if (!UiInfo.deprecationPolicy) {
      WarnCount++;
      console.log(
        "WARN - UI deprecation.policy.md missing while SST.deprecationPolicy is defined"
      );
    }

    if (SstSpec.deprecationPolicy.expiredIsError) {
      // 如果规范要求 expired 视为错误，但策略文档里完全没有 "error" / "错误" 字样，就给个轻 warn
      const text = (UiInfo.deprecationPolicy || "") + (TemplateInfo.deprecationAudit || "");
      if (text && !/error|错误/i.test(text)) {
        WarnCount++;
        console.log(
          "WARN - SST.deprecationPolicy.expiredIsError = true, but policy/audit docs do not mention error semantics explicitly"
        );
      } else if (text) {
        console.log(
          "INFO - deprecation policy/audit docs mention error semantics (expired may be treated as error)"
        );
      }
    }
  }
} else {
  WarnCount++;
  console.log("WARN - CDC & deprecation alignment skipped (missing SstSpec / TemplateInfo / UiInfo)");
}

console.log("");
console.log("---- SUMMARY ----");
console.log(`Warnings: ${WarnCount}`);
console.log(`Errors  : ${ErrorCount}`);

if (mode === "--strict" && ErrorCount > 0) {
  console.log("Result: errors found, strict mode -> exit 1");
  process.exit(1);
} else if (ErrorCount > 0) {
  console.log("Result: errors found (non-strict mode)");
  process.exit(0);
} else {
  console.log("Result: all checks passed");
  process.exit(0);
}
