## ADDED Requirements

### Requirement: 自由诊断与标准库路径同步标注
系统 SHALL 允许单诊断 Agent 在同一次正式诊断输出中同时完成真实问题诊断和标准库路径标注；标准库路径 SHALL 辅助教学归档，不得替代模型对题目、完整代码和运行结果的独立判断。

#### Scenario: 候选携带标准库路径
- **WHEN** 模型输出 `diagnosisCandidates`
- **THEN** 每个候选 SHALL 包含最接近的标准库路径描述
- **AND** 每个候选 SHALL 标明 `libraryFit` 为 `HIT`、`PARTIAL` 或 `MISS`

#### Scenario: 具体错误点未覆盖
- **WHEN** 模型诊断出的具体错误点不在当前标准库中，但上级知识点或能力点存在
- **THEN** 候选 SHALL 标记为 `PARTIAL`
- **AND** 候选 SHALL 保留上级标准库路径
- **AND** `libraryGrowth.candidates` SHALL 包含待审核的新错误点候选

#### Scenario: 标准库不改变真实诊断
- **WHEN** 标准库候选不能解释当前代码真实问题
- **THEN** 模型 SHALL 保持自由诊断结论
- **AND** 模型 SHALL NOT 为了命中标准库而把主因改写为不符合代码语义的错误点

#### Scenario: 分层优惠最短路误诊防回归
- **WHEN** 题目要求优惠券将边权变为 `floor(w / 2)` 且学生代码在优惠转移中仍使用原边权
- **THEN** 模型 SHALL 将主因诊断为优惠转移未折半
- **AND** 模型 SHALL NOT 将主因诊断为起点状态或堆初始化错误
