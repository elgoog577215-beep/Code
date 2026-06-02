## 1. 后端可比性信号

- [x] 1.1 扩展 `RuntimeAttributionSignal` DTO，新增质量可比性状态、摘要、原因数量和原因列表。
- [x] 1.2 在 `AiQualityOverviewService` 中根据 recovery、模型完成、fallback 和 partial 指标派生质量可比性。
- [x] 1.3 扩展 `AiQualityOverviewServiceTest`，覆盖 BLOCKED 不可比、RECOVERED 可比、健康不适用和 partial 部分可比。

## 2. 教师工作台展示

- [x] 2.1 扩展前端 API 类型，声明质量可比性字段。
- [x] 2.2 在教师工作台模型归因块展示可比性 chip、摘要和最多两条原因。
- [x] 2.3 补充紧凑样式，确保移动端和桌面端不挤压现有 recovery/transport 信息。

## 3. 验证

- [x] 3.1 运行 `AiQualityOverviewServiceTest`。
- [x] 3.2 运行前端 typecheck。
- [x] 3.3 运行 OpenSpec strict validate、secret scan 和 `git diff --check`。
