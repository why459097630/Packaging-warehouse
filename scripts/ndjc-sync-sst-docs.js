#!/usr/bin/env node
/**
 * NDJC SST → 文档同步脚本（全集版）
 *
 * 统一从 scripts/sst.json 推导出两个 .ndjc 目录下的所有“契约 / 自检说明”文档：
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
  if (!Array.isArray(items) || items.length === 0) return "- (none)";
  return items.map(formatter).join("\n");
}

function renderTokenDomainsAndKeys(domains) {
  const parts = [];
  for (const domain of Object.keys(domains)) {
    const keys = domains[domain] || [];
    parts.push(`## ${domain}`);
    parts.push("");
    parts.push(keys.length ? keys.map((k) => `- ${k}`).join("\n") : "- (none)");
    parts.push("");
  }
  return parts.join("\n");
}

function renderJsonPretty(obj) {
  return JSON.stringify(obj, null, 2) + "\n";
}

const requiredCapabilities =
  sst.requiredCapabilities ||
  sst.template?.requiredCapabilities ||
  [];

const rawRequiredTokens =
  sst.requiredTokens ||
  sst.required?.tokens ||
  sst.tokens?.required ||
  sst.tokens?.requiredTokens ||
  null;

const requiredTokens = normalizeRequiredTokens(rawRequiredTokens);

const requiredChecks =
  sst.requiredChecks ||
  sst.preflight?.requiredChecks || [
    "slots.exist",
    "resources.keysResolvable",
    "tokens.complete",
    "routing.connected",
    "capabilities.satisfied",
    "sst.contract.basic",
  ];

/* ------------- 1) required.capabilities.md ------------- */

function genRequiredCapabilitiesMd() {
  const caps = requiredCapabilities || [];

  const header = `# Required Capabilities (Template → must be provided by UI Pack + Modules)

本模板运行所需的最低能力（required）。缺任一项，构建必须失败。

---
`;

  const requiredSection =
    "## required（来自 SST·requiredCapabilities）\n\n" +
    "以下为模板**强依赖**的能力，UI 包和模块必须共同提供：\n\n" +
    (caps.length
      ? caps.map((c) => `- ${c}`).join("\n")
      : "- (none)") +
    "\n\n---\n";

  const optionalSection = `## optional（模板可以使用，但不得依赖）

以下能力若 UI 包提供，模板可以选择性使用，但不得依赖：

- layout.responsive
- components.card
- components.modal

---

## 说明
- required 列表 = 完全取自 \`sst.json\` 的 \`requiredCapabilities\`。
- optional 仅作提示，不影响构建结果。
`;

  return header + "\n" + requiredSection + "\n" + optionalSection;
}

/* ------------- 2) tokens.requirements.md ------------- */

function genTokensRequirementsMd() {
  const domains = requiredTokens || {};

  const header = `# Tokens Requirements for Template
本模板所需的 Design Tokens 域与必备键（**Required**）。

---
`;

  const bodyParts = [];

  for (const domain of Object.keys(domains)) {
    const keys = domains[domain] || [];
    bodyParts.push(`### ${domain}`);
    if (keys.length) {
      bodyParts.push(keys.map((k) => `- ${k}`).join("\n"));
    } else {
      bodyParts.push("- (none)");
    }
    bodyParts.push(""); // 空行分隔
  }

  const tail = `---
## 使用规则（SST 合规说明）
- 上述 required 列表完全来自 sst.json 的 \`required.tokens\`，缺任意 key → 构建失败。
- UI 包可提供更多 Token，但不得比 required 列表少。
`;

  return header + bodyParts.join("\n") + tail;
}

/* ------------- 3) theme.tokens.api.md（只改 Tokens 段落） ------------- */

function genUiThemeTokensApiMd() {
  const domains = requiredTokens || {};
  const frozenApi = sst.publicSurface?.themeAndTokens?.frozenAPI || {};

  const header = `# Theme Tokens API（UI Pack）

本文件由 \`sst.json\` 自动生成，用于声明 UI Pack 对主题 / Tokens / Frozen API 的实现契约。

---

## 1. Required Token Domains & Keys

本 UI Pack 至少需要提供以下 Design Tokens。可扩展更多键，但不能缺少这些键。

`;

  const domainsSection = renderTokenDomainsAndKeys(domains);

  const frozenParts = [];
  frozenParts.push("## 2. Frozen Theme API");
  frozenParts.push("");

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

  const rules = sst.tokens?.rules || {};
  const rulesSection = `## 3. Token Rules

- naming: \`${rules.naming || "(unspecified)"}\`
- validation: \`${rules.validation || "(unspecified)"}\`

---

## 说明
- 此处 domain & key 与 \`Core-Templates/.ndjc/tokens.requirements.md\` 保持一一对应。
- UI Pack 可以提供更多 Token，但不得比 required 列表少。
- Frozen API 来自 \`sst.publicSurface.themeAndTokens.frozenAPI\`，UI 层实现不得私自漂移。
`;

  return header + domainsSection + "\n" + frozenParts.join("\n") + "\n" + rulesSection;
}

/* ------------- 4) preflight.checklist.md ------------- */

