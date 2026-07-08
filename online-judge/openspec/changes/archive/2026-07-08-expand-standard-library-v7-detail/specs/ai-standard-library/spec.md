# AI 标准库 V7 细颗粒度扩张规范

## ADDED Requirements

### Requirement: V7 细颗粒度扩张

系统 SHALL 支持将 V7 标准库种子按独立模块组织，并由主标准库 Catalog 统一汇总。

#### Scenario: 汇总 V7 模块内容

- GIVEN V7 模块中定义了新增能力点和易错点
- WHEN 系统加载标准库种子
- THEN 主 Catalog 返回结果包含 V7 模块条目
- AND 原有 V3-V6 条目仍保持可用

### Requirement: 高频主题细分

系统 SHALL 在高频算法、数据结构、读题和调试主题中增加更细颗粒度的手写能力点与易错点。

#### Scenario: 检查 V7 主题覆盖

- GIVEN 标准库种子已加载
- WHEN 检查 V7 扩张内容
- THEN DP、二分、前缀差分、图搜索、字符串、映射、数值边界和多组状态主题均包含代表性条目
- AND 每个 V7 易错点必须关联一个 V7 能力点
- AND 每个 V7 条目必须关联合法知识节点

### Requirement: 可诊断命名

系统 SHALL 使用具体可诊断的标准库条目命名，避免空泛模板化名称。

#### Scenario: 检查 V7 条目名称

- GIVEN 标准库种子已加载
- WHEN 检查 V7 条目名称和定义
- THEN 条目名称不得使用“理解或应用偏差”这类泛化表达
- AND 易错点定义必须描述一个可观察的错误表现
