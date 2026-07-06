## ADDED Requirements

### Requirement: 正式诊断上下文包必须保留完整代码和结构邻域
系统 SHALL 在正式诊断上下文包中稳定提供题目完整信息、学生完整代码、判题参考信号和结构化标准库邻域。

#### Scenario: 构造诊断上下文
- **WHEN** 系统构造 `diagnosis-report-v2` 上下文
- **THEN** 上下文 SHALL 包含完整题目描述和完整学生代码
- **AND** 上下文 SHALL 包含判题结果或样例差异作为参考信号
- **AND** 上下文 SHALL 包含相关标准库知识路径、能力点、易错点和提升点邻域

#### Scenario: 标准库上下文不是全库倾倒
- **WHEN** 标准库条目数量超过单次模型上下文预算
- **THEN** 系统 SHALL 传递相关结构包和邻域摘要
- **AND** 系统 SHALL NOT 要求模型从无关全库条目中筛选诊断答案
