## ADDED Requirements

### Requirement: 第三批正式提升点必须补齐算法与数据结构的可验证练习
V4 新增的正式提升点 SHALL 归属于没有启用提升点的 ALGO 或 DS 正式能力，并 SHALL 将相关易错点转化为学生可执行练习和教师可观察验收。

#### Scenario: 保存第三批提升点
- **WHEN** 一个 `informatics-discipline-quality-v3` 提升点进入正式数据库
- **THEN** 它 SHALL 关联至少一个启用易错点
- **AND** `practice_strategy` SHALL 包含状态表、反例、手算轨迹、边界样例或复杂度预算中的至少一种可执行验证
- **AND** `teacher_explanation` SHALL 说明教师如何观察学生是否真正掌握

### Requirement: 第三批提升点必须保持规范表与兼容结构一致
每个 V4 提升点 SHALL 使用同一稳定 code 同步到规范化提升点、启用平铺快照和 `MAPPED` legacy mapping，并 SHALL 保持能力点、主知识点、相关知识点和关联错因归属一致。

#### Scenario: 三处归属不一致
- **WHEN** V4 提升点在任一兼容结构中缺失、停用或指向不同能力点和主知识点
- **THEN** 数据质量门禁 SHALL 失败
- **AND** 应用 SHALL NOT 带着部分同步内容发布

### Requirement: 第三批代表性导航必须返回完整算法与数据结构闭环
系统 SHALL 对至少一个 ALGO 和一个 DS 第三批主知识点验证诊断层导航，返回所属启用能力点、关联易错点和新增提升点。

#### Scenario: 展开算法知识点
- **WHEN** 教师或 AI 展开第三批 Dijkstra、DP、二分、区间或图算法代表性知识点
- **THEN** 响应 SHALL 包含对应正式能力和 `informatics-discipline-quality-v3` 提升点
- **AND** 提升点 SHALL 保留其关联启用易错点

#### Scenario: 展开数据结构知识点
- **WHEN** 教师或 AI 展开第三批链表、队列、图建模或映射代表性知识点
- **THEN** 响应 SHALL 包含对应正式能力和 `informatics-discipline-quality-v3` 提升点
- **AND** 规范化主结构 SHALL 是返回内容的判断来源
