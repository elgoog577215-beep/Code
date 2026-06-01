## Context

前几轮已经建立了多个学生级和质量侧 AI 信号：

- `teachingActionDecision` 能把个体学习状态转成下一步教学动作。
- `classReviewSuggestions` 能基于班级高频错因和能力弱点生成讲评卡片。
- 教师工作台能统计 AC 后迁移、复发误区、自解释、AI 依赖、成长风险和行动风险人数。
- AI 质量看板能评估各个局部闭环是否有证据和可执行动作。

缺口在班级层：系统还没有一个稳定结构回答“这次课堂整体怎么安排”。目前老师需要自己把多个 KPI、讲评建议和学生行信号拼起来，无法直接看到全班讲评、小组复盘、支架退场、退出题验证之间的优先级。

## Goals / Non-Goals

**Goals:**

- 新增结构化 `classTeachingStrategySignal`，把班级风险聚合成最高优先级课堂策略。
- 输出教师可执行动作、分组计划、退出题验证、证据引用和来源信号。
- 在教师作业概览和教师端页面展示策略摘要。
- 在 AI 质量概览新增班级教学策略闭环维度，评估是否能从班级风险生成可执行策略。
- 用后端测试覆盖主要策略选择，用前端 typecheck 覆盖 API 类型和展示。

**Non-Goals:**

- 不删除或替代学生级 `teachingActionDecision`。
- 不新增数据库迁移或外部模型调用。
- 不引入复杂个性化排课算法；第一版使用可解释规则。
- 不强制老师采纳策略；课堂反馈闭环仍复用已有讲评反馈和后续提交证据。

## Decisions

### Decision 1: 新增班级策略分析器

新增 `ClassTeachingStrategyAnalyzer`，输入教师作业概览已有中间结果：

- 学生进度摘要列表。
- 班级高频问题 `topIssues`。
- 班级能力弱点 `classAbilityWeaknesses`。
- 课堂讲评建议 `classReviewSuggestions`。

输出 `ClassTeachingStrategySignal`。这样策略生成不直接依赖数据库，便于单测和复用。

### Decision 2: 第一版只输出最高优先级策略

策略状态优先级如下：

1. `CALIBRATION_REVIEW`: 质量或诊断存在需要教师复核的校准/低置信冲突。
2. `WHOLE_CLASS_MINI_LESSON`: 同一错因或能力点影响多个学生，适合全班 8-10 分钟讲评。
3. `SMALL_GROUP_REVIEW`: 少数学生存在复发误区、成长停滞或同类高风险教学动作。
4. `DIFFERENTIATED_SUPPORT`: AI 支架依赖、自解释缺口或能力层次差异明显，需要分层任务。
5. `WATCH`: 有零散风险，但证据不足以组织课堂策略。
6. `NO_SIGNAL`: 没有学生提交或没有可用诊断。

第一版输出单个主策略，避免教师端信息过载；分组计划可承载多个学生分组。

### Decision 3: 分组计划使用轻量 DTO

`ClassTeachingStrategyGroup` 包含：

- `groupType`: `MINI_LESSON`、`SMALL_GROUP`、`INDEPENDENT_PRACTICE`、`TRANSFER_CHECK`
- `title`
- `studentProfileIds`
- `studentNames`
- `focus`
- `action`

只返回前若干代表学生，避免大班级响应过大。

### Decision 4: 质量维度检查“班级风险是否转成策略”

新增 `CLASS_TEACHING_STRATEGY_LOOP`：

- 没有提交：`WATCH`。
- 存在多个学生级高风险但策略为 `NO_SIGNAL` 或缺少证据：`ACTION_NEEDED`。
- 策略需要教师行动但缺少退出题或分组计划：`WATCH`。
- 有策略、证据、退出题和行动建议：`HEALTHY` 或 `WATCH`，视风险等级而定。

质量看板不重复生成策略细节，而是消费同一个 analyzer，保证教师页和质量页一致。

### Decision 5: 前端保持工作台密度

教师端新增一条紧凑的策略横幅，放在 KPI 和各类摘要之后、问题列表之前。它展示策略状态、聚焦点、教师动作、退出题和分组数量，不把页面做成营销式大卡片。

## Risks / Trade-offs

- [Risk] 规则策略可能过度简化真实课堂。-> Mitigation: 输出 evidenceRefs、sourceSignals 和分组，老师可基于证据调整。
- [Risk] 与 `classReviewSuggestions` 重复。-> Mitigation: 策略信号只负责“本节课优先怎么组织”，讲评建议继续负责具体题目/问题卡片。
- [Risk] 前端信息密度继续增加。-> Mitigation: 使用紧凑横幅和折叠分组摘要，只展示主策略。
- [Risk] 质量维度误判没有策略。-> Mitigation: analyzer 对无提交、证据不足和低风险分别输出不同状态。

## Migration Plan

无需数据库迁移。部署后从已有提交、诊断和学生级信号动态生成 `classTeachingStrategySignal`。旧前端可忽略新增字段；回滚时移除 analyzer 接入和前端展示，不影响已有诊断数据。
