## MODIFIED Requirements

### Requirement: 实时诊断使用单诊断 Agent
系统 SHALL 在学生可见内容生产上只保留一个最终诊断 Agent；前置初步诊断和 AI 标准库导航只生产中间结构、导航路径和标准库锚点，不直接生成学生可见报告。

#### Scenario: 默认实时诊断使用三阶段编排
- **WHEN** 学生提交代码且外部 AI 可用
- **THEN** 系统 MAY 调用初步诊断、AI 标准库导航和最终诊断三个阶段
- **AND** 只有最终诊断阶段 SHALL 生成 `studentReport`

#### Scenario: 默认配置不使用本地召回
- **WHEN** 系统执行默认实时诊断
- **THEN** 系统 SHALL NOT 调用 `search-location-v1`
- **AND** 系统 SHALL NOT 调用 `SearchLocationRetrievalService.retrieve(...)`
- **AND** trace SHALL NOT 将召回状态标记为 `LOCAL_RECALL`

#### Scenario: 前置阶段不污染学生报告
- **WHEN** 初步诊断或标准库导航产生中间判断
- **THEN** 学生端 SHALL NOT 直接展示这些中间判断
- **AND** 中间判断 SHALL 只用于最终诊断、教师 trace、评测和待审核成长候选

### Requirement: 单诊断 Agent 必须接收知识点诊断层候选包
最终诊断 Agent 的标准库上下文 SHALL 来自 AI 导航确认后的知识点诊断层结构，且 SHALL 明确该结构是一棵统一知识树下的诊断层，而不是知识树和标准库两套平行库。

#### Scenario: 最终诊断上下文包含导航确认路径
- **WHEN** 系统准备最终诊断 Agent 输入
- **THEN** `standardLibrary.knowledgeGroups` SHALL 以 AI 导航选中的知识点为第一层
- **AND** 每个知识点分组 SHALL 包含该知识点下被导航选中的能力点、易错点或提升点
- **AND** prompt SHALL 要求模型沿“知识点 -> 能力点 -> 易错点/提升点”理解候选结构

#### Scenario: 模型仍可自由判断命中状态
- **WHEN** AI 导航确认的知识点诊断层仍无法解释当前提交
- **THEN** 最终诊断模型 SHALL 可以返回 `PARTIAL`、`MISS` 或 `OUT_OF_LIBRARY`
- **AND** 模型 SHALL NOT 因导航阶段选择过同知识点而强行输出易错点 `HIT`
