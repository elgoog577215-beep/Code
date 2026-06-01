## Why

当前系统已经能追踪单次诊断、Coach 理解证据、推荐效果、教师介入、AC 后迁移、复发误区和自解释能力。但教育 agent 还缺少一个关键判断：学生是在借助 AI 支架逐步变得独立，还是越来越依赖提示、推荐和追问才能继续推进。

如果不把“AI 依赖度 / 自主解题度”结构化，系统可能把高频点击推荐、反复生成追问、提示后才提交通过误读为学习活跃，而老师真正需要看到的是支架是否正在撤掉、学生是否可以独立迁移。

## What Changes

- 新增 `aiDependencySignal`，表达学生近期对 Coach 追问、推荐入口和提示后提交的依赖程度，以及是否出现独立推进、支架有效或过度依赖。
- 新增确定性 `AiDependencyAnalyzer`，从现有 CoachPrompt、StudentRecommendationEvent、提交和诊断分析动态推导状态，不新增外部模型调用。
- 在学生能力画像和学习轨迹中输出该信号，让学生端和推荐系统可以区分“继续给提示”和“要求先独立尝试”。
- 在推荐服务中消费依赖信号：过度依赖时给 `INDEPENDENT_ATTEMPT` 或 `TEACHER_SCAFFOLD_FADE_REVIEW`，支架有效时继续给迁移/解释练习。
- 在教师作业概览中展示 AI 依赖风险学生数、班级摘要和学生行信号，帮助老师识别需要撤支架或示范“如何不用提示先尝试”的学生。
- 在 AI 质量概览中新增 `AI_DEPENDENCY_INDEPENDENCE_LOOP` 维度，评估系统是否发现过度依赖并转成教学动作。
- 增加后端测试与前端类型验证，覆盖独立推进、支架有效、过度依赖、推荐消费、教师概览和质量维度。

## Capabilities

### New Capabilities

- `ai-dependency-independence-loop`: 覆盖 AI 支架依赖度、自主推进信号、学生画像输出、学习轨迹输出、推荐消费、教师可见状态和 AI 质量评测。

### Modified Capabilities

- 无。

## Impact

- 后端：新增确定性分析器，影响 `StudentAbilityProfileService`、`StudentTrajectoryService`、`StudentRecommendationService`、`ClassroomService`、`AiQualityOverviewService`、`AiQualityMetrics` 和相关 DTO。
- 前端：共享 API 类型、学生题目/作业页、教师工作台学生行与 AI 质量维度展示。
- 测试：新增或更新 AI 依赖分析器、能力画像、轨迹、推荐、教师概览、AI 质量概览相关测试。
- 数据库：不新增迁移；第一版从现有 CoachPrompt、推荐事件、提交和诊断分析动态推导。
