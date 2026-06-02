## Why

教师工作台已经能在当前作业“模型归因”中展示 recovery 状态，但跨作业 AI 质量趋势的来源片段仍只展示失败数、部分完成和 transport telemetry。维护者无法判断某个 provider/model/prompt/runtime 组合是否跨作业持续恢复，还是持续 fallback。

本变更把 recovery 状态推进到跨作业 source segment，形成从单作业到趋势的外部模型恢复观察链路。

## What Changes

- `AiQualityTrendResponse.SourceQualitySegment` 新增 recovery status、check count、blocked reason count、blocked reasons 和 required checks。
- `AiQualityTrendService` 在 source segment 聚合时根据该片段内样本推导 `RECOVERED`、`BLOCKED`、`NOT_APPLICABLE`。
- 教师工作台来源质量列表展示 recovery chip 和前两条阻塞原因。
- 覆盖后端趋势测试、前端 typecheck、OpenSpec strict 校验、secret scan 和 diff check。

## Capabilities

### New Capabilities

- `runtime-recovery-trend-segments`: 约束跨作业 AI 质量趋势的 source segment 必须暴露外部模型恢复状态。

### Modified Capabilities

无。

## Impact

- 后端 DTO：`AiQualityTrendResponse.SourceQualitySegment`
- 后端服务：`AiQualityTrendService`
- 后端测试：`AiQualityTrendServiceTest`
- 前端类型：`frontend/src/shared/api/types.ts`
- 教师工作台：`frontend/src/features/teacher/TeacherPage.tsx`
- 验证：后端趋势测试、前端 typecheck、OpenSpec strict validate、secret scan、`git diff --check`
