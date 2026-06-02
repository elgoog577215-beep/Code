## Why

当前离线教育 fixture 已经较多，但真实 `live-model-eval` 的可比较核心样本不足。最近报告仍是 `0 completed + fallback + NOT_COMPARABLE`，说明下一阶段需要先建立小而稳定的 live-model core set，而不是盲目扩大普通离线样本。

现有 `assistant-live-cases.json` 已包含一批高价值提交诊断样本，覆盖输入读取、状态重置、DP 状态设计、贪心反例、复杂度、空输入、原地状态推进、输出格式、样例过拟合等教育错因。它们还没有作为 live-model diagnosis baseline 的独立核心集合进入 `ModelDiagnosisEvalTest`。

本变更把这些样本沉淀为 `live-model-core-cases.json`，接入 live-model eval，并提供 case id 过滤，让真实 ModelScope smoke 可以稳定跑核心样本子集，逐步把 baseline 从 `NOT_COMPARABLE` 推向 `PARTIAL/COMPARABLE`。

## What Changes

- 新增 `diagnosis-eval-fixtures/live-model-core-cases.json`，首批收录 6 条可比较核心诊断样本。
- 新增 live-model core fixture loader，把 JSON 转成 `ModelDiagnosisEvalTest` 的 `EvalCase`。
- `ModelDiagnosisEvalTest#allEvalCases()` 纳入 live-model core set。
- live model smoke 支持 `AI_EVAL_CASE_IDS` 过滤，便于只跑核心样本或 recovery smoke。
- 补充结构测试，验证 core set 规模、标签、证据、case id 唯一性和过滤能力。
- 不改变外部模型 prompt、诊断逻辑或 baseline gate 规则。

## Capabilities

### New Capabilities

- `live-model-core-eval-fixtures`: live-model eval 必须有可审计、可过滤、可 baseline 的核心诊断样本集。

### Modified Capabilities

- `external-model-call-stability`: live-model smoke 不只支持样本间延迟，也支持按 case id 选择小批量核心样本。

## Impact

- 新增 fixture：`live-model-core-cases.json`
- 新增 loader：`LiveModelCoreEvalFixtureLoader`
- 更新测试：`ModelDiagnosisEvalTest`
- 验证：相关后端测试、OpenSpec strict validate、secret scan、`git diff --check`，可行时运行真实 ModelScope 小样本 smoke
