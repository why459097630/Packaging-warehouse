#!/usr/bin/env bash
# NDJC: 组合装配脚本（绝对路径版）
# 说明：
#   - 读取 lib/ndjc/assembly.local.json
#   - 运行 ndjc-sst-checker.js 自检
#   - 重写 settings.gradle.kts / app/build.gradle.kts 的 NDJC-AUTO 区域
#   - 物化 UI 包 / 模块并处理资源（图标等）

set -euo pipefail

# ---------------------------
# 路径（按你仓库约定）
# ---------------------------
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEMPLATE_DIR="${ROOT_DIR}/templates/Core-Templates"
ASSEMBLY_JSON="${ROOT_DIR}/lib/ndjc/assembly.local.json"
ICON_PNG="${ROOT_DIR}/lib/ndjc/icon.png"

# ---------------------------
# Node 脚本（内联）
# ---------------------------
node <<'NODE'
const fs = require("fs");
const path = require("path");

function warn(msg){ console.log(`[NDJC-assembly][WARN] ${msg}`); }

function readJson(p){
  return JSON.parse(fs.readFileSync(p, "utf8"));
}

function writeText(p, s){
  fs.mkdirSync(path.dirname(p), { recursive: true });
  fs.writeFileSync(p, s, "utf8");
}

/**
 * 写入 launcher 图标（最小且可靠）：
 * 1) 只写 res/mipmap-*（跳过 mipmap-anydpi-v26）
 *    - 避免与 drawable 下同名 xml 冲突（ic_launcher_foreground.xml vs .png）
 * 2) 强制覆盖 mipmap-anydpi-v26/ic_launcher(.xml) 入口，让它引用 @mipmap 的 foreground/background
 */
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

  // 只写入 mipmap-*（避免与 drawable 下同名 xml 资源冲突）
  const entries = fs.readdirSync(resDir, { withFileTypes: true })
    .filter(d => d.isDirectory() && d.name.startsWith("mipmap-") && d.name !== "mipmap-anydpi-v26")
    .map(d => path.join(resDir, d.name));

  if (!entries.length) {
    warn(`未找到任何 mipmap-* 目录：${resDir}（将继续使用模板默认图标）`);
    return false;
  }

  // 写入所有常见 launcher 图标文件名（含 adaptive icon 的前景/背景资源）
  const targets = [
    "ic_launcher.png",
    "ic_launcher_round.png",
    "ic_launcher_foreground.png",
    "ic_launcher_background.png",
  ];

  for (const dir of entries) {
    for (const f of targets) {
      const target = path.join(dir, f);
      try {
        fs.copyFileSync(pngPath, target);
      } catch (e) {
        warn(`写入图标失败: ${dir}/${f} -> ${e.message}`);
      }
    }
  }

  // 强制覆盖 adaptive icon 入口 xml：让 launcher 一定引用 @mipmap 的 foreground/background
  try {
    const anydpi = path.join(resDir, "mipmap-anydpi-v26");
    fs.mkdirSync(anydpi, { recursive: true });

    const adaptiveXml =
`<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
  <background android:drawable="@mipmap/ic_launcher_background"/>
  <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
</adaptive-icon>
`;

    fs.writeFileSync(path.join(anydpi, "ic_launcher.xml"), adaptiveXml, "utf8");
    fs.writeFileSync(path.join(anydpi, "ic_launcher_round.xml"), adaptiveXml, "utf8");
  } catch (e) {
    warn(`写入 adaptive icon xml 失败: ${e.message}`);
  }

  return true;
}

const ROOT_DIR = process.cwd();
const TEMPLATE_DIR = path.join(ROOT_DIR, "templates", "Core-Templates");
const ASSEMBLY_JSON = path.join(ROOT_DIR, "lib", "ndjc", "assembly.local.json");
const ICON_PNG = path.join(ROOT_DIR, "lib", "ndjc", "icon.png");

if (!fs.existsSync(ASSEMBLY_JSON)) {
  console.error(`[NDJC-assembly] Missing ${ASSEMBLY_JSON}`);
  process.exit(1);
}

const assembly = readJson(ASSEMBLY_JSON);
const appName = (assembly.appName || assembly.app_label || "NDJC App").toString();
console.log(`[NDJC-assembly] 使用组合：`);
console.log(`  template : ${assembly.template}`);
console.log(`  uiPack   : ${assembly.uiPack}`);
console.log(`  modules  : ${(assembly.modules||[]).join(", ") || "(none)"}`);
console.log(`[NDJC-assembly] 写入 App 名称：${appName}`);
console.log(`[NDJC-assembly] 写入 App 图标：${ICON_PNG}`);

const appResDir = path.join(TEMPLATE_DIR, "app", "src", "main", "res");
writeLauncherIcons(appResDir, ICON_PNG, assembly.iconBase64 || assembly.icon_base64 || "");

NODE

# 运行契约自检
echo "[NDJC-assembly] 运行契约自检（ndjc-sst-checker.js）..."
node scripts/ndjc-sst-checker.js

echo "[NDJC-assembly] 完成"
