## Why

真实长代码样本 trace 已证明旧链路不是单点 bug：自由诊断能发现多个真实错误，但标准库导航会因空目录、空分支、超时或输出截断阻断 advice generation，导致学生端没有任何建议。当前设计把标准库导航做成模型驱动的协议执行题，偏离了“标准库服务 AI、后端控制结构、AI 诊断真实问题”的目标。

本变更将主链路重构为 issue-first：AI 先基于题目、代码和判题事实输出多个诊断 issue；后端再按 issue 逐层浏览标准库目录并做可选挂接；标准库挂接失败不得阻断多条学生建议生成。

## What Changes

- **BREAKING**: 默认诊断编排不再使用“初步诊断 -> AI 自管标准库导航 -> 最终诊断”的硬闸门。
- **BREAKING**: 标准库导航失败、空根目录、空分支或轮次耗尽不再使整次 AI 诊断失败；这些情况只影响标准库挂接状态。
- 新增 issue-first 自由诊断契约，要求模型先输出 `issues[]`，每条 issue 有稳定 `issueId`、名称、原因、证据引用和置信度。
- 新增后端控制的逐层标准库挂接能力：后端维护当前层、breadcrumb、轮次和可选 code；AI 每轮只返回 `SELECT`、`DONE` 或 `NO_MATCH`。
- 标准库挂接按 issue 进行，而不是整题只选一个路径，避免多个错误被压扁成一条建议。
- advice generation 以 `issues[]` 为主输入，以标准库 anchors 为可选参考，必须能输出多条 `basicLayerAdvice` 和多条 `improvementLayerAdvice`。
- 增加可审计 trace，记录每阶段请求摘要、模型响应、后端判定、降级原因和 advice 数量。
- Spec 作为实现前硬门禁：必须产出 schema、状态机、降级矩阵、旧链路删除/绕开清单和测试断言，不能只写概念说明。

## Capabilities

### New Capabilities

- `standard-library-layered-attachment`: 后端控制的标准库逐层目录挂接能力，覆盖当前层目录、AI 选择动作、breadcrumb、空目录降级和按 issue 挂接。

### Modified Capabilities

- `ai-diagnosis-orchestrator-v2`: 默认编排改为 issue-first 主链路，标准库挂接从硬闸门改为可选辅助阶段。
- `ai-diagnosis`: 自由诊断输出从单一主因/候选升级为多个可追踪 issue，并允许每个 issue 独立标准库挂接。
- `student-visible-ai-feedback-quality`: 学生可见建议必须基于多个 issue 保真输出，标准库挂接失败不得把多条有效建议压缩或清空。

## Impact

- 后端：影响 `AiReportService`、`PromptTemplateRegistry`、`ExternalModelAgentRuntime`、标准库导航/pack builder、AI invocation telemetry、真实样本仿真测试。
- DTO/契约：新增或调整自由诊断 issue、标准库挂接 anchor、导航动作和 trace 字段。
- 标准库读取：继续读取现有知识树和规范标准库表，不新增内容 seed，不把测试空库问题用 seed 掩盖。
- 测试：新增空标准库、按 issue 多错误、多层目录选择、导航失败降级、多建议保真和 trace 落盘回归。
- 文档：新增 Spec 产物和项目记忆，明确旧协议不再作为主路径。
