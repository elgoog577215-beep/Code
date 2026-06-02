## 1. OpenSpec

- [x] 1.1 编写 proposal、design、tasks 和 spec delta。
- [x] 1.2 运行 `openspec validate export-live-eval-quality-baselines --strict`。

## 2. Baseline DTO 与 Factory

- [x] 2.1 新增 test-scope `LiveEvalQualityBaselineDraft` DTO。
- [x] 2.2 新增 `LiveEvalQualityBaselineDraftFactory`，支持 assistant entries 与 model diagnosis entries。
- [x] 2.3 实现成功样本筛选、mustKeep、evidenceRefs、regressionPurpose、mustNotMention 和脱敏逻辑。

## 3. live eval 报告接入

- [x] 3.1 扩展 `AssistantLiveEvalReport`，新增 `qualityBaselineDraftCount` 和 `qualityBaselineDrafts`。
- [x] 3.2 扩展 `LiveModelEvalReport`，新增 `qualityBaselineDraftCount` 和 `qualityBaselineDrafts`。
- [x] 3.3 在 `AssistantLiveEvalTest.summarize(...)` 中填充 baseline 草稿。
- [x] 3.4 在 `ModelDiagnosisEvalTest.summarizeReport(...)` 中填充 baseline 草稿。

## 4. 测试与验证

- [x] 4.1 扩展无需 API Key 的结构测试，覆盖成功样本 baseline、低质量排除和脱敏。
- [x] 4.2 运行相关后端测试。
- [x] 4.3 运行真实 live eval smoke，确认成功样本生成 baseline 草稿。
- [x] 4.4 运行 OpenSpec 校验和 `git diff --check`。
