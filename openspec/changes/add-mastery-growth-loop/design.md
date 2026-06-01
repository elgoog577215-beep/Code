## Context

现有 AI 能力已经覆盖多个局部教育状态：

- 单次诊断和学习轨迹能识别本次修复、回退、反复卡住和通过后复盘。
- 能力画像能聚合近期能力点和跨题缺口，但 `trendSignal` 仍是自然语言，无法被推荐、教师端和质量指标稳定消费。
- 复发误区、自解释、AI 依赖和 AC 后迁移信号各自回答局部问题，却没有统一回答“这名学生最近是否形成长期掌握增长”。

因此，本变更将长期成长建模为结构化 `masteryGrowthSignal`，用确定性规则从既有提交和诊断证据中推导，不依赖新模型调用或新表。

## Goals / Non-Goals

**Goals:**

- 新增长期能力成长状态，区分暂无证据、正在增长、迁移已验证、停滞、回退和需要螺旋复习。
- 让学生画像、当前作业轨迹、推荐系统、教师概览和 AI 质量概览复用同一信号。
- 输出可解释证据引用，避免只给一句“需要加强”。
- 用测试覆盖状态推导、推荐消费、教师概览和质量维度。

**Non-Goals:**

- 不替代单次诊断、学习轨迹、复发误区、自解释或 AI 依赖信号。
- 不新增数据库迁移，不引入外部模型调用。
- 不试图精确预测考试成绩；第一版只做近期学习过程的可观察状态。
- 不把一次失败或一次 AC 判定为长期成长或回退。

## Decisions

### Decision 1: 新增 `MasteryGrowthAnalyzer`

分析器接收近期提交和对应诊断分析，输出 `masteryGrowthSignal`：

- `status`: `NO_SIGNAL`、`GROWING`、`TRANSFER_CONFIRMED`、`PLATEAU`、`REGRESSION`、`SPIRAL_REVIEW_NEEDED`
- `label`: 面向 UI 的短标签
- `summary`: 面向学生/教师的解释
- `growthScore`: 0 到 1，越高表示近期掌握增长越稳定
- `focusAbility`: 主能力点
- `focusTag` / `fineGrainedTag`: 主要证据标签
- `recentSubmissionCount`、`recentAcceptedCount`、`recentFailedCount`
- `crossProblemEvidenceCount`: 同一能力点或通过证据覆盖的题目数
- `regressionCount`: 已通过后又回退到失败的次数
- `plateauCount`: 近期连续失败或同类问题停滞次数
- `evidenceRefs`: `submission:<id>`、`analysis:<id>`、`ability:<ability>` 等引用
- `recommendedAction`
- `needsTeacherAttention`

### Decision 2: 成长判断以“序列变化 + 跨题证据”组合，而不是只看通过率

第一版规则强调教育解释性：

- 最近能从失败推进到 AC，或近期通过率明显提升，判为 `GROWING`。
- 同一能力点在多题出现通过或复盘/迁移证据，判为 `TRANSFER_CONFIRMED`。
- 最近多次失败且无 AC，或同一能力点反复失败，判为 `PLATEAU`。
- 曾经在某题或近期窗口中通过后又连续失败，判为 `REGRESSION`。
- 同一细分错因或能力点跨多题重复失败，且停滞次数达到阈值，判为 `SPIRAL_REVIEW_NEEDED`。
- 证据不足时输出 `NO_SIGNAL`，不强行判断。

这避免把“刷过一道题”误判为掌握，也避免把正常探索过程误判为退步。

### Decision 3: 推荐系统把成长风险转成下一步教学动作

推荐服务消费 `masteryGrowthSignal`：

- `PLATEAU`: 生成 `MASTERY_PLATEAU_REPAIR`，要求先复盘最小失败样例和共同能力点。
- `REGRESSION`: 生成 `MASTERY_REGRESSION_REPAIR`，要求对比上次 AC 与当前失败差异。
- `SPIRAL_REVIEW_NEEDED`: 生成 `MASTERY_SPIRAL_REVIEW`，建议教师或学生做跨题螺旋复习。
- `GROWING` / `TRANSFER_CONFIRMED` 不挤占风险推荐，只作为质量和画像正向证据。

### Decision 4: 教师端突出风险，学生端突出下一步

教师作业概览新增成长风险学生数、班级摘要和学生行信号，只突出 `PLATEAU`、`REGRESSION` 和 `SPIRAL_REVIEW_NEEDED`。学生端在学习抽屉中展示一条轻量成长提醒，用于解释为什么下一步是复盘、对比或迁移验证。

### Decision 5: AI 质量概览新增长期成长维度

新增 `MASTERY_GROWTH_LOOP` 维度，用于评估 AI 系统是否能把局部诊断沉淀为长期成长判断。该维度输出状态、分数、摘要、证据引用和推荐改进动作。

## Risks / Trade-offs

- [Risk] 只用近期提交可能忽略更长周期趋势。-> Mitigation: 第一版限制近期窗口，输出“近期成长”而不是永久能力判断。
- [Risk] 同一学生跨 profile 合并不完整会影响长期判断。-> Mitigation: 学生能力画像继续沿用已有合并逻辑；当前作业轨迹只判断当前作业范围。
- [Risk] 过多教师提醒造成噪音。-> Mitigation: 教师端只突出风险状态，正向状态留在画像和质量详情。
- [Risk] 规则阈值可能需要校准。-> Mitigation: 阈值集中在分析器并用 fixtures/单测覆盖，后续可迭代。

## Migration Plan

无需数据库迁移。部署后系统从现有提交和诊断分析动态生成 `masteryGrowthSignal`。旧前端忽略新增字段；回滚时删除分析器消费和展示即可，不影响已有 AI 闭环数据。
