## 1. OpenSpec

- [x] 1.1 编写 proposal、design、spec 和 tasks。
- [x] 1.2 运行 `openspec validate add-external-model-stream-telemetry --strict`。

## 2. 调用 telemetry 结构

- [x] 2.1 扩展 `SubmissionAnalysisResponse.AiInvocation`，新增安全的 transport/stream telemetry 字段。
- [x] 2.2 在 `AiReportService` 中新增最后一次调用 telemetry 结构和 reset/merge 逻辑。
- [x] 2.3 在 stream/non-stream 响应解析中统计 chunk、reasoning、invalid chunk、finish reason 和 fallback retry。
- [x] 2.4 确保 telemetry 不包含原始 prompt、响应正文、chunk 内容或密钥。

## 3. live eval 报告

- [x] 3.1 扩展 `LiveModelEvalReport.Entry`，输出 invocation transport telemetry。
- [x] 3.2 在 `ModelDiagnosisEvalTest` 写 entry 时复制 telemetry 字段。

## 4. 测试与验证

- [x] 4.1 扩展无需 API Key 的结构测试，覆盖 stream chunk 统计和 reasoning 不进入 content。
- [x] 4.2 扩展 non-stream fallback 到 stream 测试，断言 telemetry 记录 fallback retry。
- [x] 4.3 运行相关后端测试。
- [x] 4.4 使用真实 ModelScope smoke 确认 telemetry 出现在 report，并记录当前额度不足状态。
- [x] 4.5 运行 OpenSpec 校验和 `git diff --check`。
