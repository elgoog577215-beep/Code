## Context

当前 `CoachAnswerQualityAnalyzer` 已经能粗分 `NO_ANSWER`、`VAGUE_ACK`、`DIRECTION_ONLY`、`EVIDENCE_GROUNDED`、`TRANSFER_READY`，并返回证据类型、缺失证据和下一步追问动作。`CoachInteractionAnalyzer` 会把最新回答质量放入学生轨迹和教师视图。

缺口在于：质量信号还不够结构化，不能明确表达理解层级、证据完整度、学生行动是否可观察，也没有被上一轮新增的 AI 质量概览维度消费。

## Goals / Non-Goals

**Goals:**

- 把 Coach 回答质量扩展为可统计、可排序、可解释的结构化信号。
- 让回答质量影响 AI 质量概览中的 Coach 理解维度。
- 让空泛回答、疑似答案泄露、缺证据回答能触发明确改进优先级。
- 不破坏已有 Coach 响应结构，只新增字段。

**Non-Goals:**

- 不调用外部模型来判学生回答质量。
- 不新增数据库字段或迁移。
- 不重写 Coach 对话生成策略。
- 不改变现有提示安全策略的主判定来源。

## Decisions

### Decision 1: 继续使用确定性规则分析学生回答

学生回答质量是质量闭环的基础信号，必须稳定、低成本、可测试。第一版使用关键词、证据类型和回答形态规则推导，不引入新的模型调用。

### Decision 2: 新增字段表达“教学可用性”

在已有 `qualityLevel` 之外新增：

- `understandingLevel`: `NONE`、`DIRECTION`、`EVIDENCE`、`VERIFICATION`、`TRANSFER`、`RISKY`
- `evidenceCompleteness`: 0 到 1 的证据完整度分数
- `verifiable`: 是否已经能作为下一次提交/复盘的可验证依据
- `actionStatus`: `NOT_ANSWERED`、`NEEDS_EVIDENCE`、`READY_TO_VERIFY`、`READY_TO_TRANSFER`、`SAFETY_RISK`
- `recommendedTeachingAction`: 下一步教学动作

这些字段让教师和质量概览无需解析自然语言。

### Decision 3: AI 质量概览增加 Coach 理解维度

Coach 回答质量不应替代诊断准确性，但它能回答“学生是否理解了 AI 提示”。因此新增独立维度 `COACH_UNDERSTANDING`，并把低质量/风险回答作为改进优先级候选。

## Risks / Trade-offs

- [Risk] 规则可能误判简短但有效的学生回答。→ Mitigation: 分数和状态作为辅助信号，不覆盖提交诊断；测试覆盖典型场景。
- [Risk] 字段变多增加前端理解成本。→ Mitigation: 保留旧字段，新增字段命名直接对应教学含义。
- [Risk] 无数据库字段导致历史分析需动态计算。→ Mitigation: 现有 CoachPrompt 已保存学生回答，动态计算足够支撑第一版。

## Migration Plan

无需迁移。部署后旧数据可通过已有学生回答即时生成新信号；旧前端仍可读取原字段，新前端可逐步展示新增字段。
