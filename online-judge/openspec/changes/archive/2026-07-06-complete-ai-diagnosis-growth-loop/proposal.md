## Why

第一阶段已经把正式诊断契约改成“AI 自由诊断时同步标注标准库路径”，但它还只是主调用契约和校验门禁。要让这条链路真正成为可持续产品闭环，还需要真实模型回归、上下文包升级、教师审核候选池和标准库治理机制一起补齐。

## What Changes

- 增加真实 AI 回归样本集，覆盖“分层优惠最短路”等关键误诊案例，验证模型真实输出是否命中主因、路径和待审核候选。
- 升级正式诊断上下文包，让外部模型稳定看到完整题目、完整学生代码、判题参考信号和结构化标准库路径邻域。
- 完善标准库成长候选的后端接口和教师端审核流程，支持查看、批准、修改、拒绝和合并候选。
- 增加标准库治理质量视图，统计重复候选、薄弱路径、高频未命中和待审核积压。
- 保持治理 Agent 作为后续可选能力，本轮先实现可人工审核和可度量的闭环。

## Capabilities

### New Capabilities

- `standard-library-review-workflow`: 标准库成长候选的教师审核、修改、合并、拒绝和入库闭环。
- `standard-library-governance-quality`: 标准库候选治理质量统计，包括重复候选、薄弱路径、高频未命中和审核积压。

### Modified Capabilities

- `ai-diagnosis`: 正式诊断需要可被真实模型回归样本验证主因、标准库路径和候选状态。
- `ai-prompt-context-quality`: 上下文包需要稳定提供完整题目、完整代码和结构化标准库邻域，而不是零散证据或单条候选。
- `ai-diagnosis-quality-loop`: 质量回归需要覆盖真实模型输出中的路径标注、`PARTIAL` 候选和待审核成长候选。
- `standard-library-normalized-schema`: 成长候选需要支持教师审核动作、合并目标和正式入库后的状态流转。

## Impact

- 后端 AI 诊断和 live eval：`AiReportService`、`PromptTemplateRegistry`、`ExternalModelAgentRuntime`、相关 eval/report 测试。
- 标准库成长候选后端：`AiStandardLibraryGrowthAgentService`、候选实体、repository、教师管理接口。
- 教师端标准库管理页面：待审核候选列表、候选详情、审核动作和治理质量摘要。
- OpenSpec、单元测试、集成测试和前端构建验证。
