#!/usr/bin/env bash
# NDJC: 组合装配脚本（绝对路径版）
# 说明：
#   - 读取 lib/ndjc/assembly.local.json
#   - 根据组合更新：
#       templates/Core-Templates/settings.gradle.kts
#       templates/Core-Templates/app/build.gradle.kts
#   - 写入 App 名称 + 图标到模板（strings.xml / AndroidManifest.xml / mipmap 图标）
#   - 调用 scripts/ndjc-sst-checker.js 做契约自检

node - <<'NODE'
const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");

// ===== 路径常量 =====
const SETTINGS_GRADLE = "templates/Core-Templates/settings.gradle.kts";
const APP_GRADLE      = "templates/Core-Templates/app/build.gradle.kts";
const ASSEMBLY_JSON   = "lib/ndjc/assembly.local.json";
const SST_CHECKER     = "scripts/ndjc-sst-checker.js";
const SST_JSON        = "scripts/sst.json";

// ✅ 新增：把前端传来的 appName / icon 落地到模板
const STRINGS_XML     = "templates/Core-Templates/app/src/main/res/values/strings.xml";
const MANIFEST_XML    = "templates/Core-Templates/app/src/main/AndroidManifest.xml";
const RES_DIR         = "templates/Core-Templates/app/src/main/res";

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
  fs.mkdirSync(path.dirname(p), { recursive: true });
  fs.writeFileSync(p, content, "utf8");
}

