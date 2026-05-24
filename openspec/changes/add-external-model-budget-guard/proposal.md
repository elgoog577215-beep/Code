## Why

上轮完整 6 条 assistant live eval 暴露了新的在线瓶颈：前两条提交诊断可以正常调用外部模型，但连续覆盖 Coach 和成长报告时，ModelScope 返回 `insufficient_quota` 和 `rate limit`，导致后续助手走兜底。

这说明当前 agent 还缺少“外部模型预算/限流感知”。如果供应商已经明确返回额度或限流错误，后续仍继续调用同一模型，会浪费请求、拉高延迟、污染评测报告，并让老师误以为是教学质量失败。

## What Changes

- 新增统一的外部模型调用错误分类，稳定识别 quota、rate limit、timeout、unsupported model、invalid response 等失败。
- 新增短期预算保护器：当同一 JVM 内连续出现额度或限流错误时，后续 AI 助手可以快速短路到可解释兜底，不再继续撞供应商接口。
- 让提交诊断、Coach、成长报告共享同一种供应商失败分类和人类可读原因。
- 让 live eval 报告能区分“预算/限流导致未调用”和“模型完成但教学质量不足”。
- 补充单元测试，防止额度错误被误判为普通质量 miss。

## Capabilities

### New Capabilities

- `external-model-budget-guard`: 定义外部模型供应商错误分类、短期预算保护和评测报告归因。

### Modified Capabilities

- `online-education-agent-quality`: 将 live eval 中的预算/限流失败纳入质量解释，而不是教学质量失败。
- `external-model-education-agent-runtime`: 在外部模型调用前加入预算保护判断。

## Impact

- 后端 AI 调用链：`AiReportService`、`CoachAgentService`、成长报告增强路径。
- 新增共享组件：外部模型调用失败分类与预算保护。
- 评测链路：assistant live eval 失败原因与迭代建议。
- 测试：模型调用稳定性、Coach 兜底、assistant eval 报告。
