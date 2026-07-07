## ADDED Requirements

### Requirement: 单诊断 Agent 必须接收知识点诊断层候选包
单诊断 Agent 的标准库上下文 SHALL 按知识点、能力点、易错点和提升点组织，且 SHALL 明确该结构是一棵统一知识树下的诊断层，而不是知识树和标准库两套平行库。

#### Scenario: 诊断上下文包含知识点诊断层
- **WHEN** 系统准备单诊断 Agent 输入
- **THEN** `standardLibrary.knowledgeGroups` SHALL 以知识点为第一层
- **AND** 每个知识点分组 SHALL 包含该知识点下召回的能力点、易错点或提升点
- **AND** prompt SHALL 要求模型沿“知识点 -> 能力点 -> 易错点”理解候选结构

#### Scenario: 模型仍可自由判断命中状态
- **WHEN** 知识点诊断层候选无法解释当前提交
- **THEN** 模型 SHALL 可以返回 PARTIAL、MISS 或 OUT_OF_LIBRARY
- **AND** 模型 SHALL NOT 因存在同知识点候选而强行输出易错点 HIT
