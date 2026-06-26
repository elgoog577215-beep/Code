## Why

当前实时 AI 诊断链路把“搜索定位”和“诊断建议”都做成大模型阶段，导致模型重复读题、重复判断，链路更慢、更贵，也容易把自然教学表达切碎。最近单提示词对照实验说明，强模型更适合完整思考一次；标准库应作为上下文地图服务模型，而不是把模型拆成两个实时 Agent。

## What Changes

- 将实时诊断链路从“双大模型 Agent”收敛为“本地召回 + 单诊断 Agent”。
- 搜索定位不再默认调用外部大模型；后端继续用本地召回构造候选标准库包。
- 保留树形标准库上下文、命中/半命中/未命中、库外发现、后端底线校验和降级机制。
- 调整正式诊断提示词，使其像对照实验一样让模型完整阅读题目、代码、判题结果和标准库候选后生成自然学生反馈。
- 学生端继续优先展示 `studentReport` 的自然段落；结构化信息只作为后端校验、教师追踪和标准库沉淀使用。
- 不在本阶段重做标准库内容、不新增复杂工具调用、不引入新的向量服务。

## Capabilities

### New Capabilities
- `single-agent-ai-diagnosis`: 规定实时 AI 诊断使用本地召回加单诊断 Agent 的运行方式、输入输出和降级要求。

### Modified Capabilities
- `ai-diagnosis-orchestrator-v2`: 将原有诊断报告 v2 的实时搜索大模型阶段改为可关闭/默认跳过，诊断阶段直接接收本地召回后的树形标准库上下文。

## Impact

- 影响后端 AI 诊断编排：`AiReportService`、`ExternalModelAgentRuntime`、`PromptTemplateRegistry`、搜索定位相关结果记录与测试。
- 影响诊断 prompt：`diagnosis-report-v2` 需要更像单诊断 Agent 的完整教学提示词。
- 影响 trace/readiness：搜索定位阶段应标记为本地召回或跳过，而不是实时模型失败。
- 影响测试：需要验证默认实时链路只调用一次外部模型，关闭旧搜索 Agent 后仍能生成学生反馈。
