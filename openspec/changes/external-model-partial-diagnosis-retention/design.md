## Background

当前外部模型诊断 runtime 分两阶段：

```text
diagnosis-judge-v1 -> teaching-hint-v1 -> final analysis
```

阶段 A 负责错因、细粒度标签、证据、置信度和不确定性。阶段 B 负责学生提示、教学动作、教师备注和干预计划。真实测试中，阶段 A 可能成功，阶段 B 因额度、限流、JSON 或安全问题失败。如果此时全部 fallback 到规则诊断，就会丢掉外部模型最关键的判断能力。

## Goals

- 保留已通过校验的模型错因裁决。
- 在教学提示失败时，用本地安全模板补齐学生可见反馈。
- 在状态和报告里准确区分完整成功、部分成功和完全回退。
- 提升外部模型接入后的有效完成率，不靠降低安全标准换取通过率。

## Non-Goals

- 不降低 `teaching-hint-v1` 的安全校验标准。
- 不把未通过校验的诊断阶段结果写入分析。
- 不改前端展示结构。
- 不引入额外模型调用。

## Design

### Partial Completion Path

当 `diagnosis-judge-v1` 输出通过校验后，若 `teaching-hint-v1` 调用失败或输出未通过校验：

1. 构造本地 `TeachingHintOutput`。
2. 复用模型阶段 A 的 `primaryIssueTag`、`fineGrainedTag`、`evidenceRefs`、`confidence` 和 `uncertainty`。
3. 根据错因标签生成安全提示：
   - `studentHint`
   - `studentHintPlan`
   - `learningInterventionPlan`
   - `teacherNote`
4. `aiInvocation.status = MODEL_PARTIAL_COMPLETED`
5. `aiInvocation.fallbackUsed = false`
6. `uncertainty` 补充教学阶段失败信息，例如 `TEACHING_HINT:INSUFFICIENT_QUOTA`。

### Why fallbackUsed=false

这里不是完全规则回退。核心诊断由外部模型给出，本地只是补齐安全表达。因此 `fallbackUsed=false` 更能代表“模型结果被采用”。失败原因仍通过 `uncertainty` 和评测报告保留。

### Safety

本地教学模板只生成 L1/L2 风格的引导，不包含完整代码、最终答案或隐藏数据。它应比模型教学阶段更保守。

### Evaluation

live eval 对 `MODEL_PARTIAL_COMPLETED` 应判为 `completedOutput=true`，再继续检查：

- expectedSignalHit
- evidenceValid
- safetyPassed
- teachingActionValid

这样可以真实反映“模型诊断准确，但教学阶段由本地兜底”的有效产出。
