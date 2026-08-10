## ADDED Requirements

### Requirement: 第六批知识点必须跨六领域形成可教学内容
Flyway V11 SHALL 在不新增知识节点、不修改稳定 code 和不创建高中/竞赛平行树的前提下，精修 48 个仍使用占位描述且没有精确主能力点的启用叶子知识点；ALGO、BASIC、CONTEST、DS、ENG、MATH 每个领域 SHALL 恰好精修 8 个。

#### Scenario: 第六批知识点完成精修
- **WHEN** V11 在生产 V10 基线执行完成
- **THEN** `informatics-knowledge-discipline-v6` 启用知识点 SHALL 恰好为 48 个
- **AND** 六个领域各 SHALL 恰好为 8 个
- **AND** 模板知识点债务 SHALL 从 454 降到不高于 406

#### Scenario: 候选已有精确诊断能力
- **WHEN** 候选知识点在 V10 已有以自身为主锚点的启用正式能力
- **THEN** V11 SHALL NOT 为该节点新增同义能力
- **AND** 维护者 SHALL 从同领域无精确能力的模板叶子中选择替代项

### Requirement: 第六批知识文本必须表达对象边界状态和验证
每个第六批知识点 SHALL 用具体概念对象、适用边界、关键状态或不变量和验证动作表达内容，并 SHALL 提供可观察学习目标、具体典型问题和有效别名。

#### Scenario: 文本仍是模板改写
- **WHEN** 知识点描述只重复名称、使用“细颗粒知识点”“掌握相关知识”或不能说明边界、状态和验证动作
- **THEN** 学科质量门禁 SHALL 失败
- **AND** 该知识点 SHALL NOT 计入第六批完成数量

#### Scenario: 前置与别名无可靠依据
- **WHEN** 第六批知识点没有可靠前置或不同叫法
- **THEN** 相应字段 SHALL 留空
- **AND** 系统 SHALL NOT 机械复制父节点或主名
