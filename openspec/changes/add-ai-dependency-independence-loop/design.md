## Context

当前系统已经把“AI 是否给出好反馈”扩展成多个教育状态：Coach 回答质量、自解释能力、复发误区、推荐效果、教师介入、AC 后迁移和 AI 质量维度。缺口在于这些能力仍默认“更多 AI 支架 = 更好学习”，但真实教学里还需要判断支架是否正在退场。

现有数据源足够支持第一版动态推导：

- CoachPrompt 记录学生是否反复请求追问、是否回答、回答质量和时间。
- StudentRecommendationEvent 记录推荐曝光、点击、进入题目、提交和后续结果。
- Submission 与 SubmissionAnalysis 记录学生是否在没有推荐/追问牵引时独立提交、是否通过或错因转移。
- 现有学生画像、学习轨迹、教师概览和 AI 质量概览已经有类似结构化信号的接入点。

## Goals / Non-Goals

**Goals:**

- 新增 AI 依赖度/自主性信号，区分独立推进、支架有效、支架过密、过度依赖和需要教师撤支架。
- 在学生能力画像、当前作业轨迹、推荐系统、教师作业概览和 AI 质量概览中复用同一结构。
- 使用已有事件和提交动态推导，不新增数据库迁移或外部模型调用。
- 用测试覆盖状态判断、推荐消费、教师概览和 AI 质量维度。

**Non-Goals:**

- 不阻止学生使用 Coach 或推荐；信号只用于调整下一步教学动作。
- 不用 AI 依赖度替代提交结果、错因标签或自解释能力。
- 不追踪键盘行为、停留时间等当前系统没有的数据。
- 不把一次点击或一次追问判定为依赖风险。

## Decisions

### Decision 1: 新增动态 `AiDependencyAnalyzer`

分析器接收同一学生或当前作业范围内的提交、CoachPrompt 和 StudentRecommendationEvent，输出 `aiDependencySignal`：

- `status`: `NO_SIGNAL`、`INDEPENDENT_PROGRESS`、`SCAFFOLD_EFFECTIVE`、`SCAFFOLD_DENSE`、`DEPENDENCY_RISK`、`TEACHER_FADE_REVIEW`
- `label`: 短标签
- `summary`: 面向学生/教师的解释
- `independenceScore`: 0 到 1，越高表示越能独立推进
- `coachPromptCount` / `answeredCoachCount`
- `recommendationClickCount` / `recommendationSubmissionCount`
- `independentSubmissionCount` / `independentAcceptedCount`
- `scaffoldedAcceptedCount`
- `dependencyEvidenceRefs`
- `recommendedAction`
- `needsTeacherAttention`

动态分析优先于新表，因为第一版的目标是让已有历史数据立刻可观察。

### Decision 2: 用“支架后结果 + 独立尝试比例”判断，而不是看使用次数

单纯使用 Coach 或推荐不是坏事。第一版判断采用组合信号：

- 有推荐/Coach 后提交并通过或错因改善，且仍有独立提交，判为 `SCAFFOLD_EFFECTIVE`。
- 追问/推荐很多，但独立提交很少，判为 `SCAFFOLD_DENSE`。
- 多次点击推荐或生成追问后仍不提交、仍同类失败，判为 `DEPENDENCY_RISK`。
- 长期高支架密度且没有独立推进，判为 `TEACHER_FADE_REVIEW`。
- 没有 AI 支架牵引也能通过或错因改善，判为 `INDEPENDENT_PROGRESS`。

这样避免把“会使用资源”误判成“依赖 AI”。

### Decision 3: 推荐系统用依赖信号调整支架剂量

当 `status` 为 `SCAFFOLD_DENSE` 或 `DEPENDENCY_RISK` 时，推荐服务新增 `INDEPENDENT_ATTEMPT`，要求先做一次不看新提示的最小独立尝试。当 `TEACHER_FADE_REVIEW` 时，新增 `TEACHER_SCAFFOLD_FADE_REVIEW`，建议老师示范如何拆提示、撤支架和设定独立尝试边界。

信号为 `SCAFFOLD_EFFECTIVE` 或 `INDEPENDENT_PROGRESS` 时不挤占现有题目推荐，只作为画像与质量评估证据。

### Decision 4: 教师端只显示风险摘要

教师工作台新增 AI 依赖风险学生数、班级摘要和学生行状态。默认只展示需要关注的 `SCAFFOLD_DENSE`、`DEPENDENCY_RISK` 和 `TEACHER_FADE_REVIEW`，避免把正常使用 AI 的学生都推到教师面前。

### Decision 5: AI 质量维度检查“支架是否可退场”

`AI_DEPENDENCY_INDEPENDENCE_LOOP` 关注系统是否识别过度支架和自主推进，不评价模型答案对错。它输出质量状态、分数、摘要、证据引用和推荐动作，和现有质量维度并列。

## Risks / Trade-offs

- [Risk] 推荐事件不完整时可能低估支架依赖。-> Mitigation: 输出 `NO_SIGNAL` 或低置信摘要，不强行判定风险。
- [Risk] 新手阶段高频使用 AI 是合理行为。-> Mitigation: 只有在高支架密度同时缺少独立提交或后续仍失败时才升级风险。
- [Risk] 教师端信号过多。-> Mitigation: 教师端只突出风险状态，正向状态留在画像/质量详情。
- [Risk] 动态聚合可能重复扫描事件。-> Mitigation: 复用已有按学生/作业查询，并限制近期窗口。

## Migration Plan

无需数据库迁移。部署后从现有 CoachPrompt、推荐事件、提交和诊断分析动态生成信号。旧前端忽略新增字段；回滚时删除服务消费和展示即可，不影响已有诊断、Coach、推荐和教师介入数据。
