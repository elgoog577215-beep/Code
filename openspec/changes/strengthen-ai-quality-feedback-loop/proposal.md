## Why

项目已经具备单次诊断、外部模型 runtime、学习轨迹、Coach、教师纠错和 live eval，但这些能力仍分散在不同链路里。继续提升 AI 时，需要把“诊断是否准、证据是否可靠、提示是否安全、学习动作是否有效、模型调用是否稳定、教师纠错是否能沉淀”合成一个可观察、可验证、可迭代的质量反馈闭环。

本轮目标是让 AI 能力从“会生成教育化反馈”进一步升级为“能被教师和系统持续评估、定位风险并推动下一轮改进”的教育 agent。

## What Changes

- 新增 AI 质量维度画像，按诊断准确性、证据引用、提示安全、学习动作、模型运行、教师纠错沉淀输出结构化状态、评分、证据和建议动作。
- 增强教师端 AI 质量概览，让它不仅统计数量和比例，还能给出优先修复方向、评测沉淀就绪度和质量短板说明。
- 把学习动作证据和教师纠错纳入质量判断，区分“模型没调用好”“模型说得不安全”“诊断置信不足”“学生没有执行建议动作”“教师纠错应进入 eval”等不同风险来源。
- 保持现有 API 向后兼容：只新增字段，不移除旧字段。
- 增加后端测试，验证质量维度、优先级、评测就绪度和摘要策略不是自然语言偶然结果，而是结构化可断言行为。

## Capabilities

### New Capabilities

- `ai-quality-feedback-loop`: 定义 AI 质量反馈闭环，包括质量维度画像、改进优先级、评测沉淀就绪度和教师端解释策略。

### Modified Capabilities

- `online-education-agent-quality`: 在已有在线教育 agent 质量能力上，增加结构化质量维度和闭环改进输出。

## Impact

- 后端服务：`AiQualityMetrics`、`AiQualityOverviewService`、`LearningActionEvidenceAnalyzer`、`DiagnosisReportReader` 相关读取链路。
- API DTO：`AiQualityOverviewResponse` 新增兼容字段，供教师端或后续前端消费。
- 测试：扩展 AI 质量概览测试，增加质量维度、改进优先级、评测沉淀就绪度和学习动作效果覆盖。
- OpenSpec：新增本 change 的 proposal、design、spec 和 tasks。
