## ADDED Requirements

### Requirement: 第三批算法与数据结构精修必须形成连贯学科分支
第三批知识树内容精修 SHALL 在不新增节点和不改变稳定 code 的前提下，围绕算法与数据结构的高引用路径形成连贯分支，而不是只重写彼此无关的零散节点。

#### Scenario: 保存第三批知识点
- **WHEN** V4 执行第三批知识点精修
- **THEN** 系统 SHALL 精修 20 个 ALGO 和 10 个 DS 启用模板知识点
- **AND** 内容 SHALL 覆盖最短路、拓扑、区间与贪心、二分、DP、滑动窗口、链表、图建模、队列或映射中的多个连贯路径
- **AND** `library_version` SHALL 标记为 `informatics-knowledge-discipline-v3`

#### Scenario: 精修内容只改写占位模板
- **WHEN** 一个第三批节点的新描述仍不能说明概念对象、状态不变量、适用边界或验证方法
- **THEN** 该节点 SHALL NOT 计入第三批完成数量
- **AND** 质量审校 SHALL 要求补齐可观察学习目标和具体典型任务

### Requirement: 第三批知识点前置与别名必须保持学科有效性
第三批精修节点的 `prerequisites` SHALL 只表示真实学习依赖，`aliases` SHALL 只保存不同叫法、英文术语或课堂常用表达。

#### Scenario: 使用父目录或主名作为内容字段
- **WHEN** 第三批节点把 `parent_code` 机械复制为前置，或把主名原样复制为唯一别名
- **THEN** 学科质量门禁 SHALL 失败
- **AND** 无可靠前置或别名时相应字段 SHALL 留空
