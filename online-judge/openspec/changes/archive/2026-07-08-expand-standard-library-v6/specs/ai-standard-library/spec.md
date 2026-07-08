# AI 标准库 V6 增量规范

## ADDED Requirements

### Requirement: 模块化扩张

系统 SHALL 支持将新增标准库种子按独立模块组织，并由主标准库 Catalog 统一汇总。

#### Scenario: 汇总 V6 模块内容

- GIVEN V6 模块中定义了新增能力点和易错点
- WHEN 系统加载标准库种子
- THEN 主 Catalog 返回结果包含 V6 模块条目
- AND 原有 V3-V5 条目仍保持可用

### Requirement: 提高层高频主题扩张

系统 SHALL 在 DP、二分、前缀差分、贪心、枚举剪枝、图论与数据结构主题中增加高质量手写条目。

#### Scenario: 检查 V6 主题覆盖

- GIVEN 标准库种子已加载
- WHEN 检查 V6 扩张内容
- THEN 每个重点主题至少包含代表性能力点和易错点
- AND 易错点必须关联能力点和知识节点
