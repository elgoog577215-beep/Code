## Why

当前系统已经能诊断单次提交、追踪多次提交、判断 Coach 单次回答质量，并把推荐、教师介入、AC 后迁移和复发误区纳入质量闭环。但教育 agent 还缺少一个长期状态：学生是否能稳定把提示转化为自己的最小样例、变量轨迹、输出对比、复杂度判断或迁移解释。

这类“自解释能力”决定学生是否真正理解了提示，而不是只按 AI 或老师的指令局部改代码。把它结构化以后，系统才能区分“需要继续给题”“需要追问证据”“需要老师示范解释框架”和“可以进入迁移复盘”。

## What Changes

- 新增 `selfExplanationMasterySignal`，表达学生近期 Coach 回答中的证据类型覆盖、可验证性、迁移解释、空泛确认、疑似越界和教师关注状态。
- 在学生能力画像和学习轨迹中输出该信号，让推荐、学生端和教师端不需要从 Coach 自然语言摘要里重新推断长期理解状态。
- 新增确定性 `SelfExplanationMasteryAnalyzer`，复用现有 CoachPrompt 与 `CoachAnswerQualityAnalyzer`，不新增外部模型调用。
- 让推荐服务消费自解释信号：证据不足时优先给 `SELF_EXPLANATION_PRACTICE`，持续空泛或安全风险时给 `TEACHER_EXPLANATION_REVIEW`。
- 在教师作业概览中展示自解释薄弱学生数、班级摘要和学生行信号，帮助老师识别“会改代码但不会解释”的学生。
- 在 AI 质量概览中新增 `SELF_EXPLANATION_MASTERY_LOOP` 维度，评估系统是否把学生理解证据转成下一步教学动作。
- 增加后端测试与前端类型验证，覆盖证据充足、证据不足、持续空泛、推荐消费、教师概览和质量维度。

## Capabilities

### New Capabilities

- `self-explanation-mastery-loop`: 覆盖长期自解释能力信号、学生画像输出、学习轨迹输出、推荐消费、教师可见状态和 AI 质量评测。

### Modified Capabilities

- 无。

## Impact

- 后端：新增确定性分析器，影响 `StudentAbilityProfileService`、`StudentTrajectoryService`、`StudentRecommendationService`、`ClassroomService`、`AiQualityOverviewService` 和相关 DTO。
- 前端：共享 API 类型、学生练习/作业页、教师工作台学生行与 AI 质量维度展示。
- 测试：新增或更新自解释分析器、能力画像、轨迹、推荐、教师概览、AI 质量概览相关测试。
- 数据库：不新增迁移；第一版从现有 CoachPrompt、提交、诊断分析和推荐事件动态推导。
