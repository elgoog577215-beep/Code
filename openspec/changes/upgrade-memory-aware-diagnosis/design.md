## Context

当前项目已经有一条相对完整的外部模型教育 agent 链路：

```text
SubmissionAnalysisService
-> DiagnosisEvidencePackage
-> RuleSignalAnalyzer
-> ModelDiagnosisBrief
-> StandardLibraryPack
-> PromptTemplateRegistry
-> AiReportService / ExternalModelAgentRuntime
-> ModelOutputValidator
-> DiagnosticAgentService
```

上一轮已经把 `StudentLearningMemorySnapshot` 放进 `DiagnosisEvidencePackage`，并让 `ModelDiagnosisBriefBuilder` 生成 `learningMemorySummary` 和 `memory:*` 候选信号。这解决了“模型看不到学生长期画像”的问题，但还没有解决“模型是否正确使用画像”的问题。

本轮不能另起一个 memory agent，也不能新建独立记忆表。正确方向是沿现有主证据链增加一层轻量校准：把记忆当作辅助先验，当前提交证据仍是主证据。

## Goals / Non-Goals

**Goals:**

- 让记忆使用从自然语言约束升级为结构化、可测试、可校验的证据策略。
- 判断模型主错因是否有当前提交证据支撑，避免只凭 `memory:*` 证据选择主诊断。
- 当记忆与当前证据一致时，提高教学个性化；当记忆与当前证据冲突时，保护当前证据优先。
- 给教师端和后续统计留下可解释字段，例如是否建议老师复核、是否存在历史干预无效。
- 保持兼容：新增字段、辅助类和测试，不破坏已有 API 和旧报告读取。

**Non-Goals:**

- 不新增数据库表。
- 不引入向量库、RAG 记忆库或聊天式长期记忆。
- 不重写 `StudentAbilityProfileService`、`StudentTrajectoryService` 或教师端统计。
- 不把历史代码全文加入外部模型 prompt。
- 不把记忆作为比当前编译、运行、评测、源码证据更高优先级的诊断依据。

## Decisions

### 1. 新增记忆证据策略，而不是新建记忆系统

新增轻量组件，例如 `MemoryEvidencePolicy`，输入 `DiagnosisEvidencePackage`、`ModelDiagnosisBrief` 和候选信号，输出记忆策略结果。

策略结果包含：

- 记忆是否存在。
- 记忆与当前候选信号是否一致。
- 记忆是否与当前强证据冲突。
- 记忆是否只能用于教学动作，而不能用于主诊断。
- 是否建议教师复核。

不选择新建 `StudentMemoryService` 或新表，因为那会造成与当前 evidence package、report JSON、教师修正、评测链路分裂。

### 2. 在 brief 中加入结构化记忆校准摘要

保留 `learningMemorySummary`，再新增机器可读字段，例如 `memoryCalibration`。

字段建议：

```text
memoryAvailable
memoryRelevance
matchedCurrentEvidenceTags
memoryOnlyTags
conflictingMemoryTags
teachingUseOnly
teacherReviewRecommended
policy
evidenceRefs
```

这样外部模型不仅看到“这个学生经常错什么”，还能看到系统明确告诉它“这些记忆能不能用于主诊断”。

### 3. 诊断校验必须区分当前证据和记忆证据

`ModelOutputValidator` 目前只验证：

- 标签是否合法。
- evidenceRefs 是否存在。
- 是否有泄题风险。

本轮新增证据支撑检查：

- 如果模型选择的主错因只被 `memory:*` 引用支撑，且没有当前提交候选信号或 judge/source/compile/runtime 证据支撑，则校验失败。
- 如果模型引用了记忆证据，但选择了与当前强候选信号冲突的标签，则校验失败或降级到 `NEEDS_MORE_EVIDENCE`。
- 如果模型把记忆用于 `teacherNote`、干预粒度、下一步任务，则允许。

### 4. 校准结果进入 agent trace，而不是只写测试

`DiagnosticAgentService` 应把记忆校准结果纳入诊断 trace 或 evidence refs，例如：

```text
memoryCalibration=aligned/conflict/teaching-only
teacherReviewRecommended=true/false
```

这样后续教师端统计和 live eval 能区分“模型真的正确使用了记忆”还是“规则兜底碰巧命中”。

### 5. 测试以冲突样本为核心

新增测试不能只证明“记忆进了 brief”。必须覆盖：

- 记忆标签与当前强规则信号一致。
- 记忆标签与当前强规则信号冲突。
- 模型只引用 `memory:*` 却选择主错因。
- 模型同时引用当前证据和记忆证据。
- 历史干预无效时，教学任务变小或换动作。

## Risks / Trade-offs

- [Risk] 校验过严导致外部模型有效输出被拒绝 -> 先只拦截“主诊断只有记忆证据支撑”的高风险情况，保留教学表达使用记忆。
- [Risk] 新增字段增加 prompt 长度 -> 结构化摘要保持短文本和短列表，不加入历史代码全文。
- [Risk] 记忆策略与已有规则信号重复 -> 策略只判断记忆与当前证据关系，不重新推断错因。
- [Risk] 教师端短期看不到新字段 -> 先进入 trace/evidence package，后续再通过现有 reader 暴露，避免本轮多端联动过大。

## Migration Plan

1. 新增结构化字段和策略类，默认空值兼容旧数据。
2. brief 构建时生成记忆校准摘要，prompt 和标准库消费该摘要。
3. validator 增加记忆证据支撑检查。
4. agent trace 记录校准结果。
5. 增加定向测试和 OpenSpec strict validate。
