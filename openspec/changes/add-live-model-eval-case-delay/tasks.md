## 1. OpenSpec

- [x] 1.1 新增 `live-model-eval-case-delay` 能力规格。
- [x] 1.2 说明与既有 `external-model-call-stability` 的节流语义一致。

## 2. 报告结构

- [x] 2.1 为 `LiveModelEvalReport` 增加 `caseDelayMs`。
- [x] 2.2 为 `LiveModelEvalReport` 增加 `delayedCaseCount`。

## 3. live model eval 接入

- [x] 3.1 `ModelDiagnosisEvalTest` 读取 `AI_EVAL_CASE_DELAY_MS`。
- [x] 3.2 在 live model eval case 之间等待，且不把等待计入单 case latency。
- [x] 3.3 控制台摘要输出 `caseDelayMs` 与 `delayedCases`。

## 4. 验证

- [x] 4.1 补充结构测试覆盖 case delay 报告字段和摘要。
- [x] 4.2 运行相关后端测试。
- [x] 4.3 运行 OpenSpec strict validate、secret scan 和 `git diff --check`。
