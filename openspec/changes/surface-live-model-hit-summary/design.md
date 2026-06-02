## 背景

上一轮已经在 `LiveModelEvalReport` 中新增：

- final 命中：`issueTagHitCount` / `fineTagHitCount`
- model 命中：`modelIssueTagHitCount` / `modelFineTagHitCount`
- fallback 命中：`fallbackIssueTagHitCount` / `fallbackFineTagHitCount`

但真实 smoke 的控制台摘要仍输出 `issueHits=1, fineHits=1`。当实际状态是 `MODEL_RUNTIME_FALLBACK` 且 fallback 命中时，这行摘要会把“用户最终体验仍有诊断”与“外部模型完成并命中”混在一起。

## 方案

### 摘要语义显式化

新增一个 test-scope helper 生成 live-model-eval summary line，字段命名直接表达三层语义：

- `finalIssueHits` / `finalFineHits`
- `modelIssueHits` / `modelFineHits`
- `fallbackIssueHits` / `fallbackFineHits`

live smoke 控制台打印统一使用该 helper，结构测试也断言该 helper 在 fallback-only case 中仍显示 `modelIssueHits=0` 与 `fallbackIssueHits=1`。

### baseline regression 报告携带当前命中归因

`LiveEvalBaselineRegressionReport` 现在表达“当前 report 与 baseline 的对比”。对 live-model-eval 来说，如果对比报告只记录 case 覆盖数和 violations，还需要读回当前 report 才能确认本次命中归因。因此 `fromModel(...)` 会把当前 report 顶层命中计数复制到 regression report：

- `currentFinalIssueTagHitCount`
- `currentFinalFineTagHitCount`
- `currentModelIssueTagHitCount`
- `currentModelFineTagHitCount`
- `currentFallbackIssueTagHitCount`
- `currentFallbackFineTagHitCount`

assistant regression report 不填这些字段，避免把 assistant eval 和 model diagnosis eval 的语义混在一起。

## 风险与边界

- [Risk] 字段增多造成理解成本。→ 字段名保留 `final/model/fallback` 前缀，避免隐含语义。
- [Risk] 误以为 final 命中不重要。→ 保留 final 计数，因为它仍代表最终用户可见诊断是否命中。
- [Risk] 旧报告读取兼容性。→ 新增字段为 nullable，不移除旧字段。

## 验证

- OpenSpec strict validate。
- 无需 API Key 的结构测试覆盖 summary line 与 regression report 字段。
- 运行 live smoke；如果 ModelScope 返回 429/额度不足，仍应通过 fallback，并在控制台摘要中显示 model 命中为 0、fallback 命中为 1。
- 执行 secret scan，确认没有 API Key 写入仓库。
