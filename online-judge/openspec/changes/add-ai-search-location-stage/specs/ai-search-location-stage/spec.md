## ADDED Requirements

### Requirement: 本地混合召回标准库候选
系统 SHALL 在完整 AI 诊断前，根据题目、代码、语言、判题结果、错误输出和 rule signals 从启用的标准库中召回候选条目。

#### Scenario: 文本结构召回可用
- **WHEN** 提交包含编译错误、运行时错误、错误答案或超时信号
- **THEN** 系统 MUST 基于标准库名称、分类、描述、知识点、能力点、易错点、代码形态和判题信号生成候选列表

#### Scenario: 混合召回限制候选数量
- **WHEN** 标准库条目数量大于候选上限
- **THEN** 系统 MUST 按混合分数返回不超过配置上限的候选，默认目标为 80-120 个

#### Scenario: 向量不可用降级
- **WHEN** embedding 配置缺失、向量表不可用或向量请求失败
- **THEN** 系统 MUST 使用文本结构召回继续搜索定位，并将 embedding 状态标记为降级

### Requirement: LLM 精选搜索定位结果
系统 SHALL 将本地候选包交给外部模型进行搜索定位，并要求模型返回结构化精选结果。

#### Scenario: 定位输出包含基础层和提高层
- **WHEN** 外部模型成功返回搜索定位结果
- **THEN** 输出 MUST 包含基础层候选、提高层候选、支撑知识点、证据引用、置信度和不确定性

#### Scenario: 定位结果不直接展示给学生
- **WHEN** 搜索定位阶段完成
- **THEN** 系统 MUST 仅将精选结果用于构造下一阶段标准库包，不得直接作为学生可见诊断文案

### Requirement: 定位输出后端校验
系统 SHALL 对 LLM 搜索定位输出进行后端校验。

#### Scenario: 未知 ID 被拒绝
- **WHEN** LLM 返回标准库中不存在的知识点、能力点、错因或提高点 ID
- **THEN** 搜索定位阶段 MUST 判定失败并回退旧标准库包逻辑

#### Scenario: 证据缺失被拒绝
- **WHEN** LLM 返回的候选没有证据引用或置信度越界
- **THEN** 搜索定位阶段 MUST 判定失败并回退旧标准库包逻辑

### Requirement: 精选标准库包用于完整诊断
系统 SHALL 使用搜索定位精选结果构造下一阶段完整诊断的 `StandardLibraryPack`。

#### Scenario: 定位成功后压缩标准库上下文
- **WHEN** 搜索定位通过校验
- **THEN** 第二次完整诊断请求 MUST 只接收精选后的标准库包和兼容字段

#### Scenario: 定位失败回退旧链路
- **WHEN** 搜索定位调用失败、输出无效、embedding 强依赖失败或后端校验失败
- **THEN** 系统 MUST 回退现有 `StandardLibraryPackBuilder` 输出，并继续执行完整诊断

### Requirement: 搜索定位可观测
系统 SHALL 在 AI 调用信息中记录搜索定位状态。

#### Scenario: 记录定位成功
- **WHEN** 搜索定位成功参与诊断
- **THEN** `AiInvocation` MUST 记录启用状态、成功状态、候选数量、精选数量和 embedding 状态

#### Scenario: 记录定位降级
- **WHEN** 搜索定位失败或向量降级
- **THEN** `AiInvocation` MUST 记录回退原因，不得伪装为完整 hybrid 成功

### Requirement: 标准库 embedding 生命周期
系统 SHALL 为标准库条目维护可过期、可重建的 embedding 状态。

#### Scenario: 标准库编辑后过期
- **WHEN** 教师创建或更新标准库条目
- **THEN** 系统 MUST 标记该条目的 embedding 过期或等待重建

#### Scenario: embedding 重建失败不影响标准库保存
- **WHEN** embedding 重建失败
- **THEN** 标准库编辑保存 MUST 成功，搜索定位向量部分 MUST 降级并记录原因
