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

function ensureExists(p, desc) {
  if (!fs.existsSync(p)) {
    fail(`缺少 ${desc}：${p}`);
  }
}

function ensureNonEmptyFile(p, desc) {
  ensureExists(p, desc);
  const stat = fs.statSync(p);
  if (!stat.isFile()) {
    fail(`${desc} 不是文件：${p}`);
  }
  if (stat.size <= 0) {
    fail(`${desc} 为空文件：${p}`);
  }
}

function readText(p) {
  return fs.readFileSync(p, "utf8");
}

function writeText(p, content) {
  fs.mkdirSync(path.dirname(p), { recursive: true });
  fs.writeFileSync(p, content, "utf8");
}

function removeDirChildren(dir) {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
    return;
  }
  for (const name of fs.readdirSync(dir)) {
    const full = path.join(dir, name);
    fs.rmSync(full, { recursive: true, force: true });
  }
}

function copyUiPackSourcesToFeatureUi(uiPackId, modules) {
  if (!modules.length) {
    warn("没有模块可注入 UI，跳过 UI 包复制");
    return;
  }

  const targetModule = modules[0];
  const uiPackDir = path.join(TEMPLATE_DIR, uiPackId);
  const featureUiDir = path.join(
    TEMPLATE_DIR,
    targetModule,
    "src",
    "main",
    "java",
    "com",
    "ndjc",
    "feature",
    "showcase",
    "ui"
  );

  if (!fs.existsSync(uiPackDir)) {
    fail(`找不到 UI 包目录：${uiPackDir}`);
  }

  const uiSourceFiles = fs.readdirSync(uiPackDir, { withFileTypes: true })
    .filter((d) => d.isFile())
    .map((d) => d.name)
    .filter((name) => {
      const lower = name.toLowerCase();
      return (
        lower.endsWith(".kt") ||
        lower.endsWith(".java")
      );
    });

  if (!uiSourceFiles.length) {
    fail(`UI 包目录下没有可复制的源码文件：${uiPackDir}`);
  }

  removeDirChildren(featureUiDir);

  for (const fileName of uiSourceFiles) {
    const src = path.join(uiPackDir, fileName);
    const dst = path.join(featureUiDir, fileName);
    fs.copyFileSync(src, dst);
  }

  console.log("[NDJC-assembly] 已将 UI 包源码复制到逻辑模块 UI 目录:");
  console.log("  from:", uiPackDir);
  console.log("  to  :", featureUiDir);
  console.log("  files:", uiSourceFiles.join(", "));
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

function injectStoreIdIntoModuleSources(modules, injectedStoreId) {
  if (!modules.length) {
    warn("没有模块可注入 storeId，跳过 storeId 锚点注入");
    return;
  }

  for (const moduleName of modules) {
    const moduleJavaDir = path.join(
      TEMPLATE_DIR,
      moduleName,
      "src",
      "main",
      "java"
    );

    if (!fs.existsSync(moduleJavaDir)) {
      warn(`模块源码目录不存在，跳过 storeId 注入：${moduleJavaDir}`);
      continue;
    }

    const targetFiles = [];

    function walk(dir) {
      const entries = fs.readdirSync(dir, { withFileTypes: true });
      for (const entry of entries) {
        const fullPath = path.join(dir, entry.name);
        if (entry.isDirectory()) {
          walk(fullPath);
          continue;
        }
        if (!entry.isFile()) continue;

        const lower = entry.name.toLowerCase();
        if (lower.endsWith(".kt") || lower.endsWith(".java")) {
          targetFiles.push(fullPath);
        }
      }
    }

    walk(moduleJavaDir);

    let replacedCount = 0;

    for (const filePath of targetFiles) {
      const original = readText(filePath);
      if (!original.includes("__NDJC_STORE_ID__")) continue;

      const updated = original.replaceAll("__NDJC_STORE_ID__", injectedStoreId);
      writeText(filePath, updated);
      replacedCount += 1;
    }

    if (replacedCount <= 0) {
      warn(`未在模块源码中找到 __NDJC_STORE_ID__ 锚点：${moduleName}`);
    } else {
      console.log("[NDJC-assembly] 已注入 storeId 到逻辑模块源码:");
      console.log("  module :", moduleName);
      console.log("  storeId:", injectedStoreId);
      console.log("  files  :", replacedCount);
    }
  }
}

function escapeKotlinString(v) {
  return String(v)
    .replace(/\\/g, "\\\\")
    .replace(/"/g, '\\"')
    .replace(/\$/g, "\\$")
    .replace(/\n/g, "\\n");
}

function injectBuildStringsIntoModuleSources(modules, replacements) {
  if (!modules.length) {
    warn("没有模块可注入构建字符串，跳过源码锚点注入");
    return;
  }

  for (const moduleName of modules) {
    const moduleJavaDir = path.join(
      TEMPLATE_DIR,
      moduleName,
      "src",
      "main",
      "java"
    );

    if (!fs.existsSync(moduleJavaDir)) {
      warn(`模块源码目录不存在，跳过构建字符串注入：${moduleJavaDir}`);
      continue;
    }

    const targetFiles = [];

    function walk(dir) {
      const entries = fs.readdirSync(dir, { withFileTypes: true });
      for (const entry of entries) {
        const fullPath = path.join(dir, entry.name);
        if (entry.isDirectory()) {
          walk(fullPath);
          continue;
        }
        if (!entry.isFile()) continue;

        const lower = entry.name.toLowerCase();
        if (lower.endsWith(".kt") || lower.endsWith(".java")) {
          targetFiles.push(fullPath);
        }
      }
    }

    walk(moduleJavaDir);

    let replacedCount = 0;

    for (const filePath of targetFiles) {
      const original = readText(filePath);
      let updated = original;
      let touched = false;

      for (const [token, value] of Object.entries(replacements)) {
        if (!updated.includes(token)) continue;
        updated = updated.replaceAll(token, value);
        touched = true;
      }

      if (!touched) continue;

      writeText(filePath, updated);
      replacedCount += 1;
    }

    if (replacedCount <= 0) {
      warn(`未在模块源码中找到构建字符串锚点：${moduleName}`);
    } else {
      console.log("[NDJC-assembly] 已注入构建字符串到模块源码:");
      console.log("  module :", moduleName);
      console.log("  files  :", replacedCount);
      console.log("  keys   :", Object.keys(replacements).join(", "));
    }
  }
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
// ---------- 2.1) 生成运行时 assembly.json（供 Android Studio / 运行时读取）----------
// 目标：让 app/src/main/assets/assembly/assembly.json 永远由 assembly.local.json 派生，避免本地/线上分裂
const RUNTIME_ASSEMBLY_JSON = "templates/Core-Templates/app/src/main/assets/assembly/assembly.json";

try {
  const firstModule = modules[0] || "__default__";

  // 运行时最小可用结构：template/uiPack/modules/slots/startRoute
  // slots 先按“一个模块兜底”生成，后续你要做更复杂的 slots 再扩展
  const runtimeAssembly = {
    template: templateId,
    uiPack: uiPackId,
    modules,
    slots: {
      home: {
        hero: firstModule,
        primary: firstModule
      },
      detail: {
        detail: firstModule
      }
    },
    startRoute: "home"
  };

  writeText(RUNTIME_ASSEMBLY_JSON, JSON.stringify(runtimeAssembly, null, 2));
  console.log("[NDJC-assembly] 已生成运行时 assembly.json:", RUNTIME_ASSEMBLY_JSON);
} catch (e) {
  warn(`生成运行时 assembly.json 失败（不影响本次构建）：${e.message}`);
}

// ✅ 新增：App 名称
const appLabel = (assembly.appName || assembly.app_label || "NDJC App").toString().trim();
const storeId = (assembly.storeId || assembly.store_id || "").toString().trim();
const merchantEmail = (assembly.merchantEmail || assembly.adminName || "").toString().trim().toLowerCase();
const privacyUrl = (assembly.privacyUrl || "").toString().trim();

if (!storeId) {
  fail("assembly.local.json 缺少 storeId，无法注入逻辑模块锚点");
}
// ===== NDJC: Versioning (per App) =====
// 约定：assembly.local.json 中包含 versionCode/versionName
let versionCode = Number(assembly.versionCode);
let versionName = (assembly.versionName || "").toString().trim();

// 缺省策略：若未提供，则默认 v1，并写回（方便后续“更新构建”沿用并递增）
if (!Number.isInteger(versionCode) || versionCode <= 0) {
  versionCode = 1;
  assembly.versionCode = versionCode;
  if (!versionName) {
    versionName = "1.0.0";
    assembly.versionName = versionName;
  }
  try {
    writeText(ASSEMBLY_JSON, JSON.stringify(assembly, null, 2));
    console.log("[NDJC-assembly] 生成并写回默认 versionCode/versionName:", versionCode, versionName);
  } catch (e) {
    warn(`写回 assembly.local.json 失败（不影响本次构建）：${e.message}`);
  }
} else {
  // versionName 可空；空则给一个与 versionCode 不冲突的默认显示值
  if (!versionName) versionName = "1.0.0";
  console.log("[NDJC-assembly] 复用 assembly 中的 versionCode/versionName:", versionCode, versionName);
}
// ===== END NDJC: Versioning =====


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
  // 最后一段必须以字母开头，避免出现 ".2181ca" 这种不合法段
  return `com.ndjc.apps.${slug}.a${rand}`;
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
  ensureFile(xmlPath, "adaptive icon xml");
  let x = readText(xmlPath);

  x = x.replace(
    /<foreground>\s*<inset[^>]*android:drawable="[^"]*"[^>]*\/>\s*<\/foreground>/m,
    `<foreground>\n        <inset android:drawable="${foregroundRef}" android:inset="0%"/>\n    </foreground>`
  );
  x = x.replace(
    /<background>\s*<inset[^>]*android:drawable="[^"]*"[^>]*\/>\s*<\/background>/m,
    `<background>\n        <inset android:drawable="${backgroundRef}" android:inset="0%"/>\n    </background>`
  );
  x = x.replace(/android:drawable="[^"]*ic_launcher_foreground[^"]*"/g, `android:drawable="${foregroundRef}"`);
  x = x.replace(/android:drawable="[^"]*ic_launcher_background[^"]*"/g, `android:drawable="${backgroundRef}"`);

  writeText(xmlPath, x);

  const patched = readText(xmlPath);
  const hasForeground = patched.includes(`android:drawable="${foregroundRef}"`);
  const hasBackground = patched.includes(`android:drawable="${backgroundRef}"`);

  if (!hasForeground) {
    fail(`adaptive icon xml foreground 引用缺失：${xmlPath}`);
  }

  if (!hasBackground) {
    fail(`adaptive icon xml background 引用缺失：${xmlPath}`);
  }

  console.log("[NDJC-assembly] adaptive icon xml 已满足要求:", xmlPath);
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

function normalizeLauncherIconSource(pngPath) {
  ensureNonEmptyFile(pngPath, "源图标文件");

  const normalizedPath = path.join("lib", "ndjc", "icon-normalized.png");
  const result = spawnSync(
    "convert",
    [
      pngPath,
      "-auto-orient",
      "-gravity", "center",
      "-resize", "1024x1024^",
      "-extent", "1024x1024",
      "PNG32:" + normalizedPath,
    ],
    {
      stdio: "inherit",
    }
  );

  if (result.error) {
    fail(`图标规范化失败：${result.error.message}`);
  }

  if (typeof result.status === "number" && result.status !== 0) {
    fail(`图标规范化失败，退出码：${result.status}`);
  }

  ensureNonEmptyFile(normalizedPath, "规范化后的图标文件");
  console.log("[NDJC-assembly] 已生成铺满容器用的正方形图标:", normalizedPath);
  return normalizedPath;
}

function writeAdaptiveTransparentForegroundDrawable(resDir) {
  const drawableDir = path.join(resDir, "drawable");
  fs.mkdirSync(drawableDir, { recursive: true });

  const target = path.join(drawableDir, "ndjc_adaptive_foreground_transparent.xml");
  const content = `<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@android:color/transparent" />
</shape>
`;

  writeText(target, content);
  ensureNonEmptyFile(target, "adaptive foreground 透明 drawable xml");
  console.log("[NDJC-assembly] 已写入 adaptive foreground 透明 drawable:", target);
  return true;
}
function writeLauncherIcons(pngPath) {
  const ROOT = process.cwd();
  const resDir = path.join(ROOT, "templates/Core-Templates/app/src/main/res");

  ensureNonEmptyFile(pngPath, "源图标文件");

  if (!fs.existsSync(resDir)) {
    fail(`找不到 res 目录：${resDir}`);
  }

  const normalizedIconPath = normalizeLauncherIconSource(pngPath);

  const anydpiDir = path.join(resDir, "mipmap-anydpi-v26");
  if (fs.existsSync(anydpiDir)) {
    for (const f of [
      "ic_launcher.png",
      "ic_launcher_round.png",
      "ic_launcher_foreground.png",
      "ic_launcher_background.png",
    ]) {
      const p = path.join(anydpiDir, f);
      if (fs.existsSync(p)) {
        fs.rmSync(p, { force: true });
        console.log("[NDJC-assembly] 已删除 anydpi 非法 PNG:", p);
      }
    }
  }

  for (const dirent of fs.readdirSync(resDir, { withFileTypes: true })) {
    if (!dirent.isDirectory()) continue;

    const d = path.join(resDir, dirent.name);

    if (!dirent.name.startsWith("drawable") && !dirent.name.startsWith("mipmap")) continue;

    const names = ["ic_launcher", "ic_launcher_round", "ic_launcher_foreground", "ic_launcher_background"];
    for (const n of names) {
      const xml = path.join(d, `${n}.xml`);
      const png = path.join(d, `${n}.png`);

      if (fs.existsSync(xml) && fs.existsSync(png)) {
        fs.rmSync(png, { force: true });
        console.log("[NDJC-assembly] 已清理冲突资源:", png);
      }
    }
  }

  const mipmapBases = ["mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi"];
  const mipmapDirs = mipmapBases.map((b) => path.join(resDir, b));
  for (const d of mipmapDirs) {
    fs.mkdirSync(d, { recursive: true });
  }

const targets = [
  "ic_launcher.png",
  "ic_launcher_round.png",
  "ic_launcher_background.png",
];

  const writtenTargets = [];

  for (const dir of mipmapDirs) {
    for (const f of targets) {
      const target = path.join(dir, f);
      fs.copyFileSync(normalizedIconPath, target);
      writtenTargets.push(target);
      console.log("[NDJC-assembly] 已写入图标:", target);
    }
  }

  for (const target of writtenTargets) {
    ensureNonEmptyFile(target, "已写入的 launcher 图标");
  }

  console.log("[NDJC-assembly] Launcher 图标写入完成，共写入文件数:", writtenTargets.length);
  return true;
}


console.log("[NDJC-assembly] 写入 App 图标:", iconPathFromJson ? iconPngPath : `${iconPngPath} (fallback)`);

ensureNonEmptyFile(iconPngPath, "图标源文件");

const wroteLauncherIcons = writeLauncherIcons(iconPngPath);
if (!wroteLauncherIcons) {
  fail(`写入 launcher 图标失败：${iconPngPath}`);
}

const wroteAdaptiveTransparentForegroundDrawable = writeAdaptiveTransparentForegroundDrawable(RES_DIR);
if (!wroteAdaptiveTransparentForegroundDrawable) {
  fail("写入 adaptive foreground 透明 drawable 失败");
}

const patchedLauncherXml = patchAdaptiveIconXmlFile(
  "templates/Core-Templates/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
  "@drawable/ndjc_adaptive_foreground_transparent",
  "@mipmap/ic_launcher_background"
);
if (!patchedLauncherXml) {
  fail("修补 ic_launcher.xml 失败");
}

const patchedLauncherRoundXml = patchAdaptiveIconXmlFile(
  "templates/Core-Templates/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml",
  "@drawable/ndjc_adaptive_foreground_transparent",
  "@mipmap/ic_launcher_background"
);
if (!patchedLauncherRoundXml) {
  fail("修补 ic_launcher_round.xml 失败");
}

const launcherVerifyTargets = [
  "templates/Core-Templates/app/src/main/res/mipmap-mdpi/ic_launcher.png",
  "templates/Core-Templates/app/src/main/res/mipmap-hdpi/ic_launcher.png",
  "templates/Core-Templates/app/src/main/res/mipmap-xhdpi/ic_launcher.png",
  "templates/Core-Templates/app/src/main/res/mipmap-xxhdpi/ic_launcher.png",
  "templates/Core-Templates/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png",
  "templates/Core-Templates/app/src/main/res/mipmap-mdpi/ic_launcher_round.png",
  "templates/Core-Templates/app/src/main/res/mipmap-hdpi/ic_launcher_round.png",
  "templates/Core-Templates/app/src/main/res/mipmap-xhdpi/ic_launcher_round.png",
  "templates/Core-Templates/app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png",
  "templates/Core-Templates/app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png",
  "templates/Core-Templates/app/src/main/res/mipmap-mdpi/ic_launcher_background.png",
  "templates/Core-Templates/app/src/main/res/mipmap-hdpi/ic_launcher_background.png",
  "templates/Core-Templates/app/src/main/res/mipmap-xhdpi/ic_launcher_background.png",
  "templates/Core-Templates/app/src/main/res/mipmap-xxhdpi/ic_launcher_background.png",
  "templates/Core-Templates/app/src/main/res/mipmap-xxxhdpi/ic_launcher_background.png",
  "templates/Core-Templates/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
  "templates/Core-Templates/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml",
  "templates/Core-Templates/app/src/main/res/drawable/ndjc_adaptive_foreground_transparent.xml"
];

for (const verifyTarget of launcherVerifyTargets) {
  ensureNonEmptyFile(verifyTarget, "图标校验目标文件");
}

console.log("[NDJC-assembly] App 图标链路强校验通过（已启用 adaptive icon：background 铺满上传图，foreground 使用透明 drawable）");

// ---------- 2.7) 复制 UI 包源码到逻辑模块 UI 目录 ----------
copyUiPackSourcesToFeatureUi(uiPackId, modules);

// ---------- 2.8) 注入 storeId 到逻辑模块源码 ----------
injectStoreIdIntoModuleSources(modules, storeId);

// ---------- 2.9) 注入 About / Privacy 构建字符串到模块源码 ----------
injectBuildStringsIntoModuleSources(modules, {
  "__NDJC_APP_NAME__": escapeKotlinString(appLabel),
  "__NDJC_MERCHANT_EMAIL__": escapeKotlinString(merchantEmail),
  "__NDJC_PRIVACY_URL__": escapeKotlinString(privacyUrl),
});

// ---------- 3) 更新 settings.gradle.kts ----------
const moduleNames = ["app", "core-skeleton", ...modules];

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
  ...modules.map((m) => `    implementation(project(":${m}"))`)
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
// ①.5 注入版本号（用于 Play 更新：versionCode 必须递增）
appGradleContent = replaceBlock(
  appGradleContent,
  "// NDJC-AUTO-VERSION-START",
  "// NDJC-AUTO-VERSION-END",
  `versionCode = ${versionCode}\n        versionName = "${versionName}"`
);
console.log("[NDJC-assembly] 已更新 app/build.gradle.kts 的 NDJC-AUTO-VERSION 区域:", versionCode, versionName);


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
console.log("  - App 图标已按“居中裁正方形 + 铺满容器 + 允许边缘裁切”规则写入 res/mipmap-*/ic_launcher(.png) / ic_launcher_round(.png) / ic_launcher_background(.png)，并启用 adaptive icon xml（background 铺满上传图，foreground 使用透明 drawable）");
console.log("  - UI 包源码已复制到 feature 模块 ui 目录");
console.log(`  - storeId 已注入逻辑模块源码：${storeId}`);
console.log("  - settings.gradle.kts 已根据 assembly.local.json 更新（不再 include UI 包）");
console.log("  - app/build.gradle.kts 已根据 assembly.local.json 更新（不再依赖 UI 包 Gradle module）");
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
- Release AAB: (see artifact *.aab)
- Test APK: (see artifact *.apk)
- Upload Keystore: lib/ndjc/ndjc-upload-keystore.jks (in artifact)
- Signing details: lib/ndjc/signing-info.txt (in artifact)
- versionCode/versionName: from Gradle (v1 default)
`;

  writeText(PUBLISH_INFO, info);
  console.log("[NDJC-assembly] 已生成:", PUBLISH_INFO);
} catch (e) {
  warn("生成 publish-info.txt 失败（非致命）：" + e.message);
}
console.log("现在可以运行 Gradle 构建，例如：");
console.log("  ./gradlew assembleDebug");
NODE
