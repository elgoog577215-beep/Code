## 背景

真实 ModelScope live smoke 当前返回 429 今日额度超限。已有能力已经做到：

- live-model-eval entry 记录 `MODEL_RUNTIME_FALLBACK`、`RATE_LIMITED`、stream chunk 计数和 fallback 命中。
- runtime fixture draft 把 `RATE_LIMITED` 归入 `QUOTA_LIMIT`。
- quota draft 输出 `offlineProfileEvalRecommended`，引导在额度不可用期间运行 offline runtime profile eval。

缺口是“恢复后验证”仍是自然语言。对于长期提升外接大模型能力来说，维护者需要两个分离步骤：

1. 额度不可用期间，用 offline profile 证明低延迟 profile 的请求体积和结构锚点是健康的。
2. 额度恢复后，用最小 live smoke 证明真实模型完成、未 fallback、stream 有 content chunk、model hit 与 evidence 有效。

## 字段设计

在 runtime fixture draft 中新增：

- `recoverySmokeRecommended`
- `recoverySmokeCaseId`
- `recoverySmokeRuntimeProfile`
- `recoverySmokeCommandHint`
- `recoverySmokeRequiredChecks`

命名使用 recovery smoke，而不是 live smoke，强调这是“外部依赖恢复后”的验证入口，不替代正常全量 live eval。

## 触发规则

### live eval runtime draft

为以下 failureType 推荐 recovery smoke：

- `QUOTA_LIMIT`
- `BUDGET_GUARD`
- `PROVIDER_ERROR`
- `TIMEOUT`

如果 transport 是 stream 且 `streamContentChunkCount=0`，即使 failureType 不是上面几类，也推荐 recovery smoke，因为需要验证 provider 恢复后是否能产生 content chunk。

默认 smoke profile 使用 entry 的 `runtimeProfile`；为空时使用 `low-latency`。command hint 只包含环境变量名、测试名和 profile，不包含任何 API Key。

required checks 包含：

- `status=MODEL_COMPLETED`
- `fallbackUsed=false`
- `modelCompleted=true`
- `modelIssueTagHit=true or modelFineTagHit=true`
- `evidenceValid=true`
- `safetyPassed=true`
- stream 场景追加 `streamContentChunkCount>0`

### 教师端 runtime draft

生产端没有 live eval caseId，但有 submissionId。因此：

- caseId 使用 `submission:<id>`。
- runtimeProfile 使用 `runtimeMode`。
- command hint 引导运行同 assignment/submission 的最小 live eval 或诊断 smoke，不硬编码密钥。
- required checks 使用 `aiInvocation.status=MODEL_COMPLETED`、`fallbackUsed=false`、`evidenceRefs present` 等生产可解释字段。

## 边界

- 不自动执行恢复 smoke；本轮只把下一步验证结构化。
- 不把 command hint 做成可直接复制包含密钥的命令。
- 不改变 runtime failure 分类结果。
- 不改变 offline profile guidance；它与 recovery smoke 并列。

## 验证

- OpenSpec strict validate。
- 无需 API Key 的结构测试覆盖 quota/rate limit、budget guard、provider error、stream no-content 和非 runtime quality miss。
- 教师端 draft 测试覆盖 production DTO 字段。
- 真实 live smoke 在当前 429 下应继续生成 recovery smoke guidance。
- secret scan 确认字段和 command hint 不包含 API Key。
