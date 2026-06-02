## Why

live eval 现在已经能导出成功样本的 `qualityBaselineDrafts`，但这些 baseline 仍只是报告内容，后续运行不会自动判断同一个 case 是否退化。若换模型或改 prompt 后，某个原本成功的 case 失去标签命中、证据引用、安全通过或关键 mustKeep，当前质量门禁只能从汇总率间接发现，无法指出“相对哪条 baseline 退化了”。

本轮新增 baseline regression gate，让真实外部模型评测可以对照上一份 baseline 报告做逐 case 回归比较。

## What Changes

- 新增 test-scope 的 `LiveEvalBaselineRegressionGate`。
- 支持从上一份 assistant live eval JSON report 读取 `qualityBaselineDrafts`。
- 当前 assistant live eval 生成报告后，可通过 `AI_EVAL_BASELINE_REPORT` 开启 baseline regression gate。
- gate 按 caseId 比较当前 entry 与 baseline：完成状态、fallback、安全、信号命中、证据引用、教学动作和 mustKeep。
- 失败信息输出具体 caseId 与退化原因，例如 missing evidence、fallback regression、safety regression、missing mustKeep。
- 扩展无需 API Key 的结构测试，覆盖通过、退化和缺失 case。

## Capabilities

### New Capabilities

- `live-eval-baseline-regression-gate`: 覆盖 live eval 如何使用已导出的 quality baseline 草稿检测后续真实模型输出退化。

### Modified Capabilities

- 无。

## Impact

- 测试工具：新增 baseline regression gate。
- 评测链路：`AssistantLiveEvalTest` 在质量门禁后可选执行 baseline regression gate。
- 配置：新增环境变量 `AI_EVAL_BASELINE_REPORT`，指向上一份 assistant live eval JSON。
- 测试：扩展 `AssistantLiveEvalQualityGateTest`，覆盖 baseline regression。
- 生产代码：不改动生产服务。
