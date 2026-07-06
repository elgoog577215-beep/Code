## MODIFIED Requirements

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
