## 1. OpenSpec

- [x] 1.1 编写 proposal、design、spec 和 tasks。
- [x] 1.2 运行 `openspec validate surface-transport-telemetry-attribution --strict`。

## 2. Reader 与 DTO

- [x] 2.1 扩展 `DiagnosisReportReader.AiInvocationSnapshot`，读取 transport telemetry 字段。
- [x] 2.2 扩展 `AiQualityOverviewResponse.RuntimeAttributionSignal`，输出 transport mode 与 stream 计数。
- [x] 2.3 扩展 `AiQualityTrendResponse.SourceQualitySegment`，输出 transport mode 与 stream 计数。
- [x] 2.4 更新前端共享 API 类型。

## 3. 归因逻辑

- [x] 3.1 在 AI 质量概览 runtime attribution 中统计 stream no-content、invalid chunk、fallback retry。
- [x] 3.2 让 summary/action 区分 quota stream no-content 与 budget guard short-circuit。
- [x] 3.3 在 AI 质量趋势 source segment 中汇总 transport telemetry。

## 4. 测试与验证

- [x] 4.1 扩展 `AiQualityOverviewServiceTest`，覆盖 quota stream no-content 与 budget guard 区分。
- [x] 4.2 扩展 `AiQualityTrendServiceTest`，覆盖 source segment transport 计数。
- [x] 4.3 运行相关后端测试。
- [x] 4.4 运行前端 typecheck。
- [x] 4.5 运行 OpenSpec 校验和 `git diff --check`。
