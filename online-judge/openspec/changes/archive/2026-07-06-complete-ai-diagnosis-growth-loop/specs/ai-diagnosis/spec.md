## ADDED Requirements

### Requirement: 真实模型回归必须验证路径标注
系统 SHALL 使用真实诊断样本验证正式 AI 输出是否同时命中真实主因、标准库路径和成长候选状态。

#### Scenario: 分层优惠最短路真实样本
- **WHEN** 真实模型或离线 fixture 诊断“优惠券跨层转移仍使用原边权”的提交
- **THEN** 评测 SHALL 断言主因包含优惠转移未折半
- **AND** 评测 SHALL 断言输出包含非空标准库路径
- **AND** 评测 SHALL 断言未精确命中标准库时生成待审核成长候选

#### Scenario: 标准库路径缺失
- **WHEN** 真实诊断输出包含 `diagnosisCandidates` 但缺少标准库路径
- **THEN** 评测 SHALL 标记该样本为路径标注失败
