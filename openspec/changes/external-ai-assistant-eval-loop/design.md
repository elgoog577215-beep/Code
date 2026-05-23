## Background

项目已有的 AI 链路分散在三个教学场景：

- 提交诊断：`DiagnosticAgentService` 和 `AiReportService` 负责把评测结果、规则信号和外部模型输出组合为错因诊断。
- Coach 追问：`CoachAgentService` 负责基于当前诊断和学生回答生成下一问。
- 成长报告：`GrowthReportService` 通过 `AiReportService.enhanceGrowthReportMarkdown` 把多次提交轨迹写成学习报告。

之前的 live eval 主要围绕提交诊断，能判断外部模型是否被调用、标签是否命中、是否兜底。但这还不够，因为上线后的教育 agent 不只是“单次提交分析器”，还要能追问学生、总结学习轨迹，并且在外部模型不稳定时给出可解释报告。

## Goals

- 建立统一评测入口，一次运行可以覆盖三类 AI 助手。
- 每条样本都能说明老师期望、AI 输出、AI 优点、AI 不足和失败类型。
- 区分运行失败和质量失败，避免把额度不足、超时、模型不支持误判成 agent 教学能力差。
- 让评测结果能直接指导下一轮 prompt、标准库、逻辑链或结构化输出改动。

## Non-Goals

- 不替换现有 AI 服务架构。
- 不依赖前端页面完成评测。
- 不引入外部模型作为唯一裁判，避免裁判模型带来额外不稳定和成本。
- 不一次性追求 100 条样本；先做高质量小集，后续可扩展。

## Architecture

新增测试侧能力，避免影响生产路径：

```text
assistant-eval-fixtures.json
        |
        v
AssistantEvalFixtureLoader
        |
        v
AssistantLiveEvalTest
        |
        +--> DiagnosticAgentService
        +--> CoachAgentService
        +--> AiReportService.enhanceGrowthReportMarkdown
        |
        v
AssistantLiveEvalReport
        |
        v
target/ai-eval-reports/assistant-live-eval-*.json
```

### Fixture

统一 fixture 按 `assistantType` 分流：

- `SUBMISSION_DIAGNOSIS`：题目、提交、测试结果、baseline、期望粗标签、期望细标签、必须提及、禁止提及。
- `COACH_QUESTION`：提交诊断摘要、主标签、hint policy、学生回答、证据 refs、期望追问信号、禁止泄题短语。
- `GROWTH_REPORT`：题目、提交时间线、期望报告维度、禁止结论。

每条样本必须包含：

- `caseId`
- `assistantType`
- `teacherExpectation`
- `rubric`
- `qualityNotes`

### Report

报告按条目记录：

- 样本信息：caseId、assistantType、teacherExpectation。
- 运行信息：model、promptVersion、latencyMs、status、fallbackUsed、failureReason。
- 质量信息：expectedSignalHit、evidenceValid、safetyPassed、teachingActionValid、outputSummary。
- 对比信息：aiBetterThanTeacher、teacherBetterThanAi、iterationSuggestion。

### Scoring

先采用客观规则评分：

- 是否命中期望信号。
- 是否引用已有证据。
- 是否避免完整答案、参考代码、隐藏数据、直接改法。
- 是否给出可执行但不过度泄题的下一步。
- 成长报告是否基于轨迹而非泛泛鼓励。

真实外部模型只作为被测对象，不作为主裁判。

## Error Handling

评测报告必须把以下情况独立标记：

- `INSUFFICIENT_QUOTA`
- `RATE_LIMITED`
- `TIMEOUT`
- `MODEL_UNSUPPORTED`
- `MODEL_RUNTIME_FALLBACK`
- `JSON_INVALID`
- `SAFETY_REJECTED`
- `QUALITY_MISS`

这能让后续迭代明确是要改模型配置、调用协议、提示词，还是评测样本/标准库。

## Testing

- 无 key：运行 fixture 结构测试，确保样本质量和评测规则稳定。
- 有 `AI_EVAL_API_KEY`：运行 live smoke eval，默认限制样本数量，避免不必要消耗。
- 有 `AI_EVAL_FULL=true`：运行完整 live eval。

## Iteration Rule

第一次 live eval 后必须至少做一轮复盘：

- 如果主要失败是调用层问题，优先增强报告与失败归因。
- 如果主要失败是诊断标签偏差，优先调整标准库裁剪或诊断 prompt。
- 如果主要失败是 Coach 泄题或追问泛，优先调整 Coach prompt 和安全门。
- 如果主要失败是成长报告泛化，优先调整轨迹输入和报告 rubric。
