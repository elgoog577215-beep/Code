## Why

上一轮已经把外部模型 transport telemetry 接入后端 AI 质量概览和趋势 DTO，但教师工作台仍只展示“模型归因”的自然语言摘要与来源片段的失败原因。教师无法快速看出当前外部模型问题到底是：

- ModelScope stream 请求已发出但没有返回 content chunk；
- 本地 budget guard 直接短路；
- stream chunk 解析异常；
- non-stream 到 stream 的 fallback retry。

这会让“外部模型能力问题”和“工程接入/额度/传输问题”在界面上继续混在一起。

## What Changes

- 在教师工作台 AI 质量卡片的“模型归因”区域展示 transport mode 与 stream 计数摘要。
- 在跨作业趋势的“来源质量”片段中展示 transport mode、stream no-content、invalid chunk、fallback retry。
- 增加轻量前端格式化函数，把 transport telemetry 转成教师可读短语。
- 更新样式，保持当前工作台的紧凑扫描布局，不新增页面。

## Capabilities

### New Capabilities

- `teacher-transport-attribution-visibility`: 教师工作台展示外部模型传输归因信号。

### Modified Capabilities

- `transport-telemetry-attribution`: 后端输出的 transport telemetry 被教师端消费。
- `surface-ai-quality-trend-in-teacher-workbench`: 来源质量片段增加传输层指标。

## Impact

- 前端：`TeacherPage.tsx`、`styles.css`
- API 契约：复用已有 `RuntimeAttributionSignal` 与 `AiQualitySourceSegment` 字段，不改后端 DTO。
- 测试：运行前端 typecheck；如本地服务可用，进行页面 smoke 验证。
- 非目标：不改变外部模型调用、prompt、fallback、预算保护或后端统计逻辑。
