# Tokens Keys Conformance (Module: feature-demo)

本模块所使用的 Token Keys 均来自 SST (`ndjc-sst-v1@1.0.0`) 中定义的 domains & keys，
不额外发明新 key。

## 1. 一致性要求

- 所有 `color.*` / `space.*` / `radius.*` / `elevation.*` / `typography.*` / `opacity.*` key
  必须出现在 SST 的 `tokens.domains` 定义中。
- 若未来 SST 扩展新的 domain 或 key，本模块可选择性跟进。

## 2. 检查建议

- 通过静态分析或脚本扫描模块代码中出现的 token key 字符串。
- 将扫描结果与 SST tokens 列表进行比对，确保全集合满足：
  `moduleTokens ⊆ sstTokens`。

> 一旦发现模块使用了 SST 中不存在的 key，应视为构建前错误并阻止发布。
