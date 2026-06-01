## Context

`CoachAnswerQualityAnalyzer` 已经把学生回答分为 `NO_ANSWER`、`VAGUE_ACK`、`DIRECTION_ONLY`、`EVIDENCE_GROUNDED`、`VERIFICATION_READY`、`TRANSFER_READY`、`SAFETY_RISK` 等质量等级。`ClassroomService` 在学生过程摘要中已经带出 `latestCoachInteraction.answerQualitySignal`，教师端学生行也能看到一些 Coach 状态。

缺口是班级级视角：老师需要知道这一轮 Coach 追问整体有没有让学生形成证据，而不是逐个学生翻状态。

## Goals / Non-Goals

**Goals:**

- 新增班级级 `coachAnswerQualitySummary`，汇总 Coach 追问和回答质量。
- 输出可验证、可迁移、证据不足、疑似越界、需教师关注等计数。
- 输出主导缺口、summary、recommendedAction 和 evidenceRefs。
- 教师端展示摘要卡片，帮助老师决定继续追问、示范证据表达或安排复盘。
- 测试覆盖后端汇总和前端类型/视觉。

**Non-Goals:**

- 不重新设计 Coach 问题生成逻辑。
- 不新增数据库表。
- 不新增独立 Coach 管理页。
- 不改变学生端 Coach 交互。

## Decisions

### Decision 1: 聚合放在 AssignmentOverviewResponse

Coach 回答质量是当前作业的课堂过程信号，与高频问题、学生过程、自解释总结同属 assignment overview。放在 overview 响应中可以复用已有页面加载流程，避免新增接口。

### Decision 2: 汇总基于 latestCoachInteraction

第一版使用每个学生最新的 Coach interaction，避免重复计算所有历史 turn 造成噪音。历史趋势可在后续扩展为跨作业 Coach 质量趋势。

### Decision 3: 主导缺口用优先级规则

summary 优先级：

1. `SAFETY_RISK` 或 needsTeacherAttention。
2. 证据不足：`NO_ANSWER`、`VAGUE_ACK`、`DIRECTION_ONLY`。
3. 可验证但未迁移：`EVIDENCE_GROUNDED`、`VERIFICATION_READY`。
4. 可迁移：`TRANSFER_READY`。

这让老师优先处理风险和证据缺口，再沉淀优质样本。

## Risks / Trade-offs

- [Risk] latest interaction 可能遗漏早期高风险回答。-> Mitigation: 第一版用于课堂即时判断，历史趋势后续单独做。
- [Risk] 学生未回答时计数可能让课堂看起来偏弱。-> Mitigation: 单独区分 prompted、answered 和 noAnswer，不把未回答误判为回答质量差。
- [Risk] 前端信息过密。-> Mitigation: 卡片只展示汇总、关键计数和一条主建议，细节仍在学生行。
