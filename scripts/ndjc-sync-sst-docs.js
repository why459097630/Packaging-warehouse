#!/usr/bin/env node
/**
 * NDJC SST → 文档同步脚本（全集版）
 *
 * 统一从 scripts/sst.json 推导出三个 .ndjc 目录下的所有“契约 / 自检说明”文档：
 *
 * 模板侧 Core-Templates/.ndjc
 *   - required.capabilities.md
 *   - tokens.requirements.md
 *   - preflight.checklist.md
 *   - slots.contract.md
 *   - routing.schema.md
 *   - resources.rules.md
 *   - gradle.manifest.md
 *   - compat.matrix.md
 *   - moduleManifest.contract.md
 *   - cdc.cases.md
 *   - observability.md
 *   - deprecation.policy.md
 *
 * UI 包侧 Core-Templates/ui-pack-showcase-greenpink/.ndjc
 *   - theme.tokens.api.md
 *   - provided.capabilities.md
 *   - routes.provided.map.md
 *   - resources.provided.map.md
 *   - sst.lock.json
 *   - preflight.provider.md
 *   - public-surface.freeze.md
 *   - tokens.keys.conformance.md
 *   - cdc.runner.md
 *   - deprecation.policy.md
 *   - golden.snapshots.md
 *
 * 模块侧 Core-Templates/feature-showcase/.ndjc
 *   - provided.capabilities.md
 *   - theme.tokens.api.md
 *   - slots.usage.map.md
 *   - routes.provided.map.md
 *   - resources.usage.map.md
 *   - preflight.provider.md
 *   - sst.lock.json
 *   - cdc.runner.md
 *   - deprecation.policy.md
 *   - golden.snapshots.md
 *   - tokens.keys.conformance.md
 *   - public-surface.freeze.md
 *
 * 说明：
 *   - 所有内容全部从 sst.json 读取，.md / .json 文件视为“下游产物”，不再手写。
 *   - 不会反向写入 sst.json，保持 SST 单一真相（Single Source of Truth）。
 */

const fs = require("fs");
const path = require("path");

/* ---------------- paths ---------------- */

const ROOT = path.resolve(__dirname, "..");

const SST_PATH = path.join(ROOT, "scripts", "sst.json");

// 模板 .ndjc 目录
const TEMPLATE_NDJC_DIR = path.join(
  ROOT,
  "templates",
  "Core-Templates",
  ".ndjc"
);

// UI 包 .ndjc 目录
const UI_NDJC_DIR = path.join(
  ROOT,
  "templates",
  "Core-Templates",
  "ui-pack-showcase-greenpink",
  ".ndjc"
);

// 模块 .ndjc 目录
const MODULE_NDJC_DIR = path.join(
  ROOT,
  "templates",
  "Core-Templates",
  "feature-showcase",
  ".ndjc"
);

/* ------------ small helpers ------------ */

function ensureDir(p) {
  if (!fs.existsSync(p)) {
    fs.mkdirSync(p, { recursive: true });
  }
}

function writeFilePretty(filePath, content) {
  ensureDir(path.dirname(filePath));
  fs.writeFileSync(filePath, content.replace(/\r\n/g, "\n"), "utf8");
  console.log("  ✔ wrote", path.relative(ROOT, filePath));
}

/* ------------ load SST.json ------------ */

if (!fs.existsSync(SST_PATH)) {
  console.error("[ndjc-sync-sst-docs] ERROR: scripts/sst.json 不存在");
  process.exit(1);
}

const sstRaw = fs.readFileSync(SST_PATH, "utf8");
let sst;
try {
  sst = JSON.parse(sstRaw);
} catch (e) {
  console.error("[ndjc-sync-sst-docs] ERROR: sst.json 解析失败:", e.message);
  process.exit(1);
}

/**
 * 兼容几种可能字段名，优先使用你现在的：
 *   - requiredCapabilities
 *   - requiredTokens （对象或数组，或 tokens.requiredTokens）
 *   - requiredChecks  （preflight.requiredChecks）
 */

/** 把各种形态的 requiredTokens 统一成 { domain: string[] } */
function normalizeRequiredTokens(raw) {
  if (!raw) return {};
  // 扁平数组：["color.brand.primary", "space.xs", ...]
  if (Array.isArray(raw)) {
    const map = {};
    for (const item of raw) {
      if (typeof item !== "string") continue;
      const parts = item.split(".");
      if (parts.length < 2) continue;
      const domain = parts[0];
      const key = parts.slice(1).join(".");
      if (!domain || !key) continue;
      if (!map[domain]) map[domain] = [];
      map[domain].push(key);
    }
    // 去重 + 排序
    for (const d of Object.keys(map)) {
      map[d] = Array.from(new Set(map[d])).sort();
    }
    return map;
  }
  // 已经是 { domain: [keys...] } 的对象
  if (typeof raw === "object") {
    return raw;
  }
  return {};
}

function mdList(items, formatter = (v) => `- ${v}`) {
  if (!Array.isArray(items) || items.length === 0) return "";
  return items.map(formatter).join("\n");
}

