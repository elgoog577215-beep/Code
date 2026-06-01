## Context

当前推荐系统已经具备三个层次：

- 生成层：`StudentRecommendationService` 根据学生画像、错因、Coach、成长和教学动作信号生成推荐，并写入 learning hypothesis、expected completion signal、strategy、risk、fallback action。
- 事件层：`StudentRecommendationEventService` 记录曝光、点击、进入题目和后续提交，并在诊断生成后回填后续错因标签。
- 评估层：`RecommendationEffectivenessService` 统计点击率、后续提交率、AC 率、同类错因复发和 feedback signals。

缺口在行动契约层：推荐里已经写明“为什么推荐”和“完成后应该出现什么信号”，但评估层没有显式判断这个完成信号是否兑现。于是下一轮推荐只能粗略知道“有点击/有提交/仍同类错因”，却不能稳定输出“这个 token 的行动结果是完成、未执行、失败、需要改策略还是需要教师介入”。

## Goals / Non-Goals

**Goals:**

- 按 recommendation token 输出结构化 action evidence。
- 基于曝光、点击、进入、提交、后续 verdict、后续 issue/fine tag、risk 和 strategy 判断 outcome。
- 将 outcome 映射为 summary、recommendedAdjustment、needsTeacherAttention 和 evidenceRefs。
- 让推荐效果概览和 AI 质量维度消费 action evidence，而不是重复散落判断。
- 让下一轮 `StudentRecommendationService` 可以用 action evidence 判断是否降级复盘或教师介入。

**Non-Goals:**

- 不新增数据库表或迁移。
- 不把推荐 outcome 解释成严格因果证明，只作为观察性行动证据。
- 不重写推荐排序和推荐策略全集。
- 不新增前端展示，只补 API 类型。

## Decisions

### Decision 1: 新建独立 analyzer，避免在服务里继续堆 if/else

推荐行动证据涉及 token 分组、事件顺序、策略、风险、后续提交和错因标签。新建 `RecommendationActionEvidenceAnalyzer`，让 `RecommendationEffectivenessService` 和 `StudentRecommendationService` 共享同一套 outcome 判断，避免质量概览、推荐服务和效果服务各自实现一份。

### Decision 2: outcome 采用教学动作状态，而不是纯产品漏斗状态

第一版 outcome：

- `CONTRACT_FULFILLED`: 推荐后的提交通过，或没有继续命中关注错因。
- `UNRESOLVED_SAME_FOCUS`: 后续提交仍命中推荐关注错因。
- `NO_FOLLOWUP_SUBMISSION`: 点击或进入题目后没有后续提交。
- `EXPOSED_ONLY`: 仅曝光，还没有行动证据。
- `TEACHER_INTERVENTION_NEEDED`: 高风险推荐后仍提交失败或命中同类错因。
- `WAITING_DIAGNOSIS`: 后续提交存在但诊断标签尚未回填。

这些状态比“点击/提交/AC”更贴近教育 agent 的下一步决策。

### Decision 3: evidence refs 复用 token 和 follow-up submission

每个 signal 暴露 `recommendation:<token>`、`recommendation-strategy:<strategy>`、`submission:<id>`、`recommendation-outcome:<outcome>` 等 refs。这样 AI 质量概览和后续评测可以追溯到具体推荐，不依赖自然语言。

### Decision 4: 保持旧指标兼容，新增字段做结构升级

现有 unique/click/submission/accepted/sameFocus/unresolved/teacherIntervention 指标继续保留。新增 `actionEvidenceSignals` 作为更细颗粒度证据；旧 UI 和测试不会被破坏，后续教师端再渐进展示。

## Risks / Trade-offs

- [Risk] 只看推荐 token 后续提交仍是观察性信号。-> Mitigation: 文案使用“行动证据/观察性结果”，不声明因果。
- [Risk] 没有后续诊断时可能误判。-> Mitigation: 后续提交存在但标签缺失时输出 `WAITING_DIAGNOSIS`，不直接判定成功或失败。
- [Risk] 高风险推荐可能被过早升级教师介入。-> Mitigation: 仅在高风险且有失败提交或同类错因未解决时输出 teacher attention。
- [Risk] 过多 token signal 影响响应体大小。-> Mitigation: 默认按风险优先并限制输出数量。
