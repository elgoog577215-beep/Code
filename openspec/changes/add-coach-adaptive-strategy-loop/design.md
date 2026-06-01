## Context

项目已经持续补强多层 AI 能力：细粒度错因、学习轨迹、学习动作证据、Coach 回答质量、追问后提交影响、教师端质量概览等。当前缺口不在“能不能观察状态”，而在“观察到的状态是否会改变下一次教学动作”。

现有 `CoachPromptService` 会把当前诊断标签、最近提交摘要和证据包传给 `CoachAgentService`，再由模型或规则 fallback 生成追问。但追问策略没有结构化输出，前端和测试也无法稳定判断 Coach 为什么选择某一种教学动作。这会限制后续迭代：即使系统发现学生反复卡同类问题，也难以验证下一问是否真的降低了颗粒度；即使学生回答已经有证据，也难以验证系统是否推进到了最小修改验证。

## Goals / Non-Goals

**Goals:**

- 为每个 Coach prompt 响应输出结构化 `adaptiveStrategySignal`。
- 基于学习动作证据、重复错因、verdict、提交轨迹和学生回答质量选择下一问策略。
- 把策略选择写入 contextSummary、rationale 和 evidenceRefs，保证教师可解释、测试可断言。
- 让模型生成和规则 fallback 都能消费同一策略信号。
- 保持 API 向后兼容，不新增数据库表或破坏旧 prompt。

**Non-Goals:**

- 不重写 Coach 对话全流程。
- 不引入新的模型供应商或额外外部依赖。
- 不把自适应策略等同于因果证明；它只表示基于当前证据的下一问策略选择。
- 不新增复杂前端展示，本轮只补齐 API 类型和后端信号。

## Decisions

### Decision 1: 先做确定性策略选择，再让模型执行策略

策略选择由 `CoachPromptService` 根据结构化证据确定，模型只负责在安全约束下组织自然语言追问。这样做比完全交给模型自由判断更可测、更可解释，也能保证规则 fallback 和模型路径一致。

优先级如下：

1. 学生回答暴露答案或完整代码倾向 -> `SAFETY_RESET`。
2. 当前提交已 AC -> `TRANSFER_REFLECTION`。
3. 学习动作证据被后续提交反驳，或同类细分错因反复出现 -> `REDUCE_GRANULARITY`。
4. 学生回答已经包含样例、变量、复杂度等证据 -> `VERIFY_MINIMAL_CHANGE`。
5. 学生回答为空或只有方向没有证据 -> `COLLECT_EVIDENCE`。
6. 其他普通失败诊断 -> `CONTINUE_SOCRATIC` 或收集证据。

### Decision 2: 不新增表，使用现有 prompt 字段承载可追溯策略信号

`CoachPrompt` 已经持久化 `contextSummary`、`rationale` 和 `evidenceRefs`。本轮把 `coach-strategy:<strategy>` 与相关历史证据 refs 写入 `evidenceRefs`，并在响应时从 refs 和上下文还原 `adaptiveStrategySignal`。这样旧数据保持兼容，新数据又能被测试、前端和教师端后续复用。

### Decision 3: evidenceRefs 使用机器可读前缀

新增 refs 采用稳定前缀：

- `coach-strategy:<STRATEGY>` 表示最终策略。
- `coach-adaptive:previous_action:<STATUS>` 表示学习动作执行结果。
- `coach-adaptive:answer_quality:<STATE>` 表示学生回答质量。
- `coach-adaptive:repeated_fine_tag:<TAG>` 或 `coach-adaptive:repeated_issue_tag:<TAG>` 表示重复卡点。
- `coach-adaptive:verdict:ACCEPTED` 表示 AC 后迁移复盘。

这些 refs 不依赖自然语言解析，适合测试断言和后续质量概览消费。

### Decision 4: DTO 直接暴露结构化信号，旧 prompt 可为空

`CoachPromptResponse` 新增 `CoachAdaptiveStrategySignal`。旧 prompt 没有 `coach-strategy:*` ref 时返回 `null`，避免对既有数据和前端造成破坏。新 prompt 则返回 strategy、reason、recommendedCoachMove、needsTeacherAttention 和 evidenceRefs。

## Risks / Trade-offs

- [Risk] 策略选择规则过早僵化，可能压制模型灵活性。-> Mitigation: 第一版只选择教学动作方向，不写死具体措辞；模型仍可在安全约束内生成追问。
- [Risk] 从 contextSummary 还原 reason 可能不如独立字段完美。-> Mitigation: 机器判断依赖 `coach-strategy:*` ref；reason 用于解释，不作为关键控制流。
- [Risk] 学生回答质量启发式可能误判。-> Mitigation: 策略证据包含 answer_quality refs，测试覆盖主要分支，未来可接入已有 `CoachAnswerQualityAnalyzer` 做更细判断。
- [Risk] 不新增前端展示会让能力短期不可见。-> Mitigation: 后端 API 和类型先沉淀结构，后续学生端或教师端可以直接消费。
