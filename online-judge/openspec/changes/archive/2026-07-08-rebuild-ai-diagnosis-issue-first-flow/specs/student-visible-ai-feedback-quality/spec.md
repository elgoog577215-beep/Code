## ADDED Requirements

### Requirement: 学生建议不得被标准库挂接失败清空
系统 SHALL 在自由诊断 issues 有效时生成学生可见建议，即使标准库挂接为空、失败或未命中。

#### Scenario: 空标准库仍生成建议
- **WHEN** 自由诊断输出有效 issues 但标准库根目录为空
- **THEN** 学生可见反馈 SHALL 包含基于 issues 的基础层建议
- **AND** 学生可见反馈 MAY 省略标准库路径标签
- **AND** 系统 MUST NOT 返回空的基础建议和提高建议作为成功诊断结果

#### Scenario: 挂接失败不暴露内部错误
- **WHEN** 标准库挂接失败但 advice generation 成功
- **THEN** 学生可见反馈 SHALL 展示自然诊断和学习建议
- **AND** 学生可见反馈 MUST NOT 包含 `selectedBranches`、`CONTINUE`、`anchorStatus`、`MODEL_FAILED` 等内部字段

### Requirement: 多 issue 必须保留为多条建议
系统 SHALL 将多个有效 issue 映射为多条结构化建议，并 SHALL 在后处理和展示中保真。

#### Scenario: 多个基础错误
- **WHEN** advice generation 返回多条证据有效且安全的基础层建议
- **THEN** 后端 SHALL 保留这些建议
- **AND** 前端或报告 SHALL 展示多条建议
- **AND** 系统 MUST NOT 使用第一条建议覆盖整组建议

#### Scenario: 提高建议独立于基础建议
- **WHEN** 模型返回多个提高层建议且这些建议不重复基础层
- **THEN** 系统 SHALL 保留这些提高层建议
- **AND** 系统 MUST NOT 因基础层已有多条建议而丢弃提高层建议

### Requirement: 真实样本报告必须包含链路 trace 摘要
系统 SHALL 在真实样本仿真报告中输出足够复盘的阶段摘要和 trace 文件路径。

#### Scenario: 报告包含 trace 路径
- **WHEN** 真实样本仿真运行并调用外部模型
- **THEN** 报告 SHALL 包含 trace 文件路径
- **AND** trace SHALL 至少区分自由诊断、标准库挂接和 advice generation 三类阶段

#### Scenario: 报告区分前置失败和 advice 数量
- **WHEN** 学生建议数为 0
- **THEN** 报告 SHALL 标明是自由诊断失败、标准库挂接降级、advice generation 失败还是模型没有返回建议
- **AND** 报告 MUST NOT 仅用建议数 0 推断多条建议能力失败
