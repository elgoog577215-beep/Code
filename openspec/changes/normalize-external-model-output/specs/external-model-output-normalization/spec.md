## ADDED Requirements

### Requirement: 外部模型输出必须先标准化再校验

外部模型 runtime SHALL 在 parse 完模型 JSON 后、执行严格校验前，对诊断输出和教学输出进行确定性标准化。

#### Scenario: 诊断标签使用标准 ID 的不同大小写

- **GIVEN** 模型输出 `primaryIssueTag` 为 `loop_boundary`
- **AND** 标准库允许 `LOOP_BOUNDARY`
- **WHEN** runtime 校验诊断输出
- **THEN** 系统 SHALL 将其标准化为 `LOOP_BOUNDARY`
- **AND** 校验 SHALL 使用标准化后的值。

#### Scenario: 诊断标签使用标准库中文标签

- **GIVEN** 模型输出 `primaryIssueTag` 为 `循环边界`
- **AND** 标准库中 `LOOP_BOUNDARY` 的 label 为 `循环边界`
- **WHEN** runtime 校验诊断输出
- **THEN** 系统 SHALL 将其标准化为 `LOOP_BOUNDARY`
- **AND** 最终响应 SHALL 使用标准 ID。

#### Scenario: 证据引用只有大小写或首尾空白差异

- **GIVEN** brief 中存在证据引用 `code:range_excludes_n`
- **AND** 模型输出证据引用 ` CODE:RANGE_EXCLUDES_N `
- **WHEN** runtime 校验模型输出
- **THEN** 系统 SHALL 将其标准化为 `code:range_excludes_n`
- **AND** 不应因该轻微格式差异触发 evidence ref 失败。

#### Scenario: 未知标签不能被猜测修复

- **GIVEN** 模型输出 `primaryIssueTag` 为 `大概是循环问题`
- **AND** 标准库中没有该 ID 或 label
- **WHEN** runtime 校验诊断输出
- **THEN** 系统 SHALL 保留原值
- **AND** 严格校验 SHALL 返回 `INVALID_TAG`。

### Requirement: 标准化不得绕过安全校验

标准化层 SHALL NOT 删除、改写或降级模型输出中的学生提示正文和安全风险等级。

#### Scenario: 模型输出包含直接答案

- **GIVEN** 模型输出教学提示包含完整代码、最终答案或直接替换指令
- **WHEN** runtime 标准化并校验教学输出
- **THEN** 标准化层 SHALL NOT 删除这些内容
- **AND** 严格校验 SHALL 返回 `SAFETY_RISK`。

#### Scenario: 模型显式标记高泄漏风险

- **GIVEN** 模型输出 `answerLeakRisk` 为 `HIGH`
- **WHEN** runtime 标准化输出
- **THEN** 系统 SHALL 保留 `HIGH`
- **AND** 严格校验 SHALL 返回 `SAFETY_RISK`。
