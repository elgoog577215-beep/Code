## Context

当前系统已经有比较完整的 AI 教育 agent 基础：

- 单次诊断会构造 `DiagnosisEvidencePackage`，结合规则信号、外部模型、提示安全和学习轨迹生成 `SubmissionAnalysisResponse`。
- 学生学习记忆会统计历史提交、复发错因、能力焦点、学习动作效果和教师校正摘要。
- 教师校正实体已经记录 `originalIssueTag`、`originalFineGrainedTag`、`correctedIssueTag`、`correctedFineGrainedTag`、`teacherNote` 和 `evalCandidate`。
- AI 质量概览已经统计教师纠错和 eval readiness。

缺口是教师校正只作为摘要文本进入学习记忆，无法稳定表达“老师已经多次把 AI 的 A 纠正为 B，因此下一次相似证据应优先关注 B 或降低置信”。这限制了教师反馈对在线诊断的即时价值。

## Goals / Non-Goals

**Goals:**

- 从现有教师校正记录中动态构建结构化 `teacherCalibrationSignal`。
- 将校准信号写入诊断证据包、模型 brief 和最终诊断响应。
- 当当前诊断与强教师校准冲突时，降低置信、补充 uncertainty 和教师复核提示。
- 当当前诊断与教师校准一致时，补充 evidenceRefs，让教师能追踪“AI 参考了哪类教师校正”。
- 在 AI 质量概览中新增教师校准闭环维度，评估校正是否真正反哺诊断。
- 用后端测试覆盖校准信号、brief、诊断和质量维度。

**Non-Goals:**

- 不新增数据库迁移；第一版只基于现有教师校正表动态推导。
- 不把教师校正强制覆盖当前代码/评测证据；当前提交事实仍是最高优先级。
- 不让外部模型看到完整隐藏测试数据或教师私密信息；只暴露标签、简短备注摘要和证据引用。
- 不重写教师校正 UI 和 eval 导出流程。

## Decisions

### Decision 1: 在学习记忆中新增 `TeacherCalibrationPattern`

在 `DiagnosisEvidencePackage.StudentLearningMemorySnapshot` 中新增：

- `teacherCalibrationSignal`: 当前最相关的校准信号。
- `teacherCalibrationPatterns`: 最近教师校正模式列表，供模型 brief 和质量概览使用。

每个模式包含：

- `originalIssueTag`
- `originalFineGrainedTag`
- `correctedIssueTag`
- `correctedFineGrainedTag`
- `correctionCount`
- `latestTeacherNote`
- `evidenceSubmissionIds`
- `evidenceRefs`
- `evalCandidateCount`

选择“最相关模式”时优先级为：细分标签匹配当前候选信号 > 粗粒度标签匹配 > 同学生近期高频校正 > 最新校正。这样不会让历史校正无差别压过当前证据。

### Decision 2: 新增响应级 `TeacherCalibrationSignal`

在 `SubmissionAnalysisResponse` 新增 `teacherCalibrationSignal`：

- `status`: `NO_SIGNAL`、`SUPPORTED`、`APPLIED`、`CONFLICT_NEEDS_REVIEW`
- `summary`
- `originalIssueTag`
- `originalFineGrainedTag`
- `correctedIssueTag`
- `correctedFineGrainedTag`
- `correctionCount`
- `confidenceAdjustment`
- `evidenceRefs`
- `recommendedAction`
- `needsTeacherReview`

`SUPPORTED` 表示当前诊断与教师校准一致；`APPLIED` 表示当前诊断原本缺少教师修正标签，系统补充校准证据但不直接覆盖代码证据；`CONFLICT_NEEDS_REVIEW` 表示当前诊断仍命中教师曾纠正的原始标签，必须降低置信并提示教师复核。

### Decision 3: brief 接入校准，但仍遵循当前证据优先

`ModelDiagnosisBrief` 新增 `teacherCalibrationSummary`，并把校准模式转成低到中等置信的 `CandidateSignal`。规则：

- 校正后的标签进入 `allowedIssueTags` 和 `allowedFineGrainedTags`。
- 校准 evidenceRefs 进入 brief evidenceRefs。
- brief 文案明确“教师校准只能作为辅助约束，不能覆盖当前提交事实”。

这样外部模型能看到教师已确认的误判模式，同时不把历史标签当成绝对答案。

### Decision 4: 诊断后进行确定性校准处理

`DiagnosticAgentService` 在标签归一化和低置信保护后应用教师校准：

1. 没有校准信号：不改变诊断。
2. 当前标签已经包含教师修正标签：标记 `SUPPORTED`，补充 evidenceRefs。
3. 当前标签命中教师曾纠正的原始标签但缺少修正标签：标记 `CONFLICT_NEEDS_REVIEW`，降低置信，补充 uncertainty、teacherNote 和复核动作，但不直接删除原标签。
4. 当前没有明显冲突但校准模式与候选信号相关：标记 `APPLIED`，补充 evidenceRefs 和允许标签，让后续人工/模型更容易看见校准方向。

### Decision 5: 质量维度评估“校正是否进入在线诊断”

新增 `TEACHER_CALIBRATION_LOOP` 质量维度：

- 没有教师校正：`WATCH`，提示先收集校正样本。
- 有教师校正但近期分析没有校准信号：`ACTION`，说明校正没有进入在线诊断。
- 有 `CONFLICT_NEEDS_REVIEW`：`ACTION`，提示优先抽查冲突样本。
- 有 `SUPPORTED` 或 `APPLIED`：`HEALTHY` 或 `WATCH`，说明教师校正已反哺诊断。

## Risks / Trade-offs

- [Risk] 教师历史校正可能不适用于当前提交。-> Mitigation: 只作为辅助信号，并要求当前候选标签或同学生近期上下文匹配才强应用。
- [Risk] 降置信过多可能让诊断显得犹豫。-> Mitigation: 最低只降到中等置信，并用 `uncertainty` 解释原因。
- [Risk] 校准信号增加响应复杂度。-> Mitigation: 新字段可选，教师端先通过质量维度消费。
- [Risk] 多个教师校正模式冲突。-> Mitigation: 第一版只选最相关/最新模式，同时保留 patterns 列表供后续扩展。

## Migration Plan

无需数据库迁移。部署后新诊断会基于现有教师校正记录动态产生校准信号。回滚时删除新增字段消费即可，旧诊断 JSON 仍可读取。
