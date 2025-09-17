#!/usr/bin/env bash
# NDJC - template self check
# 作用：
#  1) 统计仓库内 NDJC* 锚点数量（文本/Block 粗略统计）
#  2) 校验 anchors/expected_text.txt 与 anchors/expected_block.txt 中的锚点
#     是否都已经在模板目录 templates/core-template/app 中出现；
#     兼容 Windows CRLF 行尾。
#  3) （可选）若存在最近一次 requests/XX/03_apply_result.json 且安装 jq，
#     统计本次替换总量，辅助判断是否“打空包”。

set -euo pipefail

# --- 定位到仓库根目录（脚本位于 Tools/） ---
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")"/.. && pwd)" || exit 1
cd "$ROOT"

TEMPLATE_DIR="templates/core-template/app"
TEXT_LIST="anchors/expected_text.txt"
BLOCK_LIST="anchors/expected_block.txt"

# --- 彩色输出（可在不支持时自动退化为普通文本） ---
if [[ -t 1 ]] && command -v tput >/dev/null 2>&1; then
  green="$(tput setaf 2)"; red="$(tput setaf 1)"; yellow="$(tput setaf 3)"; blue="$(tput setaf 4)"
  bold="$(tput bold)"; reset="$(tput sgr0)"
else
  green=""; red=""; yellow=""; blue=""; bold=""; reset=""
fi

echo -e "${blue}${bold}== NDJC 自查: 仓库根 ==${reset}  $ROOT"
echo

# --- 粗略统计（仅供参考） ---
echo -e "${blue}${bold}== NDJC 锚点粗略统计（仅参考） ==${reset}"
TOTAL=$(git ls-files | xargs grep -RIn "NDJC:" -- 2>/dev/null | wc -l | tr -d ' ')
BLOCK=$(git ls-files | xargs grep -RIn "NDJC:BLOCK(" -- 2>/dev/null | wc -l | tr -d ' ')
TEXT=$(( TOTAL - BLOCK ))
printf "  TEXT≈%-6s  BLOCK≈%-6s  TOTAL≈%-6s\n\n" "$TEXT" "$BLOCK" "$TOTAL"

# --- 工具函数：按行读取 key；去掉 CR；忽略空行和以#开头的注释 ---
read_keys() {
  local file="$1"
  while IFS= read -r line || [[ -n "$line" ]]; do
    # 去除 Windows 回车
    line="${line%$'\r'}"
    # 去除两侧空白
    line="$(echo -n "$line" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')"
    # 忽略空行与注释
    [[ -z "$line" || "$line" =~ ^# ]] && continue
    printf '%s\n' "$line"
  done < "$file"
}

# --- 校验清单文件存在 ---
missing_any=0
for f in "$TEXT_LIST" "$BLOCK_LIST"; do
  if [[ ! -f "$f" ]]; then
    echo -e "${red}缺少清单文件：${f}${reset}"
    missing_any=1
  fi
done
if [[ $missing_any -ne 0 ]]; then
  echo -e "${red}${bold}✖ 缺少锚点清单文件，无法继续${reset}"
  exit 2
fi

# --- 校验 TEXT 锚点 ---
echo -e "${blue}${bold}== 校验 TEXT 锚点是否已在模板中布置 ==${reset}"
any_missing=0
while IFS= read -r key; do
  if ! grep -RIn -- "$key" "$TEMPLATE_DIR" >/dev/null 2>&1; then
    echo -e "  ${red}MISSING(text):${reset} $key"
    any_missing=1
  fi
done < <(read_keys "$TEXT_LIST")
if [[ $any_missing -eq 0 ]]; then
  echo -e "  ${green}✓ 所有 TEXT 锚点均已出现${reset}"
fi
echo

# --- 校验 BLOCK 锚点 ---
echo -e "${blue}${bold}== 校验 BLOCK 锚点是否已在模板中布置 ==${reset}"
any_missing=0
while IFS= read -r key; do
  if ! grep -RIn -- "$key" "$TEMPLATE_DIR" >/dev/null 2>&1; then
    echo -e "  ${red}MISSING(block):${reset} $key"
    any_missing=1
  fi
done < <(read_keys "$BLOCK_LIST")
if [[ $any_missing -eq 0 ]]; then
  echo -e "  ${green}✓ 所有 BLOCK 锚点均已出现${reset}"
fi
echo

# 如有缺失，直接给出提示并退出非 0（用于 CI）
if [[ $any_missing -ne 0 ]]; then
  echo -e "${red}${bold}✖ 还有锚点尚未落到模板，以上为缺项列表。${reset}"
  exit 2
fi

# --- （可选）统计最近一次 apply_result 替换数量 ---
echo -e "${blue}${bold}== （可选）最近一次替换统计 ==${reset}"
if command -v jq >/dev/null 2>&1; then
  latest="$(ls -dt requests/* 2>/dev/null | head -n1 || true)"
  if [[ -n "$latest" && -f "$latest/03_apply_result.json" ]]; then
    COUNT="$(jq '[ .[] | .changes[] | select(.replacedCount>0) | .replacedCount ] | add // 0' "$latest/03_apply_result.json")"
    echo -e "  最近目录：$latest"
    echo -e "  替换总数：${green}${COUNT}${reset}"
    [[ "${COUNT:-0}" -eq 0 ]] && echo -e "  ${yellow}提示：替换为 0，可能是空包风险${reset}"
  else
    echo -e "  未找到 requests/*/03_apply_result.json（可忽略）"
  fi
else
  echo -e "  未安装 jq，跳过替换统计（可安装 jq 获得更详细统计）"
fi

echo
echo -e "${green}${bold}✓ NDJC 自查完成${reset}"
