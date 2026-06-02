## 1. OpenSpec

- [x] 1.1 编写 proposal、design、spec 和 tasks。
- [x] 1.2 运行 `openspec validate separate-live-eval-model-fallback-hits --strict`。

## 2. Report 指标拆分

- [x] 2.1 扩展 `LiveModelEvalReport` summary，新增 model/fallback issue/fine hit counts。
- [x] 2.2 扩展 `LiveModelEvalReport.Entry`，新增 modelCompleted、modelIssueTagHit、modelFineTagHit、fallbackIssueTagHit、fallbackFineTagHit。
- [x] 2.3 更新 `ModelDiagnosisEvalTest.runLiveEvalReport`，按 status/fallbackUsed 写入模型与 fallback 命中字段。
- [x] 2.4 更新 `summarizeReport`，分别汇总最终命中、模型命中和 fallback 命中。

## 3. Quota/Rate Limit 归因

- [x] 3.1 更新 `LiveEvalRuntimeFixtureDraftFactory`，将 `RATE_LIMITED`、`429` 也归为 `QUOTA_LIMIT`。
- [x] 3.2 确认 rate-limited runtime draft 触发 offline profile guidance。
- [x] 3.3 保持非 quota provider error 不触发 offline profile guidance。

## 4. 测试与真实证据

- [x] 4.1 扩展无 live model 汇总测试，覆盖 completed、partial、runtime fallback 的 model/fallback 命中拆分。
- [x] 4.2 扩展 runtime draft factory 测试，覆盖 `RATE_LIMITED` 和非 quota provider error。
- [x] 4.3 运行一条真实 low-latency live smoke，确认 quota/rate limit report 不再抬高 model hit count。

## 5. 验证

- [x] 5.1 运行相关后端测试。
- [x] 5.2 运行 OpenSpec strict 校验。
- [x] 5.3 运行 `git diff --check` 和密钥扫描。
