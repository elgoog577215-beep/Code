## Context

当前提示安全分散在三个位置：

- 诊断结果：`SubmissionAnalysis` 的结构化报告包含 `answerLeakRisk`，AI 质量概览目前主要用它统计 `HINT_SAFETY`。
- 安全降级：`HintSafetyService` 会检查学生提示、学习干预和报告文本，发现完整代码、直接改法、隐藏测试或超过提示层级时降级，并保存 `HintSafetyCheck`。
- Coach 回答：`CoachAnswerQualityAnalyzer` 能识别学生回答是否越过证据层，`AiQualityMetrics` 目前只在 `COACH_UNDERSTANDING` 中消费这类风险。

缺口是这些信号没有被合成一个“安全事件闭环”。教师只能看到某个维度异常，却不知道风险来自模型输出、规则降级还是 Coach 对话；系统也缺少一个面向评测和回归的安全事件结构。

## Goals / Non-Goals

**Goals:**

- 输出作业级 `promptSafetyIncidentSignal`，聚合高泄题诊断、安全降级记录和 Coach 安全风险。
- 为每类安全事件提供计数、主要来源、状态、教师建议和 evidence refs。
- 在 AI 质量概览中新增 `PROMPT_SAFETY_INCIDENT_LOOP` 维度，并进入改进优先级排序。
- 复用现有数据结构，不新增数据库迁移。
- 增加后端测试和前端类型兼容，确保安全闭环可验证。

**Non-Goals:**

- 不改变 `HintSafetyService` 的具体拦截规则。
- 不重写 Coach 追问或诊断 prompt。
- 不把安全事件解释为严格因果，只作为已记录的风险来源和人工复核线索。
- 不新增复杂前端展示；本轮只补充类型、维度名称和排序。

## Decisions

### Decision 1: 新建独立 analyzer，而不是继续扩大 `AiQualityOverviewService`

提示安全事件需要同时读取诊断、`HintSafetyCheck` 和 Coach answer quality。新建 `PromptSafetyIncidentAnalyzer` 负责归一化计数、状态、summary、recommendedAction 和 evidence refs，让 `AiQualityOverviewService` 只负责装配数据与输出维度。

### Decision 2: 安全 incident 采用来源维度，而不是只算总数

第一版来源：

- `DIAGNOSIS_HIGH_LEAK_RISK`: 诊断报告或教学提示标记为 `answerLeakRisk=HIGH`。
- `HINT_SAFETY_CHECK`: `HintSafetyService` 记录了 `MEDIUM` 或 `HIGH` 风险降级。
- `COACH_SAFETY_RISK`: Coach 回答质量信号显示学生回答疑似越过证据层。

这样教师可以判断是模型输出本身需要调整、规则降级频繁，还是对话中学生在索要/复述答案。

### Decision 3: 保持原 `HINT_SAFETY`，新增闭环维度

`HINT_SAFETY` 继续表示诊断结果里的高泄题风险，避免破坏旧指标和趋势；`PROMPT_SAFETY_INCIDENT_LOOP` 则表示跨诊断、降级和 Coach 的安全事件闭环。二者并存，前者是输出风险，后者是事件闭环。

### Decision 4: evidence refs 优先指向可追溯对象

安全降级使用 `hint_safety_check:<id>` 和 `hint_safety_submission:<submissionId>`；高风险诊断复用诊断 evidence refs 或 `high_leak_risk:submission:<id>`；Coach 风险使用 `coach_safety:submission:<id>`。所有 refs 去重并限制数量，保证响应体可控。

## Risks / Trade-offs

- [Risk] 同一次提交可能同时命中高泄题诊断和安全降级，计数会按来源累计。-> Mitigation: 文案描述为“安全事件来源”，不声称是唯一学生数；证据引用可追溯到提交。
- [Risk] `HintSafetyCheck` 当前会保存 LOW 检查。-> Mitigation: analyzer 只把 `MEDIUM` 和 `HIGH` 计为降级事件。
- [Risk] Coach 安全风险来自学生回答质量，不一定是 AI 主动泄题。-> Mitigation: 来源命名为 Coach 安全风险，并在建议里要求回到证据层，而不是直接归因模型错误。
- [Risk] 前端尚未完整展示新结构。-> Mitigation: 先补类型和维度 fallback，让现有 AI 质量列表能自然显示新维度。
