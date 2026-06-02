## Why

live-model-eval 已经区分了真实模型命中和规则 fallback 命中，但质量 baseline draft 与 regression gate 仍消费 `expectedIssueTagHit/expectedFineTagHit` 这类最终命中 token。为了让后续模型或 prompt 回归测试真正约束外部模型能力，需要让模型质量基线只来自 `modelIssueTagHit/modelFineTagHit`。

## What Changes

- live-model-eval quality baseline draft 只在 `modelCompleted=true` 且模型命中 expected tags 时生成。
- model baseline 的 expected signals / mustKeep 使用 `modelIssueTagHit`、`modelFineTagHit` token，而不是最终命中 token。
- model baseline regression gate 用 model hit 字段判断 issue/fine 回归。
- fallback-only 命中不得生成模型质量 baseline，也不得通过模型质量 regression gate。
- 保持 assistant live eval baseline 行为不变。

## Capabilities

### New Capabilities

- `live-eval-model-hit-baselines`: live model eval 质量基线和回归门禁使用真实模型命中信号。

### Modified Capabilities

- `live-eval-model-fallback-hit-separation`: 新增的 model/fallback hit 字段应被 baseline draft 与 regression gate 消费。

## Impact

- 测试 factory：`LiveEvalQualityBaselineDraftFactory`
- 测试 gate：`LiveEvalBaselineRegressionGate`
- 测试：`AssistantLiveEvalQualityGateTest`
- 非目标：不改变 live eval report 生成、不改变 assistant baseline、不改变真实 ModelScope 调用。
