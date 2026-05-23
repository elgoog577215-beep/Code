## Why

完整真实外部评测显示，当前瓶颈不只是模型回答质量，而是连续调用外部模型时容易被 429、额度或限流打断，导致多数样本进入运行回退。学校上线场景下，高准确率必须同时包含高质量输出、稳定完成率和可解释失败归因。

## What Changes

- 新增外部模型调用稳定性能力：对 429、限流、额度和瞬时 API 错误做可配置重试与等待。
- 在助手 live eval 中增加样本间节流配置，避免评测本身制造突发流量。
- 统一提交诊断、Coach 追问、成长报告的外部调用失败归因，让报告能区分质量失败、限流失败、额度失败和安全失败。
- 保持安全边界不降低：重试只针对调用层失败，不绕过输出校验、安全门禁或答案泄露检查。

## Capabilities

### New Capabilities

- `external-model-call-stability`: 定义外部模型调用的重试、节流和失败归因要求。

### Modified Capabilities

- 无。

## Impact

- 后端 AI 调用：`AiReportService` 与 `CoachAgentService` 的 OpenAI-compatible 请求路径。
- 评测报告：`AssistantLiveEvalTest` 的样本间节流和失败归因。
- 测试：增加 429 重试、不可重试错误和评测节流相关回归测试。
