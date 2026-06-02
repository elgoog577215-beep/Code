## 1. OpenSpec

- [x] 1.1 编写 proposal、design、spec 和 tasks。
- [x] 1.2 运行 `openspec validate preserve-runtime-fixture-transport-telemetry --strict`。

## 2. 后端 runtime fixture draft

- [x] 2.1 扩展 `DiagnosisEvalFixtureDraftResponse.RuntimeFixtureDraft` transport telemetry 字段。
- [x] 2.2 更新 `ClassroomService.toRuntimeFixtureDraft` 写入 transport telemetry。
- [x] 2.3 更新 runtime mustMention、source artifacts 和 expected action，保留 stream no-content/invalid/retry 证据。
- [x] 2.4 扩展 `ClassroomServiceCorrectionTest`，覆盖 quota stream no-content 与 budget guard 空 transport。

## 3. Live eval runtime draft

- [x] 3.1 扩展 `LiveEvalRuntimeFixtureDraft` transport telemetry 字段。
- [x] 3.2 更新 `LiveEvalRuntimeFixtureDraftFactory.fromModelEntry` 保留 model report transport telemetry。
- [x] 3.3 扩展 live eval runtime draft factory/gate 测试。

## 4. 教师端预览

- [x] 4.1 前端 runtime fixture 草稿预览展示 transport chip。
- [x] 4.2 确认前端类型与后端 DTO 一致。

## 5. 验证

- [x] 5.1 运行相关后端测试。
- [x] 5.2 运行前端 typecheck。
- [x] 5.3 运行 OpenSpec strict 校验。
- [x] 5.4 运行 `git diff --check`。
