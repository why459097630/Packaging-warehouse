#!/usr/bin/env bash
# NDJC: 组合装配脚本（绝对路径版）
# 说明：
#   - 读取 E:/NDJC/Packaging-warehouse/lib/ndjc/assembly.local.json
#   - 根据组合更新：
#       E:/NDJC/Packaging-warehouse/templates/Core-Templates/settings.gradle.kts
#       E:/NDJC/Packaging-warehouse/templates/Core-Templates/app/build.gradle.kts
#   - 调用 E:/NDJC/Packaging-warehouse/scripts/ndjc-sst-checker.js 做契约自检

node - <<'NODE'
const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");

// ===== 你当前环境的绝对路径（按你提供的为准，统一成带连字符版） =====
const SETTINGS_GRADLE = "E:/NDJC/Packaging-warehouse/templates/Core-Templates/settings.gradle.kts";
const APP_GRADLE      = "E:/NDJC/Packaging-warehouse/templates/Core-Templates/app/build.gradle.kts";
const ASSEMBLY_JSON   = "E:/NDJC/Packaging-warehouse/lib/ndjc/assembly.local.json";
const SST_CHECKER     = "E:/NDJC/Packaging-warehouse/scripts/ndjc-sst-checker.js";
const SST_JSON        = "E:/NDJC/Packaging-warehouse/scripts/sst.json";

// 模板根目录（由 settings.gradle.kts 所在目录反推）
const TEMPLATE_DIR    = path.dirname(SETTINGS_GRADLE);           // .../templates/Core-Templates
const TEMPLATE_NDJC   = path.join(TEMPLATE_DIR, ".ndjc");        // 模板 .ndjc

// ---------- 工具函数 ----------
function fail(msg) {
  console.error("[NDJC-assembly] ERROR:", msg);
  process.exit(1);
}

function warn(msg) {
  console.warn("[NDJC-assembly] WARNING:", msg);
}

function ensureFile(p, desc) {
  if (!fs.existsSync(p)) fail(`找不到 ${desc}：${p}`);
}

function readText(p) {
  return fs.readFileSync(p, "utf8");
}

function writeText(p, content) {
  fs.writeFileSync(p, content, "utf8");
}

/**
 * 替换起止标记之间的内容
 * @param {string} content 原始文本
 * @param {string} startMarker 起始标记整行
 * @param {string} endMarker 结束标记整行
 * @param {string} newBlock   新内容（不含起止标记）
 */
function replaceBlock(content, startMarker, endMarker, newBlock) {
  content = content.replace(/\r\n/g, "\n");

  const startIdx = content.indexOf(startMarker);
  if (startIdx === -1) fail(`文件中找不到起始标记：${startMarker}`);

  const endIdx = content.indexOf(endMarker, startIdx);
  if (endIdx === -1) fail(`文件中找不到结束标记：${endMarker}`);

  const before = content.slice(0, startIdx + startMarker.length);
  const after  = content.slice(endIdx);

  const middle = "\n" + newBlock.replace(/\r\n/g, "\n") + "\n";

  return before + middle + after;
}

// ---------- 1) 校验基础文件存在 ----------
ensureFile(ASSEMBLY_JSON, "装配清单 assembly.local.json");
ensureFile(SETTINGS_GRADLE, "settings.gradle.kts");
ensureFile(APP_GRADLE,      "app/build.gradle.kts");
ensureFile(SST_CHECKER,     "自检脚本 ndjc-sst-checker.js");
ensureFile(SST_JSON,        "sst.json");

if (!fs.existsSync(TEMPLATE_NDJC)) {
  warn(`找不到模板 .ndjc 目录：${TEMPLATE_NDJC}`);
}

// ---------- 2) 读取 assembly.local.json ----------
let assembly;
try {
  const raw = readText(ASSEMBLY_JSON);
  assembly = JSON.parse(raw);
} catch (e) {
  fail("解析 assembly.local.json 失败：" + e.message);
}

const templateId = assembly.template || "core-skeleton"; // 目前保留字段
const uiPackId   = assembly.uiPack  || "ui-pack-neumorph";
const modules    = Array.isArray(assembly.modules)
  ? assembly.modules.filter(Boolean)
  : [];

