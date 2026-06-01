## Context

当前 AI 能力已经形成较完整的“失败提交 -> 错因诊断 -> Coach 追问 -> 推荐/教师介入 -> 质量评测”链路。缺口在通过之后：学生拿到 AC 后，系统只统计完成数和通过率，却没有结构化表达学生是否能复盘关键错误、提炼迁移规则，或进入下一道同能力题验证。

这会让教育 agent 产生两个盲点：

- 学生刚 AC 就被视为完成，错过最适合巩固的复盘窗口。
- 教师只能看到通过率，看不到“通过但未沉淀”的学生或能力点。

## Goals / Non-Goals

**Goals:**

- 新增可复用的 AC 后迁移状态变量，覆盖学生、任务、教师和 AI 质量维度。
- 让推荐系统能基于“刚 AC 但缺迁移证据”生成复盘或迁移练习。
- 让教师端看到哪些学生/题目已经通过但仍缺复盘证据。
- 保持旧接口兼容，只新增字段。

**Non-Goals:**

- 不引入外部模型调用判断复盘质量。
- 不新增数据库迁移或新的持久化表。
- 不把 AC 后复盘强制变成提交判题的一部分。
- 不重写现有 Coach 或推荐算法，只增加可观察信号和确定性规则。

## Decisions

### Decision 1: 使用动态分析器推导 `postAcTransferSignal`

新增一个确定性分析器，读取同一学生在作业内的提交、诊断分析、Coach 回答质量和推荐事件，输出：

- `phase`: `NOT_ACCEPTED`、`JUST_ACCEPTED`、`REFLECTION_NEEDED`、`REFLECTION_EVIDENCED`、`TRANSFER_READY`、`TRANSFER_VERIFIED`
- `label`: 面向学生/教师的短标签
- `summary`: 证据解释
- `evidenceRefs`: 相关提交、Coach 或推荐事件证据
- `recommendedAction`: 下一步复盘或迁移动作
- `targetAbility` / `targetTags`: 迁移所围绕的能力点与错因
- `needsTeacherAttention`: 是否需要教师提醒复盘

选择动态分析而不是新表，是因为第一版所需证据已经存在，动态推导更容易回滚，也能直接覆盖历史数据。

### Decision 2: 把 AC 后迁移纳入学生轨迹和教师概览

学生轨迹新增全局 `postAcTransferSignal`，每个任务轨迹也新增同名字段。教师学生行新增最新迁移信号，课堂概览新增待迁移学生数和班级迁移摘要。这样前端不必解析自然语言或重新推断。

### Decision 3: 推荐系统消费迁移信号

当学生存在 AC 后但缺复盘证据的任务时，推荐系统生成 `POST_AC_REFLECTION` 或 `TRANSFER_TO_NEW_PROBLEM` 策略。推荐项会复用已有学习假设字段，说明它要验证的迁移能力和完成信号。

### Decision 4: AI 质量维度关注“通过后是否沉淀”

AI 质量概览新增 `POST_AC_TRANSFER_LOOP` 维度，统计 AC 后缺复盘证据、具备迁移证据和完成迁移验证的样本。这个维度不评价判题正确性，而评价教育闭环是否把 AC 转成可迁移学习。

## Risks / Trade-offs

- [Risk] 规则可能误判学生已经理解但未留下证据。-> Mitigation: 使用“证据不足”而非“未理解”措辞，只作为复盘提醒。
- [Risk] 推荐列表变得拥挤。-> Mitigation: 仅在刚 AC 且缺迁移证据时生成，最多占用一个推荐槽位。
- [Risk] 动态推导跨服务依赖增加。-> Mitigation: 分析器独立封装，服务只消费结果 DTO。
- [Risk] 前端展示增加状态密度。-> Mitigation: 学生端只显示一个复盘动作，教师端摘要展示，细节保持折叠。

## Migration Plan

无需数据库迁移。部署后旧提交可动态生成迁移信号；旧前端忽略新增字段，新前端逐步展示。若需要回滚，删除新增展示与分析器消费即可，旧诊断/轨迹/推荐字段不受影响。
