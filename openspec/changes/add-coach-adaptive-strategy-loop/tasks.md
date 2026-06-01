## 1. 响应结构与类型

- [x] 1.1 在 `CoachPromptResponse` 新增 `adaptiveStrategySignal` 结构化字段。
- [x] 1.2 在前端 Coach prompt API 类型新增兼容字段。

## 2. 自适应策略选择

- [x] 2.1 在 `CoachPromptService` 计算初始追问的自适应策略。
- [x] 2.2 在 `CoachPromptService` 计算 follow-up 的自适应策略。
- [x] 2.3 将策略 reason、recommendedCoachMove、needsTeacherAttention 和 evidence refs 写入响应。

## 3. 生成上下文与 fallback

- [x] 3.1 将策略摘要写入 `contextSummary`、`rationale` 和 `evidenceRefs`。
- [x] 3.2 更新规则 fallback 问题，使不同策略生成匹配的下一问。
- [x] 3.3 更新 `CoachAgentService` 模型提示或上下文，要求模型优先执行结构化策略。

## 4. 测试

- [x] 4.1 扩展 `CoachPromptServiceTest`，覆盖学习动作被反驳后降低颗粒度。
- [x] 4.2 扩展 `CoachPromptServiceTest`，覆盖 AC 后迁移复盘、回答含证据、回答含糊和答案倾向。
- [x] 4.3 如模型提示变更，运行相关 `CoachAgentServiceTest`。

## 5. 验证

- [x] 5.1 运行 `openspec validate add-coach-adaptive-strategy-loop --strict`。
- [x] 5.2 运行 Coach prompt/agent 相关后端测试。
- [x] 5.3 运行后端编译、前端 typecheck 和 `git diff --check`。
