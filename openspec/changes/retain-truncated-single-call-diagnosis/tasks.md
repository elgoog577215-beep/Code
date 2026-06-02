## 1. OpenSpec

- [x] 1.1 编写 proposal、design、spec 和 tasks。
- [x] 1.2 运行 `openspec validate retain-truncated-single-call-diagnosis --strict`。

## 2. 截断诊断保留

- [x] 2.1 在 `AiReportService` 增加 single-call 截断子对象提取，仅提取完整 `diagnosisDecision`。
- [x] 2.2 复用 normalize/validate，只有校验通过才生成 partial completion。
- [x] 2.3 保留 `OUTPUT_TRUNCATED`、`streamFinishReason=length` 和本地安全 teaching 模板。

## 3. Live Eval 报告

- [x] 3.1 在 `LiveModelEvalReport` 增加 `partialCount`。
- [x] 3.2 更新 report 汇总逻辑，让 `completedCount` 只统计 `MODEL_COMPLETED`。
- [x] 3.3 补充无 live model 的 report 汇总测试，覆盖 partial/full/fallback 计数。

## 4. 验证

- [x] 4.1 扩展 `AiReportServiceExternalRuntimeTest`，覆盖截断 teaching hint 保留诊断。
- [x] 4.2 扩展截断诊断非法时仍 fallback 的测试。
- [x] 4.3 运行相关后端测试。
- [x] 4.4 运行 OpenSpec strict 校验。
- [x] 4.5 运行 `git diff --check` 和密钥扫描。
- [x] 4.6 运行一条低 token 真实 live model smoke，对比 fallback/partial/report 变化。
