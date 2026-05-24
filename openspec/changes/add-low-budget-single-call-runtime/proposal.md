## Why

上轮真实 live eval 显示：提交诊断第一阶段可以成功，但第二阶段教学提示经常因为外部模型额度或限流失败。现有两阶段 runtime 每次诊断最多消耗两次外部模型调用，在额度紧张、班级并发或供应商限流时会明显放大失败概率。

下一步高杠杆改动是新增“低预算单次调用模式”：在需要节省调用次数时，用一次外部模型请求同时返回错因裁决和教学提示，再分别走现有诊断校验、教学提示校验和安全门禁。这样能保持结构化输出，同时减少一半调用次数。

## What Changes

- 新增单次调用 prompt 模板，要求模型同时输出 `diagnosisDecision` 和 `teachingHint`。
- 新增单次调用 payload 结构，并复用现有 `DiagnosisJudgeOutput`、`TeachingHintOutput` 校验逻辑。
- `AiReportService` 支持配置 `ai.external-runtime-mode`：
  - `staged`：现有两阶段模式。
  - `single-call`：低预算单次调用模式。
- 单次调用模式下，模型一次返回完整诊断和教学提示；任一部分校验失败时沿用现有兜底/部分完成策略。
- 补充测试，验证单次调用只调用一次外部模型，并且输出仍经过结构化校验。

## Capabilities

### New Capabilities

- `low-budget-single-call-runtime`: 定义低预算单次调用 runtime，减少外部模型请求次数。

### Modified Capabilities

- `external-model-budget-guard`: 在预算紧张场景下提供更节制的调用路径。
- `online-education-agent-quality`: live eval 可用于验证单次调用模式的质量和调用次数。

## Impact

- 后端 AI runtime：`PromptTemplateRegistry`、`ExternalModelStagePayloads`、`AiReportService`。
- 测试：外部模型 runtime 测试、prompt template 测试。
- 配置：新增 `ai.external-runtime-mode`，默认保持 `staged`，避免改变现有生产行为。
