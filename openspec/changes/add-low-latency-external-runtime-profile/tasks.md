## 1. OpenSpec

- [x] 1.1 编写 proposal、design、spec 和 tasks。
- [x] 1.2 运行 `openspec validate add-low-latency-external-runtime-profile --strict`。

## 2. Profile 与输入瘦身

- [x] 2.1 增加外部 runtime profile 配置，默认 `standard`，支持 `low-latency`。
- [x] 2.2 为 low-latency profile 生成 compact `ModelDiagnosisBrief`。
- [x] 2.3 为 low-latency profile 生成 compact `StandardLibraryPack`。
- [x] 2.4 保持 tag/evidence/safety validation 不放宽。

## 3. 请求遥测与报告

- [x] 3.1 在 `AiInvocation` 增加 runtimeProfile/requestBytes/requestCompact。
- [x] 3.2 在 chat completion 请求前记录 request byte size 和 compact 标记，不保存 raw request。
- [x] 3.3 在 `LiveModelEvalReport` entry 和 runtime draft 中导出 profile/request size。

## 4. 测试

- [x] 4.1 扩展 `AiReportServiceExternalRuntimeTest`，验证 low-latency profile 请求体更小且仍完成。
- [x] 4.2 扩展 `ModelDiagnosisEvalTest`，验证 report 汇总包含 profile/request telemetry。
- [x] 4.3 扩展 runtime draft 测试，慢响应 mustMention 包含 requestBytes。

## 5. 验证

- [x] 5.1 运行相关后端测试。
- [x] 5.2 运行 OpenSpec strict 校验。
- [x] 5.3 运行 `git diff --check` 和密钥扫描。
- [x] 5.4 运行 standard 与 low-latency 真实 live eval 对比，记录 latency/chunk/requestBytes/质量差异。
