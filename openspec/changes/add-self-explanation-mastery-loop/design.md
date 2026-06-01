## Context

当前系统已经有 `CoachAnswerQualityAnalyzer`，可以根据一次学生回答判断 `NO_ANSWER`、`VAGUE_ACK`、`DIRECTION_ONLY`、`EVIDENCE_GROUNDED`、`VERIFICATION_READY`、`TRANSFER_READY` 和 `SAFETY_RISK`，并输出证据类型、可验证性、教学动作和教师关注标记。`CoachInteractionAnalyzer` 会在提交维度汇总这些信息，学生轨迹、教师概览和 AI 质量概览已经能消费单次回答质量。

缺口在于：学生理解力不是一次回答决定的。老师更关心学生是否持续能给出最小样例、变量轨迹、输出对比、复杂度估算、反例或迁移解释；是否一直只说“懂了/我改”；是否在追问里反复要求答案或完整改法。当前系统没有把这些长期回答证据变成学生画像和推荐决策里的结构化状态。

## Goals / Non-Goals

**Goals:**

- 新增长期自解释能力信号，输出到学生能力画像、学习轨迹、教师概览、推荐和 AI 质量维度。
- 使用已有 CoachPrompt、提交、诊断分析和 Coach 回答质量规则动态推导，不新增事实源。
- 让推荐服务在自解释证据不足时优先给更小的解释练习，而不是继续加题或只重做。
- 让教师端看到自解释薄弱学生数量、班级摘要和学生行信号。
- 用后端测试验证证据充足、证据不足、持续空泛、安全风险、推荐消费和质量维度。

**Non-Goals:**

- 不新增数据库表或迁移。
- 不用外部模型评价学生理解力。
- 不替换现有 Coach 生成、诊断标签或复发误区信号。
- 不把自解释信号作为当前提交是否正确的唯一依据。

## Decisions

### Decision 1: 新增动态 `SelfExplanationMasteryAnalyzer`

分析器接收学生近期 CoachPrompt 列表，复用 `CoachAnswerQualityAnalyzer` 对每个学生回答打分，并输出：

- `status`: `NO_EVIDENCE`、`EMERGING`、`EVIDENCE_GROUNDED`、`TRANSFER_READY`、`NEEDS_COACHING`、`SAFETY_RISK`
- `label`: 面向学生/教师的短标签
- `summary`: 解释近期回答证据，不给学生贴“不会”的标签
- `evidenceCompleteness`: 0 到 1 的长期证据完整度
- `verifiableAnswerCount` / `transferReadyCount` / `vagueAnswerCount` / `safetyRiskCount` / `answeredTurnCount`
- `evidenceTypes`: 近期出现过的证据类型
- `evidenceRefs`: Coach prompt 证据引用
- `recommendedAction`
- `needsTeacherAttention`

动态分析优先于新表，因为 CoachPrompt 已经保存回答和时间，且第一版需要低成本覆盖历史数据。

### Decision 2: 状态阈值以“可验证解释比例”和“风险信号”共同判断

第一版采用保守阈值：

- 没有回答证据时为 `NO_EVIDENCE`。
- 安全风险回答达到 1 次且没有足够可验证回答时为 `SAFETY_RISK`。
- 空泛/只有方向的回答占多数，或至少 2 次空泛回答时为 `NEEDS_COACHING`。
- 至少 2 次可验证回答且证据完整度达到 0.55 时为 `EVIDENCE_GROUNDED`。
- 至少 1 次迁移解释，且整体证据完整度达到 0.7 时为 `TRANSFER_READY`。
- 其他有少量证据但还不稳定时为 `EMERGING`。

这样避免把一次好回答误判成稳定掌握，也避免把一次短回答直接升级为教师介入。

### Decision 3: 学生画像是长期状态入口，轨迹是当前作业入口

学生能力画像使用同一学生的近期 CoachPrompt，表达长期自解释能力。学习轨迹使用当前作业的 CoachPrompt，表达当前作业里的解释证据。两者复用同一 DTO 结构，避免前端和推荐系统解析两套自然语言。

### Decision 4: 推荐系统优先补“解释证据”而不是盲目加题

当 `status` 为 `NEEDS_COACHING` 或 `EMERGING` 时，推荐服务新增 `SELF_EXPLANATION_PRACTICE`，要求学生写最小样例、变量变化或输出对比。当 `SAFETY_RISK` 或长期没有回答证据且已有多轮追问时，新增 `TEACHER_EXPLANATION_REVIEW`，建议教师示范如何把提示转成证据解释。

### Decision 5: AI 质量维度检查“理解证据是否进入教学动作”

`SELF_EXPLANATION_MASTERY_LOOP` 不评价代码是否正确，而评价系统是否发现学生解释证据不足、空泛或越界，并转成 Coach、推荐或教师动作。维度输出状态、分数、摘要、证据引用和推荐改进动作，和现有质量维度保持一致。

## Risks / Trade-offs

- [Risk] 关键词规则可能误判简短但有效的回答。-> Mitigation: 用比例和多次证据判断长期状态，单次回答只影响趋势，不直接盖过提交结果。
- [Risk] 学生回答很少时样本不足。-> Mitigation: 输出 `NO_EVIDENCE` 或 `EMERGING`，不伪装为稳定结论。
- [Risk] 教师端状态密度继续增加。-> Mitigation: 教师端只显示摘要和学生行标签，细节仍放在 AI 质量折叠区。
- [Risk] 推荐过度偏向复盘。-> Mitigation: 自解释推荐只占一个高优先级槽，`TRANSFER_READY` 时不挤占加题推荐。

## Migration Plan

无需数据库迁移。部署后旧 CoachPrompt 可动态生成自解释信号；旧前端忽略新增字段。若需要回滚，删除新增展示和服务消费即可，现有诊断、Coach、推荐和复发误区字段不受影响。
