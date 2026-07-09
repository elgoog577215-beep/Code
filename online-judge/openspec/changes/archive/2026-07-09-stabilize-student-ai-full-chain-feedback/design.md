## Decisions

### 1. `studentReport` 只做摘要

`studentReport` 可以继续存在，但不能替代结构化 advice 数组。非 AC 且自由诊断有多个有效 issue 时，最终输出至少需要 `min(2, issueCount)` 条基础建议和提高建议。数量不足时只允许模型重试一次，不用后端模板补齐。

### 2. 结构化重试只用于可恢复输出

自由诊断、标准库逐层挂接和 advice 都可以走结构化重试，但只在输出截断、空内容或明显包含目标 schema 片段时触发。随机解释文本不重试，避免扩大限流风险。

### 3. 失败观测分层

学生端继续隐藏原始失败细节；教师端和观测数据使用 `aiInvocation.failureStage` 与 `failureReason`，例如 `FREE_DIAGNOSIS:OUTPUT_TRUNCATED`。如果学生反馈记录只有 `FULL_CHAIN_FAILED`，则从同 submission 的分析记录补真实原因。

### 4. 旁路失败不得影响主成功

推荐事件 backfill 和标准库成长候选都是旁路。它们失败时记录日志，不改变已经成功生成和验证的 AI 诊断。

## Risk Controls

- 不增加新队列或复杂限流器，只限制每阶段最多一次结构化重试。
- 不改变学生端 API schema。
- 不恢复本地规则或快链路。
- 不强制所有题都返回固定 2 条，只有多 issue 场景才启用数量门禁。
