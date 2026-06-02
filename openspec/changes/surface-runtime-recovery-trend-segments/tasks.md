## 1. OpenSpec

- [x] 1.1 编写 proposal、design、spec 和 tasks，定义趋势 source segment recovery 状态。
- [x] 1.2 运行 `openspec validate surface-runtime-recovery-trend-segments --strict`。

## 2. 后端趋势信号

- [x] 2.1 扩展 `AiQualityTrendResponse.SourceQualitySegment` recovery 字段。
- [x] 2.2 更新 `AiQualityTrendService.SourceAccumulator`，聚合 recovery 状态、blocked reasons 和 required checks。
- [x] 2.3 扩展 `AiQualityTrendServiceTest` 覆盖 BLOCKED source segment。

## 3. 前端展示

- [x] 3.1 扩展 `AiQualitySourceSegment` 前端类型。
- [x] 3.2 在教师工作台来源质量列表展示 recovery chips 和阻塞原因。

## 4. 验证

- [x] 4.1 运行后端趋势相关测试。
- [x] 4.2 运行前端 typecheck。
- [x] 4.3 运行 OpenSpec strict 校验。
- [x] 4.4 运行 secret scan 和 `git diff --check`。
