## Background

项目已经具备外部模型增强诊断、规则信号、输出标准化、质量门禁和教师侧学习轨迹统计。教师端还有 `LearningActionEvidenceAnalyzer` 和 `LearningInterventionImpactAnalyzer`，能基于后续提交判断一次学习干预是否产生影响。

当前缺口在于：这些教师侧判断没有进入下一次提交诊断的 evidence package。外部模型下一次被调用时，主要看到当前代码、当前评测事实、上一轮 verdict 和重复 tag，而看不到上一轮 AI 让学生做了什么，以及学生是否执行了那个动作。这会削弱多轮辅导能力。

## Goals

- 让当前提交的诊断能读取上一条提交里的学习干预计划和学习动作执行证据。
- 让外部模型输入包含简洁、结构化、可引用的学习动作反馈。
- 让本地策略在动作被反证时主动降低提示粒度或建议教师介入。
- 保持兼容，不破坏旧 evidence、旧诊断、旧评测。

## Non-Goals

- 不重写教师端轨迹统计。
- 不引入新的外部裁判模型。
- 不把学习动作证据当成唯一诊断依据；它只影响下一步教学策略和置信边界。
- 不修改前端页面和静态构建产物。

## Data Model

在 `DiagnosisEvidencePackage.HistoryEvidence` 中新增字段：

- `previousInterventionType`
- `previousInterventionTask`
- `previousInterventionCompletionSignal`
- `previousLearningActionStatus`
- `previousLearningActionConfidence`
- `previousLearningActionEvidenceRefs`
- `previousLearningActionSummary`
- `previousLearningActionNextAdjustment`

字段只描述上一条同题提交对应的学习动作，不回溯多轮完整明细。这样可以控制 token 成本，同时覆盖下一次诊断最需要的反馈变量。

## Data Flow

```text
上一条提交分析 reportJson
        |
        | DiagnosisReportReader
        v
learningInterventionPlan + learningActionEvidence
        |
        | SubmissionAnalysisService.buildHistoryEvidence
        v
HistoryEvidence.previousLearningAction*
        |
        | ModelDiagnosisBriefBuilder
        v
learningTrajectorySummary
        |
        | ExternalModelAgentRuntime
        v
外部模型诊断与教学动作
```

## Strategy

### OBSERVED

说明学生后续提交已经体现了学习动作或同题通过。下一步应转向复盘、迁移、解释不变量或复杂度。

### PARTIALLY_OBSERVED

说明学生有一定行动痕迹，但产出还不稳定。下一步应保持方向，并把学习动作缩小成更明确的可检查产出。

### CONTRADICTED

说明后续证据与上一轮动作目标相反，常见场景是同类错误仍然重复。下一步应降低提示粒度，要求最小样例、变量跟踪或教师介入，而不是继续泛泛鼓励。

### NOT_OBSERVED

说明还没有足够后续证据。下一步不能假设学生已执行动作，应提醒等待同题后续提交或要求可观察产出。

## Compatibility

新增字段全部可为空。旧 evidence JSON 仍可反序列化。旧测试和旧诊断链路不需要提供这些字段。

## Testing

- `DiagnosisEvidencePackageBuilderTest`：验证显式传入的历史学习动作证据会被保留。
- `SubmissionAnalysisService` 相关测试：验证上一条提交分析里的学习干预与动作证据会进入当前 history evidence。
- `ModelDiagnosisBriefBuilder` 相关测试：验证 brief 的学习轨迹摘要包含上一轮动作状态。
- `DiagnosticAgentServiceTest`：验证 `CONTRADICTED` 会推动更小粒度干预或教师关注。
- OpenSpec strict validate 与后端 targeted tests。
