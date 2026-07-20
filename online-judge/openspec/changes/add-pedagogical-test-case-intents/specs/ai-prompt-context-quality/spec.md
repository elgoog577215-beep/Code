## ADDED Requirements

### Requirement: AI 上下文必须正确解释测试点语义
自由诊断和最终诊断 prompt SHALL 将测试点语义定义为命题者对评测覆盖范围的说明，而不是预先计算的诊断答案，并 SHALL 要求模型先用代码和行为证据验证。

#### Scenario: 自由诊断读取测试点语义
- **WHEN** `free-diagnosis-v2` 上下文包含 `testIntentFacts`
- **THEN** prompt SHALL 允许模型使用其区分代表性、边界、结构、规模和性能覆盖
- **AND** prompt SHALL 禁止把某测试点失败直接等同于某个代码错因

#### Scenario: 最终诊断引用测试点语义
- **WHEN** `diagnosis-report-v4` 使用 `judge:test-intent:<semantic-code>` 证据
- **THEN** 模型 SHALL 保留自由诊断中已经由代码和行为支持的问题
- **AND** 标准库挂接 SHALL 只统一术语和路径，不得把测试点映射直接当作诊断命中

### Requirement: AI 上下文必须限制隐藏测试点语义的使用方式
prompt SHALL 明确隐藏测试点语义只能用于提出泛化检查方向，MUST NOT 用于猜测、复述或反推出隐藏数据。

#### Scenario: 隐藏测试点只有泛化语义
- **WHEN** `testIntentFacts.hidden` 为 true
- **THEN** prompt SHALL 禁止模型声称知道隐藏输入或输出
- **AND** 学生建议 SHALL 使用自构反例、边界手推或状态追踪等泛化动作

