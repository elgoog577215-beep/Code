# AI 链路适配标准库主路径

## Why

标准库已经收束为“知识节点 -> 能力点 -> 易错点/提升点”的主结构，但正式 AI 链路在压缩候选包和提示词消费时仍可能把 `knowledgeNodeCodes` 当成平铺标签。这样会让学生端看到一堆独立标签，也会让模型不清楚主路径和相关知识的区别。

## What Changes

- 保持默认链路为本地召回 + 单诊断 Agent，不新增实时 Agent。
- 压缩后的 `StandardLibraryPack` 继续保留 `primaryKnowledgeNodeCode` 与 `relatedKnowledgeNodeCodes`。
- 正式诊断 prompt 明确：主路径优先，相关知识只作辅助，不把相关标签拆成独立问题。
- 学生快反馈 prompt 同步说明 `knowledgePath` 是父子路径，并优先来自主路径。
- 增加最小测试，锁住主路径字段不会在运行链路中丢失。

## Impact

- 后端 AI 运行时：`ExternalModelAgentRuntime`。
- 正式诊断 prompt 与学生快反馈 prompt。
- 后端测试：标准库包压缩、快反馈上下文。

## Non-Goals

- 不重建标准库表。
- 不恢复双外部 Agent 默认链路。
- 不扩大 prompt 输出协议。
- 不改学生端展示布局。
