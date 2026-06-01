## Why

上一轮已经把 AI 质量风险做成了结构化维度，但 Coach 对话仍主要停留在“有没有回答、有没有提到证据”的粗粒度判断。教育 agent 要继续进化，需要能判断学生回答是否真正包含可验证证据、是否达到理解/迁移层级，以及下一轮应该追问、降级还是复盘。

本轮目标是把 Coach 从“追问器”升级为“理解证据观察器”，让学生回答本身成为 AI 质量闭环和教师介入判断的一部分。

## What Changes

- 扩展 Coach 回答质量信号，新增理解层级、证据完整度、可验证性、行动状态和建议教学动作。
- 强化回答分析规则，区分空泛确认、只有方向、单一证据、多证据验证、可迁移复盘和疑似答案泄露。
- 让教师端 AI 质量概览纳入 Coach 回答质量，补充 Coach 理解维度和改进优先级。
- 保持兼容：不新增数据库字段，不移除现有 `CoachAnswerQualitySignal` 字段，只新增 DTO 字段和确定性分析逻辑。
- 补充 Coach 回答分析、交互汇总和 AI 质量概览测试。

## Capabilities

### New Capabilities

- `coach-understanding-evidence`: 定义 Coach 对学生回答的理解证据分析，包括理解层级、证据完整度、可验证性和下一步教学动作。

### Modified Capabilities

- `ai-quality-feedback-loop`: 将 Coach 回答质量纳入 AI 质量维度和改进优先级。

## Impact

- 后端 Coach 链路：`CoachAnswerQualityAnalyzer`、`CoachInteractionAnalyzer`、Coach 相关 DTO。
- 教师 AI 质量概览：`AiQualityMetrics`、`AiQualityOverviewService`、`AiQualityOverviewResponse`。
- 前端共享类型：Coach 回答质量与 AI 质量概览新增字段类型。
- 测试：Coach 回答质量、Coach 交互汇总、AI 质量概览相关测试。
