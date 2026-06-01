## Context

前几轮已经把 AI 能力从单次诊断扩展到多个教育状态：

- `learningTrajectorySignal` 解释单次提交链的修复、回退、反复卡住。
- `postAcTransferSignal` 判断 AC 后是否需要复盘迁移。
- `recurringMisconceptionSignal` 判断跨题/跨作业复发误区。
- `selfExplanationMasterySignal` 判断学生是否把提示转成可验证解释。
- `aiDependencySignal` 判断 AI 支架是否过密或是否能退场。
- `masteryGrowthSignal` 判断长期能力是否增长、停滞、回退或需要螺旋复习。

缺口在于这些信号的消费方式仍分散：学习轨迹服务用多个 `if` 覆盖 `nextStep`，推荐服务也按固定顺序插入推荐。随着信号增加，系统需要一个统一的教学动作编排层，稳定回答“当前最该做什么、为什么、由谁做、如果失败怎么办”。

## Goals / Non-Goals

**Goals:**

- 新增结构化 `teachingActionDecision`，将多个局部信号编排成一个最高优先级教学动作。
- 输出候选动作排序、决策原因、证据引用、执行者和 fallback 动作。
- 在学生轨迹、学生画像、教师概览、推荐系统和 AI 质量概览复用同一决策。
- 用测试覆盖主要优先级冲突：教师介入、成长回退、AI 依赖、自解释缺口、AC 后迁移。

**Non-Goals:**

- 不删除现有信号，也不替代单次诊断。
- 不新增数据库迁移或外部模型调用。
- 不把所有动作都塞进推荐列表；第一版只输出最高优先级决策和少量候选摘要。
- 不追求复杂策略学习，先用可解释规则建立可评测闭环。

## Decisions

### Decision 1: 新增 `TeachingActionOrchestrator`

编排器接收可选信号并输出 `TeachingActionDecision`：

- `actionType`: `TEACHER_REVIEW`、`SPIRAL_REVIEW`、`REGRESSION_REPAIR`、`INDEPENDENT_ATTEMPT`、`SELF_EXPLANATION_PRACTICE`、`POST_AC_REFLECTION`、`TRANSFER_PRACTICE`、`CONTINUE_DIAGNOSIS`
- `actor`: `TEACHER`、`STUDENT`、`AI_COACH`
- `priority`: 数值越小越优先
- `riskLevel`: `LOW`、`MEDIUM`、`HIGH`
- `title`
- `summary`
- `primaryReason`
- `recommendedAction`
- `fallbackAction`
- `evidenceRefs`
- `sourceSignals`
- `candidateCount`
- `needsTeacherAttention`

### Decision 2: 先规则编排，后续再学习权重

第一版按教育风险排序：

1. 明确教师介入：复发误区升级、自解释安全风险、AI 长期依赖、成长螺旋复习。
2. 成长回退和停滞：优先修复长期学习状态，再继续加新题。
3. AI 支架过密：先做独立尝试，避免继续加提示。
4. 自解释证据不足：要求学生补最小样例或变量轨迹。
5. AC 后复盘迁移：通过后补复盘或迁移验证。
6. 无明显风险：继续按单次诊断或下一题推进。

这样能保持教师可解释性，也能让后续评测直接断言优先级。

### Decision 3: 学习轨迹由编排决策统一覆盖下一步

`StudentTrajectoryService` 仍保留原始 `nextStep` 生成逻辑，但最终由 `teachingActionDecision.recommendedAction` 统一覆盖高优先级风险动作；`attentionReason` 使用 `primaryReason` 或 `summary`。这样保留兼容字段，同时把决策依据结构化输出。

### Decision 4: 推荐服务消费编排结果

推荐服务优先消费 `teachingActionDecision`，生成与编排动作一致的推荐策略，例如：

- `TEACHING_ACTION_TEACHER_REVIEW`
- `TEACHING_ACTION_SPIRAL_REVIEW`
- `TEACHING_ACTION_REGRESSION_REPAIR`
- `TEACHING_ACTION_INDEPENDENT_ATTEMPT`

已有局部推荐继续存在，但当编排决策已经覆盖同一风险时避免重复插入同类高优先级推荐。

### Decision 5: 质量维度检查“信号是否能转成动作”

新增 `TEACHING_ACTION_ORCHESTRATION_LOOP`，衡量系统是否产生了结构化教学动作、是否有足够证据、是否存在多个风险信号但没有明确动作。质量维度输出状态、分数、摘要、证据引用和推荐改进动作。

## Risks / Trade-offs

- [Risk] 编排规则可能压过局部信号的细节。-> Mitigation: 保留 `sourceSignals` 和候选数，前端显示摘要而非隐藏原信号。
- [Risk] 固定优先级可能不适合所有班级。-> Mitigation: 第一版只处理高风险冲突，后续可根据教师反馈调权重。
- [Risk] 推荐重复。-> Mitigation: 推荐服务在编排动作命中时避免插入同类局部推荐。
- [Risk] 页面信息过多。-> Mitigation: 学生端只显示最高优先级动作，教师端只在风险动作时展示。

## Migration Plan

无需数据库迁移。部署后系统从已有信号动态生成 `teachingActionDecision`。旧前端忽略新增字段；回滚时删除编排器消费和展示即可，不影响已有 AI 信号数据。
