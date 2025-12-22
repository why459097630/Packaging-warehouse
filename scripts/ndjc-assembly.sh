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

// ===== NDJC: Unique applicationId (per App) =====
// 约定：assembly.local.json 中可包含 packageName/applicationId，用于“同一 App 后续复构建可更新”
let packageName = (assembly.packageName || assembly.applicationId || "").toString().trim();

function slugifyForAppId(s) {
  return String(s || "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "")
    .slice(0, 20);
}

function isValidApplicationId(id) {
  // 保守规则：多段，每段字母开头，后续字母/数字/下划线
  return /^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$/.test(id);
}

function generateApplicationId(label) {
  const crypto = require("crypto");
  const slug0 = slugifyForAppId(label) || "app";
  const slug  = /^[a-z]/.test(slug0) ? slug0 : `a_${slug0}`;
  const rand  = crypto.randomBytes(3).toString("hex"); // 6 chars
  return `com.ndjc.apps.${slug}.${rand}`;
}

if (!packageName) {
  packageName = generateApplicationId(appLabel);

  // 写回 assembly.local.json（目的：下次复构建复用同一包名，确保可上架/可更新）
  assembly.packageName = packageName;
  try {
    writeText(ASSEMBLY_JSON, JSON.stringify(assembly, null, 2));
    console.log("[NDJC-assembly] 生成并写回唯一 applicationId:", packageName);
  } catch (e) {
    warn(`写回 assembly.local.json 失败（不影响本次构建）：${e.message}`);
  }
} else {
  console.log("[NDJC-assembly] 复用 assembly 中的 applicationId:", packageName);
}

if (!isValidApplicationId(packageName)) {
  fail(`applicationId 不合法：${packageName}`);
}
// ===== END NDJC: Unique applicationId =====

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

function writeLauncherIcons(pngPath) {
  const ROOT = process.cwd();
  const resDir = path.join(ROOT, "templates/Core-Templates/app/src/main/res");
  if (!fs.existsSync(resDir)) {
    warn(`res dir not found: ${resDir}`);
    return false;
  }

  // A) 任何情况下：mipmap-anydpi-v26 只能放 adaptive icon 的 XML，绝对不能出现同名 PNG
  const anydpiDir = path.join(resDir, "mipmap-anydpi-v26");
  if (fs.existsSync(anydpiDir)) {
    for (const f of [
      "ic_launcher.png",
      "ic_launcher_round.png",
      "ic_launcher_foreground.png",
      "ic_launcher_background.png",
    ]) {
      const p = path.join(anydpiDir, f);
      try { if (fs.existsSync(p)) fs.rmSync(p, { force: true }); } catch {}
    }
  }

  // B) 清理“同名 xml + png”冲突（这是 Duplicate resources 的最常见根因）
  //    例如：你曾经生成过 mipmap-anydpi-v26/ic_launcher.png 或 drawable/ic_launcher_foreground.png 等
  for (const dirent of fs.readdirSync(resDir, { withFileTypes: true })) {
    if (!dirent.isDirectory()) continue;

    const d = path.join(resDir, dirent.name);

    // 只处理 drawable* 与 mipmap*（避免误删其它资源）
    if (!dirent.name.startsWith("drawable") && !dirent.name.startsWith("mipmap")) continue;

    const names = ["ic_launcher", "ic_launcher_round", "ic_launcher_foreground", "ic_launcher_background"];
    for (const n of names) {
      const xml = path.join(d, `${n}.xml`);
      const png = path.join(d, `${n}.png`);

      // 规则：如果同名 xml 与 png 同时存在 -> 删除 png（保留 xml）
      // 这样可确保你模板已有 xml 不会和 png 冲突；真正需要 png 的目录我们会在下方明确写入
      if (fs.existsSync(xml) && fs.existsSync(png)) {
        try {
          fs.rmSync(png, { force: true });
          warn(`已清理冲突资源：${png}（保留 ${xml}）`);
        } catch (e) {
          warn(`清理冲突资源失败：${png} -> ${e.message}`);
        }
      }
    }
  }

  // C) 只往 mipmap-*dpi 写 PNG（这才符合你现在 manifest: android:icon="@mipmap/ic_launcher" 的引用方式）
  const mipmapBases = ["mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi"];
  const mipmapDirs = mipmapBases.map(b => path.join(resDir, b));
  for (const d of mipmapDirs) {
    try { fs.mkdirSync(d, { recursive: true }); } catch {}
  }

  // 关键点：为了实现“上传一张图 -> adaptive icon 背景铺满”，我们把这张图同时写入 foreground/background
  // 这样 launcher 的 mask 不会露出一圈纯色底（你之前那圈就是 background 纯色导致的观感差异）
  const targets = [
    "ic_launcher.png",
    "ic_launcher_round.png",
    "ic_launcher_foreground.png",
    "ic_launcher_background.png",
  ];

  for (const dir of mipmapDirs) {
    for (const f of targets) {
      const target = path.join(dir, f);
      try {
        fs.copyFileSync(pngPath, target);
      } catch (e) {
        warn(`写入图标失败: ${dir}/${f} -> ${e.message}`);
      }
    }
  }

  return true;
}


console.log("[NDJC-assembly] 写入 App 图标:", iconPathFromJson ? iconPngPath : `${iconPngPath} (fallback)`);
writeLauncherIcons(iconPngPath);
// 把 adaptive icon 的 foreground/background 都指向 @mipmap（与我们写入的 png 目录一致）
patchAdaptiveIconXmlFile(
  "templates/Core-Templates/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
  "@mipmap/ic_launcher_foreground",
  "@mipmap/ic_launcher_background"
);
patchAdaptiveIconXmlFile(
  "templates/Core-Templates/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml",
  "@mipmap/ic_launcher_foreground",
  "@mipmap/ic_launcher_background"
);


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

// ① 注入唯一 applicationId（每个 App 唯一；同一 App 后续复用）
appGradleContent = replaceBlock(
  appGradleContent,
  "// NDJC-AUTO-APPID-START",
  "// NDJC-AUTO-APPID-END",
  `applicationId = "${packageName}"`
);
console.log("[NDJC-assembly] 已更新 app/build.gradle.kts 的 NDJC-AUTO-APPID 区域:", packageName);

// ② 注入依赖组合（模块 + UI 包）
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
console.log("  - App 图标已写入 res/mipmap-*/ic_launcher_foreground/background(.png) 以及 ic_launcher(.png) / ic_launcher_round(.png)");
console.log("  - settings.gradle.kts 已根据 assembly.local.json 更新");
console.log("  - app/build.gradle.kts 已根据 assembly.local.json 更新");
console.log("  - 契约自检已执行（ndjc-sst-checker.js）");
console.log();
// ----- NDJC publish info (for handoff) -----
const PUBLISH_INFO = "lib/ndjc/publish-info.txt";
try {
  const info =
`NDJC Publish Info
- appLabel: ${appLabel}
- applicationId: ${packageName}
- template: ${templateId}
- uiPack: ${uiPackId}
- modules: ${modules.join(", ")}

(For Google Play)
- Upload Key: provided separately in artifact (ndjc-upload-keystore.jks) if enabled in workflow
- versionCode/versionName: from Gradle
`;
  writeText(PUBLISH_INFO, info);
  console.log("[NDJC-assembly] 已生成:", PUBLISH_INFO);
} catch (e) {
  warn("生成 publish-info.txt 失败（非致命）：" + e.message);
}
console.log("现在可以运行 Gradle 构建，例如：");
console.log("  ./gradlew assembleDebug");
NODE
