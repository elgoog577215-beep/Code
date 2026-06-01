## Context

当前学生推荐链路已经形成基础闭环：`StudentRecommendationService` 基于学生能力画像生成 `REDO`、`NEXT_PROBLEM`、`REVIEW` 三类推荐；`StudentRecommendationEventService` 记录曝光、点击、进入题目和后续提交；`RecommendationEffectivenessService` 汇总点击率、提交率、通过率和同类错因率。

缺口在于推荐本身缺少结构化“教学意图”。系统能知道学生点击了某条推荐，也能知道后续提交是否通过，但不知道这条推荐原本要验证哪种学习假设、预期学生完成什么证据、失败后应该降级为复盘还是升级到教师介入。因此推荐效果还没有真正反哺下一轮 AI 决策。

## Goals / Non-Goals

**Goals:**

- 让每条推荐携带结构化学习假设和可验证完成信号。
- 让推荐事件保留策略、假设和完成信号，使效果分析可以跨事件聚合。
- 让历史推荐失败信号影响下一次推荐策略，避免对同类错因反复推同样动作。
- 让教师端推荐效果概览输出策略级效果、未完成学习信号和教师介入建议。
- 保持 API 和数据库兼容，只新增字段。

**Non-Goals:**

- 不引入新的外部模型调用来生成推荐。
- 不新增数据库迁移；新增 JPA 字段由当前开发模式保持兼容。
- 不重写题目推荐算法或全局排序体系。
- 不把推荐效果直接等同于诊断准确率；它是学习闭环信号的一部分。

## Decisions

### Decision 1: 推荐项显式携带学习假设

每条 `RecommendationItem` 新增：

- `learningHypothesis`: 这条推荐要验证的学生学习状态假设。
- `expectedCompletionSignal`: 学生完成后应出现的可观察信号。
- `strategy`: 推荐策略，如 `REPAIR_SAME_PROBLEM`、`TRANSFER_TO_NEW_PROBLEM`、`REFLECTION_EVIDENCE`、`STEP_DOWN_REVIEW`。
- `riskLevel`: `LOW`、`MEDIUM`、`HIGH`。
- `fallbackAction`: 推荐失败或证据不足时的下一步动作。

这些字段让前端、事件记录和效果分析无需解析标题或原因。

### Decision 2: 事件记录冗余保存推荐策略字段

推荐效果分析读取的是事件表，而不是即时重新生成推荐。为了让历史效果可追溯，曝光事件会保存推荐策略、学习假设、预期完成信号和 fallback 动作；点击、进入题目、提交事件从曝光事件复制这些字段。

旧事件没有新增字段时，效果服务把策略归为 `UNKNOWN`，不影响旧数据读取。

### Decision 3: 推荐失败后优先降级学习台阶

当同一学生近期存在“推荐后提交但仍命中同类错因”的事件时，下一轮推荐应把复盘或更小验证动作提前。第一版不做复杂强化学习，只用确定性规则：

- 同类错因推荐失败存在时，新增或提前 `STEP_DOWN_REVIEW` 策略。
- reason 和 fallbackAction 明确说明先补最小样例、证据解释或教师介入。
- 仍保留最多 3 条推荐，避免界面负担增加。

### Decision 4: 效果概览输出行动型反馈

推荐效果概览新增：

- `unresolvedLearningSignalCount`: 推荐后仍卡同类错因的提交数量。
- `teacherInterventionRecommendedCount`: 需要教师介入的推荐组数量。
- `feedbackSignals`: 若干条行动型反馈，包含策略、严重度、证据数量和建议动作。
- 策略分段统计 `byStrategy`，帮助判断哪类推荐有效或需要降级。

## Risks / Trade-offs

- [Risk] 规则化学习假设可能过于粗粒度。→ Mitigation: 先保证字段稳定可测，后续可由更多学习状态变量细化。
- [Risk] 新增事件字段需要数据库结构同步。→ Mitigation: 当前项目使用新增兼容字段；不删除旧字段，旧数据字段为空时走 `UNKNOWN`。
- [Risk] 历史推荐失败可能被误判为推荐无效。→ Mitigation: 只作为降级信号，不禁止原推荐；输出中标注为观察性证据。

## Migration Plan

无需数据迁移脚本。部署后新曝光事件开始写入策略字段；旧事件继续参与基础漏斗统计，策略统计归入 `UNKNOWN`。
