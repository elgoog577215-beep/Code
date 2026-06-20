## Context

当前外部模型链路已经具备 `ModelDiagnosisBrief -> 搜索定位 -> 精选 StandardLibraryPack -> 完整诊断` 的基础。搜索定位只负责缩小标准库上下文，不直接给学生展示。下一步需要提升第二次完整诊断的输出质量，让学生看到结构清晰的基础层和提高层建议。

现有 `CombinedOutput` 已包含 `diagnosisDecision`、`teachingHint` 和 `studentFeedback`，但它没有显式表达“题目理解/代码理解/行为差距”，也不能把建议稳定绑定到精选标准库里的能力点、易错点和提高点。本设计保持现有学生反馈兼容，同时新增更教育化、可校验的 advice 输出结构。

## Goals / Non-Goals

**Goals:**

- 让第二次模型调用输出完整结构化建议：题目目标、代码意图、行为差距、基础层建议、提高层建议和下一步行动。
- 建议必须基于真实证据引用，并尽量绑定精选标准库条目 ID。
- 后端校验 advice 输出，防止无证据建议、非法 ID、空建议、置信度越界和答案泄露。
- 将 advice 输出映射到现有 `StudentFeedback`，保持前端兼容。
- 在 `AiInvocation` 中记录 advice 阶段状态、失败原因、建议数量和 prompt 版本。

**Non-Goals:**

- 不实现分层提示审批、完整教程申请或教师审批流。
- 不重做学生端 UI；本阶段只新增兼容字段并继续填充现有反馈结构。
- 不把标准库变成本地规则诊断引擎；最终分析仍由外部 LLM 完成。
- 不改变搜索定位阶段的召回与精选策略。

## Decisions

1. **新增 advice 输出，而不是直接替换旧 `StudentFeedback`。**
   - 原因：`StudentFeedback` 适合前端展示，但不够表达模型的完整推理结构。
   - 方案：新增 `AdviceGenerationOutput`，然后映射到现有 `StudentFeedback`。

2. **默认仍保持两次模型调用。**
   - 第一次：搜索定位。
   - 第二次：完整诊断与建议生成。
   - 原因：两阶段可以让第二次上下文更短、更准，且不增加第三次模型调用成本。

3. **使用新的 prompt 版本 `diagnosis-and-advice-v1`。**
   - 原因：旧 `diagnosis-and-teaching-v3` 仍服务回归；新 prompt 可以更明确地要求基础层/提高层结构。
   - 兼容：配置未指定时可以继续默认旧 prompt；本变更新增配置默认切到 advice prompt。

4. **后端强校验证据、安全和标准 ID。**
   - 原因：学生可见建议必须可解释、可追溯，不能凭空生成。
   - 校验失败后回退旧诊断反馈，并记录 trace。

5. **提高层建议允许为空。**
   - 原因：CE/RE 或基础错误严重时，提高层可能不合适。
   - 约束：未 AC 且基础层为空必须失败；基础层少且 evidence 明确时可以给提高层，但不能抢主次。

## Risks / Trade-offs

- [Risk] 新结构增加模型 JSON 复杂度 -> 保留旧输出回退，并新增结构化 retry 复用现有机制。
- [Risk] 模型把完整答案写进建议 -> 校验器复用安全策略并检查完整代码/替换代码倾向。
- [Risk] 标准库 ID 绑定失败导致好建议被丢弃 -> ID 绑定尽量要求但不让所有 advice 必须有全部 ID；证据引用仍强制。
- [Risk] 前端暂时看不到所有新字段 -> 先映射到 `StudentFeedback`，后续再做基础层/提高层 UI。

## Migration Plan

- 新增字段保持向后兼容，旧客户端可忽略。
- advice 失败时回退现有 `StudentFeedback` 或规则诊断。
- 可通过 prompt 版本配置回退旧 `diagnosis-and-teaching-v3`。
- 本阶段不做数据库迁移。
