# single-agent-ai-diagnosis Specification

## Purpose
TBD - created by archiving change simplify-ai-diagnosis-single-agent. Update Purpose after archive.
## Requirements
### Requirement: 实时诊断使用单诊断 Agent
系统 SHALL 在默认实时 AI 诊断链路中只调用一次外部大模型，由单诊断 Agent 完成问题定位、候选取舍、学生反馈和结构化元数据输出。

#### Scenario: 默认实时诊断只调用一次外部模型
- **WHEN** 学生提交代码且外部 AI 可用
- **THEN** 系统 SHALL 只调用正式诊断报告模型阶段，不调用外部搜索定位模型阶段

#### Scenario: 默认配置关闭实时搜索 Agent
- **WHEN** 未显式开启 `AI_SEARCH_LOCATION_ENABLED`
- **THEN** 系统 SHALL 不调用 `search-location-v1`
- **AND** trace SHALL 将召回状态标记为 `LOCAL_RECALL`

#### Scenario: 单诊断 Agent 接收本地召回候选
- **WHEN** 后端已从标准库本地召回候选条目
- **THEN** 单诊断 Agent 的输入 MUST 包含题目、代码、判题结果、证据和树形候选标准库上下文

#### Scenario: 单诊断 Agent 接收主路径语义
- **WHEN** 候选标准库上下文包含能力点或易错点
- **THEN** 单诊断 Agent 的输入 MUST 保留主知识点、相关知识点和能力点归属
- **AND** 模型 SHALL 被提示优先沿主知识点 -> 能力点 -> 易错点理解，而不是把所有知识点当作平铺标签

#### Scenario: 显式打开搜索 Agent 对照路线
- **WHEN** 配置显式设置 `AI_SEARCH_LOCATION_ENABLED=true`
- **THEN** 系统 MAY 运行搜索 Agent 对照路线
- **AND** trace SHALL 清楚标记该路线不是默认教学路线

### Requirement: 标准库作为候选地图而不是强制答案
系统 SHALL 允许单诊断 Agent 对本地召回候选标记命中、部分命中或未命中，并允许输出库外发现。

#### Scenario: 候选不匹配真实问题
- **WHEN** 本地召回候选不能解释学生错误
- **THEN** 模型输出 SHALL 能标记未命中并给出库外发现，而不是强行绑定错误标准库条目

### Requirement: 学生反馈以自然报告为主
系统 SHALL 将学生端主反馈组织为基础层、提高层和下一步行动，并保留后端可校验元数据。学生可见报告 MUST 使用自然、连贯、适合高中学生阅读的中文表达；结构化字段 SHALL 服务校验、画像和评测，不得污染学生端主表达。

#### Scenario: 学生查看 AI 反馈
- **WHEN** 单诊断 Agent 输出有效报告
- **THEN** 学生端 SHALL 优先展示 `studentReport` 的自然语言报告，而不是碎字段拼接文本

#### Scenario: 结构化字段服务机器消费
- **WHEN** 单诊断 Agent 输出 `studentReport` 和元数据
- **THEN** 学生端 SHALL 以 `studentReport` 作为主展示
- **AND** `libraryFit`、标签、证据、泄题风险等元数据 SHALL 用于校验、画像、推荐、教师追踪和评测

#### Scenario: 学生反馈长度受控
- **WHEN** 单诊断 Agent 生成学生可见反馈
- **THEN** 系统 SHALL 要求基础层、提高层和下一步行动保持短句、明确证据和可执行动作，且不得生成长篇完整教程

#### Scenario: 基础层优先
- **WHEN** 提交未通过且存在基础层阻塞问题
- **THEN** 学生反馈 SHALL 先说明基础层问题，再给出次要提高层建议

#### Scenario: 下一步行动不是直接改法
- **WHEN** 单诊断 Agent 生成 `studentReport.nextActionText`
- **THEN** 下一步行动 SHALL 优先要求学生手推、复述状态、比较输出、估算复杂度或构造小样例
- **AND** 下一步行动 MUST NOT 以可复制代码修改步骤作为主表达

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
