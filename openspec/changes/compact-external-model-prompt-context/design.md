## Context

当前 single-call runtime 已经把一次学生诊断从两次模型调用降低为一次调用，但请求体仍包含完整 `ModelDiagnosisBrief` 和完整 `StandardLibraryPack`。其中部分字段对模型选择标签和生成教学动作有帮助，另一部分字段主要服务教师解释、前端展示或内部统计，直接发送会增加 token 成本。

live eval 的目标已经从“模型能否正确分析”推进到“外部模型能否稳定在线完成”。在配额波动和 token 预算有限的情况下，请求体压缩是比继续增加规则更直接的完成率优化。

## Goals / Non-Goals

**Goals:**

- 降低外部模型请求体字符数和 token 压力。
- 保留模型完成诊断所需的题面、代码、候选信号、证据引用、可选标签和教学动作。
- 保留后端完整验证能力，确保模型不能因为紧凑上下文绕过标准库、证据和安全门禁。
- 用测试确认紧凑请求体确实不包含冗余字段。

**Non-Goals:**

- 不改变模型输出 JSON schema。
- 不调整 live eval 目标阈值。
- 不移除标准库或 validator。
- 不把本地规则结果当作外部模型质量。

## Decisions

### 1. 在请求发送前压缩，而不是改变内部对象

`ModelDiagnosisBrief` 和 `StandardLibraryPack` 仍保持完整结构，供 validator、报告、前端和教师端使用。只有 `AiReportService` 构造外部模型 user prompt 时转成紧凑 Map。

替代方案是直接删 DTO 字段，但这会破坏现有内部能力，并且难以判断哪些消费方依赖字段。

### 2. 压缩字段遵循“诊断必需优先”

保留题面摘要、代码片段、首个失败用例、少量可见用例、候选信号、证据引用、可选标签、记忆校准和隐藏数据边界。标准库保留标签 ID、父标签、教学动作、任务模板和关键规则。

替代方案是只发送 candidateSignals，不发送标准库；这会让模型更容易输出非法标签或泛化教学动作。

### 3. 完整 RuntimePlan 继续用于校验

外部模型收到的是紧凑上下文，但后端验证仍使用完整 `RuntimePlan`。这保证了成本优化不会放宽证据引用、标签选择和安全检查。

替代方案是让 validator 也使用紧凑对象，但这会降低校验覆盖。

## Risks / Trade-offs

- [Risk] 压缩过度导致模型缺少解释性上下文。→ Mitigation：保留 candidateSignals、evidenceRefs、allowed tags 和 teachingActions；通过 live eval 观察 completed output 质量。
- [Risk] 标签或教学动作顺序不稳定。→ Mitigation：测试断言包含核心动作，而不是固定列表首项。
- [Risk] 成本降低不等于配额问题完全解决。→ Mitigation：继续通过 route outcome 区分 token 成本、供应商额度和备用路由问题。
