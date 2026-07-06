## ADDED Requirements

### Requirement: V9 标准库扩展必须补强高频算法与工程诊断主题
AI 标准库 SHALL 新增 V9 手写扩展条目，覆盖滑动窗口、并查集、递归回溯、堆或优先队列、拓扑排序、前缀差分、二分答案、树遍历、map 频次和输出构造等高频诊断主题。

#### Scenario: V9 扩展进入标准库 seed
- **WHEN** 系统加载 AI 标准库 seed
- **THEN** 标准库 SHALL 包含 `SK_V9_`、`MP_V9_` 和 `IP_V9_` 前缀的手写条目
- **AND** V9 条目 SHALL 至少包含 10 个能力点、30 个易错点和 10 个提升点

#### Scenario: V9 条目保持规范化归属
- **WHEN** 系统校验 V9 seed
- **THEN** 每个 V9 易错点 SHALL 归属于合法 V9 能力点
- **AND** 每个 V9 提升点 SHALL 归属于合法 V9 能力点并关联至少一个 V9 易错点
- **AND** 每个 V9 条目引用的知识节点 SHALL 存在于信息学知识树

#### Scenario: V9 条目具备具体教学语义
- **WHEN** 系统校验代表性 V9 条目
- **THEN** 测试 SHALL 验证条目描述、误区或学生收益包含具体错误行为、触发条件、验证动作或提升练习
- **AND** V9 条目名称 SHALL NOT 使用泛化的“理解或应用偏差”作为正式易错点名称
