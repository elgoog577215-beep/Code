## MODIFIED Requirements

### Requirement: 模型输出必须通过程序校验

系统 MUST 对外部模型每个阶段的输出执行程序校验，包括 JSON 合法性、标签合法性、证据引用合法性、字段完整性、泄题风险和记忆证据使用边界。

#### Scenario: 非法标签被拒绝或归一化
- **WHEN** 模型输出不存在于标准库的错因标签或细粒度标签
- **THEN** 系统 MUST 拒绝该字段或归一化为合法标签，并记录校验结果

#### Scenario: 无效证据引用被拒绝
- **WHEN** 模型输出的 `evidenceRefs` 不存在于 `ModelDiagnosisBrief`、规则信号或证据包中
- **THEN** 系统 MUST 拒绝该证据引用，并在结果中保留可审计的失败原因

#### Scenario: 主诊断不能只依赖学习记忆
- **WHEN** 模型输出的主错因或细粒度错因只引用 `memory:*` 证据，且没有当前提交证据支撑
- **THEN** 系统 MUST 拒绝该输出或回退到本地规则结果，并记录记忆过度依赖风险

#### Scenario: 泄题风险触发安全降级
- **WHEN** 模型输出包含完整代码、最终答案、隐藏测试数据或逐步替学生完成解法
- **THEN** 系统 MUST 将 `answerLeakRisk` 标记为 `HIGH`，并且 MUST 使用安全版本提示或本地规则结果替代学生可见内容
