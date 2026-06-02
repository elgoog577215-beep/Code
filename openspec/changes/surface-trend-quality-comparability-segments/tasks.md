## 1. 后端趋势信号

- [x] 1.1 扩展 `AiQualityTrendResponse.SourceQualitySegment`，新增质量可比性状态、摘要、原因数量和原因列表。
- [x] 1.2 在 `AiQualityTrendService` 中根据 recovery、模型完成、fallback、partial 和 runtime failure 派生 source segment 可比性。
- [x] 1.3 扩展 `AiQualityTrendServiceTest`，覆盖 blocked、recovered 和 partial 来源可比性。

## 2. 教师端展示

- [x] 2.1 扩展前端 `AiQualitySourceSegment` 类型。
- [x] 2.2 在来源质量列表展示可比性 chip、摘要和最多两条原因。
- [x] 2.3 复用现有紧凑样式，确保与 recovery/transport chip 并列时不撑破布局。

## 3. 验证

- [x] 3.1 运行 `AiQualityTrendServiceTest`。
- [x] 3.2 运行前端 typecheck。
- [x] 3.3 运行 OpenSpec strict validate、secret scan 和 `git diff --check`。
