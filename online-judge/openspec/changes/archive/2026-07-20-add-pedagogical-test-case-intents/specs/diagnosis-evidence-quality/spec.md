## ADDED Requirements

### Requirement: 测试点语义证据必须可追溯且不替代代码证据
诊断证据包 SHALL 为已执行测试点保存稳定语义引用和通过状态；模型可以把它作为评测覆盖证据，但 MUST 结合题目、代码和实际行为差距判断错因。

#### Scenario: 失败测试点具有语义快照
- **WHEN** 诊断证据包包含一个已失败且具有语义快照的测试点
- **THEN** 简报 SHALL 提供合法的 `judge:test-intent:<semantic-code>` 证据引用
- **AND** 语义事实 SHALL 明确包含该测试点的通过状态

#### Scenario: 测试意图与代码证据冲突
- **WHEN** 测试点意图暗示某类覆盖但学生代码和判题行为不支持相应错因
- **THEN** 诊断 SHALL 以代码和实际行为为准
- **AND** SHALL NOT 仅凭测试意图生成正式错因或标准库命中

#### Scenario: 历史语义快照与当前内容版本不同
- **WHEN** 一个历史判题结果保存的语义快照与当前测试点内容不同
- **THEN** 该次历史诊断 SHALL 使用保存时快照
- **AND** 审计 SHALL 能同时识别测试点身份和快照语义 code