function renderJsonPretty(obj) {
  return JSON.stringify(obj, null, 2) + "\n";
}

function uniqSorted(items) {
  return Array.from(new Set((items || []).filter(Boolean))).sort();
}

function mdSimpleList(items) {
  const list = uniqSorted(items);
  return list.length ? list.map((v) => `- ${v}`).join("\n") + "\n" : "";
}

function flattenRequiredTokens(domainsMap) {
  const out = [];
  for (const domain of Object.keys(domainsMap || {})) {
    const keys = domainsMap[domain] || [];
    for (const key of keys) {
      out.push(`${domain}.${key}`);
    }
  }
  return uniqSorted(out);
}

function extractRequiredTokensFromDomains(domainsObj) {
  if (!domainsObj || typeof domainsObj !== "object") return {};
  const out = {};
  for (const domain of Object.keys(domainsObj)) {
    const def = domainsObj[domain] || {};
    const required = def.required === true;
    const keys = Array.isArray(def.keys) ? def.keys : [];
    if (required) {
      out[domain] = Array.from(new Set(keys)).sort();
    }
  }
  return out;
}

const capabilities = sst.capabilities || {};

const templateRequiredCapabilities =
  capabilities.required ||
  sst.requiredCapabilities ||
  sst.template?.requiredCapabilities ||
  [];

const templateOptionalCapabilities =
  capabilities.optional ||
  sst.optionalCapabilities ||
  sst.template?.optionalCapabilities ||
  [];

const rawRequiredTokens =
  sst.requiredTokens ||
  sst.required?.tokens ||
  sst.tokens?.required ||
  sst.tokens?.requiredTokens ||
  extractRequiredTokensFromDomains(sst.tokens?.domains) ||
  null;

const requiredTokens = normalizeRequiredTokens(rawRequiredTokens);

const requiredChecks =
  sst.preflight?.requiredChecks ||
  sst.requiredChecks || [
    "slots.exist",
    "resources.keysResolvable",
    "tokens.complete",
    "routing.connected",
    "capabilities.satisfied",
    "sst.contract.basic",
  ];

/* ------------- 1) required.capabilities.md ------------- */

function genRequiredCapabilitiesMd() {
  const required = templateRequiredCapabilities || [];
  return mdSimpleList(required);
}

/* ------------- 2) tokens.requirements.md ------------- */

function genTokensRequirementsMd() {
  return mdSimpleList(flattenRequiredTokens(requiredTokens));
}

/* ------------- 3) theme.tokens.api.md（只改 Tokens 段落） ------------- */

function genUiThemeTokensApiMd() {
  return mdSimpleList(flattenRequiredTokens(requiredTokens));
}

/* ------------- 4) preflight.checklist.md ------------- */

function genPreflightChecklistMd() {
  return mdSimpleList(requiredChecks);
}

/* ------------- 5) slots.contract.md ------------- */

function genSlotsContractMd() {
  const slots = sst.slots || {};
  const standard = slots.standard || [];
  const optional = slots.optional || [];
  const meta = slots.meta || {};
  const overrideOrder = slots.overrideOrder || [];

  const header = `# Slots Contract (Template Layout Slots)

来自 \`sst.slots\` 的插槽约定，模板与 UI / 模块均需遵守。

---
`;

  const stdSection =
    "## 1. Standard Slots（必须实现）\n\n" +
    (standard.length ? standard.map((s) => `- ${s}`).join("\n") : "- (none)") +
    "\n\n";

  const optSection =
    "## 2. Optional Slots（可选实现）\n\n" +
    (optional.length ? optional.map((s) => `- ${s}`).join("\n") : "- (none)") +
    "\n\n";

  const metaParts = [];
  for (const key of Object.keys(meta)) {
    const values = meta[key] || [];
    metaParts.push(`### ${key}`);
    metaParts.push(
      values.length ? values.map((v) => `- ${v}`).join("\n") : "- (none)"
    );
    metaParts.push("");
  }
  const metaSection =
    "## 3. Meta 信息\n\n" +
    (metaParts.length ? metaParts.join("\n") : "- (none)") +
    "\n";

  const overrideSection =
    "## 4. Override 顺序\n\n" +
    (overrideOrder.length
      ? overrideOrder
          .map((s, idx) => `${idx + 1}. ${s}（优先级 ${idx + 1}）`)
          .join("\n")
      : "- (none)") +
    "\n\n---\n" +
    "## 说明\n- 本文件为 slots 单一契约来源的 markdown 视图，仅由 sst.json 生成，不手动修改。\n";

  return header + stdSection + optSection + metaSection + overrideSection;
}
function genTemplateSlotsMapMd() {
  const standard = sst.slots?.standard || [];
  const optional = sst.slots?.optional || [];
  return mdSimpleList([...standard, ...optional]);
}
/* ------------- 6) routing.schema.md ------------- */

