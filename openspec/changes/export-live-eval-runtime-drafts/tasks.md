## 1. OpenSpec

- [x] 1.1 编写 proposal、design、tasks 和 spec delta。
- [x] 1.2 运行 `openspec validate export-live-eval-runtime-drafts --strict`。

## 2. 报告 DTO 与草稿工厂

- [x] 2.1 新增 test-scope `LiveEvalRuntimeFixtureDraft` DTO。
- [x] 2.2 新增 `LiveEvalRuntimeFixtureDraftFactory`，支持 assistant entries 与 model diagnosis entries。
- [x] 2.3 实现 failureType 分类、expectedRuntimeAction、mustMention/mustNotMention、evidenceRefs 和脱敏逻辑。

## 3. live eval 报告接入

- [x] 3.1 扩展 `AssistantLiveEvalReport`，新增 `runtimeFixtureDraftCount` 和 `runtimeFixtureDrafts`。
- [x] 3.2 扩展 `LiveModelEvalReport`，新增 `runtimeFixtureDraftCount` 和 `runtimeFixtureDrafts`。
- [x] 3.3 在 `AssistantLiveEvalTest.summarize(...)` 中填充 runtime 草稿。
- [x] 3.4 在 `ModelDiagnosisEvalTest.summarizeReport(...)` 中填充 runtime 草稿。

## 4. 测试与验证

- [x] 4.1 扩展或新增无需 API Key 的结构测试，覆盖 fallback、partial、quality miss 和脱敏。
- [x] 4.2 运行相关后端测试。
- [x] 4.3 运行 OpenSpec 校验和 `git diff --check`。
