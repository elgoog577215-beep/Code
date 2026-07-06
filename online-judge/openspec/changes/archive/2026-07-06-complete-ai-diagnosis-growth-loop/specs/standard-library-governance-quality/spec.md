## ADDED Requirements

### Requirement: 标准库治理摘要
系统 SHALL 提供标准库成长候选治理摘要，帮助教师识别待审核积压、重复候选和薄弱知识路径。

#### Scenario: 生成治理摘要
- **WHEN** 教师打开 AI 标准库治理视图
- **THEN** 系统 SHALL 展示候选总数、待审核数、已入库数、拒绝/忽略数和重复聚合数
- **AND** 系统 SHALL 展示出现次数最高的候选路径

#### Scenario: 识别薄弱路径
- **WHEN** 多个待审核或重复候选聚集在同一标准库路径
- **THEN** 治理摘要 SHALL 标记该路径为待补强路径
- **AND** 系统 SHALL 提供对应候选列表入口

#### Scenario: 治理摘要不自动改库
- **WHEN** 系统生成治理摘要
- **THEN** 系统 SHALL NOT 自动创建、删除或合并正式标准库条目
