## Context

当前外部模型单次诊断链路会经历三层质量控制：模型输出结构校验、诊断证据校验、学生提示安全校验。live eval 结果显示，某些样本的外部模型诊断已命中真实错因，但最终输出被降级为泛化提示，导致教学动作有效率下降。

这个问题的关键不是模型完全不会诊断，而是安全判定的作用域不够清晰。学生真正看到的是 `studentHint`、`studentHintPlan`、`learningInterventionPlan` 和报告中给学生的部分；教师备注、模型自评风险、内部聚合字段不应直接等价为学生端泄题。

## Goals / Non-Goals

**Goals:**

- 将 teaching hint 验证的高风险判定限定到学生可见字段。
- 继续拒绝学生可见字段中的完整代码、直接改法、最终答案、隐藏测试数据和可执行控制结构。
- 让运行时 `answerLeakRisk` 合成逻辑不被顶层聚合风险误导。
- 用单元测试覆盖输入读取类真实场景，避免正确的 `COMPARE_INPUT_SPEC` 被误降级。

**Non-Goals:**

- 不移除安全门禁。
- 不允许模型给出完整修正代码或直接答案。
- 不改变 live eval 统计目标和路由策略。
- 不把教师端备注直接展示给学生。

## Decisions

### 1. 顶层 `answerLeakRisk` 作为模型自评，不单独触发拒绝

顶层 `teachingHint.answerLeakRisk` 更像模型对整段输出的自评或聚合标签。外部模型可能因为教师备注里写了“不要直接告诉学生某写法”而标高风险，但学生可见内容本身是安全的。验证器不应只看这个字段就拒绝。

替代方案是继续让顶层高风险一票否决，但这会误杀安全且有教学价值的输出，使外部模型能力被系统自身削弱。

### 2. 学生可见字段继续严格检查

`studentHint`、`studentHintPlan.nextAction`、`studentHintPlan.coachQuestion`、`learningInterventionPlan.studentTask` 是学生可能看到或执行的内容，仍然需要高风险字段和文本触发器双重检查。

替代方案是只依赖模型自己填写的风险等级，但这无法防止模型误标低风险却输出直接改法。

### 3. 最终风险合成使用可见计划风险

`AiReportService` 生成最终分析时，`answerLeakRisk` 应来自学生提示计划和学习干预计划的风险，再结合兜底分析风险。这样可以避免非学生可见字段触发最终 `HintSafetyService` 的误降级。

替代方案是保留顶层风险参与合成，但 live eval 已经说明这会让正确诊断在最后一环丢失教学动作。

## Risks / Trade-offs

- [Risk] 忽略顶层高风险可能漏掉某些模型自评发现的问题。→ Mitigation：学生可见字段仍做风险等级和文本触发器检查，真实泄题内容仍会被拒绝。
- [Risk] 教师备注未来如果展示给学生，当前假设会失效。→ Mitigation：本变更的边界是“教师备注不学生可见”；如果产品改变展示范围，必须把该字段纳入可见安全扫描。
- [Risk] 只修安全误判不能解决配额和路由失败。→ Mitigation：本变更只处理质量误降级；容量问题继续由 route attribution 和 fallback route 评测闭环处理。
