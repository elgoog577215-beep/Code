## Context

当前外接模型链路已经有 `ModelDiagnosisBrief`、`StandardLibraryPack`、runtime prompt、validator、fallback attribution 和 live eval report。最近的真实报告暴露出两个问题：复杂样本中 fallback/partial 会掩盖模型能力；单次大 JSON 容易让模型把注意力放在补字段，而不是完成教育判断。

本轮不把本地规则做得更强，也不把 fallback 当成 AI 能力。系统仍需要本地逻辑打包输入、做安全校验和落库，但能力提升目标只看真实外接模型是否能做出更好的教学判断。

## Goals / Non-Goals

**Goals:**

- 让外接模型输出显式教育判断：主错因、证据、次要信号取舍、教学优先级、继续提升方向和下一步动作。
- 将标准库表达为模型教学协议，而不只是可选标签列表。
- 降低 prompt 机械填表压力，让模型把 token 用在分析和教学决策上。
- 评测只统计真实模型完成的教育 agent 能力，不把本地 fallback 或模板补齐算作模型能力。

**Non-Goals:**

- 不新增学生端 DTO，不重做前端展示。
- 不扩展本地规则诊断能力，不增加新的本地 bug pattern 检测器。
- 不提交或硬编码任何外接模型 token。
- 不默认跑完整 14 条 live eval，除非显式配置 API key 和 full eval 环境变量。

## Decisions

### 外接模型输出增加教育判断字段

在 `DiagnosisJudgeOutput` 上新增模型原生判断字段：`primaryReasoning`、`secondaryIssues`、`distractorNotes`、`teachingPriority`、`improvementOpportunities` 和 `nextLearningAction`。这些字段不替代现有标签和证据字段，而是让外接模型把“为什么这样教”讲清楚，供评测和学生反馈组装使用。

替代方案是只改 prompt 文案，不改结构。但那会让教育判断继续藏在自然语言里，评测也难以区分模型是否真的做了复杂信号取舍。

### 标准库升级为教育 agent 协议

在 `StandardLibraryPack` 中新增 `educationAgentProtocol`，聚合主错因优先级、证据要求、次要信号取舍、提升建议和安全表达规则。它和现有 taxonomy 共存，但 prompt 优先要求模型遵循协议做教学判断。

替代方案是继续往 `decisionProtocol` 和 `studentFeedbackRules` 里追加规则。该方式能工作，但语义分散，后续迭代标准库时不容易看出哪部分是在放大外接模型教育能力。

### Prompt 聚焦教学判断而不是完整学生反馈 JSON

保留现有 `diagnosis-and-teaching-v3` 兼容，但将其文案改成教育 agent 任务顺序：先给主错因和证据，再给次要/干扰说明，最后给下一步动作和提升点。输出仍是结构化 JSON，但字段更短、更接近教育判断。

替代方案是把默认 runtime 改回两阶段调用。两阶段更稳，但会改变调用成本和生产路径。本轮先不改变默认 runtime，只让当前单次协议更像教育 agent，后续再按 live report 决定是否切 runtime mode。

### 学生反馈继续由后端安全组装兜底

当模型输出了有效 `studentFeedback` 时可以直接使用；当模型只完成教育判断字段时，后端可以把模型的判断字段组装进 `studentFeedback`。这不是本地规则增强，而是把真实模型教育判断转成学生端契约。

### live eval 强调真实模型教育能力

report entry 增加教育 agent 画像字段，统计模型是否输出了主错因理由、次要取舍、干扰说明、提升建议和下一步动作。只有 `modelCompleted=true` 且 `fallbackUsed=false` 的样本进入教育 agent 能力分。

## Risks / Trade-offs

- [Risk] 新增字段会略微增加输出长度。→ 字段保持短句，prompt 明确限制每项一句话，不要求长文。
- [Risk] 模型输出教育判断但 `studentFeedback` 缺失。→ 后端可用模型判断字段组装学生反馈，并标记为模型判断来源。
- [Risk] 安全校验过严导致有效模型判断被拒。→ 继续允许 partial：有效诊断保留，泄题或无效学生表达由本地安全模板补齐，但不计为完整模型学生反馈能力。
- [Risk] live eval 配额不足。→ 无 key 时只跑结构测试；有 key 时先跑 2 条 smoke，再显式跑 14 条复杂代表集。
