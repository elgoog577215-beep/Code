## Why

上一轮已经把 ModelScope 外部模型的 `runtimeMode`、`failureStage` 和 `failureReason` 写入 `aiInvocation`，live eval 也能看到真实调用中的成功、额度不足和运行回退。但跨作业 AI 质量趋势和教师端来源质量片段仍只展示 provider/model/prompt/status/fallback，老师无法直接判断真实外部模型在课堂样本中的完成率、部分完成率和失败原因。

本变更把外部模型运行归因推进到趋势层和教师端，让“模型成功、部分完成、规则兜底、额度不足、安全回退”成为可观察、可比较、可复盘的质量信号。

## What Changes

- AI 质量趋势响应新增外部模型完成、部分完成、运行失败和运行失败率字段。
- 作业趋势点新增模型完成、部分完成、运行失败和运行失败率字段，帮助老师定位问题作业。
- source segment 新增 `runtimeMode`、`failureStage`、`failureReason`、模型完成数、部分完成数、运行失败数和运行失败率。
- source segment 分组键纳入运行模式和失败归因，让同一模型的成功样本、部分完成样本和额度/安全/校验失败样本可分开观察。
- 教师工作台 AI 质量趋势区展示模型运行失败、部分完成、作业级失败 badge 和来源级失败原因。
- 更新趋势服务测试与前端类型检查，验证字段统计和 UI 类型兼容。

## Capabilities

### New Capabilities

- `external-runtime-attribution-trend`: 约束 AI 质量趋势和教师端展示必须消费外部模型运行归因字段。

### Modified Capabilities

无。

## Impact

- 后端 DTO：`AiQualityTrendResponse`
- 后端趋势聚合：`AiQualityTrendService`
- 后端测试：`AiQualityTrendServiceTest`
- 前端类型：`frontend/src/shared/api/types.ts`
- 教师工作台：`frontend/src/features/teacher/TeacherPage.tsx`
- 验证：OpenSpec strict validate、相关后端测试、前端 typecheck、`git diff --check`