console.log("[NDJC-assembly] 使用组合：");
console.log("  template :", templateId);
console.log("  uiPack   :", uiPackId);
console.log("  modules  :", modules.length ? modules.join(", ") : "(无模块，只有骨架 + UI 包)");

// ---------- 3) 更新 settings.gradle.kts ----------
/**
 * include(
 *     ":app",
 *     ":core-skeleton",
 *     ":feature-home-basic",
 *     ":feature-about-basic",
 *     ":ui-pack-neumorph"
 * )
 */
const moduleNames = ["app", "core-skeleton", ...modules, uiPackId];

const includeLines = [
  "include(",
  ...moduleNames.map((name, idx) => {
    const comma = idx === moduleNames.length - 1 ? "" : ",";
    return `    ":${name}"${comma}`;
  }),
  ")"
];
const includeBlock = includeLines.join("\n");

let settingsContent = readText(SETTINGS_GRADLE);
settingsContent = replaceBlock(
  settingsContent,
  "// NDJC-AUTO-INCLUDE-START",
  "// NDJC-AUTO-INCLUDE-END",
  includeBlock
);
writeText(SETTINGS_GRADLE, settingsContent);
console.log("[NDJC-assembly] 已更新 settings.gradle.kts 的 NDJC-AUTO-INCLUDE 区域");

// ---------- 4) 更新 app/build.gradle.kts ----------
/**
 *     // NDJC-AUTO-DEPS-START
 *     implementation(project(":core-skeleton"))
 *     implementation(project(":feature-home-basic"))
 *     implementation(project(":feature-about-basic"))
 *     implementation(project(":ui-pack-neumorph"))
 *     // NDJC-AUTO-DEPS-END
 */
const depsLines = [
  `    implementation(project(":core-skeleton"))`,
  ...modules.map((m) => `    implementation(project(":${m}"))`),
  `    implementation(project(":${uiPackId}"))`
];
const depsBlock = depsLines.join("\n");

let appGradleContent = readText(APP_GRADLE);
appGradleContent = replaceBlock(
  appGradleContent,
  "// NDJC-AUTO-DEPS-START",
  "// NDJC-AUTO-DEPS-END",
  depsBlock
);
writeText(APP_GRADLE, appGradleContent);
console.log("[NDJC-assembly] 已更新 app/build.gradle.kts 的 NDJC-AUTO-DEPS 区域");

// ---------- 5) 调用 ndjc-sst-checker.js 做契约自检 ----------
const uiNdjcDir = path.join(TEMPLATE_DIR, uiPackId, ".ndjc");
if (!fs.existsSync(uiNdjcDir)) {
  warn(`找不到 UI 包 .ndjc 目录：${uiNdjcDir}`);
}

const moduleNdjcArgs = [];
for (const m of modules) {
  const modDir = path.join(TEMPLATE_DIR, m, ".ndjc");
  if (fs.existsSync(modDir)) {
    moduleNdjcArgs.push("--module", modDir);
  } else {
    warn(`找不到模块 .ndjc 目录：${modDir}`);
  }
}

console.log("[NDJC-assembly] 运行契约自检（ndjc-sst-checker.js）...");

const result = spawnSync(
  "node",
  [
    SST_CHECKER,
    "--strict",
    "--template",
    TEMPLATE_NDJC,
    "--ui",
    uiNdjcDir,
    ...moduleNdjcArgs,
    "--sst",
    SST_JSON
  ],
  {
    stdio: "inherit"
  }
);

if (result.error) {
  fail("调用 ndjc-sst-checker.js 失败：" + result.error.message);
}
if (typeof result.status === "number" && result.status !== 0) {
  fail(`契约自检失败，退出码：${result.status}`);
}

console.log("[NDJC-assembly] 完成：");
console.log("  - settings.gradle.kts 已根据 assembly.local.json 更新");
console.log("  - app/build.gradle.kts 已根据 assembly.local.json 更新");
console.log("  - 契约自检已执行（ndjc-sst-checker.js）");
console.log();
console.log("现在可以运行 Gradle 构建，例如：");
console.log("  ./gradlew assembleDebug");
NODE