function genRoutingSchemaMd() {
  const routing = sst.routing || {};
  const runtimeAbilities = routing.runtimeAbilities || [];
  const schema = routing.schema || {};
  const routes = schema.routes || [];
  const entry = schema.entry || "(none)";
  const deepLinks = schema.deepLinks || [];
  const returnPolicy = schema.returnPolicy || "(unspecified)";

  const header = `# Routing Schema (Template-Level Contract)

定义模板可用的 routeId / params / entry / deep links，来自 \`sst.routing\`。

---
`;

  const abilitiesSection =
    "## 1. Runtime Abilities\n\n" +
    (runtimeAbilities.length
      ? runtimeAbilities.map((a) => `- \`${a}\``).join("\n")
      : "- (none)") +
    "\n\n";

  const routesParts = [];
  for (const r of routes) {
    routesParts.push(`### Route: \`${r.id}\``);
    const params = r.params || {};
    const keys = Object.keys(params);
    if (keys.length) {
      routesParts.push("**Params:**");
      routesParts.push(
        keys.map((k) => `- \`${k}\`: \`${params[k]}\``).join("\n")
      );
    } else {
      routesParts.push("**Params:** (none)");
    }
    routesParts.push("");
  }

  const routesSection =
    "## 2. Routes\n\n" +
    (routesParts.length ? routesParts.join("\n") : "- (none)") +
    "\n";

  const deepLinkParts = deepLinks.map(
    (d) => `- pattern: \`${d.pattern || ""}\``
  );
  const deepLinksSection =
    "## 3. Deep Links\n\n" +
    (deepLinkParts.length ? deepLinkParts.join("\n") : "- (none)") +
    "\n\n";

  const entrySection = `## 4. Entry & Return Policy

- entry: \`${entry}\`
- returnPolicy: \`${returnPolicy}\`

---
`;

  return header + abilitiesSection + routesSection + deepLinksSection + entrySection;
}

/* ------------- 7) resources.rules.md ------------- */

function genResourcesRulesMd() {
  const res = sst.resources || {};
  const namespaces = res.namespaces || {};
  const examples = res.examples || {};
  const conflictOrder = res.conflictOrder || [];

  const header = `# Resources Rules (命名空间 & 冲突顺序)

来自 \`sst.resources\` 的资源命名规则约定。

---
`;

  const nsParts = [];
  for (const k of Object.keys(namespaces)) {
    nsParts.push(`### ${k}`);
    nsParts.push("- pattern: `" + namespaces[k] + "`");
    nsParts.push("");
  }
  const nsSection =
    "## 1. Namespaces\n\n" +
    (nsParts.length ? nsParts.join("\n") : "- (none)") +
    "\n";

  const exParts = [];
  for (const k of Object.keys(examples)) {
    const list = examples[k] || [];
    exParts.push(`### ${k}`);
    exParts.push(
      list.length ? list.map((v) => `- \`${v}\``).join("\n") : "- (none)"
    );
    exParts.push("");
  }
  const exSection =
    "## 2. Examples\n\n" +
    (exParts.length ? exParts.join("\n") : "- (none)") +
    "\n";

  const conflictSection =
    "## 3. Conflict Resolution Order\n\n" +
    (conflictOrder.length
      ? conflictOrder
          .map((s, idx) => `${idx + 1}. ${s}`)
          .join("\n")
      : "- (none)") +
    "\n\n---\n" +
    "## 说明\n- 冲突顺序用于当同一个资源 key 在模板 / UI / 模块多处声明时的决策顺序。\n";

  return header + nsSection + exSection + conflictSection;
}
function genTemplateResourcesMapMd() {
  const examples = sst.resources?.examples || {};
  const out = [];
  for (const key of Object.keys(examples)) {
    const list = examples[key] || [];
    if (Array.isArray(list)) out.push(...list);
  }
  return mdSimpleList(out);
}

/* ------------- 8) gradle.manifest.md ------------- */

function genGradleManifestMd() {
  const g = sst.gradleManifest || {};
  const applicationId = g.applicationId || "com.example.app";
  const permissions = g.permissions;
  const dependencies = g.dependencies;
  const resConfigs = g.resConfigs;
  const conflictResolution = g.conflictResolution || "(unspecified)";

  const header = `# Gradle / AndroidManifest Contract

来自 \`sst.gradleManifest\` 的构建级约束，仅描述“形态 & 冲突策略”，不绑定具体实现。

---
`;

  const baseSection = `## 1. ApplicationId

- applicationId: \`${applicationId}\`

`;

  // 小工具：把“可能是数组 / 字符串 / undefined”的字段渲染成列表
  const renderList = (value) => {
    if (Array.isArray(value)) {
      return value.length
        ? value.map((v) => `- \`${v}\``).join("\n")
        : "- (none)";
    }
    if (typeof value === "string" && value.trim().length > 0) {
      // SST 里是描述性的字符串，比如 "list<string>"
      return `- \`${value}\``;
    }
    return "- (none)";
  };

  const permSection = `## 2. Permissions（合并策略：union）

${renderList(permissions)}

`;

  const depSection = `## 3. Dependencies（合并策略：union + conflictResolution）

${renderList(dependencies)}

`;

  const resConfigSection = `## 4. resConfigs

${renderList(resConfigs)}

`;

  const conflictSection = `## 5. Conflict Resolution

- strategy: \`${conflictResolution}\`

---
`;

  return header + baseSection + permSection + depSection + resConfigSection + conflictSection;
}

