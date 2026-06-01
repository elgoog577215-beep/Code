## Why

当前系统已经能观察单次诊断、学习轨迹、Coach 回答质量和追问后的提交影响，但学生侧 Coach 下一问仍主要依赖当前诊断标签与固定追问模板。也就是说，AI 已经能“看见”学生的学习状态，却还没有稳定把这些历史证据转化为下一轮教学策略。

要把在线判题系统的 AI 做成可迭代的教育 agent，下一步需要让 Coach 追问具备结构化自适应能力：根据前一次教学动作是否被验证、学生回答是否有证据、是否反复卡同类错误、是否已经 AC，选择更合适的下一问策略，并把选择原因和证据暴露出来。

## What Changes

- 在 Coach prompt 响应中新增 `adaptiveStrategySignal` 结构化字段，说明本轮追问采用的策略、原因、建议教学动作、是否需要教师关注和证据引用。
- `CoachPromptService` 基于学习动作证据、最近提交轨迹、重复错因、当前 verdict 和学生上一轮回答质量选择下一问策略。
- 策略覆盖降低提示颗粒度、收集证据、验证最小修改、AC 后迁移复盘、泄题风险收回到证据层等状态。
- Coach 追问上下文、rationale 和 evidenceRefs 写入机器可读的策略证据，例如 `coach-strategy:REDUCE_GRANULARITY`。
- `CoachAgentService` 的模型上下文明确要求模型优先遵循结构化自适应策略，规则 fallback 同步使用该策略。
- 前端 API 类型新增兼容字段，便于后续学生端或教师端展示策略信号。
- 增加后端测试覆盖历史学习动作被反驳、AC 后迁移、学生回答含证据、回答含糊和泄题风险等场景。

## Capabilities

### New Capabilities

- `coach-adaptive-strategy-loop`: 覆盖 Coach 下一问如何消费历史学习证据、学生回答质量和当前评测状态，输出结构化自适应策略并用测试验证。

### Modified Capabilities

无。

## Impact

- 后端：更新 `CoachPromptResponse`、`CoachPromptService`、`CoachAgentService` 和 `CoachPromptServiceTest`。
- 前端：更新 Coach prompt 相关 API 类型，保持兼容但不强制新增 UI。
- API：Coach prompt 响应新增兼容字段 `adaptiveStrategySignal`。
- 数据：无数据库迁移；策略信号通过现有 prompt 上下文、rationale 和 evidenceRefs 可追溯。
