## ADDED Requirements

### Requirement: 自由诊断必须输出多个可追踪 issue
系统 SHALL 在自由诊断阶段输出 `issues[]` 作为后续标准库挂接和建议生成的主数据源。

#### Scenario: 多错误输出多个 issue
- **WHEN** 学生提交同时包含多个可由证据支持的错误
- **THEN** 自由诊断输出 SHALL 包含多个 issue
- **AND** 每个 issue SHALL 包含稳定 `issueId`、标题、原因、证据引用和置信度
- **AND** 系统 MUST NOT 在自由诊断阶段把多个错误压缩为单一主因字符串

#### Scenario: issue 引用合法证据
- **WHEN** 自由诊断输出 issue
- **THEN** issue 的 `evidenceRefs` SHALL 引用当前 evidence package 中合法证据
- **AND** 单个 issue 证据无效时系统 SHALL 降级或丢弃该 issue
- **AND** 系统 MUST NOT 因一个 issue 证据无效而清空其他合法 issue

#### Scenario: 分层优惠最短路多 issue
- **WHEN** 题目要求优惠券将边权变为 `floor(w / 2)` 且学生代码同时存在优惠转移未折半、按节点全局剪枝和恰好 k 层取值问题
- **THEN** 自由诊断 SHALL 至少识别优惠转移未折半
- **AND** 自由诊断 SHOULD 保留其他有证据支持的问题作为独立 issue

### Requirement: advice generation 必须以 issues 为主输入
系统 SHALL 将自由诊断 issues 作为 advice generation 的主要输入，并 SHALL 将标准库 anchors 作为可选辅助。

#### Scenario: 标准库挂接成功
- **WHEN** issues 中部分或全部 issue 拥有标准库 anchors
- **THEN** advice generation SHALL 使用 anchors 统一术语和颗粒度
- **AND** advice generation SHALL 仍以 issue 的代码证据和判题事实为准

#### Scenario: 标准库挂接不可用
- **WHEN** 标准库为空、挂接失败或所有 issue 均 `NO_MATCH`
- **THEN** advice generation SHALL 继续基于 issues 生成学生可见建议
- **AND** 输出 SHALL 在内部 trace 中记录标准库挂接状态
- **AND** 学生可见文本 MUST NOT 暴露后端字段名或内部失败码

#### Scenario: advice 覆盖多个 issue
- **WHEN** issues 包含多个有效错误
- **THEN** advice generation SHALL 能为多个 issue 生成对应的基础层建议
- **AND** 系统 MUST NOT 因只有一个标准库 anchor 而删除其他 issue 的建议
