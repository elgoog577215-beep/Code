## MODIFIED Requirements

### Requirement: 学生复盘报告优先展示
系统 SHALL 在学生提交后优先提供学生可读的复盘报告，而不是直接展示机器字段拼接内容。

#### Scenario: AI 复盘成功
- **WHEN** 学生提交产生判题结果且外部 AI 返回合法诊断
- **THEN** 系统返回包含基础层、提高层和下一步行动的 `studentReport`
- **AND** 学生端优先展示 `studentReport`
- **AND** 结构化字段仅用于校验、追踪、画像和评测

#### Scenario: AI 不可用
- **WHEN** 外部模型认证、额度、超时或服务不可用
- **THEN** 系统不得伪装成 AI 成功
- **AND** 学生端 SHALL 明确展示 AI 复盘暂不可用
- **AND** 系统 MUST NOT 返回规则反馈作为 AI 复盘替代
- **AND** trace SHALL 记录失败原因
