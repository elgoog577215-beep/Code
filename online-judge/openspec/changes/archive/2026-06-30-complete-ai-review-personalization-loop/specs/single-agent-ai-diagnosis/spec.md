## MODIFIED Requirements

### Requirement: 单诊断 Agent 为默认实时路线

系统 SHALL 默认使用“本地召回树形标准库包 + 单诊断 Agent”的实时路线，且学生可见输出以自然语言 `studentReport` 为主。

#### Scenario: 默认提交诊断
- **WHEN** 学生提交代码并触发 AI 诊断
- **THEN** 系统 SHALL 先执行本地召回
- **AND** 系统 SHALL 将题目、代码、判题结果、证据、本地召回标准库包和必要学生画像上下文发送给一次诊断 Agent
- **AND** 系统 SHALL 不默认调用额外实时搜索 Agent
- **AND** 输出 SHALL 包含学生可读 `studentReport` 和机器可读元数据

#### Scenario: 显式打开搜索 Agent 对照路线
- **WHEN** 配置显式设置 `AI_SEARCH_LOCATION_ENABLED=true`
- **THEN** 系统 MAY 运行搜索 Agent 对照路线
- **AND** trace SHALL 清楚标记该路线不是默认教学路线
