## ADDED Requirements

### Requirement: AI 标准库导航必须覆盖真实知识树深度
系统 SHALL 让 AI 标准库导航能够从当前知识树根节点逐层展开到知识点诊断层，并 SHALL 在每轮导航视图中提供当前允许选择的知识节点 code 和诊断层 code。系统 MUST NOT 因默认轮数不足而在模型看到知识点诊断层前强制失败。

#### Scenario: 多层知识树导航完成
- **WHEN** 标准库路径包含根节点、大章节、小章节、知识点和知识点诊断层
- **THEN** 系统 SHALL 允许 AI 导航完成逐层展开
- **AND** 系统 SHALL 在看到诊断层后接受包含能力点、易错点或提升点的 `DONE` 导航结果
- **AND** 系统 SHALL 将导航结果传给最终诊断阶段

#### Scenario: 当前视图限定可选 code
- **WHEN** 系统向 AI 发送某一轮标准库导航视图
- **THEN** 视图 SHALL 包含当前可选择的知识节点 code 列表
- **AND** 如果已展开知识点诊断层，视图 SHALL 包含当前可选择的能力点、易错点和提升点 code 列表
- **AND** 模型返回的 `selectedBranches` MUST 只引用当前视图中出现的知识节点 code

#### Scenario: 非法分支选择修正
- **WHEN** 模型返回的 `selectedBranches` 引用了当前视图没有出现的知识节点 code
- **THEN** 系统 SHALL 最多发起一次结构化修正请求
- **AND** 修正请求 SHALL 要求模型从当前合法 code 中重选或返回 `NO_MATCH`
- **AND** 如果修正后仍不合法，系统 SHALL 标记标准库导航失败

#### Scenario: 最后一轮不得继续展开
- **WHEN** 标准库导航到达本次允许的最后一轮
- **THEN** 系统 SHALL 要求模型返回 `DONE` 或 `NO_MATCH`
- **AND** 如果模型仍返回 `CONTINUE`，系统 SHALL 标记标准库导航失败
- **AND** 系统 MUST NOT 恢复本地召回或生成缺少导航依据的学生可见诊断
