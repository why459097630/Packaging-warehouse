#!/usr/bin/env bash
set -euo pipefail

# ---------- 基础准备 ----------
rm -rf build-logs || true
mkdir -p build-logs
echo "[INFO] Starting NDJC preflight..." | tee build-logs/summary.txt

# 强制使用远端模板/干净工作区（可选）
# git clean -xfd || true

# ---------- 统一打印环境 ----------
{
  echo "==== ENV ====="
  ./gradlew -v || true
  java -version || true
  echo "which aapt2: $(command -v aapt2 || echo 'N/A')"
} > build-logs/env.txt 2>&1

# ---------- 1) 资源合并（不调用 aapt 链接），暴露重复/冲突 ----------
./gradlew :app:mergeReleaseResources \
  --stacktrace --info --warning-mode=all --no-daemon -Dorg.gradle.console=plain \
  2>&1 | tee build-logs/merge-resources.log || true

# ---------- 2) Android Lint（会把所有问题列出来，不止第一条） ----------
./gradlew :app:lintRelease \
  --stacktrace --info --warning-mode=all --no-daemon -Dorg.gradle.console=plain \
  2>&1 | tee build-logs/lint.log || true

# ---------- 3) Kotlin/Java 编译预检（不依赖 aapt 链接） ----------
./gradlew :app:compileReleaseKotlin :app:compileReleaseJavaWithJavac \
  --stacktrace --info --warning-mode=all --no-daemon -Dorg.gradle.console=plain \
  2>&1 | tee build-logs/compile.log || true

# ---------- 4) AAPT 资源链接（会暴露所有资源引用错误/Manifest 合并） ----------
# 用 --continue 让 Gradle 其它可跑的任务继续，从而产出更多错误信息。
./gradlew --continue :app:processReleaseResources \
  --stacktrace --info --warning-mode=all --no-daemon -Dorg.gradle.console=plain \
  2>&1 | tee build-logs/aapt-link.log || true

# ---------- 5) 自定义“缺失资源扫描” ：一次性找出所有 @xxx/yyy 未定义 ----------
python <<'PY' > build-logs/missing-resources.txt
import os, re, sys, pathlib
root = pathlib.Path("app/src/main/res")
# 定义型资源（values/*.xml）
def_defs = {"color":set(),"string":set(),"dimen":set(),"style":set(),"bool":set(),"integer":set(),"array":set(),"plurals":set()}
for p in root.rglob("values*/**/*.xml"):
    try:
        s = p.read_text(encoding="utf-8", errors="ignore")
    except Exception:
        continue
    for t in def_defs.keys():
        for m in re.finditer(rf"<{t}\s+name=\"([^\"]+)\"", s):
            def_defs[t].add(m.group(1))

# 文件型资源（drawable/mipmap/layout/xml 等等）
file_types = ["drawable","mipmap","layout","xml","anim","animator","menu","font","raw","transition","navigation"]
file_defs = {t:set() for t in file_types}
for t in file_types:
    for p in root.rglob(f"{t}*/*"):
        if p.is_file():
            name = p.stem  # 不含扩展名
            file_defs[t].add(name)

# 收集所有引用（在 res 与 kotlin/java 里一起扫）
use_refs = {}
ref_kinds = list(def_defs.keys())+file_types+["id"]
for kind in ref_kinds:
    use_refs[kind]=set()

def collect_refs(path):
    try:
        s = path.read_text(encoding="utf-8", errors="ignore")
    except Exception:
        return
    for kind in ref_kinds:
        for m in re.finditer(rf"@{kind}/([A-Za-z0-9_]+)", s):
            use_refs[kind].add(m.group(1))

proj = pathlib.Path(".")
for p in proj.rglob("app/src/**/*.xml"):
    collect_refs(p)
for p in proj.rglob("app/src/**/*.kt"):
    collect_refs(p)
for p in proj.rglob("app/src/**/*.java"):
    collect_refs(p)

missing = []
def report(kind, used_set, defined_set):
    miss = sorted(used_set - defined_set)
    if miss:
        missing.append(f"[MISSING {kind}] {len(miss)} item(s): " + ", ".join(miss))

# 对 values 系列
for k in def_defs.keys():
    report(k, use_refs.get(k,set()), def_defs[k])

# 对文件型
for k in file_types:
    report(k, use_refs.get(k,set()), file_defs[k])

# id 特殊：常见是 view id，不强制要求必须定义（多数来自 <View android:id=… 会自动生成）
# 如果你希望严格，也可把 id 也写入 report。

print("\n".join(missing) if missing else "No missing resources detected by static scan.")
PY

# ---------- 6) 依赖/重复类 快速体检 ----------
./gradlew :app:dependencies > build-logs/deps.txt 2>&1 || true
./gradlew :app:androidDependencies >> build-logs/deps.txt 2>&1 || true

# R8/重复类一般要到打包期才出现，这里先做一次 assemble（允许失败，拿日志）
./gradlew --continue :app:assembleRelease \
  --stacktrace --info --warning-mode=all --no-daemon -Dorg.gradle.console=plain \
  2>&1 | tee build-logs/assemble.log || true

# ---------- 7) 汇总关键错误到一屏 ----------
echo "==== CRITICALS (grep) =====" | tee -a build-logs/summary.txt
grep -nE "Android resource linking failed|AAPT|Duplicate class|Manifest merger|error:|What went wrong|FAILED" \
  build-logs/*.log || echo "No critical errors found." | tee -a build-logs/summary.txt

echo "==== Missing resources (static scan) ====" | tee -a build-logs/summary.txt
cat build-logs/missing-resources.txt | tee -a build-logs/summary.txt

echo "[INFO] Preflight done. See build-logs/summary.txt and full logs."
# 最后：如果有关键错误再退出非零码，避免“只报第一条”
if grep -qE "Android resource linking failed|AAPT|Duplicate class|Manifest merger| error:" build-logs/*.log; then
  exit 1
fi
if ! grep -q "No missing resources detected" build-logs/missing-resources.txt; then
  exit 2
fi
exit 0
