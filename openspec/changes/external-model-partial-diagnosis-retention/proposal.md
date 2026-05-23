## Why

上一轮真实外部模型评测显示：提交诊断链路存在“阶段 A 错因裁决已成功，但阶段 B 教学提示因额度、安全或格式失败导致整条诊断回退”的情况。这样会浪费外部模型已经产出的高价值诊断判断，降低真实上线有效率。

本变更要提升外部模型接入后的实际准确率：只要模型的错因裁决阶段已经通过校验，就应尽量保留其错因、证据和置信度，再用本地安全教学模板补齐学生提示，而不是全部退回规则诊断。

## What Changes

- 新增“部分模型成功”处理路径：诊断阶段成功、教学阶段失败时，保留模型诊断结论。
- 用本地安全教学模板生成 `studentHintPlan`、`learningInterventionPlan`、`teacherNote` 和报告片段。
- 将 AI 调用状态标记为 `MODEL_PARTIAL_COMPLETED`，并保留失败阶段与失败原因。
- 在 live eval 报告中把部分成功计入可用模型结果，而不是运行失败。
- 增加回归测试，覆盖教学阶段 API 失败、教学阶段安全拒绝和完整成功路径。

## Capabilities

### New Capabilities

- `external-model-partial-diagnosis-retention`: 定义外部模型诊断阶段成功后的部分结果保留能力。

### Modified Capabilities

- 无。

## Impact

- 后端 AI 诊断链：`AiReportService` 的外部模型 runtime 编排。
- 评测报告：`AssistantLiveEvalTest` 对 `MODEL_PARTIAL_COMPLETED` 的完成度判定。
- 测试：`AiReportServiceExternalRuntimeTest` 和外部助手 live eval 相关测试。
