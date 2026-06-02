## 1. OpenSpec

- [x] 1.1 创建 proposal/design/spec/tasks，定义 recovery smoke guidance。
- [x] 1.2 运行 `openspec validate add-runtime-recovery-smoke-guidance --strict`。

## 2. live eval runtime draft

- [x] 2.1 扩展 `LiveEvalRuntimeFixtureDraft`，新增 recovery smoke guidance 字段。
- [x] 2.2 更新 `LiveEvalRuntimeFixtureDraftFactory`，为 quota/rate limit、budget guard、provider error、timeout 和 stream no-content 生成 recovery smoke guidance。
- [x] 2.3 确保 command hint 不包含 API Key/token，并输出可复现的 smoke test 入口和 required checks。

## 3. 教师端 runtime draft

- [x] 3.1 扩展 `DiagnosisEvalFixtureDraftResponse.RuntimeFixtureDraft`，新增同名 recovery smoke guidance 字段。
- [x] 3.2 更新 `ClassroomService` runtime fixture draft 导出，填充生产端恢复 smoke 字段。

## 4. 测试与验证

- [x] 4.1 扩展 `AssistantLiveEvalQualityGateTest`，覆盖 quota/rate limit、stream no-content、provider error 和 quality miss 的 recovery smoke guidance。
- [x] 4.2 扩展教师端 fixture draft 测试，覆盖 quota runtime draft 的 recovery smoke 字段。
- [x] 4.3 运行相关后端测试。
- [x] 4.4 运行真实 live-model-eval smoke；当前若仍为 429，应确认 report runtime draft 携带 recovery smoke guidance。
- [x] 4.5 运行 secret scan 和 diff check。
