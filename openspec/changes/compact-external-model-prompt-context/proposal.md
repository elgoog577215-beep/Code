## Why

外部模型 live eval 已显示：当模型成功返回时，长代码诊断的错因、证据、安全和教学动作可以达标；当前更大的瓶颈是完成率和配额。继续单纯优化提示词措辞收益有限，必须降低每次外部调用的上下文成本。

本变更用于把外部模型请求体从“完整内部对象”收敛为“模型必需的紧凑上下文”，在不削弱后端验证器和安全门禁的前提下降低 token 消耗。

## What Changes

- 对发送给外部模型的 `ModelDiagnosisBrief` 做紧凑化：限制题面摘要、代码片段、候选信号和可见用例数量。
- 对发送给外部模型的 `StandardLibraryPack` 做紧凑化：保留标签 ID、父标签、教学动作、学生任务模板和关键决策规则，移除学生解释、教师解释、能力点、whenToUse 等冗余字段。
- 三条外部模型运行路径共用紧凑上下文：诊断阶段、教学提示阶段、single-call 阶段。
- 后端验证、归一化、证据校验、安全校验仍使用完整 `RuntimePlan`，避免把成本优化变成质量放松。
- 增加测试捕获真实 user prompt，验证紧凑请求体保留核心信号并移除冗余字段。

## Capabilities

### New Capabilities

- `compact-external-model-prompt-context`: 外部模型调用使用低预算上下文，同时保持诊断、证据、标准库和安全验证能力。

### Modified Capabilities

无。

## Impact

- 影响 `AiReportService` 外部模型请求体构造。
- 影响 `AiReportServiceExternalRuntimeTest`，新增 prompt context 压缩回归测试。
- 不改变线上接口、不改变模型返回 schema、不改变 validator 的完整验证依据。
