## 1. OpenSpec 契约

- [x] 1.1 创建运行归因行动信号 proposal、design、spec 和任务清单。
- [x] 1.2 运行 `openspec validate add-runtime-attribution-action-signal --strict`。

## 2. 后端信号

- [x] 2.1 扩展 `AiQualityOverviewResponse`，新增 `RuntimeAttributionSignal`。
- [x] 2.2 在 `AiQualityOverviewService` 中根据 `aiInvocation` 生成主导失败类型、原因、阶段、计数、证据和推荐动作。
- [x] 2.3 让 `MODEL_RUNTIME` 质量维度和 `improvementPriorities` 消费运行归因行动信号。
- [x] 2.4 更新 `AiQualityOverviewServiceTest` 覆盖额度不足、预算保护或部分完成归因。

## 3. 教师端展示

- [x] 3.1 更新前端 API 类型，兼容 `runtimeAttributionSignal`。
- [x] 3.2 在教师工作台 AI 质量摘要中展示模型归因和推荐动作。

## 4. 验证

- [x] 4.1 运行后端 AI 质量概览相关测试。
- [x] 4.2 运行前端 typecheck。
- [x] 4.3 运行 `git diff --check`。
- [x] 4.4 对照最近 live eval 报告说明运行归因行动信号如何覆盖额度不足和预算保护。
