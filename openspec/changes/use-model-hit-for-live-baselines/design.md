## Context

`separate-live-eval-model-fallback-hits` 已让 live-model-eval report 区分最终命中、模型命中和 fallback 命中。审计发现 baseline draft factory 与 regression gate 仍使用 `expectedIssueTagHit/expectedFineTagHit` 作为模型质量 token。这两个字段现在代表最终诊断命中，可能来自规则 fallback。

如果基线继续使用最终命中 token，额度受限或 provider 失败时的 fallback-only 命中仍可能被沉淀为“模型质量基线”。这会污染后续 prompt/runtime 回归判断。

## Goals / Non-Goals

**Goals:**

- live-model-eval 模型质量 baseline 只从真实模型命中生成。
- regression gate 用 `modelIssueTagHit/modelFineTagHit` 判断模型标签回归。
- fallback-only 命中不得生成模型质量 baseline，也不得满足模型 baseline。
- 保留 assistant baseline 行为不变。

**Non-Goals:**

- 不改变 live-model-eval report 字段。
- 不改变 runtime fixture draft。
- 不改变 baseline report 文件结构，只更新 token 语义。

## Decisions

### baseline candidate 使用 model hit

`modelBaselineCandidate` 改为要求 `modelCompleted=true`、无 fallback、latency healthy、evidence/safety 通过，并且至少一个 `modelIssueTagHit/modelFineTagHit=true`。

### baseline token 改名为 model hit

model expected signals 输出 `modelIssueTagHit`、`modelFineTagHit`。这让 mustKeep 自描述为“模型命中”，而不是“最终命中”。

### regression gate 兼容旧 token，但优先新 token

为了不让旧 baseline 立刻失效，gate 继续识别旧 `expectedIssueTagHit/expectedFineTagHit` token，但把它们映射到 model hit 字段校验。新 baseline 只生成 model hit token。

## Risks / Trade-offs

- [Risk] 旧 baseline 名称含 expected token。→ gate 兼容读取，但语义改为 model hit 校验。
- [Risk] 当前额度受限时没有新的 model baseline。→ 这是正确行为；fallback-only 结果不应成为模型质量基线。
- [Risk] 如果历史报告缺少 model hit 字段，旧 baseline gate 会判定缺失。→ 需要用新报告重新生成基线，或接受旧报告不足以证明真实模型命中。
