## Why

标准库已经收纳成“知识树 → 能力点 → 易错点 / 提升点”的规范结构，但 AI 链路仍主要通过旧式 `basicCauses/improvementPoints` 语义消费候选，容易把新结构重新压扁。现在需要让召回、候选选择、模型 prompt 和校验语义都和规范标准库匹配，让数据库结构真正进入诊断链路。

## What Changes

- 在 `StandardLibraryPack` 中加入结构化标准库视图，用来表达知识节点、能力点、易错点和提升点之间的层级关系。
- 调整本地召回和搜索定位候选包，补全父能力点、同能力易错点、相关提升点和知识路径，减少模型只看到孤立条目的情况。
- 调整搜索定位选择器：当命中易错点时，同时保留父能力点、同层可区分易错点和相关提升点；当命中能力点时，保留其下主要易错点。
- 调整诊断 prompt：明确要求模型先按新结构理解候选，再基于当前提交证据选择多个真实错因和多个提升方向。
- 保持现有输出 schema 和学生反馈 DTO 兼容，不做前端破坏性变更。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `standard-library-normalized-schema`: 标准库候选包必须提供结构化视图，表达知识点、能力点、易错点和提升点之间的关系。
- `ai-diagnosis-orchestrator-v2`: AI 诊断编排必须使用结构化标准库候选，而不是只依赖旧基础错因和提高建议列表。

## Impact

- 影响 `StandardLibraryPack`、`SearchLocationCandidate`、`SearchLocationRetrievalService`、`SearchLocationPackSelector`、`ExternalModelAgentRuntime` 和 `PromptTemplateRegistry`。
- 影响搜索定位与诊断报告相关测试。
- 不修改正式学生反馈响应格式，不删除旧兼容字段。
- 不增加独立证据模式表；证据仍来自当前提交、评测和模型输出引用。
