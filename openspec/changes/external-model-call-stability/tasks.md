## 1. 外部调用重试

- [x] 1.1 在 `AiReportService` 中新增可配置重试次数和等待时间。
- [x] 1.2 在 `AiReportService` 中对 429、限流、超时和空响应做有界重试。
- [x] 1.3 在 `CoachAgentService` 中对同类调用失败做有界重试。

## 2. 评测节流与归因

- [x] 2.1 在助手 live eval 中新增 `AI_EVAL_CASE_DELAY_MS` 样本间等待。
- [x] 2.2 让成长报告 fallback 的失败原因保留 quota、rate limit、timeout 等归因。

## 3. 验证

- [x] 3.1 增加提交诊断 429 后重试成功的单元测试。
- [x] 3.2 增加 Coach 429 后重试成功或归因正确的单元测试。
- [x] 3.3 运行相关 AI 调用、助手评测和 OpenSpec 校验。
