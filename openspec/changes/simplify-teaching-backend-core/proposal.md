## Why

当前后端已经具备判题、学生 AI 反馈、学习轨迹、教师洞察和大量 AI 质量/评测能力，但主链路被历史迭代出的 analyzer、runtime、eval、loop、fallback 状态挤得过重。学校真实使用需要的是一个简单、高效、有用的教学支撑层，而不是一个把工程细节暴露给学生和老师的复杂后盾。

本变更的目标是收敛后端结构：明确“主链路只服务教学动作，复杂能力只做内部支撑”。第一轮不大规模删除旧代码，而是先建立后端主链路边界、审计清单和轻量的接口分层规则，避免后续继续修修补补。

## What Changes

- 建立后端主链路收敛原则：
  - 学生主链路只围绕作业、题目、提交、判题事实、学生 AI 反馈。
  - 教师主链路只围绕作业、班级概况、需关注学生、共性问题、课堂洞察。
  - AI runtime、fallback、smoke、profile、eval、quality dimension、loop analyzer 等只能作为内部支撑或系统详情，不作为主接口和主 UI 的组织中心。
- 产出后端审计清单，按 `keep-main`、`simplify`、`internal-only`、`remove-later` 标记当前 submission/classroom 相关服务、DTO 和接口。
- 新增或收敛轻量边界文档/代码常量，让后续新增接口时能判断：
  - 是否服务学生下一步动作。
  - 是否服务教师课堂判断。
  - 是否只是研发/评测/排障信息。
- 第一批实现只做低风险收敛：
  - 新增审计文档。
  - 新增主链路分类说明。
  - 为明显内部用途的教师 API/前端字段增加边界命名或注释。
  - 保持现有判题、AI 反馈、教师端接口兼容。

## Capabilities

### New Capabilities

- `teaching-backend-core-simplification`: 系统 SHALL 将后端能力分为教学主链路和内部支撑链路，并提供可审计、可验证的收敛边界。

### Modified Capabilities

无。本变更第一轮不修改已归档正式 spec；未归档的近期 OpenSpec 变更只作为上下文参考。

## Impact

- OpenSpec：新增 `simplify-teaching-backend-core` proposal、design、spec、tasks。
- 文档：新增后端主链路审计文档，列出 keep-main/simplify/internal-only/remove-later。
- 后端：第一轮只做低风险边界辅助，不删除接口、不改数据库、不改变模型调用语义。
- 前端：本轮不重构页面，只确保后续不把内部状态重新推回主 UI。
- 测试：运行 OpenSpec strict validation、相关后端测试；如触及前端类型则运行 frontend typecheck。
