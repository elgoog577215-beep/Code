## ADDED Requirements

### Requirement: 提示词必须要求独立诊断并同步标注
系统 SHALL 在 `diagnosis-report-v2` 提示词中要求模型先依据题目、完整代码和运行结果判断真实错误，同时参考标准库完成路径标注；标准库 SHALL 被描述为教学坐标系和命名参考，而不是候选答案集合。

#### Scenario: 生成正式诊断提示词
- **WHEN** 系统构造 `diagnosis-report-v2` prompt
- **THEN** prompt SHALL 明确要求模型先输出真实错误原因、关键代码位置和最小修正方向
- **AND** prompt SHALL 明确要求模型同时输出标准库路径、命中状态和库外候选

#### Scenario: 标准库片段不完整
- **WHEN** 标准库片段缺少模型诊断出的具体错误点
- **THEN** prompt SHALL 要求模型保留诊断结论
- **AND** prompt SHALL 要求模型输出待审核标准库候选，而不是硬套相近错误点