/* ------------- 9) compat.matrix.md ------------- */

function genCompatMatrixMd() {
  const c = sst.compatMatrix || {};
  const androidSdk = c.androidSdk || {};
  const agp = c.agp || {};
  const gradle = c.gradle || {};
  const template = c.template || {};
  const uiPack = c.uiPack || {};
  const module = c.module || {};

  const header = `# Compat Matrix（版本兼容矩阵）

来自 \`sst.compatMatrix\`，用于快速确认支持的 Android / AGP / Gradle / 模板版本。

---
`;

  const sdkSection = `## 1. Android SDK

- min: ${androidSdk.min ?? "(unspecified)"}
- target: ${androidSdk.target ?? "(unspecified)"}
- tested: ${Array.isArray(androidSdk.tested) ? androidSdk.tested.join(", ") : "(unspecified)"}

`;

  const agpSection = `## 2. Android Gradle Plugin (AGP)

- min: \`${agp.min ?? "(unspecified)"}\`
- max: \`${agp.max ?? "(unspecified)"}\`

`;

  const gradleSection = `## 3. Gradle

- min: \`${gradle.min ?? "(unspecified)"}\`
- max: \`${gradle.max ?? "(unspecified)"}\`

`;

  const tplSection = `## 4. Template / UI Pack / Module 最低版本

- template.min: \`${template.min ?? "1.0.0"}\`
- uiPack.min: \`${uiPack.min ?? "1.0.0"}\`
- module.min: \`${module.min ?? "1.0.0"}\`

---
`;

  return header + sdkSection + agpSection + gradleSection + tplSection;
}

/* ------------- 10) UI provided.capabilities.md ------------- */

function genUiProvidedCapabilitiesMd() {
  const providedUi = (sst.capabilities?.provided && sst.capabilities.provided.uiPack) || [];
  return mdSimpleList(providedUi);
}

function genUiRoutesProvidedMapMd() {
  const routes = (sst.routing?.schema?.routes || []).map((r) => r.id).filter(Boolean);
  const deepLinks = (sst.routing?.schema?.deepLinks || []).map((d) => d.pattern).filter(Boolean);
  return mdSimpleList([...routes, ...deepLinks]);
}

function genUiResourcesProvidedMapMd() {
  const examples = sst.resources?.examples || {};
  const out = [];
  for (const key of Object.keys(examples)) {
    const list = examples[key] || [];
    if (Array.isArray(list)) out.push(...list);
  }
  return mdSimpleList(out);
}

function genUiSstLockJson() {
  return {
sstId: sst.contract?.hash || "",
    contract: sst.contract?.name || "ndjc-sst",
    version: sst.contract?.version || "1.0.0",
    hash: sst.contract?.hash || "",
    issuedAt: sst.contract?.issuedAt || "",
    uiPackMinVersion: sst.compatMatrix?.uiPack?.min || "1.0.0",
  };
}

function genUiPreflightProviderMd() {
  const checks = sst.preflight?.requiredChecks || [];
  return `# Preflight Provider（UI Pack）

本文件由 \`sst.preflight.requiredChecks\` 自动生成，用于声明 UI Pack 参与实现的预检能力。

---

## Required Checks

${mdList(checks)}

## UI Pack Provider Notes

- 本 UI Pack 至少要能配合模板 / 模块完成上述 requiredChecks。
- 若某项主要由模块承担，也必须在联调时可被 checker 识别为“已覆盖”。
`;
}

function genUiPublicSurfaceFreezeMd() {
  const components = sst.publicSurface?.components || {};
  const frozenApi = sst.publicSurface?.themeAndTokens?.frozenAPI || {};

  const compParts = [];
  for (const key of Object.keys(components)) {
    const item = components[key] || {};
    compParts.push(`### ${key}`);
    if (item.alias) compParts.push(`- alias: \`${item.alias}\``);
    if (item.slot) compParts.push(`- slot: \`${item.slot}\``);
    compParts.push(`- required: \`${item.required === true ? "true" : "false"}\``);
    const props = item.props || {};
    const propKeys = Object.keys(props);
    compParts.push(`- props: ${propKeys.length ? propKeys.map((k) => `\`${k}:${props[k]}\``).join(", ") : "(none)"}`);
    compParts.push("");
  }

  const frozenParts = [];
  for (const key of Object.keys(frozenApi)) {
    const value = frozenApi[key];
    frozenParts.push(`### ${key}`);
    if (Array.isArray(value)) {
      frozenParts.push(mdList(value, (v) => `- \`${v}\``));
    } else {
      frozenParts.push(`- \`${String(value)}\``);
    }
    frozenParts.push("");
  }

  return `# Public Surface Freeze（UI Pack）

本文件由 \`sst.publicSurface\` 自动生成，用于冻结 UI Pack 对外公共表面。

---

## Components

${compParts.length ? compParts.join("\n") : "- (none)"}

## Theme & Tokens Frozen API

${frozenParts.length ? frozenParts.join("\n") : "- (none)"}
`;
}

