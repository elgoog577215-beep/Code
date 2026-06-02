## 1. OpenSpec

- [x] 1.1 编写 proposal、design、spec 和 tasks。
- [x] 1.2 运行 `openspec validate add-output-truncated-runtime-signal --strict`。

## 2. 后端归因

- [x] 2.1 在 `ModelStageFailureReason` 新增 `OUTPUT_TRUNCATED`。
- [x] 2.2 更新 `AiReportService`，当 `streamFinishReason=length` 且解析/校验失败时修正 failure reason。
- [x] 2.3 更新 `ExternalModelFailureClassifier`，从异常文本识别 output truncated。

## 3. 草稿与质量归因

- [x] 3.1 更新 `LiveEvalRuntimeFixtureDraftFactory`，支持 `OUTPUT_TRUNCATED` 分类、建议和 mustMention。
- [x] 3.2 更新 `ClassroomService` runtime fixture draft 分类与行动建议。
- [x] 3.3 更新 `AiQualityOverviewService` runtime attribution 分类与行动建议。

## 4. 验证

- [x] 4.1 扩展 `AiReportServiceExternalRuntimeTest`，覆盖 length 截断归因。
- [x] 4.2 扩展 `AssistantLiveEvalQualityGateTest`，覆盖 live eval runtime draft 输出截断。
- [x] 4.3 运行相关后端测试。
- [x] 4.4 运行 OpenSpec strict 校验。
- [x] 4.5 运行 `git diff --check` 和密钥扫描。
- [x] 4.6 运行一条低 token 真实 live model smoke，确认 report 出现 `OUTPUT_TRUNCATED`。
