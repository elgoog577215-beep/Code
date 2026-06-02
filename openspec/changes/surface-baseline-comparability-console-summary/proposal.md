## Why

live eval baseline regression report 已经写出 `comparabilityStatus`，但测试控制台仍主要提示 report 路径和 gate pass/fail。当前真实报告出现 `status=PASSED` 且 `comparabilityStatus=NOT_COMPARABLE`，如果控制台不直接显示可比性，维护者容易误以为本次真实外部模型质量可用。

本变更把 baseline regression 可比性摘要打印到 assistant 和 live-model-eval 控制台输出，让人和脚本在第一眼就看到 pass/fail 与可比性是两件事。

## What Changes

- 为 `LiveEvalBaselineRegressionReport` 增加可复用的摘要格式方法，输出 `status`、`comparabilityStatus`、原因数量、对比 case 数和 violation 数。
- `AssistantLiveEvalTest` 写出 regression report 后打印可比性摘要。
- `ModelDiagnosisEvalTest` 写出 regression report 后打印可比性摘要。
- 补充结构测试覆盖摘要格式，确保 `PASSED + NOT_COMPARABLE` 会直接出现在摘要中。
- 不改变 baseline regression gate 的 fail/pass 断言，不改变 JSON 报告字段。

## Capabilities

### New Capabilities
- `baseline-comparability-console-summary`: 覆盖 live eval baseline regression 控制台输出必须展示 comparability 状态。

### Modified Capabilities
- `live-eval-baseline-comparability-status`: comparability 不只写入 JSON，还必须进入人类可见摘要。

## Impact

- 测试 DTO：`LiveEvalBaselineRegressionReport`
- assistant live eval：`AssistantLiveEvalTest`
- model live eval：`ModelDiagnosisEvalTest`
- 结构测试：`AssistantLiveEvalQualityGateTest`
- 验证：相关后端测试、OpenSpec strict validate、secret scan、`git diff --check`
