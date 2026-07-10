## ADDED Requirements

### Requirement: 逐层挂接必须暴露临时诊断节点
系统 SHALL 在知识点诊断层中同时提供正式诊断节点和同父节点下的临时诊断节点，并 SHALL 明确两者优先级。

#### Scenario: 展开含临时节点的诊断层
- **WHEN** 后端展开一个存在待处理成长候选的知识点
- **THEN** 返回内容 SHALL 包含正式能力点、易错点、提升点和受限数量的临时候选
- **AND** 临时候选 SHALL 包含 code、name、layer、confidence、occurrenceCount 和 provisional 标记

#### Scenario: 正式节点优先
- **WHEN** 正式节点和临时候选都可见
- **THEN** 模型提示 SHALL 要求先判断正式节点
- **AND** 只有正式节点不能解释 issue 时才允许选择临时候选

### Requirement: 挂接失败必须保留已选 breadcrumb
系统 SHALL 在 `NO_MATCH`、`ATTACHMENT_FAILED` 或轮次耗尽时保留失败前已经确认的 breadcrumb，供临时节点创建和学生反馈路径回填使用。

#### Scenario: 诊断层调用失败但已有路径
- **WHEN** 标准库挂接在已经选择一个或多个知识节点后失败
- **THEN** issue anchor SHALL 保留已选择 path
- **AND** advice generation SHALL 收到该 path 和失败状态
- **AND** 整次学生诊断 MUST NOT 因挂接失败而被清空
