## Context

当前复杂样本已经包含主错因、次错因、干扰信号、教学优先级、证据引用和 6 个 expectedMetrics。`ModelDiagnosisEvalTest` 目前只判断 issue/fine tag 命中、证据非空和安全风险，无法区分“命中了某个标签”与“在多错因中优先教对了根因”。这会让外接模型在复杂样本上看起来达标，但实际上可能被次要信号或干扰信号带偏。

## Goals / Non-Goals

**Goals:**

- 把复杂样本 6 个质量指标转成稳定、可单测的评分器。
- 在 live model eval report 中记录复杂样本质量通过情况。
- 让质量 baseline draft 与 baseline regression gate 可以保留复杂质量信号，防止后续 prompt/model 回退。
- 保持现有 100 条 fixture、24 条 live 候选和现有 live eval 调用方式不变。

**Non-Goals:**

- 不要求本轮真实调用外接模型。
- 不修改生产 `DiagnosticAgentService` 的行为。
- 不把自然语言评分交给外接模型判断。
- 不新增数据库字段或前端展示。

## Decisions

- 评分器放在 test/eval 侧，输入为复杂 fixture 真值和 `SubmissionAnalysisResponse`。这样能验证能力，又不会影响线上诊断链路。
- 每个复杂 case 计算 6 个布尔指标和一个分数；report 只做聚合，不重新解释诊断文本。
- `primaryRootCauseHit` 以 primary fine tag 命中为主，避免仅命中粗标签就被视为复杂根因定位成功。
- `teachingPriorityCorrect` 要求输出围绕 fixture 的 `mustMention` 和主错因展开，代表“先教什么”方向正确。
- `secondaryIssuesNotOverweighted` 要求 primary fine tag 位于实际 fine tags 首位，避免模型把次错因排在主错因前。
- `distractingSignalsIgnored` 检查输出没有复述 fixture 明确标记的干扰信号。
- `evidenceGrounded` 检查实际证据引用命中 fixture required evidence refs。
- `noFullSolutionLeak` 复用 fixture mustNotMention 和 answerLeakRisk，确保复杂样本不会以泄露答案换取诊断命中。

## Risks / Trade-offs

- [Risk] 字符串型指标可能对表达方式敏感 -> 使用短 mustMention/mustNotMention token，且只作为复杂样本门禁，不影响生产输出。
- [Risk] 外接模型短期可能无法全部通过严格证据引用 -> report 会记录失败项，baseline draft 只从通过样本生成。
- [Risk] 字段增加可能影响旧报告读取 -> 只新增可空字段，保留既有 report schema。
- [Risk] 评分器过度依赖中文提示词 -> 主错因和证据指标仍以结构化 tag/ref 为主，文本指标只用于教学优先级与干扰/泄露检查。