function genPreflightChecklistMd() {
  // 这里认为 requiredChecks 就是一串标识符，直接映射到标准文案。
  const CHECK_TEXT = {
    "slots.exist": {
      title: "slots.exist",
      desc: "模板声明的全部插槽存在且命名正确",
      onFail: "[Preflight] Slot missing or mismatched",
    },
    "resources.keysResolvable": {
      title: "resources.keysResolvable",
      desc: "所有资源 key 可解析，无悬空",
      onFail: "[Preflight] Resource key unresolved",
    },
    "tokens.complete": {
      title: "tokens.complete",
      desc: "满足 `tokens.requirements.md` 中全部必需键",
      onFail: "[Preflight] Missing required Token keys",
    },
    "routing.connected": {
      title: "routing.connected",
      desc: "所有路由可达、entry 可达、deep link 合法",
      onFail: "[Preflight] Routing not fully connected",
    },
    "capabilities.satisfied": {
      title: "capabilities.satisfied",
      desc: "UI 包 + 模块完全覆盖 `required.capabilities.md`",
      onFail: "[Preflight] Required capability missing",
    },
    "sst.contract.basic": {
      title: "sst.contract.basic",
      desc: "基础契约字段必须符合 `sst.json`",
      onFail: "[Preflight] SST contract mismatch",
    },
  };

  const header =
    "# Preflight Checklist (MUST PASS Before Build)\n" +
    "构建前必须全部通过，否则构建失败。\n\n---\n\n";

  const bodyParts = [];

  requiredChecks.forEach((id, idx) => {
    const info = CHECK_TEXT[id] || {
      title: id,
      desc: "(未在脚本中定义的检查，请手动补充说明)",
      onFail: `[Preflight] Check failed: ${id}`,
    };

    bodyParts.push(`## ${idx + 1}. ${info.title}`);
    bodyParts.push(info.desc);
    bodyParts.push(
      `- onFail: ❌ \`${info.onFail.replace(/`/g, "\\`")}\``
    );
    bodyParts.push("\n---\n");
  });

  const tail =
    "## 执行顺序\n" +
    "按上述列出的顺序依次执行，一旦失败立即停止构建。\n";

  return header + bodyParts.join("\n") + tail;
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
  const caps = sst.capabilities || {};
  const required = caps.required || [];
  const optional = caps.optional || [];
  const providedUi = (caps.provided && caps.provided.uiPack) || [];

  const header = `# Provided Capabilities（UI Pack 声明提供的能力）

本文件由 \`sst.capabilities\` 自动生成，用于与模板侧 \`required.capabilities.md\` 对齐。

---
`;

  const reqSection =
    "## 1. Template Required（来自 SST·capabilities.required）\n\n" +
    (required.length
      ? required.map((c) => `- ${c}`).join("\n")
      : "- (none)") +
    "\n\n";

  const optSection =
    "## 2. Optional（来自 SST·capabilities.optional）\n\n" +
    (optional.length
      ? optional.map((c) => `- ${c}`).join("\n")
      : "- (none)") +
    "\n\n";

  const provSection =
    "## 3. UI Pack Provided（来自 SST·capabilities.provided.uiPack）\n\n" +
    (providedUi.length
      ? providedUi.map((c) => `- ${c}`).join("\n")
      : "- (none)") +
    "\n\n---\n" +
    "## 说明\n- 模板构建期可检查：Template.required ⊆ UI.provided ∪ Modules.provided。\n";

  return header + reqSection + optSection + provSection;
}

function genUiRoutesProvidedMapMd() {
  const routes = sst.routing?.schema?.routes || [];
  const deepLinks = sst.routing?.schema?.deepLinks || [];
  const entry = sst.routing?.schema?.entry || "(none)";

  const routeLines = routes.map((route) => {
    const params = route.params || {};
    const paramKeys = Object.keys(params);
    return [
      `### route: ${route.id}`,
      `- id: ${route.id}`,
      `- params: ${paramKeys.length ? paramKeys.map((k) => `\`${k}:${params[k]}\``).join(", ") : "(none)"}`,
      `- entry: ${entry === route.id ? "true" : "false"}`,
      "",
    ].join("\n");
  });

  return `# Routes Provided Map（UI Pack）

本文件由 \`sst.routing.schema\` 自动生成，用于声明 UI Pack 可承载的路由与基础导航能力。

---

## Runtime Abilities

${mdList(sst.routing?.runtimeAbilities || [], (v) => `- \`${v}\``)}

## Routes

${routeLines.length ? routeLines.join("\n") : "- (none)"}

## Deep Links

${mdList(deepLinks.map((d) => d.pattern || ""), (v) => `- \`${v}\``)}
`;
}

function genUiResourcesProvidedMapMd() {
  const namespaces = sst.resources?.namespaces || {};
  const examples = sst.resources?.examples || {};

  const nsParts = [];
  for (const key of Object.keys(namespaces)) {
    nsParts.push(`### namespace: ${key}`);
    nsParts.push(`- pattern: \`${namespaces[key]}\``);
    const ex = examples[key] || [];
    nsParts.push(`- examples: ${ex.length ? ex.map((v) => `\`${v}\``).join(", ") : "(none)"}`);
    nsParts.push("");
  }

  return `# Resources Provided Map（UI Pack）

本文件由 \`sst.resources\` 自动生成，用于声明 UI Pack 资源命名空间与可解析 key 约定。

---

## Namespaces

${nsParts.length ? nsParts.join("\n") : "- (none)"}

## Conflict Resolution Order

${mdList(sst.resources?.conflictOrder || [], (v, i) => `- ${v}`)}
`;
}

function genUiSstLockJson() {
  return {
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

console.log("✅ 同步完成。模板侧与 UI 包侧 .ndjc 已重新生成。现在可以重新跑：node scripts/ndjc-sst-checker.js --strict");
