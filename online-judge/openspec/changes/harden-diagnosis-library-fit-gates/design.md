## Context

当前默认 AI 链路已经简化为本地召回 + 单诊断 Agent。诊断 Agent 输出包含两层内容：学生可见的自然语言 `studentReport`，以及教师追踪/评测使用的 `diagnosisDecision` 和 `libraryGrowth`。

30 道长代码对照实验显示：模型经常能在学生文案里说对主因，但 `actualFineGrainedTags` 或标准库 anchor ID 为空。说明自然反馈和标准化命中之间缺少硬门禁。

## Goals / Non-Goals

**Goals:**
- 让 `HIT` 成为可校验状态：必须绑定合法标准库 ID。
- 让 `PARTIAL / MISS / OUT_OF_LIBRARY` 成为标准库成长候选的唯一来源。
- 保持学生端文案自然，不把标准库 ID 暴露给学生。

**Non-Goals:**
- 不重做标准库结构。
- 不恢复实时双 Agent 链路。
- 不实现自动模型轮换。
- 不让 AI 自动改正式标准库。

## Decisions

1. 在 `AdviceGenerationOutputValidator` 做硬门禁。

   理由：这里是所有外部模型输出进入系统前的统一边界。比在前端、评测器或成长服务里补救更小、更稳。

2. 未知标准库 ID 不再软转成 `OUT_OF_LIBRARY`。

   理由：未知 ID 可能是模型幻觉，也可能是召回缺失。静默转换会污染成长池。更好的处理是让模型重试或降级，并把原因记入 trace。

3. 成长服务只吸收 `PARTIAL / MISS / OUT_OF_LIBRARY`。

   理由：`HIT` 表示标准库已有精准覆盖，不应制造新候选。若 `HIT` 没有 ID，应在 validator 层失败，而不是进入成长池。

4. Prompt 明确双层输出职责。

   理由：学生报告应该自然，ID 绑定应该结构化。二者不能混在同一段文案里。

## Risks / Trade-offs

- 模型原本说得对但漏填 ID 时会被判失败。
  - 缓解：失败后已有结构化重试和规则降级；后续可以专门加“只补元数据”的轻量重试。
- 标准库确实缺条目时，模型不能强行 `HIT`。
  - 缓解：使用 `PARTIAL / MISS / OUT_OF_LIBRARY` 并进入成长候选池。
- 早期评测通过率可能下降。
  - 缓解：这是必要的质量暴露，避免把不可统计的自然文本当成真正命中。