/**
 * 替换起止标记之间的内容
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

function escapeXml(v) {
  return String(v)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;");
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

// ✅ 新增：App 名称
const appLabel = (assembly.appName || assembly.app_label || "NDJC App").toString().trim();

// ✅ 新增：图标输入（iconPath 或 iconBase64）
const iconPathFromJson = (assembly.iconPath || assembly.icon_path || "").toString().trim();
const iconBase64       = (assembly.iconBase64 || assembly.icon_base64 || "").toString().trim();
const ICON_FALLBACK    = "lib/ndjc/icon.png"; // 约定：route.ts 可把上传图标落盘到这里
const iconPngPath      = iconPathFromJson || ICON_FALLBACK;

console.log("[NDJC-assembly] 使用组合：");
console.log("  template :", templateId);
console.log("  uiPack   :", uiPackId);
console.log("  modules  :", modules.length ? modules.join(", ") : "(无模块，只有骨架 + UI 包)");

// ---------- 2.5) 写入 App 名称到模板 ----------
function upsertAppNameToStrings(stringsXmlPath, label) {
  if (!fs.existsSync(stringsXmlPath)) return false;
  let s = readText(stringsXmlPath);

  const re = /<string\s+name="app_name">([\s\S]*?)<\/string>/m;
  if (re.test(s)) {
    s = s.replace(re, `<string name="app_name">${escapeXml(label)}</string>`);
  } else {
    const insert = `  <string name="app_name">${escapeXml(label)}</string>\n`;
    s = s.replace(/<\/resources>\s*$/m, insert + "</resources>");
  }
  writeText(stringsXmlPath, s);
  return true;
}

function patchManifestLabel(manifestPath) {
  if (!fs.existsSync(manifestPath)) return false;
  let m = readText(manifestPath);

  // 把 android:label 固定指向 @string/app_name（最稳）
  if (/android:label="/.test(m)) {
    m = m.replace(/android:label="[^"]*"/, `android:label="@string/app_name"`);
  } else {
    // application 节点没有 label 的话，插入一个（尽量保守插入）
    m = m.replace(/<application\b([^>]*)>/, `<application$1 android:label="@string/app_name">`);
  }

  writeText(manifestPath, m);
  return true;
}

console.log("[NDJC-assembly] 写入 App 名称:", appLabel);
const wroteStrings = upsertAppNameToStrings(STRINGS_XML, appLabel);
if (!wroteStrings) {
  warn(`strings.xml 不存在或写入失败：${STRINGS_XML}，将仅确保 AndroidManifest.xml 的 label 指向 @string/app_name`);
}
patchManifestLabel(MANIFEST_XML);

// ---------- 2.6) 写入 Launcher 图标到模板 ----------
function patchAdaptiveIconXmlFile(xmlPath, foregroundRef, backgroundRef) {
  if (!fs.existsSync(xmlPath)) return false;
  let x = readText(xmlPath);

  // 只做最小替换：把 foreground/background 的 android:drawable 指向 @mipmap/...
  x = x.replace(/<foreground>\s*<inset[^>]*android:drawable="[^"]*"[^>]*\/>\s*<\/foreground>/m,
                `<foreground>\n        <inset android:drawable="${foregroundRef}" android:inset="0%"/>\n    </foreground>`);
  x = x.replace(/<background>\s*<inset[^>]*android:drawable="[^"]*"[^>]*\/>\s*<\/background>/m,
                `<background>\n        <inset android:drawable="${backgroundRef}" android:inset="0%"/>\n    </background>`);
  x = x.replace(/android:drawable="[^"]*ic_launcher_foreground[^"]*"/g, `android:drawable="${foregroundRef}"`);
  x = x.replace(/android:drawable="[^"]*ic_launcher_background[^"]*"/g, `android:drawable="${backgroundRef}"`);

  writeText(xmlPath, x);
  return true;
}

function cleanupDrawableDuplicates(resDir) {
  // 如果 drawable 下同时存在 ic_launcher_foreground.xml 和 ic_launcher_foreground.png，会直接 Duplicate resources
  const drawableDirs = fs.readdirSync(resDir, { withFileTypes: true })
    .filter(d => d.isDirectory() && d.name.startsWith("drawable"))
    .map(d => path.join(resDir, d.name));

  const names = ["ic_launcher_foreground", "ic_launcher_background", "ic_launcher", "ic_launcher_round"];

  for (const dir of drawableDirs) {
    for (const n of names) {
      const xml = path.join(dir, `${n}.xml`);
      const png = path.join(dir, `${n}.png`);
      if (fs.existsSync(xml) && fs.existsSync(png)) {
        try {
          fs.unlinkSync(png);
          warn(`已清理冲突资源：${png}（保留 ${xml}）`);
        } catch (e) {
          warn(`清理冲突资源失败：${png} -> ${e.message}`);
        }
      }
    }
  }
}

function writeLauncherIcons(resDir, pngPath, base64Maybe) {
  // 如果提供了 base64，则解码落盘到 pngPath
  if (base64Maybe) {
    const cleaned = base64Maybe.replace(/^data:image\/png;base64,/, "");
    const buf = Buffer.from(cleaned, "base64");
    fs.mkdirSync(path.dirname(pngPath), { recursive: true });
    fs.writeFileSync(pngPath, buf);
  }

  if (!fs.existsSync(pngPath)) {
    warn(`未找到图标 png：${pngPath}（将继续使用模板默认图标）`);
    return false;
  }

  if (!fs.existsSync(resDir)) {
    warn(`res 目录不存在：${resDir}（将继续使用模板默认图标）`);
    return false;
  }

  // ✅ 关键修复：先清理 drawable 下可能残留的同名 png，避免 Duplicate resources
  cleanupDrawableDuplicates(resDir);

  // ✅ 关键修复：只写 mipmap-*，不再写 drawable*（drawable 常常有同名 xml，会冲突）
  const mipmapDirs = fs.readdirSync(resDir, { withFileTypes: true })
    .filter(d => d.isDirectory() && d.name.startsWith("mipmap-"))
    .map(d => path.join(resDir, d.name));

  if (!mipmapDirs.length) {
    warn(`未找到任何 mipmap-* 目录：${resDir}（将继续使用模板默认图标）`);
    return false;
  }

  for (const dir of mipmapDirs) {
    const base = path.basename(dir);

    // adaptive icon 目录只放 xml，先跳过 png 写入（后面会 patch xml）
    if (base === "mipmap-anydpi-v26") continue;

    const targets = ["ic_launcher.png", "ic_launcher_round.png", "ic_launcher_foreground.png", "ic_launcher_background.png"];

    for (const f of targets) {
      const target = path.join(dir, f);
      try {
        fs.copyFileSync(pngPath, target);
      } catch (e) {
        warn(`写入图标失败: ${dir}/${f} -> ${e.message}`);
      }
    }
  }

  // ✅ 让 adaptive icon xml 指向 @mipmap/...（确保系统启动器使用你写入的图标）
  const anydpi = path.join(resDir, "mipmap-anydpi-v26");
  const ic1 = path.join(anydpi, "ic_launcher.xml");
  const ic2 = path.join(anydpi, "ic_launcher_round.xml");
  patchAdaptiveIconXmlFile(ic1, "@mipmap/ic_launcher_foreground", "@mipmap/ic_launcher_background");
  patchAdaptiveIconXmlFile(ic2, "@mipmap/ic_launcher_foreground", "@mipmap/ic_launcher_background");

  return true;
}

console.log("[NDJC-assembly] 写入 App 图标:", iconPathFromJson ? iconPngPath : `${iconPngPath} (fallback)`);
writeLauncherIcons(RES_DIR, iconPngPath, iconBase64);

// ---------- 3) 更新 settings.gradle.kts ----------
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
console.log("  - App 名称已写入 strings.xml + manifest label 指向 @string/app_name");
console.log("  - App 图标已写入 res/mipmap-*/ic_launcher(.png) 与 ic_launcher_round(.png)");
console.log("  - settings.gradle.kts 已根据 assembly.local.json 更新");
console.log("  - app/build.gradle.kts 已根据 assembly.local.json 更新");
console.log("  - 契约自检已执行（ndjc-sst-checker.js）");
console.log();
console.log("现在可以运行 Gradle 构建，例如：");
console.log("  ./gradlew assembleDebug");
NODE