function genUiTokensKeysConformanceMd() {
  const domains = sst.tokens?.domains || {};
  const rules = sst.tokens?.rules || {};

  const parts = [];
  for (const key of Object.keys(domains)) {
    const item = domains[key] || {};
    parts.push(`### ${key}`);
    parts.push(`- required: \`${item.required === true ? "true" : "false"}\``);
    if (Array.isArray(item.keys)) {
      parts.push(`- keys: ${item.keys.length ? item.keys.map((v) => `\`${v}\``).join(", ") : "(none)"}`);
    }
    if (Array.isArray(item.enum)) {
      parts.push(`- enum: ${item.enum.length ? item.enum.map((v) => `\`${v}\``).join(", ") : "(none)"}`);
    }
    parts.push("");
  }

  return `# Tokens Keys Conformance（UI Pack）

本文件由 \`sst.tokens\` 自动生成，用于说明 UI Pack Token 键命名与校验规则。

---

## Rules

- naming: \`${rules.naming || "(unspecified)"}\`
- validation: \`${rules.validation || "(unspecified)"}\`

## Domains

${parts.length ? parts.join("\n") : "- (none)"}
`;
}

function genUiCdcRunnerMd() {
  const cases = sst.cdc?.cases || [];
  return `# CDC Runner（UI Pack）

本文件由 \`sst.cdc\` 自动生成，用于声明 UI Pack 需要参与的 Contract Driven Cases。

---

## Cases

${mdList(cases)}

## Notes

- UI Pack 必须保证上述 case 在对应模块 / 模板组合下可被执行。
`;
}

function genUiDeprecationPolicyMd() {
  const policy = sst.deprecationPolicy || {};
  const grace = policy.gracePeriods || {};
  return `# Deprecation Policy（UI Pack）

本文件由 \`sst.deprecationPolicy\` 自动生成。

---

## Grace Periods

- minor: \`${grace.minor || "(unspecified)"}\`
- major: \`${grace.major || "(unspecified)"}\`

## Expired Behavior

- expiredIsError: \`${policy.expiredIsError === true ? "true" : "false"}\`

## Registry

${mdList(policy.registry || [], (v) => `- ${JSON.stringify(v)}`)}
`;
}

function genUiGoldenSnapshotsMd() {
  const golden = sst.cdc?.golden || {};
  return `# Golden Snapshots（UI Pack）

本文件由 \`sst.cdc.golden\` 自动生成，用于声明 UI Pack 视觉黄金基线约束。

---

- baselineVersion: \`${golden.baselineVersion || "(unspecified)"}\`
- allowedDrift: \`${golden.allowedDrift || "(unspecified)"}\`
`;
}

function genTemplateRoutesMapMd() {
  const routes = (sst.routing?.schema?.routes || []).map((r) => r.id).filter(Boolean);
  const deepLinks = (sst.routing?.schema?.deepLinks || []).map((d) => d.pattern).filter(Boolean);
  return mdSimpleList([...routes, ...deepLinks]);
}

function genTemplateSstLockJson() {
  return {
sstId: sst.contract?.hash || "",
    contract: sst.contract?.name || "ndjc-sst",
    version: sst.contract?.version || "1.0.0",
    hash: sst.contract?.hash || "",
    issuedAt: sst.contract?.issuedAt || "",
    templateMinVersion: sst.compatMatrix?.template?.min || "1.0.0"
  };
}
function genModuleSlotsUsageMapMd() {
  const standard = sst.slots?.standard || [];
  const optional = sst.slots?.optional || [];
  return mdSimpleList([...standard, ...optional]);
}

function genModuleRoutesProvidedMapMd() {
  const routes = (sst.routing?.schema?.routes || []).map((r) => r.id).filter(Boolean);
  const deepLinks = (sst.routing?.schema?.deepLinks || []).map((d) => d.pattern).filter(Boolean);
  return mdSimpleList([...routes, ...deepLinks]);
}

function genModuleResourcesUsageMapMd() {
  const examples = sst.resources?.examples || {};
  const out = [];
  for (const key of Object.keys(examples)) {
    const list = examples[key] || [];
    if (Array.isArray(list)) out.push(...list);
  }
  return mdSimpleList(out);
}

function genModulePreflightProviderMd() {
  const checks = sst.preflight?.requiredChecks || [];
  return `# Preflight Provider（Feature Module）

本文件由 \`sst.preflight.requiredChecks\` 自动生成，用于声明模块参与实现的预检能力。

---

## Required Checks

${checks.length ? checks.map((v) => `- ${v}`).join("\n") : "- (none)"}

## Module Provider Notes

- 模块需与模板 / UI Pack 共同覆盖上述 requiredChecks。
- 若某项主要由 UI 承担，模块侧也应声明其已知的协作边界。
`;
}

function genModuleSstLockJson() {
  return {
    sstId: sst.contract?.hash || "",
    contract: sst.contract?.name || "ndjc-sst",
    version: sst.contract?.version || "1.0.0",
    hash: sst.contract?.hash || "",
    issuedAt: sst.contract?.issuedAt || "",
    moduleMinVersion: sst.compatMatrix?.module?.min || "1.0.0"
  };
}
function genModuleProvidedCapabilitiesMd() {
  const providedModule = (sst.capabilities?.provided && sst.capabilities.provided.module) || [];
  return mdSimpleList(providedModule);
}

function genModuleThemeTokensApiMd() {
  return mdSimpleList(flattenRequiredTokens(requiredTokens));
}

function genModuleCdcRunnerMd() {
  const cases = sst.cdc?.cases || [];
  return mdSimpleList(cases);
}

function genModuleDeprecationPolicyMd() {
  const policy = sst.deprecationPolicy || {};
  const grace = policy.gracePeriods || {};
  const registry = policy.registry || [];

  return `# Deprecation Policy（Feature Module）

## Grace Periods
- minor=${grace.minor || "(unspecified)"}
- major=${grace.major || "(unspecified)"}

## Expired Behavior
- expiredIsError=${policy.expiredIsError === true ? "true" : "false"}

## Registry
${registry.length ? registry.map((v) => `- ${JSON.stringify(v)}`).join("\n") : ""}
`;
}

function genModuleGoldenSnapshotsMd() {
  const golden = sst.cdc?.golden || {};
  return `# Golden Snapshots（Feature Module）

- baselineVersion=${golden.baselineVersion || "(unspecified)"}
- allowedDrift=${golden.allowedDrift || "(unspecified)"}
`;
}

function genModuleTokensKeysConformanceMd() {
  const rules = sst.tokens?.rules || {};
  const domains = sst.tokens?.domains || {};

  const lines = [];
  lines.push(`# Tokens Keys Conformance（Feature Module）`);
  lines.push(`- naming=${rules.naming || "(unspecified)"}`);
  lines.push(`- validation=${rules.validation || "(unspecified)"}`);

  for (const key of Object.keys(domains)) {
    const item = domains[key] || {};
    lines.push(`- domain=${key}`);
    lines.push(`- required=${item.required === true ? "true" : "false"}`);
    if (Array.isArray(item.keys)) {
      for (const tokenKey of item.keys) {
        lines.push(`- ${key}.${tokenKey}`);
      }
    }
  }

  return lines.join("\n") + "\n";
}

function genModulePublicSurfaceFreezeMd() {
  const components = sst.publicSurface?.components || {};
  const frozenApi = sst.publicSurface?.themeAndTokens?.frozenAPI || {};

  const lines = [];
  lines.push(`# Public Surface Freeze（Feature Module）`);

  for (const key of Object.keys(components)) {
    const item = components[key] || {};
    lines.push(`- component=${key}`);
    if (item.alias) lines.push(`- alias=${item.alias}`);
    if (item.slot) lines.push(`- slot=${item.slot}`);
    lines.push(`- required=${item.required === true ? "true" : "false"}`);
  }

  for (const key of Object.keys(frozenApi)) {
    const value = frozenApi[key];
    if (Array.isArray(value)) {
      for (const v of value) {
        lines.push(`- ${key}.${v}`);
      }
    } else {
      lines.push(`- ${key}=${String(value)}`);
    }
  }

  return lines.join("\n") + "\n";
}
/* ------------- 11) moduleManifest.contract.md ------------- */

function genModuleManifestContractMd() {
  const m = sst.moduleManifest || {};
  const fields = m.fields || {};
  const rules = m.rules || [];

  const header = `# Module Manifest Contract

来自 \`sst.moduleManifest\` 的模块侧契约定义，所有功能模块的 .ndjc / manifest.json 必须遵守。

---
`;

  const fieldParts = [];
  for (const k of Object.keys(fields)) {
    fieldParts.push(`- \`${k}\`: \`${fields[k]}\``);
  }

  const fieldsSection =
    "## 1. Fields\n\n" +
    (fieldParts.length ? fieldParts.join("\n") : "- (none)") +
    "\n\n";

  const rulesSection =
    "## 2. Rules\n\n" +
    (rules.length
      ? rules.map((r) => `- ${r}`).join("\n")
      : "- (none)") +
    "\n\n---\n" +
    "## 说明\n- 模块注册表和模块 .ndjc 应与本契约保持一一对应。\n";

  return header + fieldsSection + rulesSection;
}

/* ------------- 12) cdc.cases.md ------------- */

function genCdcCasesMd() {
  const cdc = sst.cdc || {};
  const cases = cdc.cases || [];
  const golden = cdc.golden || {};

  const header = `# Contract-Driven Cases（CDC）

来自 \`sst.cdc\`，用于定义模板 / UI / 模块组合必须通过的黄金用例。

---
`;

  const casesSection =
    "## 1. Cases\n\n" +
    (cases.length
      ? cases.map((c) => `- ${c}`).join("\n")
      : "- (none)") +
    "\n\n";

  const goldenSection = `## 2. Golden Baseline

- baselineVersion: \`${golden.baselineVersion ?? "(unspecified)"}\`
- allowedDrift: \`${golden.allowedDrift ?? "(unspecified)"}\`

---
`;

  return header + casesSection + goldenSection;
}

/* ------------- 13) observability.md ------------- */

function genObservabilityMd() {
  const obs = sst.observability || {};
  const events = obs.events || "(unspecified)";
  const kpi = obs.kpi || [];

  const header = `# Observability Contract

来自 \`sst.observability\`，约定埋点命名和关键指标。

---
`;

  const eventsSection = `## 1. Event Naming

- pattern: \`${events}\`

`;

  const kpiSection =
    "## 2. KPI\n\n" +
    (kpi.length ? kpi.map((x) => `- ${x}`).join("\n") : "- (none)") +
    "\n\n---\n";

  return header + eventsSection + kpiSection;
}

/* ------------- 14) deprecation.policy.md ------------- */

function genDeprecationPolicyMd() {
  const dep = sst.deprecationPolicy || {};
  const gracePeriods = dep.gracePeriods || {};
  const expiredIsError = dep.expiredIsError;
  const registry = dep.registry || [];

  const header = `# Deprecation Policy

来自 \`sst.deprecationPolicy\`，用于控制契约变更与废弃策略。

---
`;

  const gpSection = `## 1. Grace Periods

- minor: \`${gracePeriods.minor ?? "(unspecified)"}\`
- major: \`${gracePeriods.major ?? "(unspecified)"}\`

`;

  const expiredSection = `## 2. Expired Behavior

- expiredIsError: \`${expiredIsError === true ? "true" : "false"}\`

`;

  const regSection =
    "## 3. Deprecation Registry（预留）\n\n" +
    (registry.length
      ? registry.map((r) => `- ${JSON.stringify(r)}`).join("\n")
      : "- (none)") +
    "\n\n---\n";

  return header + gpSection + expiredSection + regSection;
}

/* ================== RUN ================== */

console.log("NDJC · SST → 文档同步（全集版）");
console.log("SST:", path.relative(ROOT, SST_PATH));

/* 1) required.capabilities.md */
writeFilePretty(
  path.join(TEMPLATE_NDJC_DIR, "required.capabilities.md"),
  genRequiredCapabilitiesMd()
);

/* 2) tokens.requirements.md */
writeFilePretty(
  path.join(TEMPLATE_NDJC_DIR, "tokens.requirements.md"),
  genTokensRequirementsMd()
);

/* 3) theme.tokens.api.md */
writeFilePretty(
  path.join(UI_NDJC_DIR, "theme.tokens.api.md"),
  genUiThemeTokensApiMd()
);

/* 4) preflight.checklist.md */
writeFilePretty(
  path.join(TEMPLATE_NDJC_DIR, "preflight.checklist.md"),
  genPreflightChecklistMd()
);

/* 5) slots.contract.md */
writeFilePretty(
  path.join(TEMPLATE_NDJC_DIR, "slots.contract.md"),
  genSlotsContractMd()
);

/* 5.1) slots.map.md */
writeFilePretty(
  path.join(TEMPLATE_NDJC_DIR, "slots.map.md"),
  genTemplateSlotsMapMd()
);

/* 6) routing.schema.md */
writeFilePretty(
  path.join(TEMPLATE_NDJC_DIR, "routing.schema.md"),
  genRoutingSchemaMd()
);

/* 7) resources.rules.md */
writeFilePretty(
  path.join(TEMPLATE_NDJC_DIR, "resources.rules.md"),
  genResourcesRulesMd()
);

/* 7.1) resources.map.md */
writeFilePretty(
  path.join(TEMPLATE_NDJC_DIR, "resources.map.md"),
  genTemplateResourcesMapMd()
);

/* 8) gradle.manifest.md */
writeFilePretty(
  path.join(TEMPLATE_NDJC_DIR, "gradle.manifest.md"),
  genGradleManifestMd()
);

/* 9) compat.matrix.md */
writeFilePretty(
  path.join(TEMPLATE_NDJC_DIR, "compat.matrix.md"),
  genCompatMatrixMd()
);

/* 10) UI provided.capabilities.md */
writeFilePretty(
  path.join(UI_NDJC_DIR, "provided.capabilities.md"),
  genUiProvidedCapabilitiesMd()
);

/* 10.1) UI routes.provided.map.md */
writeFilePretty(
  path.join(UI_NDJC_DIR, "routes.provided.map.md"),
  genUiRoutesProvidedMapMd()
);

/* 10.2) UI resources.provided.map.md */
writeFilePretty(
  path.join(UI_NDJC_DIR, "resources.provided.map.md"),
  genUiResourcesProvidedMapMd()
);

/* 10.3) UI sst.lock.json */
writeFilePretty(
  path.join(UI_NDJC_DIR, "sst.lock.json"),
  renderJsonPretty(genUiSstLockJson())
);

/* 10.4) UI preflight.provider.md */
writeFilePretty(
  path.join(UI_NDJC_DIR, "preflight.provider.md"),
  genUiPreflightProviderMd()
);

/* 10.5) UI public-surface.freeze.md */
writeFilePretty(
  path.join(UI_NDJC_DIR, "public-surface.freeze.md"),
  genUiPublicSurfaceFreezeMd()
);

/* 10.6) UI tokens.keys.conformance.md */
writeFilePretty(
  path.join(UI_NDJC_DIR, "tokens.keys.conformance.md"),
  genUiTokensKeysConformanceMd()
);

/* 10.7) UI cdc.runner.md */
writeFilePretty(
  path.join(UI_NDJC_DIR, "cdc.runner.md"),
  genUiCdcRunnerMd()
);

/* 10.8) UI deprecation.policy.md */
writeFilePretty(
  path.join(UI_NDJC_DIR, "deprecation.policy.md"),
  genUiDeprecationPolicyMd()
);

/* 10.9) UI golden.snapshots.md */
writeFilePretty(
  path.join(UI_NDJC_DIR, "golden.snapshots.md"),
  genUiGoldenSnapshotsMd()
);

/* 11) moduleManifest.contract.md */
writeFilePretty(
  path.join(TEMPLATE_NDJC_DIR, "moduleManifest.contract.md"),
  genModuleManifestContractMd()
);

/* 12) cdc.cases.md */
writeFilePretty(
  path.join(TEMPLATE_NDJC_DIR, "cdc.cases.md"),
  genCdcCasesMd()
);

/* 13) observability.md */
writeFilePretty(
  path.join(TEMPLATE_NDJC_DIR, "observability.md"),
  genObservabilityMd()
);

/* 14) deprecation.policy.md */
writeFilePretty(
  path.join(TEMPLATE_NDJC_DIR, "deprecation.policy.md"),
  genDeprecationPolicyMd()
);

/* 🔥 新增：template routes.map.md */
writeFilePretty(
  path.join(TEMPLATE_NDJC_DIR, "routes.map.md"),
  genTemplateRoutesMapMd()
);

/* 🔥 新增：template sst.lock.json */
writeFilePretty(
  path.join(TEMPLATE_NDJC_DIR, "sst.lock.json"),
  renderJsonPretty(genTemplateSstLockJson())
);

/* 🔥 新增：module provided.capabilities.md */
writeFilePretty(
  path.join(MODULE_NDJC_DIR, "provided.capabilities.md"),
  genModuleProvidedCapabilitiesMd()
);

/* 🔥 新增：module theme.tokens.api.md */
writeFilePretty(
  path.join(MODULE_NDJC_DIR, "theme.tokens.api.md"),
  genModuleThemeTokensApiMd()
);

/* 🔥 新增：module slots.usage.map.md */
writeFilePretty(
  path.join(MODULE_NDJC_DIR, "slots.usage.map.md"),
  genModuleSlotsUsageMapMd()
);

/* 🔥 新增：module routes.provided.map.md */
writeFilePretty(
  path.join(MODULE_NDJC_DIR, "routes.provided.map.md"),
  genModuleRoutesProvidedMapMd()
);

/* 🔥 新增：module resources.usage.map.md */
writeFilePretty(
  path.join(MODULE_NDJC_DIR, "resources.usage.map.md"),
  genModuleResourcesUsageMapMd()
);

/* 🔥 新增：module preflight.provider.md */
writeFilePretty(
  path.join(MODULE_NDJC_DIR, "preflight.provider.md"),
  genModulePreflightProviderMd()
);

/* 🔥 新增：module sst.lock.json */
writeFilePretty(
  path.join(MODULE_NDJC_DIR, "sst.lock.json"),
  renderJsonPretty(genModuleSstLockJson())
);

/* 🔥 新增：module cdc.runner.md */
writeFilePretty(
  path.join(MODULE_NDJC_DIR, "cdc.runner.md"),
  genModuleCdcRunnerMd()
);

/* 🔥 新增：module deprecation.policy.md */
writeFilePretty(
  path.join(MODULE_NDJC_DIR, "deprecation.policy.md"),
  genModuleDeprecationPolicyMd()
);

/* 🔥 新增：module golden.snapshots.md */
writeFilePretty(
  path.join(MODULE_NDJC_DIR, "golden.snapshots.md"),
  genModuleGoldenSnapshotsMd()
);

/* 🔥 新增：module tokens.keys.conformance.md */
writeFilePretty(
  path.join(MODULE_NDJC_DIR, "tokens.keys.conformance.md"),
  genModuleTokensKeysConformanceMd()
);

/* 🔥 新增：module public-surface.freeze.md */
writeFilePretty(
  path.join(MODULE_NDJC_DIR, "public-surface.freeze.md"),
  genModulePublicSurfaceFreezeMd()
);

console.log("✅ 同步完成。模板侧、UI 包侧、模块侧 .ndjc 已重新生成。现在可以重新跑：node scripts/ndjc-sst-checker.js --strict");
