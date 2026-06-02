## Context

`evalReadiness` 已经汇总：

- 教师纠错 eval candidate；
- 课堂介入和班级策略成效候选；
- 等待后续证据的 partial 状态；
- 推荐沉淀动作。

与此同时，当前作业 `runtimeAttributionSignal` 已经输出 `qualityComparabilityStatus`，能够判断当前样本是否代表真实外部模型质量。缺口是两者没有连接：教师看到“评测沉淀 READY”时，不知道 READY 指的是诊断/课堂 fixture，还是也能沉淀外部模型质量 baseline。

## Goals / Non-Goals

**Goals:**

- 在 `evalReadiness` 中输出模型质量 baseline 可比性。
- 让教师工作台评测沉淀块显示质量 baseline 是否可用、不可用原因和下一步动作。
- 保持现有 `status` 语义：诊断/课堂 fixture ready 不因为模型质量不可比而被整体降级。
- 使用当前作业运行证据，不读取 live eval JSON 作为生产数据源。

**Non-Goals:**

- 不改变 eval candidate 标记规则。
- 不改变 baseline regression report 或 live eval report 生成逻辑。
- 不新增数据库字段。
- 不把模型质量 baseline 可比性当成诊断 fixture 的唯一准入条件。

## Decisions

### 1. 在 `buildOverview` 中先构造 runtime signal，再构造 eval readiness

当前 `buildOverview` 已经在 builder 中内联调用 `buildRuntimeAttributionSignal` 和 `buildEvalReadiness`。本轮把 runtime signal 提前赋值给局部变量，再传入 `buildEvalReadiness`。这样避免重复扫描 `SubmissionAnalysis`，也保证 eval readiness 与教师端模型归因块使用同一份可比性判断。

### 2. `evalReadiness.status` 不强行降级

当教师纠错或课堂介入样本已经 ready 时，它们仍然可以沉淀为规则/教学 fixture。`modelQualityBaselineStatus=NOT_COMPARABLE` 只说明“不要把这些样本当作真实外部模型质量 baseline”。这样不阻断其他评测沉淀，同时避免误用。

### 3. baseline status 使用明确枚举

字段取值：

- `READY`：`qualityComparabilityStatus=COMPARABLE`
- `PARTIAL`：`qualityComparabilityStatus=PARTIAL`
- `BLOCKED`：`qualityComparabilityStatus=NOT_COMPARABLE`
- `NOT_APPLICABLE`：没有模型质量对比上下文

原因列表沿用 `qualityComparabilityReasons`，便于和 runtime attribution、source segments、live eval baseline report 对齐。

## Risks / Trade-offs

- [Risk] 教师可能把 `evalReadiness.status=READY` 和 `modelQualityBaselineStatus=BLOCKED` 看成冲突。→ UI 文案明确区分“fixture 沉淀”和“模型质量基线”。
- [Risk] 字段增加导致旧前端兼容问题。→ 前端类型全设为 optional，后端字段为新增非破坏字段。
- [Risk] 不读取 live eval JSON 会少看最新真实 smoke。→ 生产教师端保持基于当前作业证据；live eval JSON 仍用于测试报告和回归 gate。
