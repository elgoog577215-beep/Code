## 1. OpenSpec 与报告契约

- [x] 1.1 新增 `add-ai-agent-evaluation-report` proposal、design、spec，定义四维评估画像。
- [x] 1.2 扩展 `external-ai-assistant-eval-loop` spec，要求 live eval 输出四维能力画像。

## 2. 报告结构与汇总

- [x] 2.1 扩展 `AssistantLiveEvalReport`，新增 `EvaluationProfile`、准确率、速度、稳定性、教育有效性子结构。
- [x] 2.2 在 `AssistantLiveEvalTest` 汇总阶段生成 `evaluationProfile`。
- [x] 2.3 速度画像计算平均、P50、P90、P95、最大延迟和慢样本 caseId。
- [x] 2.4 稳定性画像计算 completedOutput 率、runtime failure 率、fallback 率和 route failure 概况。
- [x] 2.5 教育有效性画像标记真实学生改善是否已测量，并输出代理指标。

## 3. 质量门与测试

- [x] 3.1 扩展 `AssistantLiveEvalQualityGate`，优先读取 `evaluationProfile`，并保留旧报告兜底。
- [x] 3.2 增加质量门测试，覆盖准确率、速度、稳定性、教育有效性的具体违规项。
- [x] 3.3 增加报告 round-trip 测试，确认 `evaluationProfile` 可以写入并读回。

## 4. 验证与沉淀

- [x] 4.1 运行 `openspec validate add-ai-agent-evaluation-report --strict`。
- [x] 4.2 运行相关后端定向测试。
- [x] 4.3 更新项目记忆文档，沉淀 AI agent 四维评估口径。
