## Context

复杂评测样本已经包含标准主因、必需证据、必须覆盖概念、干扰信号、安全禁区和期望教学优先级。现有质量评估把这些 gold 信息拆成多组 metric，但 metric 分散在 `complexQuality`、`intelligenceQuality`、`educationAgentQuality`、`modelTraceQuality`、`studentFeedbackQuality` 中，导致主口径不统一。

本设计把 gold 信息提升为一个显式 rubric，再让一个 evaluator 输出主质量结果。旧指标不再参与主摘要，只作为 legacy 兼容字段保留，避免一次性破坏历史报告读取。

## Decisions

### 使用 gold rubric 作为权威标准答案

`DiagnosisQualityRubric` 由 `ComplexStudentSubmissionEvalFixtureLoader.Fixture` 生成，包含：

- 标准主因：issue tag、fine tag、why primary。
- 必须证据：primary evidence ref、required evidence refs。
- 必须覆盖概念：mustMention。
- 必须压低的干扰项：secondary issues、distracting signals。
- 禁止泄露内容：mustNotMention。
- 期望教学优先级与下一步动作要求。

不使用外部大模型作为裁判，因为评测标准必须可复现、可审计、可离线运行。

### 单一链路 evaluator 输出主质量结果

`DiagnosisChainRubricEvaluator` 输出五段 verdict 和一个 overall verdict：

- `evidenceVerdict`：证据引用与失败证据定位。
- `rootCauseVerdict`：主因标签、主因说明和 gold 概念覆盖。
- `distractorVerdict`：次要/干扰信号没有抢占主因。
- `teachingVerdict`：学生反馈把阻塞问题放在前面，下一步可观察且可检查。
- `safetyVerdict`：不泄题，且 answer leak risk 不为 HIGH。

总体正确口径：`evidence/rootCause/safety` 必须通过，`teaching` 必须通过；`distractor` 进入分数与失败原因，但不让没有干扰项的样本被误伤。

### report 主摘要只展示 rubric chain quality

live/offline eval report 保留 runtime、命中、fallback 等事实字段；主质量摘要改为 rubric chain count、score、stage pass/fail 和 failedReasons。旧质量字段标记为 legacy 兼容，不再进入 console summary 和主质量结论。

### fallback 不进入模型正确率

只有 `modelCompleted=true && fallbackUsed=false && complexCase=true` 的 entry 进入 rubric chain evaluated 计数。fallback、本地规则命中、异常和 quota failure 只进入 runtime 状态。

## Risks / Trade-offs

- [Risk] 旧 comparison 报告仍引用 legacy 指标。→ 本轮先让 live eval 主摘要和主报告切换主口径，comparison 可读取新增 chain score，旧字段只作为兼容。
- [Risk] rubric 判分过严导致早期正确率偏低。→ 输出分段 failedReasons，先校准标准答案与 evaluator，再把正确率作为硬门禁。
- [Risk] 删除旧字段会造成历史报告反序列化或测试大面积破坏。→ 不做无意义补丁，但保留必要兼容字段；核心结论完全迁移到 rubric chain。
